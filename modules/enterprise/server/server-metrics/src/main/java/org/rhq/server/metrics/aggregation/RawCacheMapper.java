package org.rhq.server.metrics.aggregation;

import java.util.Map;

import com.datastax.driver.core.Row;

import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.RawNumericMetric;

/**
 * @author John Sanda
 */
public class RawCacheMapper implements CacheMapper<RawNumericMetric> {

    public RawNumericMetric map(Row row) {
        return new RawNumericMetric(getScheduleId(row), getTimestamp(row), getValue(row));
    }

    private int getScheduleId(Row row) {
        return row.getInt(0);
    }

    private long getTimestamp(Row row) {
        return row.getDate(1).getTime();
    }

    private Double getValue(Row row) {
        Map<Integer, Double> values = row.getMap(2, Integer.class, Double.class);
        return values.get(AggregateType.VALUE.ordinal());
    }

}
