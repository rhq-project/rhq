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

package org.rhq.modules.plugins.wildfly10;

import static org.rhq.core.pluginapi.bundle.BundleHandoverResponse.FailureType.EXECUTION;
import static org.rhq.core.pluginapi.bundle.BundleHandoverResponse.FailureType.INVALID_ACTION;
import static org.rhq.core.pluginapi.bundle.BundleHandoverResponse.FailureType.MISSING_PARAMETER;
import static org.rhq.core.util.StringUtil.isBlank;

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
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.bundle.BundleHandoverRequest;
import org.rhq.core.pluginapi.bundle.BundleHandoverResponse;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.CompositeOperation;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.wildfly10.json.Result;

/**
 * Component class for AS7 host and domain controllers.
 *
 * @author Heiko W. Rupp
 */
public class HostControllerComponent<T extends ResourceComponent<?>> extends BaseServerComponent<T> implements
    MeasurementFacet, OperationFacet {

    private static final Log LOG = LogFactory.getLog(HostControllerComponent.class);

    private static final String DOMAIN_CONFIG_TRAIT = "domain-config-file";
    private static final String HOST_CONFIG_TRAIT = "host-config-file";
    private static final String DOMAIN_HOST_TRAIT = "domain-host-name";
    private static final String DOMAIN_NAME_TRAIT = "domain-name";
    private static final String DOMAIN_TEMP_DIR_TRAIT = "domain-temp-dir";
    private static final String PROCESS_TYPE_DC = "Domain Controller";

    private boolean domainController; // determines whether this HC is also DC

    @Override
    protected AS7Mode getMode() {
        return AS7Mode.DOMAIN;
    }

    @Override
    public void start(ResourceContext<T> resourceContext) throws Exception {
        super.start(resourceContext);
        setDomainController(PROCESS_TYPE_DC.equals(getProcessTypeAttrValue()));
    }

    @Override
    protected void onAvailGoesUp() {
        super.onAvailGoesUp();
        setDomainController(PROCESS_TYPE_DC.equals(getProcessTypeAttrValue()));
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        Set<MeasurementScheduleRequest> leftovers = new HashSet<MeasurementScheduleRequest>(requests.size());
        for (MeasurementScheduleRequest request : requests) {
            String requestName = request.getName();
            if (requestName.equals(DOMAIN_CONFIG_TRAIT) || requestName.equals(HOST_CONFIG_TRAIT)) {
                collectConfigTrait(report, request);
            } else if (requestName.equals(DOMAIN_HOST_TRAIT)) {
                MeasurementDataTrait data = new MeasurementDataTrait(request, findASDomainHostName());
                report.addData(data);
            } else if (requestName.equals(DOMAIN_NAME_TRAIT)) {
                MeasurementDataTrait data = new MeasurementDataTrait(request, readAttribute("name"));
                report.addData(data);
            } else {
                leftovers.add(request); // handled below
            }
        }

        super.getValues(report, leftovers);
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {

        OperationResult operationResult;
        if (name.equals("start")) {
            operationResult = startServer();
        } else if (name.equals("restart")) {
            operationResult = restartServer(parameters);
        } else if (name.equals("executeCommands") || name.equals("executeScript")) {
            return runCliCommand(parameters);
        } else if (name.equals("setupCli")) {
            return setupCli(parameters);
        } else if (name.equals("shutdown")) {
            // This is a bit trickier, as it needs to be executed on the level on /host=xx
            String domainHost = getASHostName();
            if (domainHost.isEmpty()) {
                OperationResult result = new OperationResult();
                result.setErrorMessage("No domain host found - can not continue");
                operationResult = result;
            }
            Operation op = new Operation("shutdown", "host", domainHost);
            Result res = getASConnection().execute(op);
            operationResult = postProcessResult(name, res);

            if (waitUntilDown()) {
                operationResult.setSimpleResult("Success");
            } else {
                operationResult.setErrorMessage("Was not able to shut down the server.");
            }
        } else if (name.equals("installRhqUser")) {
            operationResult = installManagementUser(parameters, pluginConfiguration);
        } else {

            // Defer other stuff to the base component for now
            operationResult = super.invokeOperation(name, parameters);
        }

        context.getAvailabilityContext().requestAvailabilityCheck();

        return operationResult;
    }

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {

        // If Content is to be deployed, call the deployContent method
        if (report.getPackageDetails() != null)
            return super.deployContent(report);

        String targetTypeName = report.getResourceType().getName();
        Operation op;

        String resourceName = report.getUserSpecifiedResourceName();
        Configuration rc = report.getResourceConfiguration();
        Address targetAddress;

        // Dispatch according to child type
        if (targetTypeName.equals("ServerGroup")) {
            targetAddress = new Address(); // Server groups are at / level
            targetAddress.add("server-group", resourceName);
            op = new Operation("add", targetAddress);

            String profile = rc.getSimpleValue("profile", "");
            if (profile.isEmpty()) {
                report.setErrorMessage("No profile given");
                report.setStatus(CreateResourceStatus.FAILURE);
                return report;
            }
            op.addAdditionalProperty("profile", profile);
            String socketBindingGroup = rc.getSimpleValue("socket-binding-group", "");
            if (socketBindingGroup.isEmpty()) {
                report.setErrorMessage("No socket-binding-group given");
                report.setStatus(CreateResourceStatus.FAILURE);
                return report;
            }
            op.addAdditionalProperty("socket-binding-group", socketBindingGroup);
            PropertySimple offset = rc.getSimple("socket-binding-port-offset");
            if (offset != null && offset.getStringValue() != null)
                op.addAdditionalProperty("socket-binding-port-offset", offset.getIntegerValue());

            PropertySimple jvm = rc.getSimple("jvm");
            if (jvm != null) {
                op.addAdditionalProperty("jvm", jvm.getStringValue());
            }
        } else if (targetTypeName.equals(MANAGED_SERVER)) {

            String targetHost = rc.getSimpleValue("hostname", null);
            if (targetHost == null) {
                report.setErrorMessage("No domain host given");
                report.setStatus(CreateResourceStatus.FAILURE);
                return report;
            }

            targetAddress = new Address("host", targetHost);
            targetAddress.add("server-config", resourceName);
            op = new Operation("add", targetAddress);
            String socketBindingGroup = rc.getSimpleValue("socket-binding-group", "");
            if (socketBindingGroup.isEmpty()) {
                report.setErrorMessage("No socket-binding-group given");
                report.setStatus(CreateResourceStatus.FAILURE);
                return report;
            }
            op.addAdditionalProperty("socket-binding-group", socketBindingGroup);
            String autostartS = rc.getSimpleValue("auto-start", "false");
            boolean autoStart = Boolean.valueOf(autostartS);
            op.addAdditionalProperty("auto-start", autoStart);

            String portS = rc.getSimpleValue("socket-binding-port-offset", "0");
            int portOffset = Integer.parseInt(portS);
            op.addAdditionalProperty("socket-binding-port-offset", portOffset);

            String serverGroup = rc.getSimpleValue("group", null);
            if (serverGroup == null) {
                report.setErrorMessage("No server group given");
                report.setStatus(CreateResourceStatus.FAILURE);
                return report;
            }
            op.addAdditionalProperty("group", serverGroup);

        } else if (targetTypeName.equals("JVM-Definition")) {
            return super.createResource(report);

        } else {
            throw new IllegalArgumentException("Don't know yet how to create instances of " + targetTypeName);
        }
        Result res = getASConnection().execute(op);
        if (res.isSuccess()) {
            if (targetTypeName.equals(MANAGED_SERVER)) {
                report.setResourceKey(ManagedASDiscovery.createResourceKey(rc.getSimpleValue("hostname"),
                    report.getUserSpecifiedResourceName()));
            } else {
                report.setResourceKey(targetAddress.getPath());
            }
            report.setResourceName(resourceName);
            report.setStatus(CreateResourceStatus.SUCCESS);

            if (targetTypeName.equals("ServerGroup")) {
                PropertyList sysProperties = rc.getList("*2");
                if (sysProperties != null && !sysProperties.getList().isEmpty()) {
                    // because AS7 does not allow us to pass system properties while creating server-group we must do it now
                    ConfigurationUpdateReport rep = new ConfigurationUpdateReport(rc);
                    ConfigurationDefinition configDef = report.getResourceType().getResourceConfigurationDefinition();
                    ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDef, getASConnection(),
                        targetAddress);
                    delegate.updateResourceConfiguration(rep);
                    if (ConfigurationUpdateStatus.FAILURE.equals(rep.getStatus())) {
                        report.setStatus(CreateResourceStatus.FAILURE);
                        report.setErrorMessage("Failed to additionally configure server group: "
                            + rep.getErrorMessage());
                    }
                }
            }
        } else {
            report.setErrorMessage(res.getFailureDescription());
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setException(res.getRhqThrowable());
        }
        return report;
    }

    /**
     * Handles content handed over during a bundle deployment.<br>
     * <br>
     * This component supports the following actions:<br>
     * <br>
     * <strong>action = deployment: deploys the content to a server group</strong><br>
     * <br>
     * Required parameters:<br>
     * <ul>
     *     <li>serverGroup: The name of the server group this deployment should be deployed to</li>
     * </ul>
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
            return BundleHandoverResponse.failure(INVALID_ACTION);
        } catch (Exception e) {
            return BundleHandoverResponse.failure(EXECUTION, "Unexpected handover failure", e);
        }
    }

    private BundleHandoverResponse handleDeployment(BundleHandoverRequest handoverRequest) {
        String serverGroup = handoverRequest.getParams().get("serverGroup");
        if (isBlank(serverGroup)) {
            return BundleHandoverResponse.failure(MISSING_PARAMETER, "serverGroup parameter is missing");
        }

        // make sure our server is UP. We need to check it, because this handover
        // could happen right after "execute-script" handover, which could have reloaded the server
        // @see https://bugzilla.redhat.com/show_bug.cgi?id=1252930
        Integer timeout = BUNDLE_HANDOVER_SERVER_CHECK_TIMEOUT;
        String waitForServer = handoverRequest.getParams().get(BUNDLE_HANDOVER_SERVER_CHECK_TIMEOUT_PARAM);
        if(waitForServer != null && waitForServer.length() > 0) {
            try {
                timeout = Integer.valueOf(waitForServer);
            } catch(NumberFormatException e) {
                return BundleHandoverResponse.failure(EXECUTION,
                        "Given server timeout parameter is not a valid number: " + waitForServer);
            }
        }
        if (!ensureServerUp(timeout)) { // Value 0 disables the check
            return BundleHandoverResponse.failure(EXECUTION,
                    "Failed to upload deployment content, " + this.context.getResourceDetails()
                            + " is currently not responding or " + AvailabilityType.DOWN);
        }

        HandoverContentUploader contentUploader = new HandoverContentUploader(handoverRequest, getASConnection());
        boolean uploaded = contentUploader.upload();
        if (!uploaded) {
            return contentUploader.getFailureResponse();
        }

        String filename = contentUploader.getFilename();
        String runtimeName = contentUploader.getRuntimeName();
        String hash = contentUploader.getHash();

        Redeployer redeployer = new Redeployer(filename, runtimeName, hash, getASConnection());
        if (redeployer.deploymentExists()) {
            Result result = redeployer.redeployOnServer();
            if (result.isRolledBack()) {
                return BundleHandoverResponse.failure(EXECUTION, result.getFailureDescription());
            }
            return BundleHandoverResponse.success();
        }

        // TODO use Deployer
        Operation addDeploymentStep = new Operation("add", "deployment", filename);
        List<Object> addDeploymentContentProperty = new ArrayList<Object>(1);
        Map<String, Object> contentValues = new HashMap<String, Object>();
        contentValues.put("hash", new PROPERTY_VALUE("BYTES_VALUE", hash));
        addDeploymentContentProperty.add(contentValues);
        addDeploymentStep.addAdditionalProperty("content", addDeploymentContentProperty);
        addDeploymentStep.addAdditionalProperty("name", filename);
        addDeploymentStep.addAdditionalProperty("runtime-name", runtimeName);

        Address serverGroupDeploymentAddress = new Address();
        serverGroupDeploymentAddress.add("server-group", serverGroup);
        serverGroupDeploymentAddress.add("deployment", filename);

        Operation addToServerGroupStep = new Operation("add", serverGroupDeploymentAddress);
        addToServerGroupStep.addAdditionalProperty("runtime-name", runtimeName);
        addToServerGroupStep.addAdditionalProperty("enabled", true);

        CompositeOperation compositeOperation = new CompositeOperation();
        compositeOperation.addStep(addDeploymentStep);
        compositeOperation.addStep(addToServerGroupStep);

        Result result = getASConnection().execute(compositeOperation, 300);
        if (!result.isSuccess()) {
            return BundleHandoverResponse.failure(EXECUTION, result.getFailureDescription());
        } else {
            return BundleHandoverResponse.success();
        }
    }

    private String getProcessTypeAttrValue() {
        try {
            return readAttribute(new Address("/"), "process-type");
        } catch (Exception e) {
            LOG.warn("Unable to detect HostController's process-type", e);
            return null;
        }
    }

    public synchronized boolean isDomainController() {
        return domainController;
    }

    public synchronized void setDomainController(boolean domainController) {
        this.domainController = domainController;
    }

    @NotNull
    @Override
    protected Address getEnvironmentAddress() {
        return new Address("host=" + getASHostName() + ",core-service=host-environment");
    }

    @NotNull
    @Override
    protected Address getHostAddress() {
        return new Address("host=" + getASHostName());
    }

    @NotNull
    @Override
    protected String getBaseDirAttributeName() {
        return "domain-base-dir";
    }

    @NotNull
    @Override
    protected String getConfigDirAttributeName() {
        return "domain-config-dir";
    }

    @Override
    protected String getTempDirAttributeName() {
        return DOMAIN_TEMP_DIR_TRAIT;
    }

}
