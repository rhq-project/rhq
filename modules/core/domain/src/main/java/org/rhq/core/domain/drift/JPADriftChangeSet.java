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

import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;

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
 * <p>
 * The JPA Drift Server plugin (the RHQ default) implementation of DriftChangeSet. A change
 * set instance has slightly different behavior based on whether or not it is version zero,
 * that is the initial change set. This is due to the way in which pinned templates are
 * supported.
 * </p>
 * <p>
 * All change sets belong to a drift definition, and the definition is created from a
 * {@link DriftDefinitionTemplate}. Templates can be pinned or unpinned. Each change set
 * encapsulates a collection of changes that are represented by {@link JPADrift}.
 * When a template is pinned there is a corresponding pinned snapshot that belongs to the
 * template. Each definition created from that template uses that same pinned snapshot. In
 * terms of implementation, the pinned snapshot is always change set version zero. As an
 * optimization (of the default driftserver plugin data model design), the pinned snapshot
 * is shared among definitions to avoid the overhead of making copies of what could
 * potentially be very large numbers of Drift entities.
 * </p>
 * <p>
 * When an instance of this class represents change set version zero, different fields will
 * be "live" (i.e., non-null and in use) versus when it is not the initial change set.
 * </p>
 * <p>
 * <strong>Note:</strong> Because persistence of this entity is managed by a drift server
 * plugin, other entities managed by the RHQ core server cannot maintain direct references
 * or JPA associations to this class. This restriction is necessary because entities managed
 * by the RHQ core server interact with instance of this class through its drift entity
 * interface which may have multiple implementation. Those implementations need not even
 * be based on a RDBMS. Even though this entity is managed via a drift server plugin, it
 * can maintain direct references and JPA associations to entities managed by the RHQ
 * core server since they reside in the same database. That is however an implementation
 * detail only of this class. It cannot be exposed in the drift interfaces.
 * </p>
 *   
 * @author Jay Shaughnessy
 * @author John Sanda 
 */
@Entity
@NamedQueries( { @NamedQuery(name = JPADriftChangeSet.QUERY_DELETE_BY_RESOURCES, query = "" //
    + "DELETE FROM JPADriftChangeSet dcs " //
    + " WHERE dcs.resource.id IN ( :resourceIds )"), //
    @NamedQuery(name = JPADriftChangeSet.QUERY_DELETE_BY_DRIFTDEF_RESOURCE, query = "" //
        + "DELETE FROM JPADriftChangeSet dcs " //
        + " WHERE dcs.resource.id = :resourceId " //
        + "   AND dcs.driftDefinition.id IN " //
        + "       (SELECT dc.id " //
        + "          FROM DriftDefinition dc " //
        + "         WHERE dc.resource.id = :resourceId AND dc.name = :driftDefinitionName)" //
    ) })
@Table(name = "RHQ_DRIFT_CHANGE_SET")
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_DRIFT_CHANGE_SET_ID_SEQ")
public class JPADriftChangeSet implements Serializable, DriftChangeSet<JPADrift> {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_BY_RESOURCES = "JPADriftChangeSet.deleteByResources";
    public static final String QUERY_DELETE_BY_DRIFTDEF_RESOURCE = "JPADriftChangeSet.deleteByDriftDefResource";

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

    @JoinColumn(name = "DRIFT_DEFINITION_ID", referencedColumnName = "ID")
    // TODO: remove this eager load, the drift tree build should be written to not need this
    @ManyToOne(fetch = FetchType.EAGER)
    private DriftDefinition driftDefinition;

    // Note, this is mode at the time of the changeset processing. We cant use driftDefinition.mode because
    // that is the "live" setting.
    @Column(name = "DRIFT_HANDLING_MODE", nullable = false)
    @Enumerated(EnumType.STRING)
    private DriftHandlingMode driftHandlingMode;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID")
    @ManyToOne
    private Resource resource;

    /**
     * The set of drift entries that make up this change set. If this instance of
     * JPADriftChangeSet is change set version zero, then this field will be null because
     * the drifts for the initial change set are accessed through a {@link JPADriftSet}.
     */
    @OneToMany(mappedBy = "changeSet", cascade = { CascadeType.ALL })
    private Set<JPADrift> drifts = new LinkedHashSet<JPADrift>();

    /**
     * This field is null unless this instance of JPADriftChangeSet is the initial change
     * set. If the change set belongs to a definition that was created from a pinned
     * template, then the {@link JPADriftSet} will be shared by all definitions created
     * from the template.
     */
    @ManyToOne(optional = true, cascade = { PERSIST, MERGE })
    @JoinColumn(name = "DRIFT_SET_ID", referencedColumnName = "ID")
    private JPADriftSet initialDriftSet;

    protected JPADriftChangeSet() {
    }

    public JPADriftChangeSet(Resource resource, int version, DriftChangeSetCategory category,
        DriftDefinition driftDefinition) {
        this.resource = resource;
        this.version = version;
        this.category = category;
        this.driftDefinition = driftDefinition;
        if (driftDefinition != null) {
            this.driftHandlingMode = driftDefinition.getDriftHandlingMode();
        }
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
        if (resource == null) {
            return 0;
        }
        return resource.getId();
    }

    @Override
    public void setResourceId(int id) {
        // This is a no-op because we maintain a JPA association with
        // the owning resource and therefore use setResource(Resource r)
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public DriftDefinition getDriftDefinition() {
        return driftDefinition;
    }

    public void setDriftDefinition(DriftDefinition driftDefinition) {
        this.driftDefinition = driftDefinition;
    }

    public DriftHandlingMode getDriftHandlingMode() {
        return driftHandlingMode;
    }

    public void setDriftHandlingMode(DriftHandlingMode driftHandlingMode) {
        this.driftHandlingMode = driftHandlingMode;
    }

    @Override
    public int getDriftDefinitionId() {
        if (driftDefinition == null) {
            return 0;
        }
        return driftDefinition.getId();
    }

    @Override
    public void setDriftDefinitionId(int id) {
        driftDefinition.setId(id);
    }

    @Override
    public Set<JPADrift> getDrifts() {
        if (version > 0) {
            return drifts;
        }

        // TODO do we need to check for null here?
        // If this is the initial change set, initialDriftSet should be non-null so I am
        // not sure whether or not a null check is necessary here.
        //
        // jsanda
        return initialDriftSet.getDrifts();
    }

    @Override
    public void setDrifts(Set<JPADrift> drifts) {
        this.drifts = drifts;
    }

    public JPADriftSet getInitialDriftSet() {
        return initialDriftSet;
    }

    public void setInitialDriftSet(JPADriftSet driftSet) {
        initialDriftSet = driftSet;
    }

    @Override
    public String toString() {
        return "JPADriftChangeSet [id=" + id + ", resource=" + resource + ", version=" + version + "]";
    }

}
