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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.discovery.DiscoveryBossLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;

public class ResourceStorageTest extends AbstractEJB3Test {
    private Log log = LogFactory.getLog(ResourceStorageTest.class);

    // TODO GH: Fixme (setup non-super user and group)@Test(groups = "integration.ejb3")
    public void testFindResourceComposite() throws Exception {
        getTransactionManager().begin();

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
            PageControl pc = new PageControl(0, 5, new OrderingField("rg.name", PageOrdering.ASC));
            PageList<ResourceGroupComposite> groups = groupManager.findResourceGroupComposites(
                subjectManager.getOverlord(), GroupCategory.COMPATIBLE, null, null, null, null, null, null, pc);
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

            Map<Resource, List<Resource>> queue = discoveryBoss.getQueuedPlatformsAndServers(rhqadmin,
                PageControl.getUnlimitedInstance());
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

        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

        AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();

        Subject rhqadmin = subjectManager.loginUnauthenticated("rhqadmin");
        System.out.println(rhqadmin);
    }

    /** Test creates a large number of resources and pages through them using CriteriaQuery.
     *  NOTE: CriteriaQuery uses PageList instances underneath and are susceptible to dirty
     *  read issues if the total number of resources being parsed is i)very large or ii)processing
     *  each instance takes a significant amount of time. Ex. Begin parsing all resource types, 
     *  while plugin update is removing some of those same types.
     *  
     * @throws Exception
     */
    @Test(groups = "integration.ejb3")
    public void testParsingCriteriaQueryResults() throws Exception {
        getTransactionManager().begin();
        EntityManager entityMgr = getEntityManager();
        final ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        //verify that all resource objects are actually parsed. 
        Map<String, Object> resourceNames = new HashMap<String, Object>();
        int resourceCount = 700; //assuming 200 per page at least 4 pages of results.

        try {
            final Subject subject = SessionTestHelper.createNewSubject(entityMgr, "testSubject");

            Role roleWithSubject = SessionTestHelper.createNewRoleForSubject(entityMgr, subject, "role with subject");
            roleWithSubject.addPermission(Permission.VIEW_RESOURCE);

            ResourceGroup group = SessionTestHelper.createNewCompatibleGroupForRole(entityMgr, roleWithSubject,
                "accessible group");

            String tuid = "" + new Random().nextInt();
            //create large number of resources
            String prefix = "largeResultSet-" + tuid + "-";
            System.out.println("-------- Creating " + resourceCount + " resource(s). This may take a while ....");

            long start = System.currentTimeMillis();
            for (int i = 0; i < resourceCount; i++) {
                String name = prefix + i;
                Resource r = SessionTestHelper.createNewResourceForGroup(entityMgr, group, name);
                //store away each resource name/key
                resourceNames.put(String.valueOf(r.getId()), name);
            }
            entityMgr.flush();

            System.out.println("----------- Created " + resourceCount + " resource(s) in "
                + (System.currentTimeMillis() - start) + " ms.");

            assert resourceNames.size() == resourceCount;//assert all resources loaded/created

            //query the results and delete the resources
            ResourceCriteria criteria = new ResourceCriteria();
            criteria.addFilterName(prefix);
            criteria.addSortName(PageOrdering.DESC); // use DESC just to make sure sorting on name is different than insert order
            criteria.setPaging(0, 47);

            //iterate over the results with CriteriaQuery
            CriteriaQueryExecutor<Resource, ResourceCriteria> queryExecutor = new CriteriaQueryExecutor<Resource, ResourceCriteria>() {
                @Override
                public PageList<Resource> execute(ResourceCriteria criteria) {
                    return resourceManager.findResourcesByCriteria(subject, criteria);
                }
            };

            //initiate first/(total depending on page size) request.
            CriteriaQuery<Resource, ResourceCriteria> resources = new CriteriaQuery<Resource, ResourceCriteria>(
                criteria, queryExecutor);

            start = System.currentTimeMillis();
            String prevName = null;
            //iterate over the entire result set efficiently
            ArrayList<String> alreadySeen = new ArrayList<String>();
            int actualCount = 0;
            for (Resource r : resources) {
                assert null == prevName || r.getName().compareTo(prevName) < 0 : "Results should be sorted by name DESC, something is out of order";
                prevName = r.getName();
                actualCount++;
                //                System.out.println(actualCount + " @@@ " + r.getId() + ":"
                //                    + ((resourceNames.containsKey(String.valueOf(r.getId()))) ? "NEW" : "DIRTY") + ":" + r.getName());
                resourceNames.remove(String.valueOf(r.getId()));
            }

            System.out.println("----------- Parsed " + actualCount + " resource(s) in "
                + (System.currentTimeMillis() - start) + " ms.");

            //test that entire list parsed spanning multiple pages
            assert resourceNames.size() == 0 : "Expected resourceNames to be empty. Still " + resourceNames.size()
                + " name(s).";

        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.ejb3")
    public void testParsingCriteriaQueryResults_2() throws Exception {
        // Same test as above but makes sure default id search works. use different numbers just for variety        
        getTransactionManager().begin();
        EntityManager entityMgr = getEntityManager();
        final ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        //verify that all resource objects are actually parsed. 
        Map<String, Object> resourceNames = new HashMap<String, Object>();
        int resourceCount = 344;

        try {
            final Subject subject = SessionTestHelper.createNewSubject(entityMgr, "testSubject");

            Role roleWithSubject = SessionTestHelper.createNewRoleForSubject(entityMgr, subject, "role with subject");
            roleWithSubject.addPermission(Permission.VIEW_RESOURCE);

            ResourceGroup group = SessionTestHelper.createNewCompatibleGroupForRole(entityMgr, roleWithSubject,
                "accessible group");

            String tuid = "" + new Random().nextInt();
            //create large number of resources
            String prefix = "largeResultSet-" + tuid + "-";
            System.out.println("-------- Creating " + resourceCount + " resource(s). This may take a while ....");

            long start = System.currentTimeMillis();
            for (int i = 0; i < resourceCount; i++) {
                String name = prefix + i;
                Resource r = SessionTestHelper.createNewResourceForGroup(entityMgr, group, name);
                //store away each resource name/key
                resourceNames.put(String.valueOf(r.getId()), name);
            }
            entityMgr.flush();

            System.out.println("----------- Created " + resourceCount + " resource(s) in "
                + (System.currentTimeMillis() - start) + " ms.");

            assert resourceNames.size() == resourceCount;//assert all resources loaded/created

            //query the results and delete the resources, use default ID search
            ResourceCriteria criteria = new ResourceCriteria();
            criteria.addFilterName(prefix);
            criteria.setPaging(0, 25);

            //iterate over the results with CriteriaQuery
            CriteriaQueryExecutor<Resource, ResourceCriteria> queryExecutor = new CriteriaQueryExecutor<Resource, ResourceCriteria>() {
                @Override
                public PageList<Resource> execute(ResourceCriteria criteria) {
                    return resourceManager.findResourcesByCriteria(subject, criteria);
                }
            };

            //initiate first/(total depending on page size) request.
            CriteriaQuery<Resource, ResourceCriteria> resources = new CriteriaQuery<Resource, ResourceCriteria>(
                criteria, queryExecutor);

            start = System.currentTimeMillis();
            int prevId = 0;
            //iterate over the entire result set efficiently
            ArrayList<String> alreadySeen = new ArrayList<String>();
            int actualCount = 0;
            for (Resource r : resources) {
                assert r.getId() > prevId : "Results should be sorted by ID ASC, something is out of order";
                prevId = r.getId();
                actualCount++;
                //                System.out.println(actualCount + " @@@ " + r.getId() + ":"
                //                    + ((resourceNames.containsKey(String.valueOf(r.getId()))) ? "NEW" : "DIRTY") + ":" + r.getName());
                resourceNames.remove(String.valueOf(r.getId()));
            }

            System.out.println("----------- Parsed " + actualCount + " resource(s) in "
                + (System.currentTimeMillis() - start) + " ms.");

            //test that entire list parsed spanning multiple pages
            assert resourceNames.size() == 0 : "Expected resourceNames to be empty. Still " + resourceNames.size()
                + " name(s).";

        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.ejb3")
    public void testParsingCriteriaQueryResults_3() throws Exception {
        // Same test as above but makes sure pageoverrides default id search works. use different numbers just for variety        
        getTransactionManager().begin();
        EntityManager entityMgr = getEntityManager();
        final ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        //verify that all resource objects are actually parsed. 
        Map<String, Object> resourceNames = new HashMap<String, Object>();
        int resourceCount = 423;

        try {
            final Subject subject = SessionTestHelper.createNewSubject(entityMgr, "testSubject");

            Role roleWithSubject = SessionTestHelper.createNewRoleForSubject(entityMgr, subject, "role with subject");
            roleWithSubject.addPermission(Permission.VIEW_RESOURCE);

            ResourceGroup group = SessionTestHelper.createNewCompatibleGroupForRole(entityMgr, roleWithSubject,
                "accessible group");

            String tuid = "" + new Random().nextInt();
            //create large number of resources
            String prefix = "largeResultSet-" + tuid + "-";
            System.out.println("-------- Creating " + resourceCount + " resource(s). This may take a while ....");

            long start = System.currentTimeMillis();
            for (int i = 0; i < resourceCount; i++) {
                String name = prefix + i;
                Resource r = SessionTestHelper.createNewResourceForGroup(entityMgr, group, name);
                //store away each resource name/key
                resourceNames.put(String.valueOf(r.getId()), name);
            }
            entityMgr.flush();

            System.out.println("----------- Created " + resourceCount + " resource(s) in "
                + (System.currentTimeMillis() - start) + " ms.");

            assert resourceNames.size() == resourceCount;//assert all resources loaded/created

            //query the results and delete the resources, use default ID search
            ResourceCriteria criteria = new ResourceCriteria();
            criteria.addFilterName(prefix);
            PageControl pc = new PageControl(0, 73);
            criteria.setPageControl(pc);
            assert pc.getOrderingFields().isEmpty() : "Should not have had any sorting defined";

            //iterate over the results with CriteriaQuery
            CriteriaQueryExecutor<Resource, ResourceCriteria> queryExecutor = new CriteriaQueryExecutor<Resource, ResourceCriteria>() {
                @Override
                public PageList<Resource> execute(ResourceCriteria criteria) {
                    return resourceManager.findResourcesByCriteria(subject, criteria);
                }
            };

            //initiate first/(total depending on page size) request.
            CriteriaQuery<Resource, ResourceCriteria> resources = new CriteriaQuery<Resource, ResourceCriteria>(
                criteria, queryExecutor);

            start = System.currentTimeMillis();
            int prevId = 0;
            //iterate over the entire result set efficiently
            ArrayList<String> alreadySeen = new ArrayList<String>();
            int actualCount = 0;
            for (Resource r : resources) {
                System.out.println(actualCount + " @@@ " + r.getId() + ":"
                    + ((resourceNames.containsKey(String.valueOf(r.getId()))) ? "NEW" : "DIRTY") + ":" + r.getName());
                assert r.getId() > prevId : "Results should be sorted by ID ASC, something is out of order";
                prevId = r.getId();
                actualCount++;
                resourceNames.remove(String.valueOf(r.getId()));
            }

            System.out.println("----------- Parsed " + actualCount + " resource(s) in "
                + (System.currentTimeMillis() - start) + " ms.");

            //test that entire list parsed spanning multiple pages
            assert resourceNames.size() == 0 : "Expected resourceNames to be empty. Still " + resourceNames.size()
                + " name(s).";

        } finally {
            getTransactionManager().rollback();
        }
    }

}