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

    private Double implicitAvail;
    private Double explicitAvail;
    private ResourceGroup resourceGroup;

    private GroupCategory category;
    private ResourceFacets resourceFacets;
    private long implicitUp;
    private long implicitDown;
    private long explicitUp;
    private long explicitDown;

    private class GroupDefinitionMember extends ResourceGroup {
        public void setGroupCategory(GroupCategory category) {
            super.setGroupCategory(category);
        }
    }

    public ResourceGroupComposite(long explicitCount, double explicitAvailability, long implicitCount,
        double implicitAvailability, ResourceGroup resourceGroup) {

        explicitUp = Math.round(explicitCount * explicitAvailability);
        explicitDown = explicitCount - explicitUp;
        if (explicitUp + explicitDown > 0) {
            // keep explicitAvail null if there are no explicit resources in the group
            explicitAvail = explicitAvailability;
        }

        implicitUp = Math.round(implicitCount * implicitAvailability);
        implicitDown = implicitCount - implicitUp;
        if (implicitUp + implicitDown > 0) {
            // keep implicitAvail null if there are no implicit resources in the group
            implicitAvail = implicitAvailability;
        }

        this.resourceGroup = resourceGroup;

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

    public Double getImplicitAvail() {
        return this.implicitAvail;
    }

    public Double getExplicitAvail() {
        return this.explicitAvail;
    }

    public ResourceGroup getResourceGroup() {
        return this.resourceGroup;
    }

    public GroupCategory getCategory() {
        return this.category;
    }

    public long getImplicitUp() {
        return this.implicitUp;
    }

    public long getImplicitDown() {
        return this.implicitDown;
    }

    public long getExplicitUp() {
        return this.explicitUp;
    }

    public long getExplicitDown() {
        return this.explicitDown;
    }

    public String getExplicitFormatted() {
        StringBuilder results = new StringBuilder();
        if (getExplicitUp() > 0) {
            results.append(getExplicitUp());
            results.append("<img src=\"/images/icons/availability_green_16.png\" />");
        }
        if (getExplicitUp() > 0 && getExplicitDown() > 0) {
            results.append(" / ");
        }
        if (getExplicitDown() > 0) {
            results.append(getExplicitDown());
            results.append("<img src=\"/images/icons/availability_red_16.png\" />");
        }
        return results.toString();
    }

    public String getImplicitFormatted() {
        StringBuilder results = new StringBuilder();
        if (getImplicitUp() > 0) {
            results.append(getImplicitUp());
            results.append("<img src=\"/images/icons/availability_green_16.png\" />");
        }
        if (getImplicitUp() > 0 && getImplicitDown() > 0) {
            results.append(" / ");
        }
        if (getImplicitDown() > 0) {
            results.append(getImplicitDown());
            results.append("<img src=\"/images/icons/availability_red_16.png\" />");
        }
        return results.toString();
    }

    public ResourceFacets getResourceFacets() {
        return this.resourceFacets;
    }

    @Override
    public String toString() {
        return "ResourceGroupComposite[name="
            + this.resourceGroup.getName() //
            + ", implicit[up/down/avail=," + this.implicitUp + "/" + this.implicitDown + "/" + this.implicitAvail + "]"
            + ", explicit[up/down/avail=," + this.explicitUp + "/" + this.explicitDown + "/" + this.explicitAvail + "]"
            + ", permission=" + "]";
    }
}