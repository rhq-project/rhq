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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

/**
 * This class provides operations to ensure a cluster is initialized and in a consistent
 * state. It does not offer functionality for initializing a cluster but rather to make
 * sure that nodes have started up and are accepting client connections for example.
 *
 * @author John Sanda
 */
public class ClusterInitService {

    private final Log log = LogFactory.getLog(ClusterInitService.class);

    /**
     * Attempts to establish a Thrift RPC connection to the hosts for the number specified.
     * In other words, if there are four hosts and <code>numHosts</code> is two, this
     * method will immediately return after making two successful connections.
     *
     * @param hosts The cluster nodes to which a connection should be made
     * @param numHosts The number of hosts to which a successful connection has to be made
     *                 before returning.
     * @return true if connections are made to the number of specified hosts, false
     * otherwise.
     */
    public boolean ping(List<CassandraNode> hosts, int numHosts) {
        long sleep = 100;
        int timeout = 50;
        int connections = 0;

        for (CassandraNode host : hosts) {
            TSocket socket = new TSocket(host.getHostName(), host.getThriftPort(), timeout);
            try {
                socket.open();
                if (log.isDebugEnabled()) {
                    log.debug("Successfully connected to cassandra node [" + host + "]");
                }
                ++connections;
                socket.close();
                if (connections == numHosts) {
                    return true;
                }
            } catch (TTransportException e) {
                String msg = "Unable to open thrift connection to cassandra node [" + host + "]";
                logException(msg, e);
            }
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
            }
        }

        return false;
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
        long sleep = 250;
        int timeout = 50;
        int connections = 0;
        int failedConnections = 0;
        Queue<CassandraNode> queue = new LinkedList<CassandraNode>(hosts);
        CassandraNode host = queue.poll();

        while (host != null) {
            if (failedConnections >= retries) {
                throw new RuntimeException("Unable to verify that cluster nodes have started after " +
                    failedConnections + " failed attempts");
            }
            TSocket socket = new TSocket(host.getHostName(), host.getThriftPort(), timeout);
            try {
                socket.open();
                if (log.isDebugEnabled()) {
                    log.debug("Successfully connected to cassandra node [" + host + "]");
                }
                ++connections;
                socket.close();
                if (connections == numHosts) {
                    try {
                        log.debug("Successdully connected to all nodes. Sleeping for 10 seconds to allow for the " +
                            "cassandra superuser set up to complete.");
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                    }
                    return;
                }
            } catch (TTransportException e) {
                ++failedConnections;
                queue.offer(host);
                String msg = "Unable to open thrift connection to cassandra node [" + host + "]";
                logException(msg, e);
            }
            try {
                Thread.sleep(sleep);
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
     * @param clusterName The cluster name used by underlying Hector APIs.
     * @param hosts The cluster nodes
     */
    public void waitForSchemaAgreement(String clusterName, List<CassandraNode> hosts) {
        long sleep = 100L;
        CassandraClient client = createClient(hosts.get(0));
        client.openConnection();
        boolean schemaInAgreement = false;
        String schemaVersion = null;

        while (!schemaInAgreement) {
            Map<String, List<String>> schemaVersions = null;
            try {
                schemaVersions = client.describe_schema_versions();
            } catch (InvalidRequestException e) {
                throw new RuntimeException("Unable to get schema versions from " + hosts.get(0), e);
            } catch (TException e) {
                throw new RuntimeException("Unable to get schema versions from " + hosts.get(0), e);
            }
            if (schemaVersions.size() > 1) {
                if (log.isInfoEnabled()) {
                    log.info("Schema agreement has not been reached. Found " + schemaVersions.size() +
                        " schema versions");
                }
                if (log.isDebugEnabled()) {
                    log.debug("Found the following schema versions: " + schemaVersions.keySet());
                }
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                }
            } else {
                schemaVersion = schemaVersions.keySet().iterator().next();
                List<String> hostAddresses = schemaVersions.get(schemaVersion);
                if (hostAddresses.size() == hosts.size()) {
                    schemaInAgreement = true;
                } else {
                    if (log.isInfoEnabled()) {
                        log.info("Schema agreement has not been reached. Found one schema version but only " +
                            hostAddresses.size() + " of " + hosts.size() + " nodes at version [" + schemaVersion + "]");
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Found the following nodes at schema version [" + schemaVersion + "]: " +
                            hostAddresses);
                    }
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        client.closeConnection();

        if (log.isInfoEnabled()) {
            log.info("Schema agreement has been reached at version [" + schemaVersion + "]");
        }
    }

    private CassandraClient createClient(CassandraNode node) {
        TSocket socket = new TSocket(node.getHostName(), node.getThriftPort());
        TFramedTransport transport = new TFramedTransport(socket);
        TProtocol protocol = new TBinaryProtocol(transport);

        return new CassandraClient(socket, protocol, node);
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

    private static class CassandraClient extends Cassandra.Client {
        private TSocket socket;
        private CassandraNode node;

        public CassandraClient(TSocket socket, TProtocol protocol, CassandraNode node) {
            super(protocol);
            this.socket = socket;
            this.node = node;
        }

        public void openConnection() {
            try {
                socket.open();
            } catch (TTransportException e) {
                throw new RuntimeException("Could not open thrift connection to " + node, e);
            }
        }

        public void closeConnection() {
            socket.close();
        }
    }

}
