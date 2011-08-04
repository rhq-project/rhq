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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.KeyConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.RetCodeConstants;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility.ResourceVisibilityPortalAction;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility.TraitHistoryFormPrepareAction;
import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;
import org.rhq.enterprise.gui.legacy.util.ActionUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;

/**
 * A <code>BaseDispatchAction</code> that sets up common monitor portals.
 */
public class VisibilityPortalAction extends ResourceVisibilityPortalAction {
    private static final String TITLE_CURRENT_HEALTH = "resource.common.monitor.visibility.CurrentHealthTitle";

    private static final String PORTLET_CURRENT_HEALTH = ".resource.common.monitor.visibility.CurrentHealth";

    private static final String TITLE_FAVORITE_METRICS = "resource.common.monitor.visibility.FavoriteMetricsTitle";

    private static final String PORTLET_FAVORITE_METRICS = ".resource.common.monitor.visibility.FavoriteMetrics";

    private static final String TITLE_COMMON_METRICS = "resource.common.monitor.visibility.MetricsTitle";

    private static final String PORTLET_COMMON_METRICS = ".resource.common.monitor.visibility.Metrics";

    private static final String TITLE_PERFORMANCE = "resource.common.monitor.visibility.PerformanceTitle";

    private static final String PORTLET_PERFORMANCE = ".resource.common.monitor.visibility.Performance";

    private static final String ERR_PLATFORM_PERMISSION = "resource.common.monitor.visibility.error.PlatformPermission";

    private static final String TITLE_EDIT_RANGE = "resource.common.monitor.visibility.MetricDisplayRangeTitle";

    private static final String PORTLET_EDIT_RANGE = ".resource.common.monitor.visibility.MetricDisplayRange";

    private static final String TITLE_CONFIGURE_VISIBILITY = "resource.common.monitor.visibility.MetricDisplayRangeTitle";

    private static final String PORTLET_CONFIGURE_VISIBILITY = ".resource.common.monitor.visibility.MetricDisplayRange";

    private static final String TITLE_CHART = "resource.common.monitor.visibility.ChartTitle";

    private static final String PORTLET_CHART_SMSR = ".resource.common.monitor.visibility.charts.metric.smsr";

    private static final String PORTLET_CHART_SMMR = ".resource.common.monitor.visibility.charts.metric.smmr";

    private static final String PORTLET_CHART_MMSR = ".resource.common.monitor.visibility.charts.metric.mmsr";

    private static final String PORTLET_TRAIT_HISTORY = ".resource.common.monitor.visibility.trait.history";

    private static final String TITLE_COMPARE_METRICS = "resource.common.monitor.visibility.CompareMetricsTitle";

    private static final String PORTLET_COMPARE_METRICS = ".resource.common.monitor.visibility.CompareMetrics";

    private static final String PORTLET_METRIC_METADATA = ".resource.common.monitor.visibility.MetricMetadata";

    private static final String TITLE_METRIC_METADATA = "resource.common.monitor.visibility.MetricMetadata";

    protected static Log log = LogFactory.getLog(VisibilityPortalAction.class.getName());

    /**
     * Process the specified HTTP request, and create the corresponding HTTP response (or forward to another web
     * component that will create it). Return an <code>ActionForward</code> instance describing where and how control
     * should be forwarded, or <code>null</code> if the response has already been completed.
     *
     * @param     mapping  The ActionMapping used to select this instance
     * @param     request  The HTTP request we are processing
     * @param     response The HTTP response we are creating
     * @param     form     The optional ActionForm bean for this request (if any)
     *
     * @return    Describes where and how control should be forwarded.
     *
     * @exception Exception if an error occurs
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        ActionForward fwd = super.execute(mapping, form, request, response);
        //        if (!LookupUtil.getSystemManager().isMonitoringEnabled()) {
        //            ActionRedirect redirect = new ActionRedirect(mapping.findForward("inventory-jsf"));
        //            redirect.addParameter("id", request.getParameter("id"));
        //            redirect.setRedirect(false);
        //            fwd = redirect;
        //        }

        return fwd;
    }

    @Override
    protected Properties getKeyMethodMap() {
        Properties map = new Properties();
        map.put(KeyConstants.MODE_MON_CUR, "currentHealth");
        map.put(KeyConstants.MODE_MON_RES_METS, "resourceMetrics");
        map.put(KeyConstants.MODE_MON_PERF, "performance");
        map.setProperty(KeyConstants.MODE_MON_EDIT_RANGE, "editRange");
        map.setProperty(ParamConstants.MODE_CONFIGURE, "configureVisibility");
        map.setProperty(ParamConstants.MODE_MON_CHART_SMSR, "chartSingleMetricSingleResource");
        map.setProperty(ParamConstants.MODE_MON_CHART_SMMR, "chartSingleMetricMultiResource");
        map.setProperty(ParamConstants.MODE_MON_CHART_MMSR, "chartMultiMetricSingleResource");
        map.setProperty(ParamConstants.MODE_MON_COMPARE_METRICS, "compareMetrics");
        map.setProperty(ParamConstants.MODE_MON_METRIC_METADATA, "metricMetadata");
        map.setProperty("showTraitHistory", "showTraitHistory"); // TODO constant

        return map;
    }

    @Override
    public ActionForward currentHealth(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        //findHostHealths(request);

        super.currentHealth(mapping, form, request, response);

        Portal portal = Portal.createPortal(TITLE_CURRENT_HEALTH, PORTLET_CURRENT_HEALTH);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    @Override
    public ActionForward resourceMetrics(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        //findHostHealths(request);

        super.resourceMetrics(mapping, form, request, response);

        Portal portal = Portal.createPortal(TITLE_COMMON_METRICS, PORTLET_COMMON_METRICS);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    @Override
    public ActionForward performance(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        super.performance(mapping, form, request, response);

        setResource(request);

        Portal portal = Portal.createPortal(TITLE_PERFORMANCE, PORTLET_PERFORMANCE);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward editRange(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal(TITLE_EDIT_RANGE, PORTLET_EDIT_RANGE);
        portal.setWorkflowPortal(true);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward configureVisibility(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal(TITLE_CONFIGURE_VISIBILITY, PORTLET_CONFIGURE_VISIBILITY);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    /**
     * Chart a single metric for a single resource.
     */
    public ActionForward chartSingleMetricSingleResource(ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal(TITLE_CHART, PORTLET_CHART_SMSR);
        portal.setDialog(false);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        String returnURL = SessionUtils.getReturnPath(request.getSession());
        if (returnURL != null) {
            request.setAttribute(RetCodeConstants.BACK_URL, returnURL);
        }

        return null;
    }

