package org.rhq.plugins.apache.augeasApache;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

public class ServerDiscoveryComponent implements ResourceDiscoveryComponent {

    public Set discoverResources(ResourceDiscoveryContext discoveryContext)
                  throws InvalidPluginConfigurationException, Exception {
           
  
            Configuration pluginConfiguration = discoveryContext.getDefaultPluginConfiguration();
 
             Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
              DiscoveredResourceDetails resource =
            new DiscoveredResourceDetails(discoveryContext.getResourceType(), "httpd", "HTTPD",
                "", "HTTPD", pluginConfiguration, null);

        details.add(resource);
        return details;
    }



}
