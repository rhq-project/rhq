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

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.transaction.Status;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.definition.constraint.Constraint;
import org.rhq.core.domain.configuration.definition.constraint.FloatRangeConstraint;
import org.rhq.core.domain.configuration.definition.constraint.IntegerRangeConstraint;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Tests the updating of persisted ResourceTypes' resource and plugin ConfigurationDefinitions, which is essentially
 * handled by {@link org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerBean}.
 *
 * @author Heiko Rupp
 */
public class UpdateConfigurationSubsystemTest extends UpdateSubsytemTestBase {

    private static final boolean ENABLED = true;

    @Override
    protected String getSubsystemDirectory() {
        return "configuration";
    }

    /**
     * Test updating of plugin configs.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLED)
    public void testUpdatePluginConfig() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            registerPlugin("update5-v1_0.xml", null);
            ResourceType platform1 = getResourceType("myPlatform5");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform1 = em.find(ResourceType.class, platform1.getId());

            Set<ResourceType> servers = platform1.getChildResourceTypes();
            assert servers.size() == 1 : "Expected to find 1 server in v1, but got " + servers.size();
            ResourceType server1 = servers.iterator().next();
            ConfigurationDefinition cDef = server1.getPluginConfigurationDefinition();
            assert cDef != null : "Expected to find a PluginConfigurationDefinition in v1";
            List<PropertyGroupDefinition> groups1 = cDef.getGroupDefinitions();
            assert groups1.size() == 3 : "Expected to find 3 groups in v1, but got " + groups1.size();
            int found = 0;
            for (PropertyGroupDefinition pgd : groups1) {
                if (containedIn(pgd.getName(), new String[] { "connection", "tuning", "control" })) {
                    found++;
                }

                if (pgd.getName().equals("tuning")) {
                    List<PropertyDefinition> pdl = cDef.getPropertiesInGroup("tuning");
                    for (PropertyDefinition pd : pdl) {
                        if (pd.getName().equals("rampDownTime")) {
                            assert pd.isRequired() == false : "rampDownTime is not required in 'tuning' in v1";
                            assert pd.isReadOnly() == true : "rampDownTime should be r/o in v1";
                            assert pd.isSummary() == false : "rampDownTime should not be summary in v1";
                        }
                    }
                    // TODO more checking per group
                }
            }

            assert found == 3 : "Expected to find 3 specially named control groups in v1, but only found " + found;
            // TODO check more stuff here
            getTransactionManager().rollback();

            System.out.println("-------- done with v1");

            /*
             * Now deploy version 2
             */
            registerPlugin("update5-v2_0.xml", null);
            ResourceType platform2 = getResourceType("myPlatform5");
            getTransactionManager().begin();
            em = getEntityManager();
            platform2 = em.find(ResourceType.class, platform2.getId());

            Set<ResourceType> servers2 = platform2.getChildResourceTypes();
            assert servers2.size() == 1 : "Expected to find 1 server in v2, but got " + servers2.size();
            ResourceType server2 = servers2.iterator().next();
            ConfigurationDefinition cDef2 = server2.getPluginConfigurationDefinition();
            assert cDef2 != null : "Expected to find a PluginConfigurationDefinition in v2";
            List<PropertyGroupDefinition> groups2 = cDef2.getGroupDefinitions();
            assert groups2.size() == 3 : "Expected to find 3 groups in v2, but got " + groups2.size();
            found = 0;
            for (PropertyGroupDefinition pgd : groups2) {
                if (pgd.getName().equals("fun")) {
                    found++;
                    List<PropertyDefinition> pdl = cDef2.getPropertiesInGroup("fun");
                    assert pdl.size() == 1 : "Expected to find 1 property for group 'fun' in v2, but got " + pdl.size();
                    PropertyDefinition pd = pdl.get(0);
                    assert pd.getName().equals("funRampUpTime") : "Expected to find the property 'rampUpTime', but got "
                        + pd.getName();
                } else if (pgd.getName().equals("tuning")) {
                    found++;
                    List<PropertyDefinition> pdl = cDef2.getPropertiesInGroup("tuning");
                    assert pdl.size() == 3 : "Expected 3 properties in group 'tuning' in v2, but got " + pdl.size();
                    for (PropertyDefinition pd : pdl) {
                        if (pd.getName().equals("rampDownTime")) {
                            assert pd.isRequired() == true : "rampDownTime is now required in 'tuning' in v2";
                            assert pd.isReadOnly() == false : "rampDownTime should be r/w in v2";
                            assert pd.isSummary() == true : "rampDownTime should  be summary in v2";
                        }
                    }
                } else if (pgd.getName().equals("control")) {
                    found++;
                } else {
                    //               assert true==false : "Unknown definition in v2: " + pgd.getName();
                    System.out.println("Unknown definition in v2: " + pgd.getName());
                }
                // TODO more checking per group
            }

