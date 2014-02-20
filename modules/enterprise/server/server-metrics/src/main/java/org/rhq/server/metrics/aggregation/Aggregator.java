package org.rhq.server.metrics.aggregation;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.AbortedException;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.StorageResultSetFuture;

/**
 * @author John Sanda
 */
class Aggregator {

    private static final Log LOG = LogFactory.getLog(Aggregator.class);

    private ComputeMetric computeMetric;

    private int startScheduleId;

    private int maxScheduleId;

    private int cacheBatchSize;

    private Semaphore permits;

    private DateTime startTime;

    private ListeningExecutorService aggregationTasks;

    private AggregationType aggregationType;

    private MetricsDAO dao;

    private TaskTracker taskTracker = new TaskTracker();

    void setComputeMetric(ComputeMetric computeMetric) {
        this.computeMetric = computeMetric;
    }

    void setStartScheduleId(int startScheduleId) {
        this.startScheduleId = startScheduleId;
    }

    void setMaxScheduleId(int maxScheduleId) {
        this.maxScheduleId = maxScheduleId;
    }

    void setCacheBatchSize(int cacheBatchSize) {
        this.cacheBatchSize = cacheBatchSize;
    }

    void setPermits(Semaphore permits) {
        this.permits = permits;
    }

    void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    void setAggregationTasks(ListeningExecutorService aggregationTasks) {
        this.aggregationTasks = aggregationTasks;
    }

    void setAggregationType(AggregationType aggregationType) {
        this.aggregationType = aggregationType;
    }

    void setDao(MetricsDAO dao) {
        this.dao = dao;
    }

    public int execute() throws InterruptedException, AbortedException {
        Stopwatch stopwatch = new Stopwatch().start();
        AtomicInteger numSchedules = new AtomicInteger();
        try {
            for (int i = startScheduleId; i <= maxScheduleId; i += cacheBatchSize) {
                Stopwatch batchStopwatch = new Stopwatch().start();
                permits.acquire();
                StorageResultSetFuture queryFuture = dao.findMetricsIndexEntriesAsync(aggregationType.getCacheTable(),
                    startTime.getMillis(), i);
                taskTracker.addTask();
                ListenableFuture<BatchResult> batchResultFuture = Futures.transform(queryFuture,
                    new ProcessBatch(dao, computeMetric, i, startTime, aggregationType,
                        cacheBatchSize), aggregationTasks);
                Futures.addCallback(batchResultFuture, batchFinished(numSchedules, batchStopwatch), aggregationTasks);
            }
            taskTracker.finishedSchedulingTasks();
            taskTracker.waitForTasksToFinish();

            return numSchedules.get();
        } finally {
            stopwatch.stop();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Finished " + aggregationType + " aggregation of " + numSchedules + " schedules in " +
                    stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            }
        }
    }

    private FutureCallback<BatchResult> batchFinished(final AtomicInteger numSchedules, final Stopwatch stopwatch) {
        return new FutureCallback<BatchResult>() {
            @Override
            public void onSuccess(BatchResult result) {
                updateRemainingBatches();
                int delta;
                if (aggregationType == AggregationType.SIX_HOUR) {
                    delta = result.getInsertResultSets().size() / 3;
                } else {
                    delta = result.getInsertResultSets().size() / 4;
                }
                numSchedules.getAndAdd(delta);
                stopwatch.stop();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Finished batch of " + aggregationType + " for " + delta + " schedules with starting " +
                        "schedule id " + result.getStartScheduleId() + " in " +
                        stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("There was an unexpected error while processing a batch of " + aggregationType);
                updateRemainingBatches();
            }
        };
    }

    private void updateRemainingBatches() {
        permits.release();
        taskTracker.finishedTask();
    }

}
