/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.impl.stream.RemoteInputStreamCommand;
import org.rhq.enterprise.communications.command.impl.stream.RemoteOutputStreamCommand;
import org.rhq.enterprise.communications.command.server.CommandAuthenticator;

/**
 * This is the authenticator object that will make sure we are getting a message from a valid server.
 * It will check to make sure the token the sender sent us matches our own token. The server will know
 * what token it assigned us, so it should be able to give us our own token back. If the token doesn't match,
 * we must be getting a message from some server that isn't the one that we are registered with, thus,
 * then this happens, the command is aborted.
 *
 * @author John Mazzitelli
 */
public class SecurityTokenCommandAuthenticator implements CommandAuthenticator {

    /**
     * This is the name of the command configuration property that the server will set to the security token string.
     */
    static final String CMDCONFIG_PROP_SECURITY_TOKEN = "rhq.security-token";

    private ServiceContainer serviceContainer;

    @Override
    public boolean isAuthenticated(Command command) {
        if (this.serviceContainer == null) {
            return false; // we can't authenticate yet, we don't have the service container
        }

        if (command.getCommandType().equals(RemoteOutputStreamCommand.COMMAND_TYPE)
            || (command.getCommandType().equals(RemoteInputStreamCommand.COMMAND_TYPE))) {
            return true; // remoting streaming can go through
        }

        String incomingToken = command.getConfiguration().getProperty(CMDCONFIG_PROP_SECURITY_TOKEN);
        Object ourToken = this.serviceContainer.getCustomData(CMDCONFIG_PROP_SECURITY_TOKEN); // the agent puts this in here

        if (incomingToken == null) {
            if (ourToken == null) {
                return true; // no tokens anywhere - accept this message in case this command is part of the comm setup
            } else {
                return false; // we have a token, but the incoming command doesn't - reject this command
            }
        }

        return incomingToken.equals(ourToken);
    }

    @Override
    public void setServiceContainer(ServiceContainer serviceContainer) {
        this.serviceContainer = serviceContainer;
    }
}
