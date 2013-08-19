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
import java.util.Properties;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.util.ClusterBuilder;

/**
 * @author Stefan Negrea
 */
abstract class AbstractManager {

    private static final String MANAGEMENT_BASE_FOLDER = "management";
    protected static final String DEFAULT_CASSANDRA_USER = "cassandra";
    protected static final String DEFAULT_CASSANDRA_PASSWORD = "cassandra";

    private final Log log = LogFactory.getLog(AbstractManager.class);

    enum Query {
        USER_EXISTS,
        SCHEMA_EXISTS,
        VERSION_COLUMNFAMILY_EXISTS,
        VERSION,
        REPLICATION_FACTOR,
        INSERT_SCHEMA_VERSION;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    private Session session;
    private final String username;
    private final String password;
    private final int cqlPort;
    private final String[] nodes;
    private final UpdateFile managementTasks;

    protected AbstractManager(String username, String password, String[] nodes, int cqlPort) {
        try {
            this.username = username;
            this.password = password;
            this.cqlPort = cqlPort;
            this.nodes = nodes;
        } catch (NoHostAvailableException e) {
            throw new RuntimeException("Unable create storage node session.", e);
        }

        try {
            UpdateFolder managementFolder = new UpdateFolder(MANAGEMENT_BASE_FOLDER);
            managementTasks = managementFolder.getUpdateFiles().get(0);
        } catch (Exception e) {
            throw new RuntimeException("Unable create storage node session.", e);
        }
    }

    /**
     * Init the storage cluster session with the username and password provided
     * at creation.
     */
    protected void initClusterSession() {
        initClusterSession(username, password);
    }

    /**
     * Init the storage cluster session with provided username and password.
     *
     * @param username
     * @param password
     */
    protected void initClusterSession(String username, String password) {
        shutdownClusterConnection();


        log.info("Initializing storage node session.");

        Cluster cluster = new ClusterBuilder().addContactPoints(nodes).withCredentials(username, password)
            .withPort(this.getCqlPort()).withCompression(Compression.NONE).build();

        log.info("Cluster connection configured.");

        session = cluster.connect("system");
        log.info("Cluster connected.");
    }

    /**
     * Shutdown the storage cluster connection.
     */
    protected void shutdownClusterConnection() {
        log.info("Shutting down existing cluster connections");
        if (session != null && session.getCluster() != null) {
            session.getCluster().shutdown();
        }
    }

    /**
     * Get storage cluster size.
     *
     * @return cluster size
     */
    protected int getClusterSize() {
        return nodes.length;
    }

    /**
     * @return the username
     */
    protected String getUsername() {
        return username;
    }

    /**
     * @return the password
     */
    protected String getPassword() {
        return password;
    }

    /**
     * @return the cqlPort
     */
    protected int getCqlPort() {
        return cqlPort;
    }

