package org.rhq.server.metrics.aggregation;

import static org.rhq.server.metrics.domain.MetricsTable.ONE_HOUR;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.AbortedException;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.CacheIndexEntry;
import org.rhq.server.metrics.domain.CacheIndexEntryMapper;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.domain.NumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetricMapper;
import org.rhq.server.metrics.domain.ResultSetMapper;

/**
 * @author John Sanda
 */
class PastDataAggregator extends BaseAggregator {

    private static final Log LOG = LogFactory.getLog(PastDataAggregator.class);

    private DateTime startingDay;

    private DateTime currentDay;

    void setStartingDay(DateTime startingDay) {
        this.startingDay = startingDay;
    }

    void setCurrentDay(DateTime currentDay) {
        this.currentDay = currentDay;
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

        ListenableFuture<Iterable<List<RawNumericMetric>>> iterableFuture = Futures.transform(queriesFuture,
            toIterable(new RawNumericMetricMapper()), aggregationTasks);

        ListenableFuture<List<AggregateNumericMetric>> metricsFuture = Futures.transform(iterableFuture,
            computeAggregates(indexEntry.getCollectionTimeSlice(), RawNumericMetric.class), aggregationTasks);

        ListenableFuture<IndexAggregatesPair> pairFuture = Futures.transform(metricsFuture,
            indexAggregatesPair(indexEntry));

        ListenableFuture<List<ResultSet>> insertsFuture = Futures.transform(pairFuture, persistMetrics, aggregationTasks);

        ListenableFuture<ResultSet> deleteCacheFuture = Futures.transform(insertsFuture,
            deleteCacheEntry(indexEntry), aggregationTasks);

        ListenableFuture<ResultSet> deleteCacheIndexFuture = Futures.transform(deleteCacheFuture,
            deleteCacheIndexEntry(indexEntry), aggregationTasks);

        Futures.addCallback(deleteCacheIndexFuture, batchFinished(taskTracker, queryFutures.size()), aggregationTasks);
    }

    private <T extends NumericMetric> Function<List<ResultSet>, Iterable<List<T>>> toIterable(
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

    private AsyncFunction<List<AggregateNumericMetric>, List<ResultSet>> persist1HourMetrics(final int startScheduleId) {
        return new AsyncFunction<List<AggregateNumericMetric>, List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> apply(List<AggregateNumericMetric> metrics) throws Exception {
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
