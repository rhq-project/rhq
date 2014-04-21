package org.rhq.server.metrics.aggregation;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.CacheIndexEntry;
import org.rhq.server.metrics.domain.CacheIndexEntryMapper;
import org.rhq.server.metrics.domain.RawNumericMetric;

/**
 * @author John Sanda
 */
class CacheAggregator extends BaseAggregator {

    private static final Log LOG = LogFactory.getLog(CacheAggregator.class);

    static interface CacheBlockFinishedListener {
        void onFinish(IndexAggregatesPair pair);
    }

    private DateTime currentDay;

    private AtomicInteger schedulesCount = new AtomicInteger();

    private CacheBlockFinishedListener cacheBlockFinishedListener;

    void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    void setCurrentDay(DateTime currentDay) {
        this.currentDay = currentDay;
    }

    void setCacheBlockFinishedListener(CacheBlockFinishedListener cacheBlockFinishedListener) {
        this.cacheBlockFinishedListener = cacheBlockFinishedListener;
    }

    @Override
    protected String getDebugType() {
        return aggregationType.toString();
    }

    @Override
    protected ListenableFuture<List<CacheIndexEntry>> findIndexEntries() {
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

    /**
     * Filters out index entries where {@link CacheIndexEntry#getCollectionTimeSlice() collectionTimeSlice} does not
     * equal {@link CacheIndexEntry#getInsertTimeSlice() insertTimeSlice}. The filtering is done in support of data
     * migration which will be necessary for users upgrading from versions prior to RHQ 4.11.
     *
     * @param indexEntries The index entries returned from the storage cluster
     * @return An Iterable of the index entries with those having {@link CacheIndexEntry#getCollectionTimeSlice() collectionTimeSlice}
     * not equal to {@link CacheIndexEntry#getInsertTimeSlice() insertTimeSlice} filtered out.
     */
    @Override
    protected Iterable<CacheIndexEntry> reduceIndexEntries(List<CacheIndexEntry> indexEntries) {
        final PeekingIterator<CacheIndexEntry> iterator = Iterators.peekingIterator(indexEntries.iterator());

        return new Iterable<CacheIndexEntry>() {
            @Override
            public Iterator<CacheIndexEntry> iterator() {
                return new Iterator<CacheIndexEntry>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext() &&
                            iterator.peek().getCollectionTimeSlice() == iterator.peek().getInsertTimeSlice();
                    }

                    @Override
                    public CacheIndexEntry next() {
                        return iterator.next();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override
    protected AggregationTask createAggregationTask(CacheIndexEntry indexEntry) {
        return new AggregationTask(indexEntry) {
            @Override
            void run(CacheIndexEntry indexEntry) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Executing " + getDebugType() + " aggregation task for " + indexEntry);
                }

                StorageResultSetFuture cacheFuture = dao.findCacheEntriesAsync(aggregationType.getCacheTable(),
                    startTime.getMillis(), indexEntry.getStartScheduleId());

                processCacheBlock(indexEntry, cacheFuture, persistMetrics);
            }
        };
    }

    @Override
    protected Map<AggregationType, Integer> getAggregationCounts() {
        return ImmutableMap.of(aggregationType, schedulesCount.get());
    }

    /**
     * <p>
     * This method provides the core aggregation logic. It performs the following steps:
     *
     * <ul>
     *  <li>Iterate over a cache result set (which may contain data for multiple schedules)</li>
     *  <li>Compute aggregate metrics</li>
     *  <li>Persist the aggregate metrics</li>
     *  <li>Delete the cache partition</li>
     *  <li>Delete the cache index row</li>
     * </ul>
     * </p>
     * <p>
     * Be aware that this method is completely asynchronous. Each of the preceding steps correspond to function calls
     * that return a ListenableFuture. While this method is asynchronous, the steps will execute in the order listed.
     * </p>
     * <p>
     * It is also important to note that if one of the function calls fails, then the functions for the steps that
     * follow are <strong>not</strong> executed. This is by design so that the task can be retried during a subsequent
     * aggregation run.
     * </p>
     *
     * @param indexEntry The index entry for which data is being aggregated
     * @param cacheFuture A future of the cache query result set
     * @param persistMetricsFn The function that will be used to persist the aggregate metrics
     */
    @SuppressWarnings("unchecked")
    protected void processCacheBlock(CacheIndexEntry indexEntry,
        StorageResultSetFuture cacheFuture, AsyncFunction<IndexAggregatesPair, List<ResultSet>> persistMetricsFn) {

        ListenableFuture<Iterable<List<RawNumericMetric>>> iterableFuture = Futures.transform(cacheFuture,
            toIterable(aggregationType.getCacheMapper()), aggregationTasks);

        ListenableFuture<List<AggregateNumericMetric>> metricsFuture = Futures.transform(iterableFuture,
            computeAggregates(indexEntry.getCollectionTimeSlice(), RawNumericMetric.class), aggregationTasks);

        ListenableFuture<IndexAggregatesPair> pairFuture = Futures.transform(metricsFuture,
            indexAggregatesPair(indexEntry));

        ListenableFuture<List<ResultSet>> insertsFuture = Futures.transform(pairFuture, persistMetricsFn,
            aggregationTasks);

        ListenableFuture<ResultSet> deleteCacheFuture = Futures.transform(insertsFuture,
            deleteCacheEntry(indexEntry), aggregationTasks);

        ListenableFuture<ResultSet> deleteCacheIndexFuture = Futures.transform(deleteCacheFuture,
            deleteCacheIndexEntry(indexEntry), aggregationTasks);

        aggregationTaskFinished(deleteCacheIndexFuture, pairFuture);
    }

    @SuppressWarnings("unchecked")
    private void aggregationTaskFinished(ListenableFuture<ResultSet> deleteCacheIndexFuture,
        ListenableFuture<IndexAggregatesPair> pairFuture) {

        final ListenableFuture<List<Object>> argsFuture = Futures.allAsList(deleteCacheIndexFuture, pairFuture);

        Futures.addCallback(argsFuture, new AggregationTaskFinishedCallback<List<Object>>() {
            @Override
            protected void onFinish(List<Object> args) {
                IndexAggregatesPair pair = (IndexAggregatesPair) args.get(1);

                if (cacheBlockFinishedListener != null) {
                    cacheBlockFinishedListener.onFinish(pair);
                }
                schedulesCount.addAndGet(pair.metrics.size());
            }
        }, aggregationTasks);
    }
}
