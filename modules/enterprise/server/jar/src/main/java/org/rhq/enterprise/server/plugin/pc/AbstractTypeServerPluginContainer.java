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

package org.rhq.enterprise.server.plugin.pc;

import java.util.Collection;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;

import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The abstract superclass for all plugin containers of the different {@link ServerPluginType plugin types}.
 * 
 * @author John Mazzitelli
 */
public abstract class AbstractTypeServerPluginContainer {

    private final Log log = LogFactory.getLog(this.getClass());

    private final MasterServerPluginContainer master;
    private ServerPluginManager pluginManager;

    /**
     * Instantiates the plugin container. All subclasses must support this and only this
     * constructor.
     * 
     * @param master the master plugin container that is creating this instance.
     */
    public AbstractTypeServerPluginContainer(MasterServerPluginContainer master) {
        this.master = master;
    }

    /**
     * Each plugin container will tell the master which plugins it can support via this method; this
     * method returns the type of plugin that the plugin container can process. Only one
     * plugin container can support a plugin type.
     * 
     * @return the type of plugin that this plugin container instance can support
     */
    public abstract ServerPluginType getSupportedServerPluginType();

    /**
     * Returns the master plugin container that is responsible for managing this instance.
     * 
     * @return this plugin container's master
     */
    public MasterServerPluginContainer getMasterServerPluginContainer() {
        return this.master;
    }

    /**
     * Returns the object that manages the plugins.
     * 
     * @return the plugin manager for this container
     */
    public ServerPluginManager getPluginManager() {
        return this.pluginManager;
    }

    /**
     * The initialize method that prepares the plugin container. This should get the plugin
     * container ready to accept plugins.
     * 
     * Subclasses are free to perform additional tasks by overriding this method.
     *
     * @throws Exception if the plugin container failed to initialize for some reason
     */
    public synchronized void initialize() throws Exception {
        log.debug("Server plugin container initializing");
        this.pluginManager = createPluginManager();
        this.pluginManager.initialize();
    }

    /**
     * This method informs the plugin container that all of its plugins have been loaded.
     * Once this is called, the plugin container can assume all plugins that it will
     * ever know about have been {@link #loadPlugin(ServerPluginEnvironment) loaded}.
     */
    public synchronized void start() {
        log.debug("Server plugin container starting");
        this.pluginManager.startPlugins();
        return;
    }

    /**
     * This will inform the plugin container that it must stop doing its work. Once called,
     * the plugin container must assume that soon it will be asked to {@link #shutdown()}.
     */
    public synchronized void stop() {
        log.debug("Server plugin container stopping");
        this.pluginManager.stopPlugins();
        return;
    }

    /**
     * The shutdown method that will stop and unload all plugins.
     * 
     * Subclasses are free to perform additional tasks by overriding this method.
     */
    public synchronized void shutdown() {
        log.debug("Server plugin container shutting down");

        if (this.pluginManager != null) {
            Collection<ServerPluginEnvironment> envs = this.pluginManager.getPluginEnvironments();
            for (ServerPluginEnvironment env : envs) {
                try {
                    unloadPlugin(env);
                } catch (Exception e) {
                    this.log.warn("Failed to unload plugin [" + env.getPluginName() + "].", e);
                }
            }

            try {
                this.pluginManager.shutdown();
            } finally {
                this.pluginManager = null;
            }
        }

        return;
    }

    /**
     * Informs the plugin container that it has a plugin that it must being to start managing.
     * 
     * @param env the plugin environment, including the plugin jar and its descriptor
     *
     * @throws Exception if failed to load the plugin 
     */
    public synchronized void loadPlugin(ServerPluginEnvironment env) throws Exception {
        if (this.pluginManager != null) {
            this.pluginManager.loadPlugin(env);
        } else {
            throw new Exception("Cannot load a plugin; plugin container is not initialized yet");
        }
    }

    /**
     * Informs the plugin container that a plugin should be unloaded and any of its resources
     * should be released.
     * 
     * @param env the plugin environment, including the plugin jar and its descriptor
     *
     * @throws Exception if failed to unload the plugin 
     */
    public synchronized void unloadPlugin(ServerPluginEnvironment env) throws Exception {
        if (this.pluginManager != null) {
            this.pluginManager.unloadPlugin(env);
        } else {
            throw new Exception("Cannot unload a plugin; plugin container has been shutdown");
        }
    }

