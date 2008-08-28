package org.rhq.enterprise.server.cluster;

import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cluster.PartitionEvent;
import org.rhq.core.domain.cluster.PartitionEventDetails;
import org.rhq.core.domain.cluster.PartitionEventType;
import org.rhq.core.domain.cluster.composite.FailoverListComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.core.AgentManagerLocal;

@Stateless
public class PartitionEventManagerBean implements PartitionEventManagerLocal {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    AgentManagerLocal agentManager;

    @EJB
    PartitionEventManagerLocal partitionEventManager;

    @EJB
    FailoverListManagerLocal failoverListManager;

    public FailoverListComposite agentPartitionEvent(Subject subject, String agentToken, PartitionEventType eventType) {
        if (eventType.isCloudPartitionEvent() || (null == agentToken)) {
            throw new IllegalArgumentException("Invalid agent partition event or no agent specified for event type: "
                + eventType);
        }

        Agent agent = agentManager.getAgentByAgentToken(agentToken);

        if (null == agent) {
            throw new IllegalArgumentException("Can not perform partition event, agent not found with token: "
                + agentToken);
        }

        PartitionEvent partitionEvent = new PartitionEvent(subject.getName(), eventType);
        entityManager.persist(partitionEvent);

        return failoverListManager.getForSingleAgent(partitionEvent, agent.getAgentToken());
    }

    public Map<Agent, FailoverListComposite> cloudPartitionEvent(Subject subject, PartitionEventType eventType) {
        if (!eventType.isCloudPartitionEvent()) {
            throw new IllegalArgumentException("Invalid cloud partition event type: " + eventType);
        }

        PartitionEvent partitionEvent = new PartitionEvent(subject.getName(), eventType);
        entityManager.persist(partitionEvent);

        return failoverListManager.refresh(partitionEvent);
    }

    public void deletePartitionEvent(PartitionEvent event) {
        event = entityManager.find(PartitionEvent.class, event.getId());
        for (PartitionEventDetails next : event.getEventDetails()) {
            entityManager.remove(next);
        }
        entityManager.remove(event);
    }

}
