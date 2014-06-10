package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
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
import org.rhq.server.metrics.domain.ResultSetMapper;

/**
 * @author John Sanda
 */
abstract class BaseAggregator {

    private final Log LOG = LogFactory.getLog(getClass());

    protected static final int BATCH_SIZE = 5;

    protected MetricsDAO dao;

    /**
     * This should always be raw for PastDataAggregator and for CacheAggregator it should match the bucket, e.g.,
     * raw, 1 hour, 6 hr, being aggregated.
     */
    protected AggregationType aggregationType;

    protected AsyncFunction<IndexAggregatesPair, List<ResultSet>> persistMetrics;

    protected Semaphore permits;

    protected ListeningExecutorService aggregationTasks;

    protected DateTime startTime;

    protected DateTimeService dateTimeService;

    protected TaskTracker taskTracker = new TaskTracker();

    /**
     * This is a flag that determines whether or not we pull data from the metrics_cache table. It servers as a global
     * override that applies to both past and current data. There are two use cases for when this would be false.
     * The first is for the data migration that will be necessary when users upgrade to RHQ 4.11. The second is to
     * allow resizing of cache partitions to take effect.
     */
    protected boolean cacheActive = true;

    protected int indexPageSize;

    /**
     * AggregationTask is a Runnable that computes aggregates for a set of schedules in a {@link CacheIndexEntry}.
     * If there are any unexpected errors, e.g., a NullPointerException, aggregation will be aborted.
     */
    protected abstract class AggregationTask implements Runnable {

        private CacheIndexEntry indexEntry;

        public AggregationTask(CacheIndexEntry indexEntry) {
            this.indexEntry = indexEntry;
        }

        @Override
        public void run() {
            try {
                run(indexEntry);
            } catch (Exception e) {
                LOG.error("Aggregation will be aborted due to an unexpected error", e);
                taskTracker.abort("Aborting aggregation due to an unexpected error: " + e.getMessage());
            }
        }

        abstract void run(CacheIndexEntry indexEntry);
    }

    protected class AggregationTaskFinishedCallback<T> implements FutureCallback<T> {
        @Override
        public void onSuccess(T args) {
            try {
                onFinish(args);
            } finally {
                permits.release();
                taskTracker.finishedTask();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("There are " + taskTracker.getRemainingTasks() + " remaining tasks and " +
                        permits.availablePermits() + " available permits");
                }
            }
        }

        protected void onFinish(T args) {
        }

