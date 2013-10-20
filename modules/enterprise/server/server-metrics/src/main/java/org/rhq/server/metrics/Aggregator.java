package org.rhq.server.metrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.Duration;

import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.MetricsTable;

/**
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

    private CountDownLatch rawDataIndexEntriesArrival;

    private RateLimiter readPermits;
    private RateLimiter writePermits;

    private int batchSize;

    private AggregationState state;

    private Set<AggregateNumericMetric> oneHourData;

    private AtomicInteger remainingIndexEntries;

    public Aggregator(ListeningExecutorService aggregationTasks, MetricsDAO dao, MetricsConfiguration configuration,
        DateTimeService dtService, DateTime startTime, int batchSize, RateLimiter writePermits,
        RateLimiter readPermits) {
        this.dao = dao;
        this.configuration = configuration;
        this.dtService = dtService;
        this.startTime = startTime;
        this.readPermits = readPermits;
        this.writePermits = writePermits;
        this.batchSize = batchSize;
        oneHourData = new ConcurrentSkipListSet<AggregateNumericMetric>(AGGREGATE_COMPARATOR);
        rawDataIndexEntriesArrival = new CountDownLatch(1);
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
            .setCompute1HourData(new Compute1HourData(startTime, sixHourTimeSlice, writePermits, dao, oneHourData))
            .setCompute6HourData(new Compute6HourData(sixHourTimeSlice, twentyFourHourTimeSlice, writePermits, dao))
            .set6HourTimeSliceFinished(hasTimeSliceEnded(sixHourTimeSlice, configuration.getOneHourTimeSliceDuration()))
            .set24HourTimeSliceFinished(hasTimeSliceEnded(twentyFourHourTimeSlice,
                configuration.getSixHourTimeSliceDuration()))
            .setOneHourIndexEntries(new TreeSet<Integer>())
            .setSixHourIndexEntries(new TreeSet<Integer>())
            .setOneHourIndexEntriesLock(new ReentrantReadWriteLock())
            .setSixHourIndexEntriesLock(new ReentrantReadWriteLock())
            .setCompletionOfRawDataAggregation(new CountDownLatch(1))
            .setCompletionOf1HourDataAggregation(new CountDownLatch(1));

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
        log.debug("There are " + remainingIndexEntries.get() + " remaining index entries");
        readPermits.acquire();
        StorageResultSetFuture rawFuture = dao.findMetricsIndexEntriesAsync(MetricsTable.ONE_HOUR,
            startTime.getMillis());
        Futures.addCallback(rawFuture, new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet result) {
                List<Row> rows = result.all();
                state.setRemainingRawData(new AtomicInteger(rows.size()));
                rawDataIndexEntriesArrival.countDown();
                if (rows.isEmpty()) {
                    deleteIndexEntries(MetricsTable.ONE_HOUR);
                } else {
                    deleteIndexEntries(MetricsTable.ONE_HOUR);
                    log.debug("Starting raw data aggregation for " + rows.size() + " schedules");
                    long start = System.currentTimeMillis();
                    final DateTime endTime = startTime.plus(configuration.getRawTimeSliceDuration());
                    Set<Integer> scheduleIds = new TreeSet<Integer>();
                    List<StorageResultSetFuture> rawDataFutures = new ArrayList<StorageResultSetFuture>(batchSize);
                    for (final Row row : rows) {
                        scheduleIds.add(row.getInt(1));
                        readPermits.acquire();
                        rawDataFutures.add(dao.findRawMetricsAsync(row.getInt(1), startTime.getMillis(),
                            endTime.getMillis()));
                        if (rawDataFutures.size() == batchSize) {
                            state.getAggregationTasks().submit(new AggregateRawData(dao, state, scheduleIds,
                                rawDataFutures));
                            rawDataFutures = new ArrayList<StorageResultSetFuture>();
                            scheduleIds = new TreeSet<Integer>();
                        }
                    }
                    if (!rawDataFutures.isEmpty()) {
                        state.getAggregationTasks().submit(new AggregateRawData(dao, state, scheduleIds,
                            rawDataFutures));
                    }
                    log.debug("Finished processing one hour index entries in " + (System.currentTimeMillis() - start) +
                        " ms");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Failed to retrieve raw data index entries. Raw data aggregation for time slice [" +
                    startTime + "] cannot proceed.", t);
                state.getRemainingRawData().getAndSet(0);
                deleteIndexEntries(MetricsTable.ONE_HOUR);
            }
        }, state.getAggregationTasks());

        if (state.is6HourTimeSliceFinished()) {
            log.debug("Fetching 1 hour index entries");
            StorageResultSetFuture oneHourFuture = dao.findMetricsIndexEntriesAsync(MetricsTable.SIX_HOUR,
                get6HourTimeSlice().getMillis());
            Futures.addCallback(oneHourFuture, new FutureCallback<ResultSet>() {
                @Override
                public void onSuccess(ResultSet resultSet) {
                    for (Row row : resultSet) {
                        // No need to get the write lock here because there won't be concurrent writes until after
                        // state.getOneHourIndexEntriesArrival().countDown() is called
                        state.getOneHourIndexEntries().add(row.getInt(1));
                    }
                    state.setRemaining1HourData(new AtomicInteger(state.getOneHourIndexEntries().size()));
                    state.getOneHourIndexEntriesArrival().countDown();
                    log.debug("Finished loading 1 hour index entries");
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("Failed to retrieve one hour aggregate index entries for time slice [" +
                        get6HourTimeSlice() + "]. Some six hour aggregates may not get generated.");
                    state.getOneHourIndexEntriesArrival().abort();
                }
            });
        }

//        if (state.is24HourTimeSliceFinished()) {
//            deleteIndexEntries(MetricsTable.TWENTY_FOUR_HOUR);
//            log.debug("Fetching 6 hour index entries");
//            StorageResultSetFuture sixHourFuture = dao.findMetricsIndexEntriesAsync(MetricsTable.TWENTY_FOUR_HOUR,
//                get24HourTimeSlice().getMillis());
//            Futures.addCallback(sixHourFuture, new FutureCallback<ResultSet>() {
//                @Override
//                public void onSuccess(ResultSet result) {
//                    List<Row> rows = result.all();
//                    if (rows.isEmpty()) {
//                        deleteIndexEntries(MetricsTable.TWENTY_FOUR_HOUR);
//                    } else {
//                        MetricsIndexEntryMapper mapper = new MetricsIndexEntryMapper(MetricsTable.TWENTY_FOUR_HOUR);
//
//                        for (Row row : rows) {
//                            MetricsIndexEntry indexEntry = mapper.map(row);
//                            state.getSixHourIndexEntries().add(indexEntry.getScheduleId());
//                        }
//                        state.setRemaining6HourData(new AtomicInteger(state.getSixHourIndexEntries().size()));
//                    }
//                    state.getSixHourIndexEntriesArrival().countDown();
//                    log.debug("Finished loading 6 hour index entries");
//                }
//
//                @Override
//                public void onFailure(Throwable t) {
//                    log.warn("Failed to retrieve 6 hour aggregate index entries for time slice [" +
//                        get24HourTimeSlice() + "]. Some 24 hour aggregates may not get generated.");
//                    state.getSixHourIndexEntriesArrival().abort();
//                    deleteIndexEntries(MetricsTable.TWENTY_FOUR_HOUR);
//                }
//            });
//        }

        try {
            if (state.is6HourTimeSliceFinished()) {
                rawDataIndexEntriesArrival.await();
                waitForRawAggregationToComplete();
                deleteIndexEntries(MetricsTable.SIX_HOUR);
                // Now queue up remaining 1 hour data aggregation
                try {
                    log.debug("Remaining schedule ids for 1 hour aggregation: " + state.getOneHourIndexEntries());
                    List<StorageResultSetFuture> queryFutures = new ArrayList<StorageResultSetFuture>(batchSize);
                    Set<Integer> scheduleIds = new TreeSet<Integer>();
                    state.getOneHourIndexEntriesLock().writeLock().lock();
                    for (Integer scheduleId : state.getOneHourIndexEntries()) {
                        queryFutures.add(dao.findOneHourMetricsAsync(scheduleId, state.getSixHourTimeSlice().getMillis(),
                            state.getSixHourTimeSliceEnd().getMillis()));
                        scheduleIds.add(scheduleId);
                        if (queryFutures.size() == batchSize) {
                            log.debug("Submitting 1 hour aggregation task for schedule ids " + scheduleIds);
                            state.getAggregationTasks().submit(new Aggregate1HourData(dao, state, scheduleIds,
                                queryFutures));
                            queryFutures = new ArrayList<StorageResultSetFuture>(batchSize);
                            scheduleIds = new TreeSet<Integer>();
                        }
                    }
                    if (!queryFutures.isEmpty()) {
                        log.debug("Submitting 1 hour aggregation task for schedule ids " + scheduleIds);
                        state.getAggregationTasks().submit(new Aggregate1HourData(dao, state, scheduleIds,
                            queryFutures));
                        queryFutures = null;
                        scheduleIds = null;
                    }
                } finally {
                    state.getOneHourIndexEntriesLock().writeLock().unlock();
                }
            }

            while (remainingIndexEntries.get() > 0 || state.getRemaining1HourData().get() > 0) {
                Thread.sleep(50);
            }
            log.debug("Aggregation is done. There are " + remainingIndexEntries.get() + " remaining index entries");
        } catch (InterruptedException e) {
            log.warn("An interrupt occurred while waiting for aggregation to finish", e);
        }
        return oneHourData;
    }

    private void waitForRawAggregationToComplete() throws InterruptedException {
        while (state.getRemainingRawData().get() > 0) {
            Thread.sleep(50);
        }
    }

    private void deleteIndexEntries(final MetricsTable table) {
        DateTime time;
        switch (table) {
        case ONE_HOUR:
            time = startTime;
            break;
        case SIX_HOUR:
            time = get6HourTimeSlice();
            break;
        default:
            time = get24HourTimeSlice();
            break;
        }
        log.debug("Deleting " + table + " index entries for time slice " + time);
        writePermits.acquire();
        StorageResultSetFuture future = dao.deleteMetricsIndexEntriesAsync(table, time.getMillis());
        Futures.addCallback(future, new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet result) {
                remainingIndexEntries.decrementAndGet();
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Failed to delete index entries for table " + table + " at time [" + startTime + "]");
                remainingIndexEntries.decrementAndGet();
            }
        });
    }

}