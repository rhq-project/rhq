package org.rhq.server.metrics;

import java.util.List;
import java.util.concurrent.ExecutionException;
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

    private List<StorageStateListener> listeners;

    public StorageResultSetFuture(ResultSetFuture resultSetFuture, List<StorageStateListener> listeners) {
        wrapperFuture = resultSetFuture;
        this.listeners = listeners;
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

    @Override
    public ResultSet get() throws InterruptedException, ExecutionException {
        try {
            return wrapperFuture.get();
        } catch (ExecutionException e) {
            return handleException(e);
        }
    }

    @Override
    public ResultSet get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
        TimeoutException {
        try {
            return wrapperFuture.get(timeout, unit);
        } catch (ExecutionException e) {
            return handleException(e);
        }
    }

    private ResultSet handleException(ExecutionException e) throws ExecutionException {
        if (e.getCause() instanceof NoHostAvailableException) {
            NoHostAvailableException cause = (NoHostAvailableException) e.getCause();
            for (StorageStateListener listener : listeners) {
                listener.onStorageClusterDown(cause);
            }
        }
        throw e;
    }
}
