package org.rhq.server.metrics;

import java.util.List;
import java.util.Map;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import org.rhq.server.metrics.domain.AggregateNumericMetric;

/**
 * @author John Sanda
 */
public class AggregateCacheMapper implements CacheMapper<AggregateNumericMetric> {

    public AggregateNumericMetric map(Row row) {
        // This class will be going away when we remove the metrics_cache table
        throw new UnsupportedOperationException();
    }

    public List<AggregateNumericMetric> map(ResultSet resultSet) {
        // This class will be going away when we remove the metrics_cache table
        throw new UnsupportedOperationException();
    }

    private int getScheduleId(Row row) {
        return row.getInt(0);
    }

    private long getTimestamp(Row row) {
        return row.getDate(1).getTime();
    }

    private Map<Integer, Double> getValues(Row row) {
        return row.getMap(2, Integer.class, Double.class);
    }
}
