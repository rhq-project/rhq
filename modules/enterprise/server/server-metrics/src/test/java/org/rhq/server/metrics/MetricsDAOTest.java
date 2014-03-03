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
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateSimpleNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.domain.RawNumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetricMapper;

/**
 * @author John Sanda
 */
public class MetricsDAOTest extends CassandraIntegrationTest {

    private final Log log = LogFactory.getLog(MetricsDAOTest.class);

    private static final boolean ENABLED = true;

    private final long SECOND = 1000;

    private final long MINUTE = 60 * SECOND;

    private final long HOUR = 60 * MINUTE;

    private MetricsDAO dao;

    private AggregateCacheMapper aggregateCacheMapper = new AggregateCacheMapper();

    private RawCacheMapper rawCacheMapper = new RawCacheMapper();

    @BeforeClass
    public void initDAO() throws Exception {
        dao = new MetricsDAO(storageSession, new MetricsConfiguration());
    }

    @BeforeMethod
    public void resetDB() throws Exception {
        session.execute("TRUNCATE " + MetricsTable.RAW);
        session.execute("TRUNCATE " + MetricsTable.ONE_HOUR);
        session.execute("TRUNCATE " + MetricsTable.SIX_HOUR);
        session.execute("TRUNCATE " + MetricsTable.TWENTY_FOUR_HOUR);
        session.execute("TRUNCATE " + MetricsTable.METRICS_CACHE);
    }

    @Test(enabled = ENABLED)
    public void insertAndFindRawData() throws Exception {
        DateTime hour0 = hour0();
        DateTime currentTime = hour0.plusHours(4).plusMinutes(44);
        DateTime threeMinutesAgo = currentTime.minusMinutes(3);

        int scheduleId = 1;
        MeasurementDataNumeric expected = new MeasurementDataNumeric(threeMinutesAgo.getMillis(), scheduleId, 1.23);

        WaitForWrite waitForResults = new WaitForWrite(1);

        StorageResultSetFuture resultSetFuture = dao.insertRawData(expected);
        Futures.addCallback(resultSetFuture, waitForResults);
        waitForResults.await("Failed to insert raw data");

        List<RawNumericMetric> actualMetrics = Lists.newArrayList(dao.findRawMetrics(scheduleId,
            threeMinutesAgo.minusSeconds(1).getMillis(), threeMinutesAgo.plusSeconds(1).getMillis()));

        assertEquals(actualMetrics.size(), 1, "Expected to get back one raw metric");
        assertEquals(actualMetrics.get(0), new RawNumericMetric(scheduleId, expected.getTimestamp(),
            expected.getValue()), "The raw metric does not match the expected value");
    }

    @Test(enabled = ENABLED)
    public void findLatestRawMetric() throws Exception {
        DateTime hour0 = hour0();
        DateTime currentTime = hour0.plusHours(4).plusMinutes(44);
        DateTime threeMinutesAgo = currentTime.minusMinutes(3);
        DateTime twoMinutesAgo = currentTime.minusMinutes(2);
        DateTime oneMinuteAgo = currentTime.minusMinutes(1);

        int scheduleId = 1;

        List<MeasurementDataNumeric> data = new ArrayList<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(threeMinutesAgo.getMillis(), scheduleId, 3.2));
        data.add(new MeasurementDataNumeric(twoMinutesAgo.getMillis(), scheduleId, 3.9));
        data.add(new MeasurementDataNumeric(oneMinuteAgo.getMillis(), scheduleId, 2.6));

        WaitForWrite waitForWrite = new WaitForWrite(data.size());

        for (MeasurementDataNumeric raw : data) {
            StorageResultSetFuture resultSetFuture = dao.insertRawData(raw);
            Futures.addCallback(resultSetFuture, waitForWrite);
        }
        waitForWrite.await("Failed to insert raw data");

        RawNumericMetric actual = dao.findLatestRawMetric(scheduleId);
        RawNumericMetric expected = new RawNumericMetric(scheduleId, oneMinuteAgo.getMillis(), 2.6);

