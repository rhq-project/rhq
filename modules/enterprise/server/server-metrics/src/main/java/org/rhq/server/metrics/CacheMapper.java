package org.rhq.server.metrics;

/**
 * @author John Sanda
 */

import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import org.rhq.server.metrics.domain.NumericMetric;

public interface CacheMapper<T extends NumericMetric> {

    T map(Row row);

    List<T> map(ResultSet resultSet);

}
