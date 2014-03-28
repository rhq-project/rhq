package org.rhq.server.metrics.aggregation;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.AbortedException;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.CacheIndexEntry;
import org.rhq.server.metrics.domain.CacheIndexEntryMapper;

/**
 * @author John Sanda
 */
class CacheAggregator extends BaseAggregator {

    private static final Log LOG = LogFactory.getLog(CacheAggregator.class);

    private static final int LATE_DATA_BATCH_SIZE = 5;

    private DateTime currentDay;

    void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    void setCurrentDay(DateTime currentDay) {
        this.currentDay = currentDay;
    }

    void setCacheBlockFinishedListener(CacheBlockFinishedListener cacheBlockFinishedListener) {
        this.cacheBlockFinishedListener = cacheBlockFinishedListener;
    }

    public int execute() throws InterruptedException, AbortedException {
        Stopwatch stopwatch = new Stopwatch().start();
        AtomicInteger numSchedules = new AtomicInteger();
        try {
            // need to call addTask() here for this initial callback; otherwise, the
            // following call waitForTasksToFinish can complete prematurely.
            taskTracker.addTask();
            Futures.addCallback(findCurrentCacheIndexEntries(), new FutureCallback<List<CacheIndexEntry>>() {
                @Override
                public void onSuccess(List<CacheIndexEntry> indexEntries) {
                    scheduleDataAggregationTasks(indexEntries);
                    taskTracker.finishedTask();
                }

                @Override
                public void onFailure(Throwable t) {
                    taskTracker.abort("There was an error fetching current cache index entries: " + t.getMessage());
                }
            });
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

    private ListenableFuture<List<CacheIndexEntry>> findCurrentCacheIndexEntries() {
        StorageResultSetFuture indexFuture = dao.findCurrentCacheIndexEntries(aggregationType.getCacheTable(),
            currentDay.getMillis(), AggregationManager.INDEX_PARTITION, startTime.getMillis());

        return Futures.transform(indexFuture, new Function<ResultSet, List<CacheIndexEntry>>() {
            @Override
            public List<CacheIndexEntry> apply(ResultSet resultSet) {
                CacheIndexEntryMapper mapper = new CacheIndexEntryMapper();

                return mapper.map(resultSet);
            }
        });
    }

    private void scheduleDataAggregationTasks(List<CacheIndexEntry> indexEntries) {
        try {
            for (CacheIndexEntry indexEntry : indexEntries) {
                permits.acquire();
                StorageResultSetFuture cacheFuture = dao.findCacheEntriesAsync(aggregationType.getCacheTable(),
                    startTime.getMillis(), indexEntry.getStartScheduleId());
                processCacheBlock(indexEntry, cacheFuture);
            }
            taskTracker.finishedSchedulingTasks();
        } catch (InterruptedException e) {
            LOG.warn("There was an interrupt while scheduling aggregation tasks", e);
            taskTracker.abort("There was an interrupt while scheduling aggregation tasks");
        }
    }

}
