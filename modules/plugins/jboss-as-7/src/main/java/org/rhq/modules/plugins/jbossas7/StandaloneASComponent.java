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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
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
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemoveIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.bundle.BundleHandoverRequest;
import org.rhq.core.pluginapi.bundle.BundleHandoverResponse;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
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
import org.rhq.modules.plugins.jbossas7.util.PatchDetails;

/**
 * Component class for standalone AS7 servers.
 *
 * @author Heiko W. Rupp
 */
public class StandaloneASComponent<T extends ResourceComponent<?>> extends BaseServerComponent<T> implements
    MeasurementFacet, OperationFacet, SupportFacet, ContentFacet {

    private static final Log LOG = LogFactory.getLog(StandaloneASComponent.class);

    private static final String SERVER_CONFIG_TRAIT = "config-file";
    private static final String MULTICAST_ADDRESS_TRAIT = "multicastAddress";
    private static final String DEPLOY_DIR_TRAIT = "deployDir";
    private static final String TEMP_DIR_TRAIT = "temp-dir";

    private static final String JAVA_OPTS_ADDITIONAL_PROP = "javaOptsAdditional";

    private static final Address ENVIRONMENT_ADDRESS = new Address("core-service=server-environment");

    private static final String PATCH_PACKAGE_TYPE_NAME = "wflyPatch";

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
        } else if (name.equals("rollbackPatch")) {
            String errorMessage = rollbackPatch(parameters.getSimpleValue("patch-id"), parameters);

            if (errorMessage != null) {
                throw new Exception(errorMessage);
            } else {
                PropertySimple restart = parameters.getSimple("restartImmediately");

                //noinspection ConstantConditions
                if (restart.getBooleanValue()) {
                    return restartServer(parameters);
                } else {
                    return new OperationResult("Success");
                }
            }
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

        Operation addDeploymentStep = new Operation("add", "deployment", runtimeName);
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

    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return null;
    }

    @Override
    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages,
        ContentServices contentServices) {

        DeployPackagesResponse response = new DeployPackagesResponse();
        boolean someFailed = false;

        for (ResourcePackageDetails pkg : packages) {
            if (PATCH_PACKAGE_TYPE_NAME.equals(pkg.getKey().getPackageTypeName())) {
                DeployIndividualPackageResponse resp = deployPatch(pkg, contentServices);
                response.addPackageResponse(resp);
                someFailed = someFailed || resp.getResult() != ContentResponseResult.SUCCESS;
            }
        }

        response.setOverallRequestResult(someFailed ? ContentResponseResult.FAILURE : ContentResponseResult.SUCCESS);

        return response;
    }

    @Override
    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        RemovePackagesResponse response = new RemovePackagesResponse();
        boolean someFailed = false;
        for(ResourcePackageDetails pkg : packages) {
            RemoveIndividualPackageResponse r = rollbackPatch(pkg);
            response.addPackageResponse(r);
            someFailed = someFailed || r.getResult() != ContentResponseResult.SUCCESS;
        }

        response
            .setOverallRequestResult(someFailed ? ContentResponseResult.FAILURE : ContentResponseResult.SUCCESS);

        return response;
    }

    @Override
    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        if (!PATCH_PACKAGE_TYPE_NAME.equals(type.getName())) {
            return Collections.emptySet();
        }

        CliExecutor runner = CliExecutor
            .onServer(pluginConfiguration, getMode(), context.getSystemInformation()).disconnected(true);

        ProcessExecutionResults infoResults = runner.executeCliCommand("patch info");
        if (infoResults.getError() != null || infoResults.getExitCode() == null || infoResults.getExitCode() != 0) {
            if (getLog().isDebugEnabled()) {
                if (infoResults.getError() == null) {
                    getLog().debug(
                        "Failed to obtain the patch info while discovering patches on " +
                            context.getResourceDetails() + ". Exit code: " + infoResults.getExitCode() + ", output:\n" +
                            infoResults.getCapturedOutput()
                    );
                } else {
                    getLog().debug("Failed to obtain the patch info while discovering patches on " +
                        context.getResourceDetails(), infoResults.getError());
                }
            } else if (infoResults.getError() != null) {
                getLog().info("Failed to obtain the patch info while discovering patches on " +
                    context.getResourceDetails() + ": " + infoResults.getError().getMessage());
            }
            return Collections.emptySet();
        }

        ProcessExecutionResults historyResults = runner.executeCliCommand("patch history");
        if (historyResults.getError() != null || historyResults.getExitCode() == null || historyResults.getExitCode() != 0) {
            if (getLog().isDebugEnabled()) {
                if (historyResults.getError() == null) {
                    getLog().debug(
                        "Failed to obtain the patch history while discovering patches on " +
                            context.getResourceDetails() + ". Exit code: " + historyResults.getExitCode() + ", output:\n" +
                            historyResults.getCapturedOutput()
                    );
                } else {
                    getLog().debug("Failed to obtain the patch history while discovering patches on " +
                        context.getResourceDetails(), historyResults.getError());
                }
            } else if (historyResults.getError() != null) {
                getLog().info("Failed to obtain the patch history while discovering patches on " +
                    context.getResourceDetails() + ": " + historyResults.getError().getMessage());
            }
            return Collections.emptySet();
        }

        return getInstalledPatches(infoResults.getCapturedOutput(), historyResults.getCapturedOutput());
    }

    private Set<ResourcePackageDetails> getInstalledPatches(String patchInfoResults, String patchHistoryResults) {
        List<PatchDetails> patchDetails = PatchDetails.fromInfo(patchInfoResults, patchHistoryResults);

        HashSet<ResourcePackageDetails> ret = new HashSet<ResourcePackageDetails>();

        for (PatchDetails p : patchDetails) {
            ResourcePackageDetails details = new ResourcePackageDetails(
                new PackageDetailsKey(p.getId(), "NA", PATCH_PACKAGE_TYPE_NAME, "noarch"));

            details.setInstallationTimestamp(p.getAppliedAt().getTime());
            details.setShortDescription("Type: " + p.getType().toString());
            details.setFileName(p.getId());

            ret.add(details);
        }

        return ret;
    }

    @Override
    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        //we cannot retrieve the bits really, so just return a dummy empty input stream
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return -1;
            }
        };
    }

    private DeployIndividualPackageResponse deployPatch(ResourcePackageDetails pkg, ContentServices contentServices) {
        DeployIndividualPackageResponse response = new DeployIndividualPackageResponse(pkg.getKey());

        File patchFile;
        try {
            patchFile = File.createTempFile("rhq-jbossas-7-", ".patch");
        } catch (IOException e) {
            response.setErrorMessage("Could not create a temporary file to download the patch to. " + e.getMessage());
            response.setResult(ContentResponseResult.FAILURE);
            return response;
        }

        OutputStream out;
        try {
            out = new BufferedOutputStream(new FileOutputStream(patchFile));
        } catch (FileNotFoundException e) {
            response.setErrorMessage("Could not open the temporary file to download the patch to. " + e.getMessage());
            response.setResult(ContentResponseResult.FAILURE);
            return response;
        }

        try {
            contentServices.downloadPackageBits(context.getContentContext(), pkg.getKey(), out, true);
        } finally {
            StreamUtil.safeClose(out);
        }

        String errorMessage = deployPatch(patchFile, Collections.<String, String>emptyMap());

        if (errorMessage != null) {
            response.setErrorMessage(errorMessage);
            response.setResult(ContentResponseResult.FAILURE);
            return response;
        }

        response.setResult(ContentResponseResult.SUCCESS);

        return response;
    }

    private BundleHandoverResponse deployPatch(BundleHandoverRequest request) {
        //download the file to a temp location
        File patchFile;
        try {
            patchFile = File.createTempFile("rhq-jboss-as-7-", ".patch");
        } catch (IOException e) {
            return BundleHandoverResponse
                .failure(BundleHandoverResponse.FailureType.EXECUTION, "Failed to create a temp file to copy patch to.");
        }

        try {
            StreamUtil.copy(request.getContent(), new FileOutputStream(patchFile));
        } catch (FileNotFoundException e) {
            return BundleHandoverResponse.failure(EXECUTION, "Failed to copy patch to local storage.");
        }

        Map<String, String> parameters = request.getParams();

        //param validation
        if (parameters != null) {
            for (Map.Entry<String, String> e : parameters.entrySet()) {
                String name = e.getKey();
                String value = e.getValue();

                if (!("override".equals(name) || "override-all".equals(name) || "preserve".equals(name) ||
                    "override-modules".equals(name))) {
                    return BundleHandoverResponse.failure(BundleHandoverResponse.FailureType.INVALID_PARAMETER,
                        "'" + name +
                            "' is not a supported parameter. Only 'override', 'override-all', 'preserve' and 'override-modules' are supported.");
                }
            }
        }

        String errorMessage = deployPatch(patchFile, parameters);

        return errorMessage == null ? BundleHandoverResponse.success() :
            BundleHandoverResponse.failure(EXECUTION, errorMessage);
    }

    /**
     * Deploys a patch, returning an error message, if any.
     *
     * @param patchFile the local file containing the path
     * @return error message or null if patching succeeded
     */
    private String deployPatch(File patchFile, Map<String, String> additionalParams) {
        StringBuilder command = new StringBuilder("patch apply --path=");
        command.append(patchFile.getAbsolutePath());

        if (additionalParams != null) {
            for (Map.Entry<String, String> e : additionalParams.entrySet()) {
                command.append(" --").append(e.getKey());
                if (e.getValue() != null) {
                    command.append("=").append(e.getValue());
                }
            }
        }

        ProcessExecutionResults results = CliExecutor.onServer(context.getPluginConfiguration(), getMode(),
            context.getSystemInformation()).disconnected(true).executeCliCommand(command.toString());

        if (results.getError() != null || results.getExitCode() == null || results.getExitCode() != 0) {
            String message = "Applying the patch failed ";
            if (results.getError() != null) {
                message += "with an exception: " + results.getError().getMessage();
            } else {
                if (results.getExitCode() == null) {
                    message += "with a timeout.";
                } else {
                    message += "with exit code " + results.getExitCode();
                }

                message += " The attempt produced the following output:\n" + results.getCapturedOutput();
            }

            return message;
        }

        return null;
    }

    private RemoveIndividualPackageResponse rollbackPatch(ResourcePackageDetails pkg) {
        String errorMessage = rollbackPatch(pkg.getName(), null);

        RemoveIndividualPackageResponse response = new RemoveIndividualPackageResponse(pkg.getKey());
        if (errorMessage != null) {
            response.setErrorMessage(errorMessage);
            response.setResult(ContentResponseResult.FAILURE);
        } else {
            response.setResult(ContentResponseResult.SUCCESS);
        }

        return response;
    }

    private String rollbackPatch(String patchId, Configuration params) {
        StringBuilder command = new StringBuilder("patch rollback --patch-id=");
        command.append(patchId);

        if (params == null) {
            command.append(" --reset-configuration=false");
        } else {
            appendParameter(command, params, "reset-configuration");
            appendParameter(command, params, "override-all");
            appendParameter(command, params, "override-modules");
            appendParameter(command, params, "override");
            appendParameter(command, params, "preserve");
        }

        ProcessExecutionResults results = createRunner().disconnected(true).executeCliCommand(command.toString());

        if (results.getError() != null || results.getExitCode() == null || results.getExitCode() != 0) {
            //looks like stuff failed...
            if (results.getError() != null) {
                return "Rolling back the patch " + patchId + " failed with error message: " +
                    results.getError().getMessage();
            } else if (results.getExitCode() == null) {
                return "Rolling back the patch " + patchId +
                    " timed out. Captured output of the rollback command: " + results.getCapturedOutput();
            } else {
                return "Rolling back the patch exited with an error code " + results.getExitCode() +
                    ". Captured output of the rollback command: " + results.getCapturedOutput();
            }
        }

        return null;
    }

    private CliExecutor createRunner() {
        return CliExecutor
            .onServer(pluginConfiguration, getMode(), context.getSystemInformation());
    }

    private void appendParameter(StringBuilder command, Configuration configuration, String parameterName) {
        PropertySimple prop = configuration.getSimple(parameterName);
        if (prop != null && prop.getStringValue() != null) {
            command.append(" --").append(parameterName).append("=").append(prop.getStringValue());
        }
    }
}
