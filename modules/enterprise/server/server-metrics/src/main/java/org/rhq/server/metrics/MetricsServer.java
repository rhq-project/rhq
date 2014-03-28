/*
 *
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.server.metrics.aggregation.AggregationManager;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.CacheIndexEntry;
import org.rhq.server.metrics.domain.CacheIndexEntryMapper;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.domain.RawNumericMetric;

/**
 * @author John Sanda
 */
public class MetricsServer {

    private final Log log = LogFactory.getLog(MetricsServer.class);

    private DateTimeService dateTimeService = new DateTimeService();

    private MetricsDAO dao;

    private MetricsConfiguration configuration;

    private boolean pastAggregationMissed;

    private Long mostRecentRawDataPriorToStartup;

    private AtomicLong totalAggregationTime = new AtomicLong();

    private int numAggregationWorkers = Math.min(Integer.parseInt(System.getProperty("rhq.metrics.aggregation.workers",
        "4")), Runtime.getRuntime().availableProcessors());

    private ListeningExecutorService aggregationWorkers;

    private int aggregationBatchSize = Integer.parseInt(System.getProperty("rhq.metrics.aggregation.batch-size", "5"));

    private int parallelism = Integer.parseInt(System.getProperty("rhq.metrics.aggregation.parallelism", "3"));

    private int cacheBatchSize = Integer.parseInt(System.getProperty("rhq.metrics.cache.batch-size", "100"));

    public void setDAO(MetricsDAO dao) {
        this.dao = dao;
    }

    public void setConfiguration(MetricsConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }

    public int getAggregationBatchSize() {
        return aggregationBatchSize;
    }

    public void setAggregationBatchSize(int batchSize) {
        aggregationBatchSize = batchSize;
    }

    public int getAggregationParallelism() {
        return parallelism;
    }

    public void setAggregationParallelism(int parallelism) {
        this.parallelism = parallelism;
    }

    public int getNumAggregationWorkers() {
        return numAggregationWorkers;
    }

    public void setCacheBatchSize(int size) {
        cacheBatchSize = size;
    }

    ListeningExecutorService getAggregationWorkers() {
        return aggregationWorkers;
    }

