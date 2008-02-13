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

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.util.LookupUtil;

public abstract class AbstractAggregateConfigurationUpdateJob implements Job {
    public static final String DATAMAP_INT_CONFIG_GROUP_UPDATE_ID = "configGroupUpdateId";
    public static final String DATAMAP_INT_SUBJECT_ID = "subjectId";

    public void execute(JobExecutionContext jobContext) throws JobExecutionException {
        JobDetail jobDetail = jobContext.getJobDetail();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        Integer configurationGroupUpdateId = jobDataMap.getIntFromString(DATAMAP_INT_CONFIG_GROUP_UPDATE_ID);
        Integer subjectId = jobDataMap.getIntFromString(DATAMAP_INT_SUBJECT_ID);

        Subject subject = LookupUtil.getSubjectManager().findSubjectById(subjectId);

        processAggregateConfigurationUpdate(subject, configurationGroupUpdateId);
    }

    protected abstract void processAggregateConfigurationUpdate(Subject subject, Integer configurationGroupUpdateId);
}