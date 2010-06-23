/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.pc.upgrade;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.bundle.BundleServerService;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.clientapi.server.event.EventServerService;
import org.rhq.core.clientapi.server.inventory.ResourceFactoryServerService;
import org.rhq.core.clientapi.server.measurement.MeasurementServerService;
import org.rhq.core.clientapi.server.operation.OperationServerService;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;

/**
 * Test cases for resource upgrade.
 * 
 * @author Lukas Krejci
 */
@Test(sequential = true, invocationCount = 1)
public class ResourceUpgradeTest {

    private static final String PLUGIN_V1_FILENAME = "resource-upgrade-test-plugin-1.0.0.jar";
    private static final String PLUGIN_V2_FILENAME = "resource-upgrade-test-plugin-2.0.0.jar";
    
    private static final String PLUGINS_DIR_NAME = "plugins";
    private static final String DATA_DIR_NAME = "data";
    private static final String TMP_DIR_NAME = "tmp";
    
    private File tmpDir;
    private File pluginDir;
    private File dataDir;
    
    private BundleServerService currentBundleServerService;
    private ConfigurationServerService currentConfigurationServerService;
    private ContentServerService currentContentServerService;
    private CoreServerService currentCoreServerService;
    private DiscoveryServerService currentDiscoveryServerService;
    private EventServerService currentEventServerService;
    private MeasurementServerService currentMeasurementServerService;
    private OperationServerService currentOperationServerService;
    private ResourceFactoryServerService currentResourceFactoryServerService;
    
    private FakeServerInventory currentServerSideInventory;
    
    @AfterClass
    public void undeployPlugins() throws IOException {
        FileUtils.deleteDirectory(tmpDir);
    }
    
    @BeforeClass
    public void sanityCheck() {
        verifyPluginExists(PLUGIN_V1_FILENAME);
        verifyPluginExists(PLUGIN_V2_FILENAME);
    }
    
    @BeforeClass(dependsOnMethods = "sanityCheck")
    public void init() {
        tmpDir = getTmpDirectory();
        pluginDir = new File(tmpDir, PLUGINS_DIR_NAME);
        assertTrue(pluginDir.mkdir(), "Could not create plugin deploy directory.");
        dataDir = new File(tmpDir, DATA_DIR_NAME);
        assertTrue(dataDir.mkdir(), "Could not create plugin container data directory.");
    }

    @Test
    public void testIgnoreUncommittedResources() throws Exception {
        currentServerSideInventory = new FakeServerInventory();
        initialSyncAndDiscovery(InventoryStatus.NEW);
        
        TestPayload testNoChange = new TestPayload() {           
            public void test(Resource discoveredResource) {
                assertEquals(discoveredResource.getResourceKey(), "resource-key-v1");
                assertEquals(discoveredResource.getName(), "resource-name-v1");
                assertEquals(discoveredResource.getDescription(), "resource-description-v1");
            }
            
            public Expectations getExpectations(Mockery context) throws Exception {
                return new Expectations() {
                    {
                        defineDefaultExpectations(this);
                        
                        between(1, 4).of(currentDiscoveryServerService).mergeInventoryReport(with(any(InventoryReport.class)));
                        will(currentServerSideInventory.mergeInventoryReport(InventoryStatus.COMMITTED));
                    }
                };
            }
        };
        
        executeTestWithPlugins(Collections.singleton(PLUGIN_V2_FILENAME), false, testNoChange);
    }
    
    @Test
    public void testUpgradeData() throws Exception {
        currentServerSideInventory = new FakeServerInventory();
        upgradeTest(false);
    }
    
    @Test
    public void testInventoryReinitializationFromServerDuringUpgrade() throws Exception {
        currentServerSideInventory = new FakeServerInventory();
        upgradeTest(true);
    }
    
    @Test
    public void testResourceUpgradeRunsOnlyOnce() throws Exception {
        currentServerSideInventory = new FakeServerInventory();
        initialSyncAndDiscovery(InventoryStatus.COMMITTED);
        
        executeTestWithPlugins(Collections.singleton(PLUGIN_V2_FILENAME), false, new TestPayload() {
            public void test(Resource discoveredResource) {
                for(int i = 0; i < 100; i++) {
                    PluginContainer.getInstance().getInventoryManager().fireResourceUpgrade();
                }
            }

            @SuppressWarnings("unchecked")
            public Expectations getExpectations(Mockery context) throws Exception {
                return new Expectations() {
                    {
                        defineDefaultExpectations(this);
                        
                        between(1, 4).of(currentDiscoveryServerService).mergeInventoryReport(with(any(InventoryReport.class)));
                        will(currentServerSideInventory.mergeInventoryReport(InventoryStatus.COMMITTED));
                        
                        //even though we fire the resource upgrade 100 times above, 
                        //only 1 upgrade should actually occur and go up to the server.
                        oneOf(currentDiscoveryServerService).upgradeResources(with(any(Set.class)));
                        will(currentServerSideInventory.upgradeResources());
                    }
                };
            }
        });
    }
    
