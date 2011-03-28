/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.installer;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import mazz.i18n.Msg;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.resource.security.SecureIdentityLoginModule;

import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.SQLServerDatabaseType;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.installer.i18n.InstallerI18NResourceKeys;

/**
 * Responsible for taking the settings the user selects in the installer window and saves them
 * as the server's initial configuration.
 *
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 */
public class ConfigurationBean {
    private static final Log LOG = LogFactory.getLog(ConfigurationBean.class);

    private enum ExistingSchemaOption {
        OVERWRITE, KEEP, SKIP
    };

    private Msg I18Nmsg;
    private List<SelectItem> existingSchemaOptions;
    private ServerInformation serverInfo;
    private Boolean showAdvancedSettings;
    private List<PropertyItemWithValue> configuration;
    private String lastError;
    private String lastTest;
    private String lastCreate;
    private String existingSchemaOption;
    private String adminConnectionUrl;
    private String adminUsername;
    private String adminPassword;
    private ServerInformation.Server haServer;
    private String haServerName;
    private String selectedRegisteredServerName;

    public ConfigurationBean() {
        FacesContext currentInstance = FacesContext.getCurrentInstance();
        if (currentInstance != null) {
            I18Nmsg = new Msg(InstallerI18NResourceKeys.BUNDLE_BASE_NAME, currentInstance.getViewRoot().getLocale());
        } else {
            I18Nmsg = new Msg(InstallerI18NResourceKeys.BUNDLE_BASE_NAME, Locale.getDefault());
        }
        serverInfo = new ServerInformation();
        showAdvancedSettings = Boolean.FALSE;
        existingSchemaOption = ExistingSchemaOption.KEEP.name();
        // this will initialize 'configuration'
        initConfiguration();

        if (!isAutoinstallEnabled()) {
            setHaServerName(getDefaultServerName());
        }
    }

