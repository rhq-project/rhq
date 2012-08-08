package org.rhq.core.pc.inventory.testplugin;

import java.util.Collections;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

public class ManualAddDiscoveryComponent implements ResourceDiscoveryComponent<ResourceComponent<?>>,
    ManualAddFacet<ResourceComponent<?>> {

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
        ResourceDiscoveryContext<ResourceComponent<?>> context) throws InvalidPluginConfigurationException {

        ResourceType resourceType = context.getResourceType();

        return new DiscoveredResourceDetails(resourceType, "SINGLETON", resourceType.getName(), "1.0",
            resourceType.getDescription(), pluginConfiguration, null);
    }

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ResourceComponent<?>> context)
        throws InvalidPluginConfigurationException, Exception {
        return Collections.emptySet();
    }

}
