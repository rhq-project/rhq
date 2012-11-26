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
 * An event
 * @author Heiko W. Rupp
 */
@ApiClass("This class represents one event.")
@XmlRootElement(name = "event")
public class EventRest {

    int id;
    String detail;
    int sourceId;
    long timestamp;
    String severity;


    public EventRest() {
    }

    @ApiProperty("RHQ-Internal Id of the event")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ApiProperty("The message to be transmited")
    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    @ApiProperty("Id of the source on retrieval. When adding new events, this parameter is ignored.")
    public int getSourceId() {
        return sourceId;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    @ApiProperty("Time in millis since epoch when the event occurred")
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @ApiProperty("Severity of the event. Allowed values are DEBUG, INFO, WARN, ERROR, FATAL" )
    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
