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
package org.rhq.enterprise.server.core;

import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.composite.AgentLastAvailabilityReportComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.cluster.FailoverListManagerLocal;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceMBean;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceUtil;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.util.concurrent.AvailabilityReportSerializer;

/**
 * Manages the access to {@link Agent} objects.
 *
 * @author John Mazzitelli
 */
@ExcludeDefaultInterceptors
@Stateless
public class AgentManagerBean implements AgentManagerLocal {
    private final Log log = LogFactory.getLog(AgentManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    @IgnoreDependency
    private FailoverListManagerLocal failoverListManager;

    @EJB
    @IgnoreDependency
    private AvailabilityManagerLocal availabilityManager;

    public void createAgent(Agent agent) {
        entityManager.persist(agent);

        log.debug("Persisted new agent: " + agent);
    }

    public void deleteAgent(Agent agent) {
        agent = entityManager.find(Agent.class, agent.getId());
        failoverListManager.deleteServerListsForAgent(agent);
        entityManager.remove(agent);

        ServerCommunicationsServiceMBean bootstrap = ServerCommunicationsServiceUtil.getService();
        bootstrap.destroyKnownAgentClient(agent);

        log.info("Removed agent: " + agent);
    }

    public Agent updateAgent(Agent agent) {
        agent = entityManager.merge(agent);
        log.debug("Updated agent: " + agent);
        return agent;
    }

    public AgentClient getAgentClient(Agent agent) {
        AgentClient client = null;

        try {
            ServerCommunicationsServiceMBean bootstrap = ServerCommunicationsServiceUtil.getService();
            client = bootstrap.getKnownAgentClient(agent);

            // We assume the caller is asking for a client so it can send the agent messages,
            // so let's start the sender automatically for the caller so it doesn't need to remember to do it
            client.startSending();
        } catch (Throwable t) {
            log.debug("Could not get agent client for " + agent);
        }

        return client;
    }

    public AgentClient getAgentClient(int resourceId) {
        Agent agent = getAgentByResourceId(resourceId);

        if (agent == null) {
            throw new RuntimeException("Resource [" + resourceId + "] does not exist or has no agent assigned");
        }

        return getAgentClient(agent);
    }

    public void agentIsShuttingDown(String agentName) {
        Agent downedAgent = getAgentByName(agentName);

        ServerCommunicationsServiceMBean server_bootstrap = ServerCommunicationsServiceUtil.getService();
        server_bootstrap.removeDownedAgent(downedAgent.getRemoteEndpoint());
        log.info("Agent with name [" + agentName + "] just went down");

        return;
    }

    public void agentIsAlive(Agent agent) {
        // This method needs to be very fast.  It is currently designed to be called from
        // the security token command authenticator.  It calls this method every time
        // a message comes in and the auth token needs to be verified.  This method takes
        // a detached agent rather than an agent name because I don't want to waste a round
        // trip looking up the agent from the DB when I already know the caller (the
        // authenticator) just got the agent object.
        // When the authenticator calls us, it means it just got a message from the agent,
        // so we can use this to our benefit.  Since we got a message from the given agent,
        // we know it is up!  So this is a simple way for us to detect agents coming online
        // without us having to periodically poll each and every agent.

        try {
            ServerCommunicationsServiceMBean server_comm = ServerCommunicationsServiceUtil.getService();
            server_comm.addStartedAgent(agent);
        } catch (Exception e) {
            log.info("Cannot flag the agent as started for some reason", e);
        }

        return;
    }

    @SuppressWarnings("unchecked")
    public void checkForSuspectAgents() {
        log.debug("Checking to see if there are agents that we suspect are down...");

        // TODO [mazz]: make this configurable via SystemManager bean
        long maximumQuietTimeAllowed = 1000L * 60 * 2;
        try {
            String propStr = System.getProperty("rhq.server.agent-max-quiet-time-allowed");
            if (propStr != null) {
                maximumQuietTimeAllowed = Long.parseLong(propStr);
            }
        } catch (Exception e) {
        }

        List<AgentLastAvailabilityReportComposite> records;

        long nowEpoch = System.currentTimeMillis();

        Query q = entityManager.createNamedQuery(Agent.QUERY_FIND_ALL_SUSPECT_AGENTS);
        q.setParameter("dateThreshold", new Date(nowEpoch - maximumQuietTimeAllowed), TemporalType.TIMESTAMP);
        records = q.getResultList();

        ServerCommunicationsServiceMBean serverComm = null;

        for (AgentLastAvailabilityReportComposite record : records) {
            Date lastReport = record.getLastAvailabilityReport();

            long timeSinceLastReport = (nowEpoch - lastReport.getTime());

            // Only show this message a few times so we do not flood the log with the same message if the agent is down a long time
            // we show it as soon as we detect it going down (within twice max quiet time allowed) or we show it
            // after every 6th hour it's detected to be down (again, within twice max quiet time).  Effectively, you'll see
            // this message appear about 4 times per 6-hours for a downed agent if using the default max quiet time.
            // Note that in here we also make sure the agent client is shutdown. We do it here because, even if the agent
            // was already backfilled, we still want to do this in case somehow the client was started again.
            if ((timeSinceLastReport % 21600000L) < (maximumQuietTimeAllowed * 2L)) {
                log.warn("Have not heard from agent [" + record.getAgentName() + "] since ["
                    + record.getLastAvailabilityReport() + "]. Will be backfilled since we suspect it is down");

                if (serverComm == null) {
                    serverComm = ServerCommunicationsServiceUtil.getService();
                }

                serverComm.removeDownedAgent(record.getRemoteEndpoint());
            }

            // we can avoid doing this over and over again for agents that are down a long time by seeing if it's
            // already backfilled.  Note that we do not log the above warn message down here in this if-statement,
            // because I think we still want to log that we think an agent is down periodically.
            // If it turns out we do not want to be that noisy, just move that warn message down in here so we only ever log
            // about a downed agent once, at the time it is first backfilled.
            if (!availabilityManager.isAgentBackfilled(record.getAgentName())) {
                // make sure we lock out all processing of any availability reports that might come our way to avoid concurrency problems
                AvailabilityReportSerializer.getSingleton().lock(record.getAgentName());
                try {
                    availabilityManager.setAllAgentResourceAvailabilities(record.getAgentId(), AvailabilityType.DOWN);
                } finally {
                    AvailabilityReportSerializer.getSingleton().unlock(record.getAgentName());
                }
            }
        }

        log.debug("Finished checking for suspected agents");

        return;
    }

    @SuppressWarnings("unchecked")
    public List<Agent> getAllAgents() {
        return entityManager.createNamedQuery(Agent.QUERY_FIND_ALL).getResultList();
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<Agent> getAgentsByServer(Subject subject, Integer serverId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("a.name");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, Agent.QUERY_FIND_BY_SERVER, pageControl);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, Agent.QUERY_FIND_BY_SERVER);

        query.setParameter("serverId", serverId);
        countQuery.setParameter("serverId", serverId);

        long count = (Long) countQuery.getSingleResult();
        List<Agent> results = query.getResultList();

        return new PageList<Agent>(results, (int) count, pageControl);
    }

