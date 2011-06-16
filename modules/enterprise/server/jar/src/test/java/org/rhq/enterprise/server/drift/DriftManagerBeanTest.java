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
package org.rhq.enterprise.server.drift;

import java.io.File;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftFileStatus;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Test for {@link DriftManagerBean} SLSB.
 */
@Test(groups = "drift-manager")
public class DriftManagerBeanTest extends AbstractEJB3Test {

    private static final boolean ENABLE_TESTS = true;

    private DriftManagerLocal driftManager;
    private Subject overlord;
    private Resource newResource;
    private DriftServerService driftServerService;

    /**
     * Prepares things for the entire test class.
     */
    @BeforeClass
    public void beforeClass() {
        driftManager = LookupUtil.getDriftManager();
        overlord = LookupUtil.getSubjectManager().getOverlord();

        driftServerService = new DriftServerServiceImpl();

        TestServerCommunicationsService agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.driftService = new TestConfigService();
    }

    @AfterClass
    public void afterClass() {
        driftServerService = null;
        unprepareForTestAgents();
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        prepareScheduler();

        deleteDriftFiles();

        newResource = createNewResource();
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() throws Exception {
        try {
            deleteNewResource(newResource);
        } finally {
            unprepareScheduler();
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testStoreChangeSet() throws Exception {
        // the initial changeset should not create any drift, just should request a drift file "0"
        File changeset1 = new File("./src/test/resources/org/rhq/enterprise/server/drift/changeset-1.zip");
        assertTrue(changeset1.exists());
        driftManager.storeChangeSet(newResource.getId(), changeset1);

        DriftChangeSetCriteria c = new DriftChangeSetCriteria();
        c.addFilterResourceId(newResource.getId());
        c.fetchDrifts(true);
        List<DriftChangeSet> changeSets = driftManager.findDriftChangeSetsByCriteria(overlord, c);
        assertEquals(1, changeSets.size());
        DriftChangeSet changeSet = changeSets.get(0);
        assertEquals(0, changeSet.getVersion());
        assertEquals(0, changeSet.getDrifts().size());

        DriftFile driftFile = driftManager.getDriftFile(overlord, "aaaaa");
        assertNotNull(driftFile);
        assertEquals(DriftFileStatus.REQUESTED, driftFile.getStatus());

        // the second change set should report drift
        File changeset2 = new File("./src/test/resources/org/rhq/enterprise/server/drift/changeset-2.zip");
        assertTrue(changeset2.exists());
        driftManager.storeChangeSet(newResource.getId(), changeset2);
        c.addSortVersion(PageOrdering.ASC);
        changeSets = driftManager.findDriftChangeSetsByCriteria(overlord, c);
        assertEquals(2, changeSets.size());
        changeSet = changeSets.get(0);
        assertEquals(0, changeSet.getVersion());
        assertEquals(0, changeSet.getDrifts().size());
        changeSet = changeSets.get(1);
        assertEquals(1, changeSet.getVersion());
        assertEquals(1, changeSet.getDrifts().size());
        Drift drift = changeSet.getDrifts().iterator().next();
        assertEquals("test/dir/filename.ext", drift.getPath());
        assertEquals("aaaaa", drift.getOldDriftFile().getSha256());
        assertEquals("bbbbb", drift.getNewDriftFile().getSha256());
        assertEquals(DriftCategory.FILE_CHANGED, drift.getCategory());

        driftFile = driftManager.getDriftFile(overlord, "bbbbb");
        assertNotNull(driftFile);
        assertEquals(DriftFileStatus.REQUESTED, driftFile.getStatus());
    }

    private void deleteDriftFiles() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {
            try {
                // wipe out any test DriftFiles (the test files have sha256 0,1,...)
                for (int i = 0, numDeleted = 1; (numDeleted > 0); ++i) {
                    numDeleted = getEntityManager().createQuery("delete from DriftFile where sha256 = '" + i + "'")
                        .executeUpdate();
                }
            } catch (Exception e) {
                System.out.println("CANNOT PREPARE TEST: " + e);
                getTransactionManager().rollback();
                throw e;
            }

            em.flush();
            getTransactionManager().commit();
        } finally {
            em.close();
        }
    }

    private Resource createNewResource() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        Resource resource;

        try {
            try {
                ResourceType resourceType = new ResourceType("plat" + System.currentTimeMillis(), "test",
                    ResourceCategory.PLATFORM, null);

                em.persist(resourceType);

                Agent agent = new Agent("testagent", "testaddress", 1, "", "testtoken");
                em.persist(agent);
                em.flush();

                resource = new Resource("reskey" + System.currentTimeMillis(), "resname", resourceType);
                resource.setUuid("" + new Random().nextInt());
                resource.setAgent(agent);
                resource.setInventoryStatus(InventoryStatus.COMMITTED);
                em.persist(resource);

            } catch (Exception e) {
                System.out.println("CANNOT PREPARE TEST: " + e);
                getTransactionManager().rollback();
                throw e;
            }

            em.flush();
            getTransactionManager().commit();
        } finally {
            em.close();
        }

        return resource;
    }

    private void deleteNewResource(Resource resource) throws Exception {
        if (null != resource) {
            EntityManager em = null;

            try {
                ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

                // invoke bulk delete on the resource to remove any dependencies not defined in the hibernate entity model
                // perform in-band and out-of-band work in quick succession
                List<Integer> deletedIds = resourceManager.uninventoryResource(overlord, resource.getId());
                for (Integer deletedResourceId : deletedIds) {
                    resourceManager.uninventoryResourceAsyncWork(overlord, deletedResourceId);
                }

                // now dispose of other hibernate entities
                getTransactionManager().begin();
                em = getEntityManager();

                ResourceType type = em.find(ResourceType.class, resource.getResourceType().getId());
                Agent agent = em.find(Agent.class, resource.getAgent().getId());
                if (null != agent) {
                    em.remove(agent);
                }
                if (null != type) {
                    em.remove(type);
                }

                getTransactionManager().commit();
            } catch (Exception e) {
                try {
                    System.out.println("CANNOT CLEAN UP TEST (" + this.getClass().getSimpleName() + ") Cause: " + e);
                    getTransactionManager().rollback();
                } catch (Exception ignore) {
                }
            } finally {
                if (null != em) {
                    em.close();
                }
            }
        }
    }

    private class TestConfigService implements DriftAgentService {

        @Override
        public boolean requestDriftFiles(List<DriftFile> driftFiles) {
            // pretend we actually processed the request
            return true;
        }

        @Override
        public void scheduleDriftDetection(int resourceId, DriftConfiguration driftConfiguration) {

        }

        @Override
        public void detectDrift(int resourceId, DriftConfiguration driftConfiguration) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}