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
/**
 */
package org.rhq.core.domain.measurement.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import org.testng.annotations.Test;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementDataPK;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.measurement.oob.MeasurementOutOfBounds;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.test.AbstractEJB3Test;

/**
 * @author Heiko W. Rupp
 */
public class MeasurementTest extends AbstractEJB3Test {
    private Resource testPlatform;
    private Resource testPlatform2;

    @Test(groups = "integration.ejb3")
    public void testNewDefinition() throws SystemException, NotSupportedException, HeuristicMixedException,
        HeuristicRollbackException, RollbackException {
        getTransactionManager().begin();
        try {
            EntityManager entityManager = getEntityManager();

            /*
             *       Query a = entityManager.createQuery(         "SELECT md.defaultOn, md, md.defaultInterval, res " +
             *        "FROM Resource res JOIN res.resourceType rt JOIN rt.metricDefinitions md " +         "WHERE res.id
             * = 500051");      List result = a.getResultList();      System.out.println("FOUND: " + result);      for
             * (Object res : result)      {         System.out.print("ROW: ");
             *
             *    }
             *
             *    Resource resource = entityManager.find(Resource.class, 500051);
             *
             *    Query insertQuery = entityManager.createQuery(         "INSERT into MeasurementSchedule(enabled,
             * interval, definition, resource) " +         "SELECT md.defaultOn, md.defaultInterval, md, res " +
             * "FROM MeasurementDefinition md, Resource res " +         "WHERE md.resourceType.id = 500050 and res.id =
             * 500051");
             *
             *    //insertQuery.setParameter("resource", resource);
             *
             *    int created = insertQuery.executeUpdate();      System.out.println("Created entries: " + created);
             *
             */

            ResourceType resourceType = new ResourceType("fake platform", "fake plugin", ResourceCategory.PLATFORM,
                null);
            getEntityManager().persist(resourceType);
            MeasurementDefinition md = new MeasurementDefinition(resourceType, "Heiko");
            md.setDefaultOn(true);
            getEntityManager().persist(md);
            getEntityManager().flush();
        } finally {
            getTransactionManager().rollback();
        }
    }

    @SuppressWarnings("unchecked")
    @Test(groups = "integration.ejb3")
    public void testNewSchedule() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();
            MeasurementDefinition def = setupTables(em);
            int id = def.getId();

            MeasurementDefinition d = em.find(MeasurementDefinition.class, id);
            assert d != null : "Did not find the definition previously persisted";

            assert d.isDefaultOn();

            assert d.getSchedules() != null;

            MeasurementSchedule ms = d.getSchedules().get(0);
            assert ms != null;

            assert ms.isEnabled();

            assert ms.getBaseline() != null;

            MeasurementBaseline mbl = ms.getBaseline();

            assert !mbl.isUserEntered();

            List<MeasurementSchedule> l;

            Query qu = em.createNamedQuery("MeasurementSchedule.findForDefinition");
            qu.setParameter("definition", d);
            l = qu.getResultList();

            assert l != null;
            assert l.size() == 2;
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.ejb3")
    public void testUpdateBaseline() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();
            MeasurementDefinition def = setupTables(em);
            int id = def.getId();

            MeasurementDefinition md = em.find(MeasurementDefinition.class, id);
            List<MeasurementSchedule> schedules = md.getSchedules();
            MeasurementSchedule sch = schedules.get(0);
            MeasurementBaseline mb = sch.getBaseline();
            mb.setMax(3D);
            em.flush();
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Actually a test on how to handle things in JP-QL
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test(groups = "integration.ejb3")
    public void testScheduleUpdates() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            MeasurementDefinition def = setupTables(em);
            Query q = em.createQuery("SELECT ms FROM MeasurementSchedule ms WHERE ms.definition = :def");
            q.setParameter("def", def);
            List<MeasurementSchedule> scheds = q.getResultList();

            assert scheds.size() == 2;

            q = em.createQuery("UPDATE MeasurementSchedule AS ms SET ms.enabled = true WHERE ms IN (:scheds)");
            q.setParameter("scheds", scheds);
            int count = q.executeUpdate();