    /**
     * If a plugin has scheduled jobs, this method will schedule them now.
     * This particular method implementation schedules the global job as defined in the
     * plugin descriptors. If a plugin has a global scheduled job, its lifecycle listener
     * will be the job handler class.
     * 
     * Subclasses are free to extend this method to schedule additional plugin jobs, but must
     * ensure they call this method so the global scheduled jobs get added to the scheduler.
     * 
     * Note that this is separate from the {@link #start()} method because it is possible that
     * the plugin container has been started before the scheduler has. In this case, the caller
     * must wait for the scheduler to be started before this method is called to schedule jobs.
     * 
     * @throws Exception if failed to schedule jobs
     */
    public synchronized void schedulePluginJobs() throws Exception {
        if (this.pluginManager != null) {
            // if there are any global schedules defined, schedule them now.
            // note that we know if there is no lifecycle listener, then there can't be a global schedule
            for (ServerPluginEnvironment pluginEnv : this.pluginManager.getPluginEnvironments()) {
                String pluginName = pluginEnv.getPluginName();
                if (this.pluginManager.getServerPluginLifecycleListener(pluginName) != null) {
                    Schedule schedule = this.pluginManager.getServerPluginContext(pluginEnv).getSchedule();
                    if (schedule != null) {
                        try {
                            scheduleJob(schedule, pluginName, "__globalScheduleJob", null, null);
                        } catch (Throwable t) {
                            log.warn("Failed to schedule the global plugin job for plugin [" + pluginName + "]", t);
                        }
                    }
                }
            }
        } else {
            throw new Exception("Cannot schedule plugins jobs; plugin container is not initialized yet");
        }

        return;
    }

    /**
     * This will be called when its time for this plugin container to create its plugin manager.
     * Subclasses are free to override this if they need their own specialized plugin manager.
     * 
     * @return the plugin manager for use by this plugin container
     */
    protected ServerPluginManager createPluginManager() {
        return new ServerPluginManager(this);
    }

    /**
     * Returns the logger that can be used to log messages. A convienence object so all
     * subclasses don't have to explicitly declare and create their own.
     * 
     * @return this instance's logger object
     */
    protected Log getLog() {
        return this.log;
    }

    /**
     * Schedules a job for periodic execution.
     * 
     * @param schedule instructs how the job should be scheduled
     * @param pluginName the name of the plugin scheduling the job
     * @param jobId a unique ID that identifies the job within the plugin (jobIds across plugins need not be unique)
     * @param scheduledJobClass the plugin component class name that implements {@link ScheduledJob} that will perform
     *                          the work for the job. If <code>null</code>, the lifecycle listener instance will be
     *                          used (note: if <code>null</code>, it means whatever listener instance was used to initialize
     *                          the plugin is the same instance that will handle the job; a new instance of the listener
     *                          class is not created. This allows a plugin to have a stateful object be periodically
     *                          invoked for each job invocation, as opposed to having a new object instantiated for
     *                          each job invocation).
     * @param callbackData if not <code>null</code>, this is a map of properties that will be passed to the scheduled
     *                     job - it is Properties to force both the keys and values to be strings, since that is how
     *                     we tell quartz to store the data
     *
     * @throws Exception if failed to schedule the job
     */
    protected void scheduleJob(Schedule schedule, String pluginName, String jobId,
        Class<? extends ScheduledJob> scheduledJobClass, Properties callbackData) throws Exception {

        String groupName = pluginName;
        boolean rescheduleIfExists = true; // just in case the parameters change, we'll always want to reschedule it if it exists
        boolean isVolatile = true; // if plugin is removed, this allows for the schedule to go away upon restart automatically

        // determine which quartz job class we should be using, based on the concurrency needs of the schedule
        Class<? extends Job> jobClass;
        if (schedule.isConcurrent()) {
            jobClass = ConcurrentJobWrapper.class;
        } else {
            jobClass = StatefulJobWrapper.class;
        }

        // build the data map for the job, setting some values we need, plus adding the callback data for the plugin itself
        JobDataMap jobData = new JobDataMap();
        jobData.put(AbstractJobWrapper.DATAMAP_PLUGIN_NAME, pluginName);
        jobData.put(AbstractJobWrapper.DATAMAP_PLUGIN_TYPE, getSupportedServerPluginType().stringify());
        jobData.put(AbstractJobWrapper.DATAMAP_JOB_ID, jobId);
        if (scheduledJobClass != null) {
            jobData.put(AbstractJobWrapper.DATAMAP_SCHEDULED_JOB_CLASS, scheduledJobClass.getName());
        }
        if (callbackData != null) {
            jobData.putAll(callbackData);
        }

        // schedule the job now
        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();
        if (schedule instanceof CronSchedule) {
            String cronExpression = ((CronSchedule) scheduler).getCronExpression();
            log.info("Scheduling server plugin cron job: jobName=" + jobId + ", groupName=" + groupName + ", jobClass="
                + jobClass + ", cron=" + cronExpression);
            scheduler.scheduleCronJob(jobId, groupName, jobData, jobClass, rescheduleIfExists, isVolatile,
                cronExpression);
        } else {
            long initialDelay = 10000L; // arbitrary - wait a small bit of time before triggering the job
            long interval = ((PeriodicSchedule) schedule).getPeriod();
            log.info("Scheduling server plugin periodic job: jobName=" + jobId + ", groupName=" + groupName
                + ", jobClass=" + jobClass + ", interval=" + interval);
            scheduler.scheduleRepeatingJob(jobId, groupName, jobData, jobClass, rescheduleIfExists, isVolatile,
                initialDelay, interval);
        }

        return;
    }
}
