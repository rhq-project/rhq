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

/**
 * These tests assume that there is a jopr server and an agent running on localhost.
 */

rhq.login('rhqadmin', 'rhqadmin');

skippedTests.push('testUninventoryResources');

// The following two tests are failing at present due to a known issue in the generated query that results in this
// exception,
//
//    org.hibernate.HibernateException: cannot simultaneously fetch multiple bags
//
skippedTests.push('testFindWithFilteringAndSortingAndFetchingAssociations');
skippedTests.push('testFindWithFilteringAndFetchingAssociations');

executeAllTests();

function testFindUnfiltered() {
    var resources = ResourceManager.findResourcesByCriteria(ResourceCriteria());

    Assert.assertNotNull(resources, 'Expected non-null results for criteria search.');
    Assert.assertTrue(resources.size() > 0, 'Expected non-empty result list.');
}

function testFindWithFiltering() {
    var criteria = createCriteria();
    criteria.strict = true;
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

function testFindWithFilteringAndSortingAndFetchingAssociations() {
    var criteria = ResourceCriteria();
    criteria.caseSensitive = true;
    criteria.addFilterParentResourceName('server-omega-0');
    criteria.addFilterResourceTypeName('service-beta');

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

    criteria.addSortName(PageOrdering.DESC);
    criteria.addSortResourceTypeName(PageOrdering.ASC);
    criteria.addSortInventoryStatus(PageOrdering.DESC);
    criteria.addSortVersion(PageOrdering.DESC);
    criteria.addSortResourceCategory(PageOrdering.ASC)

    var resources = ResourceManager.findResourcesByCriteria(criteria);

    Assert.assertTrue(resources.size() > 0, 'Expected get resources when filtering, sorting, and fetching associations.');
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
