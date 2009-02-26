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
package org.rhq.enterprise.server.measurement.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementOOB;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementOOBManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

@Test
public class MeasurementBaselineManagerTest extends AbstractEJB3Test {
    private Agent agent;
    private ResourceType platformType;
    private Resource platform;
    private Resource platform2;
    private MeasurementDefinition measDef;
    private MeasurementSchedule measSched;
    private MeasurementSchedule measSched2;
    private EntityManager entityManager;
    private ResourceManagerLocal resourceManager;
    private MeasurementBaselineManagerLocal baselineManager;
    private MeasurementOOBManagerLocal oobManager;
    private Subject overlord;

    // for the large inventory test
    private List<Resource> allResources;
    private List<MeasurementDefinition> allDefs;
    private List<MeasurementSchedule> allScheds;

    @BeforeMethod
    public void beforeMethod() throws Exception {
        this.prepareScheduler();
        this.resourceManager = LookupUtil.getResourceManager();
        this.baselineManager = LookupUtil.getMeasurementBaselineManager();
        this.oobManager = LookupUtil.getOOBManager();
        this.overlord = LookupUtil.getSubjectManager().getOverlord();
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        this.unprepareScheduler();
    }

    /**
     * Tests auto-calculation with a large inventory.
     *
     * @throws Throwable
     */
    public void testAutoBaselineCalculationsWithLargeInventory() throws Throwable {
        long startingTime;

        begin();

        try {
            setupManyResources(entityManager, 20, 10);

            long now = System.currentTimeMillis();
            long eldest = now - 180000;
            long elder = now - 120000;
            long young = now - 60000;
            long youngest = now;

            int dataCount = allScheds.size();
            for (MeasurementSchedule sched : allScheds) {
                insertMeasurementDataNumeric1H(eldest, sched, 30.0, 20.0, 40.0);
                insertMeasurementDataNumeric1H(elder, sched, 5.0, 2.0, 8.0);
                insertMeasurementDataNumeric1H(young, sched, 6.0, 3.0, 9.0);
                insertMeasurementDataNumeric1H(youngest, sched, 40.0, 30.0, 50.0);
                if ((--dataCount % 500) == 0) {
                    System.out.println(String.valueOf(dataCount) + " more test measurement data left to insert");
                    commitAndBegin();
                }
            }

            commit();

            startingTime = System.currentTimeMillis();

            long computeTime = baselineManager.calculateAutoBaselines(90000, System.currentTimeMillis());
            assert computeTime > 0;

            System.out.println(">>>>>>> a) [" + allScheds.size() + "] baselines calculated in ["
                + (System.currentTimeMillis() - startingTime) + "] ms");

            // calculate them again, the delete query will be triggered this time
            startingTime = System.currentTimeMillis();
            computeTime = baselineManager.calculateAutoBaselines(90000, System.currentTimeMillis());
            assert computeTime > 0;

            System.out.println(">>>>>>> b) [" + allScheds.size() + "] baselines calculated in ["
                + (System.currentTimeMillis() - startingTime) + "] ms");

            begin();
            deleteManyResources();
            commitAndBegin(); // this ensures we delete everything
        } catch (Throwable t) {
            System.out.println("TEST FAILURE STACK TRACE FOLLOWS:");
            t.printStackTrace();
            throw t;
        } finally {
            try {
                getTransactionManager().rollback();
            } catch (Exception e) {
            }

            allScheds.clear();
            allDefs.clear();
            allResources.clear();
            allScheds = null;
            allDefs = null;
            allResources = null;
        }
    }

