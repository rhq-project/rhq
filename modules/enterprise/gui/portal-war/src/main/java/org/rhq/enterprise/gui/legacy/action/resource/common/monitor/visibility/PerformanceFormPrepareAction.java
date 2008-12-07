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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.MetricRangePreferences;
import org.rhq.enterprise.gui.legacy.util.ChartData;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An <code>Action</code> that prepares pages containing the performance form.
 *
 * @author Ian Springer
 */
public class PerformanceFormPrepareAction extends MetricsControlFormPrepareAction {
    protected static Log log = LogFactory.getLog(PerformanceFormPrepareAction.class);

    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve data needed to display a resource's child metrics form. Respond to certain button clicks that alter the
     * form display.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        super.execute(mapping, form, request, response);

        // decide what timeframe we're showing. it may have been shifted on previous views of this page.
        MetricRange range = (MetricRange) request.getAttribute(Constants.METRIC_RANGE);
        if (range == null) {
            // this is the first time out. get the "metric range" user pref.
            WebUser user = SessionUtils.getWebUser(request.getSession());
            WebUserPreferences preferences = user.getPreferences();
            MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
            range = new MetricRange();
            range.setBegin(rangePreferences.begin);
            range.setEnd(rangePreferences.end);
        }

        Subject subject = WebUtility.getSubject(request);
        Resource resource = (Resource) request.getAttribute(AttrConstants.RESOURCE_ATTR);
        MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
        List<MeasurementSchedule> callTimeSchedules = scheduleManager.getMeasurementSchedulesForResourceAndType(
            subject, resource.getId(), DataType.CALLTIME, null, false);

        PerformanceForm perfForm = (PerformanceForm) form;
        prepareForm(request, perfForm, callTimeSchedules);

        MeasurementSchedule selectedSchedule = null;
        if ((perfForm.getScheduleId() != null) && !perfForm.getScheduleId().equals(PerformanceForm.DEFAULT_SCHEDULE_ID)) {
            for (MeasurementSchedule callTimeSchedule : callTimeSchedules) {
                if (callTimeSchedule.getId() == perfForm.getScheduleId()) {
                    selectedSchedule = callTimeSchedule;
                }
            }
        }

        PageList<CallTimeDataComposite> callTimeDataComposites;
        if (selectedSchedule != null) {
            MeasurementDefinition measurementDef = selectedSchedule.getDefinition();
            request.setAttribute("MeasurementDef", measurementDef);
            CallTimeDataManagerLocal callTimeDataManager = LookupUtil.getCallTimeDataManager();
            PageControl pageControl = WebUtility.getPageControl(request);
            callTimeDataComposites = callTimeDataManager.getCallTimeDataForResource(subject, selectedSchedule.getId(),
                range.getBegin(), range.getEnd(), pageControl);
        } else {
            callTimeDataComposites = new PageList<CallTimeDataComposite>();
        }

        if (log.isDebugEnabled()) {
            for (CallTimeDataComposite datum : callTimeDataComposites) {
                log.debug("Call-time datum: " + datum);
            }
        }

        request.setAttribute(Constants.PERF_SUMMARIES_ATTR, callTimeDataComposites);

        if (selectedSchedule != null) {
            // Save chart data into session, so it can be displayed by the performance chart servlet.
            ChartData chartData = createChartData(callTimeDataComposites, perfForm, selectedSchedule);
            request.getSession().setAttribute(Constants.CHART_DATA_SES_ATTR, chartData);
        }

        return null;
    }

    // ---------------------------------------------------- Protected Methods

    protected void prepareForm(HttpServletRequest request, PerformanceForm form, List<MeasurementSchedule> schedules)
        throws IllegalArgumentException {
        if (!form.isAnythingClicked()) {
            form.setLow(PerformanceForm.DEFAULT_LOW);
            form.setAvg(PerformanceForm.DEFAULT_AVG);
            form.setPeak(PerformanceForm.DEFAULT_PEAK);
        }

        PageControl pageControl = WebUtility.getPageControl(request);
        form.setPn(pageControl.getPageNumber());
        form.setPs(pageControl.getPageSize());
        form.setSc(pageControl.getPrimarySortColumn());
        PageOrdering ordering = pageControl.getPrimarySortOrder();
        form.setSo((ordering != null) ? ordering.name() : null);

        form.setSchedules(schedules.toArray(new MeasurementSchedule[schedules.size()]));
        form.setMetricCount(schedules.size());
        if (schedules.size() == 1) {
            // If there's only a single schedule, auto-select it.
            form.setScheduleId(schedules.get(0).getId());
        }

        super.prepareForm(request, form);
    }

    protected ChartData createChartData(PageList<CallTimeDataComposite> callTimeDataComposites, PerformanceForm form,
        MeasurementSchedule schedule) {
        ChartData chartData = new ChartData();
        chartData.setSummaries(callTimeDataComposites);
        chartData.setShowLow(form.getLow());
        chartData.setShowAvg(form.getAvg());
        chartData.setShowPeak(form.getPeak());
        chartData.setMeasurementDefinition(schedule.getDefinition());
        return chartData;
    }
}