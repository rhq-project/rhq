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

package org.rhq.core.domain.event.composite;

import java.io.Serializable;
import java.util.Date;

import org.rhq.core.domain.event.EventSeverity;

/**
 * A Data transfer object for the Event structure.
 * Not all fields will be filled in every given time.
 * @author Heiko W. Rupp
 *
 */
public class EventComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    String eventDetail;
    int resourceId;
    int eventId;
    String sourceLocation;
    EventSeverity severity;
    long timestamp;
    String ackUser;
    Long ackTime;

    public EventComposite() {
        eventDetail = "- UNDEF -";
        sourceLocation = "- UNDEF -";
    }

    public EventComposite(String eventDetail, int resourceId, int eventId, EventSeverity severity) {
        super();
        this.eventDetail = eventDetail;
        this.resourceId = resourceId;
        this.eventId = eventId;
        this.severity = severity;
    }

    public EventComposite(String eventDetail, int resourceId, int eventId, String sourceLocation) {
        super();
        this.eventDetail = eventDetail;
        this.resourceId = resourceId;
        this.eventId = eventId;
        this.sourceLocation = sourceLocation;
    }

    public EventComposite(String eventDetail, int resourceId, int eventId, String sourceLocation, String ackUser,
        Long ackTime) {
        super();
        this.eventDetail = eventDetail;
        this.resourceId = resourceId;
        this.eventId = eventId;
        this.sourceLocation = sourceLocation;
        this.ackTime = ackTime;
        this.ackUser = ackUser;
    }

    public EventComposite(String eventDetail, int eventId, String sourceLocation, EventSeverity severity,
        long timestamp, int resourceId) {
        super();
        this.eventDetail = eventDetail;
        this.eventId = eventId;
        this.sourceLocation = sourceLocation;
        this.severity = severity;
        this.timestamp = timestamp;
        this.resourceId = resourceId;
    }

    public EventComposite(String eventDetail, int eventId, String sourceLocation, EventSeverity severity,
        long timestamp, int resourceId, String ackUser, Long ackTime) {
        super();
        this.eventDetail = eventDetail;
        this.eventId = eventId;
        this.sourceLocation = sourceLocation;
        this.severity = severity;
        this.timestamp = timestamp;
        this.resourceId = resourceId;
        this.ackTime = ackTime;
        this.ackUser = ackUser;
    }

    public String getEventDetail() {
        return eventDetail;
    }

    public void setEventDetail(String eventDetail) {
        this.eventDetail = eventDetail;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(String sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public EventSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(EventSeverity severity) {
        this.severity = severity;
    }

    public Date getTimestamp() {
        return new Date(timestamp);
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp.getTime();
    }

    public String getAckUser() {
        return ackUser;
    }

    public void setAckUser(String ackUser) {
        this.ackUser = ackUser;
    }

    public Date getAckTime() {
        return (ackTime == null ? null : new Date(ackTime));
    }

    public void setAckTime(Date ackTime) {
        this.ackTime = ackTime.getTime();
    }

    public String getAckTimeUser() {
        if (ackUser != null)
            return ackUser + ";" + ackTime;
        else
            return "";
    }
}
