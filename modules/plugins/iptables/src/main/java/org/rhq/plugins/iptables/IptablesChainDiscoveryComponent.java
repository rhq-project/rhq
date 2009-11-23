package org.rhq.plugins.iptables;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

public class IptablesChainDiscoveryComponent implements ResourceDiscoveryComponent<IptablesTableComponent>{
	private final Log log = LogFactory.getLog(this.getClass());
	
	public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<IptablesTableComponent> context)
			throws InvalidPluginConfigurationException, Exception {
		
		     AugeasTree tree = context.getParentResourceComponent().getAugeasTree();
			 String tableName = context.getParentResourceComponent().getTableName();
		
			 List<AugeasNode> nodes = tree.matchRelative(tree.getRootNode(), File.separatorChar+tableName+File.separatorChar+"settings"+File.separatorChar+"chain");
			 ResourceType resourceType = context.getResourceType();
			
			 Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();
		   
			for (AugeasNode nd : nodes){
				String value = nd.getValue();
				resources.add(new DiscoveredResourceDetails(resourceType,value, value, "", "Chain", null, null));
		    }
		
		return resources;
	}

}
