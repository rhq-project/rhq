package org.rhq.server.metrics.aggregation;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.rhq.server.metrics.SignalingCountDownLatch;
import org.rhq.server.metrics.StorageResultSetFuture;

/**
 * @author John Sanda
 */
class BatchAggregationState {

    private List<StorageResultSetFuture> queryFutures;

    private AsyncFunction<List<ResultSet>, List<ResultSet>> computeAggregates;

    private ListeningExecutorService aggregationTasks;

    private Semaphore permits;

    private AtomicInteger remainingSchedules;

    private SignalingCountDownLatch doneSignal;

    private AggregationType aggregationType;

    private Stopwatch stopwatch;

    AggregationType getAggregationType() {
        return aggregationType;
    }

    BatchAggregationState setAggregationType(AggregationType aggregationType) {
        this.aggregationType = aggregationType;
        return this;
    }

    List<StorageResultSetFuture> getQueryFutures() {
        return queryFutures;
    }

    BatchAggregationState setQueryFutures(List<StorageResultSetFuture> queryFutures) {
        this.queryFutures = queryFutures;
        return this;
    }

    AsyncFunction<List<ResultSet>, List<ResultSet>> getComputeAggregates() {
        return computeAggregates;
    }

    BatchAggregationState setComputeAggregates(AsyncFunction<List<ResultSet>, List<ResultSet>> computeAggregates) {
        this.computeAggregates = computeAggregates;
        return this;
    }

    ListeningExecutorService getAggregationTasks() {
        return aggregationTasks;
    }

    BatchAggregationState setAggregationTasks(ListeningExecutorService aggregationTasks) {
        this.aggregationTasks = aggregationTasks;
        return this;
    }

    Semaphore getPermits() {
        return permits;
    }

    BatchAggregationState setPermits(Semaphore permits) {
        this.permits = permits;
        return this;
    }

    AtomicInteger getRemainingSchedules() {
        return remainingSchedules;
    }

    BatchAggregationState setRemainingSchedules(AtomicInteger remainingSchedules) {
        this.remainingSchedules = remainingSchedules;
        return this;
    }

    SignalingCountDownLatch getDoneSignal() {
        return doneSignal;
    }

    BatchAggregationState setDoneSignal(SignalingCountDownLatch doneSignal) {
        this.doneSignal = doneSignal;
        return this;
    }

    Stopwatch getStopwatch() {
        return stopwatch;
    }

    BatchAggregationState setStopwatch(Stopwatch stopwatch) {
        this.stopwatch = stopwatch;
        return this;
    }

}
