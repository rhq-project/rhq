package org.rhq.server.metrics.aggregation;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.ResultSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.exception.ThrowableUtil;

/**
 * @author John Sanda
 */
class BatchAggregator implements Runnable {

    private final Log log = LogFactory.getLog(BatchAggregator.class);

    private BatchAggregationState state;

    public BatchAggregator(BatchAggregationState state) {
        this.state = state;
    }

    @Override
    public void run() {
        ListenableFuture<List<ResultSet>> queriesFuture = Futures.successfulAsList(state.getQueryFutures());
        ListenableFuture<List<ResultSet>> insertFutures = Futures.transform(queriesFuture,
            state.getComputeAggregates(), state.getAggregationTasks());
        Futures.addCallback(insertFutures, new FutureCallback<List<ResultSet>>() {
            @Override
            public void onSuccess(List<ResultSet> result) {
                updateRemainingSchedules();
                state.getStopwatch().stop();

                if (log.isDebugEnabled()) {
                    log.debug("Finished aggregating " + state.getAggregationType() + " for " +
                        state.getQueryFutures().size() + " schedules in " +
                        state.getStopwatch().elapsed(TimeUnit.MILLISECONDS) + " ms");
                }

                state.getPermits().release(state.getQueryFutures().size());
            }

            @Override
            public void onFailure(Throwable t) {
                if (log.isDebugEnabled()) {
                    log.debug("There was an error during " + state.getAggregationType() + " aggregation",
                        ThrowableUtil.getRootCause(t));
                } else {
                    log.warn("There was an error during " + state.getAggregationType() + " aggregation: " +
                        ThrowableUtil.getRootMessage(t));
                }
                state.getPermits().release(state.getQueryFutures().size());
                updateRemainingSchedules();
            }
        }, state.getAggregationTasks());
    }

    private void updateRemainingSchedules() {
        int count = state.getRemainingSchedules().addAndGet(-state.getQueryFutures().size());
        if (log.isDebugEnabled()) {
            log.debug("There are " + count + " remaining schedules with " + state.getAggregationType() +
                " to be aggregated");
        }
        if (count == 0) {
            state.getDoneSignal().countDown();
        } else if (count < 0) {
            log.warn("The number of remaining schedules should never be less that zero. ");
            state.getDoneSignal().abort("There are " + count + " remaining schedules with " +
                state.getAggregationType() + " to be aggregated. The count should never be less than zero.");
        }
    }
}
