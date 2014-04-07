package org.rhq.server.metrics.aggregation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
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

    private static final int LATE_DATA_BATCH_SIZE = 5;

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
    protected ListenableFuture<List<CacheIndexEntry>> findIndexEntries() {
        return findCurrentCacheIndexEntries();
    }

    @Override
    protected AggregationTask createAggregationTask(CacheIndexEntry indexEntry) {
        return new AggregationTask(indexEntry) {
            @Override
            void run(CacheIndexEntry indexEntry) {
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
