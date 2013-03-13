/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.authz.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.collection.ArrayUtils;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.resource.group.LdapGroupManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;

/**
 * Tests the role manager.
 */
@Test
public class RoleManagerBeanTest extends AbstractEJB3Test {
    private SubjectManagerLocal subjectManager;
    private RoleManagerLocal roleManager;
    private LdapGroupManagerLocal ldapManager;

    @Override
    protected void beforeMethod() {
        roleManager = LookupUtil.getRoleManager();
        subjectManager = LookupUtil.getSubjectManager();
        ldapManager = LookupUtil.getLdapGroupManager();
    }

    /**
     * Tests getting all roles.
     */
    public void testGetAllRoles() {
        assert roleManager.findRoles(PageControl.getUnlimitedInstance()).size() > 0;
    }

    /**
     * Tests getting all roles for a user.
     *
     * @throws Exception
     */
    public void testGetSubjectRoles() throws Exception {
        getTransactionManager().begin();

        try {
            PageList<Role> roles = roleManager.findRolesBySubject(subjectManager.getOverlord().getId(),
                PageControl.getUnlimitedInstance());
            assert roles.size() == 1;

            Role role = roles.get(0);
            assert role.getName().equals("Super User Role") : "Superuser should have only the super user role: " + role;
            assert role.getPermissions().contains(Permission.MANAGE_SECURITY);
            assert role.getPermissions().contains(Permission.MANAGE_INVENTORY);
            assert role.getPermissions().contains(Permission.MANAGE_SETTINGS);
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Tests getting all subjects for a particular role.
     *
     * @throws Exception
     */
    public void testGetRoleSubjects() throws Exception {
        getTransactionManager().begin();

        try {
            PageList<Role> roles = roleManager.findRolesBySubject(subjectManager.getOverlord().getId(),
                PageControl.getUnlimitedInstance());

            for (Role role : roles) {
                PageList<Subject> subjects = roleManager.findSubjectsByRole(role.getId(),
                    PageControl.getUnlimitedInstance());
                if (role.getName().equals("Super User Role")) {
                    assert subjects.size() >= 1 : "At least rhqadmin must have super user role: " + subjects;

                    for (Subject s : subjects) {
                        if ((s.getId() == 1) || s.getName().equals("admin")) {
                            assert false : "getRoleSubjects() is not supposed to return the superuser" + s;
                        }
                    }
                }
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Tests getting roles by their IDs.
     *
     * @throws Exception
     */
    public void testGetRolesByIds() throws Exception {
        getTransactionManager().begin();
        try {
            PageList<Role> all_roles = roleManager.findRoles(PageControl.getUnlimitedInstance());
            List<Integer> role_id_list = new ArrayList<Integer>(all_roles.size());
            for (Role role : all_roles) {
                role_id_list.add(role.getId());
            }

            PageList<Role> roles = roleManager.findRolesByIds(role_id_list.toArray(new Integer[role_id_list.size()]),
                PageControl.getUnlimitedInstance());
            assert roles.size() == all_roles.size();
            assert roles.containsAll(all_roles);
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Tests getting roles that are not already assigned to a subject and not part of an exclusion list of roles.
     *
     * @throws Exception
     */
    public void testGetAvailableRolesForSubject() throws Exception {
        getTransactionManager().begin();
        try {
            Subject superuser = subjectManager.getOverlord();
            superuser = createSession(superuser);

            Subject subject = new Subject();
            subject.setName("dummy-subject");
            subject = subjectManager.createSubject(superuser, subject);
            subject = createSession(subject);

            Role new_role = new Role("dummy-role");
            Role new_role2 = new Role("dummy-role2");
            new_role = roleManager.createRole(superuser, new_role);
            new_role2 = roleManager.createRole(superuser, new_role2);

            PageList<Role> all_roles = roleManager.findRoles(PageControl.getUnlimitedInstance());
            assert all_roles.size() >= 3 : "There should at least be the two dummy roles and the super user role";

            PageList<Role> roles;

            roles = roleManager.findAvailableRolesForSubject(superuser, subject.getId(), new Integer[0],
                PageControl.getUnlimitedInstance());

            assert roles.size() == all_roles.size() : "All roles should be available for this subject";
            assert roles.containsAll(all_roles);

            roles = roleManager.findAvailableRolesForSubject(superuser, subject.getId(),
                new Integer[] { new_role.getId() }, PageControl.getUnlimitedInstance());
            assert (roles.size() + 1) == all_roles.size() : "All roles but one should be available for this subject";
            assert !roles.contains(new_role);

            roles = roleManager.findAvailableRolesForSubject(superuser, subject.getId(),
                new Integer[] { new_role.getId(), new_role2.getId() }, PageControl.getUnlimitedInstance());
            assert (roles.size() + 2) == all_roles.size() : "All roles but two should be available for this subject";
            assert !roles.contains(new_role);
            assert !roles.contains(new_role2);

            roleManager.addRolesToSubject(superuser, subject.getId(), new int[] { new_role.getId() });
            roles = roleManager.findAvailableRolesForSubject(superuser, subject.getId(), new Integer[0],
                PageControl.getUnlimitedInstance());
            assert (roles.size() + 1) == all_roles.size() : "All but one role should be available for this subject";
            assert !roles.contains(new_role) : "We already assigned this new role to the subject - it isn't available";

            roles = roleManager.findAvailableRolesForSubject(superuser, subject.getId(),
                new Integer[] { new_role2.getId() }, PageControl.getUnlimitedInstance());
            assert (roles.size() + 2) == all_roles.size() : "One is already assigned and one is excluded so all but two roles should be available for this subject";
            assert !roles.contains(new_role) : "We already assigned this new role to the subject - it isn't available";
            assert !roles.contains(new_role2) : "We excluded this new role - it isn't available";

            try {
                // dummy user doesn't have the permission for this
                roleManager.findAvailableRolesForSubject(subject, subject.getId(), new Integer[0],
                    PageControl.getUnlimitedInstance());
            } catch (PermissionException s) {
                // to be expected, this rolls the transaction back for us
            }
        } finally {
            try {
                getTransactionManager().rollback();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Test creating, assigning, removing and deleting roles.
     *
     * @throws Exception
     */
    public void testRoles() throws Exception {
        getTransactionManager().begin();

        try {
            Subject newSubject1 = new Subject();
            newSubject1.setName("role-manager-subject");
            newSubject1.setFsystem(false);

            Subject newSubject2 = new Subject();
            newSubject2.setName("secondary-role-manager");
            newSubject2.setFsystem(false);

            Role role = new Role("role-manager-role");
            role.setFsystem(false);
            role.addSubject(newSubject1);

            Subject superuser = subjectManager.getOverlord();
            subjectManager.createSubject(superuser, newSubject1);
            newSubject1 = createSession(newSubject1);

            subjectManager.createSubject(superuser, newSubject2);
            newSubject2 = createSession(newSubject2);

            assertEquals("Role should not be created or assigned yet", 0,
                roleManager.findRolesBySubject(newSubject1.getId(), PageControl.getUnlimitedInstance()).size());

            role = roleManager.createRole(superuser, role);
            assertEquals("Role should be assigned at the time the role is created", 1,
                roleManager.findRolesBySubject(newSubject1.getId(), PageControl.getUnlimitedInstance()).size());

            roleManager.addRolesToSubject(superuser, newSubject2.getId(), new int[] { role.getId() });
            assertEquals("Role should be assigned", 1,
                roleManager.findRolesBySubject(newSubject2.getId(), PageControl.getUnlimitedInstance()).size());

            roleManager.removeRolesFromSubject(superuser, newSubject1.getId(), new int[] { role.getId() });
            assertEquals("Role should have been unassigned", 0,
                roleManager.findRolesBySubject(newSubject1.getId(), PageControl.getUnlimitedInstance()).size());

            roleManager.deleteRoles(superuser, new int[] { role.getId() });
            assertFalse("Roles should have been deleted", roleManager.findRoles(PageControl.getUnlimitedInstance())
                .contains(role));
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Test creating, assigning, removing and deleting roles.
     *
     * @throws Exception
     */
    public void testLdapGroups() throws Exception {
        getTransactionManager().begin();

        try {
            Subject superuser = subjectManager.getOverlord();
            superuser = createSession(superuser);

            Role role = new Role("role-manager-role");
            role.setFsystem(false);
            role = roleManager.createRole(superuser, role);
            LdapGroup group = new LdapGroup();
            group.setName("Foo");
            role.addLdapGroup(group);
            assert ldapManager.findLdapGroupsByRole(role.getId(), PageControl.getUnlimitedInstance()).size() == 1 : "Ldap Group Foo Should be assigned";
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testSetAssignedSubjectRoles() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Subject subject = SessionTestHelper.createNewSubject(em, "fake subject");
                List<Role> roles = new ArrayList<Role>();
                for (int i = 0; i < 17; i++) {
                    roles.add(SessionTestHelper.createNewRoleForSubject(em, subject, "fake role " + String.valueOf(i)));
                }
                List<Role> rolesToKeep = roles.subList(5, 14);
                int[] rolesToKeepIds = new int[rolesToKeep.size()];
                for (int i = 0; i < rolesToKeep.size(); i++) {
                    rolesToKeepIds[i] = rolesToKeep.get(i).getId();
                }
                roleManager.setAssignedSubjectRoles(subjectManager.getOverlord(), subject.getId(), rolesToKeepIds);
                assertTrue(subject.getRoles().containsAll(rolesToKeep) && rolesToKeep.containsAll(subject.getRoles()));
            }
        });
    }

    @Test
    public void testGetPermissions() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Subject subject = SessionTestHelper.createNewSubject(em, "fake subject");
                Set<Permission> permissions = new HashSet<Permission>();
                permissions.addAll(EnumSet.allOf(Permission.class));
                Role role = SessionTestHelper.createNewRoleForSubject(em, subject, "fake role",
                    permissions.toArray(new Permission[permissions.size()]));
                Set<Permission> foundPermissions = roleManager.getPermissions(role.getId());
                assertEquals(permissions, foundPermissions);
            }
        });
    }

    @Test
    public void testUpdateRole() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Subject subject = SessionTestHelper.createNewSubject(em, "fake subject");
                Set<Permission> originalPermissions = new HashSet<Permission>();
                originalPermissions.addAll(EnumSet.allOf(Permission.class));
                Role originalRole = SessionTestHelper.createNewRoleForSubject(em, subject, "fake role",
                    originalPermissions.toArray(new Permission[originalPermissions.size()]));
                ResourceGroup resourceGroup = SessionTestHelper.createNewCompatibleGroupForRole(em, originalRole,
                    "fake group");

                Role role = new Role();
                role.setId(originalRole.getId());
                role.setName("fake role name modified");
                role.setDescription("fake role description modified");
                Set<Permission> permissions = new HashSet<Permission>(originalRole.getPermissions());
                permissions.remove(Permission.MANAGE_SECURITY);
                permissions.remove(Permission.MANAGE_INVENTORY);
                permissions.remove(Permission.MANAGE_DRIFT);
                role.setPermissions(permissions);
                Subject subject1 = SessionTestHelper.createNewSubject(em, "fake subject 1");
                role.getSubjects().add(subject1);
                Subject subject2 = SessionTestHelper.createNewSubject(em, "fake subject 2");
                role.getSubjects().add(subject2);
                role.getSubjects().remove(subject);
                ResourceGroup resourceGroup1 = SessionTestHelper.createNewCompatibleGroupForRole(em, originalRole,
                    "fake group 1");
                role.getResourceGroups().add(resourceGroup1);
                ResourceGroup resourceGroup2 = SessionTestHelper.createNewCompatibleGroupForRole(em, originalRole,
                    "fake group 2");
                role.getResourceGroups().add(resourceGroup2);
                role.getResourceGroups().remove(resourceGroup);

                role = roleManager.updateRole(subjectManager.getOverlord(), role);

                assertEquals("fake role name modified", role.getName());
                assertEquals("fake role description modified", role.getDescription());
                assertEquals(permissions, role.getPermissions());
                List<Subject> expectedSubjects = Arrays.asList(subject1, subject2);
                assertTrue(role.getSubjects().containsAll(expectedSubjects)
                    && expectedSubjects.containsAll(role.getSubjects()));
                List<ResourceGroup> expectedResourceGroups = Arrays.asList(resourceGroup1, resourceGroup2);
                assertTrue(role.getResourceGroups().containsAll(expectedResourceGroups)
                    && expectedResourceGroups.containsAll(role.getResourceGroups()));
            }
        });
    }

    @Test
    public void testSetAssignedResourceGroups() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Subject subject = SessionTestHelper.createNewSubject(em, "fake subject");
                Role role = SessionTestHelper.createNewRoleForSubject(em, subject, "fake role");
                List<ResourceGroup> resourceGroups = new ArrayList<ResourceGroup>();
                for (int i = 0; i < 17; i++) {
                    resourceGroups.add(SessionTestHelper.createNewCompatibleGroupForRole(em, role, "fake group "
                        + String.valueOf(i)));
                }
                List<ResourceGroup> resourceGroupsToKeep = resourceGroups.subList(5, 14);
                int[] resourceGroupsToKeepIds = new int[resourceGroupsToKeep.size()];
                for (int i = 0; i < resourceGroupsToKeep.size(); i++) {
                    resourceGroupsToKeepIds[i] = resourceGroupsToKeep.get(i).getId();
                }
                roleManager.setAssignedResourceGroups(subjectManager.getOverlord(), role.getId(),
                    resourceGroupsToKeepIds);
                assertTrue(role.getResourceGroups().containsAll(resourceGroupsToKeep)
                    && resourceGroupsToKeep.containsAll(role.getResourceGroups()));
            }
        });
    }

    @Test
    public void testSetAssignedSubjects() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Subject subject = SessionTestHelper.createNewSubject(em, "fake subject");
                Role role = SessionTestHelper.createNewRoleForSubject(em, subject, "fake role");
                List<Subject> subjects = new ArrayList<Subject>();
                for (int i = 0; i < 17; i++) {
                    subjects.add(SessionTestHelper.createNewSubject(em, "fake subject " + String.valueOf(i)));
                }
                List<Subject> subjectsToKeep = subjects.subList(5, 14);
                int[] subjectsToKeepIds = new int[subjectsToKeep.size()];
                for (int i = 0; i < subjectsToKeep.size(); i++) {
                    subjectsToKeepIds[i] = subjectsToKeep.get(i).getId();
                }
                roleManager.setAssignedSubjects(subjectManager.getOverlord(), role.getId(), subjectsToKeepIds);
                assertTrue(role.getSubjects().containsAll(subjectsToKeep)
                    && subjectsToKeep.containsAll(role.getSubjects()));
            }
        });
    }

    @Test
    public void testSetRolesFromResourceGroup() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Subject subject = SessionTestHelper.createNewSubject(em, "fake subject");
                Role role = SessionTestHelper.createNewRoleForSubject(em, subject, "fake role");
                ResourceGroup resourceGroup = SessionTestHelper.createNewCompatibleGroupForRole(em, role,
                    "fake resource group");
                List<Integer> rolesIds = new LinkedList<Integer>();
                List<Integer> rolesToRemoveIds = new LinkedList<Integer>();
                List<Role> rolesToKeep = new LinkedList<Role>();
                for (int i = 0; i < 17; i++) {
                    Role newRole = SessionTestHelper.createNewRoleForSubject(em, subject,
                        "fake role " + String.valueOf(i));
                    rolesIds.add(newRole.getId());
                    if (i < 5 || i >= 14) {
                        rolesToRemoveIds.add(newRole.getId());
                    } else {
                        rolesToKeep.add(newRole);
                    }
                }
                rolesToRemoveIds.add(role.getId());
                roleManager.addRolesToResourceGroup(subjectManager.getOverlord(), resourceGroup.getId(),
                    ArrayUtils.unwrapCollection(rolesIds));
                roleManager.removeRolesFromResourceGroup(subjectManager.getOverlord(), resourceGroup.getId(),
                    ArrayUtils.unwrapCollection(rolesToRemoveIds));

                List<Role> expected = rolesToKeep;
                Set<Role> actual = resourceGroup.getRoles();
                assertTrue("Expected " + expected + " but was " + actual,
                    actual.containsAll(expected) && expected.containsAll(actual));
            }
        });
    }
}
