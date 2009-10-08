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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceHealthComposite;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.legacy.measurement.MeasurementConstants;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An <code>Action</code> that retrieves data from the BizApp to facilitate display of the various pages that provide
 * metrics summaries.
 */
public abstract class MetricsDisplayFormPrepareAction extends MetricsControlFormPrepareAction {
    protected static Log log = LogFactory.getLog(MetricsDisplayFormPrepareAction.class);

    protected int groupId = -1;

    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve data needed to display a Metrics Display Form. Respond to certain button clicks that alter the form
     * display.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response, Long begin, Long end) throws Exception {
        MetricsDisplayForm displayForm = (MetricsDisplayForm) form;
        displayForm.setShowNumberCollecting(getShowNumberCollecting());
        displayForm.setShowBaseline(getShowBaseline());
        displayForm.setShowMetricSource(getShowMetricSource());
        WebUser user = SessionUtils.getWebUser(request.getSession());

        int[] resourceIds;
        groupId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.GROUP_ID_PARAM, -1);
        if (groupId > 0) {
            ResourceGroupManagerLocal resGrpMgr = LookupUtil.getResourceGroupManager();
            List<Resource> resources = resGrpMgr.findResourcesForResourceGroup(user.getSubject(), groupId,
                GroupCategory.COMPATIBLE);

            // Can we put the ResourceType in the Request so we don't need another EJB call in MetricsControlFormPrepareAction.java
            if (resources.size() > 0) {
                ResourceType resourceType = resources.get(0).getResourceType();
                request.setAttribute(AttrConstants.RESOURCE_TYPE_ATTR, resourceType);
            }

            resourceIds = new int[resources.size()];
            int i = 0;
            for (Resource res : resources) {
                resourceIds[i] = res.getId();
                i++;
            }
        } else {
            resourceIds = WebUtility.getResourceIds(request);
        }

        if ((begin == null) || (end == null)) {
            // get the "metric range" user pref
            MeasurementPreferences preferences = user.getMeasurementPreferences();
            MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();

            begin = rangePreferences.begin;
            end = rangePreferences.end;
        }

        List<MetricDisplaySummary> metrics;

        if (displayForm.isFilterSubmitClicked()) {
            metrics = getMetrics(request, resourceIds, displayForm.getFilters(), displayForm.getKeyword(), begin, end);
        } else {
            metrics = getMetrics(request, resourceIds, MeasurementConstants.FILTER_NONE, null, begin, end);
        }

        if (metrics != null) {
            //            if (log.isTraceEnabled()) {
            //                log.trace("Got formatted metrics");
            //                MonitorUtils.traceMetricDisplaySummaryMap(log, metrics);
            //            }

            /*
             * Separate numerical and trait metrics
             */
            List<MetricDisplaySummary> metricsTrait = new ArrayList<MetricDisplaySummary>();
            for (MetricDisplaySummary mds : metrics) {
                if (mds.getIsTrait()) {
                    metricsTrait.add(mds);
                }
            }

            metrics.removeAll(metricsTrait);

            // TODO GH: What's this do?
            // hwr: format the raw numerical values. Only good for numerical metrics.
            Integer resourceCount = MonitorUtils.formatMetrics(metrics, request.getLocale(), getResources(request));

            request.setAttribute(AttrConstants.NUM_CHILD_RESOURCES_ATTR, resourceCount);
            request.setAttribute(AttrConstants.METRIC_SUMMARIES_ATTR, metrics);
            request.setAttribute(AttrConstants.METRIC_SUMMARIES_ATTR_TRAIT, metricsTrait);

            //getResourceCurrentHealths  GroupMemberHealthSummaries
            ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
            PageList<ResourceHealthComposite> groupMemberHealthSummaries = resourceManager.findResourceHealth(user
                .getSubject(), resourceIds, new PageControl());
            request.setAttribute(AttrConstants.GROUP_MEMBER_HEALTH_SUMMARIES_ATTR, groupMemberHealthSummaries);

            // populate the form
            //displayForm.setupCategoryList(metrics);
            // prepareForm(request, displayForm);
            displayForm.setMeasurementSummaryList(metrics);
        } else {
            log.trace("no metrics were returned by getMetrics(...)");
        }

        // Clear any compare metric workflow
        SessionUtils.clearWorkflow(request.getSession(), AttrConstants.WORKFLOW_COMPARE_METRICS_NAME);

        return super.execute(mapping, form, request, response);
    }

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        return execute(mapping, form, request, response, null, null);
    }

    // ---------------------------------------------------- Protected Methods

    /**
     * Do we show the number collecting column on this page? The default answer is no, but subclasses can specify
     * otherwise.
     */
    protected Boolean getShowNumberCollecting() {
        return Boolean.FALSE;
    }

    /**
     * Do we show the baseline column on this page? The default answer is no, but subclasses can specify otherwise.
     */
    protected Boolean getShowBaseline() {
        return Boolean.FALSE;
    }

    /**
     * Do we show the metric source column on this page? The default answer is no, but subclasses can specify otherwise.
     */
    protected Boolean getShowMetricSource() {
        return Boolean.FALSE;
    }

    /**
     * Get from the Bizapp the set of metric summaries for the specified entities that will be displayed on the page.
     * Returns a <code>Map</code> keyed by metric category.
     *
     * @param  request     the http request
     * @param  resourceIds the array of resource id of the currently viewed resource/group
     * @param  begin       the time (in milliseconds since the epoch) that begins the timeframe for which the metrics
     *                     are summarized
     * @param  end         the time (in milliseconds since the epoch) that ends the timeframe for which the metrics are
     *                     summarized
     *
     * @return Map keyed on the category (String), values are List's of MetricDisplaySummary beans
     */
    protected abstract List<MetricDisplaySummary> getMetrics(HttpServletRequest request, int[] resourceIds,
        long filters, String keyword, Long begin, Long end) throws Exception;

    private void prepareForm(HttpServletRequest request, MetricsDisplayForm form) throws IllegalArgumentException {
        WebUser user = SessionUtils.getWebUser(request.getSession());

        // set threshold default
        MeasurementPreferences preferences = user.getMeasurementPreferences();
        Integer threshold = preferences.getMetricThresholdPreference();
        switch (threshold) {
        case MonitorUtils.THRESHOLD_HIGH_RANGE_VALUE: {
            form.setDisplayHighRange(Boolean.TRUE);
            break;
        }

        case MonitorUtils.THRESHOLD_LOW_RANGE_VALUE: {
            form.setDisplayLowRange(Boolean.TRUE);
            break;
        }

        default: {
            form.setDisplayBaseline(Boolean.TRUE);
            threshold = MonitorUtils.THRESHOLD_BASELINE_VALUE;
        }
        }

        form.setT(threshold);

        if (form.isCurrentClicked()) {
            // XXX: refresh measurements and cache them in the
            // session
        } else {
            // XXX: get measurements cached in the session; if
            // none cached, load current measurements
        }

        // set highlight state
        if (form.isHighlightClicked()) {
            // XXX: recalculate highlights
            form.setH(Boolean.TRUE);
            log.trace("calculating highlights");
        } else if (form.isClearClicked()) {
            // reset highlight widgets to null
            log.trace("clearing highlights");
            form.setH(Boolean.FALSE);
            form.setHv(null);
            form.setHp(null);
            form.setHt(null);
        } else if (form.isAddClicked() || form.isRemoveClicked()) {
            // deselect all metrics
            form.setM(new Integer[0]);
        }

        super.prepareForm(request, form);
    }
}