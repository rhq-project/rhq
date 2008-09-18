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
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cluster.FailoverListDetails;
import org.rhq.core.domain.cluster.PartitionEventType;
import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.cluster.composite.ServerWithAgentCountComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.cluster.instance.ServerManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This class manages and reports information about the RHQ Server Cloud as a whole.
 * It does not discern which server is which, and can be called from any server in 
 * the cloud and will operate identically the same results.
 * 
 * @author Joseph Marques
 */
@Stateless
public class ClusterManagerBean implements ClusterManagerLocal {
    private final Log log = LogFactory.getLog(ClusterManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    ClusterManagerLocal clusterManager;

    @EJB
    FailoverListManagerLocal failoverListManager;

    @EJB
    PartitionEventManagerLocal partitionEventManager;

    @EJB
    @IgnoreDependency
    ServerManagerLocal serverManager;

    public List<Agent> getAgentsByServerName(String serverName) {
        Server server = clusterManager.getServerByName(serverName);
        List<Agent> agents = server.getAgents();
        return agents;
    }

    public Server getServerById(int serverId) {
        Server server = entityManager.find(Server.class, serverId);
        return server;
    }

    public Server getServerByName(String serverName) {
        Query query = entityManager.createNamedQuery(Server.QUERY_FIND_BY_NAME);
        query.setParameter("name", serverName);

        try {
            Server server = (Server) query.getSingleResult();
            return server;
        } catch (NoResultException nre) {
            log.info("Server[name=" + serverName + "] not found, returning null...");
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Server> getServersByOperationMode(Server.OperationMode mode) {
        Query query = entityManager.createNamedQuery(Server.QUERY_FIND_BY_OPERATION_MODE);
        query.setParameter("mode", mode);
        List<Server> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<Server> getAllServers() {
        Query query = entityManager.createNamedQuery(Server.QUERY_FIND_ALL);
        List<Server> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<ServerWithAgentCountComposite> getServerComposites(Subject subject, PageControl pc) {
        pc.initDefaultOrderingField("s.name");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, Server.QUERY_FIND_ALL_COMPOSITES, pc);

        List<ServerWithAgentCountComposite> results = query.getResultList();
        int count = getServerCount();

        return new PageList<ServerWithAgentCountComposite>(results, count, pc);
    }

    public int getServerCount() {
        Query query = PersistenceUtility.createCountQuery(entityManager, Server.QUERY_FIND_ALL);

        try {
            long serverCount = (Long) query.getSingleResult();
            return (int) serverCount;
        } catch (NoResultException nre) {
            log.debug("Could not get count of cloud instances, returning 0...");
            return 0;
        }
    }

    public void deleteServers(Integer[] serverIds) throws ClusterManagerException {
        if (serverIds == null) {
            return;
        }

        for (Integer nextServerId : serverIds) {
            clusterManager.deleteServer(nextServerId);
        }
    }

    public void deleteServer(Integer serverId) throws ClusterManagerException {
        try {
            Server server = entityManager.find(Server.class, serverId);

            if (Server.OperationMode.NORMAL == server.getOperationMode()) {
                throw new ClusterManagerException("Could not delete server " + server.getName()
                    + ". Server must be down or in maintenance mode. Current operating mode is: "
                    + server.getOperationMode().name());
            }

            // Delete any server list entries referencing this server
            failoverListManager.deleteServerListDetailsForServer(serverId);

            // Delete any agent references to this server
            Query query = entityManager.createNamedQuery(Agent.QUERY_REMOVE_SERVER_REFERENCE);
            query.setParameter("serverId", serverId);
            query.executeUpdate();

            // Then, delete the server
            query = entityManager.createNamedQuery(Server.QUERY_DELETE_BY_ID);
            query.setParameter("serverId", serverId);
            query.executeUpdate();

            entityManager.flush();
            entityManager.clear();

            log.info("Removed server " + server);

            // Now, request a cloud repartitioning due to the server removal
            partitionEventManager.cloudPartitionEventRequest(LookupUtil.getSubjectManager().getOverlord(),
                PartitionEventType.SERVER_DELETION, server.getName());

        } catch (Exception e) {
            throw new ClusterManagerException("Could not delete server[id=" + serverId + "]: " + e.getMessage(), e);
        }
    }

    public void updateServerMode(Integer[] serverIds, Server.OperationMode mode) {
        if (serverIds == null) {
            return;
        }

        if (mode == null) {
            throw new IllegalArgumentException("mode can not be null");
        }

        if (serverIds.length > 0) {
            try {
                for (Integer id : serverIds) {
                    Server server = entityManager.find(Server.class, id);

                    if (server.getOperationMode() == mode) {
                        // don't bother doing things that are no-ops, we don't want to create 
                        // events that might instigate unnecessary cloud repartitions
                        continue;
                    }
                    // depending on the state change we may need to partition our agent load, otherwise audit the change
                    // TODO (jshaughn) Note that we can't currently distinguish whether a MM server is up or down. So,
                    // we have to assume it is up.  The resulting partition request will then incorporate the DOWN server.
                    String audit = server.getName() + ": " + server.getOperationMode().name() + " --> " + mode;

                    if (Server.OperationMode.NORMAL == mode) {
                        if (Server.OperationMode.MAINTENANCE == server.getOperationMode()) {
                            // MAINTENANCE --> NORMAL, cloud event
                            partitionEventManager.cloudPartitionEventRequest(LookupUtil.getSubjectManager()
                                .getOverlord(), PartitionEventType.OPERATION_MODE_CHANGE, audit);
                        } else {
                            // DOWN --> NORMAL, audit
                            partitionEventManager.auditPartitionEvent(LookupUtil.getSubjectManager().getOverlord(),
                                PartitionEventType.OPERATION_MODE_CHANGE, audit);
                        }
                    } else {
                        // if we're not changing *to* NORMAL, then we should at least audit what's happening
                        partitionEventManager.auditPartitionEvent(LookupUtil.getSubjectManager().getOverlord(),
                            PartitionEventType.OPERATION_MODE_CHANGE, audit);
                    }
                    server.setOperationMode(mode);
                }
            } catch (Exception e) {
                log.debug("Failed to update HA server modes: " + e);
            }
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Server updateServer(Subject subject, Server server) {
        return entityManager.merge(server);
    }

    public PageList<FailoverListDetails> getFailoverListDetailsByAgentId(int agentId, PageControl pc) {
        pc.initDefaultOrderingField("fld.ordinal");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            FailoverListDetails.QUERY_GET_VIA_AGENT_ID_WITH_SERVERS, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            FailoverListDetails.QUERY_GET_VIA_AGENT_ID);

        query.setParameter("agentId", agentId);
        countQuery.setParameter("agentId", agentId);

        @SuppressWarnings("unchecked")
        List<FailoverListDetails> list = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<FailoverListDetails>(list, (int) count, pc);
    }
}
