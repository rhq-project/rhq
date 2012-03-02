package org.rhq.enterprise.server.plugins.drift.mongodb.entities;

import java.io.Serializable;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.annotations.Transient;

import org.bson.types.ObjectId;
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
     * This is the array index of the entry in the document that exists in the database.
     * Each entry has a unique index relative to its owning change set. The index is used
     * to form a unique id for the entry.
     */
    @Property("idx")
    private int index;

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
    
    @Transient
    private ObjectId changeSetId;

    private String oldFileHash;

    private String newFileHash;

    public MongoDBChangeSetEntry() {
    }

    public MongoDBChangeSetEntry(String path, DriftCategory category) {
        this.path = path;
        int i = path.lastIndexOf("/");
        directory = (i != -1) ? path.substring(0, i) : "./";
        this.category = category;
    }

    /**
     * Returns an id that uniquely identifies this entry. Since a MongoChangeSetEntry does
     * not have a PK in the database, this is a combination of the change set id with its
     * index which is assigned by the owning change set. The format is:
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
        if (changeSetId == null) {
            return null;
        }
        return changeSetId.toString() + ":" + index;
    }

    /**
     * This method does <strong>not</strong> actually set the id. It is here
     * only because it is required by the {@link Drift} interface.
     * @param id
     */
    @Override
    public void setId(String id) {        
    }

    /**
     * Sets the index of the entry which is the array index within the document stored in the
     * database. The index is used to form a unique id for the entry. 
     *
     * @param index The array index of the entry as it is stored in the change set document
     * in the database.
     */
    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public Long getCtime() {
        return ctime;
    }

    /**
     * This is here only for testing.
     * @param ctime The timestamp
     */
    public void setCtime(Long ctime) {
        this.ctime = ctime;
    }

    @Override
    public MongoDBChangeSet getChangeSet() {
        return changeSet;
    }

    @Override
    public void setChangeSet(MongoDBChangeSet changeSet) {
        this.changeSet = changeSet;
        changeSetId = changeSet.getObjectId();
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
        int i = path.lastIndexOf("/");
        directory = (i != -1) ? path.substring(0, i) : "./";
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
        return new MongoDBFile(oldFileHash);
    }

    @Override
    public void setOldDriftFile(MongoDBFile oldDriftFile) {
        oldFileHash = oldDriftFile.getHashId();
    }

    @Override
    public MongoDBFile getNewDriftFile() {
        return new MongoDBFile(newFileHash);
    }

    @Override
    public void setNewDriftFile(MongoDBFile newDriftFile) {
        newFileHash = newDriftFile.getHashId();
    }
}
