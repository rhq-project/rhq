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

    long getRequestTimeouts();

    long getTotalRequests();

}
