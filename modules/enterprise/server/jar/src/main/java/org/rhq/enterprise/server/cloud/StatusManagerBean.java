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

import org.jboss.annotation.ejb.PoolClass;
import org.jboss.ejb3.StrictMaxPool;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.cloud.instance.CacheConsistencyManagerBean;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * There are various changes that can occur in the system that make the alertscondition cache stale.
 * This session bean interfaces captures those various types of changes, and sets a bit-mask status 
 * field on the agent managing the data that was changed.  This status field is later checked by the
 * {@link CacheConsistencyManagerBean} to determine what data needs to be reloaded.
 * 
 * Unless we're debugging, let's use the status field on the {@link Agent} and {@link Server} entities
 * as a simple bit field; this way the logic for setting the field simplifies to a simple boolean check
 * instead of a more complex bit
 * 
 * @author Joseph Marques
 */
/*
 * All public methods are marked as requires new because in the worst case that these methods complete
 * but some downstream method in the same call chain fails, it won't hurt anything to pessimistically
 * reload the caches.  By having these methods execute in their own transaction, it further reduces
 * database row contention because any locks held in larger flows that called into these methods no longer
 * require holding these locks as part of their processing.
 */
@Stateless
// NOTE: The CacheConsistencyManagerBean, CloudManagerBean, ServerManagerBean, StatusManagerBean, and SystemManagerBean
//       SLSB's are all invoked, either directly or indirectly, by EJB timers. Since EJB timer invocations are always
//       done in new threads, using the default SLSB pool impl ({@link ThreadlocalPool}) would cause a new instance of
//       this SLSB to be created every time it was invoked by an EJB timer. This would be bad because an existing
//       instance would not be reused, but it is really bad because the instance would also never get destroyed, causing
//       heap space to gradually leak until the Server eventually ran out of memory. Hence, we must use a
//       {@link StrictMaxPool}, which will use a fixed pool of instances of this SLSB, instead of a ThreadlocalPool.
//       Because most of these SLSB's are also invoked by other callers (i.e. Agents, GUI's, or CLI's) besides EJB
//       timers, we set the max pool size to 60, which is double the default value, to minimize the chances of EJB
//       timer invocations, which are the most critical, from having to block and potentially getting backed up in the
//       queue. For more details, see https://bugzilla.redhat.com/show_bug.cgi?id=693232 (ips, 05/05/11).
@PoolClass(value = StrictMaxPool.class, maxSize = 60)
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

    @EJB
    @IgnoreDependency
    AlertDefinitionManagerLocal alertDefinitionManager;

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
            Query updateQuery = entityManager.createNamedQuery(Agent.QUERY_UPDATE_CLEAR_STATUS_BY_IDS);
            updateQuery.setParameter("agentIds", agentIds);
            updateQuery.executeUpdate();
        }

        return agentIds;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateByResource(Subject subject, int resourceId) {
        log.debug("About to mark status by resource");

        /* 
         * the old alert definition is needed to know which caches to remove stale entries from; the updated / new
         * alert definition is needed to know which caches need to be reloaded to get the new conditions; by the time
         * this method is called, we only have the updated alert definition, thus it's not possible to intelligently
         * know which of the two caches to reload; so, we need to reload them both to be sure the system is consistent
         */
        markGlobalCache(); // use local references to execute in the same transaction

        Query updateAgentQuery = entityManager.createNamedQuery(Agent.QUERY_UPDATE_STATUS_BY_RESOURCE);
        updateAgentQuery.setParameter("resourceId", resourceId);
        int agentsUpdated = updateAgentQuery.executeUpdate();

        /*
         * this is informational debugging only - do NOT change the status bits here
         */
        if (log.isDebugEnabled()) {
            Agent agent = agentManager.getAgentByResourceId(LookupUtil.getSubjectManager().getOverlord(), resourceId);
            log.debug("Marking status, agent[id=" + agent.getId() + ", status=" + agent.getStatus()
                + "] for resource[id=" + resourceId + "]");

            log.debug("Agents updated: " + agentsUpdated);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateByAlertDefinition(Subject subject, int alertDefinitionId) {
        log.debug("About to mark status by alert definition");

        // alert templates and group alert definitions do not represent cache-ready entries
        if (alertDefinitionManager.isResourceAlertDefinition(alertDefinitionId) == false) {
            return;
        }

        /* 
         * the old alert definition is needed to know which caches to remove stale entries from; the updated / new
         * alert definition is needed to know which caches need to be reloaded to get the new conditions; by the time
         * this method is called, we only have the updated alert definition, thus it's not possible to intelligently
         * know which of the two caches to reload; so, we need to reload them both to be sure the system is consistent
         */
        markGlobalCache(); // use local references to execute in the same transaction

        Query updateAgentQuery = entityManager.createNamedQuery(Agent.QUERY_UPDATE_STATUS_BY_ALERT_DEFINITION);
        updateAgentQuery.setParameter("alertDefinitionId", alertDefinitionId);
        int agentsUpdated = updateAgentQuery.executeUpdate();

        /*
         * this is informational debugging only - do NOT change the status bits here
         */
        if (log.isDebugEnabled()) {
            AlertDefinition definition = entityManager.find(AlertDefinition.class, alertDefinitionId);
            Agent agent = agentManager.getAgentByResourceId(LookupUtil.getSubjectManager().getOverlord(), definition
                .getResource().getId());
            log.debug("Marking status, agent[id=" + agent.getId() + ", status=" + agent.getStatus()
                + "] for alertDefinition[id=" + alertDefinitionId + "]");

            log.debug("Agents updated: " + agentsUpdated);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void markGlobalCache() {
        Query updateServerQuery = entityManager.createNamedQuery(Server.QUERY_UPDATE_STATUS_BY_NAME);
        updateServerQuery.setParameter("identity", serverManager.getIdentity());
        int serversUpdated = updateServerQuery.executeUpdate();

        /*
         * this is informational debugging only - do NOT change the status bits here
         */
        if (log.isDebugEnabled()) {
            Server server = serverManager.getServer();
            log.debug("Marking status, server[id=" + server.getId() + ", status=" + server.getStatus() + "]");

            log.debug("Servers updated: " + serversUpdated);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateByMeasurementBaseline(int baselineId) {
        log.debug("About to mark status by measurement baseline");
        // baselines refer to measurement-based alert conditions, thus only agent statuses need to be set
        Query updateAgentQuery = entityManager.createNamedQuery(Agent.QUERY_UPDATE_STATUS_BY_MEASUREMENT_BASELINE);
        updateAgentQuery.setParameter("baselineId", baselineId);
        updateAgentQuery.executeUpdate();

        /*
         * this is informational debugging only - do NOT change the status bits here
         */
        if (log.isDebugEnabled()) {
            MeasurementBaseline baseline = entityManager.find(MeasurementBaseline.class, baselineId);
            Agent agent = baseline.getSchedule().getResource().getAgent();
            log.debug("Marking status, agent[id=" + agent.getId() + ", status=" + agent.getStatus()
                + "] for measurementBaseline[id=" + baselineId + "]");
        }
    }

    /* 
     * this is used to reload the caches because some resource was just imported by a discovery report,
     * which requires reloading the cache because alert templates would have been applied to resources;
     * we know we don't need to call updateByResource(List<Integer> resourceIds) because a discovery report
     * only ever contains resources from the same agent so we can shortcut the work and call out to update
     * the agent a single time 
     * 
     * the agent status no longer has to be updated when a resource is uninventoried because the out-of-band alerts
     * processor (AlertsConditionConsumerBean) handles data coming across the line for resources that have either
     * been deleted or uninventoried.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateByAgent(int agentId) {
        log.debug("About to mark status by agent");
        Query updateAgentQuery = entityManager.createNamedQuery(Agent.QUERY_UPDATE_STATUS_BY_AGENT);
        updateAgentQuery.setParameter("agentId", agentId);
        updateAgentQuery.executeUpdate();

        /*
         * this is informational debugging only - do NOT change the status bits here
         */
        if (log.isDebugEnabled()) {
            Agent agent = entityManager.find(Agent.class, agentId);
            log.debug("Marking status, agent[id=" + agent.getId() + ", status=" + agent.getStatus() + "]");
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateByAutoBaselineCalculationJob() {
        log.debug("About to mark status by autoBaselineCalculationJob");
        // baselines refer to measurement-based alert conditions, thus only agent statuses need to be set
        Query updateAgentQuery = entityManager.createNamedQuery(Agent.QUERY_UPDATE_STATUS_FOR_ALL);
        updateAgentQuery.executeUpdate();

        /*
         * this is informational debugging only - do NOT change the status bits here
         */
        if (log.isDebugEnabled()) {
            List<Agent> agents = agentManager.getAllAgents();
            for (Agent agent : agents) {
                log.debug("Marking status, agent[id=" + agent.getId() + ", status=" + agent.getStatus()
                    + "] for AutoBaselineCalculationJob");
            }
        }
    }
}
