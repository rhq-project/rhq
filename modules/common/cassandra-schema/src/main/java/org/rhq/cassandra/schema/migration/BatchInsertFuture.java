package org.rhq.cassandra.schema.migration;

import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.querybuilder.Batch;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.rhq.cassandra.schema.RateMonitor;

/**
 * @author John Sanda
 */
public class BatchInsertFuture extends AbstractFuture<BatchResult> {

    public BatchInsertFuture(final Batch batch, final ResultSetFuture insertFuture, final QueryExecutor queryExecutor,
        final RateMonitor rateMonitor, final AtomicInteger writeErrors) {

        Futures.addCallback(insertFuture, new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet resultSet) {
                rateMonitor.requestSucceeded();
                set(new BatchResult(null, true));
            }

            @Override
            public void onFailure(Throwable t) {
                if (isCancelled()) {
                    return;
                }

                try {
                    rateMonitor.requestFailed();
                    writeErrors.incrementAndGet();
                    ResultSetFuture retry = queryExecutor.executeWrite(batch);
                    Futures.addCallback(retry, this);
                } catch (Throwable t1) {
                    setException(t1);
                }
            }
        });
    }

}
