package org.rhq.server.metrics.aggregation;

import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Function;
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

    @Override
    ListenableFuture<List<CacheIndexEntry>> findIndexEntries() {
        return findCurrentCacheIndexEntries();
    }

    @Override
    Runnable createAggregationTask(final CacheIndexEntry indexEntry) {
        return new Runnable() {
            @Override
            public void run() {
                StorageResultSetFuture cacheFuture = dao.findCacheEntriesAsync(aggregationType.getCacheTable(),
                    startTime.getMillis(), indexEntry.getStartScheduleId());
                processCacheBlock(indexEntry, cacheFuture, persistMetrics);
            }
        };
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
    protected void processCacheBlock(CacheIndexEntry indexEntry, StorageResultSetFuture cacheFuture,
        AsyncFunction<IndexAggregatesPair, List<ResultSet>> persistMetricsFn) {

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

        Futures.addCallback(deleteCacheIndexFuture, cacheBlockFinished(pairFuture), aggregationTasks);
    }
}
