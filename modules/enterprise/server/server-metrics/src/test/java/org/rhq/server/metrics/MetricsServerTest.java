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

import static java.util.Arrays.asList;
import static org.joda.time.DateTime.now;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.Bucket;
import org.rhq.server.metrics.domain.RawNumericMetric;

/**
 * @author John Sanda
 */
public class MetricsServerTest extends MetricsTest {

    private static final double TEST_PRECISION = Math.pow(10, -9);

    private MetricsServer metricsServer;

    @BeforeMethod
    public void initServer() throws Exception {
        metricsServer = new MetricsServer();
        metricsServer.setConfiguration(configuration);

        metricsServer.setDateTimeService(dateTimeService);

        metricsServer.setDAO(dao);
        metricsServer.init();

        purgeDB();
    }

    @Test
    public void insertMultipleRawNumericDataForOneSchedule() throws Exception {
        int scheduleId = 123;

        DateTime currentTime = hour(4).plusMinutes(44);
        DateTime threeMinutesAgo = currentTime.minusMinutes(3);
        DateTime twoMinutesAgo = currentTime.minusMinutes(2);
        DateTime oneMinuteAgo = currentTime.minusMinutes(1);

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(threeMinutesAgo.getMillis(), scheduleId, 3.2));
        data.add(new MeasurementDataNumeric(twoMinutesAgo.getMillis(), scheduleId, 3.9));
        data.add(new MeasurementDataNumeric(oneMinuteAgo.getMillis(), scheduleId, 2.6));

        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.size());

        dateTimeService.setNow(currentTime);
        metricsServer.addNumericData(data, waitForRawInserts);

        waitForRawInserts.await("Failed to insert raw data");

        List<RawNumericMetric> actual = Lists.newArrayList(dao.findRawMetrics(scheduleId, hour(4)
            .getMillis(), hour(5).getMillis()));
        List<RawNumericMetric> expected = asList(
            new RawNumericMetric(scheduleId, threeMinutesAgo.getMillis(), 3.2),
            new RawNumericMetric(scheduleId, twoMinutesAgo.getMillis(), 3.9),
            new RawNumericMetric(scheduleId, oneMinuteAgo.getMillis(), 2.6)
        );

        assertEquals(actual, expected, "Failed to retrieve raw metric data");
        assertRawIndexEquals(hour(4), asList(scheduleId));
    }

    @Test
    public void insertRawDataForMultipleSchedules() throws Exception {
        int scheduleId1 = 123;
        int scheduleId2 = 148;
        int scheduleId3 = 176;
        int scheduleId4 = 177;
        Set<MeasurementDataNumeric> data = ImmutableSet.of(
            new MeasurementDataNumeric(hour(5).plusMinutes(2).getMillis(), scheduleId1, 3.14),
            new MeasurementDataNumeric(hour(5).plusMinutes(3).getMillis(), scheduleId2, 3.14),
            new MeasurementDataNumeric(hour(5).plusMinutes(3).getMillis(), scheduleId3, 3.14),
            new MeasurementDataNumeric(hour(5).plusMinutes(4).getMillis(), scheduleId4, 3.14)
        );
        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.size());

        metricsServer.setIndexPartitions(2);
        dateTimeService.setNow(hour(5).plusMinutes(5));
        metricsServer.addNumericData(data, waitForRawInserts);
        waitForRawInserts.await("Failed to insert raw data");

        RawNumericMetric expected1 = new RawNumericMetric(scheduleId1, hour(5).plusMinutes(2).getMillis(), 3.14);
        RawNumericMetric expected2 = new RawNumericMetric(scheduleId2, hour(5).plusMinutes(3).getMillis(), 3.14);
        RawNumericMetric expected3 = new RawNumericMetric(scheduleId3, hour(5).plusMinutes(3).getMillis(), 3.14);
        RawNumericMetric expected4 = new RawNumericMetric(scheduleId4, hour(5).plusMinutes(4).getMillis(), 3.14);

        assertRawDataEquals(scheduleId1, hour(5), hour(6), expected1);
        assertRawDataEquals(scheduleId2, hour(5), hour(6), expected2);
        assertRawDataEquals(scheduleId3, hour(5), hour(6), expected3);
        assertRawDataEquals(scheduleId4, hour(5), hour(6), expected4);

        assertRawIndexEquals(hour(5), asList(scheduleId2, scheduleId3, scheduleId1, scheduleId4));
    }

    @Test
    public void insertLateData() throws Exception {
        int scheduleId1 = 123;
        int scheduleId2 = 145;
        int scheduleId3 = 184;
        int scheduleId4 = 149;
        Set<MeasurementDataNumeric> data = ImmutableSet.of(
            new MeasurementDataNumeric(yesterday().plusHours(19).plusMinutes(39).getMillis(), scheduleId1, 2.17),
            new MeasurementDataNumeric(yesterday().plusHours(19).plusMinutes(51).getMillis(), scheduleId2, 85.0),
            new MeasurementDataNumeric(hour(4).plusMinutes(51).getMillis(), scheduleId4, 22.5),
            new MeasurementDataNumeric(hour(5).plusMinutes(11).getMillis(), scheduleId3, 3.14)

        );
        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.size());

        dateTimeService.setNow(hour(5).plusMinutes(12));
        metricsServer.addNumericData(data, waitForRawInserts);
        waitForRawInserts.await("Failed to insert raw data");

        RawNumericMetric expected1 = new RawNumericMetric(scheduleId1, yesterday().plusHours(19).plusMinutes(39)
            .getMillis(), 2.17);
        RawNumericMetric expected2 = new RawNumericMetric(scheduleId2, yesterday().plusHours(19).plusMinutes(51)
            .getMillis(), 85.0);
        RawNumericMetric expected3 = new RawNumericMetric(scheduleId3, hour(5).plusMinutes(11).getMillis(), 3.14);
        RawNumericMetric expected4 = new RawNumericMetric(scheduleId4, hour(4).plusMinutes(51).getMillis(), 22.5);

        assertRawDataEquals(scheduleId1, yesterday().plusHours(19), yesterday().plusHours(20), expected1);
        assertRawDataEquals(scheduleId2, yesterday().plusHours(19), yesterday().plusHours(20), expected2);
        assertRawDataEquals(scheduleId3, hour(5), hour(6), expected3);
        assertRawDataEquals(scheduleId4, hour(4), hour(5), expected4);
    }

    @Test
    public void doNotInsertDataThatIsTooOld() throws Exception {
        int scheduleId = 123;
        Set<MeasurementDataNumeric> data = ImmutableSet.of(new MeasurementDataNumeric(
            today().minusDays(8).plusHours(5).minusHours(25).getMillis(), scheduleId, 3.14));
        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.size());

        dateTimeService.setNow(hour(5).plusMinutes(2));
        metricsServer.addNumericData(data, waitForRawInserts);
        waitForRawInserts.await("Failed to insert raw data");

        assertRawDataEmpty(scheduleId, hour(5).minusHours(25), hour(5).minusHours(24));
    }

    @Test
    public void calculateAggregatesForOneScheduleWhenDBIsEmpty() throws Exception {
        int scheduleId = 123;

        DateTime hour0 = hour0();
        DateTime hour6 = hour0.plusHours(6);
        DateTime lastHour = hour6.minusHours(1);
        DateTime firstMetricTime = hour6.minusMinutes(3);
        DateTime secondMetricTime = hour6.minusMinutes(2);
        DateTime thirdMetricTime = hour6.minusMinutes(1);

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(firstMetricTime.getMillis(), scheduleId, 3.2));
        data.add(new MeasurementDataNumeric(secondMetricTime.getMillis(), scheduleId, 3.9));
        data.add(new MeasurementDataNumeric(thirdMetricTime.getMillis(), scheduleId, 2.6));

        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.size());

        dateTimeService.setNow(hour(7));
        metricsServer.addNumericData(data, waitForRawInserts);
        waitForRawInserts.await("Failed to insert raw data");

        dateTimeService.setNow(hour(8));

        metricsServer.calculateAggregates();

        // verify that one hour metric data is updated
        List<AggregateNumericMetric> expected = asList(new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR,
            divide((3.9 + 3.2 + 2.6), 3), 2.6, 3.9, lastHour.getMillis()));
        assertMetricDataEquals(scheduleId, Bucket.ONE_HOUR, expected);

        // verify that 6 hour metric data is updated
        assertMetricDataEquals(scheduleId, Bucket.SIX_HOUR, asList(new AggregateNumericMetric(scheduleId,
            Bucket.SIX_HOUR, divide((3.9 + 3.2 + 2.6), 3), 2.6, 3.9, hour0.getMillis())));
    }

    @Test
    public void aggregateRawDataDuring9thHour() throws Exception {
        int scheduleId = 123;

        DateTime hour0 = hour0();
        DateTime hour9 = hour0.plusHours(9);
        DateTime hour8 = hour9.minusHours(1);

        DateTime firstMetricTime = hour8.plusMinutes(5);
        DateTime secondMetricTime = hour8.plusMinutes(10);
        DateTime thirdMetricTime = hour8.plusMinutes(15);

        double firstValue = 1.1;
        double secondValue = 2.2;
        double thirdValue = 3.3;

        dateTimeService.setNow(hour(8).plusMinutes(55));

        insertRawData(
            new MeasurementDataNumeric(firstMetricTime.getMillis(), scheduleId, firstValue),
            new MeasurementDataNumeric(secondMetricTime.getMillis(), scheduleId, secondValue),
            new MeasurementDataNumeric(thirdMetricTime.getMillis(), scheduleId, thirdValue)
        );

        dateTimeService.setNow(hour(10));

        metricsServer.calculateAggregates();

        // verify that the 1 hour aggregates are calculated
        List<AggregateNumericMetric> expected = asList(
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, divide((1.1 + 2.2 + 3.3), 3),
                firstValue, thirdValue, hour8.getMillis()));
        assertMetricDataEquals(scheduleId, Bucket.ONE_HOUR, expected);

        // The 6 hour data should not get aggregated since the current 6 hour time slice
        // has not passed yet. More specifically, the aggregation job is running at 09:00
        // which means that the current 6 hour slice is from 06:00 to 12:00.
        assert6HourDataEmpty(scheduleId);
    }

    /**
     * This test exercises the scenario in which there is raw data from the past hour to be
     * aggregated as well as from an earlier period. This could happen in the event of a
     * server shutdown. Suppose that the most recently aggregated data is from hour 9 and
     * that the current hour is 15. This means we had a server shutdown some time during
     * hour 10 which also mean we could have raw data in the 10:00 hour in addition to the
     * previous hour that need to be aggregated.
     */
    @Test(enabled = false)
    public void runAggregationIn15thHourAfterServerOutage() throws Exception {
        int scheduleId = 123;

        dateTimeService.setNow(hour(11));

        Set<MeasurementDataNumeric> data = ImmutableSet.of(
            new MeasurementDataNumeric(hour(10).plusMinutes(5).getMillis(), scheduleId, 5.0),
            new MeasurementDataNumeric(hour(10).plusMinutes(10).getMillis(), scheduleId, 10.0),
            new MeasurementDataNumeric(hour(10).plusMinutes(15).getMillis(), scheduleId, 15.0)
        );
        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.size());
        metricsServer.addNumericData(data, waitForRawInserts);
        waitForRawInserts.await("Failed to insert raw data");

        // now after the server starts back up in the 14th hour,
        //
        //  2) re-initialize the metrics server
        //  3) insert some more raw data
        dateTimeService.setNow(hour(14).plusMinutes(55));

        metricsServer.init();

        data = ImmutableSet.of(
            new MeasurementDataNumeric(hour(14).plusMinutes(20).getMillis(), scheduleId, 3.0),
            new MeasurementDataNumeric(hour(14).plusMinutes(25).getMillis(), scheduleId, 5.0),
            new MeasurementDataNumeric(hour(14).plusMinutes(30).getMillis(), scheduleId, 13.0)
        );
        waitForRawInserts = new WaitForRawInserts(data.size());
        metricsServer.addNumericData(data, waitForRawInserts);
        waitForRawInserts.await("Failed to insert raw data");

        // Now let's assume we have reached the top of the hour and run the scheduled
        // aggregation.
        dateTimeService.setNow(hour(16));

        metricsServer.calculateAggregates();

        // verify that we have one hour aggregates
        double hour10Avg = divide(5.0 + 10.0 + 15.0, 3);
        double hour10Min = 5.0;
        double hour10Max = 15.0;

        double hour14Avg = divide(3.0 + 5.0 + 13.0, 3);
        double hour14Min = 3.0;
        double hour14Max = 13.0;

        List<AggregateNumericMetric> expectedOneHourData = asList(
            // add aggregate for hour 10
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, hour10Avg, hour10Min, hour10Max, hour(10).getMillis()),
            // add aggregate for hour 14
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, hour14Avg, hour14Min, hour14Max, hour(14).getMillis())
        );
        assert1HourDataEquals(scheduleId, expectedOneHourData);
