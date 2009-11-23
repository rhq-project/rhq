package org.rhq.plugins.iptables;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

public class IptablesTableDiscoveryComponent implements ResourceDiscoveryComponent<IptablesComponent>{

	public String [] tables = {"filter","nat","mangle"};
	
	public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<IptablesComponent> context)
			throws InvalidPluginConfigurationException, Exception {
		
		ResourceType resourceType = context.getResourceType();
		Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();
		
		for (String tableName : tables)
		{
		resources.add(new DiscoveredResourceDetails(resourceType,
                  tableName, tableName, "", "Table",
                  null, null));
		}
		
		return resources;
	}

}
