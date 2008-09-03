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

import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cluster.FailoverList;
import org.rhq.core.domain.cluster.PartitionEvent;
import org.rhq.core.domain.cluster.PartitionEventDetails;
import org.rhq.core.domain.cluster.PartitionEventType;
import org.rhq.core.domain.cluster.composite.FailoverListComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.core.AgentManagerLocal;

/**
 * This session beans acts as the underlying implementation the distribution algorithm will
 * interact.  The distribution algorithm runs as a result of various changes in the system 
 * including but not limited to: newly registering agents, currently connecting agents, cloud 
 * membership changes (server added/removed), and redistributions according to agent load. Each 
 * of these changes is captured as a {@link PartitionEvent}, and the distribution will either 
 * need to generated a single (or a set of) {@link FailoverList} objects that are sent down to 
 * the connected agents.  The agents then use these lists to determine which server to fail over 
 * to, if their primary server is unreachable and/or goes down.
 * 
 * @author Joseph Marques
 * @author Jay Shaughnessy
 */
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
