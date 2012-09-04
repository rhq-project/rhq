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
package org.rhq.enterprise.gui.coregui.client.components.graphing.d3;

import java.util.List;

import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;

/**
 * @author Denis Krusko
 */
public interface MetricProvider
{

    /**
     * @param metrics
     * @return metrics in JSON format
     */
    String getMetricsAsJson(List<MetricDisplaySummary> metrics);

    /**
     * @param metricIndex
     * @return all points for definition in JSON format
     */
    String getPointsAsJson(int metricIndex);

    /**
     * @param metricIndex
     * @param start
     * @param stop
     * @return points between start and stop for definition in JSON format
     */
    String getPointsAsJson(int metricIndex, long start, long stop);

    /**
     * @return map definition: points in JSON format
     */
    String getAllJSONPoints();

    /**
     * @param graphCanvas
     * @param step        the frequency with which DataProvider will receive data
     */
    void initDataProvider(AbstractGraphCanvas graphCanvas, final int step);

    /**
     * @return metrics in JSON format
     */
    String getMetricsAsJson();

    /**
     * Stops automatically obtain data
     */
    void stop();
}
