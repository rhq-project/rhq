/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.plugins.netservices.itest;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginEnvironment;
import org.rhq.core.pc.plugin.PluginManager;

/**
 * @author Thomas Segismont
 */
public abstract class NetServiceComponentTest {

    public static final String PLUGIN_NAME = "NetworkServices";

    private static final Log LOG = LogFactory.getLog(NetServiceComponentTest.class);

    private static final AtomicInteger resourceIdGenerator = new AtomicInteger(Integer.MIN_VALUE / 2);

    private static PluginContainer pluginContainer;

    private static InventoryManager inventoryManager;

    private static Resource platform;

    private static PluginManager pluginManager;

    private static PluginEnvironment pluginEnvironment;

    protected PluginContainer getPluginContainer() {
        return pluginContainer;
    }

    protected InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    protected Resource getPlatform() {
        return platform;
    }

    protected PluginManager getPluginManager() {
        return pluginManager;
    }

    protected PluginEnvironment getPluginEnvironment() {
        return pluginEnvironment;
    }

    @BeforeSuite
    public static void startPluginContainer() throws Exception {
        LOG.info("Setting up plugin container");
        File pluginDir = new File("target/itest/plugins");
        PluginContainerConfiguration containerConfig = new PluginContainerConfiguration();
        containerConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
        containerConfig.setPluginDirectory(pluginDir);
        containerConfig.setInsideAgent(false);
        // netservices plugin has resources which can only be manually added so we have to mock server integration.
        DiscoveryServerService discoveryServerService = Mockito.mock(DiscoveryServerService.class);
        when(discoveryServerService.addResource(any(Resource.class), anyInt())).thenAnswer(
            new Answer<MergeResourceResponse>() {

                @Override
                public MergeResourceResponse answer(InvocationOnMock invocation) throws Throwable {
                    return new MergeResourceResponse(resourceIdGenerator.decrementAndGet(), System.currentTimeMillis(),
                        false);
                }
            });
        ServerServices serverServices = new ServerServices();
        serverServices.setDiscoveryServerService(discoveryServerService);
        containerConfig.setServerServices(serverServices);
        pluginContainer = PluginContainer.getInstance();
        pluginContainer.setConfiguration(containerConfig);
        pluginContainer.initialize();
        inventoryManager = pluginContainer.getInventoryManager();
        platform = inventoryManager.getPlatform();
        pluginManager = pluginContainer.getPluginManager();
        pluginEnvironment = pluginManager.getPlugin(PLUGIN_NAME);
    }

    @AfterSuite
    public static void stopPluginContainer() {
        LOG.info("Shutting down plugin container");
        try {
            if (pluginContainer != null) {
                pluginContainer.shutdown();
            }
        } catch (Exception ignore) {
        }
    }

    @Test
    public void testPluginLoad() {
        assertNotNull(pluginEnvironment, "Plugin not loaded");
        assertEquals(pluginEnvironment.getPluginName(), PLUGIN_NAME);
    }

    public static Double getMetric(Map<String, Object> datas, String metricName) {
        assertTrue(datas.containsKey(metricName), metricName + " metric not found");
        assertTrue(datas.get(metricName).getClass().equals(Double.class), metricName + " metric value is not a Double");
        return (Double) datas.get(metricName);
    }

    public static String getTrait(Map<String, Object> datas, String traitName) {
        assertTrue(datas.containsKey(traitName), traitName + " trait not found");
        assertTrue(datas.get(traitName).getClass().equals(String.class), traitName + " traitName value is not a String");
        return (String) datas.get(traitName);
    }

}
