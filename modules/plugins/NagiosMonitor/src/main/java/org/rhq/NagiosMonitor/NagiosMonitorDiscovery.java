package org.rhq.NagiosMonitor;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;


/**
 * Discovery class
 */
public class NagiosMonitorDiscovery implements ResourceDiscoveryComponent
{
    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Run the auto-discovery
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
    	Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
            
        	discoveryContext.getResourceType(),
            "nagiosMonitorPluginKey",
            "nagiosMonitorPluginName",
            null,
            "This plugin communicates with Nagios and fills some defined metrics",
            null,
            null
        );

        // Add to return values
        discoveredResources.add(detail);
        log.info("Discovered new NagiosMonitor Ressource");

        return discoveredResources;
    }
}