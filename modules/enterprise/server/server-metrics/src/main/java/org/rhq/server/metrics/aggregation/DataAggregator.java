package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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
import org.joda.time.Duration;

import org.rhq.server.metrics.AbortedException;
import org.rhq.server.metrics.ArithmeticMeanCalculator;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsConfiguration;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateNumericMetricMapper;
import org.rhq.server.metrics.domain.Bucket;
import org.rhq.server.metrics.domain.CacheIndexEntry;
import org.rhq.server.metrics.domain.IndexBucket;
import org.rhq.server.metrics.domain.IndexEntry;
import org.rhq.server.metrics.domain.NumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetricMapper;
import org.rhq.server.metrics.domain.ResultSetMapper;

/**
 * @author John Sanda
 */
class DataAggregator<T extends NumericMetric> {

    private final Log log = LogFactory.getLog(getClass());

    /**
     * This is a hook for consuming 1 hour data produced from raw data aggregation. The 1 hour data needs to be made
     * available for baseline calculations.
     */
    static interface BatchFinishedListener {
        void onFinish(List<AggregateNumericMetric> metrics);
    }

    private MetricsDAO dao;

    private IndexBucket bucket;

    private AsyncFunction<List<AggregateNumericMetric>, List<ResultSet>> persistMetrics;

    private Semaphore permits;

    private ListeningExecutorService aggregationTasks;

    private DateTimeService dateTimeService;

    private TaskTracker taskTracker = new TaskTracker();

    private MetricsConfiguration configuration;

    private AtomicInteger schedulesCount = new AtomicInteger();

    private BatchFinishedListener batchFinishedListener;

    private ResultSetMapper resultSetMapper;

    private Duration timeSliceDuration;

    private int batchSize;

    void setDao(MetricsDAO dao) {
        this.dao = dao;
    }

    public void setBucket(IndexBucket bucket) {
        this.bucket = bucket;
        if (bucket == IndexBucket.RAW) {
            resultSetMapper = new RawNumericMetricMapper();
        } else {
            resultSetMapper = new AggregateNumericMetricMapper();
        }
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

    public void setTimeSliceDuration(Duration timeSliceDuration) {
        this.timeSliceDuration = timeSliceDuration;
    }

    void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }

    public void setConfiguration(MetricsConfiguration configuration) {
        this.configuration = configuration;
    }

