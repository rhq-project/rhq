/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Calendar;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.SchedulerMetaData;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.UnableToInterruptJobException;
import org.quartz.spi.JobFactory;

/**
 * Wraps a Quartz scheduler and provides some enhanced scheduler methods.
 */
public class EnhancedSchedulerImpl implements EnhancedScheduler {
    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1L;
    private final Log log = LogFactory.getLog(EnhancedSchedulerImpl.class.getName());
    private final Scheduler scheduler;

    public EnhancedSchedulerImpl(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * @see EnhancedScheduler#scheduleRepeatingJob(String, String, JobDataMap, Class, boolean, boolean, long, long)
     */
    public void scheduleRepeatingJob(String name, String groupName, JobDataMap jobData, Class<? extends Job> jobClass,
        boolean rescheduleIfExists, boolean isVolatile, long initialDelay, long interval) throws SchedulerException {

        // See if the job is already scheduled and if so,
        // either remove it so we can reschedule it or keep it (based on rescheduleIfExists).
        JobDetail existingJob = getExistingJob(name, groupName, rescheduleIfExists);
        if (existingJob != null) {
            if (rescheduleIfExists) {
                log.debug("Looks like repeating job [" + name + ':' + groupName
                    + "] is already scheduled - removing it so it can be rescheduled...");
                if (!this.scheduler.deleteJob(name, groupName)) {
                    throw new SchedulerException("Failed to delete repeating job [" + existingJob
                        + "] in order to reschedule it.");
                }
            } else {
                log.debug("Looks like repeating job [" + name + ':' + groupName
                    + "] is already scheduled - leaving the original job as-is.");
                return;
            }
        }

        JobDetail job = createJobDetail(name, groupName, jobClass, isVolatile, jobData);
        SimpleTrigger trigger = createSimpleTrigger(name, groupName, isVolatile, initialDelay, interval);
        Date next = this.scheduler.scheduleJob(job, trigger);

        log.info("Scheduled job [" + name + ':' + groupName + "] to fire next at [" + next + "] and repeat every ["
            + interval + "] milliseconds");

        return;
    }

    /**
     * @see EnhancedScheduler#scheduleCronJob(String, String, JobDataMap, Class, boolean, boolean, String)
     */
    public void scheduleCronJob(String name, String groupName, JobDataMap jobData, Class<? extends Job> jobClass,
        boolean rescheduleIfExists, boolean isVolatile, String cronString) throws SchedulerException {

        // See if the job is already scheduled and if so,
        // either remove it so we can reschedule it or keep it (based on rescheduleIfExists).
        JobDetail existingJob = getExistingJob(name, groupName, rescheduleIfExists);
        if (existingJob != null) {
            if (rescheduleIfExists) {
                log.debug("Looks like cron job [" + name + ':' + groupName
                    + "] is already scheduled - removing it so it can be rescheduled...");
                deleteJob(name, groupName, existingJob);
            } else {
                log.debug("Looks like cron job [" + name + ':' + groupName
                    + "] is already scheduled - leaving the original job as-is.");
                return;
            }
        }

        JobDetail job = createJobDetail(name, groupName, jobClass, isVolatile, jobData);
        CronTrigger trigger = createCronTrigger(name, groupName, isVolatile, cronString);
        Date next = this.scheduler.scheduleJob(job, trigger);

        log.info("Scheduled cron job [" + name + ':' + groupName + "] to fire next at [" + next
            + "] with the cron string [" + cronString + "].");

        return;
    }

    /**
     * @see EnhancedScheduler#scheduleSimpleRepeatingJob(Class, boolean, boolean, long, long)
     */
    public void scheduleSimpleRepeatingJob(Class<? extends Job> jobClass, boolean rescheduleIfExists,
        boolean isVolatile, long initialDelay, long interval) throws SchedulerException {

        scheduleRepeatingJob(jobClass.getName(), jobClass.getName(), null, jobClass, rescheduleIfExists, isVolatile,
            initialDelay, interval);
        return;
    }

    /**
     * @see EnhancedScheduler#scheduleSimpleCronJob(Class, boolean, boolean, String)
     */
    public void scheduleSimpleCronJob(Class<? extends Job> jobClass, boolean rescheduleIfExists, boolean isVolatile,
        String cronString) throws SchedulerException {

        scheduleCronJob(jobClass.getName(), jobClass.getName(), null, jobClass, rescheduleIfExists, isVolatile,
            cronString);
        return;
    }

    //---------------------------------------------------------------------
    //-- Scheduler interface methods
    //---------------------------------------------------------------------

    public String getSchedulerName() throws SchedulerException {
        return this.scheduler.getSchedulerName();
    }

    public String getSchedulerInstanceId() throws SchedulerException {
        return this.scheduler.getSchedulerInstanceId();
    }

    public SchedulerContext getContext() throws SchedulerException {
        return this.scheduler.getContext();
    }

    public SchedulerMetaData getMetaData() throws SchedulerException {
        return this.scheduler.getMetaData();
    }

    public void start() throws SchedulerException {
        this.scheduler.start();
    }

    public void pause() throws SchedulerException {
        this.scheduler.pause();
    }

    public boolean isPaused() throws SchedulerException {
        return this.scheduler.isPaused();
    }

    public void shutdown() throws SchedulerException {
        this.scheduler.shutdown();
    }

    public void shutdown(boolean waitForJobsToComplete) throws SchedulerException {
        this.scheduler.shutdown(waitForJobsToComplete);
    }

    public boolean isShutdown() throws SchedulerException {
        return this.scheduler.isShutdown();
    }

    public List getCurrentlyExecutingJobs() throws SchedulerException {
        return this.scheduler.getCurrentlyExecutingJobs();
    }

    public Date scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
        if (log.isDebugEnabled()) {
            log.debug("Job details: " + jobDetail);
        }

        return this.scheduler.scheduleJob(jobDetail, trigger);
    }

