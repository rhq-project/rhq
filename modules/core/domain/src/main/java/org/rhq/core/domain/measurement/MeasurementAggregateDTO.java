/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.core.domain.measurement;

import java.io.Serializable;

/**
 * @author John Sanda
 */
public class MeasurementAggregateDTO implements MeasurementAggregate, Serializable {

    private static final long serialVersionUID = 1L;

    private long timestamp;

    private int scheduleId;

    private Double avg;

    private Double max;

    private Double min;

    public MeasurementAggregateDTO() {
    }

    public MeasurementAggregateDTO(long timestamp, int scheduleId, Double avg, Double max, Double min) {
        this.timestamp = timestamp;
        this.scheduleId = scheduleId;
        this.avg = avg;
        this.max = max;
        this.min = min;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    @Override
    public Double getAvg() {
        return avg;
    }

    public void setAvg(Double avg) {
        this.avg = avg;
    }

    @Override
    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    @Override
    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    @Override
    public String toString() {
        return "MeasurementAggregateDTO[timestamp: " + timestamp + ", scheduleId: " + scheduleId + ", avg: " + avg +
            ", min: " + min + ", max: " + max + "]";
    }
}
