package org.rhq.server.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author John Sanda
 */
public class RawIndexEntriesHandler implements FutureCallback<ResultSet> {

    private final Log log = LogFactory.getLog(RawIndexEntriesHandler.class);

    private AggregationState state;

    private int batchSize;

    private MetricsDAO dao;

    private RateLimiter writePermits;

    private RateLimiter readPermits;

    private String partitionKey;

    public RawIndexEntriesHandler(AggregationState state, MetricsDAO dao, RateLimiter writePermits,
        RateLimiter readPermits, int batchSize, String partitionKey) {
        this.state = state;
        this.dao = dao;
        this.writePermits = writePermits;
        this.readPermits = readPermits;
        this.batchSize = batchSize;
        this.partitionKey = partitionKey;
    }

    @Override
    public void onSuccess(ResultSet result) {
        List<Row> rows = result.all();

        // We have to decrement remainingIndexEntries after we increment remainingRawData; otherwise, we enter
        // into a race condition where Aggregator could return before all data aggregation has finished.
        state.getRemainingRawData().addAndGet(rows.size());
        state.getRawIndexEntriesArrival().countDown();

        log.debug("Starting raw data aggregation for " + rows.size() + " schedules from index partition " + partitionKey);
        long start = System.currentTimeMillis();
        Set<Integer> scheduleIds = new TreeSet<Integer>();
        List<StorageResultSetFuture> rawDataFutures = new ArrayList<StorageResultSetFuture>(batchSize);
        for (final Row row : rows) {
            scheduleIds.add(row.getInt(1));
            readPermits.acquire();
            rawDataFutures.add(dao.findRawMetricsAsync(row.getInt(1), state.getOneHourTimeSlice().getMillis(),
                state.getOneHourTimeSliceEnd().getMillis()));
            if (rawDataFutures.size() == batchSize) {
                state.getAggregationTasks().submit(new AggregateRawData(dao, state, scheduleIds,
                    rawDataFutures));
                rawDataFutures = new ArrayList<StorageResultSetFuture>();
                scheduleIds = new TreeSet<Integer>();
            }
        }
        if (!rawDataFutures.isEmpty()) {
            state.getAggregationTasks().submit(new AggregateRawData(dao, state, scheduleIds,
                rawDataFutures));
        }
        log.debug("Finished processing one hour index entries in " + (System.currentTimeMillis() - start) +
            " ms");
        deleteIndexPartition();
    }

    @Override
    public void onFailure(Throwable t) {
        log.warn("Failed to retrieve raw data index entries from partition " + partitionKey +
            ". Raw data aggregation for time slice [" + state.getOneHourTimeSlice() + "] cannot proceed.", t);

        state.getRawIndexEntriesArrival().countDown();
        deleteIndexPartition();
    }

    private void deleteIndexPartition() {
        log.debug("Deleting raw index entries for partition " + partitionKey);
        writePermits.acquire();
        StorageResultSetFuture deleteFuture = dao.deleteMetricsIndexEntriesAsync(partitionKey,
            state.getOneHourTimeSlice().getMillis());
        Futures.addCallback(deleteFuture, new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet result) {
                log.debug("Successfully deleting raw data index partition " + partitionKey);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Failed to delete raw data index partition " + partitionKey, t);
            }
        });
    }
}