    public Date scheduleJob(Trigger trigger) throws SchedulerException {
        return this.scheduler.scheduleJob(trigger);
    }

    public void addJob(JobDetail jobDetail, boolean replace) throws SchedulerException {
        this.scheduler.addJob(jobDetail, replace);
    }

    public boolean deleteJob(String jobName, String groupName) throws SchedulerException {
        return this.scheduler.deleteJob(jobName, groupName);
    }

    public boolean unscheduleJob(String triggerName, String groupName) throws SchedulerException {
        return this.scheduler.unscheduleJob(triggerName, groupName);
    }

    public void triggerJob(String jobName, String groupName) throws SchedulerException {
        this.scheduler.triggerJob(jobName, groupName);
    }

    public void triggerJobWithVolatileTrigger(String jobName, String groupName) throws SchedulerException {
        this.scheduler.triggerJobWithVolatileTrigger(jobName, groupName);
    }

    public void pauseTrigger(String triggerName, String groupName) throws SchedulerException {
        this.scheduler.pauseTrigger(triggerName, groupName);
    }

    public void pauseTriggerGroup(String groupName) throws SchedulerException {
        this.scheduler.pauseTriggerGroup(groupName);
    }

    public void pauseJob(String jobName, String groupName) throws SchedulerException {
        this.scheduler.pauseJob(jobName, groupName);
    }

    public void pauseJobGroup(String groupName) throws SchedulerException {
        this.scheduler.pauseJobGroup(groupName);
    }

    public void resumeTrigger(String triggerName, String groupName) throws SchedulerException {
        this.scheduler.resumeTrigger(triggerName, groupName);
    }

    public void resumeTriggerGroup(String groupName) throws SchedulerException {
        this.scheduler.resumeTriggerGroup(groupName);
    }

    public void resumeJob(String jobName, String groupName) throws SchedulerException {
        this.scheduler.resumeJob(jobName, groupName);
    }

    public void resumeJobGroup(String groupName) throws SchedulerException {
        this.scheduler.resumeJobGroup(groupName);
    }

    public String[] getJobGroupNames() throws SchedulerException {
        return this.scheduler.getJobGroupNames();
    }

    public String[] getJobNames(String groupName) throws SchedulerException {
        return this.scheduler.getJobNames(groupName);
    }

    public Trigger[] getTriggersOfJob(String jobName, String groupName) throws SchedulerException {
        return this.scheduler.getTriggersOfJob(jobName, groupName);
    }

    public String[] getTriggerGroupNames() throws SchedulerException {
        return this.scheduler.getTriggerGroupNames();
    }

