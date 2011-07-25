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
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import org.rhq.core.domain.resource.Resource;

/**
 * @author Jay Shaughnessy
 * @author John Sanda 
 */
@Entity
@NamedQueries( { @NamedQuery(name = RhqDriftChangeSet.QUERY_DELETE_BY_RESOURCES, query = "" //
    + "DELETE FROM RhqDriftChangeSet dcs " //
    + " WHERE dcs.resource.id IN ( :resourceIds )") })
@Table(name = "RHQ_DRIFT_CHANGE_SET")
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_DRIFT_CHANGE_SET_ID_SEQ")
public class RhqDriftChangeSet implements Serializable, DriftChangeSet<RhqDrift> {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_BY_RESOURCES = "RhqDriftChangeSet.deleteByResources";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "CTIME", nullable = false)
    private Long ctime = -1L;

    // 0..N
    @Column(name = "VERSION", nullable = false)
    private int version;

    @Column(name = "CATEGORY", nullable = false)
    @Enumerated(EnumType.STRING)
    private DriftChangeSetCategory category;

    @Column(name = "CONFIG_ID", nullable = false)
    private int driftConfigurationId;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(optional = false)
    private Resource resource;

    @OneToMany(mappedBy = "changeSet", cascade = { CascadeType.ALL })
    private Set<RhqDrift> drifts = new LinkedHashSet<RhqDrift>();

    protected RhqDriftChangeSet() {
    }

    public RhqDriftChangeSet(Resource resource, int version, DriftChangeSetCategory category, int configId) {
        this.resource = resource;
        this.version = version;
        this.category = category;
        driftConfigurationId = configId;
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
    public int getResourceId() {
        return resource.getId();
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public int getDriftConfigurationId() {
        return driftConfigurationId;
    }

    @Override
    public void setDriftConfigurationId(int id) {
        driftConfigurationId = id;
    }

    @Override
    public Set<RhqDrift> getDrifts() {
        return drifts;
    }

    @Override
    public void setDrifts(Set<RhqDrift> drifts) {
        this.drifts = drifts;
    }

    @Override
    public String toString() {
        return "RhqDriftChangeSet [id=" + id + ", resource=" + resource + ", version=" + version + "]";
    }

}
