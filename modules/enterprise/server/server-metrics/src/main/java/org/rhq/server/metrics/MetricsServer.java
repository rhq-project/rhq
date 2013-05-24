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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.Duration;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.util.StringUtil;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.MetricsIndexEntry;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.domain.RawNumericMetric;

/**
 * @author John Sanda
 */
public class MetricsServer {

    private static final int DEFAULT_PAGE_SIZE = 200;

    private final Log log = LogFactory.getLog(MetricsServer.class);

    private DateTimeService dateTimeService = new DateTimeService();

    private Session session;

    private MetricsDAO dao;

    private MetricsConfiguration configuration;

    private ListeningExecutorService listeningExecutorService = MoreExecutors.listeningDecorator(
        Executors.newFixedThreadPool(5));

    private ConcurrentLinkedQueue<MeasurementDataNumeric> rawDataQueue =
        new ConcurrentLinkedQueue<MeasurementDataNumeric>();

    private Semaphore semaphore = new Semaphore(100);

    private boolean shutdown = false;

    public MetricsServer() {
//        listeningExecutorService.submit(new Runnable() {
//            @Override
//            public void run() {
//                while (!shutdown) {
//                    try {
//                        if (!rawDataQueue.isEmpty()) {
//                            semaphore.acquire();
//                            MeasurementDataNumeric rawData = rawDataQueue.poll();
//                            persistRawData();
//                        }
//                    } catch (InterruptedException e) {
//
//                    }
//                }
//            }
//        });
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setDAO(MetricsDAO dao) {
        this.dao = dao;
    }

    public void setConfiguration(MetricsConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }

    public void shutdown() {
        shutdown = true;
    }

    public RawNumericMetric findLatestValueForResource(int scheduleId) {
        return dao.findLatestRawMetric(scheduleId);
    }

    public Iterable<MeasurementDataNumericHighLowComposite> findDataForResource(int scheduleId, long beginTime,
        long endTime) {
        DateTime begin = new DateTime(beginTime);

        if (dateTimeService.isInRawDataRange(begin)) {
            Iterable<RawNumericMetric> metrics = dao.findRawMetrics(scheduleId, beginTime, endTime);
            return createRawComposites(metrics, beginTime, endTime);
        }

        Iterable<AggregateNumericMetric> metrics = null;
        if (dateTimeService.isIn1HourDataRange(begin)) {
            metrics = dao.findOneHourMetrics(scheduleId, beginTime, endTime);
        } else if (dateTimeService.isIn6HourDataRnage(begin)) {
            metrics = dao.findSixHourMetrics(scheduleId, beginTime, endTime);
        } else if (dateTimeService.isIn24HourDataRnage(begin)) {
            metrics = dao.findTwentyFourHourMetrics(scheduleId, beginTime, endTime);
        } else {
            throw new IllegalArgumentException("beginTime[" + beginTime + "] is outside the accepted range.");
        }

        return createComposites(metrics, beginTime, endTime);
    }

    public List<MeasurementDataNumericHighLowComposite> findDataForGroup(List<Integer> scheduleIds, long beginTime,
        long endTime) {
        DateTime begin = new DateTime(beginTime);

        if (dateTimeService.isInRawDataRange(begin)) {
            Iterable<RawNumericMetric> metrics = dao.findRawMetrics(scheduleIds, beginTime, endTime);
            return createRawComposites(metrics, beginTime, endTime);
        }

        Iterable<AggregateNumericMetric> metrics = null;
        if (dateTimeService.isIn1HourDataRange(begin)) {
            metrics = dao.findOneHourMetrics(scheduleIds, beginTime, endTime);
        } else if (dateTimeService.isIn6HourDataRnage(begin)) {
            metrics = dao.findSixHourMetrics(scheduleIds, beginTime, endTime);
        } else if (dateTimeService.isIn24HourDataRnage(begin)) {
            metrics = dao.findTwentyFourHourMetrics(scheduleIds, beginTime, endTime);
        } else {
            throw new IllegalArgumentException("beginTime[" + beginTime + "] is outside the accepted range.");
        }

        return createComposites(metrics, beginTime, endTime);
    }

    public AggregateNumericMetric getSummaryAggregate(int scheduleId, long beginTime, long endTime) {
        long start = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Calculating resource summary aggregate for [scheduleId: " + scheduleId + ", beginTime: " +
                    beginTime + ", endTime: " + endTime + "]");
            }
            DateTime begin = new DateTime(beginTime);

            if (dateTimeService.isInRawDataRange(begin)) {
                Iterable<RawNumericMetric> metrics = dao.findRawMetrics(scheduleId, beginTime, endTime);
                return calculateAggregatedRaw(metrics, beginTime);
            }

            Iterable<AggregateNumericMetric> metrics = null;
            if (dateTimeService.isIn1HourDataRange(begin)) {
                metrics = dao.findOneHourMetrics(scheduleId, beginTime, endTime);
            } else if (dateTimeService.isIn6HourDataRnage(begin)) {
                metrics = dao.findSixHourMetrics(scheduleId, beginTime, endTime);
            } else if (dateTimeService.isIn24HourDataRnage(begin)) {
                metrics = dao.findTwentyFourHourMetrics(scheduleId, beginTime, endTime);
            } else {
                throw new IllegalArgumentException("beginTime[" + beginTime + "] is outside the accepted range.");
            }

            return calculateAggregate(metrics, beginTime);
        } finally {
            long end = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debug("Finished calculating resource summary aggregate in " + (end - start) + " ms");
            }
        }
    }

    public AggregateNumericMetric getSummaryAggregate(List<Integer> scheduleIds, long beginTime, long endTime) {
        long start = System.currentTimeMillis();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Calculating group summary aggregate for [scheduleIds: [" + StringUtil.listToString(
                    scheduleIds) + "], beginTime: " + beginTime + ", endTime: " + endTime + "]");
            }
            DateTime begin = new DateTime(beginTime);

            if (dateTimeService.isInRawDataRange(new DateTime(beginTime))) {
                Iterable<RawNumericMetric> metrics = dao.findRawMetrics(scheduleIds, beginTime, endTime);
                return calculateAggregatedRaw(metrics, beginTime);
            }

            Iterable<AggregateNumericMetric> metrics = null;
            if (dateTimeService.isIn1HourDataRange(begin)) {
                metrics = dao.findOneHourMetrics(scheduleIds, beginTime, endTime);
            } else if (dateTimeService.isIn6HourDataRnage(begin)) {
                metrics = dao.findSixHourMetrics(scheduleIds, beginTime, endTime);
            } else if (dateTimeService.isIn24HourDataRnage(begin)) {
                metrics = dao.findTwentyFourHourMetrics(scheduleIds, beginTime, endTime);
            } else {
                throw new IllegalArgumentException("beginTime[" + beginTime + "] is outside the accepted range.");
            }

            return calculateAggregate(metrics, beginTime);
        } finally {
            long end = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debug("Finished calculating group summary aggregate in " + (end - start) + " ms");
            }
        }
    }

    private List<MeasurementDataNumericHighLowComposite> createRawComposites(Iterable<RawNumericMetric> metrics,
        long beginTime, long endTime) {
        Buckets buckets = new Buckets(beginTime, endTime);
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
        long beginTime, long endTime) {
        Buckets buckets = new Buckets(beginTime, endTime);
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

    public void addNumericData(final Set<MeasurementDataNumeric> dataSet,
        final RawDataInsertedCallback callback) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Inserting " + dataSet.size() + " raw metrics");
            }

        final long startTime = System.currentTimeMillis();
        final AtomicInteger remainingInserts = new AtomicInteger(dataSet.size());

        for (final MeasurementDataNumeric data : dataSet) {
            semaphore.acquire();
            ResultSetFuture resultSetFuture = dao.insertRawData(data);
            Futures.addCallback(resultSetFuture, new FutureCallback<ResultSet>() {
                @Override
                public void onSuccess(ResultSet rows) {
                    updateMetricsIndex(data, dataSet.size(), remainingInserts, startTime, callback);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log.error("An error occurred while inserting raw data " + data, throwable);
                    callback.onFailure(throwable);
                    semaphore.release();
                }
            });
        }
        } catch (Exception e) {
            log.error("An error occurred while inserting raw numeric data", e);
            throw new RuntimeException(e);
        }
    }

    private void persistRawData(final Set<MeasurementDataNumeric> dataSet, final RawDataInsertedCallback callback) {
        final long startTime = System.currentTimeMillis();
        final AtomicInteger remainingInserts = new AtomicInteger(dataSet.size());

        for (final MeasurementDataNumeric data : dataSet) {
            ResultSetFuture resultSetFuture = dao.insertRawData(data);
            Futures.addCallback(resultSetFuture, new FutureCallback<ResultSet>() {
                @Override
                public void onSuccess(ResultSet rows) {
                    updateMetricsIndex(data, dataSet.size(), remainingInserts, startTime, callback);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log.error("An error occurred while inserting raw data " + data, throwable);
                    callback.onFailure(throwable);
                    semaphore.release();
                }
            });
        }
    }

    private void updateMetricsIndex(final MeasurementDataNumeric rawData, final int total,
        final AtomicInteger remainingInserts, final long startTime, final RawDataInsertedCallback callback) {

        long timeSlice = dateTimeService.getTimeSlice(new DateTime(rawData.getTimestamp()),
            configuration.getRawTimeSliceDuration()).getMillis();
        ResultSetFuture resultSetFuture = dao.updateMetricsIndex(MetricsTable.ONE_HOUR, rawData.getScheduleId(),
            timeSlice);
        Futures.addCallback(resultSetFuture, new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet rows) {
                callback.onSuccess(rawData);
                if (remainingInserts.decrementAndGet() == 0) {
                    long endTime = System.currentTimeMillis();
                    if (log.isDebugEnabled()) {
                        log.debug("Finished inserting " + total + " raw metrics in " + (endTime - startTime) + " ms");
                    }
                    callback.onFinish();
                }
                semaphore.release();
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("An error occurred while trying to update " + MetricsTable.INDEX + " for raw data " +
                    rawData);
                callback.onFailure(throwable);
                semaphore.release();
            }
        });
    }

    void updateMetricsIndex(Set<MeasurementDataNumeric> rawMetrics) {
        Map<Integer, Long> updates = new TreeMap<Integer, Long>();
        for (MeasurementDataNumeric rawMetric : rawMetrics) {
            updates.put(rawMetric.getScheduleId(), dateTimeService.getTimeSlice(
                new DateTime(rawMetric.getTimestamp()), configuration.getRawTimeSliceDuration()).getMillis());
        }
        Set<Date> dates = new HashSet<Date>();
        for (Long ts : updates.values()) {
            dates.add(new Date(ts));
        }
        log.debug("Updating one hour index wtih time slices " + dates);
        dao.updateMetricsIndex(MetricsTable.ONE_HOUR, updates);
    }

    /**
     * Computes and stores aggregate for all time slices that are ready to be aggregated.
     * This is includes raw, 1hr, 6hr, and 24hr data.
     *
     * @param startTime A timestamp that the determines the period over which data will be
     *                  aggregated. Aggregation is normally run hourly as part of the data
     *                  purge job; so, this timestamp is rounded down to the hour and then
     *                  an hour is subtracted to get the actual starting time over which
     *                  data will be aggregated. Suppose the argument has a value (time) of
     *                  02:00:34. This means that raw data that has been stored between
     *                  01:00 and 02:00 will be aggregated.
     *
     * @return One hour aggregates. That is, any raw data that has been rolled up into onr
     * one hour aggregates. The one hour aggregates are returned because they are needed
     * for subsequently computing baselines.
     */
    public Iterable<AggregateNumericMetric> calculateAggregates(long startTime) {
        DateTime dt = new DateTime(startTime);
        DateTime currentHour = dateTimeService.getTimeSlice(dt, configuration.getRawTimeSliceDuration());
        DateTime lastHour = currentHour.minusHours(1);

        long hourTimeSlice = lastHour.getMillis();

        long sixHourTimeSlice = dateTimeService.getTimeSlice(lastHour,
            configuration.getOneHourTimeSliceDuration()).getMillis();

        long twentyFourHourTimeSlice = dateTimeService.getTimeSlice(lastHour,
            configuration.getSixHourTimeSliceDuration()).getMillis();

        // We first query the metrics index table to determine which schedules have data to
        // be aggregated. Then we retrieve the metric data and aggregate or compress the
        // data, writing the compressed values into the next wider (i.e., longer life span
        // for data) bucket/table. At this point we remove the index entries for the data
        // that has already been processed. We purge the entire row in the index table.
        // We can safely do this because the row wi..
        //
        // The last step in the work flow is to update the metrics
        // index for the newly persisted aggregates.

        Iterable<AggregateNumericMetric> newOneHourAggregates = null;

        List<AggregateNumericMetric> updatedSchedules = aggregateRawData(hourTimeSlice);
        newOneHourAggregates = updatedSchedules;
        if (!updatedSchedules.isEmpty()) {
            dao.deleteMetricsIndexEntries(MetricsTable.ONE_HOUR, hourTimeSlice);
            updateMetricsIndex(MetricsTable.SIX_HOUR, updatedSchedules, configuration.getOneHourTimeSliceDuration());
        }

        updatedSchedules = calculateAggregates(MetricsTable.ONE_HOUR, MetricsTable.SIX_HOUR, sixHourTimeSlice,
            configuration.getOneHourTimeSliceDuration());
        if (!updatedSchedules.isEmpty()) {
            dao.deleteMetricsIndexEntries(MetricsTable.SIX_HOUR, sixHourTimeSlice);
            updateMetricsIndex(MetricsTable.TWENTY_FOUR_HOUR, updatedSchedules,
                configuration.getSixHourTimeSliceDuration());
        }

        updatedSchedules = calculateAggregates(MetricsTable.SIX_HOUR, MetricsTable.TWENTY_FOUR_HOUR,
            twentyFourHourTimeSlice, configuration.getSixHourTimeSliceDuration());
        if (!updatedSchedules.isEmpty()) {
            dao.deleteMetricsIndexEntries(MetricsTable.TWENTY_FOUR_HOUR, twentyFourHourTimeSlice);
        }

        return newOneHourAggregates;
    }

    private void updateMetricsIndex(MetricsTable bucket, Iterable<AggregateNumericMetric> metrics, Duration duration) {
        Map<Integer, Long> updates = new TreeMap<Integer, Long>();
        for (AggregateNumericMetric metric : metrics) {
            updates.put(metric.getScheduleId(),
                dateTimeService.getTimeSlice(new DateTime(metric.getTimestamp()), duration).getMillis());
        }
        dao.updateMetricsIndex(bucket, updates);
    }

    private List<AggregateNumericMetric> aggregateRawData(long theHour) {
        Iterable<MetricsIndexEntry> indexEntries = dao.findMetricsIndexEntries(MetricsTable.ONE_HOUR, theHour);
        List<AggregateNumericMetric> oneHourMetrics = new ArrayList<AggregateNumericMetric>();

        for (MetricsIndexEntry indexEntry : indexEntries) {
            DateTime startTime = indexEntry.getTime();
            DateTime endTime = startTime.plusMinutes(60);

            Iterable<RawNumericMetric> rawMetrics = dao.findRawMetrics(indexEntry.getScheduleId(),
                startTime.getMillis(), endTime.getMillis());
            AggregateNumericMetric aggregatedRaw = calculateAggregatedRaw(rawMetrics, startTime.getMillis());
            aggregatedRaw.setScheduleId(indexEntry.getScheduleId());
            oneHourMetrics.add(aggregatedRaw);
        }

        for (AggregateNumericMetric metric : oneHourMetrics) {
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }
        return oneHourMetrics;
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

    private List<AggregateNumericMetric> calculateAggregates(MetricsTable fromTable,
        MetricsTable toTable, long timeSlice, Duration nextDuration) {

        Iterable<MetricsIndexEntry> indexEntries = dao.findMetricsIndexEntries(toTable, timeSlice);
        List<AggregateNumericMetric> toMetrics = new ArrayList<AggregateNumericMetric>();

        DateTime currentHour = getCurrentHour();
        DateTimeComparator dateTimeComparator = DateTimeComparator.getInstance();

        for (MetricsIndexEntry indexEntry : indexEntries) {
            DateTime startTime = indexEntry.getTime();
            DateTime endTime = startTime.plus(nextDuration);

            if (dateTimeComparator.compare(currentHour, endTime) < 0) {
                continue;
            }

            Iterable<AggregateNumericMetric> metrics = null;
            switch (fromTable) {
                case ONE_HOUR:
                    metrics = dao.findOneHourMetrics(indexEntry.getScheduleId(), startTime.getMillis(),
                        endTime.getMillis());
                    break;
                case SIX_HOUR:
                    metrics = dao.findSixHourMetrics(indexEntry.getScheduleId(), startTime.getMillis(),
                        endTime.getMillis());
                    break;
                default:  // 24 hour
                    metrics = dao.findTwentyFourHourMetrics(indexEntry.getScheduleId(), startTime.getMillis(),
                        endTime.getMillis());
                    break;
            }
            AggregateNumericMetric aggregatedMetric = calculateAggregate(metrics, startTime.getMillis());
            aggregatedMetric.setScheduleId(indexEntry.getScheduleId());
            toMetrics.add(aggregatedMetric);
        }

        switch (toTable) {
            case ONE_HOUR:
                insertOneHourAggregates(toMetrics);
                break;
            case SIX_HOUR:
                insertSixHourAggregates(toMetrics);
                break;
            default:  // 24 hour
                insertTwentyFourHourAggregates(toMetrics);
        }
        return toMetrics;
    }

    private void insertOneHourAggregates(List<AggregateNumericMetric> metrics) {
        for (AggregateNumericMetric metric : metrics) {
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertOneHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }
    }

    private void insertSixHourAggregates(List<AggregateNumericMetric> metrics) {
        for (AggregateNumericMetric metric : metrics) {
            dao.insertSixHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN, metric.getMin());
            dao.insertSixHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX, metric.getMax());
            dao.insertSixHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG, metric.getAvg());
        }
    }

    private void insertTwentyFourHourAggregates(List<AggregateNumericMetric> metrics) {
        for (AggregateNumericMetric metric : metrics) {
            dao.insertTwentyFourHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MIN,
                metric.getMin());
            dao.insertTwentyFourHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.MAX,
                metric.getMax());
            dao.insertTwentyFourHourData(metric.getScheduleId(), metric.getTimestamp(), AggregateType.AVG,
                metric.getAvg());
        }
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

