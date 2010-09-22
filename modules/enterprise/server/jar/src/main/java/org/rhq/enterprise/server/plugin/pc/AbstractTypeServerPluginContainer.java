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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.enterprise.server.scheduler.EnhancedScheduler;
import org.rhq.enterprise.server.xmlschema.CronScheduleType;
import org.rhq.enterprise.server.xmlschema.PeriodicScheduleType;
import org.rhq.enterprise.server.xmlschema.ScheduledJobDefinition;

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
     * Maps the time (in epoch milliseconds) when plugins were loaded in this plugin container.
     */
    private final Map<PluginKey, Long> loadedTimestamps;

    /**
     * Instantiates the plugin container. All subclasses must support this and only this
     * constructor.
     *
     * @param master the master plugin container that is creating this instance.
     */
    public AbstractTypeServerPluginContainer(MasterServerPluginContainer master) {
        this.master = master;
        this.loadedTimestamps = Collections.synchronizedMap(new HashMap<PluginKey, Long>());
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
     * Determines if the given plugin is loaded in the plugin container.
     * The plugin may be loaded but not enabled.
     * 
     * @param pluginKey
     * 
     * @return <code>true</code> if the plugin is loaded in this plugin container; <code>false</code> otherwise
     */
    public boolean isPluginLoaded(PluginKey pluginKey) {
        return this.pluginManager.isPluginLoaded(pluginKey.getPluginName());
    }

    /**
     * Determines if the given plugin is enabled in the plugin container.
     * <code>true</code> implies the plugin is also loaded. If <code>false</code> is returned,
     * it is either because the plugin is loaded but disabled, or the plugin is just
     * not loaded. Use {@link #isPluginLoaded(PluginKey)} to know if the plugin is loaded or not.
     * 
     * @param pluginKey
     * 
     * @return <code>true</code> if the plugin is enabled in this plugin container; <code>false</code> otherwise
     */
    public boolean isPluginEnabled(PluginKey pluginKey) {
        return this.pluginManager.isPluginEnabled(pluginKey.getPluginName());
    }

    /**
     * Given a plugin key, this returns the time (in epoch milliseconds) when that plugin was loaded into
     * this plugin container.
     * 
     * @param pluginKey identifies the plugin whose load time is to be returned
     * @return the epoch millis timestamp when the plugin was loaded; <code>null</code> if the plugin is not loaded
     */
    public Long getPluginLoadTime(PluginKey pluginKey) {
        return this.loadedTimestamps.get(pluginKey);
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
                    unloadPlugin(env.getPluginKey());
                } catch (Exception e) {
                    this.log.warn("Failed to unload plugin [" + env.getPluginKey().getPluginName() + "].", e);
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
     * Informs the plugin container that it has a plugin that it must begin to start managing.
     *
     * @param env the plugin environment, including the plugin jar and its descriptor
     * @param enabled <code>true</code> if the plugin should be initialized; <code>false</code> if
     *                the plugin's existence should be noted but it should not be initialized or started
     *
     * @throws Exception if failed to load the plugin
     */
    public synchronized void loadPlugin(ServerPluginEnvironment env, boolean enabled) throws Exception {
        if (this.pluginManager != null) {
            this.pluginManager.loadPlugin(env, enabled);
            this.loadedTimestamps.put(env.getPluginKey(), System.currentTimeMillis());
        } else {
            throw new Exception("Cannot load a plugin; plugin container is not initialized yet");
        }
    }

    /**
     * Informs the plugin container that a plugin should be unloaded and any of its resources
     * should be released.
     *
     * @param pluginKey identifies the plugin that should be shutdown
     *
     * @throws Exception if failed to unload the plugin
     */
    public synchronized void unloadPlugin(PluginKey pluginKey) throws Exception {
        if (this.pluginManager != null) {
            this.pluginManager.unloadPlugin(pluginKey.getPluginName(), false);
            this.loadedTimestamps.remove(pluginKey);
        } else {
            throw new Exception("Cannot unload a plugin; plugin container has been shutdown");
        }
    }

    /**
     * Informs the plugin container that a plugin should be reloaded and any of its resources
     * should be started if being enabled.
     *
     * @param pluginKey identifies the plugin that should be reloaded
     * @param enabled indicates if the plugin should be enabled or disabled after being loaded
     *
     * @throws Exception if failed to unload the plugin
     */
    public synchronized void reloadPlugin(PluginKey pluginKey, boolean enabled) throws Exception {
        if (this.pluginManager != null) {
            this.pluginManager.reloadPlugin(pluginKey.getPluginName(), enabled);
            this.loadedTimestamps.put(pluginKey, System.currentTimeMillis());
        } else {
            throw new Exception("Cannot reload a plugin; plugin container has been shutdown");
        }
    }

    /**
     * If a plugin has scheduled jobs, this method will schedule them now.
     * This particular method implementation schedules the global jobs as defined in the
     * plugin descriptors.
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
    public synchronized void scheduleAllPluginJobs() throws Exception {
        if (this.pluginManager != null) {
            for (ServerPluginEnvironment pluginEnv : this.pluginManager.getPluginEnvironments()) {
                schedulePluginJobs(pluginEnv.getPluginKey());
            }
        } else {
            throw new Exception("Cannot schedule plugins jobs; plugin container is not initialized yet");
        }

        return;
    }

    public synchronized void schedulePluginJobs(PluginKey pluginKey) throws Exception {
        if (this.pluginManager != null) {
            try {
                String pluginName = pluginKey.getPluginName();
                if (this.pluginManager.isPluginEnabled(pluginName)) {
                    ServerPluginEnvironment pluginEnv = this.pluginManager.getPluginEnvironment(pluginName);
                    if (pluginEnv != null) {
                        ServerPluginContext serverPluginContext = this.pluginManager.getServerPluginContext(pluginEnv);
                        List<ScheduledJobDefinition> jobs = serverPluginContext.getSchedules();
                        if (jobs != null) {
                            for (ScheduledJobDefinition job : jobs) {
                                try {
                                    scheduleJob(job, pluginKey);
                                } catch (Throwable t) {
                                    log.warn("Failed to schedule job [" + job + "] for server plugin [" + pluginKey
                                        + "]", t);
                                }
                            }
                        }
                    } else {
                        log.warn("Failed to get server plugin env for [" + pluginKey + "]; cannot schedule jobs");
                    }
                }
            } catch (Throwable t) {
                log.warn("Failed to get scheduled jobs for server plugin [" + pluginKey + "]", t);
            }
        } else {
            throw new Exception("Cannot schedule plugins jobs for server plugin [" + pluginKey
                + "]; plugin container is not initialized yet");
        }

        return;
    }

    /**
     * Unschedules any plugin jobs that are currently scheduled for the named plugin.
     *
     * Subclasses are free to extend this method to unschedule those additional plugin jobs
     * they created, but must ensure they call this method so the global scheduled jobs get
     * removed from the scheduler.
     *
     * Note that this is separate from the {@link #stop()} method because we never want
     * to unschedule jobs since other plugin containers on other servers may be running
     * and able to process the jobs. This method should only be called when a plugin
     * is being disabled or removed.
     *
     * @param pluginKey
     * @throws Exception if failed to unschedule jobs
     */
    public void unschedulePluginJobs(PluginKey pluginKey) throws Exception {

        // note: all jobs for a plugin are placed in the same group, where the group name is the plugin name
        String groupName = pluginKey.getPluginName();

        EnhancedScheduler clusteredScheduler = getMasterServerPluginContainer().getClusteredScheduler();
        EnhancedScheduler nonclusteredScheduler = getMasterServerPluginContainer().getNonClusteredScheduler();

        for (Scheduler scheduler : new Scheduler[] { clusteredScheduler, nonclusteredScheduler }) {
            String[] jobNames = scheduler.getJobNames(groupName);
            if (jobNames != null) {
                for (String jobName : jobNames) {
                    boolean deleted = scheduler.deleteJob(jobName, groupName);
                    if (deleted) {
                        log.info("Job [" + jobName + "] for plugin [" + pluginKey + "] has been unscheduled!");
                    } else {
                        log.warn("Job [" + jobName + "] for plugin [" + pluginKey + "] failed to be unscheduled!");
                    }
                }
            }
        }

        return;
    }

    /**
     * Invokes a control operation on a given plugin and returns the results. This method blocks until
     * the plugin component completes the invocation.
     * 
     * @param pluginKey identifies the plugin whose control operation is to be invoked
     * @param controlName identifies the name of the control operation to invoke
     * @param params parameters to pass to the control operation; may be <code>null</code>
     * @return the results of the invocation
     * 
     * @throws if failed to obtain the plugin component and invoke the control. This usually means an
     *         abnormal error occurred - if the control operation merely failed to do what it needed to do,
     *         the error will be reported in the returned results, not as a thrown exception.
     */
    public ControlResults invokePluginControl(PluginKey pluginKey, String controlName, Configuration params)
        throws Exception {

        if (this.pluginManager != null) {
            String pluginName = pluginKey.getPluginName();
            if (this.pluginManager.isPluginEnabled(pluginName)) {
                ServerPluginEnvironment pluginEnv = this.pluginManager.getPluginEnvironment(pluginName);
                if (pluginEnv != null) {
                    ServerPluginComponent pluginComponent = this.pluginManager.getServerPluginComponent(pluginName);
                    if (pluginComponent != null) {
                        log.debug("Invoking control [" + controlName + "] on server plugin [" + pluginKey + "]");
                        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
                        try {
                            ControlFacet controlFacet = (ControlFacet) pluginComponent; // let it throw ClassCastException when appropriate
                            Thread.currentThread().setContextClassLoader(pluginEnv.getPluginClassLoader());
                            ControlResults results = controlFacet.invoke(controlName, params);
                            return results;
                        } catch (Throwable t) {
                            throw new Exception("Failed to invoke control operation [" + controlName
                                + "] for server plugin [" + pluginKey + "]", t);
                        } finally {
                            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
                        }
                    } else {
                        throw new Exception("Cannot invoke control operation [" + controlName + "] for server plugin ["
                            + pluginKey + "]; failed to get server plugin component");
                    }
                } else {
                    throw new Exception("Cannot invoke control operation [" + controlName + "] for server plugin ["
                        + pluginKey + "]; failed to get server plugin environment");
                }
            } else {
                throw new Exception("Cannot invoke control operation [" + controlName + "] for server plugin ["
                    + pluginKey + "]; plugin is not enabled");
            }
        } else {
            throw new Exception("Cannot invoke control operation [" + controlName + "] for server plugin [" + pluginKey
                + "]; plugin container is not initialized yet");
        }
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
     * Schedules a job for periodic execution. Note that if the <code>schedule</code> indicates the
     * job is not enabled, this method returns immediately as a no-op.
     *
     * @param schedule instructs how the job should be scheduled
     * @param pluginKey the key of the plugin scheduling the job
     *
     * @throws Exception if failed to schedule the job
     */
    protected void scheduleJob(ScheduledJobDefinition schedule, PluginKey pluginKey) throws Exception {

        if (!schedule.isEnabled()) {
            return;
        }

        String groupName = pluginKey.getPluginName();
        boolean rescheduleIfExists = true; // just in case the parameters change, we'll always want to reschedule it if it exists
        boolean isVolatile = true; // if plugin is removed, this allows for the schedule to go away upon restart automatically

        // determine which quartz job class we should be using, based on the concurrency needs of the schedule
        Class<? extends Job> jobClass;
        if (schedule.getScheduleType().isConcurrent()) {
            jobClass = ConcurrentJobWrapper.class;
        } else {
            jobClass = StatefulJobWrapper.class;
        }

        // build the data map for the job, setting some values we need, plus adding the callback data for the plugin itself
        JobDataMap jobData = new JobDataMap();
        jobData.put(AbstractJobWrapper.DATAMAP_PLUGIN_NAME, pluginKey.getPluginName());
        jobData.put(AbstractJobWrapper.DATAMAP_PLUGIN_TYPE, pluginKey.getPluginType().toString());
        jobData.put(AbstractJobWrapper.DATAMAP_JOB_ID, schedule.getJobId());
        jobData.put(AbstractJobWrapper.DATAMAP_SCHEDULE_TYPE, schedule.getScheduleType().getTypeName());
        jobData.put(AbstractJobWrapper.DATAMAP_SCHEDULE_TRIGGER, schedule.getScheduleType().getScheduleTrigger());
        jobData.putAsString(AbstractJobWrapper.DATAMAP_IS_CONCURRENT, schedule.getScheduleType().isConcurrent());
        jobData.putAsString(AbstractJobWrapper.DATAMAP_IS_CLUSTERED, schedule.getScheduleType().isClustered());
        jobData.put(AbstractJobWrapper.DATAMAP_JOB_METHOD_NAME, schedule.getMethodName());
        if (schedule.getClassName() != null) {
            jobData.put(AbstractJobWrapper.DATAMAP_JOB_CLASS, schedule.getClassName());
        }
        if (schedule.getCallbackData() != null) {
            jobData.putAll(schedule.getCallbackData());
        }

        // schedule the job now
        EnhancedScheduler scheduler;

        if (schedule.getScheduleType().isClustered()) {
            scheduler = getMasterServerPluginContainer().getClusteredScheduler();
        } else {
            scheduler = getMasterServerPluginContainer().getNonClusteredScheduler();
        }

        if (schedule.getScheduleType() instanceof CronScheduleType) {
            String cronExpression = ((CronScheduleType) schedule.getScheduleType()).getCronExpression();
            log.info("Scheduling server plugin cron job: jobName=" + schedule.getJobId() + ", groupName=" + groupName
                + ", jobClass=" + jobClass + ", cron=" + cronExpression);
            scheduler.scheduleCronJob(schedule.getJobId(), groupName, jobData, jobClass, rescheduleIfExists,
                isVolatile, cronExpression);
        } else {
            long initialDelay = 10000L; // arbitrary - wait a small bit of time before triggering the job
            long interval = ((PeriodicScheduleType) schedule.getScheduleType()).getPeriod();
            log.info("Scheduling server plugin periodic job: jobName=" + schedule.getJobId() + ", groupName="
                + groupName + ", jobClass=" + jobClass + ", interval=" + interval);
            scheduler.scheduleRepeatingJob(schedule.getJobId(), groupName, jobData, jobClass, rescheduleIfExists,
                isVolatile, initialDelay, interval);
        }

        return;
    }
}
