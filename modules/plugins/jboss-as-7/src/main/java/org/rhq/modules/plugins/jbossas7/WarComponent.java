/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Monitoring of war files
 *
 * @author Heiko W. Rupp
 */
public class WarComponent implements ResourceComponent<BaseComponent>, DeleteResourceFacet {

    ResourceContext<BaseComponent> context;

    public void start(ResourceContext<BaseComponent> context) throws InvalidPluginConfigurationException, Exception {
        this.context = context;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public void deleteResource() throws Exception {
//
//        DomainDeploymentManager deploymentManager = client.getDeploymentManager();
//        DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
//
//        String name = context.getResourceKey(); // key and name are the same at the moment
//
//        UndeployDeploymentPlanBuilder udpb = builder.undeploy(name);
//        RemoveDeploymentPlanBuilder rdpb = udpb.andRemoveUndeployed();
//        rdpb.build();
    }
}
