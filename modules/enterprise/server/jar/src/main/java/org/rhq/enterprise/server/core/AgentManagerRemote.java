package org.rhq.enterprise.server.core;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.AgentCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageList;

/**
 * Remote agent management.
 */
@Remote
public interface AgentManagerRemote {

    /**
     * Deletes an existing agent by subject, if there is no platform resource for this agent.
     * This method is primarily for deleting agents that may have incompletely registered.
     *
     * Subject needs MANAGE_SETTINGS permissions.
     *
     * @param subject caller
     * @param agent agent object.
     */
    void deleteAgent(Subject subject, Agent agent);

    /**
     * Fetches the agents based on provided criteria.
     *
     * Subject needs MANAGE_SETTINGS and MANAGE_INVENTORY permissions.
     *
     * @param subject caller
     * @param criteria the criteria
     * @return list of agents
     */
    PageList<Agent> findAgentsByCriteria(Subject subject, AgentCriteria criteria);

}
