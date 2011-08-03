package org.rhq.enterprise.server.plugins.drift.mongodb.entities;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.PostLoad;

import org.bson.types.ObjectId;

import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftChangeSetCategory;

@Entity("changesets")
public class MongoDBChangeSet implements DriftChangeSet<MongoDBChangeSetEntry>, Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private ObjectId id;

    private Long ctime = System.currentTimeMillis();

    private int version;

    private DriftChangeSetCategory category;

    private int configId;

    private int resourceId;

    @Embedded("files")
    private Set<MongoDBChangeSetEntry> entries = new HashSet<MongoDBChangeSetEntry>();

    public ObjectId getObjectId() {
        return id;
    }

    @Override
    public String getId() {
        return id.toString();
    }

    @Override
    public void setId(String id) {
        this.id = new ObjectId(id);
    }

    public void setId(ObjectId id) {
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

    public void setResourceId(int id) {
        resourceId = id;
    }

    @Override
    public Set<MongoDBChangeSetEntry> getDrifts() {
        return entries;
    }

    public MongoDBChangeSet add(MongoDBChangeSetEntry entry) {
        entries.add(entry);
        entry.setChangeSet(this);
        return this;
    }

    @Override
    public void setDrifts(Set<MongoDBChangeSetEntry> drifts) {
        entries = drifts;
    }

    @PostLoad
    void initEntries() {
        for (MongoDBChangeSetEntry entry : entries) {
            entry.setChangeSet(this);
        }
    }
}
