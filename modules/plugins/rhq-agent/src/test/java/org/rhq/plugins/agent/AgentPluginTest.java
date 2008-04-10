/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.agent;

import java.io.File;
import java.io.PrintWriter;
import java.util.Set;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginEnvironment;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pc.util.InventoryPrinter;
import org.rhq.enterprise.agent.AgentMain;

/**
 * Tests the agent plugin.
 *
 * @author John Mazzitelli
 */
@Test(groups = "agent-plugin")
public class AgentPluginTest {
    private static final String PLUGIN_NAME = "RHQAgent";
    private static final String AGENT_RESOURCE_TYPE_NAME = "RHQ Agent";

    private PluginContainer pc = PluginContainer.getInstance();
    private AgentMain agent;

    /**
     * Starts the plugin container before all tests.
     *
     * @throws Exception
     */
    @BeforeMethod
    public void start() throws Exception {
        agent = new AgentMain("-p test -l -c test-agent-configuration.xml".split(" "));

        // Before we start the agent, let's start the plugin container ourselves
        // (our test agent configuration has told the agent not to start the PC).
        // We need to do this so we can create our own plugin finder.
        // Starting the PC here, rather than have the agent do it, disables the ability from a remote
        // JON Server from accessing the PC; but for these tests, that feature is not needed so its
        // OK that we lost remote access to the PC.
        PluginContainerConfiguration pc_config = agent.getConfiguration().getPluginContainerConfiguration();
        pc_config.setPluginFinder(new FileSystemPluginFinder(new File("target/itest/plugins")));
        pc_config.setInsideAgent(false);

        PluginContainer.getInstance().setConfiguration(pc_config);
        PluginContainer.getInstance().initialize();

        // now that we started the PC, we can start the agent
        // this works because the agent will not attempt to start an already started PC
        agent.start();
    }

    /**
     * Stops the plugin container after all tests.
     */
    @AfterMethod
    public void stop() {
        agent.shutdown();
    }

    /**
     * Tests that the plugin can actually load.
     */
    public void testPluginLoad() {
        PluginManager pluginManager = pc.getPluginManager();
        PluginEnvironment pluginEnvironment = pluginManager.getPlugin(PLUGIN_NAME);
        assert (pluginEnvironment != null) : "Null environment, plugin not loaded";
        assert (pluginEnvironment.getPluginName().equals(PLUGIN_NAME));
    }

    /**
     * Tests that the JON Agent can be discovered.
     *
     * @throws Exception
     */
    public void testServerDiscovery() throws Exception {
        InventoryReport report = pc.getInventoryManager().executeServerScanImmediately();
        assert report != null;

        Resource platform = pc.getInventoryManager().getPlatform();
        Set<Resource> servers = platform.getChildResources();
        assert servers.size() > 0;

        Resource agent_resource = servers.iterator().next();
        assert agent_resource.getName().indexOf("RHQ Agent") > -1 : "Bad name: " + agent_resource.getName();
        assert agent_resource.getResourceType().getName().equals(AGENT_RESOURCE_TYPE_NAME) : "Bad type: "
            + agent_resource.getResourceType();
        assert agent_resource.getResourceType().getCategory() == ResourceCategory.SERVER : "Bad type: "
            + agent_resource.getResourceType();

        pc.getInventoryManager().executeServiceScanImmediately();

        InventoryPrinter.outputInventory(new PrintWriter(System.out), false);
    }
}