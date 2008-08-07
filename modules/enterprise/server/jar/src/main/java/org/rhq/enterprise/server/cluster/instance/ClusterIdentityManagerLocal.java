package org.rhq.enterprise.server.cluster.instance;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.resource.Agent;

@Local
public interface ClusterIdentityManagerLocal {

    String getIdentity();

    List<Agent> getAgents();

    Server getServer();
}
