package org.rhq.metrics.simulator;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * @author John Sanda
 */
public class Metrics {

    public final MetricRegistry registry;

    public final Meter rawInserts;

    public final Timer batchInsertTime;

    public final Timer totalAggregationTime;

    public final Timer twentyFourHourResourceQueryTime;

    public final Timer oneWeekResourceQueryTime;

    public final Timer twoWeekResourceQueryTime;

    public final Timer monthResourceQueryTime;

    public final Timer yearResourceQueryTime;

    public final Counter totalAggregationRuns;

    public final Timer totalReadTime;

    public Metrics() {
        registry = new MetricRegistry();

        rawInserts = registry.meter(name(MeasurementCollector.class, "rawInserts"));
        batchInsertTime = registry.timer(name(MeasurementCollector.class, "batchInsertTime"));
        totalAggregationTime = registry.timer(name(MeasurementAggregator.class, "totalAggregationTime"));
        twentyFourHourResourceQueryTime = registry.timer(name(MeasurementReader.class, "24HourResourceDataQuery"));
        oneWeekResourceQueryTime = registry.timer(name(MeasurementReader.class, "oneWeekResourceQueryData"));
        twoWeekResourceQueryTime = registry.timer(name(MeasurementReader.class, "twoWeekResourceQueryData"));
        monthResourceQueryTime = registry.timer(name(MeasurementReader.class, "monthResourceQueryTime"));
        yearResourceQueryTime = registry.timer(name(MeasurementReader.class, "yearResourceQueryTime"));
        totalAggregationRuns = registry.counter(name(MeasurementAggregator.class, "totalAggregationRuns"));
        totalReadTime = registry.timer(name(MeasurementReader.class, "totalReadTime"));
    }

}
