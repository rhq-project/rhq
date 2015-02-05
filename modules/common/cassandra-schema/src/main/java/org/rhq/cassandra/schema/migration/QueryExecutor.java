package org.rhq.cassandra.schema.migration;

import java.util.concurrent.atomic.AtomicReference;

import com.datastax.driver.core.Query;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.google.common.util.concurrent.RateLimiter;

/**
 * @author John Sanda
 */
public class QueryExecutor {

    private AtomicReference<RateLimiter> readPermitsRef;

    private AtomicReference<RateLimiter> writePermitsRef;

    private Session session;

    public QueryExecutor(Session session, AtomicReference<RateLimiter> readPermitsRef,
        AtomicReference<RateLimiter> writePermitsRef) {
        this.session = session;
        this.readPermitsRef = readPermitsRef;
        this.writePermitsRef = writePermitsRef;
    }

    public ResultSetFuture executeRead(Query query) {
        readPermitsRef.get().acquire();
        return session.executeAsync(query);
    }

    public ResultSetFuture executeWrite(Query query) {
        writePermitsRef.get().acquire();
        return session.executeAsync(query);
    }
}