            assert count == 2;
        } finally {
            getTransactionManager().rollback();
        }
    }

    @SuppressWarnings("unchecked")
    @Test(groups = "integration.ejb3")
    public void testFindByResourceId() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            MeasurementDefinition def = setupTables(em);
            List<MeasurementSchedule> schedules = def.getSchedules();
            MeasurementSchedule sched = schedules.get(0);
            Resource res = sched.getResource();

            Query q = em.createNamedQuery(MeasurementDefinition.FIND_BY_RESOURCE_ID);
            q.setParameter("resId", res.getId());
            List<MeasurementDefinition> def2 = q.getResultList();
            assert def.equals(def2.get(0)) : "Got a different Definition back";
        } finally {
            getTransactionManager().rollback();
        }
    }

    @SuppressWarnings("unchecked")
    @Test(groups = "integration.ejb3")
    public void testFindScheduleByResourceAndDefinition() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            MeasurementDefinition def = setupTables(em);
            List<MeasurementSchedule> schedules = def.getSchedules();
            MeasurementSchedule sched = schedules.get(0);
            Resource res = sched.getResource();

            Query q = em.createNamedQuery(MeasurementSchedule.FIND_BY_DEFINITION_ID_AND_RESOURCE_IDS);
            q.setParameter("definitionId", def.getId());
            q.setParameter("resourceIds", res.getId());
            @SuppressWarnings("unused")
            List<MeasurementSchedule> scheds = q.getResultList();

            assert scheds.size() == 1 : "Did not find 1 schedule, but " + scheds.size();

            List<Integer> resourceIds = new ArrayList<Integer>();
            resourceIds.add(res.getId());
            resourceIds.add(res.getId());
            resourceIds.add(res.getId());
            resourceIds.add(res.getId());

            q = em.createNamedQuery(MeasurementSchedule.FIND_BY_DEFINITION_ID_AND_RESOURCE_IDS);
            q.setParameter("definitionId", def.getId());
            q.setParameter("resourceIds", resourceIds);
            scheds = q.getResultList();

            assert scheds.size() == 1 : "Did not find 1 schedule, but " + scheds.size();
        } finally {
            getTransactionManager().rollback();
        }
    }

    @SuppressWarnings("unchecked")
    @Test(groups = "integration.ejb3")
    public void testFindBaselinesToCalculate() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            setupTables(em);

            Thread.sleep(200);

            long now = System.currentTimeMillis();

            Query q = em.createNamedQuery(MeasurementSchedule.FIND_SCHEDULES_WITH_BASLINES_TO_CALC);
            q.setParameter("measType", NumericType.DYNAMIC);
            q.setParameter("ctime", now);
            List<MeasurementSchedule> schedules = q.getResultList();

            assert schedules.size() > 0 : "Should find at least one baseline";
        } finally {
            getTransactionManager().rollback();
        }
    }

    @SuppressWarnings("unchecked")
    @Test(groups = "integration.ejb3")
    public void testAddAvailability() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            MeasurementDefinition def = setupTables(em);

            Resource res = def.getSchedules().get(0).getResource();

            Availability beginUpTime = new Availability(res, getAnotherDate(), AvailabilityType.UP);
            em.persist(beginUpTime);
            em.flush();

            Date middleOfAvailabilityUP = getAnotherDate();

            Availability endUpTime = new Availability(res, getAnotherDate(), AvailabilityType.DOWN);
            em.persist(endUpTime);
            em.flush();

            beginUpTime.setEndTime(endUpTime.getStartTime());
            beginUpTime = em.merge(beginUpTime);
            em.flush();

            /*
             * We now have 2 AVailabilities for the resource FIND_CURRENT_BY_RESOURCE should return avail2
             */
            Query q = em.createNamedQuery(Availability.FIND_CURRENT_BY_RESOURCE);
            q.setParameter("resourceId", res.getId());
            Availability ava = (Availability) q.getSingleResult();

            assert ava.equals(endUpTime);

            Date afterAvailabilityDown = getAnotherDate();
            /*
             * Now try to get all relevant data for a certain time interval [ middleOfAvailabilityUP,
             * afterAvailabilityDown ]
             */

            q = em.createNamedQuery(Availability.FIND_FOR_RESOURCE_WITHIN_INTERVAL);
            q.setParameter("resourceId", res.getId());
            q.setParameter("start", middleOfAvailabilityUP.getTime());
            q.setParameter("end", afterAvailabilityDown.getTime());
            List<Availability> results = q.getResultList();

            /*
             * we should get beginUpTime availability because it straddles the middleOfAvailabilityUP we're passing; we
             * should get endUpTime availability because it starts between the two times we're passing
             */
            assert results.size() == 2;
        } finally {
            getTransactionManager().rollback();
        }
    }

    @SuppressWarnings("unchecked")
    @Test(groups = "integration.ejb3")
    public void testNewMeasurementOOB() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createNamedQuery(MeasurementOutOfBounds.QUERY_FIND_ALL_ADMIN);
            q.setParameter("oldest", 0L);
            List<MeasurementOutOfBounds> res = q.getResultList();
            int sizeOld = res.size();

            setupTables(em);

            MeasurementOutOfBounds oob = new MeasurementOutOfBounds(testPlatform.getSchedules().iterator().next(),
                System.currentTimeMillis(), 1);
            em.persist(oob);

            oob = new MeasurementOutOfBounds(testPlatform.getSchedules().iterator().next(),
                System.currentTimeMillis() - 60000, -20);
            em.persist(oob);

            em.flush();

            q = em.createNamedQuery(MeasurementOutOfBounds.QUERY_FIND_ALL_ADMIN);
            q.setParameter("oldest", 0L);
            res = q.getResultList();

            assert res.size() == (sizeOld + 2) : "Did not find my two stored OOBs";
        } finally {
            getTransactionManager().rollback();
        }
    }

    @SuppressWarnings("unchecked")
    @Test(groups = "integration.ejb3")
    public void testNewMeasurementOOBForResource() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            MeasurementDefinition def = setupTables(em);
            MeasurementSchedule sched = def.getSchedules().get(0);

            Query q = em.createNamedQuery(MeasurementOutOfBounds.QUERY_FIND_FOR_RESOURCE_ADMIN);
            q.setParameter("oldest", 0L);
            q.setParameter("resourceId", sched.getResource().getId());
            List<MeasurementOutOfBounds> res = q.getResultList();
            int sizeOld = res.size();

            q = em.createNamedQuery(MeasurementOutOfBounds.QUERY_FIND_FOR_SCHEDULE_ADMIN);
            q.setParameter("begin", 0L);
            q.setParameter("end", System.currentTimeMillis());
            q.setParameter("scheduleId", sched.getId());
            res = q.getResultList();
            assert res.size() == sizeOld;

            MeasurementOutOfBounds oob = new MeasurementOutOfBounds(sched, System.currentTimeMillis(), 33);
            em.persist(oob);

            q = em.createNamedQuery(MeasurementOutOfBounds.QUERY_FIND_FOR_RESOURCE_ADMIN);
            q.setParameter("oldest", 0L);
            q.setParameter("resourceId", sched.getResource().getId());
            res = q.getResultList();
            assert res.size() == (sizeOld + 1) : "Cant find my recently added OOB";

            q = em.createNamedQuery(MeasurementOutOfBounds.QUERY_FIND_FOR_SCHEDULE_ADMIN);
            q.setParameter("begin", 0L);
            q.setParameter("end", System.currentTimeMillis());
            q.setParameter("scheduleId", sched.getId());
            res = q.getResultList();
            assert res.size() == (sizeOld + 1) : "Cant find my recently added OOB";
        } finally {
            getTransactionManager().rollback();
        }
    }

    @SuppressWarnings("unchecked")
    @Test(groups = "integration.ejb3")
    public void testTraitQuery() throws Exception {
        System.out.println("testTraitQuery ...");
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            MeasurementDefinition def = setupTables(em);
            MeasurementSchedule sched = def.getSchedules().get(0);
            Resource resource = sched.getResource();
            int resourceId = resource.getId();

            MeasurementDataPK pk = new MeasurementDataPK(sched.getId());
            MeasurementDataTrait mdt = new MeasurementDataTrait(pk, "Hello World");
            em.persist(mdt);
            em.flush();

            Query q = em.createNamedQuery(MeasurementDataTrait.FIND_CURRENT_FOR_RESOURCE);
            q.setParameter("resourceId", resourceId);
            List<Object[]> res = q.getResultList();
            System.out.println("testTraitQuery: found " + res.size() + " item(s)");
            if (res.size() > 0) {
                MeasurementDataTrait foo = (MeasurementDataTrait) res.get(0)[0];
                String name = (String) res.get(0)[1];
                System.out.println("  and it is " + foo.toString() + " and name " + name);
            }

            Query q2 = em.createNamedQuery(MeasurementDataTrait.FIND_CURRENT_FOR_SCHEDULES);
            List<Integer> ids = new ArrayList<Integer>();
            ids.add(1);
            ids.add(2);
            ids.add(3);
            ids.add(sched.getId());
            q2.setParameter("scheduleIds", ids);
            List<MeasurementDataTrait> traits = q.getResultList();
            assert traits.size() >= 1; // at least the one for the schedule above should be found
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Setup some entities to check query against them etc.
     *
     * @param  em EntityManager to use
     *
     * @return the id of a created MeasurementDefinition
     */
    private MeasurementDefinition setupTables(EntityManager em) {
        ResourceType resourceType = new ResourceType("fake platform", "fake plugin", ResourceCategory.PLATFORM, null);
        em.persist(resourceType);
        Resource platform = new Resource("org.jboss.on.TestPlatfor", "Fake Platform", resourceType);
        em.persist(platform);
        Resource platform2 = new Resource("org.jboss.on.TestPlatform2", "Fake Platform2", resourceType);
        em.persist(platform2);

        MeasurementDefinition md = new MeasurementDefinition(resourceType, "Heiko");
        md.setDefaultOn(true);
        md.setDisplayName("Fake display name");
        md.setMeasurementType(NumericType.DYNAMIC);
        MeasurementSchedule sched = new MeasurementSchedule(md, platform);
        sched.setEnabled(true);
        MeasurementSchedule sched2 = new MeasurementSchedule(md, platform2);

        platform.addSchedule(sched);
        platform2.addSchedule(sched2);

        md.getSchedules().add(sched);
        md.getSchedules().add(sched2);
        em.persist(md);
        em.persist(sched);
        em.persist(sched2);

        em.flush();

        int id = md.getId();

        assert id != 0 : "id of MeasurementDefinition is 0, but should not be";

        em.persist(sched);
        MeasurementBaseline bl = new MeasurementBaseline();
        bl.setUserEntered(false);
        bl.setMax(1.2);
        bl.setMin(1.0);
        bl.setMean(1.1);
        bl.setSchedule(sched);
        em.persist(bl);

        em.flush();

        testPlatform = platform;
        testPlatform2 = platform2;

        return md;
    }
}