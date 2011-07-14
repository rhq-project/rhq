package org.rhq.plugins.pattern;

import java.util.HashSet;
import java.util.Set;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;


/**
 * Discovery class
 */
@SuppressWarnings("unused")
public class PatternDiscovery implements ResourceDiscoveryComponent
{

    /**
     * Run the auto-discovery
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(1);


        DiscoveredResourceDetails detail =  new DiscoveredResourceDetails(
            discoveryContext.getResourceType(), // ResourceType
                "pattern",
                "pattern",
                "1.1",
                "Generate metrics that follow a pattern",
                discoveryContext.getDefaultPluginConfiguration(),
                null
        );


        // Add to return values
        discoveredResources.add(detail);
        return discoveredResources;
    }
}