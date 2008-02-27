/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.resource.Resource;

/**
 * An event source (e.g. a log file path or an SNMP trap OID) for a specific type of {@link Event} (see
 * {@link EventDefinition}) and a specific {@link org.rhq.core.domain.resource.Resource}.
 *
 * @author Ian Springer
 */
@NamedQueries( {
        // TODO
})
@Entity
@Table(name = EventSource.TABLE_NAME)
@SequenceGenerator(name = "idGenerator", sequenceName = EventSource.TABLE_NAME + "_ID_SEQ", allocationSize = 100)
public class EventSource implements Externalizable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME = "RHQ_EVENT_SOURCE";

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idGenerator")
    private int id;

    @JoinColumn(name = "EVENT_DEF_ID", referencedColumnName = "ID", nullable = false)
    private EventDefinition eventDefinition;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Resource resource;

    @SuppressWarnings({"UnusedDeclaration"})
    @Column(name = "RESOURCE_ID", insertable = false, updatable = false)
    private int resourceId;

    @Column(name = "LOCATION", length = 2000, nullable = false)
    private String location;

    /* no-arg constructor required by EJB spec and Externalizable (Externalizable also requires it to be public) */
    public EventSource() {
    }

    public EventSource(@NotNull String location,
                       @NotNull EventDefinition eventDefinition,
                       @NotNull Resource resource) {
        if (location == null)
            throw new IllegalArgumentException("location parameter must not be null.");
        if (eventDefinition == null)
            throw new IllegalArgumentException("eventDefinition parameter must not be null.");
        if (resource == null)
            throw new IllegalArgumentException("resource parameter must not be null.");
        this.location = location;
        this.eventDefinition = eventDefinition;
        this.resource = resource;
        this.resourceId = this.resource.getId();
    }

    public int getId() {
        return this.id;
    }

    public EventDefinition getEventDefinition() {
        return eventDefinition;
    }

    public Resource getResource() {
        return resource;
    }

    public int getResourceId() {
        return resourceId;
    }

    @NotNull
    public String getLocation() {
        return this.location;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        EventSource that = (EventSource) obj;

        if (!eventDefinition.equals(that.eventDefinition))
            return false;
        if (resourceId != that.resourceId)
            return false;
        if (!location.equals(that.location))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = eventDefinition.hashCode();
        result = 31 * result + resourceId;
        result = 31 * result + location.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + "id=" + this.id + ", "
            + "eventDefinition.name=" + ((this.eventDefinition != null) ? this.eventDefinition.getName() : "null") + ", "
            + "resource.name=" + ((this.resource != null) ? this.resource.getName() : "null") + ", "
            + "location=" + this.location + "]";

    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.eventDefinition);
        out.writeInt((this.resource != null) ? this.resource.getId() : 0);
        out.writeUTF(this.location);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.eventDefinition = (EventDefinition) in.readObject();
        int resourceId = in.readInt();
        this.resource = new Resource(resourceId);
        this.location = in.readUTF();
    }
}