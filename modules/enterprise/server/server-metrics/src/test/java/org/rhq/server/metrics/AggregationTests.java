package org.rhq.server.metrics;

import static java.util.Arrays.asList;
import static org.rhq.test.AssertUtils.assertCollectionEqualsNoOrder;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import com.datastax.driver.core.ResultSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.server.metrics.aggregation.AggregationManager;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.RawNumericMetric;

/**
 * @author John Sanda
 */
public class AggregationTests extends MetricsTest {

    private Aggregates schedule1 = new Aggregates();
    private Aggregates schedule2 = new Aggregates();
    private Aggregates schedule3 = new Aggregates();
    private Aggregates schedule4 = new Aggregates();
    private Aggregates schedule5 = new Aggregates();

    private ListeningExecutorService aggregationTasks;

    private DateTime currentHour;

    private final int MIN_SCHEDULE_ID = 100;

    private final int MAX_SCHEDULE_ID = 200;

    private final int BATCH_SIZE = 10;

    private final int INDEX_PARTITION = 0;

    private MetricsServerStub metricsServer;

    private DateTimeServiceStub dateTimeService;

    private InMemoryMetricsDB testdb;

    @BeforeClass
    public void setUp() throws Exception {
        purgeDB();

        schedule1.id = 100;
        schedule2.id = 101;
        schedule3.id = 131;
        schedule4.id = 104;
        schedule5.id = 105;

        testdb = new InMemoryMetricsDB();
        dateTimeService = new DateTimeServiceStub();
        metricsServer = new MetricsServerStub();
        metricsServer.setConfiguration(new MetricsConfiguration());
        metricsServer.setDateTimeService(dateTimeService);
        metricsServer.setDAO(dao);
        metricsServer.setCacheBatchSize(PARTITION_SIZE);
        metricsServer.init();

        aggregationTasks = metricsServer.getAggregationWorkers();
    }

    @Test
    public void insertRawDataDuringHour16() throws Exception {
        dateTimeService.setNow(hour(16).plusMinutes(41));
        insertRawData(
            newRawData(hour(16).plusMinutes(20), schedule1.id, 3.0),
            newRawData(hour(16).plusMinutes(40), schedule1.id, 5.0),
            newRawData(hour(16).plusMinutes(15), schedule2.id, 0.0032),
            newRawData(hour(16).plusMinutes(30), schedule2.id, 0.104),
            newRawData(hour(16).plusMinutes(7), schedule3.id, 3.14)
        );
    }

    @Test(dependsOnMethods = "insertRawDataDuringHour16")
    public void runAggregationForHour16() throws Exception {
        currentHour = hour(17);
        testdb.aggregateRawData(hour(16), hour(17));
        AggregationManagerTestStub aggregator = new AggregationManagerTestStub(hour(16));

        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        assertCollectionEqualsNoOrder(testdb.get1HourData(hour(16)), oneHourData,
            "The returned one hour aggregates are wrong");
        // verify values in the db
        assert1HourDataEquals(schedule1.id, testdb.get1HourData(hour(16), schedule1.id));
        assert1HourDataEquals(schedule2.id, testdb.get1HourData(hour(16), schedule2.id));
        assert1HourDataEquals(schedule3.id, testdb.get1HourData(hour(16), schedule3.id));

        assert6HourDataEmpty(schedule1.id, schedule2.id, schedule3.id);
        assert24HourDataEmpty(schedule1.id, schedule2.id, schedule3.id);

        assertRawCacheEmpty(hour(16), startScheduleId(schedule1.id), startScheduleId(schedule3.id));
        assertRawCacheIndexEmpty(hour(16), INDEX_PARTITION);

        assert1HourCacheEquals(hour(12), startScheduleId(schedule1.id),
            asList(testdb.get1HourData(hour(16), schedule1.id), testdb.get1HourData(hour(16), schedule2.id)));
        assert1HourCacheIndexEquals(hour(12), INDEX_PARTITION, asList(
            new1HourCacheIndexEntry(startScheduleId(schedule1.id), hour(12)),
            new1HourCacheIndexEntry(startScheduleId(schedule3.id), hour(12))
        ));

        assert6HourCacheEmpty(hour(0), startScheduleId(schedule1.id));
        assert6HourCacheIndexEmpty(hour(0), INDEX_PARTITION);
    }

