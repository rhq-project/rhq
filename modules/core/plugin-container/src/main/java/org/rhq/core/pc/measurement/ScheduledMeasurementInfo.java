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
package org.rhq.core.pc.measurement;

import org.rhq.core.domain.measurement.MeasurementScheduleRequest;

 /**
 * This is an object that has a natural ordering by when a measurement next needs to be collected as well as by its
 * resource. Requests for the same resource that have the same measurement Implementation notes: This class has a
 * natural ordering to it used by the measurement manager in order to track the next collection time. This ordering
 * includes distinctive information that should be unnecessary, but is because the {@link java.util.PriorityQueue}
 * requires that IFF a.compareTo(b) == 0 Then a.equals(b). This is unfortunate and slows us up some.
 *
 * @author Greg Hinkle
 */
public class ScheduledMeasurementInfo extends MeasurementScheduleRequest implements
    Comparable<ScheduledMeasurementInfo> {
    private int resourceId;
    private long lastCollection;
    private long nextCollection;

    public ScheduledMeasurementInfo(MeasurementScheduleRequest scheduleRequest, Integer resourceId) {
        super(scheduleRequest);
        this.resourceId = resourceId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public long getLastCollection() {
        return lastCollection;
    }

    public void setLastCollection(long lastCollection) {
        this.lastCollection = lastCollection;
    }

    public long getNextCollection() {
        return nextCollection;
    }

    public void setNextCollection(long nextCollection) {
        this.nextCollection = nextCollection;
    }

    public int compareTo(ScheduledMeasurementInfo o) {
        int n = (nextCollection < o.getNextCollection()) ? -1 : ((nextCollection == o.getNextCollection()) ? 0 : 1);
        if (n != 0) {
            return n;
        }

        n = (resourceId < o.getResourceId()) ? -1 : ((resourceId == o.getResourceId()) ? 0 : 1);
        if (n != 0) {
            return n;
        }

        n = getName().compareTo(o.getName());
        if (n != 0) {
            return n;
        }

        return (getScheduleId()-o.getScheduleId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        ScheduledMeasurementInfo that = (ScheduledMeasurementInfo) o;

        if (getScheduleId() != that.getScheduleId()) {
            return false;
        }

        if (resourceId != that.resourceId) {
            return false;
        }

        if (!getName().equals(that.getName())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = resourceId;
        result = (31 * result) + getName().hashCode();
        result = (31 * result) + getScheduleId();
        return result;
    }

    @Override
    public String toString() {
        return "ScheduledMeasurementInfo[res=" + resourceId + ", name=" + getName() + ", sched=" + getScheduleId() + "]";
    }
}