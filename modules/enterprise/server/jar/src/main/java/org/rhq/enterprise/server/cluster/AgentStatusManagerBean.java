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
