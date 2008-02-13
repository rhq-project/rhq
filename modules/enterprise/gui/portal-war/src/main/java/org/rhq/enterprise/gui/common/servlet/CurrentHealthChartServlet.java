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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.enterprise.gui.image.chart.Chart;
import org.rhq.enterprise.gui.legacy.Constants;

/**
 * <p>CurrentHealth chart servlet. The default <code>imageWidth</code> is 250 pixels. The default <code>
 * imageHeight</code> is 130 pixels.</p>
 *
 * <p>by default, this servlet will display an 8 column chart for the past 8 hours at 1 hour intervals based on the
 * metric category returned by the <code>{@link getMetricCategory}()</code>.</p>
 *
 * <p>Additional parameters are as follows (any required parameters are in <i>italics</i>):</p>
 *
 * <table border="1">
 *   <tr>
 *     <th>key</th>
 *     <th>value</th>
 *   </tr>
 *   <tr>
 *     <td> <i>eid</i></td>
 *     <td>&lt;string or string[]&gt;</td>
 *   </tr>
 *   <tr>
 *     <td>ctype</td>
 *     <td>&lt;integer&gt;</td>
 *   </tr>
 * </table>
 */
public abstract class CurrentHealthChartServlet extends VerticalChartServlet {
    /**
     * Interval for metrics.
     */
    protected static final long INTERVAL = Constants.MINUTES * 30; // 1/2 hour

    /**
     * Default image width.
     */
    public static final int IMAGE_WIDTH_DEFAULT = 200;

    /**
     * Default image height.
     */
    public static final int IMAGE_HEIGHT_DEFAULT = 100;

    // member data
    private Log log = LogFactory.getLog(CurrentHealthChartServlet.class.getName());

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
     * Return the corresponding measurement category.
     *
     * @return <code>{@link org.rhq.enterprise.server.legacy.measurement.MeasurementConstants.CAT_AVAILABILITY}</code>
     *         or <code>{@link org.rhq.enterprise.server.legacy.measurement.MeasurementConstants.CAT_THROUGHPUT}</code>
     *         or <code>{@link org.rhq.enterprise.server.legacy.measurement.MeasurementConstants.CAT_PERFORMANCE}</code>
     *         or <code>{@link org.rhq.enterprise.server.legacy.measurement.MeasurementConstants.CAT_UTILIZATION}</code>
     */
    protected abstract String getMetricCategory();

    @Override
    protected void initializeChart(Chart chart) {
        super.initializeChart(chart);
        chart.font = Chart.SMALL_FONT;
        chart.showFullLabels = false;
    }

    /**
     * This method will be called automatically by the ChartServlet. It should handle adding data to the chart, setting
     * up the X and Y axis labels, etc.
     *
     * @param request the HTTP request
     */
    @Override
    protected void plotData(HttpServletRequest request, Chart chart) throws ServletException {
        //        AppdefEntityID[] eids = null;
        //        AppdefEntityTypeID ctype = null;
        //        try {
        //            eids = RequestUtils.getEntityIds(request);
        //        } catch (ParameterNotFoundException e) {
        //            /* platform auto-group */
        //        }
        //
        //        try {
        //            ctype = RequestUtils.getHqChildResourceTypeId(request);
        //        } catch (ParameterNotFoundException e) {
        //            /* non auto-group */
        //        }
        //
        //        Integer tid = RequestUtils.getIntParameter(request, "tid");
        //        try {
        //            VerticalChart verticalChart = (VerticalChart) chart;
        //
        //            long endTime = System.currentTimeMillis();
        //            long beginTime = endTime - (8l * Constants.HOURS);
        //
        //            MeasurementBoss mb =
        //                ContextUtils.getMeasurementBoss( getServletContext() );
        //
        //            List data = null;
        ////            try {
        ////                String user = RequestUtils.getStringParameter(request, "user");
        ////                data = getData(user, mb, verticalChart, tid, eids, ctype,
        ////                               beginTime, endTime);
        ////            } catch (ParameterNotFoundException e) {
        ////                int sessionID = RequestUtils.getSessionId(request).intValue();
        ////                data = getData(sessionID, mb, verticalChart, tid, eids, ctype,
        ////                               beginTime, endTime);
        ////            }
        //
        //            if (log.isDebugEnabled()) {
        //                log.debug("Got " + data.size() + " " + getMetricCategory()
        //                        + " metric data points.");
        //                if (log.isTraceEnabled()) {
        //                    log.debug("data=" + data);
        //                }
        //            }
        //
        //            DataPointCollection chartData = chart.getDataPoints();
        //            chartData.addAll(data);
        //        } catch (MeasurementNotFoundException e) {
        //            if ( log.isDebugEnabled() ) // don't log internal category names PR 6417
        //                log.debug( "No " + getMetricCategory() + " metric found for: " +
        //                           StringUtil.arrayToString(eids) );
        //        } catch (AppdefEntityNotFoundException e) {
        //            if ( log.isDebugEnabled() )
        //                log.debug( "One or more AppdefEntityIDs invalid: " +
        //                           StringUtil.arrayToString(eids) );
        //        } catch (DataNotAvailableException e) {
        //            if ( log.isDebugEnabled() )
        //                log.debug("No metric data available.");
        //        } catch (PermissionException e) {
        //            log.warn("Permission denied to view metric.");
        //        } catch (SessionNotFoundException e) {
        //            log.warn("Session not found.");
        //        } catch (SessionTimeoutException e) {
        //            log.warn("Session timeout.");
        //        } catch (RemoteException e) {
        //            log.warn("Unknown error.", e);
        //        } catch (TemplateNotFoundException e) {
        //            log.warn("Template " + tid + " not found", e);
        //        } catch (LoginException e) {
        //            log.warn("Unable to login user", e);
        //        } catch (ApplicationException e) {
        //            log.warn("Error looking measurement data to chart", e);
        //        } catch (ConfigPropertyException e) {
        //            log.warn("Configuration error", e);
        //        }
    }

