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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityPoint;
import org.rhq.enterprise.server.resource.ResourceAvailabilityManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Test the functionality of the AvailabilityManager
 *
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 */
public class AvailabilityManagerTest extends AbstractEJB3Test {
    private static final boolean ENABLE_TESTS = false;

    private static final AvailabilityType UP = AvailabilityType.UP;
    private static final AvailabilityType DOWN = AvailabilityType.DOWN;

    private AvailabilityManagerLocal availabilityManager;
    private ResourceAvailabilityManagerLocal resourceAvailabilityManager;
    private ResourceManagerLocal resourceManager;

    private Subject overlord;
    private Agent theAgent;
    private Resource theResource;
    private ResourceType theResourceType;
    private Availability availability1;
    private Availability availability2;
    private Availability availability3;

    @BeforeMethod
    public void beforeMethod() {
        try {
            prepareScheduler();

            this.availabilityManager = LookupUtil.getAvailabilityManager();
            this.resourceAvailabilityManager = LookupUtil.getResourceAvailabilityManager();
            this.resourceManager = LookupUtil.getResourceManager();
            this.overlord = LookupUtil.getSubjectManager().getOverlord();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        try {
            if (theResource != null) {
                // perform in-band and out-of-band work in quick succession
                // this also deletes our attached agent
                resourceManager.uninventoryResource(overlord, theResource.getId());
                resourceManager.uninventoryResourceAsyncWork(overlord, theResource.getId());
                theResource = null;
            }

            if (theResourceType != null) {
                getTransactionManager().begin();
                EntityManager em = getEntityManager();

                em.remove(em.find(ResourceType.class, theResourceType.getId()));
                theResourceType = null;

                getTransactionManager().commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        finally {
            unprepareScheduler();
        }
    }

    @SuppressWarnings("unchecked")
    @Test(enabled = true)
    public void testPurgeAvailabilities() throws Exception {
        Date now = new Date();
        Date middle = new Date(now.getTime() - 30000); // 30s ago
        Date then = new Date(now.getTime() - 60000); // 60s ago

        EntityManager em = beginTx();

        try {
            setupResource(em);
            commitAndClose(em);

            Availability aThen = new Availability(theResource, then, AvailabilityType.UP);
            aThen.setEndTime(middle);

            Availability aMiddle = new Availability(theResource, middle, AvailabilityType.DOWN);
            aMiddle.setEndTime(now);

            Availability aNow = new Availability(theResource, now, AvailabilityType.UP);

            persistAvailability(aThen);
            persistAvailability(aMiddle);
            persistAvailability(aNow);

            em = beginTx();

            int purged = availabilityManager.purgeAvailabilities(new Long(now.getTime() - 29999)); // keeps aMiddle and aNow
            assert purged == 1 : "Didn't purge 1 --> " + purged;

            Query q = em.createNamedQuery(Availability.FIND_BY_RESOURCE);
            q.setParameter("resourceId", theResource.getId());
            List<Availability> avails = q.getResultList();

            assert avails.size() == 2;
            assert avails.get(0).getAvailabilityType() == AvailabilityType.DOWN;
            assert avails.get(0).getStartTime().equals(middle);
            assert avails.get(0).getEndTime().equals(now);
            assert avails.get(1).getAvailabilityType() == AvailabilityType.UP;
            assert avails.get(1).getStartTime().equals(now);
            assert avails.get(1).getEndTime() == null;

            // try to delete them all - but we never should delete the latest
            purged = availabilityManager.purgeAvailabilities(new Long(now.getTime() + 12345));
            assert purged == 1 : "Didn't purge 1 --> " + purged;
            purged = availabilityManager.purgeAvailabilities(new Long(now.getTime() + 12345));
            assert purged == 0 : "Didn't purge 0 --> " + purged;

            q = em.createNamedQuery(Availability.FIND_BY_RESOURCE);
            q.setParameter("resourceId", theResource.getId());
            avails = q.getResultList();

            assert avails.size() == 1;
            assert avails.get(0).getAvailabilityType() == AvailabilityType.UP;
            assert avails.get(0).getStartTime().equals(now);
            assert avails.get(0).getEndTime() == null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testGetAvailabilities() throws Exception {
        EntityManager em = beginTx();

        try {
            List<AvailabilityPoint> availPoints;
            Availability avail;

            setupResource(em);
            commitAndClose(em);
            em = null;

            AvailabilityReport report = new AvailabilityReport(false, theAgent.getName());

            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(), 1, System
                .currentTimeMillis(), 3, false);
            assert availPoints.size() == 3 : "There is no avail data, but should still get 3 availability points";
            assert availPoints.get(0).getValue() == DOWN.ordinal(); // aka unknown
            assert availPoints.get(1).getValue() == DOWN.ordinal(); // aka unknown
            assert availPoints.get(2).getValue() == DOWN.ordinal(); // aka unknown

            long startMillis = 60000;
            Date startDate = new Date(startMillis);
            avail = new Availability(theResource, startDate, UP);
            report.addAvailability(avail);
            availabilityManager.mergeAvailabilityReport(report);

            // our avail data point is right on the start edge
            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(), startMillis,
                startMillis + 10000, 3, false);
            assert availPoints.size() == 3 : "There is 1 avail data, but should still get 3 availability points";
            assert availPoints.get(0).getValue() == UP.ordinal();
            assert availPoints.get(1).getValue() == UP.ordinal();
            assert availPoints.get(2).getValue() == UP.ordinal();

            // our avail data point is right on the end edge
            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(),
                startMillis - 3, startMillis, 3, false);
            assert availPoints.size() == 3 : "There is 1 avail data, but should still get 3 availability points";
            assert availPoints.get(0).getValue() == DOWN.ordinal(); // aka unknown
            assert !availPoints.get(0).isKnown() : availPoints;
            assert availPoints.get(1).getValue() == DOWN.ordinal(); // aka unknown
            assert !availPoints.get(1).isKnown() : availPoints;
            assert availPoints.get(2).getValue() == DOWN.ordinal(); // aka unknown
            assert !availPoints.get(2).isKnown() : availPoints;

            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(),
                startMillis - 20000, startMillis + 10000, 3, false);
            assert availPoints.size() == 3 : "There is 1 avail data, but should still get 3 availability points";
            assert availPoints.get(0).getValue() == DOWN.ordinal(); // aka unknown
            assert !availPoints.get(0).isKnown() : availPoints;
            assert availPoints.get(1).getValue() == DOWN.ordinal(); // aka unknown
            assert !availPoints.get(1).isKnown() : availPoints;
            assert availPoints.get(2).getValue() == UP.ordinal();

            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(),
                startMillis - 10000, startMillis + 20000, 3, false);
            assert availPoints.size() == 3 : "There is 1 avail data, but should still get 3 availability points";
            assert availPoints.get(0).getValue() == DOWN.ordinal(); // aka unknown
            assert !availPoints.get(0).isKnown() : availPoints;
            assert availPoints.get(1).getValue() == UP.ordinal();
            assert availPoints.get(1).isKnown() : availPoints;
            assert availPoints.get(2).getValue() == UP.ordinal();

            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(),
                startMillis - 20000, startMillis + 20000, 10, false);
            assert availPoints.size() == 10 : "There is 1 avail data, but should still get 10 availability points";
            assert availPoints.get(0).getValue() == DOWN.ordinal() : availPoints; // aka unknown
            assert !availPoints.get(0).isKnown() : availPoints;
            assert availPoints.get(1).getValue() == DOWN.ordinal() : availPoints;
            assert !availPoints.get(1).isKnown() : availPoints;
            assert availPoints.get(2).getValue() == DOWN.ordinal() : availPoints;
            assert !availPoints.get(2).isKnown() : availPoints;
            assert availPoints.get(3).getValue() == DOWN.ordinal() : availPoints;
            assert !availPoints.get(3).isKnown() : availPoints;
            assert availPoints.get(4).getValue() == DOWN.ordinal() : availPoints;
            assert !availPoints.get(4).isKnown() : availPoints;
            assert availPoints.get(5).getValue() == UP.ordinal() : availPoints;
            assert availPoints.get(5).isKnown() : availPoints;
            assert availPoints.get(6).getValue() == UP.ordinal() : availPoints;
            assert availPoints.get(7).getValue() == UP.ordinal() : availPoints;
            assert availPoints.get(8).getValue() == UP.ordinal() : availPoints;
            assert availPoints.get(9).getValue() == UP.ordinal() : availPoints;

            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(new Availability(theResource, new Date(startDate.getTime() + 10000), DOWN));
            availabilityManager.mergeAvailabilityReport(report);

            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(new Availability(theResource, new Date(startDate.getTime() + 20000), UP));
            availabilityManager.mergeAvailabilityReport(report);

            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(new Availability(theResource, new Date(startDate.getTime() + 30000), DOWN));
            availabilityManager.mergeAvailabilityReport(report);

            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(),
                startMillis - 15000, startMillis + 35000, 5, false);
            assert availPoints.size() == 5 : "There is 1 avail data, but should still get 5 availability points";
            assert availPoints.get(0).getValue() == DOWN.ordinal() : availPoints; // 45000-55000 == unknown=down
            assert !availPoints.get(0).isKnown() : availPoints;

