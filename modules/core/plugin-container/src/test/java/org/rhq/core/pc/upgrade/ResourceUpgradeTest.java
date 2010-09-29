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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test cases for resource upgrade.
 * 
 * @author Lukas Krejci
 */
@Test(sequential = true, invocationCount = 1)
public class ResourceUpgradeTest extends ResourceUpgradeTestBase {

    private static final String PLUGIN_V1_FILENAME = "/resource-upgrade-test-plugin-1.0.0.jar";
    private static final String PLUGIN_V2_FILENAME = "/resource-upgrade-test-plugin-2.0.0.jar";
    private static final String FAILING_PLUGIN_FILE_NAME = "/resource-upgrade-test-plugin-3.0.0.jar";
    
    static final String SINGLETON_RESOURCE_TYPE_NAME = "Resource";
    static final String SINGLETON_RESOURCE_TYPE_PLUGIN_NAME = "ResourceUpgradeTest";
    
    @BeforeClass
    public void sanityCheck() {
        verifyPluginExists(PLUGIN_V1_FILENAME);
        verifyPluginExists(PLUGIN_V2_FILENAME);
        verifyPluginExists(FAILING_PLUGIN_FILE_NAME);
    }
    
    @Test
    public void testIgnoreUncommittedResources() throws Exception {
        setCurrentServerSideInventory(new FakeServerInventory());
        initialSyncAndDiscovery(InventoryStatus.NEW);
        
        TestPayload testNoChange = new AbstractTestPayload(false, SINGLETON_RESOURCE_TYPE_NAME, SINGLETON_RESOURCE_TYPE_PLUGIN_NAME) {           
            public void test(Set<Resource> discoveredResources) {
                assertEquals(discoveredResources.size(), 1, "Expected single test resource but multiple found.");
                
                Resource discoveredResource = discoveredResources.iterator().next();

                assertEquals(discoveredResource.getResourceKey(), "resource-key-v1");
                assertEquals(discoveredResource.getName(), "resource-name-v1");
                assertEquals(discoveredResource.getDescription(), "resource-description-v1");
            }
            
            public Expectations getExpectations(Mockery context) throws Exception {
                return new Expectations() {
                    {
                        defineDefaultExpectations(this);
                        
                        between(1, 4).of(getCurrentDiscoveryServerService()).mergeInventoryReport(with(any(InventoryReport.class)));
                        will(getCurrentServerSideInventory().mergeInventoryReport(InventoryStatus.COMMITTED));
                    }
                };
            }
        };
        
        executeTestWithPlugins(Collections.singleton(PLUGIN_V2_FILENAME), testNoChange);
    }
    
    @Test
    public void testUpgradeData() throws Exception {
        setCurrentServerSideInventory(new FakeServerInventory());
        upgradeTest(false);
    }
    
    @Test
    public void testInventoryReinitializationFromServerDuringUpgrade() throws Exception {
        setCurrentServerSideInventory(new FakeServerInventory());
        upgradeTest(true);
    }
    
    @Test
    public void testSkipUpgradeWhenServerUnavailable() throws Exception {
        setCurrentServerSideInventory(new FakeServerInventory());
        initialSyncAndDiscovery(InventoryStatus.COMMITTED);
        
        getCurrentServerSideInventory().setFailing(true);

        TestPayload test = new AbstractTestPayload(false, SINGLETON_RESOURCE_TYPE_NAME, SINGLETON_RESOURCE_TYPE_PLUGIN_NAME) {           
            public void test(Set<Resource> discoveredResources) {
                assertEquals(discoveredResources.size(), 1, "Expected single test resource but multiple found.");
                
                Resource discoveredResource = discoveredResources.iterator().next();
                
                assertEquals(discoveredResource.getResourceKey(), "resource-key-v1");
                assertEquals(discoveredResource.getName(), "resource-name-v1");
                assertEquals(discoveredResource.getDescription(), "resource-description-v1");
            }
            
            @SuppressWarnings("unchecked")
            public Expectations getExpectations(Mockery context) throws Exception {
                return new Expectations() {
                    {
                        defineDefaultExpectations(this);
                        
                        between(1, 4).of(getCurrentDiscoveryServerService()).mergeInventoryReport(with(any(InventoryReport.class)));
                        will(getCurrentServerSideInventory().mergeInventoryReport(InventoryStatus.COMMITTED));
                        
                        never(getCurrentDiscoveryServerService()).upgradeResources(with(any(Set.class)));
                    }
                };
            }
        };
        
        executeTestWithPlugins(Collections.singleton(PLUGIN_V2_FILENAME), test);
    }
    
