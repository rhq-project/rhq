package org.rhq.enterprise.server.cluster;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.cluster.composite.FailoverListComposite;
import org.rhq.core.domain.cluster.composite.FailoverListComposite.ServerEntry;

@Stateless
public class FailoverListManagerBean implements FailoverListManagerLocal {

    @EJB
    ClusterManagerLocal clusterManager;

    @Override
    public FailoverListComposite getForSingleAgent(String agentRegistrationToken) {
        /* 
         * dummy implementation that return a simple FailoverList
         * until the distribution algorithm is written 
         */
        List<Server> servers = clusterManager.getAllServers();
        List<ServerEntry> serverEntries = new ArrayList<ServerEntry>();
        for (Server next : servers) {
            serverEntries.add(next.getServerEntry());
        }
        FailoverListComposite results = new FailoverListComposite(serverEntries);
        return results;
    }

}
