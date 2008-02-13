/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.resource.metadata.test;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Test the handling on Plugin updates / hotdeployments etc.
 *
 * @author Heiko W. Rupp
 */
public class PluginHandling2Test extends TestBase {
    private static final String TEST_METADATA_UPDATE2_V1_0_XML = "./test/metadata/update2-v1_0.xml";

    @BeforeSuite
    @Override
    protected void init() {
        super.init();
    }

    //   @Test  TODO further work on this
    public void testMoveResoureType() throws Exception {
        System.out.println("testUpdatePlugin2 --- start");
        getTransactionManager().begin();
        try {
            registerPlugin(TEST_METADATA_UPDATE2_V1_0_XML);
            ResourceType platform1 = getResourceType("myPlatform");
            assert platform1 != null : "I did not find myPlatform";
            Set<MeasurementDefinition> defs = platform1.getMetricDefinitions();
            assert defs.size() == 1 : "I was expecting 1 definition at platform level in v1";
            assert DisplayType.DETAIL == defs.iterator().next().getDisplayType() : "Display type should be DETAIL in v1";

            // one child service in v1
            Set<ResourceType> platformChildren = platform1.getChildResourceTypes();
            assert platformChildren.size() == 1 : "Expected 1 direct child service of platform in v1";
            ResourceType service1 = platformChildren.iterator().next();
            assert service1.getName().equals("service1") : "Expected 'service1' as name of direct platform child in v1";
            assert service1.getMetricDefinitions().size() == 1 : "Expected 1 metric for 'service1' in v1";
            Set<ResourceType> nestedService = service1.getChildResourceTypes();
            assert nestedService.size() == 1 : "Expected 1 nested service of 'service1' in v1";
            Set<MeasurementDefinition> nestedDefs = nestedService.iterator().next().getMetricDefinitions();
            assert nestedDefs.size() == 1 : "Expected 1 definition within 'nestedService' in v1";
            MeasurementDefinition defThree = nestedDefs.iterator().next();
            int definitionId = defThree.getId(); // get the id of the definition "Three" and save it for later use

            getEntityManager().flush();

            System.out.println("testUpdatePlugin2 -- done with the first plugin version");

            /*
             * The nested service got pulled out and put under platform
             */
            registerPlugin("./test/metadata/update2-v2_0.xml");
            ResourceType platform2 = getResourceType("myPlatform");
            assert platform2 != null : "I did not find myPlatform";
            Set<MeasurementDefinition> defs2 = platform2.getMetricDefinitions();
            assert defs2.size() == 1 : "I was expecting 1 definition at platform level in v2";
            assert DisplayType.SUMMARY == defs2.iterator().next().getDisplayType() : "Display type should be SUMMARY in v2";

            // two children in v2
            Set<ResourceType> platformChildren2 = platform2.getChildResourceTypes();
            assert platformChildren2.size() == 2 : "Expected 2 direct child services of platform in v2";
            Iterator<ResourceType> iter = platformChildren2.iterator();
            while (iter.hasNext()) {
                ResourceType type = iter.next();
                String typeName = type.getName();
                assert type.getMetricDefinitions().size() == 1 : "Expected one definition for " + typeName + " in v2";
                if (typeName.equals("nestedOne")) // The moved one
                {
                    Set<MeasurementDefinition> defs3 = type.getMetricDefinitions();
                    MeasurementDefinition three = defs3.iterator().next();
                    assert three.getDisplayName().equals("Three") : "Expected the nestedOne to have a metric withDisplayName Three in v2, but it was "
                        + three.getDisplayName();
                    assert three.getDisplayType() == DisplayType.SUMMARY : "Expected three to be SUMMARY in v2";

                    /*
                     * TODO check here if the Definition is the one from above which got moved. this should be the case.
                     * Else we would loose all measurment schedules (and disociate them from measurement data. But the
                     * later is a different story. We probably should cascade that anyway.
                     */
                    System.out.println("Expected the id of 'Three' to be " + definitionId + " but it was "
                        + three.getId() + " in v2");
                    assert three.getId() == definitionId : "Expected the id of 'Three' to be " + definitionId
                        + " but it was " + three.getId() + " in v2";
                } else if (typeName.equals("service1")) {
                    // check that the nested service is gone
                    Set<ResourceType> childrenOfService = type.getChildResourceTypes();
                    assert childrenOfService.size() == 0 : "No children of 'service1' expected in v2, but found: "
                        + childrenOfService.size();
                } else {
                    assert true == false : "We found an unknown type with name " + typeName;
                }
            }
        }

        // TODO now that we're done here, apply v1 again to also test the other direction
        finally {
            getTransactionManager().rollback();
        }

        System.out.println("testUpdatePlugin2 --- end");
    }