    /**
     * This is a very important test - it tests native queries that are hardcoded. We need this test because just
     * deploying the EJB3 entities does not ensure these native queries didn't break; running this test will exercise
     * the native queries.
     *
     * @throws Throwable
     */
    public void testAutoBaselineCalculations() throws Throwable {
        begin();

        try {
            setupResources(entityManager);
            assert entityManager.find(Resource.class, platform.getId()) != null : "Did not setup platform - cannot test";

            long now = System.currentTimeMillis();
            long eldest = now - 180000;
            long elder = now - 120000;
            long young = now - 60000;
            long youngest = now;

            insertMeasurementDataNumeric1H(0, measSched, 0.0, 0.0, 0.0);
            insertMeasurementDataNumeric1H(eldest, measSched, 30.0, 20.0, 40.0);
            insertMeasurementDataNumeric1H(elder, measSched, 5.0, 2.0, 8.0);
            insertMeasurementDataNumeric1H(young, measSched, 6.0, 3.0, 9.0);
            insertMeasurementDataNumeric1H(youngest, measSched, 40.0, 30.0, 50.0);

            insertMeasurementDataNumeric1H(0, measSched2, 0.0, 0.0, 0.0);
            insertMeasurementDataNumeric1H(eldest, measSched2, 5000.0, 3500.0, 6500.0);
            insertMeasurementDataNumeric1H(elder, measSched2, 5000.0, 3000.0, 7000.0);
            insertMeasurementDataNumeric1H(young, measSched2, 2000.0, 1000.0, 3000.0);
            insertMeasurementDataNumeric1H(youngest, measSched2, 1500.0, 500.0, 2500.0);

            commit();

            // pass now for olderThanTime to ensure all existing baselines are deleted
            // pass 30000 for amountOfData to only include the youngest in the baseline calculation
            long computeTime = baselineManager.calculateAutoBaselines(30000, System.currentTimeMillis());
            assert computeTime > 0;

            MeasurementBaseline bl1;
            MeasurementBaseline bl2;
            int bl1Id;
            int bl2Id;
            Date bl1ComputeTime;
            Date bl2ComputeTime;

            begin();

            bl1 = entityManager.find(MeasurementSchedule.class, measSched.getId()).getBaseline();
            assert bl1 != null : "Baseline for measSched should have been inserted";
            assert bl1.getSchedule().getId() == measSched.getId();
            assert !bl1.isUserEntered();
            assertEquals(30.0, bl1.getMin());
            assertEquals(50.0, bl1.getMax());
            assertEquals(40.0, bl1.getMean());

            bl2 = entityManager.find(MeasurementSchedule.class, measSched2.getId()).getBaseline();
            assert bl2 != null : "Baseline for measSched2 should have been inserted";
            assert bl2.getSchedule().getId() == measSched2.getId();
            assert !bl2.isUserEntered();
            assertEquals(500.0, bl2.getMin());
            assertEquals(2500.0, bl2.getMax());
            assertEquals(1500.0, bl2.getMean());

            // remember these, the next time we calculate, they will be deleted and new ones created
            bl1Id = bl1.getId();
            bl2Id = bl2.getId();
            bl1ComputeTime = bl1.getComputeTime();
            bl2ComputeTime = bl2.getComputeTime();

            commit();

            // calculate them again, the values will be the same, but the delete query will be triggered
            // wait a bit so our compute time will be assured to be different
            Thread.sleep(1000L);
            computeTime = baselineManager.calculateAutoBaselines(30000, System.currentTimeMillis());
            assert computeTime > 0;

            begin();

            bl1 = entityManager.find(MeasurementSchedule.class, measSched.getId()).getBaseline();
            assert bl1 != null : "Baseline for measSched should have been inserted";
            assert bl1.getSchedule().getId() == measSched.getId();
            assert !bl1.isUserEntered();
            assertEquals(30.0, bl1.getMin());
            assertEquals(50.0, bl1.getMax());
            assertEquals(40.0, bl1.getMean());

            bl2 = entityManager.find(MeasurementSchedule.class, measSched2.getId()).getBaseline();
            assert bl2 != null : "Baseline for measSched2 should have been inserted";
            assert bl2.getSchedule().getId() == measSched2.getId();
            assert !bl2.isUserEntered();
            assertEquals(500.0, bl2.getMin());
            assertEquals(2500.0, bl2.getMax());
            assertEquals(1500.0, bl2.getMean());

            // check the new IDs with the old ones - they should be different due to the delete query
            assert bl1.getId() != bl1Id : "bl1.getId() was " + bl1.getId() + ", bl1Id was " + bl1Id;
            assert bl2.getId() != bl2Id : "bl2.getId() was " + bl2.getId() + ", bl2Id was " + bl2Id;
            assert bl1.getComputeTime().after(bl1ComputeTime);
            assert bl2.getComputeTime().after(bl2ComputeTime);

            commit();

            // calculate them again using wider ranges to include more (all available) 1H data in the calculation
            Thread.sleep(1000L);
            computeTime = baselineManager.calculateAutoBaselines(240000, System.currentTimeMillis());
            assert computeTime > 0;

            begin();

            bl1 = entityManager.find(MeasurementSchedule.class, measSched.getId()).getBaseline();
            assert bl1 != null : "Baseline for measSched should have been inserted";
            assert bl1.getSchedule().getId() == measSched.getId();
            assert !bl1.isUserEntered();
            assertEquals(2.0, bl1.getMin());
            assertEquals(50.00, bl1.getMax());
            assertEquals(20.25, bl1.getMean());

            bl2 = entityManager.find(MeasurementSchedule.class, measSched2.getId()).getBaseline();
            assert bl2 != null : "Baseline for measSched2 should have been inserted";
            assert bl2.getSchedule().getId() == measSched2.getId();
            assert !bl2.isUserEntered();
            assertEquals(500.0, bl2.getMin());
            assertEquals(7000.0, bl2.getMax());
            assertEquals(3375.0, bl2.getMean());

            // check the new IDs with the old ones - they should be different due to the delete query
            assert bl1.getId() != bl1Id;
            assert bl2.getId() != bl2Id;
            assert bl1.getComputeTime().after(bl1ComputeTime);
            assert bl2.getComputeTime().after(bl2ComputeTime);

            commit();

            deleteResources();

            begin();
        } catch (Throwable t) {
            System.out.println("TEST FAILURE STACK TRACE FOLLOWS:");
            t.printStackTrace();
            throw t;
        } finally {
            try {
                getTransactionManager().rollback();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Calculate Out of Bound values. Those need to use baselines, so they are tested here
     * @throws Throwable If anything goes wrong.
     */
    @SuppressWarnings("unchecked")
    public void testCalculateOOB() throws Throwable {

        begin();

        try {
            setupResources(entityManager);
            assert entityManager.find(Resource.class, platform.getId()) != null : "Did not setup platform - cannot test";

            long now = System.currentTimeMillis();
            long eldest = now - 180000;
            long elder = now - 120000;
            long young = now - 60000;
            long youngest = now;

            insertMeasurementDataNumeric1H(0, measSched, 0.0, 0.0, 0.0);
            insertMeasurementDataNumeric1H(eldest, measSched, 30.0, 20.0, 40.0);
            insertMeasurementDataNumeric1H(elder, measSched, 5.0, 2.0, 8.0);
            insertMeasurementDataNumeric1H(young, measSched, 6.0, 3.0, 9.0);
            insertMeasurementDataNumeric1H(youngest, measSched, 40.0, 30.0, 50.0);

            insertMeasurementDataNumeric1H(0, measSched2, 0.0, 0.0, 0.0);
            insertMeasurementDataNumeric1H(eldest, measSched2, 5000.0, 3500.0, 6500.0);
            insertMeasurementDataNumeric1H(elder, measSched2, 5000.0, 3000.0, 7000.0);
            insertMeasurementDataNumeric1H(young, measSched2, 2000.0, 1000.0, 3000.0);
            insertMeasurementDataNumeric1H(youngest, measSched2, 1500.0, 500.0, 2500.0);

            commit();

            long computeTime = baselineManager.calculateAutoBaselines(30000, System.currentTimeMillis());
            assert computeTime > 0;

            begin();
            // check the 2 values at 5000 against their bands computed between [500 and 1000]
            oobManager.computeOOBsFromHourBeginingAt(overlord, eldest);

            // check results
            Query q = entityManager.createQuery("SELECT oo FROM MeasurementOOB oo");
            List<MeasurementOOB> oobs = q.getResultList();
            System.out.println("OOBs calculated: \n" + oobs);
            for (MeasurementOOB oob : oobs) {
                if (oob.getScheduleId() == measSched.getId()) {
                    assert oob.getOobFactor() == 50 : "Expected: 586, was " + oob.getOobFactor();
                } else {
                    assert oob.getOobFactor() == 200 : "Expected: 8, was " + oob.getOobFactor();
                }
            }

            PageControl pc = PageControl.getUnlimitedInstance();

            List<MeasurementOOBComposite> comps = oobManager.getSchedulesWithOOBs(overlord, System.currentTimeMillis(),
                pc, null ,null );
            //         System.out.println("Composites: " + comps);
            assert comps.size() == 2 : "Expected 2 composites, but got " + comps.size();

            comps = oobManager.getHighestNOOBsForResource(overlord, System.currentTimeMillis(), platform.getId(), 2);
            assert comps.size() == 1 : "Expected 1 composite, but got " + comps.size();

            // Compute some more OOBs
            oobManager.computeOOBsFromHourBeginingAt(overlord, elder);
            q = entityManager.createQuery("SELECT oo FROM MeasurementOOB oo");
            oobs = q.getResultList();
            //    System.out.println("OOBs calculated: \n" + oobs);

            comps = oobManager.getSchedulesWithOOBs(overlord, elder, pc, null , null);
            //     System.out.println("Composites: " + comps);
            assert comps.size() == 2 : "Expected 2, but was " + comps.size();

            commit();

            // Clean up

            begin();

            q = entityManager
                .createQuery("DELETE FROM MeasurementOOB oo WHERE  oo.schedule = :sched1 OR oo.schedule = :sched2");
            q.setParameter("sched1", measSched);
            q.setParameter("sched2", measSched2);
            q.executeUpdate();
            commit();

            deleteResources();
        } catch (Throwable t) {
            System.out.println("TEST FAILURE STACK TRACE FOLLOWS:");
            t.printStackTrace();
            throw t;
        } finally {
            try {
                getTransactionManager().rollback();
            } catch (Exception e) {
            }
        }

    }

    private void setupResources(EntityManager em) {
        agent = new Agent("test-agent", "localhost", 1234, "", "randomToken");
        em.persist(agent);

        platformType = new ResourceType("testplatAB", "p", ResourceCategory.PLATFORM, null);
        em.persist(platformType);

        platform = new Resource("platform1", "testAutoBaseline Platform One", platformType);
        em.persist(platform);
        platform.setAgent(agent);

        platform2 = new Resource("platform2", "testAutoBaseline Platform Two", platformType);
        em.persist(platform2);
        platform2.setAgent(agent);

        measDef = new MeasurementDefinition(platformType, "testAutoBaseline");
        measDef.setDefaultOn(true);
        measDef.setDisplayName("testAutoBaseline Measurement Display Name");
        measDef.setMeasurementType(NumericType.DYNAMIC);
        em.persist(measDef);

        measSched = new MeasurementSchedule(measDef, platform);
        measSched.setEnabled(true);
        platform.addSchedule(measSched);
        measDef.addSchedule(measSched);
        em.persist(measSched);

        measSched2 = new MeasurementSchedule(measDef, platform2);
        measSched2.setEnabled(true);
        platform2.addSchedule(measSched2);
        measDef.addSchedule(measSched2);
        em.persist(measSched2);
    }

    private void deleteResources() {
        Object doomed;

        try {
            resourceManager.deleteSingleResourceInNewTransaction(overlord, platform);
            resourceManager.deleteSingleResourceInNewTransaction(overlord, platform2);

            begin();

            if ((doomed = entityManager.find(Agent.class, agent.getId())) != null) {
                entityManager.remove(doomed);
            }

            deleteMeasurementDataNumeric1H(measSched);
            deleteMeasurementDataNumeric1H(measSched2);

            if ((doomed = entityManager.find(MeasurementSchedule.class, measSched.getId())) != null) {
                entityManager.remove(doomed);
            }

            if ((doomed = entityManager.find(MeasurementSchedule.class, measSched2.getId())) != null) {
                entityManager.remove(doomed);
            }

            if ((doomed = entityManager.find(MeasurementDefinition.class, measDef.getId())) != null) {
                entityManager.remove(doomed);
            }

            if ((doomed = entityManager.find(ResourceType.class, platformType.getId())) != null) {
                entityManager.remove(doomed);
            }

            commit();
        } catch (Exception e) {
            System.out.println("Cannot delete test resources, database still has test data in it");
            e.printStackTrace();
        }
    }

    private void setupManyResources(EntityManager em, int resourceCount, int measurementCount) {
        allResources = new ArrayList<Resource>(resourceCount);
        allDefs = new ArrayList<MeasurementDefinition>(measurementCount);
        allScheds = new ArrayList<MeasurementSchedule>(resourceCount * measurementCount);

        platformType = new ResourceType("testplatAB", "p", ResourceCategory.PLATFORM, null);
        em.persist(platformType);

        for (int m = 0; m < measurementCount; m++) {
            MeasurementDefinition def = new MeasurementDefinition(platformType, "testAutoBaselineMeasDef" + m);
            def.setDefaultOn(true);
            def.setDisplayName("testAutoBaseline Measurement Display Name" + m);
            def.setMeasurementType(NumericType.DYNAMIC);
            em.persist(def);
            allDefs.add(def);
        }

        em.flush();
        em.clear();

        System.out.println("Populating test inventory with [" + resourceCount + "] resources.");

        agent = new Agent("test-agent", "localhost", 1234, "", "randomToken");
        em.persist(agent);

        for (int r = 0; r < resourceCount; r++) {
            Resource resource = new Resource(String.valueOf(r), "testAutoBaselineResource" + r, platformType);
            em.persist(resource);
            allResources.add(resource);
            resource.setAgent(agent);

            for (MeasurementDefinition def : allDefs) {
                MeasurementSchedule sched = new MeasurementSchedule(def, resource);
                sched.setEnabled(true);
                resource.addSchedule(sched);
                def.addSchedule(sched);
                em.persist(sched);
                allScheds.add(sched);
            }

            if ((r % 50) == 0) {
                System.out.println("..." + r);
                em.flush();
                em.clear();
            }
        }

        em.flush();
        em.clear();
        System.out.println("Test inventory now has [" + resourceCount + "] resources.");

        return;
    }

    private void deleteManyResources() {
        Object doomed;

        try {
            int dataCount = allScheds.size();
            for (MeasurementSchedule doomedSched : allScheds) {
                deleteMeasurementDataNumeric1H(doomedSched);
                if ((--dataCount % 1000) == 0) {
                    System.out.println(String.valueOf(dataCount) + " more test measurement data left to delete");
                    commitAndBegin();
                }
            }

            // delete the resources which will cascade delete all schedules
            for (Resource doomedRes : allResources) {
                resourceManager.deleteSingleResourceInNewTransaction(overlord, doomedRes);
            }

            if ((doomed = entityManager.find(Agent.class, agent.getId())) != null) {
                entityManager.remove(doomed);
            }

            for (MeasurementDefinition doomedDef : allDefs) {
                if ((doomed = entityManager.find(MeasurementDefinition.class, doomedDef.getId())) != null) {
                    entityManager.remove(doomed);
                }
            }

            entityManager.flush();
            entityManager.clear();

            if ((doomed = entityManager.find(ResourceType.class, platformType.getId())) != null) {
                entityManager.remove(doomed);
            }

        } catch (Exception e) {
            System.out.println("Cannot delete test resources, database still has test data in it");
            e.printStackTrace();
        }
    }

    // commitAndBegin, commit, begin are used mainly when testing in a debugger
    // so you can commit the data, and examine it in an external SQL tool without
    // worrying about transactions timing out within the paused test thread.
    private void commitAndBegin() throws Exception {
        commit();
        begin(); // put a breakpoint here and call commitAndBegin at a place where you want to pause the test
    }

    private void commit() throws Exception {
        entityManager.flush();
        getTransactionManager().commit();
        entityManager.close();
    }

    private void begin() throws Exception {
        getTransactionManager().begin();
        entityManager = getEntityManager();
    }

    private void insertMeasurementDataNumeric1H(long timeStamp, MeasurementSchedule schedule, double value, double min,
        double max) {
        String sql = "INSERT INTO RHQ_measurement_data_num_1h "
            + "(time_stamp, schedule_id, value, minvalue, maxvalue) " + "VALUES (" + timeStamp + "," + schedule.getId()
            + "," + value + "," + min + "," + max + ")";

        Query q = entityManager.createNativeQuery(sql);
        assert q.executeUpdate() == 1;
    }

    private void deleteMeasurementDataNumeric1H(MeasurementSchedule schedule) {
        String sql = "DELETE FROM RHQ_measurement_data_num_1h WHERE schedule_id = " + schedule.getId();

        Query q = entityManager.createNativeQuery(sql);
        q.executeUpdate();
    }
}