/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.upgrade.plugins.multi.base.BaseResourceComponentInterface;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;
import org.rhq.test.pc.PluginContainerSetup;

/**
 * The plugins and their resource types form the following dependency structure:
 * <pre>
 *                                       Root(root)
 *                                       /        \
 *               ParentDependency(parentdep)  ParentDepSibling(parentsibling)
 *                      /          \
 *            TestResource(test) TestResourceSibling(sibling)  
 * </pre>
 * The dependencies in the above "chart" are formed using either the <code>&lt;runs-inside&gt;</code>
 * or <code>sourcePlugin/sourceType</code> approaches just to test that both are handled correctly.
 * <p>
 * The <code>parentdep</code>, <code>parentsibling</code>, <code>test</code> and <code>sibling</code> plugins are present
 * in two versions and support the {@link ResourceUpgradeFacet}, while the rest of the plugins is
 * present only in single version.
 *                    
 * @author Lukas Krejci
 */
@Test(singleThreaded = true, invocationCount = 1)
public class ResourceUpgradeFailureHandlingTest extends AbstractResourceUpgradeHandlingTest {

    //test names
    private static final String SUCCESS_TEST = "SUCCESS_TEST";
    private static final String FAILURE_ON_LEAF_TEST = "FAILURE_ON_LEAF_TEST";
    private static final String FAILURE_ON_DEPENDENCIES_TEST = "FAILURE_ON_DEPENDENCIES_TEST";
    private static final String RESOURCES_REVERTED_TO_ORIGINAL_STATE_AFTER_FAILED_UPGRAGE_TEST = "RESOURCES_REVERTED_TO_ORIGINAL_STATE_AFTER_FAILED_UPGRAGE_TEST";
    
    //plugin names
    private static final String BASE_PLUGIN_NAME = "classpath:///resource-upgrade-test-plugin-multi-base-1.0.0.jar";
    private static final String PARENT_DEP_V1_PLUGIN_NAME = "classpath:///resource-upgrade-test-plugin-multi-parentdep-1.0.0.jar";
    private static final String PARENT_DEP_V2_PLUGIN_NAME = "classpath:///resource-upgrade-test-plugin-multi-parentdep-2.0.0.jar";
    private static final String PARENT_SIBLING_V1_PLUGIN_NAME = "classpath:///resource-upgrade-test-plugin-multi-parentsibling-1.0.0.jar";
    private static final String PARENT_SIBLING_V2_PLUGIN_NAME = "classpath:///resource-upgrade-test-plugin-multi-parentsibling-2.0.0.jar";
    private static final String ROOT_PLUGIN_NAME = "classpath:///resource-upgrade-test-plugin-multi-root-1.0.0.jar";
    private static final String SIBLING_V1_PLUGIN_NAME = "classpath:///resource-upgrade-test-plugin-multi-sibling-1.0.0.jar";
    private static final String SIBLING_V2_PLUGIN_NAME = "classpath:///resource-upgrade-test-plugin-multi-sibling-2.0.0.jar";
    private static final String TEST_V1_PLUGIN_NAME = "classpath:///resource-upgrade-test-plugin-multi-test-1.0.0.jar";
    private static final String TEST_V2_PLUGIN_NAME = "classpath:///resource-upgrade-test-plugin-multi-test-2.0.0.jar";

    private static final ResType TEST_TYPE = new ResType("TestResource", "test");
    private static final ResType SIBLING_TYPE = new ResType("TestResourceSibling", "test");
    private static final ResType PARENT_TYPE = new ResType("TestResourceParent", "test");
    private static final ResType PARENT_DEP_TYPE = new ResType("ParentDependency", "parentdep");
    private static final ResType PARENT_DEP_SIBLING_TYPE = new ResType("ParentDepSibling", "parentsibling");
    private static final ResType ROOT_TYPE = new ResType("Root", "root");

    private static List<ResType> ALL_TYPES = Arrays.asList(TEST_TYPE, SIBLING_TYPE, PARENT_TYPE, PARENT_DEP_TYPE,
        PARENT_DEP_SIBLING_TYPE, ROOT_TYPE);

