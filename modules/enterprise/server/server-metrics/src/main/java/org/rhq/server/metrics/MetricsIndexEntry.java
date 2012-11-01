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

package org.rhq.server.metrics;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * @author John Sanda
 */
public class MetricsIndexEntry {

    private String bucket;

    private int scheduleId;

    private DateTime time;

    public MetricsIndexEntry () {
    }

    public MetricsIndexEntry(String bucket, Date time, int scheduleId) {
        this.bucket = bucket;
        this.scheduleId = scheduleId;
        this.time = new DateTime(time);
    }

    public MetricsIndexEntry(String bucket, DateTime time, int scheduleId) {
        this.bucket = bucket;
        this.scheduleId = scheduleId;
        this.time = time;
    }

    public String getBucket() {

        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public DateTime getTime() {
        return time;
    }

    public void setTime(DateTime time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetricsIndexEntry that = (MetricsIndexEntry) o;

        if (scheduleId != that.scheduleId) return false;
        if (!bucket.equals(that.bucket)) return false;
        if (!time.equals(that.time)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = bucket.hashCode();
        result = 31 * result + scheduleId;
        result = 31 * result + time.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MetricsIndexEntry[bucket=" + bucket + ", scheduleId=" + scheduleId + ", time=" +
            DateTimeFormat.mediumDateTime().print(time) + "]";
    }
}
