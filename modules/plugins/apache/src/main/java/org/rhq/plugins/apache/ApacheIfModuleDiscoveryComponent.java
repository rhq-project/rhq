package org.rhq.plugins.apache;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.apache.util.AugeasNodeSearch;

public class ApacheIfModuleDiscoveryComponent  implements ResourceDiscoveryComponent<ApacheVirtualHostServiceComponent> {

    private static final String [] parentRes = {"<IfModule"};
    private static final String IFMODULE_NODE_NAME = "<IfModule";
    private AugeasTree tree;
    private AugeasNode parentNode;
    
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ApacheVirtualHostServiceComponent> context)
        throws InvalidPluginConfigurationException, Exception {
   
    ApacheVirtualHostServiceComponent virtualHost = context.getParentResourceComponent();
    tree = virtualHost.getServerConfigurationTree();
    parentNode = virtualHost.getNode(tree);
    
    List<AugeasNode> ifModuleNodes = AugeasNodeSearch.searchNode(parentRes, IFMODULE_NODE_NAME, parentNode);
    
    Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();
    ResourceType resourceType = context.getResourceType();

    for (AugeasNode node : ifModuleNodes) {
        
        String resourceKey = AugeasNodeSearch.getParamsString(node,parentNode);
        
        int separatorPosition = resourceKey.indexOf(";");
        String resourceName = null;
        
        if (separatorPosition!=-1)
         resourceName = resourceKey.substring(0,separatorPosition);
        else
         resourceName = resourceKey.toString();

        discoveredResources.add(new DiscoveredResourceDetails(resourceType, resourceKey, resourceName, null, null,
        null, null));
        }
    return discoveredResources;
   }
}