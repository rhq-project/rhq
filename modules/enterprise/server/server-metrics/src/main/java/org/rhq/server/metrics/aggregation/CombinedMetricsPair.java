package org.rhq.server.metrics.aggregation;

import com.datastax.driver.core.ResultSet;

import org.rhq.server.metrics.domain.AggregateNumericMetric;

/**
 *
 * This is a container for a query result set of aggregate metrics for a particular time slice coupled with a recently
 * computed aggregate metric for that time slice. It is entirely possible for the result set to contain the computed
 * metric; however, because we use weak consistency on writes, it is also possible that the result set might not contain
 * the computed metric. This class is used in conjunction with {@link CombinedMetricsIterator} to ensure we have all
 * metrics for a time slice.
 *
 * @author John Sanda
 */
class CombinedMetricsPair {

    /**
     * A query result set of aggregate metrics for a single schedule id
     */
    public final ResultSet resultSet;

    /**
     * A recently computed metric having the same schedule id as the data in {@link #resultSet}
     */
    public final AggregateNumericMetric metric;

    public CombinedMetricsPair(ResultSet resultSet, AggregateNumericMetric metric) {
        this.resultSet = resultSet;
        this.metric = metric;
    }

}
