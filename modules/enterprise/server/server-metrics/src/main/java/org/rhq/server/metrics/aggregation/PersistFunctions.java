package org.rhq.server.metrics.aggregation;

import static org.rhq.server.metrics.domain.MetricsTable.ONE_HOUR;
import static org.rhq.server.metrics.domain.MetricsTable.SIX_HOUR;

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.NumericMetric;

/**
 * This is a utility class of functions for persisting aggregate metrics.
 *
 * @author John Sanda
 */
class PersistFunctions {

    private MetricsDAO dao;

    private DateTimeService dateTimeService;

    private AsyncFunction<IndexAggregatesPair, List<ResultSet>> persist1HourMetricsAndUpdateCache;

    private AsyncFunction<IndexAggregatesPair, List<ResultSet>> persist1HourMetrics;

    private AsyncFunction<IndexAggregatesPair, List<ResultSet>> persist6HourMetricsAndUpdateCache;

    private AsyncFunction<IndexAggregatesPair, List<ResultSet>> persist6HourMetrics;

    private AsyncFunction<IndexAggregatesPair, List<ResultSet>> persist24HourMetrics;

    public PersistFunctions(MetricsDAO dao, DateTimeService dateTimeService) {
        this.dao = dao;
        this.dateTimeService = dateTimeService;
        initFunctions();
    }

    private void initFunctions() {
        persist1HourMetrics = new AsyncFunction<IndexAggregatesPair, List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> apply(IndexAggregatesPair pair) {
                List<StorageResultSetFuture> futures = new ArrayList<StorageResultSetFuture>(pair.metrics.size() * 3);

                for (AggregateNumericMetric metric : pair.metrics) {
                    futures.addAll(persist1HourMetric(metric));
                }
                return Futures.allAsList(futures);
            }
        };

