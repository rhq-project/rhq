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

import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.client.InitializeCallback;
import org.rhq.enterprise.communications.command.client.RemoteCommunicator;

/**
 * This is a {@link InitializeCallback} that will send the appropriate "connect agent" message
 * to the server. This is required because an agent must never send any message to a server without first
 * ensuring it sends this "connect agent" message successfully.
 * 
 * @author John Mazzitelli
 */
public class ConnectAgentInitializeCallback implements InitializeCallback {

    private AgentMain agent;

    public ConnectAgentInitializeCallback(AgentMain agent) {
        this.agent = agent;
    }

    public boolean sendingInitialCommand(RemoteCommunicator comm, Command command) throws Throwable {
        if (this.agent.isRegistered()) {
            this.agent.sendConnectRequestToServer(comm, true);
            return true;
        } else {
            // not registered yet - we're probably in the process of registering
            // or pinging, so return false to indicate we want to be called again
            return false;
        }
    }
}
