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

executeAllTests();

rhq.logout();

function testUpdateResourceConfiguration() {
    var resource = findService('service-beta-1', 'server-omega-1');
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

    config = ConfigurationManager.getResourceConfiguration(resource.id);
    var updatedProperty = config.getSimple(propertyName);

    Assert.assertEquals(updatedProperty.stringValue, propertyValue, 'Failed to update resource configuration');
}

function testUpdatePluginConfiguration() {
    var resource = findService('service-beta-0', 'server-omega-0');
    var pluginConfig = ConfigurationManager.getPluginConfiguration(resource.id);

    var propertyName = 'beta-property0';
    var propertyValue = 'updated property value -- ' + java.util.Date();

    var property = pluginConfig.getSimple(propertyName);

    property.setStringValue(propertyValue);

    var configUpdate = ConfigurationManager.updatePluginConfiguration(resource.id, pluginConfig);

    pluginConfig = ConfigurationManager.getPluginConfiguration(resource.id);
    var updatedProperty = pluginConfig.getSimple(propertyName);

    Assert.assertEquals(updatedProperty.stringValue, propertyValue, 'Failed to update plugin configuration');
}

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

function findService(name, parentName) {
    var criteria = ResourceCriteria();
    criteria.addFilterName(name);
    criteria.addFilterParentResourceName(parentName);

    return ResourceManager.findResourcesByCriteria(criteria).get(0);
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
