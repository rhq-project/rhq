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

import java.util.Set;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceGroupComposite {
    private Double availability;
    private ResourceGroup resourceGroup;

    // TODO: Get rid of the permission field, since it's not used by the UI (ips, 08/29/07).
    private GroupCategory category;
    private ResourcePermission permission;
    private ResourceFacets resourceFacets;
    private long memberCount;

    public ResourceGroupComposite(Double availability, ResourceGroup resourceGroup, long memberCount) {
        this(availability, resourceGroup, memberCount, 1);
    }

    public ResourceGroupComposite(Double availability, ResourceGroup resourceGroup, long memberCount, Number control) {
        this.availability = availability;
        this.resourceGroup = resourceGroup;
        this.memberCount = memberCount;
        this.permission = new ResourcePermission(resourceGroup.getGroupCategory() == GroupCategory.COMPATIBLE, true,
            control.intValue() > 0, false, false, false);
        if (this.resourceGroup.getGroupCategory() == GroupCategory.COMPATIBLE) {
            this.category = GroupCategory.COMPATIBLE;
            ResourceType resourceType = this.resourceGroup.getResourceType();
            this.resourceFacets = new ResourceFacets(!resourceType.getMetricDefinitions().isEmpty(), resourceType
                .getResourceConfigurationDefinition() != null, !resourceType.getOperationDefinitions().isEmpty(),
                !resourceType.getPackageTypes().isEmpty(), exposesCallTimeMetrics(resourceType));
        } else if (this.resourceGroup.getGroupCategory() == GroupCategory.MIXED) {
            this.category = GroupCategory.MIXED;

            // Mixed groups don't support any of the resource facets.
            this.resourceFacets = new ResourceFacets(false, false, false, false, false);
        } else {
            throw new IllegalArgumentException("Unknown category " + this.resourceGroup.getGroupCategory()
                + " for ResourceGroup " + this.resourceGroup.getName());
        }
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

    public ResourcePermission getResourcePermission() {
        return this.permission;
    }

    public ResourceFacets getResourceFacets() {
        return this.resourceFacets;
    }

    @Override
    public String toString() {
        return "ResourceGroupComposite[" + "name=" + this.resourceGroup.getName() + ", members=" + this.memberCount
            + ", availability=" + this.availability + ", permission=" + this.permission + "]";
    }

    private static boolean exposesCallTimeMetrics(ResourceType resourceType) {
        Set<MeasurementDefinition> measurementDefs = resourceType.getMetricDefinitions();
        for (MeasurementDefinition measurementDef : measurementDefs) {
            if (measurementDef.getDataType() == DataType.CALLTIME) {
                return true;
            }
        }

        return false;
    }
}