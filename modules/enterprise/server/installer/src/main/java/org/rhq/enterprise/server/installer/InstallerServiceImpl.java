/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.installer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.datastax.driver.core.exceptions.AuthenticationException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.google.common.collect.ImmutableSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.as.controller.client.ModelControllerClient;

import org.rhq.cassandra.schema.SchemaManager;
import org.rhq.cassandra.schema.exception.InstalledSchemaTooOldException;
import org.rhq.cassandra.schema.exception.SchemaNotInstalledException;
import org.rhq.common.jbossas.client.controller.CoreJBossASClient;
import org.rhq.common.jbossas.client.controller.DatasourceJBossASClient;
import org.rhq.common.jbossas.client.controller.DeploymentJBossASClient;
import org.rhq.common.jbossas.client.controller.MCCHelper;
import org.rhq.common.jbossas.client.controller.WebJBossASClient;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.obfuscation.Obfuscator;
import org.rhq.core.util.obfuscation.PicketBoxObfuscator;
import org.rhq.enterprise.server.installer.ServerInstallUtil.ExistingSchemaOption;
import org.rhq.enterprise.server.installer.ServerInstallUtil.SupportedDatabaseType;

/**
 * @author John Mazzitelli
 */
public class InstallerServiceImpl implements InstallerService {

    private static final String RHQ_EXTENSION_NAME = "org.rhq.server-startup";
    private static final String RHQ_SUBSYSTEM_NAME = "rhq-startup";
    private static final String EAR_NAME = "rhq.ear";
    private static final String SYSPROP_PROPFILE = "rhq.server.properties-file";

    private static final String UNSET = "UNSET";

    private final Log log = LogFactory.getLog(InstallerServiceImpl.class);
    private final InstallerConfiguration installerConfiguration;

    private void log(String s) {
        log.info(s);
    }

    private void log(String s, Throwable t) {
        log.warn(s, t);
    }

    public InstallerServiceImpl(InstallerConfiguration config) {
        this.installerConfiguration = config;
    }

    @Override
    public String obfuscatePassword(String clearTextPassword) throws Exception {
        String obfuscatedPassword = PicketBoxObfuscator.encode(clearTextPassword);
        return obfuscatedPassword;
    }

    @Override
    public void listServers() throws Exception {
        HashMap<String, String> serverProperties = getServerProperties();
        final String dbUrl = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
        final String dbUsername = serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME);
        String obfuscatedDbPassword = serverProperties.get(ServerProperties.PROP_DATABASE_PASSWORD);
        String clearTextDbPassword = PicketBoxObfuscator.decode(obfuscatedDbPassword);
        ArrayList<ServerDetails> allServerDetails = getAllServerDetails(dbUrl, dbUsername, clearTextDbPassword);
        if (allServerDetails == null) {
            log.warn("Cannot get details on all servers");
            return;
        }
        if (allServerDetails.size() == 0) {
            log("There are no known servers currently registered");
            return;
        }

