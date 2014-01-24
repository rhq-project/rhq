package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.Duration;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.server.metrics.AbortedException;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsConfiguration;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.SignalingCountDownLatch;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * This class provides the main interface for metric data aggregation.
 *
 * @author John Sanda
 */
public class Aggregator {

    private static final Comparator<AggregateNumericMetric> AGGREGATE_COMPARATOR = new Comparator<AggregateNumericMetric>() {
        @Override
        public int compare(AggregateNumericMetric left, AggregateNumericMetric right) {
            return (left.getScheduleId() < right.getScheduleId()) ? -1 : ((left.getScheduleId() == right.getScheduleId()) ? 0 : 1);
        }
    };

    private final Log log = LogFactory.getLog(Aggregator.class);

    private MetricsDAO dao;

    private MetricsConfiguration configuration;

    private DateTimeService dtService;

    private DateTime startTime;

    /**
     * Signals when raw data index entries (in metrics_index) can be deleted. We cannot delete the row in metrics_index
     * until we know that it has been read, successfully or otherwise.
     */
    private SignalingCountDownLatch rawDataIndexEntriesArrival;

    private int batchSize;

    private AggregationState state;

    private Set<AggregateNumericMetric> oneHourData;

    private AtomicInteger remainingIndexEntries;

    public Aggregator(ListeningExecutorService aggregationTasks, MetricsDAO dao, MetricsConfiguration configuration,
        DateTimeService dtService, DateTime startTime, int batchSize) {
        this.dao = dao;
        this.configuration = configuration;
        this.dtService = dtService;
        this.startTime = startTime;
        this.batchSize = batchSize;
        oneHourData = new ConcurrentSkipListSet<AggregateNumericMetric>(AGGREGATE_COMPARATOR);
        rawDataIndexEntriesArrival = new SignalingCountDownLatch(new CountDownLatch(1));
        remainingIndexEntries = new AtomicInteger(1);

        DateTime sixHourTimeSlice = get6HourTimeSlice();
        DateTime twentyFourHourTimeSlice = get24HourTimeSlice();

        state = new AggregationState()
            .setAggregationTasks(aggregationTasks)
            .setOneHourTimeSlice(startTime)
            .setSixHourTimeSlice(sixHourTimeSlice)
            .setSixHourTimeSliceEnd(sixHourTimeSlice.plus(configuration.getOneHourTimeSliceDuration()))
            .setTwentyFourHourTimeSlice(twentyFourHourTimeSlice)
            .setTwentyFourHourTimeSliceEnd(twentyFourHourTimeSlice.plus(configuration.getSixHourTimeSliceDuration()))
            .setCompute1HourData(new Compute1HourData(startTime, sixHourTimeSlice, dao, oneHourData))
            .setCompute6HourData(new Compute6HourData(sixHourTimeSlice, twentyFourHourTimeSlice, dao))
            .setCompute24HourData(new Compute24HourData(twentyFourHourTimeSlice, dao))
            .set6HourTimeSliceFinished(hasTimeSliceEnded(sixHourTimeSlice, configuration.getOneHourTimeSliceDuration()))
            .set24HourTimeSliceFinished(hasTimeSliceEnded(twentyFourHourTimeSlice,
                configuration.getSixHourTimeSliceDuration()))
            .setRemainingRawData(new AtomicInteger(0))
            .setRemaining1HourData(new AtomicInteger(0))
            .setRemaining6HourData(new AtomicInteger(0))
            .setOneHourIndexEntries(new TreeSet<Integer>())
            .setSixHourIndexEntries(new TreeSet<Integer>())
            .setOneHourIndexEntriesLock(new ReentrantReadWriteLock())
            .setSixHourIndexEntriesLock(new ReentrantReadWriteLock());

        if (state.is6HourTimeSliceFinished()) {
            state.setOneHourIndexEntriesArrival(new SignalingCountDownLatch(new CountDownLatch(1)));
            remainingIndexEntries.incrementAndGet();
        } else {
            state.setOneHourIndexEntriesArrival(new SignalingCountDownLatch(new CountDownLatch(0)));
            state.setRemaining1HourData(new AtomicInteger(0));
        }

        if (state.is24HourTimeSliceFinished()) {
            state.setSixHourIndexEntriesArrival(new SignalingCountDownLatch(new CountDownLatch(1)));
            remainingIndexEntries.incrementAndGet();
        } else {
            state.setSixHourIndexEntriesArrival(new SignalingCountDownLatch(new CountDownLatch(0)));
            state.setRemaining6HourData(new AtomicInteger(0));
        }
    }

    private DateTime get24HourTimeSlice() {
        return dtService.getTimeSlice(startTime, configuration.getSixHourTimeSliceDuration());
    }

    private DateTime get6HourTimeSlice() {
        return dtService.getTimeSlice(startTime, configuration.getOneHourTimeSliceDuration());
    }

    private boolean hasTimeSliceEnded(DateTime startTime, Duration duration) {
        DateTime endTime = startTime.plus(duration);
        return DateTimeComparator.getInstance().compare(currentHour(), endTime) >= 0;
    }

