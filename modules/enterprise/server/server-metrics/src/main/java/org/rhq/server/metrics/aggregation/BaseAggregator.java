package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.Bucket;
import org.rhq.server.metrics.domain.CacheIndexEntry;
import org.rhq.server.metrics.domain.IndexEntry;
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

    protected AsyncFunction<List<AggregateNumericMetric>, List<ResultSet>> persistMetrics;

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
    protected AtomicInteger schedulesCount = new AtomicInteger();
    protected CacheAggregator.BatchFinishedListener batchFinishedListener;

    @SuppressWarnings("unchecked")
    protected void aggregationTaskFinished(ListenableFuture<List<AggregateNumericMetric>> metricsFuture,
        ListenableFuture<List<ResultSet>> deletedIndexEntriesFuture) {
        final ListenableFuture<List<List<?>>> argsFuture = Futures.allAsList(metricsFuture, deletedIndexEntriesFuture);
        Futures.addCallback(argsFuture, new AggregationTaskFinishedCallback<List<List<?>>>() {
            @Override
            protected void onFinish(List<List<?>> args) {
                List<AggregateNumericMetric> metrics = (List<AggregateNumericMetric>) args.get(0);
                if (batchFinishedListener != null) {
                    batchFinishedListener.onFinish(metrics);
                }
                schedulesCount.addAndGet(metrics.size());
            }
        }, aggregationTasks);
    }

    /**
     * AggregationTask is a Runnable that computes aggregates for a set of schedules in a {@link CacheIndexEntry}.
     * If there are any unexpected errors, e.g., a NullPointerException, aggregation will be aborted.
     */
    protected abstract class AggregationTask implements Runnable {

        private List<IndexEntry> batch;

        public AggregationTask(List<IndexEntry> batch) {
            this.batch = batch;
        }

        @Override
        public void run() {
            try {
                run(batch);
            } catch (Exception e) {
                LOG.error("Aggregation will be aborted due to an unexpected error", e);
                taskTracker.abort("Aborting aggregation due to an unexpected error: " + e.getMessage());
            }
        }

        abstract void run(List<IndexEntry> batch);
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

    void setPersistMetrics(AsyncFunction<List<AggregateNumericMetric>, List<ResultSet>> persistMetrics) {
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
            List<IndexEntry> indexEntries = loadIndexEntries();
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


    protected abstract List<IndexEntry> loadIndexEntries();

    protected abstract AggregationTask createAggregationTask(List<IndexEntry> batch);

    /**
     * @return A mapping of the number of schedules that had data aggregated for each bucket, e.g., raw, 1 hour, 6 hour
     */
    protected abstract Map<AggregationType, Integer> getAggregationCounts();

    /**
     * @return The aggregation type for display in debug log messages
     */
    protected abstract String getDebugType();

    private void scheduleTasks(List<IndexEntry> indexEntries) {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Scheduling " + getDebugType() + " aggregation tasks for " + indexEntries.size() +
                    " schedule ids");
            }
            List<IndexEntry> batch = new ArrayList<IndexEntry>(BATCH_SIZE);
            for (IndexEntry indexEntry : indexEntries) {
                batch.add(indexEntry);
                if (batch.size() == BATCH_SIZE) {
                    submitAggregationTask(batch);
                    batch = new ArrayList<IndexEntry>();
                }
            }
            if (!batch.isEmpty()) {
                submitAggregationTask(batch);
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

    protected void submitAggregationTask(List<IndexEntry> batch) throws InterruptedException {
        permits.acquire();
        aggregationTasks.submit(createAggregationTask(batch));
        taskTracker.addTask();
    }

    protected <T extends NumericMetric> Function<List<ResultSet>, Iterable<List<T>>> toIterable(
        final ResultSetMapper<T> mapper) {
        // TODO add a unit test for when when one of the result sets is empty
        // We need to make sure we handle the case where one of the result sets is empty. This can happen since storing
        // a metric and updating the index is done as two separate writes and not as an atomic operation.
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

    protected <T extends NumericMetric> Function<Iterable<? extends Collection<T>>, List<AggregateNumericMetric>> computeAggregates(
        final long timeSlice, Class<T> type, final Bucket bucket) {

        return new Function<Iterable<? extends Collection<T>>, List<AggregateNumericMetric>>() {
            @Override
            public List<AggregateNumericMetric> apply(Iterable<? extends Collection<T>> values) {
                List<AggregateNumericMetric> aggregates = new ArrayList<AggregateNumericMetric>(BATCH_SIZE);
                for (Collection<T> metricList : values) {
                    aggregates.add(computeAggregate(metricList, timeSlice, bucket));
                }
                return aggregates;
            }
        };
    }

    private <T extends NumericMetric> AggregateNumericMetric computeAggregate(Collection<T> metrics, long timeSlice,
        Bucket bucket) {
        Double min = Double.NaN;
        Double max = Double.NaN;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
        int scheduleId = 0;

        // TODO handle when metrics is empty to avoid NaN metrics

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
        return new AggregateNumericMetric(scheduleId, bucket, mean.getArithmeticMean(), min, max, timeSlice);
    }

    protected AsyncFunction<List<ResultSet>, List<ResultSet>> deleteIndexEntries(final List<IndexEntry> indexEntries) {
        return new AsyncFunction<List<ResultSet>, List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> apply(List<ResultSet> insertResultSets) throws Exception {
                List<StorageResultSetFuture> deleteFutures = new ArrayList<StorageResultSetFuture>(indexEntries.size());
                for (IndexEntry indexEntry : indexEntries) {
                    deleteFutures.add(dao.deleteIndexEntry(indexEntry));
                }
                return Futures.allAsList(deleteFutures);
            }
        };
    }

}
