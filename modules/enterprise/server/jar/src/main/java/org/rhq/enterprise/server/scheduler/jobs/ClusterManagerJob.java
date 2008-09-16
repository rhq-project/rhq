package org.rhq.enterprise.server.scheduler.jobs;

import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.core.domain.cluster.PartitionEventType;
import org.rhq.core.domain.cluster.Server;
import org.rhq.enterprise.server.cluster.ClusterManagerLocal;
import org.rhq.enterprise.server.cluster.PartitionEventManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ClusterManagerJob implements Job {

    // A time sufficient to determine whether a server is down.  Can be based on the initial delay set for the server instance
    // job updating the server mtimes. See StartupServlet. 
    static private final long SERVER_DOWN_INTERVAL = 1000L * 2 * 60;

    public void execute(JobExecutionContext arg0) throws JobExecutionException {

        ClusterManagerLocal clusterManager = LookupUtil.getClusterManager();
        PartitionEventManagerLocal partitionEventManager = LookupUtil.getPartitionEventManager();

        // Look for downed server instances and update their mode prior to any partition request processing
        List<Server> servers = clusterManager.getAllServers();

        long now = System.currentTimeMillis();

        for (Server server : servers) {
            // We're only looking for NORMNAL servers that may have gone down unexpectedly. A MM server can go up
            // and down while still in MM.  DOWN servers will come up as NORMAL.
            if (Server.OperationMode.NORMAL == server.getOperationMode()) {
                long timeSinceServerHeartbeat = (now - server.getMtime());

                if (timeSinceServerHeartbeat > SERVER_DOWN_INTERVAL) {
                    LookupUtil.getPartitionEventManager().auditPartitionEvent(
                        LookupUtil.getSubjectManager().getOverlord(), PartitionEventType.SERVER_DOWN, server.getName());
                    clusterManager.updateServerMode(new Integer[] { server.getId() }, Server.OperationMode.DOWN);
                }
            }
        }

        // Perform requested partition events. Note that we only need to execute one cloud partition
        // regardless of the number of pending requests, as the work would be duplicated.
        partitionEventManager.processRequestedPartitionEvents();
    }
}