    protected DateTime currentHour() {
        return dtService.getTimeSlice(dtService.now(), configuration.getRawTimeSliceDuration());
    }

    public Set<AggregateNumericMetric> run() {
        log.info("Starting aggregation for time slice " + startTime);
        StorageResultSetFuture rawFuture = dao.findMetricsIndexEntriesAsync(MetricsTable.ONE_HOUR,
            startTime.getMillis());
        Futures.addCallback(rawFuture, new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet result) {
                int schedules = 0;
                List<Row> rows = result.all();
                log.debug("Retrieved " + rows.size() + " schedule ids from raw data index");
                state.getRemainingRawData().set(rows.size());
                rawDataIndexEntriesArrival.countDown();

                Stopwatch stopwatch = new Stopwatch().start();

                final DateTime endTime = startTime.plus(configuration.getRawTimeSliceDuration());
                Set<Integer> scheduleIds = new TreeSet<Integer>();
                List<StorageResultSetFuture> rawDataFutures = new ArrayList<StorageResultSetFuture>(batchSize);
                for (final Row row : rows) {
                    scheduleIds.add(row.getInt(1));
                    rawDataFutures.add(dao.findRawMetricsAsync(row.getInt(1), startTime.getMillis(),
                        endTime.getMillis()));
                    if (rawDataFutures.size() == batchSize) {
                        state.getAggregationTasks().submit(new AggregateRawData(dao, state, scheduleIds,
                            rawDataFutures));
                        schedules += rawDataFutures.size();
                        rawDataFutures = new ArrayList<StorageResultSetFuture>();
                        scheduleIds = new TreeSet<Integer>();
                    }
                }
                if (!rawDataFutures.isEmpty()) {
                    state.getAggregationTasks().submit(new AggregateRawData(dao, state, scheduleIds,
                        rawDataFutures));
                    schedules += rawDataFutures.size();
                }

                if (log.isDebugEnabled()) {
                    stopwatch.stop();
                    log.debug("Finished scheduling raw data aggregation tasks for " + schedules + " schedules in " +
                        stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                if (log.isDebugEnabled()) {
                    log.debug("Aggregation for time slice [" + startTime + "] cannot proceed. There was an " +
                        "unexpected error while retrieving raw data index entries.", t);
                } else {
                    log.warn("Aggregation for time slice [" + startTime + "] cannot proceed. There was an " +
                        "unexpected error while retrieving raw data index entries: " + ThrowableUtil.getRootMessage(t));
                }
                state.setRemainingRawData(new AtomicInteger(0));
                rawDataIndexEntriesArrival.abort();
                deleteIndexEntries(MetricsTable.ONE_HOUR);
            }
        }, state.getAggregationTasks());

        if (state.is6HourTimeSliceFinished()) {
            log.debug("Fetching 1 hour index entries");
            Stopwatch stopwatch = new Stopwatch().start();
            StorageResultSetFuture oneHourFuture = dao.findMetricsIndexEntriesAsync(MetricsTable.SIX_HOUR,
                state.getSixHourTimeSlice().getMillis());
            Futures.addCallback(oneHourFuture, new AggregateIndexEntriesHandler(state.getOneHourIndexEntries(),
                state.getRemaining1HourData(), state.getOneHourIndexEntriesArrival(), stopwatch, "1 hour", "6 hour"),
                state.getAggregationTasks());
        }

        if (state.is24HourTimeSliceFinished()) {
            log.debug("Fetching 6 hour index entries");
            Stopwatch stopwatch = new Stopwatch().start();
            StorageResultSetFuture sixHourFuture = dao.findMetricsIndexEntriesAsync(MetricsTable.TWENTY_FOUR_HOUR,
                state.getTwentyFourHourTimeSlice().getMillis());
            Futures.addCallback(sixHourFuture, new AggregateIndexEntriesHandler(state.getSixHourIndexEntries(),
                state.getRemaining6HourData(), state.getSixHourIndexEntriesArrival(), stopwatch, "6 hour", "24 hour"),
                state.getAggregationTasks());
        }

        try {
            try {
                rawDataIndexEntriesArrival.await();
                deleteIndexEntries(MetricsTable.ONE_HOUR);
            } catch (AbortedException e) {
            }

            if (state.is6HourTimeSliceFinished()) {
                waitFor(state.getRemainingRawData());
                try {
                    state.getOneHourIndexEntriesArrival().await();
                    deleteIndexEntries(MetricsTable.SIX_HOUR);

                    List<StorageResultSetFuture> queryFutures = new ArrayList<StorageResultSetFuture>(batchSize);
                    Set<Integer> scheduleIds = new TreeSet<Integer>();
                    state.getOneHourIndexEntriesLock().writeLock().lock();
                    log.debug("Preparing to submit 1 hour data aggregation tasks for " +
                        state.getOneHourIndexEntries().size() + " schedules");
                    for (Integer scheduleId : state.getOneHourIndexEntries()) {
                        queryFutures.add(dao.findOneHourMetricsAsync(scheduleId, state.getSixHourTimeSlice().getMillis(),
                            state.getSixHourTimeSliceEnd().getMillis()));
                        scheduleIds.add(scheduleId);
                        if (queryFutures.size() == batchSize) {
                            state.getAggregationTasks().submit(new Aggregate1HourData(dao, state, scheduleIds,
                                queryFutures));
                            queryFutures = new ArrayList<StorageResultSetFuture>(batchSize);
                            scheduleIds = new TreeSet<Integer>();
                        }
                    }
                    if (!queryFutures.isEmpty()) {
                        state.getAggregationTasks().submit(new Aggregate1HourData(dao, state, scheduleIds,
                            queryFutures));
                        queryFutures = null;
                        scheduleIds = null;
                    }
                } catch (AbortedException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Some 6 hour aggregates may not get generated. There was an unexpected error while " +
                            "loading 1 hour index entries", e);
                    } else {
                        log.warn("Some 6 hour aggregates may not get generated. There was an unexpected error while " +
                            "loading 1 hour index entries: " + ThrowableUtil.getRootMessage(e));
                    }
                } finally {
                    state.getOneHourIndexEntriesLock().writeLock().unlock();
                }
            }

