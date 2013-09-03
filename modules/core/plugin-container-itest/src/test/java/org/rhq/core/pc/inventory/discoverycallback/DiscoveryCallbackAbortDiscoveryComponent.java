package org.rhq.core.pc.inventory.discoverycallback;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DiscoveryCallbackAbortDiscoveryComponent implements ResourceDiscoveryComponent<ResourceComponent<?>> {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ResourceComponent<?>> discoveryContext)
        throws InvalidPluginConfigurationException, Exception {

        ResourceType resourceType = discoveryContext.getResourceType();

        DiscoveredResourceDetails resourceDetails1 = new DiscoveredResourceDetails(resourceType,
                "key-to-be-aborted", resourceType.getName(), "1.0", resourceType.getDescription(),
                discoveryContext.getDefaultPluginConfiguration(), null);

        DiscoveredResourceDetails resourceDetails2 = new DiscoveredResourceDetails(resourceType,
                "key-ok", resourceType.getName(), "1.0", resourceType.getDescription(),
                discoveryContext.getDefaultPluginConfiguration(), null);

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>(2);
        Collections.addAll(details, resourceDetails1, resourceDetails2);
        return details;
    }
}
