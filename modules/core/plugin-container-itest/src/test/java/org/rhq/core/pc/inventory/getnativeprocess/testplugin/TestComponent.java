/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.core.pc.inventory.getnativeprocess.testplugin;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.system.ProcessInfo;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class TestComponent implements ResourceComponent<ResourceComponent<?>>, ConfigurationFacet {

    public static final AtomicInteger DISCOVERY_CALLS_COUNT = new AtomicInteger();
    
    public static final String DISCOVERY_CALL_COUNT_PROP = "nof-discovery-calls";
    public static final String CURRENT_PID_PROP = "current-pid";
    
    private static final Log LOG = LogFactory.getLog(TestComponent.class);
    
    private ResourceContext<ResourceComponent<?>> context;
    
    @Override
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    @Override
    public void start(ResourceContext<ResourceComponent<?>> context) throws InvalidPluginConfigurationException,
        Exception {
        
        this.context = context;
    }

    @Override
    public void stop() {
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration config = new Configuration();
        
        ProcessInfo pinfo = context.getNativeProcess();
        
        int pid = pinfo == null ? 0 : (int) context.getNativeProcess().getPid();

        LOG.debug("PID = " + pid);
        
        config.put(new PropertySimple(DISCOVERY_CALL_COUNT_PROP, DISCOVERY_CALLS_COUNT.get()));
        config.put(new PropertySimple(CURRENT_PID_PROP, Integer.valueOf(pid)));
        
        return config;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
    }
}
