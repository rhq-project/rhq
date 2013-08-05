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

package org.rhq.enterprise.server.storage;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.util.StringUtil;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsConfiguration;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.MetricsServer;
import org.rhq.server.metrics.StorageSession;

/**
 * @author John Sanda
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class StorageClientManagerBean {

    private final Log log = LogFactory.getLog(StorageClientManagerBean.class);

    private static final String USERNAME_PROP = "rhq.cassandra.username";
    private static final String PASSWORD_PROP = "rhq.cassandra.password";
    private static final String RHQ_KEYSPACE = "rhq";

    @EJB
    private ServerManagerLocal serverManager;

    @EJB
    private StorageNodeManagerLocal storageNodeManager;

    private StorageSession session;
    private MetricsConfiguration metricsConfiguration;
    private MetricsDAO metricsDAO;
    private MetricsServer metricsServer;
    private boolean initialized;

    public synchronized void init() {
        if (initialized) {
            if (log.isDebugEnabled()) {
                log.debug("Storage client subsystem is already initialized. Skipping initialization.");
            }
            return;
        }

        log.info("Initializing storage client subsystem");

        boolean isNewServerInstall = !storageNodeManager.storageNodeGroupExists();
        if (isNewServerInstall) {
            storageNodeManager.createStorageNodeGroup();
        }

        String username = getRequiredStorageProperty(USERNAME_PROP);
        String password = getRequiredStorageProperty(PASSWORD_PROP);

        metricsConfiguration = new MetricsConfiguration();
        List<StorageNode> storageNodes = storageNodeManager.getStorageNodes();
        if (storageNodes.isEmpty()) {
            throw new IllegalStateException(
                "There is no storage node metadata stored in the relational database. This may have happened as a "
                    + "result of running dbsetup or deleting rows from rhq_storage_node table. Please re-install the "
                    + "storage node to fix this issue.");
        }
        session = createSession(username, password, storageNodes);
        metricsDAO = new MetricsDAO(session, metricsConfiguration);

        Server server = serverManager.getServer();
        initMetricsServer(isNewServerInstall, server.getCtime());

        initialized = true;
        log.info("Storage client subsystem is now initialized");
    }

    public synchronized void shutdown() {
        if (!initialized) {
            log.info("Storage client subsystem is already shut down. Skipping shutdown steps.");
            return;
        }

        log.info("Shutting down storage client subsystem");
        metricsServer.shutdown();
        metricsDAO = null;
        metricsServer = null;
        session.getCluster().shutdown();
        session = null;
        initialized = false;
    }

    public MetricsDAO getMetricsDAO() {
        return metricsDAO;
    }

    public MetricsServer getMetricsServer() {
        return metricsServer;
    }

    public StorageSession getSession() {
        return session;
    }

    public MetricsConfiguration getMetricsConfiguration() {
        return this.metricsConfiguration;
    }

    private Session createSession(String username, String password, List<StorageNode> storageNodes) {
        if (log.isDebugEnabled()) {
            log.debug("Initializing session to connect to storage node cluster");
        }
        List<String> hostNames = new ArrayList<String>();
        for (StorageNode storageNode : storageNodes) {
            hostNames.add(storageNode.getAddress());
        }
        int port = storageNodes.get(0).getCqlPort();

        boolean compressionEnabled = Boolean.valueOf(System.getProperty("rhq.cassandra.client.compression-enabled",
            "false"));
        ProtocolOptions.Compression compression;
        if (compressionEnabled) {
            compression = ProtocolOptions.Compression.SNAPPY;
            log.info("Compression has been enabled for the storage client. Be aware that if your storage nodes do " +
                "not support compression then the client will not be able to connect to the storage cluster.");
        } else {
            compression = ProtocolOptions.Compression.NONE;
            log.debug("Storage client compression is disabled");
        }

        Cluster cluster = new ClusterBuilder()
            .addContactPoints(hostNames.toArray(new String[hostNames.size()]))
            .withCredentials(username, password)
            .withPort(port)
            .withCompression(compression)
            .build();

        return cluster.connect(RHQ_KEYSPACE);
    }

    private void initMetricsServer(boolean isNewInstall, long serverInstallTime) {
        if (log.isDebugEnabled()) {
            log.debug("Initializing " + MetricsServer.class.getName());
        }
        metricsServer = new MetricsServer();
        metricsServer.setDAO(metricsDAO);
        metricsServer.setConfiguration(metricsConfiguration);

        DateTimeService dateTimeService = new DateTimeService();
        dateTimeService.setConfiguration(metricsConfiguration);
        metricsServer.setDateTimeService(dateTimeService);
        metricsServer.init(isNewInstall, serverInstallTime);
    }

    private String getRequiredStorageProperty(String property) {
        String value = System.getProperty(property);
        if (StringUtil.isEmpty(property)) {
            throw new IllegalStateException("The system property [" + property + "] is not set. The RHQ "
                + "server will not be able connect to the RHQ storage node(s). This property should be defined "
                + "in rhq-server.properties.");
        }
        return value;
    }
}
