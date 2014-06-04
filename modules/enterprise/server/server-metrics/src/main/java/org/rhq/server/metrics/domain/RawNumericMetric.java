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

package org.rhq.server.metrics.domain;

/**
 * @author John Sanda
 */
public class RawNumericMetric implements NumericMetric {

    private int scheduleId;

    private Double value = Double.NaN;

    private long timestamp;

    private ColumnMetadata columnMetadata;

    public RawNumericMetric() {
    }

    public RawNumericMetric(int scheduleId, long timestamp, double value) {
        this.scheduleId = scheduleId;
        this.value = value;
        this.timestamp = timestamp;
    }

    public RawNumericMetric(int scheduleId, long timestamp, double value, ColumnMetadata metadata) {
        this.scheduleId = scheduleId;
        this.value = value;
        this.timestamp = timestamp;
        columnMetadata = metadata;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public Double getMin() {
        return value;
    }

    @Override
    public Double getMax() {
        return value;
    }

    @Override
    public Double getAvg() {
        return value;
    }

    public ColumnMetadata getColumnMetadata() {
        return columnMetadata;
    }

    public void setColumnMetadata(ColumnMetadata columnMetadata) {
        this.columnMetadata = columnMetadata;
    }

    @Override
    public String toString() {
        if (columnMetadata == null) {
            return "RawNumericMetric[scheduleId=" + scheduleId + ", value=" + value + ", timestamp=" + timestamp + "]";
        } else {
            return "RawNumericMetric[scheduleId=" + scheduleId + ", value=" + value + ", timestamp=" + timestamp +
                " columnMetadata=" + columnMetadata + "]";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RawNumericMetric that = (RawNumericMetric) o;

        if (scheduleId != that.scheduleId) return false;
        if (timestamp != that.timestamp) return false;
        if (!value.equals(that.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = scheduleId;
        result = 31 * result + value.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}
