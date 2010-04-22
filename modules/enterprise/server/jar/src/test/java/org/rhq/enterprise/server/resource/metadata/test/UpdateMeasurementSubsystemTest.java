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
package org.rhq.enterprise.server.resource.metadata.test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class UpdateMeasurementSubsystemTest extends UpdateSubsytemTestBase {

    static final boolean ENABLED = true;

    @Override
    protected String getSubsystemDirectory() {
        return "measurement";
    }

    /**
     * Simple test for the update of a plugin where a server has some metrics that get in the second version of the
     * plugin added / changed / removed
     *
     * @throws Exception
     */
    @Test(enabled = ENABLED)
    public void testUpdateMeasurementDefinitions() throws Exception {

        // cleanup previous run
        cleanupTest();

        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            registerPlugin("update-v1_0.xml");
            ResourceType server1 = getResourceType("testServer1");
            Set<MeasurementDefinition> definitions1 = server1.getMetricDefinitions();
            assert definitions1.size() == 4 : "There should be 4 metrics for v1";
            for (MeasurementDefinition def : definitions1) {
                if (def.getDisplayName().equals("Three")) {
                    assert def.getDisplayType() == DisplayType.DETAIL : "DisplayType for Three should be Detail in v1";
                }

                if (def.getDisplayName().equals("Five")) {
                    // this is a trick(y) one, as we do not want to honor updates
                    // of the default interval when a plugin was already deployed once and
                    // we do a redeploy
                    assert def.getDefaultInterval() == 30000L : "DefaultInterval should be 30000 for Five in v1";
                }
            }

            // now hot deploy a new version of that plugin
            registerPlugin("update-v2_0.xml");

            ResourceType server2 = getResourceType("testServer1");
            Set<MeasurementDefinition> definitions2 = server2.getMetricDefinitions();
            assert definitions2.size() == 4 : "There should be four metrics in v2";
            boolean foundFour = false;

            for (MeasurementDefinition def : definitions2) {
                assert !(def.getDisplayName().equals("One")) : "One should be gone in v2";
                if (def.getDisplayName().equals("Three")) {
                    assert def.getDisplayType() == DisplayType.SUMMARY : "DisplayType for Three should be Summary in v2";
                }

                if (def.getDisplayName().equals("Four")) {
                    foundFour = true;
                }

                if (def.getDisplayName().equals("Five")) {
                    // this is a trick(y) one, as we do not want to honor updates
                    // of the default interval when a plugin was already deployed once and
                    // we do a redeploy
                    assert def.getDefaultInterval() == 30000L : "DefaultInterval should still be 30000 for Five in v2";
                }
            }

            assert foundFour == true : "Four should be there in v2, but wasn't";

            // Now try the other way round
            registerPlugin("update-v1_0.xml", "3.0");
            ResourceType server3 = getResourceType("testServer1");
            Set<MeasurementDefinition> definitions3 = server3.getMetricDefinitions();
            assert definitions3.size() == 4 : "There should be 4 metrics for v3";
            for (MeasurementDefinition def : definitions3) {
                if (def.getDisplayName().equals("Three")) {
                    assert def.getDisplayType() == DisplayType.DETAIL : "DisplayType for Three should be Detail in v3";
                }

                if (def.getDisplayName().equals("Five")) {
                    // this is a trick(y) one, as we do not want to honor updates
                    // of the default interval when a plugin was already deployed once and
                    // we do a redeploy
                    assert def.getDefaultInterval() == 30000L : "DefaultInterval should be 30000 for Five in v3";
                }
            }
        } finally {
            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testUpdateMeasurementDefinition");
            }
        }
    }

    /**
     * Test if the full deletion of MeasurementDefinitions works JBNADM-1639
     *
     * @throws Exception
     */
    @Test(enabled = ENABLED)
    public void testDeleteMeasurementDefinition() throws Exception {
        // cleanup previous run
        cleanupTest();

        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.  
        try {
            { // extra block for variable scoping purposes
                registerPlugin("measurementDeletion-v1_0.xml");
                ResourceType server = getResourceType("testServer1");
                Set<MeasurementDefinition> def = server.getMetricDefinitions();
                assertEquals(4, def.size());
            }

            { // extra block for variable scoping purposes
                registerPlugin("measurementDeletion-v2_0.xml", "2.0");

                ResourceType server = getResourceType("testServer1");
                Set<MeasurementDefinition> def = server.getMetricDefinitions();
                assertEquals(0, def.size());
            }

            { // extra block for variable scoping purposes
                registerPlugin("measurementDeletion-v1_0.xml", "3.0");
                ResourceType server = getResourceType("testServer1");
                Set<MeasurementDefinition> def = server.getMetricDefinitions();
                assertEquals(4, def.size());
            }
        } finally {
            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testDeleteMeasurementDefinition");
            }
        }
    }

    /**
     * I am renaming (= removing + adding ) a server with metrics on it.
     * Make sure all stuff hanging on the server gets deleted. So we need
     * to add some relations like MeasurementSchedules 
     * @throws Exception
     */
    @Test(enabled = ENABLED)
    public void testRenameServer() throws Exception {

        // cleanup previous run
        cleanupTest();

        Resource testResource = null;
        MeasurementSchedule sched = null;
        MeasurementBaseline baseline = null;
        EventDefinition eDef = null;

        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.  Still, some work can be performed transactionally, as done below.
        getTransactionManager().begin();

        try {
            try {
                registerPlugin("update6-1.xml");
                ResourceType server = getResourceType("testServer1");

                EntityManager entityManager = getEntityManager();

                getPluginId(entityManager);

                Set<MeasurementDefinition> definitions1 = server.getMetricDefinitions();
                assert definitions1.size() == 1;

                /*
                 * Create a Fake Resource and a MeasurementSchedule
                 */
                testResource = new Resource("-test-", "-test resource", server);
                testResource.setUuid("" + new Random().nextInt());
                entityManager.persist(testResource);

                sched = new MeasurementSchedule(definitions1.iterator().next(), testResource);
                entityManager.persist(sched);

                entityManager.flush();

                baseline = new MeasurementBaseline();
                baseline.setSchedule(sched);
                baseline.setUserEntered(true);
                entityManager.persist(baseline);

                eDef = new EventDefinition(server, "-test event definition-");
                entityManager.persist(eDef);

                setUpAgent(entityManager, testResource);

                getTransactionManager().commit();
            } catch (Exception e) {
                getTransactionManager().rollback();
                fail("Setup of v1 failed: " + e);
            }

            // Set up done, now replace the plugin with a new one.

            try {
                getTransactionManager().begin();

                EntityManager entityManager = getEntityManager();
                entityManager.flush();

                System.out.println("Done with v1");

                registerPlugin("update6-2.xml");

                ResourceType server;
                try {
                    server = getResourceType("testServer1");
                    assert server == null : "testServer1 found, but should not";
                } catch (NoResultException nre) {
                    ; // no issue 
                }

                server = getResourceType("testServer2");
                assert server != null : "testServer2 not found";

                getTransactionManager().commit();
            } catch (Throwable t) {
                getTransactionManager().rollback();
                fail("update failed: " + t);
            }
        } finally {
            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName() + ".testRenameServer");
            }
        }
    }

    /**
     * We define a plugin with a metric. Later we upgrade the plugin to add another metric.
     * The Metadatamamanger neeeds to add another {@link MeasurementSchedule} to it.
     * @throws Exception
     */
    @Test(enabled = ENABLED)
    public void testAddScheduleOnExistingResources() throws Exception {

        Throwable savedThrowable = null;

        // cleanup previous run
        cleanupTest();

        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.  Still, some work can be performed transactionally, as done below.
        getTransactionManager().begin();

        try {
            try {
                registerPlugin("update7-1.xml");
                ResourceType platform = getResourceType("myPlatform7");

                EntityManager entityManager = getEntityManager();

                getPluginId(entityManager);

                Set<MeasurementDefinition> definitions1 = platform.getMetricDefinitions();
                assert definitions1.size() == 1;

                /*
                 * Create a Fake Resource and a MeasurementSchedule
                 */
                Resource testResource = new Resource("-test-", "-test resource", platform);
                testResource.setUuid("" + new Random().nextInt());
                entityManager.persist(testResource);

                setUpAgent(entityManager, testResource);

                getTransactionManager().commit();
            } catch (Exception e) {
                getTransactionManager().rollback();
                fail("Setup of v1 failed: " + e);
            }

            // Set up done, now replace the plugin with a new one.

            try {
                getTransactionManager().begin();

                EntityManager entityManager = getEntityManager();
                entityManager.flush();

                System.out.println("Done with v1");

                try {
                    registerPlugin("update7-2.xml");
                } catch (Throwable t) {
                    System.err.println(t);
                }

                ResourceType platform = getResourceType("myPlatform7");
                Set<MeasurementDefinition> definitions2 = platform.getMetricDefinitions();
                assert definitions2.size() == 2;

                List<Resource> resources = platform.getResources();
                assert resources != null;
                assert resources.size() == 1;

                Resource res = resources.get(0);
                Set<MeasurementSchedule> schedules = res.getSchedules();
                assert schedules != null;
                /*
                 * We only expect one schedule for the freshly created metric, as the resource we
                 * created earlier has no schedule attached yet (in the test).
                 * In the "real world" it would get its schedules on inventory sync time.
                 */
                assert schedules.size() == 1 : "Did not find the expected 1 new schedule, but: " + schedules.size();
                getTransactionManager().commit();

            } catch (Throwable t) {
                getTransactionManager().rollback();
                fail("update failed: " + t);
            }
        } finally {
            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName() + ".testRenameServer");
            }
        }

        if (savedThrowable != null)
            throw new Exception(savedThrowable);
    }

    private void cleanupTest() throws Exception {
        try {
            cleanupResourceType("testServer1");
            cleanupResourceType("testServer2");
            cleanupResourceType("myPlatform7");

            getTransactionManager().begin();
            EntityManager entityManager = getEntityManager();

            try {
                agentId = getAgentId(entityManager);
                Agent agent = entityManager.getReference(Agent.class, agentId);
                if (null != agent) {
                    entityManager.remove(agent);
                }
            } catch (Exception e) {
                // ignore, agent does not exist
            }

            try {
                plugin1Id = getPluginId(entityManager);
                Plugin plugin1 = entityManager.getReference(Plugin.class, plugin1Id);
                if (null != plugin1) {
                    entityManager.remove(plugin1);
                }
            } catch (Exception e) {
                // ignore, plugin1 does not exist
            }

        } finally {
            getTransactionManager().commit();
        }
    }

    private void cleanupResourceType(String rtName) throws Exception {
        ResourceType rt = getResourceType(rtName);

        if (null != rt) {
            Subject overlord = LookupUtil.getSubjectManager().getOverlord();
            ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

            // delete any resources first
            ResourceCriteria c = new ResourceCriteria();
            c.addFilterResourceTypeId(rt.getId());
            c.addFilterInventoryStatus(InventoryStatus.NEW);
            List<Resource> doomedResources = resourceManager.findResourcesByCriteria(overlord, c);
            c.addFilterInventoryStatus(InventoryStatus.DELETED);
            doomedResources.addAll(resourceManager.findResourcesByCriteria(overlord, c));
            c.addFilterInventoryStatus(InventoryStatus.UNINVENTORIED);
            doomedResources.addAll(resourceManager.findResourcesByCriteria(overlord, c));
            // invoke bulk delete on the resource to remove any dependencies not defined in the hibernate entity model
            // perform in-band and out-of-band work in quick succession
            for (Resource doomed : doomedResources) {
                List<Integer> deletedIds = resourceManager.deleteResource(overlord, doomed.getId());
                for (Integer deletedResourceId : deletedIds) {
                    resourceManager.deleteSingleResourceInNewTransaction(overlord, deletedResourceId);
                }
            }

            // Measurement defs go away via cascade remove
            /*
            Set<MeasurementDefinition> defs = server.getMetricDefinitions();
            if (defs != null) {
                Iterator<MeasurementDefinition> defIter = defs.iterator();
                while (defIter.hasNext()) {
                    MeasurementDefinition def = defIter.next();
                    // we can directly delete this, as there are no schdules on v2 defined.
                    em.remove(def);
                    defIter.remove();
                }
            }
            */

            getTransactionManager().begin();
            EntityManager em = getEntityManager();

            rt = em.find(ResourceType.class, rt.getId());
            ResourceType parent = rt.getParentResourceTypes().isEmpty() ? null : rt.getParentResourceTypes().iterator()
                .next();
            em.remove(rt);
            if (null != parent) {
                em.remove(parent);
            }

            getTransactionManager().commit();
        }
    }

}
