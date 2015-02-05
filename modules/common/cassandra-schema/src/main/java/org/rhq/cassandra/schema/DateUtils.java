package org.rhq.cassandra.schema;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Hours;
import org.joda.time.Period;

/**
 * @author John Sanda
 */
public class DateUtils {

    static {
        //[BZ 1161806]
        // Force the timezone to UTC to avoid any problems due to transitions
        // from DST to non-DST
        // This also solves the problem where an HA environment is distributed
        // in two different timezones
        DateTimeZone.setDefault(DateTimeZone.UTC);
    }

    private DateUtils() {
    }

    public static DateTime currentHour() {
        return getUTCTimeSlice(DateTime.now(), Hours.ONE.toStandardDuration());
    }

    public static DateTime get1HourTimeSlice(DateTime time) {
        return getUTCTimeSlice(time, Hours.ONE.toStandardDuration());
    }

    public static DateTime get6HourTimeSlice(DateTime time) {
        return getUTCTimeSlice(time, Hours.SIX.toStandardDuration());
    }

    public static DateTime get24HourTimeSlice(DateTime time) {
        return getUTCTimeSlice(time, Days.ONE.toStandardDuration());
    }

    public static DateTime getTimeSlice(DateTime dt, Duration duration) {
        Period p = duration.toPeriod();

        if (p.getYears() != 0) {
            return dt.yearOfEra().roundFloorCopy().minusYears(dt.getYearOfEra() % p.getYears());
        } else if (p.getMonths() != 0) {
            return dt.monthOfYear().roundFloorCopy().minusMonths((dt.getMonthOfYear() - 1) % p.getMonths());
        } else if (p.getWeeks() != 0) {
            return dt.weekOfWeekyear().roundFloorCopy().minusWeeks((dt.getWeekOfWeekyear() - 1) % p.getWeeks());
        } else if (p.getDays() != 0) {
            return dt.dayOfMonth().roundFloorCopy().minusDays((dt.getDayOfMonth() - 1) % p.getDays());
        } else if (p.getHours() != 0) {
            return dt.hourOfDay().roundFloorCopy().minusHours(dt.getHourOfDay() % p.getHours());
        } else if (p.getMinutes() != 0) {
            return dt.minuteOfHour().roundFloorCopy().minusMinutes(dt.getMinuteOfHour() % p.getMinutes());
        } else if (p.getSeconds() != 0) {
            return dt.secondOfMinute().roundFloorCopy().minusSeconds(dt.getSecondOfMinute() % p.getSeconds());
        }
        return dt.millisOfSecond().roundCeilingCopy().minusMillis(dt.getMillisOfSecond() % p.getMillis());
    }

    public static DateTime getUTCTimeSlice(DateTime dateTime, Duration duration) {
        return getTimeSlice(new DateTime(dateTime.getMillis(), DateTimeZone.UTC), duration);
    }

    public static DateTime plusDSTAware(DateTime dateTime, Duration duration) {
        //(BZ 1161806) Added code to adjust to the shifts in time due to
        // changes from DST to non-DST and the reverse.
        //
        // 1) When switching from DST to non-DST, the time after the
        // duration increment needs to be adjusted by a positive
        // one hour
        //
        // 2) When switching from non-DST to DST, the time after the
        // duration increment needs to be adjusted by a negative
        // one hour
        //
        // Note: this does not work if the duration is exactly one
        // hour because it will create an infinite loop when switching
        // from non-DST to DST times.

        if (duration.toPeriod().getHours() <= 1) {
            dateTime = dateTime.plus(duration);
        } else {
            DateTimeZone zone = dateTime.getZone();
            int beforeOffset = zone.getOffset(dateTime.getMillis());
            dateTime = dateTime.plus(duration);
            int afterOffset = zone.getOffset(dateTime.getMillis());
            dateTime = dateTime.plus(beforeOffset - afterOffset);
        }

        return dateTime;
    }
}
