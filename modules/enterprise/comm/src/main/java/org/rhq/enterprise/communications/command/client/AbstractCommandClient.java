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

/**
 * Provides basic functionality to all command clients.
 *
 * <p>This superclass provides the hooks by which users of the client can select the
 * {@link #setRemoteCommunicator(RemoteCommunicator) location of the remote endpoint} where the command is to be
 * invoked.</p>
 *
 * That also implicitly defines the underlying remote framework to be used when transporting the command data to and
 * from the remote endpoint since a concrete implementation of a {@link RemoteCommunicator} defines a particular remote
 * framework.
 *
 * <p>The users of this object may manually {@link #connectRemoteCommunicator() connect} and
 * {@link #disconnectRemoteCommunicator() disconnect} that communicator. Typically, there will not be a need to connect
 * since it will be done automatically when appropriate; however, it is good practice to tell this object to disconnect
 * its communicator when this object is no longer needed to issue commands to the remote endpoint.</p>
 *
 * <p>All subclasses should include a no-arg constructor so they can be built dynamically by the cmdline client.</p>
 *
 * @author John Mazzitelli
 */
public abstract class AbstractCommandClient implements CommandClient {
    /**
     * Object that abstracts the low-level remoting framework. This is the object that actually sends data to the remote
     * endpoint.
     */
    private RemoteCommunicator m_remoteCommunicator;

    /**
     * Constructor for {@link AbstractCommandClient} that initializes the client with no remote communicator defined. It
     * must later be specified through {@link #setRemoteCommunicator(RemoteCommunicator)} before any commands can be
     * issued.
     *
     * <p>Note that all subclasses are strongly urged to include this no-arg constructor so it can plug into the cmdline
     * client seamlessly.</p>
     */
    public AbstractCommandClient() {
        this(null);
    }

    /**
     * Constructor for {@link AbstractCommandClient} that allows you to specify the
     * {@link RemoteCommunicator remote communicator} to use. <code>communicator</code> may be <code>null</code>, in
     * which case, it must later be specified through {@link #setRemoteCommunicator(RemoteCommunicator)} before any
     * commands can be issued.
     *
     * @param communicator the remote communicator to be used to send commands to a remote endpoint
     */
    public AbstractCommandClient(RemoteCommunicator communicator) {
        m_remoteCommunicator = communicator;
    }

    /**
     * @see CommandClient#setRemoteCommunicator(RemoteCommunicator)
     */
    public void setRemoteCommunicator(RemoteCommunicator communicator) {
        if (communicator == null) {
            throw new IllegalArgumentException("communicator=null");
        }

        // since a new remote endpoint is being specified, disconnect any old communicator that already exists
        if (m_remoteCommunicator != null) {
            m_remoteCommunicator.disconnect();
            m_remoteCommunicator = null;
        }

        m_remoteCommunicator = communicator;
    }

    /**
     * @see CommandClient#getRemoteCommunicator()
     */
    public RemoteCommunicator getRemoteCommunicator() {
        return m_remoteCommunicator;
    }

    /**
     * @see CommandClient#connectRemoteCommunicator()
     */
    public void connectRemoteCommunicator() throws Exception {
        if ((m_remoteCommunicator != null) && !m_remoteCommunicator.isConnected()) {
            m_remoteCommunicator.connect();
        }

        return;
    }

    /**
     * @see CommandClient#disconnectRemoteCommunicator()
     */
    public void disconnectRemoteCommunicator() {
        if (m_remoteCommunicator != null) {
            m_remoteCommunicator.disconnect();
        }

        return;
    }

    /**
     * @see CommandClient#invoke(Map)
     */
    public CommandResponse invoke(Map<String, Object> params) throws Throwable {
        Command command = createNewCommand(params);

        // have the command sent back to us so the client receiving the response can also see the command issued
        command.setCommandInResponse(true);

        return invoke(command);
    }

    /**
     * @see CommandClient#invoke(Command)
     */
    public CommandResponse invoke(Command command) throws Throwable {
        CommandResponse retResponse;

        // during the validity check, we want to automatically convert invalid values to valid values
        command.checkParameterValidity(true);

        retResponse = getRemoteCommunicator().send(command);

        return retResponse;
    }
}