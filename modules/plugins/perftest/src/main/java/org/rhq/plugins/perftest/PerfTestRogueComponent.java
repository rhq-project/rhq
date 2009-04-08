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
package org.rhq.plugins.perftest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

@SuppressWarnings("unchecked")
public class PerfTestRogueComponent implements ResourceComponent, OperationFacet, ConfigurationFacet {
    // Attributes  --------------------------------------------
    private Log log = LogFactory.getLog(PerfTestRogueComponent.class);

    private ResourceContext resourceContext;
    private Configuration resourceConfiguration;

    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = context;
        ScenarioManager scenarioManager = ScenarioManager.getInstance();
        if (!scenarioManager.isEnabled())
            log.warn("[" + this.resourceContext.getResourceType().getName()
                + "] perftest Resources exist in inventory, but no Perf test scenario is enabled.");
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP; // TODO: look at plugin config to see if we should be UP or DOWN
    }

    public Configuration loadResourceConfiguration() throws Exception {
        if (this.resourceConfiguration == null) {
            Configuration config = new Configuration();
            config.put(new PropertySimple("updateSleep", "1000"));
            config.put(new PropertySimple("updateStatus", "success"));
            config.put(new PropertySimple("loadSleep", "1000"));
            config.put(new PropertySimple("loadStatus", "success"));
            this.resourceConfiguration = config;
        } else {
            PropertySimple sleepProp = (PropertySimple) this.resourceConfiguration.get("loadSleep");
            Long sleep = sleepProp.getLongValue();
            PropertySimple statusProp = (PropertySimple) this.resourceConfiguration.get("loadStatus");
            String status = statusProp.getStringValue();

            try {
                log.info("The rogue component (config-load) was told to sleep: " + sleep + "ms");
                Thread.sleep(sleep);
                log.info("The rogue component (config-load) has finished its sleep of " + sleep + "ms");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (!status.equalsIgnoreCase("success")) {
                throw new Exception("Rogue component (config-load) was told to throw an exception (status=[" + status
                    + "])");
            }
        }

        return this.resourceConfiguration;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        Configuration newConfig = report.getConfiguration();
        PropertySimple sleepProp = (PropertySimple) newConfig.get("updateSleep");
        Long sleep = sleepProp.getLongValue();
        PropertySimple statusProp = (PropertySimple) newConfig.get("updateStatus");
        String status = statusProp.getStringValue();

        try {
            log.info("The rogue component (config) was told to sleep: " + sleep + "ms");
            Thread.sleep(sleep);
            log.info("The rogue component (config) has finished its sleep of " + sleep + "ms");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        this.resourceConfiguration = newConfig;

        if (status.equalsIgnoreCase("success")) {
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } else if (status.equalsIgnoreCase("failure")) {
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
        } else {
            throw new RuntimeException("Rogue component (config) was told to throw an exception (status=[" + status
                + "])");
        }
    }

    public OperationResult invokeOperation(String name, Configuration parameters) {
        PropertySimple sleepProp = (PropertySimple) parameters.get("sleep");
        Long sleep = sleepProp.getLongValue();
        PropertySimple statusProp = (PropertySimple) parameters.get("status");
        String status = statusProp.getStringValue();

        try {
            log.info("The rogue component (op) was told to sleep: " + sleep + "ms");
            Thread.sleep(sleep);
            log.info("The rogue component (op) has finished its sleep of " + sleep + "ms");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        OperationResult result = new OperationResult();

        if (status.equalsIgnoreCase("success")) {
            // do nothing
        } else if (status.equalsIgnoreCase("failure")) {
            result.setErrorMessage("Rogue component was told to fail this operation");
        } else {
            throw new RuntimeException("Rogue component (op) was told to throw an exception (status=[" + status + "])");
        }

        return result;
    }
}