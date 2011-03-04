/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.util.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;

/**
 * This wraps a UserPreferences object to obtain measurement specific preferences.
 * 
 * @author John Mazzitelli
 */
public class MeasurementUserPreferences {

    public static final String PREF_METRIC_RANGE = UserPreferenceNames.MONITOR_VISIBILITY_METRIC_RANGE;
    public static final String PREF_METRIC_RANGE_LASTN = UserPreferenceNames.MONITOR_VISIBILITY_METRIC_RANGE_LAST_N;
    public static final String PREF_METRIC_RANGE_UNIT = UserPreferenceNames.MONITOR_VISIBILITY_METRIC_RANGE_UNIT;
    public static final String PREF_METRIC_RANGE_BEGIN_END_FLAG = UserPreferenceNames.MONITOR_VISIBILITY_METRIC_RANGE_RO;
    public static final String PREF_METRIC_THRESHOLD = UserPreferenceNames.MONITOR_VISIBILITY_THRESHOLD;

    /**
     * key values for indicator views
     */
    public static final String PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX = "monitor.visibility.indicator.views.";
    public static final String PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT = "resource.common.monitor.visibility.defaultview";
    public static final String PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME = "Default";

    public static final String DEFAULT_VALUE_RANGE_RO = Boolean.FALSE.toString();
    public static final Integer DEFAULT_VALUE_RANGE_LASTN = Integer.valueOf(8);
    public static final Integer DEFAULT_VALUE_RANGE_UNIT = Integer.valueOf(3);

    private UserPreferences userPrefs;

    public MeasurementUserPreferences(UserPreferences userPrefs) {
        this.userPrefs = userPrefs;
    }

    public static class MetricViewsPreferences {
        public List<String> views;
    }

    public static class MetricRangePreferences {
        // if readOnly is true, then the beginning and ending range dates are specified with explicit dates
        // if readOnly is false, then the time is relative to NOW and is specified as <lastN> units of <unit> time
        public boolean explicitBeginEnd;

        // simple, when readOnly is false
        public int lastN;
        public int unit; // see MeasurementUtility.UNIT_xxx

        // advanced, when readOnly is true
        public Long begin;
        public Long end;

        /**
         * Returns a two element <code>List</code> of <code>Long</code> objects representing the begin and end times (in
         * milliseconds since the epoch) of the time frame.
         **/
        public ArrayList<Long> getBeginEndTimes() {
            if (explicitBeginEnd) {
                ArrayList<Long> times = new ArrayList<Long>(2);
                times.add(begin);
                times.add(end);
                return times;
            } else {
                return MeasurementUtility.calculateTimeFrame(lastN, unit);
            }
        }

        public String toString() {
            return (explicitBeginEnd) ? "[begin=" + begin + end + ",end=" + end + "]" : "[lastN=" + lastN + ",unit="
                + unit + "]";
        }
    }

    public MetricRangePreferences getMetricRangePreferences() {
        MetricRangePreferences prefs = new MetricRangePreferences();

        prefs.explicitBeginEnd = Boolean.valueOf(
            userPrefs.getPreferenceEmptyStringIsDefault(PREF_METRIC_RANGE_BEGIN_END_FLAG, DEFAULT_VALUE_RANGE_RO))
            .booleanValue();
        if (prefs.explicitBeginEnd == false) {
            prefs.lastN = Integer.valueOf(userPrefs.getPreferenceEmptyStringIsDefault(PREF_METRIC_RANGE_LASTN,
                DEFAULT_VALUE_RANGE_LASTN.toString()));
            prefs.unit = Integer.valueOf(userPrefs.getPreferenceEmptyStringIsDefault(PREF_METRIC_RANGE_UNIT,
                DEFAULT_VALUE_RANGE_UNIT.toString()));

            List<Long> range = MeasurementUtility.calculateTimeFrame(prefs.lastN, prefs.unit);
            prefs.begin = range.get(0);
            prefs.end = range.get(1);
        } else {
            try {
                String rangeString = userPrefs.getPreference(PREF_METRIC_RANGE);
                if (rangeString != null && rangeString.trim().length() > 0) {
                    if (rangeString.contains(",")) { // legacy support: old prefs used to use commas
                        rangeString = rangeString.replace(",", UserPreferences.PREF_LIST_DELIM);
                        //userPrefs.setPreference(PREF_METRIC_RANGE, rangeString); // TODO set only if we don't support JSF anymore
                    }
                    String[] beginEnd = rangeString.split(UserPreferences.PREF_LIST_DELIM_REGEX);
                    prefs.begin = Long.parseLong(beginEnd[0]);
                    prefs.end = Long.parseLong(beginEnd[1]);
                }
            } catch (IllegalArgumentException iae) {
                // that's OK, range will remain null and we might use the lastN / unit
                List<Long> range = MeasurementUtility.calculateTimeFrame(DEFAULT_VALUE_RANGE_LASTN,
                    DEFAULT_VALUE_RANGE_UNIT);
                prefs.begin = range.get(0);
                prefs.end = range.get(1);
            }
        }

        return prefs;
    }

