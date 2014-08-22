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
 * These are the results of a successful "agent connect".
 *
 * @author John Mazzitelli
 */
public class ConnectAgentResults implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long serverTime;
    private final boolean isDown;
    private final AgentVersion latestAgentVersion;

    public ConnectAgentResults(long serverTime, boolean isDown, AgentVersion latestAgentVersion) {
        this.serverTime = serverTime;
        this.isDown = isDown;
        this.latestAgentVersion = latestAgentVersion;
    }

    /**
     * The current time as seen by the server clock.  This is the time the agent connect
     * was made and can also be used to determine if the agent's clock is in sync with the server.
     *
     * @return the server's clock, in epoch milliseconds
     */
    public long getServerTime() {
        return this.serverTime;
    }

    /**
     * If true, this indicates if the server thinks the agent is down. This happens if the agent
     * hasn't connected in a long time and the server had "backfilled" the agent's resources
     * as DOWN/UNKNOWN.
     *
     * When an agent connects to a server, and the server thinks that agent was down, the agent needs
     * to prepare to notify the server of its state - for example, the agent should soon send up
     * a full availability report so the server can get the up-to-date availability statuses of all
     * resources.
     *
     * @return true if the server had this agent's resources marked as DOWN/UNKNOWN.
     */
    public boolean isDown() {
        return isDown;
    }

    /**
     * Returns the latest agent version as known by the server returning this results object.
     * This agent version is that version of the agent update distribution that is served up by
     * this server. This should represent the most up-to-date agent version available.
     *
     * This can be null if the latest agent version cannot be determined.
     *
     * @return the most up-to-date agent version known
     */
    public AgentVersion getLatestAgentVersion() {
        return latestAgentVersion;
    }

    @Override
    public String toString() {
        return "ConnectAgentResults: [server-time=" + this.serverTime + ", is-down=" + this.isDown
            + ", latestAgentVersion=" + this.latestAgentVersion + "]";
    }
}
