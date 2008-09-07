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

import java.io.Serializable;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 * @author Ian Springer
 */
public class ResourceComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement
    private Resource resource;

    @XmlElement
    private Resource parent;

    @XmlElement
    private ResourcePermission resourcePermission;

    @XmlElement
    private AvailabilityType availability;

    private ResourceFacets resourceFacets;

    public ResourceComposite() {
    }

    /**
     * Provides full access permissions - used for admin queries.
     */
    public ResourceComposite(Resource resource, AvailabilityType availability) {
        this(resource, null, availability, new ResourcePermission());
    }

    /**
     * Provides full access permissions - used for admin queries.
     */
    public ResourceComposite(Resource resource, Resource parent, AvailabilityType availability) {
        this(resource, parent, availability, new ResourcePermission());
    }

    /**
     * Provides specified permissions - used for non-admin queries.
     */
    public ResourceComposite(Resource resource, AvailabilityType availability, Number measure, Number inventory,
        Number control, Number alert, Number configure, Number content, Number createChildResources,
        Number deleteResources) {
        this(resource, null, availability, new ResourcePermission(measure.intValue() > 0, inventory.intValue() > 0,
            control.intValue() > 0, alert.intValue() > 0, configure.intValue() > 0, content.intValue() > 0,
            createChildResources.intValue() > 0, deleteResources.intValue() > 0));
    }

    /**
     * Provides specified permissions - used for non-admin queries.
     */
    public ResourceComposite(Resource resource, Resource parent, AvailabilityType availability, Number measure,
        Number inventory, Number control, Number alert, Number configure, Number content, Number createChildResources,
        Number deleteResources) {
        this(resource, parent, availability, new ResourcePermission(measure.intValue() > 0, inventory.intValue() > 0,
            control.intValue() > 0, alert.intValue() > 0, configure.intValue() > 0, content.intValue() > 0,
            createChildResources.intValue() > 0, deleteResources.intValue() > 0));
    }

    /**
     * Private constructor that both public constructors delegate to.
     */
    private ResourceComposite(Resource resource, Resource parent, AvailabilityType availability,
        ResourcePermission resourcePermission) {
        this.resource = resource;
        this.parent = parent;
        this.availability = availability;
        this.resourcePermission = resourcePermission;

        // TODO: Add support for retrieving the supported facets to the Resource.QUERY_FIND_COMPOSITE_COUNT query.
        ResourceType resourceType = this.resource.getResourceType();
        this.resourceFacets = new ResourceFacets(!resourceType.getMetricDefinitions().isEmpty(), resourceType
            .getResourceConfigurationDefinition() != null, !resourceType.getOperationDefinitions().isEmpty(),
            !resourceType.getPackageTypes().isEmpty(), exposesCallTimeMetrics(resourceType));
    }

    public Resource getResource() {
        return resource;
    }

    public Resource getParent() {
        return parent;
    }

    public AvailabilityType getAvailability() {
        return availability;
    }

    public ResourcePermission getResourcePermission() {
        return resourcePermission;
    }

    public ResourceFacets getResourceFacets() {
        return resourceFacets;
    }

    @Override
    public String toString() {
        return "[ResourceComposite] Resource: " + resource + "\n\tAvailability: " + availability + "\n\tPermissions: "
            + resourcePermission + "\n\tFacets: " + resourceFacets;
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