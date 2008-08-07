package org.rhq.enterprise.server.cluster.instance;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.cluster.ClusterManagerLocal;

@Stateless
public class ClusterIdentityManagerBean implements ClusterIdentityManagerLocal {
    private final Log log = LogFactory.getLog(ClusterIdentityManagerBean.class);

    private static final String RHQ_SERVER_NAME_PROPERTY = "rhq.server.name";

    @EJB
    ClusterManagerLocal clusterManager;

    public String getIdentity() {
        String clusterIdentity = System.getProperty(RHQ_SERVER_NAME_PROPERTY, "");
        if (clusterIdentity.equals("")) {
            return "localhost";
        }
        return clusterIdentity;
    }

    public List<Agent> getAgents() {
        String identity = getIdentity();
        List<Agent> results = clusterManager.getAgentsByServerName(identity);
        return results;
    }

    public Server getServer() {
        String identity = getIdentity();
        Server result = clusterManager.getServerByName(identity);
        return result;
    }

}
