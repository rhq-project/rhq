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
package org.rhq.core.domain.resource.group.composite;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Maps a set of child resources or child subcategories as their summary information (counts and availability). Used for
 * child summaries views.
 *
 * @author Greg Hinkle
 */
public class AutoGroupComposite implements Serializable {

    private static final long serialVersionUID = 2L;

    private Double availability;
    private ResourceType resourceType;
    private String subcategory;
    private long memberCount;
    private int depth;
    private boolean mainResource;
    private Resource parentResource;
    private String name;

    private List<Resource> resources;

    public AutoGroupComposite(AutoGroupComposite other) {
        super();

        if (other == null) {
            return; // Throw exception?
        }

        this.availability = other.availability;
        this.resourceType = other.resourceType;
        this.subcategory = other.subcategory;
        this.memberCount = other.memberCount;
        this.depth = other.depth;
        this.mainResource = other.mainResource;
        this.parentResource = other.parentResource;
        this.resources = new ArrayList<Resource>();
        if (other.resources != null) {
            this.resources.addAll(other.resources);
        }
        this.name = other.name;
    }

    public AutoGroupComposite(Double availability, Resource parentResource, ResourceType resourceType, long memberCount) {
        this(availability, parentResource, resourceType, memberCount, false);
    }

    public AutoGroupComposite(Double availability, Resource parentResource, ResourceType resourceType, long memberCount,
                              boolean isDuplicateResourceTypeName) {
        this.availability = availability;
        this.parentResource = parentResource;
        this.resourceType = resourceType;
        this.memberCount = memberCount;
        if (isDuplicateResourceTypeName) {
            this.name = this.resourceType.getName() + "(" + this.resourceType.getPlugin() + " plugin)";
        } else {
            this.name = this.resourceType.getName();
        }
    }

    public AutoGroupComposite(Double availability, Resource parentResource, String subcategory,
        long memberCount) {
        this.availability = availability;
        this.parentResource = parentResource;
        this.subcategory = subcategory;
        this.memberCount = memberCount;
        this.name = this.subcategory;
    }

    public Double getAvailability() {
        return availability;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public long getMemberCount() {
        return memberCount;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public Resource getParentResource() {
        return parentResource;
    }

    public void setParentResource(Resource parentResource) {
        this.parentResource = parentResource;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int increaseDepth(int increment) {
        depth += increment;
        return depth;
    }

    public boolean isMainResource() {
        return mainResource;
    }

    public void setMainResource(boolean mainResource) {
        this.mainResource = mainResource;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "AutoGroupComposite[" + ((this.resourceType != null) ? "Resource: " : "Subcategory: ") + "name="
                + this.name + ", members=" + this.memberCount + ", availability=" + this.availability + "]";
    }
}