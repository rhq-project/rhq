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
import static org.rhq.server.metrics.DateTimeService.ONE_MONTH;
import static org.rhq.server.metrics.DateTimeService.ONE_YEAR;
import static org.rhq.server.metrics.DateTimeService.SEVEN_DAYS;
import static org.rhq.server.metrics.DateTimeService.TWO_WEEKS;
import static org.rhq.server.metrics.MetricsDAO.METRICS_INDEX_TABLE;
import static org.rhq.server.metrics.MetricsDAO.ONE_HOUR_METRICS_TABLE;
import static org.rhq.server.metrics.MetricsDAO.RAW_METRICS_TABLE;
import static org.rhq.server.metrics.MetricsDAO.SIX_HOUR_METRICS_TABLE;
import static org.rhq.server.metrics.MetricsDAO.TWENTY_FOUR_HOUR_METRICS_TABLE;
import static org.rhq.server.metrics.MetricsServer.RAW_TTL;
import static org.rhq.server.metrics.MetricsServer.divide;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;

import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.ColumnSliceIterator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.SliceQuery;

/**
 * @author John Sanda
 */
@Listeners({CassandraClusterManager.class})
public class MetricsServerTest extends CassandraIntegrationTest {

    private static final boolean ENABLED = false;

    private final Log log = LogFactory.getLog(MetricsServerTest.class);

    private final long SECOND = 1000;

    private final long MINUTE = 60 * SECOND;

    private final String RAW_METRIC_DATA_CF = "raw_metrics";

    private final String ONE_HOUR_METRIC_DATA_CF = "one_hour_metrics";

    private final String SIX_HOUR_METRIC_DATA_CF = "six_hour_metrics";

    private final String TWENTY_FOUR_HOUR_METRIC_DATA_CF = "twenty_four_hour_metrics";

    private final String METRICS_INDEX = "metrics_index";

    private final String TRAITS_CF = "traits";

    private final String RESOURCE_TRAITS_CF = "resource_traits";

    private MetricsServerStub metricsServer;

    private Keyspace keyspace;

    private MetricsDAO dao;

    private static class MetricsServerStub extends MetricsServer {
        private DateTime currentHour;

        public void setCurrentHour(DateTime currentHour) {
            this.currentHour = currentHour;
        }

        @Override
        protected DateTime getCurrentHour() {
            if (currentHour == null) {
                return super.getCurrentHour();
            }
            return currentHour;
        }
    }

    @BeforeMethod
    public void initServer() throws Exception {
        Cluster cluster = HFactory.getOrCreateCluster("rhq", "127.0.0.1:9160");
        keyspace = HFactory.createKeyspace("rhq", cluster);

        metricsServer = new MetricsServerStub();
        metricsServer.setCluster(cluster);
        metricsServer.setKeyspace(keyspace);
        metricsServer.setRawMetricsDataCF(RAW_METRIC_DATA_CF);
        metricsServer.setOneHourMetricsDataCF(ONE_HOUR_METRIC_DATA_CF);
        metricsServer.setSixHourMetricsDataCF(SIX_HOUR_METRIC_DATA_CF);
        metricsServer.setTwentyFourHourMetricsDataCF(TWENTY_FOUR_HOUR_METRIC_DATA_CF);
        metricsServer.setMetricsIndex(METRICS_INDEX);
        metricsServer.setTraitsCF(TRAITS_CF);
        metricsServer.setResourceTraitsCF(RESOURCE_TRAITS_CF);
        metricsServer.setCassandraDS(dataSource);

        dao = new MetricsDAO(dataSource);

        purgeDB();
    }

    private void purgeDB() throws SQLException {
//        deleteAllRows(METRICS_INDEX, StringSerializer.get());
//        deleteAllRows(RAW_METRIC_DATA_CF, IntegerSerializer.get());
//        deleteAllRows(ONE_HOUR_METRIC_DATA_CF, IntegerSerializer.get());
//        deleteAllRows(SIX_HOUR_METRIC_DATA_CF, IntegerSerializer.get());
//        deleteAllRows(TWENTY_FOUR_HOUR_METRIC_DATA_CF, IntegerSerializer.get());
//        deleteAllRows(TRAITS_CF, IntegerSerializer.get());
//        deleteAllRows(RESOURCE_TRAITS_CF, IntegerSerializer.get());
        Statement statement = connection.createStatement();
        statement.executeUpdate("TRUNCATE " + RAW_METRICS_TABLE);
        statement.executeUpdate("TRUNCATE " + ONE_HOUR_METRICS_TABLE);
        statement.executeUpdate("TRUNCATE " + SIX_HOUR_METRICS_TABLE);
        statement.executeUpdate("TRUNCATE " + METRICS_INDEX_TABLE);

        statement.close();
    }

    @Test//(enabled = ENABLED)
    public void insertMultipleRawNumericDataForOneSchedule() throws Exception {
        int scheduleId = 123;

        DateTime hour0 = hour0();
        DateTime currentTime = hour0.plusHours(4).plusMinutes(44);
        DateTime threeMinutesAgo = currentTime.minusMinutes(3);
        DateTime twoMinutesAgo = currentTime.minusMinutes(2);
        DateTime oneMinuteAgo = currentTime.minusMinutes(1);

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(threeMinutesAgo.getMillis(), scheduleId, 3.2));
        data.add(new MeasurementDataNumeric(twoMinutesAgo.getMillis(), scheduleId, 3.9));
        data.add(new MeasurementDataNumeric(oneMinuteAgo.getMillis(), scheduleId, 2.6));

