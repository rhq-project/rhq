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
package org.rhq.enterprise.gui.legacy.beans;

import java.io.Serializable;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.image.data.IComparableDatapoint;
import org.rhq.enterprise.gui.image.data.IDisplayDataPoint;
import org.rhq.enterprise.server.measurement.AvailabilityPoint;

/**
 * A numeric metric data point that can be plotted on a chart.
 *
 * @author Ian Springer
 */
public class NumericMetricDataPoint implements IDisplayDataPoint, IComparableDatapoint, Serializable {
    private long timestamp;
    private double value;

    public NumericMetricDataPoint(MeasurementDataNumericHighLowComposite dataPoint) {
        this.timestamp = dataPoint.getTimestamp();
        this.value = dataPoint.getValue();
    }

    public NumericMetricDataPoint(AvailabilityPoint availabilityPoint) {
        this.timestamp = availabilityPoint.getTimestamp();
        this.value = (availabilityPoint.getAvailabilityType() == AvailabilityType.UP) ? 100 : 0;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public String getLabel() {
        return SimpleDateFormat.getDateTimeInstance().format(this.timestamp);
    }

    public double getValue() {
        return this.value;
    }

    public Double getObjectValue() {
        return this.value;
    }

    /**
     * This is for the Datapoint interface. It compares only the values of the measurements, not the timestamps.
     */
    public int compareTo(Object obj) {
        NumericMetricDataPoint that = (NumericMetricDataPoint) obj;
        double difference = this.value - that.value;

        // can't just return subtraction, because casting to integer
        // loses the negative values for small differences (< 1), which we
        // need.
        if (difference < 0) {
            return -1;
        }

        if (difference > 0) {
            return 1;
        }

        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        NumericMetricDataPoint that = (NumericMetricDataPoint) obj;

        if (timestamp != that.timestamp) {
            return false;
        }

        if (Double.compare(that.value, value) != 0) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) (timestamp ^ (timestamp >>> 32));
        temp = (value != +0.0d) ? Double.doubleToLongBits(value) : 0L;
        result = (31 * result) + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        if (Double.isNaN(getValue())) {
            return "NaN";
        }

        return NumberFormat.getInstance().format(getValue());
    }
}