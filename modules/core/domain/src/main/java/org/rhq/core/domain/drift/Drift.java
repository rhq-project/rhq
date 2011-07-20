package org.rhq.core.domain.drift;

public interface Drift<C extends DriftChangeSet> {
    String getId();

    void setId(String id);

    Long getCtime();

    C getChangeSet();

    void setChangeSet(C changeSet);

    DriftCategory getCategory();

    void setCategory(DriftCategory category);

    String getPath();

    void setPath(String path);

    DriftFile getOldDriftFile();

    void setOldDriftFile(DriftFile oldDriftFile);

    DriftFile getNewDriftFile();

    void setNewDriftFile(DriftFile newDriftFile);
}