        long timestamp = System.currentTimeMillis();
        metricsServer.addNumericData(data);

        List<RawNumericMetric> actual = dao.findRawMetrics(scheduleId, hour0.plusHours(4), hour0.plusHours(5));
        List<RawNumericMetric> expected = asList(
            new RawNumericMetric(scheduleId, threeMinutesAgo.getMillis(), 3.2),
            new RawNumericMetric(scheduleId, twoMinutesAgo.getMillis(), 3.9),
            new RawNumericMetric(scheduleId, oneMinuteAgo.getMillis(), 2.6)
        );

        assertEquals(actual, expected, "Failed to retrieve raw metric data");
        assertColumnMetadataEquals(scheduleId, hour0.plusHours(4), hour0.plusHours(5), RAW_TTL, timestamp);

        List<MetricsIndexEntry> expectedIndex = asList(new MetricsIndexEntry(ONE_HOUR_METRIC_DATA_CF,
            hour0.plusHours(4), scheduleId));
            assertMetricsIndexEquals(ONE_HOUR_METRIC_DATA_CF, expectedIndex, "Failed to update index for " +
                ONE_HOUR_METRIC_DATA_CF);
    }

    @Test//(enabled = ENABLED)
    public void calculateAggregatesForOneScheduleWhenDBIsEmpty() {
        int scheduleId = 123;

        DateTime hour0 = hour0();
        DateTime hour6 = hour0.plusHours(6);
        DateTime lastHour = hour6.minusHours(1);
        DateTime firstMetricTime = hour6.minusMinutes(3);
        DateTime secondMetricTime = hour6.minusMinutes(2);
        DateTime thirdMetricTime = hour6.minusMinutes(1);

        String scheduleName = getClass().getName() + "_SCHEDULE";
        long interval = MINUTE * 15;
        boolean enabled = true;
        DataType dataType = DataType.MEASUREMENT;
        MeasurementScheduleRequest request = new MeasurementScheduleRequest(scheduleId, scheduleName, interval,
            enabled, dataType);

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(firstMetricTime.getMillis(), request, 3.2));
        data.add(new MeasurementDataNumeric(secondMetricTime.getMillis(), request, 3.9));
        data.add(new MeasurementDataNumeric(thirdMetricTime.getMillis(), request, 2.6));

        metricsServer.setCurrentHour(hour6);
        metricsServer.addNumericData(data);
        metricsServer.calculateAggregates();

        // verify one hour metric data is calculated
        // The ttl for 1 hour data is 14 days.
//        int ttl = Days.days(14).toStandardSeconds().getSeconds();
//        List<HColumn<Composite, Double>> expected1HourData = asList(
//            HFactory.createColumn(createAggregateKey(lastHour, AggregateType.MAX), 3.9, ttl, CompositeSerializer.get(),
//                DoubleSerializer.get()),
//            HFactory.createColumn(createAggregateKey(lastHour, AggregateType.MIN), 2.6, ttl, CompositeSerializer.get(),
//                DoubleSerializer.get()),
//            HFactory.createColumn(createAggregateKey(lastHour, AggregateType.AVG), (3.9 + 3.2 + 2.6) / 3, ttl,
//                CompositeSerializer.get(), DoubleSerializer.get())
//        );
//
//        assert1HourDataEquals(scheduleId, expected1HourData);

        // verify six hour metric data is calculated
        // the ttl for 6 hour data is 31 days
