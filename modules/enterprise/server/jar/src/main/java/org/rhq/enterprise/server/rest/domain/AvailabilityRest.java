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

import org.jboss.resteasy.annotations.providers.jaxb.json.Mapped;
import org.jboss.resteasy.annotations.providers.jaxb.json.XmlNsMap;
import org.jboss.resteasy.links.RESTServiceDiscovery;

import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * Availability implementation for exposing via REST
 * @author Heiko W. Rupp
 */
@XmlRootElement(name = "availability")
@Mapped(namespaceMap = @XmlNsMap(jsonName = "atom", namespace = "http://www.w3.org/2005/Atom"))
public class AvailabilityRest {

    long since;
    String type;

    int resourceId;

    private RESTServiceDiscovery rest;

    public AvailabilityRest() {
        // for RESTEasy/JAXB
    }

    public AvailabilityRest(AvailabilityType type, long since, int resourceId) {
        this.since = since;
        this.type = type.toString();
        this.resourceId = resourceId;
    }

    @XmlElement
    public long getSince() {
        return since;
    }

    @XmlElement
    public String getType() {
        return type;
    }

    @XmlElement
    public int getResourceId() {
        return resourceId;
    }

    @XmlElementRef
    public RESTServiceDiscovery getRest() {
        return rest;
    }

    public void setRest(RESTServiceDiscovery rest) {
        this.rest = rest;
    }
}
