package org.rhq.server.metrics.aggregation;

import static org.rhq.server.metrics.domain.MetricsTable.ONE_HOUR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

import org.rhq.server.metrics.AbortedException;
import org.rhq.server.metrics.ArithmeticMeanCalculator;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.CacheIndexEntry;
import org.rhq.server.metrics.domain.CacheIndexEntryMapper;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.domain.NumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetricMapper;

/**
 * @author John Sanda
 */
class PastDataAggregator {

    private static final Log LOG = LogFactory.getLog(PastDataAggregator.class);

    private static final int PAST_DATA_BATCH_SIZE = 5;

    private DateTime startingDay;

    private DateTime currentDay;

    private DateTime startTime;

    private MetricsDAO dao;

    private ListeningExecutorService aggregationTasks;

    private Semaphore permits;

    private DateTimeService dateTimeService;

    void setStartingDay(DateTime startingDay) {
        this.startingDay = startingDay;
    }

    void setCurrentDay(DateTime currentDay) {
        this.currentDay = currentDay;
    }

    void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    void setDao(MetricsDAO dao) {
        this.dao = dao;
    }

    void setAggregationTasks(ListeningExecutorService aggregationTasks) {
        this.aggregationTasks = aggregationTasks;
    }

    void setPermits(Semaphore permits) {
        this.permits = permits;
    }

