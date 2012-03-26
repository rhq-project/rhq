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

import org.jmock.Expectations;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.test.pc.PluginContainerSetup;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class ResourceUpgradeProgressHandlingTest extends AbstractResourceUpgradeHandlingTest {

    //test names
    private static final String DUPLICATE_RESOURCE_KEYS_HANDLED_CORRECTLY_TEST = "DuplicateResourceKeysHandledCorrectly";
    private static final String PARENT_RESOURCE_STARTED_UPGRADED_WHEN_CHILD_RESOURCE_BEING_UPGRADED_TEST = "ParentResourceStartedUpgradedWhenChildResourceBeingUpgraded";

    //plugin names
    private static final String BASE_PLUGIN_NAME = "classpath:///resource-upgrade-test-plugin-multi-base-1.0.0.jar";
    private static final String PARENT_DEP_V1_PLUGIN_NAME = "classpath:///resource-upgrade-test-plugin-multi-parentdep-1.0.0.jar";
    private static final String PARENT_DEP_V2_PLUGIN_NAME = "classpath:///resource-upgrade-test-plugin-multi-parentdep-2.0.0.jar";
    private static final String ROOT_PLUGIN_NAME = "classpath:///resource-upgrade-test-plugin-multi-root-1.0.0.jar";
    private static final String UPGRADE_PROGRESS_PLUGIN_V1_FILENAME = "classpath:///resource-upgrade-test-plugin-progress-test-1.0.0.jar";
    private static final String UPGRADE_PROGRESS_PLUGIN_V2_FILENAME = "classpath:///resource-upgrade-test-plugin-progress-test-2.0.0.jar";
    private static final String UPGRADE_DUPLICATE_PLUGIN_V1_FILENAME = "classpath:///resource-upgrade-test-plugin-duplicate-test-1.0.0.jar";
    private static final String UPGRADE_DUPLICATE_PLUGIN_V2_FILENAME = "classpath:///resource-upgrade-test-plugin-duplicate-test-2.0.0.jar";

    private static final ResType TEST_TYPE = new ResType("TestResource", "test");
    private static final ResType PARENT_DEP_TYPE = new ResType("ParentDependency", "parentdep");

    @Test(dependsOnMethods = "testDuplicateResourceKeysHandledCorrectly_V2")
    @PluginContainerSetup( //
    plugins = { UPGRADE_PROGRESS_PLUGIN_V1_FILENAME, BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V1_PLUGIN_NAME }, //
    sharedGroup = PARENT_RESOURCE_STARTED_UPGRADED_WHEN_CHILD_RESOURCE_BEING_UPGRADED_TEST, clearDataDir = true)
    public void testParentResourceStartedUpgradedWhenChildResourceBeingUpgraded_V1() throws Exception {
        final FakeServerInventory inventory = new FakeServerInventory();
        setServerSideFake(PARENT_RESOURCE_STARTED_UPGRADED_WHEN_CHILD_RESOURCE_BEING_UPGRADED_TEST, inventory);

        context.checking(new Expectations() {
            {
                defineDefaultExpectations(inventory, this);
            }
        });

        startConfiguredPluginContainer();

        //in here we set up the failures that are going to happen when
        //the v2 plugins are run

        Resource parent = findResourceWithOrdinal(PARENT_DEP_TYPE, 0);
        Assert.assertNotNull(parent, "Failed to find the parent.");
    }

    @Test(dependsOnMethods = "testParentResourceStartedUpgradedWhenChildResourceBeingUpgraded_V1")
    @PluginContainerSetup( //
    plugins = { UPGRADE_PROGRESS_PLUGIN_V2_FILENAME, BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V2_PLUGIN_NAME }, //
    sharedGroup = PARENT_RESOURCE_STARTED_UPGRADED_WHEN_CHILD_RESOURCE_BEING_UPGRADED_TEST, clearInventoryDat = false)
    public void testParentResourceStartedUpgradedWhenChildResourceBeingUpgraded_V2() throws Exception {
        final FakeServerInventory inventory = (FakeServerInventory) getServerSideFake(PARENT_RESOURCE_STARTED_UPGRADED_WHEN_CHILD_RESOURCE_BEING_UPGRADED_TEST);
        context.checking(new Expectations() {
            {
                defineDefaultExpectations(inventory, this);
            }
        });

        startConfiguredPluginContainer();

        //the upgrade progress plugin is set to check that the parent resource key
        //has been upgraded during its upgrade method, so we just need to check here
        //that everything got upgraded. If it was not, it'd mean that the the progress
        //plugin failed the upgrade because it didn't see its parent upgraded.

        checkResourcesUpgraded(getTestingResources(inventory, PARENT_DEP_TYPE), 1);
        checkResourcesUpgraded(getTestingResources(inventory, TEST_TYPE), 2);
    }

    @Test
    @PluginContainerSetup( //
    plugins = { UPGRADE_DUPLICATE_PLUGIN_V1_FILENAME, BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V1_PLUGIN_NAME }, //
    sharedGroup = DUPLICATE_RESOURCE_KEYS_HANDLED_CORRECTLY_TEST, clearDataDir = true, numberOfInitialDiscoveries = 2)
    public void testDuplicateResourceKeysHandledCorrectly_V1() throws Exception {
        final FakeServerInventory inventory = new FakeServerInventory();
        setServerSideFake(DUPLICATE_RESOURCE_KEYS_HANDLED_CORRECTLY_TEST, inventory);

        context.checking(new Expectations() {
            {
                defineDefaultExpectations(inventory, this);
            }
        });

        startConfiguredPluginContainer();

        //there's not much to check with the v1 plugins. let's just check all the 
        //resources have been discovered
        assertEquals(getTestingResources(inventory, PARENT_DEP_TYPE).size(), 1,
            "The V1 inventory should have 1 parent.");
        assertEquals(getTestingResources(inventory, TEST_TYPE).size(), 2,
            "The V1 inventory should have 2 test resources.");
        int foo = 0;
    }

    @Test(dependsOnMethods = "testDuplicateResourceKeysHandledCorrectly_V1")
    @PluginContainerSetup( //
    plugins = { UPGRADE_DUPLICATE_PLUGIN_V2_FILENAME, BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V2_PLUGIN_NAME }, //
    sharedGroup = DUPLICATE_RESOURCE_KEYS_HANDLED_CORRECTLY_TEST, clearInventoryDat = false, numberOfInitialDiscoveries = 2)
    public void testDuplicateResourceKeysHandledCorrectly_V2() throws Exception {
        final FakeServerInventory inventory = (FakeServerInventory) getServerSideFake(DUPLICATE_RESOURCE_KEYS_HANDLED_CORRECTLY_TEST);

        context.checking(new Expectations() {
            {
                defineDefaultExpectations(inventory, this);
            }
        });

        startConfiguredPluginContainer();

        //now the V2 test resource is set to create 2 resources with the same resource keys.
        //the upgrade should therefore fail.

        checkResourcesUpgraded(getTestingResources(inventory, PARENT_DEP_TYPE), 1);

        checkResourcesNotUpgraded(getTestingResources(inventory, TEST_TYPE), 2);

        for (Resource r : getTestingResources(inventory, TEST_TYPE)) {
            checkResourceFailedUpgrade(r);
        }
    }
}
