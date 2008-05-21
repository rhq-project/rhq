/*
 * JBoss, a division of Red Hat.
 * Copyright 2008, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.virt;

import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;

import java.util.Set;
import java.util.HashSet;

/**
 * @author Greg Hinkle
 */
public class VirtualizationInterfaceDiscoveryComponent implements ResourceDiscoveryComponent<VirtualizationComponent> {
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<VirtualizationComponent> discoveryContext) throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        Configuration config = discoveryContext.getParentResourceComponent().loadResourceConfiguration();

        PropertyList list = config.getList("interfaces");
        for (Property p : list.getList()) {
            PropertyMap intf = (PropertyMap) p;

            String path = intf.getSimple("target").getStringValue();

            DiscoveredResourceDetails detail =
                    new DiscoveredResourceDetails(
                            discoveryContext.getResourceType(),
                            path,
                            path + " virtual interface",
                            null,
                            "Virtual network interface",
                            null,
                            null);
            details.add(detail);
        }
        return details;
    }
}
