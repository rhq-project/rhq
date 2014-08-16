/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.enterprise.server.scheduler.jobs;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.testng.annotations.Test;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.alert.notification.ResultState;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.test.TestServerPluginService;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Tests that we can purge data.
 */
@Test
public class DataPurgeJobTest extends AbstractEJB3Test {
    private Resource newResource;

    @SuppressWarnings("unused")
    private int agentId;
    private int resourceTypeId;
    private TestServerPluginService testServerPluginService;

    @Override
    protected void beforeMethod() throws Exception {
        try {
            //we need this because the drift plugins are referenced from the system settings that we use in our tests
            testServerPluginService = new TestServerPluginService(getTempDir());
            prepareCustomServerPluginService(testServerPluginService);
            testServerPluginService.startMasterPluginContainer();

            prepareScheduler();
            TestServerCommunicationsService agentContainer = prepareForTestAgents();
            newResource = createNewResource();
        } catch (Throwable t) {
            System.err.println("Cannot prepare test: " + t);
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Override
    protected void afterMethod() throws Exception {
        try {
            deleteNewResource(newResource);
            unprepareForTestAgents();
            unprepareScheduler();
            unprepareServerPluginService();
        } catch (Throwable t) {
            System.err.println("Cannot unprepare test: " + t);
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    public void testPurge() throws Throwable {
        addDataToBePurged();
        triggerDataPurgeJobNow();
        triggerDataCalcJobNow();
        makeSureDataIsPurged();
    }

    public void testPurgeWhenDeleting() throws Throwable {
        addDataToBePurged();
        try {
            Subject overlord = LookupUtil.getSubjectManager().getOverlord();
            ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

            List<Integer> deletedIds = resourceManager.uninventoryResource(overlord, newResource.getId());
            resourceManager.uninventoryResourceAsyncWork(overlord, newResource.getId());

            assert deletedIds.size() == 1 : "didn't delete resource: " + deletedIds;
            assert deletedIds.get(0).intValue() == newResource.getId() : "what was deleted? : " + deletedIds;

            // I don't have the resource anymore so I can't use makeSureDataIsPurged to test
            // this test method will at least ensure no exceptions occur in resource manager
        } finally {
            newResource = null; // so our tear-down method doesn't try to delete it again
        }
        getTransactionManager().begin();
        try {

            /* agent is now implicitly deleted
            Agent agent = em.find(Agent.class, agentId);
            em.remove(agent);
            */

            ResourceType rt = em.find(ResourceType.class, resourceTypeId);
            Set<EventDefinition> evDs = rt.getEventDefinitions();
            if (evDs != null) {
                Iterator<EventDefinition> evdIter = evDs.iterator();
                while (evdIter.hasNext()) {
                    EventDefinition evd = evdIter.next();
                    em.remove(evd);
                    evdIter.remove();
                }
            }
            Set<MeasurementDefinition> mDefs = rt.getMetricDefinitions();
            if (mDefs != null) {
                Iterator<MeasurementDefinition> mdIter = mDefs.iterator();
                while (mdIter.hasNext()) {
                    MeasurementDefinition def = mdIter.next();
                    em.remove(def);
                    mdIter.remove();
                }
            }
            em.remove(rt);

            getTransactionManager().commit();
        } catch (Exception e) {
            getTransactionManager().rollback();
            throw e;
        }
    }

    private void addDataToBePurged() throws NotSupportedException, SystemException, Throwable {
        // add a bunch of data that is to be purged
        getTransactionManager().begin();

        try {
            try {
                // add alerts
                AlertDefinition ad = newResource.getAlertDefinitions().iterator().next();
                for (long timestamp = 0L; timestamp < 200L; timestamp++) {
                    Alert newAlert = createNewAlert(ad, timestamp);
                    assert newAlert.getCtime() == timestamp : "bad alert persisted:" + newAlert;
                    assert newAlert.getId() > 0 : "alert not persisted:" + newAlert;
                    if (timestamp % 50L == 0) {
                        em.flush();
                        em.clear();
                    }
                }
                em.flush();
                em.clear();

                // add availabilities
                for (long timestamp = 0L; timestamp < 2000L; timestamp += 2L) {
                    Availability newAvail = createNewAvailability(newResource, timestamp, timestamp + 1L);
                    assert newAvail.getStartTime() == timestamp : "bad avail persisted:" + newAvail;
                    assert newAvail.getEndTime() == (timestamp + 1L) : "bad avail persisted:" + newAvail;
                    assert newAvail.getId() > 0 : "avail not persisted:" + newAvail;
                    if (timestamp % 50L == 0) {
                        em.flush();
                        em.clear();
                    }
                }
                em.flush();
                em.clear();

                // add events
                createNewEvents(newResource, 0, 1000);

                // add calltime/response times
                createNewCalltimeData(newResource, 0, 1000);

                // add trait data
                createNewTraitData(newResource, 0L, 100);

                getTransactionManager().commit();
            } catch (Throwable t) {
                getTransactionManager().rollback();
                throw t;
            }
        } catch (Throwable t) {
            System.err.println("!!!!! DataPurgeJobTest.testPurge failed: " + ThrowableUtil.getAllMessages(t));
            t.printStackTrace();
            throw t;
        }
    }

    private void makeSureDataIsPurged() throws NotSupportedException, SystemException {
        // now that our data purge job is done, make sure none of our test data is left behind
        getTransactionManager().begin();

        try {
            Subject overlord = LookupUtil.getSubjectManager().getOverlord();
            Resource res = em.find(Resource.class, newResource.getId());

            // check alerts
            Set<AlertDefinition> alertDefinitions = res.getAlertDefinitions();
            assert alertDefinitions.size() == 1 : "why are we missing our alert definitions?: " + alertDefinitions;
            AlertDefinition ad = alertDefinitions.iterator().next();
            assert ad.getAlerts().size() == 0 : "didn't purge alerts";
            Set<AlertConditionLog> clogs = ad.getConditions().iterator().next().getConditionLogs();
            assert clogs.size() == 0 : "didn't purge condition logs: " + clogs.size();

            // check availabilities, remember, a new resource gets one initial avail record 
            List<Availability> avails = res.getAvailability();
            assert avails.size() == 1 : "didn't purge availabilities";

            // check events
            EventSource es = res.getEventSources().iterator().next();
            assert es.getEvents().size() == 0 : "didn't purge all events";

            // check calltime data
            int calltimeScheduleId = 0;
            for (MeasurementSchedule sched : res.getSchedules()) {
                if (sched.getDefinition().getDataType() == DataType.CALLTIME) {
                    calltimeScheduleId = sched.getId();
                    break;
                }
            }
            assert calltimeScheduleId > 0 : "why don't we have a calltime schedule?";
            PageList<CallTimeDataComposite> calltimeData = LookupUtil.getCallTimeDataManager()
                .findCallTimeDataForResource(overlord, calltimeScheduleId, 0, Long.MAX_VALUE, new PageControl());
            assert calltimeData.getTotalSize() == 0 : "didn't purge all calltime data";

            // check trait data
            MeasurementSchedule traitSchedule = null;
            for (MeasurementSchedule sched : res.getSchedules()) {
                if (sched.getDefinition().getDataType() == DataType.TRAIT) {
                    traitSchedule = sched;
                    break;
                }
            }
            assert traitSchedule != null : "why don't we have a trait schedule?";

            List<MeasurementDataTrait> persistedTraits = LookupUtil.getMeasurementDataManager().findTraits(overlord,
                res.getId(), traitSchedule.getDefinition().getId());
            assert persistedTraits.size() == 1 : "bad purge of trait data: " + persistedTraits.size();

        } finally {
            getTransactionManager().rollback();
        }
    }

    private void triggerDataPurgeJobNow() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        SchedulerLocal schedulerBean = LookupUtil.getSchedulerBean();
        schedulerBean.scheduleSimpleCronJob(DataPurgeJob.class, true, false, "0 0 0 1 1 ? 2099", null);

        schedulerBean.addGlobalJobListener(new JobListener() {
            public String getName() {
                return "DataPurgeJobTestListener";
            }

            public void jobExecutionVetoed(JobExecutionContext arg0) {
            }

            public void jobToBeExecuted(JobExecutionContext arg0) {
            }

            public void jobWasExecuted(JobExecutionContext c, JobExecutionException e) {
                if (c.getJobDetail().getJobClass().getName().equals(DataPurgeJob.class.getName())) {
                    latch.countDown(); // the data purge job is finished! let our test continue
                }
            }
        });

        try {
            // trigger the data purge job so it executes immediately - this does not block
            DataPurgeJob.purgeNow();

            // wait for the job to finish - abort the test if it takes too long
            assert latch.await(60, TimeUnit.SECONDS) : "Data purge job didn't complete in a timely fashion";
        } finally {
            schedulerBean.deleteJob(DataPurgeJob.class.getName(), DataPurgeJob.class.getName());
        }
    }

    private void triggerDataCalcJobNow() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        SchedulerLocal schedulerBean = LookupUtil.getSchedulerBean();
        schedulerBean.scheduleSimpleCronJob(DataCalcJob.class, true, false, "0 0 0 1 1 ? 2099", null);

        schedulerBean.addGlobalJobListener(new JobListener() {
            @Override
            public String getName() {
                return "DataCalcJobTestListener";
            }

            @Override
            public void jobExecutionVetoed(JobExecutionContext arg0) {
            }

            @Override
            public void jobToBeExecuted(JobExecutionContext arg0) {
            }

            @Override
            public void jobWasExecuted(JobExecutionContext c, JobExecutionException e) {
                if (c.getJobDetail().getJobClass().getName().equals(DataCalcJob.class.getName())) {
                    latch.countDown(); // the data calc job is finished! let our test continue
                }
            }
        });

        try {
            // trigger the data calc job so it executes immediately - this does not block
            DataCalcJob.calcNow();

            // wait for the job to finish - abort the test if it takes too long
            assertTrue("Data calc job didn't complete in a timely fashion", latch.await(60, TimeUnit.SECONDS));
        } finally {
            schedulerBean.deleteJob(DataCalcJob.class.getName(), DataCalcJob.class.getName());
        }

        return;
    }

    private void createNewTraitData(Resource res, long timestamp, int count) {
        MeasurementSchedule traitSchedule = null;
        for (MeasurementSchedule sched : res.getSchedules()) {
            if (sched.getDefinition().getDataType() == DataType.TRAIT) {
                traitSchedule = sched;
                break;
            }
        }
        assert traitSchedule != null : "why don't we have a trait schedule?";

        MeasurementDataManagerLocal mgr = LookupUtil.getMeasurementDataManager();

        MeasurementScheduleRequest msr = new MeasurementScheduleRequest(traitSchedule);

        Set<MeasurementDataTrait> dataset = new HashSet<MeasurementDataTrait>();
        for (int i = 0; i < count; i++) {
            dataset.add(new MeasurementDataTrait(timestamp + i, msr, "DataPurgeJobTestTraitValue" + i));
        }
        mgr.addTraitData(dataset);

        List<MeasurementDataTrait> persistedTraits = mgr.findTraits(LookupUtil.getSubjectManager().getOverlord(),
            res.getId(), traitSchedule.getDefinition().getId());
        assert persistedTraits.size() == count : "did not persist trait data:" + persistedTraits.size() + ":"
            + persistedTraits;
    }

    private void createNewCalltimeData(Resource res, long timestamp, int count) {
        MeasurementSchedule calltimeSchedule = null;
        for (MeasurementSchedule sched : res.getSchedules()) {
            if (sched.getDefinition().getDataType() == DataType.CALLTIME) {
                calltimeSchedule = sched;
                break;
            }
        }
        assert calltimeSchedule != null : "why don't we have a calltime schedule?";

        MeasurementScheduleRequest msr = new MeasurementScheduleRequest(calltimeSchedule);

        Set<CallTimeData> dataset = new HashSet<CallTimeData>();
        CallTimeData data = new CallTimeData(msr);

        for (int i = 0; i < count; i++) {
            for (int j = 0; j < count; j++) {
                data.addCallData("DataPurgeJobTestCalltimeData" + j, new Date(timestamp), 777);
            }
        }

        dataset.add(data);

        CallTimeDataManagerLocal mgr = LookupUtil.getCallTimeDataManager();
        mgr.addCallTimeData(dataset);

        PageList<CallTimeDataComposite> persistedData = mgr.findCallTimeDataForResource(LookupUtil.getSubjectManager()
            .getOverlord(), calltimeSchedule.getId(), timestamp - 1L, timestamp + count + 1L, new PageControl());
        // just a few sanity checks
        assert persistedData.getTotalSize() == count : "did not persist all calltime data, only persisted: "
            + persistedData.getTotalSize();
        assert persistedData.get(0).getCount() == count : "did not persist all endpoint calltime data, only persisted: "
            + persistedData.get(0).getCount();
    }

    private void createNewEvents(Resource res, long timestamp, int count) {
        EventDefinition ed = res.getResourceType().getEventDefinitions().iterator().next();
        EventSource source = new EventSource("datapurgejobtest", ed, res);
        Map<EventSource, Set<Event>> eventMap = new HashMap<EventSource, Set<Event>>();
        Set<Event> events = new HashSet<Event>();
        for (int i = 0; i < count; i++) {
            events.add(new Event(ed.getName(), source.getLocation(), timestamp + i, EventSeverity.DEBUG, "details"));
        }
        eventMap.put(source, events);

        EventManagerLocal mgr = LookupUtil.getEventManager();
        mgr.addEventData(eventMap);

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        PageList<EventComposite> persistedEvents = mgr.findEventComposites(overlord,
            EntityContext.forResource(res.getId()), timestamp - 1L, timestamp + count + 1L,
            new EventSeverity[] { EventSeverity.DEBUG }, null, null, new PageControl());
        assert persistedEvents.getTotalSize() == count : "did not persist all events, only persisted: "
            + persistedEvents.getTotalSize();

        return;
    }

    private Availability createNewAvailability(Resource res, long start, long end) {
        Availability a = new Availability(res, start, AvailabilityType.UP);
        if (end > 0) {
            a.setEndTime(end);
        }
        em.persist(a);
        return a;
    }

    private Alert createNewAlert(AlertDefinition ad, long timestamp) {
        Alert a = new Alert(ad, timestamp);
        em.persist(a);

        AlertNotificationLog anl = new AlertNotificationLog(a, "dummy", ResultState.SUCCESS, "message");
        em.persist(anl);

        AlertCondition ac = ad.getConditions().iterator().next();
        AlertConditionLog acl = new AlertConditionLog(ac, timestamp);
        acl.setAlert(a);
        acl.setValue("dummy value");
        em.persist(acl);

        return a;
    }

    private Resource createNewResource() throws Exception {
        getTransactionManager().begin();

        Resource resource;

        try {
            long now = System.currentTimeMillis();
            ResourceType resourceType = new ResourceType("plat" + now, "test", ResourceCategory.PLATFORM, null);

            em.persist(resourceType);
            resourceTypeId = resourceType.getId();

            Agent agent = new Agent("testagent" + now, "testaddress" + now, 1, "", "testtoken" + now);
            em.persist(agent);
            agentId = agent.getId();
            em.flush();

            resource = new Resource("reskey" + now, "resname", resourceType);
            resource.setUuid("" + new Random().nextInt());
            resource.setAgent(agent);
            em.persist(resource);

            AlertDefinition ad = new AlertDefinition();
            ad.setName("alertTest");
            ad.setEnabled(true);
            ad.setPriority(AlertPriority.HIGH);
            ad.setResource(resource);
            ad.setAlertDampening(new AlertDampening(AlertDampening.Category.NONE));
            ad.setConditionExpression(BooleanExpression.ALL);
            ad.setRecoveryId(0);
            em.persist(ad);

            AlertCondition ac = new AlertCondition(ad, AlertConditionCategory.AVAILABILITY);
            ac.setComparator("==");
            em.persist(ac);
            ad.addCondition(ac);

            EventDefinition ed = new EventDefinition(resourceType, "DataPurgeJobTestEventDefinition");
            em.persist(ed);
            resourceType.addEventDefinition(ed);

            // add calltime schedule
            MeasurementDefinition def = new MeasurementDefinition(resourceType, "DataPurgeJobTestCalltimeMeasDef");
            def.setCategory(MeasurementCategory.PERFORMANCE);
            def.setDataType(DataType.CALLTIME);
            def.setDefaultInterval(12345);
            def.setDefaultOn(true);
            def.setDestinationType("DataPurgeJobTestDestType");
            def.setDisplayName(def.getName());
            def.setDisplayType(DisplayType.SUMMARY);
            em.persist(def);
            MeasurementSchedule schedule = new MeasurementSchedule(def, resource);
            em.persist(schedule);
            def.addSchedule(schedule);
            resource.addSchedule(schedule);

            // add trait schedule
            def = new MeasurementDefinition(resourceType, "DataPurgeJobTestTraitMeasDef");
            def.setCategory(MeasurementCategory.PERFORMANCE);
            def.setDataType(DataType.TRAIT);
            def.setDefaultInterval(12345);
            def.setDefaultOn(true);
            def.setDisplayName(def.getName());
            def.setDisplayType(DisplayType.SUMMARY);
            em.persist(def);
            schedule = new MeasurementSchedule(def, resource);
            em.persist(schedule);
            def.addSchedule(schedule);
            resource.addSchedule(schedule);

            // add normal measurment schedule
            def = new MeasurementDefinition(resourceType, "DataPurgeJobTestNormalMeasDef");
            def.setCategory(MeasurementCategory.PERFORMANCE);
            def.setDataType(DataType.MEASUREMENT);
            def.setDefaultInterval(12345);
            def.setDefaultOn(true);
            def.setDisplayName(def.getName());
            def.setDisplayType(DisplayType.SUMMARY);
            em.persist(def);
            schedule = new MeasurementSchedule(def, resource);
            em.persist(schedule);
            def.addSchedule(schedule);
            resource.addSchedule(schedule);

        } catch (Exception e) {
            System.out.println("CANNOT PREPARE TEST: " + e);
            getTransactionManager().rollback();
            throw e;
        }

        getTransactionManager().commit();

        return resource;
    }

    private void deleteNewResource(Resource doomedResource) throws Exception {
        if (doomedResource != null) {
            // get the type and agent that we will delete after we delete the resource itself
            ResourceType doomedResourceType = doomedResource.getResourceType();
            Agent doomedAgent = doomedResource.getAgent();

            // delete the resource itself
            Subject overlord = LookupUtil.getSubjectManager().getOverlord();
            ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
            List<Integer> deletedIds = resourceManager.uninventoryResource(overlord, doomedResource.getId());
            for (Integer deletedResourceId : deletedIds) {
                resourceManager.uninventoryResourceAsyncWork(overlord, deletedResourceId);
            }

            // delete the agent and the type
            getTransactionManager().begin();

            try {
                Agent agent = em.find(Agent.class, doomedAgent.getId());
                if (agent != null)
                    em.remove(agent);

                ResourceType type = em.find(ResourceType.class, doomedResourceType.getId());
                if (type != null)
                    em.remove(type);
                getTransactionManager().commit();
            } catch (Exception e) {
                try {
                    System.out.println("CANNOT CLEAN UP TEST: Cause: " + e);
                    getTransactionManager().rollback();
                } catch (Exception ignore) {
                }
            }
        }
    }
}
