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

skippedTests.push('testUninventoryResources');

executeAllTests();

function testFindUnfiltered() {
    var resources = ResourceManager.findResourcesByCriteria(ResourceCriteria());

    Assert.assertNotNull(resources, 'Expected non-null results for criteria search.');
    Assert.assertTrue(resources.size() > 0, 'Expected non-empty result list.');
}

function testFindWithFiltering() {
    var criteria = createCriteria();
    var resources = ResourceManager.findResourcesByCriteria(criteria);

    Assert.assertNumberEqualsJS(resources.size(), 1, "Expected to get back a single resource");
}

function testFindWithOptionalFiltering() {
    var criteria = createCriteria();
    criteria.filtersOptional = true;
    criteria.addFilterParentResourceName('_does_not_exist_');

    var resources = ResourceManager.findResourcesByCriteria(criteria);

    Assert.assertTrue(resources.size() > 0, "Expected to find resources when filtering made optional");
}

function testFindWithFilteringAndFetchingAssociations() {
    var criteria = createCriteria();
    criteria.fetchAgent(true);
    criteria.fetchAlertDefinitions(true);
    criteria.fetchResourceType(true);
    criteria.fetchChildResources(true);
    criteria.fetchParentResource(true);
    criteria.fetchResourceConfiguration(true);
    criteria.fetchResourceErrors(true);
    criteria.fetchPluginConfigurationUpdates(true);
    criteria.fetchImplicitGroups(true);
    criteria.fetchExplicitGroups(true);
    criteria.fetchOperationHistories(true);

    var resources = ResourceManager.findResourcesByCriteria(criteria);

    Assert.assertNumberEqualsJS(resource.size(), 1, "Expected to get back a single resource");
    Assert.assertNotNull(resource.agent, "resource.agent should have been loaded");
    Assert.assertNotNull(resource.alertDefinitions, "resource.alertDefinitions should have been loaded");
    Assert.assertNotNull(resource.resourceType, "resource.resourceType should have been loaded");
    Assert.assertNotNull(resource.childResources, "resource.childResources should have been loaded");
    Assert.assertNotNull(resource.parentResource, "resource.parentResource should have been loaded");
    Assert.assertNotNull(resource.resourceConfiguration, "resource.resourceConfiguration should have been loaded");
    Assert.assertNotNull(resource.resourceErrors, "resource.resourceErrors should have been loaded");
    Assert.assertNotNull(resource.pluginConfigurationUpdates, "resource.pluginConfigurationUpdates should have been loaded");
    Assert.assertNotNull(resource.implicitGroups, "resource.implicitGroups should have been loaded");
    Assert.assertNotNull(resource.explicitGroups, "reosurce.explicitGroups should have been loaded");
    Assert.assertNotNull(resource.opertionHistories, "resource.operationHistories should have been loaded");
}

function testSortBySingleProperty() {
    var criteria = ResourceCriteria();
    criteria.caseSensitive = true;
    criteria.addFilterParentResourceName('server-omega-0');
    criteria.addFilterResourceTypeName('service-beta');
    criteria.addSortName(PageOrdering.DESC);

    var resources = ResourceManager.findResourcesByCriteria(criteria);

    Assert.assertTrue(resources.size() > 0, "Expected to get back resources when sorting by a single property, resource.name");

    // TODO verify resources are actually sorted
}

function testSortByMultipleProperties() {
    var criteria = ResourceCriteria();
    criteria.caseSensitive = true;
    criteria.addFilterParentResourceName('server-omega-0');
    criteria.addFilterResourceTypeName('service-beta');

    criteria.addSortName(PageOrdering.DESC);
    criteria.addSortResourceTypeName(PageOrdering.DESC);
    criteria.addSortInventoryStatus(PageOrdering.DESC);
    criteria.addSortVersion(PageOrdering.DESC);
    criteria.addSortResourceCategory(PageOrdering.DESC);

    var resources= ResourceManager.findResourcesByCriteria(criteria);

    Assert.assertTrue(resources.size() > 0, "Expected to get resources when sorting by multiple proerties.");
}

