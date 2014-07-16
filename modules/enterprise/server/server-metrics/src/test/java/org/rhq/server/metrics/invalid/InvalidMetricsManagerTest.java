package org.rhq.server.metrics.invalid;

import static org.rhq.server.metrics.domain.AggregateType.AVG;
import static org.rhq.server.metrics.domain.AggregateType.MAX;
import static org.rhq.server.metrics.domain.AggregateType.MIN;
import static org.rhq.server.metrics.domain.MetricsTable.SIX_HOUR;
import static org.rhq.server.metrics.domain.MetricsTable.TWENTY_FOUR_HOUR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.joda.time.DateTime;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.server.metrics.CassandraIntegrationTest;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsConfiguration;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * @author John Sanda
 */
public class InvalidMetricsManagerTest extends CassandraIntegrationTest {

    private MetricsDAO dao;

    private DateTimeService dateTimeService;

    private InvalidMetricsManager invalidMetricsManager;

    @BeforeClass
    public void initClass() {
        MetricsConfiguration configuration = new MetricsConfiguration();

        dateTimeService = new DateTimeService();
        dateTimeService.setConfiguration(configuration);

        dao = new MetricsDAO(storageSession, configuration);
    }

    @BeforeMethod
    public void initMethod() {
        invalidMetricsManager = new InvalidMetricsManager(dateTimeService, dao, 1, 1);
        invalidMetricsManager.setDelay(1000);
        purgeDB();
    }

    @AfterMethod
    public void shutdown() {
        invalidMetricsManager.shutdown();
    }

    private void purgeDB() {
        session.execute("TRUNCATE " + MetricsTable.RAW);
        session.execute("TRUNCATE " + MetricsTable.ONE_HOUR);
        session.execute("TRUNCATE " + MetricsTable.SIX_HOUR);
        session.execute("TRUNCATE " + MetricsTable.TWENTY_FOUR_HOUR);
        session.execute("TRUNCATE " + MetricsTable.METRICS_CACHE);
        session.execute("TRUNCATE " + MetricsTable.METRICS_CACHE_INDEX);
    }

