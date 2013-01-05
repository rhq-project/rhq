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
import java.util.List;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleAuthInfoProvider;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.CassandraNode;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * @author John Sanda
 */
public class SchemaManager {

    private final Log log = LogFactory.getLog(SchemaManager.class);

    private Session session;

    private String username;

    private String password;

    private List<CassandraNode> nodes = new ArrayList<CassandraNode>();

    public SchemaManager(String username, String password, String... nodes) {
        try {
            this.username = username;
            this.password = password;
            String[] hostNames = new String[nodes.length];
            for (String node : nodes) {
                CassandraNode cassandraNode = CassandraNode.parseNode(node);
                this.nodes.add(cassandraNode);
                hostNames[this.nodes.size() - 1] = cassandraNode.getHostName();
            }

            SimpleAuthInfoProvider authInfoProvider = new SimpleAuthInfoProvider();
            authInfoProvider.add("username", "cassandra").add("password", "cassandra");

            Cluster cluster = Cluster.builder()
                .addContactPoints(hostNames)
                .withAuthInfoProvider(authInfoProvider)
                .build();
            session = cluster.connect("system");
        } catch (NoHostAvailableException e) {
            throw new RuntimeException("Unable create session.", e);
        }
    }

    public void createSchema() {
        try {
            session.execute("CREATE USER rhqadmin SUPERUSER");
            session.execute(
                "CREATE KEYSPACE rhq WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};");
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void dropSchema() {
        try {
            session.execute("DROP KEYSPACE rhq");
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean schemaExists() {
        try {
            String cql = "SELECT keyspace_name FROM schema_keyspaces WHERE keyspace_name = 'rhq'";
            ResultSet resultSet = session.execute(cql);
            return !resultSet.fetchAll().isEmpty();
        } catch (NoHostAvailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateSchema() {
        try {
            Database database = createDatabase(nodes.get(0));
            runLiquibase(database);
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        session.getCluster().shutdown();
    }

    private void runLiquibase(Database database) {
        try {
            ClassLoaderResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();

            Liquibase liquibase = new Liquibase("changelog.xml", resourceAccessor, database);
            liquibase.update(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Database createDatabase(CassandraNode node) throws DatabaseException {
        String url = "jdbc:cassandra://" + node.getHostName() + ":" + node.getThriftPort() + "/system?version=3.0.0";
        String driver = "org.apache.cassandra.cql.jdbc.CassandraDriver";
        String databaseClass = "liquibase.database.ext.CassandraDatabase";
        String defaultCatalog = null;
        String defaultSchema = "rhq";

        log.debug("Cassandra JDBC URL: " + url);

        return CommandLineUtils.createDatabaseObject(getClass().getClassLoader(), url, username, password, driver,
            null, defaultSchema, databaseClass, null);
    }

}
