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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.clientapi.server.discovery.InvalidInventoryReportException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.inventory.ResourceContainer.ResourceComponentState;
import org.rhq.core.pc.upgrade.plugins.multi.base.BaseResourceComponentInterface;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;

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
@Test(sequential = true, invocationCount = 1)
public class ResourceUpgradeFailureHandlingTest extends ResourceUpgradeTestBase {

    //plugin names
    private static final String BASE_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-base-1.0.0.jar";
    private static final String PARENT_DEP_V1_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-parentdep-1.0.0.jar";
    private static final String PARENT_DEP_V2_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-parentdep-2.0.0.jar";
    private static final String PARENT_SIBLING_V1_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-parentsibling-1.0.0.jar";
    private static final String PARENT_SIBLING_V2_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-parentsibling-2.0.0.jar";
    private static final String ROOT_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-root-1.0.0.jar";
    private static final String SIBLING_V1_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-sibling-1.0.0.jar";
    private static final String SIBLING_V2_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-sibling-2.0.0.jar";
    private static final String TEST_V1_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-test-1.0.0.jar";
    private static final String TEST_V2_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-test-2.0.0.jar";

    private static final String UPGRADED_RESOURCE_KEY_PREFIX = "UPGRADED";

    private static final HashMap<String, List<String>> DEPS;

    static {
        DEPS = new HashMap<String, List<String>>();

        DEPS.put(BASE_PLUGIN_NAME, Collections.<String> emptyList());
        DEPS.put(PARENT_DEP_V1_PLUGIN_NAME, Arrays.asList(BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME));
        DEPS.put(PARENT_DEP_V2_PLUGIN_NAME, DEPS.get(PARENT_DEP_V1_PLUGIN_NAME));
        DEPS.put(PARENT_SIBLING_V1_PLUGIN_NAME, Arrays.asList(BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME));
        DEPS.put(PARENT_SIBLING_V2_PLUGIN_NAME, DEPS.get(PARENT_SIBLING_V1_PLUGIN_NAME));
        DEPS.put(ROOT_PLUGIN_NAME, Arrays.asList(BASE_PLUGIN_NAME));
        DEPS.put(SIBLING_V1_PLUGIN_NAME, Arrays.asList(BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V1_PLUGIN_NAME));
        DEPS.put(SIBLING_V2_PLUGIN_NAME, Arrays.asList(BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V2_PLUGIN_NAME));
        DEPS.put(TEST_V1_PLUGIN_NAME,
            Arrays.asList(BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V1_PLUGIN_NAME, SIBLING_V1_PLUGIN_NAME));
        DEPS.put(TEST_V2_PLUGIN_NAME,
            Arrays.asList(BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V1_PLUGIN_NAME, SIBLING_V2_PLUGIN_NAME));
    }

    private static final ResType TEST_TYPE = new ResType("TestResource", "test");
    private static final ResType SIBLING_TYPE = new ResType("TestResourceSibling", "test");
    private static final ResType PARENT_TYPE = new ResType("TestResourceParent", "test");
    private static final ResType PARENT_DEP_TYPE = new ResType("ParentDependency", "parentdep");
    private static final ResType PARENT_DEP_SIBLING_TYPE = new ResType("ParentDepSibling", "parentsibling");
    private static final ResType ROOT_TYPE = new ResType("Root", "root");

    private static List<ResType> ALL_TYPES = Arrays.asList(TEST_TYPE, SIBLING_TYPE, PARENT_TYPE, PARENT_DEP_TYPE,
        PARENT_DEP_SIBLING_TYPE, ROOT_TYPE);

    protected Collection<String> getRequiredPlugins() {
        return Arrays.asList(BASE_PLUGIN_NAME, PARENT_DEP_V1_PLUGIN_NAME, PARENT_DEP_V2_PLUGIN_NAME,
            PARENT_SIBLING_V1_PLUGIN_NAME, PARENT_SIBLING_V2_PLUGIN_NAME, ROOT_PLUGIN_NAME, SIBLING_V1_PLUGIN_NAME,
            SIBLING_V2_PLUGIN_NAME, TEST_V1_PLUGIN_NAME, TEST_V2_PLUGIN_NAME);
    }

