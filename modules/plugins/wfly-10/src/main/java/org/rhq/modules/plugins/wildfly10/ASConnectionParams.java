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
 * aLong with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.wildfly10;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.rhq.modules.plugins.wildfly10.helper.HostnameVerification;
import org.rhq.modules.plugins.wildfly10.helper.ServerPluginConfiguration;
import org.rhq.modules.plugins.wildfly10.helper.TrustStrategy;

/**
 * Groups {@link ASConnection} parameters.
 *
 * @author Thomas Segismont
 */
public final class ASConnectionParams {
    /**
     * By default, expire persistent connection after 5 seconds.
     */
    public static final int DEFAULT_KEEPALIVE_TIMEOUT = (int) SECONDS.toMillis(5);

    private final String host;
    private final int port;
    private final boolean secure;
    private final String username;
    private final String password;
    private final long keepAliveTimeout;
    private final TrustStrategy trustStrategy;
    private final HostnameVerification hostnameVerification;
    private final String truststoreType;
    private final String truststore;
    private final String truststorePassword;
    private final boolean clientcertAuthentication;
    private final String keystoreType;
    private final String keystore;
    private final String keystorePassword;
    private final String keyPassword;

    ASConnectionParams(String host, int port, boolean secure, String username, String password, Long keepAliveTimeout,
        TrustStrategy trustStrategy, HostnameVerification hostnameVerification, String truststoreType,
        String truststore, String truststorePassword, boolean clientcertAuthentication, String keystoreType,
        String keystore, String keystorePassword, String keyPassword) {
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.username = username;
        this.password = password;
        this.keyPassword = keyPassword;
        this.keepAliveTimeout = keepAliveTimeout != null ? keepAliveTimeout : DEFAULT_KEEPALIVE_TIMEOUT;
        this.trustStrategy = trustStrategy;
        this.hostnameVerification = hostnameVerification;
        this.truststoreType = truststoreType;
        this.truststore = truststore;
        this.truststorePassword = truststorePassword;
        this.clientcertAuthentication = clientcertAuthentication;
        this.keystoreType = keystoreType;
        this.keystore = keystore;
        this.keystorePassword = keystorePassword;
    }

    /**
     * Creates a new instance from a standalone or host controller resource plugin configuration object.
     *
     * @param serverPluginConfig top level server plugin configuration object
     */
    public static ASConnectionParams createFrom(ServerPluginConfiguration serverPluginConfig) {
        return new ASConnectionParamsBuilder() //
            .setHost(serverPluginConfig.getHostname()) //
            .setPort(serverPluginConfig.getPort()) //
            .setSecure(serverPluginConfig.isSecure()) //
            .setUsername(serverPluginConfig.getUser()) //
            .setPassword(serverPluginConfig.getPassword()) //
            .setKeepAliveTimeout(serverPluginConfig.getManagementConnectionTimeout()) //
            .setTrustStrategy(serverPluginConfig.getTrustStrategy()) //
            .setHostnameVerification(serverPluginConfig.getHostnameVerification()) //
            .setTruststoreType(serverPluginConfig.getTruststoreType()) //
            .setTruststore(serverPluginConfig.getTruststore()) //
            .setTruststorePassword(serverPluginConfig.getTruststorePassword()) //
            .setClientcertAuthentication(serverPluginConfig.isClientcertAuthentication()) //
            .setKeystoreType(serverPluginConfig.getKeystoreType()) //
            .setKeystore(serverPluginConfig.getKeystore()) //
            .setKeystorePassword(serverPluginConfig.getKeystorePassword()) //
            .setKeyPassword(serverPluginConfig.getKeyPassword()).createASConnectionParams();
    }

    /**
     * @return the remote host
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the remote port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return true if connection should be established over SSL, false otherwise
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * @return the username for authentication challenges
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the password for authentication challenges
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the number of milliseconds the connection can stay idle before being closed
     */
    public long getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    /**
     * @return how to trust server certicates
     */
    public TrustStrategy getTrustStrategy() {
        return trustStrategy;
    }

    /**
     * @return how to verify server hostname
     */
    public HostnameVerification getHostnameVerification() {
        return hostnameVerification;
    }

    /**
     * @return for example, jks or pkcs12
     */
    public String getTruststoreType() {
        return truststoreType;
    }

    /**
     * @return Location of the truststore file
     */
    public String getTruststore() {
        return truststore;
    }

    /**
     * @return truststore file password
     */
    public String getTruststorePassword() {
        return truststorePassword;
    }

    /**
     * @return truststore file password
     */
    public boolean isClientcertAuthentication() {
        return clientcertAuthentication;
    }

    /**
     * @return for example, jks or pkcs12
     */
    public String getKeystoreType() {
        return keystoreType;
    }

    /**
     * @return keystore file password
     */
    public String getKeystore() {
        return keystore;
    }

    /**
     * @return keystore file password
     */
    public String getKeystorePassword() {
        return keystorePassword;
    }

    /**
     * @return key password (securing the entry in the keystore file)
     */
    public String getKeyPassword() {
        return keyPassword;
    }
}
