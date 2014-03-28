package org.rhq.server.metrics.aggregation;

import static org.rhq.server.metrics.domain.MetricsTable.ONE_HOUR;
import static org.rhq.server.metrics.domain.MetricsTable.SIX_HOUR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.AbortedException;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.NumericMetric;

/**
 * This class is the driver for metrics aggregation.
 *
 * @author John Sanda
 */
public class AggregationManager {

    public static final int INDEX_PARTITION = 0;
    private static final Comparator<AggregateNumericMetric> AGGREGATE_COMPARATOR = new Comparator<AggregateNumericMetric>() {
        @Override
        public int compare(AggregateNumericMetric left, AggregateNumericMetric right) {
            return (left.getScheduleId() < right.getScheduleId()) ? -1 : ((left.getScheduleId() == right.getScheduleId()) ? 0 : 1);
        }
    };

    private final Log log = LogFactory.getLog(AggregationManager.class);

    private MetricsDAO dao;

    private DateTimeService dtService;

    private DateTime startTime;

    private ListeningExecutorService aggregationTasks;

    private Set<AggregateNumericMetric> oneHourData;

    private int cacheBatchSize;

    private Semaphore permits;

    public AggregationManager(ListeningExecutorService aggregationTasks, MetricsDAO dao, DateTimeService dtService,
        DateTime startTime, int batchSize, int parallelism, int cacheBatchSize) {

        this.dao = dao;
        this.dtService = dtService;
        this.startTime = startTime;
        oneHourData = new ConcurrentSkipListSet<AggregateNumericMetric>(AGGREGATE_COMPARATOR);
        this.cacheBatchSize = cacheBatchSize;
        permits = new Semaphore(batchSize * parallelism);
        this.aggregationTasks = aggregationTasks;
    }

    private boolean is6HourTimeSliceFinished() {
        return dtService.is6HourTimeSliceFinished(startTime);
    }

    private boolean is24HourTimeSliceFinished() {
        return dtService.is24HourTimeSliceFinished(startTime);
    }

    private DateTime get6HourTimeSlice() {
        return dtService.get6HourTimeSlice(startTime);
    }

    private DateTime get24HourTimeSlice() {
        return dtService.get24HourTimeSlice(startTime);
    }

