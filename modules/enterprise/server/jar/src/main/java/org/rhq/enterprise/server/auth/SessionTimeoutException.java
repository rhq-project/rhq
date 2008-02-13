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
package org.rhq.enterprise.server.auth;

/**
 * Occurs when the {@link SessionManager} has a session but its timeout has expired.
 */
public class SessionTimeoutException extends SessionException {
    private static final long serialVersionUID = 1L;

    public SessionTimeoutException() {
        super();
    }

    public SessionTimeoutException(String s) {
        super(s);
    }

    public SessionTimeoutException(Throwable t) {
        super(t);
    }

    public SessionTimeoutException(String s, Throwable t) {
        super(s, t);
    }
}