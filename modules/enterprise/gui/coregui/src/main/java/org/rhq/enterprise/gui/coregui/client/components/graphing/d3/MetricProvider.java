/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.graphing.d3;

import java.util.List;

import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;

/**
 * Define a metric provider.
 * @author Denis Krusko
 * @author Mike Thompson
 */
public interface MetricProvider
{
    /**
     * @param graphCanvas
     * @param stepInMillis        the frequency with which DataProvider will receive data
     */
    void initDataProvider(AbstractGraphCanvas graphCanvas, final int stepInMillis);

    /**
     * @param metrics List of MetricDisplaySummary
     * @return metrics in JSON format
     */
    String getMetricsDisplaySummaryAsJson(List<MetricDisplaySummary> metrics);

    /**
     * @param metricIndex
     * @return all points for definition in JSON format
     */
    String getStoredPointsAsJson(int metricIndex);

    /**
     * @return map definition: points in JSON format
     */
//    String getAllJSONPoints();

    /**
     * @return metrics in JSON format
     */
    String getMetricsAsJson();

    /**
     * @param metricIndex
     * @param start date as long
     * @param stop date as long
     * @return points between start and stop for definition in JSON format
     */
    String getPointsAsJson(int metricIndex, long start, long stop);

    /**
     * Stops automatically obtain data
     */
    void stop();
}
