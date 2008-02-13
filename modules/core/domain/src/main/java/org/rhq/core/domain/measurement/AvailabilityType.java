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
package org.rhq.core.domain.measurement;

/**
 * The availability of a resource. This enum's ordinals are important - DOWN must be 0 and UP must be 1 - this is to
 * support the ability to easily compute group/aggregate availability through averaging the ordinals of multiple
 * {@link AvailabilityType} instances.
 *
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 */
public enum AvailabilityType {
    /** Resource is down for sure */
    DOWN,
    /** Resource is up */
    UP;

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