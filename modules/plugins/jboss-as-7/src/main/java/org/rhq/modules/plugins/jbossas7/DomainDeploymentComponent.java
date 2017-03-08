/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.helper.Deployer;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenNames;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenResources;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Remove;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Handle domain deployments
 * @author Heiko W. Rupp
 * @author Libor Zoubek
 */
public class DomainDeploymentComponent extends DeploymentComponent implements OperationFacet {

    @Override
    public AvailabilityType getAvailability() {
        // Domain deployments have no 'enabled' attribute

        Operation op = new ReadResource(getAddress());
        Result res = getASConnection().execute(op, AVAIL_OP_TIMEOUT_SECONDS);
        // this resource cannot be down, either UP = exists, or MISSING

        if (res != null && res.isSuccess()) {
            return AvailabilityType.UP;
        } else if (res != null && !res.isSuccess() && res.isTimedout()) {
            return AvailabilityType.UNKNOWN;
        }

        return AvailabilityType.MISSING;
    }

    private String getManagementNodeName() {
        String managementNodeName = context.getPluginConfiguration().getSimpleValue("path");
        return managementNodeName.substring(managementNodeName.indexOf("=") + 1);
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {

        OperationResult operationResult = new OperationResult();

        if (name.equals("promote")) {
            String serverGroup = parameters.getSimpleValue("server-group", "-not set-");
            List<String> serverGroups = new ArrayList<String>();
            if (serverGroup.equals("__all")) {
                serverGroups.addAll(getServerGroups());
            } else {
                serverGroups.add(serverGroup);
            }
            String managementNodeName = getManagementNodeName();

            getLog().info("Promoting [" + managementNodeName + "] to server group(s) [" + serverGroups + "]...");

            PropertySimple simple = parameters.getSimple("enabled");
            Boolean enabled = false;
            if (simple != null && simple.getBooleanValue() != null)
                enabled = simple.getBooleanValue();

            PropertySimple runtimeNameProperty = parameters.getSimple("runtime-name");
            String runtimeName = null;
            if (runtimeNameProperty != null)
                runtimeName = runtimeNameProperty.getStringValue();

            CompositeOperation operation = new CompositeOperation();
            for (String theGroup : serverGroups) {
                Operation step = createServerGroupAssignmentStep("add", theGroup, runtimeName, enabled);
                operation.addStep(step);
            }

            Result res = getASConnection().execute(operation, 120); // wait up to 2 minutes
            if (res.isSuccess()) {
                operationResult.setSimpleResult("Successfully deployed to server groups " + serverGroups);

                //request the server to discover child resources to allow the discovery of the deployments
                //on server groups immediately
                requestDiscovery();
            } else {
                operationResult.setErrorMessage("Deployment to server groups failed: " + res.getFailureDescription());
            }
        } else if (name.equals("restart")) {
            String serverGroup = parameters.getSimpleValue("server-group", "-not set-");
            List<String> serverGroups = new ArrayList<String>();
            List<String> assignedGroups = findAssignedServerGroups();
            if (serverGroup.equals("__all")) {
                serverGroups.addAll(assignedGroups);
            } else {
                if (!assignedGroups.contains(serverGroup)) {
                    operationResult.setErrorMessage("Deployment could not be restarted in server-group [" + serverGroup
                        + "] because it is not assigned to it.");
                    return operationResult;
                }
                serverGroups.add(serverGroup);
            }
            if (serverGroups.isEmpty()) {
                operationResult
                    .setErrorMessage("Deployment could not be restarted because it is not assigned to any server-group");
                return operationResult;
            }
            CompositeOperation operation = new CompositeOperation();
            for (String theGroup : serverGroups) {
                Operation step = createServerGroupAssignmentStep("redeploy", theGroup, null, false);
                operation.addStep(step);
            }
            Result res = getASConnection().execute(operation, 120); // wait up to 2 minutes
            if (res.isSuccess()) {
                operationResult.setSimpleResult("Successfully restarted in server groups " + serverGroups);
            } else {
                operationResult.setErrorMessage("Deployment restart in server groups failed: "
                    + res.getFailureDescription());
            }
        } else {
            operationResult.setErrorMessage("Unknown operation " + name);
        }
        return operationResult;
    }

    private void requestDiscovery() {
        if (this.context.getParentResourceComponent().getClass().isInstance(HostControllerComponent.class)) {
            HostControllerComponent<?> hostController = (HostControllerComponent<?>) this.context
                .getParentResourceComponent();
            hostController.requestDeferredChildResourcesDiscovery();
        }
    }

    private List<String> findAssignedServerGroups() {
        List<String> groups = new ArrayList<String>();
        Configuration config = new Configuration();
        loadAssignedServerGroups(config);
        for (Property prop : config.getList("*1").getList()) {
            PropertyMap map = (PropertyMap) prop;
            groups.add(map.getSimpleValue("server-group", null));
        }
        return groups;
    }

    @Override
    protected Deployer createDeployerForPackageUpdate(String deploymentName, String runtimeName, String hash) {
        Deployer deployer = new Deployer(deploymentName, runtimeName, hash, getASConnection());
        // we need to find server-groups where this deployment is assigned
        Configuration config = Configuration.builder().build();
        loadAssignedServerGroups(config);
        String originalDeploymentName = getManagementNodeName();
        // then we add steps to remove from them, and at the same time we add steps to assign new deployment to same groups with same parameters
        for (Property prop : config.getList("*1").getList()) {
            PropertyMap map = (PropertyMap) prop;
            String sgName = map.getSimpleValue("server-group", null);
            String sgRuntimeName = map.getSimpleValue("runtime-name", null);
            if (originalDeploymentName.equals(sgRuntimeName)) {
                sgRuntimeName = null; // runtimeName was equal to deployment Name => not defined at deploy time
            }
            boolean sgEnabled = map.getSimple("enabled").getBooleanValue();
            deployer.addBeforeDeployStep(createServerGroupAssignmentStep("remove", sgName, null, false));
            // new assign-to-group step refers to new deploymentName
            deployer.addAfterDeployStep(createServerGroupAssignmentStep("add", sgName, deploymentName, sgRuntimeName,
                sgEnabled));
        }

        // this has to be the last beforeDeploy step as it would fail in case deployment is assigned to some server-group
        deployer.addBeforeDeployStep(new Remove(getAddress()));
        return deployer;
    }

    @SuppressWarnings("unchecked")
    private void loadAssignedServerGroups(Configuration configuration) {
        String managementNodeName = getManagementNodeName();
        Address theAddress = new Address("/");
        Operation op = new ReadChildrenResources(theAddress, "server-group");
        op.addAdditionalProperty("recursive-depth", "1");
        Result res = getASConnection().execute(op);
        PropertyList propGroups = new PropertyList("*1");
        configuration.put(propGroups);
        if (res.isSuccess()) {
            Map<String, Object> groups = (Map<String, Object>) res.getResult();
            for (Map.Entry<String, Object> entry : groups.entrySet()) {
                Map<String, Object> groupDetails = (Map<String, Object>) entry.getValue();
                Map<String, Object> deployments = (Map<String, Object>) groupDetails.get("deployment");
                if (deployments != null) {
                    Map<String, Object> deployment = (Map<String, Object>) deployments.get(managementNodeName);
                    if (deployment != null) {
                        PropertyMap map = new PropertyMap("*");
                        map.put(new PropertySimple("server-group", entry.getKey()));
                        map.put(new PropertySimple("runtime-name", deployment.get("runtime-name")));
                        map.put(new PropertySimple("enabled", deployment.get("enabled")));
                        propGroups.add(map);
                    }
                }
            }
        }
    }

    private Operation createServerGroupAssignmentStep(String action, String serverGroup,String deploymentName, String runtimeName,
        boolean enabled) {
        Address addr = new Address();
        addr.add("server-group", serverGroup);
        addr.add("deployment", deploymentName);
        Operation op = new Operation(action, addr);
        if ("add".equals(action)) {
            if (runtimeName != null && !runtimeName.isEmpty()) {
                op.addAdditionalProperty("runtime-name", runtimeName);
            }
            op.addAdditionalProperty("enabled", enabled);
        }
        return op;
    }
    
    private Operation createServerGroupAssignmentStep(String action, String serverGroup, String runtimeName,
        boolean enabled) {
        return createServerGroupAssignmentStep(action, serverGroup,  getManagementNodeName(),  runtimeName, enabled);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration configuration = new Configuration();
        // load deployment configuration - we cannot use generic method, it would fail
        Operation op = new Operation("read-resource", getAddress());
        Result res = getASConnection().execute(op);
        if (res.isSuccess()) {
            Map<String, Object> result = (Map<String, Object>) res.getResult();
            configuration.put(new PropertySimple("name", result.get("name")));
            configuration.put(new PropertySimple("runtime-name", result.get("runtime-name")));
            configuration.put(new PropertySimple("content", result.get("content")));
        } else {
            throw new IOException("Operation " + op + " failed: " + res.getFailureDescription());
        }
        includeOOBMessages(res, configuration);
        // list all server-groups, iterate them and find the ones we're deployed in
        loadAssignedServerGroups(configuration);
        return configuration;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        Configuration sgConfig = new Configuration();
        loadAssignedServerGroups(sgConfig);
        CompositeOperation operation = new CompositeOperation();
        boolean needDiscovery = false; // we will request deferred child discovery on parent HC only in case new assignment was added

        // load assigned server-groups to map, so we can easily look them up
        Map<String, PropertyMap> assignedCurrent = new HashMap<String, PropertyMap>();
        for (Property p : sgConfig.getList("*1").getList()) {
            PropertyMap map = (PropertyMap) p;
            assignedCurrent.put(map.getSimpleValue("server-group", null), map);
        }
        // also put new assignment to map, to detect possible duplicates
        Map<String, PropertyMap> assignedNew = new HashMap<String, PropertyMap>();
        // detect changes (enable/disable changes and new assignments)
        int processTimeout = 120;
        for (Property prop : report.getConfiguration().getList("*1").getList()) {
            PropertyMap mapNew = (PropertyMap) prop;
            PropertyMap duplicate = assignedNew.put(mapNew.getSimpleValue("server-group", null), mapNew);
            if (duplicate != null) {
                report.setStatus(ConfigurationUpdateStatus.FAILURE);
                report.setErrorMessage("Duplicate assignment to [" + duplicate.getSimpleValue("server-group", null)
                    + "] you cannot assign deployment to server-group more than once");
                return;
            }
            String key = mapNew.getSimpleValue("server-group", null);
            String runtimeNew = mapNew.getSimpleValue("runtime-name", null);
            boolean enabledNew = mapNew.getSimple("enabled").getBooleanValue();
            PropertyMap mapCurrent = assignedCurrent.remove(key);
            if (mapCurrent == null) {
                // new assignment was added
                operation.addStep(createServerGroupAssignmentStep("add", key, runtimeNew, enabledNew));
                needDiscovery = true;
            } else {
                boolean enabledCurrent = mapCurrent.getSimple("enabled").getBooleanValue();
                if (enabledCurrent != enabledNew) {
                    // deployment status change
                    String action = "undeploy";
                    if (enabledNew) {
                        action = "deploy";
                    }
                    operation.addStep(createServerGroupAssignmentStep(action, key, null, false));
                }
            }
            Integer configuredProcessTimeout = mapNew.getSimple("process-timeout").getIntegerValue();
            if(configuredProcessTimeout != null) {
                processTimeout = configuredProcessTimeout.intValue();
            }
        }
        // detect removals, items left in map (exist in old config, but were not sent in the new one) should be removed
        for (PropertyMap map : assignedCurrent.values()) {
            operation.addStep(createServerGroupAssignmentStep("remove", map.getSimpleValue("server-group", null), null,
                false));
        }
        if (operation.numberOfSteps() == 0) {
            report.setStatus(ConfigurationUpdateStatus.NOCHANGE);
            return;
        }
        Result res = getASConnection().execute(operation, processTimeout);
        if (res.isSuccess()) {
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } else {
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            report.setErrorMessage(res.getFailureDescription());
            return;
        }
        if (needDiscovery) {
            requestDiscovery();
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<String> getServerGroups() {
        Operation op = new ReadChildrenNames(new Address(), "server-group");
        Result res = getASConnection().execute(op);

        return (Collection<String>) res.getResult();
    }

}
