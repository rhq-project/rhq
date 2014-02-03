package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.server.metrics.SignalingCountDownLatch;
import org.rhq.server.metrics.StorageResultSetFuture;

/**
 * @author John Sanda
 */
abstract class BatchAggregationScheduler implements FutureCallback<ResultSet> {

    private final Log log = LogFactory.getLog(BatchAggregationScheduler.class);

    protected AggregationState state;

    public BatchAggregationScheduler(AggregationState state) {
        this.state = state;
    }

    @Override
    public void onSuccess(ResultSet indexResultSet) {
        Stopwatch stopwatch = new Stopwatch().start();
        Stopwatch batchStopwatch = new Stopwatch().start();
        List<StorageResultSetFuture> queryFutures = new ArrayList<StorageResultSetFuture>(state.getBatchSize());
        int numSchedules = 0;
        try {
            for (Row row : indexResultSet) {
                state.getPermits().acquire();
                ++numSchedules;
                getRemainingSchedules().incrementAndGet();
                queryFutures.add(findMetricData(row.getInt(1)));
                if (queryFutures.size() == state.getBatchSize()) {
                    state.getAggregationTasks().submit(new BatchAggregator(createBatchAggregationState(queryFutures,
                        batchStopwatch)));
                    queryFutures = new ArrayList<StorageResultSetFuture>(state.getBatchSize());
                    batchStopwatch = new Stopwatch().start();
                }
            }
            if (!queryFutures.isEmpty()) {
                state.getAggregationTasks().submit(new BatchAggregator(createBatchAggregationState(queryFutures,
                    batchStopwatch)));
            }
            if (numSchedules == 0) {
                getAggregationDoneSignal().countDown();
            }
            stopwatch.stop();
            if (log.isDebugEnabled()) {
                log.debug("Finished scheduling " + getAggregationType() + " aggregation tasks for " + numSchedules +
                    " schedules in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            }
        } catch (InterruptedException e) {
            log.info("There was an interrupt while scheduling aggregation tasks for " + getAggregationType() + ": " +
                e.getMessage());
            log.info("Aggregation will be aborted");
            getAggregationDoneSignal().abort("There was an interrupt while scheduling aggregation tasks for " +
                getAggregationType() + ": " + e.getMessage());
        }
    }

    private BatchAggregationState createBatchAggregationState(List<StorageResultSetFuture> queryFutures,
        Stopwatch batchStopwatch) {
        return new BatchAggregationState()
            .setAggregationTasks(state.getAggregationTasks())
            .setAggregationType(getAggregationType())
            .setComputeAggregates(getComputeAggregates())
            .setDoneSignal(getAggregationDoneSignal())
            .setPermits(state.getPermits())
            .setQueryFutures(queryFutures)
            .setRemainingSchedules(getRemainingSchedules())
            .setStopwatch(batchStopwatch);
    }

    @Override
    public void onFailure(Throwable t) {
        if (log.isDebugEnabled()) {
            log.debug("Aggregation for time slice [" + state.getStartTime() + "] cannot proceed. There was an " +
                "unexpected error while retrieving " + getAggregationType() + " index entries.", t);
        } else {
            log.warn("Aggregation for time slice [" + state.getStartTime() + "] cannot proceed. There was an " +
                "unexpected error while retrieving " + getAggregationType() + " index entries: " +
                ThrowableUtil.getRootMessage(t));
        }
        getAggregationDoneSignal().abort("There was an error while retrieving " + getAggregationType() +
            " index entries: " + ThrowableUtil.getRootMessage(t));
    }

    protected abstract SignalingCountDownLatch getAggregationDoneSignal();

    protected abstract AggregationType getAggregationType();

    protected abstract StorageResultSetFuture findMetricData(int scheduleId);

    protected abstract AsyncFunction<List<ResultSet>, List<ResultSet>> getComputeAggregates();

    protected abstract AtomicInteger getRemainingSchedules();

}
