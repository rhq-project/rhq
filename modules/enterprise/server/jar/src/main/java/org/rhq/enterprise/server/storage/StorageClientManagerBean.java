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
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.datastax.driver.core.policies.LoggingRetryPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.schema.SchemaManager;
import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.util.StringUtil;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
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

    private static final String USERNAME_PROP = "rhq.storage.username";
    private static final String PASSWORD_PROP = "rhq.storage.password";
    private static final String RHQ_KEYSPACE = "rhq";

    @EJB
    private StorageNodeManagerLocal storageNodeManager;

    private Cluster cluster;
    private StorageSession session;
    private MetricsConfiguration metricsConfiguration;
    private MetricsDAO metricsDAO;
    private MetricsServer metricsServer;
    private boolean initialized;
    private StorageClusterMonitor storageClusterMonitor;

    public synchronized void init() {
        if (initialized) {
            if (log.isDebugEnabled()) {
                log.debug("Storage client subsystem is already initialized. Skipping initialization.");
            }
            return;
        }

        log.info("Initializing storage client subsystem");

        String username = getRequiredStorageProperty(USERNAME_PROP);
        String password = getRequiredStorageProperty(PASSWORD_PROP);

        List<StorageNode> storageNodes = new ArrayList<StorageNode>();
        for (StorageNode storageNode : storageNodeManager.getStorageNodes()) {
            // We only want clustered nodes here because we won't be able to connect to
            // node that is not part of the cluster. The filtering here on the operation
            // mode is somewhat convservative because we could also include ADD_MAINTENANCE
            // and REMOVE_MAINTENANCE, but this errors on the side of being safe. Lastly,
            // if a storage node does not have a resource, then that means it was was
            // deployed prior to installing the server.
            if (storageNode.getOperationMode() == StorageNode.OperationMode.NORMAL ||
                storageNode.getOperationMode() == StorageNode.OperationMode.MAINTENANCE ||
                storageNode.getResource() == null) {
                storageNodes.add(storageNode);
            }
        }

        if (storageNodes.isEmpty()) {
            throw new IllegalStateException(
                "There is no storage node metadata stored in the relational database. This may have happened as a "
                    + "result of running dbsetup or deleting rows from rhq_storage_node table. Please re-install the "
                    + "storage node to fix this issue.");
        }

        checkSchemaCompability(username, password, storageNodes);


        Session wrappedSession = createSession(username, password, storageNodes);
        session = new StorageSession(wrappedSession);

        storageClusterMonitor = new StorageClusterMonitor();
        session.addStorageStateListener(storageClusterMonitor);

        metricsConfiguration = new MetricsConfiguration();
        metricsDAO = new MetricsDAO(session, metricsConfiguration);

        initMetricsServer();

        initialized = true;
        log.info("Storage client subsystem is now initialized");
    }

    /**
     * Checks storage node schema compatibility.
     *
     * @param username username
     * @param password password
     * @param storageNodes storage nodes
     */
    private void checkSchemaCompability(String username, String password, List<StorageNode> storageNodes) {
        String[] nodes = new String[storageNodes.size()];
        for (int index = 0; index < storageNodes.size(); index++) {
            nodes[index] = storageNodes.get(index).getAddress();
        }
        int cqlPort = storageNodes.get(0).getCqlPort();

        SchemaManager schemaManager = new SchemaManager(username, password, nodes, cqlPort);
        try {
            schemaManager.checkCompatibility();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public synchronized void shutdown() {
        log.info("Shutting down storage client subsystem");

        if (metricsServer != null) {
            metricsServer.shutdown();
            metricsServer = null;
        }

        metricsDAO = null;

        try {
            if (cluster != null) {
                cluster.shutdown();
            }
        } catch (Exception e) {
            log.error("Failed to shutdown the cluster connection manager for the storage cluster.", e);
        }

        cluster = null;
        session = null;
        initialized = false;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public MetricsDAO getMetricsDAO() {
        return metricsDAO;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public MetricsServer getMetricsServer() {
        return metricsServer;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public StorageSession getSession() {
        return session;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public MetricsConfiguration getMetricsConfiguration() {
        return metricsConfiguration;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean isClusterAvailable() {
        return storageClusterMonitor != null && storageClusterMonitor.isClusterAvailable();
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

        cluster = new ClusterBuilder()
            .addContactPoints(hostNames.toArray(new String[hostNames.size()]))
            .withCredentialsObfuscated(username, password)
            .withPort(port)
            .withLoadBalancingPolicy(new RoundRobinPolicy())
            .withRetryPolicy(new LoggingRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE))
            .withCompression(compression)
            .build();

        return cluster.connect(RHQ_KEYSPACE);
    }

    private void initMetricsServer() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing " + MetricsServer.class.getName());
        }
        metricsServer = new MetricsServer();
        metricsServer.setSession(session);
        metricsServer.setDAO(metricsDAO);
        metricsServer.setConfiguration(metricsConfiguration);

        DateTimeService dateTimeService = new DateTimeService();
        dateTimeService.setConfiguration(metricsConfiguration);
        metricsServer.setDateTimeService(dateTimeService);
        metricsServer.init();
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
