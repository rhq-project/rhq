package org.rhq.core.domain.resource.composite;

import java.io.Serializable;

import org.rhq.core.domain.resource.Resource;

public class ResourceWithParentComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Resource parent;
    private final Resource resource;

    public ResourceWithParentComposite(Resource parent, Resource resource) {
        super();
        this.parent = parent;
        this.resource = resource;
    }

    public Resource getParent() {
        return parent;
    }

    public Resource getResource() {
        return resource;
    }
}
