/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.core.domain.drift;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * An occurrence of drifty to be reported and managed by the user.
 
 * @author Jay Shaughnessy
 * @author John Sanda 
 */
@Entity
@NamedQueries( { @NamedQuery(name = RhqDrift.QUERY_DELETE_BY_RESOURCES, query = "" //
    + "DELETE FROM RhqDrift d " //
    + " WHERE d.changeSet IN ( SELECT dcs FROM RhqDriftChangeSet dcs WHERE dcs.resource.id IN ( :resourceIds ) ) )") })
@Table(name = "RHQ_DRIFT")
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_DRIFT_ID_SEQ")
public class RhqDrift implements Serializable, Drift<RhqDriftChangeSet, RhqDriftFile> {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_BY_RESOURCES = "RhqDrift.deleteByResources";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "CTIME", nullable = false)
    private Long ctime = -1L;

    @Column(name = "CATEGORY", nullable = false)
    @Enumerated(EnumType.STRING)
    private DriftCategory category;

    @Column(name = "PATH", nullable = false)
    @Enumerated(EnumType.STRING)
    private String path;

    @JoinColumn(name = "DRIFT_CHANGE_SET_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private RhqDriftChangeSet changeSet;

    @JoinColumn(name = "OLD_DRIFT_FILE", referencedColumnName = "HASH_ID", nullable = true)
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private RhqDriftFile oldDriftFile;

    @JoinColumn(name = "NEW_DRIFT_FILE", referencedColumnName = "HASH_ID", nullable = true)
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private RhqDriftFile newDriftFile;

    protected RhqDrift() {
    }

    /**
     * @param resource
     * @param category
     * @param oldDriftFile required for FILE_CHANGED and FILE_REMOVED, null for FILE_ADDED
     * @param newDriftFile required for FILE_CHANGED and FILE_ADDED, null for FILE_REMOVED
     */
    public RhqDrift(RhqDriftChangeSet changeSet, String path, DriftCategory category, RhqDriftFile oldDriftFile,
                    RhqDriftFile newDriftFile) {
        this.changeSet = changeSet;
        this.path = path;
        this.category = category;
        this.oldDriftFile = oldDriftFile;
        this.newDriftFile = newDriftFile;
    }

    @Override
    public String getId() {
        return Integer.toString(id);
    }

    @Override
    public void setId(String id) {
        this.id = Integer.parseInt(id);
    }

    @Override
    public Long getCtime() {
        return ctime;
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
    }

    @Override
    public RhqDriftChangeSet getChangeSet() {
        return changeSet;
    }

    @Override
    public void setChangeSet(RhqDriftChangeSet changeSet) {
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
    public RhqDriftFile getOldDriftFile() {
        return oldDriftFile;
    }

    @Override
    public void setOldDriftFile(RhqDriftFile oldDriftFile) {
        this.oldDriftFile = oldDriftFile;
    }

    @Override
    public RhqDriftFile getNewDriftFile() {
        return newDriftFile;
    }

    @Override
    public void setNewDriftFile(RhqDriftFile newDriftFile) {
        this.newDriftFile = newDriftFile;
    }

    @Override
    public String toString() {
        return "RhqDrift [ id=" + id + ", category=" + category + ", path=" + path + ", changeSet=" + changeSet + "]";
    }

}
