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

import static org.joda.time.DateTime.now;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author John Sanda
 */
public class DateTimeServiceTest {

    private DateTimeService dateTimeService;

    private MetricsConfiguration configuration;

    @BeforeMethod
    public void initService() {
        configuration = new MetricsConfiguration();
        dateTimeService = new DateTimeService();
        dateTimeService.setConfiguration(configuration);
    }

    @Test
    public void getHourTimeSliceForRawData() {
        DateTime currentHour = dateTimeService.hour0().plusHours(2);
        DateTime currentTime = currentHour.plusMinutes(2);

        DateTime timeSlice = dateTimeService.getTimeSlice(currentTime, configuration.getRawTimeSliceDuration());

        assertEquals(timeSlice, currentHour, "The hour time slice for raw data is wrong");
    }

    @Test
    public void getMinuteTimeSliceForRawData() {
        configuration = new MetricsConfiguration();
        configuration.setRawTimeSliceDuration(Minutes.ONE.toStandardDuration());

        DateTime currentMinute = dateTimeService.hour0().plusHours(2).plusMinutes(3);

        DateTime timeSlice = dateTimeService.getTimeSlice(currentMinute.plusSeconds(27),
            configuration.getRawTimeSliceDuration());

        assertEquals(timeSlice, currentMinute, "The minute time slice for raw data is wrong");
    }

    @Test
    public void getHourTimeSliceForOneHourData() {
        DateTime currentHour = dateTimeService.hour0().plusHours(9);
        DateTime timeSlice = dateTimeService.getTimeSlice(currentHour, configuration.getOneHourTimeSliceDuration());
        DateTime expected = dateTimeService.hour0().plusHours(6);

        assertEquals(timeSlice, expected, "The hour time slice for one hour data is wrong");
    }

    @Test
    public void getMinuteTimeSliceForOneHourData() {
        configuration = new MetricsConfiguration();
        configuration.setOneHourTimeSliceDuration(Minutes.minutes(6).toStandardDuration());

        DateTime currentTime = dateTimeService.hour0().plusHours(2).plusMinutes(11);
        DateTime timeSlice = dateTimeService.getTimeSlice(currentTime, configuration.getOneHourTimeSliceDuration());
        DateTime expectedTime = dateTimeService.hour0().plusHours(2).plusMinutes(6);

        assertEquals(timeSlice, expectedTime, "The minute time slice for one hour data is wrong");
    }

    @Test
    public void getMinuteTimeSliceForSixHourData() {
        configuration = new MetricsConfiguration();
        configuration.setSixHourTimeSliceDuration(Minutes.minutes(24).toStandardDuration());

        DateTime currentHour = dateTimeService.hour0().plusHours(9).plusMinutes(12).plusSeconds(47);
        DateTime timeSlice = dateTimeService.getTimeSlice(currentHour, configuration.getSixHourTimeSliceDuration());
        DateTime expected = dateTimeService.hour0().plusHours(9);

        assertEquals(timeSlice, expected, "The hour time slice for six hour data is wrong");
    }

    @Test
    public void timestampBefore7DaysShouldBeInRawDataRange() {
        assertTrue(dateTimeService.isInRawDataRange(now().minusHours(1)), "1 hour ago should be in raw data range.");
        assertTrue(dateTimeService.isInRawDataRange(now().minusDays(1)), "1 day ago should be in raw data range.");
        assertTrue(dateTimeService.isInRawDataRange(now().minusDays(5)), "5 days ago should be in raw data range.");
    }

    @Test
    public void timestampAfter7DaysShouldNotBeInRawDataRange() {
        assertFalse(dateTimeService.isInRawDataRange(now().minusDays(7)), "7 days ago should not be in raw data range.");
        assertFalse(dateTimeService.isInRawDataRange(now().minusDays(7).minusSeconds(1)),
            "7 days and 1 second ago should not be in raw data range.");
    }

    @Test
    public void timestampeBefore2WeeksShouldBeIn1HourDataRange() {
        assertTrue(dateTimeService.isIn1HourDataRange(now().minusDays(7)), "7 days ago should be in 1 hour data range");
        assertTrue(dateTimeService.isIn1HourDataRange(now().minusDays(13)),
            "13 days ago should be in 1 hour data range");
    }

    @Test
    public void timestampAfter2WeeksShouldNotBeIn1HourDataRange() {
        assertFalse(dateTimeService.isIn1HourDataRange(now().minusDays(14).minusSeconds(1)),
            "2 weeks ago should not be in 1 hour data range");
        assertFalse(dateTimeService.isIn1HourDataRange(now().minusDays(15)),
            "15 days ago should not be in 1 hour data range");
    }

    @Test
    public void timestampBefore31DaysShouldBeIn6HourDataRange() {
        assertTrue(dateTimeService.isIn6HourDataRange(now().minusDays(14)),
            "14 days ago should be in 6 hour data range.");
        assertTrue(dateTimeService.isIn6HourDataRange(now().minusDays(30)),
            "30 days ago should be in 6 hour data range.");
    }

    @Test
    public void timestampAfter31DaysShouldNotBeIn6HourDataRange() {
        assertFalse(dateTimeService.isIn6HourDataRange(now().minusDays(31)),
            "31 days ago should not be in 6 hour data range.");
        assertFalse(dateTimeService.isIn6HourDataRange(now().minusDays(32)),
            "32 days ago should not be in 6 hour data range.");
    }

    @Test
    public void timestampBefore365DaysShouldBeIn24HourDataRange() {
        assertTrue(dateTimeService.isIn24HourDataRange(now().minusDays(31)),
            "31 days ago should be in 24 hour data range.");
        assertTrue(dateTimeService.isIn24HourDataRange(now().minusDays(364)),
            "364 days ago should be in 24 hour data range.");
    }

    @Test
    public void timestampAfter365DaysShouldNotBeIn24HourDataRange() {
        assertFalse(dateTimeService.isIn24HourDataRange(now().minusDays(365)),
            "365 days ago should not be in 24 hour data range.");
    }

}
