package org.rhq.enterprise.server.scheduler.jobs;

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
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
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
    public void beforeMethod() throws Exception {
        try {
            prepareScheduler();
            TestServerCommunicationsService agentContainer = prepareForTestAgents();
            newResource = createNewResource();
        } catch (Throwable t) {
            System.err.println("Cannot prepare test: " + t);
            t.printStackTrace();
        }
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        try {
            deleteNewResource(newResource);
            unprepareForTestAgents();
            unprepareScheduler();
        } catch (Throwable t) {
            System.err.println("Cannot unprepare test: " + t);
            t.printStackTrace();
        }
    }

    public void testPurge() throws Exception {

        // TODO: add a bunch of data that is to be purged
        // - alerts
        // - availabilities
        // - events
        // - response times?

        triggerDataPurgeJobNow();

        // TODO: now that our data purge job is done, make sure none of our test data is left behind
    }

    private void triggerDataPurgeJobNow() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        SchedulerLocal schedulerBean = LookupUtil.getSchedulerBean();
        schedulerBean.scheduleSimpleCronJob(DataPurgeJob.class, true, false, "0 0 * * * ?");

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

    private Resource createNewResource() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        Resource resource;

        try {
            try {
                long now = System.currentTimeMillis();
                ResourceType resourceType = new ResourceType("plat" + now, "test", ResourceCategory.PLATFORM, null);

                em.persist(resourceType);

                Agent agent = new Agent("testagent", "testaddress", 1, "", "testtoken");
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

                EmailNotification an = new EmailNotification(ad, "foo@bar.com");
                em.persist(an);

                AlertConditionLog acl = new AlertConditionLog(ac, now);
                em.persist(acl);

                Alert a = new Alert(ad, now);
                AlertNotificationLog anl = new AlertNotificationLog(ad);
                anl.setAlert(a);
                em.persist(a);
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
