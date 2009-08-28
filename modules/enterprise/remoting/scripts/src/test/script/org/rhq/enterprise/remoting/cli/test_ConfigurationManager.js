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

executeAllTests();

rhq.logout();

function testUpdateResourceConfiguration() {
    var resource = findService('service-beta-0', 'server-omega-0');
    var config = ConfigurationManager.getResourceConfiguration(resource.id);

    var propertyName = 'beta-config0';
    var propertyValue = 'updated property value -- ' + java.util.Date();

    var property = config.getSimple(propertyName);

    property.setStringValue(propertyValue);

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

function findService(name, parentName) {
    var criteria = ResourceCriteria();
    criteria.addFilterName(name);
    criteria.addFilterParentResourceName(parentName);

    return ResourceManager.findResourcesByCriteria(criteria).get(0);
}