            assert found == 3 : "Expected to find 3 specially named control groups in v2, but only found " + found;
            // TODO check resource-config as well
            getTransactionManager().rollback();

            System.out.println("-------- done with v2");

            // TODO check changing back
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }

            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testAddDeleteDefaultTemplate");
            }
        }
    }

    @Test(enabled = false)
    public void testBZ_573034() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            // register plugin
            String version1 = "1.0";
            String version2 = "2.0";

            registerPlugin("BZ_573034_v1.xml", version1);

            // create resource with plugin configuration
            ResourceType testServerResourceType = getResourceType("TestServer");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            testServerResourceType = em.find(ResourceType.class, testServerResourceType.getId());
            ConfigurationDefinition pluginConfigurationDef = testServerResourceType.getPluginConfigurationDefinition();
            PropertyDefinition testPropertyDef = pluginConfigurationDef.getPropertyDefinitionSimple("testProperty");
            Configuration pluginConfiguration = null;
            int pluginConfigurationId = 0;
            try {
                assertNotNull(
                    "Expected plugin configuration definition to have a property definition for 'testProperty'",
                    testPropertyDef);

                String resourceKey = testServerResourceType.getName() + System.currentTimeMillis();
                Resource testResource = new Resource(resourceKey, resourceKey, testServerResourceType);
                testResource.setUuid("" + new Random().nextInt());
                em.persist(testResource);
                em.flush();

                setUpAgent(em, testResource);

                testResource = em.find(Resource.class, testResource.getId());
                pluginConfigurationId = testResource.getPluginConfiguration().getId();

                pluginConfiguration = em.find(Configuration.class, pluginConfigurationId);
                PropertySimple testProperty = pluginConfiguration.getSimple("testProperty");

                assertNull(
                    "Did not expect to find a value for 'testProperty' since a value has not been supplied for it yet",
                    testProperty);

                getTransactionManager().commit();
            } catch (Exception e) {
                getTransactionManager().rollback();
                fail("testBZ_573034:" + e);
            }

            // register new version of plugin that specifies new plugin configuration property having a default value
            registerPlugin("BZ_573034_v2.xml", version2);

            // verify existing resource has its plugin configuration updated with new property having default value
            testServerResourceType = getResourceType("TestServer");
            getTransactionManager().begin();
            em = getEntityManager();
            testServerResourceType = em.find(ResourceType.class, testServerResourceType.getId());

            pluginConfigurationDef = testServerResourceType.getPluginConfigurationDefinition();
            PropertyDefinition testPropertyWithDefaultDef = pluginConfigurationDef
                .getPropertyDefinitionSimple("testPropertyWithDefault");

            assertNotNull(
                "Expected updated plugin configuration definition to define the property 'testPropertyWithDefault'",
                testPropertyWithDefaultDef);

            pluginConfiguration = em.find(Configuration.class, pluginConfigurationId);
            PropertySimple testPropertyWithDefault = pluginConfiguration.getSimple("testPropertyWithDefault");

            assertNotNull(
                "Expected to find the property 'testPropertyWithDefault' in the updated plugin configuration",
                testPropertyWithDefault);
            assertEquals("Expected default value to be set", "default", testPropertyWithDefault.getStringValue());

            getTransactionManager().rollback();

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }

            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testResourceConfiguration");
            }
        }
    }

    /**
     * Tests the behaviour wrt <resource-configuration> entries
     *
     * @throws Exception
     */
    @Test(enabled = ENABLED)
    public void testResourceConfiguration() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            registerPlugin("update5-v1_0.xml", "1");
            ResourceType platform1 = getResourceType("myPlatform5");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform1 = em.find(ResourceType.class, platform1.getId());

            Set<ResourceType> servers = platform1.getChildResourceTypes();
            assert servers.size() == 1 : "Expected to find 1 server in v1, but got " + servers.size();
            ResourceType server1 = servers.iterator().next();
            ConfigurationDefinition def1 = server1.getResourceConfigurationDefinition();
            List<PropertyDefinition> cpdl = def1.getNonGroupedProperties();

            assert cpdl.size() == 3 : "Did not find 3 properties in <resource-configuration> in v1 but " + cpdl.size();
            for (PropertyDefinition pd : cpdl) {
                if (pd.getName().equals("jnpPort")) {
                    assert pd instanceof PropertyDefinitionSimple : "jnpPort is no simple-property in v1";
                    PropertyDefinitionSimple pds = (PropertyDefinitionSimple) pd;

                    List<PropertyDefinitionEnumeration> pdel = pds.getEnumeratedValues();
                    assert pdel.size() == 3 : "jnpPort did not have 3 options in v1, but " + pdel.size();
                    int found = 0;
                    for (PropertyDefinitionEnumeration pde : pdel) {
                        if (containedIn(pde.getName(), new String[] { "option1", "option2", "option3" })) {
                            found++;
                        }
                    }

                    assert found == 3 : "Did not find the three expected options in v1";
                } else if (pd.getName().equals("secureJnpPort")) {
                    assert pd instanceof PropertyDefinitionSimple : "secureJnpPort is no simple-property in v1";
                    PropertyDefinitionSimple pds = (PropertyDefinitionSimple) pd;
                    assert pds.getType() == PropertySimpleType.INTEGER : "Type of secureJnpPort is not integer in v1";
                    Set<Constraint> constraints = pds.getConstraints();
                    assert constraints.size() == 4 : "Did not find 4 constraints for secureJnpPort in v1, but "
                        + constraints.size();
                }
            }
            getTransactionManager().rollback();

            /*
             * Now check the changed plugin
             */
            registerPlugin("update5-v2_0.xml", "2");
            ResourceType platform2 = getResourceType("myPlatform5");
            getTransactionManager().begin();
            em = getEntityManager();
            platform2 = em.find(ResourceType.class, platform2.getId());

            Set<ResourceType> servers2 = platform2.getChildResourceTypes();
            assert servers2.size() == 1 : "Expected to find 1 server in v2, but got " + servers2.size();
            ResourceType server2 = servers2.iterator().next();
            ConfigurationDefinition def2 = server2.getResourceConfigurationDefinition();
            def2 = getEntityManager().find(ConfigurationDefinition.class, def2.getId());
            List<PropertyDefinition> cpdl2 = def2.getNonGroupedProperties();
            assert cpdl2.size() == 3 : "Did not find 3 properties in <resource-configuration> in v2 but "
                + cpdl2.size();
            int found = 0;
            for (PropertyDefinition pd : cpdl2) {
                if (containedIn(pd.getName(), new String[] { "jnpPort", "secureJnpPort", "memorySize" })) {
                    found++;
                }
            }

            assert found == 3 : "Did not find the 3 specific properties in v2";
            for (PropertyDefinition pd : cpdl2) {
                if (pd.getName().equals("jnpPort")) {
                    if (pd instanceof PropertyDefinitionSimple) {
                        PropertyDefinitionSimple pds = (PropertyDefinitionSimple) pd;

                        List<PropertyDefinitionEnumeration> pdel = pds.getEnumeratedValues();

                        /*
                         * TODO there is a strange effect where pdel has also "null" elements in it, that were not
                         * present at the time the new enumeration has been computed in the ResourceMetadataManager.
                         * That is why I am here first removing the nulls and then do the assertion
                         */
                        for (int i = pdel.size() - 1; i >= 0; i--) {
                            if (pdel.get(i) == null) {
                                pdel.remove(i);
                            }
                        }

                        assert pdel.size() == 4 : "jnpPort did not have 4 options in v2, but " + pdel.size();
                        found = 0;
                        for (PropertyDefinitionEnumeration pde : pdel) {
                            if (containedIn(pde.getName(), new String[] { "option2", "option3", "newOption4",
                                "newOption5" })) {
                                found++;
                            }

                            if (pde.getName().equals("option3")) {
                                assert pde.getValue().equals("changed") : "Value for option 'option3' did not change in v2";
                            }
                        }

                        assert found == 4 : "Did not find the four expected options in v2 ";
                    }
                } else if (pd.getName().equals("secureJnpPort")) {
                    assert pd instanceof PropertyDefinitionSimple : "secureJnpPort is no simple-property in v2";
                    PropertyDefinitionSimple pds = (PropertyDefinitionSimple) pd;
                    Set<Constraint> constraints = pds.getConstraints();
                    assert constraints.size() == 3 : "Did not find 3 constraints for secureJnpPort in v2, but "
                        + constraints.size();
                }
            }
            getTransactionManager().rollback();

            /*
             * And now back to the first version
             */

            registerPlugin("update5-v1_0.xml", "3");
            platform1 = getResourceType("myPlatform5");
            getTransactionManager().begin();
            em = getEntityManager();
            platform1 = em.find(ResourceType.class, platform1.getId());

            servers = platform1.getChildResourceTypes();
            assert servers.size() == 1 : "Expected to find 1 server in v1, but got " + servers.size();
            ResourceType server3 = servers.iterator().next();
            ConfigurationDefinition def3 = server3.getResourceConfigurationDefinition();
            List<PropertyDefinition> cpdl3 = def3.getNonGroupedProperties();

            assert cpdl3.size() == 3 : "Did not find 3 properties in <resource-configuration> in v3 again but "
                + cpdl3.size();
            for (PropertyDefinition pd : cpdl3) {
                if (pd.getName().equals("jnpPort")) {
                    if (pd instanceof PropertyDefinitionSimple) {
                        PropertyDefinitionSimple pds = (PropertyDefinitionSimple) pd;

                        List<PropertyDefinitionEnumeration> pdel = pds.getEnumeratedValues();

                        /*
                         * TODO there is a strange effect where pdel has also "null" elements in it, that were not
                         * present at the time the new enumeration has been computed in the ResourceMetadataManager.
                         * That is why I am here first removing the nulls and then do the assertion
                         */
                        for (int i = pdel.size() - 1; i >= 0; i--) {
                            if (pdel.get(i) == null) {
                                pdel.remove(i);
                            }
                        }

                        assert pdel.size() == 3 : "jnpPort did not have 3 options in v3, but " + pdel.size();
                        found = 0;
                        for (PropertyDefinitionEnumeration pde : pdel) {
                            if (containedIn(pde.getName(), new String[] { "option1", "option2", "option3" })) {
                                found++;
                            }
                        }

                        assert found == 3 : "Did not find the three expected options in v3";
                    }
                }
            }
            getTransactionManager().rollback();

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }

            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testResourceConfiguration");
            }
        }
    }

    /**
     * Test for storing of back links from Constraint to PropertyDefinitionSimple. See JBNADM-1587
     *
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    @Test(enabled = ENABLED)
    public void testConstraint() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            registerPlugin("constraint.xml");
            ResourceType platform = getResourceType("constraintPlatform");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            ConfigurationDefinition config = platform.getResourceConfigurationDefinition();
            PropertyDefinitionSimple propDef = config.getPropertyDefinitionSimple("secureJnpPort");
            Set<Constraint> constraints = propDef.getConstraints();
            assert constraints.size() == 4 : "Expected to get 4 constraints, but got " + constraints.size();

            assert propDef.getDefaultValue().equals("1234");
            getTransactionManager().rollback();

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }

            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName() + ".testConstraint");
            }
        }
    }

    /**
     * Test for storing of back links from Constraint to PropertyDefinitionSimple. See JBNADM-1587
     *
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    @Test(enabled = ENABLED)
    public void testConstraint2() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            registerPlugin("constraint.xml");
            ResourceType platform = getResourceType("constraintPlatform");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            ConfigurationDefinition config = platform.getResourceConfigurationDefinition();
            Map<String, PropertyDefinition> propDefMap = config.getPropertyDefinitions();
            PropertyDefinitionSimple propDef = (PropertyDefinitionSimple) propDefMap.get("secureJnpPort");
            Set<Constraint> constraints = propDef.getConstraints();
            assert constraints.size() == 4 : "Expected to get 4 constraints, but got " + constraints.size();

            assert propDef.getDefaultValue().equals("1234");
            getTransactionManager().rollback();

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }

            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName() + ".testConstraint2");
            }
        }
    }

    /**
     * Test for setting min/max in contraints. See JBNADM-1596 See JBNADM-1597
     *
     * @throws Exception
     */
    @Test(enabled = ENABLED)
    public void testConstraintMinMax() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            registerPlugin("constraintMinMax.xml");
            ResourceType platform = getResourceType("constraintPlatform");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            ConfigurationDefinition config = platform.getResourceConfigurationDefinition();
            PropertyDefinitionSimple propDef = config.getPropertyDefinitionSimple("secureJnpPort");
            Set<Constraint> constraints = propDef.getConstraints();
            assert constraints.size() == 2 : "Expected to get 2 constraints, but got " + constraints.size();
            for (Constraint co : constraints) {
                if (co instanceof IntegerRangeConstraint) {
                    IntegerRangeConstraint irc = (IntegerRangeConstraint) co;
                    assert irc.getMinimum() == 5; // TODO change when JBNADM-1597 is being worked on
                    assert irc.getMaximum() == 0;
                } else if (co instanceof FloatRangeConstraint) {
                    FloatRangeConstraint frc = (FloatRangeConstraint) co;
                    assert frc != null;
                    assert frc.getMaximum() == 0.0; // TODO change when JBNADM-1597 is being worked on
                    assert frc.getMinimum() == 5.0;
                }
            }
            getTransactionManager().rollback();

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }

            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName() + ".testConstraint2");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testListOfLists() throws Exception {
        try {
            // extra block for variable scoping purposes
            registerPlugin("propertyList-v1.xml");
            ResourceType platform = getResourceType("myPlatform6");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());
            ConfigurationDefinition cd = platform.getResourceConfigurationDefinition();
            Map<String, PropertyDefinition> propDefs = cd.getPropertyDefinitions();
            assert propDefs.size() == 4 : "Expected to see 4 <list-property>s in v1, but got " + propDefs.size();
            for (PropertyDefinition def : propDefs.values()) {
                assert def instanceof PropertyDefinitionList : "PropertyDefinition " + def.getName()
                    + " is no list-property in v1";
                PropertyDefinitionList pdl = (PropertyDefinitionList) def;
                PropertyDefinition member = pdl.getMemberDefinition();

                if (pdl.getName().equals("myList1")) {
                    assert pdl.getDescription().equals("Just a simple list");
                    assert member instanceof PropertyDefinitionSimple : "Expected the member of myList1 to be a simple property in v1";
                    PropertyDefinitionSimple pds = (PropertyDefinitionSimple) member;
                    assert pds.getName().equals("foo");
                } else if (pdl.getName().equals("myList2")) {
                    assert member instanceof PropertyDefinitionList : "Expected the member of myList2 to be a list property in v1";
                } else if (pdl.getName().equals("myList3")) {
                    assert member instanceof PropertyDefinitionSimple : "Expected the member of myList3 to be a simple property in v1";
                    PropertyDefinitionSimple pds = (PropertyDefinitionSimple) member;
                    assert pds.getName().equals("baz");
                } else if (pdl.getName().equals("rec1")) {
                    assert member instanceof PropertyDefinitionList : "Expected the member of rc1 to be a list property in v1";
                    PropertyDefinitionList pdl2 = (PropertyDefinitionList) member;

                    // TODO check min/max for the lists on the way. Commented out. See JBNADM-1595
                    assert pdl2.getName().equals("rec2");

                    //                  assert pdl2.getMin()==2 : "Expected rec2:min to be 2, but it was " + pdl2.getMin();
                    //                  assert pdl2.getMax()==20;
                    pdl2 = (PropertyDefinitionList) pdl2.getMemberDefinition();
                    assert pdl2.getName().equals("rec3");

                    //                  assert pdl2.getMin()==3;
                    //                  assert pdl2.getMax()==30;
                    pdl2 = (PropertyDefinitionList) pdl2.getMemberDefinition();
                    assert pdl2.getName().equals("rec4");

                    //                  assert pdl2.getMin()==4;
                    //                  assert pdl2.getMax()==40;
                    assert pdl2.getMemberDefinition() instanceof PropertyDefinitionSimple;
                    PropertyDefinitionSimple pds = (PropertyDefinitionSimple) pdl2.getMemberDefinition();
                    assert pds.getName().equals("rec5");
                    assert pds.getDescription().equals("Deeply nested");
                    List<PropertyDefinitionEnumeration> options = pds.getEnumeratedValues();
                    assert options.size() == 4;
                    int found = 0;
                    String[] optionVals = new String[] { "a", "b", "c", "d" };
                    for (PropertyDefinitionEnumeration option : options) {
                        if (containedIn(option.getValue(), optionVals)) {
                            found++;
                        }
                    }

                    assert found == 4;
                    Set<Constraint> constraints = pds.getConstraints();
                    assert constraints.size() == 1;
                } else {
                    assert true == false : "Unknown list-definition in v1: " + pdl.getName();
                }
            }
            getTransactionManager().rollback();

            /*
             * Deploy v2 of the plugin
             */
            registerPlugin("propertyList-v2.xml");
            platform = getResourceType("myPlatform6");
            getTransactionManager().begin();
            em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            cd = platform.getResourceConfigurationDefinition();
            propDefs = cd.getPropertyDefinitions();
            assert propDefs.size() == 4 : "Expected to see 4 <list-property>s in v2, but got " + propDefs.size();
            for (PropertyDefinition def : propDefs.values()) {
                assert def instanceof PropertyDefinitionList : "PropertyDefinition " + def.getName()
                    + " is no list-property in v2";
                PropertyDefinitionList pdl = (PropertyDefinitionList) def;
                PropertyDefinition member = pdl.getMemberDefinition();

                if (pdl.getName().equals("myList2")) {
                    assert member instanceof PropertyDefinitionList : "Expected the member of myList2 to be a list property in v2";
                } else if (pdl.getName().equals("myList3")) {
                    assert member instanceof PropertyDefinitionList : "Expected the member of myList3 to be a list property in v2";
                    PropertyDefinitionList pds = (PropertyDefinitionList) member;
                    assert pds.getName().equals("baz");
                    assert pds.getDescription().equals("myList3:baz");
                    assert pds.getMemberDefinition() instanceof PropertyDefinitionSimple : "Expected the member of list3:baz to be a simple property in v2";
                } else if (pdl.getName().equals("myList4")) {
                    assert pdl.getDescription().equals("Just a simple list");
                    assert member instanceof PropertyDefinitionSimple : "Expected the member of myList4 to be a simple property in v2";
                    PropertyDefinitionSimple pds = (PropertyDefinitionSimple) member;
                    assert pds.getName().equals("foo");
                } else if (pdl.getName().equals("rec1")) {
                    assert member instanceof PropertyDefinitionList : "Expected the member of rec1 to be a list property in v2";
                    PropertyDefinitionList pdl2 = (PropertyDefinitionList) member;
                    assert pdl2.getName().equals("rec2");

                    /*
                     * PropertyDefinitionList.getMin()/getMax() are commented out. See JBNADM1595
                     */

                    //                assert pdl2.getMin()==12 : "Expected rec2:min to be 12, but it was " + pdl2.getMin();
                    //                assert pdl2.getMax()==200;
                    pdl2 = (PropertyDefinitionList) pdl2.getMemberDefinition();
                    assert pdl2.getName().equals("rec3+");

                    //                assert pdl2.getMin()==13;
                    //                assert pdl2.getMax()==300;
                    pdl2 = (PropertyDefinitionList) pdl2.getMemberDefinition();
                    assert pdl2.getName().equals("rec4");

                    //                assert pdl2.getMin()==14;
                    //                assert pdl2.getMax()==400;
                    assert pdl2.getMemberDefinition() instanceof PropertyDefinitionSimple;
                    PropertyDefinitionSimple pds = (PropertyDefinitionSimple) pdl2.getMemberDefinition();
                    assert pds.getName().equals("rec5");
                    assert pds.getDescription().equals("Nested deeply");
                    List<PropertyDefinitionEnumeration> options = pds.getEnumeratedValues();
                    assert options.size() == 5;
                    int found = 0;
                    String[] optionVals = new String[] { "b", "c", "d", "x", "z" };
                    for (PropertyDefinitionEnumeration option : options) {
                        if (containedIn(option.getValue(), optionVals)) {
                            found++;
                        }
                    }

                    assert found == optionVals.length;
                    Set<Constraint> constraints = pds.getConstraints();
                    assert constraints.size() == 2;
                    for (Constraint constraint : constraints) {
                        if (constraint instanceof IntegerRangeConstraint) {
                            IntegerRangeConstraint irc = (IntegerRangeConstraint) constraint;
                            assert irc != null : "Integer-constraint was null, but should not be";
                            // See JBNADM-1596/97
                            assert irc.getDetails().equals("-2#10");
                            // TODO (ips, 3/31/10): The below is a workaround for IntegerRangeConstraint.onLoad() not being called by Hibernate.
                            irc.setDetails(irc.getDetails());
                            assert irc.getMaximum() == 10;
                            assert irc.getMinimum() == -2;
                        } else if (constraint instanceof FloatRangeConstraint) {
                            FloatRangeConstraint frc = (FloatRangeConstraint) constraint;
                            assert frc != null : "Float-constraint was null, but should not be";
                            // See JBNADM-1596/97
                            assert frc.getDetails().equals("10.0#5.0");
                            // TODO (ips, 3/31/10): The below is a workaround for FloatRangeConstraint.onLoad() not being called by Hibernate.
                            frc.setDetails(frc.getDetails());
                            assert frc.getMinimum() == 10; // TODO change when JBNADM-1597 is being worked on
                            assert frc.getMaximum() == 5;

                        } else {
                            assert true == false : "Unknown constraint type encountered";
                        }
                    }
                } else {
                    assert true == false : "Unknown list-definition in v2: " + pdl.getName();
                }
            }
            getTransactionManager().rollback();

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }

            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName() + ".testListOfLists");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testListPropertyMinMax() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            registerPlugin("propertyList-simple.xml");
            ResourceType platform = getResourceType("myPlatform");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            ConfigurationDefinition cd = platform.getResourceConfigurationDefinition();
            Map<String, PropertyDefinition> properties = cd.getPropertyDefinitions();
            PropertyDefinitionList a = (PropertyDefinitionList) properties.get("a");
            assert a.getDescription().equals("Yada !") : "Expected the description to be 'Yada !', but it was "
                + a.getDescription();

            // The next two are marked as @Transient in PropertyDefinitonList
            // See JBNADM-1595
            //            assert a.getMax()==6 : "Expected the max to be 6 but it was " + a.getMax();
            //            assert a.getMin()==4 : "Expected the min to be 4 but it was " + a.getMin();

            getTransactionManager().rollback();

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }

            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testListPropertyMinMax");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testMapProperty() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            { // extra block for variable scoping purposes
                registerPlugin("propertyMap-v1.xml");
                ResourceType platform = getResourceType("myPlatform7");
                getTransactionManager().begin();
                EntityManager em = getEntityManager();
                platform = em.find(ResourceType.class, platform.getId());

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

                assert found == 5 : "Did not find the 5 desired maps in v1";

                getTransactionManager().rollback();
            }

            System.out.println("Done with v1");

            /*
             * Now deploy v2
             */
            { // extra block for variable scoping purposes
                registerPlugin("propertyMap-v2.xml");
                ResourceType platform = getResourceType("myPlatform7");
                getTransactionManager().begin();
                EntityManager em = getEntityManager();
                platform = em.find(ResourceType.class, platform.getId());

                ConfigurationDefinition cd = platform.getResourceConfigurationDefinition();
                Map<String, PropertyDefinition> propDefs = cd.getPropertyDefinitions();
                assert propDefs.size() == 5 : "Expected to find 5 properties in v2, but got " + propDefs.size();

                int found = 0;
                for (PropertyDefinition def : propDefs.values()) {
                    if (containedIn(def.getName(), new String[] { "map1", "map2", "map3", "map4", "map5" })) {
                        found++;
                    }

                    if (def.getName().equals("map1")) {
                        assert def instanceof PropertyDefinitionSimple : "Map 1 should be a simple-property in v2";
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

                assert found == 5 : "Did not find the 5 desired properties in v2, instead found " + found;

                getTransactionManager().rollback();
            }
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }

            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName() + ".testMapProperty");
            }
        }
    }

    /**
     * This test checks what happens if a property of one type checks its type into another one upon redeploy.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLED)
    public void testChangePropertyType() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            { // extra block for variable scoping purposes
                registerPlugin("propertyChanging-v1.xml");
                ResourceType platform = getResourceType("myPlatform7");
                getTransactionManager().begin();
                EntityManager em = getEntityManager();
                platform = em.find(ResourceType.class, platform.getId());

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
                        assert true == false : "Unknown definition : " + def.getName() + " in v1";
                    }
                }
                getTransactionManager().rollback();
            }

            /*
             * Now deploy v2
             */

            { // extra block for variable scoping purposes
                registerPlugin("propertyChanging-v2.xml");
                ResourceType platform = getResourceType("myPlatform7");
                getTransactionManager().begin();
                EntityManager em = getEntityManager();
                platform = em.find(ResourceType.class, platform.getId());

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
                        assert true == false : "Unknown definition : " + def.getName() + " in v2";
                    }
                }
                getTransactionManager().rollback();
            }

            /*
             * Now deploy v1 again
             */{ // extra block for variable scoping purposes
                registerPlugin("propertyChanging-v1.xml", "3.0"); // this is our 3rd version, but reuse v1
                ResourceType platform = getResourceType("myPlatform7");
                getTransactionManager().begin();
                EntityManager em = getEntityManager();
                platform = em.find(ResourceType.class, platform.getId());

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
                        assert def instanceof PropertyDefinitionSimple : "Expected a simple-property, but it was "
                            + def.getClass().getCanonicalName();
                    } else if (def.getName().equals("six")) {
                        assert def instanceof PropertyDefinitionSimple : "Expected a simple-property, but it was "
                            + def.getClass().getCanonicalName();
                    }
                }
                getTransactionManager().rollback();
            }
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }

            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testChangePropertyType");
            }
        }

    }

    /*==================================== Resource Config Tests ======================================*/

    @Test(enabled = ENABLED)
    public void testGroupDeleted() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            registerPlugin("groupDeleted-v1.xml");
            System.out.println("==> Done with v1");
            registerPlugin("groupDeleted-v2.xml");
            System.out.println("==> Done with v2");
            registerPlugin("groupDeleted-v1.xml", "3.0");
            System.out.println("==> Done with v1");
        } finally {
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName() + ".testGroupDeleted");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testGroupPropDeleted() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            registerPlugin("groupPropDeleted-v1.xml");
            System.out.println("==> Done with v1");
            registerPlugin("groupPropDeleted-v2.xml");
            System.out.println("==> Done with v2");
            registerPlugin("groupPropDeleted-v1.xml", "3.0");
            System.out.println("==> Done with v1");
        } finally {
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testGroupPropDeleted");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testGroupPropDeletedExt() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            registerPlugin("groupPropDeleted-v3.xml");
            System.out.println("==> Done with v1");
            registerPlugin("groupPropDeleted-v4.xml");
            System.out.println("==> Done with v2");
            registerPlugin("groupPropDeleted-v3.xml", "5.0");
            System.out.println("==> Done with v1");
        } finally {
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testGroupPropDeletedExt");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testGroupPropMoved() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            registerPlugin("groupPropMoved-v1.xml");
            System.out.println("==> Done with v1");
            registerPlugin("groupPropMoved-v2.xml");
            System.out.println("==> Done with v2");
            registerPlugin("groupPropMoved-v1.xml", "3.0");
            System.out.println("==> Done with v1");
        } finally {
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName() + ".testGroupPropMoved");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testUpdateDefaultTemplate() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            registerPlugin("updateDefaultTemplate1.xml");
            ResourceType platform = getResourceType("myPlatform7");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            ConfigurationDefinition cd = platform.getResourceConfigurationDefinition();
            ConfigurationTemplate defaultTemplate = cd.getDefaultTemplate();
            assert defaultTemplate != null;
            Configuration config = defaultTemplate.getConfiguration();
            PropertySimple ps = config.getSimple("six");
            assert "foo".equals(ps.getStringValue()) : "Expected 'foo', but got " + ps.getStringValue();
            getTransactionManager().rollback();

            registerPlugin("updateDefaultTemplate2.xml");
            platform = getResourceType("myPlatform7");
            getTransactionManager().begin();
            em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            cd = platform.getResourceConfigurationDefinition();
            defaultTemplate = cd.getDefaultTemplate();
            assert defaultTemplate != null;
            config = defaultTemplate.getConfiguration();
            ps = config.getSimple("six");
            assert "bar".equals(ps.getStringValue()) : "Expected 'bar', but got " + ps.getStringValue();
            getTransactionManager().rollback();

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }

            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testUpdateDefaultTemplate");
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testAddDeleteTemplate() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            ResourceType platform;
            ConfigurationTemplate defaultTemplate;
            ConfigurationDefinition cd;
            Map<String, ConfigurationTemplate> templateMap;
            ConfigurationTemplate template;
            PropertySimple ps;

            registerPlugin("addDeleteTemplate1.xml");
            platform = getResourceType("myPlatform7");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            cd = platform.getResourceConfigurationDefinition();
            defaultTemplate = cd.getDefaultTemplate();
            assert defaultTemplate != null;
            templateMap = cd.getTemplates();
            assert templateMap.size() == 1 : "Expected only the 1 default template but got " + templateMap.size();
            getTransactionManager().rollback();

            System.out.println("Done with v1");

            registerPlugin("addDeleteTemplate2.xml");
            platform = getResourceType("myPlatform7");
            getTransactionManager().begin();
            em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());
            cd = platform.getResourceConfigurationDefinition();
            defaultTemplate = cd.getDefaultTemplate();
            templateMap = cd.getTemplates();
            assert defaultTemplate != null;
            assert templateMap.size() == 2 : "Expected 2 templates but got " + templateMap;
            template = templateMap.get("additional");
            assert template != null;
            ps = template.getConfiguration().getSimple("second_one");
            assert ps.getStringValue().equals("Bart") : "Expected 'Bart', but got " + ps.getStringValue();
            getTransactionManager().rollback();

            System.out.println("Done with v2");

            registerPlugin("addDeleteTemplate3.xml");
            platform = getResourceType("myPlatform7");
            getTransactionManager().begin();
            em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());
            cd = platform.getResourceConfigurationDefinition();
            defaultTemplate = cd.getDefaultTemplate();
            templateMap = cd.getTemplates();
            assert defaultTemplate != null;
            assert templateMap.size() == 2 : "Expected 2 templates but got " + templateMap;
            template = templateMap.get("additional");
            assert template != null;
            ps = template.getConfiguration().getSimple("second_one");
            assert ps.getStringValue().equals("Bart Simpson") : "Expected 'Bart Simpson', but got "
                + ps.getStringValue();
            getTransactionManager().rollback();

            System.out.println("Done with v3");

            registerPlugin("addDeleteTemplate1.xml", "4.0"); // this is our 4th version, but reuse v1
            platform = getResourceType("myPlatform7");
            getTransactionManager().begin();
            em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());
            cd = platform.getResourceConfigurationDefinition();
            defaultTemplate = cd.getDefaultTemplate();
            templateMap = cd.getTemplates();
            assert defaultTemplate != null;
            assert templateMap.size() == 1 : "Expected only the 1 default template but got " + templateMap;
            getTransactionManager().rollback();

            System.out.println("Done with v1(2)");

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }

            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testAddDeleteDefaultTemplate");
            }
        }
    }
}
