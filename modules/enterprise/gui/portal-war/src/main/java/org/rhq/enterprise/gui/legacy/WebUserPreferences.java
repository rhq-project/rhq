package org.rhq.enterprise.gui.legacy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.composite.ResourceIdFlyWeight;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.legacy.util.DashboardUtils;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.gui.uibeans.UIConstants;
import org.rhq.enterprise.server.alert.engine.internal.Tuple;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class WebUserPreferences {

    private final Log log = LogFactory.getLog(WebUserPreferences.class);

    public static final String PREF_FAV_RESOURCE_METRICS_PREFIX = ".resource.common.monitor.visibility.favoriteMetrics";
    public static final String PREF_METRIC_RANGE = ".resource.common.monitor.visibility.metricRange";
    public static final String PREF_METRIC_RANGE_LASTN = ".resource.common.monitor.visibility.metricRange.lastN";
    public static final String PREF_METRIC_RANGE_UNIT = ".resource.common.monitor.visibility.metricRange.unit";
    public static final String PREF_METRIC_RANGE_RO = ".resource.common.monitor.visibility.metricRange.ro";
    public static final String PREF_METRIC_THRESHOLD = ".resource.common.monitor.visibility.metricThreshold";

    public static final String PREF_DASH_SHOW_SUMMARY_COUNTS_PLATFORM = ".dashContent.summaryCounts.platform";
    public static final String PREF_DASH_SHOW_SUMMARY_COUNTS_SERVER = ".dashContent.summaryCounts.server";
    public static final String PREF_DASH_SHOW_SUMMARY_COUNTS_SERVICE = ".dashContent.summaryCounts.service";
    public static final String PREF_DASH_SHOW_SUMMARY_COUNTS_GROUP_COMPAT = ".dashContent.summaryCounts.group.compat";
    public static final String PREF_DASH_SHOW_SUMMARY_COUNTS_GROUP_MIXED = ".dashContent.summaryCounts.group.mixed";
    public static final String PREF_DASH_SHOW_SUMMARY_COUNTS_GROUP_DEFINITIONS = ".dashContent.summaryCounts.group.definition";

    public static final String PREF_DASH_AUTODISCOVERY_RANGE = ".dashContent.autoDiscovery.range";

    public static final String PREF_DASH_OPERATION_LAST_COMPLETED = ".dashContent.operations.lastCompleted";
    public static final String PREF_DASH_OPERATION_NEXT_SCHEDULED = ".dashContent.operations.nextScheduled";
    public static final String PREF_DASH_OPERATION_USE_LAST_COMPLETED = ".dashContent.operations.useLastCompleted";
    public static final String PREF_DASH_OPERATION_USE_NEXT_SCHEDULED = ".dashContent.operations.useNextScheduled";

    public static final String PREF_DASH_ALERTS_COUNT = ".dashContent.criticalalerts.numberOfAlerts";
    public static final String PREF_DASH_ALERTS_PRIORITY = ".dashContent.criticalalerts.priority";
    public static final String PREF_DASH_ALERTS_PAST = ".dashContent.criticalalerts.past";
    public static final String PREF_DASH_ALERTS_SELECTED_OR_ALL = ".dashContent.criticalalerts.selectedOrAll";
    public static final String PREF_DASH_ALERTS_RESOURCES = ".dashContent.criticalalerts.resources";

    public static final String PREF_DASH_PROBLEM_RESOURCES_ROWS = ".dashContent.problemResources.range";
    public static final String PREF_DASH_PROBLEM_RESOURCES_HOURS = ".dashContent.problemResources.hours";
    public static final String PREF_DASH_PROBLEM_RESOURCES_SHOW_IGNORED = ".dashContent.problemResources.showIgnored";
    public static final String PREF_DASH_PROBLEM_RESOURCES_IGNORED = ".dashContent.problemResources.ignoreList";

    public static final String PREF_DASH_RECENTLY_APPROVED_RANGE = ".dashContent.recentlyApproved.range";
    public static final String PREF_DASH_RECENTLY_APPROVED_EXPANDED_PLATFORMS = ".dashContent.recentlyApproved.expandedPlatforms";

    public static final String PREF_DASH_FAVORITE_RESOURCES_AVAILABILITY = ".dashContent.resourcehealth.availability";
    public static final String PREF_DASH_FAVORITE_RESOURCES_ALERTS = ".dashContent.resourcehealth.alerts";
    public static final String PREF_DASH_FAVORITE_RESOURCES = ".dashContent.resourcehealth.resources";

    /**
     * The key that holds the user's chart queries
     */
    public static final String PREF_DASH_USER_SAVED_CHARTS = ".dashContent.charts";

    /**
     * the keys that hold the user's LHS and RHS Dashboard Portlets
     */
    public static final String PREF_DASH_PORTLETS_FIRST = ".dashcontent.portal.portlets.first";
    public static final String PREF_DASH_PORTLETS_SECOND = ".dashcontent.portal.portlets.second";

    public static final String PREF_RESOURCE_BROWSER_VIEW_MODE = ".resource.browser.view";

    /**
     * key values for indicator views
     */
    public static final String PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX = "monitor.visibility.indicator.views.";
    public static final String PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT = "resource.common.monitor.visibility.defaultview";
    public static final String PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME = "Default";

    public static final String PREF_PAGE_REFRESH_PERIOD = ".page.refresh.period";

    /** delimiter for preferences that are multi-valued and stringified */
    private static final String PREF_LIST_DELIM = ",";

    private Subject subject;

    public WebUserPreferences(Subject subject) {
        this.subject = subject;
    }

    /**
     * This forces a flush of the user preferences to the database.
     */
    public void persistPreferences() {
        Integer sessionId = this.subject.getSessionId(); // let's remember our transient session ID
        this.subject = LookupUtil.getSubjectManager().updateSubject(this.subject, this.subject);
        this.subject.setSessionId(sessionId); // put the transient data back into our new subject
    }

    private String getPreference(String key) throws IllegalArgumentException {
        Configuration userConfiguration = this.subject.getUserConfiguration();
        PropertySimple prop = null;

        if (userConfiguration != null)
            prop = userConfiguration.getSimple(key);

        if (prop == null)
            throw new IllegalArgumentException("preference '" + key + "' requested is not valid");

        String value = prop.getStringValue();

        // null values are often the default for many props; let the caller determine whether this is an error
        if (value != null) {
            value = value.trim();
        }
        log.debug("Getting " + key + "[" + value + "]");

        return value;
    }

    @SuppressWarnings( { "unchecked" })
    private <T> T getPreference(String key, T defaultValue) {
        T result;
        try {
            String preferenceValue = getPreference(key);

            Class<T> type = (Class<T>) defaultValue.getClass();
            if (type == String.class) {
                result = (T) preferenceValue; // cast string to self-type
            } else {
                if (type == Boolean.class) {
                    if (preferenceValue.equalsIgnoreCase("on") || preferenceValue.equalsIgnoreCase("yes")) {
                        preferenceValue = "true"; // flexible support for boolean translations from forms
                    }
                }

                try {
                    Method m = type.getMethod("valueOf", String.class);
                    result = (T) m.invoke(null, preferenceValue); // static method
                } catch (Exception e) {
                    throw new IllegalArgumentException("No support for automatic conversion of preferences of type "
                        + type);
                }
            }
        } catch (IllegalArgumentException iae) {
            result = defaultValue;
        }
        return result;
    }

    /**
     * Break the named preference into tokens delimited by <code>PREF_LIST_DELIM</code>.
     *
     * @param key the name of the preference
     * @return <code>List</code> of <code>String</code> tokens
     */
    private List<String> getPreferenceAsList(String key) {
        return getPreferenceAsList(key, PREF_LIST_DELIM);
    }

    /**
     * Tokenize the named preference into a List of Strings. If no such preference exists, or the preference is null,
     * an empty List will be returned.
     *
     * @param delimiter the delimiter to break it up by
     * @param key the name of the preference
     * @return <code>List</code> of <code>String</code> tokens
     */
    @NotNull
    public List<String> getPreferenceAsList(String key, String delimiter) {
        String pref = null;
        try {
            pref = getPreference(key);
        } catch (IllegalArgumentException e) {
            log.debug("A user preference named '" + key + "' does not exist.");
        }
        return (pref != null) ? StringUtil.explode(pref, delimiter) : new ArrayList<String>();
    }

    private List<Integer> getPreferenceAsIntegerList(String key, String delimiter) {
        try {
            List<String> value = getPreferenceAsList(key, delimiter);

            List<Integer> result = new ArrayList<Integer>(value.size());
            for (int i = 0; i < value.size(); i++) {
                String trimmed = value.get(i).trim();
                if (trimmed.length() > 0) {
                    result.add(Integer.valueOf(trimmed));
                }
            }

            return result;
        } catch (Exception e) {
            return new ArrayList<Integer>();
        }
    }

    private void setPreference(String key, List<?> values) throws IllegalArgumentException {
        setPreference(key, values, PREF_LIST_DELIM);
    }

    private void setPreference(String key, List<?> values, String delim) throws IllegalArgumentException {
        String stringified = StringUtil.listToString(values, delim);
        setPreference(key, stringified);
    }

    public void setPreference(String key, Object value) throws IllegalArgumentException {
        String val = null;
        if (value == null) {
            val = "";
        } else if (value instanceof String) {
            val = (String) value;
        } else {
            val = value.toString();
        }

        PropertySimple existingProp = this.subject.getUserConfiguration().getSimple(key);
        if (existingProp == null) {
            log.debug("Setting " + key + "[" + value + "]");
            this.subject.getUserConfiguration().put(new PropertySimple(key, val));
        } else {
            log.debug("Overriding " + key + "[" + value + "]");
            existingProp.setStringValue(val);
        }
    }

    private void unsetPreference(String key) {
        Configuration config = subject.getUserConfiguration();
        if (config != null) {
            config.remove(key);
        }
    }

    public static class MetricRangePreferences {
        /* 
         * if readOnly is set to true, the beginning and ending range
         * dates are specified with explicit dates.  if readOnly is
         * false, then the time is relative to NOW and is specified as
         * <lastN> units of <unit> time
         */
        public boolean readOnly;

        // simple, when readOnly is false
        public int lastN;
        public int unit;

        // advanced, when readOnly is true
        public Long begin;
        public Long end;
    }

    public MetricRangePreferences getMetricRangePreferences(boolean defaultRange) {
        MetricRangePreferences prefs = new MetricRangePreferences();

        prefs.readOnly = getPreference(PREF_METRIC_RANGE_RO, MonitorUtils.DEFAULT_VALUE_RANGE_RO);
        prefs.lastN = getPreference(PREF_METRIC_RANGE_LASTN, MonitorUtils.DEFAULT_VALUE_RANGE_LASTN);
        prefs.unit = getPreference(PREF_METRIC_RANGE_UNIT, MonitorUtils.DEFAULT_VALUE_RANGE_UNIT);

        List<?> range = null;
        try {
            range = getPreferenceAsList(PREF_METRIC_RANGE);
        } catch (IllegalArgumentException iae) {
            // that's OK, range will remain null and we might use the lastN / unit
        }

        if (range != null && range.size() > 0) {
            try {
                prefs.begin = new Long((String) range.get(0));
                prefs.end = new Long((String) range.get(1));
            } catch (NumberFormatException nfe) {
                // also OK, errors still might default to lastN / unit
            }
        }

        /* 
         * sometimes we are satisfied with no range. other times we
         * need to calculate the "last n" units range and return that
         */
        if (defaultRange && prefs.begin == null && prefs.end == null) {
            range = MonitorUtils.calculateTimeFrame(prefs.lastN, prefs.unit);

            prefs.begin = (Long) range.get(0);
            prefs.end = (Long) range.get(1);
        }

        return prefs;
    }

    public MetricRangePreferences getMetricRangePreferences() {
        return getMetricRangePreferences(true);
    }

    public void setMetricRangePreferences(MetricRangePreferences prefs) {
        setPreference(PREF_METRIC_RANGE_RO, prefs.readOnly);
        if (prefs.readOnly) {
            // persist advanced mode
            setPreference(PREF_METRIC_RANGE, Arrays.asList(prefs.begin, prefs.end));
        } else {
            setPreference(PREF_METRIC_RANGE_LASTN, prefs.lastN);
            setPreference(PREF_METRIC_RANGE_UNIT, prefs.unit);
        }
    }

    /**
     * Returns a list of metric ids saved as favorites for a particular appdef type
     */
    public Integer getMetricThresholdPreference() throws IllegalArgumentException {
        return new Integer(getPreference(PREF_METRIC_THRESHOLD));
    }

    /**
     * Set a list of metric ids saved as favorites for a particular appdef type
     */
    public void setMetricThresholdPreference(Integer value) throws IllegalArgumentException {
        setPreference(PREF_METRIC_THRESHOLD, value);
    }

    /**
     * Get the value of a preference as a boolean.
     * @param key the preference to get
     * @param ifNull if the pref is undefined, return this value instead
     * @return the boolean value of 'key', or if key is null, returns the
     * 'ifNull' value.
     */
    private boolean getBooleanPref(String key) {
        String val = getPreference(key);
        return Boolean.valueOf(val).booleanValue();
    }

    /**
     * Get the value of a preference as a boolean.
     * @param key the preference to get
     * @param ifNull if the pref is undefined, return this value instead
     * @return the boolean value of 'key', or if key is null, returns the
     * 'ifNull' value.
     */
    private boolean getBooleanPref(String key, boolean ifNull) {
        String val;
        try {
            val = getPreference(key);
        } catch (IllegalArgumentException e) {
            return ifNull;
        }
        return Boolean.valueOf(val).booleanValue();
    }

    /**
     * Get the value of a preference as an int.
     * @param key the preference to get
     * @return the int value of 'key'
     */
    private int getIntPref(String key) {
        String val = getPreference(key);
        return Integer.parseInt(val);
    }

    /**
     * Get the value of a preference as an int.
     * @param key the preference to get
     * @param ifNull if the pref is null, return this value instead
     * @return the int value of 'key', or if key is null, returns the
     * 'ifNull' value.
     */
    private int getIntPref(String key, int ifNull) {
        String val;
        try {
            val = getPreference(key);
            if ("".equals(val)) {
                return ifNull;
            }
        } catch (IllegalArgumentException e) {
            return ifNull;
        }
        return Integer.parseInt(val);
    }

    /**
     * Get the value of a preference as an long.
     * @param key the preference to get
     * @return the long value of 'key'
     */
    private Long getLongPref(String key) {
        String val = getPreference(key);
        return Long.parseLong(val);
    }

    public int getPageRefreshPeriod() {
        return getIntPref(PREF_PAGE_REFRESH_PERIOD, UIConstants.DONT_REFRESH_PAGE);
    }

    public void setPageRefreshPeriod(int period) {
        setPreference(PREF_PAGE_REFRESH_PERIOD, Integer.valueOf(period));
    }

    public static class SummaryCountPortletPreferences {
        public boolean showPlatforms;
        public boolean showServers;
        public boolean showServices;
        public boolean showCompatibleGroups;
        public boolean showMixedGroups;
        public boolean showGroupDefinitions;
    }

    public SummaryCountPortletPreferences getSummaryCounts() {
        SummaryCountPortletPreferences counts = new SummaryCountPortletPreferences();
        counts.showPlatforms = getBooleanPref(PREF_DASH_SHOW_SUMMARY_COUNTS_PLATFORM, true);
        counts.showServers = getBooleanPref(PREF_DASH_SHOW_SUMMARY_COUNTS_SERVER, true);
        counts.showServices = getBooleanPref(PREF_DASH_SHOW_SUMMARY_COUNTS_SERVICE, true);
        counts.showCompatibleGroups = getBooleanPref(PREF_DASH_SHOW_SUMMARY_COUNTS_GROUP_COMPAT, true);
        counts.showMixedGroups = getBooleanPref(PREF_DASH_SHOW_SUMMARY_COUNTS_GROUP_MIXED, true);
        counts.showGroupDefinitions = getBooleanPref(PREF_DASH_SHOW_SUMMARY_COUNTS_GROUP_DEFINITIONS, true);
        return counts;
    }

    public void setSummaryCounts(SummaryCountPortletPreferences counts) {
        setPreference(PREF_DASH_SHOW_SUMMARY_COUNTS_PLATFORM, counts.showPlatforms);
        setPreference(PREF_DASH_SHOW_SUMMARY_COUNTS_SERVER, counts.showServers);
        setPreference(PREF_DASH_SHOW_SUMMARY_COUNTS_SERVICE, counts.showServices);
        setPreference(PREF_DASH_SHOW_SUMMARY_COUNTS_GROUP_COMPAT, counts.showCompatibleGroups);
        setPreference(PREF_DASH_SHOW_SUMMARY_COUNTS_GROUP_MIXED, counts.showMixedGroups);
        setPreference(PREF_DASH_SHOW_SUMMARY_COUNTS_GROUP_DEFINITIONS, counts.showGroupDefinitions);
    }

    public int getAutoDiscoveryRange() {
        return getIntPref(PREF_DASH_AUTODISCOVERY_RANGE);
    }

    public void setAutoDiscoveryRange(int range) {
        setPreference(PREF_DASH_AUTODISCOVERY_RANGE, range);
    }

    public static class OperationPortletPreferences {
        public boolean useLastCompleted;
        public boolean useNextScheduled;
        public int lastCompleted;
        public int nextScheduled;
    }

    public OperationPortletPreferences getOperationPortletPreferences() {
        OperationPortletPreferences prefs = new OperationPortletPreferences();
        prefs.lastCompleted = getIntPref(PREF_DASH_OPERATION_LAST_COMPLETED);
        prefs.nextScheduled = getIntPref(PREF_DASH_OPERATION_NEXT_SCHEDULED);
        prefs.useLastCompleted = getBooleanPref(PREF_DASH_OPERATION_USE_LAST_COMPLETED);
        prefs.useNextScheduled = getBooleanPref(PREF_DASH_OPERATION_USE_NEXT_SCHEDULED);
        return prefs;
    }

    public void setOperationPortletPreferences(OperationPortletPreferences prefs) {
        setPreference(PREF_DASH_OPERATION_LAST_COMPLETED, prefs.lastCompleted);
        setPreference(PREF_DASH_OPERATION_NEXT_SCHEDULED, prefs.nextScheduled);
        setPreference(PREF_DASH_OPERATION_USE_LAST_COMPLETED, prefs.useLastCompleted);
        setPreference(PREF_DASH_OPERATION_USE_NEXT_SCHEDULED, prefs.useNextScheduled);
    }

    public static class AlertsPortletPreferences {
        public int count;
        public int priority;
        public long timeRange;
        public String displayAll;
        public List<Integer> resourceIds;

        public Integer[] asArray() {
            return resourceIds.toArray(new Integer[resourceIds.size()]);
        }
    }

    public AlertsPortletPreferences getAlertsPortletPreferences() {
        AlertsPortletPreferences prefs = new AlertsPortletPreferences();
        prefs.count = getIntPref(PREF_DASH_ALERTS_COUNT);
        prefs.priority = getIntPref(PREF_DASH_ALERTS_PRIORITY);
        prefs.timeRange = getLongPref(PREF_DASH_ALERTS_PAST);
        prefs.displayAll = getPreference(PREF_DASH_ALERTS_SELECTED_OR_ALL);
        prefs.resourceIds = getPreferenceAsIntegerList(PREF_DASH_ALERTS_RESOURCES, DashboardUtils.DASHBOARD_DELIMITER);
        removeDeletedResources(prefs.resourceIds);
        return prefs;
    }

    public void setAlertsPortletPreferences(AlertsPortletPreferences prefs) {
        setPreference(PREF_DASH_ALERTS_COUNT, prefs.count);
        setPreference(PREF_DASH_ALERTS_PRIORITY, prefs.priority);
        setPreference(PREF_DASH_ALERTS_PAST, prefs.timeRange);
        setPreference(PREF_DASH_ALERTS_SELECTED_OR_ALL, prefs.displayAll);
        /* 
         * setting resources happens in the AddResourcesAction class since
         * all portlets that can be filtered by resources use the same logic 
         */
    }

    public static class ProblemResourcesPortletPreferences {
        public int range;
        public int hours;
        public boolean showIgnored;
        public String ignoreList;
    }

    public ProblemResourcesPortletPreferences getProblemResourcesPortletPreferences() {
        ProblemResourcesPortletPreferences prefs = new ProblemResourcesPortletPreferences();
        prefs.range = getIntPref(PREF_DASH_PROBLEM_RESOURCES_ROWS);
        prefs.hours = getIntPref(PREF_DASH_PROBLEM_RESOURCES_HOURS);
        prefs.showIgnored = getBooleanPref(PREF_DASH_PROBLEM_RESOURCES_SHOW_IGNORED);
        prefs.ignoreList = getPreference(PREF_DASH_PROBLEM_RESOURCES_IGNORED);
        return prefs;
    }

    public void setProblemResourcesPortletPreferences(ProblemResourcesPortletPreferences prefs) {
        setPreference(PREF_DASH_PROBLEM_RESOURCES_ROWS, prefs.range);
        setPreference(PREF_DASH_PROBLEM_RESOURCES_HOURS, prefs.hours);
        setPreference(PREF_DASH_PROBLEM_RESOURCES_SHOW_IGNORED, prefs.showIgnored);
        setPreference(PREF_DASH_PROBLEM_RESOURCES_IGNORED, prefs.ignoreList);
    }

    public static class RecentlyApprovedPortletPreferences {
        public int range;
        public List<String> expandedPlatforms;
    }

    public RecentlyApprovedPortletPreferences getRecentlyApprovedPortletPreferences() {
        RecentlyApprovedPortletPreferences prefs = new RecentlyApprovedPortletPreferences();
        prefs.range = getIntPref(PREF_DASH_RECENTLY_APPROVED_RANGE);
        prefs.expandedPlatforms = getPreferenceAsList(PREF_DASH_RECENTLY_APPROVED_EXPANDED_PLATFORMS);
        return prefs;
    }

    public void setRecentlyApprovedPortletPreferences(RecentlyApprovedPortletPreferences prefs) {
        setPreference(PREF_DASH_RECENTLY_APPROVED_RANGE, prefs.range);
        setPreference(PREF_DASH_RECENTLY_APPROVED_EXPANDED_PLATFORMS, prefs.expandedPlatforms);
    }

    public static class FavoriteResourcePortletPreferences {
        public boolean showAvailability;
        public boolean showAlerts;
        public List<Integer> resourceIds;

        public Integer[] asArray() {
            return resourceIds.toArray(new Integer[resourceIds.size()]);
        }
    }

    public FavoriteResourcePortletPreferences getFavoriteResourcePortletPreferences() {
        FavoriteResourcePortletPreferences prefs = new FavoriteResourcePortletPreferences();
        prefs.showAvailability = getBooleanPref(PREF_DASH_FAVORITE_RESOURCES_AVAILABILITY);
        prefs.showAlerts = getBooleanPref(PREF_DASH_FAVORITE_RESOURCES_ALERTS);
        prefs.resourceIds = getPreferenceAsIntegerList(PREF_DASH_FAVORITE_RESOURCES, DashboardUtils.DASHBOARD_DELIMITER);
        removeDeletedResources(prefs.resourceIds);
        return prefs;
    }

    public void setFavoriteResourcePortletPreferences(FavoriteResourcePortletPreferences prefs) {
        setPreference(PREF_DASH_FAVORITE_RESOURCES_AVAILABILITY, prefs.showAvailability);
        setPreference(PREF_DASH_FAVORITE_RESOURCES_ALERTS, prefs.showAlerts);
    }

    public static class DashboardPreferences {
        public String leftColumnPortletNames;
        public String rightColumnPortletNames;

        public void addPortlet(String portletName, boolean onLeft) {
            if (onLeft) {
                leftColumnPortletNames += DashboardUtils.DASHBOARD_DELIMITER + portletName;
            } else {
                rightColumnPortletNames += DashboardUtils.DASHBOARD_DELIMITER + portletName;
            }
        }

        public void removePortlet(String portletName) {
            leftColumnPortletNames = remove(leftColumnPortletNames, portletName);
            rightColumnPortletNames = remove(rightColumnPortletNames, portletName);
        }

        private String remove(String columnValues, String portletName) {
            String[] portlets = StringUtil.explodeToArray(columnValues, DashboardUtils.DASHBOARD_DELIMITER);
            StringBuilder results = new StringBuilder();
            for (int i = 0; i < portlets.length; i++) {
                String portlet = portlets[i];
                if (!portlet.equals(portletName)) {
                    if (results.length() != 0) {
                        results.append(DashboardUtils.DASHBOARD_DELIMITER);
                    }
                    results.append(portlet);

                }
            }
            return results.toString();
        }

        public void moveUp(String portletName) {
            leftColumnPortletNames = moveUp(leftColumnPortletNames, portletName);
            rightColumnPortletNames = moveUp(rightColumnPortletNames, portletName);
        }

        private String moveUp(String columnValues, String portletName) {
            String[] portlets = StringUtil.explodeToArray(columnValues, DashboardUtils.DASHBOARD_DELIMITER);
            for (int i = 0; i < portlets.length; i++) {
                String portlet = portlets[i];
                if (portlet.equals(portletName)) {
                    if (i != 0) {
                        // swap current position with previous position
                        String temp = portlets[i - 1];
                        portlets[i - 1] = portlet;
                        portlets[i] = temp;
                    }
                    break; // no more work to do
                }
            }
            return stringify(portlets);
        }

        public void moveDown(String portletName) {
            leftColumnPortletNames = moveDown(leftColumnPortletNames, portletName);
            rightColumnPortletNames = moveDown(rightColumnPortletNames, portletName);
        }

        private String moveDown(String columnValues, String portletName) {
            String[] portlets = StringUtil.explodeToArray(columnValues, DashboardUtils.DASHBOARD_DELIMITER);
            for (int i = portlets.length - 1; i >= 0; i--) {
                String portlet = portlets[i];
                if (portlet.equals(portletName)) {
                    if (i != portlets.length - 1) {
                        // swap current position with previous position
                        String temp = portlets[i + 1];
                        portlets[i + 1] = portlet;
                        portlets[i] = temp;
                    }
                    break; // no more work to do
                }
            }
            return stringify(portlets);
        }

        public String stringify(String[] portlets) {
            StringBuilder results = new StringBuilder();
            for (int i = 0; i < portlets.length; i++) {
                if (i != 0) {
                    results.append(DashboardUtils.DASHBOARD_DELIMITER);
                }
                results.append(portlets[i]);
            }
            return results.toString();
        }

    }

    public DashboardPreferences getDashboardPreferences() {
        DashboardPreferences prefs = new DashboardPreferences();
        prefs.leftColumnPortletNames = getPreference(PREF_DASH_PORTLETS_FIRST);
        prefs.rightColumnPortletNames = getPreference(PREF_DASH_PORTLETS_SECOND);
        return prefs;
    }

    public void setDashboardPreferences(DashboardPreferences prefs) {
        setPreference(PREF_DASH_PORTLETS_FIRST, prefs.leftColumnPortletNames);
        setPreference(PREF_DASH_PORTLETS_SECOND, prefs.rightColumnPortletNames);
    }

    public String getResourceBrowserViewMode() {
        return getPreference(PREF_RESOURCE_BROWSER_VIEW_MODE);
    }

    public void setResourceBrowserViewMode(String mode) {
        setPreference(PREF_RESOURCE_BROWSER_VIEW_MODE, mode);
    }

    public static class MetricViewsPreferences {
        public List<String> views;
    }

    public MetricViewsPreferences getMetricViews(String key) {
        MetricViewsPreferences prefs = new MetricViewsPreferences();
        //TODO: jmarques - externalize default view name
        // instead of PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME
        // lookup PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT from the bundle
        String value = getPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + key,
            PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME);
        prefs.views = StringUtil.explode(value, DashboardUtils.DASHBOARD_DELIMITER);
        return prefs;
    }

    public void setMetricViews(MetricViewsPreferences prefs, String key) {
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (String viewName : prefs.views) {
            if (index != 0) {
                builder.append(DashboardUtils.DASHBOARD_DELIMITER);
            }
            builder.append(viewName);
            index++;
        }
        setPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + key, builder.toString());
    }

    public static class MetricViewData {
        public List<String> charts;
    }

    public MetricViewData getMetricViewData(String context, String viewName) {
        //TODO: jmarques - externalize default view name
        // instead of PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME
        // lookup PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT from the bundle
        if (viewName == null || "".equals(viewName)) {
            viewName = "default";
        }
        MetricViewData chartPreferences = new MetricViewData();
        String data = getPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + context + "." + viewName);
        chartPreferences.charts = StringUtil.explode(data, DashboardUtils.DASHBOARD_DELIMITER);
        return chartPreferences;
    }

    public void setMetricViewData(String context, String viewName, MetricViewData prefs) {
        //TODO: jmarques - externalize default view name
        // instead of PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME
        // lookup PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT from the bundle
        if (viewName == null || "".equals(viewName)) {
            viewName = "default";
        }
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (String chart : prefs.charts) {
            if (index != 0) {
                builder.append(DashboardUtils.DASHBOARD_DELIMITER);
            }
            builder.append(chart);
            index++;
        }
        setPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + context + "." + viewName, builder.toString());
    }

    public static class SavedChartsPortletPreferences {
        public List<Tuple<String, String>> chartList;

        public void removeByTuple(String chart) {
            String[] split = chart.split(",");
            for (int i = 0; i < chartList.size(); i++) {
                Tuple<String, String> nameURL = chartList.get(i);
                if (nameURL.lefty.equals(split[0])) {
                    chartList.remove(i);
                }
            }
        }

        public void removeByURL(String chartURL) {
            for (int i = 0; i < chartList.size(); i++) {
                Tuple<String, String> nameURL = chartList.get(i);
                if (nameURL.righty.equals(chartURL)) {
                    chartList.remove(i);
                }
            }
        }

        public boolean add(String name, String url) {
            if (name == null) {
                return false;
            }

            name = StringUtil.replace(name, "|", "&#124;");
            url = StringUtil.replace(url, ",", "&#44;");

            String proposedName = name;

            // make sure its not a duplicate chart
            int i = 2;
            for (Tuple<String, String> chart : chartList) {
                // don't add duplicate charts
                if (chart.righty.equals(url)) {
                    return false;
                }

                // unique chart, but name collision
                if (chart.lefty.equals(proposedName)) {
                    proposedName = name + " (" + i + ")";
                    i++;
                }
            }

            chartList.add(new Tuple<String, String>(proposedName, url));

            return true;
        }
    }

    // PREF_DASH_USER_SAVED_CHARTS

    public SavedChartsPortletPreferences getSavedChartsPortletPreferences() {
        SavedChartsPortletPreferences prefs = new SavedChartsPortletPreferences();
        prefs.chartList = new ArrayList<Tuple<String, String>>();

        int counter = 0;
        String chart = null;
        boolean errorOccurred = false;
        do {
            chart = getPreference(PREF_DASH_USER_SAVED_CHARTS + "." + counter, null);
            if (chart != null && !chart.equals("")) {
                String[] nameURL = chart.split(",");
                if (nameURL.length != 2) {
                    log.error("Could not read saved chart, marked for removal: '" + chart + "'");
                    errorOccurred = true;
                } else {
                    nameURL[0] = StringUtil.replace(nameURL[0], "&#124;", "|");
                    nameURL[1] = StringUtil.replace(nameURL[1], "&#44;", ",");
                    prefs.chartList.add(counter, new Tuple<String, String>(nameURL[0], nameURL[1]));
                }
            }

            counter++;
        } while (chart != null && !chart.equals(""));

        if (errorOccurred) {
            // re-persist the new list so we don't read the error again 
            setSavedChartsPortletPreferences(prefs);
            persistPreferences();
        }

        return prefs;
    }

    public void setSavedChartsPortletPreferences(SavedChartsPortletPreferences prefs) {
        // since we don't know the previous count, first unset all of them
        int counter = 0;
        String prevChart = null;
        do {
            prevChart = getPreference(PREF_DASH_USER_SAVED_CHARTS + "." + counter, null);
            if (prevChart != null && !prevChart.equals("")) {
                unsetPreference(PREF_DASH_USER_SAVED_CHARTS + "." + counter);
            }
            counter++;
        } while (prevChart != null && !prevChart.equals(""));

        counter = 0;
        for (Tuple<String, String> nameURL : prefs.chartList) {
            nameURL.lefty = StringUtil.replace(nameURL.lefty, "|", "&#124;");
            nameURL.righty = StringUtil.replace(nameURL.righty, ",", "&#44;");
            setPreference(PREF_DASH_USER_SAVED_CHARTS + "." + counter, nameURL.lefty + "," + nameURL.righty);
            counter++;
        }
    }

    private void removeDeletedResources(List<Integer> resourceIds) {
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        List<ResourceIdFlyWeight> flyWeights = resourceManager.getFlyWeights(resourceIds
            .toArray(new Integer[resourceIds.size()]));
        boolean removed = false;
        for (int i = resourceIds.size() - 1; i >= 0; i--) {
            Integer resourceIdToVerify = resourceIds.get(i);
            boolean match = false;
            for (int j = 0; j < flyWeights.size(); j++) {
                ResourceIdFlyWeight resourceFlyToCompare = flyWeights.get(j);
                if (resourceIdToVerify == resourceFlyToCompare.getId()) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                resourceIds.remove(i);
                removed = true;
            }
        }
        if (removed) {
            persistPreferences();
        }
    }

    public PageControl getPageControl(PageControlView view) {
        if (view == PageControlView.NONE) {
            return PageControl.getUnlimitedInstance();
        }

        List<String> pageControlProperties = getPreferenceAsList(view.toString());
        if (pageControlProperties.size() == 0) {
            PageControl defaultControl = null;
            if (view.getShowAll()) {
                defaultControl = PageControl.getUnlimitedInstance();
            } else {
                defaultControl = new PageControl(0, 15);
            }
            setPageControl(view, defaultControl);
            return defaultControl;
        } else {
            int pageSize = Integer.valueOf(pageControlProperties.get(0));
            PageControl pageControl = new PageControl(0, pageSize);

            int i = 2;
            while (i < pageControlProperties.size()) {
                String pageOrdering = pageControlProperties.get(i - 1);
                String sortColumn = pageControlProperties.get(i);

                pageControl.addDefaultOrderingField(sortColumn, PageOrdering.valueOf(pageOrdering));

                i += 2;
            }
            return pageControl;
        }
    }

    @SuppressWarnings("unchecked")
    public void setPageControl(PageControlView view, PageControl pageControl) {
        if (view == PageControlView.NONE) {
            return; // nothing is stored in session for the special NONE view
        }

        List pageControlProperties = new ArrayList();
        pageControlProperties.add(pageControl.getPageSize());

        for (OrderingField field : pageControl.getOrderingFieldsAsArray()) {
            pageControlProperties.add(field.getOrdering().toString());
            pageControlProperties.add(field.getField());
        }

        setPreference(view.toString(), pageControlProperties);
    }
}