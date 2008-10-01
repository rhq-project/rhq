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

package org.rhq.enterprise.communications.command.client;

import org.rhq.enterprise.communications.command.Command;

/**
 * Callback that is informed when a {@link RemoteCommunicator communicator} is about to
 * send its very first command since being {@link RemoteCommunicator#connect() connected}.
 * 
 * @author John Mazzitelli
 */
public interface InitializeCallback {

    /**
     * The callback method that is called when a remote communicator is about to send its very
     * first command. The callback is not responsible for sending the given <code>command</code>,
     * rather, this is just a notification to let the callback know what command is about to be
     * sent by the communicator.
     *
     * Implementations can indicate if the request should be aborted by throwing an exception.
     * Returning normally means the initializer has finished doing what its doing and the
     * remote communicator can continue with sending that first message.
     * This callback method is allowed to send commands itself to the remote endpoint via
     * {@link RemoteCommunicator#sendWithoutCallbacks(Command)} or
     * {@link RemoteCommunicator#sendWithoutInitializeCallback(Command)}.
     * 
     * If no exception occurred within the callback method implementation, but the callback wants
     * to be called again the next time a command is to be sent, return <code>false</code>. A <code>true</code>
     * will indicate the callback is done what it had to do and no longer needs to be called.
     *
     * @param remoteCommunicator the communicator object that is making the call
     * @param command the command that is being sent that triggered this callback
     *
     * @return <code>true</code> if the callback finished performing its initialization and no longer
     *         needs to be called. <code>false</code> means the callback would like to be invoked again
     *         the next time a command is to be sent.
     *
     * @throws Exception if for some reason the callback has detected a problem that should
     *         prohibit the remote communicator from sending the command to the remote endpoint.
     */
    boolean sendingInitialCommand(RemoteCommunicator remoteCommunicator, Command command) throws Throwable;
}
