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
package org.rhq.modules.integrationTests.restApi.d;
/**
 * CallTime value for testing the rest interface
 * @author Libor Zoubek
 */

public class CallTimeValueRest {

    public static CallTimeValueRest defaultCallTimeValue(String callDestination) {
        CallTimeValueRest c = new CallTimeValueRest();
        c.callDestination = callDestination;
        c.beginTime = System.currentTimeMillis();
        c.duration = 1000L;
        return c;
    }

    String callDestination;
    long beginTime;
    long duration;

    public CallTimeValueRest() {
    }

    public long getBeginTime() {
        return beginTime;
    }

    public String getCallDestination() {
        return callDestination;
    }

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
}
