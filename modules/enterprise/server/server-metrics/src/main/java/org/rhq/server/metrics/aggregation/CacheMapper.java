package org.rhq.server.metrics.aggregation;

/**
 * @author John Sanda
 */

import com.datastax.driver.core.Row;

import org.rhq.server.metrics.domain.NumericMetric;

public interface CacheMapper<T extends NumericMetric> {

    T map(Row row);

}
