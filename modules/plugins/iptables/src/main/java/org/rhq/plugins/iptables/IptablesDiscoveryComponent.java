package org.rhq.plugins.iptables;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.rhqtransform.RhqConfig;

public class IptablesDiscoveryComponent implements ResourceDiscoveryComponent {

	public Set discoverResources(ResourceDiscoveryContext discoveryContext)
			throws InvalidPluginConfigurationException, Exception {
		
		Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(1);
		 
		 Configuration pluginConfiguration = discoveryContext.getDefaultPluginConfiguration();

		 RhqConfig config = new RhqConfig(pluginConfiguration);
		 
		  Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
		   DiscoveredResourceDetails resource =
               new DiscoveredResourceDetails(discoveryContext.getResourceType(), "iptables", "IPTABLES",
                   "", "IPTABLES.", pluginConfiguration, null);

           details.add(resource);
           return details;
	}



}
