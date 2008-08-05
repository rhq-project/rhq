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
package org.rhq.core.domain.resource.composite;

import org.rhq.core.domain.measurement.Availability;

import java.io.Serializable;

/**
 * This object is meant as part of a query that returns all resources and their availabilities, so it needs to have a
 * very small footprint. We only need the resource IDs and the availabilities.
 *
 * @author John Mazzitelli
 */
public class ResourceIdWithAvailabilityComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int resourceId;
    private final Availability availability;

    public ResourceIdWithAvailabilityComposite(int resourceId, Availability avail) {
        this.resourceId = resourceId;
        this.availability = avail;
    }

    public int getResourceId() {
        return resourceId;
    }

    public Availability getAvailability() {
        return availability;
    }
}