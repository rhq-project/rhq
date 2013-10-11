/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

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

        PageList<BundleDeployment> deployments = bundleManager.findBundleDeploymentsByCriteria(overlord, getCriteriaFromContext(context));

        if (deployments.size() > 0) {
            BundleDeployment bundleDeployment = deployments.get(0);

            if (bundleManager.determineOverallBundleDeploymentStatus(bundleDeployment.getId()).isTerminal()) {
                try {
                    context.getScheduler()
                        .deleteJob(context.getJobDetail().getName(), context.getJobDetail().getGroup());
                } catch (SchedulerException e) {
                    throw new JobExecutionException("Could not cancel the bundle deployment completion check job for "
                        + bundleDeployment + ".", e);
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
