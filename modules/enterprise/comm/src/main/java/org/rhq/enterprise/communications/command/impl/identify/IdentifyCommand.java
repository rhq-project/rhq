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
package org.rhq.enterprise.communications.command.impl.identify;

import org.rhq.enterprise.communications.command.AbstractCommand;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.param.InvalidParameterDefinitionException;
import org.rhq.enterprise.communications.command.param.ParameterDefinition;

/**
 * The IDENTIFY command used for clients to ask a server for identification.
 *
 * @author John Mazzitelli
 */
public class IdentifyCommand extends AbstractCommand {
    /**
     * command type constant identifying this type of command
     */
    public static final CommandType COMMAND_TYPE = new CommandType("identify", 1);

    /**
     * the Serializable UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link IdentifyCommand}.
     *
     * @see AbstractCommand#AbstractCommand()
     */
    public IdentifyCommand() throws IllegalArgumentException, InvalidParameterDefinitionException {
        super();
    }

    /**
     * Constructor for {@link IdentifyCommand}.
     *
     * @see AbstractCommand#AbstractCommand(Command)
     */
    public IdentifyCommand(Command commandToTransform) {
        super(commandToTransform);
    }

    /**
     * @see AbstractCommand#buildCommandType()
     */
    protected CommandType buildCommandType() {
        return COMMAND_TYPE;
    }

    /**
     * @see AbstractCommand#buildParameterDefinitions()
     */
    protected ParameterDefinition[] buildParameterDefinitions() {
        return new ParameterDefinition[0];
    }
}