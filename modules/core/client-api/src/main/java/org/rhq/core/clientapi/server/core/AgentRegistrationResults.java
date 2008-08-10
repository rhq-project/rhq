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

import org.rhq.core.domain.cluster.composite.FailoverListComposite;

/**
 * These are the results of a successful agent registration. After the agent has been registered by the server, this
 * object will contain the identification information assigned to the agent by the server.  It will also contain the
 * list of servers this agent can fail over to an in high availability setup.
 *
 * @author John Mazzitelli
 */
public class AgentRegistrationResults implements Serializable {
    private static final long serialVersionUID = 1L;

    private String agentToken;
    private FailoverListComposite failoverList;

    /**
     * The agent token that the agent must use in order to communicate with the server in the future.
     *
     * @return the agent's token assigned to it by the server
     */
    public String getAgentToken() {
        return agentToken;
    }

    /**
     * See {@link #getAgentToken()}.
     *
     * @param token
     */
    public void setAgentToken(String token) {
        agentToken = token;
    }

    /**
     * The FailoverListComposite that the agent must use in order to communicate with the server in the future.
     *
     * @return the FailoverListComposite assigned to it by the server
     */
    public FailoverListComposite getFailoverList() {
        return failoverList;
    }

    /**
     * See {@link #getFailoverList()}.
     *
     * @param failoverList
     */
    public void setFailoverList(FailoverListComposite failoverList) {
        this.failoverList = failoverList;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "AgentRegistrationResults: [agent-token=" + this.agentToken + "]";
    }
}