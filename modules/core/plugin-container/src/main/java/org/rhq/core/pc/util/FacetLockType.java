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
package org.rhq.core.pc.util;

/**
 * Defines the lock that should be obtained before a facet interface method is invoked.
 *
 * @author John Mazzitelli
 */
public enum FacetLockType {
    /**
     * Indicates that a facet method invocation does not need to be synchronized
     * at all and can be performed concurrently.
     */
    NONE,

    /**
     * Indicates that a facet method is being called with the intention of only needing to read data
     * from a managed resource and thus only needs to acquire a read-lock before it can be invoked.
     */
    READ,

    /**
     * Indicates that a facet method is being called with the intention of possibly changing
     * a managed resource and thus needs to acquire a full write-lock before it can be invoked.
     */
    WRITE
}