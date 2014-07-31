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
package org.rhq.enterprise.server.core;

import java.io.Serializable;
<<<<<<< HEAD
import java.util.Properties;
=======
>>>>>>> dafb691... BZ 1124614 - if an agent's auto-update is enabled, then always update itself if its version is not the same as the latest agent version of the agent distro in the server

import org.rhq.core.clientapi.server.core.AgentVersion;

/**
 * A simple POJO that returns results from the AgentManagerBean that indicates if
 * a particular version is supported or not. As part of these results, you get some information
 * from the agent version file that is maintained by the server in case additional information
 * is needed after the version check is made (such as, if the agent version is not supported, this
 * addition information can tell you what is supported).
 *
 * @author John Mazzitelli
 */
public class AgentVersionCheckResults implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean isSupported;
    private final AgentVersion latestAgentVersion;

    public AgentVersionCheckResults(boolean isSupported, AgentVersion latestAgentVersion) {
        this.isSupported = isSupported;
<<<<<<< HEAD
        this.latestAgentVersion = (AgentVersion) ((latestAgentVersion != null) ? latestAgentVersion : new Properties());
=======
        this.latestAgentVersion = (AgentVersion) ((latestAgentVersion != null) ? latestAgentVersion : new AgentVersion("",""));
>>>>>>> dafb691... BZ 1124614 - if an agent's auto-update is enabled, then always update itself if its version is not the same as the latest agent version of the agent distro in the server

    }

    public boolean isSupported() {
        return isSupported;
    }

    /**
     * Returns the latest agent version information as known by the server. This contains
     * information about the agent update distribution that the server provides.
     *
     * This will be null if the latest agent version information could not be determined.
     *
     * @return latest agent version information
     */
    public AgentVersion getLatestAgentVersion() {
        return latestAgentVersion;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("AgentVersionCheckResults: ");
        str.append("is-supported=[");
        str.append(this.isSupported);
        str.append("; latest-agent-version=[");
        str.append(this.latestAgentVersion);
        str.append("]");
        return str.toString();
    }
}
