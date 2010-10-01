/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.enterprise.server.configuration.metadata;

import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.test.AssertUtils;

import static org.rhq.enterprise.server.configuration.metadata.PluginDescriptorUtil.loadPluginConfigDefFor;
import static org.rhq.enterprise.server.configuration.metadata.PluginDescriptorUtil.loadPluginDescriptor;

public class ConfigurationMetadataManagerBeanTest extends AbstractEJB3Test {

    PluginDescriptor originalDescriptor;

    PluginDescriptor updatedDescriptor;

    ConfigurationDefinition originalConfigDef;

    ConfigurationDefinition updatedConfigDef;

    @BeforeClass
    public void setupClass() {
        String pluginFileBaseName = "configuration_metadata_manager_bean_test";
        String version1 = pluginFileBaseName + "_v1.xml";
        String version2 = pluginFileBaseName + "_v2.xml";

        originalDescriptor = loadPluginDescriptor(getPackagePath() + version1);
        updatedDescriptor = loadPluginDescriptor(getPackagePath() + version2);
    }

    @Test
    public void addNewUngroupedPropertyDef() {
        initConfigDefs("servers[name='MyServer1']", "test");

        String propertyName = "newUngroupedProperty";
        PropertyDefinitionSimple expected = updatedConfigDef.getPropertyDefinitionSimple(propertyName);
        PropertyDefinitionSimple actual = originalConfigDef.getPropertyDefinitionSimple(propertyName);

        assertPropertyDefinitionMatches("New ungrouped property defs should be added to the configuration definition",
            expected, actual);
    }

    @Test
    public void removePropertyDefThatIsNotInNewDescriptor() {
        initConfigDefs("servers[name='MyServer1']", "test");

        assertNull("The property exists in version 1 but not in version 2 of the plugin; therefore, it should be " +
            "removed the configuration definition", originalConfigDef.get("v1OnlyProperty"));
    }

    @Test
    public void doNotModifyExistingPropertyDefThatIsNotModifiedInUpgrade() {
        initConfigDefs("servers[name='MyServer1']", "test");

        String propertyName = "myExistingProperty";
        PropertyDefinitionSimple expected = updatedConfigDef.getPropertyDefinitionSimple(propertyName);
        PropertyDefinitionSimple actual = originalConfigDef.getPropertyDefinitionSimple(propertyName);

        assertPropertyDefinitionMatches("Existing property that is not changed in new version of pluign should " +
            "not change", expected, actual);
    }

    // This test fails with,
    //
    //    PersistentObjectException: detached entity passed to persist
    //
    // I get the exception with both PropertyGroupDefinition and PropertyDefinitionSimple when I have tried various
    // approaches to get past the exception.
    @Test(enabled = false)
    public void addNewGroup() {
        initConfigDefs("servers[name='GroupTests']", "GroupTests");

        assertNotNull("The new property should be added to the configuration definition",
            findGroup("newGroup", originalConfigDef));
    }

//    void assertGroupDefinitionExists() {
//        for (PropertyGroupDefinition groupDef : originalConfigurationDef.getGroupDefinitions()) {
//            if (groupDef.getName().equals("groupToBeRemoved")) {
//                assertTrue(groupDef.getId() != 0);
//                assertNotNull(entityMgr.find(PropertyGroupDefinition.class, groupDef.getId()));
//            }
//        }
//    }

    private void initConfigDefs(String path, String configName) {
        loadAndPersistConfigDefs(path, configName);
        updateConfigDef();
    }

    private void loadAndPersistConfigDefs(String path, String configName) {
        originalConfigDef = loadPluginConfigDefFor(originalDescriptor, path, configName);
        assertNotNull(originalConfigDef);
        updatedConfigDef = loadPluginConfigDefFor(updatedDescriptor, path, configName);
        assertNotNull(updatedConfigDef);

        try {
            getTransactionManager().begin();

            EntityManager entityMgr = getEntityManager();
            entityMgr.persist(originalConfigDef);
            //entityMgr.persist(updatedConfigDef);

            getTransactionManager().commit();
        } catch (Exception e) {
            try {
                getTransactionManager().rollback();
            } catch (SystemException e1) {
                throw new RuntimeException(e1);
            }
            throw new RuntimeException(e);
        }
    }

    private void updateConfigDef() {
        try {
            getTransactionManager().begin();

            ConfigurationMetadataManagerLocal configMetadataMgr = LookupUtil.getConfigurationMetadataManager();
            configMetadataMgr.updateConfigurationDefinition(updatedConfigDef, originalConfigDef);

            getTransactionManager().commit();
        } catch (Exception e) {
            try {
                getTransactionManager().rollback();
            } catch (SystemException e1) {
                throw new RuntimeException(e1);
            }
            throw new RuntimeException(e);
        }
    }

    private String getPackagePath() {
        return "/" + getClass().getPackage().getName().replace('.', '/') + "/";
    }

    private PropertyGroupDefinition findGroup(String name, ConfigurationDefinition configDef) {
        for (PropertyGroupDefinition groupDef : configDef.getGroupDefinitions()) {
            if (groupDef.getName().equals(name)) {
                return groupDef;
            }
        }
        return null;
    }

    void assertPropertyDefinitionMatches(String msg, PropertyDefinitionSimple expected,
            PropertyDefinitionSimple actual) {
        AssertUtils.assertPropertiesMatch(msg, expected, actual, "id", "configurationDefinition");
    }

    @Test(enabled = false)
    public void existingUngroupedPropertyDefShouldBeUpdated() throws Exception {
        PropertyDefinitionSimple expected = updatedConfigDef.getPropertyDefinitionSimple("foo");
        PropertyDefinitionSimple actual = originalConfigDef.getPropertyDefinitionSimple("foo");

        assertPropertyDefinitionMatches("Existing ungrouped property defs should be updated", expected, actual);
    }

    @Test(enabled = false)
    public void propertyDefNotInNewConfigurationDefShouldBeRemoved() throws Exception {
        assertNull(
            "A property def in the original configuration def that is removed in the new configuration def should be deleted",
            originalConfigDef.getPropertyDefinitionSimple("propertyToBeRemoved")
        );
    }

    @Test(enabled = false)
    public void propertyGroupDefNotInNewConfigurationDefShouldBeRemoved() throws Exception {
        for (PropertyGroupDefinition def : originalConfigDef.getGroupDefinitions()) {
            if (def.getName().equals("groupToBeRemoved")) {
                fail("Expected property group 'groupToBeRemoved' to be deleted since it is not in the new configuration def.");
            }
        }
    }
}
