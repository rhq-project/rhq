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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.enterprise.gui.image.chart.Chart;
import org.rhq.enterprise.gui.image.chart.DataPoint;
import org.rhq.enterprise.gui.image.chart.PerfDataPointCollection;
import org.rhq.enterprise.gui.image.chart.StackedPerformanceChart;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.util.ChartData;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.rt.SegmentInfo;

/**
 * Performance chart servlet.
 *
 * <p/>Additional parameters are as follows (any required parameters are in <i>italics</i>):
 *
 * <p/>
 * <table border="1">
 *   <tr>
 *     <th>key</th>
 *     <th>value</th>
 *   </tr>
 *   <tr>
 *     <td>perfChartType</td>
 *     <td>{ 'url' | 'urldetail' | 'type' }</td>
 *   </tr>
 * </table>
 *
 * @author Ian Springer
 */
public class PerformanceChartServlet extends ChartServlet {
    // member data
    private Log log = LogFactory.getLog(PerformanceChartServlet.class.getName());
    private String perfChartType = null;
    private int numCharts = 0;
    private String m_destinationType;

    private static final int DEFAULT_PERF_IMAGE_HEIGHT = 300;
    private static final int DEFAULT_PERF_IMAGE_WIDTH = 755;

    /**
     * Request parameter for performance chart type.
     */
    public static final String PERF_CHART_TYPE_PARAM = "perfChartType";

    /**
     * Request parameter value representing a url perfchart.
     */
    public static final String CHART_TYPE_URL = "url";

    /**
     * Request parameter value representing a urldetail perf chart.
     */
    public static final String CHART_TYPE_URLDETAIL = "urldetail";

    /**
     * Request parameter value representing an type perf chart.
     */
    public static final String CHART_TYPE_TYPE = "type";

    // the valid values of PERF_CHART_TYPE_PARAM
    private static final String[] VALID_PERF_CHART_TYPES = { CHART_TYPE_URL, CHART_TYPE_URLDETAIL, CHART_TYPE_TYPE, };

    // labels for the performance chart. These correspond to
    // the Rt tier constants in RtConstants

    // @see net.covalent.rt.RtConstants
    private static final String WEBSERVER_LABEL_PROPERTY = "resource.common.monitor.visibility.VitualHostHeaderTH";
    private static final String APPSERVER_LABEL_PROPERTY = "resource.common.monitor.visibility.WebappHeaderTH";
    private static final String ENDUSER_LABEL_PROPERTY = "resource.common.monitor.visibility.EndUserHeaderTH";

    private static final String LOW_LABEL_PROPERTY = "resource.common.monitor.visibility.LowTH";
    private static final String AVG_LABEL_PROPERTY = "resource.common.monitor.visibility.AvgTH";
    private static final String PEAK_LABEL_PROPERTY = "resource.common.monitor.visibility.PeakTH";

    public PerformanceChartServlet() {
    }

    /**
     * This method will be called automatically by the ChartServlet. It should handle the parsing and error-checking of
     * any specific parameters for the chart being rendered.
     *
     * <p/>Handles parsing of perfChartType request parameter.
     *
     * @param request the HTTP request object
     */
    @Override
    protected void parseParameters(HttpServletRequest request) {
        String tmpPerfChartType = parseRequiredStringParameter(request, PERF_CHART_TYPE_PARAM, VALID_PERF_CHART_TYPES);
        setPerfChartType(tmpPerfChartType);

        // take the chart data from the session.
        HttpSession session = request.getSession(false);
        ChartData chartData = (ChartData) session.getAttribute(Constants.CHART_DATA_SES_ATTR);
        if (chartData != null) {
            if (perfChartType.equals(CHART_TYPE_URL) || perfChartType.equals(CHART_TYPE_TYPE)) {
                setNumCharts(chartData.getSummaries().size());
            } else if (perfChartType.equals(CHART_TYPE_URLDETAIL)) {
                setNumCharts(chartData.getSegments().size());
            }

            if (units == null || units == MeasurementUnits.NONE) {
                units = chartData.getMeasurementDefinition().getUnits();
            }

            m_destinationType = chartData.getMeasurementDefinition().getDestinationType();
        }

        super.parseParameters(request);
    }

