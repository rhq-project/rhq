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
package org.rhq.common.jbossas.client.controller;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * Convenience methods to access the web management API.
 *
 * @author John Mazzitelli
 */
public class WebJBossASClient extends JBossASClient {

    public static final String SUBSYSTEM_WEB = "web";
    public static final String VIRTUAL_SERVER = "virtual-server";
    public static final String DEFAULT_HOST = "default-host";
    public static final String CONNECTOR = "connector";
    public static final String SSL = "ssl";

    public WebJBossASClient(ModelControllerClient client) {
        super(client);
    }

    /**
     * Checks to see if the web subsystem exists. This should always exist unless
     * the server is just starting up and its web subsystem has not even initialized yet.
     *
     * @return true if the web subsystem is ready
     */
    public boolean isWebSubsystem() throws Exception {
        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_WEB);
        return null != readResource(addr);
    }

    /**
     * The enable-welcome-root setting controls whether or not to deploy JBoss' welcome-content application at root context.
     * If you want to deploy your own app at the root context, you need to disable the enable-welcome-root setting
     * on the default host virtual server. If you want to show the JBoss' welcome screen, you need to enable this setting.
     *
     * @param enableFlag true if the welcome screen at the root context should be enabled; false otherwise
     * @throws Exception 
     */
    public void setEnableWelcomeRoot(boolean enableFlag) throws Exception {
        final Address address = Address.root().add(SUBSYSTEM, SUBSYSTEM_WEB, VIRTUAL_SERVER, DEFAULT_HOST);
        final ModelNode req = createWriteAttributeRequest("enable-welcome-root", Boolean.toString(enableFlag), address);
        final ModelNode response = execute(req);
        if (!isSuccess(response)) {
            throw new FailureException(response);
        }
        return;
    }

    /**
     * Checks to see if there is already a connector with the given name.
     *
     * @param name the name to check
     * @return true if there is a connector with the given name already in existence
     */
    public boolean isConnector(String name) throws Exception {
        final Address address = Address.root().add(SUBSYSTEM, SUBSYSTEM_WEB, CONNECTOR, name);
        return null != readResource(address);
    }

    /**
     * Returns the connector node with all its attributes. Will be null if it doesn't exist.
     *
     * @param name the name of the connector whose node is to be returned
     * @return the node if there is a connector with the given name already in existence, null otherwise
     */
    public ModelNode getConnector(String name) throws Exception {
        final Address address = Address.root().add(SUBSYSTEM, SUBSYSTEM_WEB, CONNECTOR, name);
        return readResource(address, true);
    }

    /**
     * Use this to modify an attribute for an existing connector.
     * @param connectorName the existing connector whose attribute is to be changed
     * @param attribName the attribute to get a new value
     * @param attribValue the new value of the attribute
     * @throws Exception if failed to change the attribute on the named connector
     */
    public void changeConnector(String connectorName, String attribName, String attribValue) throws Exception {
        final Address address = Address.root().add(SUBSYSTEM, SUBSYSTEM_WEB, CONNECTOR, connectorName);
        final ModelNode op = createWriteAttributeRequest(attribName, attribValue, address);
        final ModelNode response = execute(op);
        if (!isSuccess(response)) {
            throw new FailureException(response);
        }
        return;
    }

    /**
     * Removes the given web connector.
     *
     * @param doomedConnectorName the name of the web connector to remove.
     * @throws Exception
     */
    public void removeConnector(String doomedConnectorName) throws Exception {
        final Address address = Address.root().add(SUBSYSTEM, SUBSYSTEM_WEB, CONNECTOR, doomedConnectorName);
        if (isConnector(doomedConnectorName)) {
            remove(address);
        }
        return;
    }

    /**
     * Add a new web connector, which may be a secure SSL connector (HTTPS) or not (HTTP).
     *
     * @param name
     * @param connectorConfig
     * @throws Exception
     */
    public void addConnector(String name, ConnectorConfiguration connectorConfig) throws Exception {
        ModelNode fullRequest;

        final Address connectorAddress = Address.root().add(SUBSYSTEM, SUBSYSTEM_WEB, CONNECTOR, name);
        final ModelNode connectorRequest = createRequest(ADD, connectorAddress);
        setPossibleExpression(connectorRequest, "executor", connectorConfig.getExecutor());
        setPossibleExpression(connectorRequest, "max-connections", connectorConfig.getMaxConnections());
        setPossibleExpression(connectorRequest, "max-post-size", connectorConfig.getMaxPostSize());
        setPossibleExpression(connectorRequest, "max-save-post-size", connectorConfig.getMaxSavePostSize());
        setPossibleExpression(connectorRequest, "protocol", connectorConfig.getProtocol());
        setPossibleExpression(connectorRequest, "proxy-name", connectorConfig.getProxyName());
        setPossibleExpression(connectorRequest, "proxy-port", connectorConfig.getProxyPort());
        setPossibleExpression(connectorRequest, "scheme", connectorConfig.getScheme());
        setPossibleExpression(connectorRequest, "socket-binding", connectorConfig.getSocketBinding());
        setPossibleExpression(connectorRequest, "redirect-port", connectorConfig.getRedirectPort());
        setPossibleExpression(connectorRequest, "enabled", String.valueOf(connectorConfig.isEnabled()));
        setPossibleExpression(connectorRequest, "enable-lookups", String.valueOf(connectorConfig.isEnableLookups()));
        setPossibleExpression(connectorRequest, "secure", String.valueOf(connectorConfig.isSecure()));

        SSLConfiguration sslConfig = connectorConfig.getSslConfiguration();
        if (sslConfig != null) {
            final Address sslAddress = connectorAddress.clone().add(SSL, "configuration"); // name MUST be "configuration" here
            final ModelNode sslRequest = createRequest(ADD, sslAddress);
            setPossibleExpression(sslRequest, "ca-certificate-file", sslConfig.getCaCertificateFile());
            setPossibleExpression(sslRequest, "ca-certificate-password", sslConfig.getCaCertificatePassword());
            setPossibleExpression(sslRequest, "ca-revocation-url", sslConfig.getCaRevocationUrl());
            setPossibleExpression(sslRequest, "certificate-file", sslConfig.getCertificateFile());
            setPossibleExpression(sslRequest, "certificate-key-file", sslConfig.getCertificateKeyFile());
            setPossibleExpression(sslRequest, "cipher-suite", sslConfig.getCipherSuite());
            setPossibleExpression(sslRequest, "key-alias", sslConfig.getKeyAlias());
            setPossibleExpression(sslRequest, "keystore-type", sslConfig.getKeystoreType());
            setPossibleExpression(sslRequest, "name", sslConfig.getName());
            setPossibleExpression(sslRequest, "password", sslConfig.getPassword());
            setPossibleExpression(sslRequest, "protocol", sslConfig.getProtocol());
            setPossibleExpression(sslRequest, "session-cache-size", sslConfig.getSessionCacheSize());
            setPossibleExpression(sslRequest, "session-timeout", sslConfig.getSessionTimeout());
            setPossibleExpression(sslRequest, "truststore-type", sslConfig.getTruststoreType());
            setPossibleExpression(sslRequest, "verify-client", sslConfig.getVerifyClient());
            setPossibleExpression(sslRequest, "verify-depth", sslConfig.getVerifyDepth());
            fullRequest = createBatchRequest(connectorRequest, sslRequest);
        } else {
            fullRequest = connectorRequest;
        }

        final ModelNode response = execute(fullRequest);
        if (!isSuccess(response)) {
            throw new FailureException(response, "Failed to add new connector [" + name + "]");
        }
        return;
    }

    public static class ConnectorConfiguration {
        private boolean enabled = true;
        private boolean enableLookups = false;
        private String executor;
        private String maxConnections;
        private String maxSavePostSize;
        private String maxPostSize;
        private String protocol = "HTTP/1.1";
        private String proxyPort;
        private String proxyName;
        private String scheme;
        private boolean secure = false;
        private String socketBinding;
        private String redirectPort;
        private SSLConfiguration sslConfiguration;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnableLookups() {
            return enableLookups;
        }

        public void setEnableLookups(boolean enableLookups) {
            this.enableLookups = enableLookups;
        }

        public String getExecutor() {
            return executor;
        }

        public void setExecutor(String executor) {
            this.executor = executor;
        }

        public String getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(String maxConnections) {
            this.maxConnections = maxConnections;
        }

        public String getMaxSavePostSize() {
            return maxSavePostSize;
        }

        public void setMaxSavePostSize(String maxSavePostSize) {
            this.maxSavePostSize = maxSavePostSize;
        }

        public String getMaxPostSize() {
            return maxPostSize;
        }

        public void setMaxPostSize(String maxPostSize) {
            this.maxPostSize = maxPostSize;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getProxyPort() {
            return proxyPort;
        }

        public void setProxyPort(String proxyPort) {
            this.proxyPort = proxyPort;
        }

        public String getProxyName() {
            return proxyName;
        }

        public void setProxyName(String proxyName) {
            this.proxyName = proxyName;
        }

        public String getScheme() {
            return scheme;
        }

        public void setScheme(String scheme) {
            this.scheme = scheme;
        }

        public boolean isSecure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        public String getSocketBinding() {
            return socketBinding;
        }

        public void setSocketBinding(String socketBinding) {
            this.socketBinding = socketBinding;
        }

        public String getRedirectPort() {
            return redirectPort;
        }

        public void setRedirectPort(String redirectPort) {
            this.redirectPort = redirectPort;
        }

        public SSLConfiguration getSslConfiguration() {
            return sslConfiguration;
        }

        public void setSslConfiguration(SSLConfiguration sslConfiguration) {
            this.sslConfiguration = sslConfiguration;
            // if we are given an SSL config, we must be secure, so set that now, too
            if (sslConfiguration != null) {
                this.secure = true;
            }
        }
    }

    public static class SSLConfiguration {
        private String caCertificateFile;
        private String caCertificatePassword;
        private String caRevocationUrl;
        private String certificateKeyFile;
        private String certificateFile;
        private String cipherSuite;
        private String keyAlias;
        private String keystoreType;
        private String name;
        private String password;
        private String protocol = "HTTP/1.1";
        private String sessionCacheSize;
        private String sessionTimeout;
        private String truststoreType;
        private String verifyClient;
        private String verifyDepth;

        public String getCaCertificateFile() {
            return caCertificateFile;
        }

        public void setCaCertificateFile(String caCertificateFile) {
            this.caCertificateFile = caCertificateFile;
        }

        public String getCaCertificatePassword() {
            return caCertificatePassword;
        }

        public void setCaCertificationPassword(String caCertificatePassword) {
            this.caCertificatePassword = caCertificatePassword;
        }

        public String getCaRevocationUrl() {
            return caRevocationUrl;
        }

        public void setCaRevocationUrl(String caRevocationUrl) {
            this.caRevocationUrl = caRevocationUrl;
        }

        public String getCertificateKeyFile() {
            return certificateKeyFile;
        }

        public void setCertificateKeyFile(String certificateKeyFile) {
            this.certificateKeyFile = certificateKeyFile;
        }

        public String getCertificateFile() {
            return certificateFile;
        }

        public void setCertificateFile(String certificateFile) {
            this.certificateFile = certificateFile;
        }

        public String getCipherSuite() {
            return cipherSuite;
        }

        public void setCipherSuite(String cipherSuite) {
            this.cipherSuite = cipherSuite;
        }

        public String getKeyAlias() {
            return keyAlias;
        }

        public void setKeyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
        }

        public String getKeystoreType() {
            return keystoreType;
        }

        public void setKeystoreType(String keystoreType) {
            this.keystoreType = keystoreType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getSessionCacheSize() {
            return sessionCacheSize;
        }

        public void setSessionCacheSize(String sessionCacheSize) {
            this.sessionCacheSize = sessionCacheSize;
        }

        public String getSessionTimeout() {
            return sessionTimeout;
        }

        public void setSessionTimeout(String sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
        }

        public String getTruststoreType() {
            return truststoreType;
        }

        public void setTruststoreType(String truststoreType) {
            this.truststoreType = truststoreType;
        }

        public String getVerifyClient() {
            return verifyClient;
        }

        /**
         * @param verifyClient can be "true" or "want" (and preassumably "need")
         */
        public void setVerifyClient(String verifyClient) {
            this.verifyClient = verifyClient;
        }

        public String getVerifyDepth() {
            return verifyDepth;
        }

        public void setVerifyDepth(String verifyDepth) {
            this.verifyDepth = verifyDepth;
        }
    }
}
