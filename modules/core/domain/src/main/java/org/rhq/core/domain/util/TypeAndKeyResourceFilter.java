package org.rhq.core.domain.util;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 * @since 4.4
 * @author Ian Springer
 */
public class TypeAndKeyResourceFilter implements ResourceFilter {

    private ResourceType resourceType;
    private String resourceKey;

    public TypeAndKeyResourceFilter(ResourceType resourceType, String resourceKey) {
        this.resourceType = resourceType;
        this.resourceKey = resourceKey;
    }

    @Override
    public boolean accept(Resource resource) {
        return (resource.getResourceType().equals(resourceType) && resource.getResourceKey().equals(resourceKey));
    }

}
