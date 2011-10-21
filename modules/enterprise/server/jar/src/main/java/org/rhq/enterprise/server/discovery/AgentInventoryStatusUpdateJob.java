/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.server.discovery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Quartz job that offloads the work of sending inventory status updates to agents
 * to the background, serviced by any one node of the cluster.
 *
 * @author Robert Buck
 */
public class AgentInventoryStatusUpdateJob implements Job {

    public static final String PLATFORMS_COMMA_LIST = "PlatformsList";
    public static final String SERVERS_COMMA_LIST = "ServersList";
    public static final String KEY_TRIGGER_NAME = "TriggerName";
    public static final String KEY_TRIGGER_GROUP_NAME = "TriggerGroupName";

    private final Log log = LogFactory.getLog(AgentInventoryStatusUpdateJob.class);

    public static void externalizeJobValues(JobDataMap jobDataMap, String key, List<Resource> resources) {
        if (resources != null && resources.size() > 0) {
            StringBuilder valuesCommaSeparated = new StringBuilder();
            valuesCommaSeparated.append(resources.get(0).getId());
            for (int i = 1; i < resources.size(); i++) {
                valuesCommaSeparated.append(",").append(resources.get(i).getId());
            }
            jobDataMap.put(key, valuesCommaSeparated.toString());
        }
    }

    public static void internalizeJobValues(EntityManager entityManager, String valuesCsvList, List<Resource> resources) {
        if (valuesCsvList==null)
            return;

        final String[] resourceIdStrings = valuesCsvList.split(",");
        for (String resourceIdString : resourceIdStrings) {
            int resourceId = Integer.parseInt(resourceIdString);
            resources.add(entityManager.find(Resource.class, resourceId));
        }
    }

    private void unscheduleJob(JobDataMap jobDataMap) {
        final String triggerName = (String) jobDataMap.get(KEY_TRIGGER_NAME);
        final String triggerGroupName = (String) jobDataMap.get(KEY_TRIGGER_GROUP_NAME);
        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();
        try {
            scheduler.unscheduleJob(triggerName, triggerGroupName);
        } catch (SchedulerException e) {
            log.error("Failed to unschedule Quartz trigger [" + triggerName + "].", e);
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        final JobDataMap jobDataMap = context.getMergedJobDataMap();

        unscheduleJob(jobDataMap);

        DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();
        discoveryBoss.updateAgentInventoryStatus(
                (String) jobDataMap.get(PLATFORMS_COMMA_LIST),
                (String) jobDataMap.get(SERVERS_COMMA_LIST));
    }
}
