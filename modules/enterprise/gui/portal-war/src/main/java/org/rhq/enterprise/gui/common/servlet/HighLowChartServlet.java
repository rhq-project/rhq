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
import javax.servlet.SingleThreadModel;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.image.chart.Chart;
import org.rhq.enterprise.gui.image.chart.DataPointCollection;
import org.rhq.enterprise.gui.image.chart.HighLowChart;
import org.rhq.enterprise.gui.legacy.DefaultConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;
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
public class HighLowChartServlet extends ChartServlet implements SingleThreadModel {
    private static final int NUMBER_OF_DATA_POINTS = DefaultConstants.DEFAULT_CHART_POINTS;

    private final Log log = LogFactory.getLog(HighLowChartServlet.class);
    private int scheduleId;
    private int definitionId;
    private int groupId;
    private int parentId;
    private int childTypeId;

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

    @Override
    protected void parseParameters(HttpServletRequest request) {
        // TODO if we have a schedule, we should also check the id= and see if that matches
        scheduleId = WebUtility.getOptionalIntRequestParameter(request, "schedId", -1);
        groupId = WebUtility.getOptionalIntRequestParameter(request, "groupId", -1);
        parentId = WebUtility.getOptionalIntRequestParameter(request, "parent", -1);
        childTypeId = WebUtility.getOptionalIntRequestParameter(request, "type", -1);
        definitionId = WebUtility.getOptionalIntRequestParameter(request, "definitionId", -1);

        /* 
         * RHQ-743 - if we don't parse the request parameters here, on rare occasion the chart will be initialized 
         *           without any units, which then defaults to percentage; however, if the data represents bytes, and
         *           if that value is large (in the GB range) then the Y-axis formatter for the Chart will attempt
         *           to render GB labels are percentages, causing results like "227,040,000,000%"; by parsing all
         *           HighLowChartServlet parameters here, we can guarantee that even if the caller did not pass the
         *           units, they can be deduced from the various other parameters passed
         */
        String parameter = request.getParameter(MEASUREMENT_UNITS_PARAM);
        if (parameter == null || parameter.equals("")) {
            Subject overlord = LookupUtil.getSubjectManager().getOverlord();
            if (scheduleId > 0) {
                units = getUnitsFromScheduleId(overlord, scheduleId);
            } else {
                units = getUnitsFromDefinitionId(overlord, definitionId);
            }
            log.info("Caller did not pass MeasuremntUnits, calculated them as " + units.getName());
        } else {
            log.info("Caller passed MeasurementUnits of " + parameter);
        }

        super.parseParameters(request);
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.gui.common.servlet.ChartServlet#plotData(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected void plotData(HttpServletRequest request, Chart chart) throws ServletException {
        // Make sure the schedule id was passed in.
        // TODO: Pass in a resource/group id and a measurement definition id instead. (ips, 04/16/07)
        if (log.isDebugEnabled()) {
            log.debug("Requesting: " + request.getQueryString());
        }

        WebUser user = SessionUtils.getWebUser(request.getSession());
        MeasurementPreferences preferences = user.getMeasurementPreferences();
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

    private MeasurementUnits getUnitsFromScheduleId(Subject subject, int measurementScheduleId) {
        MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
        MeasurementSchedule schedule = scheduleManager.getMeasurementScheduleById(subject, measurementScheduleId);
        return schedule.getDefinition().getUnits();
    }

    private MeasurementUnits getUnitsFromDefinitionId(Subject subject, int measurementDefinitionId) {
        MeasurementDefinitionManagerLocal definitionManager = LookupUtil.getMeasurementDefinitionManager();
        MeasurementDefinition definition = definitionManager.getMeasurementDefinitionById(subject,
            measurementDefinitionId);
        return definition.getUnits();
    }
}