//function addSortingToCriteria(criteria) {
//
//}

function testFindResourceLineage() {
    criteria = ResourceCriteria();
    criteria.addFilterName("service-alpha-0");
    criteria.addFilterParentResourceName("server-omega-0");

    resources = ResourceManager.findResourcesByCriteria(criteria);
    resource = resources.get(0);

    resources = ResourceManager.findResourceLineage(resource.id);

    Assert.assertNumberEqualsJS(resources.size(), 3, "The wrong resource lineage returned for resource " + resource);
    Assert.assertEquals(resources.get(0).name, "localhost.localdomain", "The wrong root resource was returned");
    Assert.assertEquals(resources.get(1).name,  "server-omega-0", "The wrong parent resource was returned");
    Assert.assertEquals(resources.get(2).name, "service-alpha-0", "The last resource in the lineage is wrong");
}

function createCriteria() {
    var criteria = ResourceCriteria();
    criteria.caseSensitive = true;
    addFilters(criteria);

    return criteria;
}

function addFilters(criteria) {
    var filters = getFilters();

    for (i = 0; i < filters.length; ++i) {
        var filter = filters[i];
        criteria['addFilter' + filter.name](filter.value);
    }
}

function getFilters() {
    var resourceName = 'service-alpha-0';

    return [
        {name: 'Name',               value: resourceName},
        {name: 'ParentResourceName', value: 'server-omega-0'},
        {name: 'ResourceKey',        value: resourceName},
        {name: 'Description',        value: resourceName + ' description'},
        {name: 'PluginName',         value: 'PerfTest'},
        {name: 'Version',            value: '1.0'},
        {name: 'AgentName',          value: 'localhost.localdomain'}
    ];
}

//function testSortByResourceName() {
//    var criteria = ResourceCriteria();
//    criteria.addFilterParentResourceName("server-omega-0");
//    criteria.addFilterResourceTypeName('service-beta');
//    criteria.addSortName(PageOrdering.ASC);
//
//    var ascResources = ResourceManager.findResourcesByCriteria(criteria);
//
//    criteria.addSortName(PageOrdering.DESC);
//
//    var descResources = ResourceManager.findResourcesByCriteria(criteria);
//
//    assertResourcesSorted(ascResources, descResources, "name");
//}
//
//function testSortByPluginName() {
//    var criteria = ResourceCriteria();
//    criteria.setPaging(0, -1);
//    criteria.addFilterParentResourceName("localhost.localdomain");
//    criteria.addSortPluginName(PageOrdering.ASC);
//    criteria.fetchResourceType(true);
//
//    var ascResources = ResourceManager.findResourcesByCriteria(criteria);
//
//    criteria = ResourceCriteria();
//    criteria.setPaging(0, -1);
//    criteria.addFilterParentResourceName("localhost.localdomain");
//    criteria.addSortPluginName(PageOrdering.DESC);
//    criteria.fetchResourceType(true);
//
//    var descResources = ResourceManager.findResourcesByCriteria(criteria);
//
//    assertResourcesSorted(ascResources, descResources, "resourceType.plugin");
//}
//
//function testSortByPluginNameAndByResourceTypeNameAndAgentName() {
//    var criteria = ResourceCriteria();
//    criteria.addSortPluginName(PageOrdering.ASC);
//    criteria.addSortResourceTypeName(PageOrdering.ASC);
//    criteria.addSortAgentName(PageOrdering.ASC);
//
//    var ascResources = ResourceManager.findResourcesByCriteria(criteria);
//
//    criteria.addSortPluginName(PageOrdering.DESC);
//    criteria.addSortResourceTypeName(PageOrdering.DESC);
//    criteria.addSortAgentName(PageOrdering.DESC);
//
//    var descResources = ResourceManager.findResourcesByCriteria(criteria);
//
//    //assertResourcesSorted(ascResources, descResources, ["resourceType.plugin", "resourceType.name", "agent.name"]);
//}
//
//function testFindResourcesWithAssociations() {
//    var criteria = ResourceCriteria();
//    criteria.addFilterParentResourceName('server-omega-0');
//    criteria.fetchAgent(true);
//    criteria.fetchResourceType(true);
//    criteria.fetchChildResources(true);
//    criteria.fetchAlertDefinitions(true);
//    criteria.fetchParentResource(true);
//
//    var resources = ResourceManager.findResourcesByCriteria(criteria);
//
//    Assert.assertNotNull(resources, "Expected to get back non-null results.");
//    Assert.assertTrue(resources.size() > "Expected to get back resources when fetch associations.");
//}



