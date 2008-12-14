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
package org.rhq.core.domain.configuration;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintStream;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The abstract base class for all {@link Configuration} value property types. A property is associated with a specific
 * {@link #getName()} and can contain one or more values. Since a {@link Configuration} represents a hierarchical set of
 * data, properties can have a parent. For example, if a property is a member of a {@link PropertyList}, it will have a
 * {@link #getParentList() parent list}.
 *
 * <p>There are three different types (i.e. subclasses) of properties:</p>
 *
 * <ul>
 *   <li>{@link PropertySimple simple}</li>
 *   <li>{@link PropertyList list}</li>
 *   <li>{@link PropertyMap map}</li>
 * </ul>
 *
 * <p>These subclasses are mapped into a single table so referential integrity is easy to maintain.</p>
 *
 * <p>Maps may only have one value for a given key, while lists may have many and follow Bag rules.</p>
 *
 * <p>Note that each property can have an optional error message associated with it. This is used typically when the
 * property is stored in a configuration that is inside a {@link AbstractResourceConfigurationUpdate} object. If a
 * property failed to get set, this property's error message can be used to indicate why it failed (e.g. the property's
 * value was out of range or some other validation rule was not followed).</p>
 *
 * @author Jason Dobies
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
@DiscriminatorColumn(name = "DTYPE")
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONFIG_PROPERTY_ID_SEQ")
@Table(name = "RHQ_CONFIG_PROPERTY")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
@XmlSeeAlso( { PropertySimple.class, PropertyList.class, PropertyMap.class })
public abstract class Property implements Externalizable {
    private static final long serialVersionUID = 1L;

    @Column(name = "ID")
    @GeneratedValue(generator = "SEQ", strategy = GenerationType.SEQUENCE)
    @Id
    private int id;

    @JoinColumn(name = "CONFIGURATION_ID", referencedColumnName = "ID")
    @ManyToOne(optional = true)
    @XmlTransient
    private Configuration configuration;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "ERROR_MESSAGE", nullable = true)
    private String errorMessage;

    @JoinColumn(name = "PARENT_LIST_ID", referencedColumnName = "ID")
    @ManyToOne
    @XmlTransient
    private PropertyList parentList;

    @JoinColumn(name = "PARENT_MAP_ID", referencedColumnName = "ID")
    @ManyToOne
    @XmlTransient
    private PropertyMap parentMap;

    public Property() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the name of this property. Note that all properties, including lists and maps of properties, are
     * associated with a name.
     *
     * @return the property name
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Defines the name that this property will be associated with.
     *
     * @param name the name that this property will be associated with
     */
    public void setName(@NotNull String name) {
        this.name = name;
    }

    /**
     * Returns the parent of this property, assuming this property is a child of a {@link PropertyList}. <code>
     * null</code> will be returned if this property is not a child of any list.
     *
     * <p>Note that direct children of the {@link Configuration} object will return <code>null</code>.</p>
     *
     * @return parent list or <code>null</code>
     */
    public PropertyList getParentList() {
        return parentList;
    }

    /**
     * Sets the parent of this property, which will assume this property is a member of a {@link PropertyList list}. Set
     * this to <code>null</code> if this property is not a child of any list.
     *
     * @param parentList the parent of this property or <code>null</code>
     */
    public void setParentList(PropertyList parentList) {
        this.parentList = parentList;
    }

    /**
     * Returns the parent of this property, assuming this property is a child of a {@link PropertyMap}. <code>
     * null</code> will be returned if this property is not a child of any map.
     *
     * <p>Note that direct children of the {@link Configuration} object will return <code>null</code>.</p>
     *
     * @return parent list or <code>null</code>
     */
    public PropertyMap getParentMap() {
        return parentMap;
    }

    /**
     * Sets the parent of this property, which will assume this property is a member of a {@link PropertyMap map}. Set
     * this to <code>null</code> if this property is not a child of any map.
     *
     * @param parentMap the parent of this property or <code>null</code>
     */
    public void setParentMap(PropertyMap parentMap) {
        this.parentMap = parentMap;
    }

    /**
     * Returns the {@link Configuration} object where this property can be found. This will be <code>null</code> if this
     * property is a child of a {@link PropertyList} or {@link PropertyMap} and not a direct child of the
     * {@link Configuration} itself.
     *
     * @return this property's associated {@link Configuration}, or <code>null</code>
     */
    @XmlTransient
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Sets the {@link Configuration} object where this property can be found. Set this to <code>null</code> if this
     * property is a child of a {@link PropertyList} or {@link PropertyMap} and not a direct child of the
     * {@link Configuration} itself.
     *
     * @param configuration this property's associated {@link Configuration}, or <code>null</code>
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * If the property value has been detected to be invalid for some reason, this is an error message that describes
     * the error. This may be <code>null</code> if either the property is valid or it is not known if the property is
     * valid or not. Depending on the context of where this property instance is will dictate the semantics of a <code>
     * null</code> error message (see {@link AbstractResourceConfigurationUpdate}).
     *
     * @return error message describing how/why the property is invalid
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = (errorMessage != null) ? errorMessage.trim() : errorMessage;
    }

    /**
     * Convienence method that sets the error message to the given throwable's stack trace dump. If the given throwable
     * is <code>null</code>, the error message will be set to <code>null</code> as if passing <code>null</code> to
     * {@link #setErrorMessage(String)}.
     *
     * @param t throwable whose message and stack trace will make up the error message (may be <code>null</code>)
     */
    public void setErrorMessageFromThrowable(Throwable t) {
        if (t != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.printStackTrace(new PrintStream(baos));
            setErrorMessage(baos.toString());
        } else {
            setErrorMessage(null);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof Property)) {
            return false;
        }

        Property property = (Property) obj;
        if ((this.name != null) ? (!this.name.equals(property.name)) : (property.name != null)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return ((this.name != null) ? this.name.hashCode() : 0);
    }

    /**
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(id);
        out.writeObject(configuration);
        out.writeUTF(name);
        out.writeObject(parentList);
        out.writeObject(parentMap);
        out.writeObject(errorMessage);
    }

    /**
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = in.readInt();
        configuration = (Configuration) in.readObject();
        name = in.readUTF();
        parentList = (PropertyList) in.readObject();
        parentMap = (PropertyMap) in.readObject();
        errorMessage = (String) in.readObject();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(getClass().getSimpleName());
        str.append("[id=").append(getId());
        str.append(", name=").append(getName());
        appendToStringInternals(str); // ask subclasses if they have anything else to add
        str.append(']');
        return str.toString();
    }

    /**
     * Subclasses can override this to add things it wants to see in the toString.
     *
     * @param str the builder to append strings to
     */
    protected void appendToStringInternals(StringBuilder str) {
        return;
    }
}