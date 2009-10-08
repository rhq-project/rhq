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
 * This Session base class is thrown when there are session related issues such as {@link SessionNotFoundException} and
 * {@link SessionTimeoutException}.
 */
public class SessionException extends Exception {
    private static final long serialVersionUID = 1L;

    public SessionException() {
        super();
    }

    public SessionException(String s) {
        super(s);
    }

    public SessionException(Throwable t) {
        super(t);
    }

    public SessionException(String s, Throwable t) {
        super(s, t);
    }
}