    /**
     * Chart a single metric for a multiple resources.
     */
    public ActionForward chartSingleMetricMultiResource(ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        setResource(request); // handles resource and group

        Portal portal = Portal.createPortal(TITLE_CHART, PORTLET_CHART_SMMR);
        portal.setDialog(false);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    /**
     * Chart multiple metrics for a single resource.
     */
    public ActionForward chartMultiMetricSingleResource(ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal(TITLE_CHART, PORTLET_CHART_MMSR);
        portal.setDialog(false);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    /**
     * Show a page with the historic data of the selected trait.
     *
     * @see TraitHistoryFormPrepareAction
     */
    public ActionForward showTraitHistory(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal(TITLE_CHART, PORTLET_TRAIT_HISTORY);
        portal.setDialog(false);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward metricMetadata(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal(TITLE_METRIC_METADATA, PORTLET_METRIC_METADATA);
        portal.setDialog(true);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward compareMetrics(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal(TITLE_COMPARE_METRICS, PORTLET_COMPARE_METRICS);

        portal.setDialog(true);
        portal.setWorkflowPortal(true);

        // we potentially have a chained workflow happening here:
        //   wherever we came from =>
        //     compare metrics workflow =>
        //       edit metric display range workflow
        //
        // first we set up a bunch of workflow params so that the
        // return path that goes to the compare metrics page has
        // all the state it needs
        //
        // then we push the return path that goes to wherever we came
        // from on to the compare metrics workflow
        //
        // next, ResourceController uses our workflow params to set up
        // the return path that goes to the compare metrics page. this
        // is used if we go to the edit display range page.
        //
        // finally, when the user clicks the "back to wherever we came
        // from" link on the compare metrics page, we have to pop the
        // return path for the compare metrics page off the compare
        // metrics workflow. this leaves only one return path: the one
        // that goes back to wherever we came from. this happens in
        // CompareMetricsAction.
        //
        // the last thing to remember is that the compare metrics page
        // has to use the same workflow property in struts-config.xml
        // as the edit metric display range page. this allows them to
        // utilize the same workflow stack.

        // set up workflow params that preserve state for compare
        // metrics page

        portal.setWorkflowParams(makeCompareWorkflowParams(request));

        // push old return path onto compare metrics workflow
        SessionUtils.pushWorkflow(request.getSession(false), mapping, AttrConstants.WORKFLOW_COMPARE_METRICS_NAME);

        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    private Map makeCompareWorkflowParams(HttpServletRequest request) {
        Map params = new HashMap();

        params.put(ParamConstants.MODE_PARAM, WebUtility.getOptionalRequestParameter(request,
            ParamConstants.MODE_PARAM, null));

        /*
         * if (request.getAttribute("id") != null && !request.getAttribute("id").equals("")) {
         * params.put(Constants.RESOURCE_ID_PARAM, RequestUtils.getResourceId(request));
         * params.put(Constants.RESOURCE_TYPE_ID_PARAM, WebUtility.getResourceTypeId(request));
         * params.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM, WebUtility.getChildResourceTypeId(request));} */
        params.put("name", request.getParameter("name"));

        // make sure none of these values are duplicated
        String[] rawValues = request.getParameterValues("r");
        List<String> cookedValues = new ArrayList<String>();
        Map<String, String> idx = new HashMap<String, String>();
        for (String value : rawValues) {
            if (idx.get(value) == null) {
                cookedValues.add(value);
                idx.put(value, value);
            }
        }

        params.put("r", cookedValues.toArray(new String[0]));

        return params;
    }

    /**
     * This sets the return path for a ResourceAction by appending the type and resource id to the forward url.
     *
     * @param  request The current controller's request.
     * @param  mapping The current controller's mapping that contains the input.
     *
     * @throws ParameterNotFoundException if the type or id are not found
     * @throws ServletException           If there is not input defined for this form
     */
    @Override
    protected void setReturnPath(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        this.fetchReturnPathParams(request, params);
        String mode = (String) params.get(ParamConstants.MODE_PARAM);
        if ((mode != null) && mode.startsWith("chart")) {
            // Don't save any path back to charts
            return;
        }

        String returnPath = ActionUtils.findReturnPath(mapping, params);
        if (log.isTraceEnabled()) {
            log.trace("setting return path: " + returnPath);
        }

        SessionUtils.setReturnPath(request.getSession(), returnPath);
    }
}