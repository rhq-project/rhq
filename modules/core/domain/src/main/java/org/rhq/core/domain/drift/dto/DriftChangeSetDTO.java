package org.rhq.core.domain.drift.dto;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftChangeSetCategory;

public class DriftChangeSetDTO implements DriftChangeSet<DriftDTO> {

    private String id;

    private Long ctime;

    private int version;

    private DriftChangeSetCategory category;

    private int configId;

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
    public int getDriftConfigurationId() {
        return configId;
    }

    @Override
    public void setDriftConfigurationId(int id) {
        configId = id;
    }

    @Override
    public int getResourceId() {
        return resourceId;
    }

    @Override
    public Set<DriftDTO> getDrifts() {
        return drifts;
    }

    @Override
    public void setDrifts(Set<DriftDTO> drifts) {
        this.drifts = drifts;
    }
}
