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
 * Test the server ignoring resource types and notifying the {@link InventoryManager} about it.
 *
 * @author John Mazzitelli
 */
@Test(groups = { "ignoreTypesTests" }, singleThreaded = true)
public class IgnoreTypesInventoryManagerTest extends AbstractIgnoreTypesInventoryManagerBaseTest {

    protected void initializeIgnoredTypes() {
        this.ignoredTypes = new HashSet<ResourceType>();
        ignoredTypes.add(new ResourceType("Test Service GrandChild", "test", null, null));
        ignoredTypes.add(new ResourceType("Manual Add Server", "test", null, null));
        return;
    }

    @RunDiscovery
    public void testIgnoreTypes() throws Exception {
        // make sure the agent inventory does not have any resources of the ignored types
        validatePartiallyIgnoredInventory();

        // simulate the unignoring of all types (i.e. don't ignore any types anymore)
        ignoredTypes.clear();

        // Now execute a full discovery again, this time, we should see the full inventory come in
        System.out.println("Executing full discovery...");
        InventoryManager inventoryManager = this.pluginContainer.getInventoryManager();
        InventoryReport report = inventoryManager.executeServerScanImmediately();
        inventoryManager.handleReport(report);
        report = inventoryManager.executeServiceScanImmediately();
        inventoryManager.handleReport(report);
        waitForInventory(5);
        validateFullInventory();
    }
}
