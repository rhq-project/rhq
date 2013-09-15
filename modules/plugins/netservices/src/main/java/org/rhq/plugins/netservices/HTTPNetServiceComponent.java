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

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.rhq.plugins.netservices.HTTPNetServiceComponentConfiguration.createComponentConfiguration;
import static org.rhq.plugins.netservices.util.StringUtil.EMPTY_STRING;
import static org.rhq.plugins.netservices.util.StringUtil.isNotBlank;

import java.io.IOException;
import java.net.ProxySelector;
import java.util.Date;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

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
        public static final String PROXY_MODE = "proxyMode";
        public static final String PROXY_HOST = "proxyHost";
        public static final String PROXY_PORT = "proxyPort";

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

        BasicClientConnectionManager httpConnectionManager = new BasicClientConnectionManager();
        DefaultHttpClient client = new DefaultHttpClient(httpConnectionManager);

        String userName = pluginConfig.getSimpleValue(ConfigKeys.USER, EMPTY_STRING);
        // Set credentials only if a user name is configured
        if (isNotBlank(userName)) {
            String password = pluginConfig.getSimpleValue(ConfigKeys.PASSWORD, EMPTY_STRING);
            String realm = pluginConfig.getSimpleValue(ConfigKeys.REALM, AuthScope.ANY_REALM);
            client.getCredentialsProvider().setCredentials(
                new AuthScope(componentConfig.getEndPointUrl().getHost(), componentConfig.getEndPointUrl().getPort(),
                    realm), new UsernamePasswordCredentials(userName, password));
        }

        HttpRequestBase method;
        switch (componentConfig.getHttpMethod()) {
        case GET:
            method = new HttpGet(componentConfig.getEndPointUrl().toExternalForm());
            break;
        case HEAD:
            method = new HttpHead(componentConfig.getEndPointUrl().toExternalForm());
            break;
        default:
            throw new RuntimeException("Unsupported http method: '" + componentConfig.getHttpMethod() + "'");
        }
        Boolean followRedirects = pluginConfig.getSimple(ConfigKeys.FOLOW_REDIRECTS).getBooleanValue();
        HttpParams httpParams = client.getParams();
        HttpClientParams.setRedirecting(httpParams, followRedirects == null ? false : followRedirects.booleanValue());

        switch (componentConfig.getProxyMode()) {
        case MANUAL:
            HttpHost proxy = new HttpHost(componentConfig.getProxyHost(), componentConfig.getProxyPort());
            httpParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            break;
        case SYS_PROPS:
            ProxySelectorRoutePlanner routePlanner = new ProxySelectorRoutePlanner(client.getConnectionManager()
                .getSchemeRegistry(), ProxySelector.getDefault());
            client.setRoutePlanner(routePlanner);
            break;
        default:
        }

        try {

            long start = System.nanoTime();
            HttpResponse response = client.execute(method);
            long connectTime = NANOSECONDS.toMillis(System.nanoTime() - start);

            // Availability may depend on reponse code value 
            int responseCode = response.getStatusLine().getStatusCode();
            boolean success = !pluginConfig.getSimple(ConfigKeys.VALIDATE_RESPONSE_CODE).getBooleanValue()
                || (responseCode >= 200 && responseCode <= 299);
            // Availability may depend on reponse content matching a pattern
            success = success
                && (componentConfig.getResponseValidationPattern() == null || componentConfig
                    .getResponseValidationPattern().matcher(getResponseBody(response)).find());

            long readTime = NANOSECONDS.toMillis(System.nanoTime() - start);

            Header dateHeader = response.getFirstHeader("Date");
            Date contentDate = dateHeader == null ? null : DateUtils.parseDate(dateHeader.getValue());
            HttpEntity entity = response.getEntity();

            if (metrics != null) {
                for (MeasurementScheduleRequest request : metrics) {
                    if (request.getName().equals("connectTime")) {
                        report.addData(new MeasurementDataNumeric(request, (double) connectTime));
                    } else if (request.getName().equals("readTime")) {
                        report.addData(new MeasurementDataNumeric(request, (double) readTime));
                    } else if (request.getName().equals("contentLength")) {
                        if (entity != null) {
                            report.addData(new MeasurementDataNumeric(request, (double) entity.getContentLength()));
                        }
                    } else if (request.getName().equals("contentAge")) {
                        if (contentDate != null) {
                            report.addData(new MeasurementDataNumeric(request,
                                (double) (System.currentTimeMillis() - contentDate.getTime())));
                        }
                    }
                }
            }

            return success;

        } catch (Exception e) {
            LOG.error(e);
        } finally {
            method.abort();
            client.getConnectionManager().shutdown();
        }
        return false;
    }

    private String getResponseBody(HttpResponse response) throws IOException {
        HttpEntity httpResponseEntity = response.getEntity();
        return httpResponseEntity == null ? EMPTY_STRING : EntityUtils.toString(httpResponseEntity);

    }

}
