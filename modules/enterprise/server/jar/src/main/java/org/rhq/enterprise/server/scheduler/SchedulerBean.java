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

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.ejb.Stateless;
import javax.management.MBeanServerInvocationHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Calendar;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.UnableToInterruptJobException;
import org.quartz.spi.JobFactory;

/**
 * The Scheduler session bean is a proxy to the Quartz scheduler MBean that is used for scheduling jobs to be executed
 * within the JON Server. Use this to provide transactional access to Quartz.
 */
@Stateless
public class SchedulerBean implements SchedulerLocal {
    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1L;
    private final Log log = LogFactory.getLog(SchedulerBean.class.getName());

    /**
     * Returns a reference to a proxy to the scheduler service MBean, which itself is a proxy that delegates to the
     * Quartz scheduler.
     *
     * @return proxy to the scheduler service MBean
     *
     * @throws SchedulerException if failed to find the MBean
     */
    private SchedulerServiceMBean getSchedulerService() throws SchedulerException {
        try {
            return (SchedulerServiceMBean) MBeanServerInvocationHandler.newProxyInstance(
                ManagementFactory.getPlatformMBeanServer(), SCHEDULER_MBEAN_NAME, SchedulerServiceMBean.class, false);
        } catch (Exception e) {
            throw new SchedulerException("Failed to get a proxy to the scheduler service MBean", e);
        }
    }

    /**
     * @see SchedulerLocal#scheduleRepeatingJob(String, String, JobDataMap, Class, boolean, boolean, long, long)
     */
    public void scheduleRepeatingJob(String name, String groupName, JobDataMap jobData, Class<? extends Job> jobClass,
        boolean rescheduleIfExists, boolean isVolatile, long initialDelay, long interval) throws SchedulerException {

        new EnhancedSchedulerImpl(getSchedulerService()).scheduleRepeatingJob(name, groupName, jobData, jobClass,
            rescheduleIfExists, isVolatile, initialDelay, interval);
        return;
    }

    /**
     * @see SchedulerLocal#scheduleCronJob(String, String, JobDataMap, Class, boolean, boolean, String)
     */
    public void scheduleCronJob(String name, String groupName, JobDataMap jobData, Class<? extends Job> jobClass,
        boolean rescheduleIfExists, boolean isVolatile, String cronString) throws SchedulerException {

        new EnhancedSchedulerImpl(getSchedulerService()).scheduleCronJob(name, groupName, jobData, jobClass,
            rescheduleIfExists, isVolatile, cronString);
        return;
    }

    /**
     * @see SchedulerLocal#scheduleSimpleRepeatingJob(Class, boolean, boolean, long, long)
     */
    public void scheduleSimpleRepeatingJob(Class<? extends Job> jobClass, boolean rescheduleIfExists,
        boolean isVolatile, long initialDelay, long interval) throws SchedulerException {

        new EnhancedSchedulerImpl(getSchedulerService()).scheduleSimpleRepeatingJob(jobClass, rescheduleIfExists,
            isVolatile, initialDelay, interval);
        return;
    }

    /**
     * @see SchedulerLocal#scheduleSimpleCronJob(Class, boolean, boolean, String)
     */
    public void scheduleSimpleCronJob(Class<? extends Job> jobClass, boolean rescheduleIfExists, boolean isVolatile,
        String cronString) throws SchedulerException {

        new EnhancedSchedulerImpl(getSchedulerService()).scheduleSimpleCronJob(jobClass, rescheduleIfExists,
            isVolatile, cronString);
        return;
    }

    public Properties getQuartzProperties() {
        try {
            return getSchedulerService().getQuartzProperties();
        } catch (SchedulerException e) {
            log.error("Failed to get the Quartz properties", e);
            return null;
        }
    }

    /**
     * Delegates to the Scheduler Service MBean in order to set the properties for Quartz and reinitialize the Quartz
     * scheduler factory.
     *
     * @see SchedulerServiceMBean#setQuartzProperties(Properties)
     */
    public void setQuartzProperties(final Properties quartzProps) throws SchedulerException {
        getSchedulerService().setQuartzProperties(quartzProps);
    }

    public Integer getDefaultOperationTimeout() {
        try {
            return getSchedulerService().getDefaultOperationTimeout();
        } catch (SchedulerException e) {
            log.error("Failed to get the default operation timeout", e);
            return null;
        }
    }

    public void initQuartzScheduler() throws SchedulerException {
        getSchedulerService().initQuartzScheduler();
    }

    public void startQuartzScheduler() throws SchedulerException {
        getSchedulerService().startQuartzScheduler();
    }

    //---------------------------------------------------------------------
    //-- Scheduler interface methods
    //---------------------------------------------------------------------

    public String getSchedulerName() throws SchedulerException {
        return getSchedulerService().getSchedulerName();
    }

    public String getSchedulerInstanceId() throws SchedulerException {
        return getSchedulerService().getSchedulerInstanceId();
    }

    public SchedulerContext getContext() throws SchedulerException {
        return getSchedulerService().getContext();
    }