    void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }

    public void execute() throws InterruptedException, AbortedException {
        final TaskTracker taskTracker = new TaskTracker();
        Futures.addCallback(findPastIndexEntries(), new FutureCallback<List<CacheIndexEntry>>() {
            @Override
            public void onSuccess(List<CacheIndexEntry> pastIndexEntries) {
                scheduleLateDataAggregationTasks(taskTracker, pastIndexEntries);
            }

            @Override
            public void onFailure(Throwable t) {
                taskTracker.abort("There was an error fetching late cache index entries: " + t.getMessage());
            }
        });
        taskTracker.waitForTasksToFinish();
    }

    private ListenableFuture<List<CacheIndexEntry>> findPastIndexEntries() {
        List<ListenableFuture<ResultSet>> insertFutures = new ArrayList<ListenableFuture<ResultSet>>();
        DateTime day = startingDay;

        while (day.isBefore(currentDay)) {
            insertFutures.add(dao.findPastCacheIndexEntriesBeforeToday(MetricsTable.RAW, day.getMillis(),
                AggregationManager.INDEX_PARTITION, day.plusHours(startTime.getHourOfDay()).getMillis()));
            day = day.plusDays(1);
        }
        insertFutures.add(dao.findPastCacheIndexEntriesFromToday(MetricsTable.RAW, currentDay.getMillis(),
            AggregationManager.INDEX_PARTITION, startTime.getMillis()));

        ListenableFuture<List<ResultSet>> insertsFuture = Futures.successfulAsList(insertFutures);
        return Futures.transform(insertsFuture, new Function<List<ResultSet>, List<CacheIndexEntry>>() {
            @Override
            public List<CacheIndexEntry> apply(List<ResultSet> resultSets) {
                CacheIndexEntryMapper mapper = new CacheIndexEntryMapper();
                List<CacheIndexEntry> indexEntries = new ArrayList<CacheIndexEntry>();

                for (ResultSet resultSet : resultSets) {
                    indexEntries.addAll(mapper.map(resultSet));
                }

                return indexEntries;
            }
        }, aggregationTasks);
    }

    private void scheduleLateDataAggregationTasks(TaskTracker taskTracker, List<CacheIndexEntry> pastIndexEntries) {
        try {
            List<StorageResultSetFuture> queryFutures = new ArrayList<StorageResultSetFuture>(PAST_DATA_BATCH_SIZE);
            CacheIndexEntry lastIndexEntry = null;

            for (CacheIndexEntry indexEntry : pastIndexEntries) {
                lastIndexEntry = indexEntry;

                // create task to aggregate past data
                // Note that if indexEntry.scheduleIds is empty than we can pull
                // data from metrics_cache and schedule a ProcessBatch task.

                if (indexEntry.getScheduleIds().isEmpty()) {
                    // TODO schedule task to aggregate data in cache block
                } else {
                    for (Integer scheduleId : indexEntry.getScheduleIds()) {
                        permits.acquire();
                        queryFutures.add(dao.findRawMetricsAsync(scheduleId, indexEntry.getCollectionTimeSlice(),
                            new DateTime(indexEntry.getCollectionTimeSlice()).plusHours(1).getMillis()));
                        if (queryFutures.size() == PAST_DATA_BATCH_SIZE) {
                            processBatch(taskTracker, queryFutures, indexEntry);
                            queryFutures = new ArrayList<StorageResultSetFuture>(PAST_DATA_BATCH_SIZE);
                        }
                    }
                }
            }
            taskTracker.finishedSchedulingTasks();
            if (!queryFutures.isEmpty()) {
                processBatch(taskTracker, queryFutures, lastIndexEntry);
            }
        } catch (InterruptedException e) {
            LOG.warn("There was an interrupt while processing past data.", e);
            taskTracker.abort("There was an interrupt while processing past data.");
        }
    }

    private void processBatch(TaskTracker taskTracker, List<StorageResultSetFuture> queryFutures,
        CacheIndexEntry indexEntry) {
        taskTracker.addTask();
        ListenableFuture<List<ResultSet>> queriesFuture = Futures.successfulAsList(queryFutures);
        ListenableFuture<List<NumericMetric>> metricsFuture = Futures.transform(queriesFuture,
            computeAggregates(indexEntry.getCollectionTimeSlice()), aggregationTasks);
        ListenableFuture<List<ResultSet>> insertsFuture = Futures.transform(metricsFuture,
            persist1HourMetrics(indexEntry.getStartScheduleId()), aggregationTasks);
        ListenableFuture<ResultSet> deleteCacheFuture = Futures.transform(insertsFuture,
            deleteCacheEntry(indexEntry), aggregationTasks);
        ListenableFuture<ResultSet> deleteCacheIndexFuture = Futures.transform(deleteCacheFuture,
            deleteCacheIndexEntry(indexEntry), aggregationTasks);
        Futures.addCallback(deleteCacheIndexFuture, batchFinished(taskTracker, queryFutures.size()));
    }

    private Function<List<ResultSet>, List<NumericMetric>> computeAggregates(final long timeSlice) {
        return new Function<List<ResultSet>, List<NumericMetric>>() {
            @Override
            public List<NumericMetric> apply(List<ResultSet> resultSets) {
                List<NumericMetric> metrics = new ArrayList<NumericMetric>(resultSets.size());
                for (ResultSet resultSet : resultSets) {
                    if (!resultSet.isExhausted()) {
                        metrics.add(computeAggregate(resultSet, timeSlice));
                    }
                }

                return metrics;
            }
        };
    }

    private AggregateNumericMetric computeAggregate(ResultSet resultSet, long timeSlice) {
        RawNumericMetricMapper mapper = new RawNumericMetricMapper();
        List<? extends NumericMetric> metrics = mapper.mapAll(resultSet);
        Double min = Double.NaN;
        Double max = Double.NaN;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
        int scheduleId = 0;

        for (NumericMetric metric : metrics) {
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

    private AsyncFunction<List<NumericMetric>, List<ResultSet>> persist1HourMetrics(final int startScheduleId) {
        return new AsyncFunction<List<NumericMetric>, List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> apply(List<NumericMetric> metrics) throws Exception {
                List<StorageResultSetFuture> futures = new ArrayList<StorageResultSetFuture>(metrics.size() * 3);
                for (NumericMetric metric : metrics) {
                    futures.add(dao.insertOneHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                        AggregateType.MAX, metric.getMax()));
                    futures.add(dao.insertOneHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                        AggregateType.MIN, metric.getMin()));
                    futures.add(dao.insertOneHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                        AggregateType.AVG, metric.getAvg()));
                    futures.add(dao.updateMetricsCache(ONE_HOUR, dateTimeService.get6HourTimeSlice(startTime).getMillis(),
                        startScheduleId, metric.getScheduleId(), metric.getTimestamp(), toMap(metric)));
                    futures.add(dao.updateCacheIndex(ONE_HOUR, dateTimeService.get24HourTimeSlice(startTime).getMillis(),
                        AggregationManager.INDEX_PARTITION, dateTimeService.get6HourTimeSlice(startTime).getMillis(),
                        startScheduleId));
                }
                return Futures.successfulAsList(futures);
            }
        };
    }

    private Map<Integer, Double> toMap(NumericMetric metric) {
        return ImmutableMap.of(
            AggregateType.MAX.ordinal(), metric.getMax(),
            AggregateType.MIN.ordinal(), metric.getMin(),
            AggregateType.AVG.ordinal(), metric.getAvg()
        );
    }

    private AsyncFunction<List<ResultSet>, ResultSet> deleteCacheEntry(final CacheIndexEntry indexEntry) {
        return new AsyncFunction<List<ResultSet>, ResultSet>() {
            @Override
            public ListenableFuture<ResultSet> apply(List<ResultSet> resultSets) throws Exception {
                return dao.deleteCacheEntries(MetricsTable.RAW, indexEntry.getCollectionTimeSlice(),
                    indexEntry.getStartScheduleId());
            }
        };
    }

    private AsyncFunction<ResultSet, ResultSet> deleteCacheIndexEntry(final CacheIndexEntry indexEntry) {
        return new AsyncFunction<ResultSet, ResultSet>() {
            @Override
            public ListenableFuture<ResultSet> apply(ResultSet deleteCacheResultSet) throws Exception {
                return dao.deleteCacheIndexEntries(MetricsTable.RAW, indexEntry.getInsertTimeSlice(),
                    indexEntry.getPartition(), indexEntry.getCollectionTimeSlice(), indexEntry.getStartScheduleId());
            }
        };
    }

    private FutureCallback<ResultSet> batchFinished(final TaskTracker taskTracker, final int numSchedules) {
        return new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet result) {
                permits.release(numSchedules);
                taskTracker.finishedTask();
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("There was an error aggregating data", t);
                permits.release(numSchedules);
                taskTracker.finishedTask();
            }
        };
    }
}
