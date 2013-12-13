/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.rhq.core.pc.inventory;

import java.util.HashSet;

import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * Tests committing a full inventory then having resource types ignored server-side and
 * watching that the agent is told about that and that the ignored resources are removed
 * from agent inventory.
 *
 * @author John Mazzitelli
 */
@Test(groups = { "ignoreTypesTests" }, singleThreaded = true)
public class CommitThenIgnoreTypesInventoryManagerTest extends AbstractIgnoreTypesInventoryManagerBaseTest {

    protected void initializeIgnoredTypes() {
        this.ignoredTypes = new HashSet<ResourceType>();
        return;
    }

    @RunDiscovery
    public void testIgnoreTypesAfterFullCommit() throws Exception {
        // make sure the agent inventory has a full inventory
        waitForInventory(5);
        validateFullInventory();

        // simulate the ignoring of types
        resetSimulatedServerSideInventory();
        ignoredTypes.add(new ResourceType("Test Service GrandChild", "test", null, null));
        ignoredTypes.add(new ResourceType("Manual Add Server", "test", null, null));

        // the agent side will have the committed resources removed when they are ignored by the server
        InventoryManager inventoryManager = this.pluginContainer.getInventoryManager();
        inventoryManager.getPlatform().getChildResources().clear(); // just clear everything, sync'ing should give us back unignored ones

        // Now execute a full discovery again, this time, we should see the ignored resources go away
        System.out.println("Executing full discovery...");
        InventoryReport report = inventoryManager.executeServerScanImmediately();
        inventoryManager.handleReport(report);
        waitForInventory(3);
        validatePartiallyIgnoredInventory();
    }
}