    /**
     * This test exercises a scenario in which we wind up with both invalid 24 hour and 6
     * hour metrics. This happens due to the empty 1 hour metric from the 14:00 hour. See
     * https://bugzilla.redhat.com/show_bug.cgi?id=1117396 for details on how we could end
     * up with an empty aggregate metric.
     */
    @Test
    public void submitInvalid24HourAnd6HourMetricsWithEmpty1HourMetric() throws Exception {
        int scheduleId = 100;

        insert1HourData(scheduleId, hour(0).minusDays(7).plusHours(12), 100, 100, 100);
        insert1HourData(scheduleId, hour(0).minusDays(7).plusHours(13), 100, 100, 100);
        insert1HourData(scheduleId, hour(0).minusDays(7).plusHours(14), Double.NaN, Double.NaN, 0.0);
        insert1HourData(scheduleId, hour(0).minusDays(7).plusHours(15), 100, 100, 100);
        insert1HourData(scheduleId, hour(0).minusDays(7).plusHours(16), 100, 100, 100);
        insert1HourData(scheduleId, hour(0).minusDays(7).plusHours(17), 100, 100, 100);

        insert6HourData(scheduleId, hour(0).minusDays(7), 100, 100, 100);
        insert6HourData(scheduleId, hour(0).minusDays(7).plusHours(6), 100, 100, 100);
        insert6HourData(scheduleId, hour(0).minusDays(7).plusHours(12), 100, 100, 83.33);
        insert6HourData(scheduleId, hour(0).minusDays(7).plusHours(18), 100, 100, 100);

        insert24HourData(scheduleId, hour(0).minusDays(7), 100, 100, 95.83);

        invalidMetricsManager.submit(TWENTY_FOUR_HOUR, new AggregateNumericMetric(scheduleId, 83.33, 100.0, 100.0,
            hour(0).minusDays(7).getMillis()));

        waitForInvalidMetricsToBeProcessed();

        List<AggregateNumericMetric> updated1HourMetrics = dao.findOneHourMetrics(scheduleId,
            hour(0).minusDays(7).plusHours(14).getMillis(), hour(0).minusDays(7).plusHours(14).getMillis());

        assertTrue(updated1HourMetrics.isEmpty(), "Expected 1 hour metric to be deleted since it was empty");

        List<AggregateNumericMetric> updated6HourMetrics = dao.findSixHourMetrics(scheduleId,
            hour(0).minusDays(7).plusHours(12).getMillis(), hour(0).minusDays(7).plusHours(18).getMillis());

        assertEquals(updated6HourMetrics.size(), 1, "Expected to find one 6 hour metric");

        AggregateNumericMetric actual6HourMetric = updated6HourMetrics.get(0);
        AggregateNumericMetric expected6HourMetric = new AggregateNumericMetric(scheduleId, 100.0, 100.0, 100.0,
            hour(0).minusDays(7).plusHours(12).getMillis());

        assertEquals(actual6HourMetric, expected6HourMetric,
            "The updated 6 hour metric does not match the expected value");

        List<AggregateNumericMetric> updated24HourMetrics = dao.findTwentyFourHourMetrics(scheduleId,
            hour(0).minusDays(7).getMillis(), hour0().minusDays(6).getMillis());

        assertEquals(updated24HourMetrics.size(), 1, "Expected to find one 24 hour metric");

        AggregateNumericMetric actual24HourMetric = updated24HourMetrics.get(0);
        AggregateNumericMetric expected24HourMetric = new AggregateNumericMetric(scheduleId, 100.0, 100.0, 100.0,
            hour(0).minusDays(7).getMillis());

        assertEquals(actual24HourMetric, expected24HourMetric,
            "The updated 24 hour metric does not match the expected value");
    }

    /**
     * This test exercises a scenario in which we have both invalid 6 hour and 24 hour
     * metrics, and the 1 hour metrics have already expired. Since those 1 hour metrics are
     * gone, we should delete the 6 hour metric.
     */
    @Test
    public void submitInvalid6HourAnd24HourMetricsWhen1HourMetricsExpired() throws Exception {
        int scheduleId = 100;

        insert6HourData(scheduleId, hour(0).minusDays(7), 100, 100, 100);
        insert6HourData(scheduleId, hour(0).minusDays(7).plusHours(6), 100, 100, 100);
        insert6HourData(scheduleId, hour(0).minusDays(7).plusHours(12), 100, 100, 83.33);
        insert6HourData(scheduleId, hour(0).minusDays(7).plusHours(18), 100, 100, 100);

        insert24HourData(scheduleId, hour(0).minusDays(7), 100, 100, 95.83);

        invalidMetricsManager.submit(SIX_HOUR, new AggregateNumericMetric(scheduleId, 83.33, 100.0, 100.0,
            hour(0).minusDays(7).plusHours(12).getMillis()));

        waitForInvalidMetricsToBeProcessed();

        List<AggregateNumericMetric> updated6HourMetrics = dao.findSixHourMetrics(scheduleId,
            hour(0).minusDays(7).plusHours(12).getMillis(), hour(0).minusDays(7).plusHours(18).getMillis());

        assertTrue(updated6HourMetrics.isEmpty(), "Did not expect to find a 6 hour metric since its 1 hour metrics " +
            "are no longer available");

        List<AggregateNumericMetric> updated24HourMetrics = dao.findTwentyFourHourMetrics(scheduleId,
            hour(0).minusDays(7).getMillis(), hour0().minusDays(6).getMillis());

        assertEquals(updated24HourMetrics.size(), 1, "Expected to find one 24 hour metric");

        AggregateNumericMetric actual24HourMetric = updated24HourMetrics.get(0);
        AggregateNumericMetric expected24HourMetric = new AggregateNumericMetric(scheduleId, 100.0, 100.0, 100.0,
            hour(0).minusDays(7).getMillis());

        assertEquals(actual24HourMetric, expected24HourMetric,
            "The updated 24 hour metric does not match the expected value");
    }

