 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.plugins.jbossas;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.jboss.jbossnetwork.product.jbpm.handlers.ActionHandlerMessageLog;
import com.jboss.jbossnetwork.product.jbpm.handlers.BaseHandler;
import com.jboss.jbossnetwork.product.jbpm.handlers.ContextVariables;
import com.jboss.jbossnetwork.product.jbpm.handlers.ControlActionFacade;
import com.jboss.jbossnetwork.product.jbpm.handlers.SoftwareValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.SuperState;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.instantiation.Delegation;
import org.jbpm.instantiation.FieldInstantiator;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.pluginapi.content.ContentContext;

/**
 * Class responsible for managing the running of a JBPM process to apply a patch to a JBoss instance. This class will
 * take care of populating the context with whatever variables are necessary, including the creation of any temporary
 * directories that may be needed. The results of the workflow run will be parsed and converted into the necessary
 * domain model and returned to the caller.
 *
 * @author Jason Dobies
 */
public class JBPMWorkflowManager {
    private ContentContext contentContext;
    private ControlActionFacade controlFacade;
    private Configuration pluginConfiguration;

    private final Log log = LogFactory.getLog(this.getClass());

    public JBPMWorkflowManager(ContentContext contentContext, ControlActionFacade controlFacade,
        Configuration pluginConfiguration) {
        this.contentContext = contentContext;
        this.controlFacade = controlFacade;
        this.pluginConfiguration = pluginConfiguration;
    }

    /**
     * Runs the JBPM process included in the provided package description. This method will make calls back into
     * the plugin container as necessary to retrieve information or execute operations.
     *
     * @param packageDetails contains data to describe the package being installed
     * @return plugin container domain model representation of the result of attempting to install the package
     * @throws Exception if there are any unexpected errors during the process
     */
    public DeployIndividualPackageResponse run(ResourcePackageDetails packageDetails) throws Exception {

        checkCompatibility(packageDetails);

        // Grab the JBPM process
        byte[] metadataBytes = packageDetails.getMetadata();
        if (metadataBytes == null) {
            return null;
        }
        String process = new String(metadataBytes);

        // Generate the list of steps to execute first. If the workflow fails, we won't have log entries for the
        // unexecuted steps. Link these steps up against the log and include unexecuted steps from this list
        // in the response.
        List<DeployPackageStep> unexecutedSteps = translateSteps(packageDetails);

        // Load the JBPM process into the executor
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(process);
        ProcessInstance processInstance = new ProcessInstance(processDefinition);
        ContextInstance context = processInstance.getContextInstance();

        // Populate the variables we'll need in the handlers
        context.setVariable(ContextVariables.CONTENT_CONTEXT, contentContext);
        context.setVariable(ContextVariables.CONTROL_ACTION_FACADE, controlFacade);
        context.setVariable(ContextVariables.PACKAGE_DETAILS_KEY, packageDetails.getKey());

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date timestamp = new Date();
        String formattedTimestamp = format.format(timestamp);
        context.setVariable(ContextVariables.TIMESTAMP, formattedTimestamp);

        try {
            File downloadDir = createTempDir("jon_download");
            if (downloadDir != null) {
                context.setVariable(ContextVariables.DOWNLOAD_DIR, downloadDir.getAbsolutePath());
            }

            File patchDir = createTempDir("jon_patch");
            if (patchDir != null) {
                context.setVariable(ContextVariables.PATCH_DIR, patchDir.getAbsolutePath());
            }
        } catch (IOException e) {
            // No need to throw this error, a handler will check for these to be valid and fail the step accordingly
            log.error("Error creating temporary directories", e);
        }

        // Populate the variables describing the AS instance
        String jbossHomeDir = safeGet(pluginConfiguration, JBossASServerComponent.JBOSS_HOME_DIR_CONFIG_PROP);
        jbossHomeDir += File.separator; // Just to make sure it ends with the separator
        context.setVariable(ContextVariables.JBOSS_HOME_DIR, jbossHomeDir);

        String jbossClientDir = jbossHomeDir + File.separator + "client" + File.separator;
        context.setVariable(ContextVariables.JBOSS_CLIENT_DIR, jbossClientDir);

        String jbossServerDir = safeGet(pluginConfiguration, JBossASServerComponent.CONFIGURATION_PATH_CONFIG_PROP);
        jbossServerDir += File.separator; // Just to make sure
        context.setVariable(ContextVariables.JBOSS_SERVER_DIR, jbossServerDir);

        // The workflow will reference values inside of this object for substitution
        // Ultimately, we should parse out the workflows to use the domain object directly
        SoftwareValue softwareValue = resourcePackageDetailsToSoftwareValue(packageDetails);
        context.setVariable(ContextVariables.SOFTWARE, softwareValue);

        // Perform the workflow
        try {
            processInstance.signal();
        } catch (Exception e) {
            log.error("Error received from the workflow", e);
        }

        // Parse the logs from the workflow execution into the domain model
        List logs = processInstance.getLoggingInstance().getLogs();
        DeployIndividualPackageResponse response = parseLogs(logs, unexecutedSteps, packageDetails.getKey());

        return response;
    }

