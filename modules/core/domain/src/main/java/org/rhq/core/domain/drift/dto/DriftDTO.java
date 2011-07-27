package org.rhq.core.domain.drift.dto;

import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftCategory;

public class DriftDTO implements Drift<DriftChangeSetDTO, DriftFileDTO> {

    private String id;

    private Long ctime;

    private DriftChangeSetDTO changeSet;

    private DriftCategory category;

    private String path;

    private DriftFileDTO oldDriftFile;

    private DriftFileDTO newDriftFile;

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
    public DriftChangeSetDTO getChangeSet() {
        return changeSet;
    }

    @Override
    public void setChangeSet(DriftChangeSetDTO changeSet) {
        this.changeSet = changeSet;
    }

    @Override
    public DriftCategory getCategory() {
        return category;
    }

    @Override
    public void setCategory(DriftCategory category) {
        this.category = category;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public DriftFileDTO getOldDriftFile() {
        return oldDriftFile;
    }

    @Override
    public void setOldDriftFile(DriftFileDTO oldDriftFile) {
        this.oldDriftFile = oldDriftFile;
    }

    @Override
    public DriftFileDTO getNewDriftFile() {
        return newDriftFile;
    }

    @Override
    public void setNewDriftFile(DriftFileDTO newDriftFile) {
        this.newDriftFile = newDriftFile;
    }
}
