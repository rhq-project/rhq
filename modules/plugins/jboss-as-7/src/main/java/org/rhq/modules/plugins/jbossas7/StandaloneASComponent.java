/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.rhq.core.pluginapi.bundle.BundleHandoverResponse.FailureType.EXECUTION;
import static org.rhq.core.pluginapi.bundle.BundleHandoverResponse.FailureType.INVALID_ACTION;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.bundle.BundleHandoverRequest;
import org.rhq.core.pluginapi.bundle.BundleHandoverResponse;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.support.SnapshotReportRequest;
import org.rhq.core.pluginapi.support.SnapshotReportResults;
import org.rhq.core.pluginapi.support.SupportFacet;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.modules.plugins.jbossas7.helper.AdditionalJavaOpts;
import org.rhq.modules.plugins.jbossas7.helper.JdrReportRunner;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Component class for standalone AS7 servers.
 *
 * @author Heiko W. Rupp
 */
public class StandaloneASComponent<T extends ResourceComponent<?>> extends BaseServerComponent<T> implements
    MeasurementFacet, OperationFacet, SupportFacet {

    private static final Log LOG = LogFactory.getLog(StandaloneASComponent.class);

    private static final String SERVER_CONFIG_TRAIT = "config-file";
    private static final String MULTICAST_ADDRESS_TRAIT = "multicastAddress";
    private static final String DEPLOY_DIR_TRAIT = "deployDir";
    private static final String TEMP_DIR_TRAIT = "temp-dir";

    private static final String JAVA_OPTS_ADDITIONAL_PROP = "javaOptsAdditional";

    private static final Address ENVIRONMENT_ADDRESS = new Address("core-service=server-environment");

    @Override
    public void start(ResourceContext<T> resourceContext) throws Exception {
        super.start(resourceContext);
        updateAdditionalJavaOpts(resourceContext);
    }

    @Override
    protected AS7Mode getMode() {
        return AS7Mode.STANDALONE;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        Set<MeasurementScheduleRequest> leftovers = new HashSet<MeasurementScheduleRequest>(requests.size());
        for (MeasurementScheduleRequest request : requests) {
            String requestName = request.getName();
            if (requestName.equals(SERVER_CONFIG_TRAIT)) {
                collectConfigTrait(report, request);
            } else if (requestName.equals(MULTICAST_ADDRESS_TRAIT)) {
                collectMulticastAddressTrait(report, request);
            } else if (requestName.equals(DEPLOY_DIR_TRAIT)) {
                resolveDeployDir(report, request);
            } else {
                leftovers.add(request); // handled below
            }
        }

        super.getValues(report, leftovers);
    }

    /**
     * Try to determine the deployment directory (usually $as/standalone/deployments ).
     * For JDG we return fake data, as JDG does not have such a directory.
     * @param report Measurement report to tack the value on
     * @param request Measurement request with the schedule id to use
     */
    private void resolveDeployDir(MeasurementReport report, MeasurementScheduleRequest request) {

        if ("JDG".equals(pluginConfiguration.getSimpleValue("productType", "AS7"))) {
            LOG.debug("This is a JDG server, so there is no deployDir");
            MeasurementDataTrait trait = new MeasurementDataTrait(request, "- not applicable to JDG -");
            report.addData(trait);
            return;
        }

        // So we have an AS7/EAP6
        Address scanner = new Address("subsystem=deployment-scanner,scanner=default");
        ReadResource op = new ReadResource(scanner);
        Result res = getASConnection().execute(op);
        if (res.isSuccess()) {
            @SuppressWarnings("unchecked")
            Map<String, String> scannerMap = (Map<String, String>) res.getResult();
            String path = scannerMap.get("path");
            String relativeTo = scannerMap.get("relative-to");
            File basePath = resolveRelativePath(relativeTo);

            // It is safe to use File.separator, as the agent we are running in, will also lay down the plugins
            String deployDir = new File(basePath, path).getAbsolutePath();

            MeasurementDataTrait trait = new MeasurementDataTrait(request, deployDir);
            report.addData(trait);
        } else {
            LOG.error("No default deployment scanner was found, returning no value");
        }
    }

    private File resolveRelativePath(String relativeTo) {

        Address addr = new Address("path", relativeTo);
        ReadResource op = new ReadResource(addr);
        Result res = getASConnection().execute(op);
        if (res.isSuccess()) {
            @SuppressWarnings("unchecked")
            Map<String, String> pathMap = (Map<String, String>) res.getResult();
            String path = pathMap.get("path");
            String relativeToProp = pathMap.get("relative-to");
            if (relativeToProp == null)
                return new File(path);
            else {
                File basePath = resolveRelativePath(relativeToProp);
                return new File(basePath, path);
            }
        }
        LOG.warn("The requested path property " + relativeTo + " is not registered in the server, so not resolving it.");
        return new File(relativeTo);
    }

    @Override
    protected Address getServerAddress() {
        return getAddress();
    }

    @Override
    protected String getSocketBindingGroup() {
        // TODO (ips): Can this ever be something other than "standard-sockets"?
        return "standard-sockets";
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if (name.equals("start")) {
            return startServer();
        } else if (name.equals("restart")) {
            return restartServer(parameters);
        } else if (name.equals("installRhqUser")) {
            return installManagementUser(parameters, pluginConfiguration);
        } else if (name.equals("executeCommands") || name.equals("executeScript")) {
            return runCliCommand(parameters);
        }

        // reload, shutdown go to the remote server
        Operation op = new Operation(name, new Address());
        Result res = getASConnection().execute(op);

        OperationResult operationResult = postProcessResult(name, res);

        if (name.equals("shutdown")) {
            if (waitUntilDown()) {
                operationResult.setSimpleResult("Success");
            } else {
                operationResult.setErrorMessage("Was not able to shut down the server.");
            }
        }

        if (name.equals("reload")) {
            if (waitUntilReloaded()) {
                operationResult.setSimpleResult("Success");
            } else {
                operationResult.setErrorMessage("Was not able to reload the server.");
            }
        }

        context.getAvailabilityContext().requestAvailabilityCheck();

        return operationResult;
    }

    private boolean waitUntilReloaded() throws InterruptedException {
        boolean reloaded = false;
        while (!reloaded) {
            Operation op = new ReadAttribute(new Address(), "release-version");
            try {
                Result res = getASConnection().execute(op);
                if (res.isSuccess() && !res.isReloadRequired()) {
                    reloaded = true;
                }
            } catch (Exception e) {
                //do absolutely nothing
                //if an exception is thrown that means the server is still reloading, so consider this
                //a single failed attempt, equivalent to res.isSuccess == false
            }
            if (!reloaded) {
                if (context.getComponentInvocationContext().isInterrupted()) {
                    // Operation canceled or timed out
                    throw new InterruptedException();
                }
                Thread.sleep(SECONDS.toMillis(1));
            }
        }
        return reloaded;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // We need to filter the path properties that are marked with the read-only flag
        // This is done by setting the logical removed flag on the map to signal
        // the write delegate to skip the map
        Configuration config = report.getConfiguration();
        PropertyList propertyList = config.getList("*3");
        for (Property property : propertyList.getList()) {
            PropertyMap map = (PropertyMap) property;
            String ro = map.getSimpleValue("read-only", "false");
            if (Boolean.parseBoolean(ro)) {
                map.setErrorMessage(ConfigurationWriteDelegate.LOGICAL_REMOVED);
            }
        }
        super.updateResourceConfiguration(report);
    }

    /**
     * Handles content handed over during a bundle deployment.<br>
     * <br>
     * This component supports the following actions:<br>
     * <br>
     * <strong>action = deployment: deploys the content to the server</strong><br>
     * <br>
     * Optional parameters:<br>
     * <ul>
     *     <li>runtimeName: Runtime name of the uploaded file (e.g. 'my.war'); if not present, the file name is used</li>
     * </ul>
     * <br>
     * <strong>action = execute-script: executes a server CLI script</strong><br>
     * <br>
     * Optional parameters:<br>
     * <ul>
     *     <li>waitTime (in seconds): how long to wait for completion; defaults to an hour</li>
     *     <li>killOnTimeout (true/false): should the CLI process be killed if timeout is reached; defaults to false</li>
     * </ul>
     *
     * @param handoverRequest handover parameters and context
     * @return a report object indicating success or failure
     */
    @Override
    public BundleHandoverResponse handleContent(BundleHandoverRequest handoverRequest) {
        try {
            if (handoverRequest.getAction().equals("deployment")) {
                return handleDeployment(handoverRequest);
            }
            if (handoverRequest.getAction().equals("execute-script")) {
                return handleExecuteScript(handoverRequest);
            }
            if (handoverRequest.getAction().equals("patch")) {
                return deployPatch(handoverRequest);
            }
            return BundleHandoverResponse.failure(INVALID_ACTION);
        } catch (Exception e) {
            return BundleHandoverResponse.failure(EXECUTION, "Unexpected handover failure", e);
        }
    }

    private BundleHandoverResponse handleDeployment(BundleHandoverRequest request) {
        HandoverContentUploader contentUploader = new HandoverContentUploader(request, getASConnection());
        boolean uploaded = contentUploader.upload();
        if (!uploaded) {
            return contentUploader.getFailureResponse();
        }

        String filename = contentUploader.getFilename();
        String runtimeName = contentUploader.getRuntimeName();
        String hash = contentUploader.getHash();

        Redeployer redeployer = new Redeployer(runtimeName, hash, getASConnection());
        if (redeployer.deploymentExists()) {
            Result result = redeployer.redeployOnServer();
            if (result.isRolledBack()) {
                return BundleHandoverResponse.failure(EXECUTION, result.getFailureDescription());
            }
            return BundleHandoverResponse.success();
        }

        Operation addDeploymentStep = new Operation("add", "deployment", filename);
        List<Object> addDeploymentContentProperty = new ArrayList<Object>(1);
        Map<String, Object> contentValues = new HashMap<String, Object>();
        contentValues.put("hash", new PROPERTY_VALUE("BYTES_VALUE", hash));
        addDeploymentContentProperty.add(contentValues);
        addDeploymentStep.addAdditionalProperty("content", addDeploymentContentProperty);
        addDeploymentStep.addAdditionalProperty("name", filename);
        addDeploymentStep.addAdditionalProperty("runtime-name", runtimeName);

        Operation deployStep = new Operation("deploy", addDeploymentStep.getAddress());

        CompositeOperation compositeOperation = new CompositeOperation();
        compositeOperation.addStep(addDeploymentStep);
        compositeOperation.addStep(deployStep);

        Result result = getASConnection().execute(compositeOperation, 300);
        if (!result.isSuccess()) {
            return BundleHandoverResponse.failure(EXECUTION, result.getFailureDescription());
        } else {
            return BundleHandoverResponse.success();
        }
    }

    @NotNull
    @Override
    protected Address getEnvironmentAddress() {
        return ENVIRONMENT_ADDRESS;
    }

    @NotNull
    @Override
    protected Address getHostAddress() {
        // In standalone mode, the root address is the host address.
        return getAddress();
    }

    @NotNull
    @Override
    protected String getBaseDirAttributeName() {
        return "base-dir";
    }

    @Override
    protected String getTempDirAttributeName() {
        return TEMP_DIR_TRAIT;
    }

    /**
     * Updates JAVA_OPTS in standalone.conf and standalone.conf.bat files.
     * If JAVA_OPTS is set, then new config is added or updated in the file
     * If JAVA_OPTS is unset, then the config file will be cleared of any traced the config set via RHQ
     */
    private void updateAdditionalJavaOpts(ResourceContext<T> resourceContext) {
        if (resourceContext.getPluginConfiguration().getSimpleValue(ServerPluginConfiguration.Property.HOME_DIR) == null) {
            LOG.error("Additional JAVA_OPTS cannot be configured because "
                + ServerPluginConfiguration.Property.HOME_DIR + " property not set");
            return;
        }

        File baseDirectory = new File(resourceContext.getPluginConfiguration().getSimpleValue(
            ServerPluginConfiguration.Property.HOME_DIR));
        File binDirectory = new File(baseDirectory, "bin");

        String additionalJavaOptsContent = resourceContext.getPluginConfiguration().getSimpleValue(
            JAVA_OPTS_ADDITIONAL_PROP);

        File configFile;
        AdditionalJavaOpts additionalJavaOptsConfig;
        if (OperatingSystemType.WINDOWS.equals(resourceContext.getSystemInformation().getOperatingSystemType())) {
            configFile = new File(binDirectory, "standalone.conf.bat");
            additionalJavaOptsConfig = new AdditionalJavaOpts.WindowsConfiguration();
        } else {
            configFile = new File(binDirectory, "standalone.conf");
            additionalJavaOptsConfig = new AdditionalJavaOpts.LinuxConfiguration();
        }

        try {
            if (additionalJavaOptsContent != null && !additionalJavaOptsContent.trim().isEmpty()) {
                additionalJavaOptsConfig.update(configFile, additionalJavaOptsContent);
            } else {
                additionalJavaOptsConfig.clean(configFile);
            }
        } catch (Exception e) {
            LOG.error("Unable to update configuration file with additional JAVA_OPTS set via RHQ.", e);
        }
    }

    @Override
    public SnapshotReportResults getSnapshotReport(SnapshotReportRequest request) throws Exception {
        if (AvailabilityType.UP.equals(getAvailability())) {
            if ("jdr".equals(request.getName())) {
                InputStream is = new JdrReportRunner(getServerAddress(), getASConnection()).getReport();
                return new SnapshotReportResults(is);
            }
            return null;
        }
        throw new Exception("Cannot obtain report, resource is not UP");
    }

}
