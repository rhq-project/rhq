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
package org.rhq.enterprise.gui.legacy.exception;

/**
 * Implementation of <strong>Exception</strong> for parameter parsing
 */
public class ParameterNotFoundException extends RuntimeException {
    /**
     * Creates new <code>ParameterNotFoundException</code> without detail message.
     */
    public ParameterNotFoundException() {
        super();
    }

    /**
     * Constructs an <code>ParameterNotFoundException</code> with the specified detail message.
     *
     * @param message the detail message.
     */
    public ParameterNotFoundException(String message) {
        super(message);
    }
}