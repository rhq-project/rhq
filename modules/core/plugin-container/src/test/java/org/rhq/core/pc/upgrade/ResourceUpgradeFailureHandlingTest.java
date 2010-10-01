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
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.discovery.InvalidInventoryReportException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
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
    private static final ResType PARENT_SIBLING_TYPE = new ResType("ParentDepSibling", "parentsibling");
    private static final ResType ROOT_TYPE = new ResType("Root", "root");

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
        executeTestWithPlugins(
            getAllDepsFor(TEST_V2_PLUGIN_NAME, PARENT_SIBLING_V2_PLUGIN_NAME),
            new AbstractTestPayload(false, Arrays.asList(TEST_TYPE, SIBLING_TYPE, PARENT_TYPE, PARENT_DEP_TYPE,
                PARENT_SIBLING_TYPE, ROOT_TYPE)) {

                public void test(Map<ResType, Set<Resource>> resources) {
                    checkPresenceOfResourceTypes(resources, getExpectedResourceTypes());

                    checkNumberOfResources(resources, ROOT_TYPE, 1);
                    checkNumberOfResources(resources, PARENT_DEP_TYPE, 1);
                    checkNumberOfResources(resources, PARENT_SIBLING_TYPE, 1);
                    checkNumberOfResources(resources, PARENT_TYPE, 2);
                    checkNumberOfResources(resources, SIBLING_TYPE, 30);
                    checkNumberOfResources(resources, TEST_TYPE, 30);
                    
                    //check that the resources are upgraded
                    checkResourcesUpgraded(resources.get(PARENT_SIBLING_TYPE));
                    checkResourcesUpgraded(resources.get(PARENT_TYPE));
                    checkResourcesUpgraded(resources.get(SIBLING_TYPE));
                    checkResourcesUpgraded(resources.get(TEST_TYPE));
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
    public void testFailureOnLeaf() {
        //TODO implement
        //check that the system behaves correctly if there is an upgrade failure
        //at the leaf node of the plugin dep graph
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
            expectations.allowing(getCurrentDiscoveryServerService())
                .mergeInventoryReport(expectations.with(Expectations.any(InventoryReport.class)));
            expectations.will(getCurrentServerSideInventory().mergeInventoryReport(InventoryStatus.COMMITTED));

            expectations.allowing(getCurrentDiscoveryServerService()).upgradeResources(expectations.with(Expectations.any(Set.class)));
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
    
    private static void checkResourcesUpgraded(Set<Resource> resources) {
        for(Resource res : resources) {
            assertTrue(res.getResourceKey().startsWith(UPGRADED_RESOURCE_KEY_PREFIX), "Resource " + res + " doesn't seem to be upgraded even though it should.");
        }
    }
}
