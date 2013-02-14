/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.metrics.simulator;

/**
 * @author John Sanda
 */
public class Schedule implements Comparable<Schedule> {

    private int id;

    private long lastCollection;

    private long nextCollection;

    private long interval;

    public Schedule(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
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

    public void updateCollection() {
        nextCollection += interval;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public double getNextValue() {
        return 1.23;
    }

    @Override
    public int compareTo(Schedule that) {
        if (this.nextCollection < that.nextCollection) {
            return -1;
        }

        if (this.nextCollection > that.nextCollection) {
            return 1;
        }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Schedule schedule = (Schedule) o;

        if (id != schedule.id) return false;
        if (interval != schedule.interval) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (int) (interval ^ (interval >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Schedule[id= " + id + ", lastCollection= " + lastCollection + ", nextCollection= " + nextCollection +
            ", interval= " + interval + "]";
    }
}
