package org.rhq.server.metrics;

import static java.util.Arrays.asList;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.datastax.driver.core.ResultSet;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.CacheIndexEntry;
import org.rhq.server.metrics.domain.CacheIndexEntryMapper;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.domain.NumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetricMapper;

/**
 * @author John Sanda
 */
public class MetricsTest extends CassandraIntegrationTest {

    private static final double TEST_PRECISION = Math.pow(10, -9);

    public static final int PARTITION_SIZE = 5;

    protected MetricsDAO dao;
    protected MetricsConfiguration configuration = new MetricsConfiguration();
    protected DateTimeService dateTimeService;
    private RawCacheMapper rawCacheMapper = new RawCacheMapper();
    private AggregateCacheMapper aggregateCacheMapper = new AggregateCacheMapper();
    private CacheIndexEntryMapper cacheIndexEntryMapper = new CacheIndexEntryMapper();
    private RawNumericMetricMapper rawMapper = new RawNumericMetricMapper();

    @BeforeClass
    public void initClass() throws Exception {
        dao = new MetricsDAO(storageSession, configuration);
        dateTimeService = new DateTimeService();
        dateTimeService.setConfiguration(configuration);
    }

    protected DateTime hour(int hourOfDay) {
        return hour0().plusHours(hourOfDay);
    }

    protected double avg(double... values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return divide(sum, values.length);
    }

    protected double divide(double dividend, int divisor) {
        return new BigDecimal(Double.toString(dividend)).divide(new BigDecimal(Integer.toString(divisor)),
            MathContext.DECIMAL64).doubleValue();
    }

    protected void purgeDB() {
        session.execute("TRUNCATE " + MetricsTable.RAW);
        session.execute("TRUNCATE " + MetricsTable.ONE_HOUR);
        session.execute("TRUNCATE " + MetricsTable.SIX_HOUR);
        session.execute("TRUNCATE " + MetricsTable.TWENTY_FOUR_HOUR);
        session.execute("TRUNCATE " + MetricsTable.METRICS_CACHE);
        session.execute("TRUNCATE " + MetricsTable.METRICS_CACHE_INDEX);
    }

    protected void assertRawDataEquals(int scheduleId, DateTime startTime, DateTime endTime,
        RawNumericMetric... expected) {
        assertRawDataEquals(scheduleId, startTime, endTime, asList(expected));
    }

    protected void assertRawDataEmpty(int scheduleId, DateTime startTime, DateTime endTime) {
        List<RawNumericMetric> emptyRaws = Collections.emptyList();
        assertRawDataEquals(scheduleId, startTime, endTime, emptyRaws);
    }

    protected void assertRawDataEquals(int scheduleId, DateTime startTime, DateTime endTime,
        List<RawNumericMetric> expected) {
        ResultSet resultSet = dao.findRawMetricsAsync(scheduleId, startTime.getMillis(), endTime.getMillis()).get();
        List<RawNumericMetric> actual = rawMapper.mapAll(resultSet);

        assertEquals(actual, expected, "The raw metrics do not match the expected value");

    }

    protected void assert1HourDataEquals(int scheduleId, AggregateNumericMetric... expected) {
        assert1HourDataEquals(scheduleId, asList(expected));
    }

    protected void assert1HourDataEquals(int scheduleId, Collection<AggregateNumericMetric> expected) {
        assert1HourDataEquals(scheduleId, new ArrayList<AggregateNumericMetric>(expected));
    }

    protected void assert1HourDataEquals(int scheduleId, List<AggregateNumericMetric> expected) {
        assertMetricDataEquals(MetricsTable.ONE_HOUR, scheduleId, expected);
    }

    protected void assert6HourDataEquals(int scheduleId, AggregateNumericMetric... expected) {
        assert6HourDataEquals(scheduleId, asList(expected));
    }

    protected void assert6HourDataEquals(int scheduleId, List<AggregateNumericMetric> expected) {
        assertMetricDataEquals(MetricsTable.SIX_HOUR, scheduleId, expected);
    }

    protected void assert24HourDataEquals(int scheduleId, List<AggregateNumericMetric> expected) {
        assertMetricDataEquals(MetricsTable.TWENTY_FOUR_HOUR, scheduleId, expected);
    }

    protected void assert24HourDataEquals(int scheduleId, AggregateNumericMetric... expected) {
        assertMetricDataEquals(MetricsTable.TWENTY_FOUR_HOUR, scheduleId, asList(expected));
    }

