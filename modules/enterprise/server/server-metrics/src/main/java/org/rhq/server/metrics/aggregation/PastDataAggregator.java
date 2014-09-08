package org.rhq.server.metrics.aggregation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateNumericMetricMapper;
import org.rhq.server.metrics.domain.Bucket;
import org.rhq.server.metrics.domain.IndexBucket;
import org.rhq.server.metrics.domain.IndexEntry;
import org.rhq.server.metrics.domain.RawNumericMetric;
import org.rhq.server.metrics.domain.RawNumericMetricMapper;

/**
 * @author John Sanda
 */
class PastDataAggregator extends DataAggregator {

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
    protected Map<AggregationType, Integer> getAggregationCounts() {
        return ImmutableMap.of(
            AggregationType.RAW, rawSchedulesCount.get(),
            AggregationType.ONE_HOUR, oneHourSchedulesCount.get(),
            AggregationType.SIX_HOUR, sixHourScheduleCount.get()
        );
    }

    @Override
    protected IndexIterator loadIndexEntries() {
        // For now we will just search back up to 24 hours, but ultimately this will be
        // configurable.
        DateTime time = startTime.minusHours(24);

        return new IndexIterator(time, startTime, IndexBucket.RAW, dao, configuration);
    }

    @Override
    protected AggregationTask createAggregationTask(List<IndexEntry> batch) {
        return new AggregationTask(batch) {
            @Override
            void run(List<IndexEntry> batch) {
                DateTime startTime = new DateTime(batch.get(0).getTimestamp());
                ListenableFuture<List<ResultSet>> queriesFuture = fetchRawData(startTime, batch);
                processBatch(queriesFuture, startTime, batch);
            }
        };
    }

    private void processBatch(ListenableFuture<List<ResultSet>> queriesFuture, DateTime timeSlice,
        List<IndexEntry> indexEntries) {

        ListenableFuture<Iterable<List<RawNumericMetric>>> iterableFuture = Futures.transform(queriesFuture,
            toIterable(new RawNumericMetricMapper()), aggregationTasks);

        ListenableFuture<List<AggregateNumericMetric>> metricsFuture = Futures.transform(iterableFuture,
            computeAggregates(timeSlice.getMillis(), RawNumericMetric.class, Bucket.ONE_HOUR),
            aggregationTasks);

        ListenableFuture<List<ResultSet>> insertsFuture = Futures.transform(metricsFuture,
            persistFns.persist1HourMetricsAndNoIndexUpdates(), aggregationTasks);

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
                persistFns.persist6HourMetricsAndNoIndexUpdates(), aggregationTasks);

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

    /**
     * The returned function takes as input a future of the aggregate metrics that were just computed and a future of
     * result sets of aggregate metrics for the same bucket. If we just computed 6 hour data for the 06:00 - 12:00 time
     * slice, then the result sets will contain any data already persisted for for the 06:00 - 12:00 time slice. Each
     * aggregate metric will be combined with the data fetched from Cassandra. We combine the data because the aggregate
     * metrics that were just computed might not be included in the query results since we use weak consistency for
     * reads/writes of metric data.
     */
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