        persist1HourMetricsAndUpdateCache = new AsyncFunction<IndexAggregatesPair, List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> apply(IndexAggregatesPair pair) {
                List<StorageResultSetFuture> futures = new ArrayList<StorageResultSetFuture>(pair.metrics.size() * 5);
                long start6HourTimeSlice = dateTimeService.get6HourTimeSlice(pair.cacheIndexEntry.
                    getCollectionTimeSlice()).getMillis();
                long start24HourTimeSlice = dateTimeService.get24HourTimeSlice(pair.cacheIndexEntry
                    .getCollectionTimeSlice()).getMillis();

                for (AggregateNumericMetric metric : pair.metrics) {
                    futures.addAll(persist1HourMetric(metric));
                    futures.add(dao.updateMetricsCache(ONE_HOUR, start6HourTimeSlice,
                        pair.cacheIndexEntry.getStartScheduleId(), metric.getScheduleId(), metric.getTimestamp(),
                        metric.toMap()));
                    futures.add(dao.updateCacheIndex(ONE_HOUR, start24HourTimeSlice, AggregationManager.INDEX_PARTITION,
                        start6HourTimeSlice, pair.cacheIndexEntry.getStartScheduleId()));
                }
                return Futures.allAsList(futures);
            }
        };

        persist6HourMetrics = new AsyncFunction<IndexAggregatesPair, List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> apply(IndexAggregatesPair pair) {
                List<StorageResultSetFuture> futures = new ArrayList<StorageResultSetFuture>(pair.metrics.size() * 3);
                for (AggregateNumericMetric metric : pair.metrics) {
                    futures.addAll(persist6HourMetric(metric));
                }
                return Futures.allAsList(futures);
            }
        };

        persist6HourMetricsAndUpdateCache = new AsyncFunction<IndexAggregatesPair, List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> apply(IndexAggregatesPair pair) {
                List<StorageResultSetFuture> futures = new ArrayList<StorageResultSetFuture>(pair.metrics.size() * 5);
                long start24HourTimeSlice = dateTimeService.get24HourTimeSlice(pair.cacheIndexEntry
                    .getCollectionTimeSlice()).getMillis();

                for (AggregateNumericMetric metric : pair.metrics) {
                    futures.addAll(persist6HourMetric(metric));
                    futures.add(dao.updateMetricsCache(SIX_HOUR, start24HourTimeSlice,
                        pair.cacheIndexEntry.getStartScheduleId(), metric.getScheduleId(), metric.getTimestamp(),
                        metric.toMap()));
                    futures.add(dao.updateCacheIndex(SIX_HOUR, start24HourTimeSlice, AggregationManager.INDEX_PARTITION,
                        start24HourTimeSlice, pair.cacheIndexEntry.getStartScheduleId()));
                }
                return Futures.allAsList(futures);
            }
        };

        persist24HourMetrics = new AsyncFunction<IndexAggregatesPair, List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> apply(IndexAggregatesPair pair) {
                List<StorageResultSetFuture> futures = new ArrayList<StorageResultSetFuture>(pair.metrics.size() * 3);
                for (NumericMetric metric : pair.metrics) {
                    futures.add(dao.insertTwentyFourHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                        AggregateType.MAX, metric.getMax()));
                    futures.add(dao.insertTwentyFourHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                        AggregateType.MIN, metric.getMin()));
                    futures.add(dao.insertTwentyFourHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                        AggregateType.AVG, metric.getAvg()));
                }
                return Futures.allAsList(futures);
            }
        };
    }

    private List<StorageResultSetFuture> persist1HourMetric(AggregateNumericMetric metric) {
        return ImmutableList.of(
            dao.insertOneHourDataAsync(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX,
                metric.getMax()),
            dao.insertOneHourDataAsync(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN,
                metric.getMin()),
            dao.insertOneHourDataAsync(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG,
                metric.getAvg())
        );
    }

    private List<StorageResultSetFuture> persist6HourMetric(AggregateNumericMetric metric) {
        return ImmutableList.of(
            dao.insertSixHourDataAsync(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX,
                metric.getMax()),
            dao.insertSixHourDataAsync(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN,
                metric.getMin()),
            dao.insertSixHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                AggregateType.AVG, metric.getAvg())
        );
    }

    /**
     * Returns a function that persists 1 hour aggregate metrics. The function takes as input a
     * {@link IndexAggregatesPair} and returns a future of the result sets. If any of the writes fail, then the
     * returned future fails. This function does not update the metrics_cache and metrics_cache_index tables.
     *
     * @return A future containing a list of all the result sets for the metrics persisted
     */
    public AsyncFunction<IndexAggregatesPair, List<ResultSet>> persist1HourMetrics() {
        return persist1HourMetrics;
    }

    /**
     * Returns a function that persists 1 hour aggregate metrics. The metrics_cache and metrics_cache_index tables are
     * updated as well in preparation for 1 hour data aggregation. The function takes as input a
     * {@link IndexAggregatesPair} and returns a future of the result sets. If any of the writes fail, then the
     * returned future fails.
     *
     * @return A future containing a list of all the result sets for the metrics persisted
     */
    public AsyncFunction<IndexAggregatesPair, List<ResultSet>> persist1HourMetricsAndUpdateCache() {
        return persist1HourMetricsAndUpdateCache;
    }

    /**
     * Returns a function that persists 6 hour aggregate metrics. The function takes as input a
     * {@link IndexAggregatesPair} and returns a future of the result sets. If any of the writes fail, then the
     * returned future fails. This function does not update the metrics_cache and metrics_cache_index tables.
     *
     * @return A future containing a list of all the result sets for the metrics persisted
     */
    public AsyncFunction<IndexAggregatesPair, List<ResultSet>> persist6HourMetrics() {
        return persist6HourMetrics;
    }

    /**
     * Returns a function that persists 6 hour aggregate metrics. The metrics_cache and metrics_cache_index tables are
     * updated as well in preparation for 6 hour data aggregation. The function takes as input a
     * {@link IndexAggregatesPair} and returns a future of the result sets. If any of the writes fail, then the
     * returned future fails. This function does not update the metrics_cache and metrics_cache_index tables.
     *
     * @return A future containing a list of all the result sets for the metrics persisted
     */
    public AsyncFunction<IndexAggregatesPair, List<ResultSet>> persist6HourMetricsAndUpdateCache() {
        return persist6HourMetricsAndUpdateCache;
    }

    /**
     * Returns a function that persists 24 hour aggregate metrics. The function takes as input a
     * {@link IndexAggregatesPair} and returns a future of the result sets. If any of the writes fail, then the
     * returned future fails. This function does not update the metrics_cache and metrics_cache_index tables.
     *
     * @return A future containing a list of all the result sets for the metrics persisted
     */
    public AsyncFunction<IndexAggregatesPair, List<ResultSet>> persist24HourMetrics() {
        return persist24HourMetrics;
    }
}
