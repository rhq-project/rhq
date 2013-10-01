package org.rhq.enterprise.server.storage;

import java.net.InetAddress;

import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.scheduler.jobs.StorageClusterInitJob;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.server.metrics.StorageStateListener;

/**
 * @author John Sanda
 */
public class StorageClusterMonitor implements StorageStateListener {

    private Log log = LogFactory.getLog(StorageClusterMonitor.class);

    private boolean isClusterAvailable = true;

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
        isClusterAvailable = false;

//        // Wait long enough to allow the Server instance jobs to start executing first.
//        final long initialDelay = 1000L * 60 * 2; // 2 mins
//        final long interval = 1000L * 60; // 30 secs
//        try {
//            LookupUtil.getSchedulerBean().scheduleSimpleRepeatingJob(StorageClusterInitJob.class, true, false, initialDelay,
//                    interval);
//        } catch (Exception ex) {
//            log.error("Cannot create storage cluster init job", ex);
//        }
    }
}
