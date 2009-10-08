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

import java.util.Map;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.param.ParameterDefinition;

/**
 * Interface to all clients of commands. This interface defines the operations one can make as a client to a remote
 * command server.
 *
 * @author John Mazzitelli
 */
public interface CommandClient {
    /**
     * Gets the object that encapsulates the underlying remote framework implementation. This is the object that
     * performs the actual sending of commands and receiving of command responses over the underlying remote framework
     * transport.
     *
     * @return the remote communicator object that is specific to a particular underlying remote framework.
     */
    RemoteCommunicator getRemoteCommunicator();

    /**
     * Sets the object that encapsulates the underlying remote framework implementation. This is the object that
     * performs the actual sending of commands and receiving of command responses over the underlying remote framework
     * transport.
     *
     * @param remoteCommunicator the remote communicator object that is specific to a particular underlying remote
     *                           framework.
     */

    void setRemoteCommunicator(RemoteCommunicator remoteCommunicator);

    /**
     * Provides a mechanism by which users can notify this object that the user is about to issue commands and that a
     * remoting communicator should be created and should connect to the remote server.
     *
     * @throws Exception if failed to connect to the remote server for any reason
     *
     * @see    #disconnectRemoteCommunicator()
     */
    void connectRemoteCommunicator() throws Exception;

    /**
     * Provides a mechanism by which users can notify this object that the user is no longer interested in issuing
     * commands to the remote server. The effect of this is the remoting communicator will now disconnect from the
     * remote server.
     *
     * <p>Note that the user may later on change its mind and reconnect simply by calling
     * {@link #connectRemoteCommunicator()}. Calling that method will reconnect the remoting communicator.</p>
     */
    void disconnectRemoteCommunicator();

    /**
     * Invokes the actual command by utilizing the remoting communicator to transport the command to the server. This is
     * the method in which subclasses make the command-specific client calls necessary to invoke the command on the
     * remote server.
     *
     * <p>This method is typically used in conjunction with {@link CmdlineClient} to invoke commands that are unknown at
     * compile-time. In this case, <code>params</code> typically contain values of type <code>String</code> - these are
     * text-based parameters that were entered via a script or cmdline. That text-based data will be
     * {@link ParameterDefinition#convertObject(Object) converted} to meaningful Java types in order to be able to build
     * the proper {@link Command} objects.</p>
     *
     * @param  params a set of command parameters that are to be used to execute the proper command (may be <code>
     *                null</code>)
     *
     * @return the command response
     *
     * @throws Throwable on any error (either during the sending or execution of the command)
     *
     * @see    #createNewCommand(Map)
     */
    CommandResponse invoke(Map<String, Object> params) throws Throwable;

    /**
     * Invokes the actual command by utilizing the remoting communicator to transport the command to the server. This is
     * the method in which subclasses make the command-specific client calls necessary to invoke the command on the
     * remote server.
     *
     * <p>This is the prefered method for clients to call when it needs to invoke commands since each {@link Command} is
     * strongly typed and provides compile-time error checking. However, in order to be able to invoke this method on
     * subclasses, concrete implementations must be used and hence the actual command that is to be issued must be known
     * at compile time. Should a dynamic runtime client be needed to invoke commands unknown at compile time (see
     * {@link CmdlineClient}), the use of {@link #invoke(Map)} will be required.</p>
     *
     * @param  command encapsulates the command that is to be executed (must not be <code>null</code>)
     *
     * @return the command response
     *
     * @throws Throwable on any error (either during the sending or execution of the command)
     */
    CommandResponse invoke(Command command) throws Throwable;

    /**
     * Creates a new {@link Command} object that can be used by this client. The client can take this new command
     * instance, fill it in with parameter values and execute it. The given map of parameters is optional; if non-
     * <code>null</code>, all parameters found in the map will be added to the command.
     *
     * @param  params contains parameter values keyed on the parameter names (may be <code>null</code>)
     *
     * @return a new {@link Command} instance
     */
    Command createNewCommand(Map<String, Object> params);
}