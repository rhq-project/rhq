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
public class AggregateRawData implements Runnable {

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
        final long start = System.currentTimeMillis();
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
                log.debug("Finished aggregating raw data for " + scheduleIds.size() + " schedules in " +
                    (System.currentTimeMillis() - start) + " ms");
                state.getRemainingRawData().addAndGet(-scheduleIds.size());
                start1HourDataAggregationIfNecessary();
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Failed to aggregate raw data", t);
                // TODO maybe add debug statement to log those schedule ids for which aggregation failed
                state.getRemainingRawData().addAndGet(-scheduleIds.size());
                start1HourDataAggregationIfNecessary();
            }
        }, state.getAggregationTasks());
    }

    private void start1HourDataAggregationIfNecessary() {
        if (state.is6HourTimeSliceFinished()) {
            log.debug("Starting 1 hour data aggregation for " + scheduleIds.size() + " schedules");
            try {
                state.getOneHourIndexEntriesArrival().await();
                try {
                    state.getOneHourIndexEntriesLock().writeLock().lock();
                    state.getOneHourIndexEntries().removeAll(scheduleIds);
                } finally {
                    state.getOneHourIndexEntriesLock().writeLock().unlock();
                }
            } catch (InterruptedException e) {
                log.warn("An interrupt occurred waiting for one hour data index entries", e);
                return;
            } catch (AbortedException e) {
                // This means we failed to retrieve the index entries. We can however
                // continue generating 1 hour data because we do not need the index
                // here since we already have 1 hour data to aggregate along with the
                // schedule ids.
            }
            List<StorageResultSetFuture> oneHourDataQueryFutures = new ArrayList<StorageResultSetFuture>(
                scheduleIds.size());
            for (Integer scheduleId : scheduleIds) {
                oneHourDataQueryFutures.add(dao.findOneHourMetricsAsync(scheduleId,
                    state.getSixHourTimeSlice().getMillis(), state.getSixHourTimeSliceEnd().getMillis()));
            }
            state.getAggregationTasks().submit(new Aggregate1HourData(dao, state, scheduleIds, oneHourDataQueryFutures));
        }
    }
}
