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

function testCreateAndDeleteResourceGroup() {
    var resourceGroup = createResourceGroup();

    Assert.assertFalse(resourceGroup.id == 0, 'Failed to create resource group');

    ResourceGroupManager.deleteResourceGroup(resourceGroup.id);

    var exception = null;
    try {
        ResourceGroupManager.getResourceGroup(resourceGroup.id);
    }
    catch (e) {
        exception = e;
    }

    Assert.assertNotNull(exception, 'Failed to delete resource group');
}

function testAddResourcesToGroup() {
    var resourceGroup = createResourceGroup();

    Assert.assertFalse(resourceGroup.id == 0, 'Cannot add resources to group. Failed to create resource group.');

    var resources = findAlphaServices();
    Assert.assertNumberEqualsJS(resources.size(), 10, 'Cannot add resources to group. Failed to find the correct number of resources.');

    addResourcesToGroup(resourceGroup, resources);

    var criteria = ResourceGroupCriteria();
    criteria.addFilterId(resourceGroup.id);
    criteria.fetchExplicitResources(true);

    resourceGroup = ResourceGroupManager.findResourceGroupsByCriteria(criteria).get(0);

    Assert.assertNumberEqualsJS(resourceGroup.explicitResources.size(), 10, 'Failed to find resources in group. Resources may not have been added.');
}

function testFindWithFiltering() {
    var resourceGroup = createResourceGroup();
    var resources = findAlphaServices();
    var resource = resources.get(0);

    addResourcesToGroup(resourceGroup, resources);

    var criteria = ResourceGroupCriteria();
    criteria.addFilterId(resourceGroup.id);
    criteria.addFilterPluginName('PerfTest');
    criteria.addFilterResourceTypeId(resource.resourceType.id);
    criteria.addFilterResourceTypeName(resource.resourceType.name);
    criteria.addFilterName(resourceGroup.name);
    criteria.addFilterGroupCategory(GroupCategory.COMPATIBLE);
    criteria.addFilterExplicitResourceIds(getIds(resources));

    var resourceGroups = ResourceGroupManager.findResourceGroupsByCriteria(criteria);

    Assert.assertNumberEqualsJS(resourceGroups.size(), 1, 'Failed to find resource groups when applying filters.');
}

function createResourceGroup() {
    var resourceType = getResourceType();
    Assert.assertNotNull(resourceType, 'Failed to find resource type for new resource group.');

    var groupName = 'test-group-' + java.util.Date().getTime();

    return ResourceGroupManager.createResourceGroup(ResourceGroup(groupName, resourceType));
}

function getResourceType() {
    var resourceTypeName = 'service-alpha';
    var pluginName = 'PerfTest';

    return ResourceTypeManager.getResourceTypeByNameAndPlugin(resourceTypeName, pluginName);
}

function findAlphaServices() {
    var criteria = ResourceCriteria();
    criteria.caseSensitive = true;
    criteria.strict = true;
    criteria.addFilterParentResourceName('server-omega-0');
    criteria.addFilterResourceTypeName('service-alpha');
    criteria.fetchResourceType(true);

    return ResourceManager.findResourcesByCriteria(criteria);
}

function addResourcesToGroup(group, resources) {
    var resourceIds = getIds(resources);
    ResourceGroupManager.addResourcesToGroup(group.id, resourceIds);
}

function getIds(resources) {
    var ids = [];

    for (i = 0; i < resources.size(); ++i) {
        ids.push(resources.get(i).id);
    }

    return ids;
}
