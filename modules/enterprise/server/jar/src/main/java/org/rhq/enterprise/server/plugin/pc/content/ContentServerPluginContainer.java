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
package org.rhq.enterprise.server.plugin.pc.content;

import java.util.Date;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.scheduler.jobs.ContentProviderSyncJob;
import org.rhq.enterprise.server.scheduler.jobs.RepoSyncJob;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.content.ContentPluginDescriptorType;

/**
 * The container responsible for managing the lifecycle of content server-side plugins.
 *
 * @author John Mazzitelli
 */
public class ContentServerPluginContainer extends AbstractTypeServerPluginContainer {

    private static final String SYNC_JOB_GROUP_NAME = "syncContentSource";

    private ContentProviderManager adapterManager;

    public ContentServerPluginContainer(MasterServerPluginContainer master) {
        super(master);
    }

    @Override
    public void initialize() throws Exception {
        getLog().debug("Content server plugin container initializing");
        super.initialize();
        this.adapterManager = createAdapterManager();
        getLog().debug("Content server plugin container initialized");
    }

    @Override
    public void start() {
        super.start();
        this.adapterManager.initialize((ContentServerPluginManager) getPluginManager());
    }

    @Override
    public void shutdown() {
        getLog().debug("Content server plugin container is shutting down");
        this.adapterManager.shutdown();
        super.shutdown();
        getLog().debug("Content server plugin container is shutdown");
    }

    @Override
    public void scheduleAllPluginJobs() throws Exception {
        super.scheduleAllPluginJobs();
        scheduleSyncJobs();
        return;
    }

    @Override
    public ServerPluginType getSupportedServerPluginType() {
        return new ServerPluginType(ContentPluginDescriptorType.class);
    }

    /**
     * Returns the object that is responsible for managing all {@link ContentProvider adapters} which are the
     * things that know how to download content from a specific {@link ContentSource}.
     *
     * @return adapter manager
     */
    public ContentProviderManager getAdapterManager() {
        return this.adapterManager;
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
    public void syncProviderNow(final ContentSource contentSource) throws SchedulerException {
        // Create our job with a trigger that fires immediately and doesn't repeat.
        // Make the name unique - we may already have our cron job schedules.
        // What happens if this is triggered when our cron job is triggered? the job will abort and let the current job finish
        JobDetail job = new JobDetail(ContentProviderSyncJob.createUniqueJobName(contentSource), SYNC_JOB_GROUP_NAME,
            ContentProviderSyncJob.class, false, false, false);

        ContentProviderSyncJob.createJobDataMap(contentSource, job);

        SimpleTrigger trigger = new SimpleTrigger(job.getName(), job.getGroup());
        trigger.setVolatility(false);

        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();
        Date next = scheduler.scheduleJob(job, trigger);

        getLog().info("Scheduled content provider sync job [" + job.getName() + ':' + job.getGroup() +
            "] to fire now at [" + next + "] for [" + contentSource + "]");
    }

    /**
     * Causes the given repo to be scheduled for an immediate sync. The sync will take place through the scheduler,
     * causing this call to be asynchronous and return before the sync itself takes place.
     *
     * @param repo cannot be <code>null</code>
     * @throws SchedulerException if the job cannot be scheduled
     */
    public void syncRepoNow(Repo repo) throws SchedulerException {
        JobDetail job = new JobDetail(RepoSyncJob.createUniqueJobName(repo), SYNC_JOB_GROUP_NAME,
            RepoSyncJob.class, false, false, false);

        RepoSyncJob.createJobDataMap(job, repo);

        SimpleTrigger trigger = new SimpleTrigger(job.getName(), job.getGroup());
        trigger.setVolatility(false);

        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();
        Date next = scheduler.scheduleJob(job, trigger);

        getLog().info("Scheduled repo sync job [" + job.getName() + ':' + job.getGroup() +
            "] to fire now at [" + next + "] for [" + repo + "]");
    }

    /**
     * This will schedule the sync job for the given content source. Once the scheduling is complete, the content
     * source's adapter will be asked to synchronize with the remote content source according to the time(s) specified
     * in the {@link ContentSource#getSyncSchedule() schedule}.
     *
     * <p>If the content source's sync schedule is empty, this method assumes it should not be automatically sync'ed so
     * no schedule will be created and this method simply returns.</p>
     *
     * @param  contentSource provider to sync
     *
     * @throws SchedulerException if failed to schedule the job
     */
    public void scheduleProviderSyncJob(ContentSource contentSource) throws SchedulerException {
        String syncSchedule = contentSource.getSyncSchedule();
        if ((syncSchedule == null) || (syncSchedule.trim().length() == 0)) {
            getLog().debug(contentSource.toString() + " does not define a sync schedule - not scheduling");
            return;
        }

        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();
        scheduler.scheduleCronJob(ContentProviderSyncJob.createJobName(contentSource), SYNC_JOB_GROUP_NAME,
            ContentProviderSyncJob.createJobDataMap(contentSource, null), ContentProviderSyncJob.class, true, false,
            syncSchedule);
    }

    /**
     * It will schedule one job per adapter such that each adapter is scheduled to be synchronized as per its defined
     * sync schedule. This must only be called when all content source adapters have been initialized.
     */
    public void scheduleSyncJobs() {
        if (this.adapterManager != null) {
            for (ContentSource contentSource : this.adapterManager.getAllContentSources()) {
                try {
                    scheduleProviderSyncJob(contentSource);
                } catch (Exception e) {
                    getLog().warn("Could not schedule sync job for content source [" + contentSource + "]", e);
                }
            }
        }
    }

    /**
     * This will unschedule the sync job for the given content source. Once unscheduled, the content source's adapter
     * will not be asked to synchronize with the remote content source.
     *
     * @param  contentSource cannot be <code>null</code>
     *
     * @throws SchedulerException if failed to unschedule the job
     */
    public void unscheduleProviderSyncJob(ContentSource contentSource) throws SchedulerException {
        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();
        scheduler.deleteJob(ContentProviderSyncJob.createJobName(contentSource), SYNC_JOB_GROUP_NAME);
    }

    protected ServerPluginManager createPluginManager() {
        return new ContentServerPluginManager(this);
    }

    /**
     * Creates the adapter manager that the PC will use.
     *
     * <p>This is protected scope so subclasses can define their own adapter manager to use. This is mainly to support
     * tests.</p>
     *
     * @return the new adapter manager
     */
    protected ContentProviderManager createAdapterManager() {
        ContentProviderManager am = new ContentProviderManager();
        return am;
    }
}