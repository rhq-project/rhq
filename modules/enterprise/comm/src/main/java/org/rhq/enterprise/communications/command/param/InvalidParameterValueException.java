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
 * An exception that denotes a parameter value violated its allowed definition.
 *
 * @author John Mazzitelli
 */
public class InvalidParameterValueException extends RuntimeException {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link InvalidParameterValueException}.
     */
    public InvalidParameterValueException() {
        super();
    }

    /**
     * Constructor for {@link InvalidParameterValueException}.
     *
     * @param message
     */
    public InvalidParameterValueException(String message) {
        super(message);
    }

    /**
     * Constructor for {@link InvalidParameterValueException}.
     *
     * @param cause
     */
    public InvalidParameterValueException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor for {@link InvalidParameterValueException}.
     *
     * @param message
     * @param cause
     */
    public InvalidParameterValueException(String message, Throwable cause) {
        super(message, cause);
    }
}