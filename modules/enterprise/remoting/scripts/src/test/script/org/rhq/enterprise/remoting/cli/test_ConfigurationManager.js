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

rhq.login('rhqadmin', 'rhqadmin');

skippedTests.push('testUpdateResourceGroupConfiguration');

//executeAllTests();
executeTests(['testTranslateConfigFromStructuredToRaw'])

rhq.logout();

function testUpdateResourceConfiguration() {
    var resource = findResourceByNameAndParentName('service-beta-1', 'server-omega-1');
    var config = ConfigurationManager.getResourceConfiguration(resource.id);

    var propertyName = 'beta-config0';
    var propertyValue = 'updated property value -- ' + java.util.Date();

    var property = config.getSimple(propertyName);

    property.setStringValue(propertyValue);
    config.put(property);

    var configUpdate = ConfigurationManager.updateResourceConfiguration(resource.id, config);

    while (ConfigurationManager.isResourceConfigurationUpdateInProgress(resource.id)) {
        java.lang.Thread.sleep(1000);
    }

    var latestUpdate = ConfigurationManager.getLatestResourceConfigurationUpdate(resource.id);
    var config = latestUpdate.configuration;
    var updatedProperty = config.getSimple(propertyName);

    Assert.assertEquals(updatedProperty.stringValue, propertyValue, 'Failed to update resource configuration');
}

function testGetAndUpdateRawConfiguration() {
    var resourceName = 'Raw Server'
    var rawServer = findResourceByNameAndParentName(resourceName, 'localhost');

    var latestUpdate = loadLatestRawConfigUpdate(resourceName, rawServer.id);
    var rawConfig = getRawConfig(resourceName, latestUpdate.configuration, 0);

    var contents = java.lang.String(rawConfig.contents);
    var modifiedContents = java.lang.StringBuilder(contents).append("\nModified at ").append(getDate()).toString();

    rawConfig.contents = modifiedContents.getBytes();

    var configUdpate = ConfigurationManager.updateResourceConfiguration(rawServer.id, latestUpdate.configuration);

    waitForResourceConfigUpdateToComplete(rawServer.id);

    latestUpdate = loadLatestRawConfigUpdate(resourceName, rawServer.id);
    
    rawConfigs = java.util.LinkedList(latestUpdate.configuration.rawConfigurations);
    rawConfig = rawConfigs.get(0);
    var updatedContents = rawConfig.contents;

    Assert.assertEquals(
        java.lang.String(updatedContents),
        java.lang.String(modifiedContents),
        "Failed to update raw config for '" + resourceName + "'"
    );
}

function testGetAndUpdateStructuredConfiguration() {
    var resourceName = 'Structured Server';
    var structuredServer = findResourceByNameAndParentName(resourceName, 'localhost');

    var latestUpdate = loadLatestStructuredConfigUpdate(resourceName, structuredServer.id);
    var modifiedConfig = latestUpdate.configuration;

    modifiedConfig.put(PropertySimple("foo", java.util.Date()));

    var configUdpate = ConfigurationManager.updateResourceConfiguration(structuredServer.id, modifiedConfig);

    waitForResourceConfigUpdateToComplete(structuredServer.id);

    latestUpdate = loadLatestStructuredConfigUpdate(resourceName, structuredServer.id);

    Assert.assertEquals(
        latestUpdate.configuration,
        modifiedConfig,
        "Failed to update structured config for '" + resourceName + "'"
    );    
}

function testGetAndUpdateStructuredandRawConfigurationOnRawEdit() {
    var resourceName = 'Structured and Raw Server';
    var structuredAndRawServer = findResourceByNameAndParentName(resourceName, 'localhost');

    var latestUpdate = loadLatestStructuredAndRawConfigUpdate(resourceName, structuredAndRawServer.id);

    var rawConfig = getRawConfig(resourceName, latestUpdate.configuration, 0);

    var contents = java.lang.String(rawConfig.contents);
    var modifiedContents = java.lang.StringBuilder(contents).append("\nModified at ").append(getDate()).toString();

    rawConfig.contents = modifiedContents.bytes;

    var configUpdate = ConfigurationManager.updateResourceConfiguration(structuredAndRawServer.id,
        latestUpdate.configuration, false);

    waitForResourceConfigUpdateToComplete(structuredAndRawServer.id);

    latestpdate = loadLatestStructuredAndRawConfigUpdate(resourceName, structuredAndRawServer.id);

    rawConfigs = java.util.LinkedList(latestUpdate.configuration.rawConfigurations);
    rawConfig = rawConfigs.get(0);
    var updatedContents = rawConfig.contents;

    Assert.assertEquals(
        java.lang.String(updatedContents),
        java.lang.String(modifiedContents),
        "Failed to update raw config for '" + resourceName + "'"
    );
}

