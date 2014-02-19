package org.rhq.server.metrics;

import static java.util.Arrays.asList;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.testng.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;

import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.MetricsIndexEntry;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * @author John Sanda
 */
public class MetricsTest extends CassandraIntegrationTest {

    private static final double TEST_PRECISION = Math.pow(10, -9);

    protected MetricsDAO dao;
    protected MetricsConfiguration configuration = new MetricsConfiguration();
    protected DateTimeService dateTimeService;

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

    protected void assertMetricsIndexEquals(MetricsTable table, long timeSlice, List<MetricsIndexEntry> expected,
        String msg) {
        List<MetricsIndexEntry> actual = Lists.newArrayList(dao.findMetricsIndexEntries(table, timeSlice));
        assertCollectionMatchesNoOrder(msg + ": " + table + " index does not match expected values.",
            expected, actual);
    }

    protected void assert6HourDataEmpty(int scheduleId) {
        assertMetricDataEmpty(scheduleId, MetricsTable.SIX_HOUR);
    }

    protected void assert24HourDataEmpty(int scheduleId) {
        assertMetricDataEmpty(scheduleId, MetricsTable.TWENTY_FOUR_HOUR);
    }

    private void assertMetricDataEmpty(int scheduleId, MetricsTable columnFamily) {
        List<AggregateNumericMetric> metrics = Lists.newArrayList(findAggregateMetrics(columnFamily, scheduleId));
        assertEquals(metrics.size(), 0, "Expected " + columnFamily + " to be empty for schedule id " + scheduleId +
            " but found " + metrics);
    }

    protected void assert1HourMetricsIndexEmpty(DateTime timeSlice) {
        assertMetricsIndexEmpty(MetricsTable.ONE_HOUR, timeSlice);
    }

    protected void assert6HourMetricsIndexEmpty(DateTime timeSlice) {
        assertMetricsIndexEmpty(MetricsTable.SIX_HOUR, timeSlice);
    }

    protected void assert24HourMetricsIndexEmpty(DateTime timeSlice) {
        assertMetricsIndexEmpty(MetricsTable.TWENTY_FOUR_HOUR, timeSlice);
    }

    private void assertMetricsIndexEmpty(MetricsTable table, DateTime timeSlice) {
        List<MetricsIndexEntry> index = Lists.newArrayList(dao.findMetricsIndexEntries(table, timeSlice.getMillis()));
        assertEquals(index.size(), 0, "Expected metrics index for " + table + " to be empty but found " + index);
    }

}
