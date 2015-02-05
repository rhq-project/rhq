package org.rhq.cassandra.schema;

import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.Query;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import org.rhq.cassandra.schema.migration.QueryExecutor;

/**
 * @author John Sanda
 */
public class RetryWrite implements FutureFallback<ResultSet> {

    private QueryExecutor queryExecutor;

    private Query query;

    private RateMonitor rateMonitor;

    private AtomicInteger writeErrors;

    private ListeningExecutorService threadPool;

    public RetryWrite(QueryExecutor queryExecutor, Query query, RateMonitor rateMonitor, AtomicInteger writeErrors,
        ListeningExecutorService threadPool) {
        this.queryExecutor = queryExecutor;
        this.query = query;
        this.rateMonitor = rateMonitor;
        this.writeErrors = writeErrors;
        this.threadPool = threadPool;
    }

    @Override
    public ListenableFuture<ResultSet> create(Throwable t) throws Exception {
        rateMonitor.requestFailed();
        writeErrors.incrementAndGet();
        ResultSetFuture future = queryExecutor.executeWrite(query);
        return Futures.withFallback(future, this, threadPool);
    }
}