            if (state.is24HourTimeSliceFinished()) {
                waitFor(state.getRemaining1HourData());
                try {
                    state.getSixHourIndexEntriesArrival().await();
                    deleteIndexEntries(MetricsTable.TWENTY_FOUR_HOUR);

                    List<StorageResultSetFuture> queryFutures = new ArrayList<StorageResultSetFuture>(batchSize);
                    Set<Integer> scheduleIds = new TreeSet<Integer>();
                    state.getSixHourIndexEntriesLock().writeLock().lock();
                    log.debug("Preparing to submit 6 hour data aggregation tasks for " +
                        state.getSixHourIndexEntries().size() + " schedules");
                    for (Integer scheduleId : state.getSixHourIndexEntries()) {
                        queryFutures.add(dao.findSixHourMetricsAsync(scheduleId, state.getTwentyFourHourTimeSlice().getMillis(),
                            state.getTwentyFourHourTimeSliceEnd().getMillis()));
                        scheduleIds.add(scheduleId);
                        if (queryFutures.size() == batchSize) {
                            state.getAggregationTasks().submit(new Aggregate6HourData(dao, state, scheduleIds,
                                queryFutures));
                            queryFutures = new ArrayList<StorageResultSetFuture>(batchSize);
                            scheduleIds = new TreeSet<Integer>();
                        }
                    }
                    if (!queryFutures.isEmpty()) {
                        state.getAggregationTasks().submit(new Aggregate6HourData(dao, state, scheduleIds,
                            queryFutures));
                        queryFutures = null;
                        scheduleIds = null;
                    }
                } catch (AbortedException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Some 24 hour aggregates may not get generated. There was an unexpected error while " +
                            "loading 6 hour index entries", e);
                    } else {
                        log.warn("Some 24 hour aggregates may not get generated. There was an unexpected error while " +
                            "loading 6 hour index entries: " + ThrowableUtil.getRootMessage(e));
                    }
                } finally {
                    state.getSixHourIndexEntriesLock().writeLock().unlock();
                }
            }

            while (!isAggregationFinished()) {
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            if (log.isDebugEnabled()) {
                log.debug("An interrupt occurred while waiting for aggregation to finish. Aborting remaining work.", e);
            } else {
                log.warn("An interrupt occurred while waiting for aggregation to finish. Aborting remaining work: " +
                    ThrowableUtil.getRootMessage(e));
            }
            log.warn("An interrupt occurred while waiting for aggregation to finish", e);
        }
        return oneHourData;
    }

    private void waitFor(AtomicInteger remainingData) throws InterruptedException {
        while (remainingData.get() > 0) {
            Thread.sleep(50);
        }
    }

    private boolean isAggregationFinished() {
        return state.getRemainingRawData().get() <= 0 && state.getRemaining1HourData().get() <= 0 &&
            state.getRemaining6HourData().get() <= 0 && remainingIndexEntries.get() <= 0;
    }

    private void deleteIndexEntries(final MetricsTable table) {
        final DateTime time;
        switch (table) {
        case ONE_HOUR:
            time = startTime;
            break;
        case SIX_HOUR:
            time = state.getSixHourTimeSlice();
            break;
        default:
            time = state.getTwentyFourHourTimeSlice();
            break;
        }
        log.debug("Deleting " + table + " index entries for time slice " + time);
        StorageResultSetFuture future = dao.deleteMetricsIndexEntriesAsync(table, time.getMillis());
        Futures.addCallback(future, new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet result) {
                remainingIndexEntries.decrementAndGet();
            }

            @Override
            public void onFailure(Throwable t) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to delete index entries for table " + table + " at time [" + time + "]. An " +
                        "unexpected error occurred.", t);
                } else {
                    log.warn("Failed to delete index entries for table " + table + " at time [" + time + "]. An " +
                        "unexpected error occurred: " + ThrowableUtil.getRootMessage(t));
                }
                remainingIndexEntries.decrementAndGet();
            }
        });
    }

}
