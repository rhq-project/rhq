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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.enterprise.gui.image.chart.Chart;

/**
 * <p>This servlet returns a response that contains the binary data of an image (JPEG or PNG) that can be viewed in a
 * web browser.</p>
 *
 * <p>The chart servlet takes the following parameters (any applicable defaults are in <b>bold</b> and required
 * parameters are in <i>italics</i>):</p>
 *
 * <table border="1">
 *   <tr>
 *     <th>key</th>
 *     <th>value</th>
 *   </tr>
 *   <tr>
 *     <td>measurementUnits</td>
 *     <td>&lt;MeasurementUnits.NONE&gt;</td>
 *   </tr>
 *   <tr>
 *     <td>showPeak</td>
 *     <td>(<b>false</b> | true)</td>
 *   </tr>
 *   <tr>
 *     <td>showHighRange</td>
 *     <td>(<b>false</b> | true)</td>
 *   </tr>
 *   <tr>
 *     <td>showValues</td>
 *     <td>(false | <b>true</b>)</td>
 *   </tr>
 *   <tr>
 *     <td>showAverage</td>
 *     <td>(<b>false</b> | true)</td>
 *   </tr>
 *   <tr>
 *     <td>showLowRange</td>
 *     <td>(<b>false</b> | true)</td>
 *   </tr>
 *   <tr>
 *     <td>showLow</td>
 *     <td>(<b>false</b> | true)</td>
 *   </tr>
 *   <tr>
 *     <td>showBaseline</td>
 *     <td>(<b>false</b> | true)</td>
 *   </tr>
 *   <tr>
 *     <td>baseline*</td>
 *     <td>&lt;double&gt;</td>
 *   </tr>
 *   <tr>
 *     <td>highRange*</td>
 *     <td>&lt;double&gt;</td>
 *   </tr>
 *   <tr>
 *     <td>lowRange*</td>
 *     <td>&lt;double&gt;</td>
 *   </tr>
 * </table>
 *
 * <p>* only used and required if corresponding <code>showXXX</code> parameter is <code>true</code><br>
 * </p>
 */
public abstract class ChartServlet extends ImageServlet {
    public static final String MEASUREMENT_UNITS_PARAM = "measurementUnits";

    /**
     * Default image width.
     */
    public static final int IMAGE_WIDTH_DEFAULT = 755;

    /**
     * Default image height.
     */
    public static final int IMAGE_HEIGHT_DEFAULT = 300;

    /**
     * Request parameter for whether or not to show the peak.
     */
    public static final String SHOW_PEAK_PARAM = "showPeak";

    /**
     * Request parameter for whether or not to show high range.
     */
    public static final String SHOW_HIGHRANGE_PARAM = "showHighRange";

    /**
     * Request parameter for whether or not to show the actual values.
     */
    public static final String SHOW_VALUES_PARAM = "showValues";

    /**
     * Request parameter for whether or not to show average.
     */
    public static final String SHOW_AVERAGE_PARAM = "showAverage";

    /**
     * Request parameter for whether or not to show low range.
     */
    public static final String SHOW_LOWRANGE_PARAM = "showLowRange";

    /**
     * Request parameter for whether or not to show the low.
     */
    public static final String SHOW_LOW_PARAM = "showLow";

    /**
     * Request parameter for whether or not to show baseline.
     */
    public static final String SHOW_BASELINE_PARAM = "showBaseline";

    /**
     * Request parameter for baseline.
     */
    public static final String BASELINE_PARAM = "baseline";

    /**
     * Request parameter for baseline.
     */
    public static final String HIGHRANGE_PARAM = "highRange";

    /**
     * Request parameter for baseline.
     */
    public static final String LOWRANGE_PARAM = "lowRange";

    // member data
    private Log log = LogFactory.getLog(ChartServlet.class);
    protected MeasurementUnits units;
    private boolean showPeak;
    private boolean showHighRange;
    private boolean showValues;
    private boolean showAverage;
    private boolean showLowRange;
    private boolean showLow;
    private boolean showBaseline;
    private double baseline;

    private double highRange;
    private double lowRange;

