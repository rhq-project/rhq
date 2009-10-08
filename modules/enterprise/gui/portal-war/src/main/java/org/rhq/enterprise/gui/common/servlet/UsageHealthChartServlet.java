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

import org.rhq.enterprise.gui.image.chart.Chart;
import org.rhq.enterprise.gui.image.chart.UsageChart;
import org.rhq.enterprise.server.legacy.measurement.MeasurementConstants;

/**
 * <p>Usage current health chart servlet.</p>
 */
public class UsageHealthChartServlet extends CurrentHealthChartServlet {
    public UsageHealthChartServlet() {
    }

    /**
     * Create and return the chart. This method will be called after the parameters have been parsed.
     *
     * @return the newly created chart
     */
    protected Chart createChart() {
        return new UsageChart(getImageWidth(), getImageHeight());
    }

    /**
     * Return the corresponding measurement category.
     *
     * @return <code>{@link org.rhq.enterprise.server.legacy.measurement.MeasurementConstants.CAT_THROUGHPUT}</code> or
     */
    protected String getMetricCategory() {
        return MeasurementConstants.CAT_THROUGHPUT;
    }
}

// EOF
