/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.pc.inventory;

import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;

/**
 * This exception is thrown when a method invoked on a proxied plugin component times out. If possible, its cause
 * will be set to an Exception containing the stack trace of the plugin component thread that timed out.
 *
 * @see ResourceContainer#createResourceComponentProxy(Class, FacetLockType, long, boolean, boolean)
 * @see ComponentUtil#getComponent(int, Class, FacetLockType, long, boolean, boolean)
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