    /**
     * Create the image being rendered.
     *
     * @param request the servlet request
     */
    @Override
    protected Object createImage(HttpServletRequest request) throws ServletException {
        // initialize the chart
        Chart chart = createChart();

        initializeChart(chart);

        // the subclass is responsible for plotting the data
        if (log.isDebugEnabled())
            log.debug("Plotting data...");
        plotData(request, chart);
        return chart;
    }

    /**
     * Render a PNG version of the image into the output stream.
     *
     * @param out the output stream
     */
    @Override
    protected void renderPngImage(ServletOutputStream out, Object imgObj) throws IOException {
        Chart chart = (Chart) imgObj;
        chart.writePngImage(out);
    }

    /**
     * Render a JPEG version of the image into the output stream.
     *
     * @param out the output stream
     */
    @Override
    protected void renderJpegImage(ServletOutputStream out, Object imgObj) throws IOException {
        Chart chart = (Chart) imgObj;
        chart.writeJpegImage(out);
    }

    /**
     * This method will be called automatically by the ChartServlet. It should handle the parsing and error-checking of
     * any specific parameters for the chart being rendered.
     *
     * @param request the HTTP request object
     */
    @Override
    protected void parseParameters(HttpServletRequest request) {
        super.parseParameters(request);

        // units
        String parameter = request.getParameter(MEASUREMENT_UNITS_PARAM);
        if (parameter != null && !"".equals(parameter)) {
            units = MeasurementUnits.valueOf(parameter);
        } else {
            units = MeasurementUnits.NONE;
            log.warn("Request did not specify measurement units. Using NONE.");
        }

        // chart flags
        showPeak = parseBooleanParameter(request, SHOW_PEAK_PARAM, getDefaultShowPeak());
        showHighRange = parseBooleanParameter(request, SHOW_HIGHRANGE_PARAM, getDefaultShowHighRange());
        showValues = parseBooleanParameter(request, SHOW_VALUES_PARAM, getDefaultShowValues());
        showAverage = parseBooleanParameter(request, SHOW_AVERAGE_PARAM, getDefaultShowAverage());
        showLowRange = parseBooleanParameter(request, SHOW_LOWRANGE_PARAM, getDefaultShowLowRange());
        showLow = parseBooleanParameter(request, SHOW_LOW_PARAM, getDefaultShowLow());
        showBaseline = parseBooleanParameter(request, SHOW_BASELINE_PARAM, getDefaultShowBaseline());

        // baseline, high range and low range
        if (showBaseline) {
            try {
                baseline = parseRequiredDoubleParameter(request, BASELINE_PARAM);
            } catch (IllegalArgumentException e) {
                if (log.isDebugEnabled()) {
                    log.debug("invalid " + BASELINE_PARAM + ", setting " + SHOW_BASELINE_PARAM + " to: " + false);
                }

                showBaseline = false;
            }
        }

        if (showHighRange) {
            try {
                highRange = parseRequiredDoubleParameter(request, HIGHRANGE_PARAM);
            } catch (IllegalArgumentException e) {
                if (log.isDebugEnabled()) {
                    log.debug("invalid " + HIGHRANGE_PARAM + ", setting " + SHOW_HIGHRANGE_PARAM + " to: " + false);
                }

                showHighRange = false;
            }
        }

        if (showLowRange) {
            try {
                lowRange = parseRequiredDoubleParameter(request, LOWRANGE_PARAM);
            } catch (IllegalArgumentException e) {
                if (log.isDebugEnabled()) {
                    log.debug("invalid " + LOWRANGE_PARAM + ", setting " + SHOW_LOWRANGE_PARAM + " to: " + false);
                }

                showLowRange = false;
            }
        }

        _logParameters();
    }

    /**
     * Create and return the chart. This method will be called after the parameters have been parsed.
     *
     * @return the newly created chart
     */
    protected abstract Chart createChart();

    /**
     * Initialize the chart. This method will be called after the parameters have been parsed and the chart has been
     * created.
     *
     * @param chart the chart
     */
    protected void initializeChart(Chart chart) {
        chart.setFormat(units);
        chart.showPeak = showPeak;
        chart.showHighRange = showHighRange;
        chart.showValues = showValues;
        chart.showAverage = showAverage;
        chart.showLowRange = showLowRange;
        chart.showLow = showLow;
        chart.showBaseline = showBaseline;
        chart.baseline = baseline;
        chart.highRange = highRange;
        chart.lowRange = lowRange;
    }

