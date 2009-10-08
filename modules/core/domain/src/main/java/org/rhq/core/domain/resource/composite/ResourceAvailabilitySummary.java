/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.resource.composite;

import java.io.Serializable;
import java.util.Date;

import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * Summary information about a resource's availability history
 *
 * @author Greg Hinkle
 */
public class ResourceAvailabilitySummary implements Serializable {

    long upTime;
    long downTime;
    int failures;
    long lastChange;
    AvailabilityType current;

    public ResourceAvailabilitySummary(long upTime, long downTime, int failures, long lastChange, AvailabilityType current) {
        this.upTime = upTime;
        this.downTime = downTime;
        this.failures = failures;
        this.lastChange = lastChange;
        this.current = current;
    }

    public long getMTBF() {
        return failures != 0 ? (upTime + downTime) / failures : 0;
    }

    public long getMTTR() {
        return failures != 0 ? downTime / failures : 0;
    }

    public double getUpPercentage() {
        return ((double)upTime) / (upTime + downTime);
    }

    public long getUpTime() {
        return upTime;
    }

    public long getDownTime() {
        return downTime;
    }

    public int getFailures() {
        return failures;
    }

    public Date getLastChange() {
        return new Date(lastChange);
    }

    public AvailabilityType getCurrent() {
        return current;
    }
}
