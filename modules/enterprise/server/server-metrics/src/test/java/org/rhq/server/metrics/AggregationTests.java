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

    private final int BATCH_SIZE = 10;

    private final int INDEX_PARTITION = 0;

    private MetricsServer metricsServer;

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
        dateTimeService.setConfiguration(new MetricsConfiguration());
        metricsServer = new MetricsServer();
        metricsServer.setConfiguration(new MetricsConfiguration());
        metricsServer.setDateTimeService(dateTimeService);
        metricsServer.setDAO(dao);
        metricsServer.setCacheBatchSize(PARTITION_SIZE);
        metricsServer.init();

        aggregationTasks = metricsServer.getAggregationWorkers();

        configuration.setIndexPageSize(5);
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
        dateTimeService.setNow(hour(17).plusMinutes(1));
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
        assertRawCacheIndexEmpty(hour(16));

        assert1HourCacheEquals(hour(12), startScheduleId(schedule1.id),
            asList(testdb.get1HourData(hour(16), schedule1.id), testdb.get1HourData(hour(16), schedule2.id)));

        assert1HourCacheIndexEquals(hour(12), asList(
            new1HourCacheIndexEntry(startScheduleId(schedule1.id), schedule1.id, schedule2.id),
            new1HourCacheIndexEntry(startScheduleId(schedule3.id), schedule3.id)
        ));

        assert6HourCacheEmpty(hour(0), startScheduleId(schedule1.id));
        assert6HourCacheIndexEmpty(hour(0));
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
        dateTimeService.setNow(hour(18).plusMinutes(1));
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
        assertRawCacheIndexEmpty(hour(17));

        assert1HourCacheEmpty(hour(12), startScheduleId(schedule1.id), startScheduleId(schedule3.id));
        assert1HourCacheIndexEmpty(hour(12));

        assert6HourCacheEquals(hour(0), startScheduleId(schedule1.id), testdb.get6HourData(schedule1.id, schedule2.id));
        assert6HourCacheEquals(hour(0), startScheduleId(schedule3.id), testdb.get6HourData(scheduleIds(schedule3.id)));
        assert6HourCacheIndexEquals(hour(0), asList(
            new6HourCacheIndexEntry(startScheduleId(schedule1.id), schedule1.id, schedule2.id),
            new6HourCacheIndexEntry(startScheduleId(schedule3.id), schedule3.id)
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
        dateTimeService.setNow(hour(19).plusMinutes(1));
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
        assertRawCacheIndexEmpty(hour(18));

        assert1HourCacheEmpty(hour(12), startScheduleId(schedule1.id));
        assert1HourCacheEmpty(hour(12), startScheduleId(schedule3.id));
        assert1HourCacheIndexEmpty(hour(12));

        assert1HourCacheEquals(hour(18), startScheduleId(schedule1.id), asList(
            testdb.get1HourData(hour(18), schedule1.id),
            testdb.get1HourData(hour(18), schedule2.id)));
        assert1HourCacheEquals(hour(18), startScheduleId(schedule3.id),
            asList(testdb.get1HourData(hour(18), schedule3.id)));
        assert1HourCacheIndexEquals(hour(18), asList(
            new1HourCacheIndexEntry(startScheduleId(schedule1.id), schedule1.id, schedule2.id),
            new1HourCacheIndexEntry(startScheduleId(schedule3.id), schedule3.id)
        ));
        assert6HourCacheEquals(hour(0), startScheduleId(schedule1.id), testdb.get6HourData(schedule1.id, schedule2.id));
        assert6HourCacheEquals(hour(0), startScheduleId(schedule3.id), testdb.get6HourData(schedule3.id));
        assert6HourCacheIndexEquals(hour(0), asList(
            new6HourCacheIndexEntry(startScheduleId(schedule1.id), schedule1.id, schedule2.id),
            new6HourCacheIndexEntry(startScheduleId(schedule3.id), schedule3.id)
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
        dateTimeService.setNow(hour(24).plusMinutes(1));
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
        assertRawCacheIndexEmpty(hour(18));

        assert1HourCacheEmpty(hour(18), startScheduleId(schedule1.id));
        assert1HourCacheEmpty(hour(18), startScheduleId(schedule3.id));
        assert1HourCacheIndexEmpty(hour(18));

        assert6HourCacheEmpty(hour(18), startScheduleId(schedule1.id));
        assert6HourCacheEmpty(hour(18), startScheduleId(schedule3.id));
        assert6HourCacheIndexEmpty(hour(18));
    }


    @Test(dependsOnMethods = "runAggregationForHour24")
    public void prepareForLateDataAggregationInSame6HourTimeSlice() throws Exception {
        purgeDB();
        testdb = new InMemoryMetricsDB();
        dateTimeService.setNow(hour(3).plusMinutes(55));

        insertRawData(
            newRawData(hour(3).plusMinutes(20), schedule1.id, 20),
            newRawData(hour(3).plusMinutes(30), schedule1.id, 22),
            newRawData(hour(3).plusMinutes(15), schedule2.id, 75),
            newRawData(hour(3).plusMinutes(20), schedule2.id, 100)
        );

        new AggregationManagerTestStub(hour(3)).run();

        // Insert data here after running aggregation to simulate the scenario in which we
        // have the full data set in the cache block for past data. This would happen when
        // aggregation of a cache block fails for example. In this situation we can
        // aggregate the data stored in the cache block instead of the raw_metrics table
        // even though the data will be considered old.
        insertRawData(
            newRawData(hour(3).plusMinutes(20), schedule3.id, 30),
            newRawData(hour(3).plusMinutes(40), schedule3.id, 40)
        );

        testdb.aggregateRawData(hour(3), hour(4));
    }

    @Test(dependsOnMethods = "prepareForLateDataAggregationInSame6HourTimeSlice")
    public void aggregateLateDataInSame6HourTimeSlice() throws Exception {
        dateTimeService.setNow(hour(4).plusMinutes(55));
        insertRawData(
            newRawData(hour(3).plusMinutes(35), schedule1.id, 20),
            newRawData(hour(3).plusMinutes(40), schedule1.id, 30),
            newRawData(hour(4).plusMinutes(40), schedule1.id, 27),
            newRawData(hour(4).plusMinutes(40), schedule2.id, 321),
            newRawData(hour(4).plusMinutes(45), schedule2.id, 333),
            newRawData(hour(4).plusMinutes(20), schedule3.id, 50),
            newRawData(hour(4).plusMinutes(40), schedule3.id, 60)
        );

        testdb.aggregateRawData(hour(3), hour(4));
        testdb.aggregateRawData(hour(4), hour(5));

        AggregationManagerTestStub aggregator = new AggregationManagerTestStub(hour(4));
        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        assertCollectionEqualsNoOrder(testdb.get1HourData(hour(4)), oneHourData, "The returned 1 hour data is wrong");
        // verify values in db
        assert1HourDataEquals(schedule1.id, testdb.get1HourData(schedule1.id));
        assert1HourDataEquals(schedule2.id, testdb.get1HourData(schedule2.id));
        assert1HourDataEquals(schedule3.id, testdb.get1HourData(schedule3.id));

        assertRawCacheEmpty(hour(4), startScheduleId(schedule1.id));
        assertRawCacheEmpty(hour(4), startScheduleId(schedule3.id));
        assertRawCacheIndexEmpty(hour(3));
        assertRawCacheIndexEmpty(hour(4));

        assert1HourCacheIndexEquals(hour(0), asList(
            new1HourCacheIndexEntry(startScheduleId(schedule1.id), schedule1.id, schedule2.id),
            new1HourCacheIndexEntry(startScheduleId(schedule3.id), schedule3.id)
        ));

        assert1HourCacheEquals(hour(0), startScheduleId(schedule1.id), asList(
            testdb.get1HourData(hour(3), schedule1.id),
            testdb.get1HourData(hour(4), schedule1.id),
            testdb.get1HourData(hour(3), schedule2.id),
            testdb.get1HourData(hour(4), schedule2.id)
        ));
        assert1HourCacheEquals(hour(0), startScheduleId(schedule3.id), asList(
            testdb.get1HourData(hour(3), schedule3.id),
            testdb.get1HourData(hour(4), schedule3.id)
        ));

        assert6HourCacheEmpty(hour(0), startScheduleId(schedule1.id));
        assert6HourCacheEmpty(hour(0), startScheduleId(schedule3.id));
        assert6HourCacheIndexEmpty(hour(0));
    }

    @Test(dependsOnMethods = "aggregateLateDataInSame6HourTimeSlice")
    public void aggregateLateDuringNext6HourTimeSlice() throws Exception {
        // First we need to run aggregation for the 05:00 hour in order to generate the
        // necessary 6 hour data
        dateTimeService.setNow(hour(6).plusMinutes(1));
        new AggregationManagerTestStub(hour(5)).run();

        // Next we insert late data
        dateTimeService.setNow(hour(6).plusMinutes(55));
        insertRawData(
            newRawData(hour(5).plusMinutes(10), schedule1.id, 5),
            newRawData(hour(5).plusMinutes(15), schedule1.id, 125),
            newRawData(hour(5).plusMinutes(20), schedule2.id, 22),
            newRawData(hour(5).plusMinutes(40), schedule2.id, 18)
        );
        testdb.aggregateRawData(hour(5), hour(6));

        // now insert data for the time slice to be aggregated
        insertRawData(
            newRawData(hour(6).plusMinutes(25), schedule1.id, 35),
            newRawData(hour(6).plusMinutes(50), schedule1.id, 40),
            newRawData(hour(6).plusMinutes(15), schedule2.id, 27),
            newRawData(hour(6).plusMinutes(50), schedule2.id, 23),
            newRawData(hour(6).plusMinutes(20), schedule3.id, 86.453),
            newRawData(hour(6).plusMinutes(40), schedule3.id, 84.77)
        );

        testdb.aggregateRawData(hour(6), hour(7));
        testdb.aggregate1HourData(hour(0), hour(6));

       AggregationManagerTestStub aggregator = new AggregationManagerTestStub(hour(6));
        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        assertCollectionEqualsNoOrder(oneHourData, testdb.get1HourData(hour(6)), "The returned 1 hour data is wrong");
        // verify values in db
        assertRawCacheEmpty(hour(5), startScheduleId(schedule1.id));
        assertRawCacheIndexEmpty(hour(5));
        assertRawCacheEmpty(hour(6), startScheduleId(schedule1.id));
        assertRawCacheIndexEmpty(hour(6));

        assert1HourDataEquals(schedule1.id, testdb.get1HourData(schedule1.id));
        assert1HourDataEquals(schedule2.id, testdb.get1HourData(schedule2.id));
        assert1HourDataEquals(schedule3.id, testdb.get1HourData(schedule3.id));

        assert6HourDataEquals(schedule1.id, testdb.get6HourData(schedule1.id));
        assert6HourDataEquals(schedule2.id, testdb.get6HourData(schedule2.id));
        assert6HourDataEquals(schedule3.id, testdb.get6HourData(schedule3.id));

        // Note that while we aggregated old data from the 00:00 - 06:00 6 hour time slice, we expect the cache
        // and cache index to be empty for that time its 6 hour time slice has already passed.
        assert1HourCacheIndexEmpty(hour(0));
        assert1HourCacheEmpty(hour(0), startScheduleId(schedule1.id), startScheduleId(schedule3.id));

        assert1HourCacheIndexEquals(hour(6), asList(
            new1HourCacheIndexEntry(hour(6), startScheduleId(schedule1.id), schedule1.id, schedule2.id),
            new1HourCacheIndexEntry(hour(6), startScheduleId(schedule3.id), schedule3.id)
        ));

        assert1HourCacheEquals(hour(6), startScheduleId(schedule1.id), asList(
            testdb.get1HourData(hour(6), schedule1.id),
            testdb.get1HourData(hour(6), schedule2.id)
        ));
        assert1HourCacheEquals(hour(6), startScheduleId(schedule3.id),
            asList(testdb.get1HourData(hour(6), schedule3.id)));

        assert6HourCacheIndexEquals(hour(0), asList(
            new6HourCacheIndexEntry(hour(0), startScheduleId(schedule1.id), schedule1.id, schedule2.id),
            new6HourCacheIndexEntry(hour(0), startScheduleId(schedule3.id), schedule3.id)
        ));

        assert6HourCacheEquals(hour(0), startScheduleId(schedule1.id), asList(
            testdb.get6HourData(hour(0), schedule1.id),
            testdb.get6HourData(hour(0), schedule2.id)
        ));

        assert6HourCacheIndexEmpty(hour(6));
    }

    @Test(dependsOnMethods = "aggregateLateDuringNext6HourTimeSlice")
    public void aggregateLateDataDuringNext24HourTimeSlice() throws Exception {
        // Run aggregation to clear out cache entries from the 06:00 - 12:00 time slice
        dateTimeService.setNow(hour(12).plusMinutes(1));
        new AggregationManagerTestStub(hour(11)).run();

        testdb.aggregate1HourData(hour(6), hour(12));

        // Run aggregation to clear out cache entries from the 00:00 - 24:00 time slice
        dateTimeService.setNow(tomorrow().plusMinutes(1));
        new AggregationManagerTestStub(hour(23)).run();

        dateTimeService.setNow(tomorrow().plusHours(1).plusMinutes(50));

        insertRawData(
            newRawData(hour(23).plusMinutes(20), schedule1.id, 750),
            newRawData(hour(23).plusMinutes(40), schedule1.id, 230),
            newRawData(hour(23).plusMinutes(20), schedule3.id, 15),
            newRawData(hour(23).plusMinutes(25), schedule3.id, 325),
            newRawData(tomorrow().plusHours(1).plusMinutes(15), schedule1.id, 200),
            newRawData(tomorrow().plusHours(1).plusMinutes(10), schedule2.id, 150),
            newRawData(tomorrow().plusHours(1).plusMinutes(35), schedule3.id, 475)
        );

        testdb.aggregateRawData(hour(23), tomorrow());
        testdb.aggregate1HourData(hour(18), tomorrow());
        testdb.aggregate6HourData(today(), tomorrow());
        testdb.aggregateRawData(tomorrow().plusHours(1), tomorrow().plusHours(2));

        AggregationManagerTestStub aggregator = new AggregationManagerTestStub(tomorrow().plusHours(1));
        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        assertCollectionEqualsNoOrder(oneHourData, testdb.get1HourData(tomorrow().plusHours(1)),
            "The returned 1 hour data is wrong");
        // verify values in the db
        assertRawCacheIndexEmpty(hour(23));
        assertRawCacheEmpty(hour(23), startScheduleId(schedule1.id), startScheduleId(schedule3.id));

        assertRawCacheIndexEmpty(tomorrow().plusHours(1));
        assertRawCacheEmpty(tomorrow().plusHours(1), startScheduleId(schedule1.id), startScheduleId(schedule3.id));

        assert1HourDataEquals(schedule1.id, testdb.get1HourData(schedule1.id));
        assert1HourDataEquals(schedule2.id, testdb.get1HourData(schedule2.id));
        assert1HourDataEquals(schedule3.id, testdb.get1HourData(schedule3.id));

        assert1HourCacheIndexEquals(tomorrow(), asList(
            new1HourCacheIndexEntry(startScheduleId(schedule1.id), schedule1.id, schedule2.id),
            new1HourCacheIndexEntry(startScheduleId(schedule3.id), schedule3.id)
        ));

        assert1HourCacheEquals(tomorrow(), startScheduleId(schedule1.id), asList(
            testdb.get1HourData(tomorrow().plusHours(1), schedule1.id),
            testdb.get1HourData(tomorrow().plusHours(1), schedule2.id)
        ));

        assert6HourCacheIndexEmpty(hour(18));

        assert6HourCacheEmpty(hour(18), startScheduleId(schedule1.id));
        assert6HourCacheEmpty(hour(18), startScheduleId(schedule3.id));

        assert24HourDataEquals(schedule1.id, testdb.get24HourData(schedule1.id));
    }

    // The aggregateLateDataFromCacheXXX tests cover scenarios in which aggregation for one
    // or more cache partitions fails, and we redo the aggregation in a subsequent run. It
    // can be assumed that all the data for the time slice is present in metrics_cache since
    // we initially tried to aggregate the data from that table; therefore, we can pull
    // data from metrics_cache when we redo the failed aggregation(s).

    @Test(dependsOnMethods = "aggregateLateDataDuringNext24HourTimeSlice")
    public void aggregateLateDataFromCacheInSame6HourTimeSlice() throws Exception {
        purgeDB();
        testdb = new InMemoryMetricsDB();
        dateTimeService.setNow(hour(2).plusMinutes(55));

        insertRawData(
            newRawData(hour(2).plusMinutes(15), schedule1.id, 22)
        );

        dateTimeService.setNow(hour(3).plusMinutes(55));
        new AggregationManagerTestStub(hour(2)).run();

        insertRawData(
            newRawData(hour(3).plusMinutes(20), schedule1.id, 30),
            newRawData(hour(3).plusMinutes(30), schedule1.id, 80),
            newRawData(hour(3).plusMinutes(15), schedule2.id, 75),
            newRawData(hour(3).plusMinutes(20), schedule2.id, 100)
        );

        dateTimeService.setNow(hour(4).plusMinutes(55));

        insertRawData(
            newRawData(hour(2).plusMinutes(56), schedule1.id, 11),
            newRawData(hour(3).plusMinutes(45), schedule1.id, 95),
            newRawData(hour(4).plusMinutes(15), schedule1.id, 20),
            newRawData(hour(4).plusMinutes(30), schedule1.id, 50),
            newRawData(hour(4).plusMinutes(15), schedule2.id, 50),
            newRawData(hour(4).plusMinutes(15), schedule3.id, 50),
            newRawData(hour(4).plusMinutes(30), schedule3.id, 60)
        );

        testdb.aggregateRawData(hour(2), hour(3));
        testdb.aggregateRawData(hour(3), hour(4));
        dateTimeService.setNow(hour(5).plusSeconds(5));
        testdb.aggregateRawData(hour(4), hour(5));

        AggregationManagerTestStub aggregator = new AggregationManagerTestStub(hour(4));
        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        assertCollectionEqualsNoOrder(testdb.get1HourData(hour(4)), oneHourData, "The returned 1 hour data is wrong");

        assertRawCacheEmpty(hour(2), startScheduleId(schedule1.id));
        assertRawCacheEmpty(hour(3), startScheduleId(schedule1.id), startScheduleId(schedule3.id));
        assertRawCacheEmpty(hour(4), startScheduleId(schedule1.id), startScheduleId(schedule3.id));

        assertRawCacheIndexEmpty(hour(2));
        assertRawCacheIndexEmpty(hour(3));
        assertRawCacheIndexEmpty(hour(4));

        assert1HourDataEquals(schedule1.id, testdb.get1HourData(schedule1.id));
        assert1HourDataEquals(schedule2.id, testdb.get1HourData(schedule2.id));
        assert1HourDataEquals(schedule3.id, testdb.get1HourData(schedule3.id));

        assert1HourCacheIndexEquals(today(), asList(
            new1HourCacheIndexEntry(startScheduleId(schedule1.id), schedule1.id, schedule2.id),
            new1HourCacheIndexEntry(startScheduleId(schedule3.id), schedule3.id)
        ));

        assert1HourCacheEquals(hour(0), startScheduleId(schedule1.id),  testdb.get1HourData(schedule1.id, schedule2.id));
        assert1HourCacheEquals(hour(0), startScheduleId(schedule3.id), testdb.get1HourData(schedule3.id));

        assert6HourCacheIndexEmpty(today());
        assert6HourCacheEmpty(today(), startScheduleId(schedule1.id));
        assert6HourCacheEmpty(today(), startScheduleId(schedule3.id));
    }

    @Test(dependsOnMethods = "aggregateLateDataFromCacheInSame6HourTimeSlice")
    public void aggregateLateDataFromCacheInNext6HourTimeSlice() throws Exception {
        dateTimeService.setNow(hour(6).plusSeconds(1));
        new AggregationManagerTestStub(hour(5)).run();

        // We have to reset "now" in order for the following late data to get pulled from
        // metrics_cache during aggregation.
        dateTimeService.setNow(hour(5).plusMinutes(45));
        insertRawData(
            newRawData(hour(5).plusMinutes(15), schedule1.id, 15),
            newRawData(hour(5).plusMinutes(25), schedule1.id, 150),
            newRawData(hour(5).plusMinutes(10), schedule3.id, 20),
            newRawData(hour(5).plusMinutes(20), schedule3.id, 80)
        );

        testdb.aggregateRawData(hour(5), hour(6));
        testdb.aggregate1HourData(hour(0), hour(6));

        dateTimeService.setNow(hour(6).plusMinutes(50));

        insertRawData(
            newRawData(hour(6).plusMinutes(15), schedule1.id, 200),
            newRawData(hour(6).plusMinutes(25), schedule2.id, 225),
            newRawData(hour(6).plusMinutes(40), schedule3.id, 100)
        );

        dateTimeService.setNow(hour(7).plusSeconds(1));

        testdb.aggregateRawData(hour(6), hour(7));

        AggregationManagerTestStub aggregator = new AggregationManagerTestStub(hour(6));
        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        assertCollectionEqualsNoOrder(testdb.get1HourData(hour(6)), oneHourData, "The returned 1 hour data is wrong");

        assertRawCacheIndexEmpty(hour(5));
        assertRawCacheEmpty(hour(5), startScheduleId(schedule1.id), startScheduleId(schedule3.id));

        assertRawCacheIndexEmpty(hour(6));
        assertRawCacheEmpty(hour(6), startScheduleId(schedule1.id), schedule1.id, schedule2.id);
        assertRawCacheEmpty(hour(6), startScheduleId(schedule3.id), schedule3.id);

        assert1HourDataEquals(schedule1.id, testdb.get1HourData(schedule1.id));
        assert1HourDataEquals(schedule2.id, testdb.get1HourData(schedule2.id));
        assert1HourDataEquals(schedule3.id, testdb.get1HourData(schedule3.id));

        assert1HourCacheIndexEmpty(hour(0));
        assert1HourCacheEmpty(hour(0), startScheduleId(schedule1.id), startScheduleId(schedule3.id));

        assert6HourCacheIndexEquals(hour(0), asList(
            new6HourCacheIndexEntry(startScheduleId(schedule1.id), schedule1.id, schedule2.id),
            new6HourCacheIndexEntry(startScheduleId(schedule3.id), schedule3.id)
        ));

        assert6HourCacheEquals(today(), startScheduleId(schedule1.id), asList(
            testdb.get6HourData(hour(0), schedule1.id),
            testdb.get6HourData(hour(0), schedule2.id)
        ));

        assert6HourCacheEquals(today(), startScheduleId(schedule3.id), asList(
            testdb.get6HourData(hour(0), schedule3.id)
        ));
    }

    @Test(dependsOnMethods = "aggregateLateDataFromCacheInNext6HourTimeSlice")
    public void aggregateLateDataFromCacheInNext24HourTimeSlice() throws Exception {
        dateTimeService.setNow(hour(12).plusSeconds(1));
        new AggregationManagerTestStub(hour(11)).run();
        testdb.aggregate1HourData(hour(6), hour(12));
        testdb.aggregate1HourData(hour(12), hour(18));

        dateTimeService.setNow(hour(23).plusMinutes(55));

        insertRawData(
            newRawData(hour(23).plusMinutes(10), schedule1.id, 450),
            newRawData(hour(23).plusMinutes(10), schedule2.id, 400),
            newRawData(hour(23).plusMinutes(10), schedule3.id, 25),
            newRawData(hour(23).plusMinutes(40), schedule2.id, 10),
            newRawData(hour(23).plusMinutes(40), schedule3.id, 15),
            newRawData(hour(23).plusMinutes(45), schedule2.id, 500),
            newRawData(hour(23).plusMinutes(44), schedule3.id, 525)
        );

        testdb.aggregateRawData(hour(23), hour(24));
        testdb.aggregate1HourData(hour(18), hour(24));
        testdb.aggregate6HourData(hour(0), hour(24));

        dateTimeService.setNow(tomorrow().plusMinutes(50));

        insertRawData(
            newRawData(tomorrow().plusMinutes(10), schedule1.id, 420),
            newRawData(tomorrow().plusMinutes(15), schedule1.id, 390),
            newRawData(tomorrow().plusMinutes(15), schedule2.id, 490),
            newRawData(tomorrow().plusMinutes(15), schedule3.id, 500)
        );

        dateTimeService.setNow(tomorrow().plusHours(1).plusSeconds(1));

        AggregationManagerTestStub aggregator = new AggregationManagerTestStub(tomorrow());
        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        testdb.aggregateRawData(tomorrow(), tomorrow().plusHours(1));

        assertCollectionEqualsNoOrder(testdb.get1HourData(tomorrow()), oneHourData, "The returned 1 hour data is wrong");

        assertRawCacheIndexEmpty(hour(23));
        assertRawCacheIndexEmpty(tomorrow());

        assertRawCacheEmpty(hour(23), startScheduleId(schedule1.id), startScheduleId(schedule3.id));
        assertRawCacheEmpty(tomorrow(), startScheduleId(schedule1.id), startScheduleId(schedule3.id));

        assert1HourDataEquals(schedule1.id, testdb.get1HourData(schedule1.id));
        assert1HourDataEquals(schedule2.id, testdb.get1HourData(schedule2.id));
        assert1HourDataEquals(schedule3.id, testdb.get1HourData(schedule3.id));

        assert1HourCacheIndexEquals(tomorrow(), asList(
            new1HourCacheIndexEntry(startScheduleId(schedule1.id), schedule1.id, schedule2.id),
            new1HourCacheIndexEntry(startScheduleId(schedule3.id), schedule3.id)
        ));

        assert1HourCacheEquals(tomorrow(), startScheduleId(schedule1.id), asList(
            testdb.get1HourData(tomorrow(), schedule1.id),
            testdb.get1HourData(tomorrow(), schedule2.id)
        ));
        assert1HourCacheEquals(tomorrow(), startScheduleId(schedule3.id), asList(
            testdb.get1HourData(tomorrow(), schedule3.id)
        ));

        assert6HourCacheIndexEmpty(hour(18));

        assert6HourCacheEmpty(hour(18), startScheduleId(schedule1.id));
        assert6HourCacheEmpty(hour(18), startScheduleId(schedule3.id));

        assert24HourDataEquals(schedule1.id, testdb.get24HourData(schedule1.id));
        assert24HourDataEquals(schedule2.id, testdb.get24HourData(schedule2.id));
        assert24HourDataEquals(schedule3.id, testdb.get24HourData(schedule3.id));
    }

    @Test(dependsOnMethods = "aggregateLateDataFromCacheInNext24HourTimeSlice")
    public void runAggregationForHour16WhenCacheIsInactive() throws Exception {
        purgeDB();
        testdb = new InMemoryMetricsDB();

        dateTimeService.setNow(hour(16).plusMinutes(55));

        insertRawData(
            newRawData(hour(16).plusMinutes(10), schedule1.id, 10),
            newRawData(hour(16).plusMinutes(20), schedule1.id, 25),
            newRawData(hour(16).plusMinutes(20), schedule2.id, 44),
            newRawData(hour(16).plusMinutes(20), schedule3.id, 50),
            newRawData(hour(16).plusMinutes(40), schedule3.id, 35)
        );

        testdb.aggregateRawData(hour(16), hour(17));

        dateTimeService.setNow(hour(17));

        AggregationManagerTestStub aggregator = new AggregationManagerTestStub(hour(16));
        aggregator.setCacheActivationTime(tomorrow().getMillis());
        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        assertCollectionEqualsNoOrder(testdb.get1HourData(hour(16)), oneHourData, "The returned 1 hour data is wrong");

        assertRawCacheEmpty(hour(16));
        assertRawCacheIndexEmpty(hour(16));

        assert1HourDataEquals(schedule1.id, testdb.get1HourData(schedule1.id));
        assert1HourDataEquals(schedule2.id, testdb.get1HourData(schedule2.id));
        assert1HourDataEquals(schedule3.id, testdb.get1HourData(schedule3.id));

        assert1HourCacheIndexEquals(hour(12), asList(
            new1HourCacheIndexEntry(startScheduleId(schedule1.id), schedule1.id, schedule2.id),
            new1HourCacheIndexEntry(startScheduleId(schedule3.id), schedule3.id)
        ));

        assert1HourCacheEquals(hour(12), startScheduleId(schedule1.id), testdb.get1HourData(schedule1.id, schedule2.id));
        assert1HourCacheEquals(hour(12), startScheduleId(schedule3.id), testdb.get1HourData(schedule3.id));

        assert6HourCacheIndexEmpty(today());
        assert6HourCacheEmpty(today(), startScheduleId(schedule1.id));
        assert6HourCacheEmpty(today(), startScheduleId(schedule3.id));
    }

    @Test(dependsOnMethods = "runAggregationForHour16WhenCacheIsInactive")
    public void runAggregationForHour17WhenCacheIsInactive() throws Exception {
        dateTimeService.setNow(hour(17).plusMinutes(55));

        insertRawData(
            // insert some late data too to get coverage in PastDataAggregator for when
            // the cache is inactive
            newRawData(hour(16).plusMinutes(40), schedule1.id, 33),
            newRawData(hour(17).plusMinutes(20), schedule1.id, 20),
            newRawData(hour(17).plusMinutes(40), schedule1.id, 28),
            newRawData(hour(17).plusMinutes(20), schedule2.id, 55),
            newRawData(hour(17).plusMinutes(20), schedule3.id, 60),
            newRawData(hour(17).plusMinutes(40), schedule3.id, 14)
        );

        testdb.aggregateRawData(hour(16), hour(17));
        testdb.aggregateRawData(hour(17), hour(18));
        testdb.aggregate1HourData(hour(12), hour(18));

        dateTimeService.setNow(hour(18));

        AggregationManagerTestStub aggregator = new AggregationManagerTestStub(hour(17));
        aggregator.setCacheActivationTime(tomorrow().getMillis());
        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        assertCollectionEqualsNoOrder(testdb.get1HourData(hour(17)), oneHourData, "The returned 1 hour data is wrong");

        assertRawCacheEmpty(hour(16));
        assertRawCacheEmpty(hour(17));

        assertRawCacheIndexEmpty(hour(16));
        assertRawCacheIndexEmpty(hour(17));

        assert1HourDataEquals(schedule1.id, testdb.get1HourData(schedule1.id));
        assert1HourDataEquals(schedule2.id, testdb.get1HourData(schedule2.id));
        assert1HourDataEquals(schedule3.id, testdb.get1HourData(schedule3.id));

        assert1HourCacheEmpty(hour(12), schedule1.id, schedule3.id);
        assert1HourCacheIndexEmpty(hour(12));

        assert6HourCacheEquals(today(), startScheduleId(schedule1.id), testdb.get6HourData(schedule1.id, schedule2.id));
        assert6HourCacheEquals(today(), startScheduleId(schedule3.id), testdb.get6HourData(schedule3.id));

        assert6HourCacheIndexEquals(today(), asList(
            new6HourCacheIndexEntry(startScheduleId(schedule1.id), schedule1.id, schedule2.id),
            new6HourCacheIndexEntry(startScheduleId(schedule3.id), schedule3.id)
        ));
    }

    @Test(dependsOnMethods = "runAggregationForHour17WhenCacheIsInactive")
    public void runAggregationForHour23WhenCacheIsInactive() throws Exception {
        dateTimeService.setNow(hour(23).plusMinutes(55));

        insertRawData(
            newRawData(hour(23).plusMinutes(10), schedule1.id, 75),
            newRawData(hour(23).plusMinutes(15), schedule1.id, 80),
            newRawData(hour(23).plusMinutes(10), schedule2.id, 101),
            newRawData(hour(23).plusMinutes(15), schedule2.id, 90),
            newRawData(hour(23).plusMinutes(10), schedule3.id, 110),
            newRawData(hour(23).plusMinutes(15), schedule3.id, 120)
        );

        testdb.aggregateRawData(hour(23), tomorrow());
        testdb.aggregate1HourData(hour(18), tomorrow());
        testdb.aggregate6HourData(today(), tomorrow());

        dateTimeService.setNow(tomorrow());

        AggregationManagerTestStub aggregator = new AggregationManagerTestStub(hour(23));
        aggregator.setCacheActivationTime(tomorrow().getMillis());
        Set<AggregateNumericMetric> oneHourData = aggregator.run();

        assertCollectionEqualsNoOrder(testdb.get1HourData(hour(23)), oneHourData, "The returned 1 hour data is wrong");

        assertRawCacheEmpty(hour(23));
        assertRawCacheIndexEmpty(hour(23));

        assert1HourDataEquals(schedule1.id, testdb.get1HourData(schedule1.id));
        assert1HourDataEquals(schedule2.id, testdb.get1HourData(schedule2.id));
        assert1HourDataEquals(schedule3.id, testdb.get1HourData(schedule3.id));

        assert6HourDataEquals(schedule1.id, testdb.get6HourData(schedule1.id));
        assert6HourDataEquals(schedule2.id, testdb.get6HourData(schedule2.id));
        assert6HourDataEquals(schedule3.id, testdb.get6HourData(schedule3.id));

        assert24HourDataEquals(schedule1.id, testdb.get24HourData(schedule1.id));
        assert24HourDataEquals(schedule2.id, testdb.get24HourData(schedule2.id));
        assert24HourDataEquals(schedule3.id, testdb.get24HourData(schedule3.id));

        assert1HourCacheIndexEmpty(hour(18));
        assert1HourCacheEmpty(hour(18), startScheduleId(schedule1.id), startScheduleId(schedule3.id));

        assert6HourCacheIndexEmpty(today());
        assert6HourCacheIndexEmpty(today());
    }

//    @Test(dependsOnMethods = "runAggregationForHour24")
    public void resetDBForFailureScenarios() throws Exception {
        purgeDB();
    }

//    @Test(dependsOnMethods = "resetDBForFailureScenarios")
    public void doNotDeleteCachePartitionOnBatchFailure() throws Exception {
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

    private class AggregationManagerTestStub extends AggregationManager {

        public AggregationManagerTestStub(DateTime startTime) {
            super(aggregationTasks, dao, dateTimeService, startTime, BATCH_SIZE, 4, PARTITION_SIZE,
                configuration.getIndexPageSize());
        }

        public AggregationManagerTestStub(DateTime startTime, MetricsDAO dao) {
            super(aggregationTasks, dao, dateTimeService, startTime, BATCH_SIZE, 4, PARTITION_SIZE,
                configuration.getIndexPageSize());
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
