package org.rhq.server.metrics;

import static java.util.Arrays.asList;
import static org.rhq.test.AssertUtils.assertCollectionEqualsNoOrder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.MetricsIndexEntry;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * @author John Sanda
 */
public class AggregatorTest extends MetricsTest {

    private static final boolean ENABLED = true;

    private class AggregatorTestStub extends Aggregator {

        public AggregatorTestStub(ListeningExecutorService workers, MetricsDAO dao, MetricsConfiguration configuration,
            DateTimeService dtService, DateTime startTime) {
            super(workers, dao, configuration, dtService, startTime);
        }

        @Override
        protected DateTime currentHour() {
            return currentHour;
        }
    }

    private DateTime currentHour;

    private ListeningExecutorService workers;

    @BeforeClass
    public void initClass() {
        workers = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
    }

    @BeforeMethod
    public void initTest() throws Exception {
        purgeDB();
    }

    @Test
    public void runAggregationDuringHour3() throws Exception {
        int scheduleId1 = 123;
        int scheduleId2 = 456;
        DateTime hour3 = hour0().plusHours(3);
        currentHour = hour0().plusHours(4);

        AggregatorTestStub aggregator = new AggregatorTestStub(workers, dao, configuration, dateTimeService, hour3);

        Set<MeasurementDataNumeric> rawMetrics = ImmutableSet.of(
            new MeasurementDataNumeric(hour3.plusMinutes(20).getMillis(), scheduleId1, 2.42),
            new MeasurementDataNumeric(hour3.plusMinutes(40).getMillis(), scheduleId1, 5.9),
            new MeasurementDataNumeric(hour3.plusMinutes(15).getMillis(), scheduleId2, 0.0032),
            new MeasurementDataNumeric(hour3.plusMinutes(30).getMillis(), scheduleId2, 0.105)
        );

        WaitForWrite waitForRawInserts = insertRawData(rawMetrics);
        waitForRawInserts.await("Failed to insert raw data");

        WaitForWrite waitForIndexUpdates = new WaitForWrite(2);
        StorageResultSetFuture indexFuture1 = dao.updateMetricsIndex(MetricsTable.ONE_HOUR, scheduleId1,
            hour3.getMillis());
        StorageResultSetFuture indexFuture2 = dao.updateMetricsIndex(MetricsTable.ONE_HOUR, scheduleId2,
            hour3.getMillis());
        Futures.addCallback(indexFuture1, waitForIndexUpdates);
        Futures.addCallback(indexFuture2, waitForIndexUpdates);
        waitForIndexUpdates.await("Failed to update metrics index for raw data");

        aggregator.run();
    }

    private WaitForWrite insertRawData(Set<MeasurementDataNumeric> rawMetrics) {
        WaitForWrite waitForRawInserts = new WaitForWrite(rawMetrics.size());
        for (MeasurementDataNumeric raw : rawMetrics) {
            StorageResultSetFuture resultSetFuture = dao.insertRawData(raw);
            Futures.addCallback(resultSetFuture, waitForRawInserts);
        }
        return waitForRawInserts;
    }

    @Test(enabled = ENABLED)
    public void aggregateRawDataDuring9thHour() throws Exception {
        int scheduleId = 123;

        DateTime hour0 = hour0();
        DateTime hour9 = hour0.plusHours(9);
        DateTime hour8 = hour9.minusHours(1);

        currentHour = hour9;
        AggregatorTestStub aggregator = new AggregatorTestStub(workers, dao, configuration, dateTimeService, hour8);

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

        WaitForWrite waitForRawInserts = new WaitForWrite(rawMetrics.size());
        for (MeasurementDataNumeric raw : rawMetrics) {
            StorageResultSetFuture resultSetFuture = dao.insertRawData(raw);
            Futures.addCallback(resultSetFuture, waitForRawInserts);
        }
        waitForRawInserts.await("Failed to insert raw data");

        WaitForWrite waitForIndexUpdates = new WaitForWrite(1);
        StorageResultSetFuture indexFuture = dao.updateMetricsIndex(MetricsTable.ONE_HOUR, scheduleId, hour8.getMillis());
        Futures.addCallback(indexFuture, waitForIndexUpdates);
        waitForIndexUpdates.await("Failed to update metrics index for raw data");

        Set<AggregateNumericMetric> oneHourAggregates = aggregator.run();

        List<AggregateNumericMetric> expectedAggregates = asList(new AggregateNumericMetric(scheduleId,
            divide((1.1 + 2.2 + 3.3), 3), firstValue, thirdValue, hour8.getMillis()));

        assertCollectionEqualsNoOrder(expectedAggregates, oneHourAggregates,
            "The aggregator did not return the correct one hour aggregates");

        // verify that the 1 hour aggregates are calculated
        assert1HourDataEquals(scheduleId, asList(new AggregateNumericMetric(scheduleId, divide((1.1 + 2.2 + 3.3), 3),
            firstValue, thirdValue, hour8.getMillis())));

        // verify that the 6 hour index is updated
        List<MetricsIndexEntry> expected6HourIndex = asList(new MetricsIndexEntry(MetricsTable.SIX_HOUR,
            dateTimeService.getTimeSlice(hour9, configuration.getOneHourTimeSliceDuration()), scheduleId));

        assertMetricsIndexEquals(MetricsTable.SIX_HOUR, hour9.minusHours(3).getMillis(), expected6HourIndex,
            "Failed to update index for " + MetricsTable.SIX_HOUR);

        // The 6 hour data should not get aggregated since the current 6 hour time slice,
        // 06:00 - 12:00, has not yet passed.
        assert6HourDataEmpty(scheduleId);

        // verify that the 24 hour index is empty
        assert24HourMetricsIndexEmpty(hour0);

        // verify that the 1 hour queue has been purged
        assert1HourMetricsIndexEmpty(hour8);
    }

