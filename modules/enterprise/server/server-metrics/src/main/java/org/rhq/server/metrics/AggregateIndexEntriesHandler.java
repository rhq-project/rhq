package org.rhq.server.metrics;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.util.concurrent.FutureCallback;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    public AggregateIndexEntriesHandler(Set<Integer> indexEntries, AtomicInteger remainingData,
        SignalingCountDownLatch indexEntriesArrival, long startTime, String src, String dest) {
        this.indexEntries = indexEntries;
        this.remainingData = remainingData;
        this.indexEntriesArrival = indexEntriesArrival;
        this.startTime = startTime;
        this.src = src;
        this.dest = dest;
    }

    @Override
    public void onSuccess(ResultSet resultSet) {
        for (Row row : resultSet) {
            indexEntries.add(row.getInt(1));
        }
        remainingData.set(indexEntries.size());
        indexEntriesArrival.countDown();
        if (log.isDebugEnabled()) {
            log.debug("Finished loading " + indexEntries.size() + " " + src + " index entries in " +
                (System.currentTimeMillis() - startTime) + " ms");
        }
    }

    @Override
    public void onFailure(Throwable t) {
        log.warn("Failed to retrieve " + src + " index entries. Some " + dest + " aggregates may not get generated.",
            t);
        remainingData.set(0);
        indexEntriesArrival.abort();
    }
}