            // this next point is on the edge - part was unknown, part was up - because its on the edge, and part of it
            // was UP, we consider the data point UP
            assert availPoints.get(1).getValue() == UP.ordinal() : availPoints; // 55000-65000 == 55-60=unknown=down, 60-70=up
            assert availPoints.get(1).isKnown() : availPoints;

            assert availPoints.get(2).getValue() == DOWN.ordinal() : availPoints; // 65000-75000 == 60-70=up, 70-80=down (0.5==down)
            assert availPoints.get(3).getValue() == DOWN.ordinal() : availPoints; // 75000-85000, 0.5 == down
            assert availPoints.get(4).getValue() == DOWN.ordinal() : availPoints; // 85000-95000, 0.5 == down

            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(),
                startMillis - 30000, startMillis + 30000, 10, false);
            assert availPoints.size() == 10 : "There is 1 avail data, but should still get 10 availability points";
            assert availPoints.get(0).getValue() == DOWN.ordinal() : availPoints;
            assert availPoints.get(1).getValue() == DOWN.ordinal() : availPoints;
            assert availPoints.get(2).getValue() == DOWN.ordinal() : availPoints;
            assert availPoints.get(3).getValue() == DOWN.ordinal() : availPoints;
            assert availPoints.get(4).getValue() == DOWN.ordinal() : availPoints;
            assert availPoints.get(5).getValue() == UP.ordinal() : availPoints;
            assert availPoints.get(6).getValue() == DOWN.ordinal() : availPoints; // 0.5
            assert availPoints.get(7).getValue() == DOWN.ordinal() : availPoints;
            assert availPoints.get(8).getValue() == DOWN.ordinal() : availPoints; // 0.5
            assert availPoints.get(9).getValue() == UP.ordinal() : availPoints;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testSetAllAgentResourceAvailabilities() throws Exception {
        EntityManager em = beginTx();

        try {
            setupResource(em);
            commitAndClose(em);
            em = null;

            // setAllAgentResourceAvails will only operate on those that have at least 1 avail row
            Availability avail = new Availability(theResource, new Date(), null);
            persistAvailability(avail);

            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == null;
            availabilityManager.setAllAgentResourceAvailabilities(theAgent.getId(), UP);
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == UP;
            availabilityManager.setAllAgentResourceAvailabilities(theAgent.getId(), DOWN);
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;
            availabilityManager.setAllAgentResourceAvailabilities(theAgent.getId(), DOWN); // extend it
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testAgentBackfillNewResource() throws Exception {
        EntityManager em = beginTx();

        try {
            setupResource(em);
            commitAndClose(em);
            em = null;

            // have never heard from the new agent yet - we do not backfill anything in this case
            LookupUtil.getAgentManager().checkForSuspectAgents();
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == null; // unknown

            em = beginTx();
            Resource resource = em.find(Resource.class, theResource.getId());
            List<Availability> avails = resource.getAvailability();
            assert avails != null;
            assert avails.size() == 0;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testAgentBackfill() throws Exception {
        EntityManager em = beginTx();

        try {
            prepareForTestAgents();

            setupResource(em);
            commitAndClose(em);
            em = null;

            // add a report that says the resource was up 10 minutes ago
            Availability avail = new Availability(theResource, new Date(System.currentTimeMillis() - 600000), UP);
            AvailabilityReport report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            availabilityManager.mergeAvailabilityReport(report);
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == UP;

            // let's pretend we haven't heard from the agent in a few minutes
            em = beginTx();
            Agent agent = em.find(Agent.class, theAgent.getId());
            agent.setLastAvailabilityReport(System.currentTimeMillis() - (1000 * 60 * 6));
            commitAndClose(em);
            em = null;

            // the agent should be suspect and will be considered down
            LookupUtil.getAgentManager().checkForSuspectAgents();
            AvailabilityType curAvail;
            curAvail = availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId());
            assert curAvail == AvailabilityType.DOWN : curAvail; // backfilled with "null" to mean "unknown"

            // make sure our resource's new availabilities are consistent (first (UP) is before second (unknown))
            em = beginTx();
            Resource resource = em.find(Resource.class, theResource.getId());
            List<Availability> allAvails = resource.getAvailability();
            assert allAvails.size() == 2;
            commitAndClose(em);
            em = null;

            Availability first = allAvails.get(0);
            Availability second = allAvails.get(1);
            assert first.getAvailabilityType() == AvailabilityType.UP;
            assert second.getAvailabilityType() == AvailabilityType.DOWN : second.getAvailabilityType();
            assert first.getEndTime() != null;
            assert second.getEndTime() == null;
            assert first.getEndTime().getTime() > first.getStartTime().getTime();
            assert second.getStartTime().getTime() == first.getEndTime().getTime();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            unprepareForTestAgents();

            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testAgentBackfillPerformance() throws Exception {
        EntityManager em = beginTx();
        List<Resource> allResources = new ArrayList<Resource>();

        try {
            prepareForTestAgents();

            setupResource(em); // setup theResource

            allResources.add(theResource);

            // now create a bunch more resources
            for (int i = 0; i < 100; i++) {
                allResources.add(setupAnotherResource(em, i, theResource));
            }
            em.flush();

            commitAndClose(em);
            em = null;

            for (Resource res : allResources) {
                int resId = res.getId();
                Availability currentAvailability = availabilityManager.getCurrentAvailabilityForResource(overlord,
                    resId);
                assert currentAvailability != null : "Current Availability was null for " + resId;
                assert currentAvailability.getAvailabilityType() == null : "Current AvailabilityType should have been null for "
                    + resId;

                List<Availability> allData = availabilityManager.findAvailabilityWithinInterval(resId, new Date(0),
                    new Date(Long.MAX_VALUE));
                assert allData != null : "All availabilities was null for " + resId;
                assert allData.size() == 0 : "All availabilities size was " + allData.size() + " for " + resId;

                ResourceAvailability currentResAvail = resourceAvailabilityManager.getLatestAvailability(resId);
                assert currentResAvail != null : "Current ResourceAvailability was null for " + resId;
                assert currentResAvail.getAvailabilityType() == null : "Current ResourceAvailabilityType should have been null for "
                    + resId;
            }

            // let's pretend we haven't heard from the agent in a few minutes
            em = beginTx();
            Agent agent = em.find(Agent.class, theAgent.getId());
            agent.setLastAvailabilityReport(System.currentTimeMillis() - (1000 * 60 * 6));
            commitAndClose(em);
            em = null;

            // the agent should be suspect and will be considered down
            // none of the resources have availabilities yet, so a new row will be added for all of them
            long start = System.currentTimeMillis();
            LookupUtil.getAgentManager().checkForSuspectAgents();

            System.out.println("testAgentBackfillPerformance: checkForSuspectAgents run 1 took "
                + (System.currentTimeMillis() - start) + "ms");

            // add a report that says the resources are now up - the report will add one avail for each resource
            Thread.sleep(500);
            AvailabilityReport report = new AvailabilityReport(false, theAgent.getName());
            for (Resource resource : allResources) {
                Availability avail = new Availability(resource, new Date(), UP);
                report.addAvailability(avail);
            }

            start = System.currentTimeMillis();
            availabilityManager.mergeAvailabilityReport(report);

            System.out.println("testAgentBackfillPerformance: mergeAvailabilityReport run took "
                + (System.currentTimeMillis() - start) + "ms");

            // sanity check - make sure the merge at least appeared to work
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == UP;

            // let's again pretend we haven't heard from the agent in a few minutes
            em = beginTx();
            agent = em.find(Agent.class, theAgent.getId());
            agent.setLastAvailabilityReport(System.currentTimeMillis() - (1000 * 60 * 6));
            commitAndClose(em);
            em = null;

            // the agent should be suspect and will be considered down
            // all of the resources have availabilities now, so another row will be added to them
            start = System.currentTimeMillis();
            LookupUtil.getAgentManager().checkForSuspectAgents();

            System.out.println("testAgentBackfillPerformance: checkForSuspectAgents run 2 took "
                + (System.currentTimeMillis() - start) + "ms");

            AvailabilityType curAvail;
            start = System.currentTimeMillis();

            for (Resource resource : allResources) {
                curAvail = availabilityManager.getCurrentAvailabilityTypeForResource(overlord, resource.getId());
                assert curAvail == AvailabilityType.DOWN : curAvail; // backfilled with "null" to mean "unknown"

                // make sure our resources' new availabilities are consistent
                // the first time we suspected the agent and backfilled with unknown, there was no rows
                // in availability so there was nothing to add, later we went UP then DOWN so we'll have 2 rows)
                em = beginTx();
                resource = em.find(Resource.class, resource.getId());
                List<Availability> allAvails = resource.getAvailability();
                assert allAvails.size() == 2 : allAvails;
                commitAndClose(em);
                em = null;

                Availability a0 = allAvails.get(0);
                Availability a1 = allAvails.get(1);
                assert a0.getAvailabilityType() == AvailabilityType.UP : allAvails;
                assert a1.getAvailabilityType() == AvailabilityType.DOWN : allAvails;
                assert a0.getEndTime() != null : allAvails;
                assert a1.getEndTime() == null : allAvails;
                assert a0.getEndTime().getTime() > a0.getStartTime().getTime() : allAvails;
                assert a0.getEndTime().getTime() == a1.getStartTime().getTime() : allAvails;
            }

            System.out.println("testAgentBackfillPerformance: checking validity of data took "
                + (System.currentTimeMillis() - start) + "ms");

            em = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            unprepareForTestAgents();

            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testAgentOldReport() throws Exception {
        EntityManager em = beginTx();

        try {
            setupResource(em);
            commitAndClose(em);
            em = null;

            Availability avail;
            long now = System.currentTimeMillis();

            // add a report that says the resource is down
            avail = new Availability(theResource, new Date(now), DOWN);
            AvailabilityReport report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            availabilityManager.mergeAvailabilityReport(report);

            // now pretend the agent sent us a report from a previous time period - should insert this in the past
            avail = new Availability(theResource, new Date(now - 600000), UP);
            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            availabilityManager.mergeAvailabilityReport(report);
            assert getPointInTime(new Date(avail.getStartTime().getTime() - 1)) == DOWN; // should be unknown
            assert getPointInTime(avail.getStartTime()) == UP;
            assert getPointInTime(new Date(avail.getStartTime().getTime() + 1)) == UP;

            // its still down though - since we've received a more recent report saying it was down
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;

            // now pretend the agent sent us reports from inbetween our existing time periods
            // this UP record combines with the UP we added previously
            avail = new Availability(theResource, new Date(now - 300000), UP);
            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            availabilityManager.mergeAvailabilityReport(report);
            assert getPointInTime(new Date(avail.getStartTime().getTime() - 1)) == UP;
            assert getPointInTime(avail.getStartTime()) == UP;
            assert getPointInTime(new Date(avail.getStartTime().getTime() + 1)) == UP;

            // its still down though - since we've received a more recent report saying it was down
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;

            // this DOWN record combines with the current DOWN
            avail = new Availability(theResource, new Date(now - 100000), DOWN);
            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            availabilityManager.mergeAvailabilityReport(report);
            assert getPointInTime(new Date(avail.getStartTime().getTime() - 1)) == UP;
            assert getPointInTime(avail.getStartTime()) == DOWN;
            assert getPointInTime(new Date(avail.getStartTime().getTime() + 1)) == DOWN;

            // this DOWN record is between the two UPs we added earlier. However, because we are RLE,
            // we actually lost the information that we had an UP at both -60000 and -30000.  We just
            // have a RLE interval of UP starting at -60000.  This new DOWN record will add a new row
            // that will indicate we were only UP from -60000 to -45000 and DOWN thereafter.  This is
            // an odd test and probably will never occur in the wild (why would an agent tell us
            // we were one status in the past but another status further back in the past?)
            avail = new Availability(theResource, new Date(now - 450000), DOWN);
            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            availabilityManager.mergeAvailabilityReport(report);
            assert getPointInTime(new Date(avail.getStartTime().getTime() - 1)) == UP;
            assert getPointInTime(avail.getStartTime()) == DOWN;
            assert getPointInTime(new Date(avail.getStartTime().getTime() + 1)) == DOWN;

            // its still down
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;

            // let's insert one in the very beginning that is the same type as the current first interval
            avail = new Availability(theResource, new Date(now - 700000), UP);
            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            availabilityManager.mergeAvailabilityReport(report);
            assert getPointInTime(new Date(avail.getStartTime().getTime() - 1)) == DOWN; // should be unknown
            assert getPointInTime(avail.getStartTime()) == UP;
            assert getPointInTime(new Date(avail.getStartTime().getTime() + 1)) == UP;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testAgentOldReport2() throws Exception {
        EntityManager em = beginTx();

        try {
            setupResource(em);
            commitAndClose(em);
            em = null;

            long now = System.currentTimeMillis();

            // add a report that says the resource is down - reports can't have the same resource in it twice,
            // so just create a size=1 report multiple times
            persistAvailability(new Availability(theResource, new Date(now - 1000000), UP));
            persistAvailability(new Availability(theResource, new Date(now - 900000), DOWN));
            persistAvailability(new Availability(theResource, new Date(now - 800000), UP));
            persistAvailability(new Availability(theResource, new Date(now - 50000), DOWN));
            persistAvailability(new Availability(theResource, new Date(now - 30000), UP));
            persistAvailability(new Availability(theResource, new Date(now), DOWN));

            // now pretend the agent sent us a report from a previous time period - should insert this in the past
            persistAvailability(new Availability(theResource, new Date(now - 600000), UP));

            // its still down though - since we've received a more recent report saying it was down
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;

            // now pretend the agent sent us reports from in between our existing time periods
            // this UP record combines with the UP we added previously
            persistAvailability(new Availability(theResource, new Date(now - 300000), UP));

            // its still down though - since we've received a more recent report saying it was down
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;

            // this DOWN record combines with the current DOWN
            persistAvailability(new Availability(theResource, new Date(now - 100000), DOWN));

            // this DOWN record is between the two UPs we added earlier. However, because we are RLE,
            // we actually lost the information that we had an UP at both -60000 and -30000.  We just
            // have a RLE interval of UP starting at -60000.  This new DOWN record will add a new row
            // that will indicate we were only UP fro -60000 to -45000 and DOWN thereafter.  This is
            // an odd test and probably will never occur in the wild (why would an agent tell us
            // we were one status in the past but another status further back in the past?)
            persistAvailability(new Availability(theResource, new Date(now - 450000), DOWN));

            // its still down
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;

            // let's insert one in the very beginning that is the same type as the current first interval
            persistAvailability(new Availability(theResource, new Date(now - 700000), UP));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testGetAvailabilities2() throws Exception {
        EntityManager em = beginTx();

        try {
            Availability avail;

            setupResource(em);
            commitAndClose(em);
            em = null;

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, 2000);
            cal.set(Calendar.MONTH, 0);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR, 1);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date date1 = cal.getTime();

            cal.set(Calendar.HOUR, 1);
            cal.set(Calendar.MINUTE, 30);
            Date date2 = cal.getTime();

            cal.set(Calendar.HOUR, 2);
            cal.set(Calendar.MINUTE, 0);
            Date date3 = cal.getTime();

            cal.set(Calendar.HOUR, 2);
            cal.set(Calendar.MINUTE, 30);
            Date date4 = cal.getTime();

            cal.set(Calendar.HOUR, 3);
            cal.set(Calendar.MINUTE, 0);
            Date date5 = cal.getTime();

            cal.set(Calendar.HOUR, 3);
            cal.set(Calendar.MINUTE, 30);
            Date date6 = cal.getTime();

            avail = new Availability(theResource, date1, AvailabilityType.UP);
            avail.setEndTime(date2);
            persistAvailability(avail);

            avail = new Availability(theResource, date2, AvailabilityType.DOWN);
            avail.setEndTime(date3);
            persistAvailability(avail);

            avail = new Availability(theResource, date3, AvailabilityType.UP);
            avail.setEndTime(date4);
            persistAvailability(avail);

            avail = new Availability(theResource, date4, AvailabilityType.DOWN);
            avail.setEndTime(date5);
            persistAvailability(avail);

            avail = new Availability(theResource, date5, AvailabilityType.UP);
            avail.setEndTime(date6);
            persistAvailability(avail);

            avail = new Availability(theResource, date6, AvailabilityType.DOWN);
            persistAvailability(avail);

            List<AvailabilityPoint> points = availabilityManager.findAvailabilitiesForResource(overlord, theResource
                .getId(), date1.getTime(), date6.getTime(), 5, false);
            assert points.size() == 5;
            assert points.get(0).getValue() == 1;
            assert points.get(1).getValue() == 0;
            assert points.get(2).getValue() == 1;
            assert points.get(3).getValue() == 0;
            assert points.get(4).getValue() == 1;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    /**
     * See if merging in AvailabilityReports from the agent work.
     *
     * @throws Exception in case of error
     */
    @Test(enabled = ENABLE_TESTS)
    public void testMergeReport() throws Exception {
        EntityManager em = beginTx();

        try {
            Availability avail;
            AvailabilityReport report;

            setupResource(em);
            commitAndClose(em);
            em = null;

            long allAvailCount = setUpAvailabilities(em);

            // we now have 1:00 UP, 1:20 DOWN, 1:40 UP
            Subject overlord = LookupUtil.getSubjectManager().getOverlord();
            avail = availabilityManager.getCurrentAvailabilityForResource(overlord, theResource.getId());
            assert avail.getAvailabilityType() == UP;
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == UP;

            // add something after the start of last, but still be UP (result: nothing added)
            Date currentStartTime = avail.getStartTime();
            avail = new Availability(theResource, new Date(currentStartTime.getTime() + 3600000), UP);
            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            Thread.sleep(1000);
            availabilityManager.mergeAvailabilityReport(report);

            // the agent should have been updated, but no new rows in availability were added
            Agent agent = LookupUtil.getAgentManager().getAgentByName(theAgent.getName());
            Date lastReport = new Date(agent.getLastAvailabilityReport());
            assert lastReport != null;
            assert countAvailabilitiesInDB(null) == allAvailCount;
            avail = availabilityManager.getCurrentAvailabilityForResource(overlord, theResource.getId());

            // should have returned availability3
            assert avail.getId() == availability3.getId();
            assert avail.getAvailabilityType() == availability3.getAvailabilityType();
            assert Math.abs(avail.getStartTime().getTime() - availability3.getStartTime().getTime()) < 1000;
            assert avail.getEndTime() == null;
            assert avail.getEndTime() == availability3.getEndTime();

            // change start after the start of last (result: add new avail row)
            avail = new Availability(theResource, new Date(currentStartTime.getTime() + 7200000), DOWN);
            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            Thread.sleep(1000);
            availabilityManager.mergeAvailabilityReport(report);

            // the agent should have been updated and a new row in availability was added (resource is now DOWN)
            agent = LookupUtil.getAgentManager().getAgentByName(theAgent.getName());
            assert new Date(agent.getLastAvailabilityReport()).after(lastReport);
            assert countAvailabilitiesInDB(null) == (allAvailCount + 1);
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;
            Availability queriedAvail = availabilityManager.getCurrentAvailabilityForResource(overlord, theResource
                .getId());
            assert queriedAvail.getId() > 0;
            assert queriedAvail.getAvailabilityType() == avail.getAvailabilityType();
            assert Math.abs(queriedAvail.getStartTime().getTime() - avail.getStartTime().getTime()) < 1000;
            assert queriedAvail.getEndTime() == null;
            assert queriedAvail.getEndTime() == avail.getEndTime();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testMergeReportPerformance() throws Exception {
        EntityManager em = beginTx();
        List<Resource> allResources = new ArrayList<Resource>();

        try {
            setupResource(em); // setup theResource

            allResources.add(theResource);

            // now create a bunch more resources
            for (int i = 0; i < 100; i++) {
                allResources.add(setupAnotherResource(em, i, theResource));
            }
            em.flush();

            commitAndClose(em);
            em = null;

            // add a report that says the resources are now up - the report will add one avail for each resource
            // at this point, the resources do not yet have a row in availability - after the merge they will have 1
            AvailabilityReport report = new AvailabilityReport(false, theAgent.getName());
            for (Resource resource : allResources) {
                Availability avail = new Availability(resource, new Date(), UP);
                report.addAvailability(avail);
            }

            long start = System.currentTimeMillis();
            availabilityManager.mergeAvailabilityReport(report);

            System.out.println("testMergeReportPerformance: mergeAvailabilityReport run 1 took "
                + (System.currentTimeMillis() - start) + "ms");

            AvailabilityType curAvail;
            start = System.currentTimeMillis();

            for (Resource resource : allResources) {
                curAvail = availabilityManager.getCurrentAvailabilityTypeForResource(overlord, resource.getId());
                assert curAvail == UP : curAvail;
            }

            System.out.println("testMergeReportPerformance: checking validity of data 1 took "
                + (System.currentTimeMillis() - start) + "ms");

            // add a report that says the resources are now down - the report will add one avail for each resource
            // at this point, the resources have 1 row in availability - after the merge they will have 2
            report = new AvailabilityReport(false, theAgent.getName());
            for (Resource resource : allResources) {
                Availability avail = new Availability(resource, new Date(), DOWN);
                report.addAvailability(avail);
            }

            start = System.currentTimeMillis();
            availabilityManager.mergeAvailabilityReport(report);

            System.out.println("testMergeReportPerformance: mergeAvailabilityReport run 2 took "
                + (System.currentTimeMillis() - start) + "ms");

            start = System.currentTimeMillis();

            for (Resource resource : allResources) {
                curAvail = availabilityManager.getCurrentAvailabilityTypeForResource(overlord, resource.getId());
                assert curAvail == DOWN : curAvail;
            }

            System.out.println("testMergeReportPerformance: checking validity of data 2 took "
                + (System.currentTimeMillis() - start) + "ms");

            // add a report that says the resources are now unknown - the report will add one avail for each resource
            // at this point, the resources have 2 rows in availability - after the merge they will have 3
            report = new AvailabilityReport(false, theAgent.getName());
            for (Resource resource : allResources) {
                Availability avail = new Availability(resource, new Date(), null);
                report.addAvailability(avail);
            }

            start = System.currentTimeMillis();
            availabilityManager.mergeAvailabilityReport(report);

            System.out.println("testMergeReportPerformance: mergeAvailabilityReport run 3 took "
                + (System.currentTimeMillis() - start) + "ms");

            start = System.currentTimeMillis();

            for (Resource resource : allResources) {
                curAvail = availabilityManager.getCurrentAvailabilityTypeForResource(overlord, resource.getId());
                assert curAvail == null : curAvail;
            }

            System.out.println("testMergeReportPerformance: checking validity of data 3 took "
                + (System.currentTimeMillis() - start) + "ms");

            em = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    private EntityManager beginTx() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        return em;
    }

    private void commitAndClose(EntityManager em) throws Exception {
        getTransactionManager().commit();
        em.close();
    }

    private AvailabilityType getPointInTime(Date time) {
        List<AvailabilityPoint> list = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(),
            time.getTime(), time.getTime() + 1, 1, false);
        assert list != null;
        assert list.size() == 1 : "Should have returned a single point";
        int typeOrdinal = list.get(0).getValue();

        if (UP.ordinal() == typeOrdinal) {
            return UP;
        }

        if (DOWN.ordinal() == typeOrdinal) {
            return DOWN;
        }

        assert false : "AvailabilityType enum has some additional values not known to this test: " + typeOrdinal;
        return null;
    }

    /**
     * See how many rows we have in the availability table
     *
     * @param  em EntityManager to use
     *
     * @return the rowcount
     *
     * @throws Exception
     */
    private long countAvailabilitiesInDB(EntityManager em) throws Exception {
        TransactionManager tx = null;

        if (em == null) {
            tx = getTransactionManager();
            tx.begin();
            em = getEntityManager();
        }

        try {
            Query q = em.createQuery("SELECT count(*) FROM Availability");
            long count = (Long) q.getSingleResult();
            return count;
        } finally {
            if (tx != null) {
                tx.rollback();
                em.close();
            }
        }
    }

    /**
     * Just set up a resource where we can attach the availabilities to
     *
     * @param  em The EntityManager to use
     *
     * @return A Resource ready to use
     */
    private Resource setupResource(EntityManager em) {
        theAgent = new Agent("testagent", "localhost", 1234, "", "randomToken");
        em.persist(theAgent);

        theResourceType = new ResourceType("test-plat", "test-plugin", ResourceCategory.PLATFORM, null);
        em.persist(theResourceType);

        theResource = new Resource("test-platform-key", "test-platform-name", theResourceType);
        theResource.setUuid("" + new Random().nextInt());
        theResource.setAgent(theAgent);
        em.persist(theResource);

        em.flush();
        return theResource;
    }

    /**
     * Set up another unique resource that will be related to <code>theAgent</code>. The resource will be of type <code>
     * theResourceType</code>.
     *
     * @param  em           The EntityManager to use
     * @param  uniqueNumber used to define a unique key for the resource
     *
     * @return A Resource ready to use
     */
    private Resource setupAnotherResource(EntityManager em, int uniqueNumber, Resource parentResource) {
        Resource newResource;

        newResource = new Resource("test-platform-key-" + uniqueNumber, "test-platform-name-" + uniqueNumber,
            theResourceType);
        newResource.setUuid("" + new Random().nextInt());
        newResource.setAgent(theAgent);
        parentResource.addChildResource(newResource);
        em.persist(newResource);

        return newResource;
    }

    /**
     * Set up an availability scenario where we set up availability for one hour, split it in the middle and have 20min
     * up, 20min down, 20min up starting at 1:00am.
     *
     * @param  em An EntityManager to use
     *
     * @return total number of availability records in the DB after we've added ours
     *
     * @throws Exception
     */
    private long setUpAvailabilities(EntityManager em) throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR, 1);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date start = cal.getTime();

        // split time
        cal.set(Calendar.HOUR, 1);
        cal.set(Calendar.MINUTE, 20);
        Date splitStart = cal.getTime();
        cal.set(Calendar.MINUTE, 40);
        Date splitEnd = cal.getTime();

        long count = countAvailabilitiesInDB(em);

        availability1 = new Availability(theResource, start, AvailabilityType.UP);
        availability1.setEndTime(splitStart);
        persistAvailability(availability1);

        availability2 = new Availability(theResource, splitStart, AvailabilityType.DOWN);
        availability2.setEndTime(splitEnd);
        persistAvailability(availability2);

        availability3 = new Availability(theResource, splitEnd, AvailabilityType.UP);
        persistAvailability(availability3);

        long countNow = countAvailabilitiesInDB(em);

        assert countNow == (count + 3) : "Did not find three availabilities - instead found: " + countNow;

        return countNow;
    }

    /**
     * Convenience method for persisting availability.  Availability data can no longer be directly merged
     * by the EntityManager because it does not update the corresponding currentAvailability data on the 
     * Resource entity.  This method will update the necessary objects for you.
     */
    private void persistAvailability(Availability... availabilities) {
        AvailabilityReport report = new AvailabilityReport(false, theAgent.getName());
        for (Availability avail : availabilities) {
            report.addAvailability(avail);
        }
        availabilityManager.mergeAvailabilityReport(report);
    }
}