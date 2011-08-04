package org.rhq.core.domain.drift;

import java.io.Serializable;

import org.rhq.core.domain.resource.Resource;

public class DriftComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private Drift drift;

    private Resource resource;

    public DriftComposite() {
    }

    public DriftComposite(Drift drift, Resource resource) {
        this.drift = drift;
        this.resource = resource;
    }

    public Drift getDrift() {
        return drift;
    }

    public void setDrift(Drift drift) {
        this.drift = drift;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }
}