//    public void addTraitData(Set<MeasurementDataTrait> dataSet) {
//        Mutator<Integer> mutator = HFactory.createMutator(keyspace, IntegerSerializer.get());
//        Mutator<Integer> indexMutator = HFactory.createMutator(keyspace, IntegerSerializer.get());
//
//        for (MeasurementDataTrait trait : dataSet) {
//            mutator.addInsertion(
//                trait.getScheduleId(),
//                traitsCF,
//                HFactory.createColumn(trait.getTimestamp(), trait.getValue(), DateTimeService.ONE_YEAR,
//                    LongSerializer.get(), StringSerializer.get()));
//
//            Composite composite = new Composite();
//            composite.addComponent(trait.getTimestamp(), LongSerializer.get());
//            composite.addComponent(trait.getScheduleId(), IntegerSerializer.get());
//            composite.addComponent(trait.getDefinitionId(), IntegerSerializer.get());
//            composite.addComponent(trait.getDisplayType().ordinal(), IntegerSerializer.get());
//            composite.addComponent(trait.getDisplayName(), StringSerializer.get());
//
//            indexMutator.addInsertion(trait.getResourceId(), resourceTraitsCF,
//                HFactory.createColumn(composite, trait.getValue(), CompositeSerializer.get(), StringSerializer.get()));
//        }
//
//        mutator.execute();
//        indexMutator.execute();
//    }

//    public void addCallTimeData(Set<CallTimeData> callTimeDatas) {
//    }

    protected DateTime getCurrentHour() {
        DateTime now = new DateTime();
        return now.hourOfDay().roundFloorCopy();
    }
}