    private void initialSyncAndDiscovery(final InventoryStatus requiredInventoryStatus) throws Exception {
        executeTestWithPlugins(Collections.singleton(PLUGIN_V1_FILENAME), true, new TestPayload() {           
            public void test(Resource discoveredResource) {
                assertEquals(discoveredResource.getResourceKey(), "resource-key-v1");
                assertEquals(discoveredResource.getName(), "resource-name-v1");
                assertEquals(discoveredResource.getDescription(), "resource-description-v1");
            }

            public Expectations getExpectations(Mockery context) throws Exception {
                return new Expectations() {
                    {
                        defineDefaultExpectations(this);
                        
                        between(1, 4).of(currentDiscoveryServerService).mergeInventoryReport(with(any(InventoryReport.class)));
                        will(currentServerSideInventory.mergeInventoryReport(requiredInventoryStatus));
                    }
                };
            }
        });                
        
    }
    
    private void upgradeTest(boolean clearInventoryDat) throws Exception {
        initialSyncAndDiscovery(InventoryStatus.COMMITTED);
        
        executeTestWithPlugins(Collections.singleton(PLUGIN_V2_FILENAME), clearInventoryDat, new TestPayload() {
            public void test(Resource discoveredResource) {
                assertEquals(discoveredResource.getResourceKey(), "resource-key-v2");
                assertEquals(discoveredResource.getName(), "resource-name-v2");
                assertEquals(discoveredResource.getDescription(), "resource-description-v2");
                
                ResourceContainer container = PluginContainer.getInstance().getInventoryManager().getResourceContainer(discoveredResource);
                File dataDir = container.getResourceContext().getDataDirectory();
                
                File marker = new File(dataDir, "upgrade-succeeded");
                
                assertTrue(marker.exists(), "The upgrade success marker file wasn't found. This means the upgrade didn't actually run.");
            }

            @SuppressWarnings("unchecked")
            public Expectations getExpectations(Mockery context) throws Exception {
                return new Expectations() {
                    {
                        defineDefaultExpectations(this);
                        
                        between(1, 4).of(currentDiscoveryServerService).mergeInventoryReport(with(any(InventoryReport.class)));
                        will(currentServerSideInventory.mergeInventoryReport(InventoryStatus.COMMITTED));
                        
                        oneOf(currentDiscoveryServerService).upgradeResources(with(any(Set.class)));
                        will(currentServerSideInventory.upgradeResources());
                    }
                };
            }
        });
    }
    
    private PluginContainerConfiguration createPluginContainerConfiguration(Mockery context) throws Exception {
        PluginContainerConfiguration conf = new PluginContainerConfiguration();
        
        conf.setPluginDirectory(new File(tmpDir, PLUGINS_DIR_NAME));
        conf.setDataDirectory(new File(tmpDir, DATA_DIR_NAME));
        conf.setTemporaryDirectory(new File(tmpDir, TMP_DIR_NAME));
        conf.setInsideAgent(true); //pc must think it's inside an agent so that it persists the inventory between restarts
        conf.setPluginFinder(new FileSystemPluginFinder(conf.getPluginDirectory()));
        conf.setCreateResourceClassloaders(false); 
        
        //we're not interested in any scans happening out of our control
        conf.setAvailabilityScanInitialDelay(Long.MAX_VALUE);
        conf.setConfigurationDiscoveryInitialDelay(Long.MAX_VALUE);
        conf.setContentDiscoveryInitialDelay(Long.MAX_VALUE);
        conf.setEventSenderInitialDelay(Long.MAX_VALUE);
        conf.setMeasurementCollectionInitialDelay(Long.MAX_VALUE);
        conf.setServerDiscoveryInitialDelay(Long.MAX_VALUE);
        conf.setServiceDiscoveryInitialDelay(Long.MAX_VALUE);
        
        currentBundleServerService = context.mock(BundleServerService.class);
        currentConfigurationServerService = context.mock(ConfigurationServerService.class);
        currentContentServerService = context.mock(ContentServerService.class);
        currentCoreServerService = context.mock(CoreServerService.class);
        currentDiscoveryServerService = context.mock(DiscoveryServerService.class);
        currentEventServerService = context.mock(EventServerService.class);
        currentMeasurementServerService = context.mock(MeasurementServerService.class);
        currentOperationServerService = context.mock(OperationServerService.class);
        currentResourceFactoryServerService = context.mock(ResourceFactoryServerService.class);
        
        ServerServices serverServices = new ServerServices();
        serverServices.setBundleServerService(currentBundleServerService);
        serverServices.setConfigurationServerService(currentConfigurationServerService);
        serverServices.setContentServerService(currentContentServerService);
        serverServices.setCoreServerService(currentCoreServerService);
        serverServices.setDiscoveryServerService(currentDiscoveryServerService);
        serverServices.setEventServerService(currentEventServerService);
        serverServices.setMeasurementServerService(currentMeasurementServerService);
        serverServices.setOperationServerService(currentOperationServerService);
        serverServices.setResourceFactoryServerService(currentResourceFactoryServerService);
        
        conf.setServerServices(serverServices);
        
        return conf;
    }

