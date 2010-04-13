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
    var resourceType = findResourceType();

    var criteria = MeasurementDefinitionCriteria();
    criteria.addFilterName('alpha-metric0');
    criteria.addFilterDisplayName('Alpha Metric 0');
    criteria.addFilterDescription('Alpha Metric 0');
    criteria.addFilterResourceTypeName(resourceType.name);
    criteria.addFilterResourceTypeId(resourceType.id);
    criteria.addFilterCategory(MeasurementCategory.PERFORMANCE);
    criteria.addFilterNumericType(NumericType.DYNAMIC);
    criteria.addFilterDataType(DataType.MEASUREMENT);
    criteria.addFilterDisplayType(DisplayType.DETAIL);
    criteria.addFilterDefaultOn(false);
    criteria.addFilterDefaultInterval(1200000);
    

    var measurementDefs = MeasurementDefinitionManager.findMeasurementDefinitionsByCriteria(criteria);

    Assert.assertNumberEqualsJS(measurementDefs.size(), 1, 'Failed to find measurement definition when filtering');
}

function testFindWithSorting() {
    var criteria = MeasurementDefinitionCriteria();
    criteria.addFilterResourceTypeName('service-alpha');
    criteria.addSortName(PageOrdering.ASC);
    criteria.addSortDisplayName(PageOrdering.DESC);
    criteria.addSortResourceTypeName(PageOrdering.ASC);
    criteria.addSortCategory(PageOrdering.DESC);
    criteria.addSortUnits(PageOrdering.ASC);
    criteria.addSortNumericType(PageOrdering.DESC);
    criteria.addSortDataType(PageOrdering.ASC);
    criteria.addSortDisplayType(PageOrdering.DESC);
    criteria.addSortDefaultOn(PageOrdering.ASC);
    criteria.addSortDefaultInterval(PageOrdering.DESC);

    var measurementDefs = MeasurementDefinitionManager.findMeasurementDefinitionsByCriteria(criteria);

    Assert.assertTrue(measurementDefs.size() > 0, 'Failed to find measurement definitions when sorting');
}

function findResourceType() {
    var criteria = ResourceTypeCriteria();
    criteria.addFilterName('service-alpha');
    criteria.addFilterPluginName('PerfTest');

    return ResourceTypeManager.findResourceTypesByCriteria(criteria).get(0);
}