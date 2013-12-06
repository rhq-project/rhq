/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7.helper;

import java.io.File;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.modules.plugins.jbossas7.JBossProductType;

/**
 * A wrapper for the plugin configuration of an AS7 Server Resource, which provides strongly typed accessors for each of
 * the configuration properties.
 *
 * @author Ian Springer
 */
public class ServerPluginConfiguration {

    public abstract class Property {
        public static final String HOSTNAME = "hostname";
        public static final String PORT = "port";
        public static final String USER = "user";
        public static final String PASSWORD = "password";
        public static final String MANAGEMENT_CONNECTION_TIMEOUT = "managementConnectionTimeout";
        public static final String HOME_DIR = "homeDir";
        public static final String BASE_DIR = "baseDir";
        public static final String CONFIG_DIR = "configDir";
        public static final String LOG_DIR = "logDir";
        public static final String PRODUCT_TYPE = "productType";
        public static final String HOST_CONFIG_FILE = "hostConfigFile";
    }

    private Configuration pluginConfig;

    public ServerPluginConfiguration(Configuration pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    public String getHostname() {
        return this.pluginConfig.getSimpleValue(Property.HOSTNAME);
    }

    public void setHostname(String hostname) {
        this.pluginConfig.setSimpleValue(Property.HOSTNAME, hostname);
    }

    public Integer getPort() {
        String stringValue = this.pluginConfig.getSimpleValue(Property.PORT);
        return (stringValue != null) ? Integer.valueOf(stringValue) : null;
    }

    public void setPort(int port) {
        this.pluginConfig.setSimpleValue(Property.PORT, String.valueOf(port));
    }

    public String getUser() {
        return this.pluginConfig.getSimpleValue(Property.USER);
    }

    public void setUser(String user) {
        this.pluginConfig.setSimpleValue(Property.USER, user);
    }

    public String getPassword() {
        return this.pluginConfig.getSimpleValue(Property.PASSWORD);
    }

    public void setPassword(String password) {
        this.pluginConfig.setSimpleValue(Property.PASSWORD, password);
    }

    public Long getManagementConnectionTimeout() {
        return this.pluginConfig.getSimple(Property.MANAGEMENT_CONNECTION_TIMEOUT).getLongValue();
    }

    public File getHomeDir() {
        String stringValue = this.pluginConfig.getSimpleValue(Property.HOME_DIR);
        return (stringValue != null && !stringValue.isEmpty()) ? new File(stringValue) : null;
    }

    public void setHomeDir(File homeDir) {
        this.pluginConfig.setSimpleValue(Property.HOME_DIR, (homeDir != null) ? homeDir.toString() : null);
    }

    public File getBaseDir() {
        String stringValue = this.pluginConfig.getSimpleValue(Property.BASE_DIR);
        return (stringValue != null && !stringValue.isEmpty()) ? new File(stringValue) : null;
    }

    public void setBaseDir(File homeDir) {
        this.pluginConfig.setSimpleValue(Property.BASE_DIR, (homeDir != null) ? homeDir.toString() : null);
    }

    public File getConfigDir() {
        String stringValue = this.pluginConfig.getSimpleValue(Property.CONFIG_DIR);
        return (stringValue != null && !stringValue.isEmpty()) ? new File(stringValue) : null;
    }

    public void setConfigDir(File configDir) {
        this.pluginConfig.setSimpleValue(Property.CONFIG_DIR, (configDir != null) ? configDir.toString() : null);
    }

    public File getLogDir() {
        String stringValue = this.pluginConfig.getSimpleValue(Property.LOG_DIR);
        return (stringValue != null && !stringValue.isEmpty()) ? new File(stringValue) : null;
    }

    public void setLogDir(File logDir) {
        this.pluginConfig.setSimpleValue(Property.LOG_DIR, (logDir != null) ? logDir.toString() : null);
    }

    public JBossProductType getProductType() {
        String stringValue = this.pluginConfig.getSimpleValue(Property.PRODUCT_TYPE);
        return (stringValue != null && !stringValue.isEmpty()) ? JBossProductType.valueOf(stringValue) : null;
    }

    public void setProductType(JBossProductType productType) {
        this.pluginConfig.setSimpleValue(Property.PRODUCT_TYPE, (productType != null) ? productType.name() : null);
    }

    public File getHostConfigFile() {
        String stringValue = this.pluginConfig.getSimpleValue(Property.HOST_CONFIG_FILE);
        return (stringValue != null && !stringValue.isEmpty()) ? new File(stringValue) : null;
    }

    public void setHostConfigFile(File hostConfigFile) {
        this.pluginConfig.setSimpleValue(Property.HOST_CONFIG_FILE, (hostConfigFile != null) ?
                hostConfigFile.toString() : null);
    }

}
