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
package org.rhq.enterprise.server.plugin.content;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

import org.rhq.core.clientapi.server.plugin.content.ContentSourceAdapter;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.scheduler.jobs.ContentSourceSyncJob;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The container repsonsible for managing the content source server plugins.
 *
 * @author John Mazzitelli
 */
public class ContentSourcePluginContainer {
    private static final String SYNC_JOB_GROUP_NAME = "syncContentSource";

    private static final Log log = LogFactory.getLog(ContentSourcePluginContainer.class);

    private ContentSourcePluginContainerConfiguration configuration;
    private ContentSourcePluginManager pluginManager;
    private ContentSourceAdapterManager adapterManager;

    /**
     * Starts the plugin container, which will load all plugins and begin managing them.
     *
     * @param config
     */
    public void initialize(ContentSourcePluginContainerConfiguration config) {
        log.debug("Content server plugin container has been initialized with config: " + config);

        this.configuration = config;
        this.pluginManager = createPluginManager();
        this.adapterManager = createAdapterManager(this.pluginManager);

        return;
    }

    /**
     * Stops all plugins and cleans up after them.
     */
    public void shutdown() {
        log.debug("Content server plugin container is being shutdown");

        this.adapterManager.shutdown();
        this.pluginManager.shutdown();
        this.configuration = null;
    }

    /**
     * Returns the configuration that this object was initialized with. If this plugin container was not
     * {@link #initialize(ContentSourcePluginContainerConfiguration)} or has been {@link #shutdown()}, this will return
     * <code>null</code>.
     *
     * @return the configuration
     */
    public ContentSourcePluginContainerConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Returns the object that is responsible for managing all {@link ContentSourceAdapter adapters} which are the
     * things that know how to download content from a specific {@link ContentSource}.
     *
     * @return adapter manager
     */
    public ContentSourceAdapterManager getAdapterManager() {
        return adapterManager;
    }

    /**
     * Returns the object that is responsible for managing all plugins and their metadata.
     *
     * <p>This is protected to only allow subclasses access to this - external clients to this PC should never have
     * direct access to this plugin manager.</p>
     *
     * @return plugin manager
     */
    protected ContentSourcePluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * This will syncronize the given content source, meaning its {@link PackageVersion}s will be updated and, if not
     * lazy-loading, will load the package versions that are not loaded yet.
     *
     * <p>Note that this will perform the sync asynchronously in a separate thread. This is because this sync operation
     * can potentially run for hours and we do not want to block the calling thread.</p>
     *
     * @param  contentSource the content source to sync
     *
     * @throws SchedulerException if failed to schedule the job for immediate execution
     */
    public void syncNow(final ContentSource contentSource) throws SchedulerException {
        // Create our job with a trigger that fires immediately and doesn't repeat.
        // Make the name unique - we may already have our cron job schedules.
        // What happens if this is triggered when our cron job is triggered? the job will abort and let the current job finish
        JobDetail job = new JobDetail(ContentSourceSyncJob.createUniqueJobName(contentSource), SYNC_JOB_GROUP_NAME,
            ContentSourceSyncJob.class, false, false, false);

        ContentSourceSyncJob.createJobDataMap(contentSource, job);

        SimpleTrigger trigger = new SimpleTrigger(job.getName(), job.getGroup());
        trigger.setVolatility(false);

        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();
        Date next = scheduler.scheduleJob(job, trigger);

        log.info("Scheduled content source sync job [" + job.getName() + ':' + job.getGroup() + "] to fire now at ["
            + next + "] for [" + contentSource + "]");

        return;
    }

    /**
     * This will schedule the sync job for the given content source. Once the scheduling is complete, the content
     * source's adapter will be asked to synchronize with the remote content source according to the time(s) specified
     * in the {@link ContentSource#getSyncSchedule() schedule}.
     *
     * <p>If the content source's sync schedule is empty, this method assumes it should not be automatically sync'ed so
     * no schedule will be created and this method simply returns.</p>
     *
     * @param  contentSource
     *
     * @throws SchedulerException if failed to schedule the job
     */
    public void scheduleSyncJob(ContentSource contentSource) throws SchedulerException {
        String syncSchedule = contentSource.getSyncSchedule();
        if ((syncSchedule == null) || (syncSchedule.trim().length() == 0)) {
            log.debug(contentSource.toString() + " does not define a sync schedule - not scheduling");
            return;
        }

        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();
        scheduler.scheduleCronJob(ContentSourceSyncJob.createJobName(contentSource), SYNC_JOB_GROUP_NAME,
            ContentSourceSyncJob.createJobDataMap(contentSource, null), ContentSourceSyncJob.class, true, false,
            syncSchedule);

        return;
    }

    /**
     * It will schedule one job per adapter such that each adapter is scheduled to be synchronized as per its defined
     * sync schedule. This must only be called when all content source adapters have been initialized.
     */
    public void scheduleSyncJobs() {
        if (this.adapterManager != null) {
            for (ContentSource contentSource : this.adapterManager.getAllContentSources()) {
                try {
                    scheduleSyncJob(contentSource);
                } catch (Exception e) {
                    log.warn("Could not schedule sync job for content source [" + contentSource + "]", e);
                }
            }
        }

        return;
    }

    /**
     * This will unschedule the sync job for the given content source. Once unscheduled, the content source's adapter
     * will not be asked to synchronize with the remote content source.
     *
     * @param  contentSource
     *
     * @throws SchedulerException if failed to unschedule the job
     */
    public void unscheduleSyncJob(ContentSource contentSource) throws SchedulerException {
        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();
        scheduler.deleteJob(ContentSourceSyncJob.createJobName(contentSource), SYNC_JOB_GROUP_NAME);

        return;
    }

    /**
     * Creates, configures and initializes the plugin manager that the PC will use.
     *
     * <p>This is protected scope so subclasses can define their own plugin manager to use. This is mainly to support
     * tests.</p>
     *
     * @return the new plugin manager
     */
    protected ContentSourcePluginManager createPluginManager() {
        ContentSourcePluginManager pm = new ContentSourcePluginManager();
        pm.setConfiguration(getConfiguration());
        pm.initialize();
        return pm;
    }

    /**
     * Creates, configures and initializes the adapter manager that the PC will use.
     *
     * <p>This is protected scope so subclasses can define their own adapter manager to use. This is mainly to support
     * tests.</p>
     *
     * @param  pluginManager the plugin manager that was {@link #createPluginManager() created} for use in the PC. The
     *                       adapter manager can use this to obtain information about plugins.
     *
     * @return the new adapter manager
     */
    protected ContentSourceAdapterManager createAdapterManager(ContentSourcePluginManager pluginManager) {
        ContentSourceAdapterManager am = new ContentSourceAdapterManager();
        am.setConfiguration(getConfiguration());
        am.initialize(pluginManager);
        return am;
    }
}