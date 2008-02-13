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
import java.util.Set;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.definition.constraint.Constraint;
import org.rhq.core.domain.configuration.definition.constraint.FloatRangeConstraint;
import org.rhq.core.domain.configuration.definition.constraint.IntegerRangeConstraint;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Test the handling on Plugin updates / hotdeployments etc.
 *
 * @author Heiko W. Rupp
 */
public class PluginHandlingTest extends TestBase {
    @BeforeSuite
    @Override
    protected void init() {
        super.init();
    }

    /**
     * Simple test for the update of a plugin where a server has some metrics that get in the second version of the
     * plugin added / changed / removed
     *
     * @throws Exception
     */
    @Test
    public void testUpdateMeasurementDefinitions() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/update-v1_0.xml");
            ResourceType server1 = getResourceType("testServer1");
            Set<MeasurementDefinition> definitions1 = server1.getMetricDefinitions();
            assert definitions1.size() == 4 : "There should be 4 metrics for v1";
            for (MeasurementDefinition def : definitions1) {
                if (def.getDisplayName().equals("Three")) {
                    assert def.getDisplayType() == DisplayType.DETAIL : "DisplayType for Three should be Detail in v1";
                }

                if (def.getDisplayName().equals("Five")) {
                    // this is a trick(y) one, as we do not want to honor updates
                    // of the default interval when a plugin was already deployed once and
                    // we do a redeploy
                    assert def.getDefaultInterval() == 10000 : "DefaultInterval should be 10000 for Five in v1";
                }
            }

            // flush everything to disk
            getEntityManager().flush();

            // now hot deploy a new version of that plugin
            registerPlugin("./test/metadata/update-v2_0.xml");
            ResourceType server2 = getResourceType("testServer1");
            Set<MeasurementDefinition> definitions2 = server2.getMetricDefinitions();
            assert definitions2.size() == 4 : "There should be four metrics in v2";
            boolean foundFour = false;
            for (MeasurementDefinition def : definitions2) {
                assert !(def.getDisplayName().equals("One")) : "One should be gone in v2";
                if (def.getDisplayName().equals("Three")) {
                    assert def.getDisplayType() == DisplayType.SUMMARY : "DisplayType for Three should be Summary in v2";
                }

                if (def.getDisplayName().equals("Four")) {
                    foundFour = true;
                }

                if (def.getDisplayName().equals("Five")) {
                    // this is a trick(y) one, as we do not want to honor updates
                    // of the default interval when a plugin was already deployed once and
                    // we do a redeploy
                    assert def.getDefaultInterval() == 10000 : "DefaultInterval should still be 10000 for Five in v2";
                }
            }

            assert foundFour == true : "Four should be there in v2, but wasn't";

            // flush everything to disk
            getEntityManager().flush();

