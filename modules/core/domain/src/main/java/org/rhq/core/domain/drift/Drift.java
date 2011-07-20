package org.rhq.core.domain.drift;

public interface Drift<C extends DriftChangeSet, F extends DriftFile> {
    String getId();

    void setId(String id);

    Long getCtime();

    C getChangeSet();

    void setChangeSet(C changeSet);

    DriftCategory getCategory();

    void setCategory(DriftCategory category);

    String getPath();

    void setPath(String path);

    F getOldDriftFile();

    void setOldDriftFile(F oldDriftFile);

    F getNewDriftFile();

    void setNewDriftFile(F newDriftFile);
}
