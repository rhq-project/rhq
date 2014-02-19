package org.rhq.server.metrics.aggregation;

import java.util.List;

import org.rhq.server.metrics.ArithmeticMeanCalculator;
import org.rhq.server.metrics.StorageResultSetFuture;

/**
 * @author John Sanda
 */
public interface ComputeMetric {

    List<StorageResultSetFuture> execute(int startScheduleId, int scheduleId, Double min, Double max,
        ArithmeticMeanCalculator mean);

}