    public String[] getTriggerNames(String groupName) throws SchedulerException {
        return this.scheduler.getTriggerNames(groupName);
    }

    public JobDetail getJobDetail(String jobName, String jobGroup) throws SchedulerException {
        return this.scheduler.getJobDetail(jobName, jobGroup);
    }

    public Trigger getTrigger(String triggerName, String triggerGroup) throws SchedulerException {
        return this.scheduler.getTrigger(triggerName, triggerGroup);
    }

    public boolean deleteCalendar(String calName) throws SchedulerException {
        return this.scheduler.deleteCalendar(calName);
    }

    public Calendar getCalendar(String calName) throws SchedulerException {
        return this.scheduler.getCalendar(calName);
    }

    public String[] getCalendarNames() throws SchedulerException {
        return this.scheduler.getCalendarNames();
    }

    public void addGlobalJobListener(JobListener jobListener) throws SchedulerException {
        this.scheduler.addGlobalJobListener(jobListener);
    }

    public void addJobListener(JobListener jobListener) throws SchedulerException {
        this.scheduler.addJobListener(jobListener);
    }

    public boolean removeGlobalJobListener(JobListener jobListener) throws SchedulerException {
        return this.scheduler.removeGlobalJobListener(jobListener);
    }

    public boolean removeJobListener(String name) throws SchedulerException {
        return this.scheduler.removeJobListener(name);
    }

    public List getGlobalJobListeners() throws SchedulerException {
        return this.scheduler.getGlobalJobListeners();
    }

    public Set getJobListenerNames() throws SchedulerException {
        return this.scheduler.getJobListenerNames();
    }

    public JobListener getJobListener(String name) throws SchedulerException {
        return this.scheduler.getJobListener(name);
    }

    public void addGlobalTriggerListener(TriggerListener triggerListener) throws SchedulerException {
        this.scheduler.addGlobalTriggerListener(triggerListener);
    }

    public void addTriggerListener(TriggerListener triggerListener) throws SchedulerException {
        this.scheduler.addTriggerListener(triggerListener);
    }

    public boolean removeGlobalTriggerListener(TriggerListener triggerListener) throws SchedulerException {
        return this.scheduler.removeGlobalTriggerListener(triggerListener);
    }

    public boolean removeTriggerListener(String name) throws SchedulerException {
        return this.scheduler.removeTriggerListener(name);
    }

    public List getGlobalTriggerListeners() throws SchedulerException {
        return this.scheduler.getGlobalTriggerListeners();
    }

    public Set getTriggerListenerNames() throws SchedulerException {
        return this.scheduler.getTriggerListenerNames();
    }

    public TriggerListener getTriggerListener(String name) throws SchedulerException {
        return this.scheduler.getTriggerListener(name);
    }

    public void addSchedulerListener(SchedulerListener schedulerListener) throws SchedulerException {
        this.scheduler.addSchedulerListener(schedulerListener);
    }

    public boolean removeSchedulerListener(SchedulerListener schedulerListener) throws SchedulerException {
        return this.scheduler.removeSchedulerListener(schedulerListener);
    }

    public List getSchedulerListeners() throws SchedulerException {
        return this.scheduler.getSchedulerListeners();
    }

    // Quartz methods that are new in 1.5.1 that were not in 1.0.7

