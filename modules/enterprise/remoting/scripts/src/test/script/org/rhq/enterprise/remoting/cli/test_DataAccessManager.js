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

var query =
            "SELECT r " +
            "FROM Resource r " +
            "WHERE ( r.inventoryStatus = org.rhq.core.domain.resource.InventoryStatus.COMMITTED " +
            "AND LOWER( r.resourceType.name ) like 'service-alpha' " +
            "AND LOWER( r.parentResource.name ) like 'server-omega-0')";

rhq.login('rhqadmin', 'rhqadmin');

executeAllTests();

rhq.logout();

function testExecuteQuery() {
    var resources = DataAccessManager.executeQuery(query);

    Assert.assertNumberEqualsJS(resources.size(), 10, "Expected to get back 10 resources");
}

function testExecuteQueryWithPaging() {
    var pageControl = PageControl();
    pageControl.pageNumber = 0;
    pageControl.pageSize = 5;
    pageControl.setPrimarySort('name', PageOrdering.ASC);

    var resources = DataAccessManager.executeQueryWithPageControl(query, pageControl);

    Assert.assertNumberEqualsJS(resources.size(), 5, "Failed to fetch first page of resources");

    Assert.assertEquals(resources.get(0).name, 'service-alpha-0', 'Failed to sort first page in ascending order');
    Assert.assertEquals(resources.get(4).name, 'service-alpha-4', 'Failed to sort first page in ascending order');

    pageControl.pageNumber = 1;
    resources = DataAccessManager.executeQueryWithPageControl(query, pageControl);

    Assert.assertNumberEqualsJS(resources.size(), 5, "Failed to fetch second page of resources");
    Assert.assertEquals(resources.get(0).name, 'service-alpha-5', 'Failed to sort second page in ascending order');
    Assert.assertEquals(resources.get(4).name, 'service-alpha-9', 'Failed to sort second page in ascending order');
}