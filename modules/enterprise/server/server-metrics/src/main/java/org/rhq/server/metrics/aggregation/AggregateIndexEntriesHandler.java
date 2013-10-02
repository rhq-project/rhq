package org.rhq.server.metrics.aggregation;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.server.metrics.SignalingCountDownLatch;

/**
* @author John Sanda
*/
class AggregateIndexEntriesHandler implements FutureCallback<ResultSet> {

    private final Log log = LogFactory.getLog(AggregateIndexEntriesHandler.class);

    private Set<Integer> indexEntries;

    private AtomicInteger remainingData;

    private SignalingCountDownLatch indexEntriesArrival;

    private Stopwatch stopwatch;

    private String src;

    private String dest;

    public AggregateIndexEntriesHandler(Set<Integer> indexEntries, AtomicInteger remainingData,
        SignalingCountDownLatch indexEntriesArrival, Stopwatch stopwatch, String src, String dest) {
        this.indexEntries = indexEntries;
        this.remainingData = remainingData;
        this.indexEntriesArrival = indexEntriesArrival;
        this.stopwatch = stopwatch;
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
        stopwatch.stop();
        if (log.isDebugEnabled()) {
            log.debug("Finished loading " + indexEntries.size() + " " + src + " index entries in " +
                stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }

    @Override
    public void onFailure(Throwable t) {
        if (log.isDebugEnabled()) {
            log.debug("Some " + dest + " aggregates may not get computed. An unexpected error occurred while " +
                "retrieving " + src + " index entries", t);
        } else {
            log.warn("Some " + dest + " aggregates may not get computed. An unexpected error occurred while " +
                "retrieving " + src + " index entries: " + ThrowableUtil.getRootMessage(t));
        }
        remainingData.set(0);
        indexEntriesArrival.abort();
    }
}
