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
import org.rhq.enterprise.communications.command.AbstractCommand;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.param.InvalidParameterDefinitionException;
import org.rhq.enterprise.communications.command.param.ParameterDefinition;

/**
 * Defines a custom command in which the command metadata (in other words, command type and parameter definitions) are
 * mutable. That is, there are setters defined in this class that allow for command type and parameter definitions to be
 * modified.
 *
 * <p>Of course, a custom command is worthless unless there is a command service on the server side that can process the
 * command. That is to say, the command type and parameter definitions must be acceptable to the command service where
 * the command is issued.</p>
 *
 * <p>It is generally not a good thing to allow users of commands to be able to modify command types and parameter
 * definitions as this could make the command inoperable (which is why this capability does not exist in the base
 * superclass}. However, this capability may be needed for specialized clients.</p>
 *
 * <p>Note that modifications to a custom command have no effect on previous executions of the command. While this may
 * seem obvious, a less obvious effect is that a command whose {@link AbstractCommand#isCommandInResponse()} was <code>
 * true</code> at the time of execution will be stored in the resulting {@link CommandResponse} - changing that flag to
 * <code>false</code> after the fact will not alter that previous command response.</p>
 *
 * @author John Mazzitelli
 */
public class GenericCommand extends AbstractCommand {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * used to force the command to this new type
     */
    private CommandType m_newCommandType = null;

    /**
     * used to force the definitions to these new ones
     */
    private ParameterDefinition[] m_newParameterDefinitions = null;

    /**
     * @see AbstractCommand#AbstractCommand()
     */
    public GenericCommand() throws IllegalArgumentException, InvalidParameterDefinitionException {
        super();
    }

    /**
     * @see AbstractCommand#AbstractCommand(Map)
     */
    public GenericCommand(Map<String, Object> commandParameters) throws IllegalArgumentException,
        InvalidParameterDefinitionException {
        super(commandParameters);
    }

    /**
     * @see AbstractCommand#AbstractCommand(Command)
     */
    public GenericCommand(Command commandToTransform) {
        super(commandToTransform);
    }

    /**
     * Specialized constructor for {@link GenericCommand} that allows the instantiator to dynamically define this
     * generic commands metadata at runtime.
     *
     * @param commandType this command type
     * @param paramDefs   the definitions for this command's parameters
     */
    public GenericCommand(CommandType commandType, ParameterDefinition[] paramDefs) {
        super();

        m_newCommandType = commandType;
        m_newParameterDefinitions = paramDefs;
        initializeMetadata();

        return;
    }

    /**
     * Allows the caller to modify this custom command's {@link CommandType type}. <i>Note that calling this method with
     * a command type that is not understood by a server-side command service will render this command inoperable.</i>
     *
     * @param newCommandType this command's new command type
     */
    public void setCommandType(CommandType newCommandType) {
        m_newCommandType = newCommandType;

        initializeMetadata();

        return;
    }

    /**
     * Allows the caller to modify this custom command's {@link ParameterDefinition parameter definitions}. <i>Note that
     * calling this method with definitions that are not understood by a server-side command service will render this
     * command inoperable.</i>
     *
     * <p>The given set of parameter definitions completely override the currently existing definitions (i.e. they are
     * not merged; the new definitions replace the old definitions).</p>
     *
     * <p>Changing the parameter definitions may invalidate the current set of parameter values. Calling
     * {@link AbstractCommand#checkParameterValidity(boolean)} will determine if the current parameter values are still
     * valid for the new parameter definitions.</p>
     *
     * @param newParameterDefinitions this command's new parameter definitions
     */
    public void setParameterDefinitions(ParameterDefinition[] newParameterDefinitions) {
        m_newParameterDefinitions = newParameterDefinitions;

        initializeMetadata();

        return;
    }

    /**
     * @see AbstractCommand#buildCommandType()
     */
    protected CommandType buildCommandType() {
        if (m_newCommandType == null) {
            m_newCommandType = new CommandType("unknown", 1);
        }

        return m_newCommandType;
    }

    /**
     * @see AbstractCommand#buildParameterDefinitions()
     */
    protected ParameterDefinition[] buildParameterDefinitions() {
        return m_newParameterDefinitions;
    }
}