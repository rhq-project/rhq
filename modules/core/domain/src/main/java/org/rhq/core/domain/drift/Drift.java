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
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.resource.Resource;

/**
 * An occurrence of drifty to be reported and managed by the user.
 
 * @author Jay Shaughnessy
 * @author John Sanda 
 */
@Entity
@Table(name = "RHQ_DRIFT")
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_DRIFT_ID_SEQ")
public class Drift implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "CTIME", nullable = false)
    private Long ctime = -1L;

    @Column(name = "CATEGORY", nullable = false)
    @Enumerated(EnumType.STRING)
    private DriftCategory category;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Resource resource;

    @JoinColumn(name = "OLD_DRIFT_FILE", referencedColumnName = "SHA256", nullable = true)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    private DriftFile oldDriftFile;

    @JoinColumn(name = "NEW_DRIFT_FILE", referencedColumnName = "SHA256", nullable = true)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    private DriftFile newDriftFile;

    protected Drift() {
    }

    /**
     * @param resource
     * @param category
     * @param oldDriftFile required for FILE_CHANGED and FILE_REMOVED, null for FILE_ADDED
     * @param newDriftFile required for FILE_CHANGED and FILE_ADDED, null for FILE_REMOVED
     */
    public Drift(Resource resource, DriftCategory category, DriftFile oldDriftFile, DriftFile newDriftFile) {
        this.resource = resource;
        this.category = category;
        this.oldDriftFile = oldDriftFile;
        this.newDriftFile = newDriftFile;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Long getCtime() {
        return ctime;
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public DriftCategory getCategory() {
        return category;
    }

    public void setCategory(DriftCategory category) {
        this.category = category;
    }

    public DriftFile getOldDriftFile() {
        return oldDriftFile;
    }

    public void setOldDriftFile(DriftFile oldDriftFile) {
        this.oldDriftFile = oldDriftFile;
    }

    public DriftFile getNewDriftFile() {
        return newDriftFile;
    }

    public void setNewDriftFile(DriftFile newDriftFile) {
        this.newDriftFile = newDriftFile;
    }

}