    @Test
    public void testSuccess() throws Exception {
        setCurrentServerSideInventory(new FakeServerInventory());

        //let it all run in v1
        executeTestWithPlugins(getAllDepsFor(TEST_V1_PLUGIN_NAME, PARENT_SIBLING_V1_PLUGIN_NAME),
            new AbstractTestPayload(true, Collections.<ResType> emptyList()) {
                public void test(Map<ResType, Set<Resource>> resourceUpgradeTestResources) {
                }

                public Expectations getExpectations(Mockery context) throws Exception {
                    return new Expectations() {
                        {
                            defineDefaultExpectations(this);
                        }
                    };
                }
            });

        //now let's run with v2 plugins and check the layout of the inventory
        executeTestWithPlugins(getAllDepsFor(TEST_V2_PLUGIN_NAME, PARENT_SIBLING_V2_PLUGIN_NAME),
            new AbstractTestPayload(false, ALL_TYPES) {

                public void test(Map<ResType, Set<Resource>> resources) {
                    checkPresenceOfResourceTypes(resources, getExpectedResourceTypes());

                    checkNumberOfResources(resources, ROOT_TYPE, 1);
                    checkNumberOfResources(resources, PARENT_DEP_TYPE, 1);

                    //check that the resources are upgraded
                    checkResourcesUpgraded(resources.get(PARENT_DEP_SIBLING_TYPE), 1);
                    checkResourcesUpgraded(resources.get(PARENT_TYPE), 2);
                    checkResourcesUpgraded(resources.get(SIBLING_TYPE), 30);
                    checkResourcesUpgraded(resources.get(TEST_TYPE), 30);
                }

                public Expectations getExpectations(Mockery context) throws Exception {
                    return new Expectations() {
                        {
                            defineDefaultExpectations(this);
                        }
                    };
                }
            });
    }

