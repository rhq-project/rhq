package org.rhq.server.metrics;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

/**
* @author John Sanda
*/
class AggregateIndexEntriesHandler implements FutureCallback<ResultSet> {

    private final Log log = LogFactory.getLog(AggregateIndexEntriesHandler.class);

    private Set<Integer> indexEntries;

    private AtomicInteger remainingData;

    private SignalingCountDownLatch indexEntriesArrival;

    private long startTime;

    private String src;

    private String dest;

    private String partitionKey;

    private RateLimiter writePermits;

    private MetricsDAO dao;

    private DateTime timeSlice;

    public AggregateIndexEntriesHandler(Set<Integer> indexEntries, AtomicInteger remainingData, String partitionKey,
        MetricsDAO dao, RateLimiter writePermits, SignalingCountDownLatch indexEntriesArrival, long startTime,
        String src, String dest, DateTime timeSice) {
        this.indexEntries = indexEntries;
        this.remainingData = remainingData;
        this.indexEntriesArrival = indexEntriesArrival;
        this.startTime = startTime;
        this.src = src;
        this.dest = dest;
        this.writePermits = writePermits;
        this.dao = dao;
        this.partitionKey = partitionKey;
        this.timeSlice = timeSice;
    }

    @Override
    public void onSuccess(ResultSet resultSet) {
        int count = 0;
        for (Row row : resultSet) {
            indexEntries.add(row.getInt(1));
            count++;
        }
        remainingData.addAndGet(count);
        indexEntriesArrival.countDown();
        if (log.isDebugEnabled()) {
            log.debug("Finished loading " + indexEntries.size() + " " + src + " index entries in " +
                (System.currentTimeMillis() - startTime) + " ms");
        }
        deleteIndexPartition();
    }

    @Override
    public void onFailure(Throwable t) {
        log.warn("Failed to retrieve " + src + " index entries from partition " + partitionKey +
            ". Some " + dest + " aggregates may not get generated.", t);
        indexEntriesArrival.countDown();
        deleteIndexPartition();
    }

    private void deleteIndexPartition() {
        log.debug("Deleting " + src + " index entries for partition " + partitionKey);
        writePermits.acquire();
        StorageResultSetFuture deleteFuture = dao.deleteMetricsIndexEntriesAsync(partitionKey, timeSlice.getMillis());
        Futures.addCallback(deleteFuture, new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet result) {
                log.debug("Successfully deleted " + src + " data index partition " + partitionKey);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Failed to delete " + src + " data index partition " + partitionKey, t);
            }
        });
    }
}
