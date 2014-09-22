package org.rhq.server.metrics;

import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Function;

import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateNumericMetricMapper;
import org.rhq.server.metrics.domain.Bucket;

/**
 * @author John Sanda
 */
public class ComputeAggregate implements Function<ResultSet, AggregateNumericMetric> {

    private AggregateNumericMetricMapper mapper;

    private long timestamp;

    private Bucket bucket;

    public ComputeAggregate(long timestamp, Bucket bucket) {
        this.timestamp = timestamp;
        this.bucket = bucket;
        mapper = new AggregateNumericMetricMapper();
    }

    @Override
    public AggregateNumericMetric apply(ResultSet resultSet) {
        List<AggregateNumericMetric> metrics = mapper.mapAll(resultSet);
        double min = Double.NaN;
        double max = min;
        int count = 0;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();

        for (AggregateNumericMetric metric : metrics) {
            if (count == 0) {
                min = metric.getMin();
                max = metric.getMax();
            }
            if (metric.getMin() < min) {
                min = metric.getMin();
            } else if (metric.getMax() > max) {
                max = metric.getMax();
            }
            mean.add(metric.getAvg());
            ++count;
        }

        // We let the caller handle setting the schedule id because in some cases we do
        // not care about it.
        return new AggregateNumericMetric(0, bucket, mean.getArithmeticMean(), min, max, timestamp);
    }
}
