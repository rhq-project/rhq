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
package org.rhq.enterprise.agent;

import java.util.Properties;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Encapsulates all the version information known about the agent update
 * and the current agent.
 * 
 * @author John Mazzitelli
 */
public class AgentUpdateInformation {
    private final String AGENT_UPDATE_BINARY_MD5 = "rhq-agent.latest.md5";
    private final String AGENT_UPDATE_BINARY_VERSION = "rhq-agent.latest.version";
    private final String AGENT_UPDATE_BINARY_BUILD = "rhq-agent.latest.build-number";

    private final String agentVersion;
    private final String agentBuild;

    private final String updateVersion;
    private final String updateBuild;
    private final String updateMd5;

    /**
     * Builds the information object.
     * 
     * @param updateProps properties known about the agent update. This information
     *                              must be retrieved from the server.
     */
    public AgentUpdateInformation(Properties updateProps) {
        // get information about the current agent
        this.agentVersion = Version.getProductVersion();
        this.agentBuild = Version.getBuildNumber();

        // extract the agent update info, if known
        if (updateProps != null) {
            this.updateVersion = updateProps.getProperty(AGENT_UPDATE_BINARY_VERSION, "0.UNKNOWN_VERSION");
            this.updateBuild = updateProps.getProperty(AGENT_UPDATE_BINARY_BUILD, "0.UNKNOWN_BUILD");
            this.updateMd5 = updateProps.getProperty(AGENT_UPDATE_BINARY_MD5, "UNKNOWN_MD5");
        } else {
            this.updateVersion = "0.UNKNOWN_VERSION";
            this.updateBuild = "0.UNKNOWN_BUILD";
            this.updateMd5 = "UNKNOWN_MD5";
        }
    }

    /**
     * Returns <code>true</code> if the {@link #getAgentVersion() current agent's version}
     * is lower than the {@link #getUpdateVersion() update's version}.
     * This method does not compare build numbers - see {@link #isAgentOutOfDateStrict()} for that.
     * 
     * @return <code>true</code> if the agent's version is older than the update's version
     */
    public boolean isAgentOutOfDate() {
        ComparableVersion agent = new ComparableVersion(getAgentVersion());
        ComparableVersion update = new ComparableVersion(getUpdateVersion());
        return agent.compareTo(update) < 0;
    }

    /**
     * Returns <code>true</code> if the {@link #getAgentVersion() current agent's version}
     * is lower than the {@link #getUpdateVersion() update's version}. If they are equal,
     * the {@link #getAgentBuild() agent's build number} is compared to the
     * {@link #getUpdateBuild() update's build number} and <code>true</code> will be returned
     * if the agent's build number is less than the update's build number. If either build
     * number could not be determined or is invalid, they will be ignored which causes this
     * method to return <code>false</code> since the version strings must be equal for this method
     * to even look at the build numbers.
     * 
     * To compare only version strings and ignore build numbers, see {@link #isAgentOutOfDate()}.
     * 
     * @return <code>true</code> if the agent's version/build is older than the update's version/build
     */
    public boolean isAgentOutOfDateStrict() {
        ComparableVersion agent = new ComparableVersion(getAgentVersion());
        ComparableVersion update = new ComparableVersion(getUpdateVersion());

        int comparision = agent.compareTo(update);

        if (comparision == 0) {
            // versions are equal, compare build numbers;
            try {
                int agentBuildInt = Integer.parseInt(getAgentBuild());
                int updateBuildInt = Integer.parseInt(getUpdateBuild());
                return agentBuildInt < updateBuildInt;
            } catch (Exception e) {
                // ignore invalid numbers, just compare version strings, which are equal
                return false;
            }
        }

        return comparision < 0;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public String getAgentBuild() {
        return agentBuild;
    }

    public String getUpdateVersion() {
        return updateVersion;
    }

    public String getUpdateBuild() {
        return updateBuild;
    }

    public String getUpdateMd5() {
        return updateMd5;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("AgentUpdateInformation: ");
        str.append("Version=[");
        str.append(getUpdateVersion());
        str.append("]; Build=[");
        str.append(getUpdateBuild());
        str.append("]");
        return str.toString();
    }
}
