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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.availability.AvailabilityContextImpl;
import org.rhq.core.pc.content.ContentContextImpl;
import org.rhq.core.pc.event.EventContextImpl;
import org.rhq.core.pc.inventory.testplugin.ManualAddDiscoveryComponent;
import org.rhq.core.pc.inventory.testplugin.TestResourceComponent;
import org.rhq.core.pc.inventory.testplugin.TestResourceDiscoveryComponent;
import org.rhq.core.pc.operation.OperationContextImpl;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.test.arquillian.AfterDiscovery;
import org.rhq.test.arquillian.BeforeDiscovery;
import org.rhq.test.arquillian.FakeServerInventory;
import org.rhq.test.arquillian.MockingServerServices;
import org.rhq.test.arquillian.RunDiscovery;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

/**
 * A unit test for testing discovery.
 */
public class DiscoveryTest extends Arquillian {

    @Deployment(name = "test")
    @TargetsContainer("pc")
    public static RhqAgentPluginArchive getTestPlugin() {
        RhqAgentPluginArchive pluginJar = ShrinkWrap.create(RhqAgentPluginArchive.class, "test-plugin.jar");
        return pluginJar.setPluginDescriptor("test-great-grandchild-discovery-plugin.xml").addClasses(
            TestResourceDiscoveryComponent.class, TestResourceComponent.class, ManualAddDiscoveryComponent.class);
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
        discoveryCompleteChecker = fakeServerInventory.createAsyncDiscoveryCompletionChecker(5);
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
     * Tests that discovery was only run once per ResourceType.  This tests a deep, 4-level hierarchy.
     *
     * @throws Exception if an error occurs
     */
    @RunDiscovery
    @Test(groups = "pc.itest.discovery", priority = 10)
    public void testDiscoveryRunsOnlyOncePerType() throws Exception {
        waitForAsyncDiscoveries();

        // make sure our inventory is as we expect it to be
        validatePluginContainerInventory();

        // reset our discovery component's internal storage
        TestResourceDiscoveryComponent.getExecutionCountsByResourceType().clear();

        // run our own discovery scan
        this.pluginContainer.getInventoryManager().executeServiceScanImmediately();

        Map<ResourceType, Integer> executionCountsByResourceType = TestResourceDiscoveryComponent
            .getExecutionCountsByResourceType();
        Map<ResourceType, Integer> flaggedExecutionCountsByResourceType = new HashMap<ResourceType, Integer>();
        for (ResourceType resourceType : executionCountsByResourceType.keySet()) {
            Integer count = executionCountsByResourceType.get(resourceType);
            if (count != 1) {
                flaggedExecutionCountsByResourceType.put(resourceType, count);
            }
        }
        Assert.assertTrue(flaggedExecutionCountsByResourceType.isEmpty(),
            "Discovery was not executed once (and only once) for the following types: "
                + flaggedExecutionCountsByResourceType);
    }

    @Test(groups = "pc.itest.discovery", priority = 10)
    public void testResourceSyncedWithServerAfterManualAdd() throws Exception {
        Mockito.when(serverServices.getDiscoveryServerService().addResource(any(Resource.class), anyInt())).then(
            fakeServerInventory.addResource());

        InventoryManager inventoryManager = pluginContainer.getInventoryManager();

        Resource platform = inventoryManager.getPlatform();

        Configuration myPluginConfig = new Configuration();
        myPluginConfig.put(new PropertySimple("test", "value"));

        ResourceType resourceType = pluginContainer.getPluginManager().getMetadataManager()
            .getType("Manual Add Server", "test");

        MergeResourceResponse response = inventoryManager.manuallyAddResource(resourceType, platform.getId(),
            myPluginConfig, -1);

        assertFalse(response.resourceAlreadyExisted(), "The manual add resource shouldn't have existed");
        assertNotEquals(response.getResourceId(), 0, "The manual add resource should have had its resource id set");

        ResourceContainer resourceContainer = inventoryManager.getResourceContainer(response.getResourceId());
        ResourceContext<?> resourceContext = resourceContainer.getResourceContext();

        assertEquals(resourceContext.getPluginConfiguration(), myPluginConfig,
            "The manual add resource doesn't have the expected plugin config.");

        assertTrue(resourceContext.getAvailabilityContext() instanceof AvailabilityContextImpl,
            "Unexpected implementation clas of the AvailabilityContext, please fix this test.");
        assertEquals(((AvailabilityContextImpl) resourceContext.getAvailabilityContext()).getResource().getId(),
            response.getResourceId(),
            "Availability subsystem isn't aware of the correct resource id for manual add resource");

        assertTrue(resourceContext.getContentContext() instanceof ContentContextImpl,
            "Unexpected implementation class of ContentContext, please fix this test");
        assertEquals(((ContentContextImpl) resourceContext.getContentContext()).getResourceId(),
            response.getResourceId(),
            "Content subsystem isn't aware of the correct resource id for manual add resource");

        assertTrue(resourceContext.getEventContext() instanceof EventContextImpl,
            "Unexpected implementation clas of the EventContext, please fix this test.");
        assertEquals(((EventContextImpl) resourceContext.getEventContext()).getResource().getId(),
            response.getResourceId(), "Event subsystem isn't aware of the correct resource id for manual add resource");

        assertTrue(resourceContext.getOperationContext() instanceof OperationContextImpl,
            "Unexpected implementation clas of the OperationContext, please fix this test.");
        assertEquals(((OperationContextImpl) resourceContext.getOperationContext()).getResourceId(),
            response.getResourceId(),
            "Operation subsystem isn't aware of the correct resource id for manual add resource");
    }

    private void validatePluginContainerInventory() throws Exception {
        System.out.println("Validating PC inventory...");

        Resource platform = pluginContainer.getInventoryManager().getPlatform();
        Assert.assertNotNull(platform);
        Assert.assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        Resource server = platform.getChildResources().iterator().next();
        Assert.assertNotNull(server);
        Assert.assertEquals(server.getInventoryStatus(), InventoryStatus.COMMITTED);
        assert server.getResourceType().getName().equals("Test Server");

        Resource child = server.getChildResources().iterator().next();
        Assert.assertNotNull(child);
        Assert.assertEquals(child.getInventoryStatus(), InventoryStatus.COMMITTED);
        assert child.getResourceType().getName().equals("Test Service Child");

        Resource grandchild = child.getChildResources().iterator().next();
        Assert.assertNotNull(grandchild);
        Assert.assertEquals(grandchild.getInventoryStatus(), InventoryStatus.COMMITTED);
        assert grandchild.getResourceType().getName().equals("Test Service GrandChild");

        Resource greatgrandchild = grandchild.getChildResources().iterator().next();
        Assert.assertNotNull(greatgrandchild);
        Assert.assertEquals(greatgrandchild.getInventoryStatus(), InventoryStatus.COMMITTED);
        assert greatgrandchild.getResourceType().getName().equals("Test Service GreatGrandChild");

        System.out.println("PC inventory validated successfully!");
    }

}
