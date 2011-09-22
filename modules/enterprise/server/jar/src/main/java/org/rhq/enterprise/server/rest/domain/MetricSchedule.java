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

import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.jboss.resteasy.annotations.providers.jaxb.json.Mapped;
import org.jboss.resteasy.annotations.providers.jaxb.json.XmlNsMap;
import org.jboss.resteasy.spi.touri.URITemplate;

/**
 * A simple metric schedule
 * @author Heiko W. Rupp
 */
@XmlRootElement
//@XmlType(propOrder = {"scheduleId","scheduleName","displayName","enabled","collectionInterval","unit","type"})
@Mapped(namespaceMap = @XmlNsMap(jsonName = "atom", namespace = "http://www.w3.org/2005/Atom"))
@URITemplate("/metric/schedule/{id}")
public class MetricSchedule {

    int scheduleId;
    String scheduleName;
    Boolean enabled;
    long collectionInterval;
    String displayName;
    String unit;
    String type;

    @SuppressWarnings("unused")
    public MetricSchedule() {
    }

    public MetricSchedule(int scheduleId, String scheduleName, String displayName, boolean enabled, long collectionInterval, String unit, String type) {
        this.scheduleId = scheduleId;
        this.scheduleName = scheduleName;
        this.displayName = displayName;
        this.enabled = enabled;
        this.collectionInterval = collectionInterval;
        this.unit = unit;
        this.type = type;
    }

    @XmlID
    public String getScheduleId() {
        return ""+scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public long getCollectionInterval() {
        return collectionInterval;
    }

    public void setCollectionInterval(long collectionInterval) {
        this.collectionInterval = collectionInterval;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

//    public String getEnabled() {
//        return String.valueOf(enabled);
//    }
}
