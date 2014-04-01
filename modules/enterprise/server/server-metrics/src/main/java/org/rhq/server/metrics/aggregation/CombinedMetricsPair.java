package org.rhq.server.metrics.aggregation;

import com.datastax.driver.core.ResultSet;

import org.rhq.server.metrics.domain.AggregateNumericMetric;

/**
 * @author John Sanda
 */
public class CombinedMetricsPair {

    public final ResultSet resultSet;

    public final AggregateNumericMetric metric;

    public CombinedMetricsPair(ResultSet resultSet, AggregateNumericMetric metric) {
        this.resultSet = resultSet;
        this.metric = metric;
    }

}
