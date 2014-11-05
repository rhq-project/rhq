package org.rhq.coregui.client.util.preferences;

/**
 * Names of {@link org.rhq.core.domain.auth.Subject#getUserConfiguration() user preference properties}. The default
 * values for these preferences are stored in portal-war/WEB-INF/DefaultUserPreferences.properties and are populated for
 * a user the first time that user logs in to portal-war. For convenience sake, the default values are also stated in
 * the Javadoc below. 
 *
 * @author Ian Springer
 */
public abstract class UserPreferenceNames {
    /**
     * Default is 10.
     */
    public static final String AUTODISCOVERY_RANGE = ".dashContent.autoDiscovery.range";

    /**
     * Default is 5.
     */
    public static final String OPERATIONS_LAST_COMPLETED = ".dashContent.operations.lastCompleted";

    /**
     * Default is 5.
     */
    public static final String OPERATIONS_NEXT_SCHEDULED = ".dashContent.operations.nextScheduled";

    /**
     * Default is true.
     */
    //public static final String OPERATIONS_ = ".dashContent.operations.useLastCompleted";

    /**
     * Default is true.
     */
    public static final String OPERATIONS_USE_NEXT_SCHEDULED = ".dashContent.operations.useNextScheduled";

    /**
     * Show platform summary counts? Default is true.
     */
    public static final String SUMMARY_COUNTS_PLATFORM = ".dashContent.summaryCounts.platform";

    /**
     * Show server summary counts? Default is true.
     */
    public static final String SUMMARY_COUNTS_SERVER = ".dashContent.summaryCounts.server";

    /**
     * Show service summary counts? Default is true.
     */
    public static final String SUMMARY_COUNTS_SERVICE = ".dashContent.summaryCounts.service";

    /**
     * Default is true.
     */
    public static final String SUMMARY_COUNTS_GROUP_COMPAT = ".dashContent.summaryCounts.group.compat";

    /**
     * Default is true.
     */
    public static final String SUMMARY_COUNTS_GROUP_MIXED = ".dashContent.summaryCounts.group.mixed";

    /**
     * Default is true.
     */
    public static final String SUMMARY_COUNTS_SOFTWARE = ".dashContent.summaryCounts.software";

    /**
     * Default is true.
     */
    public static final String RESOURCE_HEALTH_AVAILABILITY = ".dashContent.resourcehealth.availability";

    /**
     * Default is true.
     */
    public static final String RESOURCE_HEALTH_ALERTS = ".dashContent.resourcehealth.alerts";

    /**
     * List of favorite {@link org.rhq.core.domain.resource.Resource} id's, delimited by '|' characters. Default is "".
     */
    public static final String RESOURCE_HEALTH_RESOURCES = ".dashContent.resourcehealth.resources";

    /**
     * List of favorite {@link org.rhq.core.domain.resource.group.ResourceGroup} id's, delimited by '|' characters.
     * Default is "".
     */
    public static final String GROUP_HEALTH_GROUPS = ".dashContent.grouphealth.groups";

    /**
     * Default is 24.
     */
    public static final String RECENTLY_APPROVED_RANGE = ".dashContent.recentlyApproved.range";

    /**
     * Default is "".
     */
    public static final String RECENTLY_APPROVED_RANGE_EXPANDED_PLATFORMS = ".dashContent.recentlyApproved.expandedPlatforms";

    /**
     * Default is 5.
     */
    public static final String ALERTS_NUMBER_OF_ALERTS = ".dashContent.criticalalerts.numberOfAlerts";

    /**
     * Default is 86400000.
     */
    public static final String ALERTS_PAST = ".dashContent.criticalalerts.past";

    /**
     * Default is 0.
     */
    public static final String ALERTS_PRIORITY = ".dashContent.criticalalerts.priority";

    /**
     * Default is "all".
     */
    public static final String ALERTS_SELECTED_OR_ALL = ".dashContent.criticalalerts.selectedOrAll";

    /**
     * Default is "".
     */
    public static final String ALERTS_RESOURCES = ".dashContent.criticalalerts.resources";

    /**
     * Default is 10.
     */
    public static final String PROBLEM_RESOURCES_RANGE = ".dashContent.problemResources.range";

    /**
     * Default is 8.
     */
    public static final String PROBLEM_RESOURCES_HOURS = ".dashContent.problemResources.hours";

    /**
     * Default is false.
     */
    public static final String PROBLEM_RESOURCES_SHOW_IGNORED = ".dashContent.problemResources.showIgnored";

    /**
     * Default is "".
     */
    public static final String PROBLEM_RESOURCES_IGNORE_LIST = ".dashContent.problemResources.ignoreList";

    /**
     * Dashboard portlets in first/left column. Default is "|.dashContent.searchResources|.dashContent.savedCharts|.dashContent.summaryCounts".
     */
    public static final String PORTAL_PORTLETS_FIRST = ".dashcontent.portal.portlets.first";

