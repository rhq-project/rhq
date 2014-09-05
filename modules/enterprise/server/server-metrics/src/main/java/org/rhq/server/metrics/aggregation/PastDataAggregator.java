package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateNumericMetricMapper;
import org.rhq.server.metrics.domain.Bucket;
import org.rhq.server.metrics.domain.IndexEntry;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.domain.RawNumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetricMapper;

/**
 * @author John Sanda
 */
class PastDataAggregator extends BaseAggregator {

    private static final Log LOG = LogFactory.getLog(PastDataAggregator.class);

    private static final String DEBUG_TYPE = "past data";

    private PersistFunctions persistFns;

    private AtomicInteger rawSchedulesCount = new AtomicInteger();

    private AtomicInteger oneHourSchedulesCount = new AtomicInteger();

    private AtomicInteger sixHourScheduleCount = new AtomicInteger();

    void setPersistFns(PersistFunctions persistFns) {
        this.persistFns = persistFns;
    }

    @Override
    protected String getDebugType() {
        return DEBUG_TYPE;
    }

    @Override
    protected List<IndexEntry> loadIndexEntries() {
        // For now we will just search back up to 24 hours, but ultimately this will be
        // configurable.
        DateTime time = startTime.minusHours(24);
        List<IndexEntry> indexEntries = new ArrayList<IndexEntry>();

        while (time.isBefore(startTime)) {
            ResultSet resultSet = dao.findIndexEntries(MetricsTable.RAW, 0, time.getMillis()).get();
            for (Row row : resultSet) {
                indexEntries.add(new IndexEntry(MetricsTable.RAW, 0, time.getMillis(), row.getInt(0)));
            }
            time = time.plusHours(1);
        }

        return indexEntries;
    }

