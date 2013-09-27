package org.rhq.metrics.simulator;

import static com.codahale.metrics.MetricRegistry.name;

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

    public Metrics() {
        registry = new MetricRegistry();

        rawInserts = registry.meter(name(MeasurementCollector.class, "rawInserts"));
        batchInsertTime = registry.timer(name(MeasurementCollector.class, "batchInsertTime"));
        totalAggregationTime = registry.timer(name(MeasurementAggregator.class, "totalAggregationTime"));
    }

}
