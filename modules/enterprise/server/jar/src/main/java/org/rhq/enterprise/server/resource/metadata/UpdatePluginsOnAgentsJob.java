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

package org.rhq.enterprise.server.resource.metadata;

import java.util.UUID;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.AgentCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Lukas Krejci
 * @since 4.11
 */
public class UpdatePluginsOnAgentsJob implements Job {

    public static JobDetail getJobDetail() {
        String groupId = UpdatePluginsOnAgentsJob.class.getName();
        String name = UUID.randomUUID().toString();
        JobDetail jobDetail = new JobDetail(name, groupId, UpdatePluginsOnAgentsJob.class);
        jobDetail.setVolatility(false);
        jobDetail.setDurability(false);
        jobDetail.setRequestsRecovery(false);

        return jobDetail;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        AgentCriteria crit = new AgentCriteria();
        crit.setPageControl(PageControl.getUnlimitedInstance());

        AgentManagerLocal agentManager = LookupUtil.getAgentManager();
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        PageList<Agent> agents = agentManager.findAgentsByCriteria(overlord, crit);

        for(Agent agent : agents) {
            AgentClient client = agentManager.getAgentClient(agent);
            client.updatePlugins();
        }
    }
}
