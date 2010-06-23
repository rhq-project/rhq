package org.rhq.core.pc.upgrade;

import java.util.Set;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * This class is used to store the requests for resource upgrade that originate in 
 * the discovery workflow, but can only be executed after an inventory sync. 
 */
public class ResourceUpgradePendingRequest<T extends ResourceComponent> {
    private Set<Resource> discoveredResources;
    private ResourceDiscoveryContext<T> discoveryContext;
    private Integer parentResourceId;
    
    public ResourceUpgradePendingRequest(Set<Resource> discoveredResources, ResourceDiscoveryContext<T> discoveryContext, Integer parentResourceId) {
        this.discoveredResources = discoveredResources;
        this.discoveryContext = discoveryContext;
        this.parentResourceId = parentResourceId;
    }

    public Set<Resource> getDiscoveredResources() {
        return discoveredResources;
    }

    public ResourceDiscoveryContext<T> getDiscoveryContext() {
        return discoveryContext;
    }
    
    public Integer getParentResourceId() {
        return parentResourceId;
    }
}