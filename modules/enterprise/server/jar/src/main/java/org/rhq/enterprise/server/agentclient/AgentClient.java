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
package org.rhq.enterprise.server.agentclient;

import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.content.ContentAgentService;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.inventory.ResourceFactoryAgentService;
import org.rhq.core.clientapi.agent.measurement.MeasurementAgentService;
import org.rhq.core.clientapi.agent.operation.OperationAgentService;
import org.rhq.core.clientapi.agent.support.SupportAgentService;
import org.rhq.core.domain.resource.Agent;

/**
 * The client interface to a JON agent - used by the JON server to send commands to the various agent subsystems. If you
 * want to know all the things the JON Server can command the JON agent to do, study this interface and the Agent
 * Services it provides.
 */
public interface AgentClient {
    /**
     * Returns the agent domain object which provides the information about the agent.
     *
     * @return agent information
     */
    Agent getAgent();

    /**
     * Puts this agent client in "sending mode" which enables this client to begin sending messages to the agent.
     * Calling this method will immediately begin sending messages that were flagged as guaranteed delivery and have yet
     * to be successfully sent (that is, messages spooled will be flushed out to the agent).
     */
    void startSending();

    /**
     * Stops the agent client from sending messages. Any messages sent asynchronously with guaranteed delivery will be
     * spooled and sent the next time an agent client is started.
     */
    void stopSending();

    /**
     * Returns <code>true</code> if the agent is up and this client can communicate with it. This will return <code>
     * false</code> if, for any reason, the agent cannot be pinged (which could mean the agent is down, or a network
     * problem has occurred that prohibits the client from reaching the agent).
     *
     * @param  timeoutMillis the amount of time, in milliseconds, the caller wants to wait before considering the agent
     *                       down
     *
     * @return <code>true</code> if the agent can be pinged; <code>false</code> if this client cannot communicate with
     *         the agent for some reason
     */
    boolean ping(long timeoutMillis);

    ContentAgentService getContentAgentService();

    ResourceFactoryAgentService getResourceFactoryAgentService();

    DiscoveryAgentService getDiscoveryAgentService();

    MeasurementAgentService getMeasurementAgentService();

    OperationAgentService getOperationAgentService();

    ConfigurationAgentService getConfigurationAgentService();

    SupportAgentService getSupportAgentService();
}