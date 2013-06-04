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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

import org.jboss.resteasy.spi.touri.URITemplate;

/**
 * A simple metric schedule
 * @author Heiko W. Rupp
 */
@ApiClass(value = "A single metrics schedule",
    description = "A schedule represents the collection interval for a metric and if the collection is enabled")
@XmlRootElement
@URITemplate("/metric/schedule/{id}")
public class MetricSchedule {

    int scheduleId;
    String scheduleName;
    Boolean enabled;
    long collectionInterval;
    String displayName;
    String unit;
    String type;
    int definitionId;
    transient long mtime;
    List<Link> links = new ArrayList<Link>();

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

    @ApiProperty("The id of the schedule")
    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    @ApiProperty("The name of the metric")
    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    @ApiProperty("Is the collection enabled?")
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @ApiProperty(value = "The collection interval in milliseconds",
        allowableValues = "A minimum of 30s")
    public long getCollectionInterval() {
        return collectionInterval;
    }

    public void setCollectionInterval(long collectionInterval) {
        this.collectionInterval = collectionInterval;
    }

    @ApiProperty("The name of the metric as shown in the UI")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @ApiProperty("The base unit of the metric")
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @ApiProperty(value ="Type of metric", allowableValues = "MEASUREMENT, TRAIT, COMPLEX, CALLTIME, AVAILABILITY")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(int definitionId) {
        this.definitionId = definitionId;
    }

    @ApiProperty("Last modification timestamp of the schedule")
    @XmlTransient
    public long getMtime() {
        return mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public void addLink(Link link) {
        links.add(link);
    }

    @XmlElementRef
    public List<Link> getLinks() {
        return links;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetricSchedule that = (MetricSchedule) o;

        if (collectionInterval != that.collectionInterval) return false;
        if (mtime != that.mtime) return false;
        if (scheduleId != that.scheduleId) return false;
        if (enabled != null ? !enabled.equals(that.enabled) : that.enabled != null) return false;
        if (scheduleName != null ? !scheduleName.equals(that.scheduleName) : that.scheduleName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = scheduleId;
        result = 31 * result + (scheduleName != null ? scheduleName.hashCode() : 0);
        result = 31 * result + (enabled != null ? enabled.hashCode() : 0);
        result = 31 * result + (int) (collectionInterval ^ (collectionInterval >>> 32));
        result = 31 * result + (int) (mtime ^ (mtime >>> 32));
        return result;
    }

}
