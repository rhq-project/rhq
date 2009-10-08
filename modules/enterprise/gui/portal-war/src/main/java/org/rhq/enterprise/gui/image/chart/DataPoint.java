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
package org.rhq.enterprise.gui.image.chart;

import org.rhq.enterprise.gui.image.data.IDisplayDataPoint;

/**
 * DataPoint holds a data and label for a single data point in a chart. A collection of DataPoint objects are used to
 * chart a series of data points.
 */
public class DataPoint implements IDisplayDataPoint {
    private String m_strLabel;
    private double m_dValue;
    private long m_timestamp;

    /**
     * Constructs a DataPoint object with the specified value and an empty label.
     *
     * @param value A floating point value for the object data point.
     */
    public DataPoint(double value) {
        this(value, null);
    }

    /**
     * Constructs a DataPoint object with the specified value and and specified label.
     *
     * @param value     A floating point value for the object's data point.
     * @param timestamp A timestamp for the object's data point.
     */
    public DataPoint(double value, long timestamp) {
        this.m_dValue = value;
        this.m_timestamp = timestamp;
    }

    /**
     * Constructs a DataPoint object with the specified value and and specified label.
     *
     * @param value A floating point value for the object's data point.
     * @param label A String label for the object's data point.
     */
    public DataPoint(double value, String label) {
        this.m_dValue = value;
        this.m_strLabel = label;
    }

    /**
     * Retrieves the absolute time.
     *
     * @return A long value for the absolute time.
     */
    public long getTimestamp() {
        return this.m_timestamp;
    }

    /**
     * Retrieves the label of a chart data point.
     *
     * @return A String label for a chart data point.
     */
    public String getLabel() {
        return this.m_strLabel;
    }

    /**
     * Sets the label of a chart data point. The label is displayed on the X axis for line and column charts.
     *
     * @param label A String label for a chart data point.
     */
    public void setLabel(String label) {
        if (label == null) {
            this.m_strLabel = Chart.EMPTY_STRING;
        } else {
            this.m_strLabel = label;
        }
    }

    /**
     * Retrieves the value of a chart data point. The data point is charted on the Y axis for line and column charts.
     *
     * @return A floating point value for a chart data point.
     */
    public double getValue() {
        return this.m_dValue;
    }

    /**
     * Sets the value of a chart data point. The data point is charted on the Y axis for line and column charts.
     *
     * @param value A floating point value for a chart data point.
     */
    public void setValue(double value) {
        this.m_dValue = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(m_dValue);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (int) (m_timestamp ^ (m_timestamp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DataPoint) {
            DataPoint val = (DataPoint) obj;
            return ((this.getTimestamp() == val.getTimestamp()) && (this.getValue() == val.getValue()));
        }

        return false;
    }

}