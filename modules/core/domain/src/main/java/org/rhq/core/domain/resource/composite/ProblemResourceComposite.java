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

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;

/**
 * Information on a resource that is considered having a "problem" - it is either {@link AvailabilityType#DOWN down},
 * has one or more alerts, or a combination of those two conditions.
 *
 * @author John Mazzitelli
 */
public class ProblemResourceComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private Resource resource;
    private long numAlerts;
    private AvailabilityType availabilityType;

    // TODO: The Resource entity has been added. Leaving these for backCompat in portal war. When portal war is
    //       removed these fields should go. Note, keep availabilityType as it is not present by default in Resource.
    private int resourceId;
    private String resourceName;

    /** Private no args contstructor for JAXB serialization. */
    @SuppressWarnings("unused")
    private ProblemResourceComposite() {
    }

    public ProblemResourceComposite(Resource resource, long numAlerts, int resourceId, String resourceName,
        AvailabilityType availabilityType) {
        this.resource = resource;
        this.numAlerts = numAlerts;
        this.availabilityType = availabilityType; // pull explicitly because lazy-loaded by default         

        this.resourceId = resourceId;
        this.resourceName = resourceName;
    }

    public int getResourceId() {
        return resourceId;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public String getResourceName() {
        return resourceName;
    }

    /**
     * Indicates if the resource is down. If this returns <code>null</code>, the resource may be up or unknown. In any
     * case, a <code>null</code> means the resource is not known to be down.
     *
     * @return up or down status of the resource
     */
    public AvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    public long getNumAlerts() {
        return numAlerts;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("ProblemResource: ");

        str.append("resource-id=[" + this.resourceId);
        str.append("], resource-name=[" + this.resourceName);
        str.append("], availability-type=[" + this.availabilityType);
        str.append("], num-alerts=[" + this.numAlerts);
        str.append("]");

        return str.toString();
    }
}