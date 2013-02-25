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

import java.net.InetAddress;
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
import org.rhq.core.util.StringUtil;

/**
 * Monitoring of IP addresses
 *
 * @author Greg Hinkle
 */
public class PingNetServiceComponent implements ResourceComponent, MeasurementFacet {

    public static final class ConfigKeys {

        private ConfigKeys() {
            // Defensive
        }

        public static final String ADDRESS = "address";
    }

    private static final Log LOG = LogFactory.getLog(PingNetServiceComponent.class);

    private static final int PING_TIMEOUT = 5000;

    private InetAddress address;

    @Override
    public void start(@SuppressWarnings("rawtypes")
    ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        address = createComponentConfiguration(resourceContext.getPluginConfiguration());
    }

    static InetAddress createComponentConfiguration(Configuration pluginConfig)
        throws InvalidPluginConfigurationException {
        String addressString = pluginConfig.getSimpleValue(ConfigKeys.ADDRESS, StringUtil.EMPTY_STRING);
        if (StringUtil.isBlank(addressString)) {
            throw new InvalidPluginConfigurationException("Address is not defined");
        }
        try {
            return InetAddress.getByName(addressString);
        } catch (UnknownHostException uhe) {
            throw new InvalidPluginConfigurationException(uhe);
        }
    }

    @Override
    public void stop() {
        address = null;
    }

    @Override
    public AvailabilityType getAvailability() {
        try {
            return address.isReachable(PING_TIMEOUT) ? AvailabilityType.UP : AvailabilityType.DOWN;
        } catch (Exception e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(address.getHostAddress() + " not reachable", e);
            }
            return AvailabilityType.DOWN;
        }
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        for (MeasurementScheduleRequest request : metrics) {
            if (request.getName().equals("ipAddress")) {
                report.addData(new MeasurementDataTrait(request, address.getHostAddress()));
            } else if (request.getName().equals("hostName")) {
                report.addData(new MeasurementDataTrait(request, address.getCanonicalHostName()));
            } else if (request.getName().equals("responseTime")) {
                long start = System.currentTimeMillis();
                address.isReachable(PING_TIMEOUT);
                report.addData(new MeasurementDataNumeric(request, (double) (System.currentTimeMillis() - start)));
            }
        }
    }

}