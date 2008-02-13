package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.KeyConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.RetCodeConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.DashboardUtils;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.apache.struts.util.MessageResources;

import org.rhq.enterprise.gui.util.MetricsDisplayMode;
import org.rhq.enterprise.gui.util.WebUtility;

import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.enterprise.server.auth.SessionNotFoundException;
import org.rhq.enterprise.server.auth.SessionTimeoutException;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementException;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Generate the metric info for the indicator charts to be displayed.
 * 
 * Identifying individual metric summaries / charts very much use the concept of a metricToken.
 * This token is a string, that identifies the metric summary and where it comes from - if
 * it is a metric summary for a single resource or for a group. See {@link #generateMetricToken(MetricDisplaySummary)} and
 * {@link #parseMetricToken(String)} on how the metric token looks like.
 * This token is also used in ListChildResources.jsp to add new charts and in DashCharts.jsp
 * to generate the input for up/down/remove. 
 * 
 * The used {@link IndicatorViewsForm} is filled in the {@link CurrentHealthAction} class,
 * which unlike most of the actions preparing a form is not called *PrepareAction.
 * 
 * @author Heiko W. Rupp (for the RHQ rewrite)
 */
public class IndicatorChartsAction extends DispatchAction {
    private static final String EWWW = "ewww";

    private final static Log log = LogFactory.getLog(IndicatorChartsAction.class);

    private static final String DEFAULT_VIEW = "Default"; // resource.common.monitor.visibility.defaultview

    private static String PREF_DELIMITER = DashboardUtils.DASHBOARD_DELIMITER;
    private static String PREF_DELIMITER_SPLIT = "\\|";

    MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
    ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();

    private List<MetricDisplaySummary> getMetricsForSchedules(HttpServletRequest request, int resourceId,
        List<Integer> scheduleIds) throws Exception {
        WebUser user = SessionUtils.getWebUser(request.getSession());
        Subject subject = user.getSubject();

        // Get metric range defaults
        Map pref = user.getMetricRangePreference(true);
        long begin = (Long) pref.get(MonitorUtils.BEGIN);
        long end = (Long) pref.get(MonitorUtils.END);

        List<MetricDisplaySummary> summaries;
        try {
            summaries = dataManager.getMetricDisplaySummariesForSchedules(subject, resourceId, scheduleIds, begin, end,
                false);
        } catch (MeasurementException e) {
            throw new RuntimeException("Failed to retrieve metric display summaries for resource with id + "
                + resourceId + ".", e);
        }
        List<MetricDisplaySummary> indicatorDisplaySummaries = new ArrayList<MetricDisplaySummary>(summaries.size());
        Locale userLocale = request.getLocale();
        for (MetricDisplaySummary summary : summaries) {
            if (summary.getMetricKeys().length > 0)
                MonitorUtils.formatSimpleMetrics(summary, userLocale);
            summary.setResourceId(resourceId);
            indicatorDisplaySummaries.add(summary);
        }
        return indicatorDisplaySummaries;
    }

    private List<MetricDisplaySummary> getViewMetricsForSingleResource(HttpServletRequest request, int resourceId,
        String viewName) throws Exception {

        MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();

        String key = KeyConstants.INDICATOR_VIEWS + generateSessionKey(request, viewName);
        WebUser user = SessionUtils.getWebUser(request.getSession());
        Subject subject = user.getSubject();
        List<MeasurementSchedule> scheds;
        /*
         * Try to get the schedules for this view from the preferences and extract the 
         * schedule ids from it. If this fails, fall back to defaults.
         */
        try {
            String metricsStr = user.getPreference(generatePrefsMetricsKey(key, viewName));
            if ("".equals(metricsStr))
                throw new IllegalArgumentException("No metrics defined"); // Use defaults then from below
            List<String> tokens = StringUtil.explode(metricsStr, PREF_DELIMITER);
            List<Integer> schIds = new ArrayList<Integer>(tokens.size());
            for (String metric : tokens) {
                metric = metric.split(",")[1];
                int schedId = Integer.parseInt(metric);
                schIds.add(schedId);
            }
            scheds = scheduleManager.getSchedulesByIds(schIds);
            // sort the schedules returned in the order they had in the tokens.
            // the backend unfortunately looses that information
            List<MeasurementSchedule> tmp = new ArrayList<MeasurementSchedule>(scheds.size());
            for (int id : schIds) {
                for (MeasurementSchedule sch : scheds) {
                    if (sch.getId() == id) {
                        tmp.add(sch);
                        break;
                    }
                }
            }
            scheds = tmp;
        } catch (IllegalArgumentException iae) {
            // No metrics in preferences? Use defaults for the resource (DisplayType==SUMMARY)
            scheds = scheduleManager.getMeasurementSchedulesForResourceAndType(subject, resourceId,
                DataType.MEASUREMENT, DisplayType.SUMMARY, false);
        }

        List<Integer> scheduleIds = new ArrayList<Integer>(scheds.size());
        for (MeasurementSchedule sched : scheds) {
            scheduleIds.add(sched.getId());
        }
        List<MetricDisplaySummary> idss = getMetricsForSchedules(request, resourceId, scheduleIds);
        return idss; // getMetrics(request, boss, resourceId, scheduleIds);

    }

    /**
     * Generates a key under which the indicator charts to show are stored and retrieved from
     * the users preferences. The key generated here must be coordinated with the one generated
     * in {@link CurrentHealthAction#setupViews(HttpServletRequest, IndicatorViewsForm, String)}
     * @param isViewsList If true this is a list of views for this monitor page, so we do not 
     * append the ".viewName"
     */
    private String generateSessionKey(IndicatorViewsForm form, boolean isViewsList) {
        String view;

        String viewName = form.getView();
        if (viewName == null || "".equals(viewName))
            viewName = DEFAULT_VIEW;

        if (form.getId() != null && form.getId() > 0)
            view = String.valueOf(form.getId());
        else if (form.getGroupId() > 0)
            view = "cg=" + form.getGroupId();
        else if (form.getCtype() != null && form.getCtype() > 0 && form.getParent() > 0)
            view = "ag=" + form.getParent() + ":" + form.getCtype();
        else
            view = EWWW;

        if (isViewsList == false && !(EWWW.equals(view))) {
            view += "." + viewName;
        }
        return view;
    }

    /**
     * Generates a key under which the indicator charts to show are stored and retrieved from
     * the users preferences.
     * This key is depending on the {@link MetricsDisplayMode} of the request.
     * @param viewName the name of the view we are interested in
     */
    private String generateSessionKey(HttpServletRequest request, String viewName) {
        MetricsDisplayMode mode = WebUtility.getMetricsDisplayMode(request);

        if (viewName == null || "".equals(viewName))
            viewName = DEFAULT_VIEW;

        String view;
        switch (mode) {
        case RESOURCE:
            view = WebUtility.getResourceId(request) + "." + viewName;
            break;
        case COMPGROUP:
            view = "cg=" + WebUtility.getOptionalIntRequestParameter(request, "groupId", -1);
            view += "." + viewName;
            break;
        case AUTOGROUP:
            int type = WebUtility.getOptionalIntRequestParameter(request, "type", -1);
            if (type == -1)
                type = WebUtility.getRequiredIntRequestParameter(request, "ctype"); // TODO JBNADM-2630
            view = "ag=" + WebUtility.getRequiredRequestParameter(request, "parent") + ":" + type;
            view += "." + viewName;
            break;
        default:
            view = EWWW;
        }
        return view;
    }

    private String generatePrefsMetricsKey(String key, String view) {
        return key + "." + view;
    }

    /**
     * Generate a key, that identifies the summary.
     * The format of the token is (without quotation marks):
     * <ul>
     * <li>For a compatible group: "cg,<i>groupId</i>,<i>definitionId</i>"</li>
     * <li>For an autogroup : "ag,<i>parentId</i>,<i>definitionId</i>,<i>childTypeId</i>"</li>
     * <li>For a single resource: "<i>resourceId</i>,<i>scheduleId</i>"</li>
     * </ul>
     * @see #parseMetricToken(String)
     */
    private String generateMetricToken(MetricDisplaySummary summary) {
        String ret = EWWW;
        MetricsDisplayMode mode = getDisplayModeForSummary(summary);
        switch (mode) {
        case RESOURCE:
            if (summary.getScheduleId() != null)
                ret = summary.getResourceId() + "," + summary.getScheduleId().toString();
            break;
        case COMPGROUP:
            ret = "cg," + summary.getGroupId() + "," + summary.getDefinitionId();
            break;
        case AUTOGROUP:
            ret = "ag," + summary.getParentId() + "," + summary.getDefinitionId() + "," + summary.getChildTypeId();
            break;
        default:
            log.warn("storeMetrics: Unknown mode for " + summary);
        }
        return ret;
    }

    /**
     * Stores the metric in the session and also in the passed form, so it can be
     * identified in moveUp()/moveDown()/remove() 
     */
    private void storeMetricsInSession(HttpServletRequest request, List<MetricDisplaySummary> metrics,
        IndicatorViewsForm form) {
        request.setAttribute(AttrConstants.CHART_DATA_KEYS, metrics);

        String[] scheduleIds = new String[metrics.size()];
        int i = 0;
        for (MetricDisplaySummary summary : metrics) {
            scheduleIds[i++] = generateMetricToken(summary);
        }
        form.setMetric(scheduleIds);

        // Set the metrics in the session
        String key = this.generateSessionKey(form, false);
        HttpSession session = request.getSession();
        if (EWWW.equals(key))
            key = (String) session.getAttribute("metricKey");
        session.setAttribute(key, metrics);
        session.setAttribute("metricKey", key);

    }

    /**
     * Look up metrics from session and load them if they are not yet there
     */
    private List<MetricDisplaySummary> retrieveMetricsFromSession(HttpServletRequest request, IndicatorViewsForm form)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException, ServletException {
        // GET group / parent etc. from the form?
        HttpSession session = request.getSession();
        String key = this.generateSessionKey(form, false);
        List<MetricDisplaySummary> metrics = (List<MetricDisplaySummary>) session.getAttribute(key);
        if (metrics == null || metrics.size() == 0) {
            key = (String) session.getAttribute("metricKey");
            if (key != null && !(key.equals("")))
                metrics = (List<MetricDisplaySummary>) request.getSession().getAttribute(key);
        }

        /* JBNADM-643 - NPE in IndicatorChartsAction
         --------------------------------------------------------------------
         The following two circumstances are very rare cases, however were
         added to help correct this issue.
         */

        // If metrics were not in the session, attempt to load them
        if (metrics == null || metrics.size() == 0) {
            String viewName = form.getView();

            try {
                MetricsDisplayMode mode = WebUtility.getMetricsDisplayMode(request);
                switch (mode) {
                case RESOURCE:
                    int resourceId = WebUtility.getResourceId(request);
                    metrics = getViewMetricsForSingleResource(request, resourceId, viewName);
                    break;
                case COMPGROUP:
                    int groupId = WebUtility.getRequiredIntRequestParameter(request, AttrConstants.GROUP_ID);
                    metrics = getViewMetricsForCompatibleGroup(request, groupId, viewName);
                    break;
                case AUTOGROUP:
                    int parent = WebUtility.getRequiredIntRequestParameter(request, "parent");
                    int type = WebUtility
                        .getRequiredIntRequestParameter(request, ParamConstants.RESOURCE_TYPE_ID_PARAM);
                    metrics = getViewMetricsForAutogroup(request, parent, type, viewName);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid display mode " + mode);
                }
            } catch (Exception e) {
                log.error("Error loading metrics (they were not found in the session)", e);
            }
        }

        // If they could not be loaded, return an empty list rather than null. We could
        // return Collections.EMPTY_LIST, but I'm unsure as to what the callers are doing, and
        // an unmodifiable list might cause issues.
        if (metrics == null) {
            metrics = new ArrayList<MetricDisplaySummary>();
        }

        return metrics;
    }

    private ActionForward error(ActionMapping mapping, HttpServletRequest request, String key) {
        RequestUtils.setError(request, key);
        return mapping.findForward(KeyConstants.MODE_MON_CUR);
    }

    public ActionForward fresh(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);

        IndicatorViewsForm ivf = (IndicatorViewsForm) form;
        String viewName = ivf.getView();

        List<MetricDisplaySummary> metrics = new ArrayList<MetricDisplaySummary>();

        /*
         * First try to load the metrics from the user preferences for that key.
         * If that fails, we load defaults for it.
         */
        String key = KeyConstants.INDICATOR_VIEWS + generateSessionKey(request, ivf.getView());
        try {
            String metricsStr = user.getPreference(generatePrefsMetricsKey(key, viewName));
            if (!("".equals(metricsStr))) {
                List<String> metricTokens = StringUtil.explode(metricsStr, PREF_DELIMITER);
                for (String token : metricTokens) {
                    MetricDisplaySummary tmp = parseMetricToken(token);
                    metrics.add(tmp);
                }
                metrics = reloadMetrics(request, metrics, true);
                storeMetricsInSession(request, metrics, ivf);

                return mapping.findForward(RetCodeConstants.SUCCESS_URL);
            }
        } catch (IllegalArgumentException iae) {
            if (log.isDebugEnabled())
                log.debug("Could not get data for view " + viewName + " from preferences: " + iae.getMessage());
        }

        /*
         * We are still here? Ok, get the stuff from preferences.
         * TODO sort out what we need from here and especially the called
         * getViewMetrics*() methods.
         */

        // Look up the metrics based on view name
        int resourceId = -1;
        int groupId = -1;

        resourceId = WebUtility.getOptionalIntRequestParameter(request, "id", -1);
        groupId = WebUtility.getOptionalIntRequestParameter(request, "groupId", -1);

        // TODO twist the logic around. First try to get the metrics from the preferences
        // and act accordingly with loading the MetricDisplaySummary s and only if that
        // fails, use the defaults.
        // The load from prefs / session and then get the MetricDisplaySummary stuff 
        // can be reused for refresh()

        if (resourceId > 0) {
            metrics = getViewMetricsForSingleResource(request, resourceId, viewName);
            for (MetricDisplaySummary summary : metrics) {
                summary.setMetricToken(generateMetricToken(summary));
            }
        } else if (groupId > 0) {
            metrics = getViewMetricsForCompatibleGroup(request, groupId, viewName);
            // loop over the metrics, put the groupId in and format the provided value
            for (MetricDisplaySummary summary : metrics) {
                summary.setGroupId(groupId);
                summary.setMetricToken(generateMetricToken(summary));
                MonitorUtils.formatSimpleMetrics(summary, null);
            }

            request.setAttribute(AttrConstants.CHART_DATA_KEYS, metrics); // for the big charts and DashCharts.jsp
        } else { // autogroup?
            int type = WebUtility.getOptionalIntRequestParameter(request, "ctype", -1);
            int parent = WebUtility.getOptionalIntRequestParameter(request, "parent", 1);
            if (type > 0 && parent > 0) {
                metrics = getViewMetricsForAutogroup(request, parent, type, viewName);
                for (MetricDisplaySummary summary : metrics) {
                    summary.setMetricToken(generateMetricToken(summary));
                    MonitorUtils.formatSimpleMetrics(summary, null);
                }
                request.setAttribute(AttrConstants.CHART_DATA_KEYS, metrics);
            }
        }
        // Set the metrics in the session and preferences
        storeMetricsInSession(request, metrics, ivf);
        storeMetricsInUserPreferences(request, ivf);

        return mapping.findForward(RetCodeConstants.SUCCESS_URL);
    }

    private List<MetricDisplaySummary> getViewMetricsForAutogroup(HttpServletRequest request, int parent, int type,
        String viewName) {
        WebUser user = SessionUtils.getWebUser(request.getSession());
        Subject subject = user.getSubject();
        String key = KeyConstants.INDICATOR_VIEWS + generateSessionKey(request, viewName);

        Map pref = user.getMetricRangePreference(true);
        long begin = (Long) pref.get(MonitorUtils.BEGIN);
        long end = (Long) pref.get(MonitorUtils.END);

        int[] measurementDefinitionIds;
        try {
            measurementDefinitionIds = fillDefinitionIdsFromUserPreferences(viewName, user, key);
        } catch (IllegalArgumentException iae) {
            // If we can't get stuff from preferences, get the defaults.
            measurementDefinitionIds = groupManager.getDefinitionsForAutoGroup(subject, parent, type, true);
        }
        // now that we have the definitions, we can get the data from the backend.
        List<MetricDisplaySummary> summaries;
        try {
            summaries = dataManager.getMetricDisplaySummariesForAutoGroup(subject, parent, type,
                measurementDefinitionIds, begin, end, false);
        } catch (MeasurementException me) {
            log.debug("Can't get ViewMetrics for autogroup: " + me);
            summaries = new ArrayList<MetricDisplaySummary>();
        }

        return summaries;
    }

    private List<MetricDisplaySummary> getViewMetricsForCompatibleGroup(HttpServletRequest request, int groupId,
        String viewName) {
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        Subject subject = user.getSubject();
        String key = KeyConstants.INDICATOR_VIEWS + generateSessionKey(request, viewName);

        Map pref = user.getMetricRangePreference(true);
        long begin = (Long) pref.get(MonitorUtils.BEGIN);
        long end = (Long) pref.get(MonitorUtils.END);

        /* Fiddle the metrics from the | delimited stored ones 
         * and only display those. Use the default list as fallback if we don't have them
         * in preferences
         */
        int[] measurementDefinitionIds;
        try {
            measurementDefinitionIds = fillDefinitionIdsFromUserPreferences(viewName, user, key);
        } catch (IllegalArgumentException iae) {
            // If we can't get stuff from preferences, get the defaults.
            measurementDefinitionIds = groupManager.getDefinitionsForCompatibleGroup(subject, groupId, true);
        }

        List<MetricDisplaySummary> summaries;
        try {
            summaries = dataManager.getMetricDisplaySummariesForCompatibleGroup(subject, groupId,
                measurementDefinitionIds, begin, end, false);
        } catch (MeasurementException me) {
            log.debug("Can't get ViewMetrics for Compat Group: " + me);
            summaries = new ArrayList<MetricDisplaySummary>();
        }
        return summaries;
    }

    /**
     * Get the definition ids (for groups) from the metrics stored in the preferences, which are separated by a vertical bar.
     */
    private int[] fillDefinitionIdsFromUserPreferences(String viewName, WebUser user, String key) {
        int[] measurementDefinitionIds;
        String metricsStr = user.getPreference(generatePrefsMetricsKey(key, viewName));
        if ("".equals(metricsStr))
            throw new IllegalArgumentException("No metrics defined"); // Use defaults then from the caller
        List<String> metrics = StringUtil.explode(metricsStr, PREF_DELIMITER);
        measurementDefinitionIds = new int[metrics.size()];
        int i = 0;
        for (String token : metrics) {
            MetricDisplaySummary tmp = parseMetricToken(token);
            measurementDefinitionIds[i++] = tmp.getDefinitionId();
        }
        return measurementDefinitionIds;
    }

    /**
     * Reload the passed metrics from the backend. The metrics need to be "preinitialized", which
     * means for each metric needs the identifiers (resource id, definition, group id, etc.) be set.
     * 
     * @param request http servlet request needed to get the time range preferences for the user. 
     * @param metrics The List of metrics to reload
     * @param force If true, always go to the backend, even if the time range preferences show a range in the past.
     * @return the refreshed list of metrics.
     * @todo Implement the timerange check 
     */
    private List<MetricDisplaySummary> reloadMetrics(HttpServletRequest request, List<MetricDisplaySummary> metrics,
        boolean force) {
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        Subject subject = user.getSubject();
        Map pref = user.getMetricRangePreference(true);
        long begin = (Long) pref.get(MonitorUtils.BEGIN);
        long end = (Long) pref.get(MonitorUtils.END);

        // TODO: if the user selected a fixed time range and not "last xxx" and force == false, then 
        //       we should not go to the backend

        List<MetricDisplaySummary> ret = new ArrayList<MetricDisplaySummary>(metrics.size());
        Locale userLocale = request.getLocale();
        for (MetricDisplaySummary sum : metrics) {
            MetricsDisplayMode mode = getDisplayModeForSummary(sum);

            List<MetricDisplaySummary> tmpList;
            MetricDisplaySummary tmp;
            switch (mode) {
            case RESOURCE:
                List<Integer> schIds = new ArrayList<Integer>(1);
                schIds.add(sum.getScheduleId());
                tmpList = dataManager.getMetricDisplaySummariesForSchedules(subject, sum.getResourceId(), schIds,
                    begin, end, false);
                break;
            case AUTOGROUP:
                tmpList = dataManager.getMetricDisplaySummariesForAutoGroup(subject, sum.getParentId(), sum
                    .getChildTypeId(), new int[] { sum.getDefinitionId() }, begin, end, false);
                break;
            case COMPGROUP:
                tmpList = dataManager.getMetricDisplaySummariesForCompatibleGroup(subject, sum.getGroupId(),
                    new int[] { sum.getDefinitionId() }, begin, end, false);
                break;
            default:
                tmpList = null;
            }
            if (tmpList != null && tmpList.size() > 0) {
                tmp = tmpList.get(0);
                tmp.setMetricToken(generateMetricToken(tmp));
                if (tmp.getMetricKeys().length > 0)
                    MonitorUtils.formatSimpleMetrics(tmp, userLocale);
                ret.add(tmp);
            } else
                log.error("We did not get a result back for " + sum);
        }

        return ret;
    }

    /**
     * A refresh() event coming in from the JSP layer. Is also called at the end of add() to
     * get the actual data from the backend of the newly added metric.
     */
    public ActionForward refresh(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;

        // Look up the metrics from the session
        List<MetricDisplaySummary> metrics = retrieveMetricsFromSession(request, ivf);

        // refresh the metrics
        metrics = reloadMetrics(request, metrics, false);

        // Now store the metrics back
        storeMetricsInSession(request, metrics, ivf);

        return mapping.findForward(RetCodeConstants.SUCCESS_URL);
    }

    /**
     * Add a metric encoded in the form to the list of indicator charts to display.
     */
    public ActionForward add(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;

        // Look up the metrics from the session
        List<MetricDisplaySummary> metrics = retrieveMetricsFromSession(request, ivf);

        if (metrics == null) {
            return mapping.findForward(RetCodeConstants.FAILURE_URL);
        }

        // Now look up the metric that we have to add and parse it
        String newMetric = ivf.getAddMetric();
        MetricDisplaySummary newSummary = parseMetricToken(newMetric);

        // Get the Metric Display summary , taking the display mode into account
        MetricsDisplayMode mode = getDisplayModeForSummary(newSummary); //WebUtility.getMetricsDisplayMode(request);

        // First check if the metric to add is already present
        boolean found = false;
        for (MetricDisplaySummary metric : metrics) {
            Integer definitionId = metric.getDefinitionId();
            switch (mode) {
            case RESOURCE:
                Integer schedId = metric.getScheduleId();
                if (schedId != null && schedId.equals(newSummary.getScheduleId())) {
                    found = true;
                    break;
                }
                break;
            case COMPGROUP:
                if (metric.getGroupId() == newSummary.getGroupId() && definitionId == newSummary.getDefinitionId()) {
                    found = true;
                    break;
                }
                break;
            case AUTOGROUP:
                int parent = metric.getParentId();
                int type = metric.getChildTypeId();
                if (parent == newSummary.getParentId() && type == newSummary.getChildTypeId()
                    && definitionId == newSummary.getDefinitionId()) {
                    found = true;
                    break;
                }
                break;
            default:
                throw new IllegalArgumentException(mode + " not valid here");

            }
        }

        // Add the new metrics
        if (!found) {
            WebUser user = SessionUtils.getWebUser(request.getSession());
            Subject subject = user.getSubject();

            // Get metric range defaults
            Map pref = user.getMetricRangePreference(true);
            long begin = (Long) pref.get(MonitorUtils.BEGIN);
            long end = (Long) pref.get(MonitorUtils.END);

            int[] measurementDefinitionIds = new int[1];
            switch (mode) {
            case RESOURCE:
                List<Integer> schedIds = new ArrayList<Integer>();
                List<MetricDisplaySummary> newSummaries;
                schedIds.add(newSummary.getScheduleId());
                metrics.addAll(getMetricsForSchedules(request, newSummary.getResourceId(), schedIds));
                // Now store the metrics back
                storeMetricsInSession(request, metrics, ivf);
                break;
            case COMPGROUP:
                // Get MetricDisplaySummaries from the backend for the new metrics and add them
                measurementDefinitionIds[0] = newSummary.getDefinitionId();

                newSummaries = dataManager.getMetricDisplaySummariesForCompatibleGroup(subject,
                    newSummary.getGroupId(), measurementDefinitionIds, begin, end, false);
                metrics.addAll(newSummaries);

                // Set the metrics in the session
                storeMetricsInSession(request, metrics, ivf);
                break;
            case AUTOGROUP:
                // Get MetricDisplaySummaries from the backend for the new metrics and add them
                measurementDefinitionIds[0] = newSummary.getDefinitionId();
                newSummaries = dataManager.getMetricDisplaySummariesForAutoGroup(subject, newSummary.getParentId(),
                    newSummary.getChildTypeId(), measurementDefinitionIds, begin, end, false);
                metrics.addAll(newSummaries);
                // Set the metrics in the session
                storeMetricsInSession(request, metrics, ivf);
                break;
            default:
                throw new IllegalArgumentException(mode + " not valid here");
            }
        }

        // persist the metrics
        storeMetricsInUserPreferences(request, ivf);

        // trigger an immediate refresh 
        // return mapping.findForward(RetCodeConstants.SUCCESS_URL);
        return refresh(mapping, form, request, response);
    }

    public ActionForward remove(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;

        // Look up the metrics from the session
        List<MetricDisplaySummary> metrics = this.retrieveMetricsFromSession(request, ivf);

        String oldMetric = ivf.getMetric()[0];

        // Go through and remove the metric
        for (Iterator<MetricDisplaySummary> it = metrics.iterator(); it.hasNext();) {
            MetricDisplaySummary summary = it.next();
            if (summary.getMetricToken().equals(oldMetric)) {
                it.remove();
                break;
            }
        }

        // Now store the metrics back
        storeMetricsInSession(request, metrics, ivf);
        storeMetricsInUserPreferences(request, ivf);

        return mapping.findForward(RetCodeConstants.AJAX_URL);
    }

    public ActionForward moveUp(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;

        // Look up the metrics from the session
        List<MetricDisplaySummary> metrics = this.retrieveMetricsFromSession(request, ivf);

        String oldMetric = ivf.getMetric()[0];

        // Go through and reorder the metric
        MetricDisplaySummary[] orderedMetrics = new MetricDisplaySummary[metrics.size()];

        Iterator<MetricDisplaySummary> it = metrics.iterator();
        for (int i = 0; it.hasNext(); i++) {
            MetricDisplaySummary summary = it.next();
            if (summary.getMetricToken().equals(oldMetric)) {
                orderedMetrics[i] = orderedMetrics[i - 1];
                orderedMetrics[i - 1] = summary;
            } else {
                orderedMetrics[i] = summary;
            }
        }

        metrics = new ArrayList<MetricDisplaySummary>(Arrays.asList(orderedMetrics));

        // Now store the metrics back
        storeMetricsInSession(request, metrics, ivf);
        storeMetricsInUserPreferences(request, ivf);

        return mapping.findForward(RetCodeConstants.AJAX_URL);
    }

    public ActionForward moveDown(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;

        // Look up the metrics from the session
        List<MetricDisplaySummary> metrics = this.retrieveMetricsFromSession(request, ivf);

        String oldMetric = ivf.getMetric()[0];

        // Go through and reorder the metric
        MetricDisplaySummary[] orderedMetrics = new MetricDisplaySummary[metrics.size()];

        Iterator<MetricDisplaySummary> it = metrics.iterator();
        for (int i = 0; it.hasNext(); i++) {
            MetricDisplaySummary summary = it.next();
            if (summary.getMetricToken().equals(oldMetric) && it.hasNext())
                orderedMetrics[i++] = it.next();

            orderedMetrics[i] = summary;
        }

        metrics = new ArrayList<MetricDisplaySummary>(Arrays.asList(orderedMetrics));

        // Now store the metrics back
        storeMetricsInSession(request, metrics, ivf);
        storeMetricsInUserPreferences(request, ivf);

        return mapping.findForward(RetCodeConstants.AJAX_URL);
    }

    public ActionForward go(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        return mapping.findForward(KeyConstants.MODE_MON_CUR);
    }

    //this used to be in StringUtil but was only used here.
    //we should probably handle all user input cases the same,
    //escapeHTML if needed before save, unescapeHTML after retrieving.
    /**
     * Find characters having special meaning <em>inside</em> HTML tags and URLs.
     * <p/>
     * <p/>
     * The special characters are : <ul> <li>< <li>> <li>" <li>' <li>\ <li>& <li>| <li>? </ul>
     * <p/>
     * <p/>
     */
    private static int indexOfSpecialChars(String aTagFragment) {
        final StringCharacterIterator iterator = new StringCharacterIterator(aTagFragment);

        int i = 0;
        for (char character = iterator.current(); character != CharacterIterator.DONE; character = iterator.next(), i++) {
            switch (character) {
            case '<':
            case '>':
            case '\"':
            case '\'':
            case '\\':
            case '&':
            case '|':
            case '?':
                return i;
            default:
                break;
            }
        }
        return -1;
    }

    /**
     * Creates a new view with the passed metrics and the passed name.
     * If a view with the new name exist, an error is reported.
     * @param mapping
     * @param form
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    public ActionForward create(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;
        WebUser user = SessionUtils.getWebUser(request.getSession());

        // A couple of checks
        if (ivf.getView().length() == 0) {
            return error(mapping, request, "resource.common.monitor.visibility.view.error.empty");
        }

        if (indexOfSpecialChars(ivf.getView()) > -1) {
            return error(mapping, request, "error.input.badchars");
        }
        String key = KeyConstants.INDICATOR_VIEWS + generateSessionKey(ivf, true);

        String views = "";
        try {
            views = user.getPreference(key);

            if (views.length() > 0) {
                // Make sure that we're not duplicating names
                List<String> viewNames = StringUtil.explode(views, PREF_DELIMITER);
                for (Iterator<String> it = viewNames.iterator(); it.hasNext();) {
                    if (ivf.getView().equals(it.next())) {
                        return error(mapping, request, "resource.common.monitor.visibility.view.error.exists");
                    }
                }

                views += PREF_DELIMITER;
            }
        } catch (IllegalArgumentException e) {
            // If this is the first new one, then let's create a default one,
            // too
            MessageResources res = getResources(request);
            String defName = res.getMessage(KeyConstants.DEFAULT_INDICATOR_VIEW);

            if (!defName.equals(ivf.getView()))
                views = defName + PREF_DELIMITER;
        }

        views += ivf.getView();
        user.setPreference(key, views);
        ivf.setViews(views.split(PREF_DELIMITER_SPLIT));

        // Call update to save the metrics to be viewed
        return update(mapping, ivf, request, response);
    }

    public ActionForward update(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;

        refresh(mapping, form, request, response);

        storeMetricsInUserPreferences(request, ivf);

        return mapping.findForward(KeyConstants.MODE_MON_CUR);
    }

    /**
     * Stores the metrics in the user preferences so that they
     * survive a logout.
     */
    private void storeMetricsInUserPreferences(HttpServletRequest request, IndicatorViewsForm ivf)
        throws SessionNotFoundException, SessionTimeoutException, ServletException {
        // Now fetch the charts from the session
        List<MetricDisplaySummary> metrics = retrieveMetricsFromSession(request, ivf);

        StringBuffer viewMetrics = new StringBuffer();
        for (MetricDisplaySummary mds : metrics) {
            viewMetrics.append(generateMetricToken(mds)).append(PREF_DELIMITER);
        }

        // Set the user preferences now
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        String sKey = generateSessionKey(ivf, false);
        if (EWWW.equals(sKey))
            sKey = (String) session.getAttribute("metricKey");
        String key = KeyConstants.INDICATOR_VIEWS + sKey;
        user.setPreference(generatePrefsMetricsKey(key, ivf.getView()), viewMetrics.toString());

        user.persistPreferences();
    }

    public ActionForward delete(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        IndicatorViewsForm ivf = (IndicatorViewsForm) form;
        WebUser user = SessionUtils.getWebUser(request.getSession());

        String key = KeyConstants.INDICATOR_VIEWS + generateSessionKey(ivf, true);

        String views;
        try {
            views = user.getPreference(key);
        } catch (IllegalArgumentException e) {
            // See, this is the "default"
            return mapping.findForward(KeyConstants.MODE_MON_CUR);
        }

        // Parse the views
        List<String> viewNames = StringUtil.explode(views, PREF_DELIMITER);

        for (Iterator<String> it = viewNames.iterator(); it.hasNext();) {
            String view = it.next();

            if (view.equals(ivf.getUpdate()))
                it.remove();
        }

        if (viewNames.size() > 0) {
            views = StringUtil.listToString(viewNames, PREF_DELIMITER);
            user.setPreference(key, views);
        } else {
            user.unsetPreference(key);
        }

        // Now unset the metrics
        user.unsetPreference(key + generatePrefsMetricsKey(key, ivf.getUpdate()));

        user.persistPreferences();

        return mapping.findForward(KeyConstants.MODE_MON_CUR);
    }

    /**
     * Parse the passed token that identifies single metric or group.
     * The format of the token is (without quotation marks):
     * <ul>
     * <li>For a compatible group: "cg,<i>groupId</i>,<i>definitionId</i>"</li>
     * <li>For an autogroup : "ag,<i>parentId</i>,<i>definitionId</i>,<i>childTypeId</i>"</li>
     * <li>For a single resource: "<i>resourceId</i>,<i>scheduleId</i>"</li>
     * </ul>
     * @param token A token that follows the form mentioned above.
     * @return a new {@link MetricDisplaySummary} where the identifiers for resource/group have been set.
     * @see #generateMetricToken(MetricDisplaySummary)
     */
    private MetricDisplaySummary parseMetricToken(String token) {
        String DELIMITER = ",";
        if (log.isTraceEnabled())
            log.trace("parseMetricToken: input is " + token);

        MetricDisplaySummary ret = new MetricDisplaySummary();

        String[] tokens = token.split(DELIMITER);
        if (tokens == null || tokens.length < 2)
            throw new IllegalArgumentException(token + " is not valid");

        if (tokens[0].equals("cg")) {
            ret.setGroupId(Integer.parseInt(tokens[1]));
            ret.setDefinitionId(Integer.parseInt(tokens[2]));
        } else if (tokens[0].equals("ag")) {
            ret.setParentId(Integer.parseInt(tokens[1]));
            ret.setDefinitionId(Integer.parseInt(tokens[2]));
            ret.setChildTypeId(Integer.parseInt(tokens[3]));
        } else {
            ret.setResourceId(Integer.parseInt(tokens[0]));
            ret.setScheduleId(Integer.parseInt(tokens[1]));
        }
        ret.setMetricToken(token);
        return ret;
    }

    /**
     * Determine if the passed summary represents single resource, a compatible
     * group or an autogroup. 
     * @param summary a {@link MetricDisplaySummary} to check
     * @return the appropriate {@link MetricsDisplayMode} or UNSET if undeterminable.
     */
    private MetricsDisplayMode getDisplayModeForSummary(MetricDisplaySummary summary) {
        if (summary.getResourceId() > 0)
            return MetricsDisplayMode.RESOURCE;
        else if (summary.getGroupId() > 0)
            return MetricsDisplayMode.COMPGROUP;
        else if (summary.getParentId() > 0 && summary.getChildTypeId() > 0)
            return MetricsDisplayMode.AUTOGROUP;
        else {
            log.debug("Mode could not be determined for " + summary);
            return MetricsDisplayMode.UNSET;
        }
    }
}
