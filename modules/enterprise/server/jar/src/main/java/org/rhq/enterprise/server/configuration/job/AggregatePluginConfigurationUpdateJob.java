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
package org.rhq.enterprise.server.configuration.job;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class AggregatePluginConfigurationUpdateJob extends AbstractAggregateConfigurationUpdateJob {
    /**
     * Prefix for all job names and job groups names of aggregate plugin configuration updates.
     */
    private static final String JOB_NAME_PREFIX = "jon-agg-plugin-config-update-";

    private static String createUniqueJobName(ResourceGroup group, Subject subject) {
        return JOB_NAME_PREFIX + group.getId() + "-" + subject.getName().hashCode() + "-" + System.currentTimeMillis();
    }

    private static String createJobGroupName(ResourceGroup group) {
        return JOB_NAME_PREFIX + group.getId();
    }

    public static JobDetail getJobDetail(ResourceGroup group, Subject subject, JobDataMap jobDataMap) {
        JobDetail jobDetail = new JobDetail();
        jobDetail.setName(AggregatePluginConfigurationUpdateJob.createUniqueJobName(group, subject));
        jobDetail.setGroup(AggregatePluginConfigurationUpdateJob.createJobGroupName(group));
        jobDetail.setVolatility(false); // we want it persisted
        jobDetail.setDurability(false);
        jobDetail.setRequestsRecovery(false);
        jobDetail.setJobClass(AggregatePluginConfigurationUpdateJob.class);
        jobDetail.setJobDataMap(jobDataMap);

        return jobDetail;
    }

    @Override
    protected void processAggregateConfigurationUpdate(Subject subject, Integer configurationGroupUpdateId) {
        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

        configurationManager.completeAggregatePluginConfigurationUpdate(configurationGroupUpdateId);
    }
}