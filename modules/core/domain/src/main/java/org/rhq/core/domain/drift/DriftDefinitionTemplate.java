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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;

@Entity
@Table(name = "RHQ_DRIFT_DEF_TEMPLATE")
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_DRIFT_DEF_TEMPLATE_ID_SEQ")
public class DriftDefinitionTemplate {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    private int id;

    @Column(name = "CTIME", nullable = false)
    private Long ctime = -1L;

    @Column(name = "NAME", length = 128, nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", length = 256)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RESOURCE_TYPE_ID")
    private ResourceType resourceType;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "CONFIG_ID")
    private Configuration configuration;

    @Column(name = "DRIFT_CHANGE_SET_ID")
    private String changeSetId;

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

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public String getChangeSetId() {
        return changeSetId;
    }

    public void setChangeSetId(String changeSetId) {
        this.changeSetId = changeSetId;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id: " + id + ", name: " + name + ", description: " + description +
            ", resourceType: " + resourceType + ", changeSetId: " + changeSetId + ", configuration: " + configuration +
            "]";
    }

    public String toString(boolean verbose) {
        if (verbose) {
            return toString();
        }

        String configId;
        if (configuration == null) {
            configId = null;
        } else {
            configId = Integer.toString(configuration.getId());
        }

        if (resourceType != null) {
            return getClass().getSimpleName() + "[id: " + id + ", name: " + name + ", resourceType[id: " +
                resourceType.getId() + ", name: " + resourceType.getName() + ", plugin: " + resourceType.getPlugin() +
                ", changeSetId: " + changeSetId + ", configuration[id: " + configId + "]]";
        }

        return getClass().getSimpleName() + "[id: " + id + ", name: " + name + ", resourceType:[null], " +
            "changeSetId: " + changeSetId + "configuration[id: " + configId + "]]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DriftDefinitionTemplate)) return false;

        DriftDefinitionTemplate that = (DriftDefinitionTemplate) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (resourceType != null ? resourceType.hashCode() : 0);
        return result;
    }
}
