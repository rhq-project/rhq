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
package org.rhq.core.domain.configuration;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.jetbrains.annotations.NotNull;

/**
 * Holds an indexed list of child {@link Property properties}. This can hold any number of properties, including
 * additional lists and maps of properties (which means you can have N-levels of hierarchical data).
 *
 * <p>This list will store the properties in the order they are {@link #add(Property) added}.</p>
 *
 * <p>Caution must be used when accessing this object. This class is not thread safe and, for entity persistence, the
 * child properties must have their {@link Property#getParentList()} field set. This is done for you when using the
 * {@link #add(Property)} method.</p>
 *
 * @author Jason Dobies
 * @author Greg Hinkle
 */
@DiscriminatorValue("list")
@Entity
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class PropertyList extends Property {
    private static final long serialVersionUID = 1L;

    // CascadeType.REMOVE has been omitted, the cascade delete has been moved to the data model for performance
    @Cascade({ CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    @OneToMany(mappedBy = "parentList", targetEntity = Property.class, fetch = FetchType.EAGER, orphanRemoval = true)
    // Order by primary key which will also put the list elements in chronological order.
    // Note, if we decide at some point to add support in the GUI for reordering list elements, we'll
    // need to add a new ORDER column and order by that.
    @OrderBy
    private List<Property> list = new ArrayList<Property>();

    @Transient
    String memberPropertyName;

    /* no-arg constructor required by EJB spec and Externalizable (Externalizable also requires it to be public) */
    public PropertyList() {
    }

    /**
     * Creates a new, empty {@link PropertyList} object that is associated with the given name.
     *
     * @param name the name of the list itself
     */
    public PropertyList(@NotNull String name) {
        setName(name);
    }

    protected PropertyList(PropertyList original, boolean keepId) {
        super(original, keepId);
    }

    /**
     * Creates a new {@link PropertyList} object that is associated with the given name and has the given properties as
     * its initial list of child properties. All properties found in <code>startingList</code> will have their
     * {@link Property#setParentList(PropertyList) parent list} set to this newly constructed list.
     *
     * @param name         the name of the list itself
     * @param startingList a list of properties to be immediately added to this list
     */
    public PropertyList(@NotNull String name, @NotNull Property... startingList) {
        this(name);
        for (Property property : startingList) {
            add(property);
        }
    }

    /**
     * Returns the children of this list.
     *
     * <p><b>Warning:</b> Caution should be used when accessing the returned list. Please see
     * {@link PropertyList the javadoc for this class} for more information.</p>
     *
     * @return the list of child properties
     */
    @NotNull
    public List<Property> getList() {
        if (this.list == null) {
            this.list = new ArrayList<Property>();
        }

        return this.list;
    }

    /**
     * Sets the list of child properties directly to the given <code>list</code> reference. This means the actual <code>
     * list</code> object is stored internally in this object. Changes made to <code>list</code> will be reflected back
     * into this object.
     *
     * <p><b>Warning:</b> Caution should be used when setting this object's internal list. Please see
     * {@link PropertyList the javadoc for this class} for more information.</p>
     *
     * @param list the new list used internally by this object
     */
    public void setList(List<Property> list) {
        if (list != null) {
            for (Property property : list) {
                add(property);
            }
        }
    }

    /**
     * Adds a child property to the end of this list. This method also sets the
     * {@link Property#setParentList(PropertyList) parent list} for the child property to make persistence work.
     *
     * @param property the property to add to this list
     */
    public void add(@NotNull Property property) {
        if (this.memberPropertyName == null) {
            this.memberPropertyName = property.getName();
        }

        if (!property.getName().equals(this.memberPropertyName)) {
            throw new IllegalStateException("All properties in a PropertyList (id=[" + getId() + "], name=["
                + getName() + "]) must have the same name: [" + property.getName() + "] != [" + this.memberPropertyName
                + "]");
        }

        getList().add(property);
        property.setParentList(this);
    }

    /**
     * NOTE: An PropertyList containing a null list is considered equal to a PropertyList containing an empty list.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof PropertyList)) {
            return false;
        }

        if (!super.equals(obj)) {
            return false; // superclass checks equality of the name fields
        }

        PropertyList that = (PropertyList) obj;
        if ((this.list == null) || this.list.isEmpty()) {
            // NOTE: Use that.getList(), rather than that.list, in case 'that' is a JPA/Hibernate proxy, to
            //       force loading of the List.
            return (that.getList() == null) || that.getList().isEmpty();
        }

        return this.list.containsAll(that.getList()) && that.getList().containsAll(this.list);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode(); // superclass hashCode is derived from the name field
        result = (31 * result) + (((this.list != null) && !this.list.isEmpty()) ? this.list.hashCode() : 0);
        return result;
    }

    public PropertyList deepCopy(boolean keepId) {
        PropertyList copy = new PropertyList(this, keepId);

        for (Property property : list) {
            copy.add(property.deepCopy(false));
        }

        return copy;
    }

    @Override
    protected void appendToStringInternals(StringBuilder str) {
        super.appendToStringInternals(str);
        str.append(", list=").append(getList());
    }

    /**
     * This listener runs after jaxb unmarshalling and reconnects children properties to their parent list (as we don't
     * send them avoiding cyclic references).
     */
    public void afterUnmarshal(Object u, Object parent) {
        for (Property p : this.list) {
            p.setParentList(this);
        }
    }
}