        @Override
        public void onFailure(Throwable t) {
            LOG.warn("There was an error aggregating data", t);
            permits.release();
            taskTracker.finishedTask();
            if (LOG.isDebugEnabled()) {
                LOG.debug("There are " + taskTracker.getRemainingTasks() + " remaining tasks and " +
                    permits.availablePermits() + " available permits");
            }
        }
    }

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

    void setCacheActive(boolean cacheActive) {
        this.cacheActive = cacheActive;
    }

    public void setIndexPageSize(int indexPageSize) {
        this.indexPageSize = indexPageSize;
    }

    public Map<AggregationType, Integer> execute() throws InterruptedException, AbortedException {
        LOG.debug("Starting " + getDebugType() + " aggregation");

        Stopwatch stopwatch = new Stopwatch().start();
        try {
            List<CacheIndexEntry> indexEntries = getIndexEntries();
            scheduleTasks(indexEntries);
            taskTracker.waitForTasksToFinish();
        } catch (CacheIndexQueryException e) {
            LOG.warn("There was an error querying the cache index", e);
            taskTracker.abort("There was an error querying the cache index: " + e.getMessage());
        } catch (Exception e) {
            LOG.warn("There was an unexpected error scheduling aggregation tasks", e);
            taskTracker.abort("There was an unexpected error scheduling aggregation tasks: " + e.getMessage());
        } finally {
            stopwatch.stop();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Finished " + getDebugType() + " aggregation in " + stopwatch.elapsed(TimeUnit.MILLISECONDS)
                    + " ms");
            }
        }
        return getAggregationCounts();
    }


    /**
     * @return The cache index entries for which aggregation tasks will be scheduled
     */
    protected abstract List<CacheIndexEntry> getIndexEntries();

    protected abstract AggregationTask createAggregationTask(CacheIndexEntry indexEntry);

    /**
     * @return A mapping of the number of schedules that had data aggregated for each bucket, e.g., raw, 1 hour, 6 hour
     */
    protected abstract Map<AggregationType, Integer> getAggregationCounts();

    /**
     * Aggregation tasks are scheduled based on a {collectionTimeSlice, startScheduleId} pair, and there can be multiple
     * index entries per pair. This method is responsible for reducing or combining those into a single a index entry.
     * Index entries can also be altogether filtered out as well. See {@link CacheAggregator#reduceIndexEntries(java.util.List)}
     * for details.
     *
     * @param indexEntries The index entries returned from the storage cluster
     * @return An Iterable of index entries for which aggregation tasks will be scheduled
     */
    protected abstract Iterable<CacheIndexEntry> reduceIndexEntries(List<CacheIndexEntry> indexEntries);

    /**
     * @return The aggregation type for display in debug log messages
     */
    protected abstract String getDebugType();

    private void scheduleTasks(List<CacheIndexEntry> indexEntries) {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Scheduling " + getDebugType() + " aggregation tasks for " + indexEntries.size() +
                    " index entries");
            }

            for (CacheIndexEntry indexEntry : reduceIndexEntries(indexEntries)) {
                submitAggregationTask(indexEntry);
            }
            taskTracker.finishedSchedulingTasks();
        } catch (InterruptedException e) {
            LOG.warn("There was an interrupt while scheduling aggregation tasks.", e);
            taskTracker.abort("There was an interrupt while scheduling aggregation tasks.");
        } catch (Exception e) {
            LOG.error("There was an unexpected error while scheduling " + getDebugType() + " aggregation tasks", e);
            taskTracker.abort("Aborting " + getDebugType() + " aggregation due to unexpected error: " + e.getMessage());
        } finally {
            LOG.debug("Finished scheduling aggregation tasks");
        }
    }

    protected void submitAggregationTask(CacheIndexEntry indexEntry) throws InterruptedException {
        permits.acquire();
        aggregationTasks.submit(createAggregationTask(indexEntry));
        taskTracker.addTask();
    }

    protected <T extends NumericMetric> Function<List<ResultSet>, Iterable<List<T>>> toIterable(
        final ResultSetMapper<T> mapper) {

        return new Function<List<ResultSet>, Iterable<List<T>>>() {
            @Override
            public Iterable<List<T>> apply(final List<ResultSet> resultSets) {
                return new Iterable<List<T>>() {
                    private Iterator<ResultSet> resultSetIterator = resultSets.iterator();

                    @Override
                    public Iterator<List<T>> iterator() {
                        return new Iterator<List<T>>() {
                            @Override
                            public boolean hasNext() {
                                return resultSetIterator.hasNext();
                            }

                            @Override
                            public List<T> next() {
                                return mapper.mapAll(resultSetIterator.next());
                            }

                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            }
        };
    }

    protected <T extends NumericMetric> Function<Iterable<List<T>>, List<AggregateNumericMetric>> computeAggregates(
        final long timeSlice, Class<T> type) {

        return new Function<Iterable<List<T>>, List<AggregateNumericMetric>>() {
            @Override
            public List<AggregateNumericMetric> apply(Iterable<List<T>> values) {
                List<AggregateNumericMetric> aggregates = new ArrayList<AggregateNumericMetric>(BATCH_SIZE);
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
                return dao.deleteCacheIndexEntry(aggregationType.getCacheTable(), indexEntry.getDay(),
                    indexEntry.getPartition(), indexEntry.getCollectionTimeSlice(), indexEntry.getStartScheduleId(),
                    indexEntry.getInsertTimeSlice());
            }
        };
    }

    /**
     * <p>
     * Some functions called during aggregation need as input both the computed, aggregate metrics and their
     * corresponding index entry. Guava is pretty limited when it comes to passing multiple arguments (which are
     * ListenableFutures) to a function. You are left with Futures.allAsList or Futures.successfulAsList. These methods
     * are fine sometimes, but when you have futures of different types, callee code gets littered with a lot type
     * casting.
     * </p>
     * <p>
     * This method returns a function that combines an index entry with aggregate metrics. The function returns a
     * {@link IndexAggregatesPair} which provides a strongly typed alternative to either Futures.allAsList or
     * Futures.successfulAsList.
     * </p>
     */
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


}
