package org.rhq.core.domain.drift;

public interface Drift {
    String getId();

    void setId(String id);

    Long getCtime();

    DriftChangeSet getChangeSet();

    void setChangeSet(RhqDriftChangeSet changeSet);

    DriftCategory getCategory();

    void setCategory(DriftCategory category);

    String getPath();

    void setPath(String path);

    DriftFile getOldDriftFile();

    void setOldDriftFile(DriftFile oldDriftFile);

    DriftFile getNewDriftFile();

    void setNewDriftFile(DriftFile newDriftFile);
}
