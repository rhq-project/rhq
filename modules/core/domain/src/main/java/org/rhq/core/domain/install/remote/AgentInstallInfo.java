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
package org.rhq.core.domain.install.remote;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides information on a new agent installation.
 *
 * @author John Mazzitelli
 * @author Greg Hinkle
 */
public class AgentInstallInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String serverAddress;
    private String agentAddress;
    private int agentPort;

    private String path;
    private String owner;
    private String version;

    private List<AgentInstallStep> steps = new ArrayList<AgentInstallStep>();
    private String customAgentConfigFile = null;
    private Boolean confirmedAgentConnection = Boolean.FALSE;

    public static final String SETUP_PROP = "rhq.agent.configuration-setup-flag";

    public static final String SERVER_ADDRESS_PROP = "rhq.agent.server.bind-address";
    public static final String SERVER_PORT_PROP = "rhq.agent.server.bind-port";

    public static final String AGENT_ADDRESS_PROP = "rhq.communications.connector.bind-address";
    public static final String AGENT_PORT_PROP = "rhq.communications.connector.bind-port";

    public static final int DEFAULT_SERVER_PORT = 7080;
    public static final int DEFAULT_AGENT_PORT = 16163;

    public AgentInstallInfo() {
    }

    public AgentInstallInfo(String path, String owner, String version, String serverAddress, String agentAddress) {
        this.path = path;
        this.owner = owner;
        this.version = version;
        this.serverAddress = serverAddress;
        this.agentAddress = agentAddress;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getAgentAddress() {
        return agentAddress;
    }

    public void setAgentAddress(String agentAddress) {
        this.agentAddress = agentAddress;
    }

    public int getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(int agentPort) {
        this.agentPort = agentPort;
    }

    public void addStep(AgentInstallStep step) {
        steps.add(step);
    }

    public List<AgentInstallStep> getSteps() {
        return steps;
    }

    public String getCustomAgentConfigurationFile() {
        return customAgentConfigFile;
    }

    public void setCustomAgentConfigurationFile(String file) {
        this.customAgentConfigFile = file;
    }

    /**
     * If true, then the agent was at least pinged and it appears up.
     * If false, then a connection to the agent failed and it appears to be down or perhaps behind a firewall.
     * If null, then no connection attempt was made to the agent - the agent status is unknown.
     *
     * @return flag to indicate if a connection to the agent was successfully made
     */
    public Boolean isConfirmedAgentConnection() {
        return confirmedAgentConnection;
    }

    public void setConfirmedAgentConnection(Boolean flag) {
        this.confirmedAgentConnection = flag;
    }

    public String getConfigurationStartString() {
        StringBuilder buf = new StringBuilder();

        buf.append("-D").append(SERVER_ADDRESS_PROP).append("=").append(serverAddress);
        buf.append(" ");

        buf.append("-D").append(AGENT_ADDRESS_PROP).append("=").append(agentAddress);
        buf.append(" ");

        buf.append("-D").append(SETUP_PROP).append("=").append("true");
        buf.append(" ");
        buf.append("--daemon ");

        if (customAgentConfigFile != null) {
            buf.append("--config=" + customAgentConfigFile);
            buf.append(" ");
        }

        return buf.toString();
    }

}
