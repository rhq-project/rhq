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
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.IgnoreDependency;

import org.jboss.annotation.ejb.PoolClass;
import org.jboss.ejb3.StrictMaxPool;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.FailoverListDetails;
import org.rhq.core.domain.cloud.PartitionEventType;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.composite.ServerWithAgentCountComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.server.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This class manages and reports information about the RHQ Server Cloud as a whole.
 * It does not discern which server is which, and can be called from any server in 
 * the cloud and will operate identically the same results.
 * 
 * @author Joseph Marques
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
public class CloudManagerBean implements CloudManagerLocal {
    private final Log log = LogFactory.getLog(CloudManagerBean.class);

    // A time sufficient to determine whether a server is down.  Can be based on the initial delay set for the server instance
    // job updating the server mtimes. See StartupServlet. 
    private static final long SERVER_DOWN_INTERVAL = 1000L * 2 * 60;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private CloudManagerLocal cloudManager;

    @EJB
    private FailoverListManagerLocal failoverListManager;

    @EJB
    private PartitionEventManagerLocal partitionEventManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    @IgnoreDependency
    private ServerManagerLocal serverManager;

    public List<Agent> getAgentsByServerName(String serverName) {
        Server server = cloudManager.getServerByName(serverName);
        List<Agent> agents = server.getAgents();
        agents.size(); // iterating over this collection out of a transactional boundaries will throw LazyInitExceptions
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
    public List<Server> getAllCloudServers() {
        Query query = entityManager.createNamedQuery(Server.QUERY_FIND_ALL_CLOUD_MEMBERS);
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

    public int getNormalServerCount() {
        Query query = PersistenceUtility.createCountQuery(entityManager, Server.QUERY_FIND_ALL_NORMAL_CLOUD_MEMBERS);

        try {
            long serverCount = (Long) query.getSingleResult();
            return (int) serverCount;
        } catch (NoResultException nre) {
            log.debug("Could not get count of normal cloud instances, returning 0...");
            return 0;
        }
    }

    public void deleteServers(Integer[] serverIds) throws CloudManagerException {
        if (serverIds == null) {
            return;
        }

        for (Integer nextServerId : serverIds) {
            cloudManager.deleteServer(nextServerId);
        }
    }

    public void deleteServer(Integer serverId) throws CloudManagerException {
        try {
            Server server = entityManager.find(Server.class, serverId);

            if (Server.OperationMode.NORMAL == server.getOperationMode()) {
                throw new CloudManagerException("Could not delete server " + server.getName()
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
            throw new CloudManagerException("Could not delete server[id=" + serverId + "]: " + e.getMessage(), e);
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
                        // ignore if there is no change
                        continue;
                    }

                    // Audit servers being set to DOWN since the state change can't be reported any other way. Servers
                    // be set to any other mode will be handled when the cloud job established the current operating mode.
                    if (Server.OperationMode.DOWN == mode) {
                        String audit = server.getName() + ": " + server.getOperationMode().name() + " --> " + mode;

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

    public void markStaleServersDown(Subject subject) {
        if (!authorizationManager.isOverlord(subject)) {
            throw new IllegalArgumentException("The markStaleServersDown method must be called by the overlord");
        }

        long staleTime = System.currentTimeMillis() - SERVER_DOWN_INTERVAL;

        String serverName = null;
        try {
            serverName = serverManager.getIdentity();
            if (log.isDebugEnabled()) {
                log.debug(serverName + " is marking stale servers DOWN");
            }
        } catch (Exception e) {
            log.error("Could not determine which instance is marking stale servers DOWN");
        }
        Query query = entityManager.createNamedQuery(Server.QUERY_UPDATE_SET_STALE_DOWN);
        query.setParameter("downMode", Server.OperationMode.DOWN);
        query.setParameter("normalMode", Server.OperationMode.NORMAL);
        query.setParameter("staleTime", staleTime);
        query.setParameter("thisServerName", serverName); // might be null
        int resultCount = query.executeUpdate();

        if (log.isDebugEnabled()) {
            log.debug(String.valueOf(resultCount) + " stale servers were marked DOWN");
        }

        // Perform requested partition events. Note that we only need to execute one cloud partition
        // regardless of the number of pending requests, as the work would be duplicated.
        partitionEventManager.processRequestedPartitionEvents();
    }
}
