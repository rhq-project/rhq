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

        // TODO remove, just return for now so we don't bust stuff
        if (1 == 1)
            return;

        ClusterManagerLocal clusterManager = LookupUtil.getClusterManager();
        PartitionEventManagerLocal partitionEventManager = LookupUtil.getPartitionEventManager();

        long now = System.currentTimeMillis();

        // Look for downed server instances and update their mode prior to any partition request processing
        List<Server> servers = clusterManager.getAllServers();

        for (Server server : servers) {
            if ((now - server.getCtime()) > SERVER_DOWN_INTERVAL) {
                // Don't set a MM server to DOWN even if it's not responding. That may be part of the
                // maintenance. Also, we will want to bring that server back up in MM.  DOWN servers will come
                // up as NORMAL.
                if (Server.OperationMode.MAINTENANCE != server.getOperationMode()) {
                    LookupUtil.getPartitionEventManager().auditPartitionEvent(
                        LookupUtil.getSubjectManager().getOverlord(), PartitionEventType.SERVER_DOWN);
                    clusterManager.updateServerMode(new Integer[] { server.getId() }, Server.OperationMode.DOWN);
                }
            }
        }

        // Perform requested partition events. Note that we only need to execute one cloud partition
        // regardless of the number of pending requests, as the work would be duplicated.
        partitionEventManager.processRequestedPartitionEvents();

    }
}
