package org.rhq.plugins.test;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

public class SingleResourceDiscoveryComponent implements ResourceDiscoveryComponent<ResourceComponent<?>> {
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ResourceComponent<?>> context)
        throws InvalidPluginConfigurationException, Exception {

        HashSet<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>(1);
        ResourceType rt = context.getResourceType();
        String key = "SingleResourceKey";
        String name = "SingleResourceName";
        String version = "1";
        Configuration pc = context.getDefaultPluginConfiguration();
        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(rt, key, name, version, null, pc, null);
        details.add(resource);

        return details;
    }
}