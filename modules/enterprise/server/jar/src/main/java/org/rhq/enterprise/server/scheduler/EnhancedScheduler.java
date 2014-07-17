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
package org.rhq.enterprise.server.scheduler;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

/**
 * An enhanced scheduler interface that provides the normal scheduler API with some additional convenience methods.
 *
 * @author John Mazzitelli
 */
public interface EnhancedScheduler extends Scheduler {
    /**
     * Schedules the job such that it triggers immediately (following the initial delay time) and then repeats every
     * <code>interval</code> seconds.
     *
     * <p>This is a convienence method to schedule jobs that need to run periodically. It schedules jobs with the given
     * <code>groupName</code> and <code>name</code> and a given set of job data to be passed to the job when it is
     * executed. The schedule repeats without end.</p>
     *
     * <p>The schedule will may or may not be <code>isVolatile</code> - that is, if it is not volatile, it will be
     * persisted and rescheduled when the JON Server starts back up again.</p>
     *
     * <p>If this method is called again with the same <code>groupName</code> and <code>name</code> <b>and</b> <code>
     * rescheduleIfExists</code> is <code>false</code>, this method will not schedule it again - it will leave the old
     * schedule. If you want to remove the old schedule, pass in <code>true</code> for <code>rescheduleIfExists</code>
     * or use {@link SchedulerServiceMBean#deleteJob(String, String)}.
     *
     * @param  name               the name of the job to be scheduled. This is also the group name.
     * @param  groupName          if you want to group jobs together, give them the same group name; otherwise, can be
     *                            the same as <code>name</code>
     * @param  rescheduleIfExists if <code>true</code>, and the job is already scheduled, this new schedule will replace
     *                            it. if <code>false</code>, any existing job will remain and this method simply does
     *                            nothing and returns immediately
     * @param  jobData            a map of serializable data to be passed to the job each time the job is executed
     * @param  jobClass           the class of the job that will be executed when the trigger fires
     * @param  isVolatile         if <code>false</code>, the job will be persisted to the database; if <code>
     *                            true</code>, when the scheduler is shutdown, the job's schedule is lost
     * @param  initialDelay       number of milliseconds to wait before triggering the job for the first time
     * @param  interval           number of milliseconds between each triggering of the job
     *
     * @throws SchedulerException
     *
     * @see    SchedulerServiceMBean#scheduleJob(org.quartz.JobDetail, org.quartz.Trigger)
     */
    void scheduleRepeatingJob(String name, String groupName, JobDataMap jobData, Class<? extends Job> jobClass,
        boolean rescheduleIfExists, boolean isVolatile, long initialDelay, long interval) throws SchedulerException;

    /**
     * Schedules the job such that it triggers according to the given cron schedule.
     *
     * <p>This is a convienence method to schedule jobs that need to run periodically. It schedules jobs with the given
     * <code>groupName</code> and <code>name</code> and a given set of job data to be passed to the job when it is
     * executed. The schedule repeats without end.</p>
     *
     * <p>The schedule will may or may not be <code>isVolatile</code> - that is, if it is not volatile, it will be
     * persisted and rescheduled when the JON Server starts back up again.</p>
     *
     * <p>If this method is called again with the same <code>groupName</code> and <code>name</code> <b>and</b> <code>
     * rescheduleIfExists</code> is <code>false</code>, this method will not schedule it again - it will leave the old
     * schedule. If you want to remove the old schedule, pass in <code>true</code> for <code>rescheduleIfExists</code>
     * or use {@link SchedulerServiceMBean#deleteJob(String, String)}.
     *
     * @param  name               the name of the job to be scheduled. This is also the group name.
     * @param  groupName          if you want to group jobs together, give them the same group name; otherwise, can be
     *                            the same as <code>name</code>
     * @param  rescheduleIfExists if <code>true</code>, and the job is already scheduled, this new schedule will replace
     *                            it. if <code>false</code>, any existing job will remain and this method simply does
     *                            nothing and returns immediately
     * @param  jobData            a map of serializable data to be passed to the job each time the job is executed
     * @param  jobClass           the class of the job that will be executed when the trigger fires
     * @param  isVolatile         if <code>false</code>, the job will be persisted to the database; if <code>
     *                            true</code>, when the scheduler is shutdown, the job's schedule is lost
     * @param  cronString         the actual schedule for when the job is triggered. See the Quartz documentation on
     *                            valid cron syntax.
     *
     * @throws SchedulerException
     *
     * @see    SchedulerServiceMBean#scheduleJob(org.quartz.JobDetail, org.quartz.Trigger)
     */
    public void scheduleCronJob(String name, String groupName, JobDataMap jobData, Class<? extends Job> jobClass,
        boolean rescheduleIfExists, boolean isVolatile, String cronString) throws SchedulerException;

