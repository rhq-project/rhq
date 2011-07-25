package org.rhq.enterprise.server.plugins.drift.mongodb.entities;

import java.io.Serializable;

import com.google.code.morphia.annotations.Embedded;

import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftCategory;

@Embedded
public class MongoDBChangeSetEntry implements Drift<MongoDBChangeSet, MongoDBFile>, Serializable {

    private static final long serialVersionUID = 1L;

    private Long ctime = System.currentTimeMillis();

    private DriftCategory category;

    private String path;

    public MongoDBChangeSetEntry() {
    }

    public MongoDBChangeSetEntry(String path, DriftCategory category) {
        this.path = path;
        this.category = category;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public void setId(String id) {
    }

    @Override
    public Long getCtime() {
        return null;
    }

    @Override
    public MongoDBChangeSet getChangeSet() {
        return null;
    }

    @Override
    public void setChangeSet(MongoDBChangeSet changeSet) {
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
    public MongoDBFile getOldDriftFile() {
        return null;
    }

    @Override
    public void setOldDriftFile(MongoDBFile oldDriftFile) {
    }

    @Override
    public MongoDBFile getNewDriftFile() {
        return null;
    }

    @Override
    public void setNewDriftFile(MongoDBFile newDriftFile) {
    }
}
