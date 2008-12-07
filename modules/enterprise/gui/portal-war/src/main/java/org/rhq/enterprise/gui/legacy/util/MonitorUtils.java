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
package org.rhq.enterprise.gui.legacy.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.LabelValueBean;
import org.apache.struts.util.MessageResources;

import org.rhq.core.clientapi.util.units.DateFormatter.DateSpecifics;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.core.domain.measurement.composite.MeasurementValueAndUnits;
import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.NumberConstants;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;

public class MonitorUtils {

    private static Log log = LogFactory.getLog(MonitorUtils.class);
    public static final String RO = "ro";
    public static final String LASTN = "lastN";
    public static final String UNIT = "unit";
    public static final String BEGIN = "begin";
    public static final String END = "end";

    public static final int DEFAULT_CURRENTHEALTH_LASTN = 8;

    public static final Boolean DEFAULT_VALUE_RANGE_RO = Boolean.FALSE;
    public static final Integer DEFAULT_VALUE_RANGE_LASTN = 8;
    public static final Integer DEFAULT_VALUE_RANGE_UNIT = 3;

    public static final int UNIT_COLLECTION_POINTS = 1;
    public static final int UNIT_MINUTES = 2;
    public static final int UNIT_HOURS = 3;
    public static final int UNIT_DAYS = 4;

    public static final int THRESHOLD_BASELINE_VALUE = 1;
    public static final String THRESHOLD_BASELINE_LABEL = "Baseline";
    public static final int THRESHOLD_HIGH_RANGE_VALUE = 2;
    public static final String THRESHOLD_HIGH_RANGE_LABEL = "HighRange";
    public static final int THRESHOLD_LOW_RANGE_VALUE = 3;
    public static final String THRESHOLD_LOW_RANGE_LABEL = "LowRange";

    public static final int THRESHOLD_UNDER_VALUE = 1;
    public static final int THRESHOLD_OVER_VALUE = 2;

    /**
     * Method calculateTimeFrame
     * <p/>
     * Returns a two element<code>List</code> of <code>Long</code> objects representing the begin and end times (in
     * milliseconds since the epoch) of the timeframe. Returns null instead if the time unit is indicated as
     * <code>UNIT_COLLECTION_POINTS</code>.
     *
     * @param lastN the number of time units in the time frame
     * @param unit  the unit of time (as defined by <code>UNIT_*</code> constants
     * @return List
     */
    public static List<Long> calculateTimeFrame(int lastN, int unit) {
        List<Long> l = new ArrayList<Long>(0);

        if (unit == UNIT_COLLECTION_POINTS) {
            return null;
        }

        long now = System.currentTimeMillis();

        long retrospective = lastN;
        switch (unit) {
        case UNIT_MINUTES:
            retrospective *= NumberConstants.MINUTES;
            break;
        case UNIT_HOURS:
            retrospective *= NumberConstants.HOURS;
            break;
        case UNIT_DAYS:
            retrospective *= NumberConstants.DAYS;
            break;
        default:
            retrospective = -1;
            break;
        }

        l.add(new Long(now - retrospective));
        l.add(new Long(now));

        return l;
    }

