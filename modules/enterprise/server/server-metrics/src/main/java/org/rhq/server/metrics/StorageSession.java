package org.rhq.server.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Query;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

/**
 * @author John Sanda
 */
public class StorageSession implements Host.StateListener {

    private Session wrappedSession;

    private List<StorageStateListener> listeners = new ArrayList<StorageStateListener>();

    public StorageSession(Session wrappedSession) {
        this.wrappedSession = wrappedSession;
        this.wrappedSession.getCluster().register(this);
    }

    public void addStorageStateListener(StorageStateListener listener) {
        listeners.add(listener);
    }

    public ResultSet execute(String query) {
        try {
            return wrappedSession.execute(query);
        } catch (NoHostAvailableException e) {
            return handleException(e);
        }
    }

    public ResultSet execute(Query query) {
        try {
            return wrappedSession.execute(query);
        } catch (NoHostAvailableException e) {
            return handleException(e);
        }
    }

    public StorageResultSetFuture executeAsync(String query) {
        ResultSetFuture future = wrappedSession.executeAsync(query);
        return new StorageResultSetFuture(future, listeners);
    }

    public StorageResultSetFuture executeAsync(Query query) {
        ResultSetFuture future = wrappedSession.executeAsync(query);
        return new StorageResultSetFuture(future, listeners);
    }

    public PreparedStatement prepare(String query) {
        return wrappedSession.prepare(query);
    }

    public void shutdown() {
        wrappedSession.shutdown();
    }

    public boolean shutdown(long timeout, TimeUnit unit) {
        return wrappedSession.shutdown(timeout, unit);
    }

    public Cluster getCluster() {
        return wrappedSession.getCluster();
    }

    @Override
    public void onAdd(Host host) {
        for (StorageStateListener listener : listeners) {
            listener.onStorageNodeUp(host.getAddress());
        }
    }

    @Override
    public void onUp(Host host) {
        for (StorageStateListener listener : listeners) {
            listener.onStorageNodeUp(host.getAddress());
        }
    }

    @Override
    public void onDown(Host host) {
        for (StorageStateListener listener : listeners) {
            listener.onStorageNodeDown(host.getAddress());
        }
    }

    @Override
    public void onRemove(Host host) {
        for (StorageStateListener listener : listeners) {
            listener.onStorageNodeRemoved(host.getAddress());
        }
    }

    private ResultSet handleException(NoHostAvailableException e) {
        for (StorageStateListener listener : listeners) {
            listener.onStorageClusterDown(e);
        }
        throw e;
    }
}