    /**
     * Translates the metadata inside the given package into a list of readable steps that will be executed
     * during this package's installation. This call will <em>not</em> execute the package deployment nor cause
     * any changes to be made to the resource.
     *
     * @param packageDetails generate the steps for this package; cannot be <code>null</code>
     * @return translated steps if the metadata in this package was populated; <code>null</code> otherwise.
     * @throws Exception if there is an error during the translation
     */
    public List<DeployPackageStep> translateSteps(ResourcePackageDetails packageDetails) throws Exception {

        checkCompatibility(packageDetails);

        // Grab the JBPM process
        byte[] metadataBytes = packageDetails.getMetadata();
        if (metadataBytes == null) {
            return null;
        }

        String process = new String(metadataBytes);

        // Load the JBPM process into the executor
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(process);

        // Iterate over the nodes manually, creating a domain representation of the step in the process
        SuperState mainProcess = (SuperState) processDefinition.getNode("main_process");
        if (mainProcess == null) {
            log.warn("Could not retrieve main process for package [" + packageDetails + "]");
            return null;
        }

        List<DeployPackageStep> steps = new ArrayList<DeployPackageStep>();
        List<Node> nodes = mainProcess.getNodes();

        for (Node node : nodes) {

            Action action = node.getAction();
            if (action != null) {

                Delegation delegation = action.getActionDelegation();
                String configProps = delegation.getConfiguration();
                String actionHandlerClassName = delegation.getClassName();
                FieldInstantiator instantiator = new FieldInstantiator();

                BaseHandler handler = (BaseHandler) instantiator.instantiate(Class.forName(actionHandlerClassName),
                    configProps);
                handler.setPropertyDefaults();

                String description = handler.getDescription();

                DeployPackageStep step = new DeployPackageStep(node.getName(), description);
                step.setStepResult(ContentResponseResult.NOT_PERFORMED);
                steps.add(step);
            }
        }

        return steps;
    }

    /**
     * Parses through the entire log list returned from the workflow application and converts the relevant entries into
     * the domain model's package installation steps. Any steps that are not present in the log list will be added
     * from the unexecuted step list and marked as "not executed".
     *
     * @param logs              logs made available from the process instance after executing the workflow
     * @param unexecutedSteps   list of all steps that are to be executed, generated prior to running the workflow
     * @param packageDetailsKey identifies the package that was deployed
     * @return domain representation of the result of the workflow
     */
    private DeployIndividualPackageResponse parseLogs(List logs, List<DeployPackageStep> unexecutedSteps,
        PackageDetailsKey packageDetailsKey) {

        List<DeployPackageStep> steps = new ArrayList<DeployPackageStep>();
        ContentResponseResult overallResult = ContentResponseResult.SUCCESS;

        for (Object uncastedLog : logs) {
            if (uncastedLog instanceof ActionHandlerMessageLog) {
                ActionHandlerMessageLog messageLog = (ActionHandlerMessageLog) uncastedLog;
                DeployPackageStep executedStep = messageLog.getStep();
                steps.add(executedStep);

                // The executed and unexecuted steps are link by their step key and the fact that the equals method
                // uses this key (as such, their descriptions may be different). This is currently simply a counter.
                // That counter is handled in different ways for translate v. logging an actual step. They should
                // still map up, however this log message should output enough of a description for the reader
                // to ensure that the unexecuted step being removed does correspond to the step found in the logs.
                if (log.isDebugEnabled()) {

                    String executedStepKey = executedStep.getStepKey();

                    DeployPackageStep unexecutedStep = null;
                    for (DeployPackageStep s : unexecutedSteps) {
                        if (s.getStepKey().equals(executedStepKey)) {
                            unexecutedStep = s;
                            break;
                        }
                    }

                    if (unexecutedStep != null && log.isDebugEnabled()) {
                        log.debug("Mapped up steps:");
                        log.debug("Executed Step: " + executedStep);
                        log.debug("Unexecuted Step: " + unexecutedStep);
                    }
                }

                boolean unexecutedStepDeleted = unexecutedSteps.remove(executedStep);
                if (!unexecutedStepDeleted) {
                    log.warn("Could not remove the following step from the unexecuted step list: " + executedStep);
                }

                // If any steps fail, mark the entire response as failed
                if (executedStep.getStepResult() == ContentResponseResult.FAILURE) {
                    overallResult = ContentResponseResult.FAILURE;
                }
            }
        }

        // For every step that was supposed to run but didn't (those remaining in the unexecuted step list), tack
        // them on to the end as not executed.
        for (DeployPackageStep unexecutedStep : unexecutedSteps) {
            unexecutedStep.setStepResult(ContentResponseResult.NOT_PERFORMED);
            steps.add(unexecutedStep);
        }

        DeployIndividualPackageResponse response = new DeployIndividualPackageResponse(packageDetailsKey, overallResult);
        response.setDeploymentSteps(steps);

        return response;
    }

