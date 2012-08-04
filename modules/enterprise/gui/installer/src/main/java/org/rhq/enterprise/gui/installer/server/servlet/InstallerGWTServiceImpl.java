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
package org.rhq.enterprise.gui.installer.server.servlet;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.jboss.as.controller.client.ModelControllerClient;

import org.rhq.common.jbossas.client.controller.Address;
import org.rhq.common.jbossas.client.controller.JBossASClient;
import org.rhq.common.jbossas.client.controller.SecurityDomainJBossASClient;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.installer.client.gwt.InstallerGWTService;
import org.rhq.enterprise.gui.installer.client.shared.ServerDetails;
import org.rhq.enterprise.gui.installer.client.shared.ServerProperties;
import org.rhq.enterprise.gui.installer.server.service.ManagementService;
import org.rhq.enterprise.gui.installer.server.servlet.ServerInstallUtil.ExistingSchemaOption;

/**
 * Remote RPC API implementation for the GWT Installer.
 *
 * @author John Mazzitelli
 */
@WebServlet(value = "/org.rhq.enterprise.gui.installer.Installer/InstallerGWTService")
public class InstallerGWTServiceImpl extends RemoteServiceServlet implements InstallerGWTService {

    private static final long serialVersionUID = 1L;

    private static final String RHQ_SECURITY_DOMAIN = "RHQDSSecurityDomain";

