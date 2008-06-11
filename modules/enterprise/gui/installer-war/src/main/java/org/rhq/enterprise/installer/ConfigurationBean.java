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

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.faces.context.FacesContext;

import mazz.i18n.Msg;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.installer.i18n.InstallerI18NResourceKeys;

public class ConfigurationBean {
    private static final Log LOG = LogFactory.getLog(ConfigurationBean.class);

    private ServerInformation serverInfo;
    private Boolean showAdvancedSettings;
    private List<PropertyItemWithValue> configuration;
    private String lastError;
    private String lastTest;
    private String lastCreate;
    private String existingSchemaAnswer;
    private String adminConnectionUrl;
    private String adminUsername;
    private String adminPassword;
    private Msg i18nMsg;

    public ConfigurationBean() {
        serverInfo = new ServerInformation();
        showAdvancedSettings = Boolean.FALSE;
    }

    /**
     * Loads in the server's current configuration and returns all the settings.
     *
     * @return current server settings
     */
    public List<PropertyItemWithValue> getConfiguration() {
        // load in the configuration from the properties file if needed for the first time
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

        // prepare the items to return - only return the basic settings unless showing advanced, too
        List<PropertyItemWithValue> retConfig;

        if (showAdvancedSettings.booleanValue()) {
            retConfig = new ArrayList<PropertyItemWithValue>(configuration);
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
     * database related properties.
     *
     * @return current server settings, minus database related settings
     * 
     * @see #getDatabaseConfiguration()
     * @see #getConfiguration()
     */
    public List<PropertyItemWithValue> getNonDatabaseConfiguration() {
        List<PropertyItemWithValue> allConfig = getConfiguration();
        List<PropertyItemWithValue> retConfig = new ArrayList<PropertyItemWithValue>();
        for (PropertyItemWithValue item : allConfig) {
            if (!item.getItemDefinition().getPropertyName().startsWith(ServerProperties.PREFIX_PROP_DATABASE)) {
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
            if (item.getItemDefinition().getPropertyName().startsWith(ServerProperties.PREFIX_PROP_DATABASE)) {
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
        Connection conn = null;
        try {
            conn = serverInfo.getDatabaseConnection(configurationAsProperties);
            lastTest = "OK";
        } catch (Exception e) {
            LOG.warn("Installer failed to test connection", e);
            lastTest = e.toString();
        } finally {
            if (conn != null)
                try {
                    conn.close();
                } catch (Exception e) {
                }
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
            return false;
        }
    }

    public String getExistingSchemaAnswer() {
        return this.existingSchemaAnswer;
    }

    public void setExistingSchemaAnswer(String existingSchemaAnswer) {
        this.existingSchemaAnswer = existingSchemaAnswer;
    }

    public StartPageResults save() {
        Properties configurationAsProperties = getConfigurationAsProperties(configuration);

        testConnection(); // so our lastTest gets set and the user will be able to get the error in the UI
        if (lastTest == null || !lastTest.equals("OK")) {
            lastError = lastTest;
            return StartPageResults.ERROR;
        }

        for (PropertyItemWithValue newValue : configuration) {
            if (Integer.class.isAssignableFrom(newValue.getItemDefinition().getPropertyType())) {
                try {
                    Integer.parseInt(newValue.getValue());
                } catch (Exception e) {
                    lastError = getI18nMsg().getMsg(InstallerI18NResourceKeys.INVALID_NUMBER,
                        newValue.getItemDefinition().getPropertyLabel(), newValue.getValue());
                    return StartPageResults.ERROR;
                }
            } else if (Boolean.class.isAssignableFrom(newValue.getItemDefinition().getPropertyType())) {
                try {
                    if (newValue.getValue() == null) {
                        newValue.setValue(Boolean.FALSE.toString());
                    }

                    Boolean.parseBoolean(newValue.getValue());
                } catch (Exception e) {
                    lastError = getI18nMsg().getMsg(InstallerI18NResourceKeys.INVALID_BOOLEAN,
                        newValue.getItemDefinition().getPropertyLabel(), newValue.getValue());
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
                if (existingSchemaAnswer == null)
                    return StartPageResults.STAY; // user didn't tell us what to do, re-display the page with the question

                if (existingSchemaAnswer.equals("overwrite")) {
                    serverInfo.createNewDatabaseSchema(configurationAsProperties);
                    // clean out existing JMS messages
                    serverInfo.cleanJmsTables(configurationAsProperties);
                } else
                    serverInfo.upgradeExistingDatabaseSchema(configurationAsProperties);
            } else {
                serverInfo.createNewDatabaseSchema(configurationAsProperties);
            }

            // now deploy RHQ Server fully
            serverInfo.moveDeploymentArtifacts(true);
        } catch (Exception e) {
            LOG.fatal("Failed to save properties and fully deploy - RHQ Server will not function properly", e);
            lastError = getI18nMsg().getMsg(InstallerI18NResourceKeys.SAVE_FAILURE, ThrowableUtil.getAllMessages(e));

            return StartPageResults.ERROR;
        }

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

    private Msg getI18nMsg() {
        if (i18nMsg == null) {
            i18nMsg = new Msg(InstallerI18NResourceKeys.BUNDLE_BASE_NAME, getLocale());
        }

        return i18nMsg;
    }

    private Locale getLocale() {
        return FacesContext.getCurrentInstance().getViewRoot().getLocale();
    }
}