    public Set<AggregateNumericMetric> run() {
        log.info("Starting aggregation for time slice " + startTime);
        Stopwatch stopwatch = new Stopwatch().start();
        int numRaw = 0;
        int num1Hour = 0;
        int num6Hour = 0;
        try {
            createPastDataAggregator().execute();
            numRaw = createRawAggregator().execute();
            if (is6HourTimeSliceFinished()) {
                num1Hour = create1HourAggregator().execute();
            }
            if (is24HourTimeSliceFinished()) {
                num6Hour = create6HourAggregator().execute();
            }

            return oneHourData;
        } catch (InterruptedException e) {
            log.info("There was an interrupt while waiting for aggregation to finish. Aggregation will be aborted.");
            return Collections.emptySet();
        }
        catch (AbortedException e) {
            log.warn("Aggregation has been aborted: " + e.getMessage());
            return Collections.emptySet();
        } finally {
            stopwatch.stop();
            log.info("Finished aggregation of {\"raw schedules\": " + numRaw + ", \"1 hour schedules\": " + num1Hour +
                ", \"6 hour schedules\": " + num6Hour + "} in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }

    private PastDataAggregator createPastDataAggregator() {
        PastDataAggregator aggregator = new PastDataAggregator();
        aggregator.setAggregationTasks(aggregationTasks);
        aggregator.setAggregationType(AggregationType.RAW);
        aggregator.setCurrentDay(get24HourTimeSlice());
        aggregator.setDao(dao);
        aggregator.setPermits(permits);
        aggregator.setStartingDay(get24HourTimeSlice().minusDays(1));
        aggregator.setStartTime(startTime);
        aggregator.setDateTimeService(dtService);
        aggregator.setPersistMetrics(persist1HourMetrics());

        return aggregator;
    }

    private CacheAggregator createRawAggregator() {
        CacheAggregator aggregator = new CacheAggregator();
        aggregator.setAggregationTasks(aggregationTasks);
        aggregator.setAggregationType(AggregationType.RAW);
        aggregator.setCacheBatchSize(cacheBatchSize);
        aggregator.setDao(dao);
        aggregator.setPermits(permits);
        aggregator.setStartTime(startTime);
        aggregator.setCurrentDay(get24HourTimeSlice());
        aggregator.setDateTimeService(dtService);
        aggregator.setPersistMetrics(persist1HourMetrics());
        aggregator.setCacheBlockFinishedListener(new CacheAggregator.CacheBlockFinishedListener() {
            @Override
            public void onFinish(IndexAggregatesPair pair) {
                oneHourData.addAll(pair.metrics);
            }
        });
        return aggregator;
    }

    private CacheAggregator create1HourAggregator() {
        CacheAggregator aggregator = new CacheAggregator();
        aggregator.setAggregationTasks(aggregationTasks);
        aggregator.setAggregationType(AggregationType.ONE_HOUR);
        aggregator.setCacheBatchSize(cacheBatchSize);
        aggregator.setDao(dao);
        aggregator.setPermits(permits);
        aggregator.setStartTime(get6HourTimeSlice());
        aggregator.setCurrentDay(get24HourTimeSlice());
        aggregator.setDateTimeService(dtService);
        aggregator.setPersistMetrics(persist6HourMetrics());

        return aggregator;
    }

    private CacheAggregator create6HourAggregator() {
        CacheAggregator aggregator = new CacheAggregator();
        aggregator.setAggregationTasks(aggregationTasks);
        aggregator.setAggregationType(AggregationType.SIX_HOUR);
        aggregator.setCacheBatchSize(cacheBatchSize);
        aggregator.setDao(dao);
        aggregator.setPermits(permits);
        aggregator.setStartTime(get24HourTimeSlice());
        aggregator.setCurrentDay(get24HourTimeSlice());
        aggregator.setDateTimeService(dtService);
        aggregator.setPersistMetrics(persist24HourMetrics());

        return aggregator;
    }

    private AsyncFunction<IndexAggregatesPair, List<ResultSet>> persist1HourMetrics() {
        return new AsyncFunction<IndexAggregatesPair, List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> apply(IndexAggregatesPair pair) throws Exception {
                List<StorageResultSetFuture> futures = new ArrayList<StorageResultSetFuture>(pair.metrics.size() * 5);
                for (NumericMetric metric : pair.metrics) {
                    futures.add(dao.insertOneHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                        AggregateType.MAX, metric.getMax()));
                    futures.add(dao.insertOneHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                        AggregateType.MIN, metric.getMin()));
                    futures.add(dao.insertOneHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                        AggregateType.AVG, metric.getAvg()));
                    futures.add(dao.updateMetricsCache(ONE_HOUR, dtService.get6HourTimeSlice(startTime).getMillis(),
                        pair.cacheIndexEntry.getStartScheduleId(), metric.getScheduleId(), metric.getTimestamp(), toMap(
                        metric)));
                    futures.add(dao.updateCacheIndex(ONE_HOUR, dtService.get24HourTimeSlice(startTime).getMillis(),
                        INDEX_PARTITION, dtService.get6HourTimeSlice(startTime).getMillis(), pair.cacheIndexEntry.getStartScheduleId()));
                }
                return Futures.successfulAsList(futures);
            }
        };
    }

    private AsyncFunction<IndexAggregatesPair, List<ResultSet>> persist6HourMetrics() {
        return new AsyncFunction<IndexAggregatesPair, List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> apply(IndexAggregatesPair pair) throws Exception {
                List<StorageResultSetFuture> futures = new ArrayList<StorageResultSetFuture>(pair.metrics.size() * 5);
                for (NumericMetric metric : pair.metrics) {
                    futures.add(dao.insertSixHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                        AggregateType.MAX, metric.getMax()));
                    futures.add(dao.insertSixHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                        AggregateType.MIN, metric.getMin()));
                    futures.add(dao.insertSixHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                        AggregateType.AVG, metric.getAvg()));
                    futures.add(dao.updateMetricsCache(SIX_HOUR, dtService.get24HourTimeSlice(startTime).getMillis(),
                        pair.cacheIndexEntry.getStartScheduleId(), metric.getScheduleId(), metric.getTimestamp(), toMap(metric)));
                    futures.add(dao.updateCacheIndex(SIX_HOUR, dtService.get24HourTimeSlice(startTime).getMillis(),
                        INDEX_PARTITION, dtService.get24HourTimeSlice(startTime).getMillis(), pair.cacheIndexEntry.getStartScheduleId()));
                }
                return Futures.successfulAsList(futures);
            }
        };
    }

    private AsyncFunction<IndexAggregatesPair, List<ResultSet>> persist24HourMetrics() {
        return new AsyncFunction<IndexAggregatesPair, List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> apply(IndexAggregatesPair pair) throws Exception {
                List<StorageResultSetFuture> futures = new ArrayList<StorageResultSetFuture>(pair.metrics.size() * 3);
                for (NumericMetric metric : pair.metrics) {
                    futures.add(dao.insertTwentyFourHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                        AggregateType.MAX, metric.getMax()));
                    futures.add(dao.insertTwentyFourHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                        AggregateType.MIN, metric.getMin()));
                    futures.add(dao.insertTwentyFourHourDataAsync(metric.getScheduleId(), metric.getTimestamp(),
                        AggregateType.AVG, metric.getAvg()));
                }
                return Futures.successfulAsList(futures);
            }
        };
    }

    // TODO move the map function into AggregateNumericMetric
    private Map<Integer, Double> toMap(NumericMetric metric) {
        return ImmutableMap.of(
            AggregateType.MAX.ordinal(), metric.getMax(),
            AggregateType.MIN.ordinal(), metric.getMin(),
            AggregateType.AVG.ordinal(), metric.getAvg()
        );
    }

}