    public static Integer formatMetrics(List<MetricDisplaySummary> metricDisplaySummaries, Locale userLocale,
        MessageResources msgs) {
        Integer resourceCount = null;
        try {
            for (MetricDisplaySummary metricDisplaySummary : metricDisplaySummaries) {
                if (Double.isNaN(metricDisplaySummary.getAvgMetric().getValue())) {
                    continue;
                }
                if (resourceCount == null)
                    resourceCount = metricDisplaySummary.getAvailUp();
                // the formatting subsystem doesn't interpret
                // units set to empty strings as "no units" so
                // we'll explicity set it so
                if (metricDisplaySummary.getUnits().length() < 1) {
                    metricDisplaySummary.setUnits(MeasurementUnits.NONE.name());
                }
                MeasurementValueAndUnits[] formattedValues;
                if (msgs.isPresent(userLocale, Constants.UNIT_FORMAT_PREFIX_KEY + metricDisplaySummary.getUnits())) {
                    // this means that there's a whole song and dance for formatting this type of thing
                    // a certain way (hopefully in a way that some descendant of java.test.Format will
                    // help with)
                    String fmtString = msgs.getMessage(userLocale, Constants.UNIT_FORMAT_PREFIX_KEY
                        + metricDisplaySummary.getUnits());
                    if (metricDisplaySummary.getUnits().equals(MeasurementUnits.EPOCH_MILLISECONDS.name())) {
                        DateSpecifics dateSpecifics = new DateSpecifics();
                        dateSpecifics.setDateFormat(new SimpleDateFormat(fmtString, userLocale));

                        // TODO jmarques - figure out how to add dateSpecifics in here
                        formattedValues = getMeasurementValueAndUnits(metricDisplaySummary.getMetricValueDoubles(),
                            metricDisplaySummary.getUnits());
                        // dateSpecifics);
                    } else {
                        formattedValues = getMeasurementValueAndUnits(metricDisplaySummary.getMetricValueDoubles(),
                            metricDisplaySummary.getUnits());
                    }
                } else {
                    formattedValues = getMeasurementValueAndUnits(metricDisplaySummary.getMetricValueDoubles(),
                        metricDisplaySummary.getUnits());
                }
                String[] keys = metricDisplaySummary.getMetricKeys();
                if (keys.length != formattedValues.length) {
                    throw new IllegalStateException("Formatting metrics failed.");
                }
                for (int i = 0; i < keys.length; i++) {
                    metricDisplaySummary.getMetric(keys[i]).setValueFmt(formattedValues[i].toString());
                }
            }
        } catch (IllegalArgumentException e) { // catch and rethrow for debug/logging only
            if (log.isDebugEnabled()) {
                log.debug("formatting metrics failed due to IllegalArgumentException: ", e);
            }
            throw e;
        }
        return resourceCount;
    }

    /**
     * Formats the passed summary. The userLocale is currently ignored
     * @param summary  MetricDisplaySummary with some values
     * @param userLocale ignored
     */
    public static void formatSimpleMetrics(MetricDisplaySummary summary, Locale userLocale) {
        // Don't try to format placeholder NaN values.
        if (!Double.isNaN(summary.getAvgMetric().getValue())) {
            if (summary.getUnits().length() < 1) {
                summary.setUnits(MeasurementUnits.NONE.name());
            }
            String[] formattedValues = MeasurementConverter.formatToSignificantPrecision(summary
                .getMetricValueDoubles(), MeasurementUnits.valueOf(summary.getUnits()), true);
            String[] metricKeys = summary.getMetricKeys();
            for (int i = 0; i < metricKeys.length; i++) {
                summary.getMetric(metricKeys[i]).setValueFmt(formattedValues[i]);
            }
        }
    }

    public static MeasurementValueAndUnits[] getMeasurementValueAndUnits(double[] metricValueDoubles, String units) {
        MeasurementValueAndUnits[] valueAndUnitsList = new MeasurementValueAndUnits[metricValueDoubles.length];

        int i = 0;
        for (Double metricValue : metricValueDoubles) {
            valueAndUnitsList[i] = new MeasurementNumericValueAndUnits(metricValue, MeasurementUnits.valueOf(units));
            i++;
        }

        return valueAndUnitsList;
    }

    public static List<LabelValueBean> getThresholdMenu() {
        List<LabelValueBean> items = new ArrayList<LabelValueBean>();
        String label = null, value = null;

        label = MonitorUtils.THRESHOLD_BASELINE_LABEL;
        value = String.valueOf(MonitorUtils.THRESHOLD_BASELINE_VALUE);
        items.add(new LabelValueBean(label, value));

        label = MonitorUtils.THRESHOLD_HIGH_RANGE_LABEL;
        value = String.valueOf(MonitorUtils.THRESHOLD_HIGH_RANGE_VALUE);
        items.add(new LabelValueBean(label, value));

        label = MonitorUtils.THRESHOLD_LOW_RANGE_LABEL;
        value = String.valueOf(MonitorUtils.THRESHOLD_LOW_RANGE_VALUE);
        items.add(new LabelValueBean(label, value));

        return items;
    }
}
