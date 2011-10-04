/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.plugins.drift.mongodb.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.PostLoad;

import org.bson.types.ObjectId;

import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;

/**
 * A change set that is stored in a MongoDB database. The change set along with its entries
 * are stored as a single document.
 */
@Entity("changesets")
public class MongoDBChangeSet implements DriftChangeSet<MongoDBChangeSetEntry>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The database primary key. This is auto-generated.
     */
    @Id
    private ObjectId id;

    /**
     * The time that the change set was created.
     */
    private Long ctime = System.currentTimeMillis();

    /**
     * Each change set is assigned a version number that is unique across change sets
     * belonging to a given drift definition (and corresponding resource).
     */
    private int version;

    private DriftChangeSetCategory category;

    private int configId;

    private DriftHandlingMode driftHandlingMode;

    private int resourceId;

    @Embedded("files")
    private List<MongoDBChangeSetEntry> entries = new ArrayList<MongoDBChangeSetEntry>();

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
    public int getDriftDefinitionId() {
        return configId;
    }

    @Override
    public void setDriftDefinitionId(int id) {
        configId = id;
    }

    @Override
    public DriftHandlingMode getDriftHandlingMode() {
        return this.driftHandlingMode;
    }

    @Override
    public void setDriftHandlingMode(DriftHandlingMode driftHandlingMode) {
        this.driftHandlingMode = driftHandlingMode;
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
        return new LinkedHashSet(entries);
    }

    public MongoDBChangeSet add(MongoDBChangeSetEntry entry) {
        entries.add(entry);
        entry.setId(entries.size() - 1);
        entry.setChangeSet(this);
        return this;
    }

    @Override
    public void setDrifts(Set<MongoDBChangeSetEntry> drifts) {
        for (MongoDBChangeSetEntry entry : drifts) {
            add(entry);
        }
    }

    @PostLoad
    void initEntries() {
        for (MongoDBChangeSetEntry entry : entries) {
            entry.setChangeSet(this);
        }
    }
}
