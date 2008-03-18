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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.faces.context.FacesContext;

import mazz.i18n.Msg;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.installer.i18n.InstallerI18NResourceKeys;

public class ConfigurationBean {
    private static final Log LOG = LogFactory.getLog(ConfigurationBean.class);

    private ServerInformation serverInfo;
    private Boolean showAdvancedSettings;
    private List<PropertyItemWithValue> configuration;
    private String lastError;
    private String lastTest;
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
        return lastError;
    }

    public String getLastTest() {
        return lastTest;
    }

    public String testConnection() {
        Properties configurationAsProperties = getConfigurationAsProperties(configuration);
        Connection conn = null;
        try {
            conn = serverInfo.getDatabaseConnection(configurationAsProperties);
            lastTest = "OK";
        } catch (SQLException sqle) {
            LOG.warn("Installer failed to test connection", sqle);
            lastTest = sqle.toString();
        } finally {
            if (conn != null)
                try {
                    conn.close();
                } catch (Exception e) {
                }
        }

        return "TEST_DONE";
    }

    public SavePropertiesResults save() {
        Properties configurationAsProperties = getConfigurationAsProperties(configuration);

        testConnection(); // so our lastTest gets set and the user will be able to get the error in the UI
        if (lastTest == null || !lastTest.equals("OK")) {
            return SavePropertiesResults.DBINVALID;
        }

        for (PropertyItemWithValue newValue : configuration) {
            if (Integer.class.isAssignableFrom(newValue.getItemDefinition().getPropertyType())) {
                try {
                    Integer.parseInt(newValue.getValue());
                } catch (Exception e) {
                    lastError = getI18nMsg().getMsg(InstallerI18NResourceKeys.INVALID_NUMBER,
                        newValue.getItemDefinition().getPropertyLabel(), newValue.getValue());
                    return SavePropertiesResults.ERROR;
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
                    return SavePropertiesResults.ERROR;
                }
            }
        }

        try {
            // indicate that no errors occurred
            lastError = null;

            // save the properties
            serverInfo.setServerProperties(configurationAsProperties);

            // create the db schema or upgrade it if it exists
            if (serverInfo.isDatabaseSchemaExist(configurationAsProperties)) {
                serverInfo.upgradeExistingDatabaseSchema(configurationAsProperties);
            } else {
                serverInfo.createNewDatabaseSchema(configurationAsProperties);
            }

            // now deploy RHQ Server fully
            serverInfo.moveDeploymentArtifacts(true);
        } catch (Exception e) {
            LOG.fatal("Failed to save properties and fully deploy - RHQ Server will not function properly", e);
            lastError = getI18nMsg().getMsg(InstallerI18NResourceKeys.SAVE_FAILURE, e);

            return SavePropertiesResults.ERROR;
        }

        return SavePropertiesResults.SUCCESS;
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