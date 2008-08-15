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

import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;

/**
 * @author Joseph Marques
 */

@Stateless
public class ClusterManagerBean implements ClusterManagerLocal {
    private final Log log = LogFactory.getLog(ClusterManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    ClusterManagerLocal clusterManager;

    public void createDefaultServerIfNecessary() {
        int serverCount = clusterManager.getServerCount();
        if (serverCount == 0) {
            Server server = new Server();
            server.setName("localhost");
            server.setAddress("localhost");
            server.setPort(7080);
            server.setSecurePort(7443);
            server.setOperationMode(Server.OperationMode.NORMAL);
            entityManager.persist(server);
        }
    }

    public List<Agent> getAgentsByServerName(String serverName) {
        Server server = clusterManager.getServerByName(serverName);
        List<Agent> agents = server.getAgents();
        return agents;
    }

    public Server getServerByName(String serverName) {
        Query query = entityManager.createNamedQuery(Server.QUERY_FIND_BY_NAME);
        query.setParameter("name", serverName);

        try {
            Server server = (Server) query.getSingleResult();
            return server;
        } catch (NoResultException nre) {
            log.debug("Server[name=" + serverName + "] not found, returning null...");
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Server> getAllServers() {
        Query query = entityManager.createNamedQuery(Server.QUERY_FIND_ALL);
        List<Server> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public PageList<Server> getAllServersAsPageList(PageControl pc) {
        pc.initDefaultOrderingField("s.name");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, Server.QUERY_FIND_ALL, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, Server.QUERY_FIND_ALL);

        List<Server> results = query.getResultList();
        long count = (Long) countQuery.getSingleResult();

        return new PageList<Server>(results, (int) count, pc);
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

    public void updateServerMode(Integer[] serverIds, Server.OperationMode mode) {

        if (serverIds.length > 0) {
            try {
                for (Integer id : serverIds) {
                    Server server = entityManager.find(Server.class, id);
                    server.setOperationMode(mode);
                }
            } catch (Exception e) {
                log.debug("Failed to update HA server modes: " + e);
            }
        }
    }

}
