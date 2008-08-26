package org.rhq.enterprise.server.cluster;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.cluster.composite.FailoverListComposite;
import org.rhq.core.domain.resource.Agent;

@Local
public interface FailoverListManagerLocal {
    FailoverListComposite getForSingleAgent(String agentRegistrationToken);

    Map<Agent, FailoverListComposite> getForAllAgents();

    Map<Agent, FailoverListComposite> getForAgents(List<Server> servers, List<Agent> agents);
}