    public int getAgentCount() {
        return ((Number) entityManager.createNamedQuery(Agent.QUERY_COUNT_ALL).getSingleResult()).intValue();
    }

    public Agent getAgentByAgentToken(String token) {
        Agent agent;

        try {
            Query query = entityManager.createNamedQuery(Agent.QUERY_FIND_BY_AGENT_TOKEN);
            query.setParameter("agentToken", token);
            agent = (Agent) query.getSingleResult();
        } catch (NoResultException e) {
            log.debug("Failed to lookup agent - none exist with token [" + token + "] : " + e);
            agent = null;
        }

        return agent;
    }

    public Agent getAgentByName(String agentName) {
        Agent agent;

        try {
            Query query = entityManager.createNamedQuery(Agent.QUERY_FIND_BY_NAME);
            query.setParameter("name", agentName);
            agent = (Agent) query.getSingleResult();
        } catch (NoResultException e) {
            log.debug("Failed to lookup agent - none exist with name [" + agentName + "] : " + e);
            agent = null;
        }

        return agent;
    }

    public Agent getAgentByID(int agentId) {
        Agent agent = entityManager.find(Agent.class, agentId);
        return agent;
    }

    public Agent getAgentByAddressAndPort(String address, int port) {
        Agent agent;
        try {
            Query query = entityManager.createNamedQuery(Agent.QUERY_FIND_BY_ADDRESS_AND_PORT);
            query.setParameter("address", address);
            query.setParameter("port", port);
            agent = (Agent) query.getSingleResult();
        } catch (NoResultException e) {
            log.debug("Agent not found with address/port: " + address + "/" + port);
            agent = null;
        }

        return agent;
    }

    public Agent getAgentByResourceId(int resourceId) {
        Agent agent;

        try {
            Query query = entityManager.createNamedQuery(Agent.QUERY_FIND_BY_RESOURCE_ID);
            query.setParameter("resourceId", resourceId);
            agent = (Agent) query.getSingleResult();
        } catch (NoResultException e) {
            log.debug("Failed to lookup agent for resource with ID of [" + resourceId + "] : " + e);
            agent = null;
        }

        return agent;
    }
}