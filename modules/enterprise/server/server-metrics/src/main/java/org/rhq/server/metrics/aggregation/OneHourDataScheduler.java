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
class OneHourDataScheduler extends BatchAggregationScheduler {

    public OneHourDataScheduler(AggregationState state) {
        super(state);
    }

    @Override
    protected SignalingCountDownLatch getAggregationDoneSignal() {
        return state.getOneHourAggregationDone();
    }

    @Override
    protected AggregationType getAggregationType() {
        return AggregationType.ONE_HOUR;
    }

    @Override
    protected StorageResultSetFuture findMetricData(int scheduleId) {
        return state.getDao().findOneHourMetricsAsync(scheduleId, state.getSixHourTimeSlice().getMillis(),
            state.getSixHourTimeSliceEnd().getMillis());
    }

    @Override
    protected AsyncFunction<List<ResultSet>, List<ResultSet>> getComputeAggregates() {
        return state.getCompute6HourData();
    }

    @Override
    protected AtomicInteger getRemainingSchedules() {
        return state.getRemaining1HourData();
    }

}
