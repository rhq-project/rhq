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
 * IStackedDataPoint is an interface that is used to allow the chart to retrieve multiple values for per column in a
 * stacked column chart. This interface is a sub-interface of the IDisplayDataPoint interface. The getValue member
 * should return the sum of all of the values in the stack.
 */
public interface IStackedDataPoint extends IDisplayDataPoint {
    /**
     * Retrieves the value of a chart data point. The label is displayed on the X axis for stacked performance charts.
     *
     * @return A floating point value for a chart data point.
     */
    public double[] getValues();
}