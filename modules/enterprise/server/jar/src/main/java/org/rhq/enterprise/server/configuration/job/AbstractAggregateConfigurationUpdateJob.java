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

import java.util.List;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public abstract class AbstractAggregateConfigurationUpdateJob implements Job
{
    public static final String DATAMAP_INT_CONFIG_GROUP_UPDATE_ID = "configGroupUpdateId";
    public static final String DATAMAP_INT_SUBJECT_ID = "subjectId";

    protected static JobDetail getJobDetail(ResourceGroup group, Subject subject, JobDataMap jobDataMap,
                                            String jobNamePrefix)
    {
        JobDetail jobDetail = new JobDetail();
        jobDetail.setName(createUniqueJobName(group, subject, jobNamePrefix));
        jobDetail.setGroup(createJobGroupName(group, jobNamePrefix));
        jobDetail.setVolatility(false); // we want it persisted
        jobDetail.setDurability(false);
        jobDetail.setRequestsRecovery(false);
        jobDetail.setJobClass(AggregatePluginConfigurationUpdateJob.class);
        jobDetail.setJobDataMap(jobDataMap);
        return jobDetail;
    }

    public void execute(JobExecutionContext jobContext) throws JobExecutionException
    {
        JobDetail jobDetail = jobContext.getJobDetail();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        Integer configurationGroupUpdateId = jobDataMap.getIntFromString(DATAMAP_INT_CONFIG_GROUP_UPDATE_ID);

        // TODO: Add authz checks?
        Integer subjectId = jobDataMap.getIntFromString(DATAMAP_INT_SUBJECT_ID);
        Subject subject = LookupUtil.getSubjectManager().findSubjectById(subjectId);

        processAggregateConfigurationUpdate(configurationGroupUpdateId);
    }

    private void processAggregateConfigurationUpdate(Integer aggregatePluginConfigurationUpdateId) {
        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

        String errorMessages = null;
        try {
            long childPluginConfigurationUpdateCount = getConfigurationUpdateCount(aggregatePluginConfigurationUpdateId,
                    configurationManager);

            int rowsProcessed = 0;
            PageControl pc = new PageControl(0, 50, new OrderingField("cu.id", PageOrdering.ASC));
            while (true) {
                List<Integer> pagedChildUpdateIds = getConfigurationUpdates(aggregatePluginConfigurationUpdateId,
                        configurationManager, pc);
                if (pagedChildUpdateIds.size() <= 0) {
                    break;
                }

                for (Integer childUpdateId : pagedChildUpdateIds) {
                    completeConfigurationUpdate(configurationManager, childUpdateId);
                }

                rowsProcessed += pagedChildUpdateIds.size();
                if (rowsProcessed >= childPluginConfigurationUpdateCount) {
                    break;
                }

                pc.setPageNumber(pc.getPageNumber() + 1);
            }
        } catch (Exception e) {
            errorMessages = ThrowableUtil.getAllMessages(e);
        } finally {
            updateAggregateConfigurationUpdateStatus(aggregatePluginConfigurationUpdateId, configurationManager,
                    errorMessages);
        }
    }

    private static String createUniqueJobName(ResourceGroup group, Subject subject, String jobNamePrefix)
    {
        return createJobGroupName(group, jobNamePrefix) + "-" + subject.getName().hashCode() + "-" + System.currentTimeMillis();
    }

    private static String createJobGroupName(ResourceGroup group, String jobNamePrefix)
    {
        return jobNamePrefix + group.getId();
    }

    protected abstract List<Integer> getConfigurationUpdates(Integer aggregatePluginConfigurationUpdateId,
                                                             ConfigurationManagerLocal configurationManager,
                                                             PageControl pc);

    protected abstract long getConfigurationUpdateCount(Integer aggregatePluginConfigurationUpdateId,
                                                        ConfigurationManagerLocal configurationManager);

    protected abstract void completeConfigurationUpdate(ConfigurationManagerLocal configurationManager, Integer childUpdateId);

    protected abstract void updateAggregateConfigurationUpdateStatus(Integer aggregatePluginConfigurationUpdateId,
                                                                     ConfigurationManagerLocal configurationManager,
                                                                     String errorMessages);
}