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

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.rhq.core.domain.common.composite.SystemSetting.PARTITION_EVENT_PURGE_PERIOD;
import static org.rhq.core.domain.common.composite.SystemSetting.RESOURCE_CONFIG_HISTORY_PURGE_PERIOD;

import java.util.ArrayList;
import java.util.Arrays;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.rhq.core.domain.cloud.FailoverList;
import org.rhq.core.domain.cloud.PartitionEvent;
import org.rhq.core.domain.cloud.PartitionEventDetails;
import org.rhq.core.domain.cloud.PartitionEventType;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.core.domain.configuration.AbstractConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.GroupResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.PartitionEventCriteria;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
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
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.PartitionEventManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerPluginService;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Tests that we can purge data.
 */
@Test
public class DataPurgeJobTest extends AbstractEJB3Test {
    private static final Log LOG = LogFactory.getLog(DataPurgeJobTest.class);

    private static final long ONE_MONTH = MILLISECONDS.convert(30, DAYS);

    private SubjectManagerLocal subjectManager;
    private SystemManagerLocal systemManager;
    private MeasurementDataManagerLocal measurementDataManager;
    private SchedulerLocal schedulerBean;
    private ResourceManagerLocal resourceManager;
    private CallTimeDataManagerLocal callTimeDataManager;
    private PartitionEventManagerLocal partitionEventManager;
    private EventManagerLocal eventManager;
    private ConfigurationManagerLocal configurationManager;

    private Subject overlord;
    private ResourceType platformType;
    private Agent testAgent;
    private Resource testPlatform;
    private ResourceGroup testGroup;
    private FailoverList testFailoverList;
    private String originalPartitionEventPurgePeriod;
    private String originalResourceConfigHistoryPurgePeriod;