    void setBatchFinishedListener(BatchFinishedListener batchFinishedListener) {
        this.batchFinishedListener = batchFinishedListener;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

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

    public int execute(DateTime start, DateTime end) throws InterruptedException,
        AbortedException {

        log.info("Starting " + bucket + " data aggregation");
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            IndexIterator iterator = new IndexIterator(start, end, bucket, dao, configuration);
            Batch batch = new Batch();
            while (iterator.hasNext()) {
                IndexEntry indexEntry = iterator.next();
                if (batch.getStartTime() == null) {
                    batch.setStartTime(indexEntry.getTimestamp());
                    batch.setEndTime(new DateTime(indexEntry.getTimestamp()).plus(timeSliceDuration));
                }
                if (batch.getStartTime().getMillis() == indexEntry.getTimestamp()) {
                    batch.add(indexEntry);
                    if (batch.size() == batchSize) {
                        submitAggregationTask(batch);
                        batch = new Batch();
                    }
                } else {
                    submitAggregationTask(batch);
                    batch = new Batch()
                        .setStartTime(indexEntry.getTimestamp())
                        .setEndTime(new DateTime(indexEntry.getTimestamp()).plus(timeSliceDuration))
                        .add(indexEntry);
                }
            }
            if (batch.size() > 0) {
                submitAggregationTask(batch);
            }
            iterator = null;
            taskTracker.finishedSchedulingTasks();
            taskTracker.waitForTasksToFinish(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            log.warn("There was an interrupt while scheduling aggregation tasks.", e);
            taskTracker.abort("There was an interrupt while scheduling aggregation tasks.");
        } catch (Exception e) {
            log.warn("There was an unexpected error scheduling aggregation tasks", e);
            taskTracker.abort("There was an unexpected error scheduling aggregation tasks: " + e.getMessage());
        } finally {
            stopwatch.stop();
            log.info("Finished " + schedulesCount + " " + bucket + " data aggregations in " +
                stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
        return schedulesCount.get();
    }

    protected void submitAggregationTask(Batch batch) throws InterruptedException {
        if (log.isDebugEnabled()) {
            log.debug("Scheduling " + bucket + " aggregation task for " + batch);
        }
        permits.acquire();
        aggregationTasks.submit(new AggregationTask(batch) {
            @Override
            void run(Batch batch) {
                switch (bucket) {
                    case RAW:
                        fetchRawData(batch);
                        processBatch(batch, Bucket.ONE_HOUR);
                        break;
                    case ONE_HOUR:
                        fetchData(batch, Bucket.ONE_HOUR);
                        processBatch(batch, Bucket.SIX_HOUR);
                        break;
                    default:
                        fetchData(batch, Bucket.SIX_HOUR);
                        processBatch(batch, Bucket.TWENTY_FOUR_HOUR);
                }

            }
        });
        taskTracker.addTask();
    }

    protected void fetchRawData(Batch batch) {
        List<StorageResultSetFuture> queryFutures = new ArrayList<StorageResultSetFuture>();
        for (IndexEntry indexEntry : batch) {
            queryFutures.add(dao.findRawMetricsAsync(indexEntry.getScheduleId(), batch.getStartTime().getMillis(),
                batch.getEndTime().getMillis()));
        }
        batch.setQueriesFuture(Futures.allAsList(queryFutures));
    }

    protected void fetchData(Batch batch, Bucket bucket) {
        List<StorageResultSetFuture> queryFutures = new ArrayList<StorageResultSetFuture>();
        for (IndexEntry indexEntry : batch) {
            queryFutures.add(dao.findAggregateMetricsAsync(indexEntry.getScheduleId(), bucket,
                batch.getStartTime().getMillis(), batch.getEndTime().getMillis()));
        }
        batch.setQueriesFuture(Futures.allAsList(queryFutures));
    }

    protected void processBatch(Batch batch, Bucket bucket) {
        ListenableFuture<Iterable<List<T>>> iterableFuture = Futures.transform(batch.getQueriesFuture(),
            toIterable(), aggregationTasks);

        ListenableFuture<List<AggregateNumericMetric>> metricsFuture = Futures.transform(iterableFuture,
            computeAggregates(batch.getStartTime().getMillis(), bucket), aggregationTasks);

        ListenableFuture<List<ResultSet>> insertsFuture = Futures.transform(metricsFuture, persistMetrics,
            aggregationTasks);

        ListenableFuture<List<ResultSet>> deleteIndexEntriesFuture = Futures.transform(insertsFuture,
            deleteIndexEntries(batch), aggregationTasks);

        aggregationTaskFinished(metricsFuture, deleteIndexEntriesFuture);
    }

    protected Function<List<ResultSet>, Iterable<List<T>>> toIterable() {
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
                                return resultSetMapper.mapAll(resultSetIterator.next());
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

    protected Function<Iterable<? extends Collection<T>>, List<AggregateNumericMetric>> computeAggregates(
        final long timeSlice, final Bucket bucket) {
        return new Function<Iterable<? extends Collection<T>>, List<AggregateNumericMetric>>() {
            @Override
            public List<AggregateNumericMetric> apply(Iterable<? extends Collection<T>> values) {
                List<AggregateNumericMetric> aggregates = new ArrayList<AggregateNumericMetric>(batchSize);
                for (Collection<T> metricList : values) {
                    if (metricList.isEmpty()) {
                        log.warn("Cannot compute a new " + AggregateNumericMetric.class.getSimpleName() +
                            " from an empty list. The bucket is " + bucket + " and the time slice is " + timeSlice);
                    } else {
                        aggregates.add(computeAggregate(metricList, timeSlice, bucket));
                    }
                }
                return aggregates;
            }
        };
    }

    private AggregateNumericMetric computeAggregate(Collection<T> metrics, long timeSlice, Bucket bucket) {
        Iterator<T> iterator = metrics.iterator();
        T metric = iterator.next();
        int scheduleId = metric.getScheduleId();
        Double min = metric.getMin();
        Double max = metric.getMax();
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
        mean.add(metric.getAvg());

        while (iterator.hasNext()) {
            metric = iterator.next();
            mean.add(metric.getAvg());
            if (metric.getMin() < min) {
                min = metric.getMin();
            }
            if (metric.getMax() > max) {
                max = metric.getMax();
            }
        }
        return new AggregateNumericMetric(scheduleId, bucket, mean.getArithmeticMean(), min, max, timeSlice);
    }

    protected AsyncFunction<List<ResultSet>, List<ResultSet>> deleteIndexEntries(final Batch batch) {
        return new AsyncFunction<List<ResultSet>, List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> apply(List<ResultSet> insertResultSets) throws Exception {
                List<StorageResultSetFuture> deleteFutures = new ArrayList<StorageResultSetFuture>();
                for (IndexEntry indexEntry : batch) {
                    deleteFutures.add(dao.deleteIndexEntry(indexEntry));
                }
                return Futures.allAsList(deleteFutures);
            }
        };
    }

    /**
     * AggregationTask is a Runnable that computes aggregates for a set of schedules in a {@link CacheIndexEntry}.
     * If there are any unexpected errors, e.g., a NullPointerException, aggregation will be aborted.
     */
    protected abstract class AggregationTask implements Runnable {

        private Batch batch;

        public AggregationTask(Batch batch) {
            this.batch = batch;
        }

        @Override
        public void run() {
            try {
                run(batch);
            } catch (Exception e) {
                permits.release();
                log.error("Aggregation will be aborted due to an unexpected error", e);
                taskTracker.abort("Aborting aggregation due to an unexpected error: " + e.getMessage());
            }
        }

        abstract void run(Batch batch);
    }

    protected class AggregationTaskFinishedCallback<R> implements FutureCallback<R> {
        @Override
        public void onSuccess(R args) {
            try {
                onFinish(args);
            } finally {
                permits.release();
                taskTracker.finishedTask();
                if (log.isDebugEnabled()) {
                    log.debug("There are " + taskTracker.getRemainingTasks() + " remaining " + bucket +
                        " aggregation tasks and " + permits.availablePermits() + " available permits");
                }
            }
        }

        protected void onFinish(R args) {
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("There was an error aggregating data", t);
            permits.release();
            taskTracker.finishedTask();
            if (log.isDebugEnabled()) {
                log.debug("There are " + taskTracker.getRemainingTasks() + " remaining tasks and " +
                    permits.availablePermits() + " available permits");
            }
        }
    }

}
