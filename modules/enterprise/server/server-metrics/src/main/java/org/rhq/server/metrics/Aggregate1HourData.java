package org.rhq.server.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.datastax.driver.core.ResultSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author John Sanda
 */
public class Aggregate1HourData implements Runnable {

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
        final long start = System.currentTimeMillis();
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
                log.debug("Finished aggregating 1 hour data for " + result.size() + " schedules in " +
                    (System.currentTimeMillis() - start) + " ms");
                state.getRemaining1HourData().addAndGet(-scheduleIds.size());
                start6HourDataAggregationIfNecessary();
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Failed to aggregate 1 hour data", t);
                state.getRemaining1HourData().addAndGet(-scheduleIds.size());
                start6HourDataAggregationIfNecessary();
            }
        });
    }

    private void start6HourDataAggregationIfNecessary() {
        if (state.is24HourTimeSliceFinished()) {
            log.debug("Starting 6 hour data aggregation for " + scheduleIds.size() + " schedules");
            try {
                state.getSixHourIndexEntriesArrival().await();
                try {
                    state.getSixHourIndexEntriesLock().writeLock().lock();
                    state.getSixHourIndexEntries().removeAll(scheduleIds);
                } finally {
                    state.getSixHourIndexEntriesLock().writeLock().unlock();
                }
            } catch (InterruptedException e) {
                log.warn("An interrupt occurred waiting for 6 hour index entries", e);
            } catch (AbortedException e) {
                // This means we failed to retrieve the index entries. We can however
                // continue generating 6 hour data because we do not need the index
                // here since we already have 6 hour data to aggregate along with the
                // schedule ids.
            }
            List<StorageResultSetFuture> queryFutures = new ArrayList<StorageResultSetFuture>(scheduleIds.size());
            for (Integer scheduleId : scheduleIds) {
                queryFutures.add(dao.findSixHourMetricsAsync(scheduleId, state.getTwentyFourHourTimeSlice().getMillis(),
                    state.getTwentyFourHourTimeSliceEnd().getMillis()));
            }
            state.getAggregationTasks().submit(new Aggregate6HourData(dao, state, scheduleIds, queryFutures));
        }
    }
}
