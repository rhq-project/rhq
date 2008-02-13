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
package org.rhq.enterprise.server.scheduler.jobs;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.DownloadMode;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.PackageVersionContentSourcePK;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is a Quartz scheduler job whose job is to synchronize one particular {@link ContentSource}. After synchronizing
 * the metadata, this will also attempt to load the content if the content source is not configured for
 * {@link ContentSource#isLazyLoad() lazy loading}.
 *
 * <p>This implements {@link StatefulJob} (as opposed to {@link Job}) because we do not need nor want this job triggered
 * concurrently. That is, we don't need multiple instances of this job running at the same time.</p>
 *
 * @author John Mazzitelli
 */
public class ContentSourceSyncJob implements StatefulJob {
    private static final Log log = LogFactory.getLog(ContentSourceSyncJob.class);
    private static final String SEPARATOR = "--";

    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            String[] jobNameParts = parseJobName(context.getJobDetail().getName());
            String name = jobNameParts[0];
            String typeName = jobNameParts[1];

            synchronizeAndLoad(name, typeName);
        } catch (Exception e) {
            String errorMsg = "Failed to sync content source in job [" + context.getJobDetail() + "]";
            log.error(errorMsg, e);
            JobExecutionException jobExecutionException = new JobExecutionException(errorMsg, e, false);

            // should we unschedule so we never attempt to sync again?
            // That would mean any error will cause this sync to never occur again automatically until
            // we restart the server, restart the server-side content plugin container or somehow manually create
            // the schedule again.  I will assume we will allow this schedule to trigger again, not sure if
            // that is what we want, but we can flip this to true if we want the other behavior.
            jobExecutionException.setUnscheduleAllTriggers(false);

            throw jobExecutionException;
        }
    }

    /**
     * This will synchronize the identified content source such that its package version information is updated and, if
     * not lazy-loading, its package bits are downloaded.
     *
     * <p>Note that this method executes outside of any transaction. This is very important since this job is
     * potentially very long running (on the order of hours potentially). We do our processing in here with this in
     * mind. We make sure we never do any one thing that potentially could timeout a transaction.</p>
     *
     * @param  contentSourceName     name of the {@link ContentSource}
     * @param  contentSourceTypeName name of the {@link ContentSourceType}
     *
     * @throws Exception if either the sync failed or one of the packages failed to download
     */
    private void synchronizeAndLoad(String contentSourceName, String contentSourceTypeName) throws Exception {
        // note that we will keep calling getOverlord on this subject manager - the overlord
        // has a very short session lifespan so we need to keep asking for a new one, due to the possibility
        // that some of the methods we call here take longer than the overlord's lifespan
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject overlord;
        ContentSourceManagerLocal contentManager = LookupUtil.getContentSourceManager();
        ContentSource contentSource;

        overlord = subjectManager.getOverlord();
        contentSource = contentManager
            .getContentSourceByNameAndType(overlord, contentSourceName, contentSourceTypeName);

        if (contentSource == null) {
            throw new Exception("Sync job was asked to sync an unknown content source: " + contentSourceName + "|"
                + contentSourceTypeName);
        }

        int contentSourceId = contentSource.getId();

        // Pulls the metadata down from the remote repository - this creates PackageVersions
        // and associates them with the content source.
        // If 'completed' is false, there was already a synchronization taking place,
        // so we should abort and let that already running sync take care of everything.
        boolean completed = contentManager.internalSynchronizeContentSource(contentSourceId);

        // That might have taken a long time and a user might have updated
        // the content source in the meantime (like, switching to download mode of NEVER)
        // so let's get an updated content source.
        overlord = subjectManager.getOverlord();
        contentSource = contentManager
            .getContentSourceByNameAndType(overlord, contentSourceName, contentSourceTypeName);

        if (contentSource == null) {
            throw new Exception("Content source was deleted, aborting sync job: " + contentSourceName + "|"
                + contentSourceTypeName);
        }

        if (!completed) {
            log.info("Content source [" + contentSourceName + "] is currently being synchronized already. "
                + "Please wait for the current sync job to finish.");
        } else if (contentSource.getDownloadMode() == DownloadMode.NEVER) {
            log.info("Content source [" + contentSourceName + "] is fully synchronized now. "
                + "It is marked to never download bits - bits will not be downloaded now.");
        } else if (contentSource.isLazyLoad()) {
            log.info("Content source [" + contentSourceName + "] is fully synchronized now. "
                + "It is marked for lazy loading - bits will not be downloaded now.");
        } else {
            log.info("Content source [" + contentSourceName + "] is fully synchronized now. "
                + "It is not marked for lazy loading - downloading all bits now.");

            long start = System.currentTimeMillis();

            List<PackageVersionContentSource> packageVersionContentSources;

            // make sure we only get back those that have not yet been loaded
            // TODO: consider paging here - do we have to load them all in at once or can we do them in chunks?
            PageControl pc = PageControl.getUnlimitedInstance();
            overlord = subjectManager.getOverlord();
            packageVersionContentSources = contentManager.getUnloadedPackageVersionsFromContentSource(overlord,
                contentSourceId, pc);

            // For each unloaded package version, let's download them now.
            // This can potentially take a very long time.
            // We abort the entire download if we fail getting just one package.
            for (PackageVersionContentSource item : packageVersionContentSources) {
                PackageVersionContentSourcePK pk = item.getPackageVersionContentSourcePK();

                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Downloading package version [" + pk.getPackageVersion() + "] located at ["
                            + item.getLocation() + "]" + "] from [" + pk.getContentSource() + "]...");
                    }

                    overlord = subjectManager.getOverlord();
                    contentManager.downloadPackageBits(overlord, item);
                } catch (Exception e) {
                    String errorMsg = "Failed to load package bits for package version [" + pk.getPackageVersion()
                        + "] from content source [" + pk.getContentSource() + "] at location [" + item.getLocation()
                        + "]." + "No more packages will be downloaded for this content source.";

                    throw new Exception(errorMsg, e);
                }
            }

            log.info("All package bits for content source [" + contentSourceName + "] have been downloaded."
                + "The downloads started at [" + new Date(start) + "] and ended at [" + new Date() + "]");
        }

        return;
    }

    public static String createJobName(ContentSource cs) {
        String jobName = cs.getName() + SEPARATOR + cs.getContentSourceType().getName();

        if (jobName.length() > 80) {
            throw new IllegalArgumentException("Job names max size is 80 chars due to DB column size restrictions: "
                + jobName);
        }

        return jobName;
    }

    public static String createJobName(ContentSource cs, String appendStr) {
        // appendStr is used to make the job unique among others for the same content source
        String jobName = cs.getName() + SEPARATOR + cs.getContentSourceType().getName() + SEPARATOR + appendStr;

        if (jobName.length() > 80) {
            throw new IllegalArgumentException("Job names max size is 80 chars due to DB column size restrictions: "
                + jobName);
        }

        return jobName;
    }

    private String[] parseJobName(String jobName) {
        return Pattern.compile(SEPARATOR).split(jobName, 3);
    }
}