// testUninventoryResources() is commented out for now because the test will fail after an initial run.
// ResourceManagerRemote currently does not provide an operation for committing resources back into inventory; so, once
// this test deletes resources, they are gone for good.
//
function testUninventoryResources() {
    criteria = ResourceCriteria();
    criteria.addFilterResourceTypeName('service-beta');
    criteria.addFilterParentResourceName('server-omega-1');

    resources = ResourceManager.findResourcesByCriteria(criteria);

    Assert.assertNumberEqualsJS(resources.size(), 10, "Expected to get back 10 service-beta services.");

    var resourceIds = [];
    for (i = 0; i < resources.size(); ++i) {
        resourceIds.push(resources.get(i).id);
    }

    ResourceManager.uninventoryResources(resourceIds);

    criteria.addFilterInventoryStatus(InventoryStatus.UNINVENTORIED);

    resources = ResourceManager.findResourcesByCriteria(criteria);

    for (i = 0; i < resources.size(); ++i) {
        resource = resources.get(i);
        Assert.assertEquals(resource.inventoryStatus, InventoryStatus.UNINVENTORIED,
                "The resource should have been uninventoried");
    }
}

function assertResourcesFound(msg) {
    if (msg == undefined) {
        msg =  "";
    }
    else {
        msg = msg + " - ";
    }

    Assert.assertNotNull(resources, msg + "Expected findResourcesByCriteria() to return a non-null result");
    Assert.assertTrue(resources.size() > 0, msg + "Expected findResourcesByCriteria() to return a non-empty result set");

    for (i = 0; i < resources.size(); ++i) {
        resource = resources.get(i);

        Assert.assertNotNull(ResourceManager.getResource(resource.id),
             'Expected getResourceTypeById to a return a ResourceType for id ' + resource.id);
    }
}

function assertResourcesSorted(ascResources, descResources, sortProperties) {
    Assert.assertNumberEqualsJS(ascResources.size(), descResources.size(), "The same number of resources should be " +
            "returned for the ASC and DESC sorts. " + ascResources.size() + " were returned for the ASC sort and " +
            descResources.size() + " were returned for the DESC sort.");

    var size = ascResources.size();
    var sortedCorrectly = true;
    var diff = null;

    for (i = 0, j = size - 1; i < size && j >= 0; ++i, --j) {
        if (!ascResources.get(i).equals(descResources.get(j))) {
            diff = "Difference found at ascResources[" + i + "] and descResources[" + j + "]\n" +
            "ascResources[" + i + "] = " + ascResources.get(i) + "\n" +
            "descResources[" + j + "] = " + descResources.get(j);
            sortedCorrectly = false;
            break;
        }
    }

    Assert.assertTrue(sortedCorrectly, "Failed to sort resources by " + sortProperties + ".\n" + diff + "\nResources " +
            "in ascending order:\n" + ascResources + "\n\nResources in descending order:\n" + descResources + "\n");
}

function assertSingleResourceReturned(resources) {
    Assert.assertNotNull(resources, "resources should not be null");
    Assert.assertNumberEqualsJS(resources.size(), 1, "Expceted to get back a single resource but " + resources.size() +
            " were returned");
}

function assertPropertyLoaded(resource, propertyName) {
    Assert.assertNotNull(resource[propertyName], "resource." + propertyName + " should have been fetched and loaded");
}

