/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

/**
 * An EventSource
 * @author Heiko W. Rupp
 */
@ApiClass("This class represents an EventSource - e.g. a logfile.")
@XmlRootElement(name="source")
public class EventSourceRest {

    int id;
    String name;
    String displayName;
    String location;
    int resourceId;
    String description;

    public EventSourceRest() {
    }

    @ApiProperty("Id of the EventSource")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ApiProperty("Name of the EventSource - defined in the definition. This is required when adding a new EventSource")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiProperty("Display name of the EventSource - defined in the definition")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @ApiProperty("'Location' of the EventSource. This is e.g. the path to the logfile being monitored. This is required when adding a new EventSource")
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @ApiProperty("Id of the resource this ")
    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    @ApiProperty("A description of this EventSource")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
