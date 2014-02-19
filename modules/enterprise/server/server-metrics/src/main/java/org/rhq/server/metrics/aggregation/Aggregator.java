package org.rhq.server.metrics.aggregation;

import static java.util.Arrays.asList;
import static org.rhq.server.metrics.domain.AggregateType.AVG;
import static org.rhq.server.metrics.domain.AggregateType.MAX;
import static org.rhq.server.metrics.domain.AggregateType.MIN;
import static org.rhq.server.metrics.domain.MetricsTable.SIX_HOUR;
import static org.rhq.server.metrics.domain.MetricsTable.TWENTY_FOUR_HOUR;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.Duration;

import org.rhq.server.metrics.AbortedException;
import org.rhq.server.metrics.ArithmeticMeanCalculator;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsConfiguration;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.SignalingCountDownLatch;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * This class is the driver for metrics aggregation.
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

    private AggregationState state;

    private Set<AggregateNumericMetric> oneHourData;

    private int minScheduleId;

    private int maxScheduleId;

    private int cacheBatchSize;

    public Aggregator(ListeningExecutorService aggregationTasks, MetricsDAO dao, MetricsConfiguration configuration,
        DateTimeService dtService, DateTime startTime, int batchSize, int parallelism, int minScheduleId,
        int maxScheduleId, int cacheBatchSize) {
        this.dao = dao;
        this.configuration = configuration;
        this.dtService = dtService;
        this.startTime = startTime;
        oneHourData = new ConcurrentSkipListSet<AggregateNumericMetric>(AGGREGATE_COMPARATOR);
        this.minScheduleId = minScheduleId;
        this.maxScheduleId = maxScheduleId;
        this.cacheBatchSize = cacheBatchSize;

        DateTime sixHourTimeSlice = get6HourTimeSlice();
        DateTime twentyFourHourTimeSlice = get24HourTimeSlice();

        state = new AggregationState()
            .setDao(dao)
            .setStartTime(startTime)
            .setBatchSize(batchSize)
            .setAggregationTasks(aggregationTasks)
            .setPermits(new Semaphore(parallelism * batchSize, true))
            .setRawAggregationDone(new SignalingCountDownLatch(new CountDownLatch(1)))
            .setOneHourAggregationDone(new SignalingCountDownLatch(new CountDownLatch(1)))
            .setSixHourAggregationDone(new SignalingCountDownLatch(new CountDownLatch(1)))
            .setOneHourTimeSlice(startTime)
            .setOneHourTimeSliceEnd(startTime.plus(configuration.getRawTimeSliceDuration()))
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
            .setRemaining6HourData(new AtomicInteger(0));
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

    private int calculateStartScheduleId(int scheduleId) {
        return (scheduleId / cacheBatchSize) * cacheBatchSize;
    }

    public Set<AggregateNumericMetric> run() {
        log.info("Starting aggregation for time slice " + startTime);
        Stopwatch stopwatch = new Stopwatch().start();
        try {
            final int startScheduleId = calculateStartScheduleId(minScheduleId);
            aggregateRawData(startScheduleId);
            if (state.is6HourTimeSliceFinished()) {
                aggregate1HourData(startScheduleId);
            }
            if (state.is24HourTimeSliceFinished()) {
                aggregate6HourData(startScheduleId);
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
            log.info("Finished aggregation in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }

    private void aggregateRawData(int startScheduleId) throws InterruptedException, AbortedException {
        Stopwatch stopwatch = new Stopwatch().start();
        AtomicInteger numSchedules = new AtomicInteger();
        try {
            ComputeMetric compute1HourMetric = new ComputeMetric() {
                @Override
                public List<StorageResultSetFuture> execute(int startScheduleId, int scheduleId, Double min, Double max,
                    ArithmeticMeanCalculator mean) {
                    oneHourData.add(new AggregateNumericMetric(scheduleId, mean.getArithmeticMean(), min, max,
                        state.getOneHourTimeSlice().getMillis()));
                    return asList(
                        dao.insertOneHourDataAsync(scheduleId, state.getOneHourTimeSlice().getMillis(), AVG,
                            mean.getArithmeticMean()),
                        dao.insertOneHourDataAsync(scheduleId, state.getOneHourTimeSlice().getMillis(), MAX,
                            max),
                        dao.insertOneHourDataAsync(scheduleId, state.getOneHourTimeSlice().getMillis(), MIN,
                            min),
                        dao.updateMetricsCache(SIX_HOUR, state.getSixHourTimeSlice().getMillis(), startScheduleId,
                            scheduleId, state.getOneHourTimeSlice().getMillis(), map(min, max,
                            mean.getArithmeticMean()))
                    );
                }
            };
            for (int i = startScheduleId; i <= maxScheduleId; i += cacheBatchSize) {
                Stopwatch batchStopwatch = new Stopwatch().start();
                state.getPermits().acquire();
                StorageResultSetFuture queryFuture = dao.findMetricsIndexEntriesAsync(MetricsTable.ONE_HOUR,
                    startTime.getMillis(), i);
                state.getRemainingRawData().incrementAndGet();
                ListenableFuture<BatchResult> batchResultFuture = Futures.transform(queryFuture,
                    new ProcessBatch(state, compute1HourMetric, i, state.getOneHourTimeSlice(), AggregationType.RAW,
                        cacheBatchSize), state.getAggregationTasks());
                Futures.addCallback(batchResultFuture, batchFinished(state.getRemainingRawData(),
                    state.getRawAggregationDone(), AggregationType.RAW, numSchedules, batchStopwatch),
                    state.getAggregationTasks());
            }
            state.getRawAggregationDone().await();
        } finally {
            stopwatch.stop();
            if (log.isDebugEnabled()) {
                log.debug("Finished raw data aggregation of " + numSchedules + " schedules in " +
                    stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            }
        }
    }

    private void aggregate1HourData(int startScheduleId) throws InterruptedException, AbortedException {
        Stopwatch stopwatch = new Stopwatch().start();
        AtomicInteger numSchedules = new AtomicInteger();
        try {
            ComputeMetric compute6HourMetric = new ComputeMetric() {
                @Override
                public List<StorageResultSetFuture> execute(int startScheduleId, int scheduleId, Double min,
                    Double max, ArithmeticMeanCalculator mean) {
                    return asList(
                        dao.insertSixHourDataAsync(scheduleId, state.getSixHourTimeSlice().getMillis(), AVG,
                            mean.getArithmeticMean()),
                        dao.insertSixHourDataAsync(scheduleId, state.getSixHourTimeSlice().getMillis(), MAX, max),
                        dao.insertSixHourDataAsync(scheduleId, state.getSixHourTimeSlice().getMillis(), MIN, min),
                        dao.updateMetricsCache(TWENTY_FOUR_HOUR, state.getTwentyFourHourTimeSlice().getMillis(),
                            startScheduleId, scheduleId, state.getSixHourTimeSlice().getMillis(), map(min, max,
                            mean.getArithmeticMean()))
                    );
                }
            };
            for (int i = startScheduleId; i <= maxScheduleId; i += cacheBatchSize) {
                Stopwatch batchStopwatch = new Stopwatch().start();
                state.getPermits().acquire();
                StorageResultSetFuture queryFuture = dao.findMetricsIndexEntriesAsync(SIX_HOUR,
                    state.getSixHourTimeSlice().getMillis(), i);
                state.getRemaining1HourData().incrementAndGet();
                ListenableFuture<BatchResult> batchResultFuture = Futures.transform(queryFuture,
                    new ProcessBatch(state, compute6HourMetric, i, state.getSixHourTimeSlice(),
                        AggregationType.ONE_HOUR, cacheBatchSize), state.getAggregationTasks());
                Futures.addCallback(batchResultFuture, batchFinished(state.getRemaining1HourData(),
                    state.getOneHourAggregationDone(), AggregationType.ONE_HOUR, numSchedules, batchStopwatch),
                    state.getAggregationTasks());
            }
            state.getOneHourAggregationDone().await();
        } finally {
            stopwatch.stop();
            if (log.isDebugEnabled()) {
                log.debug("Finished 1 hour data aggregation of " + numSchedules + " schedules in " +
                    stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            }
        }
    }

    private void aggregate6HourData(int startScheduleId) throws InterruptedException, AbortedException {
        Stopwatch stopwatch = new Stopwatch().start();
        AtomicInteger numSchedules = new AtomicInteger();
        try {
            ComputeMetric compute24HourMetric = new ComputeMetric() {
                @Override
                public List<StorageResultSetFuture> execute(int startScheduleId, int scheduleId, Double min,
                    Double max, ArithmeticMeanCalculator mean) {
                    return asList(
                        dao.insertTwentyFourHourDataAsync(scheduleId, state.getTwentyFourHourTimeSlice().getMillis(),
                            AVG, mean.getArithmeticMean()),
                        dao.insertTwentyFourHourDataAsync(scheduleId, state.getTwentyFourHourTimeSlice().getMillis(),
                            MAX, max),
                        dao.insertTwentyFourHourDataAsync(scheduleId, state.getTwentyFourHourTimeSlice().getMillis(),
                            MIN, min)
                    );
                }
            };
            for (int i = startScheduleId; i <= maxScheduleId; i += cacheBatchSize) {
                Stopwatch batchStopwatch = new Stopwatch().start();
                state.getPermits().acquire();
                StorageResultSetFuture queryFuture = dao.findMetricsIndexEntriesAsync(TWENTY_FOUR_HOUR,
                    state.getTwentyFourHourTimeSlice().getMillis(), i);
                state.getRemaining6HourData().incrementAndGet();
                ListenableFuture<BatchResult> batchResultFuture = Futures.transform(queryFuture,
                    new ProcessBatch(state, compute24HourMetric, i, state.getTwentyFourHourTimeSlice(),
                        AggregationType.SIX_HOUR, cacheBatchSize), state.getAggregationTasks());
                Futures.addCallback(batchResultFuture, batchFinished(state.getRemaining6HourData(),
                    state.getSixHourAggregationDone(), AggregationType.SIX_HOUR, numSchedules, batchStopwatch),
                    state.getAggregationTasks());
            }
            state.getSixHourAggregationDone().await();
        } finally {
            stopwatch.stop();
            if (log.isDebugEnabled()) {
                log.debug("Finished 6 hour data aggregation of " + numSchedules + " schedules in " +
                    stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            }
        }
    }

    private FutureCallback<BatchResult> batchFinished(final AtomicInteger remainingBatches,
        final SignalingCountDownLatch doneSignal, final AggregationType aggregationType,
        final AtomicInteger numSchedules, final Stopwatch stopwatch) {
        return new FutureCallback<BatchResult>() {
            @Override
            public void onSuccess(BatchResult result) {
                updateRemainingBatches(remainingBatches, doneSignal);
                int delta = result.getInsertResultSets().size() / 4;
                int count = numSchedules.getAndAdd(delta);
                count += delta;
                stopwatch.stop();
                if (log.isDebugEnabled()) {
                    log.debug("Finished batch of " + aggregationType + " for " + count + " schedules with starting " +
                        "schedule id " + result.getStartScheduleId() + " in " +
                        stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
                }

            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("There was an unexpected error while processing a batch of " + aggregationType);
                updateRemainingBatches(remainingBatches, doneSignal);
            }
        };
    }

    private void updateRemainingBatches(AtomicInteger remainingBatches, SignalingCountDownLatch doneSignal) {
        state.getPermits().release();
        int count = remainingBatches.decrementAndGet();
        if (count == 0) {
            doneSignal.countDown();
        }

    }

    private Map<Integer, Double> map(Double min, Double max, Double avg) {
        Map<Integer, Double> values = new TreeMap<Integer, Double>();
        values.put(MIN.ordinal(), min);
        values.put(MAX.ordinal(), max);
        values.put(AVG.ordinal(), avg);

        return values;
    }

}
