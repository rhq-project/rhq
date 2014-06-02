package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Preconditions;

import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateNumericMetricMapper;

/**
 * CombinedMetricsIterator takes as input a list of {@link CombinedMetricsPair}s and returns an iterator over aggregate
 * metrics. The pairs consists of results sets of either 1 hour or 6 hour metrics coupled with the recently computed
 * 1 hour or 6 hour metrics respectively. Because weak consistency is used for both reads and writes, the result sets
 * may or may not contained the recently computed metrics. The iterator returns all metrics and does not return
 * duplicate values.

 *
 * @author John Sanda
 */
class CombinedMetricsIterator implements Iterator<List<AggregateNumericMetric>> {

    private Iterator<List<AggregateNumericMetric>> iterator;

    public CombinedMetricsIterator(List<CombinedMetricsPair> pairs) {
        Preconditions.checkArgument(!pairs.isEmpty());

        List<ResultSet> resultSets = new ArrayList<ResultSet>(pairs.size());
        List<AggregateNumericMetric> metrics = new ArrayList<AggregateNumericMetric>(pairs.size());

        for (CombinedMetricsPair pair : pairs) {
            resultSets.add(pair.resultSet);
            metrics.add(pair.metric);
        }
        List<List<AggregateNumericMetric>> combinedMetrics = combine(resultSets, metrics);
        iterator = combinedMetrics.iterator();
    }

    private List<List<AggregateNumericMetric>> combine(List<ResultSet> resultSets,
        List<AggregateNumericMetric> inMemoryMetrics) {

        List<List<AggregateNumericMetric>> combinedMetrics = new ArrayList<List<AggregateNumericMetric>>();
        AggregateNumericMetricMapper mapper = new AggregateNumericMetricMapper();
        for (ResultSet resultSet : resultSets) {
            if (!resultSet.isExhausted()) {
                List<AggregateNumericMetric> metricsFromDB = mapper.mapAll(resultSet);
                int scheduleId = metricsFromDB.get(0).getScheduleId();
                int index = findIndex(scheduleId, inMemoryMetrics);
                if (index != -1) {
                    AggregateNumericMetric inMemoryMetric = inMemoryMetrics.remove(index);
                    if (!metricsFromDB.contains(inMemoryMetric)) {
                        metricsFromDB.add(inMemoryMetric);
                    }
                }
                combinedMetrics.add(metricsFromDB);
            }
        }
        return combinedMetrics;
    }

    private int findIndex(int scheduleId, List<AggregateNumericMetric> metrics) {
        int i = 0;
        for (AggregateNumericMetric metric : metrics) {
            if (metric.getScheduleId() == scheduleId) {
                break;
            }
            ++i;
        }
        return i;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public List<AggregateNumericMetric> next() {
        return iterator.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
