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

package org.rhq.core.pc.configuration;

/**
 * A plugin can throw this exception to indicate to the container that there was a failure that was handled in some
 * way as opposed to an unexpected failure that is not handled. The container will includes the contents of the
 * <code>message</code> property in the response that is sent back to the server; whereas with any other exception, the
 * entire stack trace will be included in the response that sent back to the server.
 * <p/>
 * Consider the following scenario to illustrate an appropriate use of this exception. A plugin attempts to apply an
 * update, and it receives an error code from an external process indicating that the update failed. The plugin in turn
 * could throw a <code>ConfigurationUpdateException</code> including the error code along with other relevant
 * information to provide to additional context. That error message, as opposed to the full stack trace, will be
 * included in the response that is sent back to the server.
 */
public class ConfigurationUpdateException extends RuntimeException {

    public ConfigurationUpdateException() {
        super();
    }

    public ConfigurationUpdateException(String message) {
        super(message);
    }

    public ConfigurationUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationUpdateException(Throwable cause) {
        super(cause);
    }
}
