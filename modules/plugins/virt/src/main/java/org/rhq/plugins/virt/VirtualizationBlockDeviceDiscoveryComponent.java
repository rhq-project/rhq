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
public class VirtualizationBlockDeviceDiscoveryComponent implements ResourceDiscoveryComponent<VirtualizationComponent> {
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<VirtualizationComponent> discoveryContext) throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        Configuration config = discoveryContext.getParentResourceComponent().loadResourceConfiguration();

        PropertyList list = config.getList("disks");
        for (Property p : list.getList()) {
            PropertyMap intf = (PropertyMap) p;

            String device = intf.getSimple("targetDevice").getStringValue();

            DiscoveredResourceDetails detail =
                    new DiscoveredResourceDetails(
                            discoveryContext.getResourceType(),
                            device,
                            device + " virtual device",
                            null,
                            "Virtual block device",
                            null,
                            null);
            details.add(detail);
        }
        return details;
    }
}