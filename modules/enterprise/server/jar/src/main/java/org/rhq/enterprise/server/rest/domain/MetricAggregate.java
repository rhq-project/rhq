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
    double min;
    double avg;
    double max;
    int numDataPoints;
    List<DataPoint> dataPoints;
    long minTimeStamp;
    long maxTimeStamp;

    public MetricAggregate() {
    }

    public MetricAggregate(Integer scheduleId, double min, double avg, double max) {
        this.scheduleId = scheduleId;

        this.min = min;
        this.avg = avg;
        this.max = max;
        dataPoints = new ArrayList<DataPoint>();
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
    public double getMin() {
        return min;
    }

    @XmlElement
    public double getAvg() {
        return avg;
    }

    @XmlElement
    public double getMax() {
        return max;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public void setAvg(double avg) {
        this.avg = avg;
    }

    public void setMax(double max) {
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
        double value;
        double high;
        double low;


        public DataPoint() {
        }

        public DataPoint(long timeStamp, double value, double high, double low) {
            this.timeStamp = timeStamp;
            this.value = value;
            this.high = high;
            this.low = low;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public double getValue() {
            return value;
        }

        public void setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public double getHigh() {
            return high;
        }

        public void setHigh(double high) {
            this.high = high;
        }

        public double getLow() {
            return low;
        }

        public void setLow(double low) {
            this.low = low;
        }
    }

}