    /**
     * Create and return the chart. This method will be called after the parameters have been parsed.
     *
     * @return the newly created chart
     */
    @Override
    protected Chart createChart() {
        return new StackedPerformanceChart(getImageWidth(), getNumCharts(), m_destinationType);
    }

    /**
     * This method will be called automatically by the ChartServlet. It should handle adding data to the chart, setting
     * up the X and Y axis labels, etc.
     *
     * @param request the HTTP request object
     */
    @Override
    protected void plotData(HttpServletRequest request, Chart chart) throws ServletException {
        StackedPerformanceChart perfChart = (StackedPerformanceChart) chart;

        // Grab the chart data from the session.
        HttpSession session = request.getSession(false);
        ChartData chartData = (ChartData) session.getAttribute(Constants.CHART_DATA_SES_ATTR);
        if (chartData == null) {
            log.debug("Did not find performance chart data to plot.");
            return;
        }

        /*
         *    // labels for the different types of graphs   HashMap labels = new HashMap();   labels.put(new
         * Integer(RtConstants.APPSERVER),       RequestUtils.message(request, APPSERVER_LABEL_PROPERTY));
         * labels.put(new Integer(RtConstants.WEBSERVER),       RequestUtils.message(request,
         * WEBSERVER_LABEL_PROPERTY));   labels.put(new Integer(RtConstants.ENDUSER),
         * RequestUtils.message(request, ENDUSER_LABEL_PROPERTY));
         */

        // labels for the different types of measurements
        Map mlabels = new HashMap();
        mlabels.put(LOW_LABEL_PROPERTY, RequestUtils.message(request, LOW_LABEL_PROPERTY));
        mlabels.put(AVG_LABEL_PROPERTY, RequestUtils.message(request, AVG_LABEL_PROPERTY));
        mlabels.put(PEAK_LABEL_PROPERTY, RequestUtils.message(request, PEAK_LABEL_PROPERTY));

        if (isTypeChart() || isUrlChart()) {
            List<CallTimeDataComposite> summaries = chartData.getSummaries();
            perfChart.setNumberDataSets(summaries.size());

            Iterator summariesIter = summaries.iterator();
            Iterator barsIter = perfChart.getDataSetIterator();
            while (summariesIter.hasNext() && barsIter.hasNext()) {
                PerfDataPointCollection bars = (PerfDataPointCollection) barsIter.next();
                CallTimeDataComposite summary = (CallTimeDataComposite) summariesIter.next();
                Integer segmentId = null;

                /*if (isTypeChart())
                 * { // dig through the summaries to figure out // which segment has data if (summary.getMinimum() !=
                 * null) {   segmentId = grovel(summary.getMinimum()); } else if (summary.getAverage() != null) {
                 * segmentId = grovel(summary.getAverage()); } else if (summary.getMaximum() != null) {   segmentId =
                 * grovel(summary.getMaximum()); }}*/
                fillOutSummarySection(bars, chartData, summary, segmentId, mlabels);
            }
        } else if (isUrlDetailChart()) {
            //            Map segments = data.getSegments();
            //            Set segmentIds = segments.keySet();
            //            perfChart.setNumberDataSets(segmentIds.size());
            //            PerformanceSummary summary = null;
            //            Integer segmentId = null;
            //
            //            List summaries = null;
            //            Iterator summariesIter = null;
            //            Iterator barsIter = perfChart.getDataSetIterator();
            //            Iterator segmentIdIter = segmentIds.iterator();
            //            while (segmentIdIter.hasNext()) {
            //                segmentId = (Integer) segmentIdIter.next();
            //
            //                summaries = (List) segments.get(segmentId);
            //                summariesIter = summaries.iterator();
            //                while (summariesIter.hasNext()) {
            //                    bars = (PerfDataPointCollection) barsIter.next();
            //                    summary = (PerformanceSummary) summariesIter.next();
            //
            //                    fillOutSummarySection(bars, data, summary, segmentId,
            //                                          mlabels);
            //                }
            //            }
        }

        // remove the chart data from the session.
        session.removeAttribute(Constants.CHART_DATA_SES_ATTR);
    }