    /**
     * Creates a temporary directory in which to store the bits being downloaded from the server. The name of the
     * directory will be generated by Java's built in algorithm and returned to the caller.
     *
     * @param prefix will be the first part of the file, with Java file utilities coming up with the remainder of the
     *               file name
     * @return name of the unique, randomly generated temporary directory
     * @throws IOException if the process cannot create a temporary file
     */
    private File createTempDir(String prefix) throws IOException {
        // Let's reuse the algorithm the JDK uses to determine a unique name:
        // 1) create a temp file to get a unique name using JDK createTempFile
        // 2) then quickly delete the file and...
        // 3) convert it to a directory
        // ccrouch, version 1.4

        File tmpDir = File.createTempFile(prefix, "", null); // create file with unique name
        boolean deleteSuccessful = tmpDir.delete(); // delete the tmp file and...
        boolean mkdirSuccessful = tmpDir.mkdirs(); // ...convert it to a directory

        if (deleteSuccessful && mkdirSuccessful) {
            return tmpDir;
        } else {
            return null;
        }
    }

    /**
     * Will throw an exception if the package does not contain a set of compatible installation instructions.
     *
     * @param packageDetails package in question
     * @throws UnsupportedOperationException if the instruction compatibility version is not supported
     */
    private void checkCompatibility(ResourcePackageDetails packageDetails) {
        Configuration extraProperties = packageDetails.getExtraProperties();

        // Check to make sure we know how to parse the instruction set
        String compatibilityVersion = safeGet(extraProperties, "instructionCompatibilityVersion");
        if (compatibilityVersion != null && !compatibilityVersion.equals("1.4")) {
            throw new UnsupportedOperationException("Instruction set for this package is not supported. Version: "
                + compatibilityVersion);
        }
    }

    /**
     * Converts the domain model package representation into the legacy software object that is referenced from the JBPM
     * workflows.
     *
     * @param pkg sent from the server to be installed, the values for the software entity will be taken from here
     * @return object mirroring the domain's package representation usable for substitutions into the workflow
     */
    private SoftwareValue resourcePackageDetailsToSoftwareValue(ResourcePackageDetails pkg) {
        Configuration extraProperties = pkg.getExtraProperties();

        SoftwareValue softwareValue = new SoftwareValue();

        softwareValue.setDownloadUrl(pkg.getLocation());
        softwareValue.setFilename(pkg.getFileName());
        softwareValue.setFileSize(pkg.getFileSize());
        softwareValue.setInstructionCompatibilityVersion(safeGet(extraProperties, "instructionCompatibilityVersion"));
        softwareValue.setIssueReference(safeGet(extraProperties, "jiraId"));
        softwareValue.setLicenseName(pkg.getLicenseName());
        softwareValue.setLicenseVersion(pkg.getLicenseVersion());
        softwareValue.setLongDescription(pkg.getLongDescription());
        softwareValue.setMD5(pkg.getMD5());
        softwareValue.setSHA256(pkg.getSHA265());
        softwareValue.setShortDescription(pkg.getShortDescription());
        softwareValue.setTitle(pkg.getName());

        return softwareValue;
    }

    /**
     * Utility to extract a potentially null value from a configuration.
     *
     * @param configuration may be <code>null</code>
     * @param key           value being retrieved
     * @return value if it is found in the configuration; <code>null</code> otherwise.
     */
    private String safeGet(Configuration configuration, String key) {
        if (configuration == null) {
            return null;
        }

        PropertySimple simple = configuration.getSimple(key);

        return (simple != null) ? simple.getStringValue() : null;
    }
}