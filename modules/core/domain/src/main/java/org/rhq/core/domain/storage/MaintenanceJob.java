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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

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

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_STORAGE_MAINT_JOB_ID_SEQ")
    @Id
    private int id;

    //TODO: might have to drop if type is enough
    @Column(name = "NAME", nullable = false)
    private String name;

    //TODO: might have to change to Enum if the types settle to
    //      a finite few
    @Column(name = "TYPE", nullable = false)
    private int type;

    @OneToMany(mappedBy = "maintenanceJob", fetch = FetchType.LAZY)
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

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<MaintenanceStep> getMaintenanceSteps() {
        return steps;
    }

    public void setAgents(List<MaintenanceStep> steps) {
        this.steps = steps;
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

    public void setMtime(long mtime) {
        this.mtime = mtime;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode()) + type;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof MaintenanceJob)) {
            return false;
        }

        final MaintenanceJob other = (MaintenanceJob) obj;


        if (getId() != other.getId() || getCtime() != other.getCtime()) {
            return false;
        }

        return true;
    }
}
