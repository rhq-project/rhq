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
import org.rhq.server.metrics.TaskTracker;
import org.rhq.server.metrics.SignalingCountDownLatch;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;

/**
 * This class is the driver for metrics aggregation.
 *
 * @author John Sanda
 */
public class AggregationManager {

    private static final Comparator<AggregateNumericMetric> AGGREGATE_COMPARATOR = new Comparator<AggregateNumericMetric>() {
        @Override
        public int compare(AggregateNumericMetric left, AggregateNumericMetric right) {
            return (left.getScheduleId() < right.getScheduleId()) ? -1 : ((left.getScheduleId() == right.getScheduleId()) ? 0 : 1);
        }
    };

    private final Log log = LogFactory.getLog(AggregationManager.class);

    private MetricsDAO dao;

    private MetricsConfiguration configuration;

    private DateTimeService dtService;

    private DateTime startTime;

    private AggregationState state;

    private Set<AggregateNumericMetric> oneHourData;

    private int minScheduleId;

    private int maxScheduleId;

    private int cacheBatchSize;

    public AggregationManager(ListeningExecutorService aggregationTasks, MetricsDAO dao,
        MetricsConfiguration configuration,
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
            createRawAggregator(startScheduleId).execute();
            if (state.is6HourTimeSliceFinished()) {
                create1HourAggregator(startScheduleId).execute();
            }
            if (state.is24HourTimeSliceFinished()) {
                create6HourAggregator(startScheduleId).execute();
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

    private Aggregator2 createRawAggregator(int startScheduleId) {
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

        Aggregator2 aggregator = new Aggregator2();
        aggregator.setAggregationTasks(state.getAggregationTasks());
        aggregator.setAggregationType(AggregationType.RAW);
        aggregator.setCacheBatchSize(cacheBatchSize);
        aggregator.setComputeMetric(compute1HourMetric);
        aggregator.setDao(dao);
        aggregator.setTaskTracker(new TaskTracker());
        aggregator.setMaxScheduleId(maxScheduleId);
        aggregator.setPermits(state.getPermits());
        aggregator.setStartScheduleId(startScheduleId);
        aggregator.setStartTime(startTime);

        return aggregator;
    }

    private Aggregator2 create1HourAggregator(int startScheduleId) {
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

        Aggregator2 aggregator = new Aggregator2();
        aggregator.setAggregationTasks(state.getAggregationTasks());
        aggregator.setAggregationType(AggregationType.ONE_HOUR);
        aggregator.setCacheBatchSize(cacheBatchSize);
        aggregator.setComputeMetric(compute6HourMetric);
        aggregator.setDao(dao);
        aggregator.setTaskTracker(new TaskTracker());
        aggregator.setMaxScheduleId(maxScheduleId);
        aggregator.setPermits(state.getPermits());
        aggregator.setStartScheduleId(startScheduleId);
        aggregator.setStartTime(get6HourTimeSlice());

        return aggregator;
    }

    private Aggregator2 create6HourAggregator(int startScheduleId) {
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

        Aggregator2 aggregator = new Aggregator2();
        aggregator.setAggregationTasks(state.getAggregationTasks());
        aggregator.setAggregationType(AggregationType.SIX_HOUR);
        aggregator.setCacheBatchSize(cacheBatchSize);
        aggregator.setComputeMetric(compute24HourMetric);
        aggregator.setDao(dao);
        aggregator.setTaskTracker(new TaskTracker());
        aggregator.setMaxScheduleId(maxScheduleId);
        aggregator.setPermits(state.getPermits());
        aggregator.setStartScheduleId(startScheduleId);
        aggregator.setStartTime(get24HourTimeSlice());

        return aggregator;
    }

    private Map<Integer, Double> map(Double min, Double max, Double avg) {
        Map<Integer, Double> values = new TreeMap<Integer, Double>();
        values.put(MIN.ordinal(), min);
        values.put(MAX.ordinal(), max);
        values.put(AVG.ordinal(), avg);

        return values;
    }

}
