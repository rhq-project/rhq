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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;

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

    @XmlTransient
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
        Number control, Number alert, Number event, Number configureRead, Number configureWrite, Number content,
        Number createChildResources, Number deleteResources) {
        this(resource, null, availability, new ResourcePermission(measure.intValue() > 0, inventory.intValue() > 0,
            control.intValue() > 0, alert.intValue() > 0, event.intValue() > 0, configureRead.intValue() > 0,
            configureWrite.intValue() > 0, content.intValue() > 0, createChildResources.intValue() > 0, deleteResources
                .intValue() > 0));
    }

    /**
     * Provides specified permissions - used for non-admin queries.
     */
    public ResourceComposite(Resource resource, Resource parent, AvailabilityType availability, Number measure,
        Number inventory, Number control, Number alert, Number event, Number configureRead, Number configureWrite,
        Number content, Number createChildResources, Number deleteResources) {
        this(resource, parent, availability, new ResourcePermission(measure.intValue() > 0, inventory.intValue() > 0,
            control.intValue() > 0, alert.intValue() > 0, event.intValue() > 0, configureRead.intValue() > 0,
            configureWrite.intValue() > 0, content.intValue() > 0, createChildResources.intValue() > 0, deleteResources
                .intValue() > 0));
    }

    // Private constructor that all public constructors delegate to
    private ResourceComposite(Resource resource, Resource parent, AvailabilityType availability,
        ResourcePermission resourcePermission) {
        this.resource = resource;
        this.parent = parent;
        this.availability = availability;
        this.resourcePermission = resourcePermission;
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

    public void setResourcePermission(ResourcePermission resourcePermission) {
        this.resourcePermission = resourcePermission;
    }

    @XmlTransient
    public ResourceFacets getResourceFacets() {
        return resourceFacets;
    }

    public void setResourceFacets(ResourceFacets facets) {
        this.resourceFacets = facets;
    }

    @Override
    public String toString() {
        return "[ResourceComposite] Resource: " + resource + "\n\tAvailability: " + availability + "\n\tPermissions: "
            + resourcePermission;
    }
}