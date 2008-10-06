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
package org.rhq.core.domain.resource.composite;

import java.io.Serializable;
import java.util.Set;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

// intended to be used for ResourceManager.getAvailableResourcesForResourceGroup
public class ResourceMembershipComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private Resource resource;
    private Resource parent;
    private ResourceFacets resourceFacets;
    private boolean explicit;
    private boolean implicit;

    public ResourceMembershipComposite(Resource resource, Number explicitCount, Number implicitCount) {
        this(resource, null, explicitCount, implicitCount);
    }

    public ResourceMembershipComposite(Resource resource, Resource parent, Number explicitCount, Number implicitCount) {
        this.resource = resource;
        this.parent = parent;
        ResourceType resourceType = this.resource.getResourceType();
        this.resourceFacets = new ResourceFacets(!resourceType.getMetricDefinitions().isEmpty(), resourceType
            .getResourceConfigurationDefinition() != null, !resourceType.getOperationDefinitions().isEmpty(),
            !resourceType.getPackageTypes().isEmpty(), exposesCallTimeMetrics(resourceType));

        this.explicit = (explicitCount.intValue() > 0);
        this.implicit = (implicitCount.intValue() > 0);
    }

    public Resource getResource() {
        return resource;
    }

    public Resource getParent() {
        return parent;
    }

    public ResourceFacets getResourceFacets() {
        return resourceFacets;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public boolean getExplicit() {
        return explicit;
    }

    public void setExplicit(boolean isExplicit) {
        this.explicit = isExplicit;
    }

    public boolean getImplicit() {
        return implicit;
    }

    public void setImplicit(boolean isImplicit) {
        this.implicit = isImplicit;
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

    @Override
    public String toString() {
        return "ResourceMembershipComposite[" + resource.toString() + ", " + "explicit" + explicit + ", " + "implicit"
            + implicit + "]";
    }
}