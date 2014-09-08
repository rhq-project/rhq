package org.rhq.server.metrics;

import static java.util.Arrays.asList;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.testng.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;

import org.rhq.cassandra.schema.Table;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateNumericMetricMapper;
import org.rhq.server.metrics.domain.Bucket;
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
    private RawNumericMetricMapper rawMapper = new RawNumericMetricMapper();
    private AggregateNumericMetricMapper aggregateMapper = new AggregateNumericMetricMapper();

    @BeforeClass
    public void initClass() throws Exception {
        initIndexPageSize();
        dao = new MetricsDAO(storageSession, configuration);
        dateTimeService = new DateTimeServiceStub();
        dateTimeService.setConfiguration(configuration);
    }

    protected void initIndexPageSize() {
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

    protected void assertRawIndexEquals(int partition, DateTime time, List<Integer> scheduleIds) {
        assertIndexEquals(MetricsTable.RAW, partition, time, scheduleIds);
    }

    protected void assertRawIndexEmpty(int partition, DateTime time) {
        assertIndexEquals(MetricsTable.RAW, partition, time, Collections.EMPTY_LIST);
    }

    protected void assert1HourIndexEquals(int partition, DateTime time, List<Integer> scheduleIds) {
        assertIndexEquals(MetricsTable.ONE_HOUR, partition, time, scheduleIds);
    }

    protected void assert1HourIndexEmpty(int partition, DateTime time) {
        assertIndexEquals(MetricsTable.ONE_HOUR, partition, time, Collections.EMPTY_LIST);
    }

    protected void assert6HourIndexEquals(int partition, DateTime time, List<Integer> scheduleIds) {
        assertIndexEquals(MetricsTable.SIX_HOUR, partition, time, scheduleIds);
    }

    protected void assert6HourIndexEmpty(int partition, DateTime time) {
        assertIndexEquals(MetricsTable.SIX_HOUR, partition, time, Collections.EMPTY_LIST);
    }

    protected void assert24HourIndexEquals(int partition, DateTime time, List<Integer> scheduleIds) {
        assertIndexEquals(MetricsTable.TWENTY_FOUR_HOUR, partition, time, scheduleIds);
    }

    protected void assert24HourIndexEmpty(int partition, DateTime time) {
        assertIndexEquals(MetricsTable.TWENTY_FOUR_HOUR, partition, time, Collections.EMPTY_LIST);
    }

    private void assertIndexEquals(MetricsTable bucket, int partition, DateTime time, List<Integer> scheduleIds) {
        List<IndexEntry> expected = new ArrayList<IndexEntry>(scheduleIds.size());
        for (Integer scheduleId : scheduleIds) {
            expected.add(new IndexEntry(bucket, partition, time.getMillis(), scheduleId));
        }
        ResultSet resultSet = dao.findIndexEntries(bucket, partition, time.getMillis()).get();
        List<IndexEntry> actual = new ArrayList<IndexEntry>();

        for (Row row : resultSet) {
            actual.add(new IndexEntry(bucket, partition, time.getMillis(), row.getInt(0)));
        }

        assertEquals(actual, expected, "The index entries do not match");
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
