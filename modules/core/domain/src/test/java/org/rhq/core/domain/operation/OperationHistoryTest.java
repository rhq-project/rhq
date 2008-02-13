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
package org.rhq.core.domain.operation;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.test.AbstractEJB3Test;

public class OperationHistoryTest extends AbstractEJB3Test {
    private Resource newResource;
    private OperationDefinition newOperation;
    private ResourceGroup newGroup;

    @Test(groups = "integration.ejb3")
    public void testUpdate() throws Exception {
        EntityManager em = getEntityManager();
        getTransactionManager().begin();
        try {
            ResourceOperationHistory r1 = createIndividualResourceHistory(em);
            GroupOperationHistory g1 = createGroupResourceHistory(em);
            ResourceOperationHistory r2 = createGroupIndividualResourceHistory(em, g1);

            Configuration results = new Configuration();
            results.put(new PropertySimple("exitCode", "1"));
            r1.setResults(results);
            r1.setStatus(OperationRequestStatus.SUCCESS);
            r1 = em.merge(r1);
            results = r1.getResults(); // should be a new one

            assert r1.getStatus() == OperationRequestStatus.SUCCESS;
            assert r1.getErrorMessage() == null;
            assert results != null;
            assert results.getId() > 0; // should have been cascaded
            assert results.getSimple("exitCode").getIntegerValue() == 1;
            long duration = r1.getDuration();
            Thread.sleep(500L);
            assert duration == r1.getDuration(); // operation is done, so the duration time should always be the same now

            r2.setErrorMessage("an error message!");
            r2 = em.merge(r2);
            results = r2.getResults();

            assert r2.getStatus() == OperationRequestStatus.FAILURE;
            assert r2.getErrorMessage().equals("an error message!");
            assert r2.getResults() == null;
            duration = r2.getDuration();
            Thread.sleep(500L);
            assert duration == r2.getDuration(); // operation is done, so the duration time should always be the same now

            g1.setErrorMessage("group error");
            g1 = em.merge(g1);

            assert g1.getStatus() == OperationRequestStatus.FAILURE;
            assert g1.getErrorMessage().equals("group error");
            duration = g1.getDuration();
            Thread.sleep(500L);
            assert duration == g1.getDuration(); // operation is done, so the duration time should always be the same now

            int g1Id = g1.getId();
            int r1Id = r1.getId();
            int r2Id = r2.getId();

            em.remove(g1); // should also cascade-remove its individual resource histories

            try {
                assert em.find(OperationHistory.class, g1Id) == null;
            } catch (EntityNotFoundException ignore) {
                // the current EJB3 impl seems to throw this - its OK, but find should return null, not throw error
            }

            try {
                assert em.find(OperationHistory.class, r2Id) == null;
            } catch (EntityNotFoundException ignore) {
                // the current EJB3 impl seems to throw this - its OK, but find should return null, not throw error
            }

            assert em.find(OperationHistory.class, r1Id) != null; // not part of the group, should still be there
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.ejb3")
    public void testCreate() throws Exception {
        EntityManager em = getEntityManager();
        getTransactionManager().begin();
        try {
            ResourceOperationHistory r1 = createIndividualResourceHistory(em);

            Thread.sleep(100); // without sleeping, test goes so fast that the getDuration() is saying 0
            assert r1.getId() > 0;
            assert r1.getParameters() == null;
            assert r1.getJobId() != null;
            assert r1.getResource() != null;
            assert r1.getResource().getId() == newResource.getId();
            assert r1.getGroupOperationHistory() == null;
            assert r1.getCreatedTime() > 0;
            assert r1.getModifiedTime() > 0;
            assert r1.getDuration() > 0; // its in progress, so it thinks its still on going
            long duration = r1.getDuration();
            Thread.sleep(500L);
            assert duration < r1.getDuration(); // operation is not done, so the duration time should keep growing
            assert r1.getErrorMessage() == null;
            assert r1.getOperationDefinition() != null;
            assert r1.getOperationDefinition().getName().equals("testOp");
            assert r1.getJobName().startsWith("job1");
            assert r1.getJobGroup().equals("group1");
            assert r1.getSubjectName().equals("user");
            assert r1.getStatus() == OperationRequestStatus.INPROGRESS;

            GroupOperationHistory g1 = createGroupResourceHistory(em);

            Thread.sleep(100); // without sleeping, test goes so fast that the getDuration() is saying 0
            assert g1.getId() > 0;
            assert g1.getParameters() == null;
            assert g1.getJobId() != null;
            assert g1.getCreatedTime() > 0;
            assert g1.getModifiedTime() > 0;
            assert g1.getDuration() > 0; // its in progress, so it thinks its still on going
            assert g1.getErrorMessage() == null;
            assert g1.getOperationDefinition() != null;
            assert g1.getOperationDefinition().getName().equals("testOp");
            assert g1.getJobName().startsWith("job2");
            assert g1.getJobGroup().equals("group2");
            assert g1.getSubjectName().equals("user");
            assert g1.getStatus() == OperationRequestStatus.INPROGRESS;

            // the individual resource history that was part of a group execution
            ResourceOperationHistory r2 = createGroupIndividualResourceHistory(em, g1);

            Thread.sleep(100); // without sleeping, test goes so fast that the getDuration() is saying 0
            assert r2.getId() > 0;
            assert r2.getParameters() == null;
            assert r2.getJobId() != null;
            assert r2.getResource() != null;
            assert r2.getResource().getId() == newResource.getId();
            assert r2.getCreatedTime() > 0;
            assert r2.getModifiedTime() > 0;
            assert r2.getDuration() > 0; // its in progress, so it thinks its still on going
            assert r2.getErrorMessage() == null;
            assert r2.getOperationDefinition() != null;
            assert r2.getOperationDefinition().getId() == r1.getOperationDefinition().getId();
            assert r2.getOperationDefinition().getName().equals("testOp");
            assert r2.getGroupOperationHistory().getId() == g1.getId();
            assert r2.getJobName().startsWith("job3");
            assert r2.getJobGroup().equals("group3");
            assert r2.getSubjectName().equals("user");
            assert r2.getStatus() == OperationRequestStatus.INPROGRESS;
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.ejb3")
    public void testQueryByJobId() throws Exception {
        EntityManager em;
        ResourceOperationHistory r1;
        GroupOperationHistory g1;
        ResourceOperationHistory r2;

        getTransactionManager().begin();
        em = getEntityManager();

        try {
            r1 = createIndividualResourceHistory(em);
            g1 = createGroupResourceHistory(em);
            r2 = createGroupIndividualResourceHistory(em, g1);
            getTransactionManager().commit();
            em.close();
        } catch (Throwable t) {
            getTransactionManager().rollback();
            em.close();
            throw new Exception(t);
        }

        getTransactionManager().begin();
        em = getEntityManager();

        ResourceOperationHistory r1_dup;
        GroupOperationHistory g1_dup;
        ResourceOperationHistory r2_dup;
        try {
            Query q = em.createNamedQuery(OperationHistory.QUERY_FIND_BY_JOB_ID);
            HistoryJobId jobId = r1.getJobId();
            q.setParameter("jobName", jobId.getJobName());
            q.setParameter("jobGroup", jobId.getJobGroup());
            q.setParameter("createdTime", jobId.getCreatedTime());
            r1_dup = (ResourceOperationHistory) q.getSingleResult();
            assert r1_dup.equals(r1_dup);
            assert r1_dup.getId() == r1.getId();
            assert r1_dup.getJobId().equals(r1.getJobId());
            assert r1_dup.getJobName().equals(r1.getJobName());
            assert r1_dup.getJobGroup().equals(r1.getJobGroup());
            assert r1_dup.getStatus().equals(r1.getStatus());

            jobId = g1.getJobId();
            q.setParameter("jobName", jobId.getJobName());
            q.setParameter("jobGroup", jobId.getJobGroup());
            q.setParameter("createdTime", jobId.getCreatedTime());
            g1_dup = (GroupOperationHistory) q.getSingleResult();
            assert g1_dup.equals(g1_dup);
            assert g1_dup.getId() == g1.getId();
            assert g1_dup.getJobId().equals(g1.getJobId());
            assert g1_dup.getJobName().equals(g1.getJobName());
            assert g1_dup.getJobGroup().equals(g1.getJobGroup());
            assert g1_dup.getStatus().equals(g1.getStatus());

            jobId = r2.getJobId();
            q.setParameter("jobName", jobId.getJobName());
            q.setParameter("jobGroup", jobId.getJobGroup());
            q.setParameter("createdTime", jobId.getCreatedTime());
            r2_dup = (ResourceOperationHistory) q.getSingleResult();
            assert r2_dup.equals(r2_dup);
            assert r2_dup.getId() == r2.getId();
            assert r2_dup.getJobId().equals(r2.getJobId());
            assert r2_dup.getJobName().equals(r2.getJobName());
            assert r2_dup.getJobGroup().equals(r2.getJobGroup());
            assert r2_dup.getStatus().equals(r2.getStatus());
        } finally {
            getTransactionManager().rollback();
            em.close();

            getTransactionManager().begin();
            em = getEntityManager();

            r1 = em.find(ResourceOperationHistory.class, r1.getId());
            em.remove(r1);
            g1 = em.find(GroupOperationHistory.class, g1.getId());
            em.remove(g1); // this should cascade and delete r2

            getTransactionManager().commit();
            em.close();

            try {
                // make sure our cascading on remove worked
                getTransactionManager().begin();
                em = getEntityManager();
                assert em.find(ResourceOperationHistory.class, r2.getId()) == null;
            } finally {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    private ResourceOperationHistory createGroupIndividualResourceHistory(EntityManager em, GroupOperationHistory g1) {
        ResourceOperationHistory r2 = new ResourceOperationHistory("job3" + System.currentTimeMillis(), "group3",
            "user", newOperation, null, newResource, g1);
        em.persist(r2);
        return r2;
    }

    private GroupOperationHistory createGroupResourceHistory(EntityManager em) {
        GroupOperationHistory g1 = new GroupOperationHistory("job2" + System.currentTimeMillis(), "group2", "user",
            newOperation, null, newGroup);
        em.persist(g1);
        return g1;
    }

    private ResourceOperationHistory createIndividualResourceHistory(EntityManager em) {
        ResourceOperationHistory r1 = new ResourceOperationHistory("job1" + System.currentTimeMillis(), "group1",
            "user", newOperation, null, newResource, null);
        em.persist(r1);
        return r1;
    }

    private Resource createNewResource() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        Resource resource;

        try {
            try {
                ResourceType resourceType = new ResourceType("plat" + System.currentTimeMillis(), "test",
                    ResourceCategory.PLATFORM, null);

                OperationDefinition def = new OperationDefinition(resourceType, "testOp");
                def.setTimeout(1);
                resourceType.addOperationDefinition(def);

                em.persist(resourceType);

                resource = new Resource("key" + System.currentTimeMillis(), "name", resourceType);
                em.persist(resource);

                ResourceGroup group = new ResourceGroup("testgroup" + System.currentTimeMillis(), resourceType);
                em.persist(group);
                group.addExplicitResource(resource);
            } catch (Exception e) {
                System.out.println(e);
                getTransactionManager().rollback();
                throw e;
            }

            getTransactionManager().commit();
        } finally {
            em.close();
        }

        return resource;
    }

    private void deleteNewResource(Resource resource) throws Exception {
        if (resource != null) {
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            try {
                ResourceType type = em.find(ResourceType.class, resource.getResourceType().getId());
                Resource res = em.find(Resource.class, resource.getId());
                ResourceGroup group = res.getExplicitGroups().iterator().next();

                group.removeExplicitResource(res);
                em.remove(group);
                em.remove(res);
                em.remove(type);

                getTransactionManager().commit();
            } catch (Exception e) {
                try {
                    System.out.println(e);
                    getTransactionManager().rollback();
                } catch (Exception ignore) {
                }

                throw e;
            } finally {
                em.close();
            }
        }
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        try {
            newResource = createNewResource();
            newOperation = newResource.getResourceType().getOperationDefinitions().iterator().next();
            newGroup = newResource.getExplicitGroups().iterator().next();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        deleteNewResource(newResource);
    }
}