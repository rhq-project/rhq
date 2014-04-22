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

import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;

/**
 * This class is mostly a copy of {@link AutoGroupComposite} but is linked together
 * with {@link ResourceFlyweight}, {@link ResourceTypeFlyweight} and {@link ResourceSubCategoryFlyweight} instances as opposed to
 * their fully featured counterparts which is  what makes this class flyweight.
 * <p>
 * In addition to the {@link AutoGroupComposite}, this class contains some more properties used
 * in the UI layer.
 *
 * @see AutoGroupComposite
 *
 * @author Lukas Krejci
 */
public class AutoGroupCompositeFlyweight implements Serializable {

    private static final long serialVersionUID = 1L;

    private Double availability;
    private ResourceTypeFlyweight resourceType;
    private String subcategory;
    private long memberCount;
    private int depth;
    private boolean mainResource;
    private ResourceFlyweight parentResource;
    private String name;
    private MembersAvailabilityHint membersAvailabilityHint;
    private MembersCategoryHint membersCategoryHint;

    private List<ResourceFlyweight> resources;

    public AutoGroupCompositeFlyweight(AutoGroupCompositeFlyweight other) {
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
        this.resources = new ArrayList<ResourceFlyweight>();
        if (other.resources != null) {
            this.resources.addAll(other.resources);
        }
        this.name = other.name;
    }

    public AutoGroupCompositeFlyweight(Double availability, ResourceFlyweight parentResource,
        ResourceTypeFlyweight resourceType, long memberCount) {
        this(availability, parentResource, resourceType, memberCount, false);
    }

    public AutoGroupCompositeFlyweight(Double availability, ResourceFlyweight parentResource,
        ResourceTypeFlyweight resourceType, long memberCount, boolean isDuplicateResourceTypeName) {
        this.availability = availability;
        this.parentResource = parentResource;
        this.resourceType = resourceType;
        this.memberCount = memberCount;
        if (isDuplicateResourceTypeName) {
            this.name = this.resourceType.getName() + " (" + this.resourceType.getPlugin() + " plugin)";
        } else {
            this.name = this.resourceType.getName();
        }
    }

    public AutoGroupCompositeFlyweight(Double availability, ResourceFlyweight parentResource,
 String subcategory,
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

    public ResourceTypeFlyweight getResourceType() {
        return resourceType;
    }

    public long getMemberCount() {
        return memberCount;
    }

    public void setResources(List<ResourceFlyweight> resources) {
        this.resources = resources;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public ResourceFlyweight getParentResource() {
        return parentResource;
    }

    public void setParentResource(ResourceFlyweight parentResource) {
        this.parentResource = parentResource;
    }

    public List<ResourceFlyweight> getResources() {
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

    public MembersAvailabilityHint getMembersAvailabilityHint() {
        return membersAvailabilityHint;
    }

    public void setMembersAvailabilityHint(MembersAvailabilityHint membersAvailabilityHint) {
        this.membersAvailabilityHint = membersAvailabilityHint;
    }

    public MembersCategoryHint getMembersCategoryHint() {
        return membersCategoryHint;
    }

    public void setMembersCategoryHint(MembersCategoryHint membersCategoryHint) {
        this.membersCategoryHint = membersCategoryHint;
    }

    @Override
    public String toString() {
        return "AutoGroupCompositeFlyweight[" + ((this.resourceType != null) ? "Resource: " : "Subcategory: ")
            + "name=" + this.name + ", members=" + this.memberCount + ", availability=" + this.availability + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof AutoGroupCompositeFlyweight)) {
            return false;
        }

        AutoGroupCompositeFlyweight other = (AutoGroupCompositeFlyweight) o;

        if (!parentResource.equals(other.parentResource)) {
            return false;
        }

        if (resourceType != null) {
            if (!resourceType.equals(other.resourceType)) {
                return false;
            }
        }

        if (subcategory != null) {
            if (!subcategory.equals(other.subcategory)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = parentResource.hashCode();

        if (resourceType != null) {
            hash *= resourceType.hashCode();
        }

        if (subcategory != null) {
            hash *= subcategory.hashCode();
        }

        return hash;
    }
}
