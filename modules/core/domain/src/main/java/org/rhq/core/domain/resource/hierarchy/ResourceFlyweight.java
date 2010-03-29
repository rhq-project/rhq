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
import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

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

    /**
     * @see #construct(int, String, String, Integer, int, AvailabilityType, FlyweightCache)
     * 
     * @param original the resource
     * @param cache the cached flyweights
     * @return the initialized resource flyweight
     */
    public static ResourceFlyweight construct(Resource original, FlyweightCache cache) {
        int id = original.getId();
        String name = original.getName();
        String uuid = original.getUuid();
        String resourceKey = original.getResourceKey();
        Resource parent = original.getParentResource();
        ResourceType type = original.getResourceType();
        ResourceAvailability avail = original.getCurrentAvailability();

        return construct(id, name, uuid, resourceKey, parent != null ? parent.getId() : null, type.getId(),
            avail != null ? avail.getAvailabilityType() : null, cache);
    }

    /**
     * Constructs a fully initialized instance of the resource flyweight.
     * The resource type, sub-category and parent are looked up
     * in the provided cache. If not found, new instances are created and added
     * to the cache.
     * <p>
     * Note that if the parentId is not null and not found in the cache, a new flyweight
     * is created for the parent, initialized only with the id.
     * <p>
     * The type is supposed to exist in the cache already. If it doesn't, no type is assigned
     * to the returned resource flyweight.
     * <p> 
     * If a corresponding flyweight for the provided resource id is already found in the cache,
     * it is refreshed with the data provided to this call.
     * 
     * @param id the resource id
     * @param name the resource name
     * @param uuid the resoure uuid
     * @param resourceKey the resource key
     * @param parentId the id of the parent resource
     * @param typeId the id of the resource type
     * @param currentAvailability the availability of the resource
     * @param cache the cached flyweights
     * @return
     */
    public static ResourceFlyweight construct(int id, String name, String uuid, String resourceKey, Integer parentId,
        int typeId, AvailabilityType currentAvailability, FlyweightCache cache) {

        ResourceFlyweight ret = cache.getResources().get(id);
        if (ret == null) {
            ret = new ResourceFlyweight();
            cache.getResources().put(id, ret);
        }

        ret.setId(id);
        ret.setName(name);
        ret.setUuid(uuid);
        ret.setResourceKey(resourceKey);
        ret.setCurrentAvailability(new ResourceAvailabilityFlyweight(ret, currentAvailability));

        if (parentId != null) {
            ResourceFlyweight parent = cache.getResources().get(parentId);
            if (parent == null) {
                parent = construct(parentId, null, null, null, null, -1, null, cache);
                parent.getChildResources().add(ret);
            }
            ret.setParentResource(parent);
        } else {
            ResourceFlyweight previousParent = ret.getParentResource();
            if (previousParent != null) {
                previousParent.getChildResources().remove(ret);
            }
            ret.setParentResource(null);
        }

        ret.setResourceType(cache.getResourceTypes().get(typeId));

        return ret;
    }

    public ResourceFlyweight() {

    }

    /**
     * Constructs a new flyweight from the resource.
     * Note that this does *NOT* initialize the parentResource, resourceType properties as that would create new instances of those
     * types which might not be what the user of this constructor wanted.
     * 
     * @param resource
     */
    public ResourceFlyweight(Resource resource) {
        setId(resource.getId());
        setName(resource.getName());
        setUuid(resource.getUuid());
        setResourceKey(resource.getResourceKey());
        ResourceAvailability avail = resource.getCurrentAvailability();
        setCurrentAvailability(new ResourceAvailabilityFlyweight(this, avail != null ? avail.getAvailabilityType()
            : null));
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
    
    public int hashCode() {
        return id;
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof ResourceFlyweight)) {
            return false;
        }
        
        return id == ((ResourceFlyweight)o).getId();
    }
}
