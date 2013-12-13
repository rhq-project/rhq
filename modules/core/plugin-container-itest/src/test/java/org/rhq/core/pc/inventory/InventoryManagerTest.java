/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.inventory.testplugin.TestResourceComponent;
import org.rhq.core.pc.inventory.testplugin.TestResourceDiscoveryComponent;
import org.rhq.core.util.file.FileUtil;
import org.rhq.test.arquillian.AfterDiscovery;
import org.rhq.test.arquillian.BeforeDiscovery;
import org.rhq.test.arquillian.FakeServerInventory;
import org.rhq.test.arquillian.MockingServerServices;
import org.rhq.test.arquillian.RunDiscovery;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

/**
 * An integration test for the {@link InventoryManager}.
 *
 * @author Ian Springer
 */
@RunDiscovery
public class InventoryManagerTest extends Arquillian {

    @Deployment(name = "test")
    @TargetsContainer("pc")
    public static RhqAgentPluginArchive getTestPlugin() {
        RhqAgentPluginArchive pluginJar = ShrinkWrap.create(RhqAgentPluginArchive.class, "test-plugin.jar");
        return pluginJar
                .setPluginDescriptor("test-rhq-plugin.xml")
                .addClasses(TestResourceDiscoveryComponent.class, TestResourceComponent.class);
    }

    @ArquillianResource
    private MockingServerServices serverServices;

    @ArquillianResource
    private PluginContainerConfiguration pluginContainerConfiguration;

    @ArquillianResource
    private PluginContainer pluginContainer;

    private FakeServerInventory fakeServerInventory;

    private FakeServerInventory.CompleteDiscoveryChecker discoveryCompleteChecker;

    @BeforeDiscovery
    public void resetServerServices() throws Exception {
        // Set up our fake server discovery ServerService, which will auto-import all Resources in reports it receives.
        serverServices.resetMocks();
        fakeServerInventory = new FakeServerInventory();
        discoveryCompleteChecker = fakeServerInventory.createAsyncDiscoveryCompletionChecker(2);
        when(serverServices.getDiscoveryServerService().mergeInventoryReport(any(InventoryReport.class))).then(
            fakeServerInventory.mergeInventoryReport(InventoryStatus.COMMITTED));
        when(serverServices.getDiscoveryServerService().getResourceSyncInfo(any(Integer.class))).then(
            fakeServerInventory.getResourceSyncInfo());
    }

    @AfterDiscovery
    public void waitForAsyncDiscoveries() throws Exception {
        if (discoveryCompleteChecker != null) {
            discoveryCompleteChecker.waitForDiscoveryComplete(10000);
        }
    }

    /**
     * Tests that Resources are properly synchronized after the inventory manager is restarted with a clean data
     * directory.
     *
     * @throws Exception if an error occurs
     */
    @Test(groups = "pc.itest.inventorymanager", priority = 1)
    public void testSyncUnknownResources() throws Exception {
        validatePluginContainerInventory();

        // Blow away the data dir, then restart the inventory manager with a fresh slate.
        System.out.println("Purging data directory...");
        File dataDir = pluginContainerConfiguration.getDataDirectory();
        FileUtil.purge(dataDir, true);
        System.out.println("Restarting PC...");
        pluginContainer.getInventoryManager().shutdown();
        // Note, initialize() will perform a Server->Agent sync.
        pluginContainer.getInventoryManager().initialize();

        // Inventory should now be back as it was before the clean restart.
        validatePluginContainerInventory();

        // Now execute a full discovery.
        System.out.println("Executing full discovery...");
        pluginContainer.getInventoryManager().executeServerScanImmediately();
        pluginContainer.getInventoryManager().executeServiceScanImmediately();

        // Check that inventory is still the same.
        validatePluginContainerInventory();
    }

    /**
     * Tests that uninventorying Resources works.
     *
     * @throws Exception if an error occurs
     */
    @Test(groups = "pc.itest.inventorymanager", priority = 1)
    public void testUninventoryResources() throws Exception {
        validatePluginContainerInventory();

        Resource platform = pluginContainer.getInventoryManager().getPlatform();
        Resource server = platform.getChildResources().iterator().next();

        // Uninventory the server.
        fakeServerInventory.removeResource(server);
        pluginContainer.getInventoryManager().uninventoryResource(server.getId());

        platform = pluginContainer.getInventoryManager().getPlatform();
        Assert.assertNotNull(platform);
        Assert.assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        Assert.assertTrue(platform.getChildResources().isEmpty(), "Platform still has children: "
                + platform.getChildResources());

        // Now execute a full discovery.
        System.out.println("Executing full discovery...");
        pluginContainer.getInventoryManager().executeServerScanImmediately();
        pluginContainer.getInventoryManager().executeServiceScanImmediately();

        // Check that inventory is back to what it was before we uninventoried the server.
        validatePluginContainerInventory();
    }

    private void validatePluginContainerInventory() {
        System.out.println("Validating PC inventory...");

        Resource platform = pluginContainer.getInventoryManager().getPlatform();
        Assert.assertNotNull(platform);
        Assert.assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        Assert.assertEquals(platform.getChildResources().size(), 1,
                "platform child Resources: " + platform.getChildResources());
        Resource server = platform.getChildResources().iterator().next();
        Assert.assertNotNull(server);
        Assert.assertEquals(server.getInventoryStatus(), InventoryStatus.COMMITTED);

        Assert.assertEquals(server.getChildResources().size(), 1,
                "server child Resources: " + server.getChildResources());
        Resource service = server.getChildResources().iterator().next();
        Assert.assertNotNull(service);
        Assert.assertEquals(service.getInventoryStatus(), InventoryStatus.COMMITTED);
    }

}
