/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.cloud.StorageNode;

/**
 * A Storage Maintenance Job
 *
 * @author Stefan Negrea
 */
@Entity(name = "MaintenanceJob")
@NamedQueries( //
{
 @NamedQuery(name = MaintenanceJob.QUERY_FIND_ALL, query = "SELECT s FROM MaintenanceJob s")
})
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_STORAGE_MAINT_JOB_ID_SEQ", sequenceName = "RHQ_STORAGE_MAINT_JOB_ID_SEQ")
@Table(name = "RHQ_STORAGE_MAINT_JOB")
public class MaintenanceJob implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "MaintenanceJob.findAll";

    public static enum Type {
        DEPLOY,
        UNDEPLOY,
        CHANGE_ENDPOINT,
        CHANGE_RHQ_DATA_DIR
    }

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_STORAGE_MAINT_JOB_ID_SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "TYPE", nullable = false)
    private Type type;

    // I think that most jobs will have a node associated with them; however, there might
    // be some, like anti-entropy repair, that would not have a particular node but rather
    // the whole cluster. That is this relationship is nullable.
    @ManyToOne
    @JoinColumn(name = "STORAGE_NODE_ID", nullable = true)
    private StorageNode storageNode;

    @OneToMany(mappedBy = "maintenanceJob", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    @OrderBy("step")
    private List<MaintenanceStep> steps = new ArrayList<MaintenanceStep>();

    // the time this maintenance workflow was created
    @Column(name = "CTIME", nullable = false)
    private long ctime;

    // the time this maintenance workflow was last updated
    @Column(name = "MTIME", nullable = false)
    private long mtime;

    // required for JPA
    public MaintenanceJob() {
    }

    public int getId() {
        return id;
    }

    public MaintenanceJob setId(int id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public MaintenanceJob setName(String name) {
        this.name = name;
        return this;
    }

    public Type getType() {
        return type;
    }

    public MaintenanceJob setType(Type type) {
        this.type = type;
        return this;
    }

    public List<MaintenanceStep> getMaintenanceSteps() {
        return steps;
    }

    public MaintenanceJob setSteps(List<MaintenanceStep> steps) {
        this.steps = steps;
        return this;
    }

    public StorageNode getStorageNode() {
        return storageNode;
    }

    public void setStorageNode(StorageNode storageNode) {
        this.storageNode = storageNode;
    }

    public int getMaintenanceStepCount() {
        return this.steps.size();
    }

    public long getCtime() {
        return ctime;
    }

    public long getMtime() {
        return mtime;
    }

    public MaintenanceJob setMtime(long mtime) {
        this.mtime = mtime;
        return this;
    }

    @Override
    public String toString() {
        return "MaintenanceJob[id=" + id + ", name=" + name + ", type=" + type
            + ", ctime=" + ctime + "]";
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
        this.mtime = this.ctime;
    }

    // TODO Determine the fields we should include in equals/hashCode
    //
    // I think that name and ctime will form a natural/surrogate key, but I am
    // not 100% certain.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MaintenanceJob that = (MaintenanceJob) o;

        if (ctime != that.ctime) return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (int) (ctime ^ (ctime >>> 32));
        return result;
    }

}
