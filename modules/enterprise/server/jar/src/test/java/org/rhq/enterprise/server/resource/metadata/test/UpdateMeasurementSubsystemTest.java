package org.rhq.enterprise.server.resource.metadata.test;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.oob.MeasurementOutOfBounds;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.util.LookupUtil;

public class UpdateMeasurementSubsystemTest extends UpdateSubsytemTestBase {
    /**
     * Simple test for the update of a plugin where a server has some metrics that get in the second version of the
     * plugin added / changed / removed
     *
     * @throws Exception
     */
    @Test
    public void testUpdateMeasurementDefinitions() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/update-v1_0.xml");
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
                    assert def.getDefaultInterval() == 10000 : "DefaultInterval should be 10000 for Five in v1";
                }
            }

            // flush everything to disk
            getEntityManager().flush();

            // now hot deploy a new version of that plugin
            registerPlugin("./test/metadata/update-v2_0.xml");
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
                    assert def.getDefaultInterval() == 10000 : "DefaultInterval should still be 10000 for Five in v2";
                }
            }

            assert foundFour == true : "Four should be there in v2, but wasn't";

            // flush everything to disk
            getEntityManager().flush();

            // Now try the other way round
            registerPlugin("./test/metadata/update-v1_0.xml");
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
                    assert def.getDefaultInterval() == 10000 : "DefaultInterval should be 10000 for Five in v3";
                }
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Test if the full deletion of MeasurementDefinitions works JBNADM-1639
     *
     * @throws Exception
     */
    @Test
    public void testDeleteMeasurementDefinition() throws Exception {
        getTransactionManager().begin();
        try {
            { // extra block for variable scoping purposes
                registerPlugin("./test/metadata/measurementDeletion-v1_0.xml");
                ResourceType server = getResourceType("testServer1");
                Set<MeasurementDefinition> def = server.getMetricDefinitions();
                assert def.size() == 4;
            }

            { // extra block for variable scoping purposes
                registerPlugin("./test/metadata/measurementDeletion-v2_0.xml");
                ResourceType server = getResourceType("testServer1");
                Set<MeasurementDefinition> def = server.getMetricDefinitions();
                assert def.size() == 0;
            }

            { // extra block for variable scoping purposes
                registerPlugin("./test/metadata/measurementDeletion-v1_0.xml");
                ResourceType server = getResourceType("testServer1");
                Set<MeasurementDefinition> def = server.getMetricDefinitions();
                assert def.size() == 4;
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * I am renaming (= removing + adding ) a server with metrics on it.
     * Make sure all stuff hanging on the server gets deleted. So we need
     * to add some relations like MeasurementSchedules 
     * @throws Exception
     */
    @Test
    public void testRenameServer() throws Exception {

        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/update6-1.xml");
            ResourceType server = getResourceType("testServer1");

            EntityManager entityManager = getEntityManager();

            getPluginId(entityManager);

            Set<MeasurementDefinition> definitions1 = server.getMetricDefinitions();
            assert definitions1.size() == 1;

            /*
             * Create a Fake Resource and a MeasurementSchedule
             */
            Resource testResource = new Resource("-test-", "-test resource", server);
            entityManager.persist(testResource);

            MeasurementSchedule sched = new MeasurementSchedule(definitions1.iterator().next(), testResource);
            entityManager.persist(sched);
            entityManager.flush();
            MeasurementOutOfBounds oob = new MeasurementOutOfBounds(sched, System.currentTimeMillis(), 0.0);
            entityManager.persist(oob);
            MeasurementBaseline baseline = new MeasurementBaseline();
            baseline.setSchedule(sched);
            baseline.setUserEntered(true);
            entityManager.persist(baseline);
            EventDefinition eDef = new EventDefinition(server, "-test event definition-");
            entityManager.persist(eDef);

            setUpAgent(entityManager, testResource);

            getTransactionManager().commit();
        } catch (Exception e) {
            System.err.println("Setup of v1 failed");
            throw e;
        }

        // Set up done, now replace the plugin with a new one.

        try {
            getTransactionManager().begin();

            EntityManager entityManager = getEntityManager();
            entityManager.flush();

            System.out.println("Done with v1");

            try {
                registerPlugin("./test/metadata/update6-2.xml");
            } catch (Throwable t) {
                System.err.println(t);
            }
            ResourceType server;
            try {
                server = getResourceType("testServer1");
                assert server == null : "testServer1 found, but should not";
            } catch (NoResultException nre) {
                ; // no issue 
            }

            server = getResourceType("testServer2");
            assert server != null : "testServer2 not found";
            server2id = server.getId();
        } finally {
            getTransactionManager().commit();
        }

        // clean up
        try {
            getTransactionManager().begin();

            EntityManager entityManager = getEntityManager();

            ResourceType server = entityManager.find(ResourceType.class, server2id);
            Set<MeasurementDefinition> defs = server.getMetricDefinitions();
            if (defs != null) {
                Iterator<MeasurementDefinition> defIter = defs.iterator();
                while (defIter.hasNext()) {
                    MeasurementDefinition def = defIter.next();
                    // we can directly delete this, as there are no schdules on v2 defined.
                    entityManager.remove(def);
                    defIter.remove();
                }
            }
            ResourceType parent = server.getParentResourceTypes().iterator().next();
            entityManager.remove(server);
            entityManager.remove(parent);

            Agent agent = entityManager.getReference(Agent.class, agentId);
            entityManager.remove(agent);

            Plugin plugin1 = entityManager.getReference(Plugin.class, plugin1Id);
            entityManager.remove(plugin1);

        } finally {
            getTransactionManager().commit();
        }
    }

    /**
     * We define a plugin with a metric. Later we upgrade the plugin to add another metric.
     * The Metadatamamanger neeeds to add another {@link MeasurementSchedule} to it.
     * @throws Exception
     */
    @Test
    public void testAddScheduleOnExistingResources() throws Exception {

        Throwable savedThrowable = null;

        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/update7-1.xml");
            ResourceType platform = getResourceType("myPlatform7");

            EntityManager entityManager = getEntityManager();

            getPluginId(entityManager);

            Set<MeasurementDefinition> definitions1 = platform.getMetricDefinitions();
            assert definitions1.size() == 1;

            /*
             * Create a Fake Resource and a MeasurementSchedule
             */
            Resource testResource = new Resource("-test-", "-test resource", platform);
            entityManager.persist(testResource);

            setUpAgent(entityManager, testResource);

            getTransactionManager().commit();
        } catch (Exception e) {
            System.err.println("Setup of v1 failed");
            throw e;
        }

        // Set up done, now replace the plugin with a new one.

        try {
            getTransactionManager().begin();

            EntityManager entityManager = getEntityManager();
            entityManager.flush();

            System.out.println("Done with v1");

            try {
                registerPlugin("./test/metadata/update7-2.xml");
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
            savedThrowable = t;
        }

        // clean up
        try {
            getTransactionManager().begin();

            EntityManager entityManager = getEntityManager();
            resMgr = LookupUtil.getResourceManager();

            ResourceType platform = getResourceType("myPlatform7");
            List<Resource> resources = platform.getResources();
            Subject overlord = LookupUtil.getSubjectManager().getOverlord();
            for (Resource res : resources) {
                resMgr.deleteResource(overlord, res.getId());
            }

            Set<MeasurementDefinition> defs = platform.getMetricDefinitions();
            if (defs != null) {
                Iterator<MeasurementDefinition> defIter = defs.iterator();
                while (defIter.hasNext()) {
                    MeasurementDefinition def = defIter.next();
                    def = entityManager.merge(def);
                    entityManager.remove(def);
                    defIter.remove();
                }
            }

            platform = entityManager.getReference(ResourceType.class, platform.getId());
            entityManager.remove(platform);

            Agent agent = entityManager.getReference(Agent.class, agentId);
            entityManager.remove(agent);

            Plugin plugin1 = entityManager.getReference(Plugin.class, plugin1Id);
            entityManager.remove(plugin1);

        } finally {
            getTransactionManager().commit();
        }

        if (savedThrowable != null)
            throw new Exception(savedThrowable);
    }
}