    public void setMetricRangePreferences(MetricRangePreferences prefs) {
        userPrefs.setPreference(PREF_METRIC_RANGE_BEGIN_END_FLAG, String.valueOf(prefs.explicitBeginEnd));
        if (prefs.explicitBeginEnd) {
            // persist advanced mode
            userPrefs.setPreference(PREF_METRIC_RANGE, Arrays.asList(prefs.begin, prefs.end));
            //unsetPreference(PREF_METRIC_RANGE_LASTN);
            //unsetPreference(PREF_METRIC_RANGE_UNIT);
        } else {
            userPrefs.setPreference(PREF_METRIC_RANGE_LASTN, String.valueOf(prefs.lastN));
            userPrefs.setPreference(PREF_METRIC_RANGE_UNIT, String.valueOf(prefs.unit));
            //unsetPreference(PREF_METRIC_RANGE);
        }
    }

    /*
     * I believe these are now no longer used - these were probably for the old struts pages
     * 
    public Integer getMetricThresholdPreference() throws IllegalArgumentException {
        return new Integer(userPrefs.getPreference(PREF_METRIC_THRESHOLD));
    }

    public void setMetricThresholdPreference(Integer value) throws IllegalArgumentException {
        userPrefs.setPreference(PREF_METRIC_THRESHOLD, String.valueOf(value));
    }
     */

    public MetricViewsPreferences getMetricViews(String key) {
        MetricViewsPreferences prefs = new MetricViewsPreferences();
        //TODO: jmarques - externalize default view name
        // instead of PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME
        // lookup PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT from the bundle
        String value = userPrefs.getPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + key,
            PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME);
        prefs.views = StringUtility.explode(value, UserPreferences.PREF_LIST_DELIM);
        return prefs;
    }

    public void setMetricViews(MetricViewsPreferences prefs, String key) {
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (String viewName : prefs.views) {
            if (index != 0) {
                builder.append(UserPreferences.PREF_LIST_DELIM);
            }
            builder.append(viewName);
            index++;
        }
        userPrefs.setPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + key, builder.toString());
    }

    public String getSelectedView(String key) {
        String value = userPrefs.getPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + "selected." + key,
            PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME);
        return value;
    }

    public void setSelectedView(String key, String viewName) {
        userPrefs.setPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + "selected." + key, viewName);
    }

    public static class MetricViewData {
        public List<String> charts;
    }

    public MetricViewData getMetricViewData(String context, String viewName) {
        //TODO: jmarques - externalize default view name
        // instead of PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME
        // lookup PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT from the bundle
        if (viewName == null || "".equals(viewName)) {
            viewName = PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME;
        }
        MetricViewData chartPreferences = new MetricViewData();
        // important to let IllegalArgumentException bubble out of here, so the caller can persist the default set
        String data = userPrefs.getPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + context + "." + viewName);
        chartPreferences.charts = StringUtility.explode(data, UserPreferences.PREF_LIST_DELIM);
        return chartPreferences;
    }

    public void setMetricViewData(String context, String viewName, MetricViewData prefs) {
        //TODO: jmarques - externalize default view name
        // instead of PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME
        // lookup PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT from the bundle
        if (viewName == null || "".equals(viewName)) {
            viewName = PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME;
        }
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (String chart : prefs.charts) {
            if (index != 0) {
                builder.append(UserPreferences.PREF_LIST_DELIM);
            }
            builder.append(chart);
            index++;
        }
        userPrefs.setPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + context + "." + viewName, builder.toString());
    }

    public void deleteMetricViewData(String context, String viewName) {
        userPrefs.unsetPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + context + "." + viewName);
    }

}
