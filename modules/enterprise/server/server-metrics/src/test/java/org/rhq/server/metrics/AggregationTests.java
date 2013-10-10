package org.rhq.server.metrics;

import static java.util.Arrays.asList;
import static org.rhq.test.AssertUtils.assertCollectionEqualsNoOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.MetricsIndexEntry;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * @author John Sanda
 */
public class AggregationTests extends MetricsTest {

    private Aggregates schedule1 = new Aggregates();
    private Aggregates schedule2 = new Aggregates();
    private Aggregates schedule3 = new Aggregates();

    private ListeningExecutorService workers;

    private DateTime currentHour;

    @BeforeClass
    public void setUp() throws Exception {
        workers = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        purgeDB();
        schedule1.id = 100;
        schedule2.id = 101;
        schedule3.id = 102;
    }

    @Test
    public void insertRawDataDuringHour16() throws Exception {
        insertRawData(
            new MeasurementDataNumeric(hour(16).plusMinutes(20).getMillis(), schedule1.id, 3.0),
            new MeasurementDataNumeric(hour(16).plusMinutes(40).getMillis(), schedule1.id, 5.0),
            new MeasurementDataNumeric(hour(16).plusMinutes(15).getMillis(), schedule2.id, 0.0032),
            new MeasurementDataNumeric(hour(16).plusMinutes(30).getMillis(), schedule2.id, 0.104),
            new MeasurementDataNumeric(hour(16).plusMinutes(7).getMillis(), schedule3.id, 3.14)
        ).await("Failed to insert raw data");

        updateIndex(
            new IndexUpdate(MetricsTable.ONE_HOUR, schedule1.id, hour(16)),
            new IndexUpdate(MetricsTable.ONE_HOUR, schedule2.id, hour(16)),
            new IndexUpdate(MetricsTable.ONE_HOUR, schedule3.id, hour(16))
        ).await("Failed to update raw data index");
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
        assert6HourIndexEquals(hour(12), schedule1.id, schedule2.id, schedule3.id);
        assert6HourDataEmpty(schedule1.id);
        assert6HourDataEmpty(schedule2.id);
        assert6HourDataEmpty(schedule3.id);
        assert24HourMetricsIndexEmpty(hour(0));
        assert24HourMetricsIndexEmpty(hour(0));
        assert1HourMetricsIndexEmpty(hour(16));
    }

    @Test(dependsOnMethods = "runAggregationForHour16")
    public void insertRawDataDuringHour17() throws Exception {
        insertRawData(
            new MeasurementDataNumeric(hour(17).plusMinutes(20).getMillis(), schedule1.id, 11.0),
            new MeasurementDataNumeric(hour(17).plusMinutes(40).getMillis(), schedule1.id, 16.0),
            new MeasurementDataNumeric(hour(17).plusMinutes(30).getMillis(), schedule2.id, 0.092),
            new MeasurementDataNumeric(hour(17).plusMinutes(45).getMillis(), schedule2.id, 0.0733)
        ).await("Failed to insert raw data");

        updateIndex(
            new IndexUpdate(MetricsTable.ONE_HOUR, schedule1.id, hour(17)),
            new IndexUpdate(MetricsTable.ONE_HOUR, schedule2.id, hour(17))
        ).await("Failed to update raw data index");
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
        assert6HourMetricsIndexEmpty(hour(12));
        assert6HourDataEquals(schedule1.id, schedule1.sixHourData.get(hour(12)));
        assert6HourDataEquals(schedule2.id, schedule2.sixHourData.get(hour(12)));
        assert6HourDataEquals(schedule3.id, schedule3.sixHourData.get(hour(12)));
        assert24HourDataEmpty(schedule1.id);
        assert24HourDataEmpty(schedule2.id);
        assert24HourDataEmpty(schedule3.id);
        assert24HourIndexEquals(hour(0), schedule1.id, schedule2.id, schedule3.id);
        assert1HourMetricsIndexEmpty(hour(17));
    }

