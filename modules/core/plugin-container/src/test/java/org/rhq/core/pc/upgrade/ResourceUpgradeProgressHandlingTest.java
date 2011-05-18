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

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;

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

import org.rhq.core.domain.resource.Resource;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class ResourceUpgradeProgressHandlingTest extends ResourceUpgradeFailureHandlingTest {

    private static final String BASE_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-base-1.0.0.jar";
    private static final String PARENT_DEP_V1_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-parentdep-1.0.0.jar";
    private static final String PARENT_DEP_V2_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-parentdep-2.0.0.jar";
    private static final String ROOT_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-root-1.0.0.jar";
    private static final String UPGRADE_PROGRESS_PLUGIN_V1_FILENAME = "/resource-upgrade-test-plugin-progress-test-1.0.0.jar";
    private static final String UPGRADE_PROGRESS_PLUGIN_V2_FILENAME = "/resource-upgrade-test-plugin-progress-test-2.0.0.jar";
    private static final String UPGRADE_DUPLICATE_PLUGIN_V1_FILENAME = "/resource-upgrade-test-plugin-duplicate-test-1.0.0.jar";
    private static final String UPGRADE_DUPLICATE_PLUGIN_V2_FILENAME = "/resource-upgrade-test-plugin-duplicate-test-2.0.0.jar";
        
    private static final ResType TEST_TYPE = new ResType("TestResource", "test");
    private static final ResType PARENT_DEP_TYPE = new ResType("ParentDependency", "parentdep");
    private static final ResType ROOT_TYPE = new ResType("Root", "root");
        
    private static final HashMap<String, List<String>> DEPS;

    static {
        DEPS = new HashMap<String, List<String>>();

        DEPS.put(BASE_PLUGIN_NAME, Collections.<String> emptyList());
        DEPS.put(PARENT_DEP_V1_PLUGIN_NAME, Arrays.asList(BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME));
        DEPS.put(PARENT_DEP_V2_PLUGIN_NAME, DEPS.get(PARENT_DEP_V1_PLUGIN_NAME));
        DEPS.put(ROOT_PLUGIN_NAME, Arrays.asList(BASE_PLUGIN_NAME));
        DEPS.put(UPGRADE_PROGRESS_PLUGIN_V1_FILENAME, Arrays.asList(BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V1_PLUGIN_NAME));
        DEPS.put(UPGRADE_PROGRESS_PLUGIN_V2_FILENAME, Arrays.asList(BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V2_PLUGIN_NAME));        
        DEPS.put(UPGRADE_DUPLICATE_PLUGIN_V1_FILENAME, Arrays.asList(BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V1_PLUGIN_NAME));
        DEPS.put(UPGRADE_DUPLICATE_PLUGIN_V2_FILENAME, Arrays.asList(BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V2_PLUGIN_NAME));        
    }

    private Set<String> getAllDepsFor(String... plugins) {
        HashSet<String> deps = new HashSet<String>();
        for (String plugin : plugins) {
            deps.add(plugin);
            deps.addAll(DEPS.get(plugin));
        }

        return deps;
    }
    
    @Override
    protected Collection<String> getRequiredPlugins() {
        return Arrays.asList(BASE_PLUGIN_NAME, ROOT_PLUGIN_NAME, PARENT_DEP_V1_PLUGIN_NAME, PARENT_DEP_V2_PLUGIN_NAME,
            UPGRADE_PROGRESS_PLUGIN_V1_FILENAME, UPGRADE_PROGRESS_PLUGIN_V2_FILENAME, UPGRADE_DUPLICATE_PLUGIN_V1_FILENAME,
            UPGRADE_DUPLICATE_PLUGIN_V2_FILENAME);
    }

    @Test
    public void testParentResourceStartedUpgradedWhenChildResourceBeingUpgraded() throws Exception {
        setCurrentServerSideInventory(new FakeServerInventory());
                
        executeTestWithPlugins(getAllDepsFor(UPGRADE_PROGRESS_PLUGIN_V1_FILENAME),
            new AbstractTestPayload(true, Collections.<ResType> emptyList()) {
                public void test(Map<ResType, Set<Resource>> resourceUpgradeTestResources) {
                    //in here we set up the failures that are going to happen when
                    //the v2 plugins are run

                    Resource parent = findResourceWithOrdinal(PARENT_DEP_TYPE, 0);
                    assertNotNull(parent, "Failed to find the parent.");
                }

                public Expectations getExpectations(Mockery context) throws Exception {
                    return new Expectations() {
                        {
                            defineDefaultExpectations(this);
                        }
                    };
                }
            });
        
        //the upgrade progress plugin is set to check that the parent resource key
        //has been upgraded during its upgrade method, so we just need to check here
        //that everything got upgraded. If it was not, it'd mean that the the progress
        //plugin failed the upgrade because it didn't see its parent upgraded.
        executeTestWithPlugins(getAllDepsFor(UPGRADE_PROGRESS_PLUGIN_V2_FILENAME), 
            new AbstractTestPayload(false, Arrays.asList(TEST_TYPE, PARENT_DEP_TYPE)) {
                public void test(Map<ResType, Set<Resource>> resourceUpgradeTestResources) {
                    checkResourcesUpgraded(resourceUpgradeTestResources.get(PARENT_DEP_TYPE), 1);
                    checkResourcesUpgraded(resourceUpgradeTestResources.get(TEST_TYPE), 2);
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
    public void testDuplicitResourceKeysHandledCorrectly() throws Exception {
        setCurrentServerSideInventory(new FakeServerInventory());
        
        executeTestWithPlugins(getAllDepsFor(UPGRADE_DUPLICATE_PLUGIN_V1_FILENAME),
            new AbstractTestPayload(true, Arrays.asList(PARENT_DEP_TYPE, TEST_TYPE)) {
                
                public void test(Map<ResType, Set<Resource>> resourceUpgradeTestResources) {
                    //there's not much to check with the v1 plugins. let's just check all the 
                    //resources have been discovered
                    assertEquals(resourceUpgradeTestResources.get(PARENT_DEP_TYPE).size(), 1, "The V1 inventory should have 1 parent.");                                       
                    assertEquals(resourceUpgradeTestResources.get(TEST_TYPE).size(), 2, "The V1 inventory should have 2 test resources.");                                       
                }
                
                public Expectations getExpectations(Mockery context) throws Exception {
                    return new Expectations() {
                        {
                            defineDefaultExpectations(this);
                        }
                    };
                }
            });
        
        //now the V2 test resource is set to create 2 resources with the same resource keys.
        //the upgrade should therefore fail.
        executeTestWithPlugins(getAllDepsFor(UPGRADE_DUPLICATE_PLUGIN_V2_FILENAME), 
            new AbstractTestPayload(false, Arrays.asList(PARENT_DEP_TYPE, TEST_TYPE)) {
                public void test(Map<ResType, Set<Resource>> resourceUpgradeTestResources) {
                    checkResourcesUpgraded(resourceUpgradeTestResources.get(PARENT_DEP_TYPE), 1);
                    
                    checkResourcesNotUpgraded(resourceUpgradeTestResources.get(TEST_TYPE), 2);
                    
                    for(Resource r : resourceUpgradeTestResources.get(TEST_TYPE)) {
                        checkResourceFailedUpgrade(r);
                    }
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
}