    public void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers)
        throws SchedulerException {
        this.scheduler.addCalendar(calName, calendar, replace, updateTriggers);
    }

    public Set getPausedTriggerGroups() throws SchedulerException {
        return this.scheduler.getPausedTriggerGroups();
    }

    public int getTriggerState(String triggerName, String triggerGroup) throws SchedulerException {
        return this.scheduler.getTriggerState(triggerName, triggerGroup);
    }

    public boolean interrupt(String jobName, String groupName) throws UnableToInterruptJobException {
        try {
            return this.scheduler.interrupt(jobName, groupName);
        } catch (SchedulerException e) {
            throw new UnableToInterruptJobException(e);
        }
    }

    public boolean isInStandbyMode() throws SchedulerException {
        return this.scheduler.isInStandbyMode();
    }

    public void pauseAll() throws SchedulerException {
        this.scheduler.pauseAll();
    }

    public Date rescheduleJob(String triggerName, String groupName, Trigger newTrigger) throws SchedulerException {
        return this.scheduler.rescheduleJob(triggerName, groupName, newTrigger);
    }

    public void resumeAll() throws SchedulerException {
        this.scheduler.resumeAll();
    }

    public void setJobFactory(JobFactory factory) throws SchedulerException {
        this.scheduler.setJobFactory(factory);
    }

    public void standby() throws SchedulerException {
        this.scheduler.standby();
    }

    public void triggerJob(String jobName, String groupName, JobDataMap data) throws SchedulerException {
        this.scheduler.triggerJob(jobName, groupName, data);
    }

    public void triggerJobWithVolatileTrigger(String jobName, String groupName, JobDataMap data)
        throws SchedulerException {
        this.scheduler.triggerJob(jobName, groupName, data);
    }

    /* new methods in quartz 1.6.5 below this line */
    public JobListener getGlobalJobListener(String jobName) throws SchedulerException {
        return this.scheduler.getGlobalJobListener(jobName);
    }

    public TriggerListener getGlobalTriggerListener(String triggerName) throws SchedulerException {
        return this.scheduler.getGlobalTriggerListener(triggerName);
    }

    public boolean isStarted() throws SchedulerException {
        return this.scheduler.isStarted();
    }

    public boolean removeGlobalJobListener(String jobName) throws SchedulerException {
        return this.scheduler.removeGlobalJobListener(jobName);
    }

    public boolean removeGlobalTriggerListener(String triggerName) throws SchedulerException {
        return this.scheduler.removeGlobalTriggerListener(triggerName);
    }

    public void startDelayed(int delay) throws SchedulerException {
        this.scheduler.startDelayed(delay);
    }

    private JobDetail getExistingJob(String name, String groupName, boolean rescheduleIfExists) {
        try {
            return this.scheduler.getJobDetail(name, groupName);
        } catch (SchedulerException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ClassNotFoundException) {
                // This probably means the job class was not found in the classpath. Try to delete the invalid job.
                try {
                    deleteJob(name, groupName, null);
                } catch (SchedulerException e1) {
                    log.error("Failed to delete job [" + name + ":" + groupName + "] with invalid job class (as per "
                        + cause + ")", e1);
                }
            } else {
                log.error("Error while attempting to lookup job [" + name + ":" + groupName + "].", e);
            }
            // Fall through and hope we get lucky and are still able to schedule the job.
            return null;
        }
    }

    private void deleteJob(String name, String groupName, JobDetail job) throws SchedulerException {
        if (!this.scheduler.deleteJob(name, groupName)) {
            String jobString = (job != null) ? job.toString() : (name + ":" + groupName);
            throw new SchedulerException("Failed to delete job [" + jobString + "].");
        }
    }

    private JobDetail createJobDetail(String name, String groupName, Class<? extends Job> jobClass, boolean isVolatile,
        JobDataMap jobData) {
        JobDetail job = new JobDetail(name, groupName, jobClass, isVolatile, false, false);
        job.setJobDataMap(jobData);
        return job;
    }

    private SimpleTrigger createSimpleTrigger(String name, String groupName, boolean isVolatile, long initialDelay,
        long interval) {
        Date start = new Date(System.currentTimeMillis() + initialDelay);
        SimpleTrigger trigger = new SimpleTrigger(name, groupName, start, null, SimpleTrigger.REPEAT_INDEFINITELY,
            interval);
        trigger.setVolatility(isVolatile);
        return trigger;
    }

    private CronTrigger createCronTrigger(String name, String groupName, boolean isVolatile, String cronString)
        throws SchedulerException {
        CronTrigger trigger;
        try {
            trigger = new CronTrigger(name, groupName, name, groupName, cronString);
        } catch (ParseException e) {
            throw new SchedulerException(e);
        }
        trigger.setVolatility(isVolatile);
        return trigger;
    }

    @Override
    public void scheduleTriggeredJob(Class<? extends Job> jobClass, String group, boolean isVolatile, Trigger trigger)
        throws SchedulerException {

        String name = jobClass.getName();

        // See if the job exists, if not, add it.
        JobDetail existingJob = getExistingJob(name, group, false);
        if (existingJob == null) {
            JobDetail job = new JobDetail(name, group, jobClass, isVolatile, true, false);
            scheduler.scheduleJob(job, trigger);
        } else {
            scheduleJob(trigger);
        }
    }
}