    @Test
    public void testFailureOnLeaf() throws Exception {
        setCurrentServerSideInventory(new FakeServerInventory());

        executeTestWithPlugins(getAllDepsFor(TEST_V1_PLUGIN_NAME, PARENT_SIBLING_V1_PLUGIN_NAME),
            new AbstractTestPayload(true, Collections.<ResType> emptyList()) {
                public void test(Map<ResType, Set<Resource>> resourceUpgradeTestResources) {
                    //in here we set up the failures that are going to happen when
                    //the v2 plugins are run

                    Resource parent0 = findResourceWithOrdinal(PARENT_TYPE, 0);
                    assertNotNull(parent0, "Failed to find the parent to setup the failures for.");
                    Resource parent1 = findResourceWithOrdinal(PARENT_TYPE, 1);
                    assertNotNull(parent1, "Failed to find the parent to setup the failures for.");

                    addChildrenToFail(parent0, TEST_TYPE, 1, 2);
                    addChildrenToFail(parent1, SIBLING_TYPE, 1);
                }

                public Expectations getExpectations(Mockery context) throws Exception {
                    return new Expectations() {
                        {
                            defineDefaultExpectations(this);
                        }
                    };
                }
            });

        executeTestWithPlugins(getAllDepsFor(TEST_V2_PLUGIN_NAME, PARENT_SIBLING_V2_PLUGIN_NAME),
            new AbstractTestPayload(false, ALL_TYPES) {
                public void test(Map<ResType, Set<Resource>> resources) {
                    checkPresenceOfResourceTypes(resources, getExpectedResourceTypes());

                    checkNumberOfResources(resources, ROOT_TYPE, 1);
                    checkNumberOfResources(resources, PARENT_DEP_TYPE, 1);
                    checkNumberOfResources(resources, PARENT_TYPE, 2);

                    checkResourcesUpgraded(resources.get(PARENT_TYPE), 2);
                    checkResourcesUpgraded(resources.get(PARENT_DEP_SIBLING_TYPE), 1);

                    Resource parent0 = findResourceWithOrdinal(PARENT_TYPE, 0);
                    Resource parent1 = findResourceWithOrdinal(PARENT_TYPE, 1);

                    Set<Resource> siblingsUnderParent0 = filterResources(parent0.getChildResources(), SIBLING_TYPE);
                    Set<Resource> testsUnderParent0 = filterResources(parent0.getChildResources(), TEST_TYPE);
                    Set<Resource> siblingsUnderParent1 = filterResources(parent1.getChildResources(), SIBLING_TYPE);
                    Set<Resource> testsUnderParent1 = filterResources(parent1.getChildResources(), TEST_TYPE);

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

                public Expectations getExpectations(Mockery context) throws Exception {
                    return new Expectations() {
                        {
                            defineDefaultExpectations(this);
                        }
                    };
                }
            });
    }

    @Test
    public void testFailureOnDependencies() {
        //TODO implement
        //check that stuff works if there is an upgrade failure on some of the resources
        //in the plugin some "in the middle" of the plugin dep graph
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void defineDefaultExpectations(Expectations expectations) {
        super.defineDefaultExpectations(expectations);
        try {
            expectations.allowing(getCurrentDiscoveryServerService()).mergeInventoryReport(
                expectations.with(Expectations.any(InventoryReport.class)));
            expectations.will(getCurrentServerSideInventory().mergeInventoryReport(InventoryStatus.COMMITTED));

            expectations.allowing(getCurrentDiscoveryServerService()).upgradeResources(
                expectations.with(Expectations.any(Set.class)));
            expectations.will(getCurrentServerSideInventory().upgradeResources());

            expectations.allowing(getCurrentDiscoveryServerService()).postProcessNewlyCommittedResources(
                expectations.with(Expectations.any(Set.class)));

        } catch (InvalidInventoryReportException e) {
            //this is not going to happen because we're mocking the invocation
        }
    }

    private Set<String> getAllDepsFor(String... plugins) {
        HashSet<String> deps = new HashSet<String>();
        for (String plugin : plugins) {
            deps.add(plugin);
            deps.addAll(DEPS.get(plugin));
        }

        return deps;
    }

    private static void checkPresenceOfResourceTypes(Map<ResType, Set<Resource>> resources, Set<ResType> expectedTypes) {
        for (ResType resType : expectedTypes) {
            assertNotNull(resources.get(resType), "Expecting some resources of type " + resType);
        }
    }

    private static void checkNumberOfResources(Map<ResType, Set<Resource>> resources, ResType type, int count) {
        assertEquals(resources.get(type).size(), count, "Unexpected number of " + type + " discovered.");
    }

    private static void checkResourcesUpgraded(Set<Resource> resources, int expectedSize) {
        assertEquals(resources.size(), expectedSize, "The set of resources has unexpected size.");
        for (Resource res : resources) {
            assertTrue(res.getResourceKey().startsWith(UPGRADED_RESOURCE_KEY_PREFIX), "Resource " + res
                + " doesn't seem to be upgraded even though it should.");

            ResourceContainer rc = PluginContainer.getInstance().getInventoryManager().getResourceContainer(res);

            assertEquals(rc.getResourceComponentState(), ResourceComponentState.STARTED,
                "A resource that successfully upgraded should be started.");
        }
    }

    private static void checkResourceFailedUpgrade(Resource resource) {
        assertFalse(resource.getResourceKey().startsWith(UPGRADED_RESOURCE_KEY_PREFIX), "Resource " + resource
            + " seems to be upgraded even though it shouldn't.");
        assertTrue(resource.getResourceErrors(ResourceErrorType.UPGRADE).size() == 1,
            "The failed resource should have an error associated with it.");

        ResourceContainer rc = PluginContainer.getInstance().getInventoryManager().getResourceContainer(resource);

        assertEquals(rc.getResourceComponentState(), ResourceComponentState.STOPPED,
            "A resource that failed to upgrade should be stopped.");
    }

    private static void checkOthersUpgraded(Set<Resource> resources, Resource... failedResource) {
        Set<Resource> others = new HashSet<Resource>(resources);
        others.removeAll(Arrays.asList(failedResource));
        checkResourcesUpgraded(others, others.size());
    }

    private void addChildrenToFail(Resource parent, ResType childResType, int... childrenOrdinals) {
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

    private Resource findResourceWithOrdinal(ResType resType, int ordinal) {
        ResourceType resourceType = PluginContainer.getInstance().getPluginManager().getMetadataManager()
            .getType(resType.getResourceTypeName(), resType.getResourceTypePluginName());

        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        Set<Resource> resources = inventoryManager.getResourcesWithType(resourceType);

        return findResourceWithOrdinal(resources, ordinal);
    }

    private Resource findResourceWithOrdinal(Set<Resource> resources, int ordinal) {
        for (Resource r : resources) {
            Configuration pluginConfig = r.getPluginConfiguration();
            String ordinalString = pluginConfig.getSimpleValue("ordinal", null);

            if (ordinalString != null && Integer.parseInt(ordinalString) == ordinal) {
                return r;
            }
        }

        return null;
    }

    private Set<Resource> filterResources(Set<Resource> resources, ResType resType) {
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

    private static <T> T getEqualFrom(Collection<? extends T> collection, T object) {
        for (T other : collection) {
            if (object.equals(other)) {
                return other;
            }
        }

        return null;
    }
}
