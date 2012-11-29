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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.as.controller.client.ModelControllerClient;

import org.rhq.common.jbossas.client.controller.CoreJBossASClient;
import org.rhq.common.jbossas.client.controller.DeploymentJBossASClient;
import org.rhq.common.jbossas.client.controller.WebJBossASClient;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.installer.ServerInstallUtil.ExistingSchemaOption;
import org.rhq.enterprise.server.installer.ServerInstallUtil.SupportedDatabaseType;

/**
 * @author John Mazzitelli
 */
public class InstallerServiceImpl implements InstallerService {

    private static final String EAR_NAME = "rhq.ear";
    private static final String SYSPROP_PROPFILE = "rhq.server.properties-file";

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
    public void test() throws AutoInstallDisabledException, AlreadyInstalledException, Exception {
        // checks to make sure we can read rhq-server.properties and auto-install is turned on
        // checks to make sure we aren't already installed
        // checks to make sure we can successfully connect to the AS instance
        HashMap<String, String> serverProperties = preInstall();

        // make sure the data is valid
        verifyDataFormats(serverProperties);

        // checks to make sure we can connect to the DB
        final String dbUrl = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
        final String dbUsername = serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME);
        String obfuscatedDbPassword = serverProperties.get(ServerProperties.PROP_DATABASE_PASSWORD);
        String clearTextDbPassword = ServerInstallUtil.deobfuscatePassword(obfuscatedDbPassword);
        String dbErrorStr = testConnection(dbUrl, dbUsername, clearTextDbPassword);
        if (dbErrorStr != null) {
            throw new Exception(dbErrorStr);
        }

        // check the server details configuration
        ServerDetails detailsFromProps = getServerDetailsFromPropertiesOnly(serverProperties);
        ServerDetails detailsFromDb = getServerDetails(dbUrl, dbUsername, clearTextDbPassword,
            detailsFromProps.getName());
        if (detailsFromDb == null) {
            log.info("This will be considered a new server: " + detailsFromProps);
        } else {
            log.info("This [" + detailsFromProps + "] will be considered a reinstallation of an existing server ["
                + detailsFromDb + "]");
        }

        // just warns if the schema will be overwritten
        ExistingSchemaOption existingSchemaOption = getAutoinstallExistingSchemaOption(serverProperties);
        if (existingSchemaOption == ExistingSchemaOption.OVERWRITE) {
            log.warn("The installer has been configured to OVERWRITE any existing data in the database. "
                + "If you do install with this configuration, realize that all existing data in the database "
                + "will be lost.");
        }

        // just logs the location of the AS instance where RHQ will be installed
        String appServerHomeDir = getAppServerHomeDir();
        log.info("The app server where the installation will go is found at: " + appServerHomeDir);

