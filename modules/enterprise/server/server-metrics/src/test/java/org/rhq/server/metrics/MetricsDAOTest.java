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
import static org.rhq.server.metrics.MetricsDAO.TWENTY_FOUR_HOUR_METRICS_TABLE;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

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

    private final long SECOND = 1000;

    private final long MINUTE = 60 * SECOND;

    private Session session;

    private MetricsDAO dao;

    @BeforeClass
    public void initDAO() throws Exception {
        Cluster cluster = Cluster.builder().addContactPoints("127.0.0.1", "127.0.0.2").build();
        session = cluster.connect("rhq");

        dao = new MetricsDAO(session);
    }

    @BeforeMethod
    public void resetDB() throws Exception {
        session.execute("TRUNCATE " + RAW_METRICS_TABLE);
        session.execute("TRUNCATE " + ONE_HOUR_METRICS_TABLE);
        session.execute("TRUNCATE " + SIX_HOUR_METRICS_TABLE);
        session.execute("TRUNCATE " + TWENTY_FOUR_HOUR_METRICS_TABLE);
        session.execute("TRUNCATE " + METRICS_INDEX_TABLE);
    }

    @Test
    public void insertAndFindRawMetrics() throws Exception {
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

        List<RawNumericMetric> actualMetrics = dao.findRawMetrics(scheduleId, currentHour, currentHour.plusHours(1));
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
        List<RawNumericMetric> actualMetricsWithMetadata = dao.findRawMetrics(scheduleId, currentHour,
            currentHour.plusHours(1), true) ;
        assertRawTTLSet(actualMetricsWithMetadata);
    }

    @Test
    public void insertAndFindAllOneHourMetrics() {
        int scheduleId = 1;
        DateTime hour0 = hour0();

        List<AggregatedNumericMetric> metrics = asList(
            new AggregatedNumericMetric(scheduleId, 3.0, 1.0, 8.0, hour0.getMillis()),
            new AggregatedNumericMetric(scheduleId, 4.0, 2.0, 10.0, hour0.plusHours(1).getMillis()),
            new AggregatedNumericMetric(456, 2.0, 2.0, 2.0, hour0.getMillis())
        );
        int ttl = Hours.ONE.getHours();
        List<AggregatedNumericMetric> actualUpdates = dao.insertAggregates(ONE_HOUR_METRICS_TABLE, metrics, ttl);
        List<AggregatedNumericMetric> expectedUpdates = metrics;

        assertEquals(actualUpdates, expectedUpdates, "The updates do not match the expected values");

        List<AggregatedNumericMetric> expected = asList(
            new AggregatedNumericMetric(scheduleId, 3.0, 1.0, 8.0, hour0.getMillis()),
            new AggregatedNumericMetric(scheduleId, 4.0, 2.0, 10.0, hour0.plusHours(1).getMillis())
        );
        List<AggregatedNumericMetric> actual = dao.findAggregateMetrics(ONE_HOUR_METRICS_TABLE, scheduleId);
        assertEquals(actual, expected, "Failed to find one hour metrics");

        // verify that the TTL is set
        List<AggregatedNumericMetric> actualMetricsWithMetadata = dao.findAggregateMetricsWithMetadata(
            ONE_HOUR_METRICS_TABLE, scheduleId, hour0, hour0().plusHours(1).plusSeconds(1));
        assertAggrgateTTLSet(actualMetricsWithMetadata);
    }

    @Test
    public void findRangeOfOneHourMetrics() {
        int scheduledId = 1;
        int nextScheduleId = 2;
        DateTime hour0 = hour0();
        int ttl = Hours.ONE.getHours();
        List<AggregatedNumericMetric> metrics = asList(
            new AggregatedNumericMetric(scheduledId, 2.0, 2.0, 2.0, hour0.getMillis()),
            new AggregatedNumericMetric(scheduledId, 3.0, 3.0, 3.0, hour0.plusHours(1).getMillis()),
            new AggregatedNumericMetric(scheduledId, 4.0, 4.0, 4.0, hour0.plusHours(2).getMillis()),
            new AggregatedNumericMetric(scheduledId, 5.0, 5.0, 5.0, hour0.plusHours(3).getMillis()),
            new AggregatedNumericMetric(nextScheduleId, 1.0, 1.0, 1.0, hour0.plusHours(1).getMillis())
        );
        dao.insertAggregates(ONE_HOUR_METRICS_TABLE, metrics, ttl);

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
        int scheduleId1 = 1;
        int scheduleId2 = 2;

        Map<Integer, DateTime> updates = new HashMap<Integer, DateTime>();
        updates.put(scheduleId1, hour0);
        updates.put(scheduleId2, hour0);

        dao.updateMetricsIndex(ONE_HOUR_METRICS_TABLE, updates);
        List<MetricsIndexEntry> actual = dao.findMetricsIndexEntries(ONE_HOUR_METRICS_TABLE);

        List<MetricsIndexEntry> expected = asList(new MetricsIndexEntry(ONE_HOUR_METRICS_TABLE, hour0, scheduleId1),
            new MetricsIndexEntry(ONE_HOUR_METRICS_TABLE, hour0, scheduleId2));
        assertCollectionMatchesNoOrder(expected, actual, "Failed to update or retrieve metrics index entries");
    }

    @Test
    public void purge1HourMetricsIndex() {
        int scheduleId1 = 1;
        int scheduleId2 = 2;
        Map<Integer, DateTime> updates = new HashMap<Integer, DateTime>();
        updates.put(scheduleId1, hour0());
        updates.put(scheduleId2, hour0());
        updates.put(scheduleId1, hour0().plusHours(1));
        updates.put(scheduleId2, hour0().plusHours(1));

        dao.updateMetricsIndex(ONE_HOUR_METRICS_TABLE, updates);
        dao.deleteMetricsIndexEntries(ONE_HOUR_METRICS_TABLE);

        List<MetricsIndexEntry> index = dao.findMetricsIndexEntries(ONE_HOUR_METRICS_TABLE);
        assertEquals(index.size(), 0, "Expected index for " + ONE_HOUR_METRICS_TABLE + " to be empty but found " +
            index);
    }

    private void assertRawTTLSet(List<RawNumericMetric> metrics) {
        for (RawNumericMetric metric : metrics) {
            assertNotNull(metric.getColumnMetadata(), metric + " does not contain column meta data. The meta data " +
                " must be loaded in order to verify that the TTL is set correctly.");
            assertNotNull(metric.getColumnMetadata().getTtl(), "The TTL for " + metric + " is not set.");
        }
    }

    private void assertAggrgateTTLSet(List<AggregatedNumericMetric> metrics) {
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
