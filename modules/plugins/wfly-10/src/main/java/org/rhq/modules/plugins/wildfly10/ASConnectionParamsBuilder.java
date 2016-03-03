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

package org.rhq.modules.plugins.wildfly10;

import org.rhq.modules.plugins.wildfly10.helper.HostnameVerification;
import org.rhq.modules.plugins.wildfly10.helper.TrustStrategy;

/**
 * @author Thomas Segismont
 */
public class ASConnectionParamsBuilder {
    private String host;
    private int port;
    private boolean secure;
    private String username;
    private String password;
    private Long keepAliveTimeout;
    private TrustStrategy trustStrategy;
    private HostnameVerification hostnameVerification;
    private String truststoreType;
    private String truststore;
    private String truststorePassword;
    private boolean clientcertAuthentication;
    private String keystoreType;
    private String keystore;
    private String keystorePassword;
    private String keyPassword;

    public ASConnectionParamsBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    public ASConnectionParamsBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public ASConnectionParamsBuilder setSecure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public ASConnectionParamsBuilder setUsername(String username) {
        this.username = username;
        return this;
    }

    public ASConnectionParamsBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public ASConnectionParamsBuilder setKeepAliveTimeout(Long keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
        return this;
    }

    public ASConnectionParamsBuilder setTrustStrategy(TrustStrategy trustStrategy) {
        this.trustStrategy = trustStrategy;
        return this;
    }

    public ASConnectionParamsBuilder setHostnameVerification(HostnameVerification hostnameVerification) {
        this.hostnameVerification = hostnameVerification;
        return this;
    }

    public ASConnectionParamsBuilder setTruststoreType(String truststoreType) {
        this.truststoreType = truststoreType;
        return this;
    }

    public ASConnectionParamsBuilder setTruststore(String truststore) {
        this.truststore = truststore;
        return this;
    }

    public ASConnectionParamsBuilder setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
        return this;
    }

    public ASConnectionParamsBuilder setClientcertAuthentication(boolean clientcertAuthentication) {
        this.clientcertAuthentication = clientcertAuthentication;
        return this;
    }

    public ASConnectionParamsBuilder setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
        return this;
    }

    public ASConnectionParamsBuilder setKeystore(String keystore) {
        this.keystore = keystore;
        return this;
    }

    public ASConnectionParamsBuilder setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
        return this;
    }

    public ASConnectionParamsBuilder setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
        return this;
    }

    public ASConnectionParams createASConnectionParams() {
        return new ASConnectionParams(host, port, secure, username, password, keepAliveTimeout, trustStrategy,
            hostnameVerification, truststoreType, truststore, truststorePassword, clientcertAuthentication,
            keystoreType, keystore, keystorePassword, keyPassword);
    }
}
