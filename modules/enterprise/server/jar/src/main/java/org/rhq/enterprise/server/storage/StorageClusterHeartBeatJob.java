package org.rhq.enterprise.server.storage;

import static org.rhq.core.domain.cloud.Server.OperationMode.MAINTENANCE;
import static org.rhq.core.domain.cloud.Server.OperationMode.NORMAL;

import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.ClusterInitService;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.cloud.TopologyManagerLocal;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;

/**
 * This job runs periodically to verify that the server can connect to the storage cluster.
 * If the server cannot connect to any nodes in the cluster, the server will be put into
 * maintenance mode until it is able to connect to the cluster. See
 * {@link #checkClusterHeartBeat()} for more details.
 *
 * @author John Sanda
 */
@Singleton
public class StorageClusterHeartBeatJob {

    private final Log log = LogFactory.getLog(StorageClusterHeartBeatJob.class);

    @EJB
    private ServerManagerLocal serverManager;

    @EJB
    private StorageNodeManagerLocal storageNodeManager;

    @EJB
    private TopologyManagerLocal topologyManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private StorageClientManagerBean storageClientManager;

    @Resource
    private TimerService timerService;

    public void scheduleJob() {
        long initialDelay = 3000;
        long interval = 1000 * 60;

        timerService.createIntervalTimer(initialDelay, interval, new TimerConfig(null, false));
    }

    /**
     * <p>
     * Runs periodically to verify that the server can connect to the storage cluster. If
     * the server cannot connect to any nodes in the cluster, the server will be put into
     * maintenance mode until a connection can be made to the cluster.
     * </p>
     * <p>
     * While client requests are made using CQL commands going over the native transport
     * layer introduced in Cassandra 1.2, connectivity is checked via JMX. The JMX call
     * is made to determine whether or not the native transport is running. Trying to do
     * the check using the CQL driver gets complicated and introduces some non-trivial
     * overhead due to the fact that the driver is async.
     * </p>
     */
    @Timeout
    public void checkClusterHeartBeat() {
        ClusterInitService clusterInitService = new ClusterInitService();
        Server server = serverManager.getServer();
        List<StorageNode> storageNodes = storageNodeManager.getStorageNodes();

        if (storageNodes.isEmpty()) {
            log.error("No storage nodes were found in the RHQ database. If this is your only RHQ server make sure " +
                "that the rhq.cassandra.seeds property in <rhq-server-basedir>/bin/rhq-server.properties is " +
                "properly configured. If you edit this property, you will have to restart the server for the change " +
                "to take effect. The server will now go into maintenance mode since connectivity to storage " +
                "nodes cannot be verified.");
            putServerInMaintenanceMode(server);
        } else {
            boolean pingable = clusterInitService.ping(storageNodes, 1);
            if (pingable) {
                if (server.getOperationMode() != NORMAL) {
                    changeServerMode(server, NORMAL);
                    log.info("Restarting storage client subsystem...");
                    storageClientManager.init();
                }
                return;
            }

            if (log.isWarnEnabled()) {
                log.warn(server + " is unable to connect to any Cassandra node. Server will go into maintenance mode.");
            }
            putServerInMaintenanceMode(server);
        }
    }

    private void putServerInMaintenanceMode(Server rhqServer) {
        changeServerMode(rhqServer, MAINTENANCE);
        log.info("Preparing to shut down storage client subsystem");
        storageClientManager.shutdown();
    }

    private void changeServerMode(Server rhqServer, Server.OperationMode mode) {
        if (rhqServer.getOperationMode() == mode) {
            return;
        }

        if (log.isInfoEnabled()) {
            log.info("Moving " + rhqServer + " from " + rhqServer.getOperationMode() + " to " + mode);
        }
        topologyManager.updateServerMode(subjectManager.getOverlord(), new Integer[] {rhqServer.getId()}, mode);
    }

}
