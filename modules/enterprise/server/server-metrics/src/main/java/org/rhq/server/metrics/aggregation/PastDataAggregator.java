package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.HashSet;
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

    private static final String DEBUG_TYPE = "past data";

    private DateTime startingDay;

    private DateTime currentDay;

    private PersistFunctions persistFns;

    private AtomicInteger rawSchedulesCount = new AtomicInteger();

    private AtomicInteger oneHourSchedulesCount = new AtomicInteger();

    private AtomicInteger sixHourScheduleCount = new AtomicInteger();

    void setStartingDay(DateTime startingDay) {
        this.startingDay = startingDay;
    }

    void setCurrentDay(DateTime currentDay) {
        this.currentDay = currentDay;
    }

    void setPersistFns(PersistFunctions persistFns) {
        this.persistFns = persistFns;
    }

    @Override
    protected String getDebugType() {
        return DEBUG_TYPE;
    }

    /**
     * We store a configurable amount of past data where the amount is specified as a duration in days. Suppose that the
     * duration is set at 4 days, and the current time is 14:00 Friday. This method will query the index as far back
     * as 14:00 on Monday, and each day up to the current time slice of today will be queried for past data.
     *
     * @return The past cache index entries
     */
    @Override
    protected ListenableFuture<List<CacheIndexEntry>> findIndexEntries() {
        List<ListenableFuture<ResultSet>> insertFutures = new ArrayList<ListenableFuture<ResultSet>>();
        DateTime day = startingDay;

        while (day.isBefore(currentDay)) {
            insertFutures.add(dao.findPastCacheIndexEntriesBeforeToday(MetricsTable.RAW, day.getMillis(),
                AggregationManager.INDEX_PARTITION, day.plusHours(startTime.getHourOfDay()).getMillis()));
            day = day.plusDays(1);
        }
        insertFutures.add(dao.findPastCacheIndexEntriesFromToday(MetricsTable.RAW, currentDay.getMillis(),
            AggregationManager.INDEX_PARTITION, startTime.getMillis()));

        ListenableFuture<List<ResultSet>> insertsFuture = Futures.allAsList(insertFutures);
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

    @Override
    protected Iterable<CacheIndexEntry> reduceIndexEntries(final List<CacheIndexEntry> indexEntries) {
        final PeekingIterator<CacheIndexEntry> iterator = Iterators.peekingIterator(indexEntries.iterator());

        return new Iterable<CacheIndexEntry>() {
            @Override
            public Iterator<CacheIndexEntry> iterator() {
                return new Iterator<CacheIndexEntry>() {

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public CacheIndexEntry next() {
                        CacheIndexEntry current = iterator.next();
                        List<CacheIndexEntry> currentEntries = new ArrayList<CacheIndexEntry>();
                        currentEntries.add(current);

                        // We want to group all index entries having the same startScheduleId and the same
                        // collectionTimeSlice. It is possible to have more than one index entry for a
                        // {collectionTimeSlice, startScheduleId} pair. For example, suppose we have an entry with
                        // collectionTimeSlice and insertTimeSlice 14:00. Aggregation fails and the index entry remains
                        // intact. Then at 15:10 we insert some late data such that we have another index entry where
                        // collectionTimeSlice is 14:00 and insertTimeSlice is 15:00.
                        while (iterator.hasNext() && isSameCollectionTimeSliceStartScheduleIdPair(current,
                            iterator.peek())) {

                            current = iterator.next();
                            currentEntries.add(current);
                        }

                        if (isDataInCache(currentEntries)) {
                            return combineEntries(currentEntries, current.getCollectionTimeSlice());
                        } else {
                            return combineEntries(currentEntries, 0);
                        }
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    private boolean isSameCollectionTimeSliceStartScheduleIdPair(CacheIndexEntry left, CacheIndexEntry right) {
        return left.getCollectionTimeSlice() == right.getCollectionTimeSlice() &&
            left.getStartScheduleId() == right.getStartScheduleId();
    }

    /**
     * This method determines whether or not all of the raw data for a {collectionTimeSlice, startScheduleId} pair is
     * available in the metrics_cache table. If there is at least one entry having {@link CacheIndexEntry#getCollectionTimeSlice() collectionTimeSlice}
     * the same as {@link CacheIndexEntry#getInsertTimeSlice() insertTimeSlice} then we know that the data is available
     * in metrics_cache. If this condition is satisfied, then it means that the cache partition was created during the
     * collection time slice, and it has not been deleted; thus, all raw data for the
     * {collectionTimeSlice, startScheduleId} is available in metrics_cache.
     *
     * @param indexEntries The index entries which should all have the same {@link CacheIndexEntry#getCollectionTimeSlice() collectionTimeSlice}
     *                     and the same {@link CacheIndexEntry#getStartScheduleId() startScheduleId}
     * @return True if all of the raw data for the collection time slice is available in the metrics_cache table, false
     * otherwise.
     */
    private boolean isDataInCache(List<CacheIndexEntry> indexEntries) {
        for (CacheIndexEntry indexEntry : indexEntries) {
            if (indexEntry.getCollectionTimeSlice() == indexEntry.getInsertTimeSlice()) {
                return true;
            }
        }
        return false;
    }

    private CacheIndexEntry combineEntries(List<CacheIndexEntry> indexEntries, long insertTimeSlice) {
        CacheIndexEntry combinedEntry = new CacheIndexEntry();
        combinedEntry.setBucket(MetricsTable.RAW);
        combinedEntry.setDay(indexEntries.get(0).getDay());
        combinedEntry.setStartScheduleId(indexEntries.get(0).getStartScheduleId());
        combinedEntry.setCollectionTimeSlice(indexEntries.get(0).getCollectionTimeSlice());
        combinedEntry.setInsertTimeSlice(insertTimeSlice);
        combinedEntry.setScheduleIds(new HashSet<Integer>());

        for (CacheIndexEntry indexEntry : indexEntries) {
            combinedEntry.getScheduleIds().addAll(indexEntry.getScheduleIds());
        }

        return combinedEntry;
    }

    @Override
    protected AggregationTask createAggregationTask(CacheIndexEntry indexEntry) {
        return new AggregationTask(indexEntry) {
            @Override
            public void run(CacheIndexEntry indexEntry) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Executing " + getDebugType() + " aggregation task for " + indexEntry);
                }

                if (indexEntry.getCollectionTimeSlice() == indexEntry.getInsertTimeSlice()) {
                    StorageResultSetFuture cacheFuture = dao.findCacheEntriesAsync(aggregationType.getCacheTable(),
                        indexEntry.getCollectionTimeSlice(), indexEntry.getStartScheduleId());
                    processRawDataCacheBlock(indexEntry, cacheFuture);
                } else {
                    List<StorageResultSetFuture> queryFutures = new ArrayList<StorageResultSetFuture>(PAST_DATA_BATCH_SIZE);
                    for (Integer scheduleId : indexEntry.getScheduleIds()) {
                        queryFutures.add(dao.findRawMetricsAsync(scheduleId, indexEntry.getCollectionTimeSlice(),
                            new DateTime(indexEntry.getCollectionTimeSlice()).plusHours(1).getMillis()));
                        if (queryFutures.size() == PAST_DATA_BATCH_SIZE) {
                            processBatch(queryFutures, indexEntry);
                            queryFutures = new ArrayList<StorageResultSetFuture>(PAST_DATA_BATCH_SIZE);
                        }
                    }
                    if (!queryFutures.isEmpty()) {
                        processBatch(queryFutures, indexEntry);
                    }
                }
            }
        };
    }

    @Override
    protected Map<AggregationType, Integer> getAggregationCounts() {
        return ImmutableMap.of(
            AggregationType.RAW, rawSchedulesCount.get(),
            AggregationType.ONE_HOUR, oneHourSchedulesCount.get(),
            AggregationType.SIX_HOUR, sixHourScheduleCount.get()
        );
    }

    /**
     * <p>
     * This method provides the core aggregation logic for aggregating past that is pulled from the raw_metrics table
     * and not the cache table. It performs the following steps:
     *
     * <ul>
     *   <li>Iterate over the query result sets (from the raw_metrics table)</li>
     *   <li>Compute aggregate metrics</li>
     *   <li>Persist aggregate metrics</li>
     *   <li>Aggregate 1 hour data if the 6 hour time slice has finished</li>
     *   <li>Aggregate 6 hour data if the 24 hour time slice has finished</li>
     *   <li>Delete the cache partition</li>
     *   <li>Delete the cache index row</li>
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
     * @param queryFutures Futures of the raw data result sets
     * @param indexEntry The index entry for which data is being aggregated
     */
    private void processBatch(List<StorageResultSetFuture> queryFutures, CacheIndexEntry indexEntry) {

        ListenableFuture<List<ResultSet>> queriesFuture = Futures.allAsList(queryFutures);

        ListenableFuture<Iterable<List<RawNumericMetric>>> iterableFuture = Futures.transform(queriesFuture,
            toIterable(new RawNumericMetricMapper()), aggregationTasks);

        ListenableFuture<List<AggregateNumericMetric>> metricsFuture = Futures.transform(iterableFuture,
            computeAggregates(indexEntry.getCollectionTimeSlice(), RawNumericMetric.class), aggregationTasks);

        ListenableFuture<IndexAggregatesPair> pairFuture = Futures.transform(metricsFuture,
            indexAggregatesPair(indexEntry));

        boolean is6HourTimeSliceFinished = dateTimeService.is6HourTimeSliceFinished(
            indexEntry.getCollectionTimeSlice());
        boolean is24HourTimeSliceFinished = dateTimeService.is24HourTimeSliceFinished(
            indexEntry.getCollectionTimeSlice());
        ListenableFuture<List<ResultSet>> oneHourInsertsFuture;
        ListenableFuture<List<ResultSet>> insertsFuture;

        if (is6HourTimeSliceFinished) {
            oneHourInsertsFuture = Futures.transform(pairFuture, persistFns.persist1HourMetrics(), aggregationTasks);

            MetricsFuturesPair sixHourFuturesPair = process1HourData(indexEntry,
                proceedWithMetricsAfterInserts(new MetricsFuturesPair(oneHourInsertsFuture, metricsFuture)));

            if (is24HourTimeSliceFinished) {
                MetricsFuturesPair twentyFourHourFuturesPair = process6HourData(indexEntry,
                    proceedWithMetricsAfterInserts(sixHourFuturesPair));
                insertsFuture = twentyFourHourFuturesPair.resultSetsFuture;
            } else {
                insertsFuture = sixHourFuturesPair.resultSetsFuture;
            }
        } else {
            oneHourInsertsFuture = Futures.transform(pairFuture, persistFns.persist1HourMetricsAndUpdateCache(),
                aggregationTasks);

            insertsFuture = oneHourInsertsFuture;
        }

        ListenableFuture<ResultSet> deleteCacheFuture = Futures.transform(insertsFuture,
            deleteCacheEntry(indexEntry), aggregationTasks);

        ListenableFuture<ResultSet> deleteCacheIndexFuture = Futures.transform(deleteCacheFuture,
            deleteCacheIndexEntries(indexEntry), aggregationTasks);

        aggregationTaskFinished(deleteCacheIndexFuture, pairFuture, is6HourTimeSliceFinished, is24HourTimeSliceFinished);
    }

    /**
     * <p>
     * This method provides the core aggregation logic for aggregating past that is pulled from the metrics_cache table
     * and not the raw_metrics table. It performs the following steps:
     *
     * <ul>
     *   <li>Iterate over the query result set from the metrics_cache table which may contain data for multiple
     *   schedules</li>
     *   <li>Compute aggregate metrics</li>
     *   <li>Persist aggregate metrics</li>
     *   <li>Aggregate 1 hour data if the 6 hour time slice has finished</li>
     *   <li>Aggregate 6 hour data if the 24 hour time slice has finished</li>
     *   <li>Delete the cache partition</li>
     *   <li>Delete the cache index row</li>
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
     * @param queryFutures Futures of the raw data result sets
     * @param indexEntry The index entry for which data is being aggregated
     */
    @SuppressWarnings("unchecked")
    protected void processRawDataCacheBlock(CacheIndexEntry indexEntry, StorageResultSetFuture cacheFuture) {

        ListenableFuture<Iterable<List<RawNumericMetric>>> iterableFuture = Futures.transform(cacheFuture,
            toIterable(aggregationType.getCacheMapper()), aggregationTasks);

        ListenableFuture<List<AggregateNumericMetric>> metricsFuture = Futures.transform(iterableFuture,
            computeAggregates(indexEntry.getCollectionTimeSlice(), RawNumericMetric.class), aggregationTasks);

        ListenableFuture<IndexAggregatesPair> pairFuture = Futures.transform(metricsFuture,
            indexAggregatesPair(indexEntry));

        boolean is6HourTimeSliceFinished = dateTimeService.is6HourTimeSliceFinished(
            indexEntry.getCollectionTimeSlice());
        boolean is24HourTimeSliceFinished = dateTimeService.is24HourTimeSliceFinished(
            indexEntry.getCollectionTimeSlice());
        ListenableFuture<List<ResultSet>> oneHourInsertsFuture;
        ListenableFuture<List<ResultSet>> insertsFuture;

        if (is6HourTimeSliceFinished) {
            oneHourInsertsFuture = Futures.transform(pairFuture, persistFns.persist1HourMetrics(), aggregationTasks);

            MetricsFuturesPair sixHourFuturesPair = process1HourData(indexEntry,
                proceedWithMetricsAfterInserts(new MetricsFuturesPair(oneHourInsertsFuture, metricsFuture)));

            if (is24HourTimeSliceFinished) {
                MetricsFuturesPair twentyFourHourFuturesPair = process6HourData(indexEntry,
                    proceedWithMetricsAfterInserts(sixHourFuturesPair));
                insertsFuture = twentyFourHourFuturesPair.resultSetsFuture;
            } else {
                insertsFuture = sixHourFuturesPair.resultSetsFuture;
            }
        } else {
            oneHourInsertsFuture = Futures.transform(pairFuture, persistFns.persist1HourMetricsAndUpdateCache(),
                aggregationTasks);

            insertsFuture = oneHourInsertsFuture;
        }

        ListenableFuture<ResultSet> deleteCacheFuture = Futures.transform(insertsFuture,
            deleteCacheEntry(indexEntry), aggregationTasks);

        ListenableFuture<ResultSet> deleteCacheIndexFuture = Futures.transform(deleteCacheFuture,
            deleteCacheIndexEntries(indexEntry), aggregationTasks);

        aggregationTaskFinished(deleteCacheIndexFuture, pairFuture, is6HourTimeSliceFinished, is24HourTimeSliceFinished);
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

    private Function<List<CombinedMetricsPair>, Iterable<List<AggregateNumericMetric>>> toIterable() {
        return new Function<List<CombinedMetricsPair>, Iterable<List<AggregateNumericMetric>>>() {
            @Override
            public Iterable<List<AggregateNumericMetric>> apply(final List<CombinedMetricsPair> pairs) {
                return new Iterable<List<AggregateNumericMetric>>() {
                    @Override
                    public Iterator<List<AggregateNumericMetric>> iterator() {
                        return new CombinedMetricsIterator(pairs);
                    }
                };
            }
        };
    }

    protected AsyncFunction<ResultSet, ResultSet> deleteCacheIndexEntries(final CacheIndexEntry indexEntry) {

        return new AsyncFunction<ResultSet, ResultSet>() {
            @Override
            public ListenableFuture<ResultSet> apply(ResultSet deleteCacheResultSet) throws Exception {
                return dao.deleteCacheIndexEntries(aggregationType.getCacheTable(), indexEntry.getDay(),
                    indexEntry.getPartition(), indexEntry.getCollectionTimeSlice(), indexEntry.getStartScheduleId());
            }
        };
    }

    @SuppressWarnings("unchecked")
    private void aggregationTaskFinished(ListenableFuture<ResultSet> deleteCacheIndexFuture,
        ListenableFuture<IndexAggregatesPair> pairFuture, final boolean oneHourDataAggregated,
        final boolean sixHourDataAggregated) {

        final ListenableFuture<List<Object>> argsFuture = Futures.allAsList(deleteCacheIndexFuture, pairFuture);

        Futures.addCallback(argsFuture, new AggregationTaskFinishedCallback<List<Object>>() {
            @Override
            protected void onFinish(List<Object> args) {
                IndexAggregatesPair pair = (IndexAggregatesPair) args.get(1);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Finished batch for " + pair.cacheIndexEntry);
                }

                rawSchedulesCount.addAndGet(pair.metrics.size());

                if (oneHourDataAggregated) {
                    oneHourSchedulesCount.addAndGet(pair.metrics.size());
                }

                if (sixHourDataAggregated) {
                    sixHourScheduleCount.addAndGet(pair.metrics.size());
                }
            }
        }, aggregationTasks);
    }

    private MetricsFuturesPair process1HourData(CacheIndexEntry indexEntry,
        ListenableFuture<List<AggregateNumericMetric>> metricsFuture) {

        if (LOG.isTraceEnabled()) {
            LOG.trace("Processing 1 hour data for " + indexEntry);
        }

        DateTime sixHourTimeSlice = dateTimeService.get6HourTimeSlice(new DateTime(indexEntry.getCollectionTimeSlice()));

        boolean is24HourTimeSliceFinished = dateTimeService.is24HourTimeSliceFinished(new DateTime(
            indexEntry.getCollectionTimeSlice()));

        ListenableFuture<List<CombinedMetricsPair>> pairFutures = Futures.transform(metricsFuture,
            fetch1HourData(sixHourTimeSlice), aggregationTasks);

        ListenableFuture<Iterable<List<AggregateNumericMetric>>> iterableFuture = Futures.transform(pairFutures,
            toIterable(), aggregationTasks);

        ListenableFuture<List<AggregateNumericMetric>> sixHourMetricsFuture = Futures.transform(iterableFuture,
            computeAggregates(sixHourTimeSlice.getMillis(), AggregateNumericMetric.class), aggregationTasks);

        ListenableFuture<IndexAggregatesPair> pairFuture = Futures.transform(sixHourMetricsFuture,
            indexAggregatesPair(indexEntry));

        ListenableFuture<List<ResultSet>> insertsFuture;
        if (is24HourTimeSliceFinished) {
            insertsFuture = Futures.transform(pairFuture, persistFns.persist6HourMetrics(), aggregationTasks);
        } else {
            insertsFuture = Futures.transform(pairFuture, persistFns.persist6HourMetricsAndUpdateCache(),
                aggregationTasks);
        }

        return new MetricsFuturesPair(insertsFuture, sixHourMetricsFuture);
    }

    private MetricsFuturesPair process6HourData(CacheIndexEntry indexEntry,
        ListenableFuture<List<AggregateNumericMetric>> sixHourMetricsFuture) {

        if (LOG.isTraceEnabled()) {
            LOG.trace("Processing 6 hour data for " + indexEntry);
        }

        DateTime timeSlice = dateTimeService.get24HourTimeSlice(indexEntry.getCollectionTimeSlice());

        ListenableFuture<List<CombinedMetricsPair>> pairFutures = Futures.transform(sixHourMetricsFuture,
            fetch6HourData(timeSlice));

        ListenableFuture<Iterable<List<AggregateNumericMetric>>> iterableFuture = Futures.transform(pairFutures,
            toIterable(), aggregationTasks);

        ListenableFuture<List<AggregateNumericMetric>> twentyFourHourMetricsFuture = Futures.transform(iterableFuture,
            computeAggregates(timeSlice.getMillis(), AggregateNumericMetric.class), aggregationTasks);

        ListenableFuture<IndexAggregatesPair> pairFuture = Futures.transform(twentyFourHourMetricsFuture,
            indexAggregatesPair(indexEntry));

        ListenableFuture<List<ResultSet>> insertsFuture = Futures.transform(pairFuture,
            persistFns.persist24HourMetrics(), aggregationTasks);

        return new MetricsFuturesPair(insertsFuture, twentyFourHourMetricsFuture);
    }

    /**
     * <p>
     * This method is intended for use when aggregating past data and 6 hour and 24 hour data need to be recomputed. It
     * serves two purposes. First, it ensures computations proceed only after the necessary writes complete
     * successfully and makes the written data (which is still in memory) available through <code>metricsFuture</code>.
     * </p>
     * <p>
     * See {@link CombinedMetricsPair} and {@link CombinedMetricsIterator} for details on why it is important to use
     * the data still sitting in memory.
     * </p>
     *
     * @param pair A container for the future of the inserts of 1 hour or 6 hour data that was just aggregated coupled
     *             with the future of the in memory aggregate metrics just inserted.
     *
     * @return A future of the inserted aggregate data. Note that if any of the inserts fail, then any subsequent
     * functions that using the returned future as input, will not be executed.
     */
    @SuppressWarnings("unchecked")
    private ListenableFuture<List<AggregateNumericMetric>> proceedWithMetricsAfterInserts(MetricsFuturesPair pair) {

        final ListenableFuture<List<List<?>>> futures = Futures.allAsList(pair.resultSetsFuture, pair.metricsFuture);
        return Futures.transform(futures, new Function<List<List<?>>, List<AggregateNumericMetric>>() {
            @Override
            public List<AggregateNumericMetric> apply(List<List<?>> input) {
                return (List<AggregateNumericMetric>) input.get(1);
            }
        });
    }

    private AsyncFunction<List<AggregateNumericMetric>, List<CombinedMetricsPair>> fetch1HourData(
        final DateTime timeSliceStart) {

        return new AsyncFunction<List<AggregateNumericMetric>, List<CombinedMetricsPair>>() {

            final DateTime timeSliceEnd = dateTimeService.get6HourTimeSliceEnd(timeSliceStart);

            @Override
            public ListenableFuture<List<CombinedMetricsPair>> apply(List<AggregateNumericMetric> metrics) {
                List<ListenableFuture<CombinedMetricsPair>> pairFutures =
                    new ArrayList<ListenableFuture<CombinedMetricsPair>>();

                for (AggregateNumericMetric metric : metrics) {
                    StorageResultSetFuture queryFuture = dao.findOneHourMetricsAsync(metric.getScheduleId(),
                        timeSliceStart.getMillis(), timeSliceEnd.getMillis());
                    ListenableFuture<CombinedMetricsPair> pairFuture = Futures.transform(queryFuture,
                        combineMetrics(metric));
                    pairFutures.add(pairFuture);
                }

                return Futures.allAsList(pairFutures);
            }
        };
    }

    private AsyncFunction<List<AggregateNumericMetric>, List<CombinedMetricsPair>> fetch6HourData(
        final DateTime timeSliceStart) {

        final DateTime timeSliceEnd = dateTimeService.get24HourTimeSliceEnd(timeSliceStart);

        return new AsyncFunction<List<AggregateNumericMetric>, List<CombinedMetricsPair>>() {
            @Override
            public ListenableFuture<List<CombinedMetricsPair>> apply(List<AggregateNumericMetric> metrics)
                throws Exception {
                List<ListenableFuture<CombinedMetricsPair>> pairFutures =
                    new ArrayList<ListenableFuture<CombinedMetricsPair>>();

                for (AggregateNumericMetric metric : metrics) {
                    StorageResultSetFuture queryFuture = dao.findSixHourMetricsAsync(metric.getScheduleId(),
                        timeSliceStart.getMillis(), timeSliceEnd.getMillis());
                    ListenableFuture<CombinedMetricsPair> pairFuture = Futures.transform(queryFuture,
                        combineMetrics(metric));
                    pairFutures.add(pairFuture);
                }

                return Futures.allAsList(pairFutures);
            }
        };
    }

    private Function<ResultSet, CombinedMetricsPair> combineMetrics(final AggregateNumericMetric metric) {
        return new Function<ResultSet, CombinedMetricsPair>() {
            @Override
            public CombinedMetricsPair apply(ResultSet resultSet) {
                return new CombinedMetricsPair(resultSet, metric);
            }
        };
    }

}
