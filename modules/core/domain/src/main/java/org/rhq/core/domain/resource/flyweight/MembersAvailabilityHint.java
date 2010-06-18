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

package org.rhq.core.domain.resource.flyweight;

import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * Hints about the availability of resources in an {@link AutoGroupCompositeFlyweight}.
 * 
 * @author Lukas Krejci
 */
public enum MembersAvailabilityHint {

    /**
     * All member resources are up.
     */
    UP,

    /**
     * Some member resources are down.
     */
    DOWN,

    /**
     * Some member resources don't have a known availability state.
     */
    UNKNOWN;

    public static MembersAvailabilityHint fromAvailabilityType(AvailabilityType availType) {
        if (availType == null) {
            return UNKNOWN;
        }
        
        switch (availType) {
        case UP:
            return UP;
        case DOWN:
            return DOWN;
        default:
            return UNKNOWN;
        }
    }
    
    public String toString() {
        return name().toLowerCase();
    }
}