    /**
     * Dashboard portlets in second/right column. Default is "|.dashContent.autoDiscovery|.dashContent.recentlyApproved|.dashContent.resourceHealth|.dashContent.criticalAlerts|.dashContent.controlActions|.dashContent.problemResources".
     */
    public static final String PORTAL_PORTLETS_SECOND = ".dashcontent.portal.portlets.second";

    /**
     * Default is "".
     */
    public static final String MONITOR_VISIBILITY_FAVORITE_METRICS_PLATFORM = ".resource.common.monitor.visibility.favoriteMetrics.platform";

    /**
     * Default is "".
     */
    public static final String MONITOR_VISIBILITY_FAVORITE_METRICS_SERVER = ".resource.common.monitor.visibility.favoriteMetrics.server";

    /**
     * Default is "".
     */
    public static final String MONITOR_VISIBILITY_FAVORITE_METRICS_SERVICE = ".resource.common.monitor.visibility.favoriteMetrics.service";

    /**
     * Default is "".
     */
    public static final String MONITOR_VISIBILITY_FAVORITE_METRICS_APPLICATION = ".resource.common.monitor.visibility.favoriteMetrics.application";

    /**
     * Default is "".
     */
    public static final String MONITOR_VISIBILITY_FAVORITE_METRICS_GROUP = ".resource.common.monitor.visibility.favoriteMetrics.group";

    /**
     * Default is "".
     */
    public static final String MONITOR_VISIBILITY_METRIC_RANGE = ".resource.common.monitor.visibility.metricRange";

    /**
     * Default is false.
     */
    public static final String MONITOR_VISIBILITY_METRIC_RANGE_RO = ".resource.common.monitor.visibility.metricRange.ro";

    /**
     * Default is "";
     */
    public static final String MONITOR_VISIBILITY_METRIC_RANGE_LAST_N = ".resource.common.monitor.visibility.metricRange.lastN";

    /**
     * Default is "".
     */
    public static final String MONITOR_VISIBILITY_METRIC_RANGE_UNIT = ".resource.common.monitor.visibility.metricRange.unit";

    /**
     * Default is 1.
     */
    public static final String MONITOR_VISIBILITY_THRESHOLD = ".resource.common.monitor.visibility.metricThreshold";

    /**
     * Favorite Charts portlet: List of chart queries. Default is "".
     */
    public static final String CHARTS = ".dashContent.charts";

    /**
     * The time, in seconds, between automatic refreshes of the Dashboard and
     * the monitoring Indicator charts. 0 (zero) indicates no refreshes are to happen.
     * A refresh period longer than the current session timeout, e.g. 1800 seconds (30 minutes),
     * would likely require the user to log back into the Portal when the refresh occurs.
     * Default is 60.
     */
    public static final String PAGE_REFRESH_PERIOD = ".page.refresh.period";

    /**
     * Default is 60.
     */
    public static final String GROUP_CONFIGURATION_TIMEOUT_PERIOD = ".group.configuration.timeout.period";

    /**
     * A pipe-delimited list containing the last 3 URLs the user has visited. This is used by a workaround for the
     * JSF VIewExpiredException issue when the user tries to click the browser's Back button.
     */
    public static final String LAST_URL = ".last.url";

    /**
     * List of recently visited {@link org.rhq.core.domain.resource.Resource} id's, delimited by '|' characters. Default is "".
     *      */
    public static final String RECENT_RESOURCES = ".recent.resources";

    /**
     * List of recently visited {@link org.rhq.core.domain.resource.group.ResourceGroup} id's, delimited by '|' characters. Default is "".
     *      */
    public static final String RECENT_RESOURCE_GROUPS = ".recent.resourceGroups";

    /**
     * View mode for Browse Resource page - "CHART" or "LIST". This is most likely obsolete if we switch to GWT, since
     * we probably won't provide a CHART mode.
     */
    public static final String RESOURCE_BROWSER_VIEW_MODE = ".resource.browser.view";
    
    /**
     * List of booleans delimited by '|' characters denoting which subsystems are visible in the UI.
     * Default is "1|1|1|1|1|1|1|1".
     * The values belongs to (in this order): 
     * <ol>
     * <li>show content and bundles</li>
     * <li>show reports</li>
     * <li>show administration</li> 
     * <li>show events</li>
     * <li>show operations</li>
     * <li>show alerts</li>
     * <li>show configuration</li>
     * <li>show drift</li>
     * </ol>
     */
    public static final String UI_SHOW_SUBSYSTEMS = ".ui.showSubsystems";
    
    public enum UiSubsystem {
        CONTENT, REPORTS, ADMINISTRATION, EVENTS, OPERATIONS, ALERTS, CONFIG, DRIFT
    }

    private UserPreferenceNames() {
    }
}
