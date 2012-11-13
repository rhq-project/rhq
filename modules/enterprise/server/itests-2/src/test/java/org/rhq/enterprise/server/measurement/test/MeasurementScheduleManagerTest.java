/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.persistence.Query;

import junit.framework.Assert;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeDataKey;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.measurement.MeasurementConstants;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.ResourceTreeHelper;

/**
 * Test some measurement schedule handling
 * 
 * @author Jay Shaughnessy
 */
public class MeasurementScheduleManagerTest extends AbstractMeasurementScheduleManagerTest {

    private MeasurementScheduleManagerLocal measurementScheduleManager;

    private Subject overlord;

    private Resource resource1;
    private MeasurementDefinition definitionCt1;
    private ResourceType theResourceType;
    private Agent theAgent;
    private MeasurementScheduleTestServerCommunicationsService testCommService;

    @Override
    protected void beforeMethod() {
        try {
            prepareScheduler();
            testCommService = (MeasurementScheduleTestServerCommunicationsService) prepareForTestAgents();

            this.measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();
            this.overlord = LookupUtil.getSubjectManager().getOverlord();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Override
    protected void afterMethod() {
        try {
            // delete values
            beginTx();

            // delete keys
            List<Integer> resourceIds = new ArrayList<Integer>();
            resourceIds.add(resource1.getId());
            Query q = em.createNamedQuery(CallTimeDataKey.QUERY_DELETE_BY_RESOURCES);
            q.setParameter("resourceIds", resourceIds);
            q.executeUpdate();

            resource1 = em.merge(resource1);
            for (MeasurementSchedule sched : resource1.getSchedules()) {
                em.remove(sched);
            }
            ResourceTreeHelper.deleteResource(em, resource1);

            definitionCt1 = em.merge(definitionCt1);
            em.remove(definitionCt1);

            theResourceType = em.merge(theResourceType);
            em.remove(theResourceType);

            theAgent = em.merge(theAgent);
            em.remove(theAgent);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {
                unprepareScheduler();
                unprepareForTestAgents();
            } catch (Exception e) {
            }

            commitAndClose();
        }

    }

    @Test
    public void testIntervals() {

        try {
            beginTx();

            setupResources();

            MeasurementSchedule schedule1 = new MeasurementSchedule(definitionCt1, resource1);
            em.persist(schedule1);
            definitionCt1.addSchedule(schedule1);
            resource1.addSchedule(schedule1);
            resource1 = em.merge(resource1);

            commitAndClose();

            // 60s is valid
            schedule1.setInterval(60000L);
            measurementScheduleManager.updateSchedule(overlord, schedule1);
            schedule1 = measurementScheduleManager.getScheduleById(overlord, schedule1.getId());
            Assert.assertEquals(60000L, schedule1.getInterval());

            // 10s is invalid and should be assigned 30s
            schedule1.setInterval(10000L);
            measurementScheduleManager.updateSchedule(overlord, schedule1);
            schedule1 = measurementScheduleManager.getScheduleById(overlord, schedule1.getId());
            Assert.assertEquals(MeasurementConstants.MINIMUM_COLLECTION_INTERVAL_MILLIS, schedule1.getInterval());

            // 0s is invalid and should be assigned 30s
            schedule1.setInterval(0L);
            measurementScheduleManager.updateSchedule(overlord, schedule1);
            schedule1 = measurementScheduleManager.getScheduleById(overlord, schedule1.getId());
            Assert.assertEquals(MeasurementConstants.MINIMUM_COLLECTION_INTERVAL_MILLIS, schedule1.getInterval());

            // negative intervals are invalid and should be assigned 30s
            schedule1.setInterval(-60000L);
            measurementScheduleManager.updateSchedule(overlord, schedule1);
            schedule1 = measurementScheduleManager.getScheduleById(overlord, schedule1.getId());
            Assert.assertEquals(MeasurementConstants.MINIMUM_COLLECTION_INTERVAL_MILLIS, schedule1.getInterval());

            // Try a different mechanism

            // 60s is valid
            measurementScheduleManager.updateSchedulesForResource(overlord, resource1.getId(),
                new int[] { definitionCt1.getId() }, 60000L);
            Set<ResourceMeasurementScheduleRequest> resScheds = measurementScheduleManager
                .findSchedulesForResourceAndItsDescendants(new int[] { resource1.getId() }, false);
            Assert.assertEquals(1, resScheds.size());
            Set<MeasurementScheduleRequest> scheds = resScheds.iterator().next().getMeasurementSchedules();
            Assert.assertEquals(1, scheds.size());
            Assert.assertEquals(60000L, scheds.iterator().next().getInterval());

            // 10s is invalid and should be assigned 30s
            measurementScheduleManager.updateSchedulesForResource(overlord, resource1.getId(),
                new int[] { definitionCt1.getId() }, 10000L);
            resScheds = measurementScheduleManager.findSchedulesForResourceAndItsDescendants(
                new int[] { resource1.getId() }, false);
            Assert.assertEquals(1, resScheds.size());
            scheds = resScheds.iterator().next().getMeasurementSchedules();
            Assert.assertEquals(1, scheds.size());
            Assert.assertEquals(MeasurementConstants.MINIMUM_COLLECTION_INTERVAL_MILLIS, scheds.iterator().next()
                .getInterval());

            // 0s is invalid and should be assigned 30s
            measurementScheduleManager.updateSchedulesForResource(overlord, resource1.getId(),
                new int[] { definitionCt1.getId() }, 0L);
            resScheds = measurementScheduleManager.findSchedulesForResourceAndItsDescendants(
                new int[] { resource1.getId() }, false);
            Assert.assertEquals(1, resScheds.size());
            scheds = resScheds.iterator().next().getMeasurementSchedules();
            Assert.assertEquals(1, scheds.size());
            Assert.assertEquals(MeasurementConstants.MINIMUM_COLLECTION_INTERVAL_MILLIS, scheds.iterator().next()
                .getInterval());

            // negative intervals are invalid and should be assigned 30s
            measurementScheduleManager.updateSchedulesForResource(overlord, resource1.getId(),
                new int[] { definitionCt1.getId() }, -60000L);
            resScheds = measurementScheduleManager.findSchedulesForResourceAndItsDescendants(
                new int[] { resource1.getId() }, false);
            Assert.assertEquals(1, resScheds.size());
            scheds = resScheds.iterator().next().getMeasurementSchedules();
            Assert.assertEquals(1, scheds.size());
            Assert.assertEquals(MeasurementConstants.MINIMUM_COLLECTION_INTERVAL_MILLIS, scheds.iterator().next()
                .getInterval());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();

        } finally {
            commitAndClose();
        }
    }

    @Test
    public void testBug811696() {

        try {
            beginTx();

            setupResources();
            long defaultInterval = definitionCt1.getDefaultInterval();
            long updatedInterval = defaultInterval * 2;

            // first, establish a schedule for the test resource
            MeasurementSchedule schedule1 = new MeasurementSchedule(definitionCt1, resource1);
            schedule1.setInterval(defaultInterval);
            em.persist(schedule1);
            definitionCt1.addSchedule(schedule1);
            resource1.addSchedule(schedule1);
            resource1 = em.merge(resource1);

            commitAndClose();

            // Test interval update of metrics at the template level
            testCommService.init();

            testCommService.setExpectedInterval(updatedInterval);
            testCommService.setExpectedIsEnabled(true);

            measurementScheduleManager.updateSchedulesForResourceType(overlord, new int[] { definitionCt1.getId() },
                updatedInterval, true);
            Assert.assertTrue(testCommService.isTested());
            if (testCommService.hasFailures()) {
                Assert.fail(testCommService.getFailures().get(0));
            }

            Set<ResourceMeasurementScheduleRequest> resScheds = measurementScheduleManager
                .findSchedulesForResourceAndItsDescendants(new int[] { resource1.getId() }, false);
            Assert.assertEquals(1, resScheds.size());
            ResourceMeasurementScheduleRequest rmsr = resScheds.iterator().next();
            Set<MeasurementScheduleRequest> scheds = rmsr.getMeasurementSchedules();
            Assert.assertEquals(1, scheds.size());
            MeasurementScheduleRequest msr = scheds.iterator().next();
            Assert.assertEquals(updatedInterval, msr.getInterval());
            Assert.assertEquals(true, msr.isEnabled());

            // Test disable of metrics at the template level, this should have no effect on the interval in the client
            // or in the db
            testCommService.init();

            testCommService.setExpectedInterval(updatedInterval);
            testCommService.setExpectedIsEnabled(false);

            measurementScheduleManager.disableSchedulesForResourceType(overlord, new int[] { definitionCt1.getId() },
                true);
            Assert.assertTrue(testCommService.isTested());

            resScheds = measurementScheduleManager.findSchedulesForResourceAndItsDescendants(
                new int[] { resource1.getId() }, false);
            Assert.assertEquals(1, resScheds.size());
            rmsr = resScheds.iterator().next();
            scheds = rmsr.getMeasurementSchedules();
            Assert.assertEquals(1, scheds.size());
            msr = scheds.iterator().next();
            Assert.assertEquals(updatedInterval, msr.getInterval());
            Assert.assertEquals(false, msr.isEnabled());

            // Test enable of metrics at the template level, this should have no effect on the interval in the client
            // or in the db
            testCommService.init();

            testCommService.setExpectedInterval(updatedInterval);
            testCommService.setExpectedIsEnabled(false);

            measurementScheduleManager.enableSchedulesForResourceType(overlord, new int[] { definitionCt1.getId() },
                true);
            Assert.assertTrue(testCommService.isTested());

            resScheds = measurementScheduleManager.findSchedulesForResourceAndItsDescendants(
                new int[] { resource1.getId() }, false);
            Assert.assertEquals(1, resScheds.size());
            rmsr = resScheds.iterator().next();
            scheds = rmsr.getMeasurementSchedules();
            Assert.assertEquals(1, scheds.size());
            msr = scheds.iterator().next();
            Assert.assertEquals(updatedInterval, msr.getInterval());
            Assert.assertEquals(true, msr.isEnabled());

        } catch (Exception e) {
            Assert.fail(e.getMessage());

        } finally {
            commitAndClose();
        }
    }

    /**
     * Just set up two resources plus measurement definitions
     *
     * @param  em The EntityManager to use
     *
     */
    private void setupResources() {
        theAgent = new Agent("testagent", "localhost", 1234, "", "randomToken");
        em.persist(theAgent);

        theResourceType = new ResourceType("test-plat", "test-plugin", ResourceCategory.PLATFORM, null);
        em.persist(theResourceType);

        definitionCt1 = new MeasurementDefinition("CT-Def1", MeasurementCategory.PERFORMANCE,
            MeasurementUnits.MILLISECONDS, DataType.MEASUREMENT, true, 120000L, DisplayType.SUMMARY);
        definitionCt1.setResourceType(theResourceType);
        em.persist(definitionCt1);

        resource1 = new Resource("test-platform-key1", "test-platform-name", theResourceType);
        resource1.setUuid("" + new Random().nextInt());
        resource1.setAgent(theAgent);
        em.persist(resource1);
    }

    private void beginTx() throws Exception {
        getTransactionManager().begin();
    }

    private void commitAndClose() {
        try {
            if (null != em) {
                em.flush();
                getTransactionManager().commit();
            }
        } catch (Throwable t) {

        }
    }
}
