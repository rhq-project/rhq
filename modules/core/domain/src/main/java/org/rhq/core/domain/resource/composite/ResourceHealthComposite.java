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

import org.rhq.core.domain.measurement.AvailabilityType;

import java.io.Serializable;

/**
 * Composite object meant to be used to by the favorites portlet display showing resource information about its health
 * (its availability and number of alerts it has triggered).
 *
 * @author John Mazzitelli
 */
public class ResourceHealthComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final String name;
    private final String typeName;
    private final AvailabilityType availabilityType;
    private final long alerts;

    public ResourceHealthComposite(int id, String name, String typeName, AvailabilityType availabilityType, long alerts) {
        this.id = id;
        this.name = name;
        this.typeName = typeName;
        this.availabilityType = availabilityType;
        this.alerts = alerts;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    public AvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    public long getAlerts() {
        return alerts;
    }
}