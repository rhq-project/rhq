/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This is the root object for the storage of a hierarchical value set of data. This data may represent configurations
 * of external systems or the components within ON. The data values supported are the basic primitive types in
 * containers of maps and lists. Containers may hold other containers creating the hierarchical data structure. This
 * content is loosely related to the definition entities that can provide a model for the possible values and validation
 * of them.
 *
 * <p>A <code>Configuration</code> has one or more named {@link Property} objects contained within it (similar to a
 * <code>Map</code>). Note that {@link Property} is an abstract class that actually represents either:</p>
 *
 * <ul>
 *   <li>a simple value ({@link PropertySimple})</li>
 *   <li>a list of other {@link Property} objects ({@link PropertyList})</li>
 *   <li>a map of other {@link Property} objects ({@link PropertyMap})</li>
 * </ul>
 *
 * <p>Because a Configuration can contain a list or map of properties, a Configuration can contain a hierarchy of
 * properties N-levels deep.</p>
 *
 * <p>Each Property within a Configuration has a name - this not only includes simple properties, but also lists and
 * maps of properties as well. For example, you can retrieve a list of properties via {@link #getList(String)} by
 * passing in the name of the list.</p>
 *
 * @author Jason Dobies
 * @author Greg Hinkle
 *
 * @see    Property
 * @see    PropertySimple
 * @see    PropertyList
 * @see    PropertyMap
 */
@Entity(name = "Configuration")
@NamedQueries({ //
@NamedQuery(name = Configuration.QUERY_GET_PLUGIN_CONFIG_BY_RESOURCE_ID, query = "" //
    + "select r.pluginConfiguration from Resource r where r.id = :resourceId"),
    @NamedQuery(name = Configuration.QUERY_GET_RESOURCE_CONFIG_BY_RESOURCE_ID, query = "" //
        + "select r.resourceConfiguration from Resource r where r.id = :resourceId"),
    @NamedQuery(name = Configuration.QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_ID, query = "" //
        + "SELECT r.id, r.resourceConfiguration " //
        + "  FROM ResourceGroup rg " //
        + "  JOIN rg.explicitResources r " //
        + " WHERE rg.id = :resourceGroupId AND r.inventoryStatus = 'COMMITTED'"),
    @NamedQuery(name = Configuration.QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_ID, query = "" //
        + "SELECT r.id, r.pluginConfiguration " //
        + "  FROM ResourceGroup rg " //
        + "  JOIN rg.explicitResources r " //
        + " WHERE rg.id = :resourceGroupId AND r.inventoryStatus = 'COMMITTED'"),
    @NamedQuery(name = Configuration.QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_UPDATE_ID, query = "" //
        + "SELECT res.id, cu.configuration " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + "  JOIN cu.resource res " //
        + " WHERE cu.groupConfigurationUpdate.id = :groupConfigurationUpdateId"),
    @NamedQuery(name = Configuration.QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_UPDATE_ID, query = "" //
        + "SELECT res.id, cu.configuration " //
        + "  FROM PluginConfigurationUpdate cu " //
        + "  JOIN cu.resource res " //
        + " WHERE cu.groupConfigurationUpdate.id = :groupConfigurationUpdateId"),
    @NamedQuery(name = Configuration.QUERY_BREAK_PROPERTY_RECURSION_BY_CONFIGURATION_IDS, query = "" //
        + "UPDATE Property p " //
        + "   SET p.parentMap = NULL, " //
        + "       p.parentList = NULL " //
        + " WHERE p.configuration.id IN ( :configurationIds )"),
    @NamedQuery(name = Configuration.QUERY_DELETE_RAW_CONFIGURATIONS_CONFIGURATION_IDS, query = "" //
        + "DELETE FROM RawConfiguration rc " //
        + " WHERE rc.configuration.id IN ( :configurationIds )"),
    @NamedQuery(name = Configuration.QUERY_DELETE_CONFIGURATIONS_BY_CONFIGURATION_IDs, query = "" //
        + "DELETE FROM Configuration c " //
        + " WHERE c.id IN ( :configurationIds )") })
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_CONFIG_ID_SEQ", sequenceName = "RHQ_CONFIG_ID_SEQ")
@Table(name = "RHQ_CONFIG")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Configuration implements Serializable, Cloneable, AbstractPropertyMap {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_GET_PLUGIN_CONFIG_BY_RESOURCE_ID = "Configuration.getPluginConfigByResourceId";
    public static final String QUERY_GET_RESOURCE_CONFIG_BY_RESOURCE_ID = "Configuration.getResourceConfigByResourceId";
    public static final String QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_ID = "Configuration.getResourceConfigMapByGroupId";
    public static final String QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_ID = "Configuration.getPluginConfigMapByGroupId";
    public static final String QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_UPDATE_ID = "Configuration.getResourceConfigMapByGroupUpdateId";
    public static final String QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_UPDATE_ID = "Configuration.getPluginConfigMapByGroupUpdateId";

    public static final String QUERY_BREAK_PROPERTY_RECURSION_BY_CONFIGURATION_IDS = "Property.breakPropertyRecursionByConfigurationIds";
    public static final String QUERY_DELETE_RAW_CONFIGURATIONS_CONFIGURATION_IDS = "Configuration.deleteRawByConfigurationIds";
    public static final String QUERY_DELETE_CONFIGURATIONS_BY_CONFIGURATION_IDs = "Configuration.deleteByConfigurationIdS";

    private static abstract class AbstractPropertyMapBuilder<T extends AbstractPropertyMap, This extends AbstractPropertyMapBuilder<T, This>> {
        private T map;

        protected AbstractPropertyMapBuilder(T map) {
            this.map = map;
        }

        /**
         * Adds a simple property.
         * @param name the name of the simple property
         * @param value the value of the simple property
         * @return continue with the definition
         */
        public This addSimple(String name, Object value) {
            getMap().put(new PropertySimple(name, value));
            return castThis();
        }

        /**
         * Starts defining a new sub list.
         * @param name the name of the sub list
         * @param memberName the names of the member properties of the sub list
         * @return the builder of the list
         */
        public Builder.ListInMap<This> openList(String name, String memberName) {
            return new Builder.ListInMap<This>(castThis(), name, memberName);
        }

        /**
         * Starts defining a new sub map.
         * @param name the name of the sub map
         * @return the builder of the map
         */
        public Builder.MapInMap<This> openMap(String name) {
            return new Builder.MapInMap<This>(castThis(), name);
        }

        protected T getMap() {
            return map;
        }

        @SuppressWarnings("unchecked")
        private This castThis() {
            return (This) this;
        }
    }

    private static abstract class AbstractPropertyListBuilder<This extends AbstractPropertyListBuilder<This>> {
        private PropertyList list;

        private AbstractPropertyListBuilder(String name, String memberName) {
            this.list = new PropertyList(name);
            this.list.memberPropertyName = memberName;
        }

        /**
         * Adds a simple property. The name of the property is the member name defined by this list.
         * @param value the value of the simple property
         * @return continue with the definition
         */
        public This addSimple(Object value) {
            list.add(new PropertySimple(list.memberPropertyName, value));
            return castThis();
        }

        /**
         * Adds a number of simple properties. The names of the properties are the member name defined by this list.
         * @param values the values of the simple properties
         * @return continue with the definition
         */
        public This addSimples(Object... values) {
            for(Object v : values) {
                list.add(new PropertySimple(list.memberPropertyName, v));
            }

            return castThis();
        }

        /**
         * Starts defining a new sub map.
         * @return the builder of the map
         */
        public Builder.MapInList<This> openMap() {
            return new Builder.MapInList<This>(castThis(), list.memberPropertyName);
        }

        /**
         * Starts defining a new sub list.
         * @param memberName the names of the member properties of the sub list
         * @return the builder of the list
         */
        public Builder.ListInList<This> openList(String memberName) {
            return new Builder.ListInList<This>(castThis(), list.memberPropertyName, memberName);
        }

        protected PropertyList getList() {
            return list;
        }

        @SuppressWarnings("unchecked")
        private This castThis() {
            return (This) this;
        }
    }

    /**
     * A builder to easily build Configuration instances using a fluent API.
     */
    public static class Builder extends AbstractPropertyMapBuilder<Configuration, Builder> {

        public static class MapInMap<Parent extends AbstractPropertyMapBuilder<?, ?>> extends AbstractPropertyMapBuilder<PropertyMap, MapInMap<Parent>> {
            private Parent parent;

            private MapInMap(Parent parent, String name) {
                super(new PropertyMap(name));
                this.parent = parent;
            }

            /**
             * Closes the definition of the current map and returns to the parent context.
             * @return the parent context
             */
            public Parent closeMap() {
                parent.getMap().put(getMap());
                return parent;
            }
        }

        public static class MapInList<Parent extends AbstractPropertyListBuilder<?>> extends AbstractPropertyMapBuilder<PropertyMap, MapInList<Parent>> {
            private Parent parent;

            public MapInList(Parent parent, String name) {
                super(new PropertyMap(name));
                this.parent = parent;
            }

            /**
             * Closes the definition of the current map and returns to the parent context.
             * @return the parent context
             */
            public Parent closeMap() {
                parent.getList().add(getMap());
                return parent;
            }
        }

        public static class ListInMap<Parent extends AbstractPropertyMapBuilder<?, ?>> extends AbstractPropertyListBuilder<ListInMap<Parent>> {
            private Parent parent;

            private ListInMap(Parent parent, String name, String memberName) {
                super(name, memberName);
                this.parent = parent;
            }

            /**
             * Closes the definition of the current list and returns to the parent context.
             * @return the parent context
             */
            public Parent closeList() {
                parent.getMap().put(getList());
                return parent;
            }
        }

        public static class ListInList<Parent extends AbstractPropertyListBuilder<?>> extends AbstractPropertyListBuilder<ListInList<Parent>>  {
            private Parent parent;

            private ListInList(Parent parent, String name, String memberName) {
                super(name, memberName);
                this.parent = parent;
            }

            /**
             * Closes the definition of the current list and returns to the parent context.
             * @return the parent context
             */
            public Parent closeList() {
                parent.getList().add(getList());
                return parent;
            }
        }

        public class RawConfigurationBuilder {

            private RawConfiguration rawConfig;

            public RawConfigurationBuilder() {
                rawConfig = new RawConfiguration();
                rawConfig.setConfiguration(getMap());
            }

            public RawConfigurationBuilder withPath(String path) {
                rawConfig.setPath(path);
                return this;
            }

            public RawConfigurationBuilder withContents(String content, String sha256) {
                rawConfig.setContents(content, sha256);
                return this;
            }

            /**
             * Closes the definition of the current raw configuration and returns to the parent context.
             * @return the parent context
             */
            public Builder closeRawConfiguration() {
                getMap().getRawConfigurations().add(rawConfig);
                return Builder.this;
            }
        }

        public Builder() {
            super(new Configuration());
        }

        public Builder withNotes(String notes) {
            getMap().setNotes(notes);
            return this;
        }

        public Builder withVersion(long version) {
            getMap().setVersion(version);
            return this;
        }

        /**
         * Starts defining a new raw configuration that will become part of this configuration.
         * @return the builder of the raw configuration
         */
        public RawConfigurationBuilder openRawConfiguration() {
            return new RawConfigurationBuilder();
        }

        public Configuration build() {
            return getMap();
        }
    }

    @GeneratedValue(generator = "RHQ_CONFIG_ID_SEQ", strategy = GenerationType.AUTO)
    @Id
    private int id;

    // use the prop name as the map key
    @MapKey(name = "name")
    // CascadeType.REMOVE has been omitted, the cascade delete has been moved to the data model for performance
    @Cascade({ CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    @OneToMany(mappedBy = "configuration", fetch = FetchType.EAGER, orphanRemoval = true)
    @XmlTransient
    private Map<String, Property> properties = new LinkedHashMap<String, Property>(1);

    private class PropertiesProxy implements Collection<Property> {

        @Override
        public int size() {
            return properties==null ? 0: properties.size();
        }

        @Override
        public boolean isEmpty() {
            return properties == null || properties.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return properties==null ? false : properties.containsValue(o);
        }

        @Override
        public Iterator<Property> iterator() {
            if (properties == null) {
                // TODO replace with Collections.emptyIterator(); when we require java 7
                return Configuration.emptyIterator();
            }
            else {
                return properties.values().iterator();
            }
        }

        @Override
        public Object[] toArray() {
            return properties==null ? new Object[]{} :properties.values().toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return properties.values().toArray(a);
        }

        @Override
        public boolean add(Property e) {
            put(e);
            return true; //we always allow adding an element even if it is already present
        }

        @Override
        public boolean remove(Object o) {
            return properties.values().remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return properties.values().containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends Property> c) {
            boolean ret = false;
            for (Property p : c) {
                ret = ret || add(p);
            }

            return ret;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean ret = false;
            for (Object o : c) {
                ret = ret || remove(o);
            }

            return ret;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            boolean ret = false;
            ArrayList<Property> ps = new ArrayList<Property>(properties.values());
            for (Property p : ps) {
                if (!c.contains(p)) {
                    ret = ret || remove(p);
                }
            }

            return ret;
        }

        @Override
        public void clear() {
            if (properties!=null) {
                properties.clear();
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof Collection)) {
                return false;
            }

            return properties.values().equals(obj);
        }

        @Override
        public int hashCode() {
            return properties.values().hashCode();
        }

        @Override
        public String toString() {
            return properties.values().toString();
        }
    }

    private transient PropertiesProxy propertiesProxy;

    // If we don't actually get rid of this soon (say, 4.10) then we may want to make this
    // lazy, so we don't run around doing a fetch for every config we pull.  But that means
    // we have to protect against the unresolved proxy in various places (scrub the proxies)
    // which has its own issues.  Please let's kill this...
    @OneToMany(mappedBy = "configuration", fetch = FetchType.EAGER)
    @Cascade({ CascadeType.PERSIST, CascadeType.MERGE })
    private Set<RawConfiguration> rawConfigurations;

    @Column(name = "NOTES")
    private String notes;

    @Column(name = "VERSION")
    private long version;

    @Column(name = "CTIME")
    private long ctime = System.currentTimeMillis();

    @Column(name = "MTIME")
    private long mtime = System.currentTimeMillis();

    public static Builder builder() {
        return new Builder();
    }

    public Configuration() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getCreatedTime() {
        return this.ctime;
    }

    public long getModifiedTime() {
        return this.mtime;
    }

    @PrePersist
    void onPersist() {
        this.mtime = this.ctime = System.currentTimeMillis();
    }

    @PreUpdate
    void onUpdate() {
        this.mtime = System.currentTimeMillis();
    }

    /**
     * Adds the given property to this Configuration object. The property can be a
     * {@link PropertySimple simple property}, a {@link PropertyList list of properties} or a
     * {@link PropertyMap map of properties}.
     *
     * @param value the new property
     */
    @Override
    public void put(Property value) {
        Map<String, Property> map = getMap();
        if (value.getName()!=null) {
            map.put(value.getName().intern(),value);
        } else {
            map.put(value.getName(), value);
        }
        value.setConfiguration(this);
    }

    /**
     * Retrieves the given property from this Configuration object. The named property can be a
     * {@link PropertySimple simple property}, a {@link PropertyList list of properties} or a
     * {@link PropertyMap map of properties}.
     *
     * <p>Note that this only gets direct children of this Configuration. You cannot get a property from within a child
     * list or map via this method.</p>
     *
     * @param  name the name of the property to be retrieved from this configuration
     *
     * @return the named property or <code>null</code> if there was no direct child with the given name
     */
    @Override
    public Property get(String name) {
        return getMap().get(name);
    }

    /**
     * Removes the given property from this Configuration object. The named property can be a
     * {@link PropertySimple simple property}, a {@link PropertyList list of properties} or a
     * {@link PropertyMap map of properties}.
     *
     * <p>Note that this only removes direct children of this Configuration. You cannot remove a property from within a
     * child list or map via this method.</p>
     *
     * @param  name the name of the property to be removed from this configuration
     *
     * @return the named property or <code>null</code> if there was no direct child with the given name
     */
    public Property remove(String name) {
        return getMap().remove(name);
    }

    /**
     * Same as {@link #get(String)} except that it returns the object as a {@link PropertySimple}.
     *
     * @param  name the name of the simple property to be retrieved
     *
     * @return the simple property with the given name, or <code>null</code> if there was no simple property with the
     *         given name
     *
     * @throws ClassCastException if there was a property in this Configuration with the given name, but it was not of
     *                            type {@link PropertySimple}
     */
    @Override
    public PropertySimple getSimple(String name) {
        return (PropertySimple) getMap().get(name);
    }

    /**
     * Gets the value of the simple property with the specified name. If the property is not defined, null will be
     * returned.
     *
     * @param name the name of the simple property
     * @return the value of the simple property with the specified name, or null if the property is not defined
     *
     * @since 4.4
     */
    public String getSimpleValue(String name) {
        return getSimpleValue(name, null);
    }

    public String getSimpleValue(String name, @Nullable String defaultValue) {
        PropertySimple property = (PropertySimple) getMap().get(name);
        return ((property != null) && (property.getStringValue() != null)) ? property.getStringValue() : defaultValue;
    }

    /**
     * Set the value of a simple property to the specified value. If the property is not yet defined, it will be added.
     *
     * @param name the name of the simple property to be set
     * @param value the new value for the property
     *
     * @since 4.4
     */
    public void setSimpleValue(String name, String value) {
        PropertySimple property = getSimple(name);
        if (value == null) {
            if (property != null) {
                remove(name);
            }
        } else {
            if (property == null) {
                put(new PropertySimple(name, value));
            } else {
                property.setStringValue(value);
            }
        }
    }

    /**
     * Same as {@link #get(String)} except that it returns the object as a {@link PropertyList}.
     *
     * @param  name the name of the list property to be retrieved
     *
     * @return the list property with the given name, or <code>null</code> if there was no list property with the given
     *         name
     *
     * @throws ClassCastException if there was a property in this Configuration with the given name, but it was not of
     *                            type {@link PropertyList}
     */
    @Override
    public PropertyList getList(String name) {
        return (PropertyList) getMap().get(name);
    }

    /**
     * Same as {@link #get(String)} except that it returns the object as a {@link PropertyMap}.
     *
     * @param  name the name of the map property to be retrieved
     *
     * @return the map property with the given name, or <code>null</code> if there was no map property with the given
     *         name
     *
     * @throws ClassCastException if there was a property in this Configuration with the given name, but it was not of
     *                            type {@link PropertyMap}
     */
    @Override
    public PropertyMap getMap(String name) {
        return (PropertyMap) getMap().get(name);
    }

    /**
     * Returns the contents of this Configuration as a map. The keys to the map are the member properties' names and the
     * values are the properties themselves.
     *
     * <p><b>Warning:</b> Caution should be used when accessing the returned map. Please see
     * {@link PropertyMap the javadoc for this class} for more information.</p>
     *
     * @return the actual map of the property objects - this is <b>not</b> a copy
     */
    @Override
    @NotNull
    public Map<String, Property> getMap() {
        return this.properties;
    }

    /**
     * Returns the names of all properties that are <i>direct</i> children of this Configuration object.
     *
     * @return child property names
     */
    @NotNull
    public Collection<String> getNames() {
        return getMap().keySet();
    }

    /**
     * Returns all the <i>direct</i> children of this Configuration.
     * This collection is fully modifiable and can be added to and removed from.
     * <p>
     * When adding a property to the collection returned from this method, its
     * {@link Property#getConfiguration() configuration property} is set to this instance.
     *
     * @return all child properties of this Configuration
     */
    @NotNull
    @XmlElementRefs({ @XmlElementRef(name = "PropertyList", type = PropertyList.class),
        @XmlElementRef(name = "PropertySimple", type = PropertySimple.class),
        @XmlElementRef(name = "PropertyMap", type = PropertyMap.class) })
    public Collection<Property> getProperties() {
        if (propertiesProxy == null) {
            propertiesProxy = new PropertiesProxy();
        }
        return propertiesProxy;
    }

    /**
     * This method removes the existing set of properties and adds the properties
     * that got passed as argument to the list of properties.
     *
     * @param properties new set of properties
     */
    public void setProperties(Collection<Property> properties) {
        //propertiesProxy is a mere view of the properties map.
        //thus, if one obtained an instance of propertiesProxy from the #getProperties() method and then tried to
        //pass that instance to this method, the result would be that the set of properties would be effectively
        //cleared (due to the assignment of the new map to the properties field, of which the propertiesProxy is a
        //view). We can short-circuit that behavior though, because if we determine that the propertiesProxy is being
        //assigned as the "new" set of properties, we can return immediately. Logically, the passed in properties are
        //identical to the ones already present in the properties map in that case.
        if (propertiesProxy == properties) {
            return;
        }

        // Don't replace the possible Hibernate proxy when orphanRemoval=true. It can cause
        // "collection with cascade=all-delete-orphan was no longer referenced" exceptions.
        this.properties.clear();
        for (Property p : properties) {
            this.put(p);
        }
    }

    /**
     * Returns a map of all <i>direct</i> children of this Configuration that are of type {@link PropertyMap}. The
     * returned map is keyed on the {@link PropertyMap}'s names.
     *
     * @return map containing of all of the Configuration's direct {@link PropertyMap} children
     */
    @NotNull
    public Map<String, PropertyMap> getMapProperties() {
        Map<String, PropertyMap> map = new LinkedHashMap<String, PropertyMap>();
        for (Property prop : this.getProperties()) {
            if (prop instanceof PropertyMap) {
                map.put(prop.getName(), (PropertyMap) prop);
            }
        }

        return map;
    }

    /**
     * Returns a map of all <i>direct</i> children of this Configuration that are of type {@link PropertyList}. The
     * returned map is keyed on the {@link PropertyList}'s names.
     *
     * @return map containing of all of the Configuration's direct {@link PropertyList} children
     */
    @NotNull
    public Map<String, PropertyList> getListProperties() {
        Map<String, PropertyList> map = new LinkedHashMap<String, PropertyList>();
        for (Property prop : this.getProperties()) {
            if (prop instanceof PropertyList) {
                map.put(prop.getName(), (PropertyList) prop);
            }
        }

        return map;
    }

    /**
     * Returns a map of all <i>direct</i> children of this Configuration that are of type {@link PropertySimple}. The
     * returned map is keyed on the {@link PropertySimple}'s names.
     *
     * @return map containing of all of the Configuration's direct {@link PropertySimple} children
     */
    @NotNull
    public Map<String, PropertySimple> getSimpleProperties() {
        Map<String, PropertySimple> map = new LinkedHashMap<String, PropertySimple>();
        for (Property prop : this.getProperties()) {
            if (prop instanceof PropertySimple) {
                map.put(prop.getName(), (PropertySimple) prop);
            }
        }

        return map;
    }

    public Set<RawConfiguration> getRawConfigurations() {
        if (rawConfigurations == null) {
            rawConfigurations = new HashSet<RawConfiguration>(1);
        }
        return rawConfigurations;
    }

    public void addRawConfiguration(RawConfiguration rawConfiguration) {
        rawConfiguration.setConfiguration(this);
        if (rawConfigurations==null) {
            rawConfigurations = new HashSet<RawConfiguration>(1);
        }
        rawConfigurations.add(rawConfiguration);
    }

    public boolean removeRawConfiguration(RawConfiguration rawConfiguration) {
        if (rawConfigurations==null) {
            return true;
        }
        boolean removed = rawConfigurations.remove(rawConfiguration);
        if (removed) {
            rawConfiguration.setConfiguration(null);
        }
        if (rawConfigurations.isEmpty()) {
            rawConfigurations = Collections.emptySet();
        }
        return removed;
    }

    public void cleanoutRawConfiguration() {
        if (rawConfigurations!=null && rawConfigurations.isEmpty()) {
            rawConfigurations = null;
        }
    }

    /**
     * Warning: This should probably not be performed on an attached entity, it could replace a
     * Hibernate proxy, which can lead to issues (especially when orphanRemoval=true)
     */
    public void resize() {
        Map<String,Property> tmp =new LinkedHashMap<String, Property>(this.properties.size());
        tmp.putAll(this.properties);
        this.properties=tmp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * Clones this object in the same manner as {@link #deepCopy()}.
     *
     * @return a clone of this configuration
     *
     * @see    #deepCopy()
     */
    @SuppressWarnings("override")
    //@Override //GWT trips over this, WTH!
    public Configuration clone() {
        return deepCopy();
    }

    /**
     * Makes a fully independent copy of this object and returns it. This means all children N-levels deep in the
     * hierarchy of this Configuration object are copied.
     *
     * <p>This is the underlying implementation for the {@link #clone()} method.</p>
     *
     * @return a clone of this configuration
     */
    public Configuration deepCopy() {
        return deepCopy(true);
    }

    /**
     * Makes a fully independent copy of this object and returns it. This means all children N-levels deep in the
     * hierarchy of this Configuration object are copied.
     *
     * <p>If <code>keepIds</code> is <code>false</code>, then all IDs of all properties and the config object itself are
     * set to 0. Otherwise, they are kept the same and this method behaves the same as {@link #clone()}.
     *
     * @param  keepIds if <code>false</code>, zero out all IDs
     *
     * @return the new copy
     */
    public Configuration deepCopy(boolean keepIds) {
        Configuration copy = new Configuration();

        if (keepIds) {
            copy.id = this.id;
        }

        copy.notes = this.notes;
        copy.version = this.version;
        createDeepCopyOfProperties(copy, keepIds);
        createDeepCopyOfRawConfigs(copy, keepIds);

        return copy;
    }

    public Configuration deepCopyWithoutProxies() {
        Configuration copy = new Configuration();
        copy.notes = this.notes;
        copy.version = this.version;
        createDeepCopyOfProperties(copy, false);
        createDeepCopyOfRawConfigs(copy, false);

        return copy;
    }

    private void createDeepCopyOfRawConfigs(Configuration copy, boolean keepId) {
        if (rawConfigurations==null) {
            return;
        }
        for (RawConfiguration rawConfig : rawConfigurations) {
            copy.addRawConfiguration(rawConfig.deepCopy(keepId));
        }
    }

    private void createDeepCopyOfProperties(Configuration copy, boolean keepId) {
        for (Property property : this.properties.values()) {
            copy.put(property.deepCopy(keepId));
        }
    }

    /**
     * NOTE: A Configuration containing a null map is considered equal to a Configuration containing an empty map.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof Configuration)) {
            return false;
        }

        Configuration that = (Configuration) obj;

        if (this.properties == null || this.properties.isEmpty()){
            if ( that.properties== null || that.properties.isEmpty()) {
                return true;
            }
            else {
                return false;
            }
        } else {
            if (!this.properties.equals(that.properties)) {
                return false;
            }
        }

        boolean rcEquals=true;
        if (this.rawConfigurations!=null) {
            rcEquals = this.getRawConfigurations().equals(that.getRawConfigurations());
        }

        return rcEquals;
    }

    @Override
    public int hashCode() {
        int hc = 1;
        if (properties!=null ) {
            hc = properties.hashCode(); // TODO this requires loading of all properties and is expensive
        }

        if (rawConfigurations!=null) {
            int rchc = rawConfigurations.hashCode() ;
            hc = hc * rchc + 19;
    }
        return hc ;
    }

    @Override
    public String toString() {
        // Default to non-verbose (i.e. not printing the properties), since printing them makes toStrings extremely
        // verbose for large configs.
        final boolean verbose = false;
        return toString(verbose);
    }

    public String toString(boolean verbose) {
        StringBuilder builder = new StringBuilder("Configuration[id=").append(this.id);
        if (this.notes != null) {
            builder.append(", notes=").append(this.notes);
        }

        if (verbose) {
            builder.append(", properties[");
            boolean first = true;
            for (Property property : this.getMap().values()) {
                if (!first) {
                    builder.append(", ");
                } else {
                    first = false;
                }
                builder.append(property.getName());
                builder.append("=");
                String stringValue;
                if (property instanceof PropertySimple) {
                    stringValue = ((PropertySimple) property).getStringValue();
                } else {
                    stringValue = String.valueOf(property);
                }
                builder.append(stringValue);
            }
            builder.append("], rawConfigurations[");

            if (rawConfigurations!=null) {
                for (RawConfiguration rawConfig : rawConfigurations) {
                    builder.append("[").append(rawConfig.getPath()).append(", ").append(rawConfig.getSha256()).append("]");
                }
            }
            else {
                builder.append("-none-");
            }
            builder.append("]");
        }
        return builder.append("]").toString();
    }

    /**
     * This listener runs after jaxb unmarshalling and reconnects children properties to their parent configurations (as
     * we don't send them avoiding cyclic references).
     */
    public void afterUnmarshal(Object u, Object parent) {
        for (Property p : this.properties.values()) {
            p.setConfiguration(this);
        }
    }

    /**
     * Getter for the properties reference.
     *
     * @return {@code Map&lt;String, Property&gt;}
     */
    public Map<String, Property> getAllProperties() {
        if (this.properties==null) {
            return Collections.emptyMap();
        }
        return this.properties;
    }


    /*
     ************ Copied from JDK7, as JDK6 is lacking that
     * remove when we can require JDK 7
     */

    @SuppressWarnings("unchecked")
    @Deprecated
    public static <T> Iterator<T> emptyIterator() {
        return (Iterator<T>) EmptyIterator.EMPTY_ITERATOR;
    }

    @Deprecated
    private static class EmptyIterator<E> implements Iterator<E> {
        static final EmptyIterator<Object> EMPTY_ITERATOR
            = new EmptyIterator();

        public boolean hasNext() { return false; }
        public E next() { throw new NoSuchElementException(); }
        public void remove() { throw new IllegalStateException(); }
    }

}