    @Test(dependsOnMethods = "runAggregationForHour16")
    public void insertRawDataDuringHour17() throws Exception {
        dateTimeService.setNow(hour(17).plusMinutes(50));
        insertRawData(
            newRawData(hour(17).plusMinutes(20), schedule1.id, 11.0),
            newRawData(hour(17).plusMinutes(40), schedule1.id, 16.0),
            newRawData(hour(17).plusMinutes(30), schedule2.id, 0.092),
            newRawData(hour(17).plusMinutes(45), schedule2.id, 0.0733)
        );
    }

    @Test(dependsOnMethods = "insertRawDataDuringHour17")
    public void runAggregationForHour17() throws Exception {
        currentHour = hour(18);
        testdb.aggregateRawData(hour(17), hour(18));
        testdb.aggregate1HourData(hour(12), hour(18));
        AggregationManagerTestStub aggregator = new AggregationManagerTestStub(hour(17));

        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        assertCollectionEqualsNoOrder(testdb.get1HourData(hour(17)), oneHourData,
            "The returned one hour data is wrong");
        // verify values in the db
        assert1HourDataEquals(schedule1.id, testdb.get1HourData(schedule1.id));
        assert1HourDataEquals(schedule2.id, testdb.get1HourData(schedule2.id));
        assert1HourDataEquals(schedule3.id, testdb.get1HourData(schedule3.id));

        assert6HourDataEquals(schedule1.id, testdb.get6HourData(hour(12), schedule1.id));
        assert6HourDataEquals(schedule2.id, testdb.get6HourData(hour(12), schedule2.id));
        assert6HourDataEquals(schedule3.id, testdb.get6HourData(hour(12), schedule3.id));

        assert24HourDataEmpty(schedule1.id, schedule2.id, schedule3.id);

        assertRawCacheEmpty(hour(17), startScheduleId(schedule1.id), startScheduleId(schedule3.id));
        assertRawCacheIndexEmpty(hour(17), INDEX_PARTITION);

        assert1HourCacheEmpty(hour(12), startScheduleId(schedule1.id), startScheduleId(schedule3.id));
        assert1HourCacheIndexEmpty(hour(12), INDEX_PARTITION);

        assert6HourCacheEquals(hour(0), startScheduleId(schedule1.id), testdb.get6HourData(scheduleIds(schedule1.id)));
        assert6HourCacheEquals(hour(0), startScheduleId(schedule3.id), testdb.get6HourData(scheduleIds(schedule3.id)));
        assert6HourCacheIndexEquals(hour(0), INDEX_PARTITION, asList(
            new6HourCacheIndexEntry(startScheduleId(schedule1.id), hour(0)),
            new6HourCacheIndexEntry(startScheduleId(schedule3.id), hour(0))
        ));
    }


    @Test(dependsOnMethods = "runAggregationForHour17")
    public void insertRawDataDuringHour18() throws Exception {
        dateTimeService.setNow(hour(18).plusMinutes(50));
        insertRawData(
            newRawData(hour(18).plusMinutes(20), schedule1.id, 22.0),
            newRawData(hour(18).plusMinutes(40), schedule1.id, 26.0),
            newRawData(hour(18).plusMinutes(15), schedule2.id, 0.205),
            newRawData(hour(18).plusMinutes(15), schedule3.id, 2.42)
        );
    }