    /**
     * Schedules the job such that it triggers immediately (following the initial delay time) and then repeats every
     * <code>interval</code> seconds.
     *
     * <p>This is a convienence method to schedule simple jobs that need to run periodically. It schedules simple jobs -
     * there is no {@link JobDataMap} associated with the job and the schedule repeats without end.</p>
     *
     * <p>This method delegates to
     * {@link #scheduleRepeatingJob(String, String, JobDataMap, Class, boolean, boolean, long, long)} where the <code>
     * name</code> is the name of the given class (<code>jobClass.getName()</code>) and <code>null</code> is passed in
     * as the job data map.</p>
     *
     * @param  jobClass           the class of the job that will be executed when the trigger fires
     * @param  rescheduleIfExists if <code>true</code>, and the job is already scheduled, this new schedule will replace
     *                            it. if <code>false</code>, any existing job will remain and this method simply does
     *                            nothing and returns immediately
     * @param  isVolatile         if <code>false</code>, the job will be persisted to the database; if <code>
     *                            true</code>, when the scheduler is shutdown, the job's schedule is lost
     * @param  initialDelay       number of milliseconds to wait before triggering the job for the first time
     * @param  interval           number of milliseconds between each triggering of the job
     *
     * @throws SchedulerException
     *
     * @see    SchedulerServiceMBean#scheduleJob(org.quartz.JobDetail, org.quartz.Trigger)
     */
    void scheduleSimpleRepeatingJob(Class<? extends Job> jobClass, boolean rescheduleIfExists, boolean isVolatile,
        long initialDelay, long interval) throws SchedulerException;

    /**
     * Schedules the job such that it triggers according to the given cron schedule.
     *
     * <p>This is a convienence method to schedule simple cron jobs that need to run periodically. It schedules simple
     * jobs - there is no {@link JobDataMap} associated with the job and the schedule repeats without end.</p>
     *
     * <p>This method delegates to {@link #scheduleCronJob(String, String, JobDataMap, Class, boolean, boolean, String)}
     * where the <code>name</code> is the name of the given class (<code>jobClass.getName()</code>) and <code>
     * null</code> is passed in as the job data map.</p>
     *
     * @param  jobClass           the class of the job that will be executed when the trigger fires
     * @param  rescheduleIfExists if <code>true</code>, and the job is already scheduled, this new schedule will replace
     *                            it. if <code>false</code>, any existing job will remain and this method simply does
     *                            nothing and returns immediately
     * @param  isVolatile         if <code>false</code>, the job will be persisted to the database; if <code>
     *                            true</code>, when the scheduler is shutdown, the job's schedule is lost
     * @param  cronString         the actual schedule for when the job is triggered. See the Quartz documentation on
     *                            valid cron syntax.
     *
     * @throws SchedulerException
     *
     * @see    SchedulerServiceMBean#scheduleJob(org.quartz.JobDetail, org.quartz.Trigger)
     */
    public void scheduleSimpleCronJob(Class<? extends Job> jobClass, boolean rescheduleIfExists, boolean isVolatile,
        String cronString) throws SchedulerException;

    /**
     * Schedules the job with the given trigger.  If the trigger is null then the durable job is simply created,
     * if necessary, and will await future triggers.
     *
     * <p>This is a convenience method for adding a trigger to a job.  There is no job-level {@link JobDataMap} but
     * each trigger may contain a trigger-specific {@link JobDataMap}.</p>
     *
     * <p>This method delegates to {@link #addJob(org.quartz.JobDetail, boolean)} where the <code>
     * name</code> is the name of the given class (<code>jobClass.getName()</code>) and <code>replace</code> is
     * false. If <code>trigger</code> is not null, then
     * {@link Scheduler#scheduleJob(org.quartz.JobDetail, org.quartz.Trigger)} will be called to schedule the job.</p>
     *
     * @param  jobClass           the class of the job that will be executed when the trigger fires
     * @param  trigger            an optional trigger for firing the job
     *
     * @throws SchedulerException
     *
     * @see    SchedulerServiceMBean#addJob(org.quartz.JobDetail, boolean)
     * @param  isVolatile         if <code>false</code>, the job will be persisted to the database; if <code>
     *                            true</code>, when the scheduler is shutdown, the job's schedule is lost
     * @see    SchedulerServiceMBean#scheduleJob(org.quartz.JobDetail, org.quartz.Trigger)
     */
    void scheduleTriggeredJob(Class<? extends Job> jobClass, String group, boolean isVolatile, Trigger trigger)
        throws SchedulerException;

}