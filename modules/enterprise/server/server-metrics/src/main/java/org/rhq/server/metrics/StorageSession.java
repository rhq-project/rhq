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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author John Sanda
 */
public class StorageSession implements Host.StateListener {

    private final Log log = LogFactory.getLog(StorageSession.class);

    private Session wrappedSession;

    private List<StorageStateListener> listeners = new ArrayList<StorageStateListener>();

    private boolean isClusterAvailable = false;

    public StorageSession(Session wrappedSession) {
        this.wrappedSession = wrappedSession;
        this.wrappedSession.getCluster().register(this);
    }

    public void registerNewSession(Session newWrappedSession) {
        Session oldWrappedSession = this.wrappedSession;


        this.wrappedSession = newWrappedSession;
        this.wrappedSession.getCluster().register(this);

        oldWrappedSession.getCluster().unregister(this);

        // initial waiting before the first check
        try {
            Thread.sleep(60000L);
        } catch (InterruptedException e) {
            // nothing
        }
        oldWrappedSession.shutdown();
    }

    public void addStorageStateListener(StorageStateListener listener) {
        listeners.add(listener);
    }

    public ResultSet execute(String query) {
        try {
            return wrappedSession.execute(query);
        } catch (NoHostAvailableException e) {
            fireClusterDownEvent(e);
            throw e;
        }
    }

    public ResultSet execute(Query query) {
        try {
            return wrappedSession.execute(query);
        } catch (NoHostAvailableException e) {
            fireClusterDownEvent(e);
            throw e;
        }
    }

    public StorageResultSetFuture executeAsync(String query) {
        ResultSetFuture future = wrappedSession.executeAsync(query);
        return new StorageResultSetFuture(future, this);
    }

    public StorageResultSetFuture executeAsync(Query query) {
        ResultSetFuture future = wrappedSession.executeAsync(query);
        return new StorageResultSetFuture(future, this);
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
        addOrUp(host, " added");
    }

    @Override
    public void onUp(Host host) {
        addOrUp(host, " is up");
    }

    private void addOrUp(Host host, String msg) {
        log.info(host + msg);
        if (!isClusterAvailable) {
            log.info("Storage cluster is up");
        }
        for (StorageStateListener listener : listeners) {
            if (!isClusterAvailable) {
                listener.onStorageClusterUp();
            }
            listener.onStorageNodeUp(host.getAddress());
        }
        if (!isClusterAvailable) {
            isClusterAvailable = true;
        }
    }

    @Override
    public void onDown(Host host) {
        log.info(host + " is down");
        for (StorageStateListener listener : listeners) {
            listener.onStorageNodeDown(host.getAddress());
        }
    }

    @Override
    public void onRemove(Host host) {
        log.info(host + " has been removed");
        for (StorageStateListener listener : listeners) {
            listener.onStorageNodeRemoved(host.getAddress());
        }
    }

    void fireClusterDownEvent(NoHostAvailableException e) {
        isClusterAvailable = false;
        for (StorageStateListener listener : listeners) {
            listener.onStorageClusterDown(e);
        }
    }
}
