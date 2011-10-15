package org.rhq.core.domain.drift.dto;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;

public class DriftChangeSetDTO implements DriftChangeSet<DriftDTO>, Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private Long ctime;

    private int version;

    private DriftChangeSetCategory category;

    private int configId;

    private DriftHandlingMode driftHandlingMode;

    private int resourceId;

    private Set<DriftDTO> drifts = new HashSet<DriftDTO>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Long getCtime() {
        return ctime;
    }

    public void setCtime(Long ctime) {
        this.ctime = ctime;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public DriftChangeSetCategory getCategory() {
        return category;
    }

    @Override
    public void setCategory(DriftChangeSetCategory category) {
        this.category = category;
    }

    @Override
    public int getDriftDefinitionId() {
        return configId;
    }

    @Override
    public void setDriftDefinitionId(int id) {
        configId = id;
    }

    @Override
    public int getResourceId() {
        return resourceId;
    }

    @Override
    public void setResourceId(int id) {
        resourceId = id;
    }

    @Override
    public Set<DriftDTO> getDrifts() {
        return drifts;
    }

    @Override
    public void setDrifts(Set<DriftDTO> drifts) {
        this.drifts = drifts;
    }

    @Override
    public DriftHandlingMode getDriftHandlingMode() {
        return this.driftHandlingMode;
    }

    @Override
    public void setDriftHandlingMode(DriftHandlingMode mode) {
        this.driftHandlingMode = mode;
    }
}
