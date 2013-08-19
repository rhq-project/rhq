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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

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

    private static final String JMX_CONNECTION_STRING = "service:jmx:rmi:///jndi/rmi://%s:%s/jmxrmi";

    /**
     * Pings the storage nodes to verify if they are available and native transport
     * is running.
     *
     * @param storageNodes storage node addresses
     * @param jmxPorts JMX ports
     * @param numHosts minimum number of active hosts
     *
     * @return [true] cluster available with at least minimum number of hosts available, [false] otherwise
     */
    public boolean ping(String[] storageNodes, int[] jmxPorts, int numHosts) {
        int connections = 0;
        long sleep = 100;

        for (int index = 0; index < jmxPorts.length; index++) {
            try {
                boolean isNativeTransportRunning = this.isNativeTransportRunning(storageNodes[index], jmxPorts[index]);
                if (isNativeTransportRunning) {
                    ++connections;
                }
                if (connections == numHosts) {
                    return true;
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Unable to open JMX connection on port [" + jmxPorts[index] + "] to cassandra node ["
                        + storageNodes[index] + "]", e);
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
     * See {@link #waitForClusterToStart(int, java.util.List, int)} for details.
     * @param storageNodes The cluster nodes to which a connection should be made
     * @param jmxPorts JMX port for each cluster node address
     */
    public void waitForClusterToStart(String[] storageNodes, int jmxPorts[]) {
        waitForClusterToStart(storageNodes, jmxPorts, storageNodes.length, 10);
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
     * @param numHosts The number of hosts to which a successful connection has to be made
     *                 before returning.
     * @param retries The number of times to retry connecting. A runtime exception will be
     *                thrown when the number of failed connections exceeds this value.
     * @param hosts The cluster nodes to which a connection should be made
     */
    public void waitForClusterToStart(String[] storageNodes, int jmxPorts[], int numHosts, int retries) {
        waitForClusterToStart(storageNodes, jmxPorts, numHosts, 250, retries, 1);
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
     * @param numHosts The number of hosts to which a successful connection has to be made
     *                 before returning.
     * @param delay The amount of time wait between attempts to make a connection
     * @param retries The number of times to retry connecting. A runtime exception will be
     *                thrown when the number of failed connections exceeds this value.
     * @param initialWait The amount of seconds before first try.
     * @param hosts The cluster nodes to which a connection should be made
     */
    public void waitForClusterToStart(String[] storageNodes, int jmxPorts[], int numHosts, long delay,
        int retries, int initialWait) {
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
        Queue<Integer> queue = new LinkedList<Integer>();
        for (int index = 0; index < storageNodes.length; index++) {
            queue.add(index);
        }

        Integer storageNodeIndex = queue.poll();

        while (storageNodeIndex != null) {
            if (failedConnections >= retries) {
                throw new RuntimeException("Unable to verify that cluster nodes have started after "
                    + failedConnections + " failed attempts");
            }
            try {
                boolean isNativeTransportRunning = isNativeTransportRunning(storageNodes[storageNodeIndex],
                    jmxPorts[storageNodeIndex]);
                if (log.isDebugEnabled() && isNativeTransportRunning) {
                    log.debug("Successfully connected to cassandra node [" + storageNodes[storageNodeIndex] + "]");
                }
                if (isNativeTransportRunning) {
                    ++connections;
                } else {
                    queue.offer(storageNodeIndex);
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
                queue.offer(storageNodeIndex);
                if (log.isDebugEnabled()) {
                    log.debug("Unable to open JMX connection on port [" + jmxPorts[storageNodeIndex]
                        + "] to cassandra node [" + storageNodes[storageNodeIndex] + "].", e);
                } else if (log.isInfoEnabled()) {
                    log.debug("Unable to open connection to cassandra node.");
                }
            }
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
            }
            storageNodeIndex = queue.poll();
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
    public void waitForSchemaAgreement(String[] storageNodes, int[] jmxPorts) throws Exception {
        if (storageNodes == null || storageNodes.length == 0) {
            return;
        }

        long sleep = 100L;
        boolean schemaInAgreement = false;

        while (!schemaInAgreement) {
            Set<String> schemaVersions = new HashSet<String>();
            for (int index = 0; index < storageNodes.length; index++) {
                String otherSchchemaVersion = getSchemaVersionForNode(storageNodes[index], jmxPorts[index]);
                    if (otherSchchemaVersion != null) {
                        schemaVersions.add(otherSchchemaVersion);
                    }
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
                String schemaVersion = schemaVersions.iterator().next();
                if (schemaVersion != null) {
                    schemaInAgreement = true;
                } else {
                    if (log.isInfoEnabled()) {
                        log.info("Schema agreement has not been reached. Unable to get the schema version from cassandra nodes ["
                            + storageNodes + "]");
                    }
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                    }
                }
            }

        }
    }

    public boolean isNativeTransportRunning(String storageNode, int jmxPort) throws Exception {
        Boolean nativeTransportRunning = false;
        String url = getJMXConnectionURL(storageNode, jmxPort);
        JMXServiceURL serviceURL = new JMXServiceURL(url);
        Map<String, String> env = new HashMap<String, String>();
        JMXConnector connector = null;

        try {
            connector = JMXConnectorFactory.connect(serviceURL, env);
            MBeanServerConnection serverConnection = connector.getMBeanServerConnection();
            ObjectName storageService = new ObjectName("org.apache.cassandra.db:type=StorageService");
            String attribute = "NativeTransportRunning";
            try {
                nativeTransportRunning = (Boolean) serverConnection.getAttribute(storageService, attribute);
            } catch (Exception e) {
                // It is ok to just catch and log exceptions here particularly in an integration
                // test environment where we could potentially try to do the JMX query before
                // Cassandra is fully initialized. We can query StorageService before the native
                // transport server is initialized which will result in Cassandra throwing a NPE.
                // We do not want propagate that exception because it is just a matter of waiting
                // for Cassandra to finish initializing.
                if (log.isDebugEnabled()) {
                    log.debug("Failed to read attribute [" + attribute + "] from " + storageService, e);
                } else {
                    log.info("Faied to read attribute [" + attribute + "] from " + storageService + ": " +
                        e.getMessage());
                }
            }
        } finally {
            if (connector != null) {
                connector.close();
            }
        }
        return nativeTransportRunning;
    }

    private String getSchemaVersionForNode(String storageNode, int jmxPort) throws Exception {
        String url = this.getJMXConnectionURL(storageNode, jmxPort);
        JMXServiceURL serviceURL = new JMXServiceURL(url);
        Map<String, String> env = new HashMap<String, String>();
        JMXConnector connector = null;

        try {
            connector = JMXConnectorFactory.connect(serviceURL, env);
            MBeanServerConnection serverConnection = connector.getMBeanServerConnection();
            ObjectName storageService = new ObjectName("org.apache.cassandra.db:type=StorageService");
            String attribute = "SchemaVersion";
            try {
                return (String) serverConnection.getAttribute(storageService, attribute);
            } catch (Exception e) {
                // It is ok to just catch and log exceptions here particularly in an integration
                // test environment where we could potentially try to do the JMX query before
                // Cassandra is fully initialized. We can query StorageService before the native
                // transport server is initialized which will result in Cassandra throwing a NPE.
                // We do not want propagate that exception because it is just a matter of waiting
                // for Cassandra to finish initializing.
                if (log.isDebugEnabled()) {
                    log.debug("Failed to read attribute [" + attribute + "] from " + storageService, e);
                } else {
                    log.info("Faied to read attribute [" + attribute + "] from " + storageService + ": " +
                        e.getMessage());
                }
            }
        } finally {
            if (connector != null) {
                connector.close();
            }
        }
        return null;
    }

    /**
     * Constructs the JMX connection URL based on the node address and
     * JMX port
     *
     * @param address
     * @param jmxPort
     * @return
     */
    private String getJMXConnectionURL(String address, int jmxPort) {
        String[] split = JMX_CONNECTION_STRING.split("%s");
        return split[0] + address + split[1] + jmxPort + split[2];
    }
}