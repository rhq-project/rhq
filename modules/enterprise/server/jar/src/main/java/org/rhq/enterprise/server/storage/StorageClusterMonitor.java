package org.rhq.enterprise.server.storage;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.cloud.TopologyManagerLocal;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.server.metrics.StorageStateListener;

/**
 * @author John Sanda
 */
public class StorageClusterMonitor implements StorageStateListener {

    private Log log = LogFactory.getLog(StorageClusterMonitor.class);

    private AtomicBoolean isClusterDown = new AtomicBoolean(false);

    public boolean isClusterDown() {
        return isClusterDown.get();
    }

    @Override
    public void onStorageNodeUp(InetAddress address) {
        log.info("Storage node at " + address.getHostAddress() + " is up");

        if (isClusterDown.compareAndSet(true, false)) {
            log.info("Taking server out of maintenance mode");
            updateServerMode(Server.OperationMode.NORMAL);
        }

        StorageNodeManagerLocal storageNodeManager = LookupUtil.getStorageNodeManager();
        StorageNode newClusterNode = storageNodeManager.findStorageNodeByAddress(address);

        if (newClusterNode == null) {
            log.error("Did not find storage node with address [" + address.getHostAddress() + "]. This should not " +
                "happen.");
        } else {
            log.info("Adding " + newClusterNode + " to storage cluster and scheduling cluster maintenance...");
            storageNodeManager.addToStorageNodeGroup(newClusterNode);
            storageNodeManager.runAddNodeMaintenance();
        }
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
    public void onStorageClusterDown(NoHostAvailableException e) {
        if (isClusterDown.compareAndSet(false, true)) {
            log.error("The server cannot connect to any storage nodes. The server will now go into maintenance mode.");
            updateServerMode(Server.OperationMode.MAINTENANCE);
        }
    }

    private void updateServerMode(Server.OperationMode mode) {
        ServerManagerLocal serverManager = LookupUtil.getServerManager();
        TopologyManagerLocal topologyManager = LookupUtil.getTopologyManager();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

        Server server = serverManager.getServer();

        topologyManager.updateServerMode(subjectManager.getOverlord(), new Integer[] {server.getId()}, mode);
    }
}