    @Test(dependsOnMethods = "insertRawDataDuringHour18")
    public void runAggregationForHour18() throws Exception {
        currentHour = hour(19);
        testdb.aggregateRawData(hour(18), hour(19));
        AggregationManagerTestStub aggregator = new AggregationManagerTestStub(hour(18));
        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        assertCollectionEqualsNoOrder(testdb.get1HourData(hour(18)), oneHourData, "The returned 1 hour data is wrong");
        // verify values in db
        assert1HourDataEquals(schedule1.id, testdb.get1HourData(schedule1.id));
        assert1HourDataEquals(schedule2.id, testdb.get1HourData(schedule2.id));
        assert1HourDataEquals(schedule3.id, testdb.get1HourData(schedule3.id));

        assert6HourDataEquals(schedule1.id, testdb.get6HourData(schedule1.id));
        assert6HourDataEquals(schedule2.id, testdb.get6HourData(schedule2.id));
        assert6HourDataEquals(schedule3.id, testdb.get6HourData(schedule3.id));

        assert24HourDataEmpty(schedule1.id, schedule2.id, schedule3.id);

        assertRawCacheEmpty(hour(18), startScheduleId(schedule1.id), startScheduleId(schedule3.id));
        assertRawCacheIndexEmpty(hour(18), INDEX_PARTITION);

        assert1HourCacheEmpty(hour(12), startScheduleId(schedule1.id));
        assert1HourCacheEmpty(hour(12), startScheduleId(schedule3.id));
        assert1HourCacheIndexEmpty(hour(12), INDEX_PARTITION);

        assert1HourCacheEquals(hour(18), startScheduleId(schedule1.id), asList(
            testdb.get1HourData(hour(18), schedule1.id),
            testdb.get1HourData(hour(18), schedule2.id)));
        assert1HourCacheEquals(hour(18), startScheduleId(schedule3.id),
            asList(testdb.get1HourData(hour(18), schedule3.id)));
        assert1HourCacheIndexEquals(hour(18), INDEX_PARTITION, asList(
            new1HourCacheIndexEntry(startScheduleId(schedule1.id), hour(18)),
            new1HourCacheIndexEntry(startScheduleId(schedule3.id), hour(18))
        ));

        assert6HourCacheEquals(hour(0), startScheduleId(schedule1.id), testdb.get6HourData(schedule1.id, schedule2.id));
        assert6HourCacheEquals(hour(0), startScheduleId(schedule3.id), testdb.get6HourData(schedule3.id));
        assert6HourCacheIndexEquals(hour(0), INDEX_PARTITION, asList(
            new6HourCacheIndexEntry(startScheduleId(schedule1.id), hour(0)),
            new6HourCacheIndexEntry(startScheduleId(schedule3.id), hour(0))
        ));
    }

    @Test(dependsOnMethods = "runAggregationForHour18")
    public void insertRawDataDuringHour23() throws Exception {
        dateTimeService.setNow(hour(23).plusMinutes(50));
        insertRawData(
            newRawData(hour(23).plusMinutes(25), schedule1.id, 34.0),
            newRawData(hour(23).plusMinutes(30), schedule2.id, 0.322)
        );
    }

    @Test(dependsOnMethods = "insertRawDataDuringHour23")
    public void runAggregationForHour24() throws Exception {
        currentHour = hour(24);
        testdb.aggregateRawData(hour(23), hour(24));
        testdb.aggregate1HourData(hour(18), hour(24));
        testdb.aggregate6HourData(hour(0), hour(24));
        AggregationManagerTestStub aggregator = new AggregationManagerTestStub(hour(23));
        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        assertCollectionEqualsNoOrder(testdb.get1HourData(hour(23)), oneHourData, "The returned 1 hour data is wrong");
        // verify values in db
        assert1HourDataEquals(schedule1.id, testdb.get1HourData(schedule1.id));
        assert1HourDataEquals(schedule2.id, testdb.get1HourData(schedule2.id));
        assert1HourDataEquals(schedule3.id, testdb.get1HourData(schedule3.id));

        assert6HourDataEquals(schedule1.id, testdb.get6HourData(schedule1.id));
        assert6HourDataEquals(schedule2.id, testdb.get6HourData(schedule2.id));
        assert6HourDataEquals(schedule3.id, testdb.get6HourData(schedule3.id));

        assert24HourDataEquals(schedule1.id, testdb.get24HourData(schedule1.id));
        assert24HourDataEquals(schedule2.id, testdb.get24HourData(schedule2.id));
        assert24HourDataEquals(schedule3.id, testdb.get24HourData(schedule3.id));

        assertRawCacheEmpty(hour(18), startScheduleId(schedule1.id), startScheduleId(schedule3.id));
        assertRawCacheIndexEmpty(hour(18), INDEX_PARTITION);

        assert1HourCacheEmpty(hour(18), startScheduleId(schedule1.id));
        assert1HourCacheEmpty(hour(18), startScheduleId(schedule3.id));
        assert1HourCacheIndexEmpty(hour(18), INDEX_PARTITION);

        assert6HourCacheEmpty(hour(18), startScheduleId(schedule1.id));
        assert6HourCacheEmpty(hour(18), startScheduleId(schedule3.id));
        assert6HourCacheIndexEmpty(hour(18), INDEX_PARTITION);
    }

//    @Test(dependsOnMethods = "runAggregationForHour24")
    public void resetDBForFailureScenarios() throws Exception {
        purgeDB();
    }

//    @Test(dependsOnMethods = "resetDBForFailureScenarios")
    public void doNotDeleteCachePartitionOnBatchFailure() throws Exception {
        currentHour = hour(5);
        DateTime time = hour(4).plusMinutes(20);
        insertRawData(hour(4), new MeasurementDataNumeric(time.getMillis(), schedule1.id, 3.0))
            .await("Failed to insert raw data");

        TestDAO testDAO = new TestDAO() {
            @Override
            public StorageResultSetFuture insertOneHourDataAsync(int scheduleId, long timestamp, AggregateType type,
                double value) {
                StorageResultSetFuture future = super.insertOneHourDataAsync(scheduleId, timestamp, type, value);
                future.setException(new Exception("An unexpected error occurred while inserting 1 hour data"));
                return future;
            }
        };

        AggregationManagerTestStub aggregationManager = new AggregationManagerTestStub(hour(4), testDAO);
        aggregationManager.run();

        assertRawCacheEquals(hour(4), startScheduleId(schedule1.id), asList(new RawNumericMetric(schedule1.id,
            time.getMillis(), 3.0)));
    }

