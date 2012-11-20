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
import static org.rhq.server.metrics.MetricsDAO.METRICS_INDEX_TABLE;
import static org.rhq.server.metrics.MetricsDAO.ONE_HOUR_METRICS_TABLE;
import static org.rhq.server.metrics.MetricsDAO.RAW_METRICS_TABLE;
import static org.rhq.server.metrics.MetricsDAO.SIX_HOUR_METRICS_TABLE;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.testng.Assert.assertEquals;

import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;

/**
 * @author John Sanda
 */
public class MetricsDAOTest extends CassandraIntegrationTest {

    private final long SECOND = 1000;

    private final long MINUTE = 60 * SECOND;

    @BeforeMethod
    public void resetDB() throws Exception {
        Statement statement = connection.createStatement();
//        statement.executeUpdate("TRUNCATE " + RAW_METRICS_TABLE);
//        statement.executeUpdate("TRUNCATE " + ONE_HOUR_METRICS_TABLE);
//        statement.executeUpdate("TRUNCATE " + METRICS_INDEX_TABLE);
//        statement.executeUpdate("TRUNCATE " + SIX_HOUR_METRICS_TABLE);
        statement.executeUpdate("DELETE FROM " + RAW_METRICS_TABLE + " WHERE schedule_id IN (123, 456)");
        statement.executeUpdate("DELETE FROM " + ONE_HOUR_METRICS_TABLE + " WHERE schedule_id IN (123, 456)");
        statement.executeUpdate("DELETE FROM " + SIX_HOUR_METRICS_TABLE + " WHERE schedule_id IN (123, 456)");
        statement.executeUpdate("DELETE FROM " + METRICS_INDEX_TABLE + " WHERE bucket IN ('raw_metrics', 'one_hour_metrics', 'six_hour_metrics')");
    }

    @Test
    public void insertAndFindRawMetrics() {
        int scheduleId = 123;

        DateTime hour0 = hour0();
        DateTime currentTime = hour0.plusHours(4).plusMinutes(44);
        DateTime currentHour = currentTime.hourOfDay().roundFloorCopy();
        DateTime threeMinutesAgo = currentTime.minusMinutes(3);
        DateTime twoMinutesAgo = currentTime.minusMinutes(2);
        DateTime oneMinuteAgo = currentTime.minusMinutes(1);

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(threeMinutesAgo.getMillis(), scheduleId, 3.2));
        data.add(new MeasurementDataNumeric(twoMinutesAgo.getMillis(), scheduleId, 3.9));
        data.add(new MeasurementDataNumeric(oneMinuteAgo.getMillis(), scheduleId, 2.6));

        MetricsDAO dao = new MetricsDAO(dataSource);
//        int ttl = Days.days(10).toStandardSeconds().getSeconds();
        int ttl = Hours.ONE.toStandardSeconds().getSeconds();
        long timestamp = System.currentTimeMillis();
        Set<MeasurementDataNumeric> actualUpdates = dao.insertRawMetrics(data, ttl, timestamp);

        assertEquals(actualUpdates, data, "The updates do not match expected value.");

        List<RawNumericMetric> actualMetrics = dao.findRawMetrics(scheduleId, currentHour, currentHour.plusHours(1));
        List<RawNumericMetric> expectedMetrics = asList(
            new RawNumericMetric(scheduleId, threeMinutesAgo.getMillis(), 3.2),
            new RawNumericMetric(scheduleId, twoMinutesAgo.getMillis(), 3.9),
            new RawNumericMetric(scheduleId, oneMinuteAgo.getMillis(), 2.6)
        );
        assertEquals(actualMetrics, expectedMetrics, "Failed to find raw metrics");

