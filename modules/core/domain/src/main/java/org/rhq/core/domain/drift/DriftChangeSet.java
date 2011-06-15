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

import java.io.InputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.resource.Resource;

/**
 * @author Jay Shaughnessy
 * @author John Sanda 
 */
@Entity
@NamedQueries( { @NamedQuery(name = DriftChangeSet.QUERY_DELETE_BY_RESOURCES, query = "" //
    + "DELETE FROM DriftChangeSet dcs " //
    + " WHERE dcs.resource.id IN ( :resourceIds )") })
@Table(name = "RHQ_DRIFT_CHANGE_SET")
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_DRIFT_CHANGE_SET_ID_SEQ")
public class DriftChangeSet implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_BY_RESOURCES = "DriftChangeSet.deleteByResources";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "CTIME", nullable = false)
    private Long ctime = -1L;

    // 0..N
    @Column(name = "VERSION", nullable = false)
    private int version;

    @Lob
    @Column(name = "DATA", nullable = true)
    private Blob data;

    @Column(name = "DATA_SIZE", nullable = true)
    private Long dataSize;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Resource resource;

    @OneToMany(mappedBy = "changeSet", cascade = { CascadeType.ALL })
    private Set<Drift> drifts = new LinkedHashSet<Drift>();

    protected DriftChangeSet() {
    }

    public DriftChangeSet(Resource resource, int version) {
        this.resource = resource;
        this.version = version;
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isInitialVersion() {
        return 0 == version;
    }

    public void setDataSize(Long dataSize) {
        this.dataSize = dataSize;
    }

    public Blob getBlob() {
        return data;
    }

    public InputStream getData() throws SQLException {
        return data.getBinaryStream();
    }

    public void setData(Blob blob) {
        this.data = blob;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long size) {
        dataSize = size;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Set<Drift> getDrifts() {
        return drifts;
    }

    public void setDrifts(Set<Drift> drifts) {
        this.drifts = drifts;
    }

    @Override
    public String toString() {
        return "DriftChangeSet [id=" + id + ", resource=" + resource + ", version=" + version + "]";
    }

}