    private void assertMetricDataEquals(MetricsTable columnFamily, int scheduleId,
        List<AggregateNumericMetric> expected) {
        List<AggregateNumericMetric> actual = Lists.newArrayList(findAggregateMetrics(columnFamily, scheduleId));
        assertCollectionMatchesNoOrder("Metric data for schedule id " + scheduleId + " in table " + columnFamily +
            " does not match expected values", expected, actual, TEST_PRECISION);
    }

    protected void assert6HourDataEmpty(int scheduleId) {
        assertMetricDataEmpty(scheduleId, MetricsTable.SIX_HOUR);
    }

    protected void assert6HourDataEmpty(int... scheduleIds) {
        for (Integer scheduleId : scheduleIds) {
            assert6HourDataEmpty(scheduleId);
        }
    }

    protected void assert24HourDataEmpty(int scheduleId) {
        assertMetricDataEmpty(scheduleId, MetricsTable.TWENTY_FOUR_HOUR);
    }

    protected void assert24HourDataEmpty(int... scheduleIds) {
        for (Integer scheduleId : scheduleIds) {
            assert24HourDataEmpty(scheduleId);
        }
    }

    private void assertMetricDataEmpty(int scheduleId, MetricsTable columnFamily) {
        List<AggregateNumericMetric> metrics = Lists.newArrayList(findAggregateMetrics(columnFamily, scheduleId));
        assertEquals(metrics.size(), 0, "Expected " + columnFamily + " to be empty for schedule id " + scheduleId +
            " but found " + metrics);
    }

    protected WaitForWrite insertRawData(DateTime timeSlice, MeasurementDataNumeric... data) {
        WaitForWrite waitForRawInserts = new WaitForWrite(data.length * 2);
        StorageResultSetFuture future;
        for (MeasurementDataNumeric raw : data) {
            future = dao.insertRawData(raw);
            Futures.addCallback(future, waitForRawInserts);
            future = dao.updateMetricsCache(MetricsTable.RAW, timeSlice.getMillis(),
                startScheduleId(raw.getScheduleId()), raw.getScheduleId(), raw.getTimestamp(), ImmutableMap.of(
                AggregateType.VALUE.ordinal(), raw.getValue()));
            Futures.addCallback(future, waitForRawInserts);
        }
        return waitForRawInserts;
    }

    protected WaitForWrite insert1HourData(DateTime timeSlice, AggregateNumericMetric... data) {
        WaitForWrite waitForWrites = new WaitForWrite(data.length * 4);
        StorageResultSetFuture future;
        for (AggregateNumericMetric metric : data) {
            future = dao.insertOneHourDataAsync(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN,
                metric.getMin());
            Futures.addCallback(future, waitForWrites);
            future = dao.insertOneHourDataAsync(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX,
                metric.getMax());
            Futures.addCallback(future, waitForWrites);
            future = dao.insertOneHourDataAsync(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG,
                metric.getAvg());
            Futures.addCallback(future, waitForWrites);
            future = dao.updateMetricsCache(MetricsTable.ONE_HOUR, timeSlice.getMillis(), startScheduleId(
                metric.getScheduleId()), metric.getScheduleId(), metric.getTimestamp(), ImmutableMap.of(
                AggregateType.MIN.ordinal(), metric.getMin(),
                AggregateType.MAX.ordinal(), metric.getMax(),
                AggregateType.AVG.ordinal(), metric.getAvg()
            ));
            Futures.addCallback(future, waitForWrites);
        }
        return waitForWrites;
    }

    protected WaitForWrite insert6HourData(DateTime timeSlice, AggregateNumericMetric... data) {
        WaitForWrite waitForWrites = new WaitForWrite(data.length * 4);
        StorageResultSetFuture future;
        for (AggregateNumericMetric metric : data) {
            future = dao.insertSixHourDataAsync(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN,
                metric.getMin());
            Futures.addCallback(future, waitForWrites);
            future = dao.insertSixHourDataAsync(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX,
                metric.getMax());
            Futures.addCallback(future, waitForWrites);
            future = dao.insertSixHourDataAsync(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG,
                metric.getAvg());
            Futures.addCallback(future, waitForWrites);
            future = dao.updateMetricsCache(MetricsTable.SIX_HOUR, timeSlice.getMillis(), startScheduleId(
                metric.getScheduleId()), metric.getScheduleId(), metric.getTimestamp(), ImmutableMap.of(
                AggregateType.MIN.ordinal(), metric.getMin(),
                AggregateType.MAX.ordinal(), metric.getMax(),
                AggregateType.AVG.ordinal(), metric.getAvg()
            ));
            Futures.addCallback(future, waitForWrites);
        }
        return waitForWrites;
    }

