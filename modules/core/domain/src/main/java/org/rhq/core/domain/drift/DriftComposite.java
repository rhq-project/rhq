package org.rhq.core.domain.drift;

import java.io.Serializable;

import org.rhq.core.domain.resource.Resource;

public class DriftComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private Drift<?, ?> drift;

    private Resource resource;

    private String driftDefName;

    public DriftComposite() {
    }

    public DriftComposite(Drift<?, ?> drift, Resource resource, String driftDefName) {
        this.drift = drift;
        this.resource = resource;
        this.driftDefName = driftDefName;
    }

    public Drift<?, ?> getDrift() {
        return drift;
    }

    public void setDrift(Drift<?, ?> drift) {
        this.drift = drift;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public String getDriftDefinitionName() {
        return driftDefName;
    }

    public void setDriftDefName(String driftDefName) {
        this.driftDefName = driftDefName;
    }

}
