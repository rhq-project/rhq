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

import org.rhq.core.domain.resource.Resource;

/**
 * Reduced access version of the Resource object.
 * Signifies to the client that the user has no true
 * visibility to this resource.
 *
 * @author Greg Hinkle
 */
public class LockedResource extends Resource {

    public LockedResource() {
    }


    public LockedResource(Resource resource) {
        setParentResource(resource.getParentResource());
        setResourceType(resource.getResourceType());
        setId(resource.getId());
        setUuid(resource.getUuid());
        setName(getResourceType().getName() + " (locked)");
        setCurrentAvailability(resource.getCurrentAvailability());
    }

    @Override
    public String toString() {
        return getResourceType().toString() + "(locked)";
    }
}