    protected int startScheduleId(int scheduleId) {
        return (scheduleId / PARTITION_SIZE) * PARTITION_SIZE;
    }

    protected void assertRawCacheEmpty(DateTime timeSlice, int startScheduleId) {
        List<RawNumericMetric> emptyRaws = Collections.emptyList();
        assertRawCacheEquals(timeSlice, startScheduleId, emptyRaws);
    }

    protected void assertRawCacheEmpty(DateTime timeSlice, int... startScheduleIds) {
        List<RawNumericMetric> emptyRaws = Collections.emptyList();
        for (Integer startScheduleId : startScheduleIds) {
            assertRawCacheEquals(timeSlice, startScheduleId, emptyRaws);
        }
    }

    protected void assertRawCacheEquals(DateTime timeSlice, int startScheduleId, RawNumericMetric... expected) {
        assertRawCacheEquals(timeSlice, startScheduleId, asList(expected));
    }

    protected void assertRawCacheEquals(DateTime timeSlice, int startScheduleId,
        List<RawNumericMetric> expected) {
        assertCacheEquals(MetricsTable.RAW, timeSlice, startScheduleId, expected, rawCacheMapper);
    }

    protected void assert1HourCacheEquals(DateTime timeSlice, int startScheduleId,
        List<AggregateNumericMetric> expected) {
        assertCacheEquals(MetricsTable.ONE_HOUR, timeSlice, startScheduleId, expected, aggregateCacheMapper);
    }

    protected void assert6HourCacheEquals(DateTime timeSlice, int startScheduleId,
        List<AggregateNumericMetric> expected) {
        assertCacheEquals(MetricsTable.SIX_HOUR, timeSlice, startScheduleId, expected, aggregateCacheMapper);
    }

    private <T extends NumericMetric> void assertCacheEquals(MetricsTable table, DateTime timeSlice,
        int startScheduleId, List<T> expected, CacheMapper<T> cacheMapper) {
        ResultSet resultSet = dao.findCacheEntriesAsync(table, timeSlice.getMillis(), startScheduleId).get();
        List<T> actual = cacheMapper.map(resultSet);

        assertEquals(actual, expected, "The " + table + " cache is wrong");
    }

    private void assertCacheIndexEntriesEqual(List<CacheIndexEntry> actual, List<CacheIndexEntry> expected,
        MetricsTable bucket) {
        assertEquals(actual.size(), expected.size(), "The number of " + bucket + " cache index entries is wrong");
        for (int i = 0; i < expected.size(); ++i) {
            assertPropertiesMatch(expected.get(i), actual.get(i), "The " + bucket + " cache index entry does not " +
                "match the expected value");
        }
    }

    protected void assert1HourCacheEmpty(DateTime timeSlice, int startScheduleId) {
        assertAggregateCacheEmpty(timeSlice, startScheduleId, MetricsTable.ONE_HOUR);
    }

    protected void assert1HourCacheEmpty(DateTime timeSlice, int... startScheduleIds) {
        for (Integer startScheduleId : startScheduleIds) {
            assertAggregateCacheEmpty(timeSlice, startScheduleId, MetricsTable.ONE_HOUR);
        }
    }

    protected void assert6HourCacheEmpty(DateTime timeSlice, int startScheduleId) {
        assertAggregateCacheEmpty(timeSlice, startScheduleId, MetricsTable.SIX_HOUR);
    }

    protected void assertAggregateCacheEmpty(DateTime timeSlice, int startScheduleId, MetricsTable table) {
        ResultSet resultSet = dao.findCacheEntriesAsync(table, timeSlice.getMillis(), startScheduleId).get();
        List<AggregateNumericMetric> metrics = aggregateCacheMapper.map(resultSet);
        assertEquals(metrics.size(), 0, "Expected the " + table + " cache to be empty but found " + metrics);
    }

    protected void assertRawCacheIndexEmpty(DateTime insertTimeSlice, int partition) {
        List<CacheIndexEntry> emptyEntries = Collections.emptyList();
        assertRawCacheIndexEquals(insertTimeSlice, partition, emptyEntries);
    }

    protected void assertRawCacheIndexEquals(DateTime insertTimeSlice, int partition, List<CacheIndexEntry> expected) {
        assertCacheIndexEquals(insertTimeSlice, MetricsTable.RAW, partition, expected);
    }

