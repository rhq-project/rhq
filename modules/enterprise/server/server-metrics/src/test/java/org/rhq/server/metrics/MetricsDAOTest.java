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
import static org.rhq.core.domain.util.PageOrdering.DESC;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;

/**
 * @author John Sanda
 */
public class MetricsDAOTest extends CassandraIntegrationTest {

    private static final boolean ENABLED = true;

    private final long SECOND = 1000;

    private final long MINUTE = 60 * SECOND;

    private final long HOUR = 60 * MINUTE;

    private MetricsDAO dao;

    @BeforeClass
    public void initDAO() throws Exception {
        dao = new MetricsDAO(session);
    }

    @BeforeMethod
    public void resetDB() throws Exception {
        session.execute("TRUNCATE " + MetricsTable.RAW);
        session.execute("TRUNCATE " + MetricsTable.ONE_HOUR);
        session.execute("TRUNCATE " + MetricsTable.SIX_HOUR);
        session.execute("TRUNCATE " + MetricsTable.TWENTY_FOUR_HOUR);
        session.execute("TRUNCATE " + MetricsTable.INDEX);
    }

    @Test(enabled = ENABLED)
    public void insertAndFindRawMetrics() {
        DateTime hour0 = hour0();
        DateTime currentTime = hour0.plusHours(4).plusMinutes(44);
        DateTime currentHour = currentTime.hourOfDay().roundFloorCopy();
        DateTime threeMinutesAgo = currentTime.minusMinutes(3);
        DateTime twoMinutesAgo = currentTime.minusMinutes(2);
        DateTime oneMinuteAgo = currentTime.minusMinutes(1);

        int scheduleId = 1;

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(threeMinutesAgo.getMillis(), scheduleId, 3.2));
        data.add(new MeasurementDataNumeric(twoMinutesAgo.getMillis(), scheduleId, 3.9));
        data.add(new MeasurementDataNumeric(oneMinuteAgo.getMillis(), scheduleId, 2.6));

        int ttl = Hours.ONE.toStandardSeconds().getSeconds();
        Set<MeasurementDataNumeric> actualUpdates = dao.insertRawMetrics(data, ttl);

        assertEquals(actualUpdates, data, "The updates do not match expected value.");

        List<RawNumericMetric> actualMetrics = dao.findRawMetrics(scheduleId, currentHour.getMillis(), currentHour
            .plusHours(1).getMillis());
        List<RawNumericMetric> expectedMetrics = asList(
            new RawNumericMetric(scheduleId, threeMinutesAgo.getMillis(), 3.2),
            new RawNumericMetric(scheduleId, twoMinutesAgo.getMillis(), 3.9),
            new RawNumericMetric(scheduleId, oneMinuteAgo.getMillis(), 2.6)
        );
        assertEquals(actualMetrics, expectedMetrics, "Failed to find raw metrics");

