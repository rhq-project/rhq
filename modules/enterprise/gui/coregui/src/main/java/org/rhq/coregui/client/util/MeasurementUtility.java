/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.coregui.client.util;

import java.util.ArrayList;
import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.domain.measurement.util.Moment;

public class MeasurementUtility {

    private static final DateTimeFormat FORMATTER = DateTimeFormat.getFormat("MMM d, hh:mm a");
    private static final DateTimeFormat FORMATTER_YEAR = DateTimeFormat.getFormat("MMM d, yyyy hh:mm a");

    //Time constants
    public static final int UNIT_COLLECTION_POINTS = 1;
    public static final int UNIT_MINUTES = 2;
    public static final int UNIT_HOURS = 3;
    public static final int UNIT_DAYS = 4;
    public static final int UNIT_WEEKS = 5;
    /*
     * show units in terms of milliseconds
     */
    public static final long MINUTES = 60000;
    public static final long HOURS = 3600000;
    public static final long DAYS = 86400000;
    public static final long WEEKS = 604800000;

    /**
     * one year in milliseconds
     */
    public static final long ONE_YEAR = WEEKS * 52;

    /**
     * Method calculateTimeFrame
     * <p/>
     * Returns a two element <code>List</code> of <code>Moment</code> objects representing the begin and end times.
     * Returns null instead if the time unit is indicated as
     * <code>UNIT_COLLECTION_POINTS</code>. Ported to GWT from MeasurementUtils(i:server side dep
     * ii:old DateFormat doesn't play well with GWT).
     *
     * @param lastN the number of time units in the time frame
     * @param unit  the unit of time (as defined by <code>UNIT_*</code> constants
     * @return List
     */
    public static ArrayList<Moment> calculateTimeFrame(int lastN, int unit) {
        ArrayList<Moment> returnList = new ArrayList<Moment>(0);
        if (unit == UNIT_COLLECTION_POINTS) {
            return null;
        }

        // this is bad (client may have different timezone than server)
        Date now = new Date();

        long retrospective = lastN;

        switch (unit) {
        case UNIT_WEEKS:
            retrospective *= WEEKS;
            break;
        case UNIT_MINUTES:
            retrospective *= MINUTES;
            break;
        case UNIT_HOURS:
            retrospective *= HOURS;
            break;
        case UNIT_DAYS:
            retrospective *= DAYS;
            break;
        default:
            retrospective = -1;
            break;
        }

        if (retrospective < 0) {//translate unlimited hrs to 0 time.
            retrospective = now.getTime();
        }

        returnList.add(new Moment(new Date(now.getTime() - retrospective)));
        returnList.add(new Moment(now));

        return returnList;
    }

    /**Utility to return shared DateTimeFormat("MMM d, hh:mm a");
     *
     * @return DateTimeFormat
     */
    public static DateTimeFormat getDateTimeFormatter() {
        return FORMATTER;
    }

    /**Utility to return shared DateTimeFormat("MMM d, yyyy hh:mm a");
     *
     * @return DateTimeFormat
     */
    public static DateTimeFormat getDateTimeYearFormatter() {
        return FORMATTER_YEAR;
    }

    /**
     * Formats the passed summary.
     * 
     * @param summary MetricDisplaySummary with some values
     */
    public static void formatSimpleMetrics(MetricDisplaySummary summary) {
        // Don't try to format placeholder NaN values.
        if (!Double.isNaN(summary.getAvgMetric().getValue())) {
            if (summary.getUnits().length() < 1) {
                summary.setUnits(MeasurementUnits.NONE.name());
            }
            String[] formattedValues = MeasurementConverterClient.formatToSignificantPrecision(summary
                .getMetricValueDoubles(), MeasurementUnits.valueOf(summary.getUnits()), true);
            String[] metricKeys = summary.getMetricKeys();
            for (int i = 0; i < metricKeys.length; i++) {
                summary.getMetric(metricKeys[i]).setValueFmt(formattedValues[i]);
            }
        }
    }


}
