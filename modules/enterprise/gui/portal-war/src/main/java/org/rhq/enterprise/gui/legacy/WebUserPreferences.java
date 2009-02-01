package org.rhq.enterprise.gui.legacy;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.composite.ResourceIdFlyWeight;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.legacy.util.DashboardUtils;
import org.rhq.enterprise.gui.uibeans.UIConstants;
import org.rhq.enterprise.server.alert.engine.internal.Tuple;
import org.rhq.enterprise.server.auth.SubjectPreferencesBase;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class WebUserPreferences extends SubjectPreferencesBase {

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
    public static final String PREF_DASH_RECENTLY_APPROVED_HOURS = ".dashContent.recentlyApproved.hours";
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

    public static final String PREF_PAGE_REFRESH_PERIOD = ".page.refresh.period";

    public static final String PREF_LAST_URL = ".last.url";

    public WebUserPreferences(Subject subject) {
        super(subject);
    }

    public int getPageRefreshPeriod() {
        return getIntPref(PREF_PAGE_REFRESH_PERIOD, UIConstants.DONT_REFRESH_PAGE);
    }

    public void setPageRefreshPeriod(int period) {
        setPreference(PREF_PAGE_REFRESH_PERIOD, Integer.valueOf(period));
    }

    public String getLastVisitedURL(int previousOffset) {
        List<String> urls = getPreferenceAsList(PREF_LAST_URL);
        String url = urls.get(urls.size() - previousOffset);
        return url;
    }

    public void addLastVisitedURL(String url) {
        List<String> urls = getPreferenceAsList(PREF_LAST_URL);
        if (urls == null) {
            urls = new ArrayList<String>();
        }
        urls.add(url);
        // maintain at most the last 3 urls, that's all we need to handle the ViewExpiredException elegantly
        if (urls.size() > 3) {
            urls.remove(0);
        }
        setPreference(PREF_LAST_URL, urls);
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
        prefs.range = getIntPref(PREF_DASH_PROBLEM_RESOURCES_ROWS, 10);
        prefs.hours = getIntPref(PREF_DASH_PROBLEM_RESOURCES_HOURS, -1);
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
        public int hours;
        public List<String> expandedPlatforms;
    }

    public RecentlyApprovedPortletPreferences getRecentlyApprovedPortletPreferences() {
        RecentlyApprovedPortletPreferences prefs = new RecentlyApprovedPortletPreferences();
        prefs.range = getIntPref(PREF_DASH_RECENTLY_APPROVED_RANGE, 10);
        prefs.hours = getIntPref(PREF_DASH_RECENTLY_APPROVED_HOURS, -1);
        prefs.expandedPlatforms = getPreferenceAsList(PREF_DASH_RECENTLY_APPROVED_EXPANDED_PLATFORMS);
        return prefs;
    }

    public void setRecentlyApprovedPortletPreferences(RecentlyApprovedPortletPreferences prefs) {
        setPreference(PREF_DASH_RECENTLY_APPROVED_RANGE, prefs.range);
        setPreference(PREF_DASH_RECENTLY_APPROVED_HOURS, prefs.hours);
        setPreference(PREF_DASH_RECENTLY_APPROVED_EXPANDED_PLATFORMS, prefs.expandedPlatforms);
    }

    public static class DateTimeDisplayPreferences {
        // TODO: jmarques - make DateTimeDisplayPreferences configurable
        public final String dateFormat = "M/d/yy";
        public final String timeFormat = "h:mm:ss aa, zzz";
        public final String dateTimeFormat = "M/d/yy, h:mm:ss aa, zzz";
        public final String dateTimeFormatTrigger = "M/d/yy, HH:mm";

        public String getDateTimeFormat() {
            return dateTimeFormat;
        }

        public String getDateTimeFormatTrigger() {
            return dateTimeFormatTrigger;
        }
    }

    public DateTimeDisplayPreferences getDateTimeDisplayPreferences() {
        return new DateTimeDisplayPreferences();
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
            String preferenceName = PREF_DASH_USER_SAVED_CHARTS + "." + counter;
            try {
                chart = getPreference(preferenceName, null);
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
            } catch (Exception e) {
                errorOccurred = true;
                log.warn("Error reading SavedChartsPortletPreferences for preference[name=" + preferenceName + "]: "
                    + e.getMessage());
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
            if (view.isUnlimited()) {
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

            // with recent improvements to the RF data table, it's possible to save the page number now
            if (i - 1 < pageControlProperties.size()) {
                int pageNumber = Integer.valueOf(pageControlProperties.get(i - 1));
                pageControl.setPageNumber(pageNumber);
            }

            if (view.isUnlimited() && pageSize != PageControl.SIZE_UNLIMITED) {
                // make sure pageSize for an unlimited view is actually unlimited
                pageControl.setPageSize(PageControl.SIZE_UNLIMITED);
                setPageControl(view, pageControl);
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

        pageControlProperties.add(pageControl.getPageNumber());

        setPreference(view.toString(), pageControlProperties);
    }
}