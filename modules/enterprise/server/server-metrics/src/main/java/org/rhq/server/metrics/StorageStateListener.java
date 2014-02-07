package org.rhq.server.metrics;

import java.net.InetAddress;

import com.datastax.driver.core.exceptions.NoHostAvailableException;

/**
 * @author John Sanda
 */
public interface StorageStateListener {

    void onStorageNodeUp(InetAddress address);

    void onStorageNodeDown(InetAddress address);

    void onStorageNodeRemoved(InetAddress address);

    void onStorageClusterDown(NoHostAvailableException e);

    void onStorageClusterUp();

    void onClientTimeout(NoHostAvailableException e);

}
