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
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;

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
            server.setSecyrePort(7443);
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

}
