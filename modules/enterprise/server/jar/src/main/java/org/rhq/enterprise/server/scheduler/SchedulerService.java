/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.scheduler;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;

import org.jboss.util.StringPropertyReplacer;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.util.JMXUtil;

/**
 * Scheduler MBean service that simply wraps the Quartz scheduler.
 */
@Singleton
@Startup
@LocalBean
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class SchedulerService implements SchedulerServiceMBean {

    private static final Log LOG = LogFactory.getLog(SchedulerService.class);

    private static final String TIMEOUT_PROPERTY_NAME = "rhq.server.operation-timeout";

    /**
     * The configuration properties for Quartz.
     */
    private Properties quartzProperties;

    // quartz stuff
    private StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
    private Scheduler scheduler;

    public Properties getQuartzProperties() {
        return quartzProperties;
    }

    public void setQuartzProperties(Properties quartzProps) throws SchedulerException {
        if (quartzProps != null) {
            Properties overrides = new Properties();
            for (Map.Entry<Object, Object> entry : quartzProps.entrySet()) {
                String key = entry.getKey().toString();
                String value = entry.getValue().toString();
                value = StringPropertyReplacer.replaceProperties(value);
                overrides.put(key, value);
            }
            quartzProps = overrides;
        }

        this.quartzProperties = quartzProps;
        schedulerFactory.initialize(quartzProps);

        LOG.info("Scheduler has a default operation timeout of [" + getDefaultOperationTimeout() + "] seconds.");
    }

    public Integer getDefaultOperationTimeout() {
        Integer timeout = null;

        if (this.quartzProperties != null) {
            String timeoutStr = this.quartzProperties.getProperty(TIMEOUT_PROPERTY_NAME);

            try {
                timeout = Integer.valueOf(timeoutStr);
            } catch (Exception e) {
                LOG.warn("Invalid operation timeout value specified in the quartz properties: " + TIMEOUT_PROPERTY_NAME
                    + "=" + timeoutStr);
            }
        }

        return timeout;
    }

    public void initQuartzScheduler() throws SchedulerException {
        if (scheduler == null) {
            // TODO: if we are running in a server cluster, make sure we are using Quartz's clustering capability
            LOG.debug("Scheduler service will initialize Quartz scheduler now.");
            scheduler = schedulerFactory.getScheduler();
        } else {
            LOG.debug("Quartz scheduler is initialized and can be started");
        }
    }

    public void startQuartzScheduler() throws SchedulerException {
        initQuartzScheduler();
        LOG.info("Scheduler service will start Quartz scheduler now - jobs will begin executing.");
        scheduler.start();

        return;
    }

    /**
     * Shuts down the scheduler.
     *
     * @throws SchedulerException if failed to shutdown the scheduler
     */
    public synchronized void stop() throws SchedulerException {
        if ((scheduler != null) && !scheduler.isShutdown()) {
            LOG.info("Stopping " + scheduler);
            shutdown();
            scheduler = null;
        }
    }

    //---------------------------------------------------------------------
    // Scheduler interface methods
    //---------------------------------------------------------------------

    public String getSchedulerName() throws SchedulerException {
        return scheduler.getSchedulerName();
    }

    public String getSchedulerInstanceId() throws SchedulerException {
        return scheduler.getSchedulerInstanceId();
    }

    public SchedulerContext getContext() throws SchedulerException {
        return scheduler.getContext();
    }

    public SchedulerMetaData getMetaData() throws SchedulerException {
        return scheduler.getMetaData();
    }

    public void start() {
        LOG.debug("Scheduler Service has started - however, Quartz is not going to be starting yet");
    }

    /**
     * Quartz 1.5.1 deprecates this method.
     *
     * @see        org.rhq.enterprise.server.scheduler.SchedulerServiceMBean#pause()
     * @deprecated
     */
    @Deprecated
    public void pause() throws SchedulerException {
        scheduler.pause();
    }

    /**
     * Quartz 1.5.1 deprecates this method.
     *
     * @see        org.rhq.enterprise.server.scheduler.SchedulerServiceMBean#isPaused()
     * @deprecated
     */
    @Deprecated
    public boolean isPaused() throws SchedulerException {
        return scheduler.isPaused();
    }

    public void shutdown() throws SchedulerException {
        if (isStarted()) {
            scheduler.shutdown();
        }
    }

    public void shutdown(boolean waitForJobsToComplete) throws SchedulerException {
        if (isStarted()) {
            scheduler.shutdown(waitForJobsToComplete);
        }
    }

    public boolean isShutdown() throws SchedulerException {
        return scheduler.isShutdown();
    }

    public List getCurrentlyExecutingJobs() throws SchedulerException {
        return scheduler.getCurrentlyExecutingJobs();
    }

    public Date scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
        return scheduler.scheduleJob(jobDetail, trigger);
    }

    public Date scheduleJob(Trigger trigger) throws SchedulerException {
        return scheduler.scheduleJob(trigger);
    }

    public void addJob(JobDetail jobDetail, boolean replace) throws SchedulerException {
        scheduler.addJob(jobDetail, replace);
    }

    public boolean deleteJob(String jobName, String groupName) throws SchedulerException {
        return scheduler.deleteJob(jobName, groupName);
    }

    public boolean unscheduleJob(String triggerName, String groupName) throws SchedulerException {
        return scheduler.unscheduleJob(triggerName, groupName);
    }

    public void triggerJob(String jobName, String groupName) throws SchedulerException {
        scheduler.triggerJob(jobName, groupName);
    }

    public void triggerJobWithVolatileTrigger(String jobName, String groupName) throws SchedulerException {
        scheduler.triggerJobWithVolatileTrigger(jobName, groupName);
    }

    public void pauseTrigger(String triggerName, String groupName) throws SchedulerException {
        scheduler.pauseTrigger(triggerName, groupName);
    }

    public void pauseTriggerGroup(String groupName) throws SchedulerException {
        scheduler.pauseTriggerGroup(groupName);
    }

    public void pauseJob(String jobName, String groupName) throws SchedulerException {
        scheduler.pauseJob(jobName, groupName);
    }

    public void pauseJobGroup(String groupName) throws SchedulerException {
        scheduler.pauseJobGroup(groupName);
    }

    public void resumeTrigger(String triggerName, String groupName) throws SchedulerException {
        scheduler.resumeTrigger(triggerName, groupName);
    }

    public void resumeTriggerGroup(String groupName) throws SchedulerException {
        scheduler.resumeTriggerGroup(groupName);
    }

    public void resumeJob(String jobName, String groupName) throws SchedulerException {
        scheduler.resumeJob(jobName, groupName);
    }

    public void resumeJobGroup(String groupName) throws SchedulerException {
        scheduler.resumeJobGroup(groupName);
    }

    public String[] getJobGroupNames() throws SchedulerException {
        return scheduler.getJobGroupNames();
    }

    public String[] getJobNames(String groupName) throws SchedulerException {
        return scheduler.getJobNames(groupName);
    }

    public Trigger[] getTriggersOfJob(String jobName, String groupName) throws SchedulerException {
        return scheduler.getTriggersOfJob(jobName, groupName);
    }

    public String[] getTriggerGroupNames() throws SchedulerException {
        return scheduler.getTriggerGroupNames();
    }

    public String[] getTriggerNames(String groupName) throws SchedulerException {
        return scheduler.getTriggerNames(groupName);
    }

    public JobDetail getJobDetail(String jobName, String jobGroup) throws SchedulerException {
        return scheduler.getJobDetail(jobName, jobGroup);
    }

    public Trigger getTrigger(String triggerName, String triggerGroup) throws SchedulerException {
        return scheduler.getTrigger(triggerName, triggerGroup);
    }

    public boolean deleteCalendar(String calName) throws SchedulerException {
        return scheduler.deleteCalendar(calName);
    }

    public Calendar getCalendar(String calName) throws SchedulerException {
        return scheduler.getCalendar(calName);
    }

    public String[] getCalendarNames() throws SchedulerException {
        return scheduler.getCalendarNames();
    }

    public void addGlobalJobListener(JobListener jobListener) throws SchedulerException {
        scheduler.addGlobalJobListener(jobListener);
    }

    public void addJobListener(JobListener jobListener) throws SchedulerException {
        scheduler.addJobListener(jobListener);
    }

    public boolean removeGlobalJobListener(JobListener jobListener) throws SchedulerException {
        return scheduler.removeGlobalJobListener(jobListener);
    }

    public boolean removeJobListener(String name) throws SchedulerException {
        return scheduler.removeJobListener(name);
    }

    public List getGlobalJobListeners() throws SchedulerException {
        return scheduler.getGlobalJobListeners();
    }

    public Set getJobListenerNames() throws SchedulerException {
        return scheduler.getJobListenerNames();
    }

    public JobListener getJobListener(String name) throws SchedulerException {
        return scheduler.getJobListener(name);
    }

    public void addGlobalTriggerListener(TriggerListener triggerListener) throws SchedulerException {
        scheduler.addGlobalTriggerListener(triggerListener);
    }

    public void addTriggerListener(TriggerListener triggerListener) throws SchedulerException {
        scheduler.addTriggerListener(triggerListener);
    }

    public boolean removeGlobalTriggerListener(TriggerListener triggerListener) throws SchedulerException {
        return scheduler.removeGlobalTriggerListener(triggerListener);
    }

    public boolean removeTriggerListener(String name) throws SchedulerException {
        return scheduler.removeTriggerListener(name);
    }

    public List getGlobalTriggerListeners() throws SchedulerException {
        return scheduler.getGlobalTriggerListeners();
    }

    public Set getTriggerListenerNames() throws SchedulerException {
        return scheduler.getTriggerListenerNames();
    }

    public TriggerListener getTriggerListener(String name) throws SchedulerException {
        return scheduler.getTriggerListener(name);
    }

    public void addSchedulerListener(SchedulerListener schedulerListener) throws SchedulerException {
        scheduler.addSchedulerListener(schedulerListener);
    }

    public boolean removeSchedulerListener(SchedulerListener schedulerListener) throws SchedulerException {
        return scheduler.removeSchedulerListener(schedulerListener);
    }

    public List getSchedulerListeners() throws SchedulerException {
        return scheduler.getSchedulerListeners();
    }

    @PostConstruct
    private void init() {
        Properties properties = new Properties();
        InputStream propertiesStream = getClass().getClassLoader().getResourceAsStream("quartz.properties");
        try {
            properties.load(propertiesStream);
            setQuartzProperties(properties);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            StreamUtil.safeClose(propertiesStream);
        }
        JMXUtil.registerMBean(this, SCHEDULER_MBEAN_NAME);
    }

    @PreDestroy
    private void destroy() {
        try {
            stop();
        } catch (SchedulerException ignore) {
        }
        JMXUtil.unregisterMBeanQuietly(SCHEDULER_MBEAN_NAME);
    }

    // Quartz methods that are new in 1.5.1 that were not in 1.0.7

    public void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers)
        throws SchedulerException {
        scheduler.addCalendar(calName, calendar, replace, updateTriggers);
    }

    public Set getPausedTriggerGroups() throws SchedulerException {
        return scheduler.getPausedTriggerGroups();
    }

    public int getTriggerState(String triggerName, String triggerGroup) throws SchedulerException {
        return scheduler.getTriggerState(triggerName, triggerGroup);
    }

    public boolean interrupt(String jobName, String groupName) throws UnableToInterruptJobException {
        return scheduler.interrupt(jobName, groupName);
    }

    public boolean isInStandbyMode() throws SchedulerException {
        return scheduler.isInStandbyMode();
    }

    public void pauseAll() throws SchedulerException {
        scheduler.pauseAll();
    }

    public Date rescheduleJob(String triggerName, String groupName, Trigger newTrigger) throws SchedulerException {
        return scheduler.rescheduleJob(triggerName, groupName, newTrigger);
    }

    public void resumeAll() throws SchedulerException {
        scheduler.resumeAll();
    }

    public void setJobFactory(JobFactory factory) throws SchedulerException {
        scheduler.setJobFactory(factory);
    }

    public void standby() throws SchedulerException {
        scheduler.standby();
    }

    public void triggerJob(String jobName, String groupName, JobDataMap data) throws SchedulerException {
        scheduler.triggerJob(jobName, groupName, data);
    }

    public void triggerJobWithVolatileTrigger(String jobName, String groupName, JobDataMap data)
        throws SchedulerException {
        scheduler.triggerJob(jobName, groupName, data);
    }

    public JobListener getGlobalJobListener(String jobName) throws SchedulerException {
        return scheduler.getGlobalJobListener(jobName);
    }

    public TriggerListener getGlobalTriggerListener(String triggerName) throws SchedulerException {
        return scheduler.getGlobalTriggerListener(triggerName);
    }

    public boolean isStarted() throws SchedulerException {
        return scheduler != null && scheduler.isStarted();
    }

    public boolean removeGlobalJobListener(String jobName) throws SchedulerException {
        return scheduler.removeGlobalJobListener(jobName);
    }

    public boolean removeGlobalTriggerListener(String triggerName) throws SchedulerException {
        return scheduler.removeGlobalTriggerListener(triggerName);
    }

    public void startDelayed(int delay) throws SchedulerException {
        scheduler.startDelayed(delay);
    }
}
