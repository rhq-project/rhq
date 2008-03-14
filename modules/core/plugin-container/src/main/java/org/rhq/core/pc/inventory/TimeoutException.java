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
package org.rhq.core.pc.inventory;

import org.rhq.core.pc.util.FacetLockType;

/**
 * This exception is thrown when a method invoked on a proxied
 * {@link org.rhq.core.pluginapi.inventory.ResourceComponent} times out.
 *
 * @see ResourceContainer#createResourceComponentProxy(Class, FacetLockType, long, boolean, boolean)
 * @see org.rhq.core.pc.util.ComponentUtil#getComponent(int, Class, FacetLockType, long, boolean, boolean)
 *
 * @author Ian Springer
 */
public class TimeoutException extends RuntimeException {
    public TimeoutException(String message) {
        super(message);
    }
}
