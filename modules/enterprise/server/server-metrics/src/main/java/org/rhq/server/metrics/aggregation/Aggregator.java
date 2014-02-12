package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
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

    public Aggregator(ListeningExecutorService aggregationTasks, MetricsDAO dao, MetricsConfiguration configuration,
        DateTimeService dtService, DateTime startTime, int batchSize, int parallelism) {
        this.dao = dao;
        this.configuration = configuration;
        this.dtService = dtService;
        this.startTime = startTime;
        oneHourData = new ConcurrentSkipListSet<AggregateNumericMetric>(AGGREGATE_COMPARATOR);

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

    public Set<AggregateNumericMetric> run() {
        log.info("Starting aggregation for time slice " + startTime);
        try {
            Stopwatch stopwatch = new Stopwatch().start();
            List<MetricsTable> indexUpdates = new ArrayList<MetricsTable>(3);
            indexUpdates.add(MetricsTable.ONE_HOUR);
            StorageResultSetFuture rawIndexFuture = dao.findMetricsIndexEntriesAsync(MetricsTable.ONE_HOUR,
                startTime.getMillis());
            Futures.addCallback(rawIndexFuture, new RawDataScheduler(state), state.getAggregationTasks());

            state.getRawAggregationDone().await();
            stopwatch.stop();
            log.info("Finished aggregating raw data in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");

            if (state.is6HourTimeSliceFinished()) {
                log.info("Starting aggregation of 1 hour data");
                stopwatch.reset().start();
                indexUpdates.add(MetricsTable.SIX_HOUR);
                StorageResultSetFuture oneHourIndexFuture = dao.findMetricsIndexEntriesAsync(MetricsTable.SIX_HOUR,
                    state.getSixHourTimeSlice().getMillis());
                Futures.addCallback(oneHourIndexFuture, new OneHourDataScheduler(state),
                    state.getAggregationTasks());

                state.getOneHourAggregationDone().await();
                stopwatch.stop();
                log.info("Finished aggregating one hour data in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            }

            if (state.is24HourTimeSliceFinished()) {
                log.info("Starting aggregation of 6 hour data");
                stopwatch.reset().start();
                indexUpdates.add(MetricsTable.TWENTY_FOUR_HOUR);
                StorageResultSetFuture sixHourIndexFuture = dao.findMetricsIndexEntriesAsync(
                    MetricsTable.TWENTY_FOUR_HOUR, state.getTwentyFourHourTimeSlice().getMillis());
                Futures.addCallback(sixHourIndexFuture, new SixHourDataScheduler(state),
                    state.getAggregationTasks());

                state.getSixHourAggregationDone().await();
                stopwatch.stop();
                log.info("Finished aggregating six hour data in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            }

            CountDownLatch updateIndexSignal = new CountDownLatch(indexUpdates.size());
            for (MetricsTable table : indexUpdates) {
                deleteIndexEntries(table, updateIndexSignal);
            }
            updateIndexSignal.await();

            return oneHourData;
        } catch (InterruptedException e) {
            log.info("There was an interrupt while waiting for aggregation to finish. Aggregation will be aborted.");
            return Collections.emptySet();
        } catch (AbortedException e) {
            log.warn("Aggregation has been aborted: " + e.getMessage());
            return Collections.emptySet();
        }
    }

    private void deleteIndexEntries(final MetricsTable table, final CountDownLatch doneSignal) {
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
                doneSignal.countDown();
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
                doneSignal.countDown();
            }
        });
    }

}