    @Test
    @PluginContainerSetup(plugins = {TEST_V1_PLUGIN_NAME, PARENT_SIBLING_V1_PLUGIN_NAME, BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V1_PLUGIN_NAME, SIBLING_V1_PLUGIN_NAME},
        sharedGroup = SUCCESS_TEST, clearDataDir = true, numberOfInitialDiscoveries = 3)
    public void testSuccess_V1() throws Exception {
        final FakeServerInventory inv = new FakeServerInventory();
        setServerSideFake(SUCCESS_TEST, inv);
        
        context.checking(new Expectations() {
            {
                defineDefaultExpectations(inv, this);
            }
        });
        
        //let the discovery run in V1        
        startConfiguredPluginContainer();
    }
    
    @Test(dependsOnMethods = "testSuccess_V1")
    @PluginContainerSetup(plugins = {TEST_V2_PLUGIN_NAME, PARENT_SIBLING_V2_PLUGIN_NAME, BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V2_PLUGIN_NAME, SIBLING_V2_PLUGIN_NAME},
        sharedGroup = SUCCESS_TEST, clearInventoryDat = false, numberOfInitialDiscoveries = 3)
    public void testSuccess_V2() throws Exception {
        final FakeServerInventory inventory = (FakeServerInventory) getServerSideFake(SUCCESS_TEST);
        context.checking(new Expectations() {
            {
                defineDefaultExpectations(inventory, this);
            }
        });

        startConfiguredPluginContainer();
        
        Map<ResType, Set<Resource>> resources = getResourcesFromInventory(inventory, ALL_TYPES);
        
        checkPresenceOfResourceTypes(resources, ALL_TYPES);

        checkNumberOfResources(resources, ROOT_TYPE, 1);
        checkNumberOfResources(resources, PARENT_DEP_TYPE, 1);

        //check that the resources are upgraded
        checkResourcesUpgraded(resources.get(PARENT_DEP_SIBLING_TYPE), 1);
        checkResourcesUpgraded(resources.get(PARENT_TYPE), 3);
        checkResourcesUpgraded(resources.get(SIBLING_TYPE), 45);
        checkResourcesUpgraded(resources.get(TEST_TYPE), 45);
    }
    
    @Test
    @PluginContainerSetup(plugins = {TEST_V1_PLUGIN_NAME, PARENT_SIBLING_V1_PLUGIN_NAME, BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V1_PLUGIN_NAME, SIBLING_V1_PLUGIN_NAME}, 
        sharedGroup = FAILURE_ON_LEAF_TEST, clearDataDir = true, numberOfInitialDiscoveries = 3)
    public void testFailureOnLeaf_V1() throws Exception {
        final FakeServerInventory inventory = new FakeServerInventory();
        setServerSideFake(FAILURE_ON_LEAF_TEST, inventory);
        
        context.checking(new Expectations() {
            {
                defineDefaultExpectations(inventory, this);
            }
        });

        startConfiguredPluginContainer();

        //in here we set up the failures that are going to happen when
        //the v2 plugins are run

        Resource parent0 = findResourceWithOrdinal(PARENT_TYPE, 0);
        assertNotNull(parent0, "Failed to find the parent to setup the failures for.");
        Resource parent1 = findResourceWithOrdinal(PARENT_TYPE, 1);
        assertNotNull(parent1, "Failed to find the parent to setup the failures for.");

        addChildrenToFail(parent0, TEST_TYPE, 1, 2);
        addChildrenToFail(parent1, SIBLING_TYPE, 1);
    }

