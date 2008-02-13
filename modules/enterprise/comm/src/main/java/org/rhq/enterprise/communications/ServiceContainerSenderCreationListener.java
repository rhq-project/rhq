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
package org.rhq.enterprise.communications;

import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderConfiguration;
import org.rhq.enterprise.communications.command.client.RemoteCommunicator;

/**
 * A {@link ServiceContainer} may create new {@link ClientCommandSender senders} - for example, when a remote stream is
 * being received by a remote client and a sender is created in order to retrieve the stream data from the client.
 * Because senders are typically a highly configured object and because the service container won't know all the
 * different configurations a sender needs to have, this listener object is used to notify other objects when a sender
 * object is created by the service container. Listeners are registered on the service container; when the service
 * container creates a new sender object, the listeners are notified, giving them an opportunity to complete the
 * configuration of the sender.
 *
 * @author John Mazzitelli
 */
public interface ServiceContainerSenderCreationListener {
    /**
     * Called when the given service container is going to create a sender. The implementation of this method is allowed
     * to modify the communicator and configuration as it sees fit.
     *
     * @param serviceContainer    the service container that is creating the sender
     * @param remoteCommunicator  the communicator that will be used by the sender
     * @param senderConfiguration the actual configuration for the sender
     */
    void preCreate(ServiceContainer serviceContainer, RemoteCommunicator remoteCommunicator,
        ClientCommandSenderConfiguration senderConfiguration);

    /**
     * Called when the given service container has finished creating the given sender.
     *
     * @param serviceContainer the service container that created the sender
     * @param sender           the sender that was just created
     */
    void postCreate(ServiceContainer serviceContainer, ClientCommandSender sender);
}