package org.rhq.server.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.TreeBasedTable;

import org.joda.time.DateTime;

import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.NumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetric;

/**
 * @author John Sanda
 */
public class InMemoryMetricsDB {

    private TreeBasedTable<Integer, DateTime, NumericMetric> rawData = TreeBasedTable.create();

    private TreeBasedTable<Integer, DateTime, AggregateNumericMetric> oneHourData = TreeBasedTable.create();

    private TreeBasedTable<Integer, DateTime, AggregateNumericMetric> sixHourData = TreeBasedTable.create();

    private TreeBasedTable<Integer, DateTime, AggregateNumericMetric> twentyFourHourData = TreeBasedTable.create();

    public void putRawData(RawNumericMetric raw) {
        rawData.put(raw.getScheduleId(), new DateTime(raw.getTimestamp()), raw);
    }

    public AggregateNumericMetric get1HourData(DateTime timeSlice, int scheduleId) {
        return oneHourData.get(scheduleId, timeSlice);
    }

    public List<AggregateNumericMetric> get1HourData(int scheduleId) {
        return ImmutableList.copyOf(oneHourData.row(scheduleId).values());
    }

    public List<AggregateNumericMetric> get1HourData(DateTime timeSlice) {
        Map<Integer, AggregateNumericMetric> data = oneHourData.column(timeSlice);
        return ImmutableList.copyOf(data.values());

    }

    public AggregateNumericMetric get6HourData(DateTime timeSlice, int scheduleId) {
        return sixHourData.get(scheduleId, timeSlice);
    }

    public List<AggregateNumericMetric> get6HourData(int... scheduleIds) {
        return getAllDataForBucket(sixHourData, scheduleIds);
    }

    public List<AggregateNumericMetric> get24HourData(int... scheduleIds) {
        return getAllDataForBucket(twentyFourHourData, scheduleIds);
    }

    private List<AggregateNumericMetric> getAllDataForBucket(
        TreeBasedTable<Integer, DateTime, AggregateNumericMetric> table, int... scheduleIds) {

        List<AggregateNumericMetric> metrics = new ArrayList<AggregateNumericMetric>(scheduleIds.length);
        for (Integer scheduleId : scheduleIds) {
            SortedMap<DateTime, AggregateNumericMetric> dataForSchedule = table.row(scheduleId);
            metrics.addAll(dataForSchedule.values());
        }
        return metrics;
    }

    public void aggregateRawData(DateTime startTime, DateTime endTime) {
        aggregateData(startTime, endTime, rawData, oneHourData);
    }

    public void aggregate1HourData(DateTime startTime, DateTime endTime) {
        aggregateData(startTime, endTime, oneHourData, sixHourData);
    }

    public void aggregate6HourData(DateTime startTime, DateTime endTime) {
        aggregateData(startTime, endTime, sixHourData, twentyFourHourData);
    }

    private void aggregateData(DateTime startTime, DateTime endTime,
        TreeBasedTable<Integer, DateTime, ? extends NumericMetric> from,
        TreeBasedTable<Integer, DateTime, AggregateNumericMetric> to) {

        for (Integer scheduleId : from.rowKeySet()) {
            SortedMap<DateTime, ? extends NumericMetric> row = from.row(scheduleId);
            Collection<? extends NumericMetric> dataForTimeSlice = row.subMap(startTime, endTime).values();
            if (!dataForTimeSlice.isEmpty()) {
                AggregateNumericMetric aggregate = computeAggregate(startTime, row.subMap(startTime, endTime).values());
                to.put(scheduleId, startTime, aggregate);
            }
        }
    }

    private AggregateNumericMetric computeAggregate(DateTime timestamp, Collection<? extends NumericMetric> values) {
        Double min = Double.NaN;
        Double max = Double.NaN;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
        int scheduleId = 0;

        for (NumericMetric metric : values) {
            mean.add(metric.getAvg());
            if (Double.isNaN(min)) {
                scheduleId = metric.getScheduleId();
                min = metric.getMin();
                max = metric.getMax();
            } else {
                if (metric.getMin() < min) {
                    min = metric.getMin();
                }
                if (metric.getMax() > max) {
                    max = metric.getMax();
                }
            }
        }
        return new AggregateNumericMetric(scheduleId, mean.getArithmeticMean(), min, max, timestamp.getMillis());
    }

}
