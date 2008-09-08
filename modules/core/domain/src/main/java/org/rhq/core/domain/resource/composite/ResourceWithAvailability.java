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

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;

public class ResourceWithAvailability extends ResourceMembershipComposite {

    private static final long serialVersionUID = 1L;

    private AvailabilityType availability;

    public ResourceWithAvailability(Resource resource, AvailabilityType availabilityType) {
        super(resource, 0, 0);
        this.availability = availabilityType;
    }

    public ResourceWithAvailability(Resource resource, AvailabilityType availabilityType, Number explicitCount,
        Number implicitCount) {
        super(resource, null, explicitCount, implicitCount);
        this.availability = availabilityType;
    }

    public ResourceWithAvailability(Resource resource, Resource parent, AvailabilityType availabilityType,
        Number explicitCount, Number implicitCount) {
        super(resource, parent, explicitCount, implicitCount);
        this.availability = availabilityType;
    }

    public AvailabilityType getAvailability() {
        return availability;
    }

    public void setAvailability(AvailabilityType availability) {
        this.availability = availability;
    }

    @Override
    public String toString() {
        return "ResourceWithAvailability[" + ((availability == null) ? "unknown" : availability.toString()) + ", "
            + super.toString() + "]";
    }
}