        // give some message to indicate everything looks OK and the user can start the real install
        log.info("It looks like everything is OK and you can start the installation.");
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
            throw new AutoInstallDisabledException(
                "Auto-installation is disabled. Please fully configure rhq-server.properties");
        }

        // make an attempt to connect to the app server - we must make sure its running and we can connect to it
        final String asVersion = testModelControllerClient(serverProperties);
        log("Installing into app server version [" + asVersion + "]");

        // If we are already fully installed, we don't have to do anything. Just return false immediately.
        final String installationResults = getInstallationResults();
        if (installationResults != null) {
            if (installationResults.length() == 0) {
                throw new AlreadyInstalledException(
                    "The installer has already been told to perform its work. The server should be ready soon.");
            } else {
                throw new Exception("The installer has already attempted to install the server but errors occurred:\n"
                    + installationResults);
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
        return null;
    }

    @Override
    public void install(HashMap<String, String> serverProperties, ServerDetails serverDetails,
        String existingSchemaOption) throws AutoInstallDisabledException, AlreadyInstalledException, Exception {

        if (isEarDeployed()) {
            throw new AlreadyInstalledException(
                "It looks like the installation has already been completed - there is nothing for the installer to do.");
        }

        verifyDataFormats(serverProperties);

        // if we are in auto-install mode, ignore the server details passed in and build our own using the given server properties
        // if not in auto-install mode, make sure user gave us the server details that we will need
        final boolean autoInstallMode = ServerInstallUtil.isAutoinstallEnabled(serverProperties);
        if (autoInstallMode) {
            serverDetails = getServerDetailsFromPropertiesOnly(serverProperties);
        } else {
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
            } else {
                serverProperties.put(ServerProperties.PROP_DATABASE_SERVER_NAME, "");
                serverProperties.put(ServerProperties.PROP_DATABASE_PORT, "");
                serverProperties.put(ServerProperties.PROP_DATABASE_DB_NAME, "");
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
        // the password will have been obfuscated, so we need to de-obfucate it in order to use it.
        // make sure the server properties map itself has an obfuscated password
        final String dbUrl = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
        final String dbUsername = serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME);
        String clearTextDbPassword;
        String obfuscatedDbPassword;
        if (autoInstallMode) {
            obfuscatedDbPassword = serverProperties.get(ServerProperties.PROP_DATABASE_PASSWORD);
            clearTextDbPassword = ServerInstallUtil.deobfuscatePassword(obfuscatedDbPassword);
        } else {
            clearTextDbPassword = serverProperties.get(ServerProperties.PROP_DATABASE_PASSWORD);
            obfuscatedDbPassword = ServerInstallUtil.obfuscatePassword(clearTextDbPassword);
            serverProperties.put(ServerProperties.PROP_DATABASE_PASSWORD, obfuscatedDbPassword);
        }
        final String testConnectionErrorMessage = testConnection(dbUrl, dbUsername, clearTextDbPassword);
        if (testConnectionErrorMessage != null) {
            throw new Exception("Cannot connect to the database: " + testConnectionErrorMessage);
        }

        // write the new properties to the rhq-server.properties file
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
                if (isDatabaseSchemaExist(dbUrl, dbUsername, clearTextDbPassword)) {
                    if (ExistingSchemaOption.OVERWRITE == existingSchemaOptionEnum) {
                        log("Database schema exists but installer was told to overwrite it - a new schema will be created now.");
                        ServerInstallUtil.createNewDatabaseSchema(serverProperties, serverDetails, clearTextDbPassword,
                            getLogDir());
                    } else {
                        log("Database schema exists - it will now be updated.");
                        ServerInstallUtil.upgradeExistingDatabaseSchema(serverProperties, serverDetails,
                            clearTextDbPassword, getLogDir());
                    }
                } else {
                    log("Database schema does not yet exist - it will now be created.");
                    ServerInstallUtil.createNewDatabaseSchema(serverProperties, serverDetails, clearTextDbPassword,
                        getLogDir());
                }
            } else {
                log("Ignoring database schema - installer will assume it exists and is already up-to-date.");
            }
        } catch (Exception e) {
            throw new Exception("Could not complete the database schema installation", e);
        }

        // ensure the server info is up to date and stored in the DB
        ServerInstallUtil.storeServerDetails(serverProperties, clearTextDbPassword, serverDetails);

        String appServerConfigDir = getAppServerConfigDir();

        // create a keystore whose cert has a CN of this server's public endpoint address
        ServerInstallUtil.createKeystore(serverDetails, appServerConfigDir);

        // create an rhqadmin/rhqadmin management user so when discovered, the AS7 plugin can immediately
        // connect to the RHQ Server.
        ServerInstallUtil.createDefaultManagementUser(serverDetails, appServerConfigDir);

        // perform stuff that has to get done via the JBossAS management client
        ModelControllerClient mcc = null;
        try {
            mcc = getModelControllerClient();

            // ensure the server info is up to date and stored in the DB
            ServerInstallUtil.setSocketBindings(mcc, serverProperties);

            // Make sure our deployment scanner is configured as we need it
            ServerInstallUtil.configureDeploymentScanner(mcc);
        } finally {
            safeClose(mcc);
        }

        // now create our deployment services and our main EAR
        deployServices(serverProperties);

        // deploy the main EAR app startup module extension
        deployAppExtension();

        // some of the changes we made require the app server container to reload before we can deploy the app
        reloadConfiguration();

        // we need to wait for the reload to finish - wait until we can connect again
        testModelControllerClient(60);

        // deploy the main EAR app subsystem - this is the thing that contains and actually deploys the EAR
        deployAppSubsystem();

        return;
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

        // force some hardcoded defaults for IBM JVMs that must have specific values
        final boolean isIBM = System.getProperty("java.vendor", "").contains("IBM");
        if (isIBM) {
            for (String algPropName : ServerProperties.IBM_ALGOROTHM_SETTINGS) {
                props.setProperty(algPropName, "IbmX509");
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
     * Makes sure the data is at least in the correct format (booleans are true/false, integers are valid numbers).
     *
     * @param serverProperties the server properties to check for correctness
     *
     * @throws Exception if the data was invalid
     */
    private void verifyDataFormats(HashMap<String, String> serverProperties) throws Exception {
        final StringBuilder dataErrors = new StringBuilder();
        for (Map.Entry<String, String> entry : serverProperties.entrySet()) {
            final String name = entry.getKey();
            if (ServerProperties.BOOLEAN_PROPERTIES.contains(name)) {
                final String newValue = entry.getValue();
                if (!(newValue.equals("true") || newValue.equals("false"))) {
                    dataErrors.append("[" + name + "] must be 'true' or 'false' : [" + newValue + "]\n");
                }
            } else if (ServerProperties.INTEGER_PROPERTIES.contains(name)) {
                final String newValue = entry.getValue();
                try {
                    Integer.parseInt(newValue);
                } catch (NumberFormatException e) {
                    if (ServerInstallUtil.isEmpty(newValue) && name.equals(ServerProperties.PROP_CONNECTOR_BIND_PORT)) {
                        // this is a special setting and is allowed to be empty
                    } else {
                        dataErrors.append("[" + name + "] must be a number : [" + newValue + "]\n");
                    }
                }
            }
        }
        if (dataErrors.length() > 0) {
            throw new Exception("Cannot install due to data errors:\n" + dataErrors.toString());
        }
    }

    /**
     * Save the given properties to the server's .properties file.
     *
     * Note that this is private - it is not exposed to the installer UI. It should have no need to save
     * this data outside of the normal installation process (see {@link #install()}).
     * 
     * @param serverProperties the server properties to save
     * @throws Exception if failed to save the properties to the .properties file
     */
    private void saveServerProperties(HashMap<String, String> serverProperties) throws Exception {
        final File serverPropertiesFile = getServerPropertiesFile();
        final PropertiesFileUpdate propsFile = new PropertiesFileUpdate(serverPropertiesFile.getAbsolutePath());

        // GWT can't handle Properties - so we use HashMap but convert to Properties internally
        final Properties props = new Properties();
        for (Map.Entry<String, String> entry : serverProperties.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
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
            mcc = getModelControllerClient();
            final CoreJBossASClient client = new CoreJBossASClient(mcc);
            final String version = client.getAppServerVersion();
            return version;
        } finally {
            safeClose(mcc);
        }
    }

    @Override
    public String getOperatingSystem() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = getModelControllerClient();
            final CoreJBossASClient client = new CoreJBossASClient(mcc);
            final String osName = client.getOperatingSystem();
            return osName;
        } finally {
            safeClose(mcc);
        }
    }

    private String getAppServerHomeDir() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = getModelControllerClient();
            final CoreJBossASClient client = new CoreJBossASClient(mcc);
            final String dir = client.getAppServerHomeDir();
            return dir;
        } finally {
            safeClose(mcc);
        }
    }

    private String getAppServerConfigDir() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = getModelControllerClient();
            final CoreJBossASClient client = new CoreJBossASClient(mcc);
            final String dir = client.getAppServerConfigDir();
            return dir;
        } finally {
            safeClose(mcc);
        }
    }

    private boolean isEarDeployed() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = getModelControllerClient();
            final DeploymentJBossASClient client = new DeploymentJBossASClient(mcc);
            boolean isDeployed = client.isDeployment(EAR_NAME);
            return isDeployed;
        } finally {
            safeClose(mcc);
        }
    }

    private String getLogDir() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = getModelControllerClient();
            final CoreJBossASClient client = new CoreJBossASClient(mcc);
            final String dir = client.getAppServerLogDir();
            return dir;
        } finally {
            safeClose(mcc);
        }
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
     * If we can't (i.e. the connection attempt throws an exception), this method retries periodically
     * until the given number of seconds expires. If it still fails, an exception is thrown.
     * 
     * @param secsToWait the number of seconds to wait before aborting the test
     * @return the app server version that we are connected to
     * 
     * @throws Exception if the connection attempts fail
     */
    private String testModelControllerClient(int secsToWait) throws Exception {
        final long start = System.currentTimeMillis();
        final long end = start + (secsToWait * 1000L);
        Exception error = null;

        while (System.currentTimeMillis() < end) {
            try {
                return testModelControllerClient(null);
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
                throw new Exception("Cannot obtain client connection to the app server", e);
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
                    throw new Exception("Cannot obtain client connection to the app server", e);
                }

                mcc = ModelControllerClient.Factory.create(host, port);
                client = new CoreJBossASClient(mcc);
                asVersion = client.getAppServerVersion();
                this.installerConfiguration.setManagementHost(host);
                this.installerConfiguration.setManagementPort(port);
                return asVersion;
            } catch (Exception e2) {
                // make the cause the very first exception in case it was something other than bad host/port as the problem
                throw new Exception("Cannot obtain client connection to the app server!", e);
            } finally {
                safeClose(mcc);
            }
        } finally {
            safeClose(mcc);
        }
    }

    private ModelControllerClient getModelControllerClient() {
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
            mcc = getModelControllerClient();

            // create the security domain needed by the datasources
            ServerInstallUtil.createDatasourceSecurityDomain(mcc, serverProperties);

            // create the security domain needed by REST
            ServerInstallUtil.createRESTSecurityDomain(mcc, serverProperties);

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

            // we don't want to the JBossAS welcome screen; turn it off
            new WebJBossASClient(mcc).setEnableWelcomeRoot(false);

        } catch (Exception e) {
            log("deployServices failed", e);
            throw new Exception("Failed to deploy services: " + ThrowableUtil.getAllMessages(e));
        } finally {
            safeClose(mcc);
        }
    }

    private void deployAppExtension() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = getModelControllerClient();
            CoreJBossASClient client = new CoreJBossASClient(mcc);
            log("Installing RHQ EAR startup subsystem extension");
            client.addExtension("org.rhq.rhq-enterprise-server-startup-subsystem");
        } finally {
            safeClose(mcc);
        }
    }

    private void deployAppSubsystem() throws Exception {
        ModelControllerClient mcc = null;
        try {
            mcc = getModelControllerClient();
            CoreJBossASClient client = new CoreJBossASClient(mcc);
            log("Installing RHQ EAR subsystem");
            client.addSubsystem("rhq-startup");
        } finally {
            safeClose(mcc);
        }
    }

    private static void safeClose(final ModelControllerClient mcc) {
        if (null != mcc) {
            try {
                mcc.close();
            } catch (Exception e) {
            }
        }
    }

    private void reloadConfiguration() {
        log("Will now ask the app server to reload its configuration");
        ModelControllerClient mcc = null;
        try {
            mcc = getModelControllerClient();
            final CoreJBossASClient client = new CoreJBossASClient(mcc);
            client.reload();
            log("App server has been successfully asked to reload its configuration");
        } catch (Exception e) {
            log("reloadConfiguration failed - restart the server to complete the installation", e);
        } finally {
            safeClose(mcc);
        }
    }
}
