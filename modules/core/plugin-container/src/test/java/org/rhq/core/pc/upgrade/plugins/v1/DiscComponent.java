package org.rhq.core.pc.upgrade.plugins.v1;

import java.util.Collections;
import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

public class DiscComponent implements ResourceDiscoveryComponent<ResourceComponent> {
    private static final String RESOURCE_KEY = "resource-key-v1";
    private static final String RESOURCE_NAME = "resource-name-v1";
    private static final String RESOURCE_DESCRIPTION = "resource-description-v1";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ResourceComponent> context)
        throws InvalidPluginConfigurationException, Exception {
        return Collections.singleton(new DiscoveredResourceDetails(context.getResourceType(), RESOURCE_KEY,
            RESOURCE_NAME, null, RESOURCE_DESCRIPTION, context.getDefaultPluginConfiguration(), null));
    }
}
