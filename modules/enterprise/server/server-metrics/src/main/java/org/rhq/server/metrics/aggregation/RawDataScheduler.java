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
class RawDataScheduler extends BatchAggregationScheduler {

    public RawDataScheduler(AggregationState state) {
        super(state);
    }

    @Override
    protected SignalingCountDownLatch getAggregationDoneSignal() {
        return state.getRawAggregationDone();
    }

    @Override
    protected AggregationType getAggregationType() {
        return AggregationType.RAW;
    }

    @Override
    protected StorageResultSetFuture findMetricData(int scheduleId) {
        return state.getDao().findRawMetricsAsync(scheduleId, state.getOneHourTimeSlice().getMillis(),
            state.getOneHourTimeSliceEnd().getMillis());
    }

    @Override
    protected AsyncFunction<List<ResultSet>, List<ResultSet>> getComputeAggregates() {
        return state.getCompute1HourData();
    }

    @Override
    protected AtomicInteger getRemainingSchedules() {
        return state.getRemainingRawData();
    }

}
