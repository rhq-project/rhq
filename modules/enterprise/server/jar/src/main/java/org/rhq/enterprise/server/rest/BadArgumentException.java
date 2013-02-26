/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.rest;

import javax.ejb.ApplicationException;

/**
 * Exception if arguments are bad.
 * Don't use java.lang.IllegalArgumentException, as this is no
 * Application Exception and gets wrapped
 * @author Heiko W. Rupp
 */
@ApplicationException(rollback = false, inherited = true)
public class BadArgumentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BadArgumentException(String message) {
        super("Bad parameter(s) given: " + message);
    }

    /**
     * Construct a new instance of this Exception.
     * @param parameterName Denotes which parameter is bad
     * @param cause Denotes why it is bad
     */
    public BadArgumentException(String parameterName, String cause) {
        super("Parameter " + parameterName + " is bad: " + cause);
    }

}
