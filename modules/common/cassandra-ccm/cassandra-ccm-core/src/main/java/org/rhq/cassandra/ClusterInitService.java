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

package org.rhq.cassandra;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleAuthInfoProvider;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides operations to ensure a cluster is initialized and in a consistent
 * state. It does not offer functionality for initializing a cluster but rather to make
 * sure that nodes have started up and are accepting client connections for example.
 *
 * @author John Sanda
 * @author Jirka Kremser
 */
public final class ClusterInitService {
        
    private final Log log = LogFactory.getLog(ClusterInitService.class);
    
    private Session initRhqSession(List<CassandraNode> hosts) {
        return initSession(hosts, "rhq", "rhqadmin", "rhqadmin");
    }
    
    private Session initSession(List<CassandraNode> hosts, String keySpace, String username, String password) {
        if (hosts == null) {
            throw new IllegalArgumentException("No cassandra nodes were provided.");
        }
        String[] addresses = new String[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            addresses[i] = hosts.get(i).getHostName();
        }
        Cluster cluster = Cluster.builder().addContactPoints(addresses).withoutMetrics()
            .withAuthInfoProvider(new SimpleAuthInfoProvider().add("username", username).add("password", password))
            .build();
        Session session = cluster.connect(keySpace);
        return session;
    }
    
