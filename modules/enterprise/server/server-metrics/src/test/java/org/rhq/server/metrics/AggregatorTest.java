package org.rhq.server.metrics;

import static java.util.Arrays.asList;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.MetricsIndexEntry;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * @author John Sanda
 */
public class AggregatorTest extends CassandraIntegrationTest {

    private static final boolean ENABLED = true;

    private static final double TEST_PRECISION = Math.pow(10, -9);

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

    private MetricsDAO dao;

    private DateTimeService dateTimeService;

    private MetricsConfiguration configuration = new MetricsConfiguration();

    @BeforeClass
    public void initClass() {
        workers = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
    }

    @BeforeMethod
    public void initTest() throws Exception {
        purgeDB();

        configuration = new MetricsConfiguration();

        dateTimeService = new DateTimeService();
        dateTimeService.setConfiguration(configuration);

        dao = new MetricsDAO(storageSession, configuration);
    }

    private void purgeDB() throws Exception {
        session.execute("TRUNCATE " + MetricsTable.RAW);
        session.execute("TRUNCATE " + MetricsTable.ONE_HOUR);
        session.execute("TRUNCATE " + MetricsTable.SIX_HOUR);
        session.execute("TRUNCATE " + MetricsTable.TWENTY_FOUR_HOUR);
        session.execute("TRUNCATE " + MetricsTable.INDEX);
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

        aggregator.run();

        // verify that the 1 hour aggregates are calculated
        assert1HourDataEquals(scheduleId, asList(new AggregateNumericMetric(scheduleId, divide((1.1 + 2.2 + 3.3), 3),
            firstValue, thirdValue, hour8.getMillis())));

        // verify that the 6 hour index is updated
        List<MetricsIndexEntry> expected6HourIndex = asList(new MetricsIndexEntry(MetricsTable.SIX_HOUR,
            dateTimeService.getTimeSlice(hour9, configuration.getOneHourTimeSliceDuration()), scheduleId));

        assertMetricsIndexEquals(MetricsTable.SIX_HOUR, hour9.minusHours(3).getMillis(), expected6HourIndex,
            "Failed to update index for " + MetricsTable.SIX_HOUR);

        // The 6 hour data should not get aggregated since the current 6 hour time slice
        // has not passed yet. More specifically, the aggregation job is running at 09:00
        // which means that the current 6 hour slice is from 06:00 to 12:00.
//        assert6HourDataEmpty(scheduleId);

        // verify that the 24 hour index is empty
//        assert24HourMetricsIndexEmpty(scheduleId, hour0.getMillis());

        // verify that the 1 hour queue has been purged
//        assert1HourMetricsIndexEmpty(scheduleId, hour9.getMillis());
    }

    static double divide(double dividend, int divisor) {
        return new BigDecimal(Double.toString(dividend)).divide(new BigDecimal(Integer.toString(divisor)),
            MathContext.DECIMAL64).doubleValue();
    }

    private void assert1HourDataEquals(int scheduleId, List<AggregateNumericMetric> expected) {
        assertMetricDataEquals(MetricsTable.ONE_HOUR, scheduleId, expected);
    }

    private void assert6HourDataEquals(int scheduleId, List<AggregateNumericMetric> expected) {
        assertMetricDataEquals(MetricsTable.SIX_HOUR, scheduleId, expected);
    }

    private void assert24HourDataEquals(int scheduleId, List<AggregateNumericMetric> expected) {
        assertMetricDataEquals(MetricsTable.TWENTY_FOUR_HOUR, scheduleId, expected);
    }

    private void assertMetricDataEquals(MetricsTable columnFamily, int scheduleId,
        List<AggregateNumericMetric> expected) {
        List<AggregateNumericMetric> actual = Lists.newArrayList(findAggregateMetrics(columnFamily, scheduleId));
        assertCollectionMatchesNoOrder("Metric data for schedule id " + scheduleId + " in table " + columnFamily +
            " does not match expected values", expected, actual, TEST_PRECISION);
    }

    private void assertMetricsIndexEquals(MetricsTable table, long timeSlice, List<MetricsIndexEntry> expected,
        String msg) {
        List<MetricsIndexEntry> actual = Lists.newArrayList(dao.findMetricsIndexEntries(table, timeSlice));
        assertCollectionMatchesNoOrder(msg + ": " + table + " index does not match expected values.",
            expected, actual);
    }

}
