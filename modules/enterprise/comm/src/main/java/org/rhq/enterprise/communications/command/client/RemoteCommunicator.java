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
import org.rhq.enterprise.communications.command.CommandResponse;

/**
 * Interface defining the generic contract that is used to locate a remote endpoint and communicate with that remote
 * endpoint via any remoting framework.
 *
 * @author John Mazzitelli
 */
public interface RemoteCommunicator {
    /**
     * Connects this communicator object with the remote endpoint. Once connected, this communicator may send commands
     * and receive command responses from the remote endpoint.
     *
     * @throws Exception if failed to connect to the remote endpoint for any reason
     *
     * @see    #disconnect()
     */
    void connect() throws Exception;

    /**
     * Disconnects this communicator object from the remote endpoint. Once disconnected, this communicator will not be
     * able to send commands to the remote endpoint.
     *
     * <p>Note that the user may later on change its mind and reconnect simply by calling {@link #connect()}.</p>
     */
    void disconnect();

    /**
     * Returns <code>true</code> if this object has established a live connection with the remote endpoint.
     *
     * @return <code>true</code> if connected to a remote endpoint; <code>false</code> otherise
     */
    boolean isConnected();

    /**
     * Sends the given command to the remote endpoint by utilizing a remoting framework supported by the specific
     * communicator implementation. This will transport the command to the remote endpoint. This is the method in which
     * subclasses make the command-specific client calls necessary to invoke the command on the remote endpoint.
     * 
     * If an error is detected by this method, and a {@link #setFailureCallback(FailureCallback) failure callback}
     * is defined, this method will notify that callback of the problem and ask it if it should retry.
     *
     * @param  command encapsulates the command that is to be executed (must not be <code>null</code>)
     *
     * @return the command response
     *
     * @throws Throwable on any error (either during the sending or execution of the command)
     */
    CommandResponse send(Command command) throws Throwable;

    /**
     * This is the same as {@link #send(Command)} except, on error, this method will not attempt
     * to call the failure callback, if one was set.  This is useful when the caller wants to
     * explicitly handle any failure that might occur without any interference with a failure callback.
     * This method will also not attempt to call the {@link #getInitializeCallback() initialize callback},
     * thus allowing this method to be called from the initialize callback itself.
     * 
     * @param command encapsulates the command that is to be executed (must not be <code>null</code>)
     *
     * @return the command response
     * 
     * @throws Throwable on any error (either during the sending or execution of the command)
     */
    CommandResponse sendWithoutCallbacks(Command command) throws Throwable;

    /**
     * This is the same as {@link #send(Command)} except this method will not attempt
     * to call the {@link #getInitializeCallback() initialize callback},
     * thus allowing this method to be called from the initialize callback itself.
     * 
     * @param command encapsulates the command that is to be executed (must not be <code>null</code>)
     *
     * @return the command response
     * 
     * @throws Throwable on any error (either during the sending or execution of the command)
     */
    CommandResponse sendWithoutInitializeCallback(Command command) throws Throwable;

    /**
     * Returns the failure callback currently configured within this object.
     * 
     * @return the callback (may be <code>null</code>)
     */
    FailureCallback getFailureCallback();

    /**
     * Sets the given failure callback as the one that will be notified when this object sees a comm failure.
     * You can pass in <code>null</code> if you don't need this object to notify anything when a failure is detected.
     * The callback can be used to reconfigure this communicator and retry the failed message (useful to implement
     * failover algorithms).
     * 
     * @param callback the object that listens to failures and may trigger retries (may be <code>null</code>)
     */
    void setFailureCallback(FailureCallback callback);

    /**
     * Returns the initialize callback currently configured within this object.
     * 
     * @return the callback (may be <code>null</code>)
     */
    InitializeCallback getInitializeCallback();

    /**
     * Sets the given initialize callback as the one that will be notified when this object attempts
     * to send its very first message after a {@link #connect()}. This allows the callback to perform
     * additional initialization procedures prior to the first message getting sent to the remote
     * endpoint.  The callback is free to send its own messages via {@link #sendWithoutCallbacks(Command)}
     * or {@link #sendWithoutInitializeCallback(Command)}.
     * 
     * @param callback
     */
    void setInitializeCallback(InitializeCallback callback);

    /**
     * Returns a string representation of the remote endpoint this communicator is configured to talk to.
     * 
     * @return remote endpoint string
     */
    String getRemoteEndpoint();
}