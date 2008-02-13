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
package org.rhq.enterprise.gui.common.servlet;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.image.data.IHighLowDataPoint;
import org.rhq.enterprise.gui.legacy.beans.NumericMetricDataPoint;

/**
 * Represents the value of a numeric metric.
 *
 * @see MeasurementPluginManager#getValue
 * @see MeasurementPlugin#getValue
 */
public class HighLowMetricValue extends NumericMetricDataPoint implements IHighLowDataPoint {
    private double highValue;
    private double lowValue;
    private int count = 0;

    /**
     * Full constructor - ultimately called by all other constructors.
     */
    public HighLowMetricValue(double value, double highValue, double lowValue, long rtime) {
        super(new MeasurementDataNumericHighLowComposite(rtime, value, highValue, lowValue));
        this.highValue = highValue;
        this.lowValue = lowValue;
    }

    /**
     * Construct with values.
     */
    public HighLowMetricValue(double value, long rtime) {
        this(value, value, value, rtime);
    }

    /**
     * Default retrieval time to System.currentTimeMillis()
     */
    public HighLowMetricValue(double value) {
        this(value, System.currentTimeMillis());
    }

    /**
     * one can always extend and override getRetrievalTime to be more robust.
     */
    public HighLowMetricValue(Number objectValue, long rtime) {
        this(objectValue.doubleValue(), rtime);
    }

    /*
     * one can always extend and override getRetrievalTime to be more robust.
     */
    public HighLowMetricValue(HighLowMetricValue objectValue, long rtime) {
        this(objectValue.getValue(), rtime);
    }

    /*
     * one can always extend and override getRetrievalTime to be more robust.
     */
    public HighLowMetricValue(long value, long rtime) {
        this((double) value, rtime);
    }

    public HighLowMetricValue(MeasurementDataNumericHighLowComposite dataPoint) {
        this(dataPoint.getValue(), dataPoint.getHighValue(), dataPoint.getLowValue(), dataPoint.getTimestamp());
    }

    /**
     * Get the Object value. Useful if you don't yet care what the type is.
     */
    @Override
    public Double getObjectValue() {
        return getValue();
    }

    /* (non-Javadoc)
     * @see net.covalent.chart.IDataPoint#getLabel()
     */
    @Override
    public String getLabel() {
        return SimpleDateFormat.getDateTimeInstance().format(new Date(this.getTimestamp()));
    }

    public double getLowValue() {
        return lowValue;
    }

    public void setLowValue(double lowValue) {
        this.lowValue = lowValue;
    }

    public double getHighValue() {
        return highValue;
    }

    public void setHighValue(double highValue) {
        this.highValue = highValue;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    /**
     * This is for the Datapoint interface. It compares only the value of the measurements, not the timestamp.
     */
    @Override
    public int compareTo(Object o) {
        HighLowMetricValue o2 = (HighLowMetricValue) o;
        double difference = this.getValue() - o2.getValue();

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

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HighLowMetricValue) {
            HighLowMetricValue val = (HighLowMetricValue) obj;
            return ((this.getTimestamp() == val.getTimestamp()) && (this.getValue() == val.getValue()));
        }

        return false;
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.gui.image.data.IStackedDataPoint#getValues()
     */
    public double[] getValues() {
        return new double[] { this.getValue() };
    }
}