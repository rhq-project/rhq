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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.jetbrains.annotations.NotNull;

/**
 * Holds a map of child {@link Property properties}. This can hold any number of properties, including additional lists
 * and maps of properties (which means you can have N-levels of hierarchical data).
 *
 * <p>This map will store the properties keyed on {@link Property#getName() property name}.</p>
 *
 * <p>Caution must be used when accessing this object. This class is not thread safe and, for entity persistence, the
 * child properties must have their {@link Property#getParentMap()} field set. This is done for you when using the
 * {@link #put(Property)} method.</p>
 *
 * @author Jason Dobies
 * @author Greg Hinkle
 */
@DiscriminatorValue("map")
@Entity
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class PropertyMap extends Property implements AbstractPropertyMap {
    private static final long serialVersionUID = 1L;

    // use the prop name as the map key
    @MapKey(name = "name")
    // CascadeType.REMOVE has been omitted, the cascade delete has been moved to the data model for performance
    @Cascade({ CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    @OneToMany(mappedBy = "parentMap", fetch = FetchType.EAGER, orphanRemoval = true)
    private Map<String, Property> map = new LinkedHashMap<String, Property>();

    /**
     * Creates a new unnamed and empty {@link PropertyMap} object.
     */
    public PropertyMap() {
    }

    protected PropertyMap(PropertyMap original, boolean keepId) {
        super(original, keepId);
    }

    /**
     * Creates a new, empty {@link PropertyMap} object that is associated with the given name.
     *
     * @param name the name of the map itself
     */
    public PropertyMap(@NotNull
    String name) {
        setName(name);
    }

    /**
     * Creates a new {@link PropertyMap} object that is associated with the given name and has the given properties as
     * its initial set of child properties. All properties found in <code>startingProperties</code> will have their
     * {@link Property#setParentMap(PropertyMap) parent map} set to this newly constructed map.
     *
     * @param name               the name of the map itself
     * @param startingProperties a set of properties to be immediately added to this map
     */
    public PropertyMap(@NotNull
    String name, Property... startingProperties) {
        this(name);
        for (Property property : startingProperties) {
            put(property);
        }
    }

    /**
     * Returns the contents of this PropertyMap as a map. The keys to the map are the member properties' names and the
     * values are the properties themselves.
     *
     * <p><b>Warning:</b> Caution should be used when accessing the returned map. Please see
     * {@link PropertyMap the javadoc for this class} for more information.</p>
     *
     * @return the map of key's to property objects
     */
    @NotNull
    public Map<String, Property> getMap() {
        if (this.map == null) {
            this.map = new LinkedHashMap<String, Property>();
        }

        return this.map;
    }

    /**
     * This clears the current internal map replaces it with all of the provided map entries.
     *
     * @param map the map providing the new mappings
     */
    public void setMap(Map<String, Property> map) {
        if (this.map == map) {
            return;
        }
        // Don't replace the possible Hibernate proxy when orphanRemoval=true. It can cause
        // "collection with cascade=all-delete-orphan was no longer referenced" exceptions.
        this.map = getMap();
        this.map.clear();
        if (null != map) {
            this.map.putAll(map);
        }
    }

    /**
     * Put a child property into this map keyed on the given property's name. This method also sets the
     * {@link Property#setParentMap(PropertyMap) parent map} for the child property to make persistence work.
     *
     * @param property the property to add to this map.
     */
    public void put(@NotNull
    Property property) {
        getMap().put(property.getName(), property);
        property.setParentMap(this);
    }

    /**
     * Looks for a property with the given name in the map and returns it. <code>null</code> is returned if it is not
     * found.
     *
     * @param  name the name of the property to return
     *
     * @return the named property or <code>null</code> if it does not exist as a child to this map
     */
    public Property get(String name) {
        return getMap().get(name);
    }

    /**
     * Looks for a child simple property with the given name in the map and returns it. <code>null</code> is returned if
     * it is not found.
     *
     * @param  name the name of the child simple property to return
     *
     * @return the named simple property or <code>null</code> if it does not exist as a child to this map
     *
     * @throws ClassCastException if the named property is not of type {@link PropertySimple}
     */
    public PropertySimple getSimple(String name) {
        return (PropertySimple) get(name);
    }

    public String getSimpleValue(String name, String defaultValue) {
        PropertySimple property = (PropertySimple) getMap().get(name);
        if ((property != null) && (property.getStringValue() != null)) {
            return property.getStringValue();
        } else {
            return defaultValue;
        }
    }

    /**
     * Looks for a child list property with the given name in the map and returns it. <code>null</code> is returned if
     * it is not found.
     *
     * @param  name the name of the child list property to return
     *
     * @return the named list property or <code>null</code> if it does not exist as a child to this map
     *
     * @throws ClassCastException if the named property is not of type {@link PropertyList}
     */
    public PropertyList getList(String name) {
        return (PropertyList) get(name);
    }

    /**
     * Looks for a child map property with the given name in the map and returns it. <code>null</code> is returned if it
     * is not found.
     *
     * @param  name the name of the child map property to return
     *
     * @return the named map property or <code>null</code> if it does not exist as a child to this map
     *
     * @throws ClassCastException if the named property is not of type {@link PropertyMap}
     */
    public PropertyMap getMap(String name) {
        return (PropertyMap) get(name);
    }

    /**
     * NOTE: An PropertyMap containing a null map is considered equal to a PropertyMap containing an empty map.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof PropertyMap)) {
            return false;
        }

        if (!super.equals(obj)) {
            return false; // superclass checks equality of the name fields
        }

        PropertyMap that = (PropertyMap) obj;
        // NOTE: Use that.getMap(), rather than that.map, in case 'that' is a JPA/Hibernate proxy, to
        //       force loading of the Map.
        if ((this.map == null) || this.map.isEmpty()) {
            return (that.getMap() == null) || that.getMap().isEmpty();
        }

        return this.map.equals(that.getMap());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode(); // superclass hashCode is derived from the name field
        result = (31 * result) + (((this.map != null) && !this.map.isEmpty()) ? this.map.hashCode() : 0);
        return result;
    }

    public PropertyMap deepCopy(boolean keepId) {
        PropertyMap copy = new PropertyMap(this, keepId);

        for (Property property : map.values()) {
            copy.put(property.deepCopy(keepId));
        }

        return copy;
    }

    @Override
    protected void appendToStringInternals(StringBuilder str) {
        super.appendToStringInternals(str);
        str.append(", map=").append(getMap());
    }

    /**
     * This listener runs after jaxb unmarshalling and reconnects children properties to their parent maps (as we don't
     * send them avoiding cyclic references).
     */
    public void afterUnmarshal(Object u, Object parent) {
        for (Property p : this.map.values()) {
            p.setParentMap(this);
        }
    }
}