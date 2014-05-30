package org.rhq.server.metrics;

import static org.rhq.server.metrics.StorageClientConstants.REQUEST_LIMIT;
import static org.rhq.server.metrics.StorageClientConstants.REQUEST_LIMIT_MIN;
import static org.rhq.server.metrics.StorageClientConstants.REQUEST_TIMEOUT_DAMPENING;
import static org.rhq.server.metrics.StorageClientConstants.REQUEST_TIMEOUT_DELTA;
import static org.rhq.server.metrics.StorageClientConstants.REQUEST_TOPOLOGY_CHANGE_DELTA;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Query;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryTimeoutException;
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

    private double minRequestLimit = Double.parseDouble(System.getProperty(REQUEST_LIMIT_MIN, "5000"));

    private RateLimiter permits = RateLimiter.create(Double.parseDouble(
        System.getProperty(REQUEST_LIMIT, "30000")), 3, TimeUnit.MINUTES);

    private double timeoutDelta = Double.parseDouble(System.getProperty(REQUEST_TIMEOUT_DELTA, "0.2"));

    private long permitsLastChanged = System.currentTimeMillis();

    private long timeoutDampening = Long.parseLong(System.getProperty(REQUEST_TIMEOUT_DAMPENING, "30000"));

    private double topologyDelta = Double.parseDouble(System.getProperty(REQUEST_TOPOLOGY_CHANGE_DELTA, "30000"));

    private long timeouts;

    private AtomicLong totalRequests = new AtomicLong();

    public StorageSession(Session wrappedSession) {
        this.wrappedSession = wrappedSession;
        this.wrappedSession.getCluster().register(this);
        calculateRequestLimit();
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

    private void calculateRequestLimit() {
        double rate = 0.0;
        for (Host host : wrappedSession.getCluster().getMetadata().getAllHosts()) {
            if (host.isUp()) {
                rate += topologyDelta;
            }
        }
        permits.setRate(rate);
    }

    public double getRequestLimit() {
        return new BigDecimal(permits.getRate(), new MathContext(2, RoundingMode.HALF_UP)).doubleValue();
    }

    public void setRequestLimit(double requestLimit) {
        permits.setRate(requestLimit);
    }

    public double getTimeoutDelta() {
        return timeoutDelta;
    }

    public void setTimeoutDelta(double timeoutDelta) {
        this.timeoutDelta = timeoutDelta;
    }

    public double getMinRequestLimit() {
        return minRequestLimit;
    }

    public void setMinRequestLimit(double minRequestLimit) {
        this.minRequestLimit = minRequestLimit;
    }

    public double getTopologyDelta() {
        return topologyDelta;
    }

    public void setTopologyDelta(double delta) {
        topologyDelta = delta;
    }

    public long getTimeoutDampening() {
        return timeoutDampening;
    }

    public void setTimeoutDampening(long timeoutDampening) {
        this.timeoutDampening = timeoutDampening;
    }

    public void addStorageStateListener(StorageStateListener listener) {
        listeners.add(listener);
    }

    public long getTimeouts() {
        return timeouts;
    }

    public ResultSet execute(String query) {
        try {
            totalRequests.incrementAndGet();
            permits.acquire();
            return wrappedSession.execute(query);
        } catch (QueryTimeoutException e) {
            handleTimeout();
            throw e;
        } catch (NoHostAvailableException e) {
            handleNoHostAvailable(e);
            throw e;
        }
    }

    public ResultSet execute(Query query) {
        try {
            totalRequests.incrementAndGet();
            permits.acquire();
            return wrappedSession.execute(query);
        } catch(QueryTimeoutException e) {
            handleTimeout();
            throw e;
        } catch (NoHostAvailableException e) {
            handleNoHostAvailable(e);
            throw e;
        }
    }

    public StorageResultSetFuture executeAsync(String query) {
        totalRequests.incrementAndGet();
        permits.acquire();
        ResultSetFuture future = wrappedSession.executeAsync(query);
        return new StorageResultSetFuture(future, this);
    }

    public StorageResultSetFuture executeAsync(Query query) {
        totalRequests.incrementAndGet();
        permits.acquire();
        ResultSetFuture future = wrappedSession.executeAsync(query);
        return new StorageResultSetFuture(future, this);
    }

    public PreparedStatement prepare(String query) {
        totalRequests.incrementAndGet();
        permits.acquire();
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
        increaseRequestThroughput();
        if (!isClusterAvailable) {
            log.debug("Storage cluster is up");
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
        decreaseRequestThroughput(-topologyDelta);
        for (StorageStateListener listener : listeners) {
            listener.onStorageNodeDown(host.getAddress());
        }
    }

    @Override
    public void onRemove(Host host) {
        log.debug(host + " has been removed.");
        decreaseRequestThroughput(-topologyDelta);
        for (StorageStateListener listener : listeners) {
            listener.onStorageNodeRemoved(host.getAddress());
        }
    }

    void handleNoHostAvailable(NoHostAvailableException e) {
        log.warn("Encountered " + NoHostAvailableException.class.getSimpleName() + " due to following error(s): " +
            e.getErrors());
        if (isClientTimeout(e)) {
            handleTimeout();
        } else {
            fireClusterDownEvent(e);
        }
    }

    void handleTimeout() {
        ++timeouts;
        if (System.currentTimeMillis() - permitsLastChanged > timeoutDampening) {
            decreaseRequestThroughput((int) (getRequestLimit() * timeoutDelta));
        }
    }

    private void increaseRequestThroughput() {
        changeRequestThroughput(topologyDelta);
    }

    private void decreaseRequestThroughput(double amount) {
        if (amount > 0) {
            amount = amount * -1;
        }
        changeRequestThroughput(amount);
    }

    private void changeRequestThroughput(double delta) {
        double oldRate = getRequestLimit();
        double newRate = oldRate + delta;
        if (delta < 0 && newRate < minRequestLimit) {
            newRate = minRequestLimit;
        }
        permits.setRate(newRate);
        permitsLastChanged = System.currentTimeMillis();

        log.info("Changing request throughput from " + oldRate + " request/sec to " + newRate + " requests/sec");
    }

    private boolean isClientTimeout(NoHostAvailableException e) {
        for (InetAddress address : e.getErrors().keySet()) {
            String error = e.getErrors().get(address);
            if (error != null && (error.contains("Timeout during read") ||
                error.contains("Timeout while trying to acquire available connection"))) {
                return true;
            }
        }
        return false;
    }

    private void fireClusterDownEvent(NoHostAvailableException e) {
        isClusterAvailable = false;
        for (StorageStateListener listener : listeners) {
            listener.onStorageClusterDown(e);
        }
    }
}
