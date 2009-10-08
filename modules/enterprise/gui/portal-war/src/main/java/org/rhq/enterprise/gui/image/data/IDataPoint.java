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
 * IDataPoint is an interface that is used to allow the chart to retrieve the value and label of an individual data
 * point. For line and column charts the value is drawn on the Y axis and label is displayed on the X axis. Any that are
 * added to the chart datum collection must implement the IDataPoint interface.
 */
public interface IDataPoint {
    /**
     * Retrieves the value of a chart data point. The label is displayed on the Y axis for line and column charts.
     *
     * @return A floating point value for a chart data point.
     */
    public double getValue();
}