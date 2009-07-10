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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.WorkflowPrepareAction;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.measurement.MeasurementChartsManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplayValue;
import org.rhq.enterprise.server.util.LookupUtil;

public class CompareMetricsFormPrepareAction extends WorkflowPrepareAction {
    protected static Log log = LogFactory.getLog(CompareMetricsFormPrepareAction.class);

    @Override
    public ActionForward workflow(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        CompareMetricsForm cform = (CompareMetricsForm) form;

        WebUser user = SessionUtils.getWebUser(request.getSession());

        MeasurementPreferences preferences = user.getMeasurementPreferences();
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();

        if ((cform.childResourceIds != null) && (cform.childResourceIds.length > 0)) {
            int[] definitionIds = LookupUtil.getResourceGroupManager().findDefinitionsForCompatibleGroup(
                user.getSubject(), cform.getGroupId(), false);
            Locale userLocale = request.getLocale();
            cform.setMetrics(getMetrics(user.getSubject(), ArrayUtils.unwrapArray(cform.childResourceIds),
                definitionIds, rangePreferences.begin, rangePreferences.end, userLocale));
            cform.setRb(rangePreferences.begin);
            cform.setRe(rangePreferences.end);
        }

        return null;
    }

    private Map<MeasurementCategory, Map<MeasurementDefinition, List<MetricDisplaySummary>>> getMetrics(
        Subject subject, int[] resourceIds, int[] definitionIds, long begin, long end, Locale locale) {
        MeasurementChartsManagerLocal chartsManager = LookupUtil.getMeasurementChartsManager();
        Map<MeasurementDefinition, List<MetricDisplaySummary>> data = chartsManager
            .getMetricDisplaySummariesForMetricsCompare(subject, resourceIds, definitionIds, begin, end);
        Map<MeasurementCategory, Map<MeasurementDefinition, List<MetricDisplaySummary>>> compareMetrics = new HashMap<MeasurementCategory, Map<MeasurementDefinition, List<MetricDisplaySummary>>>();
        for (MeasurementDefinition definition : data.keySet()) {
            MeasurementCategory category = definition.getCategory();
            Map<MeasurementDefinition, List<MetricDisplaySummary>> listWithinCategory = compareMetrics.get(category);
            if (listWithinCategory == null) {
                listWithinCategory = new HashMap<MeasurementDefinition, List<MetricDisplaySummary>>();
                compareMetrics.put(category, listWithinCategory);
            }

            List<MetricDisplaySummary> listOfData = data.get(definition);
            for (MetricDisplaySummary summary : listOfData) {
                MonitorUtils.formatSimpleMetrics(summary, locale);
                setSummaryHasValues(summary);
            }

            listWithinCategory.put(definition, listOfData);
        }

        return compareMetrics;
    }

    private void setSummaryHasValues(MetricDisplaySummary summary) {
        Map<String, MetricDisplayValue> metrics = summary.getMetrics();
        for (MetricDisplayValue metricDisplayValue : metrics.values()) {
            Double value = metricDisplayValue.getValue();
            if ((value != null) && !value.isNaN()) {
                summary.setValuesPresent(true);
            }
        }
    }
}