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
package org.rhq.enterprise.communications.command.param;

/**
 * An exception that denotes a bad parameter definition.
 *
 * @author John Mazzitelli
 */
public class InvalidParameterDefinitionException extends RuntimeException {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link InvalidParameterDefinitionException}.
     */
    public InvalidParameterDefinitionException() {
        super();
    }

    /**
     * Constructor for {@link InvalidParameterDefinitionException}.
     *
     * @param message
     */
    public InvalidParameterDefinitionException(String message) {
        super(message);
    }

    /**
     * Constructor for {@link InvalidParameterDefinitionException}.
     *
     * @param cause
     */
    public InvalidParameterDefinitionException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor for {@link InvalidParameterDefinitionException}.
     *
     * @param message
     * @param cause
     */
    public InvalidParameterDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}