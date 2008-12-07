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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.image.chart.Chart;
import org.rhq.enterprise.gui.image.chart.DataPointCollection;
import org.rhq.enterprise.gui.image.chart.HighLowChart;
import org.rhq.enterprise.gui.legacy.DefaultConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.MetricRangePreferences;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Display a high-low chart. This groks three kinds of input:
 *
 * <ul>
 *   <li>schedId: show the data for a single schedule denoted by schedid</li>
 *   <li>groupId + definitionId: show data for the passed definitionId and the given compatible group</li>
 *   <li>id + childTypeId + definitionId: show data for the passed definition of the autogroup id/resourceTypeId</li>
 * </ul>
 *
 * @author Ian Springer
 * @author Heiko W. Rupp
 */
public class HighLowChartServlet extends ChartServlet {
    private static final int NUMBER_OF_DATA_POINTS = DefaultConstants.DEFAULT_CHART_POINTS;

    private final Log log = LogFactory.getLog(HighLowChartServlet.class);

    /* (non-Javadoc)
     * @see org.rhq.enterprise.gui.common.servlet.ChartServlet#createChart()
     */
    @Override
    protected Chart createChart() {
        return new HighLowChart(getImageWidth(), getImageHeight());
    }

    /**
     * Initialize the chart. This method will be called after the parameters have been parsed and the chart has been
     * created.
     *
     * @param chart the chart
     */
    @Override
    protected void initializeChart(Chart chart) {
        super.initializeChart(chart);
        HighLowChart highLowChart = (HighLowChart) chart;
        highLowChart.setNumberDataSets(1);
        highLowChart.leftBorder = 0;
        highLowChart.rightLabelWidth = (int) (this.getImageWidth() * 0.1);
        highLowChart.columnWidth = 7;
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.gui.common.servlet.ChartServlet#plotData(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected void plotData(HttpServletRequest request, Chart chart) throws ServletException {
        // Make sure the schedule id was passed in.
        // TODO: Pass in a resource/group id and a measurement definition id instead. (ips, 04/16/07)
        int groupId = -1;
        int definitionId = -1;
        int parentId = -1;
        int childTypeId = -1;

        // TODO if we have a schedule, we should also check the id= and see if that matches
        int scheduleId = WebUtility.getOptionalIntRequestParameter(request, "schedId", -1);

        // if no schedule is found, check for compatible group
        if (scheduleId == -1) {
            groupId = WebUtility.getOptionalIntRequestParameter(request, "groupId", -1);
        }

        // if no schedule and no compatible group are found check for autogroup
        if ((scheduleId == -1) && (groupId == -1)) {
            parentId = WebUtility.getOptionalIntRequestParameter(request, "id", -1);
            childTypeId = WebUtility.getOptionalIntRequestParameter(request, "ctype", -1);
        }

        // for group and autogroup
        definitionId = WebUtility.getOptionalIntRequestParameter(request, "definitionId", -1);

        WebUser user = SessionUtils.getWebUser(request.getSession());
        WebUserPreferences preferences = user.getPreferences();
        Subject subject = user.getSubject();

        MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
        MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();

        // set metric range defaults
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
        long beginTime = rangePreferences.begin;
        long endTime = rangePreferences.end;
        List<MeasurementDataNumericHighLowComposite> dataPoints = null;

        if (scheduleId > 0) // single resource
        {
            MeasurementSchedule schedule = scheduleManager.getMeasurementScheduleById(subject, scheduleId);

            if (schedule != null) {
                if (log.isDebugEnabled())
                    log.debug("Plotting a high-low chart data for metric " + schedule.getDefinition().getName()
                        + " on resource " + schedule.getResource().getName() + "...");
                dataPoints = dataManager.getMeasurementDataForResource(subject, schedule.getResource().getId(),
                    new int[] { schedule.getDefinition().getId() }, beginTime, endTime, NUMBER_OF_DATA_POINTS).get(0);
            } else {
                log.debug("Passed scheduleId " + scheduleId + " has no schedule attached, ignoring");
                return;
            }
        }

        /*
         * Now look at compatible groups and autogroups
         *
         */
        else if ((groupId > 0) && (definitionId > 0)) // compatible group
        {
            dataPoints = dataManager.getMeasurementDataForCompatibleGroup(subject, groupId, definitionId, beginTime,
                endTime, NUMBER_OF_DATA_POINTS, true).get(0);
        } else if ((parentId > 0) && (childTypeId > 0) && (definitionId > 0)) //  autogroup
        {
            dataPoints = dataManager.getMeasurementDataForAutoGroup(subject, parentId, childTypeId, definitionId,
                beginTime, endTime, NUMBER_OF_DATA_POINTS, true).get(0);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No valid input found for HighLowChart: [schedId=" + scheduleId + ", defId=" + definitionId
                    + ", groupId=" + groupId + ", parentId=" + parentId + ", childTypeId=" + childTypeId + "]");
            }

            return;
        }

        List<HighLowMetricValue> chartDataPoints = new ArrayList<HighLowMetricValue>(dataPoints.size());
        for (MeasurementDataNumericHighLowComposite dataPoint : dataPoints) {
            chartDataPoints.add(new HighLowMetricValue(dataPoint));
        }

        HighLowChart highLowChart = (HighLowChart) chart;
        DataPointCollection bars = highLowChart.getDataPoints(0);
        bars.addAll(chartDataPoints);
    }
}