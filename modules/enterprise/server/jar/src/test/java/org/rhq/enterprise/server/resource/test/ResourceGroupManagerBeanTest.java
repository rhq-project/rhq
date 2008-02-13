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
package org.rhq.enterprise.server.resource.test;

import javax.persistence.EntityManager;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.resource.group.ResourceGroupAlreadyExistsException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;

public class ResourceGroupManagerBeanTest extends AbstractEJB3Test {
    private static ResourceGroupManagerLocal resourceGroupManager;

    @BeforeSuite
    @SuppressWarnings("unused")
    private void init() {
        try {
            resourceGroupManager = LookupUtil.getResourceGroupManager();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Test(groups = "integration.session")
    public void testGetCompatibleGroupById() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            /* bootstrap */
            ResourceType type = new ResourceType("type", "plugin", ResourceCategory.PLATFORM, null);
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            Role testRole = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole");
            ResourceGroup compatGroup = new ResourceGroup("testCompatGroup", type);
            compatGroup.addRole(testRole);
            em.persist(type);
            em.persist(compatGroup);
            em.flush();
            testRole.addResourceGroup(compatGroup);
            em.merge(testRole);
            em.flush();

            int id = compatGroup.getId();
            try {
                resourceGroupManager.getResourceGroupById(testSubject, id, null);
            } catch (ResourceGroupNotFoundException e) {
                assert (false) : "Could not find recently persisted compatible group by id";
            } catch (PermissionException se) {
                assert (false) : "Incorrect permissions when getting compatible group by id";
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.session")
    public void testUpdateGroupDescription() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            /* bootstrap */
            ResourceType type = new ResourceType("type", "plugin", ResourceCategory.PLATFORM, null);
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            createSession(testSubject);
            Role testRole = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole");
            testRole.addPermission(Permission.MANAGE_INVENTORY);
            ResourceGroup compatGroup = new ResourceGroup("group1", type);
            compatGroup.addRole(testRole);
            em.persist(type);
            em.persist(compatGroup);
            em.flush();
            testRole.addResourceGroup(compatGroup);
            em.merge(testRole);
            em.flush();

            int id = compatGroup.getId();
            try {
                ResourceGroup oldGroup = resourceGroupManager.getResourceGroupById(testSubject, id, null);
                oldGroup.setDescription("new description goes here ");
                resourceGroupManager.updateResourceGroup(testSubject, oldGroup);
            } catch (ResourceGroupAlreadyExistsException ex) {
                fail("ResourceGroupAlreadyExistsException should NOT have been thrown.");
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.session")
    public void testUpdateGroupName() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            /* bootstrap */
            ResourceType type = new ResourceType("type", "plugin", ResourceCategory.PLATFORM, null);
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            createSession(testSubject);
            Role testRole = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole");
            testRole.addPermission(Permission.MANAGE_INVENTORY);
            ResourceGroup compatGroup = new ResourceGroup("group1", type);
            compatGroup.addRole(testRole);
            em.persist(type);
            em.persist(compatGroup);
            em.flush();
            testRole.addResourceGroup(compatGroup);
            em.merge(testRole);
            em.flush();

            int id = compatGroup.getId();
            try {
                ResourceGroup oldGroup = resourceGroupManager.getResourceGroupById(testSubject, id, null);
                oldGroup.setName("newGroup1");
                resourceGroupManager.updateResourceGroup(testSubject, oldGroup);
            } catch (ResourceGroupAlreadyExistsException ex) {
                fail("ResourceGroupAlreadyExistsException should NOT have been thrown.");
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    //   @Test(groups = "integration.session")
    //   When I step through a test like this in the UI it works fine (i.e. update a resource to have the same name as an existing resource)
    //   I'm not sure why the test throws a NoSuchMethodException
    public void testUpdateGroupNameFail() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            /* bootstrap */
            ResourceType type = new ResourceType("type", "plugin", ResourceCategory.PLATFORM, null);
            Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
            createSession(testSubject);
            Role testRole = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole");
            testRole.addPermission(Permission.MANAGE_INVENTORY);
            ResourceGroup group1 = new ResourceGroup("group1", type);
            ResourceGroup group2 = new ResourceGroup("group2", type);
            group1.addRole(testRole);
            group2.addRole(testRole);
            em.persist(type);
            em.persist(group1);
            em.persist(group2);
            em.flush();
            testRole.addResourceGroup(group1);
            testRole.addResourceGroup(group2);
            em.merge(testRole);
            em.flush();

            int group1Id = group1.getId();
            try {
                ResourceGroup group1Retrieved = resourceGroupManager.getResourceGroupById(testSubject, group1Id, null);
                group1Retrieved.setName("group2");
                group1Retrieved.setDescription("new description");
                resourceGroupManager.updateResourceGroup(testSubject, group1Retrieved);
                fail("ResourceGroupAlreadyExistsException should have been throw");
            } catch (ResourceGroupAlreadyExistsException ex) {
                // expected
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    /*
     * CGroup  getCompatibleGroupById(Subject user, int id) MGroup  getMixedGroupById(Subject user, int id) List<>
     * getCompatibleGroupsByResourceType(Subject user, ResourceType type, PC) int     getCompatibleGroupCount(Subject
     * user) int     getMixedGroupCount(Subject user)
     */
}