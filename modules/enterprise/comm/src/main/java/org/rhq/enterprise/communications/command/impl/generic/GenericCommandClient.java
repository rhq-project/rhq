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
package org.rhq.enterprise.communications.command.impl.generic;

import java.util.Map;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.client.AbstractCommandClient;
import org.rhq.enterprise.communications.command.client.CommandClient;
import org.rhq.enterprise.communications.command.client.RemoteCommunicator;

/**
 * Provides a client API for any generic, custom command. Somewhat different than the typical command client
 * implementation, users of this class must utilize the {@link #setCommandType(CommandType)} to set the type of command
 * prior to invoking via the {@link #invoke(Map)} method. This class can be used to send any command via the
 * {@link #invoke(Command)} method (without the need to call {@link #setCommandType(CommandType)}.
 *
 * @author John Mazzitelli
 */
public class GenericCommandClient extends AbstractCommandClient {
    /**
     * the command type of new commands that are created via {@link #createNewCommand(Map)}
     */
    private CommandType m_commandType;

    /**
     * @see AbstractCommandClient#AbstractCommandClient()
     */
    public GenericCommandClient() {
        super();
    }

    /**
     * @see AbstractCommandClient#AbstractCommandClient(RemoteCommunicator)
     */
    public GenericCommandClient(RemoteCommunicator communicator) {
        super(communicator);
    }

    /**
     * Returns a {@link GenericCommand} object that can be used as a generic command. Callers should ensure that they
     * {@link #setCommandType(CommandType) set the command type} prior to calling this method.
     *
     * @see CommandClient#createNewCommand(Map)
     */
    public Command createNewCommand(Map<String, Object> params) {
        GenericCommand retCommand = new GenericCommand(params);

        retCommand.setCommandType(m_commandType);

        return retCommand;
    }

    /**
     * Sets the command type to be used when this client creates new commands via the {@link #createNewCommand(Map)}
     * method.
     *
     * @param commandType the command type to use when {@link #createNewCommand(Map) creating new commands}.
     */
    public void setCommandType(CommandType commandType) {
        m_commandType = commandType;
    }
}