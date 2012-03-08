package org.rhq.core.pc.inventory.testplugin;

import java.util.Collections;
import java.util.Set;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * @author Ian Springer
 */
public class TestResourceDiscoveryComponent implements ResourceDiscoveryComponent<ResourceComponent<?>> {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ResourceComponent<?>> discoveryContext)
        throws InvalidPluginConfigurationException, Exception {

        ResourceType resourceType = discoveryContext.getResourceType();
        DiscoveredResourceDetails resourceDetails = new DiscoveredResourceDetails(resourceType,
            "SINGLETON", resourceType.getName(), "1.0", resourceType.getDescription(),
            discoveryContext.getDefaultPluginConfiguration(), null);

        return Collections.singleton(resourceDetails);
    }

}
