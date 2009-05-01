/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.plugins.cli;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * Represents a managed resource whose management interface is a command line executable.
 *
 * @author John Mazzitelli
 */
public class CliServerComponent implements ResourceComponent, MeasurementFacet, OperationFacet, ConfigurationFacet {
    private final Log log = LogFactory.getLog(CliServerComponent.class);

    protected static final String PLUGINCONFIG_EXECUTABLE = "executable";
    protected static final String PLUGINCONFIG_WORKINGDIR = "workingDirectory";
    protected static final String PLUGINCONFIG_ENVVARS = "environmentVariables";
    protected static final String PLUGINCONFIG_ENVVAR_NAME = "name";
    protected static final String PLUGINCONFIG_ENVVAR_VALUE = "value";
    protected static final String PLUGINCONFIG_AVAIL_EXECUTE_CHECK = "availabilityExecuteCheck";
    protected static final String PLUGINCONFIG_AVAIL_EXITCODE_REGEX = "availabilityExitCodeRegex";
    protected static final String PLUGINCONFIG_AVAIL_OUTPUT_REGEX = "availabilityOutputRegex";
    protected static final String PLUGINCONFIG_AVAIL_ARGS = "availabilityArguments";
    protected static final String PLUGINCONFIG_VERSION_ARGS = "versionArguments";
    protected static final String PLUGINCONFIG_FIXED_VERSION = "fixedVersion";
    protected static final String PLUGINCONFIG_DESC_ARGS = "descriptionArguments";
    protected static final String PLUGINCONFIG_FIXED_DESC = "fixedDescription";

    protected static final String OPERATION_PARAM_ARGUMENTS = "arguments";
    protected static final String OPERATION_RESULT_EXIT_CODE = "exitCode";
    protected static final String OPERATION_RESULT_OUTPUT = "output";

    private Configuration resourceConfiguration;
    private ResourceContext resourceContext;

    public void start(ResourceContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Cli Server started: " + context.getPluginConfiguration());
        }

