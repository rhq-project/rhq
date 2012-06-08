/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Raw data that could be used to draw a chart
 * @author Heiko W. Rupp
 */
@XmlRootElement
public class MetricAggregate {

    Integer scheduleId;
    Double min;
    Double avg;
    Double max;
    int numDataPoints;
    List<DataPoint> dataPoints;
    long minTimeStamp;
    long maxTimeStamp;

    public MetricAggregate() {
        dataPoints = new ArrayList<DataPoint>();
    }

    public MetricAggregate(Integer scheduleId, Double min, Double avg, Double max) {
        this();
        this.scheduleId = scheduleId;

        this.min = min;
        this.avg = avg;
        this.max = max;

    }

    @XmlElement
    public List<DataPoint> getDataPoints() {
        return dataPoints;
    }

    public void addDataPoint(DataPoint point) {
        this.dataPoints.add(point);
    }

    public void setDataPoints(List<DataPoint> dataPoints) {
        this.dataPoints = dataPoints;
    }

    @XmlElement
    public Double getMin() {
        return min;
    }

    @XmlElement
    public Double getAvg() {
        return avg;
    }

    @XmlElement
    public Double getMax() {
        return max;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public void setAvg(Double avg) {
        this.avg = avg;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public int getNumDataPoints() {
        return numDataPoints;
    }

    public void setNumDataPoints(int numDataPoints) {
        this.numDataPoints = numDataPoints;
    }

    @XmlElement
    public Integer getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Integer scheduleId) {
        this.scheduleId = scheduleId;
    }

    public long getMinTimeStamp() {
        return minTimeStamp;
    }

    public void setMinTimeStamp(long minTimeStamp) {
        this.minTimeStamp = minTimeStamp;
    }

    public long getMaxTimeStamp() {
        return maxTimeStamp;
    }

    public void setMaxTimeStamp(long maxTimeStamp) {
        this.maxTimeStamp = maxTimeStamp;
    }

    public static class DataPoint {
        long timeStamp;
        Double value;
        Double high;
        Double low;


        public DataPoint() {
        }

        public DataPoint(long timeStamp) {
            this.timeStamp = timeStamp;
        }

        public DataPoint(long timeStamp, Double value, Double high, Double low) {
            this.timeStamp = timeStamp;
            this.value = value;
            this.high = high;
            this.low = low;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public Double getValue() {
            return value;
        }

        public void setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
        }

        public void setValue(Double value) {
            this.value = value;
        }

        public Double getHigh() {
            return high;
        }

        public void setHigh(Double high) {
            this.high = high;
        }

        public Double getLow() {
            return low;
        }

        public void setLow(Double low) {
            this.low = low;
        }
    }

}
