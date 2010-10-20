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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discovery class
 *
 *@author Alexander Kiefer
 */
public class NagiosMonitorDiscovery implements ResourceDiscoveryComponent, ManualAddFacet {
    private final Log log = LogFactory.getLog(this.getClass());

    /**
    * Support manually adding the NagiosMonitor server resource type via Platform's inventory tab
    * @param configuration Configuration data the user passed in from the UI
    * @param resourceDiscoveryContext Discovery context from the plugin container
    * @return Our server type
    * @throws org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException
     */
    public DiscoveredResourceDetails discoverResource(Configuration configuration,
        ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException {

        DiscoveredResourceDetails detail = createNagiosMonitorServerDetail(configuration, resourceDiscoveryContext);
        log.info("Adding NagiosMonitor " + detail);

        return detail;
    }

    /**
     * Don run the auto-discovery for the services below the NagiosMonitor server type.
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        // If we have no parent, it means the NagiosMonitoring server type is not yet up.
        ResourceComponent tmpComponent = discoveryContext.getParentResourceComponent();

        // Temporarily allow autodiscovery of NagiosMonitor server type, while RHQ 4 ui work is going on
        discoveredResources.add(createNagiosMonitorServerDetail(discoveryContext.getDefaultPluginConfiguration(),
            discoveryContext));

        return discoveredResources;
    }

    private DiscoveredResourceDetails createNagiosMonitorServerDetail(Configuration configuration,
        ResourceDiscoveryContext resourceDiscoveryContext) {
        String nagiosHost = configuration.getSimpleValue("nagiosHost", NagiosMonitorComponent.DEFAULT_NAGIOSIP);
        String nagiosPort = configuration.getSimpleValue("nagiosPort", NagiosMonitorComponent.DEFAULT_NAGIOSPORT);

        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(resourceDiscoveryContext.getResourceType(),
            "nagios@" + nagiosHost + ":" + nagiosPort, "Nagios@" + nagiosHost + ":" + nagiosPort, null,
            "Nagios server @ " + nagiosHost + ":" + nagiosPort, configuration, null);
        return detail;
    }
}