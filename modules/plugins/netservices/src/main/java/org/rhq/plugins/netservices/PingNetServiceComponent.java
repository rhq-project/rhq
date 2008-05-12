/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.netservices;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.UnknownHostException;


/**
 * Monitoring of HTTP Servers
 *
 * @author Greg Hinkle
 */
public class PingNetServiceComponent implements ResourceComponent, MeasurementFacet {

    public static final String CONFIG_ADDRESS = "address";


    private ResourceContext resourceContext;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        String addressString = resourceContext.getPluginConfiguration().getSimple(CONFIG_ADDRESS).getStringValue();
        try {
            InetAddress address = InetAddress.getByName(addressString);
        } catch (UnknownHostException uhe) {
            throw new InvalidPluginConfigurationException(uhe);
        }
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        try {
            String addressString = resourceContext.getPluginConfiguration().getSimple(CONFIG_ADDRESS).getStringValue();
            InetAddress address = InetAddress.getByName(addressString);
            return address.isReachable(5000) ? AvailabilityType.UP : AvailabilityType.DOWN;
        } catch (Exception e) {
            return AvailabilityType.DOWN;
        }
    }


    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        String addressString = resourceContext.getPluginConfiguration().getSimple(CONFIG_ADDRESS).getStringValue();
        InetAddress address = InetAddress.getByName(addressString);

        for (MeasurementScheduleRequest request :metrics) {
            if (request.getName().equals("ipAddress")) {
                report.addData(new MeasurementDataTrait(request, address.getHostAddress()));
            } else if (request.getName().equals("hostName")) {
                report.addData(new MeasurementDataTrait(request, address.getCanonicalHostName()));
            } else if (request.getName().equals("responseTime")) {
                long start = System.currentTimeMillis();
                address.isReachable(5000);
                report.addData(new MeasurementDataNumeric(request, (double) (System.currentTimeMillis() - start)));
            }
        }
    }

}