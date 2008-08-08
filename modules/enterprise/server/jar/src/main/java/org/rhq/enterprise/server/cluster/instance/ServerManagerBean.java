package org.rhq.enterprise.server.cluster.instance;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.cluster.AgentStatusManagerLocal;
import org.rhq.enterprise.server.cluster.ClusterManagerLocal;

@Stateless
public class ServerManagerBean implements ServerManagerLocal {
    private final Log log = LogFactory.getLog(ServerManagerBean.class);

    private static final String RHQ_SERVER_NAME_PROPERTY = "rhq.server.high-availability.name";

    @EJB
    ClusterManagerLocal clusterManager;

    @EJB
    AgentStatusManagerLocal agentStatusManager;

    public String getIdentity() {
        String identity = System.getProperty(RHQ_SERVER_NAME_PROPERTY, "");
        if (identity.equals("")) {
            return "localhost";
        }
        return identity;
    }

    public List<Agent> getAgents() {
        String identity = getIdentity();
        List<Agent> results = clusterManager.getAgentsByServerName(identity);
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<Agent> getAgentsWithStatus() {
        List<Agent> results = agentStatusManager.getAgentsWithStatusForServer(getIdentity());
        return results;
    }

    public Server getServer() {
        String identity = getIdentity();
        Server result = clusterManager.getServerByName(identity);
        return result;
    }

}
