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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.util.StringUtil;

/**
 * @author John Sanda
 */
public class SchemaManager {

    private static final String RHQ_KEYSPACE = "rhq";

    private final Log log = LogFactory.getLog(SchemaManager.class);

    private Session session;

    private String username;

    private String password;

    private List<StorageNode> nodes = new ArrayList<StorageNode>();

    private Integer systemAuthRF = null;

    public SchemaManager(String username, String password, String... nodes) {
        try {
            this.username = username;
            this.password = password;

            for (String node : nodes) {
                StorageNode storageNode = new StorageNode();
                storageNode.parseNodeInformation(node);

                this.nodes.add(storageNode);
            }

            initCluster();
        } catch (NoHostAvailableException e) {
            throw new RuntimeException("Unable create session.", e);
        }
    }

    public SchemaManager(String username, String password, List<StorageNode> nodes) {
        try {
            this.username = username;
            this.password = password;
            this.nodes = nodes;

            initCluster();
        } catch (NoHostAvailableException e) {
            throw new RuntimeException("Unable create session.", e);
        }
    }

    private void initCluster() throws NoHostAvailableException {
        String[] hostNames = new String[nodes.size()];
        for (int i = 0; i < hostNames.length; ++i) {
            hostNames[i] = nodes.get(i).getAddress();
        }

        if (log.isDebugEnabled()) {
            log.debug("Initializing session to connect to " + StringUtil.arrayToString(hostNames));
        } else {
            log.info("Initializing session");
        }

        Cluster cluster = new ClusterBuilder()
            .addContactPoints(hostNames)
            .withCredentials("cassandra", "cassandra")
            .withPort(nodes.get(0).getCqlPort())
            .build();
        session = cluster.connect("system");
    }

    public void createSchema() {
        try {
            if (!schemaExists()) {
                log.info("Preparing to create schema");
                log.debug("Creating user [rhqadmin]");
                session.execute("CREATE USER rhqadmin WITH PASSWORD 'rhqadmin' SUPERUSER");
                log.debug("Creating keyspace [" + RHQ_KEYSPACE + "]");

                int replicationFactor = 1;
                if (nodes.size() == 1) {
                    replicationFactor = 1;
                } else if (nodes.size() < 4) {
                    replicationFactor = 2;
                } else {
                    replicationFactor = 3;
                }
                log.debug("Setting replication_factor to " + replicationFactor + " for rhq keyspace");

                session.execute("CREATE KEYSPACE rhq WITH replication = {'class': 'SimpleStrategy', " +
                    "'replication_factor': " + replicationFactor + "};");

                // Note that once we have a schema management tool back in place, the call
                // to createTables will be moved back to the updateSchema method as it
                // previously was when we were using liquibase. We do NOT want to have
                // separate install/update schema changes. Treating everything as an update as
                // liquibase does dramatically simplifies things.
                createTables();
            } else {
                log.info("Ignoring createSchema, schema already exists.");
            }
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void dropSchema() {
        try {
            if (schemaExists()) {
                log.info("Dropping keyspace [" + RHQ_KEYSPACE + "]");
                session.execute("DROP KEYSPACE rhq");
                session.execute("DROP USER rhqadmin");
            } else {
                log.info("Ignoring dropSchema, schema does not exist.");
            }
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void resetSchema() {
        try {
            dropSchema();
            createSchema();
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean schemaExists() {
        try {
            String cql = "SELECT keyspace_name FROM schema_keyspaces WHERE keyspace_name = 'rhq'";
            ResultSet resultSet = session.execute(cql);
            return !resultSet.all().isEmpty();
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateSchema() {
        log.info("Applying schema updates");
        //createTables();
    }

    private void createTables() {
        int gcGraceSeconds = 864000;
        if (nodes.size() == 1) {
            gcGraceSeconds = 0;
        } else {
            gcGraceSeconds = 691200;  // 8 days
        }

        try {
            // Update the system_auth RF from server code. That makes more sense since we
            // need run repair on each node after the schema change.

//            log.debug("Setting system_auth keyspace replication_factor to " + nodes.size());
//            session.execute(
//                "ALTER KEYSPACE system_auth WITH replication = " +
//                "{'class' : 'SimpleStrategy', 'replication_factor' : " + nodes.size() + "};"
//            );

            log.debug("Creating table raw_metrics");
            session.execute(
                "CREATE TABLE rhq.raw_metrics (" +
                    "schedule_id int, " +
                    "time timestamp, " +
                    "value double, " +
                    "PRIMARY KEY (schedule_id, time) " +
                    ") WITH COMPACT STORAGE AND gc_grace_seconds = " + gcGraceSeconds + ";"
            );
            log.debug("Creating table one_hour_metrics");
            session.execute(
                "CREATE TABLE rhq.one_hour_metrics (" +
                    "schedule_id int, " +
                    "time timestamp, " +
                    "type int, " +
                    "value double, " +
                    "PRIMARY KEY (schedule_id, time, type) " +
                ") WITH COMPACT STORAGE AND gc_grace_seconds = " + gcGraceSeconds + ";"
            );
            log.debug("Creating table six_hour_metrics");
            session.execute(
                "CREATE TABLE rhq.six_hour_metrics (" +
                    "schedule_id int, " +
                    "time timestamp, " +
                    "type int, " +
                    "value double, " +
                    "PRIMARY KEY (schedule_id, time, type) " +
                ") WITH COMPACT STORAGE AND gc_grace_seconds = " + gcGraceSeconds + ";"
            );
            log.debug("Creating table twenty_four_hour_metrics");
            session.execute(
                "CREATE TABLE rhq.twenty_four_hour_metrics (" +
                    "schedule_id int, " +
                    "time timestamp, " +
                    "type int, " +
                    "value double, " +
                    "PRIMARY KEY (schedule_id, time, type) " +
                ") WITH COMPACT STORAGE AND gc_grace_seconds = " + gcGraceSeconds + ";"
            );
            log.debug("Creating table metrics_index");
            session.execute(
                "CREATE TABLE rhq.metrics_index (" +
                    "bucket varchar, " +
                    "time timestamp, " +
                    "schedule_id int, " +
                    "PRIMARY KEY ((bucket, time), schedule_id) " +
                ") WITH COMPACT STORAGE AND gc_grace_seconds = " + gcGraceSeconds + ";"
            );
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        log.info("Shutting down connections");
        session.getCluster().shutdown();
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage      : command username password nodes...");
            System.out.println("\n");
            System.out.println("Commands   : createSchema | dropSchema | resetSchema");
            System.out.println("Node format: hostname|thriftPort|nativeTransportPort");

            return;
        }

        String command = args[0];
        String username = args[1];
        String password = args[2];

        SchemaManager schemaManager = new SchemaManager(username, password, Arrays.copyOfRange(args, 3, args.length));

        if ("createSchema".equalsIgnoreCase(command)) {
            schemaManager.createSchema();

        } else if ("dropSchema".equalsIgnoreCase(command)) {
            schemaManager.dropSchema();

        } else if ("resetSchema".equalsIgnoreCase(command)) {
            schemaManager.resetSchema();
        }

    }

}
