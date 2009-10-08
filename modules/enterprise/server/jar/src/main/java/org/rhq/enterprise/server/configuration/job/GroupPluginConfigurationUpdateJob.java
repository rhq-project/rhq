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

import org.quartz.JobDataMap;
import org.quartz.JobDetail;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;

public class GroupPluginConfigurationUpdateJob extends AbstractGroupConfigurationUpdateJob {
    /**
     * Prefix for all job names and job group names of group plugin configuration updates.
     */
    private static final String JOB_NAME_PREFIX = "rhq-apcu-";

    public static JobDetail getJobDetail(ResourceGroup group, Subject subject, JobDataMap jobDataMap) {
        return AbstractGroupConfigurationUpdateJob.getJobDetail(group, subject, jobDataMap,
            GroupPluginConfigurationUpdateJob.class, JOB_NAME_PREFIX);
    }

    protected List<Integer> getConfigurationUpdateIds(Integer groupPluginConfigurationUpdateId,
        ConfigurationManagerLocal configurationManager, PageControl pc) {
        List<Integer> pagedChildUpdateIds = configurationManager.findPluginConfigurationUpdatesByParentId(
            groupPluginConfigurationUpdateId, pc);
        return pagedChildUpdateIds;
    }

    protected long getConfigurationUpdateCount(Integer groupPluginConfigurationUpdateId,
        ConfigurationManagerLocal configurationManager) {
        long childPluginConfigurationUpdateCount = configurationManager
            .getPluginConfigurationUpdateCountByParentId(groupPluginConfigurationUpdateId);
        return childPluginConfigurationUpdateCount;
    }

    protected void executeConfigurationUpdate(ConfigurationManagerLocal configurationManager, Integer childUpdateId,
        Subject subject) {
        configurationManager.completePluginConfigurationUpdate(childUpdateId);
    }

    protected void completeGroupConfigurationUpdate(ConfigurationManagerLocal configurationManager,
        Integer groupConfigurationUpdateId, String errorMessages) {
        configurationManager.updateGroupPluginConfigurationUpdateStatus(groupConfigurationUpdateId, errorMessages);
    }
}