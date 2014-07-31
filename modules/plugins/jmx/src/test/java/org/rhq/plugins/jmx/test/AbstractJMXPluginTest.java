/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.plugins.jmx.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;
import static org.rhq.core.domain.resource.ResourceCategory.SERVER;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginEnvironment;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.util.file.FileUtil;

/**
 * @author Thomas Segismont
 */
public class AbstractJMXPluginTest {
    private static final Log LOG = LogFactory.getLog(AbstractJMXPluginTest.class);

    private static final AtomicInteger resourceIdGenerator = new AtomicInteger(Integer.MIN_VALUE / 2);

    protected static final String PLUGIN_NAME = "JMX";
    protected static final String SERVER_TYPE_NAME = "JMX Server";
    protected static final ResourceType SERVER_TYPE = new ResourceType(SERVER_TYPE_NAME, PLUGIN_NAME, SERVER, null);
    protected static final List<File> ADDITIONAL_PLUGIN_FILES = new ArrayList<File>();

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
        copyAdditionalPlugins(pluginDir);
        PluginContainerConfiguration containerConfig = new PluginContainerConfiguration();
        containerConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
        containerConfig.setPluginDirectory(pluginDir);
        containerConfig.setInsideAgent(false);
        // JMX plugin has resources which can only be manually added so we have to mock server integration.
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
        LOG.info("PC started with plugins: " + pluginManager.getMetadataManager().getPluginNames());
        pluginEnvironment = pluginManager.getPlugin(PLUGIN_NAME);
    }

    private static void copyAdditionalPlugins(File pluginDir) throws IOException {
        for (File pluginFile : ADDITIONAL_PLUGIN_FILES) {
            FileUtil.copyFile(pluginFile, new File(pluginDir, pluginFile.getName()));
        }
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

    @Test(dependsOnMethods = { "testPluginLoad" })
    public void testPlatformFound() {
        Resource platform = getInventoryManager().getPlatform();
        assertNotNull(platform, "Platform not found");
    }

    public static Map<String, Object> getMetricsData(MeasurementReport report) {
        Map<String, Object> datas = new HashMap<String, Object>();
        for (MeasurementData data : report.getNumericData()) {
            datas.put(data.getName(), data.getValue());
        }
        for (MeasurementData data : report.getTraitData()) {
            datas.put(data.getName(), data.getValue());
        }
        return datas;
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
