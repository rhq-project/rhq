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
package org.rhq.core.domain.measurement;

/**
 * The availability of a resource. This enum's ordinals are important - DOWN must be 0 and UP must be 1 - this is to
 * support the ability to easily compute group availability through averaging the ordinals of multiple
 * {@link AvailabilityType} instances.
 *
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 */
public enum AvailabilityType {
    /** Resource is down for sure */
    DOWN,
    /** Resource is up */
    UP,
    /** Resource avail can't be determined. Typically meaning the agent is down */
    UNKNOWN,
    /** Resource is reporting but administratively disabled. Put another way, it is expectedly down */
    DISABLED,
    /** Resource is not just DOWN but is physically gone.  This is used to trigger automatic uninventory. It is
     *  converted to, or treated like DOWN other than for the purpose of automatic uninventory. */
    DEAD;

    /**
     * A Java bean style getter to allow us to access the enum name from JSPs or Facelets (e.g.
     * ${resource.resourceType.category.name}).
     *
     * @return the enum name
     */
    public String getName() {
        return name();
    }
}