        this.resourceContext = context;
    }

    public void stop() {
        if (log.isDebugEnabled()) {
            log.debug("Cli Server stopped: " + this.resourceContext.getPluginConfiguration());
        }
    }

    public AvailabilityType getAvailability() {
        boolean result = checkCliAvailability();
        return result ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        for (MeasurementScheduleRequest request : requests) {
            String name = request.getName();

            // TODO: based on the request information, you must collect the requested measurement(s)
            //       you can use the name of the measurement to determine what you actually need to collect
            try {
                Number value = new Integer(1); // dummy measurement value - this should come from the managed resource
                report.addData(new MeasurementDataNumeric(request, value.doubleValue()));
            } catch (Exception e) {
                log.error("Failed to obtain measurement [" + name + "]. Cause: " + e);
            }
        }

        return;
    }

    /**
     * Invokes the CLI executable. User can tell us what arguments to pass to the executable.
     * The result includes the output of the process as well as the exit code.
     * 
     * @see OperationFacet#invokeOperation(String, Configuration)
     */
    public OperationResult invokeOperation(String name, Configuration configuration) throws Exception {

        OperationResult result = new OperationResult();

        String arguments = configuration.getSimpleValue(OPERATION_PARAM_ARGUMENTS, null);

        ProcessExecutionResults exeResults = executeCliExecutable(arguments, 3600000L, true); // wait no more than 1 hour
        Integer exitcode = exeResults.getExitCode();
        String output = exeResults.getCapturedOutput();
        Throwable error = exeResults.getError();

        if (exitcode == null) {
            exitcode = Integer.valueOf(-999);
        }

        if (output == null) {
            output = "";
        }

        if (error != null) {
            result.setErrorMessage(ThrowableUtil.getAllMessages(error));
        }

        Configuration resultsConfig = result.getComplexResults();
        resultsConfig.put(new PropertySimple(OPERATION_RESULT_EXIT_CODE, exitcode));
        resultsConfig.put(new PropertySimple(OPERATION_RESULT_OUTPUT, output));

        return result;
    }

    /**
     * The plugin container will call this method and it needs to obtain the current configuration of the managed
     * resource. Your plugin will obtain the managed resource's configuration in your own custom way and populate the
     * returned Configuration object with the managed resource's configuration property values.
     *
     * @see ConfigurationFacet#loadResourceConfiguration()
     */
    public Configuration loadResourceConfiguration() {
        // here we simulate the loading of the managed resource's configuration

        if (resourceConfiguration == null) {
            // for this example, we will create a simple dummy configuration to start with.
            // note that it is empty, so we're assuming there are no required configs in the plugin descriptor.
            resourceConfiguration = new Configuration();
        }

        Configuration config = resourceConfiguration;

        return config;
    }

    /**
     * The plugin container will call this method when it has a new configuration for your managed resource. Your plugin
     * will re-configure the managed resource in your own custom way, setting its configuration based on the new values
     * of the given configuration.
     *
     * @see ConfigurationFacet#updateResourceConfiguration(ConfigurationUpdateReport)
     */
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // this simulates the plugin taking the new configuration and reconfiguring the managed resource
        resourceConfiguration = report.getConfiguration().deepCopy();

        report.setStatus(ConfigurationUpdateStatus.SUCCESS);
    }

    protected ResourceContext getResourcContext() {
        return this.resourceContext;
    }

    protected ProcessExecutionResults executeCliExecutable(String args, long wait, boolean captureOutput)
        throws InvalidPluginConfigurationException {

        SystemInfo sysInfo = this.resourceContext.getSystemInformation();
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        return executeCliExecutable(sysInfo, pluginConfig, args, wait, captureOutput);
    }

    // This is protected static so the discovery component can use it.
    protected static ProcessExecutionResults executeCliExecutable(SystemInfo sysInfo, Configuration pluginConfig,
        String args, long wait, boolean captureOutput) throws InvalidPluginConfigurationException {

        ProcessExecution processExecution = getProcessExecutionInfo(pluginConfig);
        if (args != null) {
            processExecution.setArguments(args.split(" "));
        }
        processExecution.setCaptureOutput(captureOutput);
        processExecution.setWaitForCompletion(wait);
        processExecution.setKillOnTimeout(true);

        ProcessExecutionResults results = sysInfo.executeProcess(processExecution);
        return results;
    }

    private boolean checkCliAvailability() throws InvalidPluginConfigurationException {

        String executable;
        boolean availExecuteCheck = false;
        String availArgs = null;
        String availExitCodeRegex = null;
        String availOutputRegex = null;

        // determine how we are to consider the CLI available by looking at the plugin configuration
        try {
            Configuration pc = this.resourceContext.getPluginConfiguration();

            executable = pc.getSimpleValue(PLUGINCONFIG_EXECUTABLE, null);
            if (executable == null) {
                throw new Exception("Missing executable in plugin configuraton");
            }

            PropertySimple availExecuteCheckProp = pc.getSimple(PLUGINCONFIG_AVAIL_EXECUTE_CHECK);
            PropertySimple availArgsProp = pc.getSimple(PLUGINCONFIG_AVAIL_ARGS);
            PropertySimple availExitCodeRegexProp = pc.getSimple(PLUGINCONFIG_AVAIL_EXITCODE_REGEX);
            PropertySimple availOutputRegexProp = pc.getSimple(PLUGINCONFIG_AVAIL_OUTPUT_REGEX);

            if (availExecuteCheckProp != null && availExecuteCheckProp.getBooleanValue() != null) {
                availExecuteCheck = availExecuteCheckProp.getBooleanValue().booleanValue();
            }

            if (availArgsProp != null && availArgsProp.getStringValue() != null) {
                availArgs = availArgsProp.getStringValue();
            }

            if (availExitCodeRegexProp != null && availExitCodeRegexProp.getStringValue() != null) {
                availExitCodeRegex = availExitCodeRegexProp.getStringValue();
            }

            if (availOutputRegexProp != null && availOutputRegexProp.getStringValue() != null) {
                availOutputRegex = availOutputRegexProp.getStringValue();
            }
        } catch (Exception e) {
            throw new InvalidPluginConfigurationException("Cannot get avail plugin config. Cause: " + e);
        }

        // first, make sure the executable actually exists
        File executableFile = new File(executable);
        if (!executableFile.exists()) {
            if (log.isDebugEnabled()) {
                logDebug("The executable [" + executable + "] does not exist - resource is considered DOWN");
            }
            return false;
        }

        // if we need to check the exit code or output, execute the CLI now
        if (availExecuteCheck || availExitCodeRegex != null || availOutputRegex != null) {
            ProcessExecutionResults results = executeCliExecutable(availArgs, 3000L, (availOutputRegex != null));

            // if we get some error while trying to run the executable, immediately consider the resource down
            if (results.getError() != null) {
                if (log.isDebugEnabled()) {
                    logDebug("CLI execution encountered an error, resource is considered DOWN: "
                        + ThrowableUtil.getAllMessages(results.getError()));
                }
                return false;
            }

            // if the exit code is used to determine availability, check it now
            if (availExitCodeRegex != null) {
                if (results.getExitCode() == null) {
                    if (log.isDebugEnabled()) {
                        logDebug("Cannot get exit code, resource is considered DOWN");
                    }
                    return false;
                }

                boolean exitcodeMatches = results.getExitCode().toString().matches(availExitCodeRegex);
                if (!exitcodeMatches) {
                    if (log.isDebugEnabled()) {
                        logDebug("CLI exit code=[" + results.getExitCode() + "] != [" + availExitCodeRegex + "]. DOWN");
                    }
                    return false;
                }
            }

            // if the output is used to determine availability, check it now
            if (availOutputRegex != null) {
                String output = results.getCapturedOutput();
                if (output == null) {
                    output = "";
                }

                boolean outputMatches = output.matches(availOutputRegex);
                if (!outputMatches) {
                    if (log.isDebugEnabled()) {
                        String outputToLog = output;
                        if (outputToLog.length() > 100) {
                            outputToLog = outputToLog.substring(0, 100) + "...";
                        }
                        logDebug("CLI output [" + outputToLog + "] did not match regex [" + availOutputRegex
                            + "], resource is considered DOWN");
                    }
                    return false;
                }
            }
        }

        // everything passes, resource is UP
        return true;
    }

    private static ProcessExecution getProcessExecutionInfo(Configuration pluginConfig)
        throws InvalidPluginConfigurationException {

        PropertySimple executableProp = pluginConfig.getSimple(PLUGINCONFIG_EXECUTABLE);
        PropertySimple workingDirProp = pluginConfig.getSimple(PLUGINCONFIG_WORKINGDIR);
        PropertyList envvarsProp = pluginConfig.getList(PLUGINCONFIG_ENVVARS);

        String executable = null;
        String workingDir = null;
        Map<String, String> envvars = null;

        if (executableProp == null) {
            throw new InvalidPluginConfigurationException("Missing required plugin config: " + PLUGINCONFIG_EXECUTABLE);
        } else {
            executable = executableProp.getStringValue();
            if (executable == null || executable.length() == 0) {
                throw new InvalidPluginConfigurationException("Bad plugin config: " + PLUGINCONFIG_EXECUTABLE);
            }
        }

        if (workingDirProp != null) {
            workingDir = workingDirProp.getStringValue();
            if (workingDir != null && workingDir.length() == 0) {
                workingDir = null; // empty string is as good as unsetting it (i.e. making it null)
            }
        }

        if (envvarsProp != null) {
            envvars = new HashMap<String, String>();
            try {
                List<Property> listOfMaps = envvarsProp.getList();
                for (Property envvarMap : listOfMaps) {
                    PropertySimple name = (PropertySimple) ((PropertyMap) envvarMap).get(PLUGINCONFIG_ENVVAR_NAME);
                    PropertySimple value = (PropertySimple) ((PropertyMap) envvarMap).get(PLUGINCONFIG_ENVVAR_VALUE);
                    envvars.put(name.getStringValue(), value.getStringValue());
                }
            } catch (Exception e) {
                throw new InvalidPluginConfigurationException("Bad plugin config: " + PLUGINCONFIG_ENVVARS
                    + ". Cause: " + e);
            }
        }

        ProcessExecution processExecution = new ProcessExecution(executable);
        processExecution.setEnvironmentVariables(envvars);
        processExecution.setWorkingDirectory(workingDir);

        return processExecution;
    }

    private void logDebug(String msg) {
        log.debug("[" + this.resourceContext.getResourceKey() + "]: " + msg);
    }
}