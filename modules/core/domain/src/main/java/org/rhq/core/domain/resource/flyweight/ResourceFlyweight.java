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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains only a minimal subset of properties of a Resource that are needed for display
 * purposes.
 * 
 * @author Lukas Krejci
 */
public class ResourceFlyweight implements Serializable {

    private static final long serialVersionUID = 1L;

    private ResourceFlyweight parentResource;
    private String uuid;
    private int id;
    private String name;
    private String resourceKey;
    private ResourceAvailabilityFlyweight currentAvailability;
    private ResourceTypeFlyweight resourceType;
    private boolean locked;
    private List<ResourceFlyweight> childResources;

    public ResourceFlyweight() {

    }

    public ResourceFlyweight getParentResource() {
        return parentResource;
    }

    public void setParentResource(ResourceFlyweight parent) {
        this.parentResource = parent;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public ResourceAvailabilityFlyweight getCurrentAvailability() {
        return currentAvailability;
    }

    public void setCurrentAvailability(ResourceAvailabilityFlyweight currentAvailability) {
        this.currentAvailability = currentAvailability;
    }

    public ResourceTypeFlyweight getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceTypeFlyweight resourceType) {
        this.resourceType = resourceType;
    }

    public List<ResourceFlyweight> getChildResources() {
        if (childResources == null) {
            childResources = new ArrayList<ResourceFlyweight>();
        }
        return childResources;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
    
    @Override
    public int hashCode() {
        return ((uuid != null) ? uuid.hashCode() : 0);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || !(o instanceof ResourceFlyweight)) {
            return false;
        }

        final ResourceFlyweight resource = (ResourceFlyweight) o;

        if ((uuid != null) ? (!uuid.equals(resource.uuid)) : (resource.uuid != null)) {
            return false;
        }

        return true;
    }    
}
