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

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.datastax.driver.core.exceptions.AuthenticationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.schema.exception.InstalledSchemaTooAdvancedException;
import org.rhq.cassandra.schema.exception.InstalledSchemaTooOldException;
import org.rhq.cassandra.schema.exception.SchemaNotInstalledException;
import org.rhq.core.domain.cloud.StorageNode;

/**
 * @author Stefan Negrea
 */
class VersionManager extends AbstractManager {

    private static final String SCHEMA_BASE_FOLDER = "schema";

    private final Log log = LogFactory.getLog(VersionManager.class);

    private enum Task {
        Drop("drop"),
        Create("create"),
        Update("update");

        private final String folder;

        private Task(String folder){
            this.folder = folder;
        }

        protected String getFolder() {
            return SCHEMA_BASE_FOLDER + "/" + this.folder + "/";
        }
    }

    public VersionManager(String username, String password, List<StorageNode> nodes) throws Exception {
        super(username, password, nodes);
    }

    /**
     * Install and update the RHQ schema:
     * 1) If the schema does not exist then attempt to create it and then run the updates in order.
     * 2) If the schema exists then run the updates in order.
     *
     * @throws Exception
     */
    public void install() throws Exception {
        log.info("Preparing to install storage schema");

        boolean clusterSessionInitialized = false;
        try {
            initClusterSession();
            clusterSessionInitialized = true;
        } catch (AuthenticationException e) {
            log.debug("Authentication exception. Will now attempt to create the storage schema.");
            log.debug(e);
        } finally {
            shutdownClusterConnection();
        }

        if (!clusterSessionInitialized) {
            create();
        }

        update();
    }

    /**
     * Create RHQ schema and make related updates to the Cassandra installation.
     *
     * @throws Exception
     */
    private void create() throws Exception {
        UpdateFolder updateFolder = new UpdateFolder(Task.Create.getFolder());

        Properties properties = new Properties(System.getProperties());
        properties.put("replication_factor", calculateNewReplicationFactor() + "");
        properties.put("cassandra_user_password", UUID.randomUUID() + "");
        properties.put("rhq_admin_username", getUsername());
        properties.put("rhq_admin_password", getPassword());

        /**
         * NOTE: Before applying any schema, we need to create the rhqadmin user. If we have more
         * than a single node cluster then we also need to set the RF of the system_auth
         * keyspace BEFORE we create the rhqadmin user. If we do not do in this order we will
         * get inconsistent reads which will can result in failed authentication.
         */
        //1. Execute the creation of RHQ schema, version table, admin user.
        try {
            initClusterSession(DEFAULT_CASSANDRA_USER, DEFAULT_CASSANDRA_PASSWORD);
            if (!schemaExists()) {
                execute(updateFolder.getUpdateFiles().get(0), properties);
            } else {
                log.info("Storage schema already exists.");
            }
        } catch (Exception ex) {
            log.error(ex);
            throw new RuntimeException(ex);
        } finally {
            shutdownClusterConnection();
        }

        //2. Change Cassandra default user privileges and password.
        try {
            initClusterSession();
            execute(updateFolder.getUpdateFiles().get(1), properties);
        } finally {
            shutdownClusterConnection();
        }
    }