function testGetAndUpdateStrucuturedOnStructuredEdit() {
    var resourceName = 'Structured and Raw Server';
    var structuredAndRawServer = findResourceByNameAndParentName(resourceName, 'localhost');

    var latestUpdate = loadLatestStructuredAndRawConfigUpdate(resourceName, structuredAndRawServer.id);
    var modifiedConfig = latestUpdate.configuration;

    modifiedConfig.put(PropertySimple("x", java.util.Date()));

    ConfigurationManager.updateResourceConfiguration(structuredAndRawServer.id, modifiedConfig, true);

    waitForResourceConfigUpdateToComplete(structuredAndRawServer.id);

    latestUpdate = loadLatestStructuredAndRawConfigUpdate(resourceName, structuredAndRawServer.id);

    Assert.assertEquals(
        latestUpdate.configuration.properties,
        modifiedConfig.properties,
        "Failed to update structured config for '" + resourceName + "'"
    );
}

function testTranslateConfigFromStructuredToRaw() {
    var resourceName = 'Structured and Raw Server';
    var server = findResourceByNameAndParentName(resourceName, 'localhost');

    var latestUpdate = loadLatestStructuredAndRawConfigUpdate(resourceName, server.id);
    var latestConfig = latestUpdate.configuration;

    var newValue = "merged - " + java.util.Date().toString();
    latestConfig.put(PropertySimple("y", newValue));

    var mergedConfig = ConfigurationManager.translateResourceConfiguration(server.id, latestConfig, true);

    Assert.assertNotNull(mergedConfig, "Expected to get back a configuration object after translation");

    var rawConfigs = java.util.ArrayList(mergedConfig.rawConfigurations);

    Assert.assertNumberEqualsJS(rawConfigs.size(), 3, "Expected there to be 3 raw config files");

    var rawConfig = null;
    for (i = 0; i < rawConfigs.size(); i++) {
        if (rawConfigs.get(i).path.endsWith('structured-and-raw-test-1.txt')) {
            rawConfig = rawConfigs.get(i);
            break;
        }
    }

    Assert.assertNotNull(rawConfig, "Failed to find modified raw config");

    var contents = java.lang.String(rawConfig.contents);

    Assert.assertTrue(contents.contains(newValue), "Failed to modifiy raw config");

    var properties = java.util.ArrayList(mergedConfig.properties);
    for (i = 0; i < properties.size(); i++) {
        var property = properties.get(i);
        Assert.assertNotNull(property.configuration);
        Assert.assertNumberEqualsJS(property.configuration.id, latestConfig.id, "Property references the wrong configuration");
    }
}

function loadLatestRawConfigUpdate(resourceName, resourceId) {
    var latestUpdate = ConfigurationManager.getLatestResourceConfigurationUpdate(resourceId);

    Assert.assertNotNull(latestUpdate, "Failed to find latest resource configuration update for '" + resourceName + "'");
    Assert.assertTrue(
        latestUpdate.configuration.rawConfigurations.size() > 0,
        "Expected to find at least one raw config file for '" + resourceName + "'"
    );

    return latestUpdate;
}

function loadLatestStructuredConfigUpdate(resourceName, resourceId) {
    var latestUpdate = ConfigurationManager.getLatestResourceConfigurationUpdate(resourceId);
    Assert.assertNotNull(latestUpdate, "Failed to find latest resource configuration update for '" + resourceName + "'");

    return latestUpdate;
}

function loadLatestStructuredAndRawConfigUpdate(resourceName, resourceId) {
    var latestUpdate = ConfigurationManager.getLatestResourceConfigurationUpdate(resourceId);

    Assert.assertNotNull(latestUpdate, "Failed to find latest resource configuration update for '" + resourceName + "'");
    Assert.assertTrue(
        latestUpdate.configuration.rawConfigurations.size() > 0,
        "Expected to find at least one raw config file for '" + resourceName + "'"
    )

    return latestUpdate;
}

function getRawConfig(resourceName, configuration, index) {
    var rawConfigs = java.util.LinkedList(configuration.rawConfigurations);
    var rawConfig = rawConfigs.get(index);

    Assert.assertTrue(
        rawConfig.contents.length > 0,
        "Contents of raw config file for '" + resourceName + "' should not be empty."
    );

    return rawConfig;
}

function getDate() {
    var format = java.text.DateFormat.getDateInstance();
    return format.format(java.util.Date());
}

