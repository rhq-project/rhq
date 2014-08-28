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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateNumericMetricMapper;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.Bucket;
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
    protected DateTimeServiceStub dateTimeService;
    private RawCacheMapper rawCacheMapper = new RawCacheMapper();
    private AggregateCacheMapper aggregateCacheMapper = new AggregateCacheMapper();
    private CacheIndexEntryMapper cacheIndexEntryMapper = new CacheIndexEntryMapper();
    private RawNumericMetricMapper rawMapper = new RawNumericMetricMapper();
    private AggregateNumericMetricMapper aggregateMapper = new AggregateNumericMetricMapper();

    @BeforeClass
    public void initClass() throws Exception {
        dao = new MetricsDAO(storageSession, configuration);
        dateTimeService = new DateTimeServiceStub();
        dateTimeService.setConfiguration(configuration);
    }

    protected DateTime hour(int hourOfDay) {
        return hour0().plusHours(hourOfDay);
    }

    protected DateTime today() {
        return hour(0);
    }

    protected DateTime yesterday() {
        return hour(0).minusHours(24);
    }

    protected DateTime tomorrow() {
        return hour(0).plusHours(24);
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
        session.execute("TRUNCATE " + MetricsTable.INDEX);
        session.execute("TRUNCATE " + MetricsTable.RAW);
        session.execute("TRUNCATE " + MetricsTable.AGGREGATE);
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

    /**
     * Verifies that the 1 hour data (in the historical table) matches the expected values.
     *
     * @param scheduleId The schedule id to query
     * @param expected The expected values
     */
    protected void assert1HourDataEquals(int scheduleId, AggregateNumericMetric... expected) {
        assert1HourDataEquals(scheduleId, asList(expected));
    }

    /**
     * Verifies that the 1 hour data (in the historical table) matches the expected values.
     *
     * @param scheduleId The schedule id to query
     * @param expected The expected values
     */
    protected void assert1HourDataEquals(int scheduleId, Collection<AggregateNumericMetric> expected) {
        assert1HourDataEquals(scheduleId, new ArrayList<AggregateNumericMetric>(expected));
    }

    /**
     * Verifies that the 1 hour data (in the historical table) matches the expected values.
     *
     * @param scheduleId The schedule id to query
     * @param expected The expected values
     */
    protected void assert1HourDataEquals(int scheduleId, List<AggregateNumericMetric> expected) {
        assertMetricDataEquals(MetricsTable.ONE_HOUR, scheduleId, expected);
    }

    /**
     * Verifies that the 6 hour data (in the historical table) matches the expected values.
     *
     * @param scheduleId The schedule id to query
     * @param expected The expected values
     */
    protected void assert6HourDataEquals(int scheduleId, AggregateNumericMetric... expected) {
        assert6HourDataEquals(scheduleId, asList(expected));
    }

    /**
     * Verifies that the 6 hour data (in the historical table) matches the expected values.
     *
     * @param scheduleId The schedule id to query
     * @param expected The expected values
     */
    protected void assert6HourDataEquals(int scheduleId, List<AggregateNumericMetric> expected) {
        assertMetricDataEquals(MetricsTable.SIX_HOUR, scheduleId, expected);
    }

    /**
     * Verifies that the 24 hour data (in the historical table) matches the expected values.
     *
     * @param scheduleId The schedule id to query
     * @param expected The expected values
     */
    protected void assert24HourDataEquals(int scheduleId, List<AggregateNumericMetric> expected) {
        assertMetricDataEquals(MetricsTable.TWENTY_FOUR_HOUR, scheduleId, expected);
    }

    private void assertMetricDataEquals(MetricsTable columnFamily, int scheduleId,
        List<AggregateNumericMetric> expected) {
        List<AggregateNumericMetric> actual = Lists.newArrayList(findAggregateMetrics(columnFamily, scheduleId));
        assertCollectionMatchesNoOrder("Metric data for schedule id " + scheduleId + " in table " + columnFamily +
            " does not match expected values", expected, actual, TEST_PRECISION);
    }

    protected void assertMetricDataEquals(int scheduleId, Bucket bucket, AggregateNumericMetric... expected) {
        assertMetricDataEquals(scheduleId, bucket, asList(expected));
    }

    protected void assertMetricDataEquals(int scheduleId, Bucket bucket, List<AggregateNumericMetric> expected) {
        ResultSet resultSet = session.execute(
            "select schedule_id, bucket, time, avg, max, min " +
            "from " + MetricsTable.AGGREGATE + " " +
            "where schedule_id = " + scheduleId + " and bucket = '" + bucket + "'");
        List<AggregateNumericMetric> actual = aggregateMapper.mapAll(resultSet);
        assertCollectionMatchesNoOrder("Metric data for schedule id " + scheduleId + " in bucket " + bucket +
            " does not match expected values", expected, actual, TEST_PRECISION);
    }

    /**
     * Verifies that the 6 hour data table is empty for the specified schedule id.
     *
     * @param scheduleId The schedule id to query
     */
    protected void assert6HourDataEmpty(int scheduleId) {
        assertMetricDataEmpty(scheduleId, Bucket.SIX_HOUR);
    }

    /**
     * Verifies that the 6 hour data table is empty for the specified schedule ids.
     *
     * @param scheduleIds The schedule ids to query
     */
    protected void assert6HourDataEmpty(int... scheduleIds) {
        for (Integer scheduleId : scheduleIds) {
            assert6HourDataEmpty(scheduleId);
        }
    }

    /**
     * Verifies that the 24 hour data table is empty for the specified schedule id.
     *
     * @param scheduleId The schedule id to query
     */
    protected void assert24HourDataEmpty(int scheduleId) {
        assertMetricDataEmpty(scheduleId, Bucket.TWENTY_FOUR_HOUR);
    }

    /**
     * Verifies that the 24 hour data table is empty for the specified schedule ids.
     *
     * @param scheduleIds The scheudle ids to query
     */
    protected void assert24HourDataEmpty(int... scheduleIds) {
        for (Integer scheduleId : scheduleIds) {
            assert24HourDataEmpty(scheduleId);
        }
    }

    private void assertMetricDataEmpty(int scheduleId, Bucket bucket) {
        ResultSet resultSet = session.execute(
            "select schedule_id, bucket, time, avg, max, min " +
            "from " + MetricsTable.AGGREGATE + " " +
            "where schedule_id = " + scheduleId + " and bucket = '" + bucket + "'");
        List<AggregateNumericMetric> metrics = aggregateMapper.mapAll(resultSet);

        assertEquals(metrics.size(), 0, "Expected " + bucket + " to be empty for schedule id " + scheduleId +
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

    protected int startScheduleId(int scheduleId) {
        return (scheduleId / PARTITION_SIZE) * PARTITION_SIZE;
    }

    /**
     * Verifies that the raw cache is empty for the specified time slice and start schedule id.
     *
     * @param timeSlice The time slice to query
     * @param startScheduleId The start schedule id to query
     */
    protected void assertRawCacheEmpty(DateTime timeSlice, int startScheduleId) {
        List<RawNumericMetric> emptyRaws = Collections.emptyList();
        assertRawCacheEquals(timeSlice, startScheduleId, emptyRaws);
    }

    /**
     * Verifies that the raw cache is empty for the specified time slice and start schedule ids.
     *
     * @param timeSlice The time slice to query
     * @param startScheduleIds The start schedule ids to query
     */
    protected void assertRawCacheEmpty(DateTime timeSlice, int... startScheduleIds) {
        List<RawNumericMetric> emptyRaws = Collections.emptyList();
        for (Integer startScheduleId : startScheduleIds) {
            assertRawCacheEquals(timeSlice, startScheduleId, emptyRaws);
        }
    }

    /**
     * Verifies that the raw cache equals the expected values for the specified time slice and start schedule id
     *
     * @param timeSlice The time slice to query
     * @param startScheduleId The start schedule id to query
     * @param expected The expected values
     */
    protected void assertRawCacheEquals(DateTime timeSlice, int startScheduleId, RawNumericMetric... expected) {
        assertRawCacheEquals(timeSlice, startScheduleId, asList(expected));
    }

    /**
     * Verifies that the raw cache equals the expected values for the specified time slice and start schedule id
     *
     * @param timeSlice The time slice to query
     * @param startScheduleId The start schedule id to query
     * @param expected The expected values
     */
    protected void assertRawCacheEquals(DateTime timeSlice, int startScheduleId,
        List<RawNumericMetric> expected) {
        assertCacheEquals(MetricsTable.RAW, timeSlice, startScheduleId, expected, rawCacheMapper);
    }

    /**
     * Verifies that the 1 hour cache equals the expected values for the specified time slice and start schedule id.
     *
     * @param timeSlice The time slice to query
     * @param startScheduleId The start schedule id to query
     * @param expected The expected values.
     */
    protected void assert1HourCacheEquals(DateTime timeSlice, int startScheduleId,
        List<AggregateNumericMetric> expected) {
        assertCacheEquals(MetricsTable.ONE_HOUR, timeSlice, startScheduleId, expected, aggregateCacheMapper);
    }

    /**
     * Verifies that the 6 hour cache equals the expected values for the specified time slice and start schedule id.
     *
     * @param timeSlice The time slice to query
     * @param startScheduleId The start schedule id to query
     * @param expected The expected values.
     */
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

    protected void assertCacheIndexEntriesEqual(List<CacheIndexEntry> actual, List<CacheIndexEntry> expected,
        MetricsTable bucket) {
        assertEquals(actual.size(), expected.size(), "The number of " + bucket + " cache index entries is wrong");
        for (int i = 0; i < expected.size(); ++i) {
            assertPropertiesMatch(expected.get(i), actual.get(i), "The " + bucket + " cache index entry does not " +
                "match the expected value");
        }
    }

    /**
     * Verifies that the 1 hour cache is empty for the specified time slice and start schedule id
     *
     * @param timeSlice The time slice to query
     * @param startScheduleId The start schedule id to query
     */
    protected void assert1HourCacheEmpty(DateTime timeSlice, int startScheduleId) {
        assertAggregateCacheEmpty(timeSlice, startScheduleId, MetricsTable.ONE_HOUR);
    }

    /**
     * Verifies that the 1 hour cache is empty for the specified time slice and start schedule ids.
     *
     * @param timeSlice The time slice to query
     * @param startScheduleIds The start schedule ids to query
     */
    protected void assert1HourCacheEmpty(DateTime timeSlice, int... startScheduleIds) {
        for (Integer startScheduleId : startScheduleIds) {
            assertAggregateCacheEmpty(timeSlice, startScheduleId, MetricsTable.ONE_HOUR);
        }
    }

    protected void assert6HourCacheEmpty(DateTime timeSlice, int startScheduleId) {
        assertAggregateCacheEmpty(timeSlice, startScheduleId, MetricsTable.SIX_HOUR);
    }

    private void assertAggregateCacheEmpty(DateTime timeSlice, int startScheduleId, MetricsTable table) {
        ResultSet resultSet = dao.findCacheEntriesAsync(table, timeSlice.getMillis(), startScheduleId).get();
        List<AggregateNumericMetric> metrics = aggregateCacheMapper.map(resultSet);
        assertEquals(metrics.size(), 0, "Expected the " + table + " cache to be empty but found " + metrics);
    }

    /**
     * Verifies that the raw cache index is empty for the specified collection time slice. The day to query is
     * derived from <code>collectionTimeSlice</code>.
     *
     * @param collectionTimeSlice The time slice in the index to query. The day to query is determined by the 24 hour
     *                            time slice of this value.
     */
    protected void assertRawCacheIndexEmpty(DateTime collectionTimeSlice) {
        List<CacheIndexEntry> emptyEntries = Collections.emptyList();
        assertRawCacheIndexEquals(collectionTimeSlice, emptyEntries);
    }

    /**
     * <p>
     * Verifies that the raw cache for <code>collectionTimeSlice</code> matches the expected values. The day to
     * query in the index is derived from <code>collectionTimeSlice</code>.
     * </p>
     * <p>
     * Note that expected values for {@link CacheIndexEntry#getDay() CacheIndexEntry.day},
     * {@link CacheIndexEntry#getCollectionTimeSlice() CacheIndexEntry.collectionTimeSlice}, and
     * {@link CacheIndexEntry#getCollectionTimeSlice() CacheIndexEntry.insertTimeSlice} will be overwritten and set using
     * the <code>collectionTimeSlice</code> argument. The <code>day</code> property will be set to the 24 hour time slice
     * of <code>collectionTimeSlice</code>.
     * </p>
     *
     * @param collectionTimeSlice    The time slice in the index to query. The day to query is determined by the 24 hour
     *                               time slice of this value.
     * @param expected The expected values
     */
    protected void assertRawCacheIndexEquals(DateTime collectionTimeSlice, List<CacheIndexEntry> expected) {
        setTimestamps(collectionTimeSlice, expected);
        assertCacheIndexEquals(MetricsTable.RAW, collectionTimeSlice, expected);
    }

    /**
     * Verifies that the raw cache index before <code>collectionTimeSlice</code> matches the expected values. The
     * day to query in the index is derived from <code>collectionTimeSlice</code>.
     *
     * @param collectionTimeSlice The exclusive upper bound for the date range queried. The lower bound is the start of
     *                            the 24 hour time slice for this value.
     * @param expected The expected values
     */
    protected void assertRawCacheIndexBeforeEquals(DateTime collectionTimeSlice, List<CacheIndexEntry> expected) {
        assertCacheIndexBeforeEquals(MetricsTable.RAW, collectionTimeSlice, expected);
    }

    /**
     * Verifies that the raw cache index after <code>collectionTimeSlice</code> matches the expected values. The day
     * to query in the index is derived from <code>collectionTimeSlice</code>.
     *
     * @param collectionTimeSlice The inclusive lower bound for the date range queried. The upper bound is the start of
     *                            the next 24 hour time slice, exclusive.
     * @param expected The expected values.
     */
    protected void assertRawCacheIndexAfterEquals(DateTime collectionTimeSlice, List<CacheIndexEntry> expected) {
        assertCacheIndexAfterEquals(MetricsTable.RAW, collectionTimeSlice, expected);
    }

    /**
     * <p>
     * Verifies that 1 hour cache for <code>collectionTimeSlice</code> matches the expected values. The day to
     * query in the index is derived from <code>collectionTimeSlice</code>.
     * </p>
     * <p>
     * Note that expected values for {@link CacheIndexEntry#getDay() CacheIndexEntry.day},
     * {@link CacheIndexEntry#getCollectionTimeSlice() CacheIndexEntry.collectionTimeSlice}, and
     * {@link CacheIndexEntry#getCollectionTimeSlice() CacheIndexEntry.insertTimeSlice} will be overwritten and set using
     * the <code>collectionTimeSlice</code> argument. The <code>day</code> property will be set to the 24 hour time slice
     * of <code>collectionTimeSlice</code>.
     * </p>
     *
     * @param collectionTimeSlice    The time slice in the index to query. The day to query is determined by the 24 hour
     *                               time slice of this value.
     * @param expected The expected values
     */
    protected void assert1HourCacheIndexEquals(DateTime collectionTimeSlice, List<CacheIndexEntry> expected) {
        setTimestamps(collectionTimeSlice, expected);
        assertCacheIndexEquals(MetricsTable.ONE_HOUR, collectionTimeSlice, expected);
    }

    /**
     * Verifies that the 1 hour cache index is empty for the specified collection time slice. The day to query is
     * derived from <code>collectionTimeSlice</code>.
     *
     * @param collectionTimeSlice The time slice in the index to query. The day to query is determined by the 24 hour
     *                            time slice of this value.
     */
    protected void assert1HourCacheIndexEmpty(DateTime collectionTimeSlice) {
        List<CacheIndexEntry> emptyEntries = Collections.emptyList();
        assert1HourCacheIndexEquals(collectionTimeSlice, emptyEntries);
    }

    /**
     * <p>
     * Verifies that 6 hour cache for <code>collectionTimeSlice</code> matches the expected values. The day to
     * query in the index is derived from <code>collectionTimeSlice</code>.
     * </p>
     * <p>
     * Note that expected values for {@link CacheIndexEntry#getDay() CacheIndexEntry.day},
     * {@link CacheIndexEntry#getCollectionTimeSlice() CacheIndexEntry.collectionTimeSlice}, and
     * {@link CacheIndexEntry#getCollectionTimeSlice() CacheIndexEntry.insertTimeSlice} will be overwritten and set using
     * the <code>collectionTimeSlice</code> argument. The <code>day</code> property will be set to the 24 hour time slice
     * of <code>collectionTimeSlice</code>.
     * </p>
     *
     * @param collectionTimeSlice    The time slice in the index to query. The day to query is determined by the 24 hour
     *                               time slice of this value.
     * @param expected The expected values
     */
    protected void assert6HourCacheIndexEquals(DateTime collectionTimeSlice, List<CacheIndexEntry> expected) {
        setTimestamps(collectionTimeSlice, expected);
        assertCacheIndexEquals(MetricsTable.SIX_HOUR, collectionTimeSlice, expected);
    }

    /**
     * Verifies that the 6 hour cache index is empty for the specified collection time slice. The day to query is
     * derived from <code>collectionTimeSlice</code>.
     *
     * @param collectionTimeSlice The time slice in the index to query. The day to query is determined by the 24 hour
     *                            time slice of this value.
     */
    protected void assert6HourCacheIndexEmpty(DateTime collectionTimeSlice) {
        List<CacheIndexEntry> emptyEntries = Collections.emptyList();
        assert6HourCacheIndexEquals(collectionTimeSlice, emptyEntries);
    }

    /**
     * Verifies that the cache index for <code>collectionTimeSlice</code> equals the expected entries. The day to query
     * is derived from <code>collectionTimeSlice</code>.
     *
     * @param bucket The bucket to query, e.g., raw_metrics, one_hour_metrics
     * @param collectionTimeSlice The time slice in the index to query. The day to query is determined by the 24 hour
     *                            time slice of this value.
     * @param expected The expected values.
     */
    private void assertCacheIndexEquals(MetricsTable bucket, DateTime collectionTimeSlice,
        List<CacheIndexEntry> expected) {

        ResultSet resultSet = dao.findCurrentCacheIndexEntries(bucket, dateTimeService.get24HourTimeSlice(
            collectionTimeSlice).getMillis(), 0, collectionTimeSlice.getMillis()).get();

        List<CacheIndexEntry> actual = cacheIndexEntryMapper.map(resultSet);

        assertCacheIndexEntriesEqual(actual, expected, bucket);
    }

    /**
     * Verifies that the cache index after <code>collectionTimeSlice</code> equals the expected entries. The day to
     * query is derived from <code>collectionTimeSlice</code>.
     *
     * @param bucket The bucket to query, e.g., raw_metrics, one_hour_metrics
     * @param collectionTimeSlice The inclusive lower bound for the date range queried. The upper bound is the start of
     *                            next 24 hour time slice, exclusive
     * @param expected The expected values
     */
    protected void assertCacheIndexAfterEquals(MetricsTable bucket, DateTime collectionTimeSlice,
        List<CacheIndexEntry> expected) {
        DateTime day = dateTimeService.get24HourTimeSlice(collectionTimeSlice);
        ResultSet resultSet = dao.findPastCacheIndexEntriesBeforeToday(bucket, day.getMillis(), 0,
            collectionTimeSlice.getMillis()).get();
        List<CacheIndexEntry> actual = cacheIndexEntryMapper.map(resultSet);

        assertCacheIndexEntriesEqual(actual, expected, bucket);
    }

    /**
     * Verifies that the cache index before <code>collectionTimeSlice</code> equals the expected entries. The day to
     * query is derived from <code>collectionTimeSlice</code>.
     *
     * @param bucket The bucket to query, e.g., raw_metrics, one_hour_metrics
     * @param collectionTimeSlice The exclusive upper bound for the date range queried. The lower bound is the start of
     *                            the 24 hour time slice for this value.
     * @param expected The expected values
     */
    private void assertCacheIndexBeforeEquals(MetricsTable bucket, DateTime collectionTimeSlice,
        List<CacheIndexEntry> expected) {
        DateTime day = dateTimeService.get24HourTimeSlice(collectionTimeSlice);
        ResultSet resultSet = dao.findPastCacheIndexEntriesFromToday(bucket, day.getMillis(), 0,
            collectionTimeSlice.getMillis()).get();
        List<CacheIndexEntry> actual = cacheIndexEntryMapper.map(resultSet);

        assertCacheIndexEntriesEqual(actual, expected, MetricsTable.RAW);
    }

    /**
     * Creates a raw cache index entry. {@link CacheIndexEntry#getInsertTimeSlice() insertTimeSlice} will be the same
     * as <code>collectionTimeSlice</code> and {@link CacheIndexEntry#getDay() day} will be the 24 hour time slice of
     * <code>collectionTimeSlice</code>.
     *
     * @param collectionTimeSlice The value to assign {@link CacheIndexEntry#getCollectionTimeSlice() CacheIndexEntry.collectionTimeSlice}
     * @param startScheduleId     The value to assign {@link CacheIndexEntry#getStartScheduleId() CacheIndexEntry.startScheduleId}
     * @param scheduleIds The value(s) to assign {@link CacheIndexEntry#getScheduleIds() CacheIndexEntry.scheduleIds}
     *
     * @return A new raw cache index entry
     */
    protected CacheIndexEntry newRawCacheIndexEntry(DateTime collectionTimeSlice, int startScheduleId,
        Integer... scheduleIds) {
        return newCacheIndexEntry(MetricsTable.RAW, dateTimeService.get24HourTimeSlice(collectionTimeSlice), 0,
            collectionTimeSlice, startScheduleId, collectionTimeSlice, ImmutableSet.copyOf(scheduleIds));
    }

    /**
     * Creates a raw cache index entry. The caller is responsible for assigning values to the
     * {@link CacheIndexEntry#getDay() day}, {@link CacheIndexEntry#getCollectionTimeSlice() collectionTimeSlice}, and
     * {@link CacheIndexEntry#getInsertTimeSlice() insertTimeSlice} properties.
     *
     * @param startScheduleId The value to assign {@link CacheIndexEntry#getStartScheduleId() CacheIndexEntry.startScheduleId}
     * @param scheduleIds The value(s) to assign {@link CacheIndexEntry#getScheduleIds() CacheIndexEntry.scheduleIds}
     * @return A new raw cache index entry
     */
    protected CacheIndexEntry newRawCacheIndexEntry(int startScheduleId, Integer... scheduleIds) {
        return newCacheIndexEntry(MetricsTable.RAW, null, 0, null, startScheduleId, null,
            ImmutableSet.copyOf(scheduleIds));
    }

    /**
     * Creates a raw cache index entry. {@link CacheIndexEntry#getDay() day} will be the 24 hour time slice of
     * <code>collectionTimeSlice</code>.
     *
     * @param collectionTimeSlice The value to assign {@link CacheIndexEntry#getCollectionTimeSlice() CacheIndexEntry.collectionTimeSlice}
     * @param startScheduleId     The value to assign {@link CacheIndexEntry#getStartScheduleId() CacheIndexEntry.startScheduleId}
     * @param insertTimeSlice The value to assign {@link CacheIndexEntry#getCollectionTimeSlice() CacheIndexEntry.insertTimeSlice}
     * @param scheduleIds The value(s) to assign {@link CacheIndexEntry#getScheduleIds() CacheIndexEntry.scheduleIds}
     *
     * @return A new raw cache index entry
     */
    protected CacheIndexEntry newRawCacheIndexEntry(DateTime collectionTimeSlice, int startScheduleId,
        DateTime insertTimeSlice, Integer... scheduleIds) {
        return newCacheIndexEntry(MetricsTable.RAW, dateTimeService.get24HourTimeSlice(collectionTimeSlice), 0,
            collectionTimeSlice, startScheduleId, insertTimeSlice, ImmutableSet.copyOf(scheduleIds));
    }

    /**
     * Creates a 1 hour cache index entry. The caller is responsible for assigning values to the
     * {@link CacheIndexEntry#getDay() day}, {@link CacheIndexEntry#getCollectionTimeSlice() collectionTimeSlice}, and
     * {@link CacheIndexEntry#getInsertTimeSlice() insertTimeSlice} properties.
     *
     * @param startScheduleId The value to assign {@link CacheIndexEntry#getStartScheduleId() CacheIndexEntry.startScheduleId}
     * @param scheduleIds The value(s) to assign {@link CacheIndexEntry#getScheduleIds() CacheIndexEntry.scheduleIds}
     * @return A new raw cache index entry
     */
    protected CacheIndexEntry new1HourCacheIndexEntry(int startScheduleId, Integer... scheduleIds) {
        return newCacheIndexEntry(MetricsTable.ONE_HOUR, null, 0, null, startScheduleId, null,
            ImmutableSet.copyOf(scheduleIds));
    }

    /**
     * Creates a 1 hour cache index entry. {@link CacheIndexEntry#getInsertTimeSlice() insertTimeSlice} will be the same
     * as <code>collectionTimeSlice</code> and {@link CacheIndexEntry#getDay() day} will be the 24 hour time slice of
     * <code>collectionTimeSlice</code>.
     *
     * @param collectionTimeSlice The value to assign {@link CacheIndexEntry#getCollectionTimeSlice() CacheIndexEntry.collectionTimeSlice}
     * @param startScheduleId     The value to assign {@link CacheIndexEntry#getStartScheduleId() CacheIndexEntry.startScheduleId}
     * @param scheduleIds The value(s) to assign {@link CacheIndexEntry#getScheduleIds() CacheIndexEntry.scheduleIds}
     *
     * @return A new 1 hour cache index entry
     */
    protected CacheIndexEntry new1HourCacheIndexEntry(DateTime collectionTimeSlice, int startScheduleId,
        Integer... scheduleIds) {
        return newCacheIndexEntry(MetricsTable.ONE_HOUR, dateTimeService.get24HourTimeSlice(collectionTimeSlice), 0,
            collectionTimeSlice, startScheduleId, collectionTimeSlice, ImmutableSet.copyOf(scheduleIds));
    }

    /**
     * Creates a 6 hour cache index entry. The caller is responsible for assigning values to the
     * {@link CacheIndexEntry#getDay() day}, {@link CacheIndexEntry#getCollectionTimeSlice() collectionTimeSlice}, and
     * {@link CacheIndexEntry#getInsertTimeSlice() insertTimeSlice} properties.
     *
     * @param startScheduleId The value to assign {@link CacheIndexEntry#getStartScheduleId() CacheIndexEntry.startScheduleId}
     * @param scheduleIds The value(s) to assign {@link CacheIndexEntry#getScheduleIds() CacheIndexEntry.scheduleIds}
     * @return A new raw cache index entry
     */
    protected CacheIndexEntry new6HourCacheIndexEntry(int startScheduleId, Integer... scheduleIds) {
        return newCacheIndexEntry(MetricsTable.SIX_HOUR, null, 0, null, startScheduleId, null,
            ImmutableSet.copyOf(scheduleIds));
    }

    /**
     * Creates a 6 hour cache index entry. {@link CacheIndexEntry#getInsertTimeSlice() insertTimeSlice} will be the same
     * as <code>collectionTimeSlice</code> and {@link CacheIndexEntry#getDay() day} will be the 24 hour time slice of
     * <code>collectionTimeSlice</code>.
     *
     * @param collectionTimeSlice The value to assign {@link CacheIndexEntry#getCollectionTimeSlice() CacheIndexEntry.collectionTimeSlice}
     * @param startScheduleId     The value to assign {@link CacheIndexEntry#getStartScheduleId() CacheIndexEntry.startScheduleId}
     * @param scheduleIds The value(s) to assign {@link CacheIndexEntry#getScheduleIds() CacheIndexEntry.scheduleIds}
     *
     * @return A new 6 hour cache index entry
     */
    protected CacheIndexEntry new6HourCacheIndexEntry(DateTime collectionTimeSlice, int startScheduleId,
        Integer... scheduleIds) {
        return newCacheIndexEntry(MetricsTable.SIX_HOUR, dateTimeService.get24HourTimeSlice(collectionTimeSlice), 0,
            collectionTimeSlice, startScheduleId, collectionTimeSlice, ImmutableSet.copyOf(scheduleIds));
    }

    private CacheIndexEntry newCacheIndexEntry(MetricsTable table, DateTime day, int partition,
        DateTime collectionTimeSlice, int startScheduleId, DateTime insertTimeSlice, Set<Integer> scheduleIds) {
        CacheIndexEntry indexEntry = new CacheIndexEntry();
        indexEntry.setBucket(table);

        // Note that we allow null for some arguments because some assert methods take care of setting the corresponding
        // cache index entry properties.

        if (day != null) {
            indexEntry.setDay(day.getMillis());
        }
        indexEntry.setPartition(partition);
        if (collectionTimeSlice != null) {
            indexEntry.setCollectionTimeSlice(collectionTimeSlice.getMillis());
        }
        indexEntry.setStartScheduleId(startScheduleId);
        if (insertTimeSlice != null) {
            indexEntry.setInsertTimeSlice(insertTimeSlice.getMillis());
        }
        indexEntry.setScheduleIds(scheduleIds);

        return indexEntry;
    }

    private void setTimestamps(DateTime collectionTimeSlice, List<CacheIndexEntry> expected) {
        DateTime day = dateTimeService.get24HourTimeSlice(collectionTimeSlice);

        for (CacheIndexEntry indexEntry : expected) {
            indexEntry.setDay(day.getMillis());
            indexEntry.setCollectionTimeSlice(collectionTimeSlice.getMillis());
            indexEntry.setInsertTimeSlice(collectionTimeSlice.getMillis());
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