        // Now verify that the column meta data was set. We do this separately  in order to
        // exercise both versions of the findRawMetrics method. In production code (so far),
        // we have no need to retrieve meta data when retrieving raw metrics, but we need
        // to verify that the meta data is in fact set.
        List<RawNumericMetric> actualMetricsWithMetadata = dao.findRawMetrics(scheduleId, currentHour,
            currentHour.plusHours(1), true) ;
        List<RawNumericMetric> expectedMetricsWithMetadata = asList(
            new RawNumericMetric(scheduleId, threeMinutesAgo.getMillis(), 3.2, new ColumnMetadata(ttl, timestamp)),
            new RawNumericMetric(scheduleId, twoMinutesAgo.getMillis(), 3.9, new ColumnMetadata(ttl, timestamp)),
            new RawNumericMetric(scheduleId, oneMinuteAgo.getMillis(), 2.6, new ColumnMetadata(ttl, timestamp))
        );
        assertEquals(actualMetricsWithMetadata, expectedMetricsWithMetadata, "Column meta data does not match " +
            "expected values");
    }

    @Test
    public void insertAndFindAllOneHourMetrics() {
        int scheduleId = 123;
        DateTime hour0 = hour0();

        MetricsDAO dao = new MetricsDAO(dataSource);
        List<AggregatedNumericMetric> metrics = asList(
            new AggregatedNumericMetric(scheduleId, 3.0, 1.0, 8.0, hour0.getMillis()),
            new AggregatedNumericMetric(scheduleId, 4.0, 2.0, 10.0, hour0.plusHours(1).getMillis()),
            new AggregatedNumericMetric(456, 2.0, 2.0, 2.0, hour0.getMillis())
        );

        List<AggregatedNumericMetric> actualUpdates = dao.insertAggregates(ONE_HOUR_METRICS_TABLE, metrics);
        List<AggregatedNumericMetric> expectedUpdates = metrics;

        assertEquals(actualUpdates, expectedUpdates, "The updates do not match the expected values");

        List<AggregatedNumericMetric> expected = asList(
            new AggregatedNumericMetric(scheduleId, 3.0, 1.0, 8.0, hour0.getMillis()),
            new AggregatedNumericMetric(scheduleId, 4.0, 2.0, 10.0, hour0.plusHours(1).getMillis())
        );
        List<AggregatedNumericMetric> actual = dao.findAggregateMetrics(ONE_HOUR_METRICS_TABLE, scheduleId);
        assertEquals(actual, expected, "Failed to find one hour metrics");
    }

    @Test
    public void findRangeOfOneHourMetrics() {
        int scheduledId = 123;
        DateTime hour0 = hour0();

        MetricsDAO dao = new MetricsDAO(dataSource);
        dao.insertAggregates(ONE_HOUR_METRICS_TABLE, asList(
            new AggregatedNumericMetric(scheduledId, 2.0, 2.0, 2.0, hour0.getMillis()),
            new AggregatedNumericMetric(scheduledId, 3.0, 3.0, 3.0, hour0.plusHours(1).getMillis()),
            new AggregatedNumericMetric(scheduledId, 4.0, 4.0, 4.0, hour0.plusHours(2).getMillis()),
            new AggregatedNumericMetric(scheduledId, 5.0, 5.0, 5.0, hour0.plusHours(3).getMillis()),
            new AggregatedNumericMetric(456, 1.0, 1.0, 1.0, hour0.plusHours(1).getMillis())
        ));

        DateTime startTime = hour0.plusHours(1);
        DateTime endTime = hour0.plusHours(3);
        List<AggregatedNumericMetric> expected = asList(
            new AggregatedNumericMetric(scheduledId, 3.0, 3.0, 3.0, hour0.plusHours(1).getMillis()),
            new AggregatedNumericMetric(scheduledId, 4.0, 4.0, 4.0, hour0.plusHours(2).getMillis())
        );

        List<AggregatedNumericMetric> actual = dao.findAggregateMetrics(ONE_HOUR_METRICS_TABLE, scheduledId, startTime,
            endTime);

        assertEquals(actual, expected, "Failed to find one hour metrics for date range");
    }

    @Test
    public void updateAndFindOneHourIndexEntries() {
        DateTime hour0 = hour0();

        Map<Integer, DateTime> updates = new HashMap<Integer, DateTime>();
        updates.put(100, hour0);
        updates.put(101, hour0);

        MetricsDAO dao = new MetricsDAO(dataSource);
        dao.updateMetricsIndex(ONE_HOUR_METRICS_TABLE, updates);
        List<MetricsIndexEntry> actual = dao.findMetricsIndexEntries(ONE_HOUR_METRICS_TABLE);

        List<MetricsIndexEntry> expected = asList(new MetricsIndexEntry(ONE_HOUR_METRICS_TABLE, hour0, 100),
            new MetricsIndexEntry(ONE_HOUR_METRICS_TABLE, hour0, 101));
        assertCollectionMatchesNoOrder(expected, actual, "Failed to update or retrieve metrics index entries");
    }

    @Test
    public void purge1HourMetricsIndex() {
        Map<Integer, DateTime> updates = new HashMap<Integer, DateTime>();
        updates.put(100, hour0());
        updates.put(101, hour0());
        updates.put(100, hour0().plusHours(1));
        updates.put(101, hour0().plusHours(1));

        MetricsDAO dao = new MetricsDAO(dataSource);
        dao.updateMetricsIndex(ONE_HOUR_METRICS_TABLE, updates);
        dao.deleteMetricsIndexEntries(ONE_HOUR_METRICS_TABLE);

        List<MetricsIndexEntry> index = dao.findMetricsIndexEntries(ONE_HOUR_METRICS_TABLE);
        assertEquals(index.size(), 0, "Expected index for " + ONE_HOUR_METRICS_TABLE + " to be empty but found " +
            index);
    }

}
