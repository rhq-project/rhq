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

package org.rhq.cassandra.schema;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * @author John Sanda
 */
public class SchemaManager {

    /**
     * The username that RHQ will use to connect to the storage cluster.
     */
    private final String username;

    /**
     * The password that RHQ will use to connect to the storage cluster.
     */
    private final String password;

    /**
     * Node addresses
     */
    private final String[] nodes;

    /**
     *
     */
    private final int cqlPort;

    /**
    *
    * @param username The username RHQ will use to connect to the storage cluster
    * @param password The password RHQ will use to connect to the storage cluster
    * @param nodes A list of seeds nodes that are assumed to be already running and
    *              clustered prior to apply schema changes.
    * @param cqlPort The native CQL port for the storage cluster
    */
    public SchemaManager(String username, String password, String[] nodes, int cqlPort) {
        this.username = username;
        this.password = password;
        this.cqlPort = cqlPort;
        this.nodes = nodes;
    }

    /**
     *
     * @param username The username RHQ will use to connect to the storage cluster.
     * @param password The password RHQ will use to connect to the storage cluster.
     * @param nodes A list of seeds nodes that are assumed to be already running and
     *              clustered prior to apply schema changes.
     * @param cqlPort The native CQL port for the storage cluster
     */
    public SchemaManager(String username, String password, List<String> nodes, int cqlPort) {
        this.username = username;
        this.password = password;
        this.cqlPort = cqlPort;
        this.nodes = nodes.toArray(new String[nodes.size()]);
    }

    /**
     * Install and update the storage cluster schema.
     *
     * @throws Exception
     */
    public void install() throws Exception {
        VersionManager version = new VersionManager(username, password, nodes, cqlPort);
        version.install();
    }

    /**
     * Check the existing storage cluster schema version to ensure it is compatible with the
     * current installation.
     *
     * @throws Exception
     */
    public void checkCompatibility() throws Exception {
        VersionManager version = new VersionManager(username, password, nodes, cqlPort);
        version.checkCompatibility();
    }

    /**
     * Drop storage cluster schema and revert the storage cluster to pre-RHQ state.
     *
     * @throws Exception
     */
    public void drop() throws Exception {
        VersionManager version = new VersionManager(username, password, nodes, cqlPort);
        version.drop();
    }

    /**
     * Update cluster topology settings, such as replication factor.
     *
     * @param isNewSchema
     * @return
     * @throws Exception
     */
    public void updateTopology() throws Exception {
        TopologyManager topology = new TopologyManager(username, password, nodes, cqlPort);
        topology.updateTopology();
    }

    /**
     * Returns the list of storage nodes.
     *
     * @return list of storage nodes
     */
    protected String[] getStorageNodes() {
        return nodes;
    }

    /**
     * A main runner used for direct usage of the schema manager.
     *
     * @param args arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        try {
            Logger root = Logger.getRootLogger();
            if (!root.getAllAppenders().hasMoreElements()) {
                root.addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));
            }
            Logger migratorLogging = root.getLoggerRepository().getLogger("org.rhq");
            migratorLogging.setLevel(Level.ALL);

            if (args.length < 4) {
                System.out.println("Usage      : command username password cqlPort nodes...");
                System.out.println("\n");
                System.out.println("Commands   : install | drop | topology");
                return;
            }

            String command = args[0];
            String username = args[1];
            String password = args[2];
            int cqlPort = Integer.parseInt(args[3]);
            String[] hosts = Arrays.copyOfRange(args, 4, args.length);

            SchemaManager schemaManager = new SchemaManager(username, password, hosts, cqlPort);

            if ("install".equalsIgnoreCase(command)) {
                schemaManager.install();
            } else if ("drop".equalsIgnoreCase(command)) {
                schemaManager.drop();
            } else if ("topology".equalsIgnoreCase(command)) {
                schemaManager.updateTopology();
            } else {
                throw new IllegalArgumentException(command + " not available.");
            }
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
