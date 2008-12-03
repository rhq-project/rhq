/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.scheduler.jobs;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.core.domain.cluster.Server;
import org.rhq.enterprise.server.cluster.CloudManagerLocal;
import org.rhq.enterprise.server.cluster.PartitionEventManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class CloudManagerJob extends AbstractStatefulJob {

    private final Log log = LogFactory.getLog(CloudManagerJob.class);

    // A time sufficient to determine whether a server is down.  Can be based on the initial delay set for the server instance
    // job updating the server mtimes. See StartupServlet. 
    static private final long SERVER_DOWN_INTERVAL = 1000L * 2 * 60;

    @Override
    public void executeJobCode(JobExecutionContext arg0) throws JobExecutionException {

        CloudManagerLocal cloudManager = LookupUtil.getCloudManager();
        PartitionEventManagerLocal partitionEventManager = LookupUtil.getPartitionEventManager();

        // Look for downed server instances and update their mode prior to any partition request processing
        List<Server> servers = cloudManager.getAllServers();

        long now = System.currentTimeMillis();

        log.debug("CloudManagerJob running at " + System.currentTimeMillis());
        for (Server server : servers) {
            // We're only looking for NORMNAL servers that may have gone down unexpectedly. A MM server can go up
            // and down while still in MM.  DOWN servers will come up as NORMAL.
            if (Server.OperationMode.NORMAL == server.getOperationMode()) {
                long timeSinceServerHeartbeat = (now - server.getMtime());

                if (timeSinceServerHeartbeat > SERVER_DOWN_INTERVAL) {
                    cloudManager.updateServerMode(new Integer[] { server.getId() }, Server.OperationMode.DOWN);
                }
            }
        }

        // Perform requested partition events. Note that we only need to execute one cloud partition
        // regardless of the number of pending requests, as the work would be duplicated.
        partitionEventManager.processRequestedPartitionEvents();
    }
}