    //---------------------------------------------------------------
    //-- private helpers
    //---------------------------------------------------------------

    /*
     * // this method figures out which "tier" we're in private Integer grovel(SegmentInfo info) {    for (int i =
     * RtConstants.STARTRTTYPE; i < RtConstants.ENDRTTYPE; i++){        if (info.getSegment(i-1) != null) {
     * return new Integer(i);        }    }    return new Integer(RtConstants.UNKNOWN); }
     */

    private void fillOutSummarySection(PerfDataPointCollection bars, ChartData data, CallTimeDataComposite summary,
        Integer segmentId, Map mlabels) {
        // header value
        if (segmentId != null) {
            bars.setType(segmentId.intValue());
        }

        // XXX: i really want to be able to set the type string with
        // one of the labels i pulled from message resources, but
        // there's no api for it

        bars.setURL(summary.getCallDestination());

        if ((data.getShowReq() != null) && data.getShowReq()) {
            bars.setRequest((int) summary.getCount());
        }

        if ((data.getShowLow() != null) && data.getShowLow()) {
            bars.add(new DataPoint(summary.getMinimum(), (String) mlabels.get(LOW_LABEL_PROPERTY)));
            //addDataPoint(bars, summary.getMinimum(), (String)mlabels.get(LOW_LABEL_PROPERTY));
        }

        if ((data.getShowAvg() != null) && data.getShowAvg()) {
            bars.add(new DataPoint(summary.getAverage(), (String) mlabels.get(AVG_LABEL_PROPERTY)));
            //addDataPoint(bars, summary.getAverage(), (String)mlabels.get(AVG_LABEL_PROPERTY));
        }

        if ((data.getShowPeak() != null) && data.getShowPeak()) {
            bars.add(new DataPoint(summary.getMaximum(), (String) mlabels.get(PEAK_LABEL_PROPERTY)));
            //addDataPoint(bars, summary.getMaximum(), (String)mlabels.get(PEAK_LABEL_PROPERTY));
        }
    }

    private void addDataPoint(PerfDataPointCollection bars, SegmentInfo segInfo, String label) {
        if (segInfo != null) {
            if (log.isTraceEnabled()) {
                log.trace("adding " + label + " value: " + segInfo.getSegments());
            }

            segInfo.setLabel(label);
            bars.add(segInfo);
        } else {
            bars.add(new DataPoint(Double.NaN, label));
        }
    }

    /**
     * Return the default <code>imageHeight</code>.
     */
    @Override
    protected int getDefaultImageHeight() {
        return DEFAULT_PERF_IMAGE_HEIGHT;
    }

    /**
     * Return the default <code>imageWidth</code>.
     */
    @Override
    protected int getDefaultImageWidth() {
        return DEFAULT_PERF_IMAGE_WIDTH;
    }

    public String getPerfChartType() {
        return this.perfChartType;
    }

    public void setPerfChartType(String type) {
        this.perfChartType = type;
    }

    public int getNumCharts() {
        return this.numCharts;
    }

    public void setNumCharts(int size) {
        this.numCharts = size;
    }

    public boolean isTypeChart() {
        return getPerfChartType().equals(CHART_TYPE_TYPE);
    }

    public boolean isUrlChart() {
        return getPerfChartType().equals(CHART_TYPE_URL);
    }

    public boolean isUrlDetailChart() {
        return getPerfChartType().equals(CHART_TYPE_URLDETAIL);
    }
}