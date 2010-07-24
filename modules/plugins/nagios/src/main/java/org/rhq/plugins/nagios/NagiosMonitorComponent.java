package org.rhq.plugins.nagios;

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
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ChildResourceTypeDiscoveryFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.nagios.controller.NagiosManagementInterface;
import org.rhq.plugins.nagios.data.NagiosSystemData;
import org.rhq.plugins.nagios.error.NagiosException;

/**
 * Plugin Component Class
 * To make it work you have to change the ip adress and port for your purpose
 * The metric-, service- and hostnames depend on your nagios system, please make
 * it sure that they exist and change the plugin descriptor too
 *
 * @author Alexander Kiefer
 */
public class NagiosMonitorComponent implements ResourceComponent, MeasurementFacet, ChildResourceTypeDiscoveryFacet {
    private final Log log = LogFactory.getLog(this.getClass());

    public static final String DEFAULT_NAGIOSIP = "127.0.0.1";
    public static final String DEFAULT_NAGIOSPORT = "6557";

    private ResourceContext context;
    private NagiosManagementInterface nagiosManagementInterface;
    private String nagiosHost;
    private int nagiosPort;

    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
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

    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
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

    /**
     * Tear down the rescource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {

    }

    /**
     * Gather measurement data
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) {
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

    public String getNagiosHost() {
        return nagiosHost;
    }

    public int getNagiosPort() {
        return nagiosPort;
    }

    public Set<ResourceType> discoverChildResourceTypes() {

        log.info("<nagiosMonitorComponent>discoverChildResourceTypes called");

        ResourceType parentType = this.context.getResourceType();
        ResourceType resourceType = new ResourceType("NewChild", parentType.getPlugin(), ResourceCategory.SERVICE,
            parentType);

        Set<ResourceType> resourceTypes = new HashSet<ResourceType>();
        resourceTypes.add(resourceType);

        return resourceTypes;
    }
}