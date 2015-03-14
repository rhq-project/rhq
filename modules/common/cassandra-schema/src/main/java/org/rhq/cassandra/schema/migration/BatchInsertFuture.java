package org.rhq.cassandra.schema.migration;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.querybuilder.Batch;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

/**
 * @author John Sanda
 */
public class BatchInsertFuture extends AbstractFuture<BatchResult> {

    public BatchInsertFuture(final Batch batch, final ResultSetFuture insertFuture) {

        Futures.addCallback(insertFuture, new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet resultSet) {
                set(new BatchResult(null, true));
            }

            @Override
            public void onFailure(Throwable t) {
                if (isCancelled()) {
                    return;
                }
                try {
                    set(new BatchResult(batch, false));
                } catch (Throwable t1) {
                    setException(t1);
                }
            }
        });
    }

}
