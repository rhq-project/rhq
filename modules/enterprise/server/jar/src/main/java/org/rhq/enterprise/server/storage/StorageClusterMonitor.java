package org.rhq.enterprise.server.storage;

import java.net.InetAddress;

import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.server.metrics.StorageSession;
import org.rhq.server.metrics.StorageStateListener;

/**
 * @author John Sanda
 */
public class StorageClusterMonitor implements StorageStateListener {

    private Log log = LogFactory.getLog(StorageClusterMonitor.class);

    private boolean isClusterAvailable = true;

    private StorageSession session;

    public StorageClusterMonitor(StorageSession session) {
        this.session = session;
    }

    public boolean isClusterAvailable() {
        return isClusterAvailable;
    }

    @Override
    public void onStorageNodeUp(InetAddress address) {
        log.info("Storage node at " + address.getHostAddress() + " is up");
        isClusterAvailable = true;
    }

    @Override
    public void onStorageNodeDown(InetAddress address) {
        log.info("Storage node at " + address.getHostAddress() + " is down");
    }

    @Override
    public void onStorageNodeRemoved(InetAddress address) {
        log.info("Storage node at " + address.getHostAddress() + " has been removed from the cluster");
    }

    @Override
    public void onStorageClusterUp() {
        log.info("Storage cluster is up");
        isClusterAvailable = true;
    }

    @Override
    public void onStorageClusterDown(NoHostAvailableException e) {
        log.info("Storage cluster is down");
        isClusterAvailable = false;
    }

    @Override
    public void onClientTimeout(NoHostAvailableException e) {
    }


}
