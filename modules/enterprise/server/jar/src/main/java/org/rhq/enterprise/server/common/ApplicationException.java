/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.enterprise.server.common;


/**
 * Indicates a general server-side application exception. Exception details should be provided in the message.
 * <p/>
 * Declare this an {@link javax.ejb.ApplicationException} because we don't want these to be wrapped or to rollback an ongoing
 * transaction.
 */
@javax.ejb.ApplicationException(rollback = false, inherited = true)
public class ApplicationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    // Default no-arg constructor required by JAXB
    public ApplicationException() {
    }

    public ApplicationException(String message) {
        super(message);
    }

    /**
     * Discouraged unless you really want to include a long, often useless trace.
     * @param t
     */
    public ApplicationException(Throwable t) {
        super(t);
    }

    /**
     * Discouraged unless you really want to include a long, often useless trace.
     * @param t
     */
    public ApplicationException(String s, Throwable t) {
        super(s, t);
    }
}