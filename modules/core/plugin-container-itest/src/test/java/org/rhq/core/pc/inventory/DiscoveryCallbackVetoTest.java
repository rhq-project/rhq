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
import org.rhq.core.pc.inventory.discoverycallback.DiscoveryCallbackAbortDiscoveryComponent;
import org.rhq.core.pc.inventory.discoverycallback.DiscoveryCallbackVetoCallbackOK;
import org.rhq.core.pc.inventory.discoverycallback.DiscoveryCallbackVetoCallbackVETO;
import org.rhq.core.pc.inventory.testplugin.TestResourceComponent;
import org.rhq.test.arquillian.AfterDiscovery;
import org.rhq.test.arquillian.BeforeDiscovery;
import org.rhq.test.arquillian.FakeServerInventory;
import org.rhq.test.arquillian.MockingServerServices;
import org.rhq.test.arquillian.RunDiscovery;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * A unit test for testing discovery callbacks and their veto feature.
 */
public class DiscoveryCallbackVetoTest extends Arquillian {

    @Deployment(name = "test")
    @TargetsContainer("pc")
    public static RhqAgentPluginArchive getTestPluginWithVetoCallback() {
        RhqAgentPluginArchive pluginJar1 = ShrinkWrap.create(RhqAgentPluginArchive.class, "test-discovery-callback-veto-plugin.jar");
        pluginJar1.setPluginDescriptor("discovery-callback-veto.xml").addClasses(TestResourceComponent.class,
            DiscoveryCallbackAbortDiscoveryComponent.class, DiscoveryCallbackVetoCallbackOK.class,
            DiscoveryCallbackVetoCallbackVETO.class);
        return pluginJar1;
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
    }

    @AfterDiscovery
    public void waitForAsyncDiscoveries() throws Exception {
        if (discoveryCompleteChecker != null) {
            discoveryCompleteChecker.waitForDiscoveryComplete(10000);
        }
    }

    @RunDiscovery
    @Test(groups = "pc.itest.discoverycallbacks.veto", priority = 5) // these attribs are important; otherwise, this test will fail
    public void testDiscoveryCallbacks() throws Exception {
        // make sure our inventory is as we expect it to be
        validatePluginContainerInventory();
    }
    
    private void validatePluginContainerInventory() throws Exception {
        System.out.println("Validating PC inventory...");

        Resource platform = pluginContainer.getInventoryManager().getPlatform();
        Assert.assertNotNull(platform);
        Assert.assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        // one of the resources the discovery component found should have been aborted because one callback vetoed it.
        Set<Resource> servers = platform.getChildResources();
        Assert.assertNotNull(servers);
        Assert.assertEquals(servers.size(), 1, servers.toString());

        Resource server = servers.iterator().next();
        Assert.assertEquals(server.getResourceKey(), "key-ok");
        Assert.assertEquals(server.getName(), "CallbackOK", "The callback should have altered the resource name");

        System.out.println("PC inventory validated successfully!");
    }

}
