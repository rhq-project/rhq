package org.rhq.plugins.nagios.rhqNagiosPlugin;

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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ChildResourceTypeDiscoveryFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.nagios.data.NagiosSystemData;
import org.rhq.plugins.nagios.error.NagiosException;
import org.rhq.plugins.nagios.managementInterface.NagiosManagementInterface;

/**
 *
 * @author Alexander Kiefer
 *
 */
public class NagiosMonitorChildTypeComponent implements ResourceComponent, MeasurementFacet
     {

    private final Log log = LogFactory.getLog(this.getClass());

    public static final String DEFAULT_NAGIOSIP = "127.0.0.1";
    public static final String DEFAULT_NAGIOSPORT = "6557";

    private ResourceContext context;
    private NagiosManagementInterface nagiosManagementInterface;
    private String nagiosHost;
    private int nagiosPort;

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        NagiosSystemData nagiosSystemData = null;
        String serviceName = this.context.getResourceType().getName();
        log.info("getValues() of ResourceType: " + serviceName);

        try {
            //Getting all Nagios system information
            nagiosSystemData = nagiosManagementInterface.createNagiosSystemData();
        } catch (Exception e) {
            log.warn(" Can not get information from Nagios: ", e);
            return;
        }

        //iterating over the metrics
        for (MeasurementScheduleRequest req : metrics) {
            try { // Don't let one bad egg spoil the cake

                String[] splitter = req.getName().split("\\|");
                String property = splitter[1];
                String pattern = splitter[2];

                if (log.isDebugEnabled()) {
                    log.debug("Name of Metric: " + property);
                    log.debug("RegEx: " + pattern);
                }

                // Handle our special discovered case
                if (req.getName().equals(NagiosMonitorComponent.DYNAMIC_TYPE+"Metric")) {
                    MeasurementDataNumeric res = new MeasurementDataNumeric(req,Math.random()*100.0);
                    report.addData(res);
                    continue;
                }


                // Get "raw" data from nagios data structures - we need to pick our value below
                String value = nagiosSystemData.getSingleHostServiceMetric(property, serviceName, "localhost")
                    .getValue(); // TODO use 'real' host

                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(value);
                if (m.matches()) {
                    String val = m.group(1); // Our metric is always in the first match group.

                    // We have a match, now dispatch by dataType of the request
                    if (req.getDataType() == DataType.MEASUREMENT) {
                        MeasurementDataNumeric res = new MeasurementDataNumeric(req, Double.valueOf(val));
                        report.addData(res);
                    } else if (req.getDataType() == DataType.TRAIT) {
                        MeasurementDataTrait res = new MeasurementDataTrait(req, val);
                        report.addData(res);
                    } else
                        log.error("Unknown DataType for request " + req);
                } else {
                    log.warn("Pattern >>" + pattern + "<< did not match for input >>" + value + "<< and request: >>"
                        + req.getName());
                }
            } catch (NagiosException e) {
                log.error(e);
            }
        }

    }


    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
        //get context of this component instance
        this.context = context;

        //get config
        if (context.getParentResourceComponent() instanceof NagiosMonitorComponent) {
            NagiosMonitorComponent parent = (NagiosMonitorComponent) context.getParentResourceComponent();

            nagiosHost = parent.getNagiosHost();
            nagiosPort = parent.getNagiosPort();
        } else {
            Configuration conf = context.getPluginConfiguration();
            nagiosHost = conf.getSimpleValue("nagiosHost", DEFAULT_NAGIOSIP);
            String tmp = conf.getSimpleValue("nagiosPort", DEFAULT_NAGIOSPORT);
            nagiosPort = Integer.parseInt(tmp);
        }

        //Interface class to the nagios system
        nagiosManagementInterface = new NagiosManagementInterface(nagiosHost, nagiosPort);

        //log.info("nagios Plugin started");

    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub

    }

    @Override
    public AvailabilityType getAvailability() {
        if (context.getParentResourceComponent() instanceof NagiosMonitorComponent) {
            return AvailabilityType.UP; // TODO get from parent?
        } else {
            boolean available = false;
            if (nagiosManagementInterface != null)
                available = nagiosManagementInterface.pingNagios();

            if (available)
                return AvailabilityType.UP;
        }
        return AvailabilityType.DOWN;
    }

}
