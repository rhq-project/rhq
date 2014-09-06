/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.scheduler.jobs;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.QuartzUtil;

/**
 * @author Lukas Krejci
 * @since 4.10
 */
public class BundleDeploymentStatusCheckJob implements Job {

    private static final String BUNDLE_DEPLOYMENT_ID_KEY = "BUNDLE_DEPLOYMENT_ID";

    public static JobDetail getJobDetail(int bundleDeploymentId) {
        JobDetail jobDetail = new JobDetail(String.valueOf(bundleDeploymentId),
            BundleDeploymentStatusCheckJob.class.getName(), BundleDeploymentStatusCheckJob.class);
        jobDetail.setVolatility(false);
        jobDetail.setDurability(false);
        jobDetail.setRequestsRecovery(false);

        JobDataMap map = new JobDataMap();
        map.putAsString(BUNDLE_DEPLOYMENT_ID_KEY, bundleDeploymentId);

        jobDetail.setJobDataMap(map);

        return jobDetail;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        BundleManagerLocal bundleManager = LookupUtil.getBundleManager();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

        Subject overlord = subjectManager.getOverlord();

        PageList<BundleDeployment> deployments = bundleManager.findBundleDeploymentsByCriteria(overlord,
            getCriteriaFromContext(context));

        if (deployments.size() > 0) {
            BundleDeployment bundleDeployment = deployments.get(0);
            SchedulerLocal scheduler = LookupUtil.getSchedulerBean();
            JobDetail jobDetail = context.getJobDetail();

            BundleDeploymentStatus bundleDeploymentStatus = bundleManager.determineBundleDeploymentStatus(bundleDeployment.getId());
            if (bundleDeploymentStatus.isTerminal()) {
                // delete this job, we've assigned a final status
                try {
                    context.setResult(bundleDeploymentStatus); // Return status to possible listeners
                    scheduler.deleteJob(jobDetail.getName(), jobDetail.getGroup());
                } catch (SchedulerException e) {
                    throw new JobExecutionException("Could not delete the bundle deployment completion check job for "
                        + bundleDeployment + ".", e);
                }
            } else {
                // try again in 10s
                try {
                    Trigger trigger = QuartzUtil.getFireOnceOffsetTrigger(jobDetail, 10000L);
                    // just need a trigger name unique for this job
                    trigger.setName(String.valueOf(System.currentTimeMillis()));
                    scheduler.scheduleJob(trigger);
                } catch (SchedulerException e) {
                    throw new JobExecutionException(
                        "Could not schedule the bundle deployment completion check job for " + bundleDeployment + ".",
                        e);
                }
            }
        }
    }

    private BundleDeploymentCriteria getCriteriaFromContext(JobExecutionContext context) {
        int bundleDeploymentId = context.getJobDetail().getJobDataMap().getInt(BUNDLE_DEPLOYMENT_ID_KEY);
        BundleDeploymentCriteria crit = new BundleDeploymentCriteria();
        crit.addFilterId(bundleDeploymentId);
        return crit;
    }
}
