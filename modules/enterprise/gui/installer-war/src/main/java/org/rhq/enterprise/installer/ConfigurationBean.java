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

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import mazz.i18n.Msg;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.installer.i18n.InstallerI18NResourceKeys;

/**
 * 
 * @author Jay Shaughnessy
 */
public class ConfigurationBean {
    private static final Log LOG = LogFactory.getLog(ConfigurationBean.class);

    private static final Msg I18NMSG = new Msg(InstallerI18NResourceKeys.BUNDLE_BASE_NAME, FacesContext
        .getCurrentInstance().getViewRoot().getLocale());

    private enum ExistingSchemaOption {
        OVERWRITE, KEEP
    };

    public static final List<SelectItem> EXISTING_SCHEMA_OPTIONS;

    static {
        EXISTING_SCHEMA_OPTIONS = new ArrayList<SelectItem>();
        EXISTING_SCHEMA_OPTIONS.add(new SelectItem(ExistingSchemaOption.OVERWRITE.name(), I18NMSG
            .getMsg(InstallerI18NResourceKeys.EXISTING_SCHEMA_OPTION_OVERWRITE)));
        EXISTING_SCHEMA_OPTIONS.add(new SelectItem(ExistingSchemaOption.KEEP.name(), I18NMSG
            .getMsg(InstallerI18NResourceKeys.EXISTING_SCHEMA_OPTION_KEEP)));
    }

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
        serverInfo = new ServerInformation();
        showAdvancedSettings = Boolean.FALSE;
        existingSchemaOption = ExistingSchemaOption.KEEP.name();
        // this will initialize 'configuration'
        initConfiguration();
        setHaServerName(getDefaultServerName());
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
     *
     * @return the requested server setting
     */
    public PropertyItemWithValue getConfigurationProperty(String propertyName) {
        return getConfigurationProperty(getConfiguration(), propertyName);
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
        }
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
        this.existingSchemaOption = existingSchemaOption;
    }

    public boolean isKeepExistingSchema() {
        return ExistingSchemaOption.KEEP.name().equals(existingSchemaOption);
    }

    public StartPageResults save() {
        LOG.info("Installer raw values: " + configuration);

        // its possible the JDBC URL was changed, clear the factory cache in case the DB version is different now
        DatabaseTypeFactory.clearDatabaseTypeCache();

        try {
            // update server properties with the latest ha info to keep the form and server properties file up to date
            getConfigurationProperty(configuration, ServerProperties.PROP_HIGH_AVAILABILITY_NAME).setValue(
                getHaServer().getName());
            getConfigurationProperty(configuration, ServerProperties.PROP_HTTP_PORT).setValue(
                getHaServer().getEndpointPortString());
            getConfigurationProperty(configuration, ServerProperties.PROP_HTTPS_PORT).setValue(
                getHaServer().getEndpointSecurePortString());

            // the comm bind port is a special setting - it is allowed to be blank;
            // if it was originally blank, it will have been set to 0 because its an integer property;
            // but we do not want it to be 0, so make sure we switch it back to empty
            PropertyItemWithValue portConfig = getConfigurationProperty(configuration,
                ServerProperties.PROP_CONNECTOR_BIND_PORT);
            if ("0".equals(portConfig.getValue())) {
                portConfig.setRawValue("");
            }
        } catch (Exception e) {
            LOG.fatal("Could not save the settings for some reason", e);
            lastError = I18NMSG.getMsg(InstallerI18NResourceKeys.SAVE_ERROR, ThrowableUtil.getAllMessages(e));

            return StartPageResults.ERROR;
        }

        try {
            String url = getConfigurationProperty(configuration, ServerProperties.PROP_DATABASE_CONNECTION_URL)
                .getValue();
            String db = getConfigurationProperty(configuration, ServerProperties.PROP_DATABASE_TYPE).getValue();
            Pattern pattern = null;
            if (db.toLowerCase().indexOf("postgres") > -1) {
                pattern = Pattern.compile(".*://(.*):([0123456789]+)/(.*)"); // jdbc:postgresql://host.name:5432/rhq
            } else if (db.toLowerCase().indexOf("oracle") > -1) {
                LOG.info("Oracle does not need to have server-name, port and db-name individually set, skipping");
                // if we ever find that we'll need these props set, uncomment below and it should all work
                //pattern = Pattern.compile(".*@(.*):([0123456789]+):(.*)"); // jdbc:oracle:thin:@host.name:1521:rhq
            } else {
                LOG.info("Unknown database type - will not set server-name, port and db-name");
                // don't bother throwing error; these three extra settings are only for postgres anyway
            }
            if (pattern != null) {
                Matcher match = pattern.matcher(url);
                if (match.find() && (match.groupCount() == 3)) {
                    String serverName = match.group(1);
                    String port = match.group(2);
                    String dbName = match.group(3);
                    getConfigurationProperty(configuration, ServerProperties.PROP_DATABASE_SERVER_NAME).setValue(
                        serverName);
                    getConfigurationProperty(configuration, ServerProperties.PROP_DATABASE_PORT).setValue(port);
                    getConfigurationProperty(configuration, ServerProperties.PROP_DATABASE_DB_NAME).setValue(dbName);
                } else {
                    throw new Exception("Cannot get server, port or db name from connection URL: " + url);
                }
            } else {
                getConfigurationProperty(configuration, ServerProperties.PROP_DATABASE_SERVER_NAME).setValue("");
                getConfigurationProperty(configuration, ServerProperties.PROP_DATABASE_PORT).setValue("");
                getConfigurationProperty(configuration, ServerProperties.PROP_DATABASE_DB_NAME).setValue("");
            }
        } catch (Exception e) {
            LOG.fatal("JDBC connection URL seems to be invalid", e);
            lastError = I18NMSG.getMsg(InstallerI18NResourceKeys.SAVE_ERROR, ThrowableUtil.getAllMessages(e));

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
            lastError = I18NMSG.getMsg(InstallerI18NResourceKeys.INVALID_STRING, I18NMSG
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
                        lastError = I18NMSG.getMsg(InstallerI18NResourceKeys.INVALID_NUMBER, newValue
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
                    lastError = I18NMSG.getMsg(InstallerI18NResourceKeys.INVALID_BOOLEAN, newValue.getItemDefinition()
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
            if (serverInfo.isDatabaseSchemaExist(configurationAsProperties)) {
                if (existingSchemaOption == null)
                    return StartPageResults.STAY; // user didn't tell us what to do, re-display the page with the question

                if (existingSchemaOption.equals(ExistingSchemaOption.OVERWRITE.name())) {
                    serverInfo.createNewDatabaseSchema(configurationAsProperties);
                    // clean out existing JMS messages
                    serverInfo.cleanJmsTables(configurationAsProperties);
                } else
                    serverInfo.upgradeExistingDatabaseSchema(configurationAsProperties);
            } else {
                serverInfo.createNewDatabaseSchema(configurationAsProperties);
            }

            // Ensure the install server info is up to date and stored in the DB
            serverInfo.storeServer(configurationAsProperties, haServer);

            // now deploy RHQ Server fully
            serverInfo.moveDeploymentArtifacts(true);
        } catch (Exception e) {
            LOG.fatal("Failed to updated properties and fully deploy - RHQ Server will not function properly", e);
            lastError = I18NMSG.getMsg(InstallerI18NResourceKeys.SAVE_FAILURE, ThrowableUtil.getAllMessages(e));

            return StartPageResults.ERROR;
        }

        LOG.info("Installer: final submitted values: " + configurationAsProperties);

        return StartPageResults.SUCCESS;
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
        return EXISTING_SCHEMA_OPTIONS;
    }

    /** To set the server name use setServerName() */
    public PropertyItemWithValue getPropHaServerName() {
        return getConfigurationProperty(ServerProperties.PROP_HIGH_AVAILABILITY_NAME);
    }

    public PropertyItemWithValue getPropHaEndpointPort() {
        return getConfigurationProperty(ServerProperties.PROP_HTTP_PORT);
    }

    public PropertyItemWithValue getPropHaEndpointSecurePort() {
        return getConfigurationProperty(ServerProperties.PROP_HTTPS_PORT);
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
                result.add(0, new SelectItem(I18NMSG.getMsg(InstallerI18NResourceKeys.NEW_SERVER_SELECT_ITEM)));
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
        if (I18NMSG.getMsg(InstallerI18NResourceKeys.NEW_SERVER_SELECT_ITEM).equals(serverName)) {
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
                    getConfigurationProperty(ServerProperties.PROP_HTTP_PORT).getValue());
            } catch (Exception e) {
                LOG.debug("Could not determine default port: ", e);
            }

            try {
                getHaServer().setEndpointSecurePortString(
                    getConfigurationProperty(ServerProperties.PROP_HTTPS_PORT).getValue());
            } catch (Exception e) {
                LOG.debug("Could not determine default secure port: ", e);
            }
        } else {
            getHaServer().setName(serverName);
        }
    }

    public ServerInformation.Server getHaServer() {
        return haServer;
    }

    public void setHaServer(ServerInformation.Server haServer) {
        this.haServer = haServer;
    }

}