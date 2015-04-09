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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Days;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.server.metrics.aggregation.AggregationManager;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateNumericMetricMapper;
import org.rhq.server.metrics.domain.Bucket;
import org.rhq.server.metrics.domain.IndexBucket;
import org.rhq.server.metrics.domain.RawNumericMetric;
import org.rhq.server.metrics.invalid.InvalidMetricsManager;

/**
 * @author John Sanda
 */
public class MetricsServer {

    private final Log log = LogFactory.getLog(MetricsServer.class);

    private static final int RAW_DATA_AGE_LIMIT_MAX = 5;
    private static final int QUEUE_SIZE = 200000;

    private DateTimeService dateTimeService = new DateTimeService();

    private MetricsDAO dao;

    private MetricsConfiguration configuration;

    private ListeningExecutorService tasks = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(
        new StorageClientThreadFactory("MetricsServerTasks")));

    private InvalidMetricsManager invalidMetricsManager;

    private AggregationManager aggregationManager;

    private volatile BlockingQueue<MeasurementDataNumeric> numericQueue;

    private NumericQueueConsumer numericQueueConsumer;

    private Days rawDataAgeLimit = Days.days(Math.min(3, Integer.parseInt(
        System.getProperty("rhq.metrics.data.age-limit", "3"))));

    public void setDAO(MetricsDAO dao) {
        this.dao = dao;
    }

    public void setConfiguration(MetricsConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }

    public int getRawDataAgeLimit() {
        return rawDataAgeLimit.getDays();
    }

    public void setRawDataAgeLimit(int rawDataAgeLimit) {
        if (rawDataAgeLimit > RAW_DATA_AGE_LIMIT_MAX) {
            throw new IllegalArgumentException("The requested limit, " + rawDataAgeLimit + ", exceeds the max age " +
                "limit of " + RAW_DATA_AGE_LIMIT_MAX);
        }
        this.rawDataAgeLimit = Days.days(rawDataAgeLimit);
    }

    public void setIndexPartitions(int indexPartitions) {
        configuration.setIndexPartitions(indexPartitions);
    }

    public void init() {
        aggregationManager = new AggregationManager(dao, dateTimeService, configuration);
        invalidMetricsManager = new InvalidMetricsManager(dateTimeService, dao);

        numericQueue = new ArrayBlockingQueue<MeasurementDataNumeric>(QUEUE_SIZE);
        numericQueueConsumer = new NumericQueueConsumer();
        numericQueueConsumer.setName("MetricsServer numericQueueConsumer");
        numericQueueConsumer.start();
    }

    /**
     * A test hook
     */
    InvalidMetricsManager getInvalidMetricsManager() {
        return invalidMetricsManager;
    }

    public AggregationManager getAggregationManager() {
        return aggregationManager;
    }

    public void shutdown() {
        numericQueueConsumer.shutdown();
        try {
            numericQueueConsumer.join(); // Lets wait for it to finish before we continue..
        } catch (InterruptedException e) {
            log.warn("Potential data loss while waiting for the queue to empty, ", e);
        }
        aggregationManager.shutdown();
        invalidMetricsManager.shutdown();
    }

    public RawNumericMetric findLatestValueForResource(int scheduleId) {
        log.debug("Querying for most recent raw metrics for [scheduleId: " + scheduleId + "]");
        return dao.findLatestRawMetric(scheduleId);
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

            List<AggregateNumericMetric> metrics = null;
            if (dateTimeService.isIn1HourDataRange(begin)) {
                metrics = dao.findAggregateMetrics(scheduleId, Bucket.ONE_HOUR, beginTime,
                    endTime);
                return createComposites(metrics, beginTime, endTime, numberOfBuckets);
            } else if (dateTimeService.isIn6HourDataRange(begin)) {
                metrics = dao.findAggregateMetrics(scheduleId, Bucket.SIX_HOUR, beginTime, endTime);
                return createComposites(metrics, beginTime, endTime, numberOfBuckets);
            } else if (dateTimeService.isIn24HourDataRange(begin)) {
                metrics = dao.findAggregateMetrics(scheduleId, Bucket.TWENTY_FOUR_HOUR, beginTime, endTime);
                return createComposites(metrics, beginTime, endTime, numberOfBuckets);
            } else {
                throw new IllegalArgumentException("beginTime[" + beginTime + "] is outside the accepted range.");
            }
        } finally {
            stopwatch.stop();
            if (log.isDebugEnabled()) {
                log.debug("Finished calculating resource summary aggregate in " +
                    stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            }
        }
    }

    public List<MeasurementDataNumericHighLowComposite> findDataForGroup(List<Integer> scheduleIds, long beginTime,
        long endTime, int numberOfBuckets) {
        if (log.isDebugEnabled()) {
            log.debug("Querying for metric data using parameters [scheduleIds: " + scheduleIds + ", beingTime: " +
                beginTime + ", endTime: " + endTime + ", numberOfBuckets: " + numberOfBuckets + "]");
        }

        DateTime begin = new DateTime(beginTime);
        if (dateTimeService.isInRawDataRange(begin)) {
            Iterable<RawNumericMetric> metrics = dao.findRawMetrics(scheduleIds, beginTime, endTime);
            return createRawComposites(metrics, beginTime, endTime, numberOfBuckets);
        }
        Bucket bucket = getBucket(begin);
        List<AggregateNumericMetric> metrics = loadMetrics(scheduleIds, beginTime, endTime, bucket);

        return createComposites(metrics, beginTime, endTime, numberOfBuckets);
    }

    public AggregateNumericMetric getSummaryAggregate(int scheduleId, long beginTime, long endTime) {
        Stopwatch stopwatch = new Stopwatch().start();
        try {
            DateTime begin = new DateTime(beginTime);

            if (dateTimeService.isInRawDataRange(begin)) {
                Iterable<RawNumericMetric> metrics = dao.findRawMetrics(scheduleId, beginTime, endTime);
                return calculateAggregatedRaw(metrics, beginTime);
            }

            Bucket bucket = getBucket(begin);
            List<AggregateNumericMetric> metrics = dao.findAggregateMetrics(scheduleId, bucket, beginTime, endTime);
            return calculateAggregate(metrics, beginTime, bucket);
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
            Bucket bucket = getBucket(begin);
            queryFuture = dao.findAggregateMetricsAsync(scheduleId, bucket, beginTime, endTime);

            return Futures.transform(queryFuture, new ComputeAggregate(beginTime, bucket));
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
            Bucket bucket = getBucket(begin);
            List<AggregateNumericMetric> metrics = loadMetrics(scheduleIds, beginTime, endTime, bucket);

            return calculateAggregate(metrics, beginTime, bucket);
        } finally {
            stopwatch.stop();
            if (log.isDebugEnabled()) {
                log.debug("Finished calculating group summary aggregate for [scheduleIds: " + scheduleIds +
                    ", beginTime: " + beginTime + ", endTime: " + endTime + "] in " +
                    stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            }
        }
    }

    private List<AggregateNumericMetric> loadMetrics(List<Integer> scheduleIds, long begin, long end, Bucket bucket) {
        List<StorageResultSetFuture> futures = new ArrayList<StorageResultSetFuture>(scheduleIds.size());
        for (Integer scheduleId : scheduleIds) {
            futures.add(dao.findAggregateMetricsAsync(scheduleId, bucket, begin, end));
        }
        ListenableFuture<List<ResultSet>> resultSetsFuture = Futures.successfulAsList(futures);
        try {
            List<ResultSet> resultSets = resultSetsFuture.get();
            AggregateNumericMetricMapper mapper = new AggregateNumericMetricMapper();
            List<AggregateNumericMetric> metrics = new ArrayList<AggregateNumericMetric>();
            for (ResultSet resultSet : resultSets) {
                metrics.addAll(mapper.mapAll(resultSet));
            }
            return metrics;
        } catch (Exception e) {
            log.warn("There was an error while fetching " + bucket + " data for {scheduleIds: " + scheduleIds +
                ", beginTime: " + begin + ", endTime: " + end + "}", e);
            return Collections.emptyList();
        }
    }

    protected Bucket getBucket(DateTime begin) {
        Bucket bucket;
        if (dateTimeService.isIn1HourDataRange(begin)) {
            bucket = Bucket.ONE_HOUR;
        } else if (dateTimeService.isIn6HourDataRange(begin)) {
            bucket = Bucket.SIX_HOUR;
        } else if (dateTimeService.isIn24HourDataRange(begin)) {
            bucket = Bucket.TWENTY_FOUR_HOUR;
        } else {
            throw new IllegalArgumentException("beginTime[" + begin.getMillis() + "] is outside the accepted range.");
        }
        return bucket;
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
            if (invalidMetricsManager.isInvalidMetric(metric)) {
                log.warn("The " + metric.getBucket() + " metric " + metric + " is invalid. It will be excluded from " +
                    "the results sent to the client and we will attempt to recompute the metric.");
                invalidMetricsManager.submit(metric);
            } else {
                buckets.insert(metric.getTimestamp(), metric.getAvg(), metric.getMin(), metric.getMax());
            }
        }

        List<MeasurementDataNumericHighLowComposite> data = new ArrayList<MeasurementDataNumericHighLowComposite>();
        for (int i = 0; i < buckets.getNumDataPoints(); ++i) {
            Buckets.Bucket bucket = buckets.get(i);
            data.add(new MeasurementDataNumericHighLowComposite(bucket.getStartTime(), bucket.getAvg(),
                bucket.getMax(), bucket.getMin()));
        }
        return data;

    }

    public synchronized void addNumericData(final Set<MeasurementDataNumeric> dataSet, final FutureCallback<Void> callback) {
        if(numericQueue.remainingCapacity() > dataSet.size()) {
            for(final MeasurementDataNumeric data : dataSet) {
                try {
                    if(data != null) {
                        numericQueue.offer(data, 1, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException e) {
                    // Modify the exception to include sane message telling us even the buffer is full..
                    // and actually do something with it in MeasurementDataManagerBean ..
                    callback.onFailure(new RuntimeException("The queue insert timed out after one second, can't finish the queue loading.", e));
                    return;
                }
            }
            callback.onSuccess(null);
        } else {
            callback.onFailure(new RuntimeException("The server is overloaded, queue is full."));
        }
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
        return aggregationManager.run();
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
        return new AggregateNumericMetric(0, Bucket.ONE_HOUR, mean.getArithmeticMean(), min, max, timestamp);
    }

    private AggregateNumericMetric calculateAggregate(Iterable<AggregateNumericMetric> metrics, long timestamp,
        Bucket bucket) {
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
            }
            if (metric.getMax() > max) {
                max = metric.getMax();
            }
            mean.add(metric.getAvg());
            ++count;
        }

        // We let the caller handle setting the schedule id because in some cases we do
        // not care about it.
        return new AggregateNumericMetric(0, bucket, mean.getArithmeticMean(), min, max, timestamp);
    }

    public int getQueueAvailableCapacity() {
        return this.numericQueue.remainingCapacity();
    }

    private class NumericQueueConsumer extends Thread {
        private final MeasurementDataNumeric KILL_SIGNAL = new MeasurementDataNumeric(Long.MIN_VALUE, Integer.MIN_VALUE, Double.valueOf(Double.MIN_VALUE));

        @Override
        public void run() {
            while(true) {
                final MeasurementDataNumeric data;
                try {
                    data = numericQueue.take();
                    if(data == KILL_SIGNAL) {
                        return;
                    }
                } catch (InterruptedException e) {
                    // We still keep getting interrupted.. we really need to die
                    return;
                }
                DateTime collectionTimeSlice = dateTimeService.getTimeSlice(new DateTime(data.getTimestamp()),
                        configuration.getRawTimeSliceDuration());
                Days days = Days.daysBetween(collectionTimeSlice, dateTimeService.now());

                if (days.isGreaterThan(rawDataAgeLimit)) {
                    log.info(data + " is older than the raw data age limit of " + rawDataAgeLimit.getDays() +
                            " days. It will not be stored.");
                } else {
                    StorageResultSetFuture rawFuture = dao.insertRawData(data);
                    StorageResultSetFuture indexFuture = dao.updateIndex(IndexBucket.RAW, collectionTimeSlice.getMillis(),
                            data.getScheduleId());
                    ListenableFuture<List<ResultSet>> insertsFuture = Futures.successfulAsList(rawFuture, indexFuture);
                    Futures.addCallback(insertsFuture, new FutureCallback<List<ResultSet>>() {
                        @Override
                        public void onSuccess(List<ResultSet> result) {
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            boolean offerSuccess = numericQueue.offer(data);
                            if(!offerSuccess) {
                                if (log.isDebugEnabled()) {
                                    log.debug("An error occurred while inserting raw data", ThrowableUtil.getRootCause(t));
                                } else {
                                    log.warn("An error occurred while inserting raw data: " + ThrowableUtil.getRootMessage(t)
                                    + ", data was: " + data);
                                }
                            }
                        }
                    }, tasks);
                }
            }
        }

        /**
         * Push KILL_SIGNAL to the processing queue
         */
        public void shutdown() {
            try {
                numericQueue.put(KILL_SIGNAL);
            } catch (InterruptedException e) {
                log.warn("Could not push kill signal to the numericQueue. Possible data loss.");
                Thread.currentThread().interrupt();
            }
        }
    }

}
