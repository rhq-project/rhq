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

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.management.ObjectName;

import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.spi.JobFactory;

import org.rhq.core.util.ObjectNameFactory;
import org.rhq.enterprise.server.operation.OperationManagerLocal;

/**
 * MBean interface which is also the local interface to the scheduler SLSB. This is essentially the same interface as
 * the Quartz scheduler. We have both an MBean and SLSB facade to Quartz - see {@link SchedulerLocal}.
 */
public interface SchedulerServiceMBean extends Scheduler {
    /**
     * The Scheduler MBean Server name.
     */
    ObjectName SCHEDULER_MBEAN_NAME = ObjectNameFactory.create("rhq:service=scheduler");

    /**
     * Get the properties for Quartz.
     *
     * @return Quartz configuration properties
     */
    Properties getQuartzProperties();

    /**
     * Set the properties for Quartz and reinitialize the Quartz scheduler factory.
     *
     * @param  quartzProps the new properties
     *
     * @throws SchedulerException if failed to initialize Quartz
     */
    void setQuartzProperties(Properties quartzProps) throws SchedulerException;

    /**
     * Returns the configured operation timeout. The scheduler can have defined in its
     * {@link #getQuartzProperties() configuration} a custom property that indicates how long an operation is allowed to
     * run before it is considered to have timed out. This is used to clean out old running operations that are hung in
     * the in-progress state.
     *
     * @return the default operation timeout, or <code>null</code> if not defined
     *
     * @see    OperationManagerLocal#checkForTimedOutOperations()
     */
    Integer getDefaultOperationTimeout();

    /**
     * This creates the scheduler but does not start it. Calling this method will NOT start executing jobs.
     *
     * @throws SchedulerException
     */
    void initQuartzScheduler() throws SchedulerException;

    /**
     * This actually starts the scheduler. Calling this method will start executing jobs. Make sure when you call this
     * method that the server is fully up and all EJBs are deployed.
     *
     * @throws SchedulerException
     */
    void startQuartzScheduler() throws SchedulerException;

    String getSchedulerName() throws SchedulerException;

    String getSchedulerInstanceId() throws SchedulerException;

    SchedulerContext getContext() throws SchedulerException;

    SchedulerMetaData getMetaData() throws SchedulerException;

    /**
     * This method's semantics are overloaded since start() is used both by the Quartz interface and the JBossAS MBean
     * service interface. Calling this method does <b>not</b> start the Quartz scheduler. It only starts the MBean
     * service. To actually start the Quartz scheduler, the MBean service must have been started (that is, this method
     * called) <b>and</b> {@link #startQuartzScheduler()} must be called.
     *
     * @throws SchedulerException
     *
     * @see    #startQuartzScheduler()
     */
    void start() throws SchedulerException;

    void pause() throws SchedulerException;

    boolean isPaused() throws SchedulerException;

    void shutdown() throws SchedulerException;

    void shutdown(boolean waitForJobsToComplete) throws SchedulerException;

    boolean isShutdown() throws SchedulerException;

    List getCurrentlyExecutingJobs() throws SchedulerException;

    Date scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException;

    Date scheduleJob(Trigger trigger) throws SchedulerException;

    void addJob(JobDetail jobDetail, boolean replace) throws SchedulerException;

    boolean deleteJob(String jobName, String groupName) throws SchedulerException;

    boolean unscheduleJob(String triggerName, String groupName) throws SchedulerException;

    void triggerJob(String jobName, String groupName) throws SchedulerException;

    void triggerJobWithVolatileTrigger(String jobName, String groupName) throws SchedulerException;

    void pauseTrigger(String triggerName, String groupName) throws SchedulerException;

    void pauseTriggerGroup(String groupName) throws SchedulerException;

    void pauseJob(String jobName, String groupName) throws SchedulerException;

    void pauseJobGroup(String groupName) throws SchedulerException;

    void resumeTrigger(String triggerName, String groupName) throws SchedulerException;

    void resumeTriggerGroup(String groupName) throws SchedulerException;

    void resumeJob(String jobName, String groupName) throws SchedulerException;

    void resumeJobGroup(String groupName) throws SchedulerException;

    String[] getJobGroupNames() throws SchedulerException;

    String[] getJobNames(String groupName) throws SchedulerException;

    Trigger[] getTriggersOfJob(String jobName, String groupName) throws SchedulerException;

    String[] getTriggerGroupNames() throws SchedulerException;

    String[] getTriggerNames(String groupName) throws SchedulerException;

    JobDetail getJobDetail(String jobName, String jobGroup) throws SchedulerException;

    Trigger getTrigger(String triggerName, String triggerGroup) throws SchedulerException;

    boolean deleteCalendar(String calName) throws SchedulerException;

    Calendar getCalendar(String calName) throws SchedulerException;

    String[] getCalendarNames() throws SchedulerException;

    void addGlobalJobListener(JobListener jobListener) throws SchedulerException;

    void addJobListener(JobListener jobListener) throws SchedulerException;

    boolean removeGlobalJobListener(JobListener jobListener) throws SchedulerException;

    boolean removeJobListener(String name) throws SchedulerException;

    List getGlobalJobListeners() throws SchedulerException;

    Set getJobListenerNames() throws SchedulerException;

    JobListener getJobListener(String name) throws SchedulerException;

    void addGlobalTriggerListener(TriggerListener triggerListener) throws SchedulerException;

    void addTriggerListener(TriggerListener triggerListener) throws SchedulerException;

    boolean removeGlobalTriggerListener(TriggerListener triggerListener) throws SchedulerException;

    boolean removeTriggerListener(String name) throws SchedulerException;

    List getGlobalTriggerListeners() throws SchedulerException;

    Set getTriggerListenerNames() throws SchedulerException;

    TriggerListener getTriggerListener(String name) throws SchedulerException;

    void addSchedulerListener(SchedulerListener schedulerListener) throws SchedulerException;

    boolean removeSchedulerListener(SchedulerListener schedulerListener) throws SchedulerException;

    List getSchedulerListeners() throws SchedulerException;

    void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers)
        throws SchedulerException;

    Set getPausedTriggerGroups() throws SchedulerException;

    int getTriggerState(String triggerName, String triggerGroup) throws SchedulerException;

    boolean interrupt(String jobName, String groupName) throws org.quartz.UnableToInterruptJobException;

    boolean isInStandbyMode() throws SchedulerException;

    void pauseAll() throws SchedulerException;

    Date rescheduleJob(String triggerName, String groupName, Trigger newTrigger) throws SchedulerException;

    void resumeAll() throws SchedulerException;

    void setJobFactory(JobFactory factory) throws SchedulerException;

    void standby() throws SchedulerException;

    void triggerJob(String jobName, String groupName, JobDataMap data) throws SchedulerException;

    void triggerJobWithVolatileTrigger(String jobName, String groupName, JobDataMap data) throws SchedulerException;
}