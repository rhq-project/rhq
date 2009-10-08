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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.gui.image.chart.Chart;
import org.rhq.enterprise.gui.image.chart.Trend;
import org.rhq.enterprise.gui.image.chart.VerticalChart;
import org.rhq.enterprise.server.legacy.measurement.MeasurementConstants;

/**
 * <p>Abstract base class for vertical charts.</p>
 *
 * <p>The chart servlet takes the following parameters (any applicable
 * defaults are in <b>bold</b> and required parameters are in
 * <i>italics</i>):</p>
 *
 * <table border="1">
 * <tr><th> key              </th><th> value                       </th></tr>
 * <tr><td> collectionType   </td><td> &lt;integer <b>(0)</b>&gt;* </td></tr>
 * </table>
 *
 * <p>* Must be a valid value from <code>{@link
 * org.rhq.enterprise.server.legacy.measurement.MeasurementConstants</code>.</p>
 *
 * @see org.rhq.enterprise.server.legacy.measurement.MeasurementConstants
 */
public abstract class VerticalChartServlet extends ChartServlet {
    /** Request parameter for unit scale. */
    public static final String COLLECTION_TYPE_PARAM = "collectionType";

    // member data
    private Log log = LogFactory.getLog(VerticalChartServlet.class.getName());
    private int collectionType;

    public VerticalChartServlet() {
    }

    /**
     * Return the default <code>collectionType</code>.
     */
    protected int getDefaultCollectionType() {
        return MeasurementConstants.COLL_TYPE_DYNAMIC;
    }

    /**
     * This method will be called automatically by the ChartServlet.
     * It should handle the parsing and error-checking of any specific
     * parameters for the chart being rendered.
     *
     * @param request the HTTP request object
     */
    protected void parseParameters(HttpServletRequest request) {
        super.parseParameters(request);

        // cumulative trend
        collectionType = parseIntParameter(request, COLLECTION_TYPE_PARAM, getDefaultCollectionType());
        _logParameters();
    }

    /**
     * Initialize the chart.  This method will be called after the
     * parameters have been parsed and the chart has been created.
     *
     * @param chart the chart
     */
    protected void initializeChart(Chart chart) {
        super.initializeChart(chart);

        VerticalChart verticalChart = (VerticalChart) chart;
        int cumulativeTrend = getTrendForCollectionType(collectionType);
        verticalChart.setCumulativeTrend(cumulativeTrend);
    }

    /**
     * Get the trend based on the collection type.  If the collection
     * type is invalid, it will return <code>TREND_NONE</code>.
     *
     * @param collectionType the collection type from <code>{@link
     * org.rhq.enterprise.server.legacy.measurement.MeasurementConstants}</code>
     * @return the trend from <code>{@link
     * net.covalent.chart.Trend}</code>
     * @see org.rhq.enterprise.server.legacy.measurement.MeasurementConstants
     * @see net.covalent.chart.Trend
     */
    protected int getTrendForCollectionType(int collectionType) {
        int trend = Trend.TREND_NONE;
        switch (collectionType) {
        case MeasurementConstants.COLL_TYPE_DYNAMIC:
        case MeasurementConstants.COLL_TYPE_STATIC:
            trend = Trend.TREND_NONE;
            break;
        case MeasurementConstants.COLL_TYPE_TRENDSUP:
            trend = Trend.TREND_UP;
            break;
        case MeasurementConstants.COLL_TYPE_TRENDSDOWN:
            trend = Trend.TREND_DOWN;
            break;
        default:
            log.warn("Invalid collection type: " + collectionType);
            break;
        }
        return trend;
    }

    private void _logParameters() {
        if (log.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer("Parameters:");
            sb.append("\n");
            sb.append("\t");
            sb.append(COLLECTION_TYPE_PARAM);
            sb.append(": ");
            sb.append(collectionType);
            log.debug(sb.toString());
        }
    }
}

// EOF
