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

function testFindWithFiltering() {
    var resourceType = ResourceTypeManager.getResourceTypeByNameAndPlugin('service-alpha', 'PerfTest');

    var criteria = ResourceTypeCriteria();
    criteria.addFilterName('service-alpha');
    criteria.addFilterDescription(resourceType.description);
    criteria.addFilterCategory(ResourceCategory.SERVICE);
    criteria.addFilterPluginName('PerfTest');
    criteria.addFilterCreationDataType(ResourceCreationDataType.CONFIGURATION);
    criteria.addFilterCreateDeletePolicy(CreateDeletePolicy.NEITHER);
    criteria.addFilterSupportsManualAdd(false);

    var resourceTypes = ResourceTypeManager.findResourceTypesByCriteria(criteria);

    Assert.assertNumberEqualsJS(resourceTypes.size(), 1, 'Failed to find resource type when filtering');
}

function testFindWithFetchingAssociations() {
    var resourceType = ResourceTypeManager.getResourceTypeByNameAndPlugin('service-alpha', 'PerfTest');

    var criteria = ResourceTypeCriteria();
    criteria.addFilterId(resourceType.id);
    criteria.fetchSubCategory(true);
    criteria.fetchChildResourceTypes(true);
    criteria.fetchParentResourceTypes(true);
    criteria.fetchPluginConfigurationDefinition(true);
    criteria.fetchResourceConfigurationDefinition(true);
    criteria.fetchMetricDefinitions(true);
    criteria.fetchEventDefinitions(true);
    criteria.fetchOperationDefinitions(true);
    criteria.fetchProcessScans(true);
    criteria.fetchPackageTypes(true);
    criteria.fetchSubCategories(true);
    criteria.fetchProductVersions(true);

    var resourceTypes = ResourceTypeManager.findResourceTypesByCriteria(criteria);

    Assert.assertNumberEqualsJS(resourceTypes.size(), 1, 'Failed to find resource type when fetching associations');
}

function testFindWithSorting() {
    var criteria = ResourceTypeCriteria();
    criteria.addSortName(PageOrdering.ASC);
    criteria.addSortCategory(PageOrdering.DESC);
    criteria.addSortPluginName(PageOrdering.ASC);

    var resourceTypes = ResourceTypeManager.findResourceTypesByCriteria(criteria);

    Assert.assertTrue(resourceTypes.size() > 0, 'Failed to find resource types when sorting');
}

