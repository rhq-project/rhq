/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.core.pc.upgrade;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.util.Set;

import org.jmock.Expectations;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.inventory.ResourceContainer.ResourceComponentState;
import org.rhq.test.pc.PluginContainerSetup;
import org.rhq.test.pc.PluginContainerTest;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class ResourceUpgradeTest extends ResourceUpgradeTestBase {

    private static final String INCLUDE_UNCOMMITTED_RESOURCES_TEST = "includeUncommittedResources";
    private static final String UPGRADE_DATA_TEST = "upgradeData";
    private static final String INVENTORY_REINITIALIZATION_FROM_SERVER_DURING_UPGRADE_TEST = "inventoryReinitializationFromServerDuringUpgrade";
    private static final String SKIP_UPGRADE_WHEN_SERVER_UNAVAILABLE_TEST = "skipUpgradeWhenServerUnavailable";
    private static final String UPGRADE_WITH_PLATFORM_DELETED_ON_SERVER_TEST = "upgradeWithPlatformDeletedOnServer";
    private static final String UPGRADE_FAILURE_HANDLING = "upgradeFailureHandling";

    private static final String PLUGIN_V1_FILENAME = "classpath:///resource-upgrade-test-plugin-1.0.0.jar";
    private static final String PLUGIN_V2_FILENAME = "classpath:///resource-upgrade-test-plugin-2.0.0.jar";
    private static final String FAILING_PLUGIN_FILE_NAME = "classpath:///resource-upgrade-test-plugin-3.0.0.jar";

    private static final String SINGLETON_RESOURCE_TYPE_NAME = "Resource";
    private static final String SINGLETON_RESOURCE_TYPE_PLUGIN_NAME = "ResourceUpgradeTest";
    private static final ResType SINGLETON_TYPE = new ResType(SINGLETON_RESOURCE_TYPE_NAME,
        SINGLETON_RESOURCE_TYPE_PLUGIN_NAME);

    @AfterSuite
    public void cleanAfterPluginContainers() throws Exception {
        PluginContainerTest.clearStorage();
    }

    @Test
    @PluginContainerSetup(plugins = { PLUGIN_V1_FILENAME }, sharedGroup = INCLUDE_UNCOMMITTED_RESOURCES_TEST, clearDataDir = true)
    public void testIncludeUncommittedResources_V1() throws Exception {
        initialSyncAndDiscovery(INCLUDE_UNCOMMITTED_RESOURCES_TEST, InventoryStatus.NEW);
    }

    @Test(dependsOnMethods = { "testIncludeUncommittedResources_V1" })
    @PluginContainerSetup(plugins = { PLUGIN_V2_FILENAME }, sharedGroup = INCLUDE_UNCOMMITTED_RESOURCES_TEST, clearInventoryDat = false)
    @SuppressWarnings("unchecked")
    public void testIncludeUncommittedResources_V2() throws Exception {
        final FakeServerInventory inv = (FakeServerInventory) PluginContainerTest
            .getServerSideFake(INCLUDE_UNCOMMITTED_RESOURCES_TEST);
        final ServerServices ss = PluginContainerTest.getCurrentPluginContainerConfiguration().getServerServices();

        PluginContainerTest.getCurrentMockContext().checking(new Expectations() {
            {
                defineDefaultExpectations(inv, this);

                allowing(ss.getDiscoveryServerService()).mergeInventoryReport(with(any(InventoryReport.class)));
                will(inv.mergeInventoryReport(InventoryStatus.COMMITTED));

                oneOf(ss.getDiscoveryServerService()).upgradeResources(with(any(Set.class)));
                will(inv.upgradeResources());
            }
        });

        PluginContainerTest.startConfiguredPluginContainer();

        Set<Resource> resources = getTestingResources(inv, SINGLETON_TYPE);

        assertEquals(resources.size(), 1, "Expected single test resource but multiple found.");

        Resource discoveredResource = resources.iterator().next();

        assertEquals(discoveredResource.getResourceKey(), "resource-key-v2");
        assertEquals(discoveredResource.getName(), "resource-name-v2");
        assertEquals(discoveredResource.getDescription(), "resource-description-v2");
    }

    @Test
    @PluginContainerSetup(plugins = { PLUGIN_V1_FILENAME }, sharedGroup = UPGRADE_DATA_TEST, clearDataDir = true)
    public void testUpgradeData_V1() throws Exception {
        initialSyncAndDiscovery(UPGRADE_DATA_TEST, InventoryStatus.COMMITTED);
    }

    @Test(dependsOnMethods = "testUpgradeData_V1")
    @PluginContainerSetup(plugins = { PLUGIN_V2_FILENAME }, sharedGroup = UPGRADE_DATA_TEST, clearInventoryDat = false)
    public void testUpgradeData_V2() throws Exception {
        upgradeTest(UPGRADE_DATA_TEST);
    }

    @Test
    @PluginContainerSetup(plugins = { PLUGIN_V1_FILENAME }, sharedGroup = INVENTORY_REINITIALIZATION_FROM_SERVER_DURING_UPGRADE_TEST, clearDataDir = true)
    public void testInventoryReinitializationFromServerDuringUpgrade_V1() throws Exception {
        initialSyncAndDiscovery(INVENTORY_REINITIALIZATION_FROM_SERVER_DURING_UPGRADE_TEST, InventoryStatus.COMMITTED);
    }

    @Test(dependsOnMethods = "testInventoryReinitializationFromServerDuringUpgrade_V1")
    @PluginContainerSetup(plugins = { PLUGIN_V2_FILENAME }, sharedGroup = INVENTORY_REINITIALIZATION_FROM_SERVER_DURING_UPGRADE_TEST, clearInventoryDat = false)
    public void testInventoryReinitializationFromServerDuringUpgrade_V2() throws Exception {
        upgradeTest(INVENTORY_REINITIALIZATION_FROM_SERVER_DURING_UPGRADE_TEST);
    }

    @Test
    @PluginContainerSetup(plugins = { PLUGIN_V1_FILENAME }, sharedGroup = SKIP_UPGRADE_WHEN_SERVER_UNAVAILABLE_TEST, clearDataDir = true)
    public void testSkipUpgradeWhenServerUnavailable_V1() throws Exception {
        initialSyncAndDiscovery(SKIP_UPGRADE_WHEN_SERVER_UNAVAILABLE_TEST, InventoryStatus.COMMITTED);
    }

    @Test(dependsOnMethods = "testSkipUpgradeWhenServerUnavailable_V1")
    @PluginContainerSetup(plugins = { PLUGIN_V2_FILENAME }, sharedGroup = SKIP_UPGRADE_WHEN_SERVER_UNAVAILABLE_TEST, clearInventoryDat = false)
    @SuppressWarnings("unchecked")
    public void testSkipUpgradeWhenServerUnavailable_V2() throws Exception {
        final FakeServerInventory inv = (FakeServerInventory) PluginContainerTest
            .getServerSideFake(SKIP_UPGRADE_WHEN_SERVER_UNAVAILABLE_TEST);
        final ServerServices ss = PluginContainerTest.getCurrentPluginContainerConfiguration().getServerServices();

        inv.setFailing(true);

        PluginContainerTest.getCurrentMockContext().checking(new Expectations() {
            {
                defineDefaultExpectations(inv, this);

                allowing(ss.getDiscoveryServerService()).mergeInventoryReport(with(any(InventoryReport.class)));
                will(inv.mergeInventoryReport(InventoryStatus.COMMITTED));

                never(ss.getDiscoveryServerService()).upgradeResources(with(any(Set.class)));
            }
        });

        PluginContainerTest.startConfiguredPluginContainer();

        Set<Resource> discoveredResources = getTestingResources(inv, SINGLETON_TYPE);

        assertEquals(discoveredResources.size(), 1, "Expected single test resource but multiple found.");

        Resource discoveredResource = discoveredResources.iterator().next();

        assertEquals(discoveredResource.getResourceKey(), "resource-key-v1");
        assertEquals(discoveredResource.getName(), "resource-name-v1");
        assertEquals(discoveredResource.getDescription(), "resource-description-v1");
    }

    @Test
    @PluginContainerSetup(plugins = { PLUGIN_V1_FILENAME }, sharedGroup = UPGRADE_WITH_PLATFORM_DELETED_ON_SERVER_TEST, clearDataDir = true)
    public void testUpgradeWithPlatformDeletedOnServer_V1() throws Exception {
        initialSyncAndDiscovery(UPGRADE_WITH_PLATFORM_DELETED_ON_SERVER_TEST, InventoryStatus.COMMITTED);
    }

    @Test(dependsOnMethods = "testUpgradeWithPlatformDeletedOnServer_V1")
    @PluginContainerSetup(plugins = { PLUGIN_V2_FILENAME }, sharedGroup = UPGRADE_WITH_PLATFORM_DELETED_ON_SERVER_TEST, clearInventoryDat = false)
    @SuppressWarnings("unchecked")
    public void testUpgradeWithPlatformDeletedOnServer_V2() throws Exception {

        final FakeServerInventory inv = (FakeServerInventory) PluginContainerTest
            .getServerSideFake(UPGRADE_WITH_PLATFORM_DELETED_ON_SERVER_TEST);
        final ServerServices ss = PluginContainerTest.getCurrentPluginContainerConfiguration().getServerServices();

        PluginContainerTest.getCurrentMockContext().checking(new Expectations() {
            {
                defineDefaultExpectations(inv, this);

                //the first merge will be triggered from within the upgrade process and we are
                //going to report null sync.
                oneOf(ss.getDiscoveryServerService()).mergeInventoryReport(with(any(InventoryReport.class)));
                will(inv.clearPlatform());

                //the rest of the inventory merges are executed by discoveries, so let's import the
                //discovered stuff into the server-side inventory.
                allowing(ss.getDiscoveryServerService()).mergeInventoryReport(with(any(InventoryReport.class)));
                will(inv.mergeInventoryReport(InventoryStatus.COMMITTED));

                never(ss.getDiscoveryServerService()).upgradeResources(with(any(Set.class)));
            }
        });

        PluginContainerTest.startConfiguredPluginContainer();

        Set<Resource> discoveredResources = getTestingResources(inv, SINGLETON_TYPE);

        assertEquals(discoveredResources.size(), 1, "Expected single test resource but multiple found.");

        Resource discoveredResource = discoveredResources.iterator().next();

        assertEquals(discoveredResource.getResourceKey(), "resource-key-v2");
        assertEquals(discoveredResource.getName(), "resource-name-v2");
        assertEquals(discoveredResource.getDescription(), "resource-description-v2");

        ResourceContainer container = PluginContainer.getInstance().getInventoryManager()
            .getResourceContainer(discoveredResource);
        File dataDir = container.getResourceContext().getDataDirectory();

        File marker = new File(dataDir, "upgrade-succeeded");

        assertFalse(marker.exists(),
            "The upgrade seems to have occured even though there shouldn't have been a resource to upgrade.");
    }

    @Test
    @PluginContainerSetup(plugins = PLUGIN_V1_FILENAME, sharedGroup = UPGRADE_FAILURE_HANDLING, clearDataDir = true)
    public void testUpgradeFailureHandling_V1() throws Exception {
        initialSyncAndDiscovery(UPGRADE_FAILURE_HANDLING, InventoryStatus.COMMITTED);
    }

    @Test(dependsOnMethods = "testUpgradeFailureHandling_V1")
    @PluginContainerSetup(plugins = FAILING_PLUGIN_FILE_NAME, sharedGroup = UPGRADE_FAILURE_HANDLING, clearInventoryDat = false)
    @SuppressWarnings("unchecked")
    public void testUpgradeFailureHandling_V2() throws Exception {
        final FakeServerInventory inv = (FakeServerInventory) PluginContainerTest
            .getServerSideFake(UPGRADE_FAILURE_HANDLING);
        final ServerServices ss = PluginContainerTest.getCurrentPluginContainerConfiguration().getServerServices();

        PluginContainerTest.getCurrentMockContext().checking(new Expectations() {
            {
                defineDefaultExpectations(inv, this);

                allowing(ss.getDiscoveryServerService()).mergeInventoryReport(with(any(InventoryReport.class)));
                will(inv.mergeInventoryReport(InventoryStatus.COMMITTED));

                oneOf(ss.getDiscoveryServerService()).upgradeResources(with(any(Set.class)));
                will(inv.upgradeResources());
            }
        });

        PluginContainerTest.startConfiguredPluginContainer();

        Set<Resource> resourceUpgradeTestResources = getTestingResources(inv, SINGLETON_TYPE);

        Resource discoveredResource = resourceUpgradeTestResources.iterator().next();

        assertTrue(discoveredResource.getResourceErrors().size() > 0,
            "There should be upgrade errors persisted on the server side.");

        //the discovery of the failed resource mustn't have run
        ResourceContainer container = PluginContainer.getInstance().getInventoryManager()
            .getResourceContainer(discoveredResource);
        File dataDir = container.getResourceContext().getDataDirectory();

        File marker = new File(dataDir, "failing-discovery-ran");

        assertFalse(marker.exists(),
            "The discovery of the resource type with a failed upgraded resource must not be executed but it was.");
    }

    private FakeServerInventory initialSyncAndDiscovery(String key, final InventoryStatus requiredInventoryStatus)
        throws Exception {
        final FakeServerInventory inv = new FakeServerInventory();
        PluginContainerTest.setServerSideFake(key, inv);
        final ServerServices ss = PluginContainerTest.getCurrentPluginContainerConfiguration().getServerServices();

        PluginContainerTest.getCurrentMockContext().checking(new Expectations() {
            {
                defineDefaultExpectations(inv, this);

                allowing(ss.getDiscoveryServerService()).mergeInventoryReport(with(any(InventoryReport.class)));
                will(inv.mergeInventoryReport(requiredInventoryStatus));
            }
        });

        PluginContainerTest.startConfiguredPluginContainer();

        Set<Resource> resources = getTestingResources(inv, SINGLETON_TYPE);

        assertEquals(resources.size(), 1, "Expected single test resource but multiple found.");

        Resource discoveredResource = resources.iterator().next();

        assertEquals(discoveredResource.getResourceKey(), "resource-key-v1");
        assertEquals(discoveredResource.getName(), "resource-name-v1");
        assertEquals(discoveredResource.getDescription(), "resource-description-v1");

        InventoryManager im = PluginContainer.getInstance().getInventoryManager();
        ResourceContainer container = im.getResourceContainer(discoveredResource.getId());
        if (discoveredResource.getInventoryStatus() == InventoryStatus.COMMITTED) {
            assert container.getResourceComponentState() == ResourceComponentState.STARTED;
        } else {
            assert container.getResourceComponentState() == ResourceComponentState.STOPPED;
        }

        return inv;
    }

    @SuppressWarnings("unchecked")
    private void upgradeTest(String key) throws Exception {
        final FakeServerInventory serverInventory = (FakeServerInventory) PluginContainerTest.getServerSideFake(key);
        final ServerServices ss = PluginContainerTest.getCurrentPluginContainerConfiguration().getServerServices();

        PluginContainerTest.getCurrentMockContext().checking(new Expectations() {
            {
                defineDefaultExpectations(serverInventory, this);

                allowing(ss.getDiscoveryServerService()).mergeInventoryReport(with(any(InventoryReport.class)));
                will(serverInventory.mergeInventoryReport(InventoryStatus.COMMITTED));

                oneOf(ss.getDiscoveryServerService()).upgradeResources(with(any(Set.class)));
                will(serverInventory.upgradeResources());
            }
        });

        PluginContainerTest.startConfiguredPluginContainer();

        Set<Resource> discoveredResources = getTestingResources(serverInventory, SINGLETON_TYPE);

        assertEquals(discoveredResources.size(), 1, "Expected single test resource but multiple found.");

        Resource discoveredResource = discoveredResources.iterator().next();

        assertEquals(discoveredResource.getResourceKey(), "resource-key-v2");
        assertEquals(discoveredResource.getName(), "resource-name-v2");
        assertEquals(discoveredResource.getDescription(), "resource-description-v2");

        ResourceContainer container = PluginContainer.getInstance().getInventoryManager()
            .getResourceContainer(discoveredResource);
        File dataDir = container.getResourceContext().getDataDirectory();

        File marker = new File(dataDir, "upgrade-succeeded");

        assertTrue(marker.exists(),
            "The upgrade success marker file wasn't found. This means the upgrade didn't actually run.");
    }
}
