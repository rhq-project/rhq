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

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerBean;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.discovery.DiscoveryBossLocal;
import org.rhq.enterprise.server.resource.ResourceManagerBean;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourceStorageTest extends AbstractEJB3Test {
    private Log log = LogFactory.getLog(ResourceStorageTest.class);

    // TODO GH: Fixme (setup non-super user and group)@Test(groups = "integration.ejb3")
    public void testFindResourceComposite() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
            SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
            PageControl pc = new PageControl(1, 5, new OrderingField("res.name", PageOrdering.ASC));
            Subject subject = subjectManager.loginUnauthenticated("ghinkle");

            //Subject subject = subjectManager.getOverlord();
            subject = createSession(subject);
            ResourceType t = em.getReference(ResourceType.class, new Integer(501064));
            String typeNameFilter = t == null ? null : t.getName();
            PageList<ResourceComposite> resources = resourceManager.findResourceComposites(subject,
                ResourceCategory.SERVER, typeNameFilter, null, null, "g", false, pc);
            System.out.println("Found resource composites: " + resources.size());
            for (ResourceComposite resourceComposite : resources) {
                System.out.println("\t" + resourceComposite);
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    // TODO GH: Is this supposed to work? @Test(groups = "integration.ejb3")
    @SuppressWarnings("unchecked")
    public void testBindOrderBy() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            log.error("QUERY TEST!!");
            Query q = em.createQuery("SELECT r FROM Resource r ORDER BY :col");
            q.setParameter("col", "r.name desc");
            List<Resource> resources = q.getResultList();
            System.out.println("Sorted resources: ");
            for (Resource r : resources) {
                System.out.println("\t" + r);
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.ejb3")
    public void testMixedGroupCompositeQuery() throws Exception {
        getTransactionManager().begin();
        try {
            ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();
            SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
            PageControl pc = new PageControl(1, 5, new OrderingField("rg.name", PageOrdering.ASC));

            //Subject subject = subjectManager.loginUnauthenticated("ghinkle");
            Subject subject = subjectManager.getOverlord();
            subject = createSession(subject);
            List<ResourceGroupComposite> groups = groupManager.findResourceGroupComposites(subject,
                GroupCategory.MIXED, null, null, null, null, null, null, pc);
            System.out.println("Found mixed groups: " + groups.size());
            for (ResourceGroupComposite group : groups) {
                System.out.println("\t" + group);
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.ejb3")
    public void testCompatibleGroupCompositeQuery() throws Exception {
        getTransactionManager().begin();
        try {
            ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();
            SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
            PageControl pc = new PageControl(1, 5, new OrderingField("rg.name", PageOrdering.ASC));
            PageList<ResourceGroupComposite> groups = groupManager.findResourceGroupComposites(subjectManager
                .getOverlord(), GroupCategory.COMPATIBLE, null, null, null, null, null, null, pc);
            System.out.println("Found compatible groups: " + groups.getTotalSize());
            for (ResourceGroupComposite group : groups) {
                System.out.println("\t" + group);
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    @SuppressWarnings("unchecked")
    @Test(groups = "integration.ejb3")
    public void testConstructorQuery() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            Query q = em
                .createQuery("SELECT new org.rhq.enterprise.server.resource.test.ResourceWithStatus(a.availabilityType,r) "
                    + "FROM Resource r, Availability a "
                    + "WHERE r.id = a.resource.id "
                    + "AND  a.startTime = (SELECT MAX(aa.startTime) FROM Availability aa WHERE aa.resource.id = r.id)");
            List<ResourceWithStatus> resourceWithStatuses = q.getResultList();

            for (ResourceWithStatus r : resourceWithStatuses) {
                System.out.println(r.getAvailabilityType() + " - " + r.getResource().getName());
                System.out.println("\tchildren: " + r.getResource().getChildResources().size());
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    @SuppressWarnings("unchecked")
    @Test(groups = "integration.ejb3")
    public void getDeadResources() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            Query q = em
                .createQuery("SELECT new org.rhq.enterprise.server.resource.test.ResourceWithStatus(a.availabilityType,r) "
                    + "FROM Resource r, Availability a "
                    + "WHERE r.id = a.resource.id "
                    + "AND a.availabilityType = 1 "
                    + "AND  a.startTime = (SELECT MAX(aa.startTime) FROM Availability aa WHERE aa.resource.id = r.id)");
            List<ResourceWithStatus> resourceWithStatuses = q.getResultList();

            System.out.println("Resource currently down");
            for (ResourceWithStatus r : resourceWithStatuses) {
                System.out.println(r.getAvailabilityType() + " - " + r.getResource().getName());
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.ejb3")
    public void discoveryQueueTest() throws Exception {
        getTransactionManager().begin();
        try {
            DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();
            SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
            Subject rhqadmin = subjectManager.loginUnauthenticated("rhqadmin");
            rhqadmin = createSession(rhqadmin);

            Map<Resource, List<Resource>> queue = discoveryBoss.getQueuedPlatformsAndServers(rhqadmin, PageControl
                .getUnlimitedInstance());
            for (Resource root : queue.keySet()) {
                System.out.println("Queue root resource: " + root);
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    @SuppressWarnings("unused")
    @Test(groups = "integration.ejb3")
    public void resourceTest() throws Exception {
        // TODO GH: Implement actual tests

        System.out.println("foo");

        ResourceManagerLocal resourceManager = (ResourceManagerLocal) getInitialContext().lookup(
            ResourceManagerBean.class.getSimpleName() + "/local");

        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        //         (SubjectManagerLocal) getInitialContext().lookup(SubjectManagerBean.class.getSimpleName() + "/local");

        AuthorizationManagerLocal authorizationManager = (AuthorizationManagerLocal) getInitialContext().lookup(
            AuthorizationManagerBean.class.getSimpleName() + "/local");

        Subject rhqadmin = subjectManager.loginUnauthenticated("rhqadmin");
        System.out.println(rhqadmin);
    }
}