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
package org.rhq.enterprise.gui.image.data;

/**
 * IHighLowDataPoint is an interface that is used to allow the chart to retrieve the high value and low value of an
 * individual data point. This interface extends IStackedDataPoint and must implement the getValues() method from that
 * interface by returning an array of three double values with the high, low and average. The values can be placed in
 * the array in any order. IStackedDataPoint extends the IDisplayDataPoint interface. The getValue method from that
 * interface should return the average value for the HighLow chart.
 *
 * @see org.rhq.enterprise.gui.image.data.IDisplayDataPoint
 * @see org.rhq.enterprise.gui.image.data.IStackedDataPoint
 */
public interface IHighLowDataPoint extends IStackedDataPoint {
    /**
     * Retrieves the high value of a chart data point.
     *
     * @return A floating point value for a chart data point.
     */
    public double getHighValue();

    /**
     * Retrieves the low value of a chart data point.
     *
     * @return A floating point value for a chart data point.
     */
    public double getLowValue();
}