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
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public abstract class AbstractGroupConfigurationUpdateJob implements Job {
    public static final String DATAMAP_INT_CONFIG_GROUP_UPDATE_ID = "configGroupUpdateId";
    public static final String DATAMAP_INT_SUBJECT_ID = "subjectId";

    protected static JobDetail getJobDetail(ResourceGroup group, Subject subject, JobDataMap jobDataMap,
        Class jobClass, String jobNamePrefix) {
        JobDetail jobDetail = new JobDetail();
        jobDetail.setName(createUniqueJobName(group, subject, jobNamePrefix));
        jobDetail.setGroup(createJobGroupName(group, jobNamePrefix));
        jobDetail.setVolatility(false); // we want it persisted
        jobDetail.setDurability(false);
        jobDetail.setRequestsRecovery(false);
        jobDetail.setJobClass(jobClass);
        jobDetail.setJobDataMap(jobDataMap);
        return jobDetail;
    }

    public void execute(JobExecutionContext jobContext) throws JobExecutionException {
        JobDetail jobDetail = jobContext.getJobDetail();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        Integer configurationGroupUpdateId = jobDataMap.getIntFromString(DATAMAP_INT_CONFIG_GROUP_UPDATE_ID);

        Integer subjectId = jobDataMap.getIntFromString(DATAMAP_INT_SUBJECT_ID);
        Subject subject = LookupUtil.getSubjectManager().getSubjectById(subjectId);

        processGroupConfigurationUpdate(configurationGroupUpdateId, subject);
    }

    private void processGroupConfigurationUpdate(Integer groupConfigurationUpdateId, Subject subject) {
        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

        String errorMessages = null;
        try {
            long childPluginConfigurationUpdateCount = getConfigurationUpdateCount(groupConfigurationUpdateId,
                configurationManager);

            int rowsProcessed = 0;
            PageControl pc = new PageControl(0, 1000, new OrderingField("cu.id", PageOrdering.ASC));
            while (true) {
                List<Integer> pagedChildUpdateIds = getConfigurationUpdateIds(groupConfigurationUpdateId,
                    configurationManager, pc);
                if (pagedChildUpdateIds.size() <= 0) {
                    break;
                }

                for (Integer childUpdateId : pagedChildUpdateIds) {
                    executeConfigurationUpdate(configurationManager, childUpdateId, subject);
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
            completeGroupConfigurationUpdate(configurationManager, groupConfigurationUpdateId, errorMessages);
        }
    }

    private static String createUniqueJobName(ResourceGroup group, Subject subject, String jobNamePrefix) {
        return createJobGroupName(group, jobNamePrefix) + "-" + subject.getName().hashCode() + "-"
            + System.currentTimeMillis();
    }

    private static String createJobGroupName(ResourceGroup group, String jobNamePrefix) {
        return jobNamePrefix + group.getId();
    }

    protected abstract List<Integer> getConfigurationUpdateIds(Integer groupPluginConfigurationUpdateId,
        ConfigurationManagerLocal configurationManager, PageControl pc);

    protected abstract long getConfigurationUpdateCount(Integer groupPluginConfigurationUpdateId,
        ConfigurationManagerLocal configurationManager);

    protected abstract void executeConfigurationUpdate(ConfigurationManagerLocal configurationManager,
        Integer childUpdateId, Subject subject);

    protected abstract void completeGroupConfigurationUpdate(ConfigurationManagerLocal configurationManager,
        Integer groupConfigurationUpdateId, String errorMessages);
}