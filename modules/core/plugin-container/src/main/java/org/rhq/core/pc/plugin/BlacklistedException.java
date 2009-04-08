/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pc.plugin;

/**
 * This is thrown when a plugin or one of its components has been blacklisted.
 * This means that plugin or component is not allowed to be used or accessed.
 * 
 * @author John Mazzitelli
 */
public class BlacklistedException extends Exception {
    private static final long serialVersionUID = 1L;

    public BlacklistedException() {
        super();
    }

    public BlacklistedException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlacklistedException(String message) {
        super(message);
    }

    public BlacklistedException(Throwable cause) {
        super(cause);
    }
}
