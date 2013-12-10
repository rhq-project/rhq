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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenNames;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Handle domain deployments
 * @author Heiko W. Rupp
 */
public class DomainDeploymentComponent extends DeploymentComponent implements OperationFacet {

    @Override
    public AvailabilityType getAvailability() {
        // Domain deployments have no 'enabled' attribute

        Operation op = new ReadResource(getAddress());
        Result res = getASConnection().execute(op);

        return (res != null && res.isSuccess()) ? AvailabilityType.UP : AvailabilityType.DOWN;
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
            String resourceKey = context.getResourceKey();
            resourceKey = resourceKey.substring(resourceKey.indexOf("=") + 1);

            getLog().info("Promoting [" + resourceKey + "] to server group(s) [" + serverGroups + "]...");

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
                Address theAddress = new Address();
                theAddress.add("server-group", theGroup);

                theAddress.add("deployment", resourceKey);
                Operation step = new Operation("add", theAddress);
                step.addAdditionalProperty("enabled", enabled);
                if (runtimeName != null && !runtimeName.isEmpty())
                    step.addAdditionalProperty("runtime-name", runtimeName);
                operation.addStep(step);
            }

            Result res = getASConnection().execute(operation, 120); // wait up to 2 minutes
            if (res.isSuccess()) {
                operationResult.setSimpleResult("Successfully deployed to server groups " + serverGroups);

                //request the server to discover child resources to allow the discovery of the deployments
                //on server groups immediately
                if (this.context.getParentResourceComponent().getClass().isInstance(HostControllerComponent.class)) {
                    HostControllerComponent<?> hostController = (HostControllerComponent<?>) this.context
                        .getParentResourceComponent();
                    hostController.requestDeferredChildResourcesDiscovery();
                }
            } else {
                operationResult.setErrorMessage("Deployment to server groups failed: " + res.getFailureDescription());
            }
        } else {
            operationResult.setErrorMessage("Unknown operation " + name);
        }

        return operationResult;
    }

    @SuppressWarnings("unchecked")
    private Collection<String> getServerGroups() {
        Operation op = new ReadChildrenNames(new Address(), "server-group");
        Result res = getASConnection().execute(op);

        return (Collection<String>) res.getResult();
    }

}