    @Test(enabled = ENABLED)
    public void aggregate6HourDataDuring24thHour() throws Exception {
        // set up the test fixture
        int scheduleId = 123;

        DateTime hour0 = hour0();
        DateTime hour12 = hour0.plusHours(12);
        DateTime hour6 = hour0.plusHours(6);
        DateTime hour24 = hour0.plusHours(24);

        currentHour = hour24;
        AggregatorTestStub aggregator = new AggregatorTestStub(workers, dao, configuration, dateTimeService,
            hour24.minusHours(1));

        double min1 = 1.1;
        double avg1 = 2.2;
        double max1 = 3.3;

        double min2 = 4.4;
        double avg2 = 5.5;
        double max2 = 6.6;

        // insert 6 hour data to be aggregated
        List<AggregateNumericMetric> sixHourMetrics = asList(
            new AggregateNumericMetric(scheduleId, avg1, min1, max1, hour6.getMillis()),
            new AggregateNumericMetric(scheduleId, avg2, min2, max2, hour12.getMillis())
        );
        for (AggregateNumericMetric metric : sixHourMetrics) {
            dao.insertSixHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertSixHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertSixHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }

        // update the 24 queue
        Map<Integer, Long> indexUpdates = new HashMap<Integer, Long>();
        indexUpdates.put(scheduleId, hour0.getMillis());
        dao.updateMetricsIndex(MetricsTable.TWENTY_FOUR_HOUR, indexUpdates);

        // execute the system under test
        aggregator.run();

        // verify the results
        // verify that the 6 hour data is aggregated
        assert24HourDataEquals(scheduleId, asList(new AggregateNumericMetric(scheduleId, divide(avg1 + avg2, 2),
            min1, max2, hour0.getMillis())));

        // verify that the 24 hour queue is updated
        assert24HourMetricsIndexEmpty(hour0);
    }

    @Test//(enabled = ENABLED)
    public void aggregate1HourDataDuring12thHour() throws Exception {
        // set up the test fixture
        int scheduleId = 123;

        DateTime hour0 = hour0();
        DateTime hour12 = hour0.plusHours(12);
        DateTime hour6 = hour0.plusHours(6);
        DateTime hour7 = hour0.plusHours(7);
        DateTime hour8 = hour0.plusHours(8);

        currentHour = hour12;
        AggregatorTestStub aggregator = new AggregatorTestStub(workers, dao, configuration, dateTimeService,
            hour12.minusHours(1));

        double min1 = 1.1;
        double avg1 = 2.2;
        double max1 = 9.9;

        double min2 = 4.4;
        double avg2 = 5.5;
        double max2 = 6.6;

        // insert one hour data to be aggregated
        List<AggregateNumericMetric> oneHourMetrics = asList(
            new AggregateNumericMetric(scheduleId, avg1, min1, max1, hour7.getMillis()),
            new AggregateNumericMetric(scheduleId, avg2, min2, max2, hour8.getMillis())
        );
        for (AggregateNumericMetric metric : oneHourMetrics) {
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }

        // update the 6 hour queue
        Map<Integer, Long> indexUpdates = new HashMap<Integer, Long>();
        indexUpdates.put(scheduleId, hour6.getMillis());
        dao.updateMetricsIndex(MetricsTable.SIX_HOUR, indexUpdates);

        aggregator.run();

        // verify the results
        // verify that the one hour data has been aggregated
        assert6HourDataEquals(scheduleId, asList(new AggregateNumericMetric(scheduleId, divide((avg1 + avg2), 2), min1,
            max1, hour6.getMillis())));

        // verify that the 6 hour queue has been updated
        assert6HourMetricsIndexEmpty(hour6);

        // verify that the 24 hour queue is updated
        assertMetricsIndexEquals(MetricsTable.TWENTY_FOUR_HOUR, hour0.getMillis(), asList(new MetricsIndexEntry(
            MetricsTable.TWENTY_FOUR_HOUR, hour0, scheduleId)), "Failed to update index for "
            + MetricsTable.TWENTY_FOUR_HOUR);

        // verify that 6 hour data is not rolled up into the 24 hour bucket
        assert24HourDataEmpty(scheduleId);
    }

}