    public void init() {
        aggregationWorkers = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(numAggregationWorkers,
            new StorageClientThreadFactory()));
        determineMostRecentRawDataSinceLastShutdown();
    }

    /**
     * In normal operating mode we compute aggregates from the last hour. If the server has
     * been down, we need to determine the most recently stored raw data so we know the
     * starting hour for which to compute aggregates. We only need to check up to the raw
     * retention period though since anything older than that will automatically get
     * purged.
     */
    private void determineMostRecentRawDataSinceLastShutdown() {
        DateTime previousHour = currentHour().minus(configuration.getRawTimeSliceDuration());
        DateTime oldestRawTime = previousHour.minus(configuration.getRawRetention());  // e.g., 7 days ago
        DateTime day = dateTimeService.current24HourTimeSlice();

        CacheIndexEntryMapper mapper = new CacheIndexEntryMapper();
        StorageResultSetFuture future = dao.findPastCacheIndexEntriesFromToday(MetricsTable.RAW, day.getMillis(), 0,
            previousHour.getMillis());
        List<CacheIndexEntry> indexEntries = mapper.map(future.get());
        CacheIndexEntry lastIndexEntry = null;

        if (!indexEntries.isEmpty()) {
            log.info("Raw data aggregate computations are up to date");
            lastIndexEntry = indexEntries.get(indexEntries.size() - 1);
            mostRecentRawDataPriorToStartup = lastIndexEntry.getCollectionTimeSlice();
            pastAggregationMissed = true;
        } else {
            day = day.minus(configuration.getSixHourTimeSliceDuration());
            previousHour = previousHour.minus(configuration.getSixHourTimeSliceDuration());
            future = dao.findPastCacheIndexEntriesBeforeToday(MetricsTable.RAW, day.getMillis(), 0,
                previousHour.getMillis());
            indexEntries = mapper.map(future.get());

            while (indexEntries.isEmpty() && previousHour.isAfter(oldestRawTime)) {
                day = day.minus(configuration.getSixHourTimeSliceDuration());
                previousHour = previousHour.minus(configuration.getSixHourTimeSliceDuration());
                future = dao.findPastCacheIndexEntriesBeforeToday(MetricsTable.RAW, day.getMillis(), 0,
                    previousHour.getMillis());
                indexEntries = mapper.map(future.get());
            }

            if (indexEntries.isEmpty()) {
                log.info("Did not find any raw data in the storage database since the last server shutdown. Raw data " +
                    "aggregate computations are up to date.");
            } else {
                lastIndexEntry = indexEntries.get(indexEntries.size() - 1);
                mostRecentRawDataPriorToStartup = lastIndexEntry.getCollectionTimeSlice();
                pastAggregationMissed = true;

                log.info("Found the most recently inserted raw data prior to this server start up with a timestamp " +
                    "of [" + mostRecentRawDataPriorToStartup + "]. Aggregates for this data will be computed the " +
                    "next time the aggregation job runs.");
            }
        }
    }

    protected DateTime currentHour() {
        return dateTimeService.getTimeSlice(dateTimeService.now(), configuration.getRawTimeSliceDuration());
    }

    protected DateTime roundDownToHour(long timestamp) {
        return dateTimeService.getTimeSlice(new DateTime(timestamp), configuration.getRawTimeSliceDuration());
    }

    public void shutdown() {
        aggregationWorkers.shutdown();
    }

    public RawNumericMetric findLatestValueForResource(int scheduleId) {
        log.debug("Querying for most recent raw metrics for [scheduleId: " + scheduleId + "]");
        return dao.findLatestRawMetric(scheduleId);
    }

    /**
     * @return The total aggregation time in milliseconds since server start. This property is updated after each of
     * raw, one hour, and six hour data are aggregated.
     */
    public long getTotalAggregationTime() {
        return totalAggregationTime.get();
    }

    public Iterable<MeasurementDataNumericHighLowComposite> findDataForResource(int scheduleId, long beginTime,
        long endTime, int numberOfBuckets) {
        Stopwatch stopwatch = new Stopwatch().start();
        try {
            DateTime begin = new DateTime(beginTime);

            if (dateTimeService.isInRawDataRange(begin)) {
                Iterable<RawNumericMetric> metrics = dao.findRawMetrics(scheduleId, beginTime, endTime);
                return createRawComposites(metrics, beginTime, endTime, numberOfBuckets);
            }

            Iterable<AggregateNumericMetric> metrics = null;
            if (dateTimeService.isIn1HourDataRange(begin)) {
                metrics = dao.findOneHourMetrics(scheduleId, beginTime, endTime);
            } else if (dateTimeService.isIn6HourDataRange(begin)) {
                metrics = dao.findSixHourMetrics(scheduleId, beginTime, endTime);
            } else if (dateTimeService.isIn24HourDataRange(begin)) {
                metrics = dao.findTwentyFourHourMetrics(scheduleId, beginTime, endTime);
            } else {
                throw new IllegalArgumentException("beginTime[" + beginTime + "] is outside the accepted range.");
            }

            return createComposites(metrics, beginTime, endTime, numberOfBuckets);
        } finally {
            stopwatch.stop();
            if (log.isDebugEnabled()) {
                log.debug("Retrieved resource data for [scheduleId: " + scheduleId + ", beginTime: " + beginTime +
                    ", endTime: " + endTime + "] in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            }
        }
    }

    public List<MeasurementDataNumericHighLowComposite> findDataForGroup(List<Integer> scheduleIds, long beginTime,
        long endTime, int numberOfBuckets) {
        Stopwatch stopwatch = new Stopwatch().start();
        try {
            DateTime begin = new DateTime(beginTime);

            if (dateTimeService.isInRawDataRange(begin)) {
                Iterable<RawNumericMetric> metrics = dao.findRawMetrics(scheduleIds, beginTime, endTime);
                return createRawComposites(metrics, beginTime, endTime, numberOfBuckets);
            }

            Iterable<AggregateNumericMetric> metrics = null;
            if (dateTimeService.isIn1HourDataRange(begin)) {
                metrics = dao.findOneHourMetrics(scheduleIds, beginTime, endTime);
            } else if (dateTimeService.isIn6HourDataRange(begin)) {
                metrics = dao.findSixHourMetrics(scheduleIds, beginTime, endTime);
            } else if (dateTimeService.isIn24HourDataRange(begin)) {
                metrics = dao.findTwentyFourHourMetrics(scheduleIds, beginTime, endTime);
            } else {
                throw new IllegalArgumentException("beginTime[" + beginTime + "] is outside the accepted range.");
            }

            return createComposites(metrics, beginTime, endTime, numberOfBuckets);
        } finally {
            stopwatch.stop();
            if (log.isDebugEnabled()) {
                log.debug("Retrieved resource group data for [scheduleIds: " + scheduleIds + ", beginTime: " +
                    beginTime + ", endTime: " + endTime + "] in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            }
        }
    }

    public AggregateNumericMetric getSummaryAggregate(int scheduleId, long beginTime, long endTime) {
        Stopwatch stopwatch = new Stopwatch().start();
        try {
            DateTime begin = new DateTime(beginTime);

            if (dateTimeService.isInRawDataRange(begin)) {
                Iterable<RawNumericMetric> metrics = dao.findRawMetrics(scheduleId, beginTime, endTime);
                return calculateAggregatedRaw(metrics, beginTime);
            }

            Iterable<AggregateNumericMetric> metrics = null;
            if (dateTimeService.isIn1HourDataRange(begin)) {
                metrics = dao.findOneHourMetrics(scheduleId, beginTime, endTime);
            } else if (dateTimeService.isIn6HourDataRange(begin)) {
                metrics = dao.findSixHourMetrics(scheduleId, beginTime, endTime);
            } else if (dateTimeService.isIn24HourDataRange(begin)) {
                metrics = dao.findTwentyFourHourMetrics(scheduleId, beginTime, endTime);
            } else {
                throw new IllegalArgumentException("beginTime[" + beginTime + "] is outside the accepted range.");
            }

            return calculateAggregate(metrics, beginTime);
        } finally {
            stopwatch.stop();
            if (log.isDebugEnabled()) {
                log.debug("Finished calculating resource summary aggregate for [scheduleId: " + scheduleId +
                    ", beginTime: " + beginTime + ", endTime: " + endTime + "] in " +
                    stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            }
        }
    }

    public ListenableFuture<AggregateNumericMetric> getSummaryAggregateAsync(int scheduleId, long beginTime,
        long endTime) {
        long start = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Calculating resource summary aggregate (async) for [scheduleId: " + scheduleId +
                    ", beginTime: " + beginTime + ", endTime: " + endTime + "]");
            }
            DateTime begin = new DateTime(beginTime);
            StorageResultSetFuture queryFuture;

            if (dateTimeService.isInRawDataRange(begin)) {
                queryFuture = dao.findRawMetricsAsync(scheduleId, beginTime, endTime);
                return Futures.transform(queryFuture, new ComputeRawAggregate(beginTime));
            }

            if (dateTimeService.isIn1HourDataRange(begin)) {
                queryFuture = dao.findOneHourMetricsAsync(scheduleId, beginTime, endTime);
            } else if (dateTimeService.isIn6HourDataRange(begin)) {
                queryFuture = dao.findSixHourMetricsAsync(scheduleId, beginTime, endTime);
            } else if (dateTimeService.isIn24HourDataRange(begin)) {
                queryFuture = dao.findTwentyFourHourMetricsAsync(scheduleId, beginTime, endTime);
            } else {
                throw new IllegalArgumentException("beginTime[" + beginTime + "] is outside the accepted range.");
            }

            return Futures.transform(queryFuture, new ComputeAggregate(beginTime));
        } finally {
            long end = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debug("Finished calculating resource summary aggregate (async) in " + (end - start) + " ms");
            }
        }
    }

    public AggregateNumericMetric getSummaryAggregate(List<Integer> scheduleIds, long beginTime, long endTime) {
        Stopwatch stopwatch = new Stopwatch().start();
        try {
            DateTime begin = new DateTime(beginTime);

            if (dateTimeService.isInRawDataRange(new DateTime(beginTime))) {
                Iterable<RawNumericMetric> metrics = dao.findRawMetrics(scheduleIds, beginTime, endTime);
                return calculateAggregatedRaw(metrics, beginTime);
            }

            Iterable<AggregateNumericMetric> metrics = null;
            if (dateTimeService.isIn1HourDataRange(begin)) {
                metrics = dao.findOneHourMetrics(scheduleIds, beginTime, endTime);
            } else if (dateTimeService.isIn6HourDataRange(begin)) {
                metrics = dao.findSixHourMetrics(scheduleIds, beginTime, endTime);
            } else if (dateTimeService.isIn24HourDataRange(begin)) {
                metrics = dao.findTwentyFourHourMetrics(scheduleIds, beginTime, endTime);
            } else {
                throw new IllegalArgumentException("beginTime[" + beginTime + "] is outside the accepted range.");
            }

            return calculateAggregate(metrics, beginTime);
        } finally {
            stopwatch.stop();
            if (log.isDebugEnabled()) {
                log.debug("Finished calculating group summary aggregate for [scheduleIds: " + scheduleIds +
                    ", beginTime: " + beginTime + ", endTime: " + endTime + "] in " +
                    stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            }
        }
    }

    private List<MeasurementDataNumericHighLowComposite> createRawComposites(Iterable<RawNumericMetric> metrics,
        long beginTime, long endTime, int numberOfBuckets) {
        Buckets buckets = new Buckets(beginTime, endTime, numberOfBuckets);
        for (RawNumericMetric metric : metrics) {
            buckets.insert(metric.getTimestamp(), metric.getValue(), metric.getValue(), metric.getValue());
        }

        List<MeasurementDataNumericHighLowComposite> data = new ArrayList<MeasurementDataNumericHighLowComposite>();
        for (int i = 0; i < buckets.getNumDataPoints(); ++i) {
            Buckets.Bucket bucket = buckets.get(i);
            data.add(new MeasurementDataNumericHighLowComposite(bucket.getStartTime(), bucket.getAvg(),
                bucket.getMax(), bucket.getMin()));
        }
        return data;
    }

    private List<MeasurementDataNumericHighLowComposite> createComposites(Iterable<AggregateNumericMetric> metrics,
        long beginTime, long endTime, int numberOfBuckets) {

        Buckets buckets = new Buckets(beginTime, endTime, numberOfBuckets);
        for (AggregateNumericMetric metric : metrics) {
            buckets.insert(metric.getTimestamp(), metric.getAvg(), metric.getMin(), metric.getMax());
        }

        List<MeasurementDataNumericHighLowComposite> data = new ArrayList<MeasurementDataNumericHighLowComposite>();
        for (int i = 0; i < buckets.getNumDataPoints(); ++i) {
            Buckets.Bucket bucket = buckets.get(i);
            data.add(new MeasurementDataNumericHighLowComposite(bucket.getStartTime(), bucket.getAvg(),
                bucket.getMax(), bucket.getMin()));
        }
        return data;

    }

    public void addNumericData(final Set<MeasurementDataNumeric> dataSet, final RawDataInsertedCallback callback) {
        if (log.isDebugEnabled()) {
            log.debug("Inserting " + dataSet.size() + " raw metrics");
        }
        final Stopwatch stopwatch = new Stopwatch().start();
        final AtomicInteger remainingInserts = new AtomicInteger(dataSet.size());
        final long insertTimeSlice = dateTimeService.currentHour().getMillis();
        // TODO add support for splitting cache index partition
        final int partition = 0;
        DateTimeComparator dateTimeComparator = DateTimeComparator.getInstance();

        for (final MeasurementDataNumeric data : dataSet) {
            long collectionTimeSlice = dateTimeService.getTimeSlice(new DateTime(data.getTimestamp()),
                configuration.getRawTimeSliceDuration()).getMillis();
            // TODO make the age cap configurable
            if (dateTimeComparator.compare(collectionTimeSlice, dateTimeService.now().minusHours(24)) < 0) {
                callback.onSuccess(data);
                continue;
            }
            int startScheduleId = calculateStartScheduleId(data.getScheduleId());
            DateTime day = dateTimeService.getTimeSlice(collectionTimeSlice,
                configuration.getSixHourTimeSliceDuration());

            StorageResultSetFuture rawFuture = dao.insertRawData(data);
            StorageResultSetFuture cacheFuture = dao.updateMetricsCache(MetricsTable.RAW, collectionTimeSlice, startScheduleId,
                data.getScheduleId(), data.getTimestamp(), ImmutableMap.of(AggregateType.VALUE.ordinal(),
                data.getValue()));
            StorageResultSetFuture indexFuture;
            if (collectionTimeSlice < insertTimeSlice) {
                indexFuture = dao.updateCacheIndex(MetricsTable.RAW, day.getMillis(), partition, startScheduleId,
                    collectionTimeSlice, ImmutableSet.of(data.getScheduleId()));
            } else {
                indexFuture = dao.updateCacheIndex(MetricsTable.RAW, day.getMillis(), partition, collectionTimeSlice,
                    startScheduleId);
            }
            ListenableFuture<List<ResultSet>> insertsFuture = Futures.successfulAsList(rawFuture, cacheFuture,
                indexFuture);
            Futures.addCallback(insertsFuture, new FutureCallback<List<ResultSet>>() {
                @Override
                public void onSuccess(List<ResultSet> result) {
                    callback.onSuccess(data);
                    if (remainingInserts.decrementAndGet() == 0) {
                        stopwatch.stop();
                        if (log.isDebugEnabled()) {
                            log.debug("Finished inserting " + dataSet.size() + " raw metrics in " +
                                stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
                        }
                        callback.onFinish();
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    if (log.isDebugEnabled()) {
                        log.debug("An error occurred while inserting raw data", ThrowableUtil.getRootCause(t));
                    } else {
                        log.warn("An error occurred while inserting raw data: " + ThrowableUtil.getRootMessage(t));
                    }
                    callback.onFailure(t);
                }
            }, aggregationWorkers);
        }
    }

    private int calculateStartScheduleId(int scheduleId) {
        return (scheduleId / cacheBatchSize) * cacheBatchSize;
    }

    /**
     * Computes and stores aggregates for all buckets that are ready to be aggregated.
     * This includes raw, 1hr, 6hr, and 24hr data.
     *
     * @return One hour aggregates. That is, any raw data that has been rolled up into onr
     * one hour aggregates. The one hour aggregates are returned because they are needed
     * for subsequently computing baselines.
     */
    public Iterable<AggregateNumericMetric> calculateAggregates() {
        Stopwatch stopwatch = new Stopwatch().start();
        try {
            DateTime theHour = currentHour();
            if (pastAggregationMissed) {
                DateTime missedHour = roundDownToHour(mostRecentRawDataPriorToStartup);
                new AggregationManager(aggregationWorkers, dao, dateTimeService, missedHour, aggregationBatchSize, parallelism,
                    cacheBatchSize).run();
                pastAggregationMissed = false;
            }
            DateTime timeSlice = theHour.minus(configuration.getRawTimeSliceDuration());

            return new AggregationManager(aggregationWorkers, dao, dateTimeService, timeSlice, aggregationBatchSize,
                parallelism, cacheBatchSize).run();
        } finally {
            stopwatch.stop();
            totalAggregationTime.addAndGet(stopwatch.elapsed(TimeUnit.MILLISECONDS));
            log.info("Finished metrics aggregation in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }

    private AggregateNumericMetric calculateAggregatedRaw(Iterable<RawNumericMetric> rawMetrics, long timestamp) {
        double min = Double.NaN;
        double max = min;
        int count = 0;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
        double value;

        for (RawNumericMetric metric : rawMetrics) {
            value = metric.getValue();
            if (count == 0) {
                min = value;
                max = min;
            }
            if (value < min) {
                min = value;
            } else if (value > max) {
                max = value;
            }
            mean.add(value);
            ++count;
        }

        // We let the caller handle setting the schedule id because in some cases we do
        // not care about it.
        return new AggregateNumericMetric(0, mean.getArithmeticMean(), min, max, timestamp);
    }

    private AggregateNumericMetric calculateAggregate(Iterable<AggregateNumericMetric> metrics, long timestamp) {
        double min = Double.NaN;
        double max = min;
        int count = 0;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();

        for (AggregateNumericMetric metric : metrics) {
            if (count == 0) {
                min = metric.getMin();
                max = metric.getMax();
            }
            if (metric.getMin() < min) {
                min = metric.getMin();
            } else if (metric.getMax() > max) {
                max = metric.getMax();
            }
            mean.add(metric.getAvg());
            ++count;
        }

        // We let the caller handle setting the schedule id because in some cases we do
        // not care about it.
        return new AggregateNumericMetric(0, mean.getArithmeticMean(), min, max, timestamp);
    }

}
