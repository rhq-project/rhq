package org.rhq.server.metrics.aggregation;

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
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.StorageResultSetFuture;

/**
 * Generates 24 hour data for a batch of 1 hour data futures. After data is inserted for the batch, aggregation of 6
 * hour data will start immediately for the batch if the 24 hour time slice has finished.
 *
 * @see Compute24HourData
 * @author John Sanda
 */
class Aggregate6HourData implements Runnable {

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
        final Stopwatch stopwatch = new Stopwatch().start();
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
                stopwatch.stop();
                log.debug("Finished aggregating 6 hour data for " + result.size() + " schedules in " +
                    stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
                updateRemaining6HrData();
            }

            @Override
            public void onFailure(Throwable t) {
                if (log.isDebugEnabled()) {
                    // TODO should we log the schedule ids?
                    log.debug("Failed to aggregate 6 hour data for " + scheduleIds.size() + " schedules. An " +
                        "unexpected error occurred.", t);
                } else {
                    log.warn("Failed to aggregate 6 hour data for " + scheduleIds.size() + " schedules. An " +
                        "unexpected error occurred: " + ThrowableUtil.getRootMessage(t));
                }
                updateRemaining6HrData();
            }
        });
    }

    private void updateRemaining6HrData() {
        int remainingSchedules = state.getRemaining6HourData().addAndGet(-scheduleIds.size());
        if (log.isDebugEnabled()) {
            log.debug("There are " + remainingSchedules + " remaining schedules with 6 hr data to be aggregated");
        }
    }
}
