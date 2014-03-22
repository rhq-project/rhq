package org.rhq.server.metrics.aggregation;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
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
import org.rhq.server.metrics.domain.CacheIndexEntry;
import org.rhq.server.metrics.domain.CacheIndexEntryMapper;

/**
 * @author John Sanda
 */
class Aggregator {

    private static final Log LOG = LogFactory.getLog(Aggregator.class);

    private ComputeMetric computeMetric;

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
            StorageResultSetFuture indexFuture = dao.findCacheIndexEntries(aggregationType.getCacheTable(),
                startTime.getMillis(), AggregationManager.INDEX_PARTITION);
            ResultSet resultSet = indexFuture.get();
            CacheIndexEntryMapper indexEntryMapper = new CacheIndexEntryMapper();

            for (Row row : resultSet) {
                CacheIndexEntry indexEntry = indexEntryMapper.map(row);
                Stopwatch batchStopwatch = new Stopwatch().start();
                permits.acquire();
                StorageResultSetFuture cacheFuture = dao.findCacheEntriesAsync(aggregationType.getCacheTable(),
                    startTime.getMillis(), indexEntry.getStartScheduleId());
                taskTracker.addTask();
                ListenableFuture<BatchResult> batchResultFuture = Futures.transform(cacheFuture,
                    new ProcessBatch(dao, computeMetric, indexEntry, aggregationType, cacheBatchSize), aggregationTasks);
                Futures.addCallback(batchResultFuture, batchFinished(indexEntry, numSchedules, batchStopwatch),
                    aggregationTasks);
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

//    public int execute() throws InterruptedException, AbortedException {
//        Stopwatch stopwatch = new Stopwatch().start();
//        AtomicInteger numSchedules = new AtomicInteger();
//        try {
//            for (int i = startScheduleId; i <= maxScheduleId; i += cacheBatchSize) {
//                Stopwatch batchStopwatch = new Stopwatch().start();
//                permits.acquire();
//                StorageResultSetFuture queryFuture = dao.findCacheEntriesAsync(aggregationType.getCacheTable(),
//                    startTime.getMillis(), i);
//                taskTracker.addTask();
//                ListenableFuture<BatchResult> batchResultFuture = Futures.transform(queryFuture,
//                    new ProcessBatch(dao, computeMetric, i, startTime, aggregationType,
//                        cacheBatchSize), aggregationTasks);
//                Futures.addCallback(batchResultFuture, batchFinished(numSchedules, batchStopwatch), aggregationTasks);
//            }
//            taskTracker.finishedSchedulingTasks();
//            taskTracker.waitForTasksToFinish();
//
//            return numSchedules.get();
//        } finally {
//            stopwatch.stop();
//            if (LOG.isDebugEnabled()) {
//                LOG.debug("Finished " + aggregationType + " aggregation of " + numSchedules + " schedules in " +
//                    stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
//            }
//        }
//    }

    private FutureCallback<BatchResult> batchFinished(final CacheIndexEntry indexEntry,
        final AtomicInteger numSchedules, final Stopwatch stopwatch) {
        return new FutureCallback<BatchResult>() {
            @Override
            public void onSuccess(BatchResult result) {
                deleteCacheIndexEntry(indexEntry);
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
                        "schedule id " + indexEntry.getStartScheduleId() + " in " +
                        stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof BatchException) {
                    BatchException exception = (BatchException) t;
                    LOG.warn("There were errors while processing a batch of " + aggregationType + " with starting " +
                        "schedule id " + indexEntry.getStartScheduleId() + ": " + exception.getErrorMessages());
                    if (LOG.isDebugEnabled()) {
                        for (Throwable error : exception.getRootCauses()) {
                            LOG.debug("Root cause for batch error", error);
                        }
                    }
                } else {
                    LOG.warn("There was an unexpected error while processing a batch of " + aggregationType +
                        " with starting schedule id " + indexEntry.getStartScheduleId(), t);
                }
                deleteCacheIndexEntry(indexEntry);
                // TODO add some configurable strategy to determine whether or not to abort
                updateRemainingBatches();
            }
        };
    }

    private void deleteCacheIndexEntry(CacheIndexEntry indexEntry) {
        StorageResultSetFuture deleteFuture = dao.deleteCacheIndexEntries(aggregationType.getCacheTable(),
            indexEntry.getInsertTimeSlice(), AggregationManager.INDEX_PARTITION, indexEntry.getStartScheduleId(),
            indexEntry.getCollectionTimeSlice());
        Futures.addCallback(deleteFuture, new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet result) {
                LOG.debug("deleted cache index entry");
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("Failed to delete cache index entry", t);
            }
        }, aggregationTasks);
    }

    private void updateRemainingBatches() {
        permits.release();
        taskTracker.finishedTask();
    }

}
