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

package org.rhq.core.pc.upgrade;


import java.util.Set;

import org.jmock.Expectations;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.ServerServices;
import org.rhq.test.pc.PluginContainerTest;

/**
 *
 * @author Lukas Krejci
 */
public abstract class ResourceUpgradeTestBase extends PluginContainerTest {

    @SuppressWarnings("unchecked")
    protected void defineDefaultExpectations(FakeServerInventory inventory, Expectations expectations) {
        ServerServices ss = pluginContainerConfiguration.getServerServices();
        
        expectations.ignoring(ss.getBundleServerService());
        expectations.ignoring(ss.getConfigurationServerService());
        expectations.ignoring(ss.getContentServerService());
        expectations.ignoring(ss.getCoreServerService());
        expectations.ignoring(ss.getEventServerService());
        expectations.ignoring(ss.getMeasurementServerService());
        expectations.ignoring(ss.getOperationServerService());
        expectations.ignoring(ss.getResourceFactoryServerService());

        //just ignore these invocations if we get a availability scan in the PC...
        expectations.allowing(ss.getDiscoveryServerService()).mergeAvailabilityReport(
            expectations.with(Expectations.any(AvailabilityReport.class)));

        expectations.allowing(ss.getDiscoveryServerService()).getResources(
            expectations.with(Expectations.any(Set.class)), expectations.with(Expectations.any(boolean.class)));
        expectations.will(inventory.getResources());

        expectations.allowing(ss.getDiscoveryServerService()).postProcessNewlyCommittedResources(
            expectations.with(Expectations.any(Set.class)));
    }

    protected Set<Resource> getTestingResources(FakeServerInventory serverInventory, ResType type) {
        ResourceType resType = PluginContainer.getInstance().getPluginManager().getMetadataManager()
            .getType(type.getResourceTypeName(), type.getResourceTypePluginName());
    
        return serverInventory.findResourcesByType(resType);
    }
}
