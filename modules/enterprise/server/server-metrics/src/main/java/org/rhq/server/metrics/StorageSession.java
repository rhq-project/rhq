package org.rhq.server.metrics;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Query;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.google.common.util.concurrent.RateLimiter;

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

    private int minRequestLimit = Integer.parseInt(System.getProperty("rhq.storage.request-limit.min", "1000"));

    private RateLimiter permits = RateLimiter.create(Double.parseDouble(
        System.getProperty("rhq.storage.request-limit", "50000")), 3, TimeUnit.MINUTES);

    private int requestLimitDelta;

    private long permitsLastChanged = System.currentTimeMillis();

    private long permitsChangeWindow = 1000 * 10;

    private boolean permitsChanging;

    private ReentrantReadWriteLock permitsLock = new ReentrantReadWriteLock();

    public StorageSession(Session wrappedSession) {
        this.wrappedSession = wrappedSession;
        this.wrappedSession.getCluster().register(this);

        if (System.getProperty("rhq.storage.request-limit.delta") != null) {
            requestLimitDelta = Integer.parseInt(System.getProperty("rhq.storage.request-limit.delta"));
        } else {
            requestLimitDelta = (int) (permits.getRate() * 0.2);
        }
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

    RateLimiter getPermits() {
        return permits;
    }

    void setPermits(RateLimiter permits) {
        this.permits = permits;
    }

    int getRequestLimitDelta() {
        return requestLimitDelta;
    }

    void setRequestLimitDelta(int requestLimitDelta) {
        this.requestLimitDelta = requestLimitDelta;
    }

    public void addStorageStateListener(StorageStateListener listener) {
        listeners.add(listener);
    }

    public ResultSet execute(String query) {
        try {
//            permits.acquire();
            acquirePermit();
            return wrappedSession.execute(query);
        } catch (NoHostAvailableException e) {
            handleNoHostAvailable(e);
            throw e;
        }
    }

    public ResultSet execute(Query query) {
        try {
//            permits.acquire();
            acquirePermit();
            return wrappedSession.execute(query);
        } catch (NoHostAvailableException e) {
            handleNoHostAvailable(e);
            throw e;
        }
    }

    public StorageResultSetFuture executeAsync(String query) {
//        permits.acquire();
        acquirePermit();
        ResultSetFuture future = wrappedSession.executeAsync(query);
        return new StorageResultSetFuture(future, this);
    }

    public StorageResultSetFuture executeAsync(Query query) {
//        permits.acquire();
        acquirePermit();
        ResultSetFuture future = wrappedSession.executeAsync(query);
        return new StorageResultSetFuture(future, this);
    }

    public PreparedStatement prepare(String query) {
//        permits.acquire();
        acquirePermit();
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

    void handleNoHostAvailable(NoHostAvailableException e) {
        log.warn("Encountered " + NoHostAvailableException.class.getSimpleName() + " due to following error(s): " +
            e.getErrors());
        for (InetAddress address : e.getErrors().keySet()) {
            String error = e.getErrors().get(address);
            if (error != null && error.contains("Timeout during read")) {
                try {
                    permitsLock.writeLock().lock();
                    if (System.currentTimeMillis() - permitsLastChanged > permitsChangeWindow) {
                        permitsChanging = true;
                        int newRate = (int) permits.getRate() - requestLimitDelta;
                        if (newRate < minRequestLimit) {
                            newRate = minRequestLimit;
                        }
                        log.warn("The request timed out. Decreasing request throughput to " + newRate);
//                        permits.setRate(newRate);
                        permits = RateLimiter.create(newRate, 2, TimeUnit.MINUTES);
                        permitsLastChanged = System.currentTimeMillis();
                        requestLimitDelta = requestLimitDelta * 2;
                    }
                } finally {
                    permitsChanging = false;
                    permitsLock.writeLock().unlock();
                }

                break;
            }
        }
//        for (String error  : e.getErrors().values()) {
//            if (error.contains("Timeout during read")) {
//                log.warn("The request timed out. Decreasing request throughput.");
//                permits.setRate(permits.getRate() - requestLimitDelta);
//                break;
//            }
//        }
    }

    private void acquirePermit() {
        if (permitsChanging) {
            try {
                permitsLock.readLock().lock();
                permits.acquire();
            } finally {
                permitsLock.readLock().unlock();
            }
        } else {
            permits.acquire();
        }
    }

    void fireClusterDownEvent(NoHostAvailableException e) {
        isClusterAvailable = false;
        for (StorageStateListener listener : listeners) {
            listener.onStorageClusterDown(e);
        }
    }
}
