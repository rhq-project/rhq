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

package org.rhq.plugins.netservices;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
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
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * Monitoring of HTTP Servers
 * 
 * @author Greg Hinkle
 */
public class HTTPNetServiceComponent implements ResourceComponent, MeasurementFacet, OperationFacet {
    private final Log log = LogFactory.getLog(this.getClass());

    public static final String CONFIG_URL = "url";
    public static final String CONFIG_USER = "user";
    public static final String CONFIG_PASSWORD = "password";
    public static final String CONFIG_REALM = "realm";

    // One of { none, SSL, TLS }
    public static final String CONFIG_SSL_PROTOCOL = "sslProtocol";

    // One of { HEAD, GET }
    public static final String CONFIG_METHOD = "method";
    public static final String CONFIG_FOLOW_REDIRECTS = "followRedirects";
    public static final String CONFIG_RESPONSE_PATTERN = "responsePattern";

    private ResourceContext resourceContext;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        String url = resourceContext.getPluginConfiguration().getSimple(CONFIG_URL).getStringValue();
        if (!url.startsWith("http:") && !url.startsWith("https")) {
            throw new InvalidPluginConfigurationException("Url not valid. Must start with 'http:' or 'https:'");
        }
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        try {
            return getValuesOrAvailability(null, null) ? AvailabilityType.UP : AvailabilityType.DOWN;
        } catch (Exception e) {
            return AvailabilityType.DOWN;
        }
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        getValuesOrAvailability(report, metrics);
    }

    public boolean getValuesOrAvailability(MeasurementReport report, Set<MeasurementScheduleRequest> metrics)
        throws Exception {

        try {

            Configuration config = resourceContext.getPluginConfiguration();

            HttpClient httpClient = new HttpClient();

            GetMethod method = new GetMethod(config.getSimple(CONFIG_URL).getStringValue());

            method.setFollowRedirects(config.getSimple(CONFIG_FOLOW_REDIRECTS).getBooleanValue());

            long start = System.currentTimeMillis();
            int responseCode = httpClient.executeMethod(method);
            long connectTime = System.currentTimeMillis() - start;

            boolean success = !config.getSimple("validateResponseCode").getBooleanValue()
                || (responseCode >= 200 && responseCode <= 299);

            String response = method.getResponseBodyAsString();

            long readTime = (System.currentTimeMillis() - start);

            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss zzz");
            Date contentDate = sdf.parse(method.getResponseHeader("Date").getValue());

            //            System.out.println("Success: " + success);
            //            System.out.println("Response: " + responseCode);
            //            System.out.println("Connect Time: " + connectTime);
            //            System.out.println("Read Time: " + readTime);
            //            System.out.println("Content Length: " + response.length());
            //            System.out.println("Content Date: " + contentDate);
            //            System.out.println("Content Charset: " + method.getResponseCharSet());
            //            System.out.println("Content Age: " + (System.currentTimeMillis() - contentDate.getTime()));

            //            System.out.println("-----------------------");
            //            for (Header header : method.getResponseHeaders()) {
            //                System.out.println(header.getName() + " =  " + header.getValue());
            //            }

            if (metrics != null) {
                for (MeasurementScheduleRequest request : metrics) {
                    if (request.getName().equals("connectTime")) {
                        report.addData(new MeasurementDataNumeric(request, (double) connectTime));
                    } else if (request.getName().equals("readTime")) {
                        report.addData(new MeasurementDataNumeric(request, (double) readTime));
                    } else if (request.getName().equals("contentLength")) {
                        report.addData(new MeasurementDataNumeric(request, (double) response.length()));
                    } else if (request.getName().equals("contentAge")) {
                        report.addData(new MeasurementDataNumeric(request,
                            (double) (System.currentTimeMillis() - contentDate.getTime())));
                    }
                }
            }

            return success;

        } catch (Exception e) {
            // e.printStackTrace();
            log.error(e);
        }
        return false;
    }

    // TODO GH: This really only makes sense to offer to go get the content if we can support long config content for responses bigger than 4k
    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {
        Configuration config = resourceContext.getPluginConfiguration();

        HttpClient httpClient = new HttpClient();

        GetMethod method = new GetMethod(config.getSimple(CONFIG_URL).getStringValue());

        method.setFollowRedirects(config.getSimple(CONFIG_FOLOW_REDIRECTS).getBooleanValue());

        int responseCode = httpClient.executeMethod(method);

        String response = method.getResponseBodyAsString();

        OperationResult result = new OperationResult(response);
        return result;
    }
}
