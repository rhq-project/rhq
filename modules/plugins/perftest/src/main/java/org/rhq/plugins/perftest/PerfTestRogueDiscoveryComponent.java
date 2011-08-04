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

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.perftest.resource.ResourceFactory;

@SuppressWarnings("unchecked")
public class PerfTestRogueDiscoveryComponent implements ResourceDiscoveryComponent {
    private Log log = LogFactory.getLog(PerfTestRogueDiscoveryComponent.class);

    private static final String SYSPROP_DISCOVERY = "rhq.perftest.server-rogue-discovery";
    private static final String SYSPROP_DISCOVERY_INT = "rhq.perftest.server-rogue-discovery-interruptable";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context)
        throws InvalidPluginConfigurationException, Exception {
        ResourceType resourceType = context.getResourceType();

        ScenarioManager manager = ScenarioManager.getInstance();
        Set<DiscoveredResourceDetails> allResourceDetails = null;
        if (manager.isEnabled()) {
            ResourceFactory resourceFactory = manager.getResourceFactory(resourceType.getName());
            allResourceDetails = resourceFactory.discoverResources(context);

            String value = System.getProperty(SYSPROP_DISCOVERY);
            if (value != null) {
                if (value.equalsIgnoreCase("error")) {
                    throw new Exception("The rogue discovery component was configured to throw this exception");
                }
                try {
                    long ms = Long.parseLong(value); // throws number format exception if not a number
                    log.info("The rogue discovery component was told to sleep: " + ms + "ms");
                    sleep(ms);
                    log.info("The rogue discovery component has finished its sleep of " + ms + "ms");
                } catch (Exception e) {
                    throw new InvalidPluginConfigurationException(
                        "The rogue discovery component was configured to throw this invalid config exception", e);
                }
            } else {
                log.info("The rogue discovery component was not configured to do anything bad - returning normally");
            }
        }

        return allResourceDetails;
    }

    /**
     * Ensure we sleep for the full amount of millis, even if interrupted.
     * If the thread is interrupted, but our sysprop {@link #SYSPROP_DISCOVERY_INT} is set
     * to true, this method will abort.
     * 
     * @param ms millis to sleep
     */
    private void sleep(long ms) {
        long start = System.currentTimeMillis();
        long finish = start + ms;

        while (System.currentTimeMillis() < finish) {
            try {
                Thread.sleep(finish - System.currentTimeMillis());
            } catch (InterruptedException e) {
                log.warn("The rogue discovery component was interrupted during its sleep", e);
                if (Boolean.getBoolean(SYSPROP_DISCOVERY_INT)) {
                    log.warn("The rogue discovery component will abort its sleep due to the interrupt");
                    return;
                }
            }
        }
    }
}