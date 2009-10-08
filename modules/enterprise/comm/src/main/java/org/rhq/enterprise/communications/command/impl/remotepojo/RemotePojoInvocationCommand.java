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
package org.rhq.enterprise.communications.command.impl.remotepojo;

import java.util.Map;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.rhq.enterprise.communications.command.AbstractCommand;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.param.InvalidParameterDefinitionException;
import org.rhq.enterprise.communications.command.param.ParameterDefinition;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * The command to be sent when a remote invocation on a POJO is to be made.
 *
 * @author John Mazzitelli
 */
public class RemotePojoInvocationCommand extends AbstractCommand {
    /**
     * command type constant identifying this type of command
     */
    public static final CommandType COMMAND_TYPE = new CommandType("remotepojo", 1);

    /**
     * the required command parameter name containing the NameBasedInvocation object to describe what method to invoke
     * on the remote POJO.
     */
    public static final ParameterDefinition PARAM_INVOCATION = new ParameterDefinition("invocation",
        NameBasedInvocation.class.getName(), ParameterDefinition.REQUIRED, ParameterDefinition.NOT_NULLABLE,
        ParameterDefinition.NOT_HIDDEN, CommI18NFactory.getMsg().getMsg(
            CommI18NResourceKeys.REMOTE_POJO_INVOCATION_COMMAND_INVOCATION));

    /**
     * the remote POJO's interface that the command wants to invoke.
     */
    public static final ParameterDefinition PARAM_TARGET_INTERFACE_NAME = new ParameterDefinition(
        "targetInterfaceName", String.class.getName(), ParameterDefinition.REQUIRED, ParameterDefinition.NOT_NULLABLE,
        ParameterDefinition.NOT_HIDDEN, CommI18NFactory.getMsg().getMsg(
            CommI18NResourceKeys.REMOTE_POJO_INVOCATION_COMMAND_TARGET_INTERFACE_NAME));

    /**
     * the Serializable UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link RemotePojoInvocationCommand}.
     *
     * @see AbstractCommand#AbstractCommand()
     */
    public RemotePojoInvocationCommand() throws IllegalArgumentException, InvalidParameterDefinitionException {
        super();
    }

    /**
     * Constructor for {@link RemotePojoInvocationCommand}.
     *
     * @see AbstractCommand#AbstractCommand(Map)
     */
    public RemotePojoInvocationCommand(Map<String, Object> commandParameters) throws IllegalArgumentException,
        InvalidParameterDefinitionException {
        super(commandParameters);
    }

    /**
     * Constructor for {@link RemotePojoInvocationCommand}.
     *
     * @see AbstractCommand#AbstractCommand(Command)
     */
    public RemotePojoInvocationCommand(Command commandToTransform) {
        super(commandToTransform);
    }

    /**
     * Gets the invocation information that describes what needs to be invoked on the remote POJO.
     *
     * @return the invocation information
     */
    public NameBasedInvocation getNameBasedInvocation() {
        return (NameBasedInvocation) getParameterValue(PARAM_INVOCATION.getName());
    }

    /**
     * Sets the invocation information that describes what needs to be invoked on the remote POJO.
     *
     * @param invocation the invocation information
     */
    public void setNameBasedInvocation(NameBasedInvocation invocation) {
        setParameterValue(PARAM_INVOCATION.getName(), invocation);
    }

    /**
     * Returns the name of the remote POJO's target interface that is being invoked.
     *
     * @return the name of the target POJO's interface
     */
    public String getTargetInterfaceName() {
        return (String) getParameterValue(PARAM_TARGET_INTERFACE_NAME.getName());
    }

    /**
     * Sets the name of the remote POJO's target interface that is being invoked.
     *
     * @param targetInterfaceName the name of the target POJO's interface
     */
    public void setTargetInterfaceName(String targetInterfaceName) {
        setParameterValue(PARAM_TARGET_INTERFACE_NAME.getName(), targetInterfaceName);
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
        return new ParameterDefinition[] { PARAM_INVOCATION, PARAM_TARGET_INTERFACE_NAME };
    }
}