    public SchedulerMetaData getMetaData() throws SchedulerException {
        return getSchedulerService().getMetaData();
    }

    public void start() throws SchedulerException {
        getSchedulerService().start();
    }

    public void pause() throws SchedulerException {
        getSchedulerService().pause();
    }

    public boolean isPaused() throws SchedulerException {
        return getSchedulerService().isPaused();
    }

    public void shutdown() throws SchedulerException {
        getSchedulerService().shutdown();
    }

    public void shutdown(boolean waitForJobsToComplete) throws SchedulerException {
        getSchedulerService().shutdown(waitForJobsToComplete);
    }

    public boolean isShutdown() throws SchedulerException {
        return getSchedulerService().isShutdown();
    }

    public List getCurrentlyExecutingJobs() throws SchedulerException {
        return getSchedulerService().getCurrentlyExecutingJobs();
    }

    public Date scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
        if (log.isDebugEnabled()) {
            log.debug("Job details: " + jobDetail);
        }

        return getSchedulerService().scheduleJob(jobDetail, trigger);
    }

    public Date scheduleJob(Trigger trigger) throws SchedulerException {
        return getSchedulerService().scheduleJob(trigger);
    }

    public void addJob(JobDetail jobDetail, boolean replace) throws SchedulerException {
        getSchedulerService().addJob(jobDetail, replace);
    }

    public boolean deleteJob(String jobName, String groupName) throws SchedulerException {
        return getSchedulerService().deleteJob(jobName, groupName);
    }

    public boolean unscheduleJob(String triggerName, String groupName) throws SchedulerException {
        return getSchedulerService().unscheduleJob(triggerName, groupName);
    }

    public void triggerJob(String jobName, String groupName) throws SchedulerException {
        getSchedulerService().triggerJob(jobName, groupName);
    }

    public void triggerJobWithVolatileTrigger(String jobName, String groupName) throws SchedulerException {
        getSchedulerService().triggerJobWithVolatileTrigger(jobName, groupName);
    }

    public void pauseTrigger(String triggerName, String groupName) throws SchedulerException {
        getSchedulerService().pauseTrigger(triggerName, groupName);
    }

    public void pauseTriggerGroup(String groupName) throws SchedulerException {
        getSchedulerService().pauseTriggerGroup(groupName);
    }

    public void pauseJob(String jobName, String groupName) throws SchedulerException {
        getSchedulerService().pauseJob(jobName, groupName);
    }

    public void pauseJobGroup(String groupName) throws SchedulerException {
        getSchedulerService().pauseJobGroup(groupName);
    }

    public void resumeTrigger(String triggerName, String groupName) throws SchedulerException {
        getSchedulerService().resumeTrigger(triggerName, groupName);
    }

    public void resumeTriggerGroup(String groupName) throws SchedulerException {
        getSchedulerService().resumeTriggerGroup(groupName);
    }

    public void resumeJob(String jobName, String groupName) throws SchedulerException {
        getSchedulerService().resumeJob(jobName, groupName);
    }

    public void resumeJobGroup(String groupName) throws SchedulerException {
        getSchedulerService().resumeJobGroup(groupName);
    }

    public String[] getJobGroupNames() throws SchedulerException {
        return getSchedulerService().getJobGroupNames();
    }

    public String[] getJobNames(String groupName) throws SchedulerException {
        return getSchedulerService().getJobNames(groupName);
    }

    public Trigger[] getTriggersOfJob(String jobName, String groupName) throws SchedulerException {
        return getSchedulerService().getTriggersOfJob(jobName, groupName);
    }

    public String[] getTriggerGroupNames() throws SchedulerException {
        return getSchedulerService().getTriggerGroupNames();
    }

    public String[] getTriggerNames(String groupName) throws SchedulerException {
        return getSchedulerService().getTriggerNames(groupName);
    }

    public JobDetail getJobDetail(String jobName, String jobGroup) throws SchedulerException {
        return getSchedulerService().getJobDetail(jobName, jobGroup);
    }

    public Trigger getTrigger(String triggerName, String triggerGroup) throws SchedulerException {
        return getSchedulerService().getTrigger(triggerName, triggerGroup);
    }

    public boolean deleteCalendar(String calName) throws SchedulerException {
        return getSchedulerService().deleteCalendar(calName);
    }

    public Calendar getCalendar(String calName) throws SchedulerException {
        return getSchedulerService().getCalendar(calName);
    }

    public String[] getCalendarNames() throws SchedulerException {
        return getSchedulerService().getCalendarNames();
    }

    public void addGlobalJobListener(JobListener jobListener) throws SchedulerException {
        getSchedulerService().addGlobalJobListener(jobListener);
    }

    public void addJobListener(JobListener jobListener) throws SchedulerException {
        getSchedulerService().addJobListener(jobListener);
    }

    public boolean removeGlobalJobListener(JobListener jobListener) throws SchedulerException {
        return getSchedulerService().removeGlobalJobListener(jobListener);
    }

    public boolean removeJobListener(String name) throws SchedulerException {
        return getSchedulerService().removeJobListener(name);
    }

    public List getGlobalJobListeners() throws SchedulerException {
        return getSchedulerService().getGlobalJobListeners();
    }

    public Set getJobListenerNames() throws SchedulerException {
        return getSchedulerService().getJobListenerNames();
    }

    public JobListener getJobListener(String name) throws SchedulerException {
        return getSchedulerService().getJobListener(name);
    }

    public void addGlobalTriggerListener(TriggerListener triggerListener) throws SchedulerException {
        getSchedulerService().addGlobalTriggerListener(triggerListener);
    }

    public void addTriggerListener(TriggerListener triggerListener) throws SchedulerException {
        getSchedulerService().addTriggerListener(triggerListener);
    }

    public boolean removeGlobalTriggerListener(TriggerListener triggerListener) throws SchedulerException {
        return getSchedulerService().removeGlobalTriggerListener(triggerListener);
    }

    public boolean removeTriggerListener(String name) throws SchedulerException {
        return getSchedulerService().removeTriggerListener(name);
    }

    public List getGlobalTriggerListeners() throws SchedulerException {
        return getSchedulerService().getGlobalTriggerListeners();
    }

    public Set getTriggerListenerNames() throws SchedulerException {
        return getSchedulerService().getTriggerListenerNames();
    }

    public TriggerListener getTriggerListener(String name) throws SchedulerException {
        return getSchedulerService().getTriggerListener(name);
    }

    public void addSchedulerListener(SchedulerListener schedulerListener) throws SchedulerException {
        getSchedulerService().addSchedulerListener(schedulerListener);
    }

    public boolean removeSchedulerListener(SchedulerListener schedulerListener) throws SchedulerException {
        return getSchedulerService().removeSchedulerListener(schedulerListener);
    }

    public List getSchedulerListeners() throws SchedulerException {
        return getSchedulerService().getSchedulerListeners();
    }

    // Quartz methods that are new in 1.5.1 that were not in 1.0.7

    public void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers)
        throws SchedulerException {
        getSchedulerService().addCalendar(calName, calendar, replace, updateTriggers);
    }

    public Set getPausedTriggerGroups() throws SchedulerException {
        return getSchedulerService().getPausedTriggerGroups();
    }

    public int getTriggerState(String triggerName, String triggerGroup) throws SchedulerException {
        return getSchedulerService().getTriggerState(triggerName, triggerGroup);
    }

    public boolean interrupt(String jobName, String groupName) throws UnableToInterruptJobException {
        try {
            return getSchedulerService().interrupt(jobName, groupName);
        } catch (SchedulerException e) {
            throw new UnableToInterruptJobException(e);
        }
    }

    public boolean isInStandbyMode() throws SchedulerException {
        return getSchedulerService().isInStandbyMode();
    }

    public void pauseAll() throws SchedulerException {
        getSchedulerService().pauseAll();
    }

    public Date rescheduleJob(String triggerName, String groupName, Trigger newTrigger) throws SchedulerException {
        return getSchedulerService().rescheduleJob(triggerName, groupName, newTrigger);
    }

    public void resumeAll() throws SchedulerException {
        getSchedulerService().resumeAll();
    }

    public void setJobFactory(JobFactory factory) throws SchedulerException {
        getSchedulerService().setJobFactory(factory);
    }

    public void standby() throws SchedulerException {
        getSchedulerService().standby();
    }

    public void triggerJob(String jobName, String groupName, JobDataMap data) throws SchedulerException {
        getSchedulerService().triggerJob(jobName, groupName, data);
    }

    public void triggerJobWithVolatileTrigger(String jobName, String groupName, JobDataMap data)
        throws SchedulerException {
        getSchedulerService().triggerJob(jobName, groupName, data);
    }

    /* new methods in quartz 1.6.5 below this line */
    public JobListener getGlobalJobListener(String jobName) throws SchedulerException {
        return getSchedulerService().getGlobalJobListener(jobName);
    }

    public TriggerListener getGlobalTriggerListener(String triggerName) throws SchedulerException {
        return getSchedulerService().getGlobalTriggerListener(triggerName);
    }

    public boolean isStarted() throws SchedulerException {
        return getSchedulerService().isStarted();
    }

    public boolean removeGlobalJobListener(String jobName) throws SchedulerException {
        return getSchedulerService().removeGlobalJobListener(jobName);
    }

    public boolean removeGlobalTriggerListener(String triggerName) throws SchedulerException {
        return getSchedulerService().removeGlobalTriggerListener(triggerName);
    }

    public void startDelayed(int delay) throws SchedulerException {
        getSchedulerService().startDelayed(delay);
    }

    @Override
    public void scheduleTriggeredJob(Class<? extends Job> jobClass, String group, boolean isVolatile, Trigger trigger)
        throws SchedulerException {

        new EnhancedSchedulerImpl(getSchedulerService()).scheduleTriggeredJob(jobClass, group, isVolatile, trigger);
        return;
    }
}