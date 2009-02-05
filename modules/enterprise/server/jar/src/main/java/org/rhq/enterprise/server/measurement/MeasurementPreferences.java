package org.rhq.enterprise.server.measurement;

import java.util.Arrays;
import java.util.List;

import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.auth.SubjectPreferencesBase;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;

public class MeasurementPreferences extends SubjectPreferencesBase {

    public static final String PREF_FAV_RESOURCE_METRICS_PREFIX = ".resource.common.monitor.visibility.favoriteMetrics";
    public static final String PREF_METRIC_RANGE = ".resource.common.monitor.visibility.metricRange";
    public static final String PREF_METRIC_RANGE_LASTN = ".resource.common.monitor.visibility.metricRange.lastN";
    public static final String PREF_METRIC_RANGE_UNIT = ".resource.common.monitor.visibility.metricRange.unit";
    public static final String PREF_METRIC_RANGE_RO = ".resource.common.monitor.visibility.metricRange.ro";
    public static final String PREF_METRIC_THRESHOLD = ".resource.common.monitor.visibility.metricThreshold";

    public static final Boolean DEFAULT_VALUE_RANGE_RO = Boolean.FALSE;
    public static final Integer DEFAULT_VALUE_RANGE_LASTN = 8;
    public static final Integer DEFAULT_VALUE_RANGE_UNIT = 3;

    /**
     * key values for indicator views
     */
    public static final String PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX = "monitor.visibility.indicator.views.";
    public static final String PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT = "resource.common.monitor.visibility.defaultview";
    public static final String PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME = "Default";

    public MeasurementPreferences(Subject subject) {
        super(subject);
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

        public String toString() {
            return getClass().getSimpleName() + "[readOnly=" + readOnly + ",lastN=" + lastN + ",unit=" + unit
                + ",begin=" + begin + end + ",end=" + end + "]";
        }
    }

    public MetricRangePreferences getMetricRangePreferences() {
        MetricRangePreferences prefs = new MetricRangePreferences();

        prefs.readOnly = getPreference(PREF_METRIC_RANGE_RO, DEFAULT_VALUE_RANGE_RO);
        prefs.lastN = getPreference(PREF_METRIC_RANGE_LASTN, DEFAULT_VALUE_RANGE_LASTN);
        prefs.unit = getPreference(PREF_METRIC_RANGE_UNIT, DEFAULT_VALUE_RANGE_UNIT);

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
         * get the begin/end range if: 
         * 
         * 1) we are in "last X units" mode, or
         * 2) we're in "advanced" mode, but the begin/end is not set
         */
        if (prefs.readOnly == false | (prefs.begin == null && prefs.end == null)) {
            // try to get the range from the "last X units" preferences first
            range = MeasurementUtils.calculateTimeFrame(prefs.lastN, prefs.unit);
            if (range == null) {
                // but if that fails, fall back to default preferences
                range = MeasurementUtils.calculateTimeFrame(DEFAULT_VALUE_RANGE_LASTN, DEFAULT_VALUE_RANGE_UNIT);
            }

            prefs.begin = (Long) range.get(0);
            prefs.end = (Long) range.get(1);
        }

        log.info("Getting Metric Range Preferences:" + prefs);

        return prefs;
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

        log.info("Setting Metric Range Preferences:" + prefs);
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
        prefs.views = StringUtil.explode(value, PREF_ITEM_DELIM);
        return prefs;
    }

    public void setMetricViews(MetricViewsPreferences prefs, String key) {
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (String viewName : prefs.views) {
            if (index != 0) {
                builder.append(PREF_ITEM_DELIM);
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
        chartPreferences.charts = StringUtil.explode(data, PREF_ITEM_DELIM);
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
                builder.append(PREF_ITEM_DELIM);
            }
            builder.append(chart);
            index++;
        }
        setPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + context + "." + viewName, builder.toString());
    }

    public void deleteMetricViewData(String context, String viewName) {
        unsetPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + context + "." + viewName);
    }

}
