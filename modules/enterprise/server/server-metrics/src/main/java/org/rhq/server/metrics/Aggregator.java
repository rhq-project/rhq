package org.rhq.server.metrics;

import static org.rhq.server.metrics.MetricsUtil.METRICS_INDEX_ROW_SIZE;
import static org.rhq.server.metrics.MetricsUtil.indexPartitionKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    private RateLimiter readPermits;
    private RateLimiter writePermits;

    private int batchSize;

    private AggregationState state;

    private Set<AggregateNumericMetric> oneHourData;

    private int startScheduleId;

    private int endScheduleId;

    public Aggregator(ListeningExecutorService aggregationTasks, MetricsDAO dao, MetricsConfiguration configuration,
        DateTimeService dtService, DateTime startTime, int batchSize, RateLimiter writePermits,
        RateLimiter readPermits, int startScheduleId, int endScheduleId) {
        this.dao = dao;
        this.configuration = configuration;
        this.dtService = dtService;
        this.startTime = startTime;
        this.readPermits = readPermits;
        this.writePermits = writePermits;
        this.batchSize = batchSize;
        oneHourData = new ConcurrentSkipListSet<AggregateNumericMetric>(AGGREGATE_COMPARATOR);
        this.startScheduleId = startScheduleId;
        this.endScheduleId = endScheduleId;

        DateTime sixHourTimeSlice = get6HourTimeSlice();
        DateTime twentyFourHourTimeSlice = get24HourTimeSlice();

        log.debug("6 hour time slice = " + sixHourTimeSlice);
        log.debug("24 hour time slice = " + twentyFourHourTimeSlice);

        int numPartitions = (endScheduleId - startScheduleId) / METRICS_INDEX_ROW_SIZE;
        if ((endScheduleId - startScheduleId) % METRICS_INDEX_ROW_SIZE != 0) {
            numPartitions++;
        }

        state = new AggregationState()
            .setAggregationTasks(aggregationTasks)
            .setOneHourTimeSlice(startTime)
            .setOneHourTimeSliceEnd(startTime.plus(configuration.getRawTimeSliceDuration()))
            .setSixHourTimeSlice(sixHourTimeSlice)
            .setSixHourTimeSliceEnd(sixHourTimeSlice.plus(configuration.getOneHourTimeSliceDuration()))
            .setTwentyFourHourTimeSlice(twentyFourHourTimeSlice)
            .setTwentyFourHourTimeSliceEnd(twentyFourHourTimeSlice.plus(configuration.getSixHourTimeSliceDuration()))
            .setRawIndexEntriesArrival(new CountDownLatch(numPartitions))
            .setCompute1HourData(new Compute1HourData(startTime, sixHourTimeSlice, writePermits, dao, oneHourData))
            .setCompute6HourData(new Compute6HourData(sixHourTimeSlice, twentyFourHourTimeSlice, writePermits, dao))
            .setCompute24HourData(new Compute24HourData(twentyFourHourTimeSlice, writePermits, dao))
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
            state.setOneHourIndexEntriesArrival(new SignalingCountDownLatch(new CountDownLatch(numPartitions)));
        } else {
            state.setOneHourIndexEntriesArrival(new SignalingCountDownLatch(new CountDownLatch(0)));
            state.setRemaining1HourData(new AtomicInteger(0));
        }

        if (state.is24HourTimeSliceFinished()) {
            state.setSixHourIndexEntriesArrival(new SignalingCountDownLatch(new CountDownLatch(numPartitions)));
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
        log.debug("Loading raw index entries");
        for (int scheduleId = startScheduleId; scheduleId <= endScheduleId; scheduleId += METRICS_INDEX_ROW_SIZE) {
            String partitionKey = indexPartitionKey(MetricsTable.ONE_HOUR, scheduleId);
            readPermits.acquire();
            StorageResultSetFuture indexFuture = dao.findMetricsIndexEntriesAsync(partitionKey, startTime.getMillis());
            Futures.addCallback(indexFuture, new RawIndexEntriesHandler(state, dao, writePermits, readPermits,
                batchSize, partitionKey), state.getAggregationTasks());
        }

        if (state.is6HourTimeSliceFinished()) {
            log.debug("Loading 1 hour index entries");
            long start = System.currentTimeMillis();
            for (int scheduleId = startScheduleId; scheduleId <= endScheduleId; scheduleId += METRICS_INDEX_ROW_SIZE) {
                String partitionKey = indexPartitionKey(MetricsTable.SIX_HOUR, scheduleId);
                readPermits.acquire();
                StorageResultSetFuture indexFuture = dao.findMetricsIndexEntriesAsync(partitionKey,
                    state.getSixHourTimeSlice().getMillis());
                Futures.addCallback(indexFuture, new AggregateIndexEntriesHandler(state.getOneHourIndexEntries(),
                    state.getRemaining1HourData(), partitionKey, dao, writePermits,
                    state.getOneHourIndexEntriesArrival(), start, "1 hour", "6 hour", state.getSixHourTimeSlice()),
                    state.getAggregationTasks());
            }
        }

        if (state.is24HourTimeSliceFinished()) {
            long start = System.currentTimeMillis();
            log.debug("Fetching 6 hour index entries");
            for (int scheduleId = startScheduleId; scheduleId <= endScheduleId; scheduleId += METRICS_INDEX_ROW_SIZE) {
                String partitionKey = indexPartitionKey(MetricsTable.TWENTY_FOUR_HOUR, scheduleId);
                readPermits.acquire();
                StorageResultSetFuture indexFuture = dao.findMetricsIndexEntriesAsync(partitionKey,
                    state.getTwentyFourHourTimeSlice().getMillis());
                Futures.addCallback(indexFuture, new AggregateIndexEntriesHandler(state.getSixHourIndexEntries(),
                    state.getRemaining6HourData(), partitionKey, dao, writePermits,
                    state.getSixHourIndexEntriesArrival(), start, "6 hour", "24 hour", state.getTwentyFourHourTimeSlice()),
                    state.getAggregationTasks());
            }
        }

        try {
            state.getRawIndexEntriesArrival().await();

            if (state.is6HourTimeSliceFinished()) {
                waitFor(state.getRemainingRawData());
                try {
                    state.getOneHourIndexEntriesArrival().await();
                    List<StorageResultSetFuture> queryFutures = new ArrayList<StorageResultSetFuture>(batchSize);
                    Set<Integer> scheduleIds = new TreeSet<Integer>();
                    state.getOneHourIndexEntriesLock().writeLock().lock();
                    log.debug("Remaining schedule ids for 1 hour data: " + state.getOneHourIndexEntries());
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
                    log.warn("Failed to load 1 hour index entries. Some 6 hour aggregates may not get generated.", e);
                } finally {
                    state.getOneHourIndexEntriesLock().writeLock().unlock();
                }
            }

            if (state.is24HourTimeSliceFinished()) {
                waitFor(state.getRemaining1HourData());
                try {
                    state.getSixHourIndexEntriesArrival().await();

                    List<StorageResultSetFuture> queryFutures = new ArrayList<StorageResultSetFuture>(batchSize);
                    Set<Integer> scheduleIds = new TreeSet<Integer>();
                    state.getSixHourIndexEntriesLock().writeLock().lock();
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
                        log.debug("Submitting 6 hour aggregation task for schedule ids " + scheduleIds);
                        state.getAggregationTasks().submit(new Aggregate6HourData(dao, state, scheduleIds,
                            queryFutures));
                        queryFutures = null;
                        scheduleIds = null;
                    }
                } catch (AbortedException e) {
                    log.warn("Failed to load 6 hour index entries. Some 24 hour aggregates may not get generated.", e);
                } finally {
                    state.getSixHourIndexEntriesLock().writeLock().unlock();
                }
            }
            while (!isAggregationFinished()) {
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            log.warn("An interrupt occurred while waiting for aggregation to finish", e);
        }
        log.debug("Finished aggregating metric data for " + oneHourData.size() + " schedules");
        return oneHourData;
    }

    private void waitFor(AtomicInteger remainingData) throws InterruptedException {
        while (remainingData.get() > 0) {
            Thread.sleep(50);
        }
    }

    private boolean isAggregationFinished() throws InterruptedException {
        return state.getRemainingRawData().get() <= 0 && state.getRemaining1HourData().get() <= 0 &&
            state.getRemaining6HourData().get() <= 0;
    }

}