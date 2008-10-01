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
package org.rhq.core.clientapi.server.core;

import java.io.Serializable;

/**
 * These are the results of a successful "agent connect".
 *
 * @author John Mazzitelli
 */
public class ConnectAgentResults implements Serializable {
    private static final long serialVersionUID = 1L;

    private long serverTime;

    public ConnectAgentResults(long serverTime) {
        this.serverTime = serverTime;
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
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ConnectAgentResults: [server-time=" + this.serverTime + "]";
    }
}