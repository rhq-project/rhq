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
import org.rhq.core.clientapi.server.discovery.InvalidInventoryReportException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * An abstract base class for Resource upgrade handling tests.
 *
 * @author Lukas Krejci
 */
public abstract class AbstractResourceUpgradeHandlingTest extends ResourceUpgradeTestBase {

    private static final String UPGRADED_RESOURCE_KEY_PREFIX = "UPGRADED";

    @SuppressWarnings("unchecked")
    protected void defineDefaultExpectations(FakeServerInventory inventory, Expectations expectations) {
        super.defineDefaultExpectations(inventory, expectations);
        try {
            ServerServices ss = pluginContainerConfiguration.getServerServices();
            expectations.allowing(ss.getDiscoveryServerService()).mergeInventoryReport(
                expectations.with(Expectations.any(InventoryReport.class)));
            expectations.will(inventory.mergeInventoryReport(InventoryStatus.COMMITTED));

            expectations.allowing(ss.getDiscoveryServerService()).upgradeResources(
                expectations.with(Expectations.any(Set.class)));
            expectations.will(inventory.upgradeResources());
        } catch (InvalidInventoryReportException e) {
            //this is not going to happen because we're mocking the invocation
        }
    }

    protected Resource findResourceWithOrdinal(ResType resType, int ordinal) {
        ResourceType resourceType = PluginContainer.getInstance().getPluginManager().getMetadataManager()
            .getType(resType.getResourceTypeName(), resType.getResourceTypePluginName());

        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        Set<Resource> resources = inventoryManager.getResourcesWithType(resourceType);

        return findResourceWithOrdinal(resources, ordinal);
    }

    protected Resource findResourceWithOrdinal(Set<Resource> resources, int ordinal) {
        for (Resource r : resources) {
            Configuration pluginConfig = r.getPluginConfiguration();
            String ordinalString = pluginConfig.getSimpleValue("ordinal", null);

            if (ordinalString != null && Integer.parseInt(ordinalString) == ordinal) {
                return r;
            }
        }

        return null;
    }

    protected static void checkResourcesUpgraded(Set<Resource> resources, int expectedSize) {
        assertEquals(resources.size(), expectedSize, "The set of resources has unexpected size.");
        for (Resource res : resources) {
            assertTrue(res.getResourceKey().startsWith(UPGRADED_RESOURCE_KEY_PREFIX), "Resource " + res
                + " doesn't seem to be upgraded even though it should.");

            ResourceContainer rc = PluginContainer.getInstance().getInventoryManager().getResourceContainer(res);

            assertEquals(rc.getResourceComponentState(), ResourceContainer.ResourceComponentState.STARTED,
                "A resource that successfully upgraded should be started.");
        }
    }

    protected static void checkResourcesNotUpgraded(Set<Resource> resources, int expectedSize) {
        assertEquals(resources.size(), expectedSize, "The set of resources has unexpected size.");
        for(Resource res : resources) {
            assertFalse(res.getResourceKey().startsWith(UPGRADED_RESOURCE_KEY_PREFIX), "Resource " + res
                + " seems to be upgraded even though it shouldn't.");

            ResourceContainer rc = PluginContainer.getInstance().getInventoryManager().getResourceContainer(res);

            assertEquals(rc.getResourceComponentState(), ResourceContainer.ResourceComponentState.STOPPED,
                "A resource that has not been upgraded due to upgrade error in parent should be stopped.");

            //recurse, since the whole subtree under the failed resource should be not upgraded and stopped.
            checkResourcesNotUpgraded(res.getChildResources(), res.getChildResources().size());
        }
    }

    protected static void checkResourceFailedUpgrade(Resource resource) {
        assertFalse(resource.getResourceKey().startsWith(UPGRADED_RESOURCE_KEY_PREFIX), "Resource " + resource
            + " seems to be upgraded even though it shouldn't.");
        assertTrue(resource.getResourceErrors(ResourceErrorType.UPGRADE).size() == 1,
            "The failed resource should have an error associated with it.");

        ResourceContainer rc = PluginContainer.getInstance().getInventoryManager().getResourceContainer(resource);

        assertEquals(rc.getResourceComponentState(), ResourceContainer.ResourceComponentState.STOPPED,
            "A resource that failed to upgrade should be stopped.");
    }

}
