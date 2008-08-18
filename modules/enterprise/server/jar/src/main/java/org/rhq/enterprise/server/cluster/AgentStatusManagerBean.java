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
package org.rhq.enterprise.server.cluster;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.core.AgentManagerLocal;

/**
 * @author Joseph Marques
 */

@Stateless
public class AgentStatusManagerBean implements AgentStatusManagerLocal {

    private final Log log = LogFactory.getLog(AgentStatusManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    AgentManagerLocal agentManager;

    @SuppressWarnings("unchecked")
    public List<Agent> getAgentsWithStatusForServer(String serverName) {
        // replace this query with QUERY_FIND_ALL_WITH_STATUS_BY_SERVER and uncomment setParameter
        // when agents can successfully register with server and the HA tables are updating
        Query query = entityManager.createNamedQuery(Agent.QUERY_FIND_ALL_WITH_STATUS);
        //query.setParameter("serverName", serverName);

        List<Agent> results = query.getResultList();
        return results;
    }

    public void updateByAlertDefinition(int alertDefinitionId) {
        AlertDefinition definition = entityManager.find(AlertDefinition.class, alertDefinitionId);
        boolean isAlertTemplate = (null != definition.getResourceType());

        // protect against template update, it has no resource and/or agent
        if (isAlertTemplate)
            return;

        Agent agent = definition.getResource().getAgent();

        agent.addStatus(Agent.Status.ALERT_DEFINITIONS_CHANGED);

        log.info("Marking status, agent[id=" + agent.getId() + ", status=" + agent.getStatus()
            + "] for alertDefinition[id=" + alertDefinitionId + "]");
    }

    public void updateByMeasurementBaseline(int baselineId) {
        MeasurementBaseline baseline = entityManager.find(MeasurementBaseline.class, baselineId);
        Agent agent = baseline.getSchedule().getResource().getAgent();

        agent.addStatus(Agent.Status.BASELINES_CALCULATED);

        log.info("Marking status, agent[id=" + agent.getId() + ", status=" + agent.getStatus()
            + "] for measurementBaseline[id=" + baselineId + "]");
    }

    public void updateByResource(int resourceId) {
        Resource resource = entityManager.find(Resource.class, resourceId);
        Agent agent = resource.getAgent();
        if (agent == null) {
            //TODO: jmarques - fix ResourceFactoryManagerBeanTest, see rev1202-1204 for examples of the proper fix
            return; // some unit tests won't always have attached agents for all resources
        }

        agent.addStatus(Agent.Status.RESOURCE_HIERARCHY_UPDATED);

        log.info("Marking status, agent[id=" + agent.getId() + ", status=" + agent.getStatus() + "] for resource[id="
            + resourceId + "]");
    }

    public void updateByAutoBaselineCalculationJob() {
        List<Agent> agents = agentManager.getAllAgents();
        for (Agent agent : agents) {
            agent.addStatus(Agent.Status.BASELINES_CALCULATED);

            log.info("Marking status, agent[id=" + agent.getId() + ", status=" + agent.getStatus()
                + "] for AutoBaselineCalculationJob");
        }
    }
}
