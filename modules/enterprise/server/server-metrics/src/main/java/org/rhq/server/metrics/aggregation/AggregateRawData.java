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
 * Generates 1 hour data for a batch of raw data futures. After data is inserted for the batch, aggregation of 1 hour
 * data will start immediately for the batch if the 6 hour time slice has finished.
 *
 * @see Compute1HourData
 * @author John Sanda
 */
class AggregateRawData implements Runnable {

    private final Log log = LogFactory.getLog(AggregateRawData.class);

    private MetricsDAO dao;

    private AggregationState state;

    private Set<Integer> scheduleIds;

    private List<StorageResultSetFuture> queryFutures;

    public AggregateRawData(MetricsDAO dao, AggregationState state, Set<Integer> scheduleIds,
        List<StorageResultSetFuture> queryFutures) {
        this.dao = dao;
        this.state = state;
        this.scheduleIds = scheduleIds;
        this.queryFutures = queryFutures;
    }

    @Override
    public void run() {
        final Stopwatch stopwatch = new Stopwatch().start();
        ListenableFuture<List<ResultSet>> rawDataFutures = Futures.successfulAsList(queryFutures);
        Futures.withFallback(rawDataFutures, new FutureFallback<List<ResultSet>>() {
            @Override
            public ListenableFuture<List<ResultSet>> create(Throwable t) throws Exception {
                log.error("An error occurred while fetching raw data", t);
                return Futures.immediateFailedFuture(t);
            }
        });

        final ListenableFuture<List<ResultSet>> insert1HourDataFutures = Futures.transform(rawDataFutures,
            state.getCompute1HourData(), state.getAggregationTasks());
        Futures.addCallback(insert1HourDataFutures, new FutureCallback<List<ResultSet>>() {
            @Override
            public void onSuccess(List<ResultSet> resultSets) {
                stopwatch.stop();
                log.debug("Finished aggregating raw data for " + resultSets.size() + " schedules in " +
                    stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
                start1HourDataAggregationIfNecessary();
            }

            @Override
            public void onFailure(Throwable t) {
                if (log.isDebugEnabled()) {
                    // TODO should we log the schedule ids?
                    log.debug("Failed to aggregate raw data for " + scheduleIds.size() + " schedules. An unexpected " +
                        "error occurred.", t);
                } else {
                    log.warn("Failed to aggregate raw data for " + scheduleIds.size() + " schedules. An " +
                        "unexpected error occurred: " + ThrowableUtil.getRootMessage(t));
                }
                start1HourDataAggregationIfNecessary();
            }
        }, state.getAggregationTasks());
    }

    private void start1HourDataAggregationIfNecessary() {
        try {
            if (state.is6HourTimeSliceFinished()) {
                update1HourIndexEntries();
                List<StorageResultSetFuture> oneHourDataQueryFutures = new ArrayList<StorageResultSetFuture>(
                    scheduleIds.size());
                for (Integer scheduleId : scheduleIds) {
                    oneHourDataQueryFutures.add(dao.findOneHourMetricsAsync(scheduleId,
                        state.getSixHourTimeSlice().getMillis(), state.getSixHourTimeSliceEnd().getMillis()));
                }
                state.getAggregationTasks().submit(new Aggregate1HourData(dao, state, scheduleIds,
                    oneHourDataQueryFutures));
            }
        } catch (InterruptedException e) {
            if (log.isDebugEnabled()) {
                log.debug("An interrupt occurred while waiting for 1 hour data index entries. Aborting data aggregation",
                    e);
            } else {
                log.info("An interrupt occurred while waiting for 1 hour data index entries. Aborting data " +
                    "aggregation: " + e.getMessage());
            }
        } finally {
            state.getRemainingRawData().addAndGet(-scheduleIds.size());
        }
    }

    private void update1HourIndexEntries() throws InterruptedException {
        try {
            // Wait for the arrival so that we can remove the schedules ids in this
            // batch from the one hour index entries. This will prevent duplicate tasks
            // being submitted to process the same 1 hour data.
            state.getOneHourIndexEntriesArrival().await();
            try {
                state.getOneHourIndexEntriesLock().writeLock().lock();
                state.getOneHourIndexEntries().removeAll(scheduleIds);
            } finally {
                state.getOneHourIndexEntriesLock().writeLock().unlock();
            }
        } catch (AbortedException e) {
            // This means we failed to retrieve the index entries. We can however
            // continue generating 1 hour data because we do not need the index
            // here since we already have 1 hour data to aggregate along with the
            // schedule ids.
        }
    }
}