//        assert1HourCacheEquals(hour(12), startScheduleId(scheduleId), expectedOneHourData.subList(1, 2));

        // verify that we have 6 hour aggregates for hour 6. The data from the
        // 10:00 hour falls into the 6:00 - 12:00 time slice so we should have
        // a 6 hour aggregate.
        List<AggregateNumericMetric> expected6HourData = asList(new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR,
            hour10Avg, hour10Min, hour10Max, hour(6).getMillis()));
        assert6HourDataEquals(scheduleId, expected6HourData);
//        assert6HourCacheEquals(hour(0), startScheduleId(scheduleId), expected6HourData);
    }

    @Test(enabled = false)
    public void runAggregationIn8thHourAfterServerOutageFromPreviousDay() throws Exception {
        int scheduleId = 123;
        DateTime hour20Yesterday = hour0().minusHours(4);
        DateTime hour18Yesterday = hour0().minusHours(6);
        DateTime hour0Yesterday = hour0().minusDays(1);

        // insert data before server shutdown
        dateTimeService.setNow(hour20Yesterday.plusMinutes(55));

        Set<MeasurementDataNumeric> data = ImmutableSet.of(
            new MeasurementDataNumeric(hour20Yesterday.plusMinutes(5).getMillis(), scheduleId, 7.0),
            new MeasurementDataNumeric(hour20Yesterday.plusMinutes(10).getMillis(), scheduleId, 2.5),
            new MeasurementDataNumeric(hour20Yesterday.plusMinutes(15).getMillis(), scheduleId, 4.0)
        );
        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.size());
        metricsServer.addNumericData(data, waitForRawInserts);
        waitForRawInserts.await("Failed to insert raw data");

        // now after the server starts back up in the 8th hour,
        //
        //  2) re-initialize the metrics server
        //  3) insert some more raw data
        dateTimeService.setNow(hour(8).plusMinutes(55));

        metricsServer.init();

        data = ImmutableSet.of(
            new MeasurementDataNumeric(hour(8).plusMinutes(20).getMillis(), scheduleId, 8.0),
            new MeasurementDataNumeric(hour(8).plusMinutes(25).getMillis(), scheduleId, 16.0),
            new MeasurementDataNumeric(hour(8).plusMinutes(30).getMillis(), scheduleId, 3.0)
        );
        waitForRawInserts = new WaitForRawInserts(data.size());
        metricsServer.addNumericData(data, waitForRawInserts);
        waitForRawInserts.await("Failed to insert raw data");

        // Now let's assume we have reached the top of the hour and run the scheduled
        // aggregation.
        dateTimeService.setNow(hour(10));

        metricsServer.calculateAggregates();

        // verify that we have one hour aggregates
        double hour20YesterdayAvg = divide(7.0 + 2.5 + 4.0, 3);
        double hour20YesterdayMin = 2.5;
        double hour20YesterdayMax = 7.0;

        double hour8Avg = divide(8.0 + 16.0 + 3.0, 3);
        double hour8Min = 3.0;
        double hour8Max = 16.0;

        List<AggregateNumericMetric> expectedOneHourData = asList(
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, hour20YesterdayAvg, hour20YesterdayMin, hour20YesterdayMax, hour20Yesterday.getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, hour8Avg, hour8Min, hour8Max, hour(8).getMillis())
        );

