/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.plugins.apache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * Represents a Apache server-status handler in Apache configuration &lt;Location&gt; section.
 * 
 * @author Jeremie Lagarde
 */
public class ApacheServerStatusComponent implements ResourceComponent<ApacheLocationComponent>, MeasurementFacet {

    private static final Log LOG = LogFactory.getLog(ApacheServerStatusComponent.class);

    public static final String REGEXP_PROP = "regexp";

    private static final String SCOREBOARD = "Scoreboard";

    private static final String METRIC_OPEN_SLOT_WITH_NO_CURRENT_PROCESS = "openSlotWithNoCurrentProcess";
    private static final String METRIC_IDLE_CLEANUP_OF_WORKER = "idleCleanupOfWorker";
    private static final String METRIC_GRACEFULLY_FINISHING = "gracefullyFinishing";
    private static final String METRIC_LOGGING = "logging";
    private static final String METRIC_CLOSING_CONNECTION = "closingConnection";
    private static final String METRIC_DNS_LOOKUP = "dnsLookup";
    private static final String METRIC_KEEPALIVE = "keepalive";
    private static final String METRIC_SENDING_REPLY = "sendingReply";
    private static final String METRIC_READING_REQUEST = "readingRequest";
    private static final String METRIC_STARTING_UP = "startingUp";
    private static final String METRIC_WAITING_FOR_CONNECTION = "waitingForConnection";

    ResourceContext<ApacheLocationComponent> resourceContext;

    public void start(ResourceContext<ApacheLocationComponent> context) throws InvalidPluginConfigurationException,
        Exception {
        resourceContext = context;
    }

    public void stop() {
        resourceContext = null;
    }

    public AvailabilityType getAvailability() {
        AvailabilityType availability = resourceContext.getParentResourceComponent().getAvailability();
        if (AvailabilityType.UP.equals(availability)) {
            InputStream datas = null;
            BufferedReader reader = null;
            try {
                datas = getDatas();
                String line;
                reader = new BufferedReader(new InputStreamReader(datas, "UTF-8"));
                while ((line = reader.readLine()) != null) {
                    if (line.contains(SCOREBOARD))
                        return AvailabilityType.UP;
                }
                return AvailabilityType.MISSING;
            } catch (Exception e) {
                LOG.error("Can't read the scoreboard : " + e, e);
            } finally {
                if (reader != null)
                    try {
                        reader.close();
                    } catch (IOException e) {
                        LOG.error("Can't close the scoreboard reader : " + e);
                    }
                if (datas != null)
                    try {
                        datas.close();
                    } catch (IOException e) {
                        LOG.error("Can't close the scoreboard data : " + e);
                    }
            }
        }
        return availability;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> schedules) throws Exception {
        InputStream datas = null;
        try {
            datas = getDatas();
            Map<String, Double> result = parse(datas);
            for (MeasurementScheduleRequest schedule : schedules) {
                String metricName = schedule.getName();
                if (result.containsKey(metricName)) {
                    MeasurementDataNumeric metric = new MeasurementDataNumeric(schedule, result.get(metricName));
                    report.addData(metric);
                }
            }
        } finally {
            if (datas != null)
                datas.close();
        }

    }

    private InputStream getDatas() throws MalformedURLException, IOException {
        InputStream datas;
        URL url = new URL(resourceContext.getParentResourceComponent().getURL() + "?auto");
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(2000);
        datas = connection.getInputStream();
        return datas;
    }

    private Map<String, Double> parse(InputStream statusPage) {
        Map<String, Double> result = new HashMap<String, Double>();
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(statusPage, "UTF-8"));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    parseLine(result, line);
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                statusPage.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private void parseLine(Map<String, Double> result, String line) {
        String[] datas = line.split(": ");
        if (datas.length == 2) {
            if (datas[0].equals(SCOREBOARD)) {
                char[] scoreboard = datas[1].toCharArray();
                double waitingForConnection = 0; // '_'
                double startingUp = 0; // 'S'
                double readingRequest = 0; // 'R'
                double sendingReply = 0; // 'W'
                double keepalive = 0; // 'K'
                double dnsLookup = 0; // 'D'
                double closingConnection = 0; // 'C'
                double logging = 0; // 'L'
                double gracefullyFinishing = 0; // 'G'
                double idleCleanupOfWorker = 0; // 'I'
                double openSlotWithNoCurrentProcess = 0; // '.'
                for (char c : scoreboard) {
                    switch (c) {
                    case '_':
                        waitingForConnection++;
                        break;
                    case 'S':
                        startingUp++;
                        break;
                    case 'R':
                        readingRequest++;
                        break;
                    case 'W':
                        sendingReply++;
                        break;
                    case 'K':
                        keepalive++;
                        break;
                    case 'D':
                        dnsLookup++;
                        break;
                    case 'C':
                        closingConnection++;
                        break;
                    case 'L':
                        logging++;
                        break;
                    case 'G':
                        gracefullyFinishing++;
                        break;
                    case 'I':
                        idleCleanupOfWorker++;
                        break;
                    case '.':
                        openSlotWithNoCurrentProcess++;
                        break;
                    default:
                        break;
                    }
                }
                result.put(METRIC_WAITING_FOR_CONNECTION, waitingForConnection);
                result.put(METRIC_STARTING_UP, startingUp);
                result.put(METRIC_READING_REQUEST, readingRequest);
                result.put(METRIC_SENDING_REPLY, sendingReply);
                result.put(METRIC_KEEPALIVE, keepalive);
                result.put(METRIC_DNS_LOOKUP, dnsLookup);
                result.put(METRIC_CLOSING_CONNECTION, closingConnection);
                result.put(METRIC_LOGGING, logging);
                result.put(METRIC_GRACEFULLY_FINISHING, gracefullyFinishing);
                result.put(METRIC_IDLE_CLEANUP_OF_WORKER, idleCleanupOfWorker);
                result.put(METRIC_OPEN_SLOT_WITH_NO_CURRENT_PROCESS, openSlotWithNoCurrentProcess);
            } else {
                try {
                    result.put(datas[0].replace(" ", ""), Double.parseDouble(datas[1]));
                } catch (NumberFormatException ex) {
                }
            }
        }
    }
}
