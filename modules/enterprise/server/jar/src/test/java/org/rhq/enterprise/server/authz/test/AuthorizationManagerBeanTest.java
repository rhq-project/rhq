/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;

@Test
public class AuthorizationManagerBeanTest extends AbstractEJB3Test {
    private AuthorizationManagerLocal authorizationManager;

    @BeforeMethod
    @SuppressWarnings( { "unused" })
    private void init() {
        try {
            authorizationManager = LookupUtil.getAuthorizationManager();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    /*
     * Test methods:   getGlobalPermissions(Subject)   hasGlobalPermission(Subject, Permission)
     */
    @Test(groups = "integration.session")
    public void testGlobalPermissions() throws Exception {
        /*
         * accuracy test: all global permissions applied to a single role
         *
         *          p (all & only global perms)            | subject -- role
         */
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            /* bootstrap */
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            Role testRole1 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole1");

            /* setup permissions */
            Set<Permission> globalPermissions = SessionTestHelper.getAllGlobalPerms();
            testRole1.getPermissions().addAll(globalPermissions);
            em.merge(testRole1);
            em.flush();

            /* verify getGlobalPermissions test */
            Set<Permission> globalResults = authorizationManager.getExplicitGlobalPermissions(testSubject);
            assert SessionTestHelper.samePermissions(globalPermissions, globalResults) : "Failed to get 1-subject, 1-role global permissions";

            /* verify hasGlobalPermission test */
            for (Permission permission : globalPermissions) {
                assert (authorizationManager.hasGlobalPermission(testSubject, permission)) : "Failed to get global permission "
                    + permission.toString();
            }
        } finally {
            getTransactionManager().rollback();
        }

        /*
         * accuracy test: redundant global permissions in 2 roles
         *
         *          p (all & only global perms)            | subject -- role       \        \- role            |
         *    p (all & only global perms)
         */
        getTransactionManager().begin();
        em = getEntityManager();
        try {
            /* bootstrap */
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            Role testRole1 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole1");
            Role testRole2 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole2");

            /* setup permissions */
            Set<Permission> globalPermissions = SessionTestHelper.getAllGlobalPerms();
            testRole1.getPermissions().addAll(globalPermissions);
            testRole2.getPermissions().addAll(globalPermissions);
            em.merge(testRole1);
            em.merge(testRole2);
            em.flush();

            /* verify getGlobalPermissions test */
            Set<Permission> globalResults = authorizationManager.getExplicitGlobalPermissions(testSubject);
            assert SessionTestHelper.samePermissions(globalPermissions, globalResults) : "Failed to get 1-subject, 2-role distinct global permissions";

            /* verify hasGlobalPermission test */
            for (Permission permission : globalPermissions) {
                assert (authorizationManager.hasGlobalPermission(testSubject, permission)) : "Failed to get global permission "
                    + permission.toString();
            }
        } finally {
            getTransactionManager().rollback();
        }

        /*
         * negative test: no global perms on either role
         *
         *          p (no global perms)            | subject -- role       \        \- role            |            p
         * (no global perms)
         */
        getTransactionManager().begin();
        em = getEntityManager();
        try {
            /* bootstrap */
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            Role testRole1 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole1");
            Role testRole2 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole2");

            /* setup permissions */
            testRole1.getPermissions().add(Permission.CONTROL);
            testRole2.getPermissions().add(Permission.CONFIGURE_WRITE);
            em.merge(testRole1);
            em.merge(testRole2);
            em.flush();

            /* verify getGlobalPermissions test */
            Set<Permission> noPermissions = EnumSet.noneOf(Permission.class);
            Set<Permission> noResults = authorizationManager.getExplicitGlobalPermissions(testSubject);
            assert SessionTestHelper.samePermissions(noPermissions, noResults) : "Failed by getting resource permissions as global permissions";

            /* verify hasGlobalPermission test */
            Set<Permission> globalPermissions = SessionTestHelper.getAllGlobalPerms();
            for (Permission permission : globalPermissions) {
                assert (!authorizationManager.hasGlobalPermission(testSubject, permission)) : "Failed by  a resource permission as a global permission "
                    + permission.toString();
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    /*
     * Test methods:   getGroupPermissions(Subject, ResourceGroup)   hasGroupPermission(Subject, Permission,
     * ResourceGroup)
     */
    @Test(groups = "integration.session")
    public void testGroupPermissions() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        /*
         * accuracy test: simplest case
         *
         *          p (mixed permissions)            | subject -- role -- group
         */
        try {
            /* bootstrap */
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            Role testRole1 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole1");
            ResourceGroup testGroup1 = SessionTestHelper.createNewMixedGroupForRole(em, testRole1, "testGroup1", false);

            /* setup permissions */
            Set<Permission> someMixedPermissions = EnumSet.of(Permission.CONFIGURE_WRITE, Permission.CONTROL,
                Permission.MANAGE_SECURITY);
            testRole1.getPermissions().addAll(someMixedPermissions);
            em.merge(testRole1);
            em.flush();

            /* verify getGroupPermissions test */
            Set<Permission> someMixedResults = authorizationManager.getExplicitGroupPermissions(testSubject, testGroup1
                .getId());
            assert SessionTestHelper.samePermissions(someMixedPermissions, someMixedResults) : "Failed to get all 1-role, 1-group permissions";

            /* verify hasGroupPermission test */
            for (Permission permission : someMixedPermissions) {
                assert (authorizationManager.hasGroupPermission(testSubject, permission, testGroup1.getId())) : "Failed to get single single 1-role, 1-group permission "
                    + permission.toString();
            }
        } finally {
            getTransactionManager().rollback();
        }

        /*
         * accuracy test: no overlap of permissions, groups should return different sets
         *
         *          p (some permissions)            | subject -- role -- group       \        \- role -- group
         *  |            p (other permissions)
         */
        getTransactionManager().begin();
        em = getEntityManager();
        try {
            /* bootstrap */
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            Role testRole1 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole1");
            ResourceGroup testGroup1 = SessionTestHelper.createNewMixedGroupForRole(em, testRole1, "testGroup1", false);

            Role testRole2 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole2");
            ResourceGroup testGroup2 = SessionTestHelper.createNewMixedGroupForRole(em, testRole2, "testGroup2", false);

            /* setup permissions */
            Set<Permission> someMixedPermissions = EnumSet.of(Permission.MANAGE_INVENTORY, Permission.MANAGE_CONTENT,
                Permission.DELETE_RESOURCE);
            testRole1.getPermissions().addAll(someMixedPermissions);
            em.merge(testRole1);
            em.flush();

            Set<Permission> otherMixedPermissions = EnumSet.of(Permission.CONFIGURE_WRITE, Permission.MANAGE_INVENTORY,
                Permission.MANAGE_SECURITY);
            testRole2.getPermissions().addAll(otherMixedPermissions);
            em.merge(testRole2);
            em.flush();

            /* verify getGroupPermissions test */
            Set<Permission> someMixedResults = authorizationManager.getExplicitGroupPermissions(testSubject, testGroup1
                .getId());
            assert SessionTestHelper.samePermissions(someMixedPermissions, someMixedResults) : "Failed to get all 2-role, 2-group permissions";

            /* verify hasGroupPermission test */
            for (Permission permission : someMixedPermissions) {
                assert (authorizationManager.hasGroupPermission(testSubject, permission, testGroup1.getId())) : "Failed to get single single 1-role, 1-group permission "
                    + permission.toString();
            }

            /* verify getGroupPermissions test */
            Set<Permission> otherMixedResults = authorizationManager.getExplicitGroupPermissions(testSubject,
                testGroup2.getId());
            assert SessionTestHelper.samePermissions(otherMixedPermissions, otherMixedResults) : "Failed to get all 2-role, 2-group permissions";

            /* verify hasGroupPermission test */
            for (Permission permission : otherMixedPermissions) {
                assert (authorizationManager.hasGroupPermission(testSubject, permission, testGroup2.getId())) : "Failed to get single single 1-role, 1-group permission "
                    + permission.toString();
            }
        } finally {
            getTransactionManager().rollback();
        }

        /*
         * accuracy test: disjoint sets of permissions, results should be the simple union
         *
         *          p            | subject -- role -- group       \          /        \- role -/            |
         * p
         */
        getTransactionManager().begin();
        em = getEntityManager();
        try {
            /* bootstrap */
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            Role testRole1 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole1");
            Role testRole2 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole2");
            ResourceGroup testGroup1 = SessionTestHelper.createNewMixedGroupForRole(em, testRole1, "testGroup1", false);

            testRole2.getResourceGroups().add(testGroup1);
            testGroup1.addRole(testRole2);
            em.merge(testRole2);
            em.merge(testGroup1);
            em.flush();

            /* setup permissions */
            Set<Permission> someMixedPermissions = EnumSet.of(Permission.CONFIGURE_WRITE, Permission.CONTROL,
                Permission.MANAGE_SECURITY);
            testRole1.getPermissions().addAll(someMixedPermissions);
            em.merge(testRole1);
            em.flush();

            Set<Permission> otherMixedPermissions = EnumSet.of(Permission.MANAGE_INVENTORY, Permission.MANAGE_CONTENT,
                Permission.DELETE_RESOURCE);
            testRole2.getPermissions().addAll(otherMixedPermissions);
            em.merge(testRole2);
            em.flush();

            /* setup shared object for result testing */
            Set<Permission> totalMixedPermissions = EnumSet.noneOf(Permission.class);
            totalMixedPermissions.addAll(someMixedPermissions);
            totalMixedPermissions.addAll(otherMixedPermissions);

            /* verify getGroupPermissions test */
            Set<Permission> totalMixedResults = authorizationManager.getExplicitGroupPermissions(testSubject,
                testGroup1.getId());
            assert SessionTestHelper.samePermissions(totalMixedPermissions, totalMixedResults) : "Failed to get all 1-role, 2-group permissions";

            /* verify hasGroupPermission test */
            for (Permission permission : totalMixedPermissions) {
                assert (authorizationManager.hasGroupPermission(testSubject, permission, testGroup1.getId())) : "Failed to get single single 1-role, 2-group permission "
                    + permission.toString();
            }
        } finally {
            getTransactionManager().rollback();
        }

        /*
         * accuracy test: intersecting sets of permissions, results should be the distinct union
         *
         *          p            | subject -- role -- group       \          /        \- role -/            |
         * p
         */
        getTransactionManager().begin();
        em = getEntityManager();
        try {
            /* bootstrap */
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            Role testRole1 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole1");
            Role testRole2 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole2");
            ResourceGroup testGroup1 = SessionTestHelper.createNewMixedGroupForRole(em, testRole1, "testGroup1", false);

            testRole2.getResourceGroups().add(testGroup1);
            testGroup1.addRole(testRole2);
            em.merge(testRole2);
            em.merge(testGroup1);
            em.flush();

            /* setup permissions */
            Set<Permission> someMixedPermissions = EnumSet.of(Permission.CONFIGURE_WRITE, Permission.CONTROL,
                Permission.MANAGE_SECURITY);
            testRole1.getPermissions().addAll(someMixedPermissions);
            em.merge(testRole1);
            em.flush();

            Set<Permission> otherMixedPermissions = EnumSet.of(Permission.MANAGE_INVENTORY, Permission.MANAGE_CONTENT,
                Permission.DELETE_RESOURCE);
            testRole2.getPermissions().addAll(otherMixedPermissions);
            em.merge(testRole2);
            em.flush();

            /* setup redundant permissions */
            testRole2.getPermissions().addAll(someMixedPermissions);
            em.merge(testRole2);
            em.flush();

            /* setup shared object for result testing */
            Set<Permission> totalMixedPermissions = EnumSet.noneOf(Permission.class);
            totalMixedPermissions.addAll(someMixedPermissions);
            totalMixedPermissions.addAll(otherMixedPermissions);

            /* verify getGroupPermissions test */
            Set<Permission> totalMixedDistinctResults = authorizationManager.getExplicitGroupPermissions(testSubject,
                testGroup1.getId());
            assert SessionTestHelper.samePermissions(totalMixedPermissions, totalMixedDistinctResults) : "Failed to get all 1-role, 2-group distinct permissions";

            /* verify hasGroupPermission test */
            for (Permission permission : totalMixedPermissions) {
                assert (authorizationManager.hasGroupPermission(testSubject, permission, testGroup1.getId())) : "Failed to get single single 1-role, 2-group distinct permission "
                    + permission.toString();
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testCanViewResourcesWhenSubjectIsInRole() throws Exception {
        getTransactionManager().begin();
        EntityManager entityMgr = getEntityManager();

        try {
            Subject subject = SessionTestHelper.createNewSubject(entityMgr, "testSubject");

            Role roleWithSubject = SessionTestHelper.createNewRoleForSubject(entityMgr, subject, "role with subject");
            roleWithSubject.addPermission(Permission.VIEW_RESOURCE);

            ResourceGroup group = SessionTestHelper.createNewCompatibleGroupForRole(entityMgr, roleWithSubject,
                "accessible group");

            Resource r1 = SessionTestHelper.createNewResourceForGroup(entityMgr, group, "r1");
            Resource r2 = SessionTestHelper.createNewResourceForGroup(entityMgr, group, "r2");

            entityMgr.flush();

            List<Integer> resourceIds = Arrays.asList(r1.getId(), r2.getId());

            assertTrue("The subject should have permission to view the resources", authorizationManager
                .canViewResources(subject, resourceIds));

        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testCanViewResourceWhenSubjectIsInMultipleRolesAndResourcesInMultipleGroups() throws Exception {
        getTransactionManager().begin();
        EntityManager entityMgr = getEntityManager();

        try {
            Subject subject = SessionTestHelper.createNewSubject(entityMgr, "testSubject");

            Role role1 = SessionTestHelper.createNewRoleForSubject(entityMgr, subject, "role1");
            role1.addPermission(Permission.VIEW_RESOURCE);

            Role role2 = SessionTestHelper.createNewRoleForSubject(entityMgr, subject, "role2");
            role2.addPermission(Permission.VIEW_RESOURCE);

            ResourceGroup group1 = SessionTestHelper.createNewCompatibleGroupForRole(entityMgr, role1, "group 1");
            ResourceGroup group2 = SessionTestHelper.createNewCompatibleGroupForRole(entityMgr, role2, "group 2");

            Resource r1 = SessionTestHelper.createNewResourceForGroup(entityMgr, group1, "r1");
            Resource r2 = SessionTestHelper.createNewResourceForGroup(entityMgr, group2, "r2");

            entityMgr.flush();

            List<Integer> resourceIds = Arrays.asList(r1.getId(), r2.getId());

            assertTrue(
                "The subject should have permission to view the resources in different groups since the subject is in roles for those groups",
                authorizationManager.canViewResources(subject, resourceIds));
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testCanViewResourcesWhenSubjectIsNotInRole() throws Exception {
        getTransactionManager().begin();
        EntityManager entityMgr = getEntityManager();

        try {
            Subject subject = SessionTestHelper.createNewSubject(entityMgr, "testSubject");
            Subject subjectNotInRole = SessionTestHelper.createNewSubject(entityMgr, "subjectNotInRole");

            Role roleWithSubject = SessionTestHelper.createNewRoleForSubject(entityMgr, subject, "role with subject");
            roleWithSubject.addPermission(Permission.VIEW_RESOURCE);

            ResourceGroup group = SessionTestHelper.createNewCompatibleGroupForRole(entityMgr, roleWithSubject,
                "accessible group");

            Resource r1 = SessionTestHelper.createNewResourceForGroup(entityMgr, group, "r1");
            Resource r2 = SessionTestHelper.createNewResourceForGroup(entityMgr, group, "r2");

            entityMgr.flush();

            List<Integer> resourceIds = Arrays.asList(r1.getId(), r2.getId());

            assertFalse("The subject should not have permission to view the resources", authorizationManager
                .canViewResources(subjectNotInRole, resourceIds));

        } finally {
            getTransactionManager().rollback();
        }
    }

    /*
     * Test methods:   getResourcePermissions(Subject, Resource)   hasResourcePermission(Subject, Permission, Resource)
     *  hasResourcePermission(Subject, Permission, Collection<Resource>)
     */
    @Test(groups = "integration.session")
    public void testResourcePermissions() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        /*
         *             p            | subject -- role -- group -- resource
         */
        try {
            /* bootstrap */
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            Role testRole1 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole1");
            ResourceGroup testGroup1 = SessionTestHelper.createNewMixedGroupForRole(em, testRole1, "testGroup1", false);
            Resource resource1 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource1");
            Resource resource2 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource2");
            Resource resource3 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource3");

            /* setup permissions */
            Set<Permission> allPermissions = EnumSet.allOf(Permission.class);
            testRole1.getPermissions().addAll(allPermissions);
            em.merge(testRole1);
            em.flush();

            /* verify getResourcePermissions test */
            Set<Permission> allPermissionsResults = authorizationManager.getExplicitResourcePermissions(testSubject,
                resource1.getId());
            assert SessionTestHelper.samePermissions(allPermissions, allPermissionsResults) : "Failed to get all 1-role, 1-group, 1-resource permissions";

            /* verify hasResourcePermission test */
            for (Permission permission : allPermissions) {
                assert (authorizationManager.hasResourcePermission(testSubject, permission, resource1.getId())) : "Failed to get single 1-role, 1-group, 1-resource permission";
            }

            /* verify hasResourcePermission(collection) test */
            Collection<Integer> allResources = SessionTestHelper.getResourceList(resource1, resource2, resource3);

            for (Permission permission : allPermissions) {
                assert (authorizationManager.hasResourcePermission(testSubject, permission, allResources)) : "Failed to get every positive 1-role, 1-group, resource-list permission";
            }
        } finally {
            getTransactionManager().rollback();
        }

        /*
         *             p            | subject -- role -- group -- resource       \          /        \- role -/
         *   |            p          without redundant permissions
         */
        getTransactionManager().begin();
        em = getEntityManager();
        try {
            /* bootstrap */
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            Role testRole1 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole1");
            ResourceGroup testGroup1 = SessionTestHelper.createNewMixedGroupForRole(em, testRole1, "testGroup1", false);
            Resource resource1 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource1");
            Resource resource2 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource2");
            Resource resource3 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource3");

            Role testRole2 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole2");
            testRole2.getResourceGroups().add(testGroup1);
            testGroup1.addRole(testRole2);
            em.merge(testRole2);
            em.merge(testGroup1);
            em.flush();

            /* setup permissions */
            Set<Permission> somePermissions = EnumSet.of(Permission.CONFIGURE_WRITE);
            Set<Permission> otherPermissions = EnumSet.of(Permission.CONTROL);
            testRole1.getPermissions().addAll(somePermissions);
            testRole2.getPermissions().addAll(otherPermissions);
            em.merge(testRole1);
            em.merge(testRole2);
            em.flush();

            /* setup shared object for result testing */
            Set<Permission> allPermissions = EnumSet.noneOf(Permission.class);
            allPermissions.addAll(somePermissions);
            allPermissions.addAll(otherPermissions);

            /* verify getResourcePermissions test */
            Set<Permission> allPermissionsResults = authorizationManager.getExplicitResourcePermissions(testSubject,
                resource1.getId());
            assert SessionTestHelper.samePermissions(allPermissions, allPermissionsResults) : "Failed to get all 2-role, 1-group, 1-resource permissions";

            /* verify hasResourcePermission test */
            for (Permission permission : allPermissions) {
                assert (authorizationManager.hasResourcePermission(testSubject, permission, resource1.getId())) : "Failed to get single 2-role, 1-group, 1-resource permission";
            }

            /* verify hasResourcePermission(collection) test */
            Collection<Integer> allResources = SessionTestHelper.getResourceList(resource1, resource2, resource3);
            for (Permission permission : allPermissions) {
                assert (authorizationManager.hasResourcePermission(testSubject, permission, allResources)) : "Failed to get every positive 2-role, 1-group, resource-list permission";
            }
        } finally {
            getTransactionManager().rollback();
        }

        /*
         *             p            | subject -- role -- group -- resource       \                   /        \- role --
         * group -/            |            p          without redundant permissions
         */
        getTransactionManager().begin();
        em = getEntityManager();
        try {
            /* bootstrap */
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            Role testRole1 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole1");
            ResourceGroup testGroup1 = SessionTestHelper.createNewMixedGroupForRole(em, testRole1, "testGroup1", false);
            Resource resource1 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource1");
            Resource resource2 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource2");
            Resource resource3 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource3");

            Role testRole2 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole2");
            ResourceGroup testGroup2 = SessionTestHelper.createNewMixedGroupForRole(em, testRole2, "testGroup2", false);

            testGroup2.addExplicitResource(resource1);
            resource1.getExplicitGroups().add(testGroup2);

            /*
             * Single resource implies the implicit resource list should mirror the explicit one
             */
            testGroup2.addImplicitResource(resource1);
            resource1.getImplicitGroups().add(testGroup2);

            em.merge(testGroup2);
            em.merge(resource1);
            em.flush();

            /* setup permissions */
            Set<Permission> sharedPermissions = EnumSet.of(Permission.MANAGE_CONTENT);
            Set<Permission> otherPermissions = EnumSet.of(Permission.MANAGE_SECURITY);
            testRole1.getPermissions().addAll(sharedPermissions);
            testRole2.getPermissions().addAll(otherPermissions);
            em.merge(testRole1);
            em.merge(testRole2);
            em.flush();

            /* setup shared object for result testing */
            Set<Permission> allPermissions = EnumSet.noneOf(Permission.class);
            allPermissions.addAll(sharedPermissions);
            allPermissions.addAll(otherPermissions);

            /* verify getResourcePermissions test */
            Set<Permission> allPermissionsResults = authorizationManager.getExplicitResourcePermissions(testSubject,
                resource1.getId());
            assert SessionTestHelper.samePermissions(allPermissions, allPermissionsResults) : "Failed to get all 2-role, 2-group, 1-resource permissions";

            /* verify hasResourcePermission test */
            for (Permission permission : allPermissions) {
                assert (authorizationManager.hasResourcePermission(testSubject, permission, resource1.getId())) : "Failed to get single 2-role, 2-group, 1-resource permission";
            }

            /* verify hasResourcePermission(collection) test */
            Collection<Integer> allResources = SessionTestHelper.getResourceList(resource1, resource2, resource3);

            for (Permission permission : sharedPermissions) {
                assert (authorizationManager.hasResourcePermission(testSubject, permission, allResources)) : "Failed to get every positive 2-role, 2-group, resource-list permission";
            }

            for (Permission permission : otherPermissions) {
                assert (!authorizationManager.hasResourcePermission(testSubject, permission, allResources)) : "Failed to get every negative 2-role, 2-group, resource-list permission";
            }
        } finally {
            getTransactionManager().rollback();
        }

        /*
         *             p            | subject -- role -- group -- resource               \           /                \-
         * group -/
         */
        getTransactionManager().begin();
        em = getEntityManager();
        try {
            /* bootstrap */
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            Role testRole1 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole1");
            ResourceGroup testGroup1 = SessionTestHelper.createNewMixedGroupForRole(em, testRole1, "testGroup1", false);
            Resource resource1 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource1");
            Resource resource2 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource2");
            Resource resource3 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource3");

            ResourceGroup testGroup2 = SessionTestHelper.createNewMixedGroupForRole(em, testRole1, "testGroup2", false);

            testGroup2.addExplicitResource(resource1);
            resource1.getExplicitGroups().add(testGroup2);

            /*
             * Single resource implies the implicit resource list should mirror the explicit one
             */
            testGroup2.addImplicitResource(resource1);
            resource1.getImplicitGroups().add(testGroup2);

            em.merge(testGroup2);
            em.merge(resource1);
            em.flush();

            /* setup permissions */
            Set<Permission> sharedPermissions = EnumSet.of(Permission.MANAGE_SETTINGS, Permission.MANAGE_ALERTS);
            testRole1.getPermissions().addAll(sharedPermissions);
            em.merge(testRole1);
            em.flush();

            /* verify getResourcePermissions test */
            Set<Permission> somePermissionsResults = authorizationManager.getExplicitResourcePermissions(testSubject,
                resource1.getId());
            assert SessionTestHelper.samePermissions(sharedPermissions, somePermissionsResults) : "Failed to get all 1-role, 2-group, 1-resource permissions";

            /* verify hasResourcePermission test */
            for (Permission permission : sharedPermissions) {
                assert (authorizationManager.hasResourcePermission(testSubject, permission, resource1.getId())) : "Failed to get single 1-role, 2-group, 1-resource permission";
            }

            /* verify hasResourcePermission(collection) test */
            Collection<Integer> allResources = SessionTestHelper.getResourceList(resource1, resource2, resource3);
            for (Permission permission : sharedPermissions) {
                assert (authorizationManager.hasResourcePermission(testSubject, permission, allResources)) : "Failed to get every positive 1-role, 2-group, resource-list permission";
            }
        } finally {
            getTransactionManager().rollback();
        }

        /*
         *             p            | subject -- role -- group -- resource       \        \/         /        \       /\
         *        /         \ role -- group /            |            p          with highly redundant permissions
         */
        getTransactionManager().begin();
        em = getEntityManager();
        try {
            /* bootstrap */
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            Role testRole1 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole1");
            ResourceGroup testGroup1 = SessionTestHelper.createNewMixedGroupForRole(em, testRole1, "testGroup1", false);
            Resource resource1 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource1");
            Resource resource2 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource2");
            Resource resource3 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource3");

            Role testRole2 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole2");
            ResourceGroup testGroup2 = SessionTestHelper.createNewMixedGroupForRole(em, testRole2, "testGroup2", false);

            testGroup2.addRole(testRole1);
            testRole1.getResourceGroups().add(testGroup2);

            testGroup1.addRole(testRole2);
            testRole2.getResourceGroups().add(testGroup1);

            testGroup2.addExplicitResource(resource1);
            resource1.getExplicitGroups().add(testGroup2);

            /*
             * Single resource implies the implicit resource list should mirror the explicit one
             */
            testGroup2.addImplicitResource(resource1);
            resource1.getImplicitGroups().add(testGroup2);

            em.merge(testGroup1);
            em.merge(testGroup2);
            em.merge(testRole1);
            em.merge(testRole2);
            em.merge(resource1);
            em.flush();

            /* setup permissions */
            Set<Permission> somePermissions = EnumSet.of(Permission.CONFIGURE_WRITE, Permission.CONTROL,
                Permission.MANAGE_INVENTORY);
            Set<Permission> otherPermissions = EnumSet.of(Permission.CONTROL, Permission.MANAGE_SECURITY,
                Permission.MANAGE_INVENTORY);
            testRole1.getPermissions().addAll(somePermissions);
            testRole2.getPermissions().addAll(otherPermissions);
            em.merge(testRole1);
            em.merge(testRole2);
            em.flush();

            /* setup shared object for result testing */
            Set<Permission> allPermissions = EnumSet.noneOf(Permission.class);
            allPermissions.addAll(somePermissions);
            allPermissions.addAll(otherPermissions);

            /* verify getResourcePermissions test */
            Set<Permission> allPermissionsResults = authorizationManager.getExplicitResourcePermissions(testSubject,
                resource1.getId());
            assert SessionTestHelper.samePermissions(allPermissions, allPermissionsResults) : "Failed to get all 2-role crossed 2-group, 1-resource distinct permissions";

            /* verify hasResourcePermission test */
            for (Permission permission : allPermissions) {
                assert (authorizationManager.hasResourcePermission(testSubject, permission, resource1.getId())) : "Failed to get single 2-role crossed 2-group, 1-resource distinct permission";
            }

            /* verify hasResourcePermission(collection) test */
            Collection<Integer> allResources = SessionTestHelper.getResourceList(resource1, resource2, resource3);
            for (Permission permission : allPermissions) {
                assert (authorizationManager.hasResourcePermission(testSubject, permission, allResources)) : "Failed to get every positive 2-role crossed 2-group, resource-list permission";
            }
        } finally {
            getTransactionManager().rollback();
        }

        /*
         *             p            | subject -- role -- group -- resource       \        \- role -- group -- resource
         *          |            p
         */
        getTransactionManager().begin();
        em = getEntityManager();
        try {
            /* bootstrap */
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            Role testRole1 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole1");
            Role testRole2 = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole2");
            ResourceGroup testGroup1 = SessionTestHelper.createNewMixedGroupForRole(em, testRole1, "testGroup1", false);
            ResourceGroup testGroup2 = SessionTestHelper.createNewMixedGroupForRole(em, testRole2, "testGroup2", false);
            Resource resource1 = SessionTestHelper.createNewResourceForGroup(em, testGroup1, "testResource1");
            Resource resource2 = SessionTestHelper.createNewResourceForGroup(em, testGroup2, "testResource2");

            /* setup permissions */
            Set<Permission> somePermissions = EnumSet.of(Permission.MANAGE_ALERTS, Permission.MANAGE_SETTINGS);
            Set<Permission> otherPermissions = EnumSet.of(Permission.MANAGE_ALERTS, Permission.MANAGE_CONTENT);
            testRole1.getPermissions().addAll(somePermissions);
            testRole2.getPermissions().addAll(otherPermissions);
            em.merge(testRole1);
            em.merge(testRole2);
            em.flush();

            /* setup shared object for result testing */
            Set<Permission> intersection = EnumSet.copyOf(somePermissions);
            intersection.retainAll(otherPermissions);

            Set<Permission> symmetricDifference = EnumSet.copyOf(somePermissions);
            symmetricDifference.addAll(otherPermissions);
            symmetricDifference.removeAll(intersection);

            Collection<Integer> allResources = SessionTestHelper.getResourceList(resource1, resource2);

            /* verify hasResourcePermission(collection) test */
            for (Permission permission : intersection) {
                assert (authorizationManager.hasResourcePermission(testSubject, permission, allResources)) : "Failed to get every positive 2-role independent 2-group, resource-list permission";
            }

            for (Permission permission : symmetricDifference) {
                assert (!authorizationManager.hasResourcePermission(testSubject, permission, allResources)) : "Failed to get every negative 2-role independent 2-group, resource-list permission";
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.session")
    public void testSetPermission() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            Role testRole = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole");

            // moved setPerm to the role manager - leave this test here, just use that manager instead
            RoleManagerLocal roleManager = LookupUtil.getRoleManager();
            Subject superuser = LookupUtil.getSubjectManager().getOverlord();

            /* verify role got all global permissions */
            Set<Permission> globalPermissions = SessionTestHelper.getAllGlobalPerms();
            roleManager.setPermissions(superuser, testRole.getId(), globalPermissions);
            em.refresh(testRole);
            assert SessionTestHelper.samePermissions(testRole.getPermissions(), globalPermissions) : "Failed to set global permissions";

            /* verify role still has global and got all resource permissions */
            Set<Permission> resourcePermissions = SessionTestHelper.getAllResourcePerms();
            roleManager.setPermissions(superuser, testRole.getId(), resourcePermissions);
            em.refresh(testRole);
            assert SessionTestHelper.samePermissions(testRole.getPermissions(), resourcePermissions) : "Failed to set resource permissions";

            /* verify role still has global and got all resource permissions */
            Set<Permission> noPermissions = EnumSet.noneOf(Permission.class);
            roleManager.setPermissions(superuser, testRole.getId(), noPermissions);
            em.refresh(testRole);
            assert SessionTestHelper.samePermissions(testRole.getPermissions(), noPermissions) : "Failed to set empty list of permissions";
        } finally {
            getTransactionManager().rollback();
        }
    }
}