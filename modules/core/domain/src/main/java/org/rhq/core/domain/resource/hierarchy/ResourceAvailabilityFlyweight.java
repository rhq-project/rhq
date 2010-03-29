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

package org.rhq.core.domain.resource.hierarchy;

import java.io.Serializable;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;

/**
 * This is basically a copy of {@link ResourceAvailability} class, but is linked to the
 * {@link ResourceFlyweight} instead of Resource. This enables us to retain the same API
 * for the flyweights as for the original domain entities.
 * 
 * @author Lukas Krejci
 */
public class ResourceAvailabilityFlyweight implements Serializable {

    private ResourceFlyweight resource;
    private int resourceId;
    private AvailabilityType availabilityType;
    
    public ResourceAvailabilityFlyweight(ResourceFlyweight resource, AvailabilityType type) {
        this.resource = resource;
        this.resourceId = resource.getId();
        this.availabilityType = type;
    }

    public ResourceFlyweight getResource() {
        return resource;
    }

    public void setResource(ResourceFlyweight resource) {
        this.resource = resource;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public AvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    public void setAvailabilityType(AvailabilityType availabilityType) {
        this.availabilityType = availabilityType;
    }
}
