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
package org.rhq.enterprise.server.cloud;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.cloud.instance.CacheConsistencyManagerBean;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;

/**
 * There are various changes that can occur in the system that make the alertscondition cache stale.
 * This session bean interfaces captures those various types of changes, and sets a bit-mask status 
 * field on the agent managing the data that was changed.  This status field is later checked by the
 * {@link CacheConsistencyManagerBean} to determine what data needs to be reloaded.
 * 
 * @author Joseph Marques
 */

@Stateless
public class StatusManagerBean implements StatusManagerLocal {

    private final Log log = LogFactory.getLog(StatusManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    AgentManagerLocal agentManager;

    @EJB
    @IgnoreDependency
    ServerManagerLocal serverManager;

    @EJB
    @IgnoreDependency
    CloudManagerLocal cloudManager;

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Integer> getAndClearAgentsWithStatusForServer(String serverName) {
        Query selectQuery = entityManager.createNamedQuery(Agent.QUERY_FIND_ALL_WITH_STATUS_BY_SERVER);
        selectQuery.setParameter("serverName", serverName);
        List<Integer> agentIds = selectQuery.getResultList();

        if (agentIds.size() > 0) {
            /* 
             * note: not worried about size of the in clause, because the number of
             * agents per server will be reasonable, say, 50-150
             */
            Query updateQuery = entityManager.createNamedQuery(Agent.UPDATE_CLEAR_STATUS_BY_IDS);
            updateQuery.setParameter("agentIds", agentIds);
            updateQuery.executeUpdate();
        }

        return agentIds;
    }

    public void updateByAlertDefinition(int alertDefinitionId) {
        AlertDefinition definition = entityManager.find(AlertDefinition.class, alertDefinitionId);
        boolean isAlertTemplate = (null != definition.getResourceType());

        // protect against template update, it has no resource and/or agent
        if (isAlertTemplate) {
            return;
        }

        /* 
         * the old alert definition is needed to know which caches to remove stale entries from; the updated / new
         * alert definition is needed to know which caches need to be reloaded to get the new conditions; by the time
         * this method is called, we only have the updated alert definition, thus it's not possible to intelligently
         * know which of the two caches to reload; so, we need to reload them both to be sure the system is consistent
         */
        Server server = serverManager.getServer();
        server.addStatus(Server.Status.ALERT_DEFINITION);
        if (log.isDebugEnabled()) {
            log.debug("Marking status, server[id=" + server.getId() + ", status=" + server.getStatus()
                + "] for alertDefinition[id=" + alertDefinitionId + "]");
        }

        Agent agent = agentManager.getAgentByResourceId(definition.getResource().getId());
        agent.addStatus(Agent.Status.ALERT_DEFINITION);
        if (log.isDebugEnabled()) {
            log.debug("Marking status, agent[id=" + agent.getId() + ", status=" + agent.getStatus()
                + "] for alertDefinition[id=" + alertDefinitionId + "]");
        }
    }

    public void updateByMeasurementBaseline(int baselineId) {
        // baselines refer to measurement-based alert conditions, thus only agent statuses need to be set
        MeasurementBaseline baseline = entityManager.find(MeasurementBaseline.class, baselineId);
        Agent agent = baseline.getSchedule().getResource().getAgent();

        agent.addStatus(Agent.Status.BASELINES_CALCULATED);

        if (log.isDebugEnabled()) {
            log.debug("Marking status, agent[id=" + agent.getId() + ", status=" + agent.getStatus()
                + "] for measurementBaseline[id=" + baselineId + "]");
        }
    }

    /* 
     * this is used to reload the caches because some resource was just imported by a discovery report,
     * which requires reloading the cached because alert templates would have been applied to resources;
     * we know we don't need to call updateByResource(List<Integer> resourceIds) because a discovery report
     * only ever contains resources from the same agent so we can shortcut the work and call out to update
     * the agent a single time 
     * 
     * the agent status no longer has to be updated when a resource is uninventoried because the out-of-band alerts
     * processor (AlertsConditionConsumerBean) handles data coming across the line for resources that have either
     * been deleted or uninventoried.
     */
    public void updateByAgent(int agentId) {
        if (log.isDebugEnabled()) {
            log.debug("Marking status=" + Server.Status.RESOURCE_HIERARCHY_UPDATED + " for all servers in the cloud");
        }

        Agent agent = entityManager.find(Agent.class, agentId);
        agent.addStatus(Agent.Status.RESOURCE_HIERARCHY_UPDATED);
        if (log.isDebugEnabled()) {
            log.debug("Marking status, agent[id=" + agent.getId() + ", status=" + agent.getStatus() + "]");
        }
    }

    public void updateByAutoBaselineCalculationJob() {
        // baselines refer to measurement-based alert conditions, thus only agent statuses need to be set
        List<Agent> agents = agentManager.getAllAgents();
        for (Agent agent : agents) {
            agent.addStatus(Agent.Status.BASELINES_CALCULATED);

            if (log.isDebugEnabled()) {
                log.debug("Marking status, agent[id=" + agent.getId() + ", status=" + agent.getStatus()
                    + "] for AutoBaselineCalculationJob");
            }
        }
    }

}