    public boolean ping(List<CassandraNode> hosts, int numHosts) {
        int connections = 0;
        long sleep = 100;

        for (CassandraNode host : hosts) {
            try {
                boolean isNativeTransportRunning = host.isNativeTransportRunning();
                if (isNativeTransportRunning) {
                    ++connections;
                }
                if (connections == numHosts) {
                    return true;
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Unable to open JMX connection to cassandra node [" + host + "]", e);
                }
                return false;
            }
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ex) {
            }
        }
        return true;
    }

    /**
     * This method attempts to establish a Thrift RPC connection to each host. If the
     * connection fails, the host is retried after going through the other, remaining
     * hosts. A runtime exception will be thrown after 10 failed retries.
     * <br/><br/>
     * After connecting to all nodes, this method will then sleep for a fixed delay.
     * See {@link #waitForClusterToStart(java.util.List, int, int)} for details.
     *
     * @param hosts The cluster nodes to which a connection should be made
     */
    public void waitForClusterToStart(List<CassandraNode> hosts) {
        waitForClusterToStart(hosts, hosts.size(), 10);
    }

    /**
     * This method attempts to establish a Thrift RPC connection to each host for the
     * number specified. In other words, if there are four hosts and <code>numHosts</code>
     * is 2, this method will block only until it can connect to two of the hosts. If the
     * connection fails, the host is retried after going through the other, remaining
     * hosts.
     * <br/><br/>
     * After connecting to all cluster nodes, this method will sleep for 10 seconds
     * before returning. This is to give the cluster a chance to create the system auth
     * schema and to create the cassandra super user. Cassandra has a hard-coded delay of
     * 10 sceonds before it creates the super user, which means the rhq schema cannot be
     * created before that.
     *
     * @param hosts The cluster nodes to which a connection should be made
     * @param numHosts The number of hosts to which a successful connection has to be made
     *                 before returning.
     * @param retries The number of times to retry connecting. A runtime exception will be
     *                thrown when the number of failed connections exceeds this value.
     */
    public void waitForClusterToStart(List<CassandraNode> hosts, int numHosts, int retries) {
        waitForClusterToStart(hosts, numHosts, 250, retries, 1);
    }
    
    
    /**
     * This method attempts to establish a Thrift RPC connection to each host for the
     * number specified. In other words, if there are four hosts and <code>numHosts</code>
     * is 2, this method will block only until it can connect to two of the hosts. If the
     * connection fails, the host is retried after going through the other, remaining
     * hosts.
     * <br/><br/>
     * After connecting to all cluster nodes, this method will sleep for 10 seconds
     * before returning. This is to give the cluster a chance to create the system auth
     * schema and to create the cassandra super user. Cassandra has a hard-coded delay of
     * 10 sceonds before it creates the super user, which means the rhq schema cannot be
     * created before that.
     *
     * @param hosts The cluster nodes to which a connection should be made
     * @param numHosts The number of hosts to which a successful connection has to be made
     *                 before returning.
     * @param delay The amount of time wait between attempts to make a connection
     * @param retries The number of times to retry connecting. A runtime exception will be
     *                thrown when the number of failed connections exceeds this value.
     * @param initialWait The amount of seconds before first try.
     */
    public void waitForClusterToStart(List<CassandraNode> hosts, int numHosts, long delay, int retries, int initialWait) {
        if (initialWait > 0) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Waiting before JMX calls to the storage nodes for " + initialWait + " seconds...");
                }
                Thread.sleep(initialWait * 1000);
            } catch (InterruptedException e) {
            }
        }
        
        int connections = 0;
        int failedConnections = 0;
        Queue<CassandraNode> queue = new LinkedList<CassandraNode>(hosts);
        CassandraNode host = queue.poll();

        while (host != null) {
            if (failedConnections >= retries) {
                throw new RuntimeException("Unable to verify that cluster nodes have started after "
                    + failedConnections + " failed attempts");
            }
            try {
                boolean isNativeTransportRunning = host.isNativeTransportRunning();
                if (log.isDebugEnabled() && isNativeTransportRunning) {
                    log.debug("Successfully connected to cassandra node [" + host + "]");
                }
                if (isNativeTransportRunning) {
                    ++connections;
                }
                if (connections == numHosts) {
                        if (log.isDebugEnabled()) {
                        log.debug("Successdully connected to all nodes. Sleeping for 10 seconds to allow for the "
                            + "cassandra superuser set up to complete.");
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                    }
                    return;
                }
            } catch (Exception e) {
                ++failedConnections;
                queue.offer(host);
                if (log.isDebugEnabled()) {
                    log.debug("Unable to open JMX connection to cassandra node [" + host + "].", e);
                } else if (log.isInfoEnabled()) {
                    log.debug("Unable to open connection to cassandra node.");
                }
            }
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
            }
            host = queue.poll();
        }
    }
    
    /**
     * Waits for the cluster to reach schema agreement. During cluster initialization
     * before and while schema changes propagate throughout the cluster, there could be
     * multiple schema versions found among nodes. Schema agreement is reached when there
     * is a single schema version and all nodes are on that version.
     *
     * @param hosts The cluster nodes
     */
    public void waitForSchemaAgreement(List<CassandraNode> hosts) {
        if (hosts == null) return;
        long sleep = 100L;
        boolean schemaInAgreement = false;

        while (!schemaInAgreement) {
            Set<UUID> schemaVersions = new HashSet<UUID>();
            try {
                for (CassandraNode host : hosts) {
                    UUID otherSchchemaVersion = getSchemaVersionForNode(host);
                    schemaVersions.add(otherSchchemaVersion);
                }
            } catch (NoHostAvailableException e) {
                throw new RuntimeException("Unable to get schema versions from " + hosts.get(0), e);
            }
            if (schemaVersions.size() > 1) {
                if (log.isInfoEnabled()) {
                    log.info("Schema agreement has not been reached. Found " + schemaVersions.size() +
                        " schema versions");
                }
                if (log.isDebugEnabled()) {
                    log.debug("Found the following schema versions: " + schemaVersions);
                }
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                }
            } else {
                UUID schemaVersion = schemaVersions.iterator().next();
                if (schemaVersion != null) {
                    schemaInAgreement = true;
                } else {
                    if (log.isInfoEnabled()) {
                        log.info("Schema agreement has not been reached. Unable to get the schema version from cassandra nodes ["
                            + hosts + "]");
                    }
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                    }
                }
            }

        }
    }
    
    private UUID getSchemaVersionForNode(CassandraNode node) {
        Session session = null;
        try {
            session = initRhqSession(Arrays.asList(node));
            PreparedStatement statement = session.prepare("SELECT schema_version from system.local");
            BoundStatement boundStatement = statement.bind();
            ResultSet rs = session.execute(boundStatement);
            for (Row row : rs) {
                return row.getUUID(0);
            }
            return null;
        } finally {
            if (session != null)
                session.getCluster().shutdown();
        }
    }

}