    //@Test(dependsOnMethods = "resetDBForFailureScenarios")
//    public void failToFetchRawDataIndexDuringAggregationForHour12() throws Exception {
//        currentHour = hour(12);
//        AggregationManagerTestStub aggregator = new AggregationManagerTestStub(hour(11), new MetricsDAO(storageSession, configuration) {
//            @Override
//            public StorageResultSetFuture findMetricsIndexEntriesAsync(MetricsTable table, long timestamp) {
//                if (table == MetricsTable.ONE_HOUR) {
//                    return new FailedStorageResultSetFuture(new Exception("Failed to fetch raw data index"));
//                } else {
//                    return super.findMetricsIndexEntriesAsync(table,
//                        timestamp);
//                }
//            }
//        });
//
//        insertRawData(
//            new MeasurementDataNumeric(hour(12).plusMinutes(10).getMillis(), schedule4.id, 7.456),
//            new MeasurementDataNumeric(hour(12).plusMinutes(14).getMillis(), schedule5.id, 29.3)
//        ).await("Failed to insert raw data");
//
//        updateIndex(
//            new IndexUpdate(MetricsTable.ONE_HOUR, schedule4.id, hour(12)),
//            new IndexUpdate(MetricsTable.ONE_HOUR, schedule5.id, hour(12))
//        ).await("Failed to update raw data index");
//
//        insert1HourData(
//            new AggregateNumericMetric(schedule4.id, 26.6, 18.33, 29.02, hour(10).getMillis()),
//            new AggregateNumericMetric(schedule4.id, 25.2, 21.12, 28.05, hour(11).getMillis())
//        ).await("Failed to insert 1 hour data");
//
//        updateIndex(new IndexUpdate(MetricsTable.SIX_HOUR, schedule4.id, hour(6)))
//            .await("Failed to update 1 hr data index");
//
//        schedule4.oneHourData.put(hour(10), new AggregateNumericMetric(schedule4.id, 26.6, 18.33, 29.02,
//            hour(10).getMillis()));
//        schedule4.oneHourData.put(hour(11), new AggregateNumericMetric(schedule4.id, 25.2, 21.12, 28.05,
//            hour(11).getMillis()));
//        schedule4.sixHourData.put(hour(6), new AggregateNumericMetric(schedule4.id,
//            avg(schedule4.oneHourData, hour(10), hour(11)),
//            min(schedule4.oneHourData, hour(10), hour(11)),
//            max(schedule4.oneHourData, hour(10), hour(11)),
//            hour(6).getMillis()));
//
//        Set<AggregateNumericMetric> oneHourData = aggregator.run();
//        List<AggregateNumericMetric> emptyAggregates = Collections.emptyList();
//
//        assertTrue(oneHourData.isEmpty(), "Did not expect to get back any one hour aggregates");
//        // verify values in db
//        assert1HourDataEquals(schedule4.id, schedule4.oneHourData.get(hour(10)), schedule4.oneHourData.get(hour(11)));
//        assert1HourDataEquals(schedule5.id, emptyAggregates);
//        assert6HourDataEquals(schedule4.id, schedule4.sixHourData.get(hour(6)));
//        assert6HourDataEmpty(schedule5.id);
//        assert24HourDataEmpty(schedule4.id);
//        assert24HourDataEmpty(schedule5.id);
//        assert1HourMetricsIndexEmpty(hour(11));
//        assert6HourMetricsIndexEmpty(hour(6));
//        assert24HourIndexEquals(hour(0), schedule4.id);
//    }