//        ttl = Days.days(31).toStandardSeconds().getSeconds();
//        List<HColumn<Composite, Double>> expected6HourData = asList(
//            HFactory.createColumn(createAggregateKey(hour0, AggregateType.MAX), 3.9, ttl, CompositeSerializer.get(),
//                DoubleSerializer.get()),
//            HFactory.createColumn(createAggregateKey(hour0, AggregateType.MIN), 2.6, ttl, CompositeSerializer.get(),
//                DoubleSerializer.get()),
//            HFactory.createColumn(createAggregateKey(hour0, AggregateType.AVG), (3.9 + 3.2 + 2.6) / 3, ttl,
//                CompositeSerializer.get(), DoubleSerializer.get())
//        );
//
//        assert6HourDataEquals(scheduleId, expected6HourData);

        // verify that one hour metric data is updated
        List<AggregatedNumericMetric> expected = asList(new AggregatedNumericMetric(scheduleId,
            divide((3.9 + 3.2 + 2.6), 3), 2.6, 3.9, lastHour.getMillis()));
        assertMetricDataEquals(ONE_HOUR_METRICS_TABLE, scheduleId, expected);

        // verify that 6 hour metric data is updated
        assertMetricDataEquals(SIX_HOUR_METRICS_TABLE, scheduleId, asList(new AggregatedNumericMetric(scheduleId,
            divide((3.9 + 3.2 + 2.6), 3), 2.6, 3.9, hour0.getMillis())));

        // TODO verify that 24 hour data is *not* updated
    }

    @Test//(enabled = ENABLED)
    public void aggregateRawDataDuring9thHour() {
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

        Set<MeasurementDataNumeric> rawMetrics = new HashSet<MeasurementDataNumeric>();
        rawMetrics.add(new MeasurementDataNumeric(firstMetricTime.getMillis(), scheduleId, firstValue));
        rawMetrics.add(new MeasurementDataNumeric(secondMetricTime.getMillis(), scheduleId, secondValue));
        rawMetrics.add(new MeasurementDataNumeric(thirdMetricTime.getMillis(), scheduleId, thirdValue));

        long timestamp = System.currentTimeMillis();
        Set<MeasurementDataNumeric> insertedRawMetrics = dao.insertRawMetrics(rawMetrics, RAW_TTL, timestamp);
        metricsServer.updateMetricsIndex(insertedRawMetrics);

        // insert raw data to be aggregated
//        Mutator<Integer> rawMetricsMutator = HFactory.createMutator(keyspace, IntegerSerializer.get());
//        rawMetricsMutator.addInsertion(scheduleId, RAW_METRIC_DATA_CF, createRawDataColumn(firstMetricTime,
//            firstValue));
//        rawMetricsMutator.addInsertion(scheduleId, RAW_METRIC_DATA_CF,
//            createRawDataColumn(secondMetricTime, secondValue));
//        rawMetricsMutator.addInsertion(scheduleId, RAW_METRIC_DATA_CF, createRawDataColumn(thirdMetricTime,
//            thirdValue));
//
//        rawMetricsMutator.execute();

        // update the one hour queue
//        Mutator<String> queueMutator = HFactory.createMutator(keyspace, StringSerializer.get());
//        Composite key = createQueueColumnName(hour8, scheduleId);
//        HColumn<Composite, Integer> oneHourQueueColumn = HFactory.createColumn(key, 0, CompositeSerializer.get(),
//            IntegerSerializer.get());
//        queueMutator.addInsertion(ONE_HOUR_METRIC_DATA_CF, METRICS_INDEX, oneHourQueueColumn);
//
//        queueMutator.execute();

        metricsServer.setCurrentHour(hour9);
        metricsServer.calculateAggregates();

        // verify that the 1 hour aggregates are calculated
        assertMetricDataEquals(ONE_HOUR_METRICS_TABLE, scheduleId, asList(
            new AggregatedNumericMetric(scheduleId, divide((1.1 + 2.2 + 3.3), 3), firstValue, thirdValue,
                hour8.getMillis())
        ));

        // verify that the 6 hour index is updated
        DateTimeService dateTimeService = new DateTimeService();
        List<MetricsIndexEntry> expected6HourIndex = asList(new MetricsIndexEntry(SIX_HOUR_METRICS_TABLE,
            dateTimeService.getTimeSlice(hour9, Minutes.minutes(60 * 6)), scheduleId));

        assertMetricsIndexEquals(SIX_HOUR_METRICS_TABLE, expected6HourIndex, "Failed to update index for " +
            SIX_HOUR_METRICS_TABLE);

        // The 6 hour data should not get aggregated since the current 6 hour time slice
        // has not passed yet. More specifically, the aggregation job is running at 09:00
        // which means that the current 6 hour slice is from 06:00 to 12:00.
        assert6HourDataEmpty(scheduleId);

        // verify that the 24 hour index is empty
        assert24HourMetricsIndexEmpty(scheduleId);

        // verify that the 1 hour queue has been purged
        assert1HourMetricsIndexEmpty(scheduleId);
    }

    @Test(enabled = ENABLED)
    public void aggregate1HourDataDuring12thHour() {
        // set up the test fixture
        int scheduleId = 123;

        DateTime now = new DateTime();
        DateTime hour0 = hour0();
        DateTime hour12 = hour0.plusHours(12);
        DateTime hour6 = hour0.plusHours(6);
        DateTime hour7 = hour0.plusHours(7);
        DateTime hour8 = hour0.plusHours(8);

        double min1 = 1.1;
        double avg1 = 2.2;
        //double max1 = 3.3;
        double max1 = 9.9;

        double min2 = 4.4;
        double avg2 = 5.5;
        double max2 = 6.6;

        // insert one hour data to be aggregated
        Mutator<Integer> oneHourMutator = HFactory.createMutator(keyspace, IntegerSerializer.get());
        oneHourMutator.addInsertion(scheduleId, ONE_HOUR_METRIC_DATA_CF, create1HourColumn(hour7, AggregateType.MAX,
            max1));
        oneHourMutator.addInsertion(scheduleId, ONE_HOUR_METRIC_DATA_CF, create1HourColumn(hour7, AggregateType.MIN,
            min1));
        oneHourMutator.addInsertion(scheduleId, ONE_HOUR_METRIC_DATA_CF, create1HourColumn(hour7, AggregateType.AVG,
            avg1));
        oneHourMutator.addInsertion(scheduleId, ONE_HOUR_METRIC_DATA_CF, create1HourColumn(hour8, AggregateType.MAX,
            max2));
        oneHourMutator.addInsertion(scheduleId, ONE_HOUR_METRIC_DATA_CF, create1HourColumn(hour8, AggregateType.MIN,
            min2));
        oneHourMutator.addInsertion(scheduleId, ONE_HOUR_METRIC_DATA_CF, create1HourColumn(hour8, AggregateType.AVG,
            avg2));
        oneHourMutator.execute();

        // update the 6 hour queue
        Mutator<String> queueMutator = HFactory.createMutator(keyspace, StringSerializer.get());
        Composite key = createQueueColumnName(hour6, scheduleId);
        HColumn<Composite, Integer> sixHourQueueColumn = HFactory.createColumn(key, 0, CompositeSerializer.get(),
            IntegerSerializer.get());
        queueMutator.addInsertion(SIX_HOUR_METRIC_DATA_CF, METRICS_INDEX, sixHourQueueColumn);

        queueMutator.execute();

        // execute the system under test
        metricsServer.setCurrentHour(hour12);
        metricsServer.calculateAggregates();

        // verify the results
        // verify that the one hour data has been aggregated
        assert6HourDataEquals(scheduleId, asList(
            create6HourColumn(hour6, AggregateType.MAX, max1),
            create6HourColumn(hour6, AggregateType.MIN, min1),
            create6HourColumn(hour6, AggregateType.AVG, (avg1 + avg2) / 2)
        ));

        // verify that the 6 hour queue has been updated
        assert6HourMetricsIndexEmpty(scheduleId);

        // verify that the 24 hour queue is updated
        assert24HourMetricsQueueEquals(asList(HFactory.createColumn(createQueueColumnName(hour0, scheduleId), 0,
            CompositeSerializer.get(), IntegerSerializer.get())));

        // verify that 6 hour data is not rolled up into the 24 hour bucket
        assert24HourDataEmpty(scheduleId);
    }

    @Test(enabled = ENABLED)
    public void aggregate6HourDataDuring24thHour() {
        // set up the test fixture
        int scheduleId = 123;

        DateTime now = new DateTime();
        DateTime hour0 = hour0();
        DateTime hour12 = hour0.plusHours(12);
        DateTime hour6 = hour0.plusHours(6);
        DateTime hour24 = hour0.plusHours(24);

        double min1 = 1.1;
        double avg1 = 2.2;
        double max1 = 3.3;

        double min2 = 4.4;
        double avg2 = 5.5;
        double max2 = 6.6;

        // insert 6 hour data to be aggregated
        Mutator<Integer> sixHourMutator = HFactory.createMutator(keyspace, IntegerSerializer.get());
        sixHourMutator.addInsertion(scheduleId, SIX_HOUR_METRIC_DATA_CF, create6HourColumn(hour6, AggregateType.MAX,
            max1));
        sixHourMutator.addInsertion(scheduleId, SIX_HOUR_METRIC_DATA_CF, create6HourColumn(hour6, AggregateType.MIN,
            min1));
        sixHourMutator.addInsertion(scheduleId, SIX_HOUR_METRIC_DATA_CF, create6HourColumn(hour6, AggregateType.AVG,
            avg1));
        sixHourMutator.addInsertion(scheduleId, SIX_HOUR_METRIC_DATA_CF, create6HourColumn(hour12, AggregateType.MAX,
            max2));
        sixHourMutator.addInsertion(scheduleId, SIX_HOUR_METRIC_DATA_CF, create6HourColumn(hour12, AggregateType.MIN,
            min2));
        sixHourMutator.addInsertion(scheduleId, SIX_HOUR_METRIC_DATA_CF, create6HourColumn(hour12, AggregateType.AVG,
            avg2));
        sixHourMutator.execute();

        // update the 24 queue
        Mutator<String> queueMutator = HFactory.createMutator(keyspace, StringSerializer.get());
        Composite key = createQueueColumnName(hour0, scheduleId);
        HColumn<Composite, Integer> twentyFourHourQueueColumn = HFactory.createColumn(key, 0, CompositeSerializer.get(),
            IntegerSerializer.get());
        queueMutator.addInsertion(TWENTY_FOUR_HOUR_METRIC_DATA_CF, METRICS_INDEX, twentyFourHourQueueColumn);

        queueMutator.execute();

        // execute the system under test
        metricsServer.setCurrentHour(hour24);
        metricsServer.calculateAggregates();

        // verify the results
        // verify that the 6 hour data is aggregated
        assert24HourDataEquals(scheduleId, asList(
            create24HourColumn(hour0, AggregateType.MAX, max2),
            create24HourColumn(hour0, AggregateType.MIN, min1),
            create24HourColumn(hour0, AggregateType.AVG, (avg1 + avg2) / 2)
        ));

        // verify that the 24 hour queue is updated
        assert24HourMetricsIndexEmpty(scheduleId);
    }

    @Test(enabled = ENABLED)
    public void findRawDataComposites() {
        DateTime beginTime = now().minusHours(4);
        DateTime endTime = now();

        Buckets buckets = new Buckets(beginTime, endTime);

        String scheduleName = getClass().getName() + "_SCHEDULE";
        MeasurementSchedule schedule = new MeasurementSchedule();
        schedule.setId(123);
        long interval = MINUTE * 10;
        boolean enabled = true;
        DataType dataType = DataType.MEASUREMENT;
        MeasurementScheduleRequest request = new MeasurementScheduleRequest(schedule.getId(), scheduleName, interval,
            enabled, dataType);

        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 10, request, 1.1));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 20, request, 2.2));
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() + 30, request, 3.3));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 10, request, 4.4));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 20, request, 5.5));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + 30, request, 6.6));

        // add some data outside the range
        data.add(new MeasurementDataNumeric(buckets.get(0).getStartTime() - 100, request, 1.23));
        data.add(new MeasurementDataNumeric(buckets.get(59).getStartTime() + buckets.getInterval() + 50, request,
            4.56));

        metricsServer.addNumericData(data);
        List<MeasurementDataNumericHighLowComposite> actualData = metricsServer.findDataForContext(null, null,
            schedule, beginTime.getMillis(), endTime.getMillis());

        assertEquals(actualData.size(), buckets.getNumDataPoints(), "Expected to get back 60 data points.");

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0).getStartTime(), (1.1 + 2.2 + 3.3) / 3, 3.3, 1.1);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59).getStartTime(), (4.4 + 5.5 + 6.6) / 3, 6.6, 4.4);
        MeasurementDataNumericHighLowComposite expectedBucket29Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(29).getStartTime(), Double.NaN, Double.NaN, Double.NaN);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actualData.get(0));
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actualData.get(59));
        assertPropertiesMatch("The data for bucket 29 does not match the expected values.", expectedBucket29Data,
            actualData.get(29));
    }

    @Test(enabled = ENABLED)
    public void find1HourDataComposites() {
        DateTime beginTime = now().minusDays(11);
        DateTime endTime = now();

        Buckets buckets = new Buckets(beginTime, endTime);
        DateTime bucket0Time = new DateTime(buckets.get(0).getStartTime());
        DateTime bucket59Time = new DateTime(buckets.get(59).getStartTime());

        String scheduleName = getClass().getName() + "_SCHEDULE";
        MeasurementSchedule schedule = new MeasurementSchedule();
        schedule.setId(123);
        long interval = MINUTE * 10;
        boolean enabled = true;
        DataType dataType = DataType.MEASUREMENT;
        MeasurementScheduleRequest request = new MeasurementScheduleRequest(schedule.getId(), scheduleName, interval,
            enabled, dataType);

        // insert one hour data to be aggregated
        Mutator<Integer> oneHourMutator = HFactory.createMutator(keyspace, IntegerSerializer.get());
        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket0Time, AggregateType.MAX, 3.0));
        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket0Time, AggregateType.AVG, 2.0));
        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket0Time, AggregateType.MIN, 1.0));

        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket0Time.plusHours(1), AggregateType.MAX, 6.0));
        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket0Time.plusHours(1), AggregateType.AVG, 5.0));
        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket0Time.plusHours(1), AggregateType.MIN, 4.0));

        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket0Time.plusHours(2), AggregateType.MAX, 3.0));
        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket0Time.plusHours(2), AggregateType.AVG, 3.0));
        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket0Time.plusHours(2), AggregateType.MIN, 3.0));

        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket59Time, AggregateType.MAX, 9.0));
        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket59Time, AggregateType.AVG, 5.0));
        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket59Time, AggregateType.MIN, 2.0));

        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket59Time.plusHours(1), AggregateType.MAX, 6.0));
        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket59Time.plusHours(1), AggregateType.AVG, 5.0));
        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket59Time.plusHours(1), AggregateType.MIN, 4.0));

        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket59Time.plusHours(2), AggregateType.MAX, 3.0));
        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket59Time.plusHours(2), AggregateType.AVG, 3.0));
        oneHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create1HourColumn(bucket59Time.plusHours(2), AggregateType.MIN, 3.0));

        oneHourMutator.execute();

        List<MeasurementDataNumericHighLowComposite> actualData = metricsServer.findDataForContext(null, null,
            schedule, beginTime.getMillis(), endTime.getMillis());

        assertEquals(actualData.size(), buckets.getNumDataPoints(), "Expected to get back 60 data points.");

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0).getStartTime(), (2.0 + 5.0 + 3.0) / 3, 5.0, 2.0);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59).getStartTime(), (5.0 + 5.0 + 3.0) / 3, 5.0, 3.0);
        MeasurementDataNumericHighLowComposite expectedBucket29Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(29).getStartTime(), Double.NaN, Double.NaN, Double.NaN);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actualData.get(0));
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actualData.get(59));
        assertPropertiesMatch("The data for bucket 29 does not match the expected values.", expectedBucket29Data,
            actualData.get(29));
    }

    @Test(enabled = ENABLED)
    public void find6HourDataComposites() {
        DateTime beginTime = now().minusDays(20);
        DateTime endTime = now();

        Buckets buckets = new Buckets(beginTime, endTime);
        DateTime bucket0Time = new DateTime(buckets.get(0).getStartTime());
        DateTime bucket59Time = new DateTime(buckets.get(59).getStartTime());

        String scheduleName = getClass().getName() + "_SCHEDULE";
        MeasurementSchedule schedule = new MeasurementSchedule();
        schedule.setId(123);
        long interval = MINUTE * 10;
        boolean enabled = true;
        DataType dataType = DataType.MEASUREMENT;
        MeasurementScheduleRequest request = new MeasurementScheduleRequest(schedule.getId(), scheduleName, interval,
            enabled, dataType);

        // insert six hour data to be aggregated
        Mutator<Integer> sixHourMutator = HFactory.createMutator(keyspace, IntegerSerializer.get());
        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket0Time, AggregateType.MAX, 3.0));
        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket0Time, AggregateType.AVG, 2.0));
        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket0Time, AggregateType.MIN, 1.0));

        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket0Time.plusHours(1), AggregateType.MAX, 6.0));
        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket0Time.plusHours(1), AggregateType.AVG, 5.0));
        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket0Time.plusHours(1), AggregateType.MIN, 4.0));

        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket0Time.plusHours(2), AggregateType.MAX, 3.0));
        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket0Time.plusHours(2), AggregateType.AVG, 3.0));
        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket0Time.plusHours(2), AggregateType.MIN, 3.0));

        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket59Time, AggregateType.MAX, 9.0));
        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket59Time, AggregateType.AVG, 5.0));
        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket59Time, AggregateType.MIN, 2.0));

        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket59Time.plusHours(1), AggregateType.MAX, 6.0));
        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket59Time.plusHours(1), AggregateType.AVG, 5.0));
        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket59Time.plusHours(1), AggregateType.MIN, 4.0));

        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket59Time.plusHours(2), AggregateType.MAX, 3.0));
        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket59Time.plusHours(2), AggregateType.AVG, 3.0));
        sixHourMutator.addInsertion(schedule.getId(), ONE_HOUR_METRIC_DATA_CF,
            create6HourColumn(bucket59Time.plusHours(2), AggregateType.MIN, 3.0));

        sixHourMutator.execute();

        List<MeasurementDataNumericHighLowComposite> actualData = metricsServer.findDataForContext(null, null,
            schedule, beginTime.getMillis(), endTime.getMillis());

        assertEquals(actualData.size(), buckets.getNumDataPoints(), "Expected to get back 60 data points.");

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0).getStartTime(), (2.0 + 5.0 + 3.0) / 3, 5.0, 2.0);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59).getStartTime(), (5.0 + 5.0 + 3.0) / 3, 5.0, 3.0);
        MeasurementDataNumericHighLowComposite expectedBucket29Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(29).getStartTime(), Double.NaN, Double.NaN, Double.NaN);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actualData.get(0));
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actualData.get(59));
        assertPropertiesMatch("The data for bucket 29 does not match the expected values.", expectedBucket29Data,
            actualData.get(29));
    }

    private HColumn<Long, Double> createRawDataColumn(DateTime timestamp, double value) {
        return HFactory.createColumn(timestamp.getMillis(), value, SEVEN_DAYS, LongSerializer.get(),
            DoubleSerializer.get());
    }

    private Composite createQueueColumnName(DateTime dateTime, int scheduleId) {
        Composite composite = new Composite();
        composite.addComponent(dateTime.getMillis(), LongSerializer.get());
        composite.addComponent(scheduleId, IntegerSerializer.get());

        return composite;
    }

    private void assertColumnMetadataEquals(int scheduleId, DateTime startTime, DateTime endTime, Integer ttl,
        long timestamp) {
        List<RawNumericMetric> metrics = dao.findRawMetrics(scheduleId, startTime, endTime, true);
        for (RawNumericMetric metric : metrics) {
            assertEquals(metric.getColumnMetadata().getTtl(), ttl, "The TTL does not match the expected value for " +
                metric);
            assertTrue(metric.getColumnMetadata().getWriteTime() >= timestamp, "The column timestamp for " + metric +
                " should be >= " + timestamp + " but it is " + metric.getColumnMetadata().getWriteTime());
        }
    }

    private void assert1HourMetricsQueueEquals(List<HColumn<Composite, Integer>> expected) {
        assertMetricsQueueEquals(ONE_HOUR_METRIC_DATA_CF, expected);
    }

    private void assert6HourMetricsQueueEquals(List<HColumn<Composite, Integer>> expected) {
        assertMetricsQueueEquals(SIX_HOUR_METRIC_DATA_CF, expected);
    }

    private void assert24HourMetricsQueueEquals(List<HColumn<Composite, Integer>> expected) {
        assertMetricsQueueEquals(TWENTY_FOUR_HOUR_METRIC_DATA_CF, expected);
    }

    private void assertMetricsQueueEquals(String columnFamily, List<HColumn<Composite, Integer>> expected) {
        SliceQuery<String,Composite, Integer> sliceQuery = HFactory.createSliceQuery(keyspace, StringSerializer.get(),
            new CompositeSerializer().get(), IntegerSerializer.get());
        sliceQuery.setColumnFamily(METRICS_INDEX);
        sliceQuery.setKey(columnFamily);

        ColumnSliceIterator<String, Composite, Integer> iterator = new ColumnSliceIterator<String, Composite, Integer>(
            sliceQuery, (Composite) null, (Composite) null, false);

        List<HColumn<Composite, Integer>> actual = new ArrayList<HColumn<Composite, Integer>>();
        while (iterator.hasNext()) {
            actual.add(iterator.next());
        }

        assertEquals(actual.size(), expected.size(), "The number of entries in the queue do not match.");
        int i = 0;
        for (HColumn<Composite, Integer> expectedColumn :  expected) {
            HColumn<Composite, Integer> actualColumn = actual.get(i++);
            assertEquals(getTimestamp(actualColumn.getName()), getTimestamp(expectedColumn.getName()),
                "The timestamp does not match the expected value.");
            assertEquals(getScheduleId(actualColumn.getName()), getScheduleId(expectedColumn.getName()),
                "The schedule id does not match the expected value.");
        }
    }

    private void assertMetricsIndexEquals(String columnFamily, List<MetricsIndexEntry> expected, String msg) {
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            List<MetricsIndexEntry> actual = new ArrayList<MetricsIndexEntry>();
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT bucket, time, schedule_id FROM " + METRICS_INDEX +
                " WHERE bucket = '" + columnFamily + "'");
            while (resultSet.next()) {
                actual.add(new MetricsIndexEntry(resultSet.getString(1), resultSet.getDate(2), resultSet.getInt(3)));
            }
            assertCollectionMatchesNoOrder("Failed to retrieve raw metric data", expected, actual, msg + ": " +
                columnFamily + " index not match expected values.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void assert1HourDataEquals(int scheduleId, List<HColumn<Composite, Double>> expected) {
        assertMetricDataEquals(scheduleId, ONE_HOUR_METRIC_DATA_CF, expected);
    }

    private void assert6HourDataEquals(int scheduleId, List<HColumn<Composite, Double>> expected) {
        assertMetricDataEquals(scheduleId, SIX_HOUR_METRIC_DATA_CF, expected);
    }

    private void assert24HourDataEquals(int scheduleId, List<HColumn<Composite, Double>> expected) {
        assertMetricDataEquals(scheduleId, TWENTY_FOUR_HOUR_METRIC_DATA_CF, expected);
    }

    private void assertMetricDataEquals(int scheduleId, String columnFamily, List<HColumn<Composite,
        Double>> expected) {
        SliceQuery<Integer, Composite, Double> query = HFactory.createSliceQuery(keyspace, IntegerSerializer.get(),
            CompositeSerializer.get(), DoubleSerializer.get());
        query.setColumnFamily(columnFamily);
        query.setKey(scheduleId);

        ColumnSliceIterator<Integer, Composite, Double> iterator = new ColumnSliceIterator<Integer, Composite, Double>(
            query, (Composite) null, (Composite) null, false);

        List<HColumn<Composite, Double>> actual = new ArrayList<HColumn<Composite, Double>>();
        while (iterator.hasNext()) {
            actual.add(iterator.next());
        }

        String prefix;
        if (columnFamily.equals(ONE_HOUR_METRIC_DATA_CF)) {
            prefix = "The one hour data for schedule id " + scheduleId + " is wrong.";
        } else if (columnFamily.equals(SIX_HOUR_METRIC_DATA_CF)) {
            prefix = "The six hour data for schedule id " + scheduleId + " is wrong.";
        } else if (columnFamily.equals(TWENTY_FOUR_HOUR_METRIC_DATA_CF)) {
            prefix = "The twenty-four hour data for schedule id " + scheduleId + " is wrong.";
        } else {
            throw new IllegalArgumentException(columnFamily + " is not a recognized column family");
        }

        assertEquals(actual.size(), expected.size(), prefix + " The number of columns do not match.");
        int i = 0;
        for (HColumn<Composite, Double> expectedColumn : expected) {
            HColumn<Composite, Double> actualColumn = actual.get(i++);
            assertEquals(getTimestamp(actualColumn.getName()), getTimestamp(expectedColumn.getName()),
                prefix + " The timestamp does not match the expected value.");
            assertEquals(getAggregateType(actualColumn.getName()), getAggregateType(expectedColumn.getName()),
                prefix + " The column data type does not match the expected value");
            assertEquals(actualColumn.getValue(), expectedColumn.getValue(), "The column value is wrong");
            assertEquals(actualColumn.getTtl(), expectedColumn.getTtl(), "The ttl for the column is wrong.");
        }
    }

    private void assertMetricDataEquals(String columnFamily, int scheduleId, List<AggregatedNumericMetric> expected) {
        List<AggregatedNumericMetric> actual = dao.findAggregateMetrics(columnFamily, scheduleId);
        assertCollectionMatchesNoOrder(expected, actual, "Metric data for schedule id " + scheduleId +
            " in table " + columnFamily + " does not match expected values");
    }

    private void assert6HourDataEmpty(int scheduleId) {
        assertMetricDataEmpty(scheduleId, SIX_HOUR_METRICS_TABLE);
    }

    private void assert24HourDataEmpty(int scheduleId) {
        assertMetricDataEmpty(scheduleId, TWENTY_FOUR_HOUR_METRIC_DATA_CF);
    }

    private void assertMetricDataEmpty(int scheduleId, String columnFamily) {
//        SliceQuery<Integer, Composite, Double> query = HFactory.createSliceQuery(keyspace, IntegerSerializer.get(),
//            CompositeSerializer.get(), DoubleSerializer.get());
//        query.setColumnFamily(columnFamily);
//        query.setKey(scheduleId);
//
//        ColumnSliceIterator<Integer, Composite, Double> iterator = new ColumnSliceIterator<Integer, Composite, Double>(
//            query, (Composite) null, (Composite) null, false);
//
//        List<HColumn<Composite, Double>> actual = new ArrayList<HColumn<Composite, Double>>();
//        while (iterator.hasNext()) {
//            actual.add(iterator.next());
//        }
//
//        String prefix;
//        if (columnFamily.equals(ONE_HOUR_METRIC_DATA_CF)) {
//            prefix = "The one hour data for schedule id " + scheduleId + " is wrong.";
//        } else if (columnFamily.equals(SIX_HOUR_METRIC_DATA_CF)) {
//            prefix = "The six hour data for schedule id " + scheduleId + " is wrong.";
//        } else if (columnFamily.equals(TWENTY_FOUR_HOUR_METRIC_DATA_CF)) {
//            prefix = "The twenty-four hour data for schedule id " + scheduleId + " is wrong.";
//        } else {
//            throw new IllegalArgumentException(columnFamily + " is not a recognized column family");
//        }
//
//        assertEquals(actual.size(), 0, prefix + " Expected the row to be empty.");
        List<AggregatedNumericMetric> metrics = dao.findAggregateMetrics(columnFamily, scheduleId);
        assertEquals(metrics.size(), 0, "Expected " + columnFamily + " to be empty for schedule id " + scheduleId +
            " but found " + metrics);
    }

    private void assert1HourMetricsIndexEmpty(int scheduleId) {
        assertMetricsIndexEmpty(scheduleId, ONE_HOUR_METRICS_TABLE);
    }

    private void assert6HourMetricsIndexEmpty(int scheduleId) {
        assertMetricsIndexEmpty(scheduleId, SIX_HOUR_METRICS_TABLE);
    }

    private void assert24HourMetricsIndexEmpty(int scheduleId) {
        assertMetricsIndexEmpty(scheduleId, TWENTY_FOUR_HOUR_METRICS_TABLE);
    }

    private void assertMetricsIndexEmpty(int scheduleId, String table) {
//        SliceQuery<String,Composite, Integer> sliceQuery = HFactory.createSliceQuery(keyspace, StringSerializer.get(),
//            new CompositeSerializer().get(), IntegerSerializer.get());
//        sliceQuery.setColumnFamily(METRICS_INDEX);
//        sliceQuery.setKey(columnFamily);
//
//        ColumnSliceIterator<String, Composite, Integer> iterator = new ColumnSliceIterator<String, Composite, Integer>(
//            sliceQuery, (Composite) null, (Composite) null, false);
//
//        List<HColumn<Composite, Integer>> actual = new ArrayList<HColumn<Composite, Integer>>();
//        while (iterator.hasNext()) {
//            actual.add(iterator.next());
//        }
//
//        String queueName;
//        if (columnFamily.equals(ONE_HOUR_METRIC_DATA_CF)) {
//            queueName = "1 hour";
//        } else if (columnFamily.equals(SIX_HOUR_METRIC_DATA_CF)) {
//            queueName = "6 hour";
//        } else if (columnFamily.equals(TWENTY_FOUR_HOUR_METRIC_DATA_CF)) {
//            queueName = "24 hour";
//        } else {
//            throw new IllegalArgumentException(columnFamily + " is not a recognized metric data column family.");
//        }

//        assertEquals(actual.size(), 0, "Expected the " + queueName + " queue to be empty for schedule id " +
//            scheduleId);
        List<MetricsIndexEntry> index = dao.findMetricsIndexEntries(table);
        assertEquals(index.size(), 0, "Expected metrics index for " + table + " to be empty but found " + index);
    }

    private Integer getScheduleId(Composite composite) {
        return composite.get(1, IntegerSerializer.get());
    }

    private Long getTimestamp(Composite composite) {
        return composite.get(0, LongSerializer.get());
    }

    private AggregateType getAggregateType(Composite composite) {
        Integer type = composite.get(1, IntegerSerializer.get());
        return AggregateType.valueOf(type);
    }

    private HColumn<Composite, Double> create1HourColumn(DateTime dateTime, AggregateType type, double value) {
        return HFactory.createColumn(createAggregateKey(dateTime, type), value, TWO_WEEKS, CompositeSerializer.get(),
            DoubleSerializer.get());
    }

    private HColumn<Composite, Double> create6HourColumn(DateTime dateTime, AggregateType type, double value) {
        return HFactory.createColumn(createAggregateKey(dateTime, type), value, ONE_MONTH, CompositeSerializer.get(),
            DoubleSerializer.get());
    }

    private HColumn<Composite, Double> create24HourColumn(DateTime dateTime, AggregateType type, double value) {
        return HFactory.createColumn(createAggregateKey(dateTime, type), value, ONE_YEAR, CompositeSerializer.get(),
            DoubleSerializer.get());
    }

    private Composite createAggregateKey(DateTime dateTime, AggregateType type) {
        Composite composite = new Composite();
        composite.addComponent(dateTime.getMillis(), LongSerializer.get());
        composite.addComponent(type.ordinal(), IntegerSerializer.get());

        return composite;
    }
}
