package org.rhq.modules.plugins.jbossas7.util;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 *
 */
public abstract class ResourceUtility {

    // TODO: Move this to a utils module.
    public static Resource getChildResource(Resource parent, ResourceType type, String key) {
        for (Resource resource : parent.getChildResources()) {
            if (resource.getResourceType().equals(type) && resource.getResourceKey().equals(key)) {
                return resource;
            }
        }
        return null;
    }

    private ResourceUtility() {
    }

}
