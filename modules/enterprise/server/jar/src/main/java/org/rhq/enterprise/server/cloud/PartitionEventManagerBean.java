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
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.FailoverList;
import org.rhq.core.domain.cloud.PartitionEvent;
import org.rhq.core.domain.cloud.PartitionEvent.ExecutionStatus;
import org.rhq.core.domain.cloud.PartitionEventDetails;
import org.rhq.core.domain.cloud.PartitionEventType;
import org.rhq.core.domain.cloud.composite.FailoverListComposite;
import org.rhq.core.domain.criteria.PartitionEventCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.authz.RequiredPermissions;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.purge.PurgeManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.QueryUtility;

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
    private final Log LOG = LogFactory.getLog(PartitionEventManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    AgentManagerLocal agentManager;

    @EJB
    FailoverListManagerLocal failoverListManager;

    @EJB
    PurgeManagerLocal purgeManager;

    @EJB
    //@IgnoreDependency
    PartitionEventManagerLocal partitionEventManager;

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public FailoverListComposite agentPartitionEvent(Subject subject, String agentName, PartitionEventType eventType,
        String eventDetail) {
        if (eventType.isCloudPartitionEvent() || (null == agentName)) {
            throw new IllegalArgumentException("Invalid agent partition event or no agent specified for event type: "
                + eventType);
        }

        Agent agent = agentManager.getAgentByName(agentName);

        if (null == agent) {
            throw new IllegalArgumentException("Can not perform partition event, agent not found with name: "
                + agentName);
        }

        PartitionEvent partitionEvent = new PartitionEvent(subject.getName(), eventType, eventDetail,
            PartitionEvent.ExecutionStatus.IMMEDIATE);
        partitionEventManager.createPartitionEvent(subject, partitionEvent);

        return failoverListManager.getForSingleAgent(partitionEvent, agent.getName());
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void createPartitionEvent(Subject subject, PartitionEvent partitionEvent) {
        entityManager.persist(partitionEvent);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Map<Agent, FailoverListComposite> cloudPartitionEvent(Subject subject, PartitionEventType eventType,
        String eventDetail) {
        if (!eventType.isCloudPartitionEvent()) {
            throw new IllegalArgumentException("Invalid cloud partition event type: " + eventType);
        }

        PartitionEvent partitionEvent = new PartitionEvent(subject.getName(), eventType, eventDetail,
            PartitionEvent.ExecutionStatus.IMMEDIATE);
        entityManager.persist(partitionEvent);

        return failoverListManager.refresh(partitionEvent);
    }

    @RequiredPermissions({ @RequiredPermission(Permission.MANAGE_SETTINGS),
        @RequiredPermission(Permission.MANAGE_INVENTORY) })
    public void cloudPartitionEventRequest(Subject subject, PartitionEventType eventType, String eventDetail) {
        if (!eventType.isCloudPartitionEvent()) {
            throw new IllegalArgumentException("Invalid cloud partition event type: " + eventType);
        }

        PartitionEvent partitionEvent = new PartitionEvent(subject.getName(), eventType, eventDetail,
            PartitionEvent.ExecutionStatus.REQUESTED);
        entityManager.persist(partitionEvent);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void auditPartitionEvent(Subject subject, PartitionEventType eventType, String eventDetail) {
        PartitionEvent partitionEvent = new PartitionEvent(subject.getName(), eventType, eventDetail,
            PartitionEvent.ExecutionStatus.AUDIT);
        entityManager.persist(partitionEvent);
    }

    @RequiredPermissions({ @RequiredPermission(Permission.MANAGE_SETTINGS),
        @RequiredPermission(Permission.MANAGE_INVENTORY) })
    public void deletePartitionEvents(Subject subject, Integer[] partitionEventIds) {
        for (int partitionEventId : partitionEventIds) {
            PartitionEvent doomedEvent = entityManager.find(PartitionEvent.class, partitionEventId);
            entityManager.remove(doomedEvent); // cascade rules should take care of this
        }
    }

    @RequiredPermissions({ @RequiredPermission(Permission.MANAGE_SETTINGS),
        @RequiredPermission(Permission.MANAGE_INVENTORY) })
    public int purgeAllEvents(Subject subject) {
        long deleteUpToTime = System.currentTimeMillis();
        return purgeManager.purgePartitionEvents(deleteUpToTime);
    }

    public void processRequestedPartitionEvents() {
        boolean completedRequest = false;

        Query query = entityManager.createNamedQuery(PartitionEvent.QUERY_FIND_BY_EXECUTION_STATUS);
        query.setParameter("executionStatus", PartitionEvent.ExecutionStatus.REQUESTED);

        @SuppressWarnings("unchecked")
        List<PartitionEvent> requestedPartitionEvents = query.getResultList();

        for (PartitionEvent next : requestedPartitionEvents) {

            // in the rare case of multiple requested partitioning events, just perform one and set
            // the rest completed. There is no sense in repartitioning multiple times on the same data.
            if (!completedRequest) {
                if (!next.getEventType().isCloudPartitionEvent()) {
                    LOG.warn("Invalid cloud partition event type: " + next.getEventType());
                }

                try {
                    failoverListManager.refresh(next);
                    completedRequest = true;
                } catch (Exception e) {
                    LOG.warn("Failed requested partition event. Setting COMPLETED to avoid repeated failure: " + e);
                }
            }

            next.setExecutionStatus(PartitionEvent.ExecutionStatus.COMPLETED);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PartitionEvent getPartitionEvent(Subject subject, int partitionEventId) {
        PartitionEvent event = entityManager.find(PartitionEvent.class, partitionEventId);
        return event;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<PartitionEvent> getPartitionEvents(Subject subject, PartitionEventType type,
        ExecutionStatus status, String details, PageControl pageControl) {
        pageControl.initDefaultOrderingField("pe.ctime", PageOrdering.DESC);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, PartitionEvent.QUERY_FIND_ALL,
            pageControl);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, PartitionEvent.QUERY_FIND_ALL);

        details = QueryUtility.formatSearchParameter(details);

        query.setParameter("type", type);
        countQuery.setParameter("type", type);
        query.setParameter("status", status);
        countQuery.setParameter("status", status);
        query.setParameter("details", details);
        countQuery.setParameter("details", details);
        query.setParameter("escapeChar", QueryUtility.getEscapeCharacter());
        countQuery.setParameter("escapeChar", QueryUtility.getEscapeCharacter());

        List<PartitionEvent> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<PartitionEvent>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    @RequiredPermissions({ @RequiredPermission(Permission.MANAGE_SETTINGS),
        @RequiredPermission(Permission.MANAGE_INVENTORY) })
    public PageList<PartitionEventDetails> getPartitionEventDetails(Subject subject, int partitionEventId,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("ped.id", PageOrdering.ASC);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PartitionEventDetails.QUERY_FIND_BY_EVENT_ID, pageControl);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            PartitionEventDetails.QUERY_FIND_BY_EVENT_ID);

        query.setParameter("eventId", partitionEventId);
        countQuery.setParameter("eventId", partitionEventId);

        List<PartitionEventDetails> detailsList = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<PartitionEventDetails>(detailsList, (int) count, pageControl);
    }

    @RequiredPermissions({ @RequiredPermission(Permission.MANAGE_SETTINGS),
        @RequiredPermission(Permission.MANAGE_INVENTORY) })
    public PageList<PartitionEvent> findPartitionEventsByCriteria(Subject subject, PartitionEventCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<PartitionEvent> runner = new CriteriaQueryRunner<PartitionEvent>(criteria, generator,
            entityManager);
        return runner.execute();
    }
}
