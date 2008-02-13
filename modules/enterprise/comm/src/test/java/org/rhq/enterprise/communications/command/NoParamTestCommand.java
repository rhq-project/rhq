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
package org.rhq.enterprise.communications.command;

import java.util.Map;
import org.rhq.enterprise.communications.command.param.InvalidParameterDefinitionException;
import org.rhq.enterprise.communications.command.param.ParameterDefinition;

/**
 * Test command that accepts no parameters.
 *
 * @author John Mazzitelli
 */
public class NoParamTestCommand extends SimpleTestCommand {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link NoParamTestCommand}.
     *
     * @throws IllegalArgumentException
     * @throws InvalidParameterDefinitionException
     */
    public NoParamTestCommand() throws IllegalArgumentException, InvalidParameterDefinitionException {
        super();
    }

    /**
     * Constructor for {@link NoParamTestCommand}.
     *
     * @param  commandParameters
     *
     * @throws IllegalArgumentException
     * @throws InvalidParameterDefinitionException
     */
    public NoParamTestCommand(Map<String, Object> commandParameters) throws IllegalArgumentException,
        InvalidParameterDefinitionException {
        super(commandParameters);
    }

    /**
     * @see AbstractCommand#buildParameterDefinitions()
     */
    protected ParameterDefinition[] buildParameterDefinitions() {
        // this test command accepts no parameters
        return new ParameterDefinition[0];
    }
}