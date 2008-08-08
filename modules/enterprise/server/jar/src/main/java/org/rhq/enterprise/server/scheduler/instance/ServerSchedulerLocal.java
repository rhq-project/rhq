package org.rhq.enterprise.server.scheduler.instance;

import javax.ejb.Local;

import org.quartz.Job;
import org.quartz.SchedulerException;

@Local
public interface ServerSchedulerLocal {

    void scheduleRepeatingJob(String jobName, String groupName, Class<? extends Job> jobClass,
        boolean rescheduleIfExists, boolean isVolatile, long initialDelay, long interval) throws SchedulerException;

    void scheduleSimpleRepeatingJob(Class<? extends Job> jobClass, boolean rescheduleIfExists, boolean isVolatile,
        long initialDelay, long interval) throws SchedulerException;

}
