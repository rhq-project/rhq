package org.rhq.enterprise.server.cluster;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.rhq.core.domain.cluster.PartitionEvent;
import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.cluster.composite.FailoverListComposite;
import org.rhq.core.domain.resource.Agent;

@Local
public interface FailoverListManagerLocal {

    /**
     * For an agent being deleted it is necessary to remove server lists for the agent.
     * <p>
     * This is primarily a test entry point as of 1.1.0 since we currently never delete agents from the database.<p>
     * 
     * @param agent
     */
    public void deleteServerListsForAgent(Agent agent);

    /**
     * For a server being deleted it is necessary to remove server lists details referencing the server. This may create a gap
     * in the server list ordinal values but that shouldn't matter, ordering is still preserved and we never access a specific
     * server by ordinal.
     * @param agent
     */
    public void deleteServerListDetailsForServer(Server server);

    /**
     * Returns the existing server list for the specified agent, if it exists, otherwise a newly generated server list is returned.
     * Note that load is balanced as best as possible without a full refresh. 
     * 
     * @param agentRegistrationToken
     * @return
     */
    FailoverListComposite getForSingleAgent(PartitionEvent event, String agentRegistrationToken);

    /**
     * <p>Performs a full repartition, re-balancing the agent load on available servers and generating new server lists
     * for every agent.  Previous server lists are deleted from the database and the new server lists are persisted.</p>
     *  
     * <p> Currently assigns to all known agents. This seems right even though some "down" agents may be dead and never come back
     * online. That is really a separate design decision and is subject to change.</p> 
     * 
     * @return
     */
    Map<Agent, FailoverListComposite> refresh(PartitionEvent event);

    /**
     * Primarily a testing entry point. In general use refresh(). 
     * @param servers
     * @param agents
     * @return
     */
    Map<Agent, FailoverListComposite> refresh(PartitionEvent event, List<Server> servers, List<Agent> agents);

}
