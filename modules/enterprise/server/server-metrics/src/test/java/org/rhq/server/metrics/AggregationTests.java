package org.rhq.server.metrics;

import static java.util.Arrays.asList;
import static org.rhq.test.AssertUtils.assertCollectionEqualsNoOrder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.datastax.driver.core.ResultSet;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.server.metrics.aggregation.AggregateCacheMapper;
import org.rhq.server.metrics.aggregation.Aggregator;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.MetricsIndexEntry;
import org.rhq.server.metrics.domain.MetricsTable;

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

//    private final int MAX_SCHEDULE_ID = 1000;
    private final int MAX_SCHEDULE_ID = 200;

    private final int BATCH_SIZE = 10;

    private final int CACHE_BATCH_SIZE = 2;

    private AggregateCacheMapper aggregateCacheMapper = new AggregateCacheMapper();

    @BeforeClass
    public void setUp() throws Exception {
        purgeDB();

        schedule1.id = 100;
        schedule2.id = 101;
        schedule3.id = 102;
        schedule4.id = 104;
        schedule5.id = 105;

        aggregationTasks = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
    }

    @Test
    public void insertRawDataDuringHour16() throws Exception {
        insertRawData(hour(16),
            new MeasurementDataNumeric(hour(16).plusMinutes(20).getMillis(), schedule1.id, 3.0),
            new MeasurementDataNumeric(hour(16).plusMinutes(40).getMillis(), schedule1.id, 5.0),
            new MeasurementDataNumeric(hour(16).plusMinutes(15).getMillis(), schedule2.id, 0.0032),
            new MeasurementDataNumeric(hour(16).plusMinutes(30).getMillis(), schedule2.id, 0.104),
            new MeasurementDataNumeric(hour(16).plusMinutes(7).getMillis(), schedule3.id, 3.14)
        ).await("Failed to insert raw data");
    }

    @Test(dependsOnMethods = "insertRawDataDuringHour16")
    public void runAggregationForHour16() throws Exception {
        currentHour = hour(17);
        AggregatorTestStub aggregator = new AggregatorTestStub(hour(16));

        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        schedule1.oneHourData.put(hour(16), new AggregateNumericMetric(schedule1.id, avg(3.0, 5.0), 3.0, 5.0,
            hour(16).getMillis()));
        schedule2.oneHourData.put(hour(16), new AggregateNumericMetric(schedule2.id, avg(0.0032, 0.104), 0.0032, 0.104,
            hour(16).getMillis()));
        schedule3.oneHourData.put(hour(16), new AggregateNumericMetric(schedule3.id, 3.14, 3.14, 3.14,
            hour(16).getMillis()));

        List<AggregateNumericMetric> expected = asList(schedule1.oneHourData.get(hour(16)),
            schedule2.oneHourData.get(hour(16)), schedule3.oneHourData.get(hour(16)));
        assertCollectionEqualsNoOrder(expected, oneHourData, "The returned one hour aggregates are wrong");
        // verify values in the db
        assert1HourDataEquals(schedule1.id, schedule1.oneHourData.get(hour(16)));
        assert1HourDataEquals(schedule2.id, schedule2.oneHourData.get(hour(16)));
        assert1HourDataEquals(schedule3.id, schedule3.oneHourData.get(hour(16)));
        assert6HourCacheEquals(hour(12), startScheduleId(schedule1.id), expected);
        assert6HourDataEmpty(schedule1.id);
        assert6HourDataEmpty(schedule2.id);
        assert6HourDataEmpty(schedule3.id);
        assert24HourCacheEmpty(hour(0), startScheduleId(schedule1.id));
        assert1HourCacheEmpty(hour(16), startScheduleId(schedule1.id));
    }

    @Test(dependsOnMethods = "runAggregationForHour16")
    public void insertRawDataDuringHour17() throws Exception {
        insertRawData(hour(17),
            new MeasurementDataNumeric(hour(17).plusMinutes(20).getMillis(), schedule1.id, 11.0),
            new MeasurementDataNumeric(hour(17).plusMinutes(40).getMillis(), schedule1.id, 16.0),
            new MeasurementDataNumeric(hour(17).plusMinutes(30).getMillis(), schedule2.id, 0.092),
            new MeasurementDataNumeric(hour(17).plusMinutes(45).getMillis(), schedule2.id, 0.0733)
        ).await("Failed to insert raw data");
    }

    @Test(dependsOnMethods = "insertRawDataDuringHour17")
    public void runAggregationForHour17() throws Exception {
        currentHour = hour(18);
        AggregatorTestStub aggregator = new AggregatorTestStub(hour(17));

        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        schedule1.oneHourData.put(hour(17), new AggregateNumericMetric(schedule1.id, avg(11.0, 16.0), 11.0, 16.0,
            hour(17).getMillis()));
        schedule2.oneHourData.put(hour(17), new AggregateNumericMetric(schedule2.id, avg(0.092, 0.0733), 0.0733, 0.092,
            hour(17).getMillis()));

        schedule1.sixHourData.put(hour(12), new AggregateNumericMetric(schedule1.id,
            avg(schedule1.oneHourData, hour(16), hour(17)), min(schedule1.oneHourData, hour(16), hour(17)),
            max(schedule1.oneHourData, hour(16), hour(17)), hour(12).getMillis()));
        schedule2.sixHourData.put(hour(12), new AggregateNumericMetric(schedule2.id,
            avg(schedule2.oneHourData, hour(16), hour(17)), min(schedule2.oneHourData, hour(16), hour(17)),
            max(schedule2.oneHourData, hour(16), hour(17)), hour(12).getMillis()));
        schedule3.sixHourData.put(hour(12), new AggregateNumericMetric(schedule3.id, 3.14, 3.14, 3.14,
            hour(12).getMillis()));

        List<AggregateNumericMetric> expected = asList(schedule1.oneHourData.get(hour(17)),
            schedule2.oneHourData.get(hour(17)));
        assertCollectionEqualsNoOrder(expected, oneHourData, "The returned one hour data is wrong");
        // verify values in the db
        assert1HourDataEquals(schedule1.id, schedule1.oneHourData.get(hour(16)), schedule1.oneHourData.get(hour(17)));
        assert1HourDataEquals(schedule2.id, schedule2.oneHourData.get(hour(16)), schedule2.oneHourData.get(hour(17)));
        assert1HourDataEquals(schedule3.id, schedule3.oneHourData.get(hour(16)));
        assert6HourDataEquals(schedule1.id, schedule1.sixHourData.get(hour(12)));
        assert6HourDataEquals(schedule2.id, schedule2.sixHourData.get(hour(12)));
        assert6HourDataEquals(schedule3.id, schedule3.sixHourData.get(hour(12)));
        assert24HourDataEmpty(schedule1.id);
        assert24HourDataEmpty(schedule2.id);
        assert24HourDataEmpty(schedule3.id);
        assert1HourCacheEmpty(hour(17), startScheduleId(schedule1.id));
        assert6HourCacheEmpty(hour(12), startScheduleId(schedule1.id));
        assert24HourCacheEquals(hour(0), startScheduleId(schedule1.id), asList(schedule1.sixHourData.get(hour(12)),
            schedule2.sixHourData.get(hour(12)), schedule3.sixHourData.get(hour(12))
        ));
    }

    @Test(dependsOnMethods = "runAggregationForHour17")
    public void insertRawDataDuringHour18() throws Exception {
        insertRawData(hour(18),
            new MeasurementDataNumeric(hour(18).plusMinutes(20).getMillis(), schedule1.id, 22.0),
            new MeasurementDataNumeric(hour(18).plusMinutes(40).getMillis(), schedule1.id, 26.0),
            new MeasurementDataNumeric(hour(18).plusMinutes(15).getMillis(), schedule2.id, 0.205),
            new MeasurementDataNumeric(hour(18).plusMinutes(15).getMillis(), schedule3.id, 2.42)
        ).await("Failed to insert raw data");
    }

    @Test(dependsOnMethods = "insertRawDataDuringHour18")
    public void runAggregationForHour18() throws Exception {
        currentHour = hour(19);
        AggregatorTestStub aggregator = new AggregatorTestStub(hour(18));

        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        schedule1.oneHourData.put(hour(18), new AggregateNumericMetric(schedule1.id, avg(22.0, 26.0), 22.0, 26.0,
            hour(18).getMillis()));
        schedule2.oneHourData.put(hour(18), new AggregateNumericMetric(schedule2.id, 0.205, 0.205, 0.205,
            hour(18).getMillis()));
        schedule3.oneHourData.put(hour(18), new AggregateNumericMetric(schedule3.id, 2.42, 2.42, 2.42,
            hour(18).getMillis()));

        List<AggregateNumericMetric> expected = asList(schedule1.oneHourData.get(hour(18)),
            schedule2.oneHourData.get(hour(18)), schedule3.oneHourData.get(hour(18)));
        assertCollectionEqualsNoOrder(expected, oneHourData, "The returned one hour data is wrong");
        // verify values in db
        assert1HourDataEquals(schedule1.id, schedule1.oneHourData.get(hour(18)), schedule1.oneHourData.get(hour(17)),
            schedule1.oneHourData.get(hour(16)));
        assert1HourDataEquals(schedule2.id, schedule2.oneHourData.get(hour(18)), schedule2.oneHourData.get(hour(17)),
            schedule2.oneHourData.get(hour(16)));
        assert1HourDataEquals(schedule3.id, schedule3.oneHourData.get(hour(18)), schedule3.oneHourData.get(hour(16)));
        assert6HourDataEquals(schedule1.id, schedule1.sixHourData.get(hour(12)));
        assert6HourDataEquals(schedule2.id, schedule2.sixHourData.get(hour(12)));
        assert6HourDataEquals(schedule3.id, schedule3.sixHourData.get(hour(12)));
        assert6HourCacheEquals(hour(18), startScheduleId(schedule1.id), expected);
        assert24HourDataEmpty(schedule1.id);
        assert24HourDataEmpty(schedule2.id);
        assert24HourDataEmpty(schedule3.id);
        assert24HourCacheEquals(hour(0), startScheduleId(schedule1.id), asList(schedule1.sixHourData.get(hour(12)),
            schedule2.sixHourData.get(hour(12)), schedule3.sixHourData.get(hour(12))));
        assert1HourCacheEmpty(hour(18), startScheduleId(schedule1.id));
    }

    @Test(dependsOnMethods = "runAggregationForHour18")
    public void insertRawDataDuringHour23() throws Exception {
        insertRawData(hour(23),
            new MeasurementDataNumeric(hour(23).plusMinutes(25).getMillis(), schedule1.id, 34.0),
            new MeasurementDataNumeric(hour(23).plusMinutes(30).getMillis(), schedule2.id, 0.322)
        ).await("Failed to insert raw data");
    }

    @Test(dependsOnMethods = "insertRawDataDuringHour23")
    public void runAggregationForHour24() throws Exception {
        currentHour = hour(24);
        AggregatorTestStub aggregator = new AggregatorTestStub(hour(23));

        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        schedule1.oneHourData.put(hour(23), new AggregateNumericMetric(schedule1.id, 34.0, 34.0, 34.0,
            hour(23).getMillis()));
        schedule1.sixHourData.put(hour(18), new AggregateNumericMetric(schedule1.id,
            avg(schedule1.oneHourData, hour(18), hour(23)),
            min(schedule1.oneHourData, hour(18), hour(23)),
            max(schedule1.oneHourData, hour(18), hour(23)),
            hour(18).getMillis()));
        schedule1.twentyFourHourData.put(hour(0),
            new AggregateNumericMetric(schedule1.id,
                avg(schedule1.sixHourData, hour(12), hour(18)),
                min(schedule1.sixHourData, hour(12), hour(18)),
                max(schedule1.sixHourData, hour(12), hour(18)),
                hour(0).getMillis()));
        schedule2.oneHourData.put(hour(23), new AggregateNumericMetric(schedule2.id, 0.322, 0.322, 0.322,
            hour(23).getMillis()));
        schedule2.sixHourData.put(hour(18), new AggregateNumericMetric(schedule2.id,
            avg(schedule2.oneHourData, hour(18), hour(23)),
            min(schedule2.oneHourData, hour(18), hour(23)),
            max(schedule2.oneHourData, hour(18), hour(23)),
            hour(18).getMillis()));
        schedule2.twentyFourHourData.put(hour(0), new AggregateNumericMetric(schedule2.id,
            avg(schedule2.sixHourData, hour(12), hour(18)),
            min(schedule2.sixHourData, hour(12), hour(18)),
            max(schedule2.sixHourData, hour(12), hour(18)),
            hour(0).getMillis()));
        schedule3.sixHourData.put(hour(18), new AggregateNumericMetric(schedule3.id, 2.42, 2.42, 2.42,
            hour(18).getMillis()));
        schedule3.twentyFourHourData.put(hour(0), new AggregateNumericMetric(schedule3.id,
            avg(schedule3.sixHourData, hour(12), hour(18)),
            min(schedule3.sixHourData, hour(12), hour(18)),
            max(schedule3.sixHourData, hour(12), hour(18)),
            hour(0).getMillis()));

        List<AggregateNumericMetric> expected = asList(schedule1.oneHourData.get(hour(23)),
            schedule2.oneHourData.get(hour(23)));

        assertCollectionEqualsNoOrder(expected, oneHourData, "The returned one hour data is wrong");
        // verify values in db
        assert1HourDataEquals(schedule1.id, schedule1.oneHourData.get(hour(23)), schedule1.oneHourData.get(hour(18)),
            schedule1.oneHourData.get(hour(17)), schedule1.oneHourData.get(hour(16)));
        assert1HourDataEquals(schedule2.id, schedule2.oneHourData.get(hour(23)), schedule2.oneHourData.get(hour(18)),
            schedule2.oneHourData.get(hour(17)), schedule2.oneHourData.get(hour(16)));
        assert1HourDataEquals(schedule3.id, schedule3.oneHourData.get(hour(18)), schedule3.oneHourData.get(hour(16)));
        assert6HourDataEquals(schedule1.id, schedule1.sixHourData.get(hour(12)), schedule1.sixHourData.get(hour(18)));
        assert6HourDataEquals(schedule2.id, schedule2.sixHourData.get(hour(12)), schedule2.sixHourData.get(hour(18)));
        assert6HourDataEquals(schedule3.id, schedule3.sixHourData.get(hour(12)), schedule3.sixHourData.get(hour(18)));
        assert24HourDataEquals(schedule1.id, schedule1.twentyFourHourData.get(hour(0)));
        assert24HourDataEquals(schedule2.id, schedule2.twentyFourHourData.get(hour(0)));
        assert24HourDataEquals(schedule3.id, schedule3.twentyFourHourData.get(hour(0)));
        assert1HourCacheEmpty(hour(23), startScheduleId(schedule1.id));
        assert6HourCacheEmpty(hour(18), startScheduleId(schedule1.id));
        assert24HourCacheEmpty(hour(0), startScheduleId(schedule1.id));
    }

    @Test(dependsOnMethods = "runAggregationForHour24")
    public void resetDBForFailureScenarios() throws Exception {
        purgeDB();
    }

    //@Test(dependsOnMethods = "resetDBForFailureScenarios")
