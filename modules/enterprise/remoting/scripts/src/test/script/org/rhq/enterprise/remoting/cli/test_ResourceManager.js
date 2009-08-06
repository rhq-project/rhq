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

criteria = ResourceCriteria();
resources = null;

for (func in this) {
    if (func.indexOf('test') == 0) {
        this[func]();
    }
}

function testFindUnfiltered() {
    resources = ResourceManager.findResourcesByCriteria(criteria);
      
    assertResourcesFound();
}

function testFindByCriteria() {
    var resourceName = "service-alpha-0";
    var parentResourceName = "server-omega-0";
    var description = resourceName + " description";
    var version = "1.0";

    criteria = ResourceCriteria();
    criteria.caseSensitive = true;
    criteria.addFilterName(resourceName);
    criteria.addFilterParentResourceName(parentResourceName);

    criteria.addFilterDescription(description);
    criteria.addFilterVersion(version);
    criteria.addFilterPluginName("PerfTest");
    criteria.addFilterResourceKey(resourceName);
    criteria.addFilterAgentName("localhost.localdomain");

    criteria.fetchAgent(true);
    criteria.fetchAlertDefinitions(true);
    criteria.fetchResourceType(true);
    criteria.fetchChildResources(true);
    criteria.fetchParentResource(true);
    criteria.fetchResourceConfiguration(true);
    criteria.fetchResourceErrors(true);

    resources = ResourceManager.findResourcesByCriteria(criteria);

    assertSingleResourceReturned(resources);

    var resource = resources.get(0);

    assertPropertyLoaded(resource, "agent");
    assertPropertyLoaded(resource, "resourceType");
    assertPropertyLoaded(resource, "parentResource");
    assertPropertyLoaded(resource, "resourceConfiguration");
    assertPropertyLoaded(resource, "childResources");
    assertPropertyLoaded(resource, "currentAvailability");
    assertPropertyLoaded(resource, "resourceErrors");
}

function testFindByCriteriaWithSorting() {
    criteria = ResourceCriteria();
    criteria.addFilterParentResourceName("server-omega-0");
    criteria.addFilterResourceTypeName('service-beta');
    criteria.addSortName(PageOrdering.ASC);

    var size = 10;

    var ascResources = ResourceManager.findResourcesByCriteria(criteria);

    Assert.assertNumberEqualsJS(ascResources.size(), size, "Expected to get back " + size + " service-beta services.");

    criteria.addSortName(PageOrdering.DESC);

    var descResources = ResourceManager.findResourcesByCriteria(criteria);

    Assert.assertNumberEqualsJS(descResources.size(), 10, "Expected to get back " + size + " service-beta services.");
}

function assertResourcesSorted(ascResources, descResources, sortProperties) {
    var sortedCorrectly = true;
    for (i = 0, j = 9; i < size && j >= 0; ++i, --j) {
        if (!ascResources.get(i).equals(descResources.get(j))) {
            sortedCorrectly = false;
            break;
        }
    }

    Assert.assertTrue(sortedCorrectly, "Failed to sort resources by name.\nResources in ascending order:\n" +
            ascResources + "\n\nResources in descending order:\n" + descResources + "\n");

}

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

// testUninventoryResources() is commented out for now because the test will fail after an initial run.
// ResourceManagerRemote currently does not provide an operation for committing resources back into inventory; so, once
// this test deletes resources, they are gone for good.
//
//function testUninventoryResources() {
//    criteria = ResourceCriteria();
//    criteria.addFilterResourceTypeName('service-beta');
//    criteria.addFilterParentResourceName('server-omega-1');
//
//    resources = ResourceManager.findResourcesByCriteria(criteria);
//
//    Assert.assertNumberEqualsJS(resources.size(), 10, "Expected to get back 10 service-beta services.");
//
//    var resourceIds = [];
//    for (i = 0; i < resources.size(); ++i) {
//        resourceIds.push(resources.get(i).id);
//    }
//
//    ResourceManager.uninventoryResources(resourceIds);
//
//    criteria.addFilterInventoryStatus(InventoryStatus.UNINVENTORIED);
//
//    resources = ResourceManager.findResourcesByCriteria(criteria);
//
//    for (i = 0; i < resources.size(); ++i) {
//        resource = resources.get(i);
//        Assert.assertEquals(resource.inventoryStatus, InventoryStatus.UNINVENTORIED,
//                "The resource should have been uninventoried");
//    }
//}

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

function assertSingleResourceReturned(resources) {
    Assert.assertNotNull(resources, "resources should not be null");
    Assert.assertNumberEqualsJS(resources.size(), 1, "Expceted to get back a single resource but " + resources.size() +
            " were returned");
}

function assertPropertyLoaded(resource, propertyName) {
    Assert.assertNotNull(resource[propertyName], "resource." + propertyName + " should have been fetched and loaded");
}