    @Test(dependsOnMethods = "runAggregationForHour17")
    public void insertRawDataDuringHour18() throws Exception {
        insertRawData(
            new MeasurementDataNumeric(hour(18).plusMinutes(20).getMillis(), schedule1.id, 22.0),
            new MeasurementDataNumeric(hour(18).plusMinutes(40).getMillis(), schedule1.id, 26.0),
            new MeasurementDataNumeric(hour(18).plusMinutes(15).getMillis(), schedule2.id, 0.205),
            new MeasurementDataNumeric(hour(18).plusMinutes(15).getMillis(), schedule3.id, 2.42)
        ).await("Failed to insert raw data");

        updateIndex(
            new IndexUpdate(MetricsTable.ONE_HOUR, schedule1.id, hour(18)),
            new IndexUpdate(MetricsTable.ONE_HOUR, schedule2.id, hour(18)),
            new IndexUpdate(MetricsTable.ONE_HOUR, schedule3.id, hour(18))
        ).await("Failed to update raw data index");
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
        assert6HourIndexEquals(hour(18), schedule1.id, schedule2.id, schedule3.id);
        assert24HourDataEmpty(schedule1.id);
        assert24HourDataEmpty(schedule2.id);
        assert24HourDataEmpty(schedule3.id);
        assert24HourIndexEquals(hour(0), schedule1.id, schedule2.id, schedule3.id);
        assert1HourMetricsIndexEmpty(hour(18));
    }

    @Test(dependsOnMethods = "runAggregationForHour18")
    public void insertRawDataDuringHour23() throws Exception {
        insertRawData(
            new MeasurementDataNumeric(hour(23).plusMinutes(25).getMillis(), schedule1.id, 34.0),
            new MeasurementDataNumeric(hour(23).plusMinutes(30).getMillis(), schedule2.id, 0.322)
        ).await("Failed to insert raw data");

        updateIndex(
            new IndexUpdate(MetricsTable.ONE_HOUR, schedule1.id, hour(23)),
            new IndexUpdate(MetricsTable.ONE_HOUR, schedule2.id, hour(23))
        ).await("Failed to update raw data index");
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
        assert1HourMetricsIndexEmpty(hour(23));
        assert6HourMetricsIndexEmpty(hour(18));
        assert24HourMetricsIndexEmpty(hour(0));
    }

    private WaitForWrite insertRawData(MeasurementDataNumeric... data) {
        WaitForWrite waitForRawInserts = new WaitForWrite(data.length);
        for (MeasurementDataNumeric raw : data) {
            StorageResultSetFuture resultSetFuture = dao.insertRawData(raw);
            Futures.addCallback(resultSetFuture, waitForRawInserts);
        }
        return waitForRawInserts;
    }

    private WaitForWrite updateIndex(IndexUpdate... updates) {
        WaitForWrite waitForWrite = new WaitForWrite(updates.length);
        for (IndexUpdate update : updates) {
            StorageResultSetFuture future = dao.updateMetricsIndex(update.table, update.scheduleId,
                update.time.getMillis());
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

    protected void assert6HourIndexEquals(DateTime timeSlice, int... scheduleIds) {
        List<MetricsIndexEntry> indexEntries = new ArrayList<MetricsIndexEntry>(scheduleIds.length);
        for (int scheduleId : scheduleIds) {
            indexEntries.add(new MetricsIndexEntry(MetricsTable.SIX_HOUR, timeSlice, scheduleId));
        }
        assertMetricsIndexEquals(MetricsTable.SIX_HOUR, timeSlice.getMillis(), indexEntries,
            "The 6 hour index is wrong");
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
            super(workers, dao, configuration, dateTimeService, startTime);
        }

        @Override
        protected DateTime currentHour() {
            return currentHour;
        }
    }

    private class IndexUpdate {
        MetricsTable table;
        int scheduleId;
        DateTime time;

        public IndexUpdate(MetricsTable table, int scheduleId, DateTime time) {
            this.table = table;
            this.scheduleId = scheduleId;
            this.time = time;
        }
    }

    private class Aggregates {
        int id;  // schedule id
        Map<DateTime, AggregateNumericMetric> oneHourData = new HashMap<DateTime, AggregateNumericMetric>();
        Map<DateTime, AggregateNumericMetric> sixHourData = new HashMap<DateTime, AggregateNumericMetric>();
        Map<DateTime, AggregateNumericMetric> twentyFourHourData = new HashMap<DateTime, AggregateNumericMetric>();
    }

}
