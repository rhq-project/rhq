package org.rhq.enterprise.gui.legacy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.gui.uibeans.UIConstants;
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

    public static final String PREF_PAGE_REFRESH_PERIOD = ".page.refresh.period";

    /** delimiter for preferences that are multi-valued and stringified */
    private static final String PREF_LIST_DELIM = ",";

    private Subject subject;

    public WebUserPreferences(Subject subject) {
        this.subject = subject;
    }

    public Configuration getPreferences() {
        return this.subject.getUserConfiguration();
    }

    public void setPreferences(Configuration preferences) {
        this.subject.setUserConfiguration(preferences);
    }

    /**
     * This forces a flush of the user preferences to the database.
     */
    public void persistPreferences() {
        Integer sessionId = this.subject.getSessionId(); // let's remember our transient session ID
        this.subject = LookupUtil.getSubjectManager().updateSubject(this.subject, this.subject);
        this.subject.setSessionId(sessionId); // put the transient data back into our new subject
    }

    public String getPreference(String key) throws IllegalArgumentException {
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

        return value;
    }

    public String getPreference(String key, String defaultValue) {
        String value;
        try {
            value = getPreference(key);
        } catch (IllegalArgumentException iae) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Break the named preference into tokens delimited by <code>PREF_LIST_DELIM</code>.
     *
     * @param key the name of the preference
     * @return <code>List</code> of <code>String</code> tokens
     */
    public List<String> getPreferenceAsList(String key) {
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

    public void setPreference(String key, List values) throws IllegalArgumentException {
        setPreference(key, values, PREF_LIST_DELIM);
    }

    public void setPreference(String key, List values, String delim) throws IllegalArgumentException {
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
            this.subject.getUserConfiguration().put(new PropertySimple(key, val));
        } else {
            existingProp.setStringValue(val);
        }
    }

    public void unsetPreference(String key) {
        Configuration config = subject.getUserConfiguration();
        if (config != null)
            config.remove(key);
    }

    /**
     * Returns a list of metric ids saved as favorites for a particular appdef
     * type
     */
    public List getResourceFavoriteMetricsPreference(String appdefTypeName) throws IllegalArgumentException {
        return getPreferenceAsList(getResourceFavoriteMetricsKey(appdefTypeName));
    }

    /**
     * Method getResourceFavoriteMetricsKey.
     *
     * Encapsulates the logic for how the favorite metrics key for a particular appdef
     * type is calculated
     *
     * @param appdefTypeName i.e. application, platform, server, service
     * @return String the calculated preferences key
     */
    public String getResourceFavoriteMetricsKey(String appdefTypeName) {
        StringBuffer sb = new StringBuffer(PREF_FAV_RESOURCE_METRICS_PREFIX);
        sb.append('.').append(appdefTypeName);
        return sb.toString();
    }

    /**
     * Returns a Map of pref values:
     *
     * <ul>
     *   <li><code>MonitorUtils.RO</code>: Boolean
     *   <li><code>MonitorUtils.LASTN</code>: Integer
     *   <li><code>MonitorUtils.UNIT</code>: Unit
     *   <li><code>MonitorUtils.BEGIN</code>: Long
     *   <li><code>MonitorUtils.END</code>: Long
     * </ul>
     */
    public Map<String, ?> getMetricRangePreference(boolean defaultRange) throws IllegalArgumentException {
        Map<String, Object> m = new HashMap<String, Object>();

        //  properties may be empty or unparseable strings (ex:
        //  "null"). if so, use their default values.
        Boolean ro;
        try {
            ro = Boolean.valueOf(getPreference(PREF_METRIC_RANGE_RO));
        } catch (IllegalArgumentException nfe) {
            ro = MonitorUtils.DEFAULT_VALUE_RANGE_RO;
        }
        m.put(MonitorUtils.RO, ro);

        Integer lastN = null;
        try {
            lastN = Integer.valueOf(getPreference(PREF_METRIC_RANGE_LASTN));
        } catch (IllegalArgumentException nfe) {
            lastN = MonitorUtils.DEFAULT_VALUE_RANGE_LASTN;
        }
        m.put(MonitorUtils.LASTN, lastN);

        Integer unit = null;
        try {
            unit = Integer.valueOf(getPreference(PREF_METRIC_RANGE_UNIT));
        } catch (IllegalArgumentException nfe) {
            unit = MonitorUtils.DEFAULT_VALUE_RANGE_UNIT;
        }
        m.put(MonitorUtils.UNIT, unit);

        List range = null;
        try {
            range = getPreferenceAsList(PREF_METRIC_RANGE);
        } catch (IllegalArgumentException iae) {
            // that's ok
        }
        Long begin = null;
        Long end = null;
        if (range != null && range.size() > 0) {
            try {
                begin = new Long((String) range.get(0));
                end = new Long((String) range.get(1));
            } catch (NumberFormatException nfe) {
                begin = null;
                end = null;
            }
        }

        // sometimes we are satisfied with no range. other times we
        // need to calculate the "last n" units range and return
        // that.
        if (defaultRange && begin == null && end == null) {
            range = MonitorUtils.calculateTimeFrame(lastN.intValue(), unit.intValue());

            begin = (Long) range.get(0);
            end = (Long) range.get(1);
        }

        m.put(MonitorUtils.BEGIN, begin);
        m.put(MonitorUtils.END, end);

        return m;
    }

    public Map<String, ?> getMetricRangePreference() throws IllegalArgumentException {
        return getMetricRangePreference(true);
    }

    /**
     * Returns a list of metric ids saved as favorites for a particular appdef
     * type
     */
    public Integer getMetricThresholdPreference() throws IllegalArgumentException {
        return new Integer(getPreference(PREF_METRIC_THRESHOLD));
    }

    /**
     * Get the value of a preference as a boolean.
     * @param key the preference to get
     * @param ifNull if the pref is undefined, return this value instead
     * @return the boolean value of 'key', or if key is null, returns the
     * 'ifNull' value.
     */
    public boolean getBooleanPref(String key, boolean ifNull) {
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
     * @param ifNull if the pref is null, return this value instead
     * @return the int value of 'key', or if key is null, returns the
     * 'ifNull' value.
     */
    public int getIntPref(String key) {
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
    public int getIntPref(String key, int ifNull) {
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

    public int getPageRefreshPeriod() {
        return getIntPref(PREF_PAGE_REFRESH_PERIOD, UIConstants.DONT_REFRESH_PAGE);
    }

    public void setPageRefreshPeriod(int period) {
        setPreference(PREF_PAGE_REFRESH_PERIOD, Integer.valueOf(period));
    }

    public static class SummaryCountPreferences {
        public boolean showPlatforms;
        public boolean showServers;
        public boolean showServices;
        public boolean showCompatibleGroups;
        public boolean showMixedGroups;
        public boolean showGroupDefinitions;
    }

    public SummaryCountPreferences getSummaryCounts() {
        SummaryCountPreferences counts = new SummaryCountPreferences();
        counts.showPlatforms = getBooleanPref(PREF_DASH_SHOW_SUMMARY_COUNTS_PLATFORM, true);
        counts.showServers = getBooleanPref(PREF_DASH_SHOW_SUMMARY_COUNTS_SERVER, true);
        counts.showServices = getBooleanPref(PREF_DASH_SHOW_SUMMARY_COUNTS_SERVICE, true);
        counts.showCompatibleGroups = getBooleanPref(PREF_DASH_SHOW_SUMMARY_COUNTS_GROUP_COMPAT, true);
        counts.showMixedGroups = getBooleanPref(PREF_DASH_SHOW_SUMMARY_COUNTS_GROUP_MIXED, true);
        counts.showGroupDefinitions = getBooleanPref(PREF_DASH_SHOW_SUMMARY_COUNTS_GROUP_DEFINITIONS, true);
        return counts;
    }

    public void setSummaryCounts(SummaryCountPreferences counts) {
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
}
