package org.rhq.core.domain.drift;

import java.util.Set;

import org.rhq.core.domain.resource.Resource;

public interface DriftChangeSet<D extends Drift> {
    String getId();

    void setId(String id);

    Long getCtime();

    int getVersion();

    void setVersion(int version);

    DriftChangeSetCategory getCategory();

    void setCategory(DriftChangeSetCategory category);

    int getDriftConfigurationId();

    void setDriftConfigurationId(int id);

    Resource getResource();

    void setResource(Resource resource);

    Set<D> getDrifts();

    void setDrifts(Set<D> drifts);
}