    @Test(dependsOnMethods = "testFailureOnLeaf_V1")
    @PluginContainerSetup(plugins = {TEST_V2_PLUGIN_NAME, PARENT_SIBLING_V2_PLUGIN_NAME, BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V2_PLUGIN_NAME, SIBLING_V2_PLUGIN_NAME}, 
        sharedGroup = FAILURE_ON_LEAF_TEST, clearInventoryDat = false, numberOfInitialDiscoveries = 3)
    public void testFailureOnLeaf_V2() throws Exception {
        final FakeServerInventory inventory = (FakeServerInventory) getServerSideFake(FAILURE_ON_LEAF_TEST);
        context.checking(new Expectations() {
            {
                defineDefaultExpectations(inventory, this);
            }
        });
        
        startConfiguredPluginContainer();
        
        Map<ResType, Set<Resource>> resources = getResourcesFromInventory(inventory, ALL_TYPES);
        
        checkPresenceOfResourceTypes(resources, ALL_TYPES);

        checkNumberOfResources(resources, ROOT_TYPE, 1);
        checkNumberOfResources(resources, PARENT_DEP_TYPE, 1);

        checkResourcesUpgraded(resources.get(PARENT_TYPE), 3);
        checkResourcesUpgraded(resources.get(PARENT_DEP_SIBLING_TYPE), 1);

        Resource parent0 = findResourceWithOrdinal(PARENT_TYPE, 0);
        Resource parent1 = findResourceWithOrdinal(PARENT_TYPE, 1);
        Resource parent2 = findResourceWithOrdinal(PARENT_TYPE, 2);
        
        Set<Resource> testsUnderParent0 = filterResources(parent0.getChildResources(), TEST_TYPE);
        Set<Resource> siblingsUnderParent0 = filterResources(parent0.getChildResources(), SIBLING_TYPE);
        Set<Resource> testsUnderParent1 = filterResources(parent1.getChildResources(), TEST_TYPE);
        Set<Resource> siblingsUnderParent1 = filterResources(parent1.getChildResources(), SIBLING_TYPE);
        Set<Resource> testsUnderParent2 = filterResources(parent2.getChildResources(), TEST_TYPE);
        Set<Resource> siblingsUnderParent2 = filterResources(parent2.getChildResources(), SIBLING_TYPE);
        
        //first check for the successful upgrades
        checkResourcesUpgraded(testsUnderParent2, 15);
        checkResourcesUpgraded(siblingsUnderParent2, 15);
        checkResourcesUpgraded(siblingsUnderParent0, 15);
        checkResourcesUpgraded(testsUnderParent1, 15);

        //there should be no newly discovered sibling resources of the failed ones
        assertEquals(testsUnderParent0.size(), 10);
        assertEquals(siblingsUnderParent1.size(), 10);

        //check that the failed resources have the error attached to them
        //we find the resource instance from the map provided to this method
        //because that map contains the resources as found on the server-side 
        //(i.e. they include error objects). 
        Resource failedTest1 = getEqualFrom(resources.get(TEST_TYPE),
            findResourceWithOrdinal(testsUnderParent0, 1));
        Resource failedTest2 = getEqualFrom(resources.get(TEST_TYPE),
            findResourceWithOrdinal(testsUnderParent0, 2));

        checkResourceFailedUpgrade(failedTest1);
        checkResourceFailedUpgrade(failedTest2);
        checkOthersUpgraded(testsUnderParent0, failedTest1, failedTest2);

        Resource failedSibling = getEqualFrom(resources.get(SIBLING_TYPE),
            findResourceWithOrdinal(siblingsUnderParent1, 1));

        checkResourceFailedUpgrade(failedSibling);
        checkOthersUpgraded(siblingsUnderParent1, failedSibling);        
    }
    
    @Test
    @PluginContainerSetup(plugins = {TEST_V1_PLUGIN_NAME, PARENT_SIBLING_V1_PLUGIN_NAME, BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V1_PLUGIN_NAME, SIBLING_V1_PLUGIN_NAME}, 
        sharedGroup = FAILURE_ON_DEPENDENCIES_TEST, clearDataDir = true, numberOfInitialDiscoveries = 3)
    public void testFailureOnDependencies_V1() throws Exception {
        final FakeServerInventory inventory = new FakeServerInventory();
        setServerSideFake(FAILURE_ON_DEPENDENCIES_TEST, inventory);
        
        context.checking(new Expectations() {
            {
                defineDefaultExpectations(inventory, this);
            }
        });
        
        startConfiguredPluginContainer();        
        
        //in here we set up the failures that are going to happen when
        //the v2 plugins are run

        Resource parent = findResourceWithOrdinal(PARENT_DEP_TYPE, 0);
        assertNotNull(parent, "Failed to find the parent to setup the failures for.");
        
        addChildrenToFail(parent, PARENT_TYPE, 0);
    }
    
