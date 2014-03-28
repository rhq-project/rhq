package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.ArithmeticMeanCalculator;
import org.rhq.server.metrics.CacheMapper;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.CacheIndexEntry;
import org.rhq.server.metrics.domain.NumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetric;

/**
 * @author John Sanda
 */
class BaseAggregator {

    private final Log LOG = LogFactory.getLog(getClass());

    protected static final int PAST_DATA_BATCH_SIZE = 5;

    protected MetricsDAO dao;

    protected AggregationType aggregationType;

    protected AsyncFunction<IndexAggregatesPair, List<ResultSet>> persistMetrics;

    protected Semaphore permits;

    protected ListeningExecutorService aggregationTasks;

    protected DateTime startTime;

    protected DateTimeService dateTimeService;

    protected TaskTracker taskTracker = new TaskTracker();
    protected CacheBlockFinishedListener cacheBlockFinishedListener;
    private int cacheBatchSize;

    void setDao(MetricsDAO dao) {
        this.dao = dao;
    }

    void setAggregationType(AggregationType aggregationType) {
        this.aggregationType = aggregationType;
    }

    void setPersistMetrics(AsyncFunction<IndexAggregatesPair, List<ResultSet>> persistMetrics) {
        this.persistMetrics = persistMetrics;
    }

    void setPermits(Semaphore permits) {
        this.permits = permits;
    }

    void setAggregationTasks(ListeningExecutorService aggregationTasks) {
        this.aggregationTasks = aggregationTasks;
    }

    void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }

    protected <T extends NumericMetric> Function<Iterable<List<T>>, List<AggregateNumericMetric>> computeAggregates(
        final long timeSlice, Class<T> type) {

        return new Function<Iterable<List<T>>, List<AggregateNumericMetric>>() {
            @Override
            public List<AggregateNumericMetric> apply(Iterable<List<T>> values) {
                List<AggregateNumericMetric> aggregates = new ArrayList<AggregateNumericMetric>(PAST_DATA_BATCH_SIZE);
                for (List<T> metricList : values) {
                    aggregates.add(computeAggregate(metricList, timeSlice));
                }
                return aggregates;
            }
        };
    }

    private <T extends NumericMetric> AggregateNumericMetric computeAggregate(List<T> metrics, long timeSlice) {
        Double min = Double.NaN;
        Double max = Double.NaN;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
        int scheduleId = 0;

        for (T metric : metrics) {
            mean.add(metric.getAvg());
            if (Double.isNaN(min)) {
                scheduleId = metric.getScheduleId();
                min = metric.getMin();
                max = metric.getMax();
            } else {
                if (metric.getMin() < min) {
                    min = metric.getMin();
                }
                if (metric.getMax() > max) {
                    max = metric.getMax();
                }
            }
        }
        return new AggregateNumericMetric(scheduleId, mean.getArithmeticMean(), min, max, timeSlice);
    }

    protected AsyncFunction<List<ResultSet>, ResultSet> deleteCacheEntry(final CacheIndexEntry indexEntry) {
        return new AsyncFunction<List<ResultSet>, ResultSet>() {
            @Override
            public ListenableFuture<ResultSet> apply(List<ResultSet> resultSets) throws Exception {
                return dao.deleteCacheEntries(aggregationType.getCacheTable(), indexEntry.getCollectionTimeSlice(),
                    indexEntry.getStartScheduleId());
            }
        };
    }

    protected AsyncFunction<ResultSet, ResultSet> deleteCacheIndexEntry(final CacheIndexEntry indexEntry) {
        return new AsyncFunction<ResultSet, ResultSet>() {
            @Override
            public ListenableFuture<ResultSet> apply(ResultSet deleteCacheResultSet) throws Exception {
                return dao.deleteCacheIndexEntries(aggregationType.getCacheTable(), indexEntry.getInsertTimeSlice(),
                    indexEntry.getPartition(), indexEntry.getCollectionTimeSlice(), indexEntry.getStartScheduleId());
            }
        };
    }

    protected Map<Integer, Double> toMap(NumericMetric metric) {
        return ImmutableMap.of(
            AggregateType.MAX.ordinal(), metric.getMax(),
            AggregateType.MIN.ordinal(), metric.getMin(),
            AggregateType.AVG.ordinal(), metric.getAvg()
        );
    }

    protected Function<List<AggregateNumericMetric>, IndexAggregatesPair> indexAggregatesPair(
        final CacheIndexEntry indexEntry) {
        return new Function<List<AggregateNumericMetric>, IndexAggregatesPair>() {
            @Override
            public IndexAggregatesPair apply(List<AggregateNumericMetric> metrics) {
                return new IndexAggregatesPair(indexEntry, metrics);
            }
        };
    }

    void setCacheBatchSize(int cacheBatchSize) {
        this.cacheBatchSize = cacheBatchSize;
    }

    @SuppressWarnings("unchecked")
    protected void processCacheBlock(CacheIndexEntry indexEntry, StorageResultSetFuture cacheFuture) {
//        if (LOG.isDebugEnabled()) {
//            LOG.debug("Processing " + indexEntry);
//        }

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
            cacheBlockFinishedListener.onFinish(pair);
        } catch (InterruptedException e) {
            LOG.info("There was an interrupt while trying to notify the cache block finished listener", e);
        } catch (ExecutionException e) {
            LOG.error("There was an unexpected error obtaining the " + IndexAggregatesPair.class.getSimpleName() +
                ". This should not happen!", e);
        }
    }

    static interface CacheBlockFinishedListener {
        void onFinish(IndexAggregatesPair pair);
    }
}
