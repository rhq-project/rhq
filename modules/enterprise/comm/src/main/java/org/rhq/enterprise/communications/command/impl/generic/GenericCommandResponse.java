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

import org.rhq.enterprise.communications.command.AbstractCommandResponse;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;

/**
 * Provides a concrete implementation of the {@link AbstractCommandResponse base command response} object. This class
 * does not define any strongly typed methods to extract specific data from the
 * {@link AbstractCommandResponse#getResults() results object} since this class represents a generic command response
 * from an unknown, custom command.
 *
 * @author John Mazzitelli
 */
public class GenericCommandResponse extends AbstractCommandResponse {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link GenericCommandResponse}.
     *
     * @see AbstractCommandResponse#AbstractCommandResponse(Command)
     */
    public GenericCommandResponse(Command command) {
        super(command);
    }

    /**
     * Constructor for {@link GenericCommandResponse}.
     *
     * @see AbstractCommandResponse#AbstractCommandResponse(Command, boolean, Object, Throwable)
     */
    public GenericCommandResponse(Command command, boolean success, Object results, Throwable exception) {
        super(command, success, results, exception);
    }

    /**
     * Constructor for {@link GenericCommandResponse}.
     *
     * @see AbstractCommandResponse#AbstractCommandResponse(CommandResponse)
     */
    public GenericCommandResponse(CommandResponse responseToTransform) {
        super(responseToTransform);
    }
}