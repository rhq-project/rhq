package org.rhq.enterprise.server.cluster;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.resource.Agent;

@Local
public interface ClusterManagerLocal {

    void createDefaultServerIfNecessary();

    List<Agent> getAgentsByServerName(String serverName);

    Server getServerByName(String serverName);

    int getServerCount();
}
