package org.rhq.server.metrics.aggregation;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
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
import org.rhq.server.metrics.CacheMapper;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.CacheIndexEntry;
import org.rhq.server.metrics.domain.CacheIndexEntryMapper;
import org.rhq.server.metrics.domain.NumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetric;

/**
 * @author John Sanda
 */
class CacheAggregator extends BaseAggregator {

    private static final Log LOG = LogFactory.getLog(CacheAggregator.class);

    private static final int LATE_DATA_BATCH_SIZE = 5;

    static interface CacheBlockFinishedListener {
        void onFinish(IndexAggregatesPair pair);
    }

    private int cacheBatchSize;

    private DateTime currentDay;

    private TaskTracker taskTracker = new TaskTracker();

    private CacheBlockFinishedListener cacheBlockFinishedListener;

    void setCacheBatchSize(int cacheBatchSize) {
        this.cacheBatchSize = cacheBatchSize;
    }

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

    @SuppressWarnings("unchecked")
    private void processCacheBlock(CacheIndexEntry indexEntry, StorageResultSetFuture cacheFuture) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing " + indexEntry);
        }

        taskTracker.addTask();

        ListenableFuture<Iterable<List<RawNumericMetric>>> iterableFuture = Futures.transform(cacheFuture,
            toIterable(aggregationType.getCacheMapper()), aggregationTasks);

        ListenableFuture<List<AggregateNumericMetric>> metricsFuture = Futures.transform(iterableFuture,
            computeAggregates(indexEntry.getCollectionTimeSlice(), RawNumericMetric.class), aggregationTasks);

        ListenableFuture<IndexAggregatesPair> pairFuture = Futures.transform(metricsFuture,
            indexAggregatesPair(indexEntry));

        ListenableFuture<List<ResultSet>> insertsFuture = Futures.transform(pairFuture, persistMetrics, aggregationTasks);

        ListenableFuture<ResultSet> deleteCacheFuture = Futures.transform(insertsFuture,
            deleteCacheEntry(indexEntry), aggregationTasks);

        ListenableFuture<ResultSet> deleteCacheIndexFuture = Futures.transform(deleteCacheFuture,
            deleteCacheIndexEntry(indexEntry), aggregationTasks);

        Futures.addCallback(deleteCacheIndexFuture, cacheBlockFinished(pairFuture), aggregationTasks);
    }

    private <T extends NumericMetric> Function<ResultSet, Iterable<List<T>>> toIterable(final CacheMapper<T> mapper) {
        return new Function<ResultSet, Iterable<List<T>>>() {
            @Override
            public Iterable<List<T>> apply(final ResultSet resultSet) {
                return new Iterable<List<T>>() {
                    @Override
                    public Iterator<List<T>> iterator() {
                        return new CacheIterator<T>(mapper, resultSet);
                    }
                };
            }
        };
    }

    private FutureCallback<ResultSet> cacheBlockFinished(final ListenableFuture<IndexAggregatesPair> pairFuture) {
        return new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet result) {
                if (cacheBlockFinishedListener != null) {
                    notifyListener(pairFuture);
                }
                permits.release();
                taskTracker.finishedTask();
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("There was an error aggregating data", t);
                permits.release();
                taskTracker.finishedTask();
            }
        };
    }

    private void notifyListener(ListenableFuture<IndexAggregatesPair> pairFuture) {
        try {
            IndexAggregatesPair pair = pairFuture.get();
            LOG.debug("Notifying listener for " + pair.metrics);
            cacheBlockFinishedListener.onFinish(pair);
        } catch (InterruptedException e) {
            LOG.info("There was an interrupt while trying to notify the cache block finished listener", e);
        } catch (ExecutionException e) {
            LOG.error("There was an unexpected error obtaining the " + IndexAggregatesPair.class.getSimpleName() +
                ". This should not happen!", e);
        }
    }

}
