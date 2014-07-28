package org.rhq.enterprise.server.scheduler.jobs;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.storage.StorageClusterMaintenanceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * <p>
 * This job is responsible for polling the storage cluster maintenance queue. It will drain the queue, executing
 * everything found. When the queue is empty or when remaining elements cannot be executed, this job will reschedule
 * itself to execute again in the future.
 * </p>
 * <p>
 * This job does not execute using a pre-defined schedule or a pre-defined interval because execution times can vary
 * greatly depending on the maintenance tasks being performed. For instance, running anti-entropy repair on a cluster
 * can easily take several hours whereas changing a node's heap settings might take a couple seconds. Because of this
 * we are implementing a simple form of job chaining (which is not provided out of the box by Quartz AFAIK).
 * </p>
 *
 * @author John Sanda
 */
public class StorageClusterMaintenanceJob extends AbstractStatefulJob {

    public static final String TRIGGER_PREFIX = "StorageClusterMaintenanceTrigger";

    public static final String GROUP_NAME = "StorageClusterMaintenanceGroup";

    private Log log = LogFactory.getLog(StorageClusterMaintenanceJob.class);

    public static Trigger getTrigger() {
        String triggerName = TRIGGER_PREFIX + "-" + System.currentTimeMillis();
        SimpleTrigger trigger = new SimpleTrigger(triggerName, GROUP_NAME,
            new Date(System.currentTimeMillis() + getInterval()));
        trigger.setJobName(StorageClusterMaintenanceJob.class.getName());
        trigger.setJobGroup(GROUP_NAME);

        return trigger;
    }

    private static long getInterval() {
        // Eventually we will want to store the interval in the storage cluster settings
        // and allow the user to modify it that way. And then the following system property
        // can be used as an override.
        return Long.parseLong(System.getProperty("rhq.storage.maintenance-job.interval",
            Long.toString(300000))); // 5 minutes
    }

    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("Running cluster maintenance");
            StorageClusterMaintenanceManagerLocal maintenanceManager = LookupUtil.getStorageClusterMaintenanceManager();
            maintenanceManager.scheduleMaintenance();
        } catch (Exception e) {
            log.error("There was an unexpected error while performing cluster maintenance", e);
        } finally {
            try {
                SchedulerLocal scheduler = LookupUtil.getSchedulerBean();
                scheduler.scheduleJob(getTrigger());
            } catch (SchedulerException e) {
                // TODO What should we do in the event of an error?
                // It is necessary but not sufficient to simply log the error. Should we
                // try rescheduling again? Should we log a message that the user should
                // restart the server? And what if there are multiple servers?
                log.error("There was an error rescheduling the job", e);
            }
        }
    }

}