    @Override
    public void install(HashMap<String, String> serverProperties, ServerDetails serverDetails, String existingSchemaOption) throws Exception {
        // make sure the data is at least in the correct format (booleans are true/false, integers are valid numbers)
        StringBuilder dataErrors = new StringBuilder();
        for (Map.Entry<String, String> entry : serverProperties.entrySet()) {
            String name = entry.getKey();
            if (ServerProperties.BOOLEAN_PROPERTIES.contains(name)) {
                String newValue = entry.getValue();
                if (!(newValue.equals("true") || newValue.equals("false"))) {
                    dataErrors.append("[" + name + "] must be 'true' or 'false' : [" + newValue + "]\n");
                }
            } else if (ServerProperties.INTEGER_PROPERTIES.contains(name)) {
                String newValue = entry.getValue();
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

        // if we are in auto-install mode, ignore the server details passed in and build our own using the given server properties
        // if not in auto-install mode, make sure user gave us the server details that we will need
        boolean autoInstallMode = ServerInstallUtil.isAutoinstallEnabled(serverProperties);
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
        String databaseType = serverProperties.get(ServerProperties.PROP_DATABASE_TYPE);
        if (ServerInstallUtil.isEmpty(databaseType)) {
            throw new Exception("Please indicate the type of database to connect to");
        }

        boolean isPostgres = databaseType.toLowerCase().indexOf("postgres") > -1;
        boolean isOracle = databaseType.toLowerCase().indexOf("oracle") > -1;

        if (isPostgres == false && isOracle == false) {
            throw new IllegalArgumentException("Invalid database type: " + databaseType);
        }

        // parse the database connection URL to extract the servername/port/dbname; this is needed for the XA datasource
        try {
            String url = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
            Pattern pattern = null;
            if (isPostgres) {
                pattern = Pattern.compile(".*://(.*):([0123456789]+)/(.*)"); // jdbc:postgresql://host.name:5432/rhq
            } else if (isOracle) {
                // if we ever find that we'll need these props set, uncomment below and it should all work
                //pattern = Pattern.compile(".*@(.*):([0123456789]+)[:/](.*)"); // jdbc:oracle:thin:@host.name:1521:rhq (or /rhq)
            }

            if (pattern != null) {
                Matcher match = pattern.matcher(url);
                if (match.find() && (match.groupCount() == 3)) {
                    String serverName = match.group(1);
                    String port = match.group(2);
                    String dbName = match.group(3);
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
            throw new Exception("JDBC connection URL seems to be invalid: " + ThrowableUtil.getAllMessages(e));
        }

        // make sure the internal database related settings are correct
        try {
            String dialect = null;
            String quartzDriverDelegateClass = "org.quartz.impl.jdbcjobstore.StdJDBCDelegate";
            String quartzSelectWithLockSQL = "SELECT * FROM {0}LOCKS ROWLOCK WHERE LOCK_NAME = ? FOR UPDATE";
            String quartzLockHandlerClass = "org.quartz.impl.jdbcjobstore.StdRowLockSemaphore";

            if (isPostgres) {
                dialect = "org.hibernate.dialect.PostgreSQLDialect";
                quartzDriverDelegateClass = "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate";
            } else if (isOracle) {
                dialect = "org.hibernate.dialect.Oracle10gDialect";
                quartzDriverDelegateClass = "org.quartz.impl.jdbcjobstore.oracle.OracleDelegate";
            }

            serverProperties.put(ServerProperties.PROP_DATABASE_HIBERNATE_DIALECT, dialect);
            serverProperties.put(ServerProperties.PROP_QUARTZ_DRIVER_DELEGATE_CLASS, quartzDriverDelegateClass);
            serverProperties.put(ServerProperties.PROP_QUARTZ_SELECT_WITH_LOCK_SQL, quartzSelectWithLockSQL);
            serverProperties.put(ServerProperties.PROP_QUARTZ_LOCK_HANDLER_CLASS, quartzLockHandlerClass);

        } catch (Exception e) {
            throw new Exception("Cannot configure internal database settings: " + ThrowableUtil.getAllMessages(e));
        }

        // test the connection to make sure everything is OK - note that if we are in auto-install mode,
        // the password will have been obfuscated, so we need to de-obfucate it in order to use it.
        // make sure the server properties map itself has an obfuscated password
        String dbUrl = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
        String dbUsername = serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME);
        String dbPassword = serverProperties.get(ServerProperties.PROP_DATABASE_PASSWORD);
        if (autoInstallMode) {
            dbPassword = ServerInstallUtil.deobfuscatePassword(dbPassword);
        } else {
            serverProperties.put(ServerProperties.PROP_DATABASE_PASSWORD,
                ServerInstallUtil.obfuscatePassword(dbPassword));
        }
        String testConnectionErrorMessage = testConnection(dbUrl, dbUsername, dbPassword);
        if (testConnectionErrorMessage != null) {
            throw new Exception("Cannot connect to the database: " + testConnectionErrorMessage);
        }

        // write the new properties to the rhq-server.properties file
        saveServerProperties(serverProperties);

        // Prepare the db schema.
        // existingSchemaOption is either overwrite, keep or skip.
        // If in auto-install mode, we can be told to overwrite, skip or auto (meaning "keep" if schema exists)
        ServerInstallUtil.ExistingSchemaOption existingSchemaOptionEnum;
        if (autoInstallMode) {
            String s = serverProperties.get(ServerProperties.PROP_AUTOINSTALL_DATABASE);
            if (s == null || s.equalsIgnoreCase("auto")) {
                existingSchemaOptionEnum = ServerInstallUtil.ExistingSchemaOption.KEEP;
            } else {
                existingSchemaOptionEnum = ServerInstallUtil.ExistingSchemaOption.valueOf(s);
            }
        } else {
            if (existingSchemaOption == null) {
                throw new Exception("Don't know what to do with the database schema");
            }
            existingSchemaOptionEnum = ServerInstallUtil.ExistingSchemaOption.valueOf(existingSchemaOption);
        }

        if (ServerInstallUtil.ExistingSchemaOption.SKIP != existingSchemaOptionEnum) {
            if (isDatabaseSchemaExist(dbUrl, dbUsername, dbPassword)) {
                if (ExistingSchemaOption.OVERWRITE == existingSchemaOptionEnum) {
                    ServerInstallUtil.createNewDatabaseSchema(serverProperties, serverDetails, dbPassword, getLogDir());
                } else {
                    ServerInstallUtil.upgradeExistingDatabaseSchema(serverProperties, serverDetails, dbPassword,
                        getLogDir());
                }
            } else {
                ServerInstallUtil.createNewDatabaseSchema(serverProperties, serverDetails, dbPassword, getLogDir());
            }
        }

        // ensure the server info is up to date and stored in the DB
        ServerInstallUtil.storeServerDetails(serverProperties, dbPassword, serverDetails);

        // create a keystore whose cert has a CN of this server's public endpoint address
        ServerInstallUtil.createKeystore(serverDetails, getAppServerConfigDir());

        // now create our deployment services and our main EAR
        // TODO: finish this
        return;
    }

    @Override
    public ArrayList<String> getServerNames(String connectionUrl, String username, String password) throws Exception {
        try {
            return ServerInstallUtil.getServerNames(connectionUrl, username, password);
        } catch (Exception e) {
            log("Could not get the list of registered server names", e);
            return null;
        }
    }

    @Override
    public ServerDetails getServerDetails(String connectionUrl, String username, String password, String serverName)
        throws Exception {
        try {
            ServerDetails sd = ServerInstallUtil.getServerDetails(connectionUrl, username, password, serverName);
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
            return sd;
        } catch (Exception e) {
            log("Could not get server details for [" + serverName + "]", e);
            return null;
        }
    }

    @Override
    public boolean isDatabaseSchemaExist(String connectionUrl, String username, String password) throws Exception {
        try {
            return ServerInstallUtil.isDatabaseSchemaExist(connectionUrl, username, password);
        } catch (Exception e) {
            log("Could not determine database existence", e);
            return false;
        }
    }

    @Override
    public String testConnection(String connectionUrl, String username, String password) throws Exception {
        String results = ServerInstallUtil.testConnection(connectionUrl, username, password);
        return results;
    }

    @Override
    public HashMap<String, String> getServerProperties() throws Exception {
        File serverPropertiesFile = getServerPropertiesFile();
        PropertiesFileUpdate propsFile = new PropertiesFileUpdate(serverPropertiesFile.getAbsolutePath());
        Properties props = propsFile.loadExistingProperties();

        // force some hardcoded defaults for IBM JVMs that must have specific values
        boolean isIBM = System.getProperty("java.vendor", "").contains("IBM");
        if (isIBM) {
            for (String algPropName : ServerProperties.IBM_ALGOROTHM_SETTINGS) {
                props.setProperty(algPropName, "IbmX509");
            }
        }

        // GWT can't handle Properties - convert to HashMap
        HashMap<String, String> map = new HashMap<String, String>(props.size());
        for (Object property : props.keySet()) {
            map.put(property.toString(), props.getProperty(property.toString()));
        }
        return map;
    }

    /**
     * Save the given properties to the server's .properties file.
     *
     * Note that this is private - it is not exposed to the installer UI. It should have no need to save
     * this data outside of the normal installation process (see {@link #install()}).
     * 
     * @param serverProperties the server properties to save
     * 
     * @throws Exception if failed to save the properties to the .properties file
     */
    private void saveServerProperties(HashMap<String, String> serverProperties) throws Exception {
        File serverPropertiesFile = getServerPropertiesFile();
        PropertiesFileUpdate propsFile = new PropertiesFileUpdate(serverPropertiesFile.getAbsolutePath());

        // GWT can't handle Properties - so we use HashMap but convert to Properties internally
        Properties props = new Properties();
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
        JBossASClient client = new JBossASClient(getClient());
        String version = client.getStringAttribute("release-version", Address.root());
        return version;
    }

    @Override
    public String getOperatingSystem() throws Exception {
        JBossASClient client = new JBossASClient(getClient());
        String[] address = { "core-service", "platform-mbean", "type", "operating-system" };
        String osName = client.getStringAttribute("name", Address.root().add(address));
        return osName;
    }

    private String getAppServerHomeDir() throws Exception {
        JBossASClient client = new JBossASClient(getClient());
        String[] address = { "core-service", "server-environment" };
        String dir = client.getStringAttribute(true, "home-dir", Address.root().add(address));
        return dir;
    }

    private String getAppServerConfigDir() throws Exception {
        JBossASClient client = new JBossASClient(getClient());
        String[] address = { "core-service", "server-environment" };
        String dir = client.getStringAttribute(true, "config-dir", Address.root().add(address));
        return dir;
    }

    private String getLogDir() throws Exception {
        File asHomeDir = new File(getAppServerHomeDir());
        File logDir = new File(asHomeDir, "../logs"); // this is RHQ's log dir, not JBossAS's log dir
        logDir.mkdirs(); // create it in case it doesn't yet exist
        return logDir.getAbsolutePath();
    }

    private File getServerPropertiesFile() throws Exception {
        File appServerHomeDir = new File(getAppServerHomeDir());
        File serverPropertiesFile = new File(appServerHomeDir, "../bin/rhq-server.properties");
        return serverPropertiesFile;
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
    public ServerDetails getServerDetailsFromPropertiesOnly(HashMap<String, String> serverProperties) throws Exception {

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

    private ModelControllerClient getClient() {
        ModelControllerClient client = ManagementService.getClient();
        return client;
    }

    private void createDatasourceSecurityDomain(String username, String password) throws Exception {
        final SecurityDomainJBossASClient client = new SecurityDomainJBossASClient(getClient());
        final String securityDomain = RHQ_SECURITY_DOMAIN;
        if (!client.isSecurityDomain(securityDomain)) {
            client.createNewSecureIdentitySecurityDomainRequest(securityDomain, username, password);
            log("Security domain [" + securityDomain + "] created");
        } else {
            log("Security domain [" + securityDomain + "] already exists, skipping the creation request");
        }
    }
}
