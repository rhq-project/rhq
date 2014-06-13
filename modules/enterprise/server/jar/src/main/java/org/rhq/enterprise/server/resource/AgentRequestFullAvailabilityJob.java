/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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
package org.rhq.enterprise.server.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.AgentCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Quartz job that requests a Collection of agents send a full avail report.
 *
 * @author Jay Shaughnessy
 * @author Robert Buck
 */
public class AgentRequestFullAvailabilityJob implements Job {

    public static final String AGENTS = "Agents";
    public static final String KEY_TRIGGER_NAME = "TriggerName";
    public static final String KEY_TRIGGER_GROUP_NAME = "TriggerGroupName";

    private final Log log = LogFactory.getLog(AgentRequestFullAvailabilityJob.class);

    public static void externalizeJobValues(JobDataMap jobDataMap, String key, Collection<Agent> agents) {
        if (agents != null && agents.size() > 0) {
            StringBuilder valuesCommaSeparated = new StringBuilder();
            String comma = "";
            for (Agent agent : agents) {
                valuesCommaSeparated.append(comma).append(agent.getId());
                comma = ",";
            }
            jobDataMap.put(key, valuesCommaSeparated.toString());
        }
    }

    public static List<Agent> internalizeJobValues(String valuesCsvList) {
        if (valuesCsvList == null)
            return Collections.EMPTY_LIST;

        final String[] resourceIdStrings = valuesCsvList.split(",");
        final Integer[] resourceIds = new Integer[resourceIdStrings.length];
        AgentCriteria c = new AgentCriteria();
        for (int i = 0, len = resourceIdStrings.length; (i < len); ++i) {
            resourceIds[i] = Integer.parseInt(resourceIdStrings[i]);
        }
        c.addFilterIds(resourceIds);
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        List<Agent> result = LookupUtil.getAgentManager().findAgentsByCriteria(overlord, c);
        return result;
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

        // On a best effort basic, ask the relevant agents that their next avail report be full, so that we get
        // the current avail type for the newly enabled resources.  If we can't contact the agent don't worry about
        // it; if it's down we'll get a full report when it comes up.
        AgentManagerLocal agentManager = LookupUtil.getAgentManager();
        for (Agent agent : internalizeJobValues((String) jobDataMap.get(AGENTS))) {
            if (agent.isSynthetic()) {
                continue;
            }

            try {
                AgentClient agentClient = agentManager.getAgentClient(agent);
                agentClient.getDiscoveryAgentService().requestFullAvailabilityReport();
            } catch (Throwable t) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to notify Agent ["
                        + agent
                        + "] of enabled resources. The agent is likely down. This is ok, the avails will be updated when the agent is restarted or prompt command 'avail --force is executed'.");
                }
            }
        }
    }
}
