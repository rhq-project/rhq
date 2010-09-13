package org.rhq.enterprise.gui.coregui.client.util;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.i18n.client.DateTimeFormat;

public class MeasurementUtility {

    private static DateTimeFormat formatter = DateTimeFormat.getFormat("MMM d, hh:mm a");
    private static DateTimeFormat formatterYear = DateTimeFormat.getFormat("MMM d, yyyy hh:mm a");

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
     * five minutes in millisecond increments
     */
    public static final long FIVE_MINUTES = 300000;
    public static final long ONE_YEAR = WEEKS * 52;

    /**
     * Method calculateTimeFrame
     * <p/>
     * Returns a two element<code>List</code> of <code>Long</code> objects representing the begin and end times (in
     * milliseconds since the epoch) of the timeframe. Returns null instead if the time unit is indicated as
     * <code>UNIT_COLLECTION_POINTS</code>. Ported to GWT from MeasurementUtils(i:server side dep
     * ii:old DateFormat doesn't play well with GWT).
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
            retrospective = now;
        }

        l.add(now - retrospective);
        l.add(now);

        return l;
    }

    /**Utility to return shared DateTimeFormat("MMM d, hh:mm a");
     *
     * @return DateTimeFormat
     */
    public static DateTimeFormat getDateTimeFormatter() {
        return formatter;
    }

    /**Utility to return shared DateTimeFormat("MMM d, yyyy hh:mm a");
     *
     * @return DateTimeFormat
     */
    public static DateTimeFormat getDateTimeYearFormatter() {
        return formatterYear;
    }

}