    @Test(dependsOnMethods = "testFailureOnDependencies_V1")
    @PluginContainerSetup(plugins = {TEST_V2_PLUGIN_NAME, PARENT_SIBLING_V2_PLUGIN_NAME, BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V2_PLUGIN_NAME, SIBLING_V2_PLUGIN_NAME}, 
        sharedGroup = FAILURE_ON_DEPENDENCIES_TEST, clearInventoryDat = false, numberOfInitialDiscoveries = 3)
    public void testFailureOnDependencies_V2() throws Exception {
        final FakeServerInventory inventory = (FakeServerInventory) getServerSideFake(FAILURE_ON_DEPENDENCIES_TEST);
        context.checking(new Expectations() {
            {
                defineDefaultExpectations(inventory, this);
            }
        });
        
        startConfiguredPluginContainer();
        
        Map<ResType, Set<Resource>> resources = getResourcesFromInventory(inventory, ALL_TYPES);
        
        checkPresenceOfResourceTypes(resources, ALL_TYPES);

        checkNumberOfResources(resources, ROOT_TYPE, 1);
        checkNumberOfResources(resources, PARENT_DEP_TYPE, 1);
        checkResourcesUpgraded(resources.get(PARENT_DEP_SIBLING_TYPE), 1);

        //check that the failed resources have the error attached to them
        //we find the resource instance from the map provided to this method
        //because that map contains the resources as found on the server-side 
        //(i.e. they include error objects).
        Resource parent0 = getEqualFrom(resources.get(PARENT_TYPE), findResourceWithOrdinal(PARENT_TYPE, 0));
        Resource parent1 = getEqualFrom(resources.get(PARENT_TYPE), findResourceWithOrdinal(PARENT_TYPE, 1));

        checkResourceFailedUpgrade(parent0);
        checkOthersUpgraded(resources.get(PARENT_TYPE), parent0);

        //v2 plugin discovers 3 resources but because parent0 failed to upgrade,
        //the discovery shouldn't have occurred leaving us with the 2 already existing resources.
        checkNumberOfResources(resources, PARENT_TYPE, 2);
        
        //parent1 upgraded ok, so discovering its children should have executed.
        //this is v2, so we should find 15 of each.
        checkResourcesUpgraded(filterResources(parent1.getChildResources(), TEST_TYPE), 15);
        checkResourcesUpgraded(filterResources(parent1.getChildResources(), SIBLING_TYPE), 15);
                           
        //these shouldn't have been upgraded. in v1 we had 10 resources of TEST_TYPE
        //and 10 resources of SIBLING_TYPE and that's what we should be seeing
        //now.
        checkResourcesNotUpgraded(filterResources(parent0.getChildResources(), TEST_TYPE), 10);
        checkResourcesNotUpgraded(filterResources(parent0.getChildResources(), SIBLING_TYPE), 10);        
    }
    
    @Test
    @PluginContainerSetup(plugins = {TEST_V1_PLUGIN_NAME, PARENT_SIBLING_V1_PLUGIN_NAME, BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V1_PLUGIN_NAME, SIBLING_V1_PLUGIN_NAME}, 
        sharedGroup = RESOURCES_REVERTED_TO_ORIGINAL_STATE_AFTER_FAILED_UPGRAGE_TEST, clearDataDir = true, numberOfInitialDiscoveries = 3)
    public void testResourcesRevertedToOriginalStateAfterFailedUpgrade_V1() throws Exception {
        final FakeServerInventory inventory = new FakeServerInventory();
        setServerSideFake(RESOURCES_REVERTED_TO_ORIGINAL_STATE_AFTER_FAILED_UPGRAGE_TEST, inventory);
        
        context.checking(new Expectations() {
            {
                defineDefaultExpectations(inventory, this);
            }
        });
        
        startConfiguredPluginContainer();        
    }
    