    @Override
    protected AggregationTask createAggregationTask(List<IndexEntry> batch) {
        return new AggregationTask(batch) {
            @Override
            void run(List<IndexEntry> batch) {
                DateTime timeSlice = null;
                List<StorageResultSetFuture> queryFutures = new ArrayList<StorageResultSetFuture>(batch.size());
                for (IndexEntry indexEntry : batch) {
                    if (timeSlice == null) {
                        timeSlice = new DateTime(indexEntry.getTimestamp());
                    }
                    queryFutures.add(dao.findRawMetricsAsync(indexEntry.getScheduleId(), indexEntry.getTimestamp(),
                        timeSlice.plusHours(1).getMillis()));
                }
                processBatch(queryFutures, timeSlice, batch);
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

    private void processBatch(List<StorageResultSetFuture> queryFutures, DateTime timeSlice,
        List<IndexEntry> indexEntries) {

        ListenableFuture<List<ResultSet>> queriesFuture = Futures.allAsList(queryFutures);

        ListenableFuture<Iterable<List<RawNumericMetric>>> iterableFuture = Futures.transform(queriesFuture,
            toIterable(new RawNumericMetricMapper()), aggregationTasks);

        ListenableFuture<List<AggregateNumericMetric>> metricsFuture = Futures.transform(iterableFuture,
            computeAggregates(timeSlice.getMillis(), RawNumericMetric.class, Bucket.ONE_HOUR),
            aggregationTasks);

        ListenableFuture<List<ResultSet>> insertsFuture = Futures.transform(metricsFuture, persistMetrics,
            aggregationTasks);

        if (dateTimeService.is6HourTimeSliceFinished(timeSlice)) {
            DateTime start = dateTimeService.get6HourTimeSlice(timeSlice);
            DateTime end = dateTimeService.get6HourTimeSliceEnd(timeSlice);

            ListenableFuture<List<ResultSet>> oneHourQueryFutures = fetchData(start, end, Bucket.ONE_HOUR,
                indexEntries);

            ListenableFuture<List<List<?>>> combinedFutures = Futures.allAsList(metricsFuture, oneHourQueryFutures);

            ListenableFuture<Iterable<Set<AggregateNumericMetric>>> aggregatesIterableFuture = Futures.transform(
                combinedFutures, aggregatesToIterable(), aggregationTasks);

            ListenableFuture<List<AggregateNumericMetric>> sixHourMetricsFuture = Futures.transform(
                aggregatesIterableFuture, computeAggregates(start.getMillis(), AggregateNumericMetric.class,
                    Bucket.SIX_HOUR));

            ListenableFuture<List<ResultSet>> sixHourInsertsFuture = Futures.transform(sixHourMetricsFuture,
                persistFns.persist6HourMetrics(), aggregationTasks);

            if (dateTimeService.is24HourTimeSliceFinished(timeSlice)) {
                start = dateTimeService.get24HourTimeSlice(timeSlice);
                end = dateTimeService.get24HourTimeSliceEnd(timeSlice);

                ListenableFuture<List<ResultSet>> sixHourQueryFutures = fetchData(start, end, Bucket.SIX_HOUR,
                    indexEntries);

                combinedFutures = Futures.allAsList(sixHourMetricsFuture, sixHourQueryFutures);

                aggregatesIterableFuture = Futures.transform(combinedFutures, aggregatesToIterable(), aggregationTasks);

                ListenableFuture<List<AggregateNumericMetric>> twentyFourHourMetricsFuture = Futures.transform(
                    aggregatesIterableFuture, computeAggregates(start.getMillis(), AggregateNumericMetric.class,
                        Bucket.TWENTY_FOUR_HOUR));

                ListenableFuture<List<ResultSet>> twentyFourHourInsertsFuture = Futures.transform(
                    twentyFourHourMetricsFuture, persistFns.persist24HourMetrics(), aggregationTasks);

                ListenableFuture<List<ResultSet>> deleteIndexFuture = Futures.transform(twentyFourHourInsertsFuture,
                    deleteIndexEntries(indexEntries), aggregationTasks);

                late6HourAggregationFinished(indexEntries.size(), deleteIndexFuture);

            } else {
                ListenableFuture<List<ResultSet>> deleteIndexFuture = Futures.transform(sixHourInsertsFuture,
                    deleteIndexEntries(indexEntries), aggregationTasks);

                late1HourAggregationFinished(indexEntries.size(), deleteIndexFuture);
            }
        } else {
            ListenableFuture<List<ResultSet>> deleteIndexEntriesFuture = Futures.transform(insertsFuture,
                deleteIndexEntries(indexEntries), aggregationTasks);

            aggregationTaskFinished(metricsFuture, deleteIndexEntriesFuture);
        }
    }

    private Function<List<List<?>>, Iterable<Set<AggregateNumericMetric>>> aggregatesToIterable() {
        return new Function<List<List<?>>, Iterable<Set<AggregateNumericMetric>>>() {
            @Override
            public Iterable<Set<AggregateNumericMetric>> apply(List<List<?>> input) {
                List<AggregateNumericMetric> newMetrics = (List<AggregateNumericMetric>) input.get(0);
                List<ResultSet> resultSets = (List<ResultSet>) input.get(1);
                final Map<Integer, Set<AggregateNumericMetric>> metricsMap =
                    new HashMap<Integer, Set<AggregateNumericMetric>>();
                AggregateNumericMetricMapper mapper = new AggregateNumericMetricMapper();

                for (ResultSet resultSet : resultSets) {
                    Set<AggregateNumericMetric> set = new HashSet<AggregateNumericMetric>();
                    List<AggregateNumericMetric> metrics = mapper.mapAll(resultSet);
                    if (!metrics.isEmpty()) {
                        set.addAll(metrics);
                        metricsMap.put(metrics.get(0).getScheduleId(), set);
                    }
                }

                for (AggregateNumericMetric newMetric : newMetrics) {
                    Set<AggregateNumericMetric> metrics;
                    if (metricsMap.containsKey(newMetric.getScheduleId())) {
                        metrics = metricsMap.get(newMetric.getScheduleId());
                    } else {
                        metrics = new HashSet<AggregateNumericMetric>();
                    }
                    metrics.add(newMetric);
                    metricsMap.put(newMetric.getScheduleId(), metrics);
                }

                return new Iterable<Set<AggregateNumericMetric>>() {
                    @Override
                    public Iterator<Set<AggregateNumericMetric>> iterator() {
                        return metricsMap.values().iterator();
                    }
                };
            }
        };
    }

    private ListenableFuture<List<ResultSet>> fetchData(DateTime start, DateTime end, Bucket bucket,
        List<IndexEntry> indexEntries) {
        List<StorageResultSetFuture> queryFutures = new ArrayList<StorageResultSetFuture>(indexEntries.size());
        for (IndexEntry indexEntry : indexEntries) {
            queryFutures.add(dao.findAggregateMetricsAsync(indexEntry.getScheduleId(), bucket, start.getMillis(),
                end.getMillis()));
        }
        return Futures.allAsList(queryFutures);
    }

    private void late1HourAggregationFinished(final int count,
        ListenableFuture<List<ResultSet>> deleteIndexEntriesFuture) {
        Futures.addCallback(deleteIndexEntriesFuture, new AggregationTaskFinishedCallback<List<ResultSet>>() {
            @Override
            protected void onFinish(List<ResultSet> deleteResultSets) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Finished aggregating late raw and 1 hour data for " + count + " schedules");
                }

                rawSchedulesCount.addAndGet(count);
                oneHourSchedulesCount.addAndGet(count);
            }
        }, aggregationTasks);
    }

    private void late6HourAggregationFinished(final int count,
        ListenableFuture<List<ResultSet>> deleteIndexEntriesFuture) {
        Futures.addCallback(deleteIndexEntriesFuture, new AggregationTaskFinishedCallback<List<ResultSet>>() {
            @Override
            protected void onFinish(List<ResultSet> deleteResultSets) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Finished aggregating late raw, 1 hour, and 6 hour data for " + count + " schedules");
                }

                rawSchedulesCount.addAndGet(count);
                oneHourSchedulesCount.addAndGet(count);
                sixHourScheduleCount.addAndGet(count);
            }
        }, aggregationTasks);
    }

}
