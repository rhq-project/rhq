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

package org.rhq.coregui.client.util.preferences;

import java.util.Arrays;
import java.util.List;

import org.rhq.core.domain.auth.Subject;
import org.rhq.coregui.client.components.measurement.AbstractMeasurementRangeEditor.MetricRangePreferences;
import org.rhq.coregui.client.util.MeasurementUtility;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.async.Command;
import org.rhq.coregui.client.util.async.CountDownLatch;

import com.google.gwt.user.client.rpc.AsyncCallback;

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

    /**
     * key values for indicator views
     */
    public static final String PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX = "monitor.visibility.indicator.views.";
    public static final String PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME = "Default";

    public static final String DEFAULT_VALUE_RANGE_RO = Boolean.FALSE.toString();
    public static final Integer DEFAULT_VALUE_RANGE_LASTN = 8;
    public static final Integer DEFAULT_VALUE_RANGE_UNIT = 3;

    private UserPreferences userPrefs;

    public MeasurementUserPreferences(UserPreferences userPrefs) {
        this.userPrefs = userPrefs;
    }

    public static class MetricViewsPreferences {
        public List<String> views;
    }

    public MetricRangePreferences getMetricRangePreferences() {
        MetricRangePreferences prefs = new MetricRangePreferences();

        prefs.explicitBeginEnd = Boolean.valueOf(
            userPrefs.getPreferenceEmptyStringIsDefault(PREF_METRIC_RANGE_BEGIN_END_FLAG, DEFAULT_VALUE_RANGE_RO))
            .booleanValue();
        if (!prefs.explicitBeginEnd) {
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

    /**
     * set the Metric Range preferences and do an implicit CoreGui.refresh() afterward.
     * @param prefs
     */
    public void setMetricRangePreferences(MetricRangePreferences prefs) {
        setMetricRangePreferences(prefs, true, null);
    }

    /**
     * set the Metric Range preferences but do not do a CoreGui.refresh() afterward.
     * @param prefs
     */
    public void setMetricRangePreferencesNoRefresh(MetricRangePreferences prefs) {
        setMetricRangePreferences(prefs, false, null);
    }

    /**
     * @param prefs The updated preferences
     * @param allowRefresh setting false will inhibit a preference change from calling CoreGui.refresh().  By
     * default a preference change will call for a refresh, so the current view can have the change applied. In
     * situations where refresh is being handled by the caller, this can be used to avoid a redundant refresh.
     */
    public void setMetricRangePreferences(MetricRangePreferences prefs, boolean allowRefresh, Command callback) {
        AsyncCallback<Subject> persistCallback = null;
        if (null != callback) {
            // there are either 2 or 3 setPreference calls depending on prefs.explicitBeginEnd
            final CountDownLatch latch = CountDownLatch.create(prefs.explicitBeginEnd ? 2 : 3, callback);
            persistCallback = new AsyncCallback<Subject>() {
                @Override
                public void onFailure(Throwable arg0) {
                    latch.countDown();
                }

                @Override
                public void onSuccess(Subject arg0) {
                    latch.countDown();
                }
            };
        }
        userPrefs.setPreference(PREF_METRIC_RANGE_BEGIN_END_FLAG, String.valueOf(prefs.explicitBeginEnd), allowRefresh, persistCallback);
        if (prefs.explicitBeginEnd) {
            // persist advanced mode
            userPrefs.setPreference(PREF_METRIC_RANGE, Arrays.asList(prefs.begin, prefs.end), allowRefresh, persistCallback);
        } else {
            userPrefs.setPreference(PREF_METRIC_RANGE_LASTN, String.valueOf(prefs.lastN), allowRefresh, persistCallback);
            userPrefs.setPreference(PREF_METRIC_RANGE_UNIT, String.valueOf(prefs.unit), allowRefresh, persistCallback);
        }
    }



    public MetricViewsPreferences getMetricViews(String key) {
        MetricViewsPreferences prefs = new MetricViewsPreferences();
        String value = userPrefs.getPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + key,
            PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME);
        prefs.views = StringUtility.explode(value, UserPreferences.PREF_LIST_DELIM);
        return prefs;
    }



    public String getSelectedView(String key) {
        String value = userPrefs.getPreference(PREF_MEASUREMENT_INDICATOR_VIEW_PREFIX + "selected." + key,
            PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT_NAME);
        return value;
    }

}
