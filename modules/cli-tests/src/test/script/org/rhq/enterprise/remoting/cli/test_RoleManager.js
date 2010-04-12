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
    var criteria = RoleCriteria();
    criteria.setFilterName('Super User Role');
    criteria.setFilterDescription('System superuser role that provides full access to everything. This role cannot be modified.');

    var roles = RoleManager.findRolesByCriteria(criteria);

    Assert.assertNumberEqualsJS(roles.size(), 1, 'Failed to find role when filtering');
}

function testFindWithFetchingAssociations() {
    var criteria = RoleCriteria();
    criteria.setFilterName('Super User Role');
    criteria.setFetchSubjects(true);
    criteria.setFetchResourceGroups(true);
    criteria.setFetchPermissions(true);
    criteria.setFetchRoleNotifications(true);

    var roles = RoleManager.findRolesByCriteria(criteria);

    Assert.assertNumberEqualsJS(roles.size(), 1, 'Failed to find role when fetching associations');
}

function testFindWithSorting() {
    var criteria = RoleCriteria();
    criteria.setFilterName('Super User Role');
    criteria.setSortName(PageOrdering.ASC);

    var roles = RoleManager.findRolesByCriteria(criteria);

    Assert.assertTrue(roles.size() > 0, 'Failed to find roles when sorting');
}

function testCreateUpdateDelete() {
    var role = Role('Test Role - ' + java.util.Date());
    role.description = 'This role is for testing only';
    role.addPermission(Permission.MANAGE_INVENTORY);
    role.addPermission(Permission.MANAGE_ALERTS);

    var savedRole = RoleManager.createRole(role);

    Assert.assertTrue(savedRole.id > 0, 'Failed to save/create role');

    savedRole.addPermission(Permission.MANAGE_MEASUREMENTS);

    var updatedRole = RoleManager.updateRole(savedRole);

    //Assert.assertEqualsNoOrder(updatedRole.permissions, savedRole.permissions, 'Failed to update role permissions');

    RoleManager.deleteRoles([updatedRole.id]);

    var deletedRole = RoleManager.getRole(updatedRole.id);

    Assert.assertNull(deletedRole, 'Failed to delete role');
}