    //    private List getData(String user, MeasurementBoss mb, VerticalChart chart,
    //                         Integer tid, AppdefEntityID[] eids,
    //                         AppdefEntityTypeID ctype, long beginTime, long endTime)
    //        throws LoginException, ApplicationException, RemoteException,
    //               ConfigPropertyException {
    //        Integer[] tids = new Integer[] { tid };
    //
    //        List templates =
    //            mb.findMeasurementTemplates(user, tids, PageControl.PAGE_ALL);
    //
    //        MeasurementTempl tmpv =
    //            (MeasurementTempl) templates.get(0);
    //
    //        if (log.isDebugEnabled())
    //            log.debug("template ID=" + tmpv.getId());
    //
    //        setChartUnits(chart, tmpv);
    //
    //        if (null == ctype) {
    //            return mb.findMeasurementData(user, eids[0], tmpv,
    //                                          beginTime, endTime, INTERVAL,
    //                                          true, PageControl.PAGE_ALL);
    //        } else {
    //            return mb.findAGMeasurementData(user, eids, tmpv, ctype,
    //                                            beginTime, endTime, INTERVAL,
    //                                            true, PageControl.PAGE_ALL);
    //        }
    //    }
    //
    //    private List getData(int sessionID, MeasurementBoss mb, VerticalChart chart,
    //                         Integer tid, AppdefEntityID[] eids,
    //                         AppdefEntityTypeID ctype, long beginTime, long endTime)
    //        throws TemplateNotFoundException, SessionNotFoundException,
    //               SessionTimeoutException, DataNotAvailableException,
    //               AppdefEntityNotFoundException, MeasurementNotFoundException,
    //               PermissionException, RemoteException {
    //        Integer[] tids = new Integer[] { tid };
    //
    //        List templates =
    //            mb.findMeasurementTemplates(sessionID, tids, PageControl.PAGE_ALL);
    //
    //        MeasurementTempl tmpv =
    //            (MeasurementTempl) templates.get(0);
    //
    //        if (log.isDebugEnabled())
    //            log.debug("template ID=" + tmpv.getId());
    //
    //        setChartUnits(chart, tmpv);
    //
    //        if (null == ctype) {
    //            return mb.findMeasurementData(sessionID, eids[0], tmpv,
    //                                          beginTime, endTime, INTERVAL,
    //                                          true, PageControl.PAGE_ALL);
    //        } else {
    //            return mb.findAGMeasurementData(sessionID, eids, tmpv, ctype,
    //                                            beginTime, endTime, INTERVAL,
    //                                            true, PageControl.PAGE_ALL);
    //        }
    //    }
    //
    //    private void setChartUnits(VerticalChart chart,
    //                               MeasurementTempl tmpv) {
    //        // override default / parsed units with the one from the metric
    //        UnitsConstants unitUnits = UnitsConvert.getUnitForUnit(tmpv.getUnits());
    //        ScaleConstants unitScale = UnitsConvert.getScaleForUnit(tmpv.getUnits());
    //        chart.setFormat(unitUnits, unitScale);
    //        int cumulativeTrend =
    //            getTrendForCollectionType(tmpv.getCollectionType());
    //        chart.setCumulativeTrend(cumulativeTrend);
    //    }
}

// EOF