    @Test(dependsOnMethods = "testResourcesRevertedToOriginalStateAfterFailedUpgrade_V1")
    @PluginContainerSetup(plugins = {TEST_V2_PLUGIN_NAME, PARENT_SIBLING_V2_PLUGIN_NAME, BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V2_PLUGIN_NAME, SIBLING_V2_PLUGIN_NAME}, 
        sharedGroup = RESOURCES_REVERTED_TO_ORIGINAL_STATE_AFTER_FAILED_UPGRAGE_TEST, clearInventoryDat = false, numberOfInitialDiscoveries = 3)
    public void testResourcesRevertedToOriginalStateAfterFailedUpgrade_V2() throws Exception {
        final FakeServerInventory inventory = (FakeServerInventory) getServerSideFake(RESOURCES_REVERTED_TO_ORIGINAL_STATE_AFTER_FAILED_UPGRAGE_TEST);
        context.checking(new Expectations() {
            {
                defineDefaultExpectations(inventory, this);
            }
        });

        inventory.setFailUpgrade(true);
        
        startConfiguredPluginContainer();
        
        Map<ResType, Set<Resource>> resources = getResourcesFromInventory(inventory, ALL_TYPES);
        
        checkPresenceOfResourceTypes(resources, ALL_TYPES);

        checkNumberOfResources(resources, ROOT_TYPE, 1);
        checkNumberOfResources(resources, PARENT_DEP_TYPE, 1);

        //check that the resources are not upgraded
        checkResourcesNotUpgraded(resources.get(PARENT_DEP_SIBLING_TYPE), 1);
        checkResourcesNotUpgraded(resources.get(PARENT_TYPE), 2);
        checkResourcesNotUpgraded(resources.get(SIBLING_TYPE), 20);
        checkResourcesNotUpgraded(resources.get(TEST_TYPE), 20);        
    }

    protected static void checkPresenceOfResourceTypes(Map<ResType, Set<Resource>> resources, Collection<ResType> expectedTypes) {
        for (ResType resType : expectedTypes) {
            assertNotNull(resources.get(resType), "Expecting some resources of type " + resType);
        }
    }

    protected static void checkNumberOfResources(Map<ResType, Set<Resource>> resources, ResType type, int count) {
        assertEquals(resources.get(type).size(), count, "Unexpected number of " + type + " discovered.");
    }

    protected static void checkOthersUpgraded(Set<Resource> resources, Resource... failedResource) {
        Set<Resource> others = new HashSet<Resource>(resources);
        others.removeAll(Arrays.asList(failedResource));
        checkResourcesUpgraded(others, others.size());
    }

    protected void addChildrenToFail(Resource parent, ResType childResType, int... childrenOrdinals) {
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        ResourceContainer parentContainer = inventoryManager.getResourceContainer(parent);
        BaseResourceComponentInterface parentComponent = (BaseResourceComponentInterface) parentContainer
            .getResourceComponent();

        Map<String, Set<Integer>> childrenToFail = new HashMap<String, Set<Integer>>();
        Set<Integer> ordinals = new HashSet<Integer>();
        for (int i = 0; i < childrenOrdinals.length; ++i) {
            ordinals.add(childrenOrdinals[i]);
        }

        childrenToFail.put(childResType.getResourceTypeName(), ordinals);

        Configuration newPluginConfig = parentComponent.createPluginConfigurationWithMarkedFailures(childrenToFail);

        try {
            int resourceId = parent.getId();
            inventoryManager.updatePluginConfiguration(resourceId, newPluginConfig);
        } catch (InvalidPluginConfigurationClientException e) {
            fail("Updating plugin configuration failed.", e);
        } catch (PluginContainerException e) {
            fail("Updating plugin configuration failed.", e);
        }
    }

    protected Set<Resource> filterResources(Set<Resource> resources, ResType resType) {
        Set<Resource> ret = new HashSet<Resource>(resources);

        Iterator<Resource> it = ret.iterator();

        while (it.hasNext()) {
            ResourceType resourceType = it.next().getResourceType();

            if (!resourceType.getName().equals(resType.getResourceTypeName())
                || !resourceType.getPlugin().equals(resType.getResourceTypePluginName())) {

                it.remove();
            }
        }

        return ret;
    }

    private Map<ResType, Set<Resource>> getResourcesFromInventory(FakeServerInventory inventory, Collection<ResType> types) {
        Map<ResType, Set<Resource>> resources = new HashMap<ResType, Set<Resource>>();

        for(ResType type : types) {
            ResourceType resType = PluginContainer.getInstance().getPluginManager().getMetadataManager()
            .getType(type.getResourceTypeName(), type.getResourceTypePluginName());

            Set<Resource> rs = inventory.findResourcesByType(resType);
            resources.put(type, rs);
        }
        
        return resources;
    }

    private static <T> T getEqualFrom(Collection<? extends T> collection, T object) {
        for (T other : collection) {
            if (object.equals(other)) {
                return other;
            }
        }

        return null;
    }
}
