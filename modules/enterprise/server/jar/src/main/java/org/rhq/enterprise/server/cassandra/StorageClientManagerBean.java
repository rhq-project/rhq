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

import java.util.ArrayList;
import java.util.List;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.util.StringUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.server.metrics.CQLException;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsConfiguration;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.MetricsServer;

/**
 * @author John Sanda
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class StorageClientManagerBean {

    private final Log log = LogFactory.getLog(StorageClientManagerBean.class);

    private final String USERNAME_PROP = "rhq.cassandra.username";

    private final String PASSWORD_PROP = "rhq.cassandra.password";

    private final String SEEDS_PROP = "rhq.cassandra.seeds";

    private final String RHQ_KEYSPACE = "rhq";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    private Session session;

    private MetricsConfiguration metricsConfiguration = new MetricsConfiguration();

    private MetricsDAO metricsDAO;

    private MetricsServer metricsServer;

    private boolean initialized;

    @EJB
    private StorageNodeManagerLocal storageNodeManager;

    public synchronized void init() {
        if (initialized) {
            if (log.isDebugEnabled()) {
                log.debug("Storage client subsystem is already initialized. Skipping initialization.");
            }
            return;
        }

        if (log.isInfoEnabled()) {
            log.info("Initializing storage client subsystem");
        }

        String username = getRequiredStorageProperty(USERNAME_PROP);
        String password = getRequiredStorageProperty(PASSWORD_PROP);

        List<StorageNode> existingStorageNodes = storageNodeManager.getStorageNodes();
        if (log.isDebugEnabled()) {
            log.debug("Found existing storage nodes [" + StringUtil.listToString(existingStorageNodes) +
                "] in the database");
        }

        String seeds = System.getProperty(SEEDS_PROP);

        if (StringUtil.isEmpty(seeds) && existingStorageNodes.isEmpty()) {
            // We need to find storage node connection info from one or the other but not
            // necessarily both. If this is a single server deployment where the storage
            // node(s) is running on a separate machine, then SEEDS_PROP will have to be set
            // manually. And in this scenario during the initial deployment, there will not
            // be any storage nodes in the db. In a HA deployment, where there are already
            // storage nodes in the db, an RHQ server does not have to have SEEDS_PROP set
            // since it can obtain connection info from the storage node table.
            throw new IllegalStateException("There are no existing storage nodes defined in the RHQ database and " +
                "the system property [" + SEEDS_PROP + "] is not set. The RHQ server will not be able to connect " +
                "to the RHQ storage node(s). The [" + SEEDS_PROP + "] property should be defined in " +
                "rhq-server.properties.");
        }

        List<StorageNode> seedNodes = parseSeedsProperty(seeds);

        if (existingStorageNodes.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("No storage node entities exist in the database");
                log.debug("Persisting seed nodes [" + StringUtil.listToString(seedNodes) + "]");
            }
            persistNodes(seedNodes);
        } else {
            List<StorageNode> newNodes = findNewStorageNodes(existingStorageNodes, seedNodes);
            if (!newNodes.isEmpty()) {
                log.info("Detected topology change. New seed nodes will be persisted.");
                if (log.isDebugEnabled()) {
                    log.debug("Persisting new seed nodes [" + StringUtil.listToString(newNodes));
                }
                persistNodes(newNodes);
                // TODO schedule quartz job here to run the "add node maintenance" operation on nodes
            }
        }

        List<StorageNode> storageNodes = new ArrayList<StorageNode>(existingStorageNodes.size() + seedNodes.size());
        storageNodes.addAll(existingStorageNodes);
        storageNodes.addAll(seedNodes);

        session = createSession(username, password, storageNodes);
        metricsDAO = new MetricsDAO(session, metricsConfiguration);
        initMetricsServer();

        initialized = true;
        log.info("Storage client subsystem is now initialized");
    }

    public synchronized void shutdown() {
        if (!initialized) {
            log.info("Storage client subsytem is already shut down. Skipping shutdown steps.");
            return;
        }

        log.info("Shuttting down storage client subsystem");
        metricsServer.shutdown();
        metricsDAO = null;
        metricsServer = null;
        session.getCluster().shutdown();
        session = null;
        initialized = false;
    }

    private String getRequiredStorageProperty(String property) {
        String value = System.getProperty(property);
        if (StringUtil.isEmpty(property)) {
            throw new IllegalStateException("The system property [" + property + "] is not set. The RHQ " +
                "server will not be able connect to the RHQ storage node(s). This property should be defined " +
                "in rhq-server.properties.");
        }
        return value;
    }

    private List<StorageNode> parseSeedsProperty(String seedsProperty) {
        String[] seeds = seedsProperty.split(",");
        List<StorageNode> storageNodes = new ArrayList<StorageNode>();
        for (String seed : seeds) {
            StorageNode node = new StorageNode();
            node.parseNodeInformation(seed);
            storageNodes.add(node);
        }
        return storageNodes;
    }

    private List<StorageNode> findNewStorageNodes(List<StorageNode> nodes, List<StorageNode> seedNodes) {
        if (log.isDebugEnabled()) {
            log.debug("Checking system property [" + SEEDS_PROP + "] for any new nodes to be persisted");
        }
        List<StorageNode> newNodes = new ArrayList<StorageNode>();
        for (StorageNode seedNode : seedNodes) {
            // The contains call should be ok even though it is an O(N) operation because
            // the number of storage nodes will be small and this is only done at start up.
            if (!nodes.contains(seedNode)) {
                if (log.isDebugEnabled()) {
                    log.debug("Detected new storage node [" + seedNode + "]");
                }
                newNodes.add(seedNode);
            }
        }
        return newNodes;
    }

    private void persistNodes(List<StorageNode> nodes) {
        for (StorageNode node : nodes) {
            node.setOperationMode(StorageNode.OperationMode.INSTALLED);
            entityManager.persist(node);
        }
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

    private void initMetricsServer() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing " + MetricsServer.class.getName());
        }
        metricsServer = new MetricsServer();
        metricsServer.setDAO(metricsDAO);
        metricsServer.setSession(getSession());
        metricsServer.setConfiguration(metricsConfiguration);

        DateTimeService dateTimeService = new DateTimeService();
        dateTimeService.setConfiguration(metricsConfiguration);
        metricsServer.setDateTimeService(dateTimeService);
    }


//    @PostConstruct
    private void createSession() {
        try {
            String username = System.getProperty("rhq.cassandra.username");
            if (username == null) {
                throw new CQLException("The rhq.cassandra.username property is null. Cannot create session.");
            }

            String password = System.getProperty("rhq.cassandra.password");
            if (password == null) {
                throw new CQLException("The rhq.cassandra.password property is null. Cannot create session.");
            }

            List<StorageNode> storageNodes = storageNodeManager.getStorageNodes();
            if (storageNodes.size() <= 0) {
                throw new CQLException("Storage node seed list not available.");
            }

            List<String> hostNames = new ArrayList<String>();
            for (StorageNode storageNode : storageNodes) {
                hostNames.add(storageNode.getAddress());
            }
            int port = storageNodes.get(0).getCqlPort();

            Cluster cluster = new ClusterBuilder()
                .addContactPoints(hostNames.toArray(new String[hostNames.size()]))
                .withCredentials(username, password)
                .withCompression(ProtocolOptions.Compression.NONE)
                .withPort(port)
                .build();
            session = cluster.connect("rhq");

            metricsDAO = new MetricsDAO(session, metricsConfiguration);
        } catch (Exception  e) {
            throw new CQLException("Unable to create session", e);
        }
    }

    public MetricsDAO getMetricsDAO() {
        return metricsDAO;
    }

    public MetricsServer getMetricsServer() {
        return metricsServer;
    }

    public Session getSession() {
        return session;
    }

    public MetricsConfiguration getMetricsConfiguration() {
        return metricsConfiguration;
    }

}
