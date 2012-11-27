/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.server.rest.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

import org.jboss.resteasy.links.RESTServiceDiscovery;

import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * Availability implementation for exposing via REST
 * @author Heiko W. Rupp
 */
@ApiClass("Represents the availability of a resource")
@XmlRootElement(name = "availability")
public class AvailabilityRest {

    long since;
    String type;
    Long until;

    int resourceId;

    public AvailabilityRest() {
        // for RESTEasy/JAXB
    }

    public AvailabilityRest(AvailabilityType type, long since, int resourceId) {
        this.since = since;
        this.type = type.toString();
        this.resourceId = resourceId;
    }

    public AvailabilityRest(long since, int resourceId) {
        this.since = since;
        this.type = "- unknown -";
        this.resourceId = resourceId;
    }

    @ApiProperty("Time since the type is valid")
    @XmlElement
    public long getSince() {
        return since;
    }

    @ApiProperty(value = "Type of availability", allowableValues = "UP, DOWN, DISABLED, UNKNOWN")
    @XmlElement
    public String getType() {
        return type;
    }

    @ApiProperty("Id of the resource that reports the availability")
    @XmlElement
    public int getResourceId() {
        return resourceId;
    }

    public void setSince(long since) {
        this.since = since;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    @ApiProperty("Time until the type is valid. May be null if type is ongoing")
    public Long getUntil() {
        return until;
    }

    public void setUntil(Long until) {
        this.until = until;
    }
}
