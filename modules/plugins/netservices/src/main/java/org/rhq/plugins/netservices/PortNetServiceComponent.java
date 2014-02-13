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

package org.rhq.plugins.netservices;

import static org.rhq.plugins.netservices.util.StringUtil.EMPTY_STRING;
import static org.rhq.plugins.netservices.util.StringUtil.isBlank;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * @author Thomas Segismont
 */
public class PortNetServiceComponent implements ResourceComponent, MeasurementFacet {

    public static final class ConfigKeys {

        private ConfigKeys() {
            // Defensive
        }

        public static final String ADDRESS = "address";
        public static final String PORT = "port";

    }

    private static final Log LOG = LogFactory.getLog(PortNetServiceComponent.class);

    private static final int CONNECTION_TIMEOUT = 2000;

    private PortNetServiceComponentConfiguration componentConfiguration;

    @Override
    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException {
        componentConfiguration = createComponentConfiguration(resourceContext.getPluginConfiguration());
    }

    static PortNetServiceComponentConfiguration createComponentConfiguration(Configuration pluginConfig)
        throws InvalidPluginConfigurationException {
        String addressString = pluginConfig.getSimpleValue(ConfigKeys.ADDRESS, EMPTY_STRING);
        if (isBlank(addressString)) {
            throw new InvalidPluginConfigurationException("Address is not defined");
        }
        String portString = pluginConfig.getSimpleValue(ConfigKeys.PORT, EMPTY_STRING);
        if (isBlank(portString)) {
            throw new InvalidPluginConfigurationException("Port is not defined");
        }
        int port;
        try {
            port = Integer.parseInt(portString);
        } catch (NumberFormatException e) {
            throw new InvalidPluginConfigurationException("Invalid port number: " + portString);
        }
        InetAddress address;
        try {
            address = InetAddress.getByName(addressString);
        } catch (UnknownHostException uhe) {
            throw new InvalidPluginConfigurationException(uhe);
        }
        return new PortNetServiceComponentConfiguration(address, port);
    }

    @Override
    public void stop() {
        componentConfiguration = null;
    }

    @Override
    public AvailabilityType getAvailability() {
        return portReachable() ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        for (MeasurementScheduleRequest request : metrics) {
            if (request.getName().equals("ipAddress")) {
                report.addData(new MeasurementDataTrait(request, componentConfiguration.getAddress().getHostAddress()));
            } else if (request.getName().equals("hostName")) {
                report.addData(new MeasurementDataTrait(request, componentConfiguration.getAddress()
                    .getCanonicalHostName()));
            } else if (request.getName().equals("connectTime")) {
                long start = System.currentTimeMillis();
                if (portReachable()) {
                    report.addData(new MeasurementDataNumeric(request, (double) (System.currentTimeMillis() - start)));
                }
            }
        }
    }

    private boolean portReachable() {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(
                new InetSocketAddress(componentConfiguration.getAddress(), componentConfiguration.getPort()),
                CONNECTION_TIMEOUT);
            return true;
        } catch (Exception e) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Unable to reach remote port: " + componentConfiguration.toString(), e);
            }
        } finally {
            if (socket != null) {
                closeQuietly(socket);
            }
        }
        return false;
    }

    private void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignore) {
        }
    }

}