    /**
     * This method will be called automatically by the ChartServlet. It should handle adding data to the chart, setting
     * up the X and Y axis labels, etc.
     *
     * @param request the HTTP request
     */
    protected abstract void plotData(HttpServletRequest request, Chart chart) throws ServletException;

    /**
     * Return the value of property <code>showLow</code>.
     */
    public boolean getShowLow() {
        return this.showLow;
    }

    /**
     * Return the value of property <code>showPeak</code>.
     */
    public boolean getShowPeak() {
        return this.showPeak;
    }

    /**
     * Return the value of property <code>showAverage</code>.
     */
    public boolean getShowAvg() {
        return this.showAverage;
    }

    /**
     * Return the default <code>imageWidth</code>.
     */
    @Override
    protected int getDefaultImageWidth() {
        return IMAGE_WIDTH_DEFAULT;
    }

    /**
     * Return the default <code>imageHeight</code>.
     */
    @Override
    protected int getDefaultImageHeight() {
        return IMAGE_HEIGHT_DEFAULT;
    }

    /**
     * Return the default <code>showPeak</code>.
     */
    protected boolean getDefaultShowPeak() {
        return false;
    }

    /**
     * Return the default <code>showHighRange</code>.
     */
    protected boolean getDefaultShowHighRange() {
        return false;
    }

    /**
     * Return the default <code>showValues</code>.
     */
    protected boolean getDefaultShowValues() {
        return true;
    }

    /**
     * Return the default <code>showAverage</code>.
     */
    protected boolean getDefaultShowAverage() {
        return false;
    }

    /**
     * Return the default <code>Range</code>.
     */
    protected boolean getDefaultShowLowRange() {
        return false;
    }

    /**
     * Return the default <code>showLow</code>.
     */
    protected boolean getDefaultShowLow() {
        return false;
    }

    /**
     * Return the default <code>showBaseline</code>.
     */
    protected boolean getDefaultShowBaseline() {
        return false;
    }

    //---------------------------------------------------------------
    //-- private helpers
    //---------------------------------------------------------------
    private void _logParameters() {
        if (log.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer("Parameters:");
            sb.append("\n");
            sb.append("\t");
            sb.append(MEASUREMENT_UNITS_PARAM);
            sb.append(": ");
            sb.append(units);
            sb.append("\n");
            sb.append("\t");
            sb.append(SHOW_PEAK_PARAM);
            sb.append(": ");
            sb.append(showPeak);
            sb.append("\n");
            sb.append("\t");
            sb.append(SHOW_HIGHRANGE_PARAM);
            sb.append(": ");
            sb.append(showHighRange);
            sb.append("\n");
            sb.append("\t");
            sb.append(SHOW_VALUES_PARAM);
            sb.append(": ");
            sb.append(showValues);
            sb.append("\n");
            sb.append("\t");
            sb.append(SHOW_AVERAGE_PARAM);
            sb.append(": ");
            sb.append(showAverage);
            sb.append("\n");
            sb.append("\t");
            sb.append(SHOW_LOWRANGE_PARAM);
            sb.append(": ");
            sb.append(showLowRange);
            sb.append("\n");
            sb.append("\t");
            sb.append(SHOW_LOW_PARAM);
            sb.append(": ");
            sb.append(showLow);
            sb.append("\n");
            sb.append("\t");
            sb.append(SHOW_BASELINE_PARAM);
            sb.append(": ");
            sb.append(showBaseline);
            sb.append("\n");
            sb.append("\t");
            sb.append(BASELINE_PARAM);
            sb.append(": ");
            sb.append(baseline);
            sb.append("\n");
            sb.append("\t");
            sb.append(HIGHRANGE_PARAM);
            sb.append(": ");
            sb.append(highRange);
            sb.append("\n");
            sb.append("\t");
            sb.append(LOWRANGE_PARAM);
            sb.append(": ");
            sb.append(lowRange);
            log.debug(sb.toString());
        }
    }
}

// EOF