    private String getDefaultServerName() {
        String defaultServerName = "";

        try {
            defaultServerName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (Exception e) {
            LOG.info("Could not determine default server name: ", e);
        }

        return defaultServerName;
    }

    /**
     * Loads in the server's current configuration and returns all the settings.
     * This only returns the property if it is a basic setting or the UI is showing advanced, too.
     *
     * @return the requested server setting
     */
    public PropertyItemWithValue getConfigurationProperty(String propertyName) {
        return getConfigurationProperty(getConfiguration(), propertyName);
    }

    /**
     * Loads in the server's current configuration and returns all the settings.
     * This returns any property, basic or advanced, no matter what the "show advanced" setting is.
     *
     * @return the requested server setting
     */
    private PropertyItemWithValue getConfigurationPropertyFromAll(String propertyName) {
        return getConfigurationProperty(this.configuration, propertyName);
    }

    private PropertyItemWithValue getConfigurationProperty(List<PropertyItemWithValue> config, String propertyName) {
        PropertyItemWithValue result = null;

        for (PropertyItemWithValue item : config) {
            if (item.getItemDefinition().getPropertyName().equalsIgnoreCase(propertyName)) {
                result = item;
                break;
            }
        }

        return result;
    }

    public void initConfiguration() {
        if (configuration == null) {
            Properties properties = serverInfo.getServerProperties();
            List<PropertyItem> itemDefs = new ServerProperties().getPropertyItems();
            configuration = new ArrayList<PropertyItemWithValue>();

            for (PropertyItem itemDef : itemDefs) {
                String property = properties.getProperty(itemDef.getPropertyName());
                PropertyItemWithValue value = new PropertyItemWithValue(itemDef, property);
                configuration.add(value);
            }

            // force some hardcoded defaults for IBM JVMs that must have specific values
            boolean isIBM = System.getProperty("java.vendor", "").contains("IBM");
            if (isIBM) {
                String[] algPropNames = { ServerProperties.PROP_TOMCAT_SECURITY_ALGORITHM,
                    ServerProperties.PROP_SECURITY_CLIENT_KEYSTORE_ALGORITHM,
                    ServerProperties.PROP_SECURITY_CLIENT_TRUSTSTORE_ALGORITHM,
                    ServerProperties.PROP_SECURITY_SERVER_KEYSTORE_ALGORITHM,
                    ServerProperties.PROP_SECURITY_SERVER_TRUSTSTORE_ALGORITHM };

                for (String algPropName : algPropNames) {
                    PropertyItemWithValue prop = getConfigurationProperty(this.configuration, algPropName);
                    if (prop != null) {
                        prop.setValue("IbmX509");
                    }
                }
            }
        }

        return;
    }

    /**
     * Loads in the server's current configuration and returns all the settings.
     *
     * @return current server settings
     */
    public List<PropertyItemWithValue> getConfiguration() {
        // prepare the items to return - only return the basic settings unless showing advanced, too
        List<PropertyItemWithValue> retConfig;

        if (showAdvancedSettings.booleanValue()) {
            retConfig = configuration;
        } else {
            retConfig = new ArrayList<PropertyItemWithValue>();
            for (PropertyItemWithValue item : configuration) {
                if (!item.getItemDefinition().isAdvanced()) {
                    retConfig.add(item);
                }
            }
        }

        return retConfig;
    }

    /**
     * Loads in the server's current configuration and returns all the settings except
     * database related properties. This additionally strips out hidden properties too,
     * so the caller will not see any of the hidden properties in the returned list.
     *
     * @return current server settings, minus database related settings and hidden settings.
     *
     * @see #getDatabaseConfiguration()
     * @see #getConfiguration()
     */
    public List<PropertyItemWithValue> getNonDatabaseConfiguration() {
        List<PropertyItemWithValue> allConfig = getConfiguration();
        List<PropertyItemWithValue> retConfig = new ArrayList<PropertyItemWithValue>();

        for (PropertyItemWithValue item : allConfig) {
            if (!(item.getItemDefinition().getPropertyName().startsWith(ServerProperties.PREFIX_PROP_DATABASE) || item
                .getItemDefinition().isHidden())) {

                retConfig.add(item);
            }
        }

        return retConfig;
    }

    /**
     * Loads in the server's current configuration and returns only the database related properties.
     *
     * @return current database settings
     *
     * @see #getNonDatabaseConfiguration()
     * @see #getConfiguration()
     */
    public List<PropertyItemWithValue> getDatabaseConfiguration() {
        List<PropertyItemWithValue> allConfig = getConfiguration();
        List<PropertyItemWithValue> retConfig = new ArrayList<PropertyItemWithValue>();

        for (PropertyItemWithValue item : allConfig) {
            if (item.getItemDefinition().getPropertyName().startsWith(ServerProperties.PREFIX_PROP_DATABASE)
                && !item.getItemDefinition().isHidden()) {
                retConfig.add(item);
            }
        }

        return retConfig;
    }

    /**
     * Checks to see if the server has been preconfigured and should be auto-installed. If <code>true</code>
     * is returned, the installer webapp should not be needed to install the server and the installer should
     * immediately begin the installation process.
     *
     * @return <code>true</code> if auto-install should occur; <code>false</code> means the user needs to use
     *         the installer GUI before the installation can begin
     */
    public boolean isAutoinstallEnabled() {
        PropertyItemWithValue enableProp = getConfigurationPropertyFromAll(ServerProperties.PROP_AUTOINSTALL_ENABLE);
        if (enableProp != null) {
            return Boolean.parseBoolean(enableProp.getValue());
        }
        return false;
    }

    public void setConfiguration(List<PropertyItemWithValue> newConfig) {
        PropertyItemWithValue oldValue;

        // we can't just store the entire thing because newConfig may be missing items (if not viewing advanced)
        // just loop through the new config and overwrite the old values with the new values
        for (PropertyItemWithValue newValue : newConfig) {
            oldValue = findPropertyItemWithValue(newValue.getItemDefinition().getPropertyName());
            oldValue.setValue(newValue.getValue());
        }

        return;
    }

    public Boolean isShowAdvancedSettings() {
        return showAdvancedSettings;
    }

    public Boolean getShowAdvancedSettings() {
        return showAdvancedSettings;
    }

    public void setShowAdvancedSettings(Boolean showAdvancedSettings) {
        if (showAdvancedSettings == null) {
            showAdvancedSettings = Boolean.FALSE;
        }

        this.showAdvancedSettings = showAdvancedSettings;
    }

    public String getLastError() {
        return (lastError != null) ? lastError.replaceAll("'", "\\\\'") : null;
    }

    public String getLastTest() {
        return (lastTest != null) ? lastTest.replaceAll("'", "\\\\'") : null;
    }

    public String getLastCreate() {
        return (lastCreate != null) ? lastCreate.replaceAll("'", "\\\\'") : null;
    }

    public String getAdminConnectionUrl() {
        if (adminConnectionUrl == null) {
            adminConnectionUrl = getConfigurationAsProperties(configuration).getProperty(
                ServerProperties.PROP_DATABASE_CONNECTION_URL);
        }
        return adminConnectionUrl;
    }

    public void setAdminConnectionUrl(String adminUrl) {
        this.adminConnectionUrl = adminUrl;
    }

    public String getAdminUsername() {
        if (adminUsername == null) {
            Properties config = getConfigurationAsProperties(configuration);
            String dbtype = config.getProperty(ServerProperties.PROP_DATABASE_TYPE);
            if (dbtype != null) {
                if (dbtype.toLowerCase().indexOf("oracle") > -1) {
                    adminUsername = "sys";
                } else if (dbtype.toLowerCase().indexOf("postgres") > -1) {
                    adminUsername = "postgres";
                } else if (dbtype.toLowerCase().indexOf("h2") > -1) {
                    adminUsername = "sa";
                } else if (dbtype.toLowerCase().indexOf("sqlserver") > -1) {
                    adminUsername = "sa";
                } else if (dbtype.toLowerCase().indexOf("mysql") > -1) {
                    adminUsername = "mysqladmin";
                }
            }
        }

        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public StartPageResults testConnection() {
        Properties configurationAsProperties = getConfigurationAsProperties(configuration);

        // its possible the JDBC URL was changed, clear the factory cache in case the DB version is different now
        DatabaseTypeFactory.clearDatabaseTypeCache();

        try {
            serverInfo.ensureDatabaseIsSupported(configurationAsProperties);
            lastTest = "OK";
        } catch (Exception e) {
            LOG.warn("Installer failed to test connection", e);
            lastTest = e.toString();
        }

        return StartPageResults.STAY;
    }

    public StartPageResults showCreateDatabasePage() {
        adminConnectionUrl = null;
        adminUsername = null;
        adminPassword = null;
        return StartPageResults.CREATEDB;
    }

    public StartPageResults createDatabase() {
        Properties config = getConfigurationAsProperties(configuration);
        String dbType = config.getProperty(ServerProperties.PROP_DATABASE_TYPE, "-unknown-");

        Properties adminConfig = new Properties();
        adminConfig.put(ServerProperties.PROP_DATABASE_CONNECTION_URL, adminConnectionUrl);
        adminConfig.put(ServerProperties.PROP_DATABASE_USERNAME, adminUsername);
        adminConfig.put(ServerProperties.PROP_DATABASE_PASSWORD, adminPassword);

        Connection conn = null;
        Statement stmt = null;

        // If we successfully add the user/database, we'll change the values in the UI
        // by modifying the configuration property items that this bean manages.
        PropertyItemWithValue propertyItemUsername = null;
        PropertyItemWithValue propertyItemPassword = null;
        PropertyItemWithValue propertyItemUrl = null;

        for (PropertyItemWithValue item : configuration) {
            String propName = item.getItemDefinition().getPropertyName();
            if (propName.equals(ServerProperties.PROP_DATABASE_USERNAME)) {
                propertyItemUsername = item;
            } else if (propName.equals(ServerProperties.PROP_DATABASE_PASSWORD)) {
                propertyItemPassword = item;
            } else if (propName.equals(ServerProperties.PROP_DATABASE_CONNECTION_URL)) {
                propertyItemUrl = item;
            }
        }

        if (propertyItemUsername == null || propertyItemPassword == null || propertyItemUrl == null) {
            throw new NullPointerException("Missing a property item - this is a bug please report it");
        }

        LOG.info("Will attempt to create user/database 'rhqadmin' using URL [" + getAdminConnectionUrl()
            + "] and admin user [" + getAdminUsername() + "]. Admin password was"
            + (getAdminPassword().length() > 0 ? " not " : " ") + "empty");

        try {
            String sql1, sql2;

            conn = serverInfo.getDatabaseConnection(adminConfig);
            conn.setAutoCommit(true);
            stmt = conn.createStatement();

            if (dbType.equalsIgnoreCase("postgresql")) {
                sql1 = "CREATE ROLE rhqadmin LOGIN ENCRYPTED PASSWORD 'rhqadmin' NOSUPERUSER NOINHERIT CREATEDB NOCREATEROLE";
                sql2 = "CREATE DATABASE rhq WITH OWNER = rhqadmin ENCODING = 'SQL_ASCII' TABLESPACE = pg_default";
            } else if (dbType.equalsIgnoreCase("oracle10g")) {
                sql1 = "CREATE USER rhqadmin IDENTIFIED BY rhqadmin";
                sql2 = "GRANT connect, resource TO rhqadmin";
            } else if (dbType.equalsIgnoreCase("h2")) {
                // I have no idea if these are correct for H2 - I just copied oracle's sql
                sql1 = "CREATE USER rhqadmin IDENTIFIED BY rhqadmin";
                sql2 = "GRANT connect, resource TO rhqadmin";
            } else if (dbType.equalsIgnoreCase("sqlserver")) {
                // I have no idea if these are correct for sql server - I just copied oracle's sql
                sql1 = "CREATE USER rhqadmin IDENTIFIED BY rhqadmin";
                sql2 = "GRANT connect, resource TO rhqadmin";
            } else {
                throw new Exception("Unknown database type: " + dbType);
            }

            stmt.addBatch(sql1);
            stmt.addBatch(sql2);
            int[] results = stmt.executeBatch();

            if (results[0] == Statement.EXECUTE_FAILED)
                throw new Exception("Failed to execute: " + sql1);
            if (results[1] == Statement.EXECUTE_FAILED)
                throw new Exception("Failed to execute: " + sql2);

            // success! let's set our properties to the values we just created
            propertyItemUsername.setValue("rhqadmin");
            propertyItemPassword.setValue("rhqadmin");
            if (dbType.equalsIgnoreCase("postgresql") || dbType.equalsIgnoreCase("mysql")) {
                if (!propertyItemUrl.getValue().endsWith("/rhq")) {
                    propertyItemUrl.setValue(propertyItemUrl.getValue() + "/rhq");
                }
            }

            testConnection();

            lastCreate = "OK";
        } catch (Exception e) {
            LOG.warn("Installer failed to create database", e);
            lastCreate = ThrowableUtil.getAllMessages(e);
        } finally {
            adminConnectionUrl = null;
            adminUsername = null;
            adminPassword = null;

            if (stmt != null)
                try {
                    stmt.close();
                } catch (Exception e) {
                }
            if (conn != null)
                try {
                    conn.close();
                } catch (Exception e) {
                }
        }

        return StartPageResults.STAY;
    }

    public String getDataDirectory() {
        try {
            String path = this.serverInfo.getDataDirectory().getCanonicalPath();
            path = path.replace('\\', '/'); // in case we are on windows, we still want forward slashes
            return path;
        } catch (Exception e) {
            throw new RuntimeException(e); // this should never happen unless the file system is out of wack
        }
    }

    public boolean isDatabaseSchemaExist() {
        try {
            Properties configurationAsProperties = getConfigurationAsProperties(configuration);
            return serverInfo.isDatabaseSchemaExist(configurationAsProperties);
        } catch (Exception e) {
            LOG.info("Could not determine database existence: " + e);
            return false;
        }
    }

    public String getExistingSchemaOption() {
        return this.existingSchemaOption;
    }

    public void setExistingSchemaOption(String existingSchemaOption) {
        // this is allowed to be null to support auto-installer
        this.existingSchemaOption = existingSchemaOption;
    }

    public boolean isKeepExistingSchema() {
        return ExistingSchemaOption.KEEP.name().equals(existingSchemaOption)
            || ExistingSchemaOption.SKIP.name().equals(existingSchemaOption);
    }

    public StartPageResults saveEmbeddedMode() {
        // embedded mode simply means use our embedded database and the embedded agent - used mainly for demo purposes

        // set embedded agent enabled to true
        PropertyItemWithValue prop = getConfigurationPropertyFromAll(ServerProperties.PROP_EMBEDDED_AGENT_ENABLED);
        prop.setValue(Boolean.TRUE.toString());
        List<PropertyItemWithValue> newConfig = new ArrayList<PropertyItemWithValue>(1);
        newConfig.add(prop);
        setConfiguration(newConfig);

        // use the H2 database, which is our embedded DB
        List<PropertyItemWithValue> dbConfig = getDatabaseConfiguration();
        for (PropertyItemWithValue dbProp : dbConfig) {
            if (dbProp.getItemDefinition().getPropertyName().equals(ServerProperties.PROP_DATABASE_CONNECTION_URL)) {
                dbProp.setValue("jdbc:h2:" + getDataDirectory() + "/rhq;MVCC=TRUE;DB_CLOSE_ON_EXIT=FALSE;LOG=2");
            } else if (dbProp.getItemDefinition().getPropertyName().equals(ServerProperties.PROP_DATABASE_TYPE)) {
                dbProp.setValue("H2");
            } else if (dbProp.getItemDefinition().getPropertyName().equals(ServerProperties.PROP_DATABASE_DRIVER_CLASS)) {
                dbProp.setValue("org.h2.Driver");
            } else if (dbProp.getItemDefinition().getPropertyName().equals(ServerProperties.PROP_DATABASE_XA_DS_CLASS)) {
                dbProp.setValue("org.h2.jdbcx.JdbcDataSource");
            } else if (dbProp.getItemDefinition().getPropertyName().equals(ServerProperties.PROP_DATABASE_USERNAME)) {
                dbProp.setValue("rhqadmin");
            } else if (dbProp.getItemDefinition().getPropertyName().equals(ServerProperties.PROP_DATABASE_PASSWORD)) {
                dbProp.setValue("rhqadmin");
            }
        }
        setExistingSchemaOption(ExistingSchemaOption.OVERWRITE.name());

        return save();
    }

    public StartPageResults save() {
        LOG.info("Installer raw values: " + configuration);

        // if auto-install is enabled, the db password will be encrypted - decrypt it internally and we'll re-encrypt later
        if (isAutoinstallEnabled()) {
            try {
                PropertyItemWithValue prop = getConfigurationPropertyFromAll(ServerProperties.PROP_DATABASE_PASSWORD);
                String pass = prop.getValue();
                pass = decodePassword(pass);
                prop.setValue(pass);
                // log the unencrypted pw, but only at the trace level so it isn't put in the log file
                // unless someone explicitly enables the trace level so they can see the pass that is to be used for debugging
                LOG.trace(">" + pass);
            } catch (Exception e) {
                LOG.fatal("Could not decrypt the password for some reason - auto-installation failed", e);
                lastError = I18Nmsg.getMsg(InstallerI18NResourceKeys.SAVE_ERROR, ThrowableUtil.getAllMessages(e));
                return StartPageResults.ERROR;
            }
        }

        // its possible the JDBC URL was changed, clear the factory cache in case the DB version is different now
        DatabaseTypeFactory.clearDatabaseTypeCache();

        try {
            // update server properties with the latest ha info to keep the form and server properties file up to date
            getConfigurationPropertyFromAll(ServerProperties.PROP_HIGH_AVAILABILITY_NAME).setValue(
                getHaServer().getName());
            getConfigurationPropertyFromAll(ServerProperties.PROP_HTTP_PORT).setValue(
                getHaServer().getEndpointPortString());
            getConfigurationPropertyFromAll(ServerProperties.PROP_HTTPS_PORT).setValue(
                getHaServer().getEndpointSecurePortString());

            // the comm bind port is a special setting - it is allowed to be blank;
            // if it was originally blank, it will have been set to 0 because its an integer property;
            // but we do not want it to be 0, so make sure we switch it back to empty
            PropertyItemWithValue portConfig = getConfigurationPropertyFromAll(ServerProperties.PROP_CONNECTOR_BIND_PORT);
            if ("0".equals(portConfig.getValue())) {
                portConfig.setRawValue("");
            }
        } catch (Exception e) {
            LOG.fatal("Could not save the settings for some reason", e);
            lastError = I18Nmsg.getMsg(InstallerI18NResourceKeys.SAVE_ERROR, ThrowableUtil.getAllMessages(e));

            return StartPageResults.ERROR;
        }

        try {
            String url = getConfigurationPropertyFromAll(ServerProperties.PROP_DATABASE_CONNECTION_URL).getValue();
            String db = getConfigurationPropertyFromAll(ServerProperties.PROP_DATABASE_TYPE).getValue();
            Pattern pattern = null;
            if (db.toLowerCase().indexOf("postgres") > -1) {
                pattern = Pattern.compile(".*://(.*):([0123456789]+)/(.*)"); // jdbc:postgresql://host.name:5432/rhq
            } else if (db.toLowerCase().indexOf("oracle") > -1) {
                LOG.info("Oracle does not need to have server-name, port and db-name individually set, skipping");
                // if we ever find that we'll need these props set, uncomment below and it should all work
                //pattern = Pattern.compile(".*@(.*):([0123456789]+):(.*)"); // jdbc:oracle:thin:@host.name:1521:rhq
            } else if (db.toLowerCase().indexOf("h2") > -1) {
                LOG.info("H2 does not need to have server-name, port and db-name individually set, skipping");
            } else if (db.toLowerCase().indexOf("sqlserver") > -1) {
                pattern = Pattern.compile("(?i).*://(.*):([0123456789]+).*databaseName=([^;]*)"); // jdbc:jtds:sqlserver://localhost:7777;databaseName=rhq
            } else {
                LOG.info("Unknown database type - will not set server-name, port and db-name");
                // don't bother throwing error; these three extra settings may not be necessary anyway
            }
            if (pattern != null) {
                Matcher match = pattern.matcher(url);
                if (match.find() && (match.groupCount() == 3)) {
                    String serverName = match.group(1);
                    String port = match.group(2);
                    String dbName = match.group(3);
                    getConfigurationPropertyFromAll(ServerProperties.PROP_DATABASE_SERVER_NAME).setValue(serverName);
                    getConfigurationPropertyFromAll(ServerProperties.PROP_DATABASE_PORT).setValue(port);
                    getConfigurationPropertyFromAll(ServerProperties.PROP_DATABASE_DB_NAME).setValue(dbName);
                } else {
                    throw new Exception("Cannot get server, port or db name from connection URL: " + url);
                }
            } else {
                getConfigurationPropertyFromAll(ServerProperties.PROP_DATABASE_SERVER_NAME).setValue("");
                getConfigurationPropertyFromAll(ServerProperties.PROP_DATABASE_PORT).setValue("");
                getConfigurationPropertyFromAll(ServerProperties.PROP_DATABASE_DB_NAME).setValue("");
            }
        } catch (Exception e) {
            LOG.fatal("JDBC connection URL seems to be invalid", e);
            lastError = I18Nmsg.getMsg(InstallerI18NResourceKeys.SAVE_ERROR, ThrowableUtil.getAllMessages(e));

            return StartPageResults.ERROR;
        }

        try {
            String db = getConfigurationPropertyFromAll(ServerProperties.PROP_DATABASE_TYPE).getValue();
            String dialect;
            String quartzDriverDelegateClass = "org.quartz.impl.jdbcjobstore.StdJDBCDelegate";
            String quartzSelectWithLockSQL = "SELECT * FROM {0}LOCKS ROWLOCK WHERE LOCK_NAME = ? FOR UPDATE";
            String quartzLockHandlerClass = "org.quartz.impl.jdbcjobstore.StdRowLockSemaphore";

            if (db.toLowerCase().indexOf("postgres") > -1) {
                dialect = "org.hibernate.dialect.PostgreSQLDialect";
                quartzDriverDelegateClass = "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate";
            } else if (db.toLowerCase().indexOf("oracle") > -1) {
                dialect = "org.hibernate.dialect.Oracle10gDialect";
                quartzDriverDelegateClass = "org.quartz.impl.jdbcjobstore.oracle.OracleDelegate";
            } else if (db.toLowerCase().indexOf("h2") > -1) {
                dialect = "org.rhq.core.server.H2CustomDialect";
            } else if (db.toLowerCase().indexOf("sqlserver") > -1) {
                dialect = "org.hibernate.dialect.SQLServerDialect";
                quartzDriverDelegateClass = "org.quartz.impl.jdbcjobstore.MSSQLDelegate";
                quartzSelectWithLockSQL = "UPDATE {0}LOCKS SET LOCK_NAME = LOCK_NAME WHERE LOCK_NAME = ?";
                quartzLockHandlerClass = "org.quartz.impl.jdbcjobstore.UpdateLockRowSemaphore";
            } else if (db.toLowerCase().indexOf("mysql") > -1) {
                dialect = "org.hibernate.dialect.MySQL5InnoDBDialect";
                quartzDriverDelegateClass = "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate";
            } else {
                throw new Exception("Unknown db type: " + db);
            }

            getConfigurationPropertyFromAll(ServerProperties.PROP_DATABASE_HIBERNATE_DIALECT).setValue(dialect);
            getConfigurationPropertyFromAll(ServerProperties.PROP_QUARTZ_DRIVER_DELEGATE_CLASS).setValue(
                quartzDriverDelegateClass);
            getConfigurationPropertyFromAll(ServerProperties.PROP_QUARTZ_SELECT_WITH_LOCK_SQL).setValue(
                quartzSelectWithLockSQL);
            getConfigurationPropertyFromAll(ServerProperties.PROP_QUARTZ_LOCK_HANDLER_CLASS).setValue(
                quartzLockHandlerClass);

        } catch (Exception e) {
            LOG.fatal("Invalid database type", e);
            lastError = I18Nmsg.getMsg(InstallerI18NResourceKeys.SAVE_ERROR, ThrowableUtil.getAllMessages(e));

            return StartPageResults.ERROR;
        }

        Properties configurationAsProperties = getConfigurationAsProperties(configuration);
        testConnection(); // so our lastTest gets set and the user will be able to get the error in the UI
        if (lastTest == null || !lastTest.equals("OK")) {
            lastError = lastTest;
            return StartPageResults.ERROR;
        }

        // Ensure server info has been set
        if ((null == haServer) || (null == haServer.getName()) || "".equals(haServer.getName().trim())) {
            lastError = I18Nmsg.getMsg(InstallerI18NResourceKeys.INVALID_STRING, I18Nmsg
                .getMsg(InstallerI18NResourceKeys.PROP_HIGH_AVAILABILITY_NAME));
            return StartPageResults.ERROR;
        }

        for (PropertyItemWithValue newValue : configuration) {
            if (Integer.class.isAssignableFrom(newValue.getItemDefinition().getPropertyType())) {
                try {
                    Integer.parseInt(newValue.getValue());
                } catch (Exception e) {
                    // there is one special property - the connector bind port - that is allowed to be empty
                    // ignore this error if we are looking at that property and its empty; otherwise, this is an error
                    if (!(newValue.getItemDefinition().getPropertyName().equals(
                        ServerProperties.PROP_CONNECTOR_BIND_PORT) && newValue.getValue().length() == 0)) {
                        lastError = I18Nmsg.getMsg(InstallerI18NResourceKeys.INVALID_NUMBER, newValue
                            .getItemDefinition().getPropertyLabel(), newValue.getValue());
                        return StartPageResults.ERROR;
                    }
                }
            } else if (Boolean.class.isAssignableFrom(newValue.getItemDefinition().getPropertyType())) {
                try {
                    if (newValue.getValue() == null) {
                        newValue.setValue(Boolean.FALSE.toString());
                    }

                    Boolean.parseBoolean(newValue.getValue());
                } catch (Exception e) {
                    lastError = I18Nmsg.getMsg(InstallerI18NResourceKeys.INVALID_BOOLEAN, newValue.getItemDefinition()
                        .getPropertyLabel(), newValue.getValue());
                    return StartPageResults.ERROR;
                }
            }
        }

        try {
            // indicate that no errors occurred
            lastError = null;

            // save the properties
            serverInfo.setServerProperties(configurationAsProperties);

            // prepare the db schema
            if (!ExistingSchemaOption.SKIP.name().equals(existingSchemaOption)) {
                if (serverInfo.isDatabaseSchemaExist(configurationAsProperties)) {
                    if (existingSchemaOption == null) {
                        if (!isAutoinstallEnabled()) {
                            return StartPageResults.STAY; // user didn't tell us what to do, re-display the page with the question
                        }
                        // we are supposed to auto-install but wasn't explicitly told what to do - the default is "auto"
                        // and since we know the database schema exists, that means we upgrade it
                    }

                    if (ExistingSchemaOption.OVERWRITE.name().equals(existingSchemaOption)) {
                        serverInfo.createNewDatabaseSchema(configurationAsProperties);
                    } else {
                        serverInfo.upgradeExistingDatabaseSchema(configurationAsProperties);
                    }
                } else {
                    serverInfo.createNewDatabaseSchema(configurationAsProperties);
                }
            }

            // Ensure the install server info is up to date and stored in the DB
            serverInfo.storeServer(configurationAsProperties, haServer);

            // encode database password and set updated properties
            String pass = configurationAsProperties.getProperty(ServerProperties.PROP_DATABASE_PASSWORD);
            pass = encryptPassword(pass);
            configurationAsProperties.setProperty(ServerProperties.PROP_DATABASE_PASSWORD, pass);

            serverInfo.setServerProperties(configurationAsProperties);

            // We have changed the password of the database connection, so we need to
            // tell the login config about it
            serverInfo.restartLoginConfig();

            // build a keystore whose cert has a CN of this server's public endpoint address
            serverInfo.createKeystore(haServer);

            // now deploy RHQ Server fully
            serverInfo.moveDeploymentArtifacts(true);
        } catch (Exception e) {
            LOG.fatal("Failed to updated properties and fully deploy - RHQ Server will not function properly", e);
            lastError = I18Nmsg.getMsg(InstallerI18NResourceKeys.SAVE_FAILURE, ThrowableUtil.getAllMessages(e));

            return StartPageResults.ERROR;
        }

        LOG.info("Installer: final submitted values: " + configurationAsProperties);

        return StartPageResults.SUCCESS;
    }

    private String encryptPassword(String password) throws Exception {

        // We need to do some mumbo jumbo, as the interesting method is private
        // in SecureIdentityLoginModule

        try {
            SecureIdentityLoginModule lm = new SecureIdentityLoginModule();
            Class<?> clazz = SecureIdentityLoginModule.class;
            Method m = clazz.getDeclaredMethod("encode", String.class);
            m.setAccessible(true);
            String res = (String) m.invoke(lm, password);
            return res;
        } catch (Exception e) {
            throw new Exception("Encoding db password failed: ", e);
        }
    }

    private String decodePassword(String encrypedPassword) throws Exception {

        // We need to do some mumbo jumbo, as the interesting method is private
        // in SecureIdentityLoginModule

        try {
            SecureIdentityLoginModule lm = new SecureIdentityLoginModule();
            Class<?> clazz = SecureIdentityLoginModule.class;
            Method m = clazz.getDeclaredMethod("decode", String.class);
            m.setAccessible(true);
            char[] res = (char[]) m.invoke(lm, encrypedPassword);
            return new String(res);
        } catch (Exception e) {
            throw new Exception("Decoding db password failed: ", e);
        }
    }

    private Properties getConfigurationAsProperties(List<PropertyItemWithValue> config) {
        Properties props = new Properties();

        for (PropertyItemWithValue itemWithValue : config) {
            props.setProperty(itemWithValue.getItemDefinition().getPropertyName(), itemWithValue.getValue());
        }

        return props;
    }

    private PropertyItemWithValue findPropertyItemWithValue(String propertyName) {
        for (PropertyItemWithValue value : configuration) {
            if (value.getItemDefinition().getPropertyName().equals(propertyName)) {
                return value;
            }
        }

        return null;
    }

    public List<SelectItem> getExistingSchemaOptions() {
        if (existingSchemaOptions == null) {
            existingSchemaOptions = new ArrayList<SelectItem>();
            existingSchemaOptions.add(new SelectItem(ExistingSchemaOption.OVERWRITE.name(), I18Nmsg
                .getMsg(InstallerI18NResourceKeys.EXISTING_SCHEMA_OPTION_OVERWRITE)));
            existingSchemaOptions.add(new SelectItem(ExistingSchemaOption.KEEP.name(), I18Nmsg
                .getMsg(InstallerI18NResourceKeys.EXISTING_SCHEMA_OPTION_KEEP)));
            existingSchemaOptions.add(new SelectItem(ExistingSchemaOption.SKIP.name(), I18Nmsg
                .getMsg(InstallerI18NResourceKeys.EXISTING_SCHEMA_OPTION_SKIP)));
        }
        return existingSchemaOptions;
    }

    /** To set the server name use setServerName() */
    public PropertyItemWithValue getPropHaServerName() {
        return getConfigurationPropertyFromAll(ServerProperties.PROP_HIGH_AVAILABILITY_NAME);
    }

    public PropertyItemWithValue getPropHaEndpointPort() {
        return getConfigurationPropertyFromAll(ServerProperties.PROP_HTTP_PORT);
    }

    public PropertyItemWithValue getPropHaEndpointSecurePort() {
        return getConfigurationPropertyFromAll(ServerProperties.PROP_HTTPS_PORT);
    }

    public boolean isRegisteredServers() {

        if (!this.isKeepExistingSchema())
            return false;

        List<SelectItem> registeredServerNames = getRegisteredServerNames();

        return ((null != registeredServerNames) && !registeredServerNames.isEmpty());
    }

    public List<SelectItem> getRegisteredServerNames() {
        List<SelectItem> result = new ArrayList<SelectItem>(0);

        if (!isDatabaseSchemaExist())
            return result;

        try {
            Properties configurationAsProperties = getConfigurationAsProperties(configuration);
            for (String serverName : serverInfo.getServerNames(configurationAsProperties)) {
                result.add(new SelectItem(serverName));
            }
            if (!result.isEmpty()) {
                result.add(0, new SelectItem(I18Nmsg.getMsg(InstallerI18NResourceKeys.NEW_SERVER_SELECT_ITEM)));
            }
        } catch (Exception e) {
            // Should not be able to get here since we checked for schema above
            LOG.warn("Unexpected Exception getting registered server info: ", e);
        }

        return result;
    }

    public String getSelectedRegisteredServerName() {
        return selectedRegisteredServerName;
    }

    // If an existing server name is selected from the list set haServerName to the selection. Note, this function
    // should not call getServerConfiguration.setValue()
    public void setSelectedRegisteredServerName(String selectedRegisteredServerName) {
        this.selectedRegisteredServerName = selectedRegisteredServerName;
    }

    public String getHaServerName() {
        return haServerName;
    }

    public void setHaServerName(String serverName) {
        // handle the case where the user selected the dummy entry in the registered servers drop down
        if (I18Nmsg.getMsg(InstallerI18NResourceKeys.NEW_SERVER_SELECT_ITEM).equals(serverName)) {
            serverName = this.getDefaultServerName();
        }

        this.haServerName = serverName;

        // try pulling info from the database for this server name
        if (isRegisteredServers()) {
            Properties configurationAsProperties = getConfigurationAsProperties(configuration);
            setHaServer(serverInfo.getServerDetail(configurationAsProperties, serverName));
        }

        // if the server was not registered in the database then populate the ha server info with proper defaults
        if (null == getHaServer()) {
            String endpointAddress = "";

            try {
                endpointAddress = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (Exception e) {
                LOG.info("Could not determine default server address: ", e);
            }

            setHaServer(new ServerInformation.Server(serverName, endpointAddress,
                ServerInformation.Server.DEFAULT_ENDPOINT_PORT, ServerInformation.Server.DEFAULT_ENDPOINT_SECURE_PORT,
                ServerInformation.Server.DEFAULT_AFFINITY_GROUP));

            // override default settings with current property values
            try {
                getHaServer().setEndpointPortString(
                    getConfigurationPropertyFromAll(ServerProperties.PROP_HTTP_PORT).getValue());
            } catch (Exception e) {
                LOG.debug("Could not determine default port: ", e);
            }

            try {
                getHaServer().setEndpointSecurePortString(
                    getConfigurationPropertyFromAll(ServerProperties.PROP_HTTPS_PORT).getValue());
            } catch (Exception e) {
                LOG.debug("Could not determine default secure port: ", e);
            }
        } else {
            getHaServer().setName(serverName);
        }
    }

    /**
     * This method will set the HA Server information based solely on the server configuration
     * properties. It does not rely on any database access.
     *
     * This is used by the auto-installation process - see {@link AutoInstallServlet}.
     *
     * @throws Exception
     */
    public void setHaServerFromPropertiesOnly() throws Exception {

        PropertyItemWithValue preconfigDb = getConfigurationPropertyFromAll(ServerProperties.PROP_AUTOINSTALL_DB);
        if (preconfigDb != null && preconfigDb.getValue() != null) {
            if ("overwrite".equals(preconfigDb.getValue().toLowerCase())) {
                setExistingSchemaOption(ExistingSchemaOption.OVERWRITE.name());
            } else if ("skip".equals(preconfigDb.getValue().toLowerCase())) {
                setExistingSchemaOption(ExistingSchemaOption.SKIP.name());
            } else if ("auto".equals(preconfigDb.getValue().toLowerCase())) {
                setExistingSchemaOption(null);
            } else {
                LOG.warn("An invalid setting for [" + ServerProperties.PROP_AUTOINSTALL_DB + "] was provided: ["
                    + preconfigDb.getValue()
                    + "]. Valid values are 'auto', 'overwrite' or 'skip'. Defaulting to 'auto'");
                preconfigDb.setRawValue("auto");
                setExistingSchemaOption(null);
            }
        } else {
            LOG.debug("[" + ServerProperties.PROP_AUTOINSTALL_DB + "] was not provided. Defaulting to 'auto'");
            setExistingSchemaOption(null);
        }

        // create a ha server stub with some defaults - we'll fill this in based on our property settings
        ServerInformation.Server preconfiguredHaServer = new ServerInformation.Server("", "",
            ServerInformation.Server.DEFAULT_ENDPOINT_PORT, ServerInformation.Server.DEFAULT_ENDPOINT_SECURE_PORT,
            ServerInformation.Server.DEFAULT_AFFINITY_GROUP);

        // determine the name of the server - its either preconfigured as the high availability name, or
        // we auto-determine it by using the machine's canonical hostname
        PropertyItemWithValue haNameProp = getPropHaServerName();
        if (haNameProp != null) {
            preconfiguredHaServer.setName(haNameProp.getValue()); // this leaves it alone if value is null or empty
        }
        if (preconfiguredHaServer.getName().equals("")) {
            String serverName = getDefaultServerName(); // gets hostname
            if (serverName == null || "".equals(serverName)) {
                throw new Exception("Server name is not preconfigured and could not be determined automatically");
            }
            preconfiguredHaServer.setName(serverName);
        }

        // the public endpoint address is one that can be preconfigured in the special autoinstall property.
        // if that is not specified, then we use either the connector's bind address or the server bind address.
        // if nothing was specified, we'll default to the canonical host name.
        String publicEndpointAddress;

        PropertyItemWithValue preconfigAddr = getConfigurationPropertyFromAll(ServerProperties.PROP_AUTOINSTALL_PUBLIC_ENDPOINT);
        if (preconfigAddr != null && preconfigAddr.getValue() != null && !"".equals(preconfigAddr.getValue().trim())) {
            // its preconfigured using the autoinstall setting, use that and ignore the other settings
            publicEndpointAddress = preconfigAddr.getValue().trim();
        } else {
            PropertyItemWithValue connBindAddress = getConfigurationPropertyFromAll(ServerProperties.PROP_CONNECTOR_BIND_ADDRESS);
            if (connBindAddress != null && connBindAddress.getValue() != null
                && !"".equals(connBindAddress.getValue().trim())
                && !"0.0.0.0".equals(connBindAddress.getValue().trim())) {
                // the server-side connector bind address is explicitly set, use that
                publicEndpointAddress = connBindAddress.getValue().trim();
            } else {
                PropertyItemWithValue serverBindAddress = getConfigurationPropertyFromAll(ServerProperties.PROP_SERVER_BIND_ADDRESS);
                if (serverBindAddress != null && serverBindAddress.getValue() != null
                    && !"".equals(serverBindAddress.getValue().trim())
                    && !"0.0.0.0".equals(serverBindAddress.getValue().trim())) {
                    // the main JBossAS server bind address is set and it isn't 0.0.0.0, use that
                    publicEndpointAddress = serverBindAddress.getValue().trim();
                } else {
                    publicEndpointAddress = InetAddress.getLocalHost().getCanonicalHostName();
                }
            }
        }
        preconfiguredHaServer.setEndpointAddress(publicEndpointAddress);

        // define the public endpoint ports.
        // note that if using a different transport other than (ssl)servlet, we'll
        // take the connector's bind port and use it for both ports. This is to support a special deployment
        // use-case - 99% of the time, the agents will go through the web/tomcat connector and thus we'll use
        // the http/https ports for the public endpoints.
        PropertyItemWithValue connectorTransport = getConfigurationPropertyFromAll(ServerProperties.PROP_CONNECTOR_TRANSPORT);
        if (connectorTransport != null && connectorTransport.getValue() != null
            && connectorTransport.getValue().contains("socket")) {

            // we aren't using the (ssl)servlet protocol, take the connector bind port and use it for the public endpoint ports
            PropertyItemWithValue connectorBindPort = getConfigurationPropertyFromAll(ServerProperties.PROP_CONNECTOR_BIND_PORT);
            if (connectorBindPort == null || connectorBindPort.getValue() == null
                || "".equals(connectorBindPort.getValue().trim()) || "0".equals(connectorBindPort.getValue().trim())) {
                throw new Exception("Using non-servlet transport [" + connectorTransport + "] but didn't define a port");
            }
            preconfiguredHaServer.setEndpointPort(Integer.parseInt(connectorBindPort.getValue()));
            preconfiguredHaServer.setEndpointSecurePort(Integer.parseInt(connectorBindPort.getValue()));
        } else {
            // this is the typical use-case - the transport is probably (ssl)servlet so use the web http/https ports
            try {
                PropertyItemWithValue httpPort = getPropHaEndpointPort();
                preconfiguredHaServer.setEndpointPortString(httpPort.getValue());
            } catch (Exception e) {
                LOG.warn("Could not determine port, will use default: " + e);
            }

            try {
                PropertyItemWithValue httpsPort = getPropHaEndpointSecurePort();
                preconfiguredHaServer.setEndpointSecurePortString(httpsPort.getValue());
            } catch (Exception e) {
                LOG.warn("Could not determine secure port, will use default: " + e);
            }
        }

        // everything looks good - remember these
        setHaServer(preconfiguredHaServer);
        this.haServerName = preconfiguredHaServer.getName();
    }

    public ServerInformation.Server getHaServer() {
        return haServer;
    }

    public void setHaServer(ServerInformation.Server haServer) {
        this.haServer = haServer;
    }

}