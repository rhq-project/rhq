/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

/**
 * When installing an agent to a remote machine, this object provides some information
 * about how the new agent installation should be configured.
 *
 * @author John Mazzitelli
 */
public class CustomAgentInstallData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String parentPath;
    private boolean overwriteExistingAgent;
    private String agentConfigurationXml;
    private String rhqAgentEnv;

    public CustomAgentInstallData() {
        // needed for GWT
    }

    public CustomAgentInstallData(String parentPath, boolean overwriteExistingAgent, String agentConfigurationXml,
        String rhqAgentEnv) {
        this.parentPath = parentPath;
        this.overwriteExistingAgent = overwriteExistingAgent;
        this.agentConfigurationXml = agentConfigurationXml;
        this.rhqAgentEnv = rhqAgentEnv;
    }

    /**
     * The agent update binary distribution file will be copied to this parent
     * directory and the actual agent install directory will be a child of
     * this parent directory, with that child install directory named "rhq-agent".
     */
    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    /**
     * If true, any existing agent in the install path will be shutdown and overwritten.
     *
     * @return overwrite flag
     */
    public boolean isOverwriteExistingAgent() {
        return overwriteExistingAgent;
    }

    public void setOverwriteExistingAgent(boolean overwriteExistingAgent) {
        this.overwriteExistingAgent = overwriteExistingAgent;
    }

    /**
     * If not null, this is a file path as seen from the server where an agent-configuration.xml file has been uploaded.
     * The server will take this file and configure the remote agent install with it.
     *
     * @return config file as seen on the server's file system, or null if no custom config file is to be used
     */
    public String getAgentConfigurationXml() {
        return agentConfigurationXml;
    }

    public void setAgentConfigurationXml(String agentConfigurationXml) {
        this.agentConfigurationXml = agentConfigurationXml;
    }

    /**
     * If not null, this is a file path as seen from the server where an rhq-agent-env.sh file has been uploaded.
     * The server will take this file and configure the remote agent install with it.
     *
     * @return env script file as seen on the server's file system, or null if no custom env script file is to be used
     */
    public String getRhqAgentEnv() {
        return rhqAgentEnv;
    }

    public void setRhqAgentEnv(String rhqAgentEnv) {
        this.rhqAgentEnv = rhqAgentEnv;
    }
}
