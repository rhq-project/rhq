package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.server.metrics.AbortedException;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.StorageResultSetFuture;

/**
 * Generates 6 hour data for a batch of 1 hour data futures. After data is inserted for the batch, aggregation of 6 hour
 * data will start immediately for the batch if the 24 hour time slice has finished.
 *
 * @see Compute6HourData
 * @author John Sanda
 */
class Aggregate1HourData implements Runnable {

    private final Log log = LogFactory.getLog(Aggregate1HourData.class);

    private MetricsDAO dao;

    private AggregationState state;

    private Set<Integer> scheduleIds;

    private List<StorageResultSetFuture> queryFutures;

    public Aggregate1HourData(MetricsDAO dao, AggregationState state, Set<Integer> scheduleIds,
        List<StorageResultSetFuture> queryFutures) {
        this.dao = dao;
        this.state = state;
        this.scheduleIds = scheduleIds;
        this.queryFutures = queryFutures;
    }

    @Override
    public void run() {
        final Stopwatch stopwatch = new Stopwatch().start();
        ListenableFuture<List<ResultSet>> queriesFuture = Futures.successfulAsList(queryFutures);
        Futures.withFallback(queriesFuture, new FutureFallback<List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> create(Throwable t) throws Exception {
                log.error("An error occurred while fetching one hour data", t);
                return Futures.immediateFailedFuture(t);
            }
        });
        ListenableFuture<List<ResultSet>> computeFutures = Futures.transform(queriesFuture,
            state.getCompute6HourData(), state.getAggregationTasks());
        Futures.addCallback(computeFutures, new FutureCallback<List<ResultSet>>() {
            @Override
            public void onSuccess(List<ResultSet> result) {
                stopwatch.stop();
                log.debug("Finished aggregating 1 hour data for " + result.size() + " schedules in " +
                    stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
                start6HourDataAggregationIfNecessary();
            }

            @Override
            public void onFailure(Throwable t) {
                if (log.isDebugEnabled()) {
                    // TODO should we log the schedule ids?
                    log.debug("Failed to aggregate 1 hour data for " + scheduleIds.size() + " schedules. An " +
                        "unexpected error occurred.", t);
                } else {
                    log.warn("Failed to aggregate 1 hour data for " + scheduleIds.size() + " schedules. An " +
                        "unexpected error occurred: " + ThrowableUtil.getRootMessage(t));
                }
                start6HourDataAggregationIfNecessary();
            }
        });
    }

    private void start6HourDataAggregationIfNecessary() {
        try {
            if (state.is24HourTimeSliceFinished()) {
                update6HourIndexEntries();
                List<StorageResultSetFuture> queryFutures = new ArrayList<StorageResultSetFuture>(scheduleIds.size());
                for (Integer scheduleId : scheduleIds) {
                    queryFutures.add(dao.findSixHourMetricsAsync(scheduleId, state.getTwentyFourHourTimeSlice().getMillis(),
                        state.getTwentyFourHourTimeSliceEnd().getMillis()));
                }
                state.getAggregationTasks().submit(new Aggregate6HourData(dao, state, scheduleIds, queryFutures));
            }
        } catch (InterruptedException e) {
            if (log.isDebugEnabled()) {
                log.debug("An interrupt occurred while waiting for 6 hour data index entries. Aborting data aggregation",
                    e);
            } else {
                log.info("An interrupt occurred while waiting for 6 hour data index entries. Aborting data " +
                    "aggregation: " + e.getMessage());
            }
        } finally {
            int remainingSchedules = state.getRemaining1HourData().addAndGet(-scheduleIds.size());
            if (log.isDebugEnabled()) {
                log.debug("There are " + remainingSchedules + " remaining schedules with 1 hr data to be aggregated");
            }
        }
    }

    private void update6HourIndexEntries() throws InterruptedException {
        try {
            state.getSixHourIndexEntriesArrival().await();
            try {
                state.getSixHourIndexEntriesLock().writeLock().lock();
                state.getSixHourIndexEntries().removeAll(scheduleIds);
            } finally {
                state.getSixHourIndexEntriesLock().writeLock().unlock();
            }
        } catch (AbortedException e) {
            // This means we failed to retrieve the index entries. We can however
            // continue generating 6 hour data because we do not need the index
            // here since we already have 6 hour data to aggregate along with the
            // schedule ids.
            log.debug("The wait for 6 hour index entries has been aborted. Proceeding with scheduling computation of " +
                "1 hour aggregates for previously assigned schedules.");
            state.getRemaining6HourData().addAndGet(scheduleIds.size());
        }
    }
}