    @Override
    protected void beforeMethod() throws Exception {
        try {

            //we need this because the drift plugins are referenced from the system settings that we use in our tests
            TestServerPluginService testServerPluginService = new TestServerPluginService(getTempDir());
            prepareCustomServerPluginService(testServerPluginService);
            testServerPluginService.startMasterPluginContainer();

            prepareScheduler();
            prepareForTestAgents();
            createBaseData();

            subjectManager = LookupUtil.getSubjectManager();
            overlord = subjectManager.getOverlord();

            systemManager = LookupUtil.getSystemManager();
            SystemSettings systemSettings = systemManager.getSystemSettings(overlord);
            originalPartitionEventPurgePeriod = systemSettings.get(PARTITION_EVENT_PURGE_PERIOD);
            systemSettings.put(PARTITION_EVENT_PURGE_PERIOD, String.valueOf(ONE_MONTH));
            originalResourceConfigHistoryPurgePeriod = systemSettings.get(RESOURCE_CONFIG_HISTORY_PURGE_PERIOD);
            systemSettings.put(RESOURCE_CONFIG_HISTORY_PURGE_PERIOD, String.valueOf(ONE_MONTH));
            systemManager.setSystemSettings(overlord, systemSettings);

            measurementDataManager = LookupUtil.getMeasurementDataManager();
            schedulerBean = LookupUtil.getSchedulerBean();
            resourceManager = LookupUtil.getResourceManager();
            callTimeDataManager = LookupUtil.getCallTimeDataManager();
            partitionEventManager = LookupUtil.getPartitionEventManager();
            eventManager = LookupUtil.getEventManager();
            configurationManager = LookupUtil.getConfigurationManager();

        } catch (Throwable t) {
            LOG.error("Cannot prepare test", t);
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Override
    protected void afterMethod() throws Exception {
        try {

            SystemSettings systemSettings = systemManager.getSystemSettings(overlord);
            systemSettings.put(PARTITION_EVENT_PURGE_PERIOD, originalPartitionEventPurgePeriod);
            systemSettings.put(RESOURCE_CONFIG_HISTORY_PURGE_PERIOD, originalResourceConfigHistoryPurgePeriod);
            systemManager.setSystemSettings(overlord, systemSettings);

            deleteBaseData();
            unprepareForTestAgents();
            unprepareScheduler();
            unprepareServerPluginService();

        } catch (Throwable t) {
            LOG.error("Cannot unprepare test", t);
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

    public void testResourceConfigurationPurge() throws Throwable {
        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                createNewResourceConfigurationUpdates(1000);
            }
        });
        triggerDataPurgeJobNow();
        triggerDataCalcJobNow();
        makeSureResourceConfigurationDataIsPurged();
    }

    public void testGroupResourceConfigurationPurge() throws Throwable {
        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                createNewGroupResourceConfigurationUpdates(1000);
            }
        });
        triggerDataPurgeJobNow();
        triggerDataCalcJobNow();
        makeSureGroupResourceConfigurationDataIsPurged();
    }

    public void testPurgeWhenDeleting() throws Throwable {
        addDataToBePurged();
        triggerDataPurgeJobNow();
        triggerDataCalcJobNow();
        try {
            List<Integer> deletedIds = resourceManager.uninventoryResource(overlord, testPlatform.getId());
            resourceManager.uninventoryResourceAsyncWork(overlord, testPlatform.getId());

            assertEquals("didn't delete resource: " + deletedIds, 1, deletedIds.size());
            assertEquals("what was deleted? : " + deletedIds, testPlatform.getId(), deletedIds.get(0).intValue());

            // I don't have the resource anymore so I can't use makeSureDataIsPurged to test
            // this test method will at least ensure no exceptions occur in resource manager
        } finally {
            testPlatform = null; // so our tear-down method doesn't try to delete it again
        }

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {

                ResourceType rt = em.find(ResourceType.class, platformType.getId());

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

            }
        });
    }

    private void addDataToBePurged() throws Throwable {
        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {

                // add alerts
                AlertDefinition ad = testPlatform.getAlertDefinitions().iterator().next();
                for (long timestamp = 0L; timestamp < 200L; timestamp++) {
                    Alert newAlert = createNewAlert(ad, timestamp);
                    assertEquals("bad alert persisted:" + newAlert, timestamp, newAlert.getCtime());
                    assertTrue("alert not persisted:" + newAlert, newAlert.getId() > 0);
                    if (timestamp % 50L == 0) {
                        em.flush();
                        em.clear();
                    }
                }
                em.flush();
                em.clear();

                // add availabilities
                for (long timestamp = 0L; timestamp < 2000L; timestamp += 2L) {
                    Availability newAvail = createNewAvailability(testPlatform, timestamp, timestamp + 1L);
                    assertEquals("bad avail persisted:" + newAvail, timestamp, newAvail.getStartTime().longValue());
                    assertEquals("bad avail persisted:" + newAvail, timestamp + 1, newAvail.getEndTime().longValue());
                    assertTrue("avail not persisted:" + newAvail, newAvail.getId() > 0);
                    if (timestamp % 50L == 0) {
                        em.flush();
                        em.clear();
                    }
                }
                em.flush();
                em.clear();

                // add events
                createNewEvents(testPlatform, 0, 1000);

                // add calltime/response times
                createNewCalltimeData(testPlatform, 0, 1000);

                // add trait data
                createNewTraitData(testPlatform, 0L, 100);

                // add partition event data
                createNewPartitionEvents(1000);
            }
        });
    }

    private void makeSureDataIsPurged() throws NotSupportedException, SystemException {
        // now that our data purge job is done, make sure none of our test data is left behind
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {

                Subject overlord = subjectManager.getOverlord();
                Resource res = em.find(Resource.class, testPlatform.getId());

                // check alerts
                Set<AlertDefinition> alertDefinitions = res.getAlertDefinitions();
                assertEquals("why are we missing our alert definitions?: " + alertDefinitions, 1,
                    alertDefinitions.size());
                AlertDefinition ad = alertDefinitions.iterator().next();
                assertEquals("didn't purge alerts", 0, ad.getAlerts().size());
                Set<AlertConditionLog> clogs = ad.getConditions().iterator().next().getConditionLogs();
                assertEquals("didn't purge condition logs: " + clogs.size(), 0, clogs.size());

                // check availabilities, remember, a new resource gets one initial avail record
                List<Availability> avails = res.getAvailability();
                assertEquals("didn't purge availabilities", 1, avails.size());

                // check events
                EventSource es = res.getEventSources().iterator().next();
                assertEquals("didn't purge all events", 0, es.getEvents().size());

                // check calltime data
                int calltimeScheduleId = 0;
                for (MeasurementSchedule sched : res.getSchedules()) {
                    if (sched.getDefinition().getDataType() == DataType.CALLTIME) {
                        calltimeScheduleId = sched.getId();
                        break;
                    }
                }
                assertTrue("why don't we have a calltime schedule?", calltimeScheduleId > 0);
                PageList<CallTimeDataComposite> calltimeData = callTimeDataManager.findCallTimeDataForResource(
                    overlord, calltimeScheduleId, 0, Long.MAX_VALUE, new PageControl());
                assertEquals("didn't purge all calltime data", 0, calltimeData.getTotalSize());

                // check trait data
                MeasurementSchedule traitSchedule = null;
                for (MeasurementSchedule sched : res.getSchedules()) {
                    if (sched.getDefinition().getDataType() == DataType.TRAIT) {
                        traitSchedule = sched;
                        break;
                    }
                }
                assertNotNull("why don't we have a trait schedule?", traitSchedule);

                List<MeasurementDataTrait> persistedTraits = measurementDataManager.findTraits(overlord, res.getId(),
                    traitSchedule.getDefinition().getId());
                assertEquals("bad purge of trait data: " + persistedTraits.size(), 1, persistedTraits.size());

                // check partition events
                // There's no need to check partition event details purge as there's a FK constraint on events
                PartitionEventCriteria partitionEventCriteria = new PartitionEventCriteria();
                partitionEventCriteria.addFilterEventDetail(DataPurgeJobTest.class.getName());
                PageList<PartitionEvent> partitionEventData = partitionEventManager.findPartitionEventsByCriteria(
                    overlord, partitionEventCriteria);
                // one event should be kept as it is attached to a failover list
                assertEquals("didn't purge all partition event data", 1, partitionEventData.getTotalSize());

            }
        });
    }

    private void makeSureResourceConfigurationDataIsPurged() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
                criteria.addFilterResourceIds(testPlatform.getId());
                criteria.addSortCreatedTime(PageOrdering.DESC);

                PageList<ResourceConfigurationUpdate> resourceConfigUpdates = configurationManager
                    .findResourceConfigurationUpdatesByCriteria(subjectManager.getOverlord(), criteria);
                List<AbstractConfigurationUpdate> configUpdates = new ArrayList<AbstractConfigurationUpdate>(
                    resourceConfigUpdates.size());
                configUpdates.addAll(resourceConfigUpdates);

                checkConfigurationUpdates(configUpdates);
            }
        });
    }

    private void makeSureGroupResourceConfigurationDataIsPurged() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                ResourceConfigurationUpdateCriteria updateCriteria = new ResourceConfigurationUpdateCriteria();
                updateCriteria.addFilterResourceIds(testPlatform.getId());
                updateCriteria.addFilterResourceGroupIds(testGroup.getId());
                updateCriteria.addSortCreatedTime(PageOrdering.DESC);

                PageList<ResourceConfigurationUpdate> resourceConfigUpdates = configurationManager
                    .findResourceConfigurationUpdatesByCriteria(subjectManager.getOverlord(), updateCriteria);
                List<AbstractConfigurationUpdate> configUpdates = new ArrayList<AbstractConfigurationUpdate>(
                    resourceConfigUpdates.size());
                configUpdates.addAll(resourceConfigUpdates);

                checkConfigurationUpdates(configUpdates);

                GroupResourceConfigurationUpdateCriteria groupUpdateCriteria = new GroupResourceConfigurationUpdateCriteria();
                groupUpdateCriteria.addFilterResourceGroupIds(Arrays.asList(testGroup.getId()));
                groupUpdateCriteria.addSortCreatedTime(PageOrdering.DESC);

                PageList<GroupResourceConfigurationUpdate> groupResourceConfigUpdates = configurationManager
                    .findGroupResourceConfigurationUpdatesByCriteria(subjectManager.getOverlord(), groupUpdateCriteria);
                configUpdates = new ArrayList<AbstractConfigurationUpdate>(groupResourceConfigUpdates.size());
                configUpdates.addAll(groupResourceConfigUpdates);

                checkConfigurationUpdates(configUpdates);
            }
        });
    }

    private void checkConfigurationUpdates(List<AbstractConfigurationUpdate> configUpdates) {
        assertEquals("Expected 2 config updates: " + configUpdates, 2, configUpdates.size());

        Iterator<AbstractConfigurationUpdate> iterator = configUpdates.iterator();
        long purgeUpToTime = System.currentTimeMillis() - ONE_MONTH;

        AbstractConfigurationUpdate configUpdate = iterator.next();
        assertEquals("Expected youngest update to be a failure", ConfigurationUpdateStatus.FAILURE,
            configUpdate.getStatus());
        assertTrue("Expected youngest update creation time to be greater than " + new Date(purgeUpToTime),
            configUpdate.getCreatedTime() > purgeUpToTime);

        configUpdate = iterator.next();
        assertEquals("Expected oldest update to be a success", ConfigurationUpdateStatus.SUCCESS,
            configUpdate.getStatus());
        assertTrue("Expected oldest update creation time to be less than " + new Date(purgeUpToTime),
            configUpdate.getCreatedTime() < purgeUpToTime);
    }

    private void triggerDataPurgeJobNow() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        schedulerBean.scheduleSimpleCronJob(DataPurgeJob.class, true, false, "0 0 0 1 1 ? 2099", null);

        schedulerBean.addGlobalJobListener(new JobListener() {
            @Override
            public String getName() {
                return "DataPurgeJobTestListener";
            }

            @Override
            public void jobExecutionVetoed(JobExecutionContext arg0) {
            }

            @Override
            public void jobToBeExecuted(JobExecutionContext arg0) {
            }

            @Override
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
            assertTrue("Data purge job didn't complete in a timely fashion", latch.await(60, TimeUnit.SECONDS));
        } finally {
            schedulerBean.deleteJob(DataPurgeJob.class.getName(), DataPurgeJob.class.getName());
        }
    }

    private void triggerDataCalcJobNow() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

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

    }

    private void createNewTraitData(Resource res, long timestamp, int count) {
        MeasurementSchedule traitSchedule = null;
        for (MeasurementSchedule sched : res.getSchedules()) {
            if (sched.getDefinition().getDataType() == DataType.TRAIT) {
                traitSchedule = sched;
                break;
            }
        }
        assertNotNull("why don't we have a trait schedule?", traitSchedule);

        MeasurementScheduleRequest msr = new MeasurementScheduleRequest(traitSchedule);

        Set<MeasurementDataTrait> dataset = new HashSet<MeasurementDataTrait>();
        for (int i = 0; i < count; i++) {
            dataset.add(new MeasurementDataTrait(timestamp + i, msr, "DataPurgeJobTestTraitValue" + i));
        }
        measurementDataManager.addTraitData(dataset);

        List<MeasurementDataTrait> persistedTraits = measurementDataManager.findTraits(subjectManager.getOverlord(),
            res.getId(), traitSchedule.getDefinition().getId());
        assertEquals("did not persist trait data:" + persistedTraits.size() + ":" + persistedTraits, count,
            persistedTraits.size());
    }

    private void createNewCalltimeData(Resource res, long timestamp, int count) {
        MeasurementSchedule calltimeSchedule = null;
        for (MeasurementSchedule sched : res.getSchedules()) {
            if (sched.getDefinition().getDataType() == DataType.CALLTIME) {
                calltimeSchedule = sched;
                break;
            }
        }
        assertNotNull("why don't we have a calltime schedule?", calltimeSchedule);

        MeasurementScheduleRequest msr = new MeasurementScheduleRequest(calltimeSchedule);

        Set<CallTimeData> dataset = new HashSet<CallTimeData>();
        CallTimeData data = new CallTimeData(msr);

        for (int i = 0; i < count; i++) {
            for (int j = 0; j < count; j++) {
                data.addCallData("DataPurgeJobTestCalltimeData" + j, new Date(timestamp), 777);
            }
        }

        dataset.add(data);

        callTimeDataManager.addCallTimeData(dataset);

        PageList<CallTimeDataComposite> persistedData = callTimeDataManager.findCallTimeDataForResource(
            subjectManager.getOverlord(), calltimeSchedule.getId(), timestamp - 1L, timestamp + count + 1L,
            new PageControl());
        // just a few sanity checks
        assertEquals("did not persist all calltime data, only persisted: " + persistedData.getTotalSize(), count,
            persistedData.getTotalSize());
        assertEquals("did not persist all endpoint calltime data, only persisted: " + persistedData.get(0).getCount(),
            count, persistedData.get(0).getCount());
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

        eventManager.addEventData(eventMap);

        Subject overlord = subjectManager.getOverlord();
        PageList<EventComposite> persistedEvents = eventManager.findEventComposites(overlord,
            EntityContext.forResource(res.getId()), timestamp - 1L, timestamp + count + 1L,
            new EventSeverity[] { EventSeverity.DEBUG }, null, null, new PageControl());
        assertEquals("did not persist all events, only persisted: " + persistedEvents.getTotalSize(), count,
            persistedEvents.getTotalSize());
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

    private void createNewPartitionEvents(int count) {
        long start = System.currentTimeMillis() - MILLISECONDS.convert(60, DAYS);
        PartitionEventType[] partitionEventTypes = PartitionEventType.values();
        PartitionEvent.ExecutionStatus[] executionStatuses = PartitionEvent.ExecutionStatus.values();
        Server server = new Server();
        server.setName("DataPurgeJobTest Server");
        String eventDetail = DataPurgeJobTest.class.getName();
        for (int i = 0; i < count; i++) {
            PartitionEventType eventType = partitionEventTypes[i % partitionEventTypes.length];
            PartitionEvent.ExecutionStatus executionStatus = executionStatuses[i % executionStatuses.length];
            PartitionEvent event = new PartitionEvent(overlord.getName(), eventType, eventDetail, executionStatus);
            PartitionEventDetails details = new PartitionEventDetails(event, testAgent, server);
            em.persist(event);
            event.setCtime(start - MILLISECONDS.convert(i, HOURS));
            em.persist(details);
            if (i == 0) {
                testFailoverList = new FailoverList(event, testAgent);
                em.persist(testFailoverList);
            }
        }
    }

    private void createNewResourceConfigurationUpdates(int count) {
        long now = System.currentTimeMillis();
        long start = now - MILLISECONDS.convert(90, DAYS);
        for (int i = 0; i < count; i++) {
            Configuration config = new Configuration();
            config.setSimpleValue("pipo", "molo");
            ResourceConfigurationUpdate configUpdate = new ResourceConfigurationUpdate(testPlatform, config, "testUser");
            configUpdate.setStatus(i % 2 == 0 ? ConfigurationUpdateStatus.SUCCESS : ConfigurationUpdateStatus.FAILURE);
            em.persist(configUpdate);
            em.flush();
            if (i < count - 1) {
                long time = start + MILLISECONDS.convert(i, HOURS);
                int updated = em
                    .createQuery(
                        "update ResourceConfigurationUpdate set createdTime = :createdTime"
                            + ", modifiedTime = :modifiedTime where id = :id").setParameter("createdTime", time)
                    .setParameter("modifiedTime", time).setParameter("id", configUpdate.getId()).executeUpdate();
                assertEquals(1, updated);
            } else {
                configUpdate.setStatus(ConfigurationUpdateStatus.FAILURE);
                em.flush();
            }
        }
    }

    private void createNewGroupResourceConfigurationUpdates(int count) {
        long now = System.currentTimeMillis();
        long start = now - MILLISECONDS.convert(90, DAYS);
        for (int i = 0; i < count; i++) {
            ConfigurationUpdateStatus status = i % 2 == 0 ? ConfigurationUpdateStatus.SUCCESS
                : ConfigurationUpdateStatus.FAILURE;
            GroupResourceConfigurationUpdate groupConfigUpdate = new GroupResourceConfigurationUpdate(testGroup,
                "testUser");
            groupConfigUpdate.setStatus(status);
            Configuration config = new Configuration();
            config.setSimpleValue("pipo", "molo");
            ResourceConfigurationUpdate configUpdate = new ResourceConfigurationUpdate(testPlatform, config, "testUser");
            configUpdate.setStatus(status);
            groupConfigUpdate.addConfigurationUpdate(configUpdate);
            configUpdate.setGroupConfigurationUpdate(groupConfigUpdate);
            em.persist(groupConfigUpdate);
            em.flush();
            if (i < count - 1) {
                long time = start + MILLISECONDS.convert(i, HOURS);
                int updated = em
                    .createQuery(
                        "update ResourceConfigurationUpdate set createdTime = :createdTime"
                            + ", modifiedTime = :modifiedTime where id = :id").setParameter("createdTime", time)
                    .setParameter("modifiedTime", time).setParameter("id", configUpdate.getId()).executeUpdate();
                assertEquals(1, updated);
                updated = em
                    .createQuery(
                        "update GroupResourceConfigurationUpdate set createdTime = :createdTime"
                            + ", modifiedTime = :modifiedTime where id = :id").setParameter("createdTime", time)
                    .setParameter("modifiedTime", time).setParameter("id", groupConfigUpdate.getId()).executeUpdate();
                assertEquals(1, updated);
            } else {
                groupConfigUpdate.setStatus(ConfigurationUpdateStatus.FAILURE);
                configUpdate.setStatus(ConfigurationUpdateStatus.FAILURE);
                em.flush();
            }
        }
    }

    private void createBaseData() throws Exception {
        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {

                long now = System.currentTimeMillis();

                platformType = new ResourceType("plat" + now, "test", ResourceCategory.PLATFORM, null);
                em.persist(platformType);

                testAgent = new Agent("testagent" + now, "testaddress" + now, 1, "", "testtoken" + now);
                em.persist(testAgent);
                em.flush();

                testPlatform = new Resource("reskey" + now, "resname", platformType);
                Random random = new Random();
                testPlatform.setUuid("" + random.nextInt());
                testPlatform.setAgent(testAgent);
                em.persist(testPlatform);

                testGroup = new ResourceGroup("resgroup" + now, platformType);
                testGroup.addExplicitResource(testPlatform);
                em.persist(testGroup);

                AlertDefinition ad = new AlertDefinition();
                ad.setName("alertTest");
                ad.setEnabled(true);
                ad.setPriority(AlertPriority.HIGH);
                ad.setResource(testPlatform);
                ad.setAlertDampening(new AlertDampening(AlertDampening.Category.NONE));
                ad.setConditionExpression(BooleanExpression.ALL);
                ad.setRecoveryId(0);
                em.persist(ad);

                AlertCondition ac = new AlertCondition(ad, AlertConditionCategory.AVAILABILITY);
                ac.setComparator("==");
                em.persist(ac);
                ad.addCondition(ac);

                EventDefinition ed = new EventDefinition(platformType, "DataPurgeJobTestEventDefinition");
                em.persist(ed);
                platformType.addEventDefinition(ed);

                // add calltime schedule
                MeasurementDefinition def = new MeasurementDefinition(platformType, "DataPurgeJobTestCalltimeMeasDef");
                def.setCategory(MeasurementCategory.PERFORMANCE);
                def.setDataType(DataType.CALLTIME);
                def.setDefaultInterval(12345);
                def.setDefaultOn(true);
                def.setDestinationType("DataPurgeJobTestDestType");
                def.setDisplayName(def.getName());
                def.setDisplayType(DisplayType.SUMMARY);
                em.persist(def);
                MeasurementSchedule schedule = new MeasurementSchedule(def, testPlatform);
                em.persist(schedule);
                def.addSchedule(schedule);
                testPlatform.addSchedule(schedule);

                // add trait schedule
                def = new MeasurementDefinition(platformType, "DataPurgeJobTestTraitMeasDef");
                def.setCategory(MeasurementCategory.PERFORMANCE);
                def.setDataType(DataType.TRAIT);
                def.setDefaultInterval(12345);
                def.setDefaultOn(true);
                def.setDisplayName(def.getName());
                def.setDisplayType(DisplayType.SUMMARY);
                em.persist(def);
                schedule = new MeasurementSchedule(def, testPlatform);
                em.persist(schedule);
                def.addSchedule(schedule);
                testPlatform.addSchedule(schedule);

                // add normal measurment schedule
                def = new MeasurementDefinition(platformType, "DataPurgeJobTestNormalMeasDef");
                def.setCategory(MeasurementCategory.PERFORMANCE);
                def.setDataType(DataType.MEASUREMENT);
                def.setDefaultInterval(12345);
                def.setDefaultOn(true);
                def.setDisplayName(def.getName());
                def.setDisplayType(DisplayType.SUMMARY);
                em.persist(def);
                schedule = new MeasurementSchedule(def, testPlatform);
                em.persist(schedule);
                def.addSchedule(schedule);
                testPlatform.addSchedule(schedule);

            }
        });
    }

    private void deleteBaseData() throws Exception {
        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                if (testFailoverList != null) {
                    FailoverList failoverListToDelete = em.find(FailoverList.class, testFailoverList.getId());
                    if (failoverListToDelete != null) {
                        em.remove(failoverListToDelete);
                        em.flush();
                    }
                }

                List<PartitionEvent> partitionEvents = em //
                    .createQuery("from PartitionEvent where eventDetail = :eventDetail", PartitionEvent.class) //
                    .setParameter("eventDetail", DataPurgeJobTest.class.getName()) //
                    .getResultList();

                for (PartitionEvent event : partitionEvents) {
                    // Will cascade remove PartitionEventDetails
                    em.remove(event);
                }
                em.flush();

                if (testGroup != null) {
                    ResourceGroup resourceGroup = em.find(ResourceGroup.class, testGroup.getId());
                    em.remove(resourceGroup);
                    em.flush();
                }
            }
        });

        if (testPlatform != null) {

            Subject overlord = subjectManager.getOverlord();
            List<Integer> uninventoriedResourceIds = resourceManager
                .uninventoryResource(overlord, testPlatform.getId());
            for (Integer uninventoriedResourceId : uninventoriedResourceIds) {
                resourceManager.uninventoryResourceAsyncWork(overlord, uninventoriedResourceId);
            }

            executeInTransaction(false, new TransactionCallback() {
                @Override
                public void execute() throws Exception {

                    Agent agent = em.find(Agent.class, testPlatform.getAgent().getId());
                    if (agent != null) {
                        em.remove(agent);
                        em.flush();
                    }

                    ResourceType typeToDelete = em.find(ResourceType.class, testPlatform.getResourceType().getId());
                    if (typeToDelete != null) {
                        em.remove(typeToDelete);
                        em.flush();
                    }

                }
            });
        }
    }
}