//    public void failToFetchRawDataIndexDuringAggregationForHour12() throws Exception {
//        currentHour = hour(12);
//        AggregatorTestStub aggregator = new AggregatorTestStub(hour(11), new MetricsDAO(storageSession, configuration) {
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

    private WaitForWrite insertRawData(DateTime timeSlice, MeasurementDataNumeric... data) {
        WaitForWrite waitForRawInserts = new WaitForWrite(data.length);
        StorageResultSetFuture future;
        for (MeasurementDataNumeric raw : data) {
            future = dao.insertRawData(raw);
            Futures.addCallback(future, waitForRawInserts);
            future = dao.updateMetricsCache(MetricsTable.ONE_HOUR, timeSlice.getMillis(),
                startScheduleId(raw.getScheduleId()), raw.getScheduleId(), raw.getTimestamp(), ImmutableMap.of(
                    AggregateType.VALUE.ordinal(), raw.getValue()));
            Futures.addCallback(future, waitForRawInserts);
        }
        return waitForRawInserts;
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

    private WaitForWrite updateIndex(IndexUpdate... updates) {
        WaitForWrite waitForWrite = new WaitForWrite(updates.length);
        for (IndexUpdate update : updates) {
            StorageResultSetFuture future = dao.updateMetricsCache(update.table, update.timeSlice.getMillis(),
                startScheduleId(update.scheduleId), update.scheduleId, update.time.getMillis(), update.values);
            Futures.addCallback(future, waitForWrite);
        }
        return waitForWrite;
    }

    private int startScheduleId(int scheduleId) {
        return (scheduleId / BATCH_SIZE) * BATCH_SIZE;
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

    protected void assert6HourCacheEquals(DateTime timeSlice, int startScheduleId,
        List<AggregateNumericMetric> expected) {
        assertCacheEquals(MetricsTable.SIX_HOUR, timeSlice, startScheduleId, expected);
    }

    protected void assert24HourCacheEquals(DateTime timeSlice, int startScheduleId,
        List<AggregateNumericMetric> expected) {
        assertCacheEquals(MetricsTable.TWENTY_FOUR_HOUR, timeSlice, startScheduleId, expected);
    }

    protected void assertCacheEquals(MetricsTable table, DateTime timeSlice, int startScheduleId,
        List<AggregateNumericMetric> expected) {
        ResultSet resultSet = dao.findMetricsIndexEntriesAsync(table, timeSlice.getMillis(), startScheduleId).get();
        List<AggregateNumericMetric> actual = aggregateCacheMapper.map(resultSet);

        assertEquals(actual, expected, "The " + table + " cache is wrong");
    }

    protected void assert1HourCacheEmpty(DateTime timeSlice, int startScheduleId) {
        assertAggregateCacheEmpty(timeSlice, startScheduleId, MetricsTable.ONE_HOUR);
    }

    protected void assert6HourCacheEmpty(DateTime timeSlice, int startScheduleId) {
        assertAggregateCacheEmpty(timeSlice, startScheduleId, MetricsTable.SIX_HOUR);
    }

    protected void assert24HourCacheEmpty(DateTime timeSlice, int startScheduleId) {
        assertAggregateCacheEmpty(timeSlice, startScheduleId, MetricsTable.TWENTY_FOUR_HOUR);
    }

    protected void assertAggregateCacheEmpty(DateTime timeSlice, int startScheduleId, MetricsTable table) {
        ResultSet resultSet = dao.findMetricsIndexEntriesAsync(table, timeSlice.getMillis(), startScheduleId).get();
        List<AggregateNumericMetric> metrics = aggregateCacheMapper.map(resultSet);
        assertEquals(metrics.size(), 0, "Expected the " + table + " cache to be empty but found " + metrics);
    }

    protected void assert24HourIndexEquals(DateTime timeSlice, int... scheduleIds) {
        List<MetricsIndexEntry> indexEntries = new ArrayList<MetricsIndexEntry>(scheduleIds.length);
        for (int scheduleId : scheduleIds) {
            indexEntries.add(new MetricsIndexEntry(MetricsTable.TWENTY_FOUR_HOUR, timeSlice, scheduleId));
        }
        assertMetricsIndexEquals(MetricsTable.TWENTY_FOUR_HOUR, timeSlice.getMillis(), indexEntries,
            "The 24 hour index is wrong");
    }

    private class AggregatorTestStub extends Aggregator {

        public AggregatorTestStub(DateTime startTime) {
            super(aggregationTasks, dao, configuration, dateTimeService, startTime, BATCH_SIZE, 4, MIN_SCHEDULE_ID,
                MAX_SCHEDULE_ID, CACHE_BATCH_SIZE);
        }

        public AggregatorTestStub(DateTime startTime, MetricsDAO dao) {
            super(aggregationTasks, dao, configuration, dateTimeService, startTime, BATCH_SIZE, 4, MIN_SCHEDULE_ID,
                MAX_SCHEDULE_ID, CACHE_BATCH_SIZE);
        }

        @Override
        protected DateTime currentHour() {
            return currentHour;
        }
    }

    private class IndexUpdate {
        MetricsTable table;
        DateTime timeSlice;
        int scheduleId;
        DateTime time;
        Map<Integer, Double> values;

        public IndexUpdate(MetricsTable table, DateTime timeSlice, int scheduleId, DateTime time, Double value) {
            this.table = table;
            this.timeSlice = timeSlice;
            this.scheduleId = scheduleId;
            this.time = time;
            values = new TreeMap<Integer, Double>();
            values.put(AggregateType.VALUE.ordinal(), value);
        }
    }

    private class Aggregates {
        int id;  // schedule id
        Map<DateTime, AggregateNumericMetric> oneHourData = new HashMap<DateTime, AggregateNumericMetric>();
        Map<DateTime, AggregateNumericMetric> sixHourData = new HashMap<DateTime, AggregateNumericMetric>();
        Map<DateTime, AggregateNumericMetric> twentyFourHourData = new HashMap<DateTime, AggregateNumericMetric>();
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
