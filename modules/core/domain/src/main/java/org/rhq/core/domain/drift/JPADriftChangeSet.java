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
import javax.persistence.FetchType;
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

import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;
import org.rhq.core.domain.resource.Resource;

/**
 * The JPA Drift Server plugin (the RHQ default) implementation of DriftChangeSet.
 *   
 * @author Jay Shaughnessy
 * @author John Sanda 
 */
@Entity
@NamedQueries( { @NamedQuery(name = JPADriftChangeSet.QUERY_DELETE_BY_RESOURCES, query = "" //
    + "DELETE FROM JPADriftChangeSet dcs " //
    + " WHERE dcs.resource.id IN ( :resourceIds )"), //
    @NamedQuery(name = JPADriftChangeSet.QUERY_DELETE_BY_DRIFTCONFIG_RESOURCE, query = "" //
        + "DELETE FROM JPADriftChangeSet dcs " //
        + " WHERE dcs.resource.id = :resourceId " //
        + "   AND dcs.driftConfiguration.id IN " //
        + "       (SELECT dc.id " //
        + "          FROM DriftConfiguration dc " //
        + "         WHERE dc.resource.id = :resourceId AND dc.name = :driftConfigurationName)" //
    ) })
@Table(name = "RHQ_DRIFT_CHANGE_SET")
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_DRIFT_CHANGE_SET_ID_SEQ")
public class JPADriftChangeSet implements Serializable, DriftChangeSet<JPADrift> {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_BY_RESOURCES = "JPADriftChangeSet.deleteByResources";
    public static final String QUERY_DELETE_BY_DRIFTCONFIG_RESOURCE = "JPADriftChangeSet.deleteByDriftConfigResource";

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

    @JoinColumn(name = "DRIFT_CONFIG_ID", referencedColumnName = "ID", nullable = false)
    // @ManyToOne(optional = false)
    // TODO: remove this eager load, the drift tree build should be written to not need this
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private DriftConfiguration driftConfiguration;

    // Note, this is mode at the time of the changeset processing. We cant use driftConfiguration.mode because
    // that is the "live" setting.
    @Column(name = "DRIFT_CONFIG_MODE", nullable = false)
    @Enumerated(EnumType.STRING)
    private DriftHandlingMode driftHandlingMode;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(optional = false)
    private Resource resource;

    @OneToMany(mappedBy = "changeSet", cascade = { CascadeType.ALL })
    private Set<JPADrift> drifts = new LinkedHashSet<JPADrift>();

    protected JPADriftChangeSet() {
    }

    public JPADriftChangeSet(Resource resource, int version, DriftChangeSetCategory category,
        DriftConfiguration driftConfiguration) {
        this.resource = resource;
        this.version = version;
        this.category = category;
        this.driftConfiguration = driftConfiguration;
        this.driftHandlingMode = driftConfiguration.getDriftHandlingMode();
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

    public DriftConfiguration getDriftConfiguration() {
        return driftConfiguration;
    }

    public void setDriftConfiguration(DriftConfiguration driftConfiguration) {
        this.driftConfiguration = driftConfiguration;
    }

    public DriftHandlingMode getDriftHandlingMode() {
        return driftHandlingMode;
    }

    public void setDriftHandlingMode(DriftHandlingMode driftHandlingMode) {
        this.driftHandlingMode = driftHandlingMode;
    }

    @Override
    public int getDriftConfigurationId() {
        return this.driftConfiguration.getId();
    }

    @Override
    public void setDriftConfigurationId(int id) {
        this.driftConfiguration.setId(id);
    }

    @Override
    public Set<JPADrift> getDrifts() {
        return drifts;
    }

    @Override
    public void setDrifts(Set<JPADrift> drifts) {
        this.drifts = drifts;
    }

    @Override
    public String toString() {
        return "JPADriftChangeSet [id=" + id + ", resource=" + resource + ", version=" + version + "]";
    }

}
