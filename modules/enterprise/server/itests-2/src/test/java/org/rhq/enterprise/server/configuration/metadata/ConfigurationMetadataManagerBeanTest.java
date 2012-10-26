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

import static java.util.Arrays.asList;
import static org.rhq.core.clientapi.shared.PluginDescriptorUtil.loadPluginConfigDefFor;
import static org.rhq.core.clientapi.shared.PluginDescriptorUtil.loadPluginDescriptor;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.configuration.definition.PropertyOptionsSource;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.test.AssertUtils;

/**
 * These are data-driven tests that exercise the plugin upgrade functionality around configurations such as plugin
 * configurations and resource configurations. The data sets that are used are defined in two plugin descriptors. One
 * is the original version and the other is the upgraded version. In order to avoid inter-dependencies between test
 * methods, a separate plugin configuration should be used for each test method. A separate resource type is declared
 * for each test, further documenting and delinating where each configuration is used. In the test methods, the
 * original and updated configuration definitions are initialized with an xpath expression that specifies the owning
 * resource type.
 */
public class ConfigurationMetadataManagerBeanTest extends AbstractEJB3Test {

    private static final boolean ENABLED = true;

    PluginDescriptor originalDescriptor;

    PluginDescriptor updatedDescriptor;

    ConfigurationDefinition originalConfigDef;

    ConfigurationDefinition updatedConfigDef;

    @BeforeClass
    public void setupClass() {
        String pluginFileBaseName = "configuration_metadata_manager_bean_test";
        String version1 = pluginFileBaseName + "_v1.xml";
        String version2 = pluginFileBaseName + "_v2.xml";

        originalDescriptor = loadPluginDescriptor(getClass().getResource(version1));
        updatedDescriptor = loadPluginDescriptor(getClass().getResource(version2));
    }

    @Test(enabled = ENABLED)
    public void addNewUngroupedSimplePropertyDef() {
        initConfigDefs("servers[name='MyServer1']", "test");

        String propertyName = "newUngroupedProperty";
        PropertyDefinitionSimple expected = updatedConfigDef.getPropertyDefinitionSimple(propertyName);
        PropertyDefinitionSimple actual = originalConfigDef.getPropertyDefinitionSimple(propertyName);

        assertPropertyDefinitionMatches("New ungrouped property defs should be added to the configuration definition",
            expected, actual);
    }

    @Test(enabled = ENABLED)
    public void removePropertyDefThatIsNotInNewDescriptor() {
        initConfigDefs("servers[name='MyServer1']", "test");

        assertNull("The property exists in version 1 but not in version 2 of the plugin; therefore, it should be "
            + "removed the configuration definition", originalConfigDef.get("v1OnlyProperty"));
    }

    @Test(enabled = ENABLED)
    public void doNotModifyExistingPropertyDefThatIsNotModifiedInUpgrade() {
        initConfigDefs("servers[name='MyServer1']", "test");

        String propertyName = "myExistingProperty";
        PropertyDefinitionSimple expected = updatedConfigDef.getPropertyDefinitionSimple(propertyName);
        PropertyDefinitionSimple actual = originalConfigDef.getPropertyDefinitionSimple(propertyName);

        assertPropertyDefinitionMatches("Existing property that is not changed in new version of pluign should "
            + "not change", expected, actual);
    }

    @Test(enabled = ENABLED)
    public void updatePropertyWithAddedOptions() {
        initConfigDefs("servers[name='ServerWithAddedOptions']", "UpdatedPropertyWithAddedOptions");

        String propertyName = "mySimple";
        PropertyDefinitionSimple expectedProperty = updatedConfigDef.getPropertyDefinitionSimple(propertyName);
        List<PropertyDefinitionEnumeration> expected = expectedProperty.getEnumeratedValues();

        PropertyDefinitionSimple actualProperty = originalConfigDef.getPropertyDefinitionSimple(propertyName);
        List<PropertyDefinitionEnumeration> actual = actualProperty.getEnumeratedValues();

        // TODO Need to verify order here as well
        AssertUtils.assertCollectionEqualsNoOrder(expected, actual, "Options should have been added to property");
    }