    /**
     * @param pluginResourcePath
     */
    private void verifyPluginExists(String pluginResourcePath) {
        URL url = getClass().getResource(pluginResourcePath);
        
        File pluginFile = FileUtils.toFile(url);
        
        assertTrue(pluginFile.exists(), pluginFile.getAbsoluteFile() + " plugin jar could not be found.");
    }
    
    private void copyPlugin(String pluginResourcePath, File pluginDirectory) throws IOException {
        URL pluginUrl = getClass().getResource(pluginResourcePath);
        
        File pluginFile = new File(pluginResourcePath);
        String pluginFileName = pluginFile.getName();
        
        FileUtils.copyURLToFile(pluginUrl, new File(pluginDirectory, pluginFileName));
    }
    
    private static File getTmpDirectory() {
        File ret = new File(System.getProperty("java.io.tmpdir"), "resource-upgrade-test" + System.currentTimeMillis());
        
        while (ret.exists() || !ret.mkdir()) {
            ret = new File(System.getProperty("java.io.tmpdir"), "resource-upgrade-test" + System.currentTimeMillis());
        }

        return ret;
    }

    private interface TestPayload {
        Expectations getExpectations(Mockery context) throws Exception;
        void test(Resource resourceUpgradeTestResource);
    }
    
    private Set<Resource> getTestingResources() {
        ResourceType resType = PluginContainer.getInstance().getPluginManager().getMetadataManager().getType("Resource", "ResourceUpgradeTest");
        
        return currentServerSideInventory.findResourcesByType(resType);
    }
    
    private void executeTestWithPlugins(Set<String> pluginResourcePaths, boolean clearInventoryDat, TestPayload test) throws Exception {
        FileUtils.cleanDirectory(new File(tmpDir, PLUGINS_DIR_NAME));
        
        for(String pluginResourcePath : pluginResourcePaths) {
            copyPlugin(pluginResourcePath, pluginDir);
        }
        
        Mockery context = new Mockery();
        
        PluginContainerConfiguration pcConfig = createPluginContainerConfiguration(context);
        
        if (clearInventoryDat) {
            File inventoryDat = new File(pcConfig.getDataDirectory(), "inventory.dat");
            inventoryDat.delete();
        }
        
        context.checking(test.getExpectations(context));
        
        PluginContainer.getInstance().setConfiguration(pcConfig);
        PluginContainer.getInstance().initialize();
       
        //give the pc the time to finish resource upgrade
        Thread.sleep(1000);
        
        //execute full discovery
        InventoryManager im = PluginContainer.getInstance().getInventoryManager();
        im.executeServerScanImmediately();
        im.executeServiceScanImmediately();
        
        Set<Resource> resources = getTestingResources();
        
        assertEquals(resources.size(), 1, "There should be only a single testing resource but " + resources + " were found.");
        
        Resource discoveredResource = resources.iterator().next();
        
        test.test(discoveredResource);
        
        PluginContainer.getInstance().shutdown();    
        
        context.assertIsSatisfied();
    }
    
    @SuppressWarnings("unchecked")
    private void defineDefaultExpectations(Expectations expectations) {
        expectations.ignoring(currentBundleServerService);
        expectations.ignoring(currentConfigurationServerService);
        expectations.ignoring(currentContentServerService);
        expectations.ignoring(currentCoreServerService);
        expectations.ignoring(currentEventServerService);
        expectations.ignoring(currentMeasurementServerService);
        expectations.ignoring(currentOperationServerService);
        expectations.ignoring(currentResourceFactoryServerService);
        
        //just ignore these invocations if we get a availability scan in the PC...
        expectations.allowing(currentDiscoveryServerService).mergeAvailabilityReport(expectations.with(Expectations.any(AvailabilityReport.class)));

        expectations.allowing(currentDiscoveryServerService).getResources(expectations.with(Expectations.any(Set.class)), expectations.with(Expectations.any(boolean.class)));
        expectations.will(currentServerSideInventory.getResources());
    }
}