            // Now try the other way round
            registerPlugin("./test/metadata/update-v1_0.xml");
            ResourceType server3 = getResourceType("testServer1");
            Set<MeasurementDefinition> definitions3 = server3.getMetricDefinitions();
            assert definitions3.size() == 4 : "There should be 4 metrics for v3";
            for (MeasurementDefinition def : definitions3) {
                if (def.getDisplayName().equals("Three")) {
                    assert def.getDisplayType() == DisplayType.DETAIL : "DisplayType for Three should be Detail in v3";
                }

                if (def.getDisplayName().equals("Five")) {
                    // this is a trick(y) one, as we do not want to honor updates
                    // of the default interval when a plugin was already deployed once and
                    // we do a redeploy
                    assert def.getDefaultInterval() == 10000 : "DefaultInterval should be 10000 for Five in v3";
                }
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Check updates of artifacts and operations
     *
     * @throws Exception
     */
    @Test
    public void testOperationAndArtifactUpdates() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/update3-v1_0.xml");
            ResourceType platform1 = getResourceType("myPlatform3");
            Set<PackageType> packageTypes = platform1.getPackageTypes();
            assert packageTypes.size() == 3 : "Did not find the three expected package types in v1";
            Set<OperationDefinition> ops = platform1.getOperationDefinitions();
            assert ops.size() == 3 : "Did not find three expected operations in v1";

            getEntityManager().flush();

            /*
             * Now deploy the changed version of the plugin
             */
            registerPlugin("./test/metadata/update3-v2_0.xml");

            ResourceType platform2 = getResourceType("myPlatform3");
            Set<PackageType> packageTypes2 = platform2.getPackageTypes();
            assert packageTypes2.size() == 3 : "Did not find the expected three package types in v2";
            Set<OperationDefinition> opDefs = platform2.getOperationDefinitions();
            assert opDefs.size() == 3 : "Did not find the three expected operations in v2";
            // now that the basics are tested, go for the details...

            boolean ubuFound = false;
            for (PackageType pt : packageTypes2) {
                //            System.out.println(at.getName());
                assert !(pt.getName().equals("rpm")) : "RPM should be gone in v2";
                if (pt.getName().equals("ubu")) {
                    ubuFound = true;
                }
            }

            assert ubuFound == true : "Ubu should be in v2";

            boolean startFound = false;
            for (OperationDefinition opDef : opDefs) {
                //            System.out.println(opDef.getName());
                assert !(opDef.getName().equals("restart")) : "Restart should be gone in v2";
                if (opDef.getName().equals("start")) {
                    startFound = true;
                }

                if (opDef.getName().equals("status")) {
                    assert opDef.getDescription().equals("Yadda!") : "Description for 'start' should be 'Yadda!', but was "
                        + opDef.getDescription();
                }
            }

            assert startFound == true : "Start should be in v2";
            getEntityManager().flush();

            /*
             * Now try the other way round
             */

            registerPlugin("./test/metadata/update3-v1_0.xml");
            ResourceType platform3 = getResourceType("myPlatform3");
            Set<PackageType> packageTypes3 = platform3.getPackageTypes();
            assert packageTypes3.size() == 3 : "Did not find the three package types in v3";
            Set<OperationDefinition> ops3 = platform3.getOperationDefinitions();
            assert ops3.size() == 3 : "Did not find three expected operations in v3";

            // we should have rpm, deb, mpkg. ubu from v2 should be gone again.
            boolean rpmFound = false;
            for (PackageType pt : packageTypes3) {
                System.out.println(pt.getName());
                assert !(pt.getName().equals("ubu")) : "ubu should be gone in v3";
                if (pt.getName().equals("rpm")) {
                    rpmFound = true;
                }
            }

            assert rpmFound == true : "rpm should be in v3";
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * See if deletion of a resource type just works
     *
     * @throws Exception
     */
    @Test
    public void testResourceTypeDeletion() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/update4-v1_0.xml");
            ResourceType platform1 = getResourceType("myPlatform4");
            Set<ResourceType> servers1 = platform1.getChildResourceTypes();
            assert servers1.size() == 2 : "Expected to find 2 servers in v1";
            int found = 0;
            for (ResourceType server : servers1) {
                if (server.getName().equals("testServer1")) {
                    found++;
                }

                if (server.getName().equals("testServer2")) {
                    found++;
                }
            }

            assert found == 2 : "I did not find the expected servers in v1";

            registerPlugin("./test/metadata/update4-v2_0.xml");
            ResourceType platform2 = getResourceType("myPlatform4");
            Set<ResourceType> servers2 = platform2.getChildResourceTypes();
            assert servers2.size() == 1 : "Expected to find 1 servers in v2";
            ResourceType server2 = servers2.iterator().next();
            assert server2.getName().equals("testServer1");
            Set<MeasurementDefinition> mdef = server2.getMetricDefinitions();
            assert mdef.size() == 1 : "Expected one MeasurementDefinition in v2";

            registerPlugin("./test/metadata/update4-v1_0.xml");
            ResourceType platform3 = getResourceType("myPlatform4");
            Set<ResourceType> servers3 = platform3.getChildResourceTypes();
            assert servers3.size() == 2 : "Expected to find 2 servers in v1/2";
            found = 0;
            for (ResourceType server : servers3) {
                if (server.getName().equals("testServer1")) {
                    found++;
                }

                if (server.getName().equals("testServer2")) {
                    found++;
                }
            }

            assert found == 2 : "I did not find the expected servers in v1/2";
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Test updating of plugin and resource configs
     *
     * @throws Exception
     */
    @Test
    public void testUpdatePluginConfig() throws Exception {
        getTransactionManager().begin();
        try {
            ResourceType server1 = getServer1ForConfig5();
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

            System.out.println("-------- done with v1");

            //         }

            getEntityManager().flush();

            /*
             * Now deploy version 2
             */

            ResourceType server2 = getServer2ForConfig5();
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

            System.out.println("-------- done with v2");

            // TODO check changing back
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Tests the behaviour of the MetadataManager wrt <process-scan> entries.
     *
     * @throws Exception
     */
    @Test
    public void testProcessScans() throws Exception {
        getTransactionManager().begin();
        try {
            ResourceType server1 = getServer1ForConfig5();

            /*
             * TODO check process scans as well
             */
            Set<ProcessScan> scans1 = server1.getProcessScans();
            assert scans1.size() == 3 : "Expected to find 3 process scans in v1, but got " + scans1.size();
            int found = 0;
            for (ProcessScan ps : scans1) {
                if (containedIn(ps.getName(), new String[] { "JBoss4", "JBoss5", "JBoss6" })) {
                    found++;
                }
            }

            assert found == 3 : "Expected to find 3 process scans in v1";
            // TODO also check query

            getEntityManager().flush();

            /*
             * check process scans in v2 as well
             */
            ResourceType server2 = getServer2ForConfig5();
            Set<ProcessScan> scans2 = server2.getProcessScans();
            assert scans2.size() == 3 : "Expected to find 3 process scans in v2, but got " + scans2.size();
            found = 0;
            for (ProcessScan ps : scans2) {
                if (containedIn(ps.getName(), new String[] { "JBoss5", "JBoss6", "Hibernate" })) {
                    found++;
                }
            }

            assert found == 3 : "Expected to find 3 specific process scans in v2, but got " + found;

            getEntityManager().flush();

            /*
             * Now return to first version of plugin
             */
            server1 = getServer1ForConfig5();
            scans1 = server1.getProcessScans();
            assert scans1.size() == 3 : "Expected to find 3 process scans in v1, but got " + scans1.size();
            found = 0;
            for (ProcessScan ps : scans1) {
                if (containedIn(ps.getName(), new String[] { "JBoss4", "JBoss5", "JBoss6" })) {
                    found++;
                }
            }

            assert found == 3 : "Expected to find 3 specific process scans in v1 again, but got " + found;
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Tests the behaviour wrt <resource-configuration> entries
     *
     * @throws Exception
     */
    @Test
    public void testResourceConfiguration() throws Exception {
        getTransactionManager().begin();
        try {
            ResourceType server1 = getServer1ForConfig5();
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

            getEntityManager().flush();

            /*
             * Now check the changed plugin
             */

            ResourceType server2 = getServer2ForConfig5();
            ConfigurationDefinition def2 = server2.getResourceConfigurationDefinition();
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

            getEntityManager().flush();
            /*
             * And now back to the first version
             */

            ResourceType server3 = getServer1ForConfig5();
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
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Test for storing of back links from Constraint to PropertyDefinitionSimple. See JBNADM-1587
     *
     * @throws Exception
     */
    @Test
    public void testConstraint() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/constraint.xml");
            ResourceType platform = getResourceType("constraintPlatform");
            ConfigurationDefinition config = platform.getResourceConfigurationDefinition();
            PropertyDefinitionSimple propDef = config.getPropertyDefinitionSimple("secureJnpPort");
            Set<Constraint> constraints = propDef.getConstraints();
            assert constraints.size() == 4 : "Expected to get 4 constraints, but got " + constraints.size();

            assert propDef.getDefaultValue().equals("1234");
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Test for storing of back links from Constraint to PropertyDefinitionSimple. See JBNADM-1587
     *
     * @throws Exception
     */
    @Test
    public void testConstraint2() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/constraint.xml");
            ResourceType platform = getResourceType("constraintPlatform");
            ConfigurationDefinition config = platform.getResourceConfigurationDefinition();
            Map<String, PropertyDefinition> propDefMap = config.getPropertyDefinitions();
            PropertyDefinitionSimple propDef = (PropertyDefinitionSimple) propDefMap.get("secureJnpPort");
            Set<Constraint> constraints = propDef.getConstraints();
            assert constraints.size() == 4 : "Expected to get 4 constraints, but got " + constraints.size();

            assert propDef.getDefaultValue().equals("1234");
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Test for setting min/max in contraints. See JBNADM-1596 See JBNADM-1597
     *
     * @throws Exception
     */
    @Test
    public void testConstraintMinMax() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/constraintMinMax.xml");
            ResourceType platform = getResourceType("constraintPlatform");
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
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testListProperty() throws Exception {
        getTransactionManager().begin();
        try {
            { // extra block for variable scoping purposes
                registerPlugin("./test/metadata/propertyList-v1.xml");
                ResourceType platform = getResourceType("myPlatform6");
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
            }

            getEntityManager().flush();

            /*
             * Deploy v2 of the plugin
             */{ // extra block for variable scoping purposes
                registerPlugin("./test/metadata/propertyList-v2.xml");
                ResourceType platform = getResourceType("myPlatform6");
                ConfigurationDefinition cd = platform.getResourceConfigurationDefinition();
                Map<String, PropertyDefinition> propDefs = cd.getPropertyDefinitions();
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
                                // See JBNADM-1596/97
                                assert irc.getMaximum() == 10;
                                assert irc.getMinimum() == -2;
                                assert irc.getDetails().equals("-2#10");
                            } else if (constraint instanceof FloatRangeConstraint) {
                                FloatRangeConstraint frc = (FloatRangeConstraint) constraint;
                                assert frc != null : "Float-constraint was null, but should not be";
                                // See JBNADM-1596/97
                                assert frc.getMinimum() == 10; // TODO change when JBNADM-1597 is being worked on
                                assert frc.getMaximum() == 5;
                                assert frc.getDetails().equals("10.0#5.0");
                            } else {
                                assert true == false : "Unknown constraint type encoutered";
                            }
                        }
                    } else {
                        assert true == false : "Unknown list-definition in v2: " + pdl.getName();
                    }
                }

                // done with v2
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testListPropertyMinMax() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/propertyList-simple.xml");
            ResourceType platform = getResourceType("myPlatform");
            ConfigurationDefinition cd = platform.getResourceConfigurationDefinition();
            Map<String, PropertyDefinition> properties = cd.getPropertyDefinitions();
            PropertyDefinitionList a = (PropertyDefinitionList) properties.get("a");
            assert a.getDescription().equals("Yada !") : "Expected the description to be 'Yada !', but it was "
                + a.getDescription();

            // The next two are marked as @Transient in PropertyDefinitonList
            // See JBNADM-1595
            //            assert a.getMax()==6 : "Expected the max to be 6 but it was " + a.getMax();
            //            assert a.getMin()==4 : "Expected the min to be 4 but it was " + a.getMin();
        } finally {
            getTransactionManager().rollback();
        }
    }

    private ResourceType getServer1ForConfig5() throws Exception {
        registerPlugin("./test/metadata/update5-v1_0.xml");
        ResourceType platform1 = getResourceType("myPlatform5");
        Set<ResourceType> servers = platform1.getChildResourceTypes();
        assert servers.size() == 1 : "Expected to find 1 server in v1, but got " + servers.size();
        ResourceType server1 = servers.iterator().next();
        return server1;
    }

    private ResourceType getServer2ForConfig5() throws Exception {
        registerPlugin("./test/metadata/update5-v2_0.xml");
        ResourceType platform2 = getResourceType("myPlatform5");
        Set<ResourceType> servers2 = platform2.getChildResourceTypes();
        assert servers2.size() == 1 : "Expected to find 1 server in v2, but got " + servers2.size();
        ResourceType server2 = servers2.iterator().next();
        return server2;
    }
}