/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.rest.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

/**
 * @author Libor Zoubek
 */
@ApiClass("One callTime data point of a metric (aka schedule)")
@XmlRootElement
public class CallTimeValueRest {

    String callDestination;
    long beginTime;
    long duration;

    public CallTimeValueRest() {
        // Needed for JAXB
    }
    @ApiProperty("Time in millis since epoch when the request occurred")
    @XmlAttribute
    public long getBeginTime() {
        return beginTime;
    }

    @ApiProperty("Destination URI of request")
    @XmlAttribute
    public String getCallDestination() {
        return callDestination;
    }

    @ApiProperty("Time in millis - duration of request")
    @XmlAttribute
    public long getDuration() {
        return duration;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public void setCallDestination(String callDestination) {
        this.callDestination = callDestination;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return new StringBuilder("[callDestination="+callDestination)
            .append(", beginTime="+beginTime)
            .append(", duration="+duration)
            .append("]").toString();
    }
}