    protected void assert1HourCacheIndexEquals(DateTime insertTimeSlice, int partition,
        List<CacheIndexEntry> expected) {
        assertCacheIndexEquals(insertTimeSlice, MetricsTable.ONE_HOUR, partition, expected);
    }

    protected void assert6HourCacheIndexEquals(DateTime insertTimeSlice, int partition,
        List<CacheIndexEntry> expected) {
        assertCacheIndexEquals(insertTimeSlice, MetricsTable.SIX_HOUR, partition, expected);
    }

    protected void assert6HourCacheIndexEmpty(DateTime insertTimeSlice, int partition) {
        List<CacheIndexEntry> emptyEntries = Collections.emptyList();
        assert6HourCacheIndexEquals(insertTimeSlice, partition, emptyEntries);
    }

    private void assertCacheIndexEquals(DateTime insertTimeSlice, MetricsTable table, int partition,
        List<CacheIndexEntry> expected) {
        ResultSet resultSet = dao.findCacheIndexEntries(table, insertTimeSlice.getMillis(), partition).get();
        List<CacheIndexEntry> actual = cacheIndexEntryMapper.map(resultSet);
        for (CacheIndexEntry entry : expected) {
            entry.setInsertTimeSlice(insertTimeSlice.getMillis());
            entry.setPartition(partition);
        }
        assertCacheIndexEntriesEqual(actual, expected, table);
    }

    protected void assert1HourCacheIndexEmpty(DateTime insertTimeSlice, int partition) {
        List<CacheIndexEntry> emptyEntries = Collections.emptyList();
        assert1HourCacheIndexEquals(insertTimeSlice, partition, emptyEntries);
    }

    protected CacheIndexEntry newRawCacheIndexEntry(int startScheduleId, DateTime collectionTimeSlice) {
        return newCacheIndexEntry(MetricsTable.RAW, null, 0, startScheduleId, collectionTimeSlice);
    }

    protected CacheIndexEntry newRawCacheIndexEntry(int startScheduleId, DateTime collectionTimeSlice,
        Set<Integer> scheduleIds) {
        return newCacheIndexEntry(MetricsTable.RAW, null, 0, startScheduleId, collectionTimeSlice, scheduleIds);
    }

    protected CacheIndexEntry new1HourCacheIndexEntry(int startScheduleId, DateTime collectionTimeSlice) {
        return newCacheIndexEntry(MetricsTable.ONE_HOUR, null, 0, startScheduleId, collectionTimeSlice);
    }

    protected CacheIndexEntry new6HourCacheIndexEntry(int startScheduleId, DateTime collectionTimeSlice) {
        return newCacheIndexEntry(MetricsTable.SIX_HOUR, null, 0, startScheduleId, collectionTimeSlice);
    }

    @SuppressWarnings("unchecked")
    protected CacheIndexEntry newCacheIndexEntry(MetricsTable table, DateTime insertTimeSlice, int partition,
        int startScheduleId, DateTime collectionTimeSlice) {
        return newCacheIndexEntry(table, insertTimeSlice, partition, startScheduleId, collectionTimeSlice,
            Collections.EMPTY_SET);
    }

    protected CacheIndexEntry newCacheIndexEntry(MetricsTable table, DateTime insertTimeSlice, int partition,
        int startScheduleId, DateTime collectionTimeSlice, Set<Integer> scheduleIds) {
        CacheIndexEntry entry = new CacheIndexEntry();
        entry.setBucket(table);
        if (insertTimeSlice != null) {
            entry.setInsertTimeSlice(insertTimeSlice.getMillis());
        }
        entry.setPartition(partition);
        entry.setStartScheduleId(startScheduleId);
        entry.setCollectionTimeSlice(collectionTimeSlice.getMillis());
        entry.setScheduleIds(scheduleIds);

        return entry;
    }

    static class MetricsServerStub extends MetricsServer {
        private DateTime currentHour;

        public void setCurrentHour(DateTime currentHour) {
            this.currentHour = currentHour;
        }

        @Override
        protected DateTime currentHour() {
            if (currentHour == null) {
                return super.currentHour();
            }
            return currentHour;
        }

    }

    static class DateTimeServiceStub extends DateTimeService {

        private DateTime now;

        public void setNow(DateTime now) {
            this.now = now;
        }

        @Override
        public DateTime now() {
            if (now == null) {
                return super.now();
            }
            return now;
        }

        @Override
        public long nowInMillis() {
            if (now == null) {
                return super.nowInMillis();
            }
            return now.getMillis();
        }
    }
}
