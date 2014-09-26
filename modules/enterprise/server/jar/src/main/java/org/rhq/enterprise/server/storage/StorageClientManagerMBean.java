package org.rhq.enterprise.server.storage;

/**
 * @author John Sanda
 */
public interface StorageClientManagerMBean {

    double getRequestLimit();

    void setRequestLimit(double requestLimit);

    double getMinRequestLimit();

    void setMinRequestLimit(double minRequestLimit);

    double getRequestLimitTopologyDelta();

    void setRequestLimitTopologyDelta(double delta);

    double getRequestTimeoutDelta();

    void setRequestTimeoutDelta(double requestTimeoutDelta);

    long getRequestTimeoutDampening();

    void setRequestTimeoutDampening(long requestTimeoutDampening);

    // Cassandra driver's exposed methods
    int getConnectedToHosts();
    int getKnownHosts();
    int getOpenConnections();
    long getReadRequestTimeouts();
    long getWriteRequestTimeouts();
    long getTotalRequests();

    // Metrics.Errors, not exposing RetryPolicy statistics
    long getRetries();
    long getConnectionErrors();

    // Timers
    double oneMinuteAvgRate();
    double fiveMinuteAvgRate();
    double fifteenMinuteAvgRate();
    double meanRate();
    double meanLatency();
}
