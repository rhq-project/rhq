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

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceGroupComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private Double availability;
    private ResourceGroup resourceGroup;

    private GroupCategory category;
    private ResourceFacets resourceFacets;
    private long memberCount;
    private long upCount;
    private long downCount;

    private class GroupDefinitionMember extends ResourceGroup {
        public void setGroupCategory(GroupCategory category) {
            super.setGroupCategory(category);
        }
    }

    private void init(Long upCount, Long downCount, Double availability, ResourceGroup resourceGroup, long memberCount) {
        if (upCount != null) {
            this.upCount = upCount;
        }
        if (downCount != null) {
            this.downCount = downCount;
        }

        this.availability = availability;
        this.resourceGroup = resourceGroup;
        this.memberCount = memberCount;

        if (this.resourceGroup.getGroupCategory() == GroupCategory.COMPATIBLE) {
            this.category = GroupCategory.COMPATIBLE;
            ResourceType resourceType = this.resourceGroup.getResourceType();
            this.resourceFacets = new ResourceFacets(resourceType);
        } else if (this.resourceGroup.getGroupCategory() == GroupCategory.MIXED) {
            this.category = GroupCategory.MIXED;

            // Mixed groups don't support any of the resource facets.
            this.resourceFacets = new ResourceFacets(false, false, false, false, false, false, false);
        } else {
            throw new IllegalArgumentException("Unknown category " + this.resourceGroup.getGroupCategory()
                + " for ResourceGroup " + this.resourceGroup.getName());
        }
    }

    public ResourceGroupComposite(Long upCount, Long downCount, ResourceGroup group) {
        Double availability = null;
        Long memberCount = null;
        if (upCount != null && downCount != null) {
            availability = upCount / (double) (upCount + downCount);
            memberCount = upCount + downCount;
        }
        init(upCount, downCount, availability, group, memberCount);
    }

    public ResourceGroupComposite(Long upCount, Long downCount, long memberCount, Number groupId, String groupName,
        GroupCategory groupCategory, String groupByClause) {
        Double availability = null;
        if (upCount != null && downCount != null) {
            availability = upCount / (double) downCount;
        }
        GroupDefinitionMember group = new GroupDefinitionMember();
        group.setId(groupId.intValue());
        group.setName(groupName);
        group.setGroupCategory(groupCategory);
        group.setGroupByClause(groupByClause);
        init(upCount, downCount, availability, group, memberCount);
    }

    public ResourceGroupComposite(Double availability, ResourceGroup resourceGroup, long memberCount) {
        Long up = null;
        Long down = null;
        if (availability != null) {
            up = (long) (memberCount * availability);
            down = memberCount - up;
        }
        init(up, down, availability, resourceGroup, memberCount);
    }

    public Double getAvailability() {
        return this.availability;
    }

    public ResourceGroup getResourceGroup() {
        return this.resourceGroup;
    }

    public GroupCategory getCategory() {
        return this.category;
    }

    public long getMemberCount() {
        return this.memberCount;
    }

    public long getUpCount() {
        return this.upCount;
    }

    public long getDownCount() {
        return this.downCount;
    }

    public ResourceFacets getResourceFacets() {
        return this.resourceFacets;
    }

    @Override
    public String toString() {
        return "ResourceGroupComposite[" + "name=" + this.resourceGroup.getName() + ", up=," + this.upCount + " down=,"
            + this.downCount + " members=" + this.memberCount + ", availability=" + this.availability + ", permission="
            + "]";
    }
}