        assertEquals(actual, expected, "Failed to find latest raw metric");
    }

    @Test
    public void findRawDataAsync() throws Exception {
        DateTime hour0 = hour0();
        DateTime currentTime = hour0.plusHours(4).plusMinutes(44);
        DateTime threeMinutesAgo = currentTime.minusMinutes(3);
        DateTime twoMinutesAgo = currentTime.minusMinutes(2);
        DateTime oneMinuteAgo = currentTime.minusMinutes(1);

        int scheduleId = 1;

        List<MeasurementDataNumeric> data = new ArrayList<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(threeMinutesAgo.getMillis(), scheduleId, 3.2));
        data.add(new MeasurementDataNumeric(twoMinutesAgo.getMillis(), scheduleId, 3.9));
        data.add(new MeasurementDataNumeric(oneMinuteAgo.getMillis(), scheduleId, 2.6));

        WaitForWrite waitForWrite = new WaitForWrite(data.size());

        for (MeasurementDataNumeric raw : data) {
            StorageResultSetFuture resultSetFuture = dao.insertRawData(raw);
            Futures.addCallback(resultSetFuture, waitForWrite);
        }
        waitForWrite.await("Failed to insert raw data");

        RawNumericMetricMapper mapper = new RawNumericMetricMapper();
        WaitForRead<RawNumericMetric> waitForRead = new WaitForRead<RawNumericMetric>(mapper);
        StorageResultSetFuture resultSetFuture = dao.findRawMetricsAsync(scheduleId,
            threeMinutesAgo.minusSeconds(5).getMillis(), oneMinuteAgo.plusSeconds(5).getMillis());
        Futures.addCallback(resultSetFuture, waitForRead);

        waitForRead.await("Failed to fetch raw data");
        List<RawNumericMetric> actual = waitForRead.getResults();
        List<RawNumericMetric> expected = map(data);

        assertEquals(actual, expected, "Async read of raw data failed");
    }

    @Test(enabled = ENABLED)
    public void findRawMetricsForMultipleSchedules() throws Exception {
        DateTime currentTime = hour0().plusHours(4).plusMinutes(44);
        DateTime currentHour = currentTime.hourOfDay().roundFloorCopy();
        DateTime threeMinutesAgo = currentTime.minusMinutes(3);
        DateTime twoMinutesAgo = currentTime.minusMinutes(2);
        DateTime oneMinuteAgo = currentTime.minusMinutes(1);

        int scheduleId1 = 1;
        int scheduleId2 = 2;

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(threeMinutesAgo.getMillis(), scheduleId1, 1.1));
        data.add(new MeasurementDataNumeric(threeMinutesAgo.getMillis(), scheduleId2, 1.2));
        data.add(new MeasurementDataNumeric(twoMinutesAgo.getMillis(), scheduleId1, 2.1));
        data.add(new MeasurementDataNumeric(twoMinutesAgo.getMillis(), scheduleId2, 2.2));
        data.add(new MeasurementDataNumeric(oneMinuteAgo.getMillis(), scheduleId1, 3.1));
        data.add(new MeasurementDataNumeric(oneMinuteAgo.getMillis(), scheduleId2, 3.2));

        WaitForWrite waitForWrite = new WaitForWrite(data.size());

        for (MeasurementDataNumeric raw : data) {
            StorageResultSetFuture resultSetFuture = dao.insertRawData(raw);
            Futures.addCallback(resultSetFuture, waitForWrite);
        }
        waitForWrite.await("Failed to insert raw data");

        List<RawNumericMetric> actualMetrics = Lists.newArrayList(dao.findRawMetrics(asList(scheduleId1, scheduleId2),
            currentHour.getMillis(), currentHour.plusHours(1).getMillis()));
        List<RawNumericMetric> expectedMetrics = asList(
            new RawNumericMetric(scheduleId1, threeMinutesAgo.getMillis(), 1.1),
            new RawNumericMetric(scheduleId1, twoMinutesAgo.getMillis(), 2.1),
            new RawNumericMetric(scheduleId1, oneMinuteAgo.getMillis(), 3.1),
            new RawNumericMetric(scheduleId2, threeMinutesAgo.getMillis(), 1.2),
            new RawNumericMetric(scheduleId2, twoMinutesAgo.getMillis(), 2.2),
            new RawNumericMetric(scheduleId2, oneMinuteAgo.getMillis(), 3.2)
        );
        assertEquals(actualMetrics, expectedMetrics, "Failed to find raw metrics for multiple schedules");
    }

    @Test(enabled = ENABLED)
    public void insertAndFindAllOneHourMetrics() {
        int scheduleId = 1;
        DateTime hour0 = hour0();

        List<AggregateNumericMetric> metrics = asList(
            new AggregateNumericMetric(scheduleId, 3.0, 1.0, 8.0, hour0.getMillis()),
            new AggregateNumericMetric(scheduleId, 4.0, 2.0, 10.0, hour0.plusHours(1).getMillis()),
            new AggregateNumericMetric(456, 2.0, 2.0, 2.0, hour0.getMillis())
        );

        for (AggregateNumericMetric metric : metrics) {
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }

        List<AggregateNumericMetric> expected = asList(
            new AggregateNumericMetric(scheduleId, 3.0, 1.0, 8.0, hour0.getMillis()),
            new AggregateNumericMetric(scheduleId, 4.0, 2.0, 10.0, hour0.plusHours(1).getMillis())
        );

        List<AggregateNumericMetric> actual = Lists.newArrayList(findAggregateMetrics(MetricsTable.ONE_HOUR,
            scheduleId));
        assertEquals(actual, expected, "Failed to find one hour metrics");

        // verify that the TTL is set
        List<AggregateNumericMetric> actualMetricsWithMetadata = Lists.newArrayList(
            findAggregateMetricsWithMetadata(MetricsTable.ONE_HOUR, scheduleId, hour0.getMillis(), hour0()
                .plusHours(1).plusSeconds(1).getMillis()));
        assertAggregateTTLSet(actualMetricsWithMetadata);
    }

    @Test(enabled = ENABLED)
    public void findOneHourMetricsForMultipleSchedules() {
        int schedule1 = 1;
        int schedule2 = 2;
        int schedule3 = 3;

        List<AggregateNumericMetric> metrics = asList(
            new AggregateNumericMetric(schedule1, 1.1, 1.1, 1.1, hour0().getMillis()),
            new AggregateNumericMetric(schedule2, 1.2, 1.2, 1.2, hour0().getMillis()),
            new AggregateNumericMetric(schedule1, 2.1, 2.1, 2.1, hour0().plusHours(1).getMillis()),
            new AggregateNumericMetric(schedule2, 2.2, 2.2, 2.2, hour0().plusHours(1).getMillis()),
            new AggregateNumericMetric(schedule3, 3.2, 3.2, 3.2, hour0().plusHours(1).getMillis())
        );

        for (AggregateNumericMetric metric : metrics) {
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }

        List<AggregateNumericMetric> actual = Lists.newArrayList(dao.findOneHourMetrics(asList(schedule1, schedule2),
            hour0().plusHours(1).getMillis(), hour0().plusHours(2).getMillis()));
        List<AggregateNumericMetric> expected = asList(
            new AggregateNumericMetric(schedule1, 2.1, 2.1, 2.1, hour0().plusHours(1).getMillis()),
            new AggregateNumericMetric(schedule2, 2.2, 2.2, 2.2, hour0().plusHours(1).getMillis())
        );

        assertEquals(actual, expected, "Failed to find one hour metrics for multiple schedules");
    }

    @Test(enabled = ENABLED)
    public void findRangeOfOneHourMetrics() {
        int scheduledId = 1;
        int nextScheduleId = 2;
        DateTime hour0 = hour0();

        List<AggregateNumericMetric> metrics = asList(
            new AggregateNumericMetric(scheduledId, 2.0, 2.0, 2.0, hour0.getMillis()),
            new AggregateNumericMetric(scheduledId, 3.0, 3.0, 3.0, hour0.plusHours(1).getMillis()),
            new AggregateNumericMetric(scheduledId, 4.0, 4.0, 4.0, hour0.plusHours(2).getMillis()),
            new AggregateNumericMetric(scheduledId, 5.0, 5.0, 5.0, hour0.plusHours(3).getMillis()),
            new AggregateNumericMetric(nextScheduleId, 1.0, 1.0, 1.0, hour0.plusHours(1).getMillis())
        );

        for (AggregateNumericMetric metric : metrics) {
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }

        DateTime startTime = hour0.plusHours(1);
        DateTime endTime = hour0.plusHours(3);
        List<AggregateNumericMetric> expected = asList(
            new AggregateNumericMetric(scheduledId, 3.0, 3.0, 3.0, hour0.plusHours(1).getMillis()),
            new AggregateNumericMetric(scheduledId, 4.0, 4.0, 4.0, hour0.plusHours(2).getMillis())
        );

        List<AggregateNumericMetric> actual = Lists.newArrayList(dao.findOneHourMetrics(scheduledId,
            startTime.getMillis(), endTime.getMillis()));

        assertEquals(actual, expected, "Failed to find one hour metrics for date range");
    }

    @Test(enabled = ENABLED)
    public void updateSixHourCache() throws Exception {
        int startScheduleId = 100;
        int scheduleId1 = 101;
        int scheduleId2= 102;

        WaitForWrite waitForWrite = new WaitForWrite(2);

        StorageResultSetFuture resultSetFuture1 = dao.updateMetricsCache(MetricsTable.TWENTY_FOUR_HOUR,
            hour0().getMillis(), startScheduleId, scheduleId1, hour0().getMillis(), ImmutableMap.of(
            AggregateType.MIN.ordinal(), 3.14,
            AggregateType.AVG.ordinal(), 3.14,
            AggregateType.MAX.ordinal(), 3.14));
        StorageResultSetFuture resultSetFuture2 = dao.updateMetricsCache(MetricsTable.TWENTY_FOUR_HOUR,
            hour0().getMillis(), startScheduleId, scheduleId2, hour0().getMillis(), ImmutableMap.of(
            AggregateType.MIN.ordinal(), 3.14,
            AggregateType.AVG.ordinal(), 3.14,
            AggregateType.MAX.ordinal(), 3.14));

        Futures.addCallback(resultSetFuture1, waitForWrite);
        Futures.addCallback(resultSetFuture2, waitForWrite);

        waitForWrite.await("Failed to update metrics cache");

        List<AggregateNumericMetric> expected = asList(
            new AggregateNumericMetric(scheduleId1, 3.14, 3.14, 3.14, hour0().getMillis()),
            new AggregateNumericMetric(scheduleId2, 3.14, 3.14, 3.14, hour0().getMillis())
        );

        StorageResultSetFuture cacheFuture = dao.findCacheEntriesAsync(MetricsTable.TWENTY_FOUR_HOUR,
            hour0().getMillis(), startScheduleId);
        ResultSet resultSet = cacheFuture.get();
        List<Row> rows = resultSet.all();

        assertEquals(rows.size(), expected.size(), "Expected to get back two rows from cache query");

        List<AggregateNumericMetric> actual = asList(aggregateCacheMapper.map(rows.get(0)),
            aggregateCacheMapper.map(rows.get(1)));
        assertCollectionMatchesNoOrder(expected, actual, "Failed to update or retrieve metrics cache entries");
    }

    @Test(enabled = ENABLED)
    public void insertAndGetCacheEntries() throws Exception {
        int startScheduleId = 100;
        int scheduleId1 = 101;
        int scheduleId2 = 102;
        long timeSlice = hour0().plusHours(7).getMillis();
        long timestamp1 = hour0().plusHours(7).plusMinutes(3).getMillis();
        long timestamp2 = hour0().plusHours(7).plusMinutes(5).getMillis();

        WaitForWrite waitForWrite = new WaitForWrite(2);
        StorageResultSetFuture insertFuture1 = dao.updateMetricsCache(MetricsTable.ONE_HOUR, timeSlice, startScheduleId,
            scheduleId1, timestamp1, ImmutableMap.of(AggregateType.VALUE.ordinal(), 2.14));
        StorageResultSetFuture insertFuture2 = dao.updateMetricsCache(MetricsTable.ONE_HOUR, timeSlice, startScheduleId,
            scheduleId2, timestamp2, ImmutableMap.of(AggregateType.VALUE.ordinal(), 1.01));
        Futures.addCallback(insertFuture1, waitForWrite);
        Futures.addCallback(insertFuture2, waitForWrite);
        waitForWrite.await("Failed to update raw cache");

        List<RawNumericMetric> expected = asList(new RawNumericMetric(scheduleId1, timestamp1, 2.14),
            new RawNumericMetric(scheduleId2, timestamp2, 1.01));
        StorageResultSetFuture queryFuture = dao.findCacheEntriesAsync(MetricsTable.ONE_HOUR, timeSlice,
            startScheduleId);
        ResultSet resultSet = queryFuture.get();
        List<Row> rows = resultSet.all();

        assertEquals(rows.size(), expected.size(), "Expected to get back two rows from raw cache query");

        List<RawNumericMetric> actual = asList(rawCacheMapper.map(rows.get(0)), rawCacheMapper.map(rows.get(1)));

        assertEquals(actual, expected, "The raw cache entries do not match");
    }


    @Test(enabled = ENABLED)
    public void deleteRawCacheEntries() {
        int startScheduleId = 100;
        int scheduleId1 = 101;
        int scheduleId2 = 102;
        DateTime timeSlice = hour0().plusHours(2);

        dao.updateMetricsCache(MetricsTable.ONE_HOUR, timeSlice.getMillis(), startScheduleId, scheduleId1,
            timeSlice.plusMinutes(2).getMillis(), ImmutableMap.of(AggregateType.VALUE.ordinal(), 3.14)).get();
        dao.updateMetricsCache(MetricsTable.ONE_HOUR, timeSlice.getMillis(), startScheduleId, scheduleId2,
            timeSlice.plusMinutes(4).getMillis(), ImmutableMap.of(AggregateType.VALUE.ordinal(), 3.14)).get();

        dao.deleteCacheEntries(MetricsTable.ONE_HOUR, timeSlice.getMillis(), startScheduleId).get();

        ResultSet resultSet = dao.findCacheEntriesAsync(MetricsTable.ONE_HOUR,
            timeSlice.getMillis(), startScheduleId).get();

        assertTrue(resultSet.isExhausted(), "Expected an empty result set");
    }

    @Test(enabled = ENABLED)
    public void findAggregatedSimpleMetrics() throws InterruptedException {
        List<AggregateNumericMetric> metrics;

        long startTime = System.currentTimeMillis();
        long endTime = startTime + HOUR;

        int scheduleId = 123;
        int numberOfAggregatedMetrics = 250;
        int ttl = Hours.TWO.toStandardSeconds().getSeconds();


        metrics = this.generateRandomAggregatedMetrics(scheduleId, numberOfAggregatedMetrics, startTime);
        for (AggregateNumericMetric metric : metrics) {
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }
        double expectedMinSum = 0;
        double expectedMaxSum = 0;
        double expectedAverageSum = 0;

        for (AggregateNumericMetric aggregatedMetric : metrics) {
            expectedAverageSum += aggregatedMetric.getAvg();
            expectedMaxSum += aggregatedMetric.getMax();
            expectedMinSum += aggregatedMetric.getMin();
        }

        int alternateScheduleId = 321;
        int alternateNumberOfAggregatedMetrics = 75;
        metrics = this.generateRandomAggregatedMetrics(alternateScheduleId, alternateNumberOfAggregatedMetrics,
            startTime);
        for (AggregateNumericMetric metric : metrics) {
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }

        List<AggregateSimpleNumericMetric> retrievedItems = Lists.newArrayList(dao.findAggregatedSimpleOneHourMetric(
            scheduleId, startTime, endTime));
        assertEquals(numberOfAggregatedMetrics * 3, retrievedItems.size());
        double actualAverageSum = 0;
        double actualMinSum = 0;
        double actualMaxSum = 0;
        for (AggregateSimpleNumericMetric metric : retrievedItems) {
            if (AggregateType.AVG.equals(metric.getType())) {
                actualAverageSum += metric.getValue();
            } else if (AggregateType.MAX.equals(metric.getType())) {
                actualMaxSum += metric.getValue();
            } else if (AggregateType.MIN.equals(metric.getType())) {
                actualMinSum += metric.getValue();
            }
        }

        assertEquals(expectedAverageSum, actualAverageSum);
        assertEquals(expectedMaxSum, actualMaxSum);
        assertEquals(expectedMinSum, actualMinSum);
    }

    @Test(enabled = ENABLED)
    public void randomizedInsertAndFindMetrics() {

        long startTime = System.currentTimeMillis();
        long endTime = startTime + 2 * HOUR;

        int scheduleId = 1;
        int alternateScheduleId = 2;
        int ttl = Hours.TWO.toStandardSeconds().getSeconds();

        //insert data
        List<AggregateNumericMetric> firstHourMetrics = this.generateRandomAggregatedMetrics(scheduleId, 234,
            startTime);
        for (AggregateNumericMetric metric : firstHourMetrics) {
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }

        List<AggregateNumericMetric> secondHourMetrics = this.generateRandomAggregatedMetrics(scheduleId, 234,
            startTime + HOUR);
        for (AggregateNumericMetric metric : secondHourMetrics) {
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }

        List<AggregateNumericMetric> alternateScheduleIdMetrics = this.generateRandomAggregatedMetrics(
            alternateScheduleId, 159, startTime);
        for (AggregateNumericMetric metric : alternateScheduleIdMetrics) {
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }

        //verify data can be retrieved
        List<AggregateNumericMetric> combinedList = new ArrayList<AggregateNumericMetric>();
        combinedList.addAll(firstHourMetrics);
        combinedList.addAll(secondHourMetrics);

        List<AggregateNumericMetric> actualCombined = Lists.newArrayList(findAggregateMetrics(MetricsTable.ONE_HOUR,
            scheduleId));
        assertEquals(actualCombined, combinedList, "Failed to find one hour metrics");

        List<AggregateNumericMetric> actualFirstHour = Lists.newArrayList(dao.findOneHourMetrics(scheduleId,
            startTime, startTime + HOUR - 1));
        assertEquals(actualFirstHour, firstHourMetrics, "Failed to find one hour metrics");

        List<AggregateNumericMetric> actualSecondHour = Lists.newArrayList(dao.findOneHourMetrics(scheduleId,
            startTime + HOUR, endTime));
        assertEquals(actualSecondHour, secondHourMetrics, "Failed to find one hour metrics");

        List<AggregateNumericMetric> actualAlternateScheduleIdMetrics = Lists.newArrayList(findAggregateMetrics(
            MetricsTable.ONE_HOUR, alternateScheduleId));
        assertEquals(actualAlternateScheduleIdMetrics, alternateScheduleIdMetrics, "Failed to find one hour metrics");

        // verify that the TTL is set
        List<AggregateNumericMetric> actualMetricsWithMetadata = Lists.newArrayList(findAggregateMetricsWithMetadata(
            MetricsTable.ONE_HOUR, scheduleId, startTime, endTime));
        assertAggregateTTLSet(actualMetricsWithMetadata);

        actualMetricsWithMetadata = Lists.newArrayList(findAggregateMetricsWithMetadata(MetricsTable.ONE_HOUR,
            alternateScheduleId, startTime, endTime));
        assertAggregateTTLSet(actualMetricsWithMetadata);
    }

    @Test(enabled = ENABLED)
    public void randomizedSimpleInsertAndFindMetrics() {

        long startTime = System.currentTimeMillis();
        long endTime = startTime + 2 * HOUR;

        int scheduleId = 1;
        int alternateScheduleId = 2;

        MetricsTable[] tablesToTest = new MetricsTable[] { MetricsTable.ONE_HOUR, MetricsTable.SIX_HOUR,
            MetricsTable.TWENTY_FOUR_HOUR };

        for (MetricsTable table : tablesToTest) {
            switch (table) {
                case ONE_HOUR:
                    testRandomOneHourData(scheduleId, alternateScheduleId, startTime, endTime);
                    break;
                case SIX_HOUR:
                    testRandomSixHourData(scheduleId, alternateScheduleId, startTime, endTime);
                    break;
                default: // 24 hour
                    testRandomTwentyFourHourData(scheduleId, alternateScheduleId, startTime, endTime);
            }
        }
    }

    private void testRandomOneHourData(int scheduleId, int alternateScheduleId, long startTime, long endTime) {
        //insert data
        List<AggregateNumericMetric> metrics = this.generateRandomAggregatedMetrics(scheduleId, 2, startTime);
        for (AggregateNumericMetric metric : metrics) {
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }

        List<AggregateNumericMetric> alternateMetrics = this.generateRandomAggregatedMetrics(alternateScheduleId,
            3, startTime);
        for (AggregateNumericMetric metric : alternateMetrics) {
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }

        //verify data can be retrieve
        List<AggregateNumericMetric> actualMetrics = Lists.newArrayList(findAggregateMetrics(MetricsTable.ONE_HOUR,
            scheduleId));
        assertEquals(actualMetrics, metrics, "Failed to find metrics in " + MetricsTable.ONE_HOUR + " table.");

        List<AggregateNumericMetric> actualAlternateMetrics = Lists.newArrayList(findAggregateMetrics(
            MetricsTable.ONE_HOUR, alternateScheduleId));
        assertEquals(actualAlternateMetrics, alternateMetrics, "Failed to find metrics in " + MetricsTable.ONE_HOUR +
            " table.");

        // verify that the TTL is set
        List<AggregateNumericMetric> actualMetricsWithMetadata = Lists.newArrayList(
            findAggregateMetricsWithMetadata(MetricsTable.ONE_HOUR, scheduleId, startTime, endTime));
        assertAggregateTTLSet(actualMetricsWithMetadata);
    }

    private void testRandomSixHourData(int scheduleId, int alternateScheduleId, long startTime, long endTime) {
        //insert data
        List<AggregateNumericMetric> metrics = this.generateRandomAggregatedMetrics(scheduleId, 2, startTime);
        for (AggregateNumericMetric metric : metrics) {
            dao.insertSixHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertSixHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertSixHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }

        List<AggregateNumericMetric> alternateMetrics = this.generateRandomAggregatedMetrics(alternateScheduleId,
            3, startTime);
        for (AggregateNumericMetric metric : alternateMetrics) {
            dao.insertSixHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertSixHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertSixHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }

        //verify data can be retrieve
        List<AggregateNumericMetric> actualMetrics = Lists.newArrayList(findAggregateMetrics(MetricsTable.SIX_HOUR,
            scheduleId));
        assertEquals(actualMetrics, metrics, "Failed to find metrics in " + MetricsTable.SIX_HOUR + " table.");

        List<AggregateNumericMetric> actualAlternateMetrics = Lists.newArrayList(findAggregateMetrics(
            MetricsTable.SIX_HOUR, alternateScheduleId));
        assertEquals(actualAlternateMetrics, alternateMetrics, "Failed to find metrics in " + MetricsTable.SIX_HOUR +
            " table.");

        // verify that the TTL is set
        List<AggregateNumericMetric> actualMetricsWithMetadata = Lists.newArrayList(
            findAggregateMetricsWithMetadata(MetricsTable.SIX_HOUR, scheduleId, startTime, endTime));
        assertAggregateTTLSet(actualMetricsWithMetadata);
    }

    private void testRandomTwentyFourHourData(int scheduleId, int alternateScheduleId, long startTime, long endTime) {
        //insert data
        List<AggregateNumericMetric> metrics = this.generateRandomAggregatedMetrics(scheduleId, 2, startTime);
        for (AggregateNumericMetric metric : metrics) {
            dao.insertTwentyFourHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN,
                metric.getMin());
            dao.insertTwentyFourHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX,
                metric.getMax());
            dao.insertTwentyFourHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG,
                metric.getAvg());
        }

        List<AggregateNumericMetric> alternateMetrics = this.generateRandomAggregatedMetrics(alternateScheduleId,
            3, startTime);
        for (AggregateNumericMetric metric : alternateMetrics) {
            dao.insertTwentyFourHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN,
                metric.getMin());
            dao.insertTwentyFourHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX,
                metric.getMax());
            dao.insertTwentyFourHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG,
                metric.getAvg());
        }

        //verify data can be retrieve
        List<AggregateNumericMetric> actualMetrics = Lists.newArrayList(findAggregateMetrics(
            MetricsTable.TWENTY_FOUR_HOUR, scheduleId));
        assertEquals(actualMetrics, metrics, "Failed to find metrics in " + MetricsTable.TWENTY_FOUR_HOUR + " table.");

        List<AggregateNumericMetric> actualAlternateMetrics = Lists.newArrayList(findAggregateMetrics(
            MetricsTable.TWENTY_FOUR_HOUR, alternateScheduleId));
        assertEquals(actualAlternateMetrics, alternateMetrics, "Failed to find metrics in " +
            MetricsTable.TWENTY_FOUR_HOUR + " table.");

        // verify that the TTL is set
        List<AggregateNumericMetric> actualMetricsWithMetadata = Lists.newArrayList(
            findAggregateMetricsWithMetadata(MetricsTable.TWENTY_FOUR_HOUR, scheduleId, startTime, endTime));
        assertAggregateTTLSet(actualMetricsWithMetadata);
    }

    /**
     * Generates a set of aggregated metrics with randomized data. Using the schedule id provided the
     * aggregated metrics are 1 millisecond apart beginning with start time and min<average<max.
     *
     * @param scheduleId
     * @param numberOfAggregatedMetrics
     * @param startTime
     * @return
     */
    private List<AggregateNumericMetric> generateRandomAggregatedMetrics(int scheduleId,
        int numberOfAggregatedMetrics, long startTime) {
        double max;
        double min;
        double average;
        double temp;

        Random random = new Random();
        List<AggregateNumericMetric> generatedMetrics = new ArrayList<AggregateNumericMetric>();

        for (int i = 0; i < numberOfAggregatedMetrics; i++) {
            max = random.nextDouble() * 1000;
            average = random.nextDouble() * 1000;
            if (average > max) {
                temp = max;
                max = average;
                average = temp;
            }

            min = random.nextDouble() * 1000;
            if (min > max) {
                temp = min;
                min = average;
                average = max;
                max = temp;
            } else if (min > average) {
                temp = average;
                average = min;
                min = temp;
            }

            generatedMetrics.add(new AggregateNumericMetric(scheduleId, average, min, max, startTime + i));
        }

        return generatedMetrics;
    }

    private void assertRawTTLSet(List<RawNumericMetric> metrics) {
        for (RawNumericMetric metric : metrics) {
            assertNotNull(metric.getColumnMetadata(), metric + " does not contain column meta data. The meta data " +
                " must be loaded in order to verify that the TTL is set correctly.");
            assertNotNull(metric.getColumnMetadata().getTtl(), "The TTL for " + metric + " is not set.");
        }
    }

    private void assertAggregateTTLSet(List<AggregateNumericMetric> metrics) {
        for (AggregateNumericMetric metric : metrics) {
            assertNotNull(metric.getAvgColumnMetadata(), metric + " does not contain column meta data for its " +
                " average value. The meta data must be loaded in order to verify that the TTL is set correctly.");
            assertNotNull(metric.getAvgColumnMetadata().getTtl(), "The TTL for average column of " + metric +
                " is not set.");
            assertNotNull(metric.getMinColumnMetadata(), metric + " does not contain column meta data for its " +
                " minimum value. The meta data must be loaded in order to verify that the TTL is set correctly.");
            assertNotNull(metric.getMinColumnMetadata().getTtl(), "The TTL for minimum column of " + metric +
                " is not set.");
            assertNotNull(metric.getMaxColumnMetadata(), metric + " does not contain column meta data for its " +
                " maximum value. The meta data must be loaded in order to verify that the TTL is set correctly.");
            assertNotNull(metric.getMaxColumnMetadata().getTtl(), "The TTL for maximum column of " + metric +
                " is not set.");
        }
    }

    private List<RawNumericMetric> map(List<MeasurementDataNumeric> data) {
        List<RawNumericMetric> raw = new ArrayList<RawNumericMetric>(data.size());
        for (MeasurementDataNumeric datum : data) {
            raw.add(new RawNumericMetric(datum.getScheduleId(), datum.getTimestamp(), datum.getValue()));
        }
        return raw;
    }

}
