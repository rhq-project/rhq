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

package org.rhq.core.domain.drift;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;

/**
 * <p>
 * A drift definition template (or drift template for short) is a drift definition that
 * applies at the type level like an alert template. Drift templates can be defined in
 * plugin descriptors as a way to provide suggested and/or common definitions for users
 * wanting to monitor resources of that type. Users can also create templates from admin
 * section of the UI where templates can be managed. Template can be created from existing
 * definitions.
 * </p>
 * <p>
 * A pinned drift template is pinned to a specific snapshot and applies at the type level. A
 * pinned template can be created from an existing snapshot that belongs to a particular
 * resource. Definitions created from a pinned template are pinned as well and use the
 * template's snapshot as their snapshots.
 * </p>
 * <p>
 * When a pinned template is deleted, all definitions created from said template are deleted
 * as well. With an unpinned template however, the definitions can outlive the template.
 * </p>
 * @see DriftDefinition
 */
@Entity
@Table(name = "RHQ_DRIFT_DEF_TEMPLATE")
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "SEQ", sequenceName = "RHQ_DRIFT_DEF_TEMPLATE_ID_SEQ")
public class DriftDefinitionTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    private int id;

    @Column(name = "CTIME", nullable = false)
    private Long ctime = -1L;

    @Column(name = "NAME", length = 128, nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", length = 512, nullable = true)
    private String description;

    @Column(name = "IS_USER_DEFINED", nullable = false)
    private boolean isUserDefined;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RESOURCE_TYPE_ID")
    private ResourceType resourceType;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "CONFIG_ID")
    private Configuration configuration;

    /**
     * If the template is pinned this will be set to the id of the pinned snapshot/change
     * set. Note that change sets are managed by the drift server plugin.
     */
    @Column(name = "DRIFT_CHANGE_SET_ID")
    private String changeSetId;

    // TODO do we want to cascade other operations? - jsanda
    @OneToMany(mappedBy = "template", cascade = CascadeType.PERSIST)
    private Set<DriftDefinition> driftDefinitions = new HashSet<DriftDefinition>();

    //private transient DriftDefinition configWrapper;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Long getCtime() {
        return ctime;
    }

    public void setCtime(Long ctime) {
        this.ctime = ctime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isUserDefined() {
        return isUserDefined;
    }

    public void setUserDefined(boolean isUserDefined) {
        this.isUserDefined = isUserDefined;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType type) {
        resourceType = type;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public DriftDefinition getTemplateDefinition() {
        return new DriftDefinition(configuration);
    }

    public void setTemplateDefinition(DriftDefinition templateDefinition) {
        configuration = templateDefinition.getConfiguration().deepCopyWithoutProxies();
        name = templateDefinition.getName();
        description = templateDefinition.getDescription();
    }

    /**
     * If the template is pinned this will be set to the id of the pinned snapshot/change
     * set. Note that change sets are managed by the drift server plugin.
     *
     * @return The id of the pinned change set or null if the template is not pinned.
     */
    public String getChangeSetId() {
        return changeSetId;
    }

    /**
     * @param changeSetId The id of the change set to which the template is pinned
     */
    public void setChangeSetId(String changeSetId) {
        this.changeSetId = changeSetId;
    }

    public Set<DriftDefinition> getDriftDefinitions() {
        return driftDefinitions;
    }

    public void addDriftDefinition(DriftDefinition definition) {
        driftDefinitions.add(definition);
        definition.setTemplate(this);
    }

    public void removeDriftDefinition(DriftDefinition definition) {
        if (driftDefinitions.remove(definition)) {
            definition.setTemplate(null);
        }
    }

    public Configuration createConfiguration() {
        return createDefinition().getConfiguration();
    }

    public DriftDefinition createDefinition() {
        DriftDefinition definition = new DriftDefinition(configuration.deepCopyWithoutProxies());
        definition.setAttached(true);
        definition.setPinned(isPinned());
        definition.setTemplate(this);

        return definition;
    }

    public boolean isPinned() {
        return changeSetId != null;
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean verbose) {
        String resourceTypeString;
        if ((resourceType != null) && (resourceType.getClass() == ResourceType.class)) {
            // not null and not a Hibernate proxy - safe to invoke getters
            resourceTypeString = "{" + resourceType.getPlugin() + "}" + resourceType.getName();
        } else {
            resourceTypeString = "[null]";
        }
        
        String result;        
        if (verbose) {
            result = "DriftDefinitionTemplate[id: " + id + ", name: " + name + ", resourceType: " + resourceTypeString
                    + ", changeSetId: " + changeSetId + ", configuration: " + configuration + "]";
        } else {
            String configId = (configuration != null) ? Integer.toString(configuration.getId()) : null;
            result = "DriftDefinitionTemplate[id: " + id + ", name: " + name + ", resourceType: " + resourceTypeString
                    + ", " + "changeSetId: " + changeSetId + ", configuration[id: " + configId + "]]";
        }

        return result;
    }

    /**
     * Equality for drift templates is defined as having the same resource type and the
     * same name.
     *
     * @param o The object against which to compare
     * @return True if the arugment is a DriftDefinitionTemplate and has the same resource
     * type and name as this template.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DriftDefinitionTemplate))
            return false;

        DriftDefinitionTemplate that = (DriftDefinitionTemplate) o;

        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        if (resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (resourceType != null ? resourceType.hashCode() : 0);
        return result;
    }
}
