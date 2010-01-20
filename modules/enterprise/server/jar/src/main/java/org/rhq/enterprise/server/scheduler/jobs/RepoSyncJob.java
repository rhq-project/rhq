/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
* All rights reserved.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License, version 2, as
* published by the Free Software Foundation, and/or the GNU Lesser
* General Public License, version 2.1, also as published by the Free
* Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License and the GNU Lesser General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License
* and the GNU Lesser General Public License along with this program;
* if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
package org.rhq.enterprise.server.scheduler.jobs;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.quartz.UnableToInterruptJobException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Repo;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Quartz schedule job that handles synchronizing contents of a repo, including:
 * <ul>
 * <li>Package Metadata</li>
 * <li>Package Bits</li>
 * <li>Distribution Tree Metadata</li>
 * <li>Distribution Tree Bits</li>
 * </ul>
 * <p/>
 * Many of the calls out to the plugin are ultimately made from the
 * {@link org.rhq.enterprise.server.plugin.pc.content.ContentProviderManager}. The methods in
 * this job do not call that directly, but rather go through EJB methods to get there.
 * <p/>
 * This implements {@link StatefulJob} (as opposed to {@link org.quartz.Job}) because we do not need
 * nor want this job triggered concurrently. That is, we don't need multiple instances of this job
 * running at the same time.
 * <p/>
 * Much of the functionality in this class was migrated from {@link ContentProviderSyncJob}.
 *
 * @author Jason Dobies
 */
public class RepoSyncJob implements StatefulJob, InterruptableJob {

    public static final String KEY_REPO_NAME = "repoName";

    private final Log log = LogFactory.getLog(this.getClass());

    private Thread executionThread;

    /**
     * {@inheritDoc}
     */
    public void execute(JobExecutionContext context) throws JobExecutionException {

        executionThread = Thread.currentThread();

        try {
            JobDetail jobDetail = context.getJobDetail();
            if (jobDetail == null) {
                throw new IllegalStateException("The job does not have any details");
            }

            JobDataMap dataMap = jobDetail.getJobDataMap();
            if (dataMap == null) {
                throw new IllegalStateException("The job does not have any data in its details");
            }

            String repoName = dataMap.getString(KEY_REPO_NAME);

            if (repoName == null) {
                throw new IllegalStateException("Missing repo name in details context");
            }

            sync(repoName);
        } catch (InterruptedException ie) {
            log.error("Cancelled job [" + context.getJobDetail() + "]");
        } catch (Exception e) {
            String errorMsg = "Failed to sync repo in job [" + context.getJobDetail() + "]";

            log.error(errorMsg, e);
            JobExecutionException jobExecutionException = new JobExecutionException(errorMsg, e, false);

            // Do not retrigger if we threw IllegalStateException because it'll never work anyway
            if (!(e instanceof IllegalStateException)) {
                jobExecutionException.setUnscheduleAllTriggers(false);
            }

            throw jobExecutionException;
        }
    }

    /**
     * Creates (if necessary) and populates a job details map to contain the necessary data to
     * perform a repo sync. If there is an existing details map in the provided details object,
     * it will be reused, leaving existing data intact.
     *
     * @param details may not be <code>null</code>
     * @param repo    may not be <code>null</code>
     * @return populated map used to drive a repo sync job; this will be the same map as what
     *         exists in the {@link JobDetail#getJobDataMap()} call of the details object if
     *         that call does not return <code>null</code>
     */
    public static JobDataMap createJobDataMap(JobDetail details, Repo repo) {
        JobDataMap dataMap;

        if (details != null) {
            dataMap = details.getJobDataMap();
        } else {
            dataMap = new JobDataMap();
        }

        dataMap.put(KEY_REPO_NAME, repo.getName());

        return dataMap;
    }

    /**
     * Creates a name to use when scheduling a repo sync job. Calling this method multiple times
     * on the same parameters will always produce the <em>same</em> name.
     *
     * @param repo may not be <code>null</code>
     * @return name to use to schedule the job; will not be <code>null</code>
     */
    public static String createJobName(Repo repo) {
        // The quartz table has a limited column width of 80 - but we need to use the names to make
        // jobs unique so encode the names' hashcodes to ensure we fit into the quartz job name.
        String jobName = Integer.toHexString(repo.getName().hashCode());

        if (jobName.length() > 80) {
            throw new IllegalArgumentException("Job names max size is 80 chars due to DB column "
                + "size restrictions: " + jobName);
        }

        return jobName;
    }

    /**
     * Performs similar to {#createJobName} except adds uniqueness to the name regardless of the
     * parameters. In other words, calling this method multiple times on the same parameters
     * will always (almost, it's based on system time) produce a <em>unique</em> name.
     *
     * @param repo may not be <code>null</code>
     * @return name to use to schedule the job; will not be <code>null</code>
     */
    public static String createUniqueJobName(Repo repo) {
        // Append current time to add uniqueness to exising job name algorithm
        String jobName = createJobName(repo);

        String uniquifier = Long.toHexString(System.currentTimeMillis());
        jobName = jobName + "-" + uniquifier;

        if (jobName.length() > 80) {
            throw new IllegalArgumentException("Job names max size is 80 chars due to DB column "
                + "size restrictions: " + jobName);
        }

        return jobName;
    }

    /**
     * Performs the repo synchronization.
     * <p/>
     * Note that this method executes outside of any transaction. This is very important since this
     * job is potentially very long running (on the order of hours potentially). We do our processing
     * in here with this in mind. We make sure we never do any one thing that potentially could
     * timeout a transaction.
     *
     * @param repoName may not be <code>null</code>
     * @throws InterruptedException 
     * @throws  
     * @throws Exception if there is an error in the sync
     */
    private void sync(String repoName) throws InterruptedException {

        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();

        // Note that we will keep calling getOverlord on this subject manager - the overlord
        // has a very short session lifespan so we need to keep asking for a new one, due to
        // the possibility that some of the methods we call here take longer than its lifespan
        Subject overlord;

        overlord = subjectManager.getOverlord();
        List<Repo> repoList = repoManager.getRepoByName(repoName);

        if (repoList.size() != 1) {
            throw new RuntimeException("Unexpected number of repos found for name [" + repoName + "]. " + "Found ["
                + repoList.size() + "] repos");
        }
        Repo repoToSync = repoList.get(0);

        // This call executes all of the logic associated with synchronizing the given repo
        repoManager.internalSynchronizeRepos(overlord, new Integer[] { repoToSync.getId() });
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {

        if (executionThread == null) {
            log.error("execution thread is null, cant interrupt", new IllegalStateException());

        } else {
            log.debug("exeThread : [" + executionThread.getName() + "]");
            executionThread.interrupt();
        }
    }
}
