package org.rhq.server.metrics;

import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Function;

import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.Bucket;
import org.rhq.server.metrics.domain.RawNumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetricMapper;

/**
 * @author John Sanda
 */
public class ComputeRawAggregate implements Function<ResultSet, AggregateNumericMetric> {

    private RawNumericMetricMapper mapper;

    private long timestamp;

    public ComputeRawAggregate(long timestamp) {
        this.timestamp = timestamp;
        mapper = new RawNumericMetricMapper();
    }

    @Override
    public AggregateNumericMetric apply(ResultSet resultSet) {
        List<RawNumericMetric> rawMetrics = mapper.mapAll(resultSet);
        double min = Double.NaN;
        double max = min;
        int count = 0;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
        double value;

        for (RawNumericMetric metric : rawMetrics) {
            value = metric.getValue();
            if (count == 0) {
                min = value;
                max = min;
            }
            if (value < min) {
                min = value;
            } else if (value > max) {
                max = value;
            }
            mean.add(value);
            ++count;
        }

        // We let the caller handle setting the schedule id because in some cases we do
        // not care about it.
        return new AggregateNumericMetric(0, Bucket.ONE_HOUR, mean.getArithmeticMean(), min, max, timestamp);
    }
}