    @Test
    public void testMapProperty() throws Exception {
        getTransactionManager().begin();
        try {
            { // extra block for variable scoping purposes
                registerPlugin("./test/metadata/propertyMap-v1.xml");
                ResourceType platform = getResourceType("myPlatform7");
                ConfigurationDefinition cd = platform.getResourceConfigurationDefinition();
                Map<String, PropertyDefinition> propDefs = cd.getPropertyDefinitions();
                assert propDefs.size() == 5 : "Expected to find 5 properties in v1, but got " + propDefs.size();
                int found = 0;
                for (PropertyDefinition def : propDefs.values()) {
                    if (containedIn(def.getName(), new String[] { "map1", "map2", "map3", "map4", "map5" })) {
                        found++;
                    }

                    assert def instanceof PropertyDefinitionMap : "Not all properties are maps in v1";

                    if (def.getName().equals("map4")) {
                        PropertyDefinitionMap map = (PropertyDefinitionMap) def;
                        Map<String, PropertyDefinition> children = map.getPropertyDefinitions();
                        assert children.size() == 1 : "Map4 should have 1 child";
                        children = map.getPropertyDefinitions();
                        map = (PropertyDefinitionMap) children.get("map4:2");
                        assert map != null : "Child map4:2 not found";
                        children = map.getPropertyDefinitions();
                        map = (PropertyDefinitionMap) children.get("map4:2:3");
                        assert map != null : "Child map4:2:3 not found";
                        children = map.getPropertyDefinitions();
                        PropertyDefinitionSimple simple = (PropertyDefinitionSimple) children.get("simple");
                        assert simple != null : "Child simple not found";
                    }

                    if (def.getName().equals("map5")) {
                        PropertyDefinitionMap map = (PropertyDefinitionMap) def;
                        Map<String, PropertyDefinition> children = map.getPropertyDefinitions();
                        assert children.size() == 1 : "Map4 should have 1 child";
                        children = map.getPropertyDefinitions();
                        PropertyDefinitionSimple simple = (PropertyDefinitionSimple) children.get("hugo");
                        assert simple.getDescription().equals("foo");
                    }
                }

                assert found == 5 : "Did not find the 5 desird maps in v1";
            }

            System.out.println("Done with v1");
            getEntityManager().flush();

            /*
             * Now deploy v2
             */
            { // extra block for variable scoping purposes
                registerPlugin("./test/metadata/propertyMap-v2.xml");
                ResourceType platform = getResourceType("myPlatform7");
                ConfigurationDefinition cd = platform.getResourceConfigurationDefinition();
                Map<String, PropertyDefinition> propDefs = cd.getPropertyDefinitions();
                assert propDefs.size() == 5 : "Expected to find 5 properties in v2, but got " + propDefs.size();

                int found = 0;
                for (PropertyDefinition def : propDefs.values()) {
                    if (containedIn(def.getName(), new String[] { "map1", "map2", "map3", "map4", "map5" })) {
                        found++;
                    }

                    if (def.getName().equals("map1")) {
                        assert def instanceof PropertyDefinitionSimple : "Map 1 should be a simle-property in v2";
                    } else {
                        assert def instanceof PropertyDefinitionMap : "Not all properties are maps in v2";
                    }

                    if (def.getName().equals("map3")) {
                        assert def.isRequired() == false : "Map 3 should not be false in v2";
                    }

                    if (def.getName().equals("map4")) {
                        PropertyDefinitionMap map = (PropertyDefinitionMap) def;
                        Map<String, PropertyDefinition> children = map.getPropertyDefinitions();
                        assert children.size() == 1 : "Map4 should have 1 child, but has " + children.size();
                        children = map.getPropertyDefinitions();
                        map = (PropertyDefinitionMap) children.get("map4:2+");
                        assert map != null : "Child map4:2 not found";
                        children = map.getPropertyDefinitions();
                        assert children.size() == 1 : "Map4:2 should have 1 child, but has " + children.size();
                        map = (PropertyDefinitionMap) children.get("map4:2:3");
                        assert map != null : "Child map4:2:3 not found";
                        children = map.getPropertyDefinitions();
                        assert children.size() == 2 : "Map4:2:3 should have 1 child, but has " + children.size();
                        PropertyDefinitionList list = (PropertyDefinitionList) children.get("list");
                        assert list != null : "Child list not found";
                        PropertyDefinitionSimple simple = (PropertyDefinitionSimple) children.get("simple2");
                        assert simple != null : "Child simple2 not found";
                    }

                    if (def.getName().equals("map5")) {
                        PropertyDefinitionMap map = (PropertyDefinitionMap) def;
                        Map<String, PropertyDefinition> children = map.getPropertyDefinitions();
                        assert children.size() == 1 : "Map5 should have 1 child";
                        children = map.getPropertyDefinitions();
                        PropertyDefinitionSimple simple = (PropertyDefinitionSimple) children.get("hugo");
                        assert simple.getDescription().equals("bar") : "Map5:hugo should have 'bar' in v2";
                    }
                }

                assert found == 5 : "Did not find the 5 desired properties in v2";
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * This test checks what happens if a property of one type checks its type into another one upon redeploy.
     *
     * @throws Exception
     */
    @Test
    public void testChangePropertyType() throws Exception {
        getTransactionManager().begin();
        try {
            { // extra block for variable scoping purposes
                registerPlugin("./test/metadata/propertyChanging-v1.xml");
                ResourceType platform = getResourceType("myPlatform7");
                ConfigurationDefinition cd = platform.getResourceConfigurationDefinition();
                Map<String, PropertyDefinition> propDefs = cd.getPropertyDefinitions();
                for (PropertyDefinition def : propDefs.values()) {
                    if (def.getName().equals("one")) {
                        assert def instanceof PropertyDefinitionMap;
                    } else if (def.getName().equals("two")) {
                        assert def instanceof PropertyDefinitionMap;
                    } else if (def.getName().equals("three")) {
                        assert def instanceof PropertyDefinitionList;
                    } else if (def.getName().equals("four")) {
                        assert def instanceof PropertyDefinitionList;
                    } else if (def.getName().equals("five")) {
                        assert def instanceof PropertyDefinitionSimple;
                    } else if (def.getName().equals("six")) {
                        assert def instanceof PropertyDefinitionSimple;
                    } else {
                        assert true == false : "Unknwon definition : " + def.getName() + " in v1";
                    }
                }
            }

            getEntityManager().flush();
            /*
             * Now deploy v2
             */

            { // extra block for variable scoping purposes
                registerPlugin("./test/metadata/propertyChanging-v2.xml");
                ResourceType platform = getResourceType("myPlatform7");
                ConfigurationDefinition cd = platform.getResourceConfigurationDefinition();
                Map<String, PropertyDefinition> propDefs = cd.getPropertyDefinitions();
                for (PropertyDefinition def : propDefs.values()) {
                    if (def.getName().equals("one")) {
                        assert def instanceof PropertyDefinitionList : "Expected a list-property, but it was "
                            + def.getClass().getCanonicalName();
                    } else if (def.getName().equals("two")) {
                        assert def instanceof PropertyDefinitionSimple : "Expected a simple-property, but it was "
                            + def.getClass().getCanonicalName();
                    } else if (def.getName().equals("three")) {
                        assert def instanceof PropertyDefinitionMap : "Expected a map-property, but it was "
                            + def.getClass().getCanonicalName();
                    } else if (def.getName().equals("four")) {
                        assert def instanceof PropertyDefinitionSimple : "Expected a simple-property, but it was "
                            + def.getClass().getCanonicalName();
                    } else if (def.getName().equals("five")) {
                        assert def instanceof PropertyDefinitionMap : "Expected a map-property, but it was "
                            + def.getClass().getCanonicalName();
                    } else if (def.getName().equals("six")) {
                        assert def instanceof PropertyDefinitionList : "Expected a list-property, but it was "
                            + def.getClass().getCanonicalName();
                    } else {
                        assert true == false : "Unknwon definition : " + def.getName() + " in v2";
                    }
                }
            }

            getEntityManager().flush();

            /*
             * Now deploy v1 again
             */{ // extra block for variable scoping purposes
                registerPlugin("./test/metadata/propertyChanging-v1.xml");
                ResourceType platform = getResourceType("myPlatform7");
                ConfigurationDefinition cd = platform.getResourceConfigurationDefinition();
                Map<String, PropertyDefinition> propDefs = cd.getPropertyDefinitions();
                for (PropertyDefinition def : propDefs.values()) {
                    if (def.getName().equals("one")) {
                        assert def instanceof PropertyDefinitionMap : "Expected a map-property, but it was "
                            + def.getClass().getCanonicalName();
                    } else if (def.getName().equals("two")) {
                        assert def instanceof PropertyDefinitionMap : "Expected a map-property, but it was "
                            + def.getClass().getCanonicalName();
                    } else if (def.getName().equals("three")) {
                        assert def instanceof PropertyDefinitionList : "Expected a list-property, but it was "
                            + def.getClass().getCanonicalName();
                    } else if (def.getName().equals("four")) {
                        assert def instanceof PropertyDefinitionList : "Expected a list-property, but it was "
                            + def.getClass().getCanonicalName();
                    } else if (def.getName().equals("five")) {
                        assert def instanceof PropertyDefinitionSimple : "Expected a simle-property, but it was "
                            + def.getClass().getCanonicalName();
                    } else if (def.getName().equals("six")) {
                        assert def instanceof PropertyDefinitionSimple : "Expected a simple-property, but it was "
                            + def.getClass().getCanonicalName();
                    }
                }
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Test if the full deletion of MeasurementDefinitions works JBNADM-1639
     *
     * @throws Exception
     */
    @Test
    public void testDeleteMeasurementDefinition() throws Exception {
        getTransactionManager().begin();
        try {
            { // extra block for variable scoping purposes
                registerPlugin("./test/metadata/measurementDeletion-v1_0.xml");
                ResourceType server = getResourceType("testServer1");
                Set<MeasurementDefinition> def = server.getMetricDefinitions();
                assert def.size() == 4;
            }

            { // extra block for variable scoping purposes
                registerPlugin("./test/metadata/measurementDeletion-v2_0.xml");
                ResourceType server = getResourceType("testServer1");
                Set<MeasurementDefinition> def = server.getMetricDefinitions();
                assert def.size() == 0;
            }

            { // extra block for variable scoping purposes
                registerPlugin("./test/metadata/measurementDeletion-v1_0.xml");
                ResourceType server = getResourceType("testServer1");
                Set<MeasurementDefinition> def = server.getMetricDefinitions();
                assert def.size() == 4;
            }
        } finally {
            getTransactionManager().rollback();
        }
    }
}