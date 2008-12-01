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
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.WorkflowPrepareAction;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplayValue;
import org.rhq.enterprise.server.util.LookupUtil;

public class CompareMetricsFormPrepareAction extends WorkflowPrepareAction {
    protected static Log log = LogFactory.getLog(CompareMetricsFormPrepareAction.class.getName());

    /* (non-Javadoc)
     * @see
     * org.rhq.enterprise.gui.legacy.action.WorkflowPrepareAction#workflow(org.apache.struts.tiles.ComponentContext,
     * org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm,
     * javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public ActionForward workflow(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        CompareMetricsForm cform = (CompareMetricsForm) form;
        Subject subject = WebUtility.getSubject(request);
        WebUser user = SessionUtils.getWebUser(request.getSession());
        Map<String, ?> range = user.getPreferences().getMetricRangePreference();

        long begin = ((Long) range.get(MonitorUtils.BEGIN));
        long end = ((Long) range.get(MonitorUtils.END));

        if ((cform.childResourceIds != null) && (cform.childResourceIds.length > 0)) {
            int[] definitionIds = LookupUtil.getResourceGroupManager().getDefinitionsForCompatibleGroup(subject,
                cform.getGroupId(), false);
            Locale userLocale = request.getLocale();
            cform.setMetrics(getMetrics(subject, cform.childResourceIds, definitionIds, begin, end, userLocale));
            MetricRange mr = new MetricRange(begin, end);
            prepareForm(request, cform, mr);
        }

        return null;
    }

    protected void prepareForm(HttpServletRequest request, MetricsControlForm form, MetricRange range)
        throws IllegalArgumentException {
        WebUser user = SessionUtils.getWebUser(request.getSession());

        // set metric range defaults
        Map pref = user.getPreferences().getMetricRangePreference(true);
        form.setReadOnly((Boolean) pref.get(MonitorUtils.RO));
        form.setRn((Integer) pref.get(MonitorUtils.LASTN));
        form.setRu((Integer) pref.get(MonitorUtils.UNIT));

        if (range != null) {
            form.setRb(range.getBegin());
            form.setRe(range.getEnd());
        } else {
            form.setRb((Long) pref.get(MonitorUtils.BEGIN));
            form.setRe((Long) pref.get(MonitorUtils.END));
        }
    }

    private Map<MeasurementCategory, Map<MeasurementDefinition, List<MetricDisplaySummary>>> getMetrics(
        Subject subject, Integer[] rids, int[] definitionIds, long begin, long end, Locale locale) {
        MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
        Map<MeasurementDefinition, List<MetricDisplaySummary>> data = dataManager
            .getMetricDisplaySummariesForMetricsCompare(subject, rids, definitionIds, begin, end);
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