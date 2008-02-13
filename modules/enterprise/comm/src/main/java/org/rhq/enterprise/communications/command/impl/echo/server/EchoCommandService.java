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
package org.rhq.enterprise.communications.command.impl.echo.server;

import java.io.InputStream;
import java.io.OutputStream;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandExecutor;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.impl.echo.EchoCommand;
import org.rhq.enterprise.communications.command.impl.echo.EchoCommandResponse;
import org.rhq.enterprise.communications.command.server.CommandService;
import org.rhq.enterprise.communications.command.server.CommandServiceMBean;

/**
 * Performs the {@link EchoCommand echo command} which will simply return the received message, with an optional prefix.
 * This is used mainly for testing and debugging of connectivity to the command processor.
 *
 * @author John Mazzitelli
 */
public class EchoCommandService extends CommandService {
    /**
     * @see CommandExecutor#execute(Command, java.io.InputStream, java.io.OutputStream)
     */
    public CommandResponse execute(Command command, InputStream in, OutputStream out) {
        EchoCommand echoCommand = new EchoCommand(command);

        String msg = echoCommand.getMessage();
        String prefix = echoCommand.getPrefix();

        if (msg == null) {
            msg = "<null echo message>";
        }

        if (prefix != null) {
            msg = prefix + msg;
        }

        return new EchoCommandResponse(echoCommand, msg);
    }

    /**
     * Supports {@link EchoCommand#COMMAND_TYPE}.
     *
     * @see CommandServiceMBean#getSupportedCommandTypes()
     */
    public CommandType[] getSupportedCommandTypes() {
        return new CommandType[] { EchoCommand.COMMAND_TYPE };
    }
}