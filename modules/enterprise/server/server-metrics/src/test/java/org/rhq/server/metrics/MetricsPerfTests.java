package org.rhq.server.metrics;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import com.datastax.driver.core.ResultSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetricMapper;

/**
 * @author John Sanda
 */
public class MetricsPerfTests extends MetricsTest {

    private class MetricsServerStub extends MetricsServer {

        DateTime currentHour;

        @Override
        public DateTime currentHour() {
            return currentHour;
        }

        public void setCurrentHour(DateTime currentHour) {
            this.currentHour = currentHour;
        }
    }

    private class DateTimeServiceStub extends DateTimeService {

        DateTime currentHour;

        long startTime;

        public DateTimeServiceStub(DateTime currentHour, long startTime) {
            this.currentHour = currentHour;
            this.startTime = startTime;
        }

        @Override
        public DateTime now() {
            return currentHour.plus(System.currentTimeMillis() - startTime);
        }

        @Override
        public long nowInMillis() {
            return now().getMillis();
        }
    }

    private final Log log = LogFactory.getLog(MetricsPerfTests.class);

    private MetricsServerStub metricsServer;

    private final int NUM_SCHEDULES = 1000;

    private double requestLimit;

    @BeforeClass
    public void setupClass() throws Exception {
        purgeDB();
        log.info("Sleeping while table truncation completes...");
        Thread.sleep(3000);
        metricsServer = new MetricsServerStub();
        metricsServer.setConfiguration(configuration);
        metricsServer.setDAO(dao);
        metricsServer.setDateTimeService(dateTimeService);
        requestLimit = storageSession.getRequestLimit();
    }

    private void resetRateLimits() {
        storageSession.setRequestLimit(requestLimit);
    }

    @Test
    public void insertRawData() throws Exception {
        Random random = new Random();
        DateTime currentHour = hour(3);
        storageSession.setRequestLimit(10000);
        metricsServer.setCurrentHour(currentHour);
        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>();
        for (int i = 0; i < NUM_SCHEDULES; ++i) {
            DateTime time = currentHour;
            for (int j = 0; j < 120; ++j) {
                data.add(new MeasurementDataNumeric(time.getMillis(), i, random.nextDouble()));
                time = time.plusSeconds(30);
            }
        }
        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.size());
        metricsServer.addNumericData(data, waitForRawInserts);
        waitForRawInserts.await("Failed to add raw data");
    }

    //@Test(dependsOnMethods = "insertRawData")
    public void queryRawDataAsync() throws Exception {
        RateLimiter readPermits = RateLimiter.create(50);

        log.info("Running queryRawDataAsync");
        long start = System.currentTimeMillis();

        DateTime startTime = hour(3).minusHours(1).minusSeconds(1);
        DateTime endTime = hour(3);
        final CountDownLatch rawDataArrival = new CountDownLatch(100);
        final RawNumericMetricMapper mapper = new RawNumericMetricMapper();
        final Map<Integer, List<RawNumericMetric>> rawDataMap =
            new ConcurrentHashMap<Integer, List<RawNumericMetric>>(100);

        for (int i = 0; i < NUM_SCHEDULES; ++i) {
            final int scheduleId = i;
//            readPermits.acquire();
            StorageResultSetFuture rawDataFuture = dao.findRawMetricsAsync(scheduleId, startTime.getMillis(),
                endTime.getMillis());
            Futures.addCallback(rawDataFuture, new FutureCallback<ResultSet>() {
                @Override
                public void onSuccess(ResultSet result) {
                    List<RawNumericMetric> rawData = mapper.mapAll(result);
                    rawDataMap.put(scheduleId, rawData);
                    rawDataArrival.countDown();
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("Failed to retrieve raw data for schedule id " + scheduleId, t);
                }
            });
        }

        rawDataArrival.await();
        log.info("Finished raw data aysnc query in " + (System.currentTimeMillis() - start) + " ms");
    }

    //@Test(dependsOnMethods = "insertRawData")
    public void queryDataSync() throws Exception {
        log.info("Running queryDataSync");

        long start = System.currentTimeMillis();
        DateTime startTime = hour(3).minusHours(1).minusSeconds(1);
        DateTime endTime = hour(3);
        RawNumericMetricMapper mapper = new RawNumericMetricMapper();
        Map<Integer, List<RawNumericMetric>> rawDataMp = new HashMap<Integer, List<RawNumericMetric>>(100);

        for (int i = 0; i < NUM_SCHEDULES; ++i) {
            ResultSet resultSet = dao.findRawMetricsSync(i, startTime.getMillis(), endTime.getMillis());
            rawDataMp.put(i, mapper.mapAll(resultSet));
        }

        log.info("Finished raw data sync query in " + (System.currentTimeMillis() - start) + " ms");
    }

    @Test(dependsOnMethods = "insertRawData")
    public void runAggregation() {
        log.info("Running aggregation");

        resetRateLimits();

        long start = System.currentTimeMillis();
        DateTime currentHour = hour(4);
        metricsServer.setCurrentHour(currentHour);
        metricsServer.setAggregationBatchSize(250);
        metricsServer.setUseAsyncAggregation(false);
        metricsServer.setDateTimeService(new DateTimeServiceStub(hour(4), start));
        Collection<AggregateNumericMetric> oneHourData =
            (Collection<AggregateNumericMetric>) metricsServer.calculateAggregates(0, NUM_SCHEDULES);

        log.info("Finished computing " + oneHourData.size() + " one hour aggregates in " +
            (System.currentTimeMillis() - start) + " ms");
    }

}
