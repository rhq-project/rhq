/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.clientapi.server.core;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.rhq.core.domain.cloud.composite.FailoverListComposite;
import org.rhq.core.domain.plugin.Plugin;

/**
 * The interface to a JON server's core administrative functions used by remote agents.
 */
public interface CoreServerService {
    /**
     * Register an agent with this server. The agent may or may not already exist. An agent can re-register if it
     * changes its remote endpoint for example (in the case when its port changes).
     *
     * @param  agentRegistrationRequest
     *
     * @return the resultant registration information
     *
     * @throws AgentRegistrationException if the agent's registration request was rejected
     * @throws AgentNotSupportedException if the agent is not supported by this server
     */
    AgentRegistrationResults registerAgent(AgentRegistrationRequest agentRegistrationRequest)
        throws AgentRegistrationException, AgentNotSupportedException;

    /**
     * Connect an agent with this server.  This is the server that will process all of this agent's
     * activity. The agent must already be registered.
     *
     * @param connectRequest
     * 
     * @return the results, which includes the current time in epoch millis of the server
     *
     * @throws AgentRegistrationException if the agent is not registered
     * @throws AgentNotSupportedException if the agent is not supported by this server
     */
    ConnectAgentResults connectAgent(ConnectAgentRequest connectRequest) throws AgentRegistrationException,
        AgentNotSupportedException;

    /**
     * Get a list of the registered plugins managed in the server.
     *
     * @return the list of the plugin information describing the more recent plugins the server is managing
     */
    List<Plugin> getLatestPlugins();

    /**
     * Return a stream containing the contents of a plugin jar.
     *
     * @param  pluginName The name of the plugin
     *
     * @return a stream by which the caller can use to pull down the contents of the requested plugin jar
     */
    InputStream getPluginArchive(String pluginName);

    /**
     * Returns a stream that contains the given file contents. The file is located relative to a server-side defined
     * location. If the characters ".." exist anywhere in the given file name, a runtime exception is thrown - you must
     * ask for a file that is located under a server-side defined location. The <code>file</code> may specify one or
     * more subdirectories within its relative path.
     *
     * @param  file the file to download
     *
     * @return a stream that contains the file's data.
     */
    InputStream getFileContents(String file);

    /**
     * When an agent is shutting down, it will notify the server by calling this method.
     *
     * @param agentName the name of the agent that is shutting down
     */
    void agentIsShuttingDown(String agentName);

    /**
     * Returns the current server list for the agent. This will return null if no server list exists
     * for the agent.
     * 
     * @param agentName the name of the agent requesting the server list
     * 
     * @return The active server list for the agent
     * @throws IllegalArgumentException if the agentName does not match a registered agent.
     */
    FailoverListComposite getFailoverList(String agentName);

    /**
     * Ping the server.
     * 
     * @param request Any optional action or data requests.
     * @return Any response data for the requested data or actions.
     */
    PingRequest ping(PingRequest request);

    /**
     * Returns public agent update endpoint address.
     *
     * @return public agent update endpoint address.
     */
    String getPublicAgentUpdateEndpointAddress();


}