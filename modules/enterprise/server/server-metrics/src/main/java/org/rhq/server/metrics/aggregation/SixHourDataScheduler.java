package org.rhq.server.metrics.aggregation;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.google.common.util.concurrent.AsyncFunction;

import org.rhq.server.metrics.SignalingCountDownLatch;
import org.rhq.server.metrics.StorageResultSetFuture;

/**
 * @author John Sanda
 */
class SixHourDataScheduler extends BatchAggregationScheduler {

    public SixHourDataScheduler(AggregationState state) {
        super(state);
    }

    @Override
    protected SignalingCountDownLatch getAggregationDoneSignal() {
        return state.getSixHourAggregationDone();
    }

    @Override
    protected AggregationType getAggregationType() {
        return AggregationType.SIX_HOUR;
    }

    @Override
    protected StorageResultSetFuture findMetricData(int scheduleId) {
        return state.getDao().findSixHourMetricsAsync(scheduleId, state.getTwentyFourHourTimeSlice().getMillis(),
            state.getTwentyFourHourTimeSliceEnd().getMillis());
    }

    @Override
    protected AsyncFunction<List<ResultSet>, List<ResultSet>> getComputeAggregates() {
        return state.getCompute24HourData();
    }

    @Override
    protected AtomicInteger getRemainingSchedules() {
        return state.getRemaining6HourData();
    }
}
