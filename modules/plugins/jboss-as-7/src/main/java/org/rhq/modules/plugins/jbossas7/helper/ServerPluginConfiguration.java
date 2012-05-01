package org.rhq.modules.plugins.jbossas7.helper;

import java.io.File;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.modules.plugins.jbossas7.JBossProductType;

/**
 * A wrapper for the plugin configuration of an AS7 Server Resource, which provides strongly typed accessors for each of
 * the configuration properties.
 */
public class ServerPluginConfiguration {

    public abstract class Property {
        public static final String HOSTNAME = "hostname";
        public static final String PORT = "port";
        public static final String USER = "user";
        public static final String PASSWORD = "password";
        public static final String HOME_DIR = "homeDir";
        public static final String BASE_DIR = "baseDir";
        public static final String CONFIG_DIR = "configDir";
        public static final String LOG_DIR = "logDir";
        public static final String PRODUCT_TYPE = "productType";
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

}
