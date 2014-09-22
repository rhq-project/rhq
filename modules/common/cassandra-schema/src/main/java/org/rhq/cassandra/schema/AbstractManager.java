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

import java.util.Properties;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.AuthenticationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.obfuscation.PicketBoxObfuscator;

/**
 * @author Stefan Negrea
 */
abstract class AbstractManager {

    private static final String MANAGEMENT_BASE_FOLDER = "management";
    protected static final String DEFAULT_CASSANDRA_USER = "cassandra";
    protected static final String DEFAULT_CASSANDRA_PASSWORD = "-1e4662ac0d7ddef155fd5fac8f894a49";

    private final Log log = LogFactory.getLog(AbstractManager.class);
    protected UpdateFolderFactory updateFolderFactory;

    enum Query {
        USER_EXISTS, SCHEMA_EXISTS, VERSION_COLUMNFAMILY_EXISTS, VERSION, REPLICATION_FACTOR, INSERT_SCHEMA_VERSION;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    private SessionManager sessionManager;
    private final String username;
    private final String password;
    private final int cqlPort;
    private final String[] nodes;
    private final UpdateFile managementTasks;

    protected AbstractManager(String username, String password, String[] nodes, int cqlPort,
        SessionManager sessionManager, UpdateFolderFactory updateFolderFactory) {

        this.username = username;
        this.password = password;
        this.cqlPort = cqlPort;
        this.nodes = nodes;
        this.sessionManager = sessionManager;

        try {
            UpdateFolder managementFolder = updateFolderFactory.newUpdateFolder(MANAGEMENT_BASE_FOLDER);
            managementTasks = managementFolder.getUpdateFiles().get(0);
        } catch (Exception e) {
            throw new RuntimeException("Unable create storage node session.", e);
        }
        this.updateFolderFactory = updateFolderFactory;
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
        sessionManager.initSession(username, password, cqlPort, nodes);
    }

    /**
     * Shutdown the storage cluster connection.
     */
    protected void shutdownClusterConnection() {
        log.info("Shutting down existing cluster connections");
        sessionManager.shutdownCluster();
    }

    /**
     * @return The actual size of the cluster which includes both specified and discovered nodes
     */
    protected int getActualClusterSize() {
        return sessionManager.getSession().getCluster().getMetadata().getAllHosts().size();
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
            ResultSet resultSet = execute("SELECT * FROM system_auth.users WHERE name = '" + username + "'");
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
            ResultSet resultSet = execute("SELECT * FROM system.schema_keyspaces WHERE keyspace_name = 'rhq'");
            if (!resultSet.all().isEmpty()) {
                resultSet = execute(
                    "SELECT * FROM system.schema_columnfamilies " +
                    "WHERE keyspace_name='rhq' AND columnfamily_name='schema_version'");
                return !resultSet.all().isEmpty();
            }
            return false;
        } catch (AuthenticationException exp) {
            throw exp;
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
            ResultSet resultSet = execute("SELECT version FROM rhq.schema_version");
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
        int actualClusterSize = getActualClusterSize();
        if (actualClusterSize < 3) {
            replicationFactor = actualClusterSize;
        } else if (actualClusterSize < 4) {
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
            ResultSet resultSet = execute(
                "SELECT strategy_options FROM system.schema_keyspaces where keyspace_name='rhq'");
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
     * Execute all the queries in an update file as returned by @link {@link UpdateFile#getOrderedSteps()}.
     *
     * @param updateFile update file
     * @return list of result sets, one for each executed query.
     */
    protected void execute(UpdateFile updateFile) {
        execute(updateFile, null);
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
    protected void execute(UpdateFile updateFile, String propertyName, String propertyValue) {
        Properties properties = new Properties();
        properties.put(propertyName, propertyValue);
        execute(updateFile, properties);
    }

    /**
     * Execute all the queries in an update file as returned by @link {@link UpdateFile#getOrderedSteps(Properties))} with
     * the given property (name,value).
     *
     * @param updateFile update file
     * @param properties properties
     * @return list of result sets, one for each executed query.
     */
    protected void execute(UpdateFile updateFile, Properties properties) {
        log.info("Applying update file: " + updateFile);
        for (Step step : updateFile.getOrderedSteps()) {
            log.debug(step);
            step.bind(properties);
            step.setSession(sessionManager.getSession());
            step.execute();
        }

        log.info("Applied update file: " + updateFile);
    }

    /**
     * Execute a CQL query.
     *
     * @param query query
     * @return result for the query
     */
    protected ResultSet execute(String query) {
        return sessionManager.getSession().execute(query);
    }

}