    /**
     * Update existing schema to the most current version in the update folder.
     *
     * @throws Exception
     */
    private void update() throws Exception {
        try {
            initClusterSession();

            if (!schemaExists()) {
                log.error("Storage schema not installed.");
                throw new RuntimeException("Storage schema not installed propertly, cannot apply schema updates.");
            }

            UpdateFolder updateFolder = new UpdateFolder(Task.Update.getFolder());

            int installedSchemaVersion = getInstalledSchemaVersion();
            log.info("Installed storage schema version is " + installedSchemaVersion);

            int requiredSchemaVersion = updateFolder.getLatestVersion();
            log.info("Required storage schema version is " + requiredSchemaVersion);

            if (requiredSchemaVersion == installedSchemaVersion) {
                log.info("Storage schema version is current ( " + installedSchemaVersion + " ). No updates applied.");
            } else if (requiredSchemaVersion < installedSchemaVersion) {
                log.error("Installed storage cluster schema version: " + installedSchemaVersion +
                    ". Required schema version: " + requiredSchemaVersion
                    + ". Storage cluster schema has been updated beyond the capability of the existing server installation.");
                throw new InstalledSchemaTooAdvancedException();
            } else {
                log.info("Storage schema requires udpates. Updating from version " + installedSchemaVersion
                    + " to version " + requiredSchemaVersion + ".");

                updateFolder.removeAppliedUpdates(installedSchemaVersion);

                if (updateFolder.getUpdateFiles().size() == 0) {
                    log.info("Storage schema is current! No updates applied.");
                } else {
                    for (UpdateFile updateFile : updateFolder.getUpdateFiles()) {
                        execute(updateFile);

                        Properties versionProperties = new Properties();
                        versionProperties.put("version", updateFile.extractVersion() + "");
                        versionProperties.put("time", System.currentTimeMillis() + "");
                        executeManagementQuery(Query.INSERT_SCHEMA_VERSION, versionProperties);

                        log.info("Storage schema update " + updateFile + " applied.");
                    }
                }
            }
        } finally {
            shutdownClusterConnection();
        }
    }

    /**
     * Drop RHQ schema and revert the database to pre-RHQ state:
     * 1) Reinstate Cassandra superuser
     * 2) Drop RHQ schema
     * 3) Drop RHQ user
     *
     * @throws Exception
     */
    public void drop() throws Exception {
        log.info("Preparing to drop storage schema.");

        UpdateFolder updateFolder = new UpdateFolder(Task.Drop.getFolder());
        Properties properties = new Properties(System.getProperties());
        properties.put("rhq_admin_username", getUsername());

        try{
            initClusterSession();
            //1. Reinstated Cassandra superuser
            execute(updateFolder.getUpdateFiles().get(0), properties);
            log.info("Cassandra user reverted to default configuration.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            shutdownClusterConnection();
        }

        try {
            //Use Cassandra superuser to drop RHQ schema and user
            initClusterSession(DEFAULT_CASSANDRA_USER, DEFAULT_CASSANDRA_PASSWORD);

            if (schemaExists()) {
                //2. Drop RHQ schema
                execute(updateFolder.getUpdateFiles().get(1), properties);
                log.info("Storage schema dropped.");
            } else {
                log.info("Storage schema does not exist. Drop operation not required.");
            }

            if (userExists()) {
                //3. Drop RHQ user
                execute(updateFolder.getUpdateFiles().get(2), properties);
                log.info("RHQ admin user dropped from storage cluster.");
            } else {
                log.info("RHQ admin user does not exist on the storage cluster. Drop operation not required.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            shutdownClusterConnection();
        }
    }

    /**
     * Check storage cluster schema version compatibility.
     * If the version installed on the storage cluster is too advanced or too old compared
     * to the version available in the current schema manager an error will thrown.
     *
     * @throws Exception schema compatibility exception
     */
    public void checkCompatibility() throws Exception {
        log.info("Preparing to check storage schema compatibility.");
        try {
            initClusterSession();

            if (!this.schemaExists()) {
                log.error("Storage cluster schema not installed. Please re-run the server installer to install the storage cluster schema properly.");
                throw new SchemaNotInstalledException();
            }

            int installedSchemaVersion = this.getInstalledSchemaVersion();

            UpdateFolder folder = new UpdateFolder(Task.Update.getFolder());
            int requiredSchemaVersion = folder.getLatestVersion();

            if (installedSchemaVersion < requiredSchemaVersion) {
                log.error("Storage cluster schema version:" + installedSchemaVersion + ". Required schema version: "
                    + requiredSchemaVersion + ". Please update storage cluster schema version.");
                throw new InstalledSchemaTooOldException();
            }

            if (installedSchemaVersion > requiredSchemaVersion) {
                log.error("Storage cluster schema version:" + installedSchemaVersion + ". Required schema version: "
                    + requiredSchemaVersion
                    + ". Storage clutser has been updated beyond the capability of the current server installation.");
                throw new InstalledSchemaTooAdvancedException();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            shutdownClusterConnection();

            log.info("Completed storage schema compatibility check.");
        }
    }
}
