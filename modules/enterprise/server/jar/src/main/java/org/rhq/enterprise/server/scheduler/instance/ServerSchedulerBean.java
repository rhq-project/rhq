package org.rhq.enterprise.server.scheduler.instance;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.quartz.Job;
import org.quartz.SchedulerException;

import org.rhq.enterprise.server.cluster.instance.ServerManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;

@Stateless
public class ServerSchedulerBean implements ServerSchedulerLocal {

    @EJB
    ServerManagerLocal serverManager;

    @EJB
    SchedulerLocal scheduler;

    public void scheduleRepeatingJob(String jobName, String groupName, Class<? extends Job> jobClass,
        boolean rescheduleIfExists, boolean isVolatile, long initialDelay, long interval) throws SchedulerException {
        scheduler.scheduleRepeatingJob(jobName + "_" + serverManager.getIdentity(), groupName, null, jobClass,
            rescheduleIfExists, isVolatile, initialDelay, interval);
    }

    public void scheduleSimpleRepeatingJob(Class<? extends Job> jobClass, boolean rescheduleIfExists,
        boolean isVolatile, long initialDelay, long interval) throws SchedulerException {
        scheduler.scheduleRepeatingJob(jobClass.getName() + "_" + serverManager.getIdentity(), jobClass.getName(),
            null, jobClass, rescheduleIfExists, isVolatile, initialDelay, interval);
    }
}
