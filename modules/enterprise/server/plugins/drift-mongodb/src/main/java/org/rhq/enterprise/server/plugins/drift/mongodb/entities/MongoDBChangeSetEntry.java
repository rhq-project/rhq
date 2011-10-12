package org.rhq.enterprise.server.plugins.drift.mongodb.entities;

import java.io.Serializable;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Transient;

import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftCategory;

/**
 * A MongoChangeSetEntry is embedded in a {@link MongoDBChangeSet}. In the database, a change set
 * is stored as a single document, and the document contains an array of its entries.
 */
@Embedded
public class MongoDBChangeSetEntry implements Drift<MongoDBChangeSet, MongoDBFile>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * This id field is only unique within the parent change set. The purpose of this field
     * is to provide fast, efficient access to a file entry a change set document.
     */
    private int id;

    /**
     * The time that the entry was created.
     */
    private Long ctime = System.currentTimeMillis();

    /**
     * The category is one added, removed, or changed.
     */
    private DriftCategory category;

    /**
     * This is the path relative to the base directory defined in the
     * {@link org.rhq.core.domain.drift.DriftDefinition}
     */
    private String path;

    private String directory;

    @Transient
    private MongoDBChangeSet changeSet;

    private MongoDBFile oldFile;

    private MongoDBFile newFile;

    private String oldFileHash;

    private String newFileHash;

    public MongoDBChangeSetEntry() {
    }

    public MongoDBChangeSetEntry(String path, DriftCategory category) {
        this.path = path;
        this.category = category;
    }

    /**
     * Returns an id that uniquely identifies this entry. Since a MongoChangeSetEntry does
     * have a PK in the database, this is a combination of the change set id with the id
     * assigned by the owning change set. The format is:
     * <p/>
     * <pre>
     *     &lt;changeset_id&gt;:&lt;n&gt;
     * </pre>
     * <p/>
     * where <i>n</i> is an integer id assigned by the owning change set that is unique
     * within that change set.
     *
     * @return A unique identifier for the change set entry
     */
    @Override
    public String getId() {
        return changeSet.getId() + ":" + id;
    }

    /**
     * Sets the id for the entry which is assumed to be an integer that is assigned
     * by the owning change set.
     *
     * @param id An integer id that should be unique among other entries within the owning
     * change set.
     */
    @Override
    public void setId(String id) {
        this.id = Integer.parseInt(id);
    }

    /**
     * Returns the change set assigned id. This id is unique across file entries within
     * the owning change set.
     *
     * @return The change set assigned id
     */
    public int getInternalId() {
        return id;
    }

    /**
     * Sets the id for the entry which is assumed to be an integer that is assigned
     * by the owning change set.
     *
     * @param id An integer id that should be unique among other entries within the owning
     * change set.
     */
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public Long getCtime() {
        return ctime;
    }

    @Override
    public MongoDBChangeSet getChangeSet() {
        return changeSet;
    }

    @Override
    public void setChangeSet(MongoDBChangeSet changeSet) {
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
    public String getDirectory() {
        return this.directory;
    }

    @Override
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getOldFileHash() {
        return oldFileHash;
    }

    public void setOldFileHash(String oldFileHash) {
        this.oldFileHash = oldFileHash;
    }

    public String getNewFileHash() {
        return newFileHash;
    }

    public void setNewFileHash(String newFileHash) {
        this.newFileHash = newFileHash;
    }

    @Override
    public MongoDBFile getOldDriftFile() {
        return oldFile;
    }

    @Override
    public void setOldDriftFile(MongoDBFile oldDriftFile) {
        oldFile = oldDriftFile;
    }

    @Override
    public MongoDBFile getNewDriftFile() {
        return newFile;
    }

    @Override
    public void setNewDriftFile(MongoDBFile newDriftFile) {
        newFile = newDriftFile;
    }
}