//        assert1HourDataEquals(scheduleId, expectedOneHourData);
//        assert1HourCacheEquals(hour(6), startScheduleId(scheduleId), asList(new AggregateNumericMetric(scheduleId,
//            hour8Avg, hour8Min, hour8Max, hour(8).getMillis())));

        // verify that we a 6 hour aggregate for the previous day's 18:00 - 00:00
        // time slice
        List<AggregateNumericMetric> expected6HourData = asList(new AggregateNumericMetric(scheduleId, Bucket.SIX_HOUR,
            hour20YesterdayAvg, hour20YesterdayMin, hour20YesterdayMax, hour18Yesterday.getMillis()));
        assert6HourDataEquals(scheduleId, expected6HourData);
//        assert6HourCacheEmpty(hour(0).minusDays(24), startScheduleId(scheduleId));

        // verify that we have a 24 hour aggregate for the previous day's data
        assert24HourDataEquals(scheduleId, asList(new AggregateNumericMetric(scheduleId, Bucket.TWENTY_FOUR_HOUR, hour20YesterdayAvg,
            hour20YesterdayMin, hour20YesterdayMax, hour0Yesterday.getMillis())));
    }

    @Test
    public void findRawDataCompositesForResource() throws Exception {
        DateTime beginTime = now().minusHours(4);
        DateTime endTime = now();
        Buckets buckets = new Buckets(beginTime, endTime);
        int scheduleId = 123;

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 10, scheduleId, 1.1));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 20, scheduleId, 2.2));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 30, scheduleId, 3.3));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 10, scheduleId, 4.4));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 20, scheduleId, 5.5));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 30, scheduleId, 6.6));

        // add some data outside the range
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() - 100, scheduleId, 1.23));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + buckets.getInterval() + 50, scheduleId,
            4.56));

        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.size());

        metricsServer.addNumericData(data, waitForRawInserts);
        waitForRawInserts.await("Failed to insert raw data");

        List<MeasurementDataNumericHighLowComposite> actualData = Lists.newArrayList(metricsServer.findDataForResource(
            scheduleId, beginTime.getMillis(), endTime.getMillis(),60));

        assertEquals(actualData.size(), buckets.getNumDataPoints(), "Expected to get back 60 data points.");

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0).getStartTime(), divide(1.1 + 2.2 + 3.3, 3), 3.3, 1.1);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59).getStartTime(), divide(4.4 + 5.5 + 6.6, 3), 6.6, 4.4);
        MeasurementDataNumericHighLowComposite expectedBucket29Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(29).getStartTime(), Double.NaN, Double.NaN, Double.NaN);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actualData.get(0));
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actualData.get(59));
        assertPropertiesMatch("The data for bucket 29 does not match the expected values.", expectedBucket29Data,
            actualData.get(29));
    }

    @Test
    public void findLatestValueForResource() throws Exception {
        int scheduleId = 123;

        DateTime fifteenMinutesAgo = now().minusMinutes(15);
        DateTime tenMinutesAgo = now().minusMinutes(10);
        DateTime fiveMinutesAgo = now().minusMinutes(5);

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(fifteenMinutesAgo.getMillis(), scheduleId, 1.1));
        data.add(new MeasurementDataNumeric(tenMinutesAgo.getMillis(), scheduleId, 2.2));
        data.add(new MeasurementDataNumeric(fiveMinutesAgo.getMillis(), scheduleId, 3.3));

        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.size());

        metricsServer.addNumericData(data, waitForRawInserts);

        waitForRawInserts.await("Failed to insert raw data");

        RawNumericMetric actual = metricsServer.findLatestValueForResource(scheduleId);
        RawNumericMetric expected = new RawNumericMetric(scheduleId, fiveMinutesAgo.getMillis(), 3.3);

        assertEquals(actual, expected, "Failed to find latest metric value for resource");
    }

    @Test
    public void getSummaryRawAggregateForResource() throws Exception {
        DateTime beginTime = now().minusHours(4);
        DateTime endTime = now();
        Buckets buckets = new Buckets(beginTime, endTime);
        int scheduleId = 123;

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 10, scheduleId, 1.1));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 20, scheduleId, 2.2));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 30, scheduleId, 3.3));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 10, scheduleId, 4.4));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 20, scheduleId, 5.5));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 30, scheduleId, 6.6));

        // add some data outside the range
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() - 100, scheduleId, 1.23));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + buckets.getInterval() + 50, scheduleId,
            4.56));

        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.size());

        metricsServer.addNumericData(data, waitForRawInserts);

        waitForRawInserts.await("Failed to insert raw data");

        AggregateNumericMetric actual = metricsServer.getSummaryAggregate(scheduleId, beginTime.getMillis(),
            endTime.getMillis());
        double avg = divide(1.1 + 2.2 + 3.3 + 4.4 + 5.5 + 6.6, 6);
        AggregateNumericMetric expected = new AggregateNumericMetric(0, Bucket.ONE_HOUR, avg, 1.1, 6.6,
            beginTime.getMillis());

        assertPropertiesMatch("Failed to get resource summary aggregate for raw data.", expected, actual,
            TEST_PRECISION);
    }

    @Test
    public void getSummary1HourAggregateForResource() throws Exception {
        DateTime beginTime = now().minusDays(11);
        DateTime endTime = now();

        Buckets buckets = new Buckets(beginTime, endTime);
        DateTime bucket0Time = new DateTime(buckets.get(0).getStartTime());
        DateTime bucket59Time = new DateTime(buckets.get(59).getStartTime());

        int scheduleId = 123;
        List<AggregateNumericMetric> metrics = asList(
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, 2.0, 1.0, 3.0, bucket0Time.getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, 5.0, 4.0, 6.0, bucket0Time.plusHours(1).getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, 3.0, 3.0, 3.0, bucket0Time.plusHours(2).getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, 5.0, 2.0, 9.0, bucket59Time.getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, 5.0, 4.0, 6.0, bucket59Time.plusHours(1).getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, 3.0, 3.0, 3.0, bucket59Time.plusHours(2).getMillis())
        );
        for (AggregateNumericMetric metric : metrics) {
            insert1HourData(metric).get();
        }

        AggregateNumericMetric actual = metricsServer.getSummaryAggregate(scheduleId, beginTime.getMillis(),
            endTime.getMillis());
        double avg = divide(2.0 + 5.0 + 3.0 + 5.0 + 5.0 + 3.0, 6);
        AggregateNumericMetric expected = new AggregateNumericMetric(0, Bucket.ONE_HOUR, avg, 1.0, 9.0,
            beginTime.getMillis());

        assertPropertiesMatch("Failed to get resource summary aggregate for one hour data", expected, actual,
            TEST_PRECISION);
    }

    @Test
    public void getSummaryAggregateForGroup() throws Exception {
        DateTime beginTime = now().minusDays(11);
        DateTime endTime = now();

        Buckets buckets = new Buckets(beginTime, endTime);
        DateTime bucket0Time = new DateTime(buckets.get(0).getStartTime());
        DateTime bucket59Time = new DateTime(buckets.get(59).getStartTime());

        int scheduleId1 = 123;
        int scheduleId2 = 456;

        List<AggregateNumericMetric> metrics = asList(
            new AggregateNumericMetric(scheduleId1, Bucket.ONE_HOUR, 1.1, 1.1, 1.1, bucket0Time.getMillis()),
            new AggregateNumericMetric(scheduleId2, Bucket.ONE_HOUR, 1.2, 1.2, 1.2, bucket0Time.getMillis()),

            new AggregateNumericMetric(scheduleId1, Bucket.ONE_HOUR, 5.1, 5.1, 5.1, bucket59Time.getMillis()),
            new AggregateNumericMetric(scheduleId2, Bucket.ONE_HOUR, 5.2, 5.2, 5.2, bucket59Time.getMillis())
        );
        for (AggregateNumericMetric metric : metrics) {
            insert1HourData(metric).get();
        }

        AggregateNumericMetric actual = metricsServer.getSummaryAggregate(asList(scheduleId1, scheduleId2),
            beginTime.getMillis(), endTime.getMillis());
        double avg = divide(1.1 + 1.2 + 5.1 + 5.2, 4);
        AggregateNumericMetric expected = new AggregateNumericMetric(0, Bucket.ONE_HOUR, avg, 1.1, 5.2,
            beginTime.getMillis());

//        assertEquals(actual, expected, "Failed to get group summary aggregate for one hour data");
        assertPropertiesMatch("Failed to get group summary aggregate for one hour data", expected, actual,
            TEST_PRECISION);
    }

    @Test
    public void getSummaryRawAggregateForGroup() throws Exception {
        DateTime beginTime = now().minusHours(4);
        DateTime endTime = now();
        Buckets buckets = new Buckets(beginTime, endTime);
        int scheduleId1 = 123;
        int scheduleId2 = 456;

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 10, scheduleId1, 1.1));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 10, scheduleId2, 1.2));

        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 20, scheduleId1, 2.1));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 20, scheduleId2, 2.2));

        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 30, scheduleId1, 3.1));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 30, scheduleId2, 3.2));

        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 10, scheduleId1, 4.1));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 10, scheduleId2, 4.2));

        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 20, scheduleId1, 5.1));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 20, scheduleId2, 5.2));

        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 30, scheduleId1, 6.1));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 30, scheduleId2, 6.2));

        // add some data outside the range
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() - 100, scheduleId1, 1.23));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() - 100, scheduleId2, 2.23));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + buckets.getInterval() + 50, scheduleId1,
            4.56));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + buckets.getInterval() + 50, scheduleId2,
            4.56));

        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.size());

        metricsServer.addNumericData(data, waitForRawInserts);

        waitForRawInserts.await("Failed to insert raw data");

        AggregateNumericMetric actual = metricsServer.getSummaryAggregate(asList(scheduleId1, scheduleId2),
            beginTime.getMillis(), endTime.getMillis());

        double avg = divide(1.1 + 1.2 + 2.1 + 2.2 + 3.1 + 3.2 + 4.1 + 4.2 + 5.1 + 5.2 + 6.1 + 6.2, 12);
        AggregateNumericMetric expected = new AggregateNumericMetric(0, Bucket.ONE_HOUR, avg, 1.1, 6.2, beginTime.getMillis());

        assertPropertiesMatch("Failed to get group summary aggregate for raw data.", expected, actual,
            TEST_PRECISION);
    }

    @Test
    public void findRawDataCompositesForGroup() throws Exception {
        DateTime beginTime = now().minusHours(4);
        DateTime endTime = now();
        Buckets buckets = new Buckets(beginTime, endTime);
        int scheduleId1 = 123;
        int scheduleId2 = 456;

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 10, scheduleId1, 1.1));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 10, scheduleId2, 1.2));

        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 20, scheduleId1, 2.1));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 20, scheduleId2, 2.2));

        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 30, scheduleId1, 3.1));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 30, scheduleId2, 3.2));

        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 10, scheduleId1, 4.1));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 10, scheduleId2, 4.2));

        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 20, scheduleId1, 5.1));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 20, scheduleId2, 5.2));

        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 30, scheduleId1, 6.1));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 30, scheduleId2, 6.2));

        // add some data outside the range
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() - 100, scheduleId1, 1.23));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() - 100, scheduleId2, 2.23));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + buckets.getInterval() + 50, scheduleId1,
            4.56));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + buckets.getInterval() + 50, scheduleId2,
            4.56));

        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.size());

        metricsServer.addNumericData(data, waitForRawInserts);

        waitForRawInserts.await("Failed to insert raw data");

        List<MeasurementDataNumericHighLowComposite> actualData = metricsServer.findDataForGroup(
            asList(scheduleId1, scheduleId2), beginTime.getMillis(), endTime.getMillis(),60);

        assertEquals(actualData.size(), buckets.getNumDataPoints(), "Expected to get back 60 data points.");

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0).getStartTime(), divide(1.1 + 1.2 + 2.1 + 2.2 + 3.1 + 3.2, 6), 3.2, 1.1);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59).getStartTime(), divide(4.1 + 4.2 + 5.1 + 5.2 + 6.1 + 6.2, 6), 6.2, 4.1);
        MeasurementDataNumericHighLowComposite expectedBucket29Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(29).getStartTime(), Double.NaN, Double.NaN, Double.NaN);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actualData.get(0));
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actualData.get(59));
        assertPropertiesMatch("The data for bucket 29 does not match the expected values.", expectedBucket29Data,
            actualData.get(29));
    }

    @Test
    public void find1HourDataComposites() throws Exception {
        DateTime beginTime = now().minusDays(11);
        DateTime endTime = now();

        Buckets buckets = new Buckets(beginTime, endTime);
        DateTime bucket0Time = new DateTime(buckets.get(0).getStartTime());
        DateTime bucket59Time = new DateTime(buckets.get(59).getStartTime());

        int scheduleId = 123;
        List<AggregateNumericMetric> metrics = asList(
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, 2.0, 1.0, 3.0, bucket0Time.getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, 5.0, 4.0, 6.0, bucket0Time.plusHours(1).getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, 3.0, 3.0, 3.0, bucket0Time.plusHours(2).getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, 5.0, 2.0, 9.0, bucket59Time.getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, 5.0, 4.0, 6.0, bucket59Time.plusHours(1).getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.ONE_HOUR, 3.0, 3.0, 3.0, bucket59Time.plusHours(2).getMillis())
        );
        for (AggregateNumericMetric metric : metrics) {
            insert1HourData(metric).get();
        }

        List<MeasurementDataNumericHighLowComposite> actualData = Lists.newArrayList(metricsServer.findDataForResource(
            scheduleId, beginTime.getMillis(), endTime.getMillis(),60));

        assertEquals(actualData.size(), buckets.getNumDataPoints(), "Expected to get back 60 data points.");

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0).getStartTime(), divide(2.0 + 5.0 + 3.0, 3), 6.0, 1.0);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59).getStartTime(), divide(5.0 + 5.0 + 3.0, 3), 9.0, 2.0);
        MeasurementDataNumericHighLowComposite expectedBucket29Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(29).getStartTime(), Double.NaN, Double.NaN, Double.NaN);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actualData.get(0), TEST_PRECISION);
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actualData.get(59), TEST_PRECISION);
        assertPropertiesMatch("The data for bucket 29 does not match the expected values.", expectedBucket29Data,
            actualData.get(29), TEST_PRECISION);
    }

    @Test
    public void find6HourDataCompositesHavingInvalidMax() throws Exception {
        DateTime beginTime = now().minusDays(30);
        DateTime endTime = now();

        Buckets buckets = new Buckets(beginTime, endTime);
        DateTime bucket0Time = new DateTime(buckets.get(0).getStartTime());
        DateTime bucket59Time = new DateTime(buckets.get(59).getStartTime());

        int scheduleId = 123;
        List<AggregateNumericMetric> metrics = asList(
            new AggregateNumericMetric(scheduleId, Bucket.SIX_HOUR, 3.0, 1.0, 2.9, bucket0Time.getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.SIX_HOUR, 5.0, 4.0, 6.0, bucket0Time.plusHours(1).getMillis())
        );
        for (AggregateNumericMetric metric : metrics) {
            insert6HourData(metric).get();
        }

        List<MeasurementDataNumericHighLowComposite> actualData = Lists.newArrayList(metricsServer.findDataForResource(
            scheduleId, beginTime.getMillis(), endTime.getMillis(), 60));

        assertEquals(actualData.size(), buckets.getNumDataPoints(), "Expected to get back 60 data points.");

        MeasurementDataNumericHighLowComposite expectedBucket0 = new MeasurementDataNumericHighLowComposite(
            buckets.get(0).getStartTime(), 5.0, 6.0, 4.0);

        assertPropertiesMatch("The data for bucket 0 does not match expected values", expectedBucket0,
            actualData.get(0), TEST_PRECISION);
    }

    @Test
    public void find1HourDatCompositesForGroup() throws Exception {
        DateTime beginTime = now().minusDays(11);
        DateTime endTime = now();

        Buckets buckets = new Buckets(beginTime, endTime);
        DateTime bucket0Time = new DateTime(buckets.get(0).getStartTime());
        DateTime bucket59Time = new DateTime(buckets.get(59).getStartTime());

        int scheduleId1 = 123;
        int scheduleId2 = 456;

        List<AggregateNumericMetric> metrics = asList(
            new AggregateNumericMetric(scheduleId1, Bucket.ONE_HOUR, 1.1, 1.1, 1.1, bucket0Time.getMillis()),
            new AggregateNumericMetric(scheduleId2, Bucket.ONE_HOUR, 1.2, 1.2, 1.2, bucket0Time.getMillis()),

            new AggregateNumericMetric(scheduleId1, Bucket.ONE_HOUR, 3.1, 3.1, 3.1, bucket0Time.plusHours(2).getMillis()),
            new AggregateNumericMetric(scheduleId2, Bucket.ONE_HOUR, 3.2, 3.2, 3.2, bucket0Time.plusHours(2).getMillis()),

            new AggregateNumericMetric(scheduleId1, Bucket.ONE_HOUR, 4.1, 4.1, 4.1, bucket59Time.getMillis()),
            new AggregateNumericMetric(scheduleId2, Bucket.ONE_HOUR, 4.2, 4.2, 4.2, bucket59Time.getMillis())
        );
        for (AggregateNumericMetric metric : metrics) {
            insert1HourData(metric).get();
        }

        List<MeasurementDataNumericHighLowComposite> actual = metricsServer.findDataForGroup(
            asList(scheduleId1, scheduleId2), beginTime.getMillis(), endTime.getMillis(),60);

        assertEquals(actual.size(), buckets.getNumDataPoints(), "Expected to get back " + buckets.getNumDataPoints() +
            " data points.");

        MeasurementDataNumericHighLowComposite expectedBucket0 = new MeasurementDataNumericHighLowComposite(
            buckets.get(0).getStartTime(), divide(1.1 + 1.2 + 3.1 + 3.2, 4), 3.2, 1.1);
        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0,
            actual.get(0));

        MeasurementDataNumericHighLowComposite expectedBucket59 = new MeasurementDataNumericHighLowComposite(
            buckets.get(59).getStartTime(), divide(4.1 + 4.2, 2), 4.2, 4.1);
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59,
            actual.get(59));
    }

    @Test
    public void find6HourDataComposites() throws Exception {
        DateTime beginTime = now().minusDays(20);
        DateTime endTime = now();

        Buckets buckets = new Buckets(beginTime, endTime);
        DateTime bucket0Time = new DateTime(buckets.get(0).getStartTime());
        DateTime bucket59Time = new DateTime(buckets.get(59).getStartTime());

        int scheduleId = 123;
        List<AggregateNumericMetric> metrics = asList(
            new AggregateNumericMetric(scheduleId, Bucket.SIX_HOUR, 2.0, 1.0, 3.0, bucket0Time.getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.SIX_HOUR, 5.0, 4.0, 6.0, bucket0Time.plusHours(1).getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.SIX_HOUR, 3.0, 3.0, 3.0, bucket0Time.plusHours(2).getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.SIX_HOUR, 5.0, 2.0, 9.0, bucket59Time.getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.SIX_HOUR, 5.0, 4.0, 6.0, bucket59Time.plusHours(1).getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.SIX_HOUR, 3.0, 3.0, 3.0, bucket59Time.plusHours(2).getMillis())
        );
        for (AggregateNumericMetric metric : metrics) {
            insert6HourData(metric).get();
        }

        List<MeasurementDataNumericHighLowComposite> actualData = Lists.newArrayList(metricsServer.findDataForResource(
            scheduleId, beginTime.getMillis(), endTime.getMillis(),60));

        assertEquals(actualData.size(), buckets.getNumDataPoints(), "Expected to get back 60 data points.");

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0).getStartTime(), divide(2.0 + 5.0 + 3.0, 3), 6.0, 1.0);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59).getStartTime(), divide(5.0 + 5.0 + 3.0, 3), 9.0, 2.0);
        MeasurementDataNumericHighLowComposite expectedBucket29Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(29).getStartTime(), Double.NaN, Double.NaN, Double.NaN);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actualData.get(0), TEST_PRECISION);
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actualData.get(59), TEST_PRECISION);
        assertPropertiesMatch("The data for bucket 29 does not match the expected values.", expectedBucket29Data,
            actualData.get(29), TEST_PRECISION);
    }

    @Test
    public void find24HourDataComposites() throws Exception {
        DateTime beginTime = hour(0).minusDays(100);
        DateTime endTime = now();

        Buckets buckets = new Buckets(beginTime, endTime);
        DateTime bucket0Time = new DateTime(buckets.get(0).getStartTime());
        DateTime bucket59Time = new DateTime(buckets.get(59).getStartTime());

        int scheduleId = 123;
        List<AggregateNumericMetric> metrics = asList(
            new AggregateNumericMetric(scheduleId, Bucket.TWENTY_FOUR_HOUR, 4.0, 4.0, 4.0,
                bucket0Time.minusDays(1).getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.TWENTY_FOUR_HOUR, 5.0, 5.0, 5.0, bucket0Time.getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.TWENTY_FOUR_HOUR, 7.0, 7.0, 7.0, bucket59Time.getMillis()),
            new AggregateNumericMetric(scheduleId, Bucket.TWENTY_FOUR_HOUR, 8.0, 8.0, 8.0,
                bucket59Time.plusDays(2).getMillis())
        );
        for (AggregateNumericMetric metric : metrics) {
            insert24HourData(metric).get();
        }

        List<MeasurementDataNumericHighLowComposite> actual = Lists.newArrayList(metricsServer.findDataForResource(
            scheduleId, beginTime.getMillis(), endTime.getMillis(),60));

        assertEquals(actual.size(), buckets.getNumDataPoints(), "Expected to get back 60 data points");

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0).getStartTime(), 5.0, 5.0, 5.0);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59).getStartTime(), 7.0, 7.0, 7.0);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actual.get(0), TEST_PRECISION);
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actual.get(59), TEST_PRECISION);
    }

    private void insertRawData(MeasurementDataNumeric... data) throws Exception {
        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.length * 3);
        metricsServer.addNumericData(ImmutableSet.copyOf(data), waitForRawInserts);
        waitForRawInserts.await("Failed to insert raw data");
    }

}
