/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.metrics;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.Duration;
import org.joda.time.Period;

/**
 * Provides common DateTime utility functions.
 *
 * @author John Sanda
 */
public class DateTimeService {

    private DateTimeComparator dateTimeComparator = DateTimeComparator.getInstance();

    protected MetricsConfiguration configuration;

    public void setConfiguration(MetricsConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * This method is preferred over DateTime.now() and System.currentTimeMillis() because it provides a hook for tests.
     *
     * @return a DateTime object set to milliseconds.
     */
    public DateTime now() {
        return new DateTime(nowInMillis());
    }

    public long nowInMillis() {
        return System.currentTimeMillis();
    }

    public DateTime getTimeSlice(long timestamp, Duration duration) {
        return getTimeSlice(new DateTime(timestamp), duration);
    }

    public DateTime getTimeSlice(DateTime dt, Duration duration) {
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

    public boolean isInRawDataRange(DateTime dateTime) {
        return dateTimeComparator.compare(now().minus(configuration.getRawRetention()), dateTime) < 0;
    }

    public boolean isIn1HourDataRange(DateTime dateTime) {
        return dateTimeComparator.compare(now().minus(configuration.getOneHourRetention()), dateTime) < 0;
    }

    public boolean isIn6HourDataRange(DateTime dateTime) {
        return dateTimeComparator.compare(now().minus(configuration.getSixHourRetention()), dateTime) < 0;
    }

    public boolean isIn24HourDataRange(DateTime dateTime) {
        return dateTimeComparator.compare(now().minus(configuration.getTwentyFourHourRetention()), dateTime) < 0;
    }

    /**
     * @return A DateTime object rounded down to the start of the current hour. For example, if the current time is
     * 17:21:09, then 17:00:00 is returned.
     */
    public DateTime currentHour() {
        return getTimeSlice(now(), configuration.getRawTimeSliceDuration());
    }

    /**
     * The six hour time slices for a day are fixed - 00:00 to 06:00, 06:00 to 12:00, 12:00 to 18:00, 18:00 to 24:00.
     * This method determines the six hour time slice based on {@link #currentHour()} and returns the start of the time
     * slice.
     *
     * @return A DateTime object rounded down to the start of the current six hour time slice.
     */
    public DateTime current6HourTimeSlice() {
        return get6HourTimeSlice(currentHour());
    }

    /**
     * The 24 hour time slices are fix - 00:00 to 24:00. This method determines the 24 hour time slice based on
     * {@link #currentHour()} and returns the start of the time slice.
     *
     * @return A DateTime object rounded down to the start of the current 24 hour time slice.
     */
    public DateTime current24HourTimeSlice() {
        return get24HourTimeSlice(currentHour());
    }

    /**
     * This method determines the 24 hour time slice for the specified time and returns the start of that time slice.
     *
     * @param time The DateTime to be rounded down
     * @return A DateTime rounded down to the start of the 24 hour time slice in which the time parameter falls.
     * @see #current24HourTimeSlice()
     */
    public DateTime get24HourTimeSlice(DateTime time) {
        return getTimeSlice(time, configuration.getSixHourTimeSliceDuration());
    }

    /**
     * This method determines the six hour time slice for the specified time and returns the start of that time slice.
     *
     * @param time The DateTime to be rounded down
     * @return A DateTime rounded down to the start of the six hour time slice in which the time parameter falls.
     * @see #current6HourTimeSlice()
     */
    public DateTime get6HourTimeSlice(DateTime time) {
        return getTimeSlice(time, configuration.getOneHourTimeSliceDuration());
    }

    public DateTime get6HourTimeSliceEnd(DateTime time) {
        return get6HourTimeSlice(time).plus(configuration.getOneHourTimeSliceDuration());
    }

    /**
     * Determines if the current six hour time slice for the specified time has completed.
     * <p>
     * Suppose that the current time is 14:23 and that the specified time is 13:15:00 which falls into the
     * 12:00 - 18:00 time slice. In this case the time slice has not yet finished.
     * </p>
     * <p>
     * Now suppose that the current time is 12:24 and the specified time is 11:34 which falls into the 06:00 - 12:00
     * time slice. In this case the time slice has finished.
     * </p>
     *
     * @param time The DateTime to evaluate
     * @return true if the six hour time slice for the specified time has completed, false otherwise.
     */
    public boolean is6HourTimeSliceFinished(DateTime time) {
        return hasTimeSliceEnded(get6HourTimeSlice(time), configuration.getOneHourTimeSliceDuration());
    }

    /**
     * Determines if the current 24 hour time slice for the specified time has completed.
     * <p>
     * Suppose that the current time 22:12 Monday and that the specified time is 21:48. The current time slice, i.e.,
     * Monday, has not yet finished.
     * </p>
     * <p>
     * Now suppose that the current time is 01:13 Tuesday and that the specified time is 23:19 Monday. The time slice
     * for the specified time has finished.
     * </p>
     *
     * @param time The DateTime to evaluate
     * @return true if the 24 hour time slice for the specified time has completed, false otherwise.
     */
    public boolean is24HourTimeSliceFinished(DateTime time) {
        return hasTimeSliceEnded(get24HourTimeSlice(time), configuration.getSixHourTimeSliceDuration());
    }

    private boolean hasTimeSliceEnded(DateTime startTime, Duration duration) {
        DateTime endTime = startTime.plus(duration);
        return DateTimeComparator.getInstance().compare(currentHour(), endTime) >= 0;
    }

    public DateTime hour0() {
        DateTime rightNow = now();
        return rightNow.hourOfDay().roundFloorCopy().minusHours(
            rightNow.hourOfDay().roundFloorCopy().hourOfDay().get());
    }

}
