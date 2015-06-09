/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

import static org.rhq.modules.plugins.jbossas7.util.SecurityUtil.loadKeystore;

import java.io.File;
import java.security.KeyStore;
import java.security.KeyStoreException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
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
        public static final String SECURE = "secure";
        public static final String NATIVE_HOST = "nativeHost";
        public static final String NATIVE_PORT = "nativePort";
        public static final String NATIVE_LOCAL_AUTH = "nativeLocalAuth";
        public static final String USER = "user";
        public static final String PASSWORD = "password";
        public static final String MANAGEMENT_CONNECTION_TIMEOUT = "managementConnectionTimeout";
        public static final String HOME_DIR = "homeDir";
        public static final String BASE_DIR = "baseDir";
        public static final String CONFIG_DIR = "configDir";
        public static final String LOG_DIR = "logDir";
        public static final String PRODUCT_TYPE = "productType";
        public static final String HOST_CONFIG_FILE = "hostConfigFile";
        @Deprecated
        public static final String AVAIL_CHECK_PERIOD_CONFIG_PROP = "availabilityCheckPeriod";
        public static final String TRUST_STRATEGY = "trustStrategy";
        public static final String HOSTNAME_VERIFICATION = "hostnameVerification";
        public static final String TRUSTSTORE_TYPE = "truststoreType";
        public static final String TRUSTSTORE = "truststore";
        public static final String TRUSTSTORE_PASSWORD = "truststorePassword";
        public static final String CLIENTCERT_AUTHENTICATION = "clientcertAuthentication";
        public static final String KEYSTORE_TYPE = "keystoreType";
        public static final String KEYSTORE = "keystore";
        public static final String KEYSTORE_PASSWORD = "keystorePassword";
        public static final String KEY_PASSWORD = "keyPassword";
    }

    private final Configuration pluginConfig;

    public ServerPluginConfiguration(Configuration pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    public Configuration getPluginConfig() {
        return pluginConfig;
    }

    /**
     * returns detected path based on given path name 
     * @see <a href="https://docs.jboss.org/author/display/AS7/Admin+Guide#AdminGuide-Paths">https://docs.jboss.org/author/display/AS7/Admin+Guide#AdminGuide-Paths</a>
     * @param pathName - is path name defined in AS7 config xml file
     * @return File representing absolute path, return null if given pathName is not known
     */
    public File getPath(String pathName) {
        if ("jboss.home.dir".equals(pathName)) {
            return getHomeDir();
        }
        if ("jboss.server.base.dir".equals(pathName) || "jboss.domain.base.dir".equals(pathName)) {
            return getBaseDir();
        }
        if ("jboss.server.config.dir".equals(pathName) || "jboss.domain.config.dir".equals(pathName)) {
            return getConfigDir();
        }
        if ("jboss.server.log.dir".equals(pathName) || "jboss.domain.log.dir".equals(pathName)) {
            return getLogDir();
        }
        return null;
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

    public boolean isSecure() {
        String stringValue = this.pluginConfig.getSimpleValue(Property.SECURE);
        return stringValue != null && Boolean.parseBoolean(stringValue);
    }

    public void setSecure(boolean secure) {
        this.pluginConfig.setSimpleValue(Property.SECURE, String.valueOf(secure));
    }

    public Integer getNativePort() {
        String stringValue = this.pluginConfig.getSimpleValue(Property.NATIVE_PORT);
        return (stringValue != null) ? Integer.valueOf(stringValue) : null;
    }

    public void setNativeHost(String host) {
        this.pluginConfig.setSimpleValue(Property.NATIVE_HOST, host);
    }

    public String getNativeHost() {
        return this.pluginConfig.getSimpleValue(Property.NATIVE_HOST);

    }

    public boolean isNativeLocalAuth() {
        String stringValue = this.pluginConfig.getSimpleValue(Property.NATIVE_LOCAL_AUTH);
        return stringValue != null && Boolean.parseBoolean(stringValue);
    }

    public void setNativeLocalAuth(boolean nativeLocalAuth) {
        this.pluginConfig.setSimpleValue(Property.NATIVE_LOCAL_AUTH, String.valueOf(nativeLocalAuth));
    }

    public void setNativePort(int port) {
        this.pluginConfig.setSimpleValue(Property.NATIVE_PORT, String.valueOf(port));
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
        this.pluginConfig.setSimpleValue(Property.HOST_CONFIG_FILE,
            (hostConfigFile != null) ? hostConfigFile.toString() : null);
    }

    @Deprecated
    public Integer getAvailabilityCheckPeriod() {
        return 0;
    }

    @Deprecated
    public void setAvailabilityCheckPeriod(Integer availabilityCheckPeriod) {
    }

    public TrustStrategy getTrustStrategy() {
        return TrustStrategy.findByName(pluginConfig.getSimpleValue(Property.TRUST_STRATEGY, TrustStrategy.STANDARD.name));
    }

    public HostnameVerification getHostnameVerification() {
        return HostnameVerification.findByName(pluginConfig.getSimpleValue(Property.HOSTNAME_VERIFICATION, HostnameVerification.STRICT.name));
    }

    public String getTruststoreType() {
        return this.pluginConfig.getSimpleValue(Property.TRUSTSTORE_TYPE);
    }

    public String getTruststore() {
        return this.pluginConfig.getSimpleValue(Property.TRUSTSTORE);
    }

    public String getTruststorePassword() {
        return this.pluginConfig.getSimpleValue(Property.TRUSTSTORE_PASSWORD);
    }

    public boolean isClientcertAuthentication() {
        String stringValue = this.pluginConfig.getSimpleValue(Property.CLIENTCERT_AUTHENTICATION);
        return stringValue != null && Boolean.parseBoolean(stringValue);
    }

    public String getKeystoreType() {
        return this.pluginConfig.getSimpleValue(Property.KEYSTORE_TYPE);
    }

    public String getKeystore() {
        return this.pluginConfig.getSimpleValue(Property.KEYSTORE);
    }

    public String getKeystorePassword() {
        return this.pluginConfig.getSimpleValue(Property.KEYSTORE_PASSWORD);
    }

    public String getKeyPassword() {
        return this.pluginConfig.getSimpleValue(Property.KEY_PASSWORD);
    }

    /**
     * Checks server resource connection settings
     *
     * @throws InvalidPluginConfigurationException if settings are incorrect
     */
    public void validate() {
        if (getPort() == null || getPort() <= 0) {
            throw new InvalidPluginConfigurationException(
                "Unable to detect management port. Please enable management HTTP interface on and then set correct port number in Connection Settings of this resource");
        }
        if (isSecure()) {
            String truststore = getTruststore();
            if (truststore != null) {
                if (!new File(truststore).isFile()) {
                    throw new InvalidPluginConfigurationException("Truststore file does not exist");
                }
                String truststoreType = getTruststoreType();
                if (truststoreType == null) {
                    throw new InvalidPluginConfigurationException(
                        "Truststore type is required when using a custom truststore file");
                }
                try {
                    KeyStore.getInstance(truststoreType);
                } catch (KeyStoreException e) {
                    throw new InvalidPluginConfigurationException("Truststore type not supported: " + e.getMessage());
                }
                try {
                    loadKeystore(truststoreType, truststore, getTruststorePassword());
                } catch (Exception e) {
                    throw new InvalidPluginConfigurationException("Cannot read the truststore: " + e.getMessage());
                }
            }
            if (isClientcertAuthentication()) {
                String keystore = getKeystore();
                if (keystore == null) {
                    throw new InvalidPluginConfigurationException(
                        "Keystore is required when using client certificate authentication");
                }
                if (!new File(keystore).isFile()) {
                    throw new InvalidPluginConfigurationException("Keystore file does not exist");
                }
                String keystoreType = getKeystoreType();
                if (keystoreType == null) {
                    throw new InvalidPluginConfigurationException(
                        "Keystore type is required when using a custom keystore file");
                }
                try {
                    KeyStore.getInstance(keystoreType);
                } catch (KeyStoreException e) {
                    throw new InvalidPluginConfigurationException("Keystore type not supported: " + e.getMessage());
                }
                try {
                    loadKeystore(keystoreType, keystore, getKeystorePassword());
                } catch (Exception e) {
                    throw new InvalidPluginConfigurationException("Cannot read the keystore: " + e.getMessage());
                }
            }
        }
    }
}