    @Test
    public void testUpgradeWithPlatformDeletedOnServer() throws Exception {
        setCurrentServerSideInventory(new FakeServerInventory());
        initialSyncAndDiscovery(InventoryStatus.COMMITTED);
        
        TestPayload test = new AbstractTestPayload(false, SINGLETON_RESOURCE_TYPE_NAME, SINGLETON_RESOURCE_TYPE_PLUGIN_NAME) {           
            public void test(Set<Resource> discoveredResources) {
                assertEquals(discoveredResources.size(), 1, "Expected single test resource but multiple found.");
                
                Resource discoveredResource = discoveredResources.iterator().next();
                
                assertEquals(discoveredResource.getResourceKey(), "resource-key-v2");
                assertEquals(discoveredResource.getName(), "resource-name-v2");
                assertEquals(discoveredResource.getDescription(), "resource-description-v2");

                ResourceContainer container = PluginContainer.getInstance().getInventoryManager().getResourceContainer(discoveredResource);
                File dataDir = container.getResourceContext().getDataDirectory();
                
                File marker = new File(dataDir, "upgrade-succeeded");
                
                assertFalse(marker.exists(), "The upgrade seems to have occured even though there shouldn't have been a resource to upgrade.");
            }
            
            @SuppressWarnings("unchecked")
            public Expectations getExpectations(Mockery context) throws Exception {
                return new Expectations() {
                    {
                        defineDefaultExpectations(this);
                        
                        //the first merge will be triggered from within the upgrade process and we are
                        //going to report null sync.
                        oneOf(getCurrentDiscoveryServerService()).mergeInventoryReport(with(any(InventoryReport.class)));
                        will(getCurrentServerSideInventory().clearPlatform());
                        
                        //the rest of the inventory merges are executed by discoveries, so let's import the
                        //discovered stuff into the server-side inventory.
                        between(1, 3).of(getCurrentDiscoveryServerService()).mergeInventoryReport(with(any(InventoryReport.class)));
                        will(getCurrentServerSideInventory().mergeInventoryReport(InventoryStatus.COMMITTED));
                        
                        never(getCurrentDiscoveryServerService()).upgradeResources(with(any(Set.class)));
                    }
                };
            }
        };
        
        executeTestWithPlugins(Collections.singleton(PLUGIN_V2_FILENAME), test);
    }
    
    @Test
    public void testUpgradeFailureHandling() throws Exception {
        setCurrentServerSideInventory(new FakeServerInventory());
        initialSyncAndDiscovery(InventoryStatus.COMMITTED);
        
        TestPayload test = new AbstractTestPayload(false, SINGLETON_RESOURCE_TYPE_NAME, SINGLETON_RESOURCE_TYPE_PLUGIN_NAME) {            
            public void test(Set<Resource> resourceUpgradeTestResources) {
                assertEquals(resourceUpgradeTestResources.size(), 1, "Expected single test resource but multiple found.");
                
                Resource discoveredResource = resourceUpgradeTestResources.iterator().next();
                
                assertTrue(discoveredResource.getResourceErrors().size() > 0, "There should be upgrade errors persisted on the server side.");
                
                //the discovery of the failed resource mustn't have run
                ResourceContainer container = PluginContainer.getInstance().getInventoryManager().getResourceContainer(discoveredResource);
                File dataDir = container.getResourceContext().getDataDirectory();
                
                File marker = new File(dataDir, "failing-discovery-ran");
                
                assertFalse(marker.exists(), "The discovery of the resource type with a failed upgraded resource must not be executed but it was.");                
            }
            
            @SuppressWarnings("unchecked")
            public Expectations getExpectations(Mockery context) throws Exception {
                return new Expectations() {
                    {
                        defineDefaultExpectations(this);
                        
                        between(1, 4).of(getCurrentDiscoveryServerService()).mergeInventoryReport(with(any(InventoryReport.class)));
                        will(getCurrentServerSideInventory().mergeInventoryReport(InventoryStatus.COMMITTED));
                        
                        oneOf(getCurrentDiscoveryServerService()).upgradeResources(with(any(Set.class)));
                        will(getCurrentServerSideInventory().upgradeResources());
                    }
                };
            }
        };
        
        executeTestWithPlugins(Collections.singleton(FAILING_PLUGIN_FILE_NAME), test);
    }
    
    private void initialSyncAndDiscovery(final InventoryStatus requiredInventoryStatus) throws Exception {
        cleanDataDir();
        executeTestWithPlugins(Collections.singleton(PLUGIN_V1_FILENAME), new AbstractTestPayload(true, SINGLETON_RESOURCE_TYPE_NAME, SINGLETON_RESOURCE_TYPE_PLUGIN_NAME) {           
            public void test(Set<Resource> discoveredResources) {
                assertEquals(discoveredResources.size(), 1, "Expected single test resource but multiple found.");
                
                Resource discoveredResource = discoveredResources.iterator().next();
                
                assertEquals(discoveredResource.getResourceKey(), "resource-key-v1");
                assertEquals(discoveredResource.getName(), "resource-name-v1");
                assertEquals(discoveredResource.getDescription(), "resource-description-v1");
            }
    
            public Expectations getExpectations(Mockery context) throws Exception {
                return new Expectations() {
                    {
                        defineDefaultExpectations(this);
                        
                        between(1, 4).of(getCurrentDiscoveryServerService()).mergeInventoryReport(with(any(InventoryReport.class)));
                        will(getCurrentServerSideInventory().mergeInventoryReport(requiredInventoryStatus));
                    }
                };
            }
        });                
        
    }

    
    private void upgradeTest(boolean clearInventoryDat) throws Exception {
        initialSyncAndDiscovery(InventoryStatus.COMMITTED);
        
        executeTestWithPlugins(Collections.singleton(PLUGIN_V2_FILENAME), new AbstractTestPayload(clearInventoryDat, SINGLETON_RESOURCE_TYPE_NAME, SINGLETON_RESOURCE_TYPE_PLUGIN_NAME) {
            public void test(Set<Resource> discoveredResources) {
                assertEquals(discoveredResources.size(), 1, "Expected single test resource but multiple found.");
                
                Resource discoveredResource = discoveredResources.iterator().next();
                
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
                        
                        between(1, 4).of(getCurrentDiscoveryServerService()).mergeInventoryReport(with(any(InventoryReport.class)));
                        will(getCurrentServerSideInventory().mergeInventoryReport(InventoryStatus.COMMITTED));
                        
                        oneOf(getCurrentDiscoveryServerService()).upgradeResources(with(any(Set.class)));
                        will(getCurrentServerSideInventory().upgradeResources());
                    }
                };
            }
        });
    }
}
