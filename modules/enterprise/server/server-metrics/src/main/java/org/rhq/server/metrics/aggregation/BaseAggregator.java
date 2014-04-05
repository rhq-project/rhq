package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.AbortedException;
import org.rhq.server.metrics.ArithmeticMeanCalculator;
import org.rhq.server.metrics.CacheMapper;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.CacheIndexEntry;
import org.rhq.server.metrics.domain.NumericMetric;

/**
 * @author John Sanda
 */
abstract class BaseAggregator {

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

    // Currently this is only used by CacheAggregator as a hook to return the 1 hour data
    // that is needed for subsequent baseline calculations
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

    void setCacheBatchSize(int cacheBatchSize) {
        this.cacheBatchSize = cacheBatchSize;
    }

    public int execute() throws InterruptedException, AbortedException {
        Stopwatch stopwatch = new Stopwatch().start();
        AtomicInteger numSchedules = new AtomicInteger();
        try {
            // need to call addTask() here for this initial callback; otherwise, the
            // following call waitForTasksToFinish can complete prematurely.
            taskTracker.addTask();
            Futures.addCallback(findIndexEntries(), new FutureCallback<List<CacheIndexEntry>>() {
                @Override
                public void onSuccess(List<CacheIndexEntry> indexEntries) {
                    scheduleTasks(indexEntries);
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

    abstract ListenableFuture<List<CacheIndexEntry>> findIndexEntries();

    abstract Runnable createAggregationTask(CacheIndexEntry indexEntry);

    private void scheduleTasks(List<CacheIndexEntry> indexEntries) {
        try {
            for (CacheIndexEntry indexEntry : indexEntries) {
                permits.acquire();
                aggregationTasks.submit(createAggregationTask(indexEntry));
                taskTracker.addTask();
            }
            taskTracker.finishedSchedulingTasks();
        } catch (InterruptedException e) {
            LOG.warn("There was an interrupt while scheduling aggregation tasks.", e);
            taskTracker.abort("There was an interrupt while scheduling aggregation tasks.");
        }
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

    /**
     * @param indexEntry
     * @return
     */
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

    protected Function<List<AggregateNumericMetric>, IndexAggregatesPair> indexAggregatesPair(
        final CacheIndexEntry indexEntry) {
        return new Function<List<AggregateNumericMetric>, IndexAggregatesPair>() {
            @Override
            public IndexAggregatesPair apply(List<AggregateNumericMetric> metrics) {
                return new IndexAggregatesPair(indexEntry, metrics);
            }
        };
    }

    protected <T extends NumericMetric> Function<ResultSet, Iterable<List<T>>> toIterable(final CacheMapper<T> mapper) {
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

    protected FutureCallback<ResultSet> cacheBlockFinished(final ListenableFuture<IndexAggregatesPair> pairFuture) {
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