function waitForResourceConfigUpdateToComplete(resourceId) {
    while (ConfigurationManager.isResourceConfigurationUpdateInProgress(resourceId)) {
        java.lang.Thread.sleep(1000);
    }
}

//function testUpdatePluginConfiguration() {
//    var resource = findService('service-beta-0', 'server-omega-0');
//    var pluginConfig = ConfigurationManager.getPluginConfiguration(resource.id);
//
//    var propertyName = 'beta-property0';
//    var propertyValue = 'updated property value -- ' + java.util.Date();
//
//    var property = pluginConfig.getSimple(propertyName);
//
//    property.setStringValue(propertyValue);
//
//    var configUpdate = ConfigurationManager.updatePluginConfiguration(resource.id, pluginConfig);
//
//    pluginConfig = ConfigurationManager.getPluginConfiguration(resource.id);
//    var updatedProperty = pluginConfig.getSimple(propertyName);
//
//    Assert.assertEquals(updatedProperty.stringValue, propertyValue, 'Failed to update plugin configuration');
//}

// This test is failing I think due to the asynchronous nature of the operations involved. I have verified through the
// web UI that the configuration updates are actually happening. I just need to figure out how to make the test wait
// so that it can (consistently) get the udpated values.
// - jsanda
function testUpdateResourceGroupConfiguration() {
    var groupName = 'service-beta-group -- ' + java.util.Date();
    var group = createResourceGroup(groupName);

    var services = findBetaServices('server-omega-0');

    Assert.assertNumberEqualsJS(services.size(), 10, 'Failed to find beta services');

    addServicesToGroup(services, group);

    var configs = loadConfigs(services);

    Assert.assertNumberEqualsJS(configs.length, 10, 'Failed to load all resource configurations');

    var propertyName = 'beta-config0';
    var propertyValue = 'updated property value -- ' + java.util.Date();

    updateResourceConfigs(configs, propertyValue);

    var configMap = toMap(services, configs);

    var updateId = ConfigurationManager.scheduleGroupResourceConfigurationUpdate(group.id, configMap);

    while (ConfigurationManager.isGroupResourceConfigurationUpdateInProgress(updateId)) {
        java.lang.Thread.sleep(1000);
    }

    var updatedConfigs = loadConfigs(services);

    for (i in updatedConfigs) {
        var updatedProperty = updatedConfigs[i].getSimple(propertyName);

        Assert.assertEquals(updatedProperty.stringValue, propertyValue, "Failed to update resource group configuration");
    }
}

function findResourceByNameAndParentName(name, parentName) {
    var criteria = ResourceCriteria();
    criteria.addFilterName(name);
    criteria.addFilterParentResourceName(parentName);
    criteria.strict = true;

    var resource = ResourceManager.findResourcesByCriteria(criteria).get(0);

    Assert.assertNotNull(resource, "Failed to find '" + name + "'");

    return resource;
}

function findBetaServices(parentName) {
    var criteria = ResourceCriteria();
    criteria.addFilterParentResourceName(parentName);
    criteria.addFilterResourceTypeName('service-beta');
    criteria.caseSensitive = true;
    criteria.strict = true;

    return ResourceManager.findResourcesByCriteria(criteria);
}

function createResourceGroup(name) {
    var resourceType = getResourceType('service-beta');
    Assert.assertNotNull(resourceType, 'Failed to find resource type for new resource group.');

    return ResourceGroupManager.createResourceGroup(ResourceGroup(name, resourceType));
}

function addServicesToGroup(services, group) {
    var serviceIds = [];

    for (i = 0; i < services.size(); ++i) {
        serviceIds.push(services.get(i).id);
    }

    ResourceGroupManager.addResourcesToGroup(group.id, serviceIds);
}

function getResourceType(resourceTypeName) {
    var pluginName = 'PerfTest';

    return ResourceTypeManager.getResourceTypeByNameAndPlugin(resourceTypeName, pluginName);
}

function loadConfigs(resources) {
    var configs = [];

    for (i = 0; i < resources.size(); ++i) {
        configs.push(ConfigurationManager.getResourceConfiguration(resources.get(i).id));
    }

    return configs;
}

function updateResourceConfigs(configs, propertyValue) {
    var propertyName = 'beta-config0';
    var property = null;

    for (i in configs) {
        property = configs[i].getSimple(propertyName);
        property.stringValue = propertyValue;
        configs[i].put(property);
    }
}

function toMap(services, configs) {
    var map = java.util.HashMap();

    for (i = 0; i < services.size(); ++i) {
        map.put(java.lang.Integer(services.get(i).id), configs[i]);
    }

    return map;
}
