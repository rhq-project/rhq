/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.event;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.resource.ResourceType;

/**
 * The definition of a type of {@link Event} supported by a particular {@link ResourceType}.
 *
 * @author Ian Springer
 */
@Entity
@NamedQueries( { @NamedQuery(name = EventDefinition.QUERY_EVENT_DEFINITIONS_BY_RESOURCE_TYPE_ID, query = "" //
    + "SELECT ed " //
    + "  FROM EventDefinition ed " //
    + " WHERE ed.resourceTypeId = :resourceTypeId ") })
@Table(name = EventDefinition.TABLE_NAME)
@SequenceGenerator(name = "idGenerator", sequenceName = EventDefinition.TABLE_NAME + "_ID_SEQ", allocationSize = 100)
public class EventDefinition implements Externalizable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME = "RHQ_EVENT_DEF";

    public static final String QUERY_EVENT_DEFINITIONS_BY_RESOURCE_TYPE_ID = "EventDefinition.findByResourceTypeId";

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "idGenerator")
    private int id;

    @JoinColumn(name = "RESOURCE_TYPE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private ResourceType resourceType;

    @Column(name = "RESOURCE_TYPE_ID", insertable = false, updatable = false)
    private int resourceTypeId;

    @Column(name = "NAME", length = 100, nullable = false)
    private String name;

    @Column(name = "DISPLAY_NAME", length = 100)
    private String displayName;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    /* no-arg constructor required by EJB spec and Externalizable (Externalizable also requires it to be public) */
    public EventDefinition() {
    }

    public EventDefinition(@NotNull ResourceType resourceType, @NotNull String name) {
        if (resourceType == null)
            throw new IllegalArgumentException("resourceType parameter must not be null.");
        if (name == null)
            throw new IllegalArgumentException("name parameter must not be null.");
        this.resourceType = resourceType;
        this.resourceTypeId = this.resourceType.getId();
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    @NotNull
    public ResourceType getResourceType() {
        return resourceType;
    }

    public int getResourceTypeId() {
        return resourceTypeId;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || !(obj instanceof EventDefinition))
            return false;

        EventDefinition that = (EventDefinition) obj;

        if ((resourceType != null) ? (!resourceType.equals(that.resourceType)) : (that.resourceType != null)) {
            return false;
        }

        if (!name.equals(that.name))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (resourceType != null) ? resourceType.hashCode() : 0;
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + "id=" + this.id + ", " + "resourceType.name="
            + ((this.resourceType != null) ? this.resourceType.getName() : "null") + ", " + "name=" + this.name + "]";

    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.resourceType);
        out.writeUTF(this.name);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.resourceType = (ResourceType) in.readObject();
        this.name = in.readUTF();
    }

    public void setResourceTypeId(int resourceTypeId) {
        this.resourceTypeId = resourceTypeId;
    }
}