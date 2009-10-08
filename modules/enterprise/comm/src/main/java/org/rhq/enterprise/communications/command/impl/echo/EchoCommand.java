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
package org.rhq.enterprise.communications.command.impl.echo;

import java.util.Map;
import org.rhq.enterprise.communications.command.AbstractCommand;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.param.InvalidParameterDefinitionException;
import org.rhq.enterprise.communications.command.param.ParameterDefinition;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * The ECHO command used for mainly testing and debugging connectivity.
 *
 * @author John Mazzitelli
 */
public class EchoCommand extends AbstractCommand {
    /**
     * command type constant identifying this type of command
     */
    public static final CommandType COMMAND_TYPE = new CommandType("echo", 1);

    /**
     * the required command parameter name that identifies the message to echo back to the client
     */
    public static final ParameterDefinition PARAM_MESSAGE = new ParameterDefinition("message", String.class.getName(),
        ParameterDefinition.REQUIRED, ParameterDefinition.NOT_NULLABLE, ParameterDefinition.NOT_HIDDEN, CommI18NFactory
            .getMsg().getMsg(CommI18NResourceKeys.ECHO_COMMAND_MESSAGE));

    /**
     * the optional command parameter name that identifies a string to prefix the echo message
     */
    public static final ParameterDefinition PARAM_PREFIX = new ParameterDefinition("prefix", String.class.getName(),
        ParameterDefinition.OPTIONAL, ParameterDefinition.NULLABLE, ParameterDefinition.NOT_HIDDEN, CommI18NFactory
            .getMsg().getMsg(CommI18NResourceKeys.ECHO_COMMAND_PREFIX));

    /**
     * the Serializable UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link EchoCommand}.
     *
     * @see AbstractCommand#AbstractCommand()
     */
    public EchoCommand() throws IllegalArgumentException, InvalidParameterDefinitionException {
        super();
    }

    /**
     * Constructor for {@link EchoCommand}.
     *
     * @see AbstractCommand#AbstractCommand(Map)
     */
    public EchoCommand(Map<String, Object> commandParameters) throws IllegalArgumentException,
        InvalidParameterDefinitionException {
        super(commandParameters);
    }

    /**
     * Constructor for {@link EchoCommand}.
     *
     * @see AbstractCommand#AbstractCommand(Command)
     */
    public EchoCommand(Command commandToTransform) {
        super(commandToTransform);
    }

    /**
     * Gets the message parameter value that will be echoed back to the client.
     *
     * @return the message that the server will echo back to the client
     */
    public String getMessage() {
        return (String) getParameterValue(PARAM_MESSAGE.getName());
    }

    /**
     * Sets the message that will be echoed back to the client.
     *
     * @param message a message that the server will echo back to the client
     */
    public void setMessage(String message) {
        setParameterValue(PARAM_MESSAGE.getName(), message);
    }

    /**
     * Gets the (optional) parameter whose value will be used by the server to prefix the
     * {@link #getMessage() echoed message}.
     *
     * @return string to be prefixed to the echo message when it is returned to the client
     */
    public String getPrefix() {
        return (String) getParameterValue(PARAM_PREFIX.getName());
    }

    /**
     * An optional parameter whose value will be used by the server to prefix the {@link #getMessage() echoed message}.
     *
     * @param prefix string to be prefixed to the echo message when it is returned to the client
     */
    public void setPrefix(String prefix) {
        setParameterValue(PARAM_PREFIX.getName(), prefix);
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
        return new ParameterDefinition[] { PARAM_MESSAGE, PARAM_PREFIX };
    }
}