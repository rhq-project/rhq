package org.rhq.server.metrics;

import static java.util.Arrays.asList;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.testng.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;

import org.rhq.cassandra.schema.Table;
import org.rhq.server.metrics.aggregation.IndexIterator;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateNumericMetricMapper;
import org.rhq.server.metrics.domain.Bucket;
import org.rhq.server.metrics.domain.IndexBucket;
import org.rhq.server.metrics.domain.IndexEntry;
import org.rhq.server.metrics.domain.MetricsTable;
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
    protected PreparedStatement insert1HourData;
    protected PreparedStatement insert6HourData;
    protected PreparedStatement insert24HourData;
    private RawNumericMetricMapper rawMapper = new RawNumericMetricMapper();
    private AggregateNumericMetricMapper aggregateMapper = new AggregateNumericMetricMapper();

    @BeforeClass
    public void initClass() throws Exception {
        configuration = createConfiguration();
        dateTimeService = new DateTimeServiceStub();
        dateTimeService.setConfiguration(configuration);
        InsertStatements insertStatements = new InsertStatements(storageSession, dateTimeService, configuration);
        insertStatements.init();
        dao = new MetricsDAO(storageSession, configuration, insertStatements, dateTimeService);

        insert1HourData = storageSession.prepare(
            "INSERT INTO " + MetricsTable.AGGREGATE + "(schedule_id, bucket, time, avg, max, min) " +
            "VALUES (?, '" + Bucket.ONE_HOUR + "', ?, ?, ?, ?) " +
            "USING TTL " + configuration.getOneHourTTL());

        insert6HourData = storageSession.prepare(
            "INSERT INTO " + MetricsTable.AGGREGATE + "(schedule_id, bucket, time, avg, max, min) " +
            "VALUES (?, '" + Bucket.SIX_HOUR + "', ?, ?, ?, ?) " +
            "USING TTL " + configuration.getSixHourTTL());

        insert24HourData = storageSession.prepare(
            "INSERT INTO " + MetricsTable.AGGREGATE + "(schedule_id, bucket, time, avg, max, min) " +
            "VALUES (?, '" + Bucket.TWENTY_FOUR_HOUR + "', ?, ?, ?, ?) " +
            "USING TTL " + configuration.getTwentyFourHourTTL());
    }

    protected MetricsConfiguration createConfiguration() {
        return new MetricsConfiguration();
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
        for (Table table : Table.values()) {
            session.execute("TRUNCATE " + table.getTableName());
        }
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
        assertMetricDataEquals(scheduleId, Bucket.ONE_HOUR, expected);
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
        assertMetricDataEquals(scheduleId, Bucket.SIX_HOUR, expected);
    }

    /**
     * Verifies that the 24 hour data (in the historical table) matches the expected values.
     *
     * @param scheduleId The schedule id to query
     * @param expected The expected values
     */
    protected void assert24HourDataEquals(int scheduleId, List<AggregateNumericMetric> expected) {
        assertMetricDataEquals(scheduleId, Bucket.TWENTY_FOUR_HOUR, expected);
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

    protected void assertRawIndexEquals(DateTime time, List<Integer> scheduleIds) {
        assertIndexEquals(IndexBucket.RAW, time, time.plus(configuration.getRawTimeSliceDuration()), scheduleIds);
    }

    @SuppressWarnings("unchecked")
    protected void assertRawIndexEmpty(DateTime time) {
        assertIndexEquals(IndexBucket.RAW, time, time.plus(configuration.getRawTimeSliceDuration()),
            Collections.EMPTY_LIST);
    }

    protected void assert1HourIndexEquals(DateTime time, List<Integer> scheduleIds) {
        assertIndexEquals(IndexBucket.ONE_HOUR, time, time.plus(configuration.getOneHourTimeSliceDuration()),
            scheduleIds);
    }

    @SuppressWarnings("unchecked")
    protected void assert1HourIndexEmpty(DateTime time) {
        assertIndexEquals(IndexBucket.ONE_HOUR, time, time.plus(configuration.getOneHourTimeSliceDuration()),
            Collections.EMPTY_LIST);
    }

    protected void assert6HourIndexEquals(DateTime time, List<Integer> scheduleIds) {
        assertIndexEquals(IndexBucket.SIX_HOUR, time, time.plus(configuration.getSixHourTimeSliceDuration()),
            scheduleIds);
    }

    @SuppressWarnings("unchecked")
    protected void assert6HourIndexEmpty(DateTime time) {
        assertIndexEquals(IndexBucket.SIX_HOUR, time, time.plus(configuration.getSixHourTimeSliceDuration()),
            Collections.EMPTY_LIST);
    }

    private void assertIndexEquals(IndexBucket bucket, DateTime startTime, DateTime endTime,
        List<Integer> scheduleIds) {
        List<IndexEntry> expected = new ArrayList<IndexEntry>();
        for (Integer scheduleId : scheduleIds) {
            expected.add(new IndexEntry(bucket, (scheduleId % configuration.getIndexPartitions()), startTime,
                scheduleId));
        }

        List<IndexEntry> actual = new ArrayList<IndexEntry>();
        IndexIterator iterator = new IndexIterator(startTime, endTime, bucket, dao, configuration);
        while (iterator.hasNext()) {
            actual.add(iterator.next());
        }

        assertEquals(actual, expected, "The " + bucket + " index entries do not match");
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

    protected int startScheduleId(int scheduleId) {
        return (scheduleId / PARTITION_SIZE) * PARTITION_SIZE;
    }

    protected ResultSetFuture insert1HourData(AggregateNumericMetric metric) {
        BoundStatement statement = insert1HourData.bind(metric.getScheduleId(), new Date(metric.getTimestamp()),
            metric.getAvg(), metric.getMax(), metric.getMin());
        return session.executeAsync(statement);
    }

    protected ResultSetFuture insert6HourData(AggregateNumericMetric metric) {
        BoundStatement statement = insert6HourData.bind(metric.getScheduleId(), new Date(metric.getTimestamp()),
            metric.getAvg(), metric.getMax(), metric.getMin());
        return session.executeAsync(statement);
    }

    protected ResultSetFuture insert24HourData(AggregateNumericMetric metric) {
        BoundStatement statement = insert24HourData.bind(metric.getScheduleId(), new Date(metric.getTimestamp()),
            metric.getAvg(), metric.getMax(), metric.getMin());
        return session.executeAsync(statement);
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
