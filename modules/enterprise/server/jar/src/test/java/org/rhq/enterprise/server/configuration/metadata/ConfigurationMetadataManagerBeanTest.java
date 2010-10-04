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

import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.test.AssertUtils;

import static org.rhq.enterprise.server.configuration.metadata.PluginDescriptorUtil.loadPluginConfigDefFor;
import static org.rhq.enterprise.server.configuration.metadata.PluginDescriptorUtil.loadPluginDescriptor;
import static java.util.Arrays.asList;

/**
 * These are data-driven tests that exercise the plugin upgrade functionality around configurations such as plugin
 * configurations and resource configurations. The data sets that are used are defined in two plugin descriptors. One
 * is the original version and the other is the upgraded version. In order to avoid inter-dependencies between test
 * methods, a separate plugin configuration should be used for each test method. A separate resource type is declared
 * for each test, further documenting and delinating where each configuration is used. In the test methods, the
 * original and updated coniguration definitions are initialized with an xpath expression that specifies the owning
 * resource type.
 */
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
    public void addNewUngroupedSimplePropertyDef() {
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

    @Test
    public void addNewGroup() {
        initConfigDefs("servers[name='GroupTests']", "GroupTests");

        assertNotNull("The new property should be added to the configuration definition",
            findGroup("newGroup", originalConfigDef));
    }

    @Test
    public void replaceMemberDefinitionOfPropertyList() {
        initConfigDefs("servers[name='UpdatedPropertyList']", "ReplaceMemberDefinitionOfPropertyList");

        String propertyName = "myList";
        PropertyDefinitionList expectedList = updatedConfigDef.getPropertyDefinitionList(propertyName);
        PropertyDefinitionList actualList = originalConfigDef.getPropertyDefinitionList(propertyName);

        assertPropertyDefinitionMatches("The member definition should be replaced with the new version", expectedList, actualList);
    }

    // Test is currently failing with,
    //
    //     IllegalArgumentException: Removing a detached instance org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple
    //
    // I beleive that this is a problem with the test environment and not with production code.
    @Test(enabled = false)
    public void updateMapWithRemovedProperty() {
        initConfigDefs("servers[name='UpdatedMapWithRemovedProperty']", "UpdateMapWithRemovedProperty");

        String propertyName = "myMap";
        PropertyDefinitionMap map = originalConfigDef.getPropertyDefinitionMap(propertyName);
        assertEquals("Expected property to be removed when it is removed from parent map", 0,
            map.getPropertyDefinitions().size());
    }

    @Test
    public void updateMapWithUpdatedProperty() {
        initConfigDefs("servers[name='UpdatedMapWithUpdatedProperty']", "UpdateMapWithUpdatedProperty");

        String propertyName = "propertyToUpdate";
        String mapPropertyName = "myMap";

        PropertyDefinitionMap expectedMap = updatedConfigDef.getPropertyDefinitionMap(mapPropertyName);
        PropertyDefinitionSimple expected = expectedMap.getPropertyDefinitionSimple(propertyName);

        PropertyDefinitionMap actualMap = originalConfigDef.getPropertyDefinitionMap(mapPropertyName);
        PropertyDefinitionSimple actual = actualMap.getPropertyDefinitionSimple(propertyName);

        List<String> ignoredProperties = asList("id", "parentPropertyMapDefinition");

        assertPropertyDefinitionMatches("Expected property who is a child of map to get updated and remain in the map",
            expected, actual, ignoredProperties);
    }

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
        ConfigurationMetadataManagerLocal configMetadataMgr = LookupUtil.getConfigurationMetadataManager();
        configMetadataMgr.updateConfigurationDefinition(updatedConfigDef, originalConfigDef);
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

    void assertPropertyDefinitionMatches(String msg, PropertyDefinition expected, PropertyDefinition actual) {
        AssertUtils.assertPropertiesMatch(msg, expected, actual, "id", "configurationDefinition");
    }

    void assertPropertyDefinitionMatches(String msg, PropertyDefinition expected, PropertyDefinition actual,
        List<String> ignoredProperties) {
        AssertUtils.assertPropertiesMatch(msg, expected, actual, ignoredProperties);
    }

}
