package org.rhq.server.metrics;

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
public class Aggregate6HourData implements Runnable {

    private final Log log = LogFactory.getLog(Aggregate6HourData.class);

    private MetricsDAO dao;

    private AggregationState state;

    private Set<Integer> scheduleIds;

    private List<StorageResultSetFuture> queryFutures;

    public Aggregate6HourData(MetricsDAO dao, AggregationState state, Set<Integer> scheduleIds,
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
                log.error("An error occurred while fetching 6 hour data", t);
                return Futures.immediateFailedFuture(t);
            }
        });
        ListenableFuture<List<ResultSet>> computeFutures = Futures.transform(queriesFuture,
            state.getCompute24HourData(), state.getAggregationTasks());
        Futures.addCallback(computeFutures, new FutureCallback<List<ResultSet>>() {
            @Override
            public void onSuccess(List<ResultSet> result) {
                log.debug("Finished aggregating 6 hour data for " + result.size() + " schedules in " +
                    (System.currentTimeMillis() - start) + " ms");
                state.getRemaining6HourData().addAndGet(-scheduleIds.size());
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Failed to aggregate 6 hour data", t);
                state.getRemaining6HourData().addAndGet(-scheduleIds.size());
            }
        });
    }
}
