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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;

import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
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
            this.pluginManager.unloadPlugin(pluginKey.getPluginName());
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
        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();

        // note: all jobs for a plugin are placed in the same group, where the group name is the plugin name
        String groupName = pluginKey.getPluginName();

        scheduler.pauseJobGroup(groupName);
        String[] jobNames = scheduler.getJobNames(groupName);
        if (jobNames != null) {
            for (String jobName : jobNames) {
                boolean deleted = scheduler.deleteJob(jobName, groupName);
                if (!deleted) {
                    log.warn("Plugin [" + pluginKey + "] failed to get its job [" + jobName + "] unscheduled!");
                }
            }
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
        jobData.put(AbstractJobWrapper.DATAMAP_JOB_METHOD_NAME, schedule.getMethodName());
        if (schedule.getClassName() != null) {
            jobData.put(AbstractJobWrapper.DATAMAP_JOB_CLASS, schedule.getClassName());
        }
        if (schedule.getCallbackData() != null) {
            jobData.putAll(schedule.getCallbackData());
        }

        // schedule the job now
        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();
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