        // Now verify that the TTL was set. We do this separately in order to exercise both
        // versions of the findRawMetrics method. In production code (so far), we have no
        // need to retrieve meta data when retrieving raw metrics, but we need to verify
        // that the TTL is set.
        List<RawNumericMetric> actualMetricsWithMetadata = dao.findRawMetrics(scheduleId, currentHour.getMillis(),
            currentHour.plusHours(1).getMillis(), true);
        assertRawTTLSet(actualMetricsWithMetadata);
    }

    @Test(enabled = ENABLED)
    public void findLimitedRawMetricsInDescendingOrder() {
        DateTime hour0 = hour0();
        DateTime currentTime = hour0.plusHours(4).plusMinutes(44);
        DateTime threeMinutesAgo = currentTime.minusMinutes(3);
        DateTime twoMinutesAgo = currentTime.minusMinutes(2);
        DateTime oneMinuteAgo = currentTime.minusMinutes(1);

        int scheduleId = 1;

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(threeMinutesAgo.getMillis(), scheduleId, 3.2));
        data.add(new MeasurementDataNumeric(twoMinutesAgo.getMillis(), scheduleId, 3.9));
        data.add(new MeasurementDataNumeric(oneMinuteAgo.getMillis(), scheduleId, 2.6));

        int ttl = Hours.ONE.toStandardSeconds().getSeconds();
        dao.insertRawMetrics(data, ttl);

        List<RawNumericMetric> actualMetrics = dao.findRawMetrics(scheduleId, DESC, 2);
        List<RawNumericMetric> expectedMetrics = asList(
            new RawNumericMetric(scheduleId, oneMinuteAgo.getMillis(), 2.6),
            new RawNumericMetric(scheduleId, twoMinutesAgo.getMillis(), 3.9)
        );

        assertEquals(actualMetrics, expectedMetrics, "Failed to find raw metrics with order and limit specified");
    }

    @Test(enabled = ENABLED)
    public void findRawMetricsForMultipleSchedules() {
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

        int ttl = Hours.ONE.toStandardSeconds().getSeconds();
        Set<MeasurementDataNumeric> actualUpdates = dao.insertRawMetrics(data, ttl);
        assertEquals(actualUpdates, data);

        List<RawNumericMetric> actualMetrics = dao.findRawMetrics(asList(scheduleId1, scheduleId2),
            currentHour.getMillis(), currentHour.plusHours(1).getMillis());
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

        List<AggregatedNumericMetric> metrics = asList(
            new AggregatedNumericMetric(scheduleId, 3.0, 1.0, 8.0, hour0.getMillis()),
            new AggregatedNumericMetric(scheduleId, 4.0, 2.0, 10.0, hour0.plusHours(1).getMillis()),
            new AggregatedNumericMetric(456, 2.0, 2.0, 2.0, hour0.getMillis())
        );
        int ttl = Hours.ONE.getHours();
        List<AggregatedNumericMetric> actualUpdates = dao.insertAggregates(MetricsTable.ONE_HOUR, metrics, ttl);
        List<AggregatedNumericMetric> expectedUpdates = metrics;

        assertEquals(actualUpdates, expectedUpdates, "The updates do not match the expected values");

        List<AggregatedNumericMetric> expected = asList(
            new AggregatedNumericMetric(scheduleId, 3.0, 1.0, 8.0, hour0.getMillis()),
            new AggregatedNumericMetric(scheduleId, 4.0, 2.0, 10.0, hour0.plusHours(1).getMillis())
        );
        List<AggregatedNumericMetric> actual = dao.findAggregateMetrics(MetricsTable.ONE_HOUR, scheduleId);
        assertEquals(actual, expected, "Failed to find one hour metrics");

        // verify that the TTL is set
        List<AggregatedNumericMetric> actualMetricsWithMetadata = dao.findAggregateMetricsWithMetadata(
            MetricsTable.ONE_HOUR, scheduleId, hour0.getMillis(), hour0().plusHours(1).plusSeconds(1).getMillis());
        assertAggregateTTLSet(actualMetricsWithMetadata);
    }

    @Test(enabled = ENABLED)
    public void findOneHourMetricsForMultipleSchedules() {
        int schedule1 = 1;
        int schedule2 = 2;
        int schedule3 = 3;

        List<AggregatedNumericMetric> metrics = asList(
            new AggregatedNumericMetric(schedule1, 1.1, 1.1, 1.1, hour0().getMillis()),
            new AggregatedNumericMetric(schedule2, 1.2, 1.2, 1.2, hour0().getMillis()),
            new AggregatedNumericMetric(schedule1, 2.1, 2.1, 2.1, hour0().plusHours(1).getMillis()),
            new AggregatedNumericMetric(schedule2, 2.2, 2.2, 2.2, hour0().plusHours(1).getMillis()),
            new AggregatedNumericMetric(schedule3, 3.2, 3.2, 3.2, hour0().plusHours(1).getMillis())
        );
        int ttl = Hours.ONE.toStandardSeconds().getSeconds();

        dao.insertAggregates(MetricsTable.ONE_HOUR, metrics, ttl);
        List<AggregatedNumericMetric> actual = dao.findAggregateMetrics(MetricsTable.ONE_HOUR,
            asList(schedule1, schedule2), hour0().plusHours(1).getMillis(), hour0().plusHours(2).getMillis());
        List<AggregatedNumericMetric> expected = asList(
            new AggregatedNumericMetric(schedule1, 2.1, 2.1, 2.1, hour0().plusHours(1).getMillis()),
            new AggregatedNumericMetric(schedule2, 2.2, 2.2, 2.2, hour0().plusHours(1).getMillis())
        );

        assertEquals(actual, expected, "Failed to find one hour metrics for multiple schedules");
    }

    @Test(enabled = ENABLED)
    public void findRangeOfOneHourMetrics() {
        int scheduledId = 1;
        int nextScheduleId = 2;
        DateTime hour0 = hour0();
        int ttl = Hours.ONE.toStandardSeconds().getSeconds();

        List<AggregatedNumericMetric> metrics = asList(
            new AggregatedNumericMetric(scheduledId, 2.0, 2.0, 2.0, hour0.getMillis()),
            new AggregatedNumericMetric(scheduledId, 3.0, 3.0, 3.0, hour0.plusHours(1).getMillis()),
            new AggregatedNumericMetric(scheduledId, 4.0, 4.0, 4.0, hour0.plusHours(2).getMillis()),
            new AggregatedNumericMetric(scheduledId, 5.0, 5.0, 5.0, hour0.plusHours(3).getMillis()),
            new AggregatedNumericMetric(nextScheduleId, 1.0, 1.0, 1.0, hour0.plusHours(1).getMillis())
        );
        dao.insertAggregates(MetricsTable.ONE_HOUR, metrics, ttl);

        DateTime startTime = hour0.plusHours(1);
        DateTime endTime = hour0.plusHours(3);
        List<AggregatedNumericMetric> expected = asList(
            new AggregatedNumericMetric(scheduledId, 3.0, 3.0, 3.0, hour0.plusHours(1).getMillis()),
            new AggregatedNumericMetric(scheduledId, 4.0, 4.0, 4.0, hour0.plusHours(2).getMillis())
        );

        List<AggregatedNumericMetric> actual = dao.findAggregateMetrics(MetricsTable.ONE_HOUR, scheduledId,
            startTime.getMillis(), endTime.getMillis());

        assertEquals(actual, expected, "Failed to find one hour metrics for date range");
    }

    @Test(enabled = ENABLED)
    public void updateAndFindOneHourIndexEntries() {
        DateTime hour0 = hour0();
        int scheduleId1 = 1;
        int scheduleId2 = 2;

        Map<Integer, Long> updates = new HashMap<Integer, Long>();
        updates.put(scheduleId1, hour0.getMillis());
        updates.put(scheduleId2, hour0.getMillis());

        dao.updateMetricsIndex(MetricsTable.ONE_HOUR, updates);
        List<MetricsIndexEntry> actual = dao.findMetricsIndexEntries(MetricsTable.ONE_HOUR);

        List<MetricsIndexEntry> expected = asList(new MetricsIndexEntry(MetricsTable.ONE_HOUR, hour0, scheduleId1),
            new MetricsIndexEntry(MetricsTable.ONE_HOUR, hour0, scheduleId2));
        assertCollectionMatchesNoOrder(expected, actual, "Failed to update or retrieve metrics index entries");
    }

    @Test(enabled = ENABLED)
    public void purge1HourMetricsIndex() {
        int scheduleId1 = 1;
        int scheduleId2 = 2;
        Map<Integer, Long> updates = new HashMap<Integer, Long>();
        updates.put(scheduleId1, hour0().getMillis());
        updates.put(scheduleId2, hour0().getMillis());
        updates.put(scheduleId1, hour0().plusHours(1).getMillis());
        updates.put(scheduleId2, hour0().plusHours(1).getMillis());

        dao.updateMetricsIndex(MetricsTable.ONE_HOUR, updates);
        dao.deleteMetricsIndexEntries(MetricsTable.ONE_HOUR);

        List<MetricsIndexEntry> index = dao.findMetricsIndexEntries(MetricsTable.ONE_HOUR);
        assertEquals(index.size(), 0, "Expected index for " + MetricsTable.ONE_HOUR + " to be empty but found " +
            index);
    }

    @Test(enabled = ENABLED)
    public void findAggregatedSimpleMetrics() throws InterruptedException {
        List<AggregatedNumericMetric> metrics;

        long startTime = System.currentTimeMillis();
        long endTime = startTime + HOUR;

        int scheduleId = 123;
        int numberOfAggregatedMetrics = 250;
        int ttl = Hours.TWO.toStandardSeconds().getSeconds();


        metrics = this.generateRandomAggregatedMetrics(scheduleId, numberOfAggregatedMetrics, startTime);
        dao.insertAggregates(MetricsTable.ONE_HOUR, metrics, ttl);
        double expectedMinSum = 0;
        double expectedMaxSum = 0;
        double expectedAverageSum = 0;

        for (AggregatedNumericMetric aggregatedMetric : metrics) {
            expectedAverageSum += aggregatedMetric.getAvg();
            expectedMaxSum += aggregatedMetric.getMax();
            expectedMinSum += aggregatedMetric.getMin();
        }

        int alternateScheduleId = 321;
        int alternateNumberOfAggregatedMetrics = 75;
        metrics = this.generateRandomAggregatedMetrics(alternateScheduleId, alternateNumberOfAggregatedMetrics,
            startTime);
        dao.insertAggregates(MetricsTable.ONE_HOUR, metrics, ttl);


        List<AggregatedSimpleNumericMetric> retrievedItems = dao.findAggregateSimpleMetrics(MetricsTable.ONE_HOUR,
            scheduleId, startTime, endTime);
        assertEquals(numberOfAggregatedMetrics * 3, retrievedItems.size());
        double actualAverageSum = 0;
        double actualMinSum = 0;
        double actualMaxSum = 0;
        for (AggregatedSimpleNumericMetric metric : retrievedItems) {
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
        List<AggregatedNumericMetric> firstHourMetrics = this.generateRandomAggregatedMetrics(scheduleId, 234,
            startTime);
        List<AggregatedNumericMetric> actualUpdates = dao
            .insertAggregates(MetricsTable.ONE_HOUR, firstHourMetrics, ttl);
        assertEquals(actualUpdates, firstHourMetrics, "The updates do not match the expected values");

        List<AggregatedNumericMetric> secondHourMetrics = this.generateRandomAggregatedMetrics(scheduleId, 234,
            startTime + HOUR);
        actualUpdates = dao.insertAggregates(MetricsTable.ONE_HOUR, secondHourMetrics, ttl);
        assertEquals(actualUpdates, secondHourMetrics, "The updates do not match the expected values");

        List<AggregatedNumericMetric> alternateScheduleIdMetrics = this.generateRandomAggregatedMetrics(
            alternateScheduleId, 159, startTime);
        actualUpdates = dao.insertAggregates(MetricsTable.ONE_HOUR, alternateScheduleIdMetrics, ttl);
        assertEquals(actualUpdates, alternateScheduleIdMetrics, "The updates do not match the expected values");

        //verify data can be retrieved
        List<AggregatedNumericMetric> combinedList = new ArrayList<AggregatedNumericMetric>();
        combinedList.addAll(firstHourMetrics);
        combinedList.addAll(secondHourMetrics);

        List<AggregatedNumericMetric> actualCombined = dao.findAggregateMetrics(MetricsTable.ONE_HOUR, scheduleId);
        assertEquals(actualCombined, combinedList, "Failed to find one hour metrics");

        List<AggregatedNumericMetric> actualFirstHour = dao.findAggregateMetrics(MetricsTable.ONE_HOUR, scheduleId,
            startTime, startTime + HOUR - 1);
        assertEquals(actualFirstHour, firstHourMetrics, "Failed to find one hour metrics");

        List<AggregatedNumericMetric> actualSecondHour = dao.findAggregateMetrics(MetricsTable.ONE_HOUR, scheduleId,
            startTime + HOUR, endTime);
        assertEquals(actualSecondHour, secondHourMetrics, "Failed to find one hour metrics");

        List<AggregatedNumericMetric> actualAlternateScheduleIdMetrics = dao.findAggregateMetrics(
            MetricsTable.ONE_HOUR,
            alternateScheduleId);
        assertEquals(actualAlternateScheduleIdMetrics, alternateScheduleIdMetrics, "Failed to find one hour metrics");

        // verify that the TTL is set
        List<AggregatedNumericMetric> actualMetricsWithMetadata = dao.findAggregateMetricsWithMetadata(
            MetricsTable.ONE_HOUR, scheduleId, startTime, endTime);
        assertAggregateTTLSet(actualMetricsWithMetadata);

        actualMetricsWithMetadata = dao.findAggregateMetricsWithMetadata(MetricsTable.ONE_HOUR, alternateScheduleId,
            startTime, endTime);
        assertAggregateTTLSet(actualMetricsWithMetadata);
    }

    @Test(enabled = ENABLED)
    public void randomizedSimpleInsertAndFindMetrics() {

        long startTime = System.currentTimeMillis();
        long endTime = startTime + 2 * HOUR;

        int scheduleId = 1;
        int alternateScheduleId = 2;
        int ttl = Hours.TWO.toStandardSeconds().getSeconds();

        MetricsTable[] tablesToTest = new MetricsTable[] { MetricsTable.ONE_HOUR, MetricsTable.SIX_HOUR,
            MetricsTable.TWENTY_FOUR_HOUR };

        for (MetricsTable table : tablesToTest) {
            //insert data
            List<AggregatedNumericMetric> metrics = this.generateRandomAggregatedMetrics(scheduleId, 2, startTime);
            dao.insertAggregates(table, metrics, ttl);

            List<AggregatedNumericMetric> alternateMetrics = this.generateRandomAggregatedMetrics(alternateScheduleId,
                3, startTime);
            dao.insertAggregates(table, alternateMetrics, ttl);

            //verify data can be retrieve
            List<AggregatedNumericMetric> actualMetrics = dao.findAggregateMetrics(table, scheduleId);
            assertEquals(actualMetrics, metrics, "Failed to find metrics in " + table + " table.");

            List<AggregatedNumericMetric> actualAlternateMetrics = dao.findAggregateMetrics(table, alternateScheduleId);
            assertEquals(actualAlternateMetrics, alternateMetrics, "Failed to find metrics in " + table + " table.");

            // verify that the TTL is set
            List<AggregatedNumericMetric> actualMetricsWithMetadata = dao.findAggregateMetricsWithMetadata(table,
                scheduleId, startTime, endTime);
            assertAggregateTTLSet(actualMetricsWithMetadata);
        }
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
    private List<AggregatedNumericMetric> generateRandomAggregatedMetrics(int scheduleId,
        int numberOfAggregatedMetrics, long startTime) {
        double max;
        double min;
        double average;
        double temp;

        Random random = new Random();
        List<AggregatedNumericMetric> generatedMetrics = new ArrayList<AggregatedNumericMetric>();

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

            generatedMetrics.add(new AggregatedNumericMetric(scheduleId, average, min, max, startTime + i));
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

    private void assertAggregateTTLSet(List<AggregatedNumericMetric> metrics) {
        for (AggregatedNumericMetric metric : metrics) {
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

}