    @Test(enabled = ENABLED)
    public void addNewGroup() {
        initConfigDefs("servers[name='GroupTests']", "GroupTests");

        assertNotNull("The new property should be added to the configuration definition",
            findGroup("newGroup", originalConfigDef));
    }

    @Test(enabled = ENABLED)
    public void replaceMemberDefinitionOfPropertyList() {
        initConfigDefs("servers[name='UpdatedPropertyList']", "ReplaceMemberDefinitionOfPropertyList");

        String propertyName = "myList";
        PropertyDefinitionList expectedList = updatedConfigDef.getPropertyDefinitionList(propertyName);
        PropertyDefinitionList actualList = originalConfigDef.getPropertyDefinitionList(propertyName);

        assertPropertyDefinitionMatches("The member definition should be replaced with the new version", expectedList,
            actualList);
    }

    @Test(enabled = ENABLED)
    public void mapPropertyOrderTest() {
        initConfigDefs("servers[name='UnchangedMap']", "UnchangedMap");

        String propertyName = "myMap";
        PropertyDefinitionMap map = originalConfigDef.getPropertyDefinitionMap(propertyName);
        Map<String, PropertyDefinition> propDefs = map.getMap();
        assertEquals("Expected three properties in unchanged map", 2, propDefs.size());
        assertEquals("Expected property to be kept", "property1ToKeep", propDefs.get("property1ToKeep").getName());
        assertEquals("Expected property to be kept", "property2ToKeep", propDefs.get("property2ToKeep").getName());
        assertEquals("Expected order_index to be 0", 0, propDefs.get("property1ToKeep").getOrder());
        assertEquals("Expected order_index to be 1", 1, propDefs.get("property2ToKeep").getOrder());
    }

    @Test(enabled = ENABLED)
    public void updateMapWithAddRemoveProperty() {
        initConfigDefs("servers[name='UpdatedMapWithAddRemoveProperty']", "UpdateMapWithAddRemoveProperty");

        String propertyName = "myMap";
        PropertyDefinitionMap map = originalConfigDef.getPropertyDefinitionMap(propertyName);
        Map<String, PropertyDefinition> propDefs = map.getMap();
        assertEquals("Expected three properties in updated map", 3, propDefs.size());
        assertEquals("Expected property to be kept", "property1ToKeep", propDefs.get("property1ToKeep").getName());
        assertEquals("Expected property to be added", "propertyToAdd", propDefs.get("propertyToAdd").getName());
        assertEquals("Expected property to be kept", "property2ToKeep", propDefs.get("property2ToKeep").getName());
        assertEquals("Expected order_index to be 0", 0, propDefs.get("property1ToKeep").getOrder());
        assertEquals("Expected order_index to be 1", 1, propDefs.get("propertyToAdd").getOrder());
        assertEquals("Expected order_index to be 2", 2, propDefs.get("property2ToKeep").getOrder());
    }

    @Test(enabled = ENABLED)
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

        assertEquals("Expected order_index to be 0", 0, actualMap.getMap().get("propertyToUpdate")
            .getOrder());
    }

    @Test(enabled = ENABLED)
    public void updatePropertyDefinitionOptionSource() {
        initConfigDefs("servers[name='OptionSourceTest']", "OptionSourceTest");

        // now check the upgrades
        PropertyDefinitionSimple prop1 = (PropertyDefinitionSimple) updatedConfigDef.get("prop1");
        assert prop1 != null;
        assert prop1.getEnumeratedValues().size() == 0 : "Found an option value. ";
        PropertyOptionsSource source = prop1.getOptionsSource();
        assert source != null : "PropertyOptionSource was not persisted";
        assert source.getFilter() == null : "Assumed filter to be null, but was " + source.getFilter();

        PropertyDefinitionSimple prop2 = (PropertyDefinitionSimple) updatedConfigDef.get("prop2");
        assert prop2 != null;
        assert prop2.getEnumeratedValues().size() == 1;
        assert prop2.getOptionsSource() != null;
        assert prop2.getOptionsSource().getExpression().equals("*");

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
        // The next line updates originalConfigDef with the content of updatedConfigDef
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