    /**
     * Runs a CQL query to check the existence of the RHQ user on the storage cluster.
     *
     * @return true if the RHQ user exists, false otherwise
     */
    protected boolean userExists() {
        try {
            ResultSet resultSet = executeManagementQuery(Query.USER_EXISTS, "username", username);
            return !resultSet.all().isEmpty();
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Run a CQL query to check the existence of the RHQ schema.
     *
     * @return true if the RHQ schema exists, false otherwise
     */
    protected boolean schemaExists() {
        try {
            ResultSet resultSet = executeManagementQuery(Query.SCHEMA_EXISTS);
            if (!resultSet.all().isEmpty()) {
                resultSet = executeManagementQuery(Query.VERSION_COLUMNFAMILY_EXISTS);
                return !resultSet.all().isEmpty();
            }
            return false;
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Run a CQL query to retrieve the installed storage schema version.
     *
     * @return current RHQ schema version
     */
    protected int getInstalledSchemaVersion() {
        int maxVersion = 0;
        try {
            ResultSet resultSet = executeManagementQuery(Query.VERSION);
            for (Row row : resultSet.all()) {
                if (maxVersion < row.getInt(0)) {
                    maxVersion = row.getInt(0);
                }
            }
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }

        return maxVersion;
    }

    /**
     * Calculate the replication factor based on the input cluster size.
     *
     * @return calculated replication factor
     */
    protected int calculateNewReplicationFactor() {
        int replicationFactor;
        if (getClusterSize() < 3) {
            replicationFactor = getClusterSize();
        } else if (getClusterSize() < 4) {
            replicationFactor = 2;
        } else {
            replicationFactor = 3;
        }
        return replicationFactor;
    }

    /**
     * Run a CQL query to retrieve the current replication factor for RHQ schema.
     *
     * @return existing replication factor
     */
    protected int queryReplicationFactor() {
        int replicationFactor = 1;
        try {
            ResultSet resultSet = executeManagementQuery(Query.REPLICATION_FACTOR);
            Row row = resultSet.one();

            String replicationFactorString = "replication_factor\"";
            String resultString = row.getString(0);
            resultString = resultString.substring(resultString.indexOf(replicationFactorString)
                + replicationFactorString.length());
            resultString = resultString.substring(resultString.indexOf('"') + 1);
            resultString = resultString.substring(0, resultString.indexOf('"'));

            replicationFactor = Integer.parseInt(resultString);
        } catch (Exception e) {
            log.error(e);
        }

        return replicationFactor;
    }

    /**
     * Execute a named management query.
     *
     * @param query named management query
     * @return result
     */
    protected ResultSet executeManagementQuery(Query query) {
        return executeManagementQuery(query, null);
    }

    /**
     * Execute a named management query with the given property (name,value).
     *
     * @param query named management query
     * @param propertyName property name
     * @param propertyValue property value.
     * @return
     */
    protected ResultSet executeManagementQuery(Query query, String propertyName, String propertyValue) {
        Properties properties = new Properties();
        properties.put(propertyName, propertyValue);
        return executeManagementQuery(query, properties);
    }

    /**
     * Execute a named management query with the given properties.
     *
     * @param query named management query
     * @param properties properties
     * @return
     */
    protected ResultSet executeManagementQuery(Query query, Properties properties) {
        String queryString = managementTasks.getNamedStep(query.toString(), properties);
        return execute(queryString);
    }


    /**
     * Execute all the queries in an update file as returned by @link {@link UpdateFile#getOrderedSteps()}.
     *
     * @param updateFile update file
     * @return list of result sets, one for each executed query.
     */
    protected List<ResultSet> execute(UpdateFile updateFile) {
        return execute(updateFile, null);
    }

    /**
     * Execute all the queries in an update file as returned by @link {@link UpdateFile#getOrderedSteps(Properties))} with
     * the given property (name,value).
     *
     * @param updateFile update file
     * @param propertyName property name
     * @param propertyValue property value
     * @return list of result sets, one for each executed query.
     */
    protected List<ResultSet> execute(UpdateFile updateFile, String propertyName, String propertyValue) {
        Properties properties = new Properties();
        properties.put(propertyName, propertyValue);
        return execute(updateFile, properties);
    }

    /**
     * Execute all the queries in an update file as returned by @link {@link UpdateFile#getOrderedSteps(Properties))} with
     * the given property (name,value).
     *
     * @param updateFile update file
     * @param properties properties
     * @return list of result sets, one for each executed query.
     */
    protected List<ResultSet> execute(UpdateFile updateFile, Properties properties) {
        List<ResultSet> results = new ArrayList<ResultSet>();

        log.info("Applying update file: " + updateFile);
        for (String step : updateFile.getOrderedSteps(properties)) {
            log.info("Statement: \n" + step);
            results.add(execute(step));
        }
        log.info("Applied update file: " + updateFile);

        return results;
    }

    /**
     * Execute a CQL query.
     *
     * @param query query
     * @return result for the query
     */
    protected ResultSet execute(String query) {
        return session.execute(query);
    }

}
