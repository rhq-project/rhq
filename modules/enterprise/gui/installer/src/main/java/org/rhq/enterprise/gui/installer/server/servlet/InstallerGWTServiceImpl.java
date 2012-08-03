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
    public void install(HashMap<String, String> serverProperties) throws Exception {
        // its possible the JDBC URL was changed, clear the factory cache in case the DB version is different now
        DatabaseTypeFactory.clearDatabaseTypeCache();

        // determine the type of database to connect to
        String databaseType = serverProperties.get(ServerProperties.PROP_DATABASE_TYPE);
        if (databaseType == null || databaseType.length() == 0) {
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
        String url = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
        String user = serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME);
        String pw = serverProperties.get(ServerProperties.PROP_DATABASE_PASSWORD);
        if (ServerInstallUtil.isAutoinstallEnabled(serverProperties)) {
            pw = ServerInstallUtil.deobfuscatePassword(pw);
        }
        String testConnectionErrorMessage = testConnection(url, user, pw);
        if (testConnectionErrorMessage != null) {
            throw new Exception("Cannot connect to the database: " + testConnectionErrorMessage);
        }

        // TODO: CONTINUE WITH CONFIGURATION BEAN LINE 712
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
            return ServerInstallUtil.getServerDetails(connectionUrl, username, password, serverName);
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

    private File getServerPropertiesFile() throws Exception {
        File appServerHomeDir = new File(getAppServerHomeDir());
        File serverPropertiesFile = new File(appServerHomeDir, "../bin/rhq-server.properties");
        return serverPropertiesFile;
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
