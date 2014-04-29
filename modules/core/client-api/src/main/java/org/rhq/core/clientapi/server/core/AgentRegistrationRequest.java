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

import java.io.Serializable;

/**
 * Represents a request to register a new agent.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class AgentRegistrationRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    // This is the name of the system property that should be passed into an agent to identify its installation.
    // Note that it purposefully does not start with "rhq.agent" or "rhq.communications" so it isn't persisted in agent config prefs.
    public static final String SYSPROP_INSTALL_ID = "rhq.install.id";

    private final String name;
    private final String address;
    private final int port;
    private final String remoteEndpoint;
    private final boolean regenerateTokenFlag;
    private final String originalToken;
    private final AgentVersion agentVersion;
    private final String installId;
    private final String installLocation;

    /**
     * Creates a new {@link AgentRegistrationRequest} object. Note that <code>address</code> and <code>port</code> must
     * be specified, even though <code>remoteEndpoint</code> might also encode address and port. The <code>
     * originalToken</code> may be <code>null</code> even if the agent was already registered (this can happen in the
     * case if the token file was deleted on the agent machine; or if the agent was reinstalled which caused the loss of
     * the token). The version information helps the server determine if this agent is obsolete or not.
     *
     * @param name                unique name of the agent; usually just the <code>address</code>, but doesn't have to
     *                            be
     * @param address             the address that the agent is listening to
     * @param port                the port that the agent is listening to
     * @param remoteEndpoint      the full remote endpoint string that the agent wants the server to use to connect to
     *                            the agent
     * @param regenerateTokenFlag if <code>true</code>, the agent will be assigned a new token. If <code>false</code>
     *                            and the agent already exists, its current token is returned.
     * @param originalToken       the agent's original token, if this is a re-registration (may be <code>null</code>)
     * @param agentVersion        the agent's version information
     * @param installId           if the agent was started with an install id, its value is passed here; otherwise, null
     * @param installLocation     location where the agent is installed - could be null if its not known or not applicable
     */
    public AgentRegistrationRequest(String name, String address, int port, String remoteEndpoint,
        boolean regenerateTokenFlag, String originalToken, AgentVersion agentVersion, String installId,
        String installLocation) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.remoteEndpoint = remoteEndpoint;
        this.regenerateTokenFlag = regenerateTokenFlag;
        this.originalToken = originalToken;
        this.agentVersion = agentVersion;
        this.installId = installId;
        this.installLocation = installLocation;
    }

    /**
     * Returns the name that the agent is to be known as. No two agents can have the same name. For that reason, this is
     * usually the same string as that of the {@link #getAddress() address}, but technically this does not have to be
     * so. An agent name can be any string, so long as it is globally unique across all agents.
     *
     * @return the agent name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the address that the agent is bound to in which it listens for requests. This address is the address as
     * seen by the server (which may or may not be the same as the address as seen by the agent).
     *
     * @return the address that the server should use when connecting to the agent
     */
    public String getAddress() {
        return address;
    }

    /**
     * Returns the port that the agent is listening to. The server should connect to the agent on this port.
     *
     * @return the port the agent listens to for incoming requests
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the remote endpoint string that fully describes how to connect to the agent. It typically encodes and
     * overrides the {@link #getAddress() address} and {@link #getPort() port} values.
     *
     * @return the full remote endpoint to describe how to connect to the agent
     */
    public String getRemoteEndpoint() {
        return remoteEndpoint;
    }

    /**
     * Returns the agent's original token, as it was known to the agent. This may be <code>null</code> if the agent was
     * never registered before or the agent lost its token.
     *
     * @return agent's currently known token, or <code>null</code>
     */
    public String getOriginalToken() {
        return originalToken;
    }

    /**
     * Returns <code>true</code> if the agent should be given a new token, even if the agent already has a token. If
     * <code>false</code>, and the agent is already registered, the agent will keep its old token. This allows an agent
     * that already exists to request a new token, effectively invalidating the old token.
     *
     * @return regenerate token flag
     */
    public boolean getRegenerateToken() {
        return regenerateTokenFlag;
    }

    /**
     * Returns the information that identifies the version of the agent asking to be registered.
     *
     * @return agent version information
     */
    public AgentVersion getAgentVersion() {
        return agentVersion;
    }

    /**
     * If the agent was given an install ID at startup, this is that ID. Otherwise, this will be null;
     *
     * @return agent installation ID
     */
    public String getInstallId() {
        return installId;
    }

    /**
     * Returns the location of the agent install. If the agent doesn't know it or its not applicable for the agent,
     * this can be null.
     *
     * @return location of the agent installation if known
     */
    public String getInstallLocation() {
        return installLocation;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("AgentRegistrationRequest: [");
        str.append("name=[" + this.name);
        str.append("]; address=[" + this.address);
        str.append("]; port=[" + this.port);
        str.append("]; remote-endpoint=[" + this.remoteEndpoint);
        str.append("]; regenerate-token=[" + this.regenerateTokenFlag);
        str.append("]; original-token=[<was " + ((this.originalToken == null) ? "" : "not ") + "null>");
        str.append("]; agent-version=[" + this.agentVersion);
        str.append("]; install-id=[" + this.installId);
        str.append("]; install-location=[" + this.installLocation);
        str.append("]");
        return str.toString();
    }
}