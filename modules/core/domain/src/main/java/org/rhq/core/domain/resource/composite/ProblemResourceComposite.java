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

/**
 * Information on a resource that is considered having a "problem" - it is either {@link AvailabilityType#DOWN down},
 * has one or more alerts, or a combination of those two conditions.
 *
 * @author John Mazzitelli
 */
public class ProblemResourceComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    //private Resource resource;
    private int resourceId;
    private int resourceTypeId;
    private String resourceName;
    private String ancestry;
    private long numAlerts;
    private AvailabilityType availabilityType;
    private Integer ancestryLength;

    /** Private no args constructor for JAXB serialization. */
    @SuppressWarnings("unused")
    private ProblemResourceComposite() {
    }

    public ProblemResourceComposite(int resourceId, int resourceTypeId, String resourceName, String ancestry,
        long numAlerts, AvailabilityType availabilityType) {
        this(resourceId, resourceTypeId, resourceName, ancestry, numAlerts, availabilityType, ((null == ancestry) ? 0
            : ancestry.length()));
    }

    /**
     * This constructor is typically used only to support some predefined queries.
     * @since 4.12
     */
    public ProblemResourceComposite(int resourceId, int resourceTypeId, String resourceName, String ancestry,
        long numAlerts, AvailabilityType availabilityType, Integer ancestryLength) {
        this.resourceId = resourceId;
        this.resourceTypeId = resourceTypeId;
        this.resourceName = resourceName;
        this.ancestry = ancestry;
        this.numAlerts = numAlerts;
        this.availabilityType = availabilityType; // pull explicitly because lazy-loaded by default
        this.ancestryLength = (null == ancestryLength) ? 0 : ancestry.length();
    }

    public int getResourceId() {
        return resourceId;
    }

    public int getResourceTypeId() {
        return resourceTypeId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getAncestry() {
        return ancestry;
    }

    public long getNumAlerts() {
        return numAlerts;
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