    /**
     * This text exercises a scenario in which we have an invalid 24 hour metric, and the 6
     * hour metrics have already expired.
     */
    @Test
    public void submitInvalid24HourMetricWhen6HourMetricsExpired() throws Exception {
        int scheduleId = 100;

        insert24HourData(scheduleId, hour(0).minusDays(7), 100, 100, 95.83);

        invalidMetricsManager.submit(TWENTY_FOUR_HOUR, new AggregateNumericMetric(scheduleId, 83.33, 100.0, 100.0,
            hour(0).minusDays(7).getMillis()));

        waitForInvalidMetricsToBeProcessed();

        List<AggregateNumericMetric> updated24HourMetrics = dao.findTwentyFourHourMetrics(scheduleId,
            hour(0).minusDays(7).getMillis(), hour0().minusDays(6).getMillis());

        assertTrue(updated24HourMetrics.isEmpty(), "Did not expect to find 24 hour metric since 6 hour metrics are " +
            "not available");
    }

    @Test
    public void addMultipleMetricsFromSameDayToQueue() throws Exception {
        int scheduleId = 100;

        invalidMetricsManager.setDelay(30000);

        invalidMetricsManager.submit(SIX_HOUR, new AggregateNumericMetric(scheduleId, 83.33, 100.0, 100.0,
            hour(6).getMillis()));
        invalidMetricsManager.submit(SIX_HOUR, new AggregateNumericMetric(scheduleId, 83.33, 100.0, 100.0,
            hour(12).getMillis()));
        invalidMetricsManager.submit(TWENTY_FOUR_HOUR, new AggregateNumericMetric(scheduleId, 83.33, 100.0, 100.0,
            hour(0).getMillis()));

        assertEquals(invalidMetricsManager.getRemainingInvalidMetrics(), 1, "Expected there to be 1 invalid metric " +
            "in the queue since one element is stored for metrics falling within the same day.");

        invalidMetricsManager.submit(SIX_HOUR, new AggregateNumericMetric(scheduleId, 83.33, 100.0, 100.0,
            hour(0).minusDays(1).plusHours(6).getMillis()));

        assertEquals(invalidMetricsManager.getRemainingInvalidMetrics(), 2, "Expected there to be 2 invalid metrics " +
            "in the queue");
    }

    private void waitForInvalidMetricsToBeProcessed() throws InterruptedException {
        while (invalidMetricsManager.getRemainingInvalidMetrics() > 0) {
            Thread.sleep(1000);
        }
    }

    private void insert1HourData(int scheduleId, DateTime time, double max, double min, double avg) {
        dao.insertOneHourData(scheduleId, time.getMillis(), MAX, max);
        dao.insertOneHourData(scheduleId, time.getMillis(), MIN, min);
        dao.insertOneHourData(scheduleId, time.getMillis(), AVG, avg);
    }

    private void insert6HourData(int scheduleId, DateTime time, double max, double min, double avg) {
        dao.insertSixHourData(scheduleId, time.getMillis(), MAX, max);
        dao.insertSixHourData(scheduleId, time.getMillis(), MIN, min);
        dao.insertSixHourData(scheduleId, time.getMillis(), AVG, avg);
    }

    private void insert24HourData(int scheduleId, DateTime time, double max, double min, double avg) {
        dao.insertTwentyFourHourData(scheduleId, time.getMillis(), MAX, max);
        dao.insertTwentyFourHourData(scheduleId, time.getMillis(), MIN, min);
        dao.insertTwentyFourHourData(scheduleId, time.getMillis(), AVG, avg);
    }

    private DateTime hour(int hours) {
        return dateTimeService.hour0().plusHours(hours);
    }

}
