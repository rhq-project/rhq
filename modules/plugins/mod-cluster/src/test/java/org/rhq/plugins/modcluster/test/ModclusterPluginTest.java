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
package org.rhq.plugins.modcluster.test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginEnvironment;
import org.rhq.core.pc.plugin.PluginManager;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * @author Fady Matar
 */
@Test(groups = "tomcat-plugin")
public class ModclusterPluginTest {
    private Log log = LogFactory.getLog(this.getClass());
    private static final String PLUGIN_NAME = "Tomcat";

    @BeforeSuite
    public void start() {
        try {
            File pluginDir = new File("target/itest/plugins");
            PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
            pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
            pcConfig.setPluginDirectory(pluginDir);

            pcConfig.setInsideAgent(false);
            PluginContainer.getInstance().setConfiguration(pcConfig);
            PluginContainer.getInstance().initialize();
            log.info("PC started.");
            for (String plugin : PluginContainer.getInstance().getPluginManager().getMetadataManager().getPluginNames()) {
                log.info("...Loaded plugin: " + plugin);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterSuite
    public void stop() {
        PluginContainer.getInstance().shutdown();
    }

    @Test
    public void testPluginLoad() {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginEnvironment pluginEnvironment = pluginManager.getPlugin(PLUGIN_NAME);
        assert (pluginEnvironment != null) : "Null environment, plugin not loaded";
        assert (pluginEnvironment.getPluginName().equals(PLUGIN_NAME));
    }

    @Test(dependsOnMethods = "testPluginLoad")
    public void testDiscovery() throws Exception {
        InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
        assert report != null;
        log.info("Discovery took: " + (report.getEndTime() - report.getStartTime()) + "ms");
        Set<Resource> resources = findResource(PluginContainer.getInstance().getInventoryManager().getPlatform(),
            "Tomcat");
        log.info("Found " + resources.size() + " ews / apache tomcat instance(s).");
    }

    private Set<Resource> findResource(Resource parent, String typeName) {
        Set<Resource> found = new HashSet<Resource>();
        Resource platform = parent;
        for (Resource resource : platform.getChildResources()) {
            log.info("Discovered resource of type: " + resource.getResourceType().getName());
            if (resource.getResourceType().getName().equals(typeName)) {
                found.add(resource);
            }
            if (resource.getChildResources() != null) {
                for (Resource child : found) {
                    found.addAll(findResource(child, typeName));
                }
            }
        }
        return found;
    }
}
