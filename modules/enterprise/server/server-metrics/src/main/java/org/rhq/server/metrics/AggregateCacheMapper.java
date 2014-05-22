package org.rhq.server.metrics;

import java.util.ArrayList;
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
        return new AggregateNumericMetric(getScheduleId(row), getTimestamp(row), getValues(row));
    }

    public List<AggregateNumericMetric> map(ResultSet resultSet) {
        List<AggregateNumericMetric> metrics = new ArrayList<AggregateNumericMetric>();
        for (Row row : resultSet) {
            metrics.add(map(row));
        }
        return metrics;
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
