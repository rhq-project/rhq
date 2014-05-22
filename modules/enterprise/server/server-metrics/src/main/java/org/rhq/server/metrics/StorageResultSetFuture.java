package org.rhq.server.metrics;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * @author John Sanda
 */
public class StorageResultSetFuture implements ListenableFuture<ResultSet> {

    private ResultSetFuture wrapperFuture;

    private StorageSession session;

    public StorageResultSetFuture(ResultSetFuture resultSetFuture, StorageSession session) {
        wrapperFuture = resultSetFuture;
        this.session = session;
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        wrapperFuture.addListener(listener, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return wrapperFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return wrapperFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return wrapperFuture.isDone();
    }

    public void setException(Throwable t) {
        wrapperFuture.setException(t);
    }

    /**
     * Delegates to {@link com.datastax.driver.core.ResultSetFuture#getUninterruptibly()}
     */
    @Override
    public ResultSet get() {
        try {
            return wrapperFuture.getUninterruptibly();
        } catch (NoHostAvailableException e) {
            session.handleNoHostAvailable(e);
            throw e;
        }
    }

    /**
     * Delegates to {@link ResultSetFuture#getUninterruptibly(long, java.util.concurrent.TimeUnit)}
     */
    @Override
    public ResultSet get(long timeout, TimeUnit unit) throws TimeoutException {
        try {
            return wrapperFuture.getUninterruptibly(timeout, unit);
        } catch (NoHostAvailableException e) {
            session.handleNoHostAvailable(e);
            throw e;
        }
    }

}
