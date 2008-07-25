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
package org.rhq.core.domain.resource.group.composite;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Maps a set of child resources or child subcategories as their summary information (counts and availability). Used for
 * child summaries views.
 *
 * @author Greg Hinkle
 */
public class AutoGroupComposite {
    private Double availability;
    private ResourceType resourceType;
    private ResourceSubCategory subcategory;
    private long memberCount;
    private int depth;
    private boolean mainResource;

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
        this.resources = new ArrayList<Resource>();
        if (other.resources != null) {
            this.resources.addAll(other.resources);
        }
    }

    public AutoGroupComposite(Double availability, ResourceType resourceType, long memberCount) {
        this.availability = availability;
        this.resourceType = resourceType;
        this.memberCount = memberCount;
    }

    public AutoGroupComposite(Double availability, ResourceSubCategory subcategory, long memberCount) {
        this.availability = availability;
        this.subcategory = subcategory;
        this.memberCount = memberCount;
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

    public ResourceSubCategory getSubcategory() {
        return subcategory;
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

    public boolean isMainResource() {
        return mainResource;
    }

    public void setMainResource(boolean mainResource) {
        this.mainResource = mainResource;
    }

    @Override
    public String toString() {
        return "AutoGroupComposite[" + ((resourceType != null) ? "Resource: " : "Subcategory: ") + "name="
            + ((resourceType != null) ? resourceType.getName() : subcategory.getName()) + ", members=" + memberCount
            + ", availability=" + availability + "]";
    }
}