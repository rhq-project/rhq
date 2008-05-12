/*
 * JBoss, a division of Red Hat.
 * Copyright 2008, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.netservices;

import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.domain.configuration.Configuration;

import java.util.Set;
import java.util.Collections;
import java.net.URL;

/**
 * @author Greg Hinkle
 */
public class PingNetServiceDiscoveryComponent implements ResourceDiscoveryComponent {
    public Set discoverResources(ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException, Exception {

        if (resourceDiscoveryContext.getPluginConfigurations().size() > 0) {
            Configuration config = (Configuration) resourceDiscoveryContext.getPluginConfigurations().get(0);
            String address = config.getSimple(PingNetServiceComponent.CONFIG_ADDRESS).getStringValue();

            DiscoveredResourceDetails details =
                    new DiscoveredResourceDetails(
                            resourceDiscoveryContext.getResourceType(),
                            address,
                            "Ping " + address,
                            null,
                            null,
                            config,
                            null);


            return Collections.singleton(details);
        }
        return null;
    }
}