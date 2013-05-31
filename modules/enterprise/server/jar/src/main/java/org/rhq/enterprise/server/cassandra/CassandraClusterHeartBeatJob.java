/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.enterprise.server.cassandra;

import static org.rhq.core.domain.cloud.Server.OperationMode.MAINTENANCE;
import static org.rhq.core.domain.cloud.Server.OperationMode.NORMAL;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.cassandra.ClusterInitService;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.TopologyManagerLocal;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author John Sanda
 */
public class CassandraClusterHeartBeatJob implements Job {

    public static final String JOB_NAME = CassandraClusterHeartBeatJob.class.getSimpleName();
    public static final String KEY_CONNECTION_TIMEOUT = "rhq.cassandra.connection.timeout";
    public static final String KEY_CASSANDRA_HOSTS = "rhq.cassandra.hosts";

    private final Log log = LogFactory.getLog(CassandraClusterHeartBeatJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        ClusterInitService clusterInitService = new ClusterInitService();
        Server rhqServer = getRhqServer();
        JobDataMap dataMap = context.getMergedJobDataMap();
        String hosts = (String) dataMap.get(KEY_CASSANDRA_HOSTS);
        int timeout =  Integer.parseInt((String) dataMap.get(KEY_CONNECTION_TIMEOUT));
        List<StorageNode> cassandraHosts = new ArrayList<StorageNode>();

        for (String s : hosts.split(",")) {
            StorageNode storageNode = new StorageNode();
            storageNode.parseNodeInformation(s);
            cassandraHosts.add(storageNode);

        }
        boolean pingable = clusterInitService.ping(cassandraHosts, 1);
        if (pingable) {
            if (rhqServer.getOperationMode() != NORMAL) {
                changeServerMode(rhqServer, NORMAL);
            }
            return;
        }

        if (log.isWarnEnabled()) {
            log.warn(rhqServer + " is unable to connect to any Cassandra node. Server will go into maintenance mode.");
        }
        changeServerMode(rhqServer, MAINTENANCE);
    }

    private Server getRhqServer() {
        ServerManagerLocal serverManager = LookupUtil.getServerManager();
        return serverManager.getServer();
    }

    private void changeServerMode(Server rhqServer, OperationMode mode) {
        if (rhqServer.getOperationMode() == mode) {
            return;
        }

        if (log.isInfoEnabled()) {
            log.info("Moving " + rhqServer + " from " + rhqServer.getOperationMode() + " to " + mode);
        }
        TopologyManagerLocal rhqClusterManager = LookupUtil.getTopologyManager();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        rhqClusterManager.updateServerMode(subjectManager.getOverlord(), new Integer[] {rhqServer.getId()}, mode);
    }

    private void logException(String msg, Exception e) {
        if (log.isDebugEnabled()) {
            log.debug(msg, e);
        } else if (log.isInfoEnabled()) {
            log.info(msg + ": " + e.getMessage());
        } else {
            log.warn(msg);
        }
    }
}
