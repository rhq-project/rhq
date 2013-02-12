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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.plugins.netservices;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.util.StringUtil;

/**
 * Monitoring of HTTP Servers
 * 
 * @author Greg Hinkle
 */
public class HTTPNetServiceComponent implements ResourceComponent, MeasurementFacet {

    public static final class ConfigKeys {

        private ConfigKeys() {
            // Defensive
        }

        public static final String URL = "url";
        public static final String USER = "user";
        public static final String PASSWORD = "password";
        public static final String REALM = "realm";
        public static final String METHOD = "method";
        public static final String FOLOW_REDIRECTS = "followRedirects";
        public static final String VALIDATE_RESPONSE_CODE = "validateResponseCode";
        public static final String VALIDATE_RESPONSE_PATTERN = "validateResponsePattern";

    }

    public static enum HttpMethod {
        GET, HEAD
    }

    private static final Log LOG = LogFactory.getLog(HTTPNetServiceComponent.class);

    private ResourceContext resourceContext;

    private Configuration pluginConfig;

    private HTTPNetServiceComponentConfiguration componentConfig;

    @Override
    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        pluginConfig = resourceContext.getPluginConfiguration();
        componentConfig = createComponentConfiguration(pluginConfig);
    }

    /**
     * Create a foram {@link HTTPNetServiceComponentConfiguration} instance with the supplied {@link Configuration}.
     * May throw {@link InvalidPluginConfigurationException} if:
     * <ul>
     * <li>Url is empty, invalid, or pointing an non http/https resource</li>
     * <li>Http method is not HEAD or GET</li>
     * <li>If both content validation and HEAD method are configured</li>
     * <li>If the content validation pattern is invalid</li>
     * <ul>
     *
     * @param pluginConfig
     * @return
     * @throws InvalidPluginConfigurationException
     */
    static HTTPNetServiceComponentConfiguration createComponentConfiguration(Configuration pluginConfig) {

        URL endPointUrl = null;
        String configUrl = pluginConfig.getSimpleValue(ConfigKeys.URL, StringUtil.EMPTY_STRING);
        if (StringUtil.isBlank(configUrl)) {
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
        String configMethod = pluginConfig.getSimpleValue(ConfigKeys.METHOD, StringUtil.EMPTY_STRING);
        try {
            httpMethod = HttpMethod.valueOf(configMethod);
        } catch (IllegalArgumentException e) {
            throw new InvalidPluginConfigurationException("Invalid http method: " + configMethod);
        }

        Pattern responseValidationPattern = null;
        String configValidateResponsePattern = pluginConfig.getSimpleValue(ConfigKeys.VALIDATE_RESPONSE_PATTERN);
        if (configValidateResponsePattern != null) {
            if (httpMethod.equals(HttpMethod.HEAD)) {
                throw new InvalidPluginConfigurationException("Cannot validate response content with HEAD request");
            }
            try {
                responseValidationPattern = Pattern.compile(configValidateResponsePattern);
            } catch (PatternSyntaxException e) {
                throw new InvalidPluginConfigurationException("Invalid pattern: " + configValidateResponsePattern);
            }
        }

        return new HTTPNetServiceComponentConfiguration(endPointUrl, httpMethod, responseValidationPattern);
    }

    @Override
    public void stop() {
        resourceContext = null;
        pluginConfig = null;
        componentConfig = null;
    }

    @Override
    public AvailabilityType getAvailability() {
        try {
            return getValuesOrAvailability(null, null) ? AvailabilityType.UP : AvailabilityType.DOWN;
        } catch (Exception e) {
            return AvailabilityType.DOWN;
        }
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        getValuesOrAvailability(report, metrics);
    }

    private boolean getValuesOrAvailability(MeasurementReport report, Set<MeasurementScheduleRequest> metrics)
        throws Exception {

        SimpleHttpConnectionManager httpConnectionManager = new SimpleHttpConnectionManager();
        HttpClient client = new HttpClient(httpConnectionManager);

        String userName = pluginConfig.getSimpleValue(ConfigKeys.USER, StringUtil.EMPTY_STRING);
        // Set credentials only if a user name is configured
        if (StringUtil.isNotBlank(userName)) {
            String password = pluginConfig.getSimpleValue(ConfigKeys.PASSWORD, StringUtil.EMPTY_STRING);
            String realm = pluginConfig.getSimpleValue(ConfigKeys.REALM, AuthScope.ANY_REALM);
            client.getState().setCredentials(
                new AuthScope(componentConfig.getEndPointUrl().getHost(), componentConfig.getEndPointUrl().getPort(),
                    realm), new UsernamePasswordCredentials(userName, password));
        }

        HttpMethodBase method = null;
        switch (componentConfig.getHttpMethod()) {
        case GET:
            method = new GetMethod(componentConfig.getEndPointUrl().toExternalForm());
            break;
        case HEAD:
            method = new HeadMethod(componentConfig.getEndPointUrl().toExternalForm());
            break;
        default:
            throw new RuntimeException("Unsupported http method: '" + componentConfig.getHttpMethod() + "'");
        }
        Boolean followRedirects = pluginConfig.getSimple(ConfigKeys.FOLOW_REDIRECTS).getBooleanValue();
        method.setFollowRedirects(followRedirects == null ? false : followRedirects.booleanValue());

        try {

            long start = System.currentTimeMillis();
            int responseCode = client.executeMethod(method);
            long connectTime = System.currentTimeMillis() - start;

            // Availability may depend on reponse code value 
            boolean success = !pluginConfig.getSimple(ConfigKeys.VALIDATE_RESPONSE_CODE).getBooleanValue()
                || (responseCode >= 200 && responseCode <= 299);
            // Availability may depend on reponse content matching a pattern
            success = success
                && (componentConfig.getResponseValidationPattern() == null || componentConfig
                    .getResponseValidationPattern().matcher(method.getResponseBodyAsString()).find());

            long readTime = (System.currentTimeMillis() - start);

            Header dateHeader = method.getResponseHeader("Date");
            Date contentDate = dateHeader == null ? new Date(System.currentTimeMillis()) : DateUtil
                .parseDate(dateHeader.getValue());

            if (metrics != null) {
                for (MeasurementScheduleRequest request : metrics) {
                    if (request.getName().equals("connectTime")) {
                        report.addData(new MeasurementDataNumeric(request, (double) connectTime));
                    } else if (request.getName().equals("readTime")) {
                        report.addData(new MeasurementDataNumeric(request, (double) readTime));
                    } else if (request.getName().equals("contentLength")) {
                        report.addData(new MeasurementDataNumeric(request, (double) method.getResponseContentLength()));
                    } else if (request.getName().equals("contentAge")) {
                        report.addData(new MeasurementDataNumeric(request,
                            (double) (System.currentTimeMillis() - contentDate.getTime())));
                    }
                }
            }

            return success;

        } catch (Exception e) {
            LOG.error(e);
        } finally {
            // First release connection
            method.releaseConnection();
            // Then force close
            httpConnectionManager.closeIdleConnections(0);
        }
        return false;
    }

}