    private void insertRawData(MeasurementDataNumeric... data) throws Exception {
        Set<MeasurementDataNumeric> dataSet = Sets.newHashSet(data);
        for (MeasurementDataNumeric datum : data) {
            testdb.putRawData(new RawNumericMetric(datum.getScheduleId(), datum.getTimestamp(), datum.getValue()));
        }
        WaitForRawInserts waitForRawInserts = new WaitForRawInserts(data.length);
        metricsServer.addNumericData(dataSet, waitForRawInserts);

        waitForRawInserts.await("Failed to insert raw data");
    }

    private MeasurementDataNumeric newRawData(DateTime timestamp, int scheduleId, double value) {
        return new MeasurementDataNumeric(timestamp.getMillis(), scheduleId, value);
    }

    private int[] scheduleIds(int scheduleId) {
        int[] ids = new int[BATCH_SIZE];
        int startId = startScheduleId(scheduleId);
        for (int i = 0; i < BATCH_SIZE; ++i) {
            ids[i] = startId + i;
        }
        return ids;
    }

    private WaitForWrite insert1HourData(AggregateNumericMetric... data) {
        WaitForWrite waitForWrite = new WaitForWrite(data.length * 3);
        for (AggregateNumericMetric datum : data) {
            StorageResultSetFuture future = dao.insertOneHourDataAsync(datum.getScheduleId(), datum.getTimestamp(),
                AggregateType.AVG, datum.getAvg());
            Futures.addCallback(future, waitForWrite);

            future = dao.insertOneHourDataAsync(datum.getScheduleId(), datum.getTimestamp(), AggregateType.MIN,
                datum.getMin());
            Futures.addCallback(future, waitForWrite);

            future = dao.insertOneHourDataAsync(datum.getScheduleId(), datum.getTimestamp(), AggregateType.MAX,
                datum.getMax());
            Futures.addCallback(future, waitForWrite);
        }
        return waitForWrite;
    }

    private double avg(Map<DateTime, AggregateNumericMetric> data, DateTime... times) {
        double[] values = new double[times.length];
        for (int i = 0; i < times.length; ++i) {
            values[i] = data.get(times[i]).getAvg();
        }
        return avg(values);
    }

    private double min(Map<DateTime, AggregateNumericMetric> data, DateTime... times) {
        double min = data.get(times[0]).getMin();
        for (DateTime time : times) {
            if (data.get(time).getMin() < min) {
                min = data.get(time).getMin();
            }
        }
        return min;
    }

    private double max(Map<DateTime, AggregateNumericMetric> data, DateTime... times) {
        double max = data.get(times[0]).getMin();
        for (DateTime time : times) {
            if (data.get(time).getMax() > max) {
                max = data.get(time).getMax();
            }
        }
        return max;
    }

    private class AggregationManagerTestStub extends AggregationManager {

        public AggregationManagerTestStub(DateTime startTime) {
            super(aggregationTasks, dao, configuration, dateTimeService, startTime, BATCH_SIZE, 4, MIN_SCHEDULE_ID,
                MAX_SCHEDULE_ID, PARTITION_SIZE);
        }

        public AggregationManagerTestStub(DateTime startTime, MetricsDAO dao) {
            super(aggregationTasks, dao, configuration, dateTimeService, startTime, BATCH_SIZE, 4, MIN_SCHEDULE_ID,
                MAX_SCHEDULE_ID, PARTITION_SIZE);
        }

        @Override
        protected DateTime currentHour() {
            return currentHour;
        }
    }

    private class Aggregates {
        int id;  // schedule id
        Map<DateTime, AggregateNumericMetric> oneHourData = new HashMap<DateTime, AggregateNumericMetric>();
        Map<DateTime, AggregateNumericMetric> sixHourData = new HashMap<DateTime, AggregateNumericMetric>();
        Map<DateTime, AggregateNumericMetric> twentyFourHourData = new HashMap<DateTime, AggregateNumericMetric>();
    }

    private class TestDAO extends MetricsDAO {

        public TestDAO() {
            super(storageSession, configuration);
        }
    }

    private class FailedStorageResultSetFuture extends StorageResultSetFuture implements ListenableFuture<ResultSet> {

        private SettableFuture future;

        private Throwable t;

        public FailedStorageResultSetFuture(Throwable t) {
            super(null, null);
            future = SettableFuture.create();
            this.t = t;
            assertTrue(future.setException(t), "Failed to set exception for future");
        }

        @Override
        public void addListener(Runnable listener, Executor executor) {
            future.addListener(listener, executor);
        }

        @Override
        public ResultSet get() {
            throw new AssertionError();
        }
    }
}
