/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

package org.rhq.plugins.netservices;

import static org.rhq.core.util.StringUtil.EMPTY_STRING;
import static org.rhq.plugins.netservices.HTTPNetServiceComponent.ConfigKeys.METHOD;
import static org.rhq.plugins.netservices.HTTPNetServiceComponent.ConfigKeys.PROXY_HOST;
import static org.rhq.plugins.netservices.HTTPNetServiceComponent.ConfigKeys.PROXY_MODE;
import static org.rhq.plugins.netservices.HTTPNetServiceComponent.ConfigKeys.PROXY_PORT;
import static org.rhq.plugins.netservices.HTTPNetServiceComponent.ConfigKeys.URL;
import static org.rhq.plugins.netservices.HTTPNetServiceComponent.ConfigKeys.VALIDATE_RESPONSE_PATTERN;
import static org.rhq.plugins.netservices.HTTPNetServiceComponentConfiguration.HttpMethod.HEAD;
import static org.rhq.plugins.netservices.HTTPNetServiceComponentConfiguration.ProxyMode.MANUAL;
import static org.rhq.plugins.netservices.HTTPNetServiceComponentConfiguration.ProxyMode.NONE;
import static org.rhq.plugins.netservices.util.StringUtil.isBlank;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;

/**
 * @author Thomas Segismont
 */
class HTTPNetServiceComponentConfiguration {

    static enum HttpMethod {
        GET, HEAD
    }

    static enum ProxyMode {
        NONE, SYS_PROPS, MANUAL
    }

    private URL endPointUrl;

    private HttpMethod httpMethod;

    private Pattern responseValidationPattern;

    private ProxyMode proxyMode;

    private String proxyHost;

    private int proxyPort;

    HTTPNetServiceComponentConfiguration(URL endPointUrl, HttpMethod httpMethod, Pattern responseValidationPattern,
        ProxyMode proxyMode, String proxyHost, int proxyPort) {
        this.endPointUrl = endPointUrl;
        this.httpMethod = httpMethod;
        this.responseValidationPattern = responseValidationPattern;
        this.proxyMode = proxyMode;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    /**
     * Create a foram {@link org.rhq.plugins.netservices.HTTPNetServiceComponentConfiguration} instance with the supplied {@link org.rhq.core.domain.configuration.Configuration}.
     * May throw {@link org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException} if:
     * <ul>
     * <li>Url is empty, invalid, or pointing an non http/https resource</li>
     * <li>Http method is not HEAD or GET</li>
     * <li>If both content validation and HEAD method are configured</li>
     * <li>If the content validation pattern is invalid</li>
     * <ul>
     *
     * @param pluginConfig
     * @return
     * @throws org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException
     */
    static HTTPNetServiceComponentConfiguration createComponentConfiguration(Configuration pluginConfig) {

        URL endPointUrl = null;
        String configUrl = pluginConfig.getSimpleValue(URL, EMPTY_STRING);
        if (isBlank(configUrl)) {
            throw new InvalidPluginConfigurationException("Endpoint URL is not defined");
        }
        try {
            endPointUrl = new URL(configUrl);
        } catch (MalformedURLException e) {
            throw new InvalidPluginConfigurationException(configUrl + " is not a valid URL");
        }
        String protocol = endPointUrl.getProtocol();
        if (!protocol.equals("http") && !protocol.equals("https")) {
            throw new InvalidPluginConfigurationException(configUrl + "does not point to an http(s) resource");
        }

        HttpMethod httpMethod = null;
        String configMethod = pluginConfig.getSimpleValue(METHOD, EMPTY_STRING);
        try {
            httpMethod = HttpMethod.valueOf(configMethod);
        } catch (IllegalArgumentException e) {
            throw new InvalidPluginConfigurationException("Invalid http method: " + configMethod);
        }

        Pattern responseValidationPattern = null;
        String configValidateResponsePattern = pluginConfig.getSimpleValue(VALIDATE_RESPONSE_PATTERN);
        if (configValidateResponsePattern != null) {
            if (httpMethod.equals(HEAD)) {
                throw new InvalidPluginConfigurationException("Cannot validate response content with HEAD request");
            }
            try {
                responseValidationPattern = Pattern.compile(configValidateResponsePattern);
            } catch (PatternSyntaxException e) {
                throw new InvalidPluginConfigurationException("Invalid pattern: " + configValidateResponsePattern);
            }
        }

        ProxyMode proxyMode;
        String configProxyMode = pluginConfig.getSimpleValue(PROXY_MODE, NONE.name());
        try {
            proxyMode = ProxyMode.valueOf(configProxyMode);
        } catch (IllegalArgumentException e) {
            throw new InvalidPluginConfigurationException("Invalid proxy mode: " + configProxyMode);
        }

        String proxyHost = pluginConfig.getSimpleValue(PROXY_HOST, EMPTY_STRING);
        String configProxyPort = pluginConfig.getSimpleValue(PROXY_PORT, EMPTY_STRING);
        int proxyPort = -1;

        switch (proxyMode) {
        case MANUAL:
            if (isBlank(proxyHost)) {
                throw new InvalidPluginConfigurationException("In '" + MANUAL.name() + "' proxy mode the " + PROXY_HOST
                    + " property must be set");
            }
            if (isBlank(configProxyPort)) {
                throw new InvalidPluginConfigurationException("In '" + MANUAL.name() + "' proxy mode the " + PROXY_PORT
                    + " property must be set");
            }
            try {
                proxyPort = Integer.parseInt(configProxyPort);
            } catch (NumberFormatException e) {
                throw new InvalidPluginConfigurationException(configProxyPort + " is not a number");
            }
            if (proxyPort < 1 || proxyPort > 65535) {
                throw new InvalidPluginConfigurationException(configProxyPort + " is not a valid port number");
            }
            break;
        default:
        }

        return new HTTPNetServiceComponentConfiguration(endPointUrl, httpMethod, responseValidationPattern, proxyMode,
            proxyHost, proxyPort);
    }

    URL getEndPointUrl() {
        return endPointUrl;
    }

    HttpMethod getHttpMethod() {
        return httpMethod;
    }

    Pattern getResponseValidationPattern() {
        return responseValidationPattern;
    }

    ProxyMode getProxyMode() {
        return proxyMode;
    }

    String getProxyHost() {
        return proxyHost;
    }

    int getProxyPort() {
        return proxyPort;
    }
}
