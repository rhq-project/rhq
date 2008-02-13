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
package org.rhq.enterprise.server.core;

import java.util.List;
import javax.ejb.Local;
import org.jetbrains.annotations.NotNull;
import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.agentclient.AgentClient;

/**
 * Local interface to the {@link AgentManagerBean} SLSB.
 *
 * @author John Mazzitelli
 */
@Local
public interface AgentManagerLocal {
    /**
     * Persists a new agent.
     *
     * @param agent
     */
    void createAgent(Agent agent);

    /**
     * Updates an existing agent.
     *
     * @param  agent the agent to be updated, with the new data in it
     *
     * @return an updated (attached) copy of the passed-in agent
     */
    Agent updateAgent(Agent agent);

    /**
     * Deletes an existing agent.
     *
     * @param agent
     */
    void deleteAgent(Agent agent);

    /**
     * Returns an agent client that can be used to send commands to the specified JON agent.
     *
     * @param  agent a JON agent
     *
     * @return an agent client that can be used to send commands to the specified JON agent
     */
    @NotNull
    AgentClient getAgentClient(@NotNull
    Agent agent);

    /**
     * Returns an agent client that can be used to send commands to the JON agent that managed the specified resource.
     *
     * @param  resourceId the ID of the resource whose agent is to be returned
     *
     * @return an agent client that can be used to send commands to the JON agent that manages the resource
     */
    @NotNull
    AgentClient getAgentClient(int resourceId);

    /**
     * Returns a collection of all agents currently in inventory.
     *
     * @return list of all known agents in inventory
     */
    List<Agent> getAllAgents();

    /**
     * Returns the total number of agents that are in inventory.
     *
     * @return total agent count
     */
    int getAgentCount();

    /**
     * Given an agent name, this will look up and return the {@link Agent} with that name. If no agent with the given
     * name exists, <code>null</code> is returned.
     *
     * @param  agentName
     *
     * @return the agent whose name matches the given name; <code>null</code> if there is no agent with the given name
     */
    Agent getAgentByName(String agentName);

    /**
     * Given an agent token string, this will look up and return the {@link Agent} associated with that token. If the
     * given token is invalid, <code>null</code> is returned.
     *
     * @param  token the agent token
     *
     * @return the agent whose agent token matches the given token; <code>null</code> if there is no agent with the
     *         given token
     */
    Agent getAgentByAgentToken(String token);

    /**
     * Given an agent's address and port, this will look up and return the {@link Agent} associated with that address
     * and port. If no agent is found, <code>null</code> is returned.
     *
     * @param  address the address that the agent is bound to
     * @param  port    the port at the given address that the agent is listening on
     *
     * @return the agent to be known at the given address and port; <code>null</code> if there is no agent with the
     *         given token
     */
    Agent getAgentByAddressAndPort(String address, int port);

    /**
     * Given a resource ID, this will return the agent responsible for servicing that resource.
     *
     * @param  resourceId
     *
     * @return the agent that services the resource, or <code>null</code> if the resource ID was invalid
     */
    Agent getAgentByResourceId(int resourceId);

    /**
     * This method is called whenever an agent is going down.
     *
     * <p>This will usually be triggered when an agent explicitly tells us that it is shutting down. See
     * {@link CoreServerService#agentIsShuttingDown(String)}.</p>
     *
     * @param agentName the name of the agent that is going down
     */
    void agentIsShuttingDown(String agentName);

    /**
     * This method should only be called when it is confirmed that an agent is alive. This can perform some tasks that
     * can only be done when it is known that the agent is up.
     *
     * @param agent the agent that is confirmed alive and well
     */
    void agentIsAlive(Agent agent);

    /**
     * Call this method to see if there are agents that we might suspect are down. This is periodically called via our
     * scheduled job {@link org.rhq.enterprise.server.scheduler.jobs.CheckForSuspectedAgentsJob}.
     */
    void checkForSuspectAgents();
}