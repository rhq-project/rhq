package org.rhq.server.metrics.aggregation;

import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.google.common.util.concurrent.ListenableFuture;

import org.rhq.server.metrics.domain.AggregateNumericMetric;

/**
 * @author John Sanda
 */
class MetricsFuturesPair {

    public final ListenableFuture<List<ResultSet>> resultSetsFuture;

    public final ListenableFuture<List<AggregateNumericMetric>> metricsFuture;

    public MetricsFuturesPair(ListenableFuture<List<ResultSet>> resultSetsFuture,
        ListenableFuture<List<AggregateNumericMetric>> metricsFuture) {
        this.resultSetsFuture = resultSetsFuture;
        this.metricsFuture = metricsFuture;
    }

}
