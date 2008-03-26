package org.rhq.enterprise.server.scheduler.jobs;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
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
import org.rhq.core.domain.alert.notification.EmailNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Tests that we can purge data.
 */
@Test
public class DataPurgeJobTest extends AbstractEJB3Test {
    private Resource newResource;

    @BeforeMethod
    public void beforeMethod() throws Throwable {
        try {
            prepareScheduler();
            TestServerCommunicationsService agentContainer = prepareForTestAgents();
            newResource = createNewResource();
        } catch (Throwable t) {
            System.err.println("Cannot prepare test: " + t);
            t.printStackTrace();
            throw t;
        }
    }

    @AfterMethod
    public void afterMethod() throws Throwable {
        try {
            deleteNewResource(newResource);
            unprepareForTestAgents();
            unprepareScheduler();
        } catch (Throwable t) {
            System.err.println("Cannot unprepare test: " + t);
            t.printStackTrace();
            throw t;
        }
    }

    public void testPurge() throws Throwable {
        // add a bunch of data that is to be purged
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            try {
                // add alerts
                AlertDefinition ad = newResource.getAlertDefinitions().iterator().next();
                for (long timestamp = 0; timestamp < 1000; timestamp++) {
                    Alert newAlert = createNewAlert(em, ad, timestamp);
                    assert newAlert.getCtime() == timestamp : "bad alert persisted:" + newAlert;
                    assert newAlert.getId() > 0 : "alert not persisted:" + newAlert;
                }
                em.flush();
                em.clear();

                // add availabilities
                for (long timestamp = 0; timestamp < 2000; timestamp += 2) {
                    Availability newAvail = createNewAvailability(em, newResource, timestamp, timestamp + 1);
                    assert newAvail.getStartTime().getTime() == timestamp : "bad avail persisted:" + newAvail;
                    assert newAvail.getEndTime().getTime() == (timestamp + 1) : "bad avail persisted:" + newAvail;
                    assert newAvail.getId() > 0 : "avail not persisted:" + newAvail;
                }
                em.flush();
                em.clear();

                // add events
                createNewEvents(newResource, 0, 1000);

                getTransactionManager().commit();
            } catch (Throwable t) {
                getTransactionManager().rollback();
                throw t;
            }
        } catch (Throwable t) {
            System.err.println("!!!!! DataPurgeJobTest.testPurge failed: " + ThrowableUtil.getAllMessages(t));
            throw t;
        } finally {
            em.close();
        }

        triggerDataPurgeJobNow();

        // now that our data purge job is done, make sure none of our test data is left behind
        getTransactionManager().begin();
        em = getEntityManager();
        try {
            Resource res = em.find(Resource.class, newResource.getId());

            // check alerts
            Set<AlertDefinition> alertDefinitions = res.getAlertDefinitions();
            assert alertDefinitions.size() == 1 : "why are we missing our alert definitions?: " + alertDefinitions;
            assert alertDefinitions.iterator().next().getAlerts().size() == 0 : "didn't purge alerts";

            // check availabilities
            List<Availability> avails = res.getAvailability();
            assert avails.size() == 0 : "didn't purge availabilities";
        } finally {
            getTransactionManager().rollback();
            em.close();
        }

        return;
    }

    private void triggerDataPurgeJobNow() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        SchedulerLocal schedulerBean = LookupUtil.getSchedulerBean();
        schedulerBean.scheduleSimpleCronJob(DataPurgeJob.class, true, false, "0 0 0 1 1 ? 2099");

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

        return;
    }

    private void createNewEvents(Resource res, long timestamp, int count) {
        EventDefinition ed = res.getResourceType().getEventDefinitions().iterator().next();
        EventSource source = new EventSource("datapurgejobtest", ed, res);
        Map<EventSource, Set<Event>> eventMap = new HashMap<EventSource, Set<Event>>();
        Set<Event> events = new HashSet<Event>();
        for (int i = 0; i < count; i++) {
            events.add(new Event(ed.getName(), source.getLocation(), new Date(timestamp + i), EventSeverity.DEBUG,
                "details"));
        }
        eventMap.put(source, events);

        EventManagerLocal mgr = LookupUtil.getEventManager();
        mgr.addEventData(eventMap);
        PageList<EventComposite> persistedEvents = mgr.getEvents(LookupUtil.getSubjectManager().getOverlord(),
            new int[] { res.getId() }, timestamp - 1, timestamp + count + 1, EventSeverity.DEBUG, -1, null, null,
            new PageControl());
        assert persistedEvents.getTotalSize() == count : "did not persist all events, only persisted "
            + persistedEvents.getTotalSize();

        return;
    }

    private Availability createNewAvailability(EntityManager em, Resource res, long start, long end) {
        Availability a = new Availability(res, new Date(start), AvailabilityType.UP);
        if (end > 0) {
            a.setEndTime(new Date(end));
        }
        em.persist(a);
        return a;
    }

    private Alert createNewAlert(EntityManager em, AlertDefinition ad, long timestamp) {
        AlertCondition ac = ad.getConditions().iterator().next();
        AlertConditionLog acl = new AlertConditionLog(ac, timestamp);
        em.persist(acl);

        Alert a = new Alert(ad, timestamp);
        em.persist(a);

        AlertNotificationLog anl = new AlertNotificationLog(ad);
        anl.setAlert(a);
        em.persist(anl);

        return a;
    }

    private Resource createNewResource() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        Resource resource;

        try {
            try {
                long now = System.currentTimeMillis();
                ResourceType resourceType = new ResourceType("plat" + now, "test", ResourceCategory.PLATFORM, null);

                em.persist(resourceType);

                Agent agent = new Agent("testagent" + now, "testaddress" + now, 1, "", "testtoken" + now);
                em.persist(agent);
                em.flush();

                resource = new Resource("reskey" + now, "resname", resourceType);
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

                EmailNotification an = new EmailNotification(ad, "foo@bar.com");
                em.persist(an);
                ad.addAlertNotification(an);

                EventDefinition ed = new EventDefinition(resourceType, "DataPurgeJobTestEventDefinition");
                em.persist(ed);
                resourceType.addEventDefinition(ed);
            } catch (Exception e) {
                System.out.println("CANNOT PREPARE TEST: " + e);
                getTransactionManager().rollback();
                throw e;
            }

            getTransactionManager().commit();
        } finally {
            em.close();
        }

        return resource;
    }

    private void deleteNewResource(Resource doomedResource) throws Exception {
        if (doomedResource != null) {
            // get the type and agent that we will delete after we delete the resource itself
            ResourceType doomedResourceType = doomedResource.getResourceType();
            Agent doomedAgent = doomedResource.getAgent();

            // delete the resource itself
            Subject overlord = LookupUtil.getSubjectManager().getOverlord();
            LookupUtil.getResourceManager().deleteResource(overlord, doomedResource.getId());

            // delete the agent and the type
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
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
            } finally {
                em.close();
            }
        }
    }
}
