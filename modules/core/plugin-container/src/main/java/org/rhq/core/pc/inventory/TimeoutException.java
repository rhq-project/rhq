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
package org.rhq.core.pc.inventory;

import org.rhq.core.pc.util.ComponentUtil;

/**
 * This exception is thrown when a method invoked on a proxied plugin component times out. If possible, its cause
 * will be set to an Exception containing the stack trace of the plugin component thread that timed out.
 *
 * @see ResourceContainer#createResourceComponentProxy(Class, org.rhq.core.pc.util.FacetLockType, long, boolean, boolean, boolean)
 * @see ComponentUtil#getComponent(int, Class, org.rhq.core.pc.util.FacetLockType, long, boolean, boolean, boolean)
 *
 * @author Ian Springer
 */
public class TimeoutException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TimeoutException(String message) {
        super(message);
    }

    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
