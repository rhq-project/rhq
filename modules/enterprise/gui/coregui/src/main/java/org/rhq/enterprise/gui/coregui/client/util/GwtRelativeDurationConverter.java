package org.rhq.enterprise.gui.coregui.client.util;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.i18n.client.TimeZoneInfo;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;

/** An RPC gwt serializable implemenation of the jsf RelativeDurationConverter
 *  utility.
 *
 * Convert the passed value in to a relative date compared to now.
 * If the passed value is -1, the output will be empty (as a value of 0
 * returns the number of days since the epoch)
 * @author Joseph Marques
 * @author Simeon Pinder
 */
public class GwtRelativeDurationConverter {
    private static final long MILLIS_IN_SECOND = 1000L;
    private static final long MILLIS_IN_MINUTE = 60 * MILLIS_IN_SECOND;
    private static final long MILLIS_IN_HOUR = 60 * MILLIS_IN_MINUTE;
    private static final long NEVER = -1;

    private static final long MILLIS_IN_DAY = 24 * MILLIS_IN_HOUR;
    private static final DateTimeFormat dayFormatter = DateTimeFormat.getFormat("d");
    private static final DateTimeFormat formatter = DateTimeFormat.getFormat("hh:mm aaa Z");
    private static final Messages MSG = CoreGUI.getMessages();

    public static TimeZone tz = null;

    public static String format(long eventMillis) {
        long now = System.currentTimeMillis();
        int dayOfYearToday = Integer.parseInt(dayFormatter.format(new Date(now)));
        int dayOfYearEvent = Integer.parseInt(dayFormatter.format(new Date(eventMillis)));
        String result = null;
        //add additional check to fix when dayOfYear and dayOfYearToday is same but month apart.
        if ((dayOfYearEvent == dayOfYearToday) & ((now - eventMillis) < MILLIS_IN_DAY)) {
            // <time>
            result = formatter.format(new Date(eventMillis));
        } else if ((dayOfYearEvent == dayOfYearToday - 1) & ((now - eventMillis) < MILLIS_IN_DAY * 28)) {
            // "yesterday" <time>
            result = MSG.common_label_yesterday() + ", " + formatter.format(new Date(eventMillis));
        } else {
            // <time> "ago"
            result = getRelativeTimeAgo(eventMillis) + " " + MSG.common_label_ago();
        }
        return result;
    }

    private static TimeZone getTimeZone() {
        if (tz == null) {
            return TimeZone.createTimeZone(TimeZoneInfo.buildTimeZoneData(""));
        } else {
            return tz;
        }
    }

    private static String getRelativeTimeAgo(long millis) {
        StringBuilder buf = new StringBuilder();
        long timeAgo = System.currentTimeMillis() - millis;

        int count = 0;
        int days = (int) (timeAgo / MILLIS_IN_DAY);
        timeAgo %= MILLIS_IN_DAY;
        if (days > 0) {
            count++;
            buf.append(days).append(" day");
            if (days != 1)
                buf.append("s");
        }

        int hours = (int) (timeAgo / MILLIS_IN_HOUR);
        timeAgo %= MILLIS_IN_HOUR;
        if (hours > 0) {
            count++;
            if (buf.length() > 0)
                buf.append(", ");
            buf.append(hours).append(" hour");
            if (hours != 1)
                buf.append("s");
        }

        if (count < 2) {
            int mins = (int) (timeAgo / MILLIS_IN_MINUTE);
            timeAgo %= MILLIS_IN_MINUTE;
            if (mins > 0) {
                if (buf.length() > 0)
                    buf.append(", ");
                buf.append(mins).append(" minute");
                if (mins != 1)
                    buf.append("s");
            }
        }

        return buf.toString();
    }

}