        StringBuilder info = new StringBuilder("Details on currently registered servers");
        info.append("\n");
        info.append("Server Name");
        info.append("\t");
        info.append("Public Endpoint Address");
        info.append("\t");
        info.append("Secure Port");
        info.append("\n");
        for (ServerDetails serverDetails : allServerDetails) {
            info.append(serverDetails.getName());
            info.append("\t");
            info.append(serverDetails.getEndpointAddress());
            info.append("\t");
            info.append(serverDetails.getEndpointPortString());
            info.append("\t");
            info.append(serverDetails.getEndpointSecurePortString());
            info.append("\n");
        }
        log(info.toString());
        return;
    }

    @Override
    public void test() throws AutoInstallDisabledException, AlreadyInstalledException, Exception {
        // checks to make sure we can read rhq-server.properties and auto-install is turned on
        // checks to make sure we aren't already installed
        // checks to make sure we can successfully connect to the AS instance
        HashMap<String, String> serverProperties = preInstall();

        // make sure the data is valid
        ServerProperties.validate(serverProperties);

        // checks to make sure we can connect to the DB
        final String dbUrl = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
        final String dbUsername = serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME);
        final String obfuscatedDbPassword = serverProperties.get(ServerProperties.PROP_DATABASE_PASSWORD);
        String clearTextDbPassword = PicketBoxObfuscator.decode(obfuscatedDbPassword);
        String dbErrorStr = testConnection(dbUrl, dbUsername, clearTextDbPassword);
        if (dbErrorStr != null) {
            throw new Exception(dbErrorStr);
        }

        // check the server details configuration
        ServerDetails detailsFromProps = getServerDetailsFromPropertiesOnly(serverProperties);
        ServerDetails detailsFromDb = getServerDetails(dbUrl, dbUsername, clearTextDbPassword,
            detailsFromProps.getName());
        ExistingSchemaOption existingSchemaOption = getAutoinstallExistingSchemaOption(serverProperties);

        if (detailsFromDb == null) {
            log("This will be considered a new server: " + detailsFromProps);
        } else {
            if (existingSchemaOption == ExistingSchemaOption.OVERWRITE) {
                log("This [" + detailsFromProps + "] will OVERWRITE the existing server [" + detailsFromDb
                    + "] that already exists in the database.");
            } else {
                log("This [" + detailsFromProps + "] will be considered a reinstallation of an existing server ["
                    + detailsFromDb + "]");
            }
        }

        // just warns if the schema will be overwritten
        if (existingSchemaOption == ExistingSchemaOption.OVERWRITE) {
            log.warn("The installer has been configured to OVERWRITE any existing data in the database. "
                + "If you do install with this configuration, realize that all existing data in the database "
                + "will be lost.");
        }

        // just logs the location of the AS instance where RHQ will be installed
        String appServerHomeDir = getAppServerHomeDir();
        log("The app server where the installation will go is found at: " + appServerHomeDir);

        // give some message to indicate everything looks OK and the user can start the real install
        log("It looks like everything is OK and you can start the installation.");
    }

    @Override
    public HashMap<String, String> preInstall() throws AutoInstallDisabledException, AlreadyInstalledException,
        Exception {
        // first, make sure auto-install mode has been enabled, this at least tells us
        // the user edited the server properties for their environment.
        final boolean autoInstallMode;
        final HashMap<String, String> serverProperties;

        try {
            serverProperties = getServerProperties();
            autoInstallMode = ServerInstallUtil.isAutoinstallEnabled(serverProperties);
        } catch (Throwable t) {
            throw new Exception("Cannot determine if in auto-install mode", t);
        }

        if (autoInstallMode) {
            log("The server is preconfigured and ready for auto-install.");
        } else {
            if (this.installerConfiguration.isForceInstall()) {
                log("Auto-installation would have been disabled, but installer was asked to force the install... continuing.");
            } else {
                throw new AutoInstallDisabledException(
                    "Auto-installation is disabled. Please fully configure rhq-server.properties");
            }
        }

        // make an attempt to connect to the app server - we must make sure its running and we can connect to it
        final String asVersion = testModelControllerClient(serverProperties);
        log("Installing into app server version [" + asVersion + "]");

        // If we are already fully installed, we don't have to do anything. Just return false immediately.
        final String installationResults = getInstallationResults();
        if (installationResults != null) {
            if (installationResults.length() == 0) {
                if (this.installerConfiguration.isForceInstall()) {
                    log("The installer appears to have already been told to perform its work, but the installer was asked for force the install... continuing.");
                } else {
                    throw new AlreadyInstalledException(
                        "The installer has already been told to perform its work. The server should be ready soon.");
                }
            } else {
                if (this.installerConfiguration.isForceInstall()) {
                    log("The installer is going to force another installation attempt, even though a previous attempt encountered errors:\n"
                        + installationResults);
                } else {
                    throw new Exception(
                        "The installer has already attempted to install the server but errors occurred:\n"
                            + installationResults);
                }
            }
        }

        // ready for installation
        return serverProperties;
    }

    @Override
    public String getInstallationResults() throws Exception {
        if (isEarDeployed()) {
            return ""; // if the ear is deployed, we've already been fully installed
        }

        // its possible the ear is not yet deployed (during server init/startup, it won't show up)
        // but our marker file should exist (since its the last thing the installer will write).
        // If the marker file exists, we can assume the installer is done and we just have to wait.

        if (getInstalledFileMarker().exists()) {
            return ""; // installer has done all it could - just need to wait for the EAR to fully startup
        }

        // in the future, if we can determine if any errors occurred during past installations,
        // we can return that error message here. For now, just assume the installer is free to try to install.

        return null;
    }

    @Override
    public void install(HashMap<String, String> serverProperties, ServerDetails serverDetails,
        String existingSchemaOption) throws AutoInstallDisabledException, AlreadyInstalledException, Exception {

        if (isEarDeployed()) {
            if (this.installerConfiguration.isForceInstall()) {
                log("It looks like the installation has already been completed, but the installer was asked for force the install... continuing.");
            } else {
                throw new AlreadyInstalledException(
                    "It looks like the installation has already been completed - there is nothing for the installer to do.");
            }
        }

        String appServerConfigDir = getAppServerConfigDir();

        // create an rhqadmin management user so when discovered, the AS7 plugin can immediately
        // connect to the RHQ Server.  The password is generated as we try to make the RHQ server manageable by
        // the plugin without the user having to get involved.
        Random random = new Random();
        String managementPassword = Obfuscator.generateString(random, null, 8);
        ServerInstallUtil.createDefaultManagementUser(managementPassword, serverDetails, appServerConfigDir);

        // Doing this prior to prepareDatabase sets the property before they are validated and saved.
        // The generated password is encoded and then saved as rhq.server.management.password.  This value can then
        // be picked up agent-side by the discovery component, decoded, and set in the connection properties. If all
        // works well no dolphins will be harmed, the rhq server will be protected, and the user sleeps through it.
        String encodedManagementPassword = Obfuscator.encode(managementPassword);
        serverProperties.put(ServerProperties.PROP_MGMT_USER_PASSWORD, encodedManagementPassword);

        // Similarly generate a storage username and password, and encode the password. If already set, don't
        // override. This allows for canned values in a dev env, or user override in a prod env.
        String storageUsername = serverProperties.get(ServerProperties.PROP_STORAGE_USERNAME);
        String storagePassword = serverProperties.get(ServerProperties.PROP_STORAGE_PASSWORD);
        if (ServerInstallUtil.isEmpty(storageUsername)) {
            // note, limit to alpha usernames to ensure we don't violate cassandra identifier rules
            storageUsername = Obfuscator.generateString(random, "abcdefghijklmnopqrstuvwxyz", 8);
            serverProperties.put(ServerProperties.PROP_STORAGE_USERNAME, storageUsername);
        }
        if (ServerInstallUtil.isEmpty(storagePassword)) {
            storagePassword = Obfuscator.generateString(random, null, 8);
            String encodedStoragePassword = PicketBoxObfuscator.encode(storagePassword);
            serverProperties.put(ServerProperties.PROP_STORAGE_PASSWORD, encodedStoragePassword);
        }

        // After manipulating the server props, sanity check them
        Set<String> additionalProperties = new HashSet<String>();
        additionalProperties.add(ServerProperties.PROP_MGMT_USER_PASSWORD);
        additionalProperties.add(ServerProperties.PROP_STORAGE_USERNAME);
        additionalProperties.add(ServerProperties.PROP_STORAGE_PASSWORD);
        ServerProperties.validate(serverProperties, additionalProperties);

        prepareDatabase(serverProperties, serverDetails, existingSchemaOption);

        // perform stuff that has to get done via the JBossAS management client
        ModelControllerClient mcc = null;
        try {
            mcc = createModelControllerClient();

            // ensure the server info is up to date and stored in the DB
            ServerInstallUtil.setSocketBindings(mcc, serverProperties);

            // Make sure our deployment scanner is configured as we need it
            ServerInstallUtil.configureDeploymentScanner(mcc);

            // Set up the transaction manager.
            ServerInstallUtil.configureTransactionManager(mcc);

            // Set up the logging subsystem
            ServerInstallUtil.configureLogging(mcc, serverProperties);

            ServerInstallUtil.createUserSecurityDomain(mcc);
            ServerInstallUtil.createRestSecurityDomain(mcc);

            // create a keystore whose cert has a CN of this server's public endpoint address
            File keystoreFile = ServerInstallUtil.createKeystore(serverDetails != null ? serverDetails
                : getServerDetailsFromPropertiesOnly(serverProperties), appServerConfigDir);

            // make sure all necessary web connectors are configured
            ServerInstallUtil.setupWebConnectors(mcc, appServerConfigDir, serverProperties);
        } finally {
            MCCHelper.safeClose(mcc);
        }

        // now create our deployment services
        deployServices(serverProperties);

        // deploy the main EAR app startup module extension
        deployAppExtension();

        // some of the changes we made require the app server container to reload before we can deploy the app
        reloadConfiguration();

        // we need to wait for the reload to finish - wait until we can connect again
        testModelControllerClient(60);

        // deploy the main EAR app subsystem - this is the thing that contains and actually deploys the EAR
        deployAppSubsystem();

        // write a file marker so that rhqctl can easily determine that the server has been
        // installed.
        try {
            writeInstalledFileMarker();
        } catch (IOException e) {
            log.warn("An error occurred while creating the installed file marker", e);
        }
    }

    @Override
    public void prepareDatabase(HashMap<String, String> serverProperties, ServerDetails serverDetails,
        String existingSchemaOption) throws Exception {

        // if we are in auto-install mode, ignore the server details passed in and build our own using the given server properties
        // if not in auto-install mode, make sure user gave us the server details that we will need
        final boolean autoInstallMode = ServerInstallUtil.isAutoinstallEnabled(serverProperties);
        if (autoInstallMode) {
            serverDetails = getServerDetailsFromPropertiesOnly(serverProperties);
        } else {
            if (serverDetails == null) {
                throw new Exception("Auto-installation is disabled and cannot determine server details");
            }
            if (ServerInstallUtil.isEmpty(serverDetails.getName())) {
                throw new Exception("Please enter a server name");
            }
            if (ServerInstallUtil.isEmpty(serverDetails.getEndpointAddress())) {
                try {
                    serverDetails.setEndpointAddress(InetAddress.getLocalHost().getCanonicalHostName());
                } catch (Exception e) {
                    throw new Exception("Could not assign a server public address automatically - please specify one.");
                }
            }
        }

        // its possible the JDBC URL was changed, clear the factory cache in case the DB version is different now
        DatabaseTypeFactory.clearDatabaseTypeCache();

        // determine the type of database to connect to
        final String databaseType = serverProperties.get(ServerProperties.PROP_DATABASE_TYPE);
        if (ServerInstallUtil.isEmpty(databaseType)) {
            throw new Exception("Please indicate the type of database to connect to");
        }

        SupportedDatabaseType supportedDbType = ServerInstallUtil.getSupportedDatabaseType(databaseType);
        if (supportedDbType == null) {
            throw new Exception("Invalid database type: " + databaseType);
        }

        // parse the database connection URL to extract the servername/port/dbname; this is needed for the XA datasource
        try {
            final String url = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
            Pattern pattern = null;
            if (supportedDbType == SupportedDatabaseType.POSTGRES) {
                pattern = Pattern.compile(".*://(.*):([0123456789]+)/(.*)"); // jdbc:postgresql://host.name:5432/rhq
            } else if (supportedDbType == SupportedDatabaseType.ORACLE) {
                // if we ever find that we'll need these props set, uncomment below and it should all work
                //pattern = Pattern.compile(".*@(.*):([0123456789]+)[:/](.*)"); // jdbc:oracle:thin:@host.name:1521:rhq (or /rhq)
            }

            if (pattern != null) {
                final Matcher match = pattern.matcher(url);
                if (match.find() && (match.groupCount() == 3)) {
                    final String serverName = match.group(1);
                    final String port = match.group(2);
                    final String dbName = match.group(3);
                    serverProperties.put(ServerProperties.PROP_DATABASE_SERVER_NAME, serverName);
                    serverProperties.put(ServerProperties.PROP_DATABASE_PORT, port);
                    serverProperties.put(ServerProperties.PROP_DATABASE_DB_NAME, dbName);
                } else {
                    throw new Exception("Cannot get server, port or db name from connection URL: " + url);
                }
            }
        } catch (Exception e) {
            throw new Exception("JDBC connection URL seems to be invalid", e);
        }

        // make sure the internal database related settings are correct
        try {
            String dialect = null;
            String quartzDriverDelegateClass = "org.quartz.impl.jdbcjobstore.StdJDBCDelegate";
            String quartzSelectWithLockSQL = "SELECT * FROM {0}LOCKS ROWLOCK WHERE LOCK_NAME = ? FOR UPDATE";
            String quartzLockHandlerClass = "org.quartz.impl.jdbcjobstore.StdRowLockSemaphore";

            if (supportedDbType == SupportedDatabaseType.POSTGRES) {
                dialect = "org.hibernate.dialect.PostgreSQLDialect";
                quartzDriverDelegateClass = "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate";
            } else if (supportedDbType == SupportedDatabaseType.ORACLE) {
                dialect = "org.hibernate.dialect.Oracle10gDialect";
                quartzDriverDelegateClass = "org.quartz.impl.jdbcjobstore.oracle.OracleDelegate";
            }

            serverProperties.put(ServerProperties.PROP_DATABASE_HIBERNATE_DIALECT, dialect);
            serverProperties.put(ServerProperties.PROP_QUARTZ_DRIVER_DELEGATE_CLASS, quartzDriverDelegateClass);
            serverProperties.put(ServerProperties.PROP_QUARTZ_SELECT_WITH_LOCK_SQL, quartzSelectWithLockSQL);
            serverProperties.put(ServerProperties.PROP_QUARTZ_LOCK_HANDLER_CLASS, quartzLockHandlerClass);

        } catch (Exception e) {
            throw new Exception("Cannot configure internal database settings", e);
        }

        // test the connection to make sure everything is OK - note that if we are in auto-install mode,
        // the password will have been obfuscated, so we need to de-obfuscate it in order to use it.
        // make sure the server properties map itself has an obfuscated password
        final String dbUrl = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
        final String dbUsername = serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME);
        String clearTextDbPassword;
        String obfuscatedDbPassword;
        if (autoInstallMode) {
            obfuscatedDbPassword = serverProperties.get(ServerProperties.PROP_DATABASE_PASSWORD);
            clearTextDbPassword = PicketBoxObfuscator.decode(obfuscatedDbPassword);
        } else {
            clearTextDbPassword = serverProperties.get(ServerProperties.PROP_DATABASE_PASSWORD);
            obfuscatedDbPassword = PicketBoxObfuscator.encode(clearTextDbPassword);
            serverProperties.put(ServerProperties.PROP_DATABASE_PASSWORD, obfuscatedDbPassword);
        }
        final String testConnectionErrorMessage = testConnection(dbUrl, dbUsername, clearTextDbPassword);
        if (testConnectionErrorMessage != null) {
            throw new Exception("Cannot connect to the database: " + testConnectionErrorMessage);
        }

        // write the new properties to the rhq-server.properties file (this ensures the encoded password is written
        // out).  Being paranoid, leaving this here despite the fact that we now write it out again, at the bottom.
        saveServerProperties(serverProperties);

        // Prepare the db schema.
        // existingSchemaOption is either overwrite, keep or skip.
        // If in auto-install mode, we can be told to overwrite, skip or auto (meaning "keep" if schema exists)
        ExistingSchemaOption existingSchemaOptionEnum;
        if (autoInstallMode) {
            existingSchemaOptionEnum = getAutoinstallExistingSchemaOption(serverProperties);
        } else {
            if (existingSchemaOption == null) {
                throw new Exception("Don't know what to do with the database schema");
            }
            existingSchemaOptionEnum = ExistingSchemaOption.valueOf(existingSchemaOption);
        }

        try {
            if (ExistingSchemaOption.SKIP != existingSchemaOptionEnum) {
                String dbSetupLogDir = getDatabaseSetupLogDir();
                if (isDatabaseSchemaExist(dbUrl, dbUsername, clearTextDbPassword)) {
                    if (ExistingSchemaOption.OVERWRITE == existingSchemaOptionEnum) {
                        log("Database schema exists but installer was told to overwrite it - a new schema will be created now.");
                        ServerInstallUtil.createNewDatabaseSchema(serverProperties, serverDetails, clearTextDbPassword,
                            dbSetupLogDir);
                    } else {
                        log("Database schema exists - it will now be updated.");
                        ServerInstallUtil.upgradeExistingDatabaseSchema(serverProperties, serverDetails,
                            clearTextDbPassword, dbSetupLogDir);
                    }
                } else {
                    log("Database schema does not yet exist - it will now be created.");
                    ServerInstallUtil.createNewDatabaseSchema(serverProperties, serverDetails, clearTextDbPassword,
                        dbSetupLogDir);
                }
            } else {
                log("Ignoring database schema - installer will assume it exists and is already up-to-date.");
            }
        } catch (Exception e) {
            throw new Exception("Could not complete the database schema installation", e);
        }

        // if the storage cluster credentials are already set in the DB (typically an HA install), override
        // what's currently in the server properties file, and then continue with storage schema setup
        Map<String, String> storageProperties = ServerInstallUtil.fetchStorageClusterSettings(serverProperties,
            clearTextDbPassword);
        String[] properties = new String[] { ServerProperties.PROP_STORAGE_USERNAME,
            ServerProperties.PROP_STORAGE_PASSWORD, ServerProperties.PROP_STORAGE_NODES,
            ServerProperties.PROP_STORAGE_GOSSIP_PORT, ServerProperties.PROP_STORAGE_CQL_PORT };
        for (String property : properties) {
            if (!StringUtil.isBlank(storageProperties.get(property))
                && !storageProperties.get(property).equals("UNSET")) {
                serverProperties.put(property, storageProperties.get(property));
            }
        }

        SchemaManager storageNodeSchemaManager = null;
        Set<String> storageNodeAddresses = Collections.emptySet();
        try {
            storageNodeSchemaManager = createStorageNodeSchemaManager(serverProperties);
            if (ExistingSchemaOption.SKIP != existingSchemaOptionEnum) {
                if (ExistingSchemaOption.OVERWRITE == existingSchemaOptionEnum) {
                    log("Storage cluster schema exists but installer was told to overwrite it - a the existing  schema will be "
                        + "created now.");
                    storageNodeSchemaManager.drop();
                }

                try {
                    storageNodeSchemaManager.checkCompatibility();
                } catch (AuthenticationException e1) {
                    log("Install RHQ schema along with updates to storage nodes.");
                    storageNodeSchemaManager.install();
                    storageNodeSchemaManager.updateTopology();
                } catch (SchemaNotInstalledException e2) {
                    log("Install RHQ schema along with updates to storage nodes.");
                    storageNodeSchemaManager.install();
                    storageNodeSchemaManager.updateTopology();
                } catch (InstalledSchemaTooOldException e3) {
                    log("Install RHQ schema updates to storage cluster.");
                    storageNodeSchemaManager.install();
                }
                storageNodeAddresses = storageNodeSchemaManager.getStorageNodeAddresses();
                storageNodeSchemaManager.shutdown();
            } else {
                log("Ignoring storage cluster schema - installer will assume it exists and is already up-to-date.");
            }
        } catch (NoHostAvailableException e) {
            log.error("Failed to connect to the storage cluster. Please check the following:\n" +
                "\t1) At least one storage node is running\n" +
                "\t2) The rhq.storage.nodes property specifies the correct hostname/address of at least one storage node\n" +
                "\t3) The rhq.storage.cql-port property has the correct value\n");
            throw new Exception("Could not connect to the storage cluster: " + ThrowableUtil.getRootMessage(e));
        } catch (Exception e) {
            String msg = "Could not complete storage cluster schema installation: " + ThrowableUtil.getRootMessage(e);
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        // ensure the server info is up to date and stored in the DB
        ServerInstallUtil.storeServerDetails(serverProperties, clearTextDbPassword, serverDetails);
        ServerInstallUtil.persistStorageNodesIfNecessary(serverProperties, clearTextDbPassword,
            parseNodeInformation(serverProperties, storageNodeAddresses));
        ServerInstallUtil.persistStorageClusterSettingsIfNecessary(serverProperties, clearTextDbPassword);

        // For sanity, make sure the server props file is in sync with the db settings.
        saveServerProperties(serverProperties);
    }

    @Override
    public ArrayList<String> getServerNames(String connectionUrl, String username, String password) {
        try {
            return ServerInstallUtil.getServerNames(connectionUrl, username, password);
        } catch (Exception e) {
            log("Could not get the list of registered server names", e);
            return null;
        }
    }

    @Override
    public ArrayList<ServerDetails> getAllServerDetails(String connectionUrl, String username, String password) {
        ArrayList<String> serverNames = getServerNames(connectionUrl, username, password);
        if (serverNames == null) {
            return null;
        }

        ArrayList<ServerDetails> serverDetails = new ArrayList<ServerDetails>(serverNames.size());
        for (String serverName : serverNames) {
            ServerDetails currentDetails = getServerDetails(connectionUrl, username, password, serverName);
            if (currentDetails == null) {
                return null; // just abort - this error should not occur unless the db has problems
            }
            serverDetails.add(currentDetails);
        }

        return serverDetails;
    }

    @Override
    public ServerDetails getServerDetails(String connectionUrl, String username, String password, String serverName) {
        try {
            final ServerDetails sd = ServerInstallUtil.getServerDetails(connectionUrl, username, password, serverName);
            if (sd != null) {
                if (ServerInstallUtil.isEmpty(sd.getName())) {
                    try {
                        sd.setEndpointAddress(InetAddress.getLocalHost().getCanonicalHostName());
                    } catch (Exception ignore) {
                        // oh well.. we'll have to expect the user to set the name they want to use
                    }
                }
                if (ServerInstallUtil.isEmpty(sd.getEndpointAddress())) {
                    try {
                        sd.setEndpointAddress(InetAddress.getLocalHost().getHostAddress());
                    } catch (Exception ignore) {
                        // oh well.. we'll have to expect the user to set the address they want to use
                    }
                }
            }
            return sd;
        } catch (Exception e) {
            log("Could not get server details for [" + serverName + "]", e);
            return null;
        }
    }

    @Override
    public boolean isDatabaseSchemaExist(String connectionUrl, String username, String password) {
        try {
            return ServerInstallUtil.isDatabaseSchemaExist(connectionUrl, username, password);
        } catch (Exception e) {
            log("Could not determine database existence", e);
            return false;
        }
    }

    @Override
    public String testConnection(String connectionUrl, String username, String password) {
        final String results = ServerInstallUtil.testConnection(connectionUrl, username, password);
        return results;
    }

    @Override
    public HashMap<String, String> getServerProperties() throws Exception {
        final File serverPropertiesFile = getServerPropertiesFile();
        final PropertiesFileUpdate propsFile = new PropertiesFileUpdate(serverPropertiesFile.getAbsolutePath());
        final Properties props = propsFile.loadExistingProperties();

        // the default algorithm that RHQ will use in the comm layer will be defined at runtime based on the VM
        // but if the user mistakenly set the algorithm to the Sun value while using IBM JVM, then force
        // some hardcoded defaults so it can more likely work for IBM JVMs.
        final boolean isIBM = System.getProperty("java.vendor", "").contains("IBM");
        if (isIBM) {
            for (String algPropName : ServerProperties.IBM_ALGOROTHM_SETTINGS) {
                if (props.getProperty(algPropName, "").equalsIgnoreCase("SunX509")) {
                    props.setProperty(algPropName, "IbmX509");
                }
            }
        }

        // GWT can't handle Properties - convert to HashMap
        final HashMap<String, String> map = new HashMap<String, String>(props.size());
        for (Object property : props.keySet()) {
            map.put(property.toString(), props.getProperty(property.toString()));
        }
        return map;
    }

    /**
     * Save the given properties to the server's .properties file.
     *
     * Note that this is private - it is not exposed to the installer UI. It should have no need to save
     * this data outside of the normal installation process (see {@link #install}).
     *
     * @param serverProperties the server properties to save
     * @throws Exception if failed to save the properties to the .properties file
     */
    private void saveServerProperties(HashMap<String, String> serverProperties) throws Exception {
        ServerProperties.validate(serverProperties);

        final File serverPropertiesFile = getServerPropertiesFile();
        final PropertiesFileUpdate propsFile = new PropertiesFileUpdate(serverPropertiesFile.getAbsolutePath());

        // this code use to be used within GWT which is why the signature uses HashMap and we convert to Properties here
        final Properties props = new Properties();
        for (Map.Entry<String, String> entry : serverProperties.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
        }

        // BZ 1080508 - the server will fail to install if rhq.server.log-level isn't set
        // (as will be the case for upgrades from older versions), so force it to be set now
        if (!props.containsKey(ServerProperties.PROP_LOG_LEVEL)) {
            props.setProperty(ServerProperties.PROP_LOG_LEVEL, "INFO");
            serverProperties.put(ServerProperties.PROP_LOG_LEVEL, "INFO");
        }

        propsFile.update(props);

        // we need to put them as system properties now so when we hot deploy,
        // the replacement variables in the config files pick up the new values
        for (Map.Entry<String, String> entry : serverProperties.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }

        return;
    }

    @Override
    public String getAppServerVersion() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = createModelControllerClient();
            final CoreJBossASClient client = new CoreJBossASClient(mcc);
            final String version = client.getAppServerVersion();
            return version;
        } finally {
            MCCHelper.safeClose(mcc);
        }
    }

    @Override
    public String getOperatingSystem() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = createModelControllerClient();
            final CoreJBossASClient client = new CoreJBossASClient(mcc);
            final String osName = client.getOperatingSystem();
            return osName;
        } finally {
            MCCHelper.safeClose(mcc);
        }
    }

    private String getAppServerHomeDir() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = createModelControllerClient();
            final CoreJBossASClient client = new CoreJBossASClient(mcc);
            final String dir = client.getAppServerHomeDir();
            return dir;
        } finally {
            MCCHelper.safeClose(mcc);
        }
    }

    private String getAppServerDataDir() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = createModelControllerClient();
            final CoreJBossASClient client = new CoreJBossASClient(mcc);
            final String dir = client.getAppServerDataDir();
            return dir;
        } finally {
            MCCHelper.safeClose(mcc);
        }
    }

    private String getAppServerConfigDir() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = createModelControllerClient();
            final CoreJBossASClient client = new CoreJBossASClient(mcc);
            final String dir = client.getAppServerConfigDir();
            return dir;
        } finally {
            MCCHelper.safeClose(mcc);
        }
    }

    private boolean isEarDeployed() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = createModelControllerClient();
            final DeploymentJBossASClient client = new DeploymentJBossASClient(mcc);
            boolean isDeployed = client.isDeployment(EAR_NAME);
            return isDeployed;
        } finally {
            MCCHelper.safeClose(mcc);
        }
    }

    private boolean isExtensionDeployed() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = createModelControllerClient();
            final CoreJBossASClient client = new CoreJBossASClient(mcc);
            boolean isDeployed = client.isExtension(RHQ_EXTENSION_NAME);
            return isDeployed;
        } finally {
            MCCHelper.safeClose(mcc);
        }
    }

    private String getDatabaseSetupLogDir() throws Exception {

        File logDir;

        // Our installer normally sets this sysprop - so use it for our log dir.
        // If we don't have a log dir sysprop (don't know why we wouldn't), just create a tmp location.
        final String installerLogDirStr = System.getProperty("rhq.server.installer.logdir");
        if (installerLogDirStr != null) {
            logDir = new File(installerLogDirStr);
            logDir.mkdirs();
            if (!logDir.isDirectory()) {
                throw new Exception("Cannot create installer log directory: " + logDir);
            }
        } else {
            final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            if (!tmpDir.isDirectory()) {
                log("Missing tmp dir [" + tmpDir + "]; will use current directory to store logs");
                logDir = new File(".");
            } else {
                logDir = new File(tmpDir, "rhq-installer-db");
                logDir.mkdir();
                if (!logDir.isDirectory()) {
                    log("Cannot create database setup log directory [" + logDir + "]; will use current directory");
                    logDir = new File(".");
                }
            }
        }

        final String logDirPath = logDir.getAbsolutePath();
        log("Database setup log file directory: " + logDirPath);
        return logDirPath;
    }

    private File getServerPropertiesFile() throws Exception {
        // first see if we have a rhq-server.properties system property - if so, use it.
        final String sysprop = System.getProperty(SYSPROP_PROPFILE);
        if (sysprop != null) {
            final File propFile = new File(sysprop);
            if (propFile.isFile()) {
                return propFile;
            } else {
                throw new IllegalArgumentException("System property [" + SYSPROP_PROPFILE
                    + "] pointing to invalid file: " + propFile);
            }
        }

        // otherwise, let's try to find it based on the app server location (we'll assume
        // its the app server bundled with RHQ)
        final File appServerHomeDir = new File(getAppServerHomeDir());
        final File serverPropertiesFile = new File(appServerHomeDir, "../bin/rhq-server.properties");
        return serverPropertiesFile;
    }

    private ExistingSchemaOption getAutoinstallExistingSchemaOption(HashMap<String, String> serverProperties) {
        ExistingSchemaOption existingSchemaOptionEnum;
        final String s = serverProperties.get(ServerProperties.PROP_AUTOINSTALL_DATABASE);
        if (s == null || s.equalsIgnoreCase("auto")) {
            existingSchemaOptionEnum = ExistingSchemaOption.KEEP;
        } else {
            existingSchemaOptionEnum = ExistingSchemaOption.valueOf(s.toUpperCase());
        }
        return existingSchemaOptionEnum;
    }

    /**
     * Returns server details based on information found solely in the given server properties.
     * It does not rely on any database access.
     *
     * This is used by the auto-installation process.
     *
     * @param serverProperties the server properties
     * @throws Exception
     */
    private ServerDetails getServerDetailsFromPropertiesOnly(HashMap<String, String> serverProperties) throws Exception {

        String highAvailabilityName = serverProperties.get(ServerProperties.PROP_HIGH_AVAILABILITY_NAME);
        String publicEndpoint = serverProperties.get(ServerProperties.PROP_AUTOINSTALL_PUBLIC_ADDR);
        int port;
        int securePort;

        if (ServerInstallUtil.isEmpty(highAvailabilityName)) {
            try {
                highAvailabilityName = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (Exception e) {
                log("Could not determine default server name: ", e);
                throw new Exception("Server name is not preconfigured and could not be determined automatically");
            }
        }

        // the public endpoint address is one that can be preconfigured in the special autoinstall property.
        // if that is not specified, then we use either the connector's bind address or the server bind address.
        // if nothing was specified, we'll default to the canonical host name.
        if (ServerInstallUtil.isEmpty(publicEndpoint)) {
            String connBindAddress = serverProperties.get(ServerProperties.PROP_CONNECTOR_BIND_ADDRESS);

            if ((!ServerInstallUtil.isEmpty(connBindAddress)) && (!"0.0.0.0".equals(connBindAddress.trim()))) {
                // the server-side connector bind address is explicitly set, use that
                publicEndpoint = connBindAddress.trim();

            } else {
                String serverBindAddress = serverProperties.get(ServerProperties.PROP_JBOSS_BIND_ADDRESS);
                if ((!ServerInstallUtil.isEmpty(serverBindAddress)) && (!"0.0.0.0".equals(serverBindAddress.trim()))) {
                    // the main JBossAS server bind address is set and it isn't 0.0.0.0, use that
                    publicEndpoint = serverBindAddress.trim();

                } else {
                    try {
                        publicEndpoint = InetAddress.getLocalHost().getCanonicalHostName();
                    } catch (Exception e) {
                        log("Could not determine default public endpoint address: ", e);
                        throw new Exception(
                            "Public endpoint address not preconfigured and could not be determined automatically");
                    }
                }
            }
        }

        // define the public endpoint ports.
        // note that if using a different transport other than (ssl)servlet, we'll
        // take the connector's bind port and use it for both ports. This is to support a special deployment
        // use-case - 99% of the time, the agents will go through the web/tomcat connector and thus we'll use
        // the http/https ports for the public endpoints.
        String connectorTransport = serverProperties.get(ServerProperties.PROP_CONNECTOR_TRANSPORT);
        if (connectorTransport != null && connectorTransport.contains("socket")) {
            // we aren't using the (ssl)servlet protocol, take the connector bind port and use it for the public endpoint ports
            String connectorBindPort = serverProperties.get(ServerProperties.PROP_CONNECTOR_BIND_PORT);
            if (ServerInstallUtil.isEmpty(connectorBindPort) || "0".equals(connectorBindPort.trim())) {
                throw new Exception("Using non-servlet transport [" + connectorTransport + "] but didn't define a port");
            }
            port = Integer.parseInt(connectorBindPort);
            securePort = Integer.parseInt(connectorBindPort);
        } else {
            // this is the typical use-case - the transport is probably (ssl)servlet so use the web http/https ports
            try {
                port = Integer.parseInt(serverProperties.get(ServerProperties.PROP_WEB_HTTP_PORT));
            } catch (Exception e) {
                log("Could not determine port, will use default: " + e);
                port = ServerDetails.DEFAULT_ENDPOINT_PORT;
            }
            try {
                securePort = Integer.parseInt(serverProperties.get(ServerProperties.PROP_WEB_HTTPS_PORT));
            } catch (Exception e) {
                log("Could not determine secure port, will use default: " + e);
                securePort = ServerDetails.DEFAULT_ENDPOINT_SECURE_PORT;
            }
        }

        // everything looks good
        ServerDetails serverDetails = new ServerDetails(highAvailabilityName, publicEndpoint, port, securePort);
        return serverDetails;
    }

    /**
     * This will attempt to determine if we can get a client using our current installer configuration.
     * If we can't (i.e. the connection attempt throws an exception), this method looks at the fallback
     * props for the management host and port values and will re-try using those values. If the retry
     * succeeds, the host/port it used to successfully connect will be stored in the {@link #installerConfiguration}
     * object. If it still fails, this method retries periodically until the given number of seconds expires.
     * If it still fails, an exception is thrown.
     *
     * @param fallbackProps contains jboss.bind.address.management and/or jboss.native.management.port to use
     *                      if the initial connection attempt fails. If null, will be ignored.
     * @param secsToWait the number of seconds to wait before aborting the test
     * @return the app server version that we are connected to
     *
     * @throws Exception if the connection attempts fail
     */
    private String testModelControllerClient(HashMap<String, String> fallbackProps, int secsToWait) throws Exception {
        final long start = System.currentTimeMillis();
        final long end = start + (secsToWait * 1000L);
        Exception error = null;

        while (System.currentTimeMillis() < end) {
            try {
                String retVal = testModelControllerClient(fallbackProps);

                // Not only do we want to make sure we can connect, but we also want to wait for the subsystems to initialize.
                // Let's wait for one of the subsystems to exist; once we know this is up, the rest are probably ready too.
                ModelControllerClient mcc = null;
                try {
                    mcc = createModelControllerClient();
                    if (!(new WebJBossASClient(mcc).isWebSubsystem())) {
                        throw new IllegalStateException(
                            "The server does not appear to be fully started yet (the web subsystem did not start)");
                    }

                    return retVal;
                } finally {
                    MCCHelper.safeClose(mcc);
                }
            } catch (Exception e) {
                error = e;
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ignore) {
                }
            }
        }

        throw new RuntimeException("Timed out before being able to successfully connect to the server", error);
    }

    /**
     * This will attempt to determine if we can get a client using our current installer configuration.
     * If we can't (i.e. the connection attempt throws an exception), this method retries periodically
     * until the given number of seconds expires. If it still fails, an exception is thrown.
     *
     * @param secsToWait the number of seconds to wait before aborting the test
     * @return the app server version that we are connected to
     *
     * @throws Exception if the connection attempts fail
     */
    private String testModelControllerClient(int secsToWait) throws Exception {
        return testModelControllerClient(null, secsToWait);
    }

    /**
     * This will attempt to determine if we can get a client using our current installer configuration.
     * If we can't (i.e. the connection attempt throws an exception), this method looks at the fallback
     * props for the management host and port values and will re-try using those values. If the retry
     * succeeds, the host/port it used to successfully connect will be stored in the {@link #installerConfiguration}
     * object. If it still fails, an exception is thrown.
     *
     * @param fallbackProps contains jboss.bind.address.management and/or jboss.native.management.port to use
     *                      if the initial connection attempt fails. If null, will be ignored.
     * @return the app server version that we are connected to
     *
     * @throws Exception if the connection attempts fail
     */
    private String testModelControllerClient(HashMap<String, String> fallbackProps) throws Exception {
        String host = this.installerConfiguration.getManagementHost();
        int port = this.installerConfiguration.getManagementPort();
        ModelControllerClient mcc = null;
        CoreJBossASClient client;
        String asVersion;

        try {
            mcc = ModelControllerClient.Factory.create(host, port);
            client = new CoreJBossASClient(mcc);
            Properties sysprops = client.getSystemProperties();
            if (!sysprops.containsKey("rhq.server.database.connection-url")) {
                throw new Exception("Not an RHQ Server");
            }
            asVersion = client.getAppServerVersion();
            return asVersion;
        } catch (Exception e) {
            try {
                mcc.close(); // so we don't leak threads
                mcc = null;
            } catch (IOException ignore) {
            }

            // if the caller didn't give us any fallback props, just immediately fail
            if (fallbackProps == null) {
                throw new Exception("Cannot obtain client connection to the RHQ app server", e);
            }

            try {
                // try the host/port as specified in the fallbackProps
                // if the host/port in the fallbackProps are the sames as the ones we tried above,
                // don't bother trying again
                boolean differentValues = false;
                String hostStr = fallbackProps.get("jboss.bind.address.management");
                if (hostStr != null && !hostStr.equals(host)) {
                    host = hostStr;
                    differentValues = true;
                }
                String portStr = fallbackProps.get("jboss.management.native.port");
                if (portStr != null && !portStr.equals(String.valueOf(port))) {
                    port = Integer.parseInt(portStr);
                    differentValues = true;
                }
                if (!differentValues) {
                    throw new Exception("Cannot obtain client connection to the RHQ app server!", e);
                }

                mcc = ModelControllerClient.Factory.create(host, port);
                client = new CoreJBossASClient(mcc);
                Properties sysprops = client.getSystemProperties();
                if (!sysprops.containsKey("rhq.server.database.connection-url")) {
                    throw new Exception("Not an RHQ Server");
                }
                asVersion = client.getAppServerVersion();
                this.installerConfiguration.setManagementHost(host);
                this.installerConfiguration.setManagementPort(port);
                return asVersion;
            } catch (Exception e2) {
                // make the cause the very first exception in case it was something other than bad host/port as the problem
                throw new Exception("Cannot obtain client connection to the RHQ app server!!", e);
            } finally {
                MCCHelper.safeClose(mcc);
                mcc = null;
            }
        } finally {
            MCCHelper.safeClose(mcc);
        }
    }

    private ModelControllerClient createModelControllerClient() {
        ModelControllerClient client;
        try {
            String host = this.installerConfiguration.getManagementHost();
            int port = this.installerConfiguration.getManagementPort();
            client = ModelControllerClient.Factory.create(host, port);
        } catch (Exception e) {
            throw new RuntimeException("Cannot obtain client connection to the app server", e);
        }
        return client;
    }

    private void deployServices(HashMap<String, String> serverProperties) throws Exception {

        ModelControllerClient mcc = null;
        try {
            mcc = createModelControllerClient();

            // create the security domain needed by the datasources
            ServerInstallUtil.createDatasourceSecurityDomain(mcc, serverProperties);

            // set up REST cache
            ServerInstallUtil.createNewCaches(mcc, serverProperties);

            // create the JDBC driver configurations for use by datasources
            ServerInstallUtil.createNewJdbcDrivers(mcc, serverProperties);

            // create the datasources
            ServerInstallUtil.createNewDatasources(mcc, serverProperties);

            // create the JMS queues
            ServerInstallUtil.createNewJMSQueues(mcc, serverProperties);

            // setup the email service
            ServerInstallUtil.setupMailService(mcc, serverProperties);

            // we use the welcome root webapp for indicating the state of installation
            // new WebJBossASClient(mcc).setEnableWelcomeRoot(false);

            // we don't want users to access the admin console
            new CoreJBossASClient(mcc).setEnableAdminConsole(false);

            // no need for the example datasource - if it exists, remove it
            new DatasourceJBossASClient(mcc).removeDatasource("ExampleDS");

        } catch (Exception e) {
            log("deployServices failed", e);
            throw new Exception("Failed to deploy services: " + ThrowableUtil.getAllMessages(e));
        } finally {
            MCCHelper.safeClose(mcc);
        }
    }

    private void deployAppExtension() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = createModelControllerClient();
            CoreJBossASClient client = new CoreJBossASClient(mcc);
            boolean isDeployed = client.isExtension(RHQ_EXTENSION_NAME);
            if (!isDeployed) {
                log("Installing RHQ EAR startup subsystem extension");
                client.addExtension(RHQ_EXTENSION_NAME);
            } else {
                log("RHQ EAR startup subsystem extension is already deployed");
            }
        } finally {
            MCCHelper.safeClose(mcc);
        }
    }

    private void deployAppSubsystem() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = createModelControllerClient();
            CoreJBossASClient client = new CoreJBossASClient(mcc);
            boolean isDeployed = client.isSubsystem(RHQ_SUBSYSTEM_NAME);
            if (!isDeployed) {
                log("Installing RHQ EAR subsystem");
                client.addSubsystem(RHQ_SUBSYSTEM_NAME);
            } else {
                log("RHQ EAR subsystem is already deployed");
            }
        } finally {
            MCCHelper.safeClose(mcc);
        }
    }

    private void reloadConfiguration() {
        log("Will now ask the app server to reload its configuration");
        ModelControllerClient mcc = null;
        try {
            mcc = createModelControllerClient();
            final CoreJBossASClient client = new CoreJBossASClient(mcc);
            client.reload();
            log("App server has been successfully asked to reload its configuration");
        } catch (Exception e) {
            log("reloadConfiguration failed - restart the server to complete the installation", e);
        } finally {
            MCCHelper.safeClose(mcc);
        }
    }

    private Set<StorageNode> parseNodeInformation(Map<String, String> serverProps, Set<String> storageNodeAddresses) {
        int cqlPort = Integer.parseInt(serverProps.get(ServerProperties.PROP_STORAGE_CQL_PORT));

        Set<StorageNode> parsedNodes = new TreeSet<StorageNode>(new Comparator<StorageNode>() {
            @Override
            public int compare(StorageNode left, StorageNode right) {
                return left.getAddress().compareTo(right.getAddress());
            }
        });
        for (String address : storageNodeAddresses) {
            StorageNode node = new StorageNode();
            node.setAddress(address);
            node.setCqlPort(cqlPort);
            parsedNodes.add(node);
        }

        return parsedNodes;
    }

    private Set<StorageNode> parseNodeInformation(Map<String, String> serverProps) {
        return parseNodeInformation(serverProps, ImmutableSet.copyOf(serverProps.get(
            ServerProperties.PROP_STORAGE_NODES).split(",")));
    }

    private SchemaManager createStorageNodeSchemaManager(Map<String, String> serverProps) {
        String username = serverProps.get(ServerProperties.PROP_STORAGE_USERNAME);
        String password = serverProps.get(ServerProperties.PROP_STORAGE_PASSWORD);

        List<StorageNode> storageNodes = new ArrayList<StorageNode>(parseNodeInformation(serverProps));
        String[] nodes = new String[storageNodes.size()];
        for (int index = 0; index < storageNodes.size(); index++) {
            nodes[index] = storageNodes.get(index).getAddress();
        }
        int cqlPort = storageNodes.get(0).getCqlPort();

        return new SchemaManager(username, password, nodes, cqlPort);
    }

    private File getInstalledFileMarker() throws Exception {
        File datadir = new File(getAppServerDataDir());
        if (!datadir.isDirectory()) {
            throw new IOException("Directory Not Found: [" + datadir.getPath() + "]");
        }
        File markerFile = new File(datadir, "rhq.installed");
        return markerFile;
    }

    private void writeInstalledFileMarker() throws Exception {
        File markerFile = getInstalledFileMarker();
        markerFile.createNewFile();
    }
}
