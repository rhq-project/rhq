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
package org.rhq.core.domain.configuration.definition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
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
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The entire definition for a {@link import javax.persistence.CascadeType.Configuration}. This includes mapped property
 * definitions of arbitrary complexity, made up of {@link PropertyDefinitionSimple}s, {@link PropertyDefinitionList}s,
 * and {@link PropertyDefinitionMap}s. TODO GH: Groups aren't required right now... do we want to make them required?
 *
 * @author Greg Hinkle
 */
//@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@Entity
// TODO: non-admin version of the below query
@NamedQueries( {
    @NamedQuery(name = ConfigurationDefinition.QUERY_FIND_RESOURCE_BY_RESOURCE_TYPE_ID, query = "SELECT cd FROM ResourceType rt JOIN rt.resourceConfigurationDefinition cd "
        + "WHERE rt.id = :resourceTypeId"),
    @NamedQuery(name = ConfigurationDefinition.QUERY_FIND_PLUGIN_BY_RESOURCE_TYPE_ID, query = "SELECT cd FROM ResourceType rt JOIN rt.pluginConfigurationDefinition cd "
        + "WHERE rt.id = :resourceTypeId") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONFIG_DEF_ID_SEQ")
@Table(name = "RHQ_CONFIG_DEF")
public class ConfigurationDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_RESOURCE_BY_RESOURCE_TYPE_ID = "ConfigurationDefinition.findResourceByResourceTypeId";
    public static final String QUERY_FIND_PLUGIN_BY_RESOURCE_TYPE_ID = "ConfigurationDefinition.findPluginByResourceTypeId";

    @Column(name = "id")
    @GeneratedValue(generator = "SEQ", strategy = GenerationType.AUTO)
    @Id
    private int id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @MapKey(name = "name")
    @OneToMany(mappedBy = "configurationDefinition", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy
    private Map<String, PropertyDefinition> propertyDefinitions;

    @MapKey(name = "name")
    @OneToMany(mappedBy = "configurationDefinition", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy
    private Map<String, ConfigurationTemplate> templates;

    protected ConfigurationDefinition() {
        // JPA use only
    }

    public ConfigurationDefinition(@NotNull String name, String description) {
        this.name = name;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the properties for this configuration. This is the only method that returns a Collection, where an update
     * will be reflected in the database.
     *
     * @return
     */
    @NotNull
    public Map<String, PropertyDefinition> getPropertyDefinitions() {
        if (this.propertyDefinitions == null) {
            this.propertyDefinitions = new LinkedHashMap<String, PropertyDefinition>();
        }

        return this.propertyDefinitions;
    }

    public void setPropertyDefinitions(Map<String, PropertyDefinition> propertyDefinitions) {
        this.propertyDefinitions = propertyDefinitions;
    }

    public void put(PropertyDefinition propertyDefinition) {
        if (this.propertyDefinitions == null) {
            this.propertyDefinitions = new LinkedHashMap<String, PropertyDefinition>();
        }

        this.propertyDefinitions.put(propertyDefinition.getName(), propertyDefinition);
        propertyDefinition.setConfigurationDefinition(this);
    }

    public PropertyDefinition get(String name) {
        return this.propertyDefinitions.get(name);
    }

    public PropertyDefinitionSimple getPropertyDefinitionSimple(String name) {
        return (PropertyDefinitionSimple) this.propertyDefinitions.get(name);
    }

    public PropertyDefinitionList getPropertyDefinitionList(String name) {
        return (PropertyDefinitionList) this.propertyDefinitions.get(name);
    }

    public PropertyDefinitionMap getPropertyDefinitionMap(String name) {
        return (PropertyDefinitionMap) this.propertyDefinitions.get(name);
    }

    /**
     * Goes through the properties of this definition and builds a list of the groups that contain its properties. The
     * list is sorted by group order index, then by name.
     *
     * <p/>NOTE: updates of the result List will not be reflected in the database!
     *
     * @return all groups holding properties in this definition
     */
    @NotNull
    public List<PropertyGroupDefinition> getGroupDefinitions() {
        Set<PropertyGroupDefinition> groupSet = new HashSet<PropertyGroupDefinition>();
        for (PropertyDefinition propertyDefinition : getPropertyDefinitions().values()) {
            if (propertyDefinition.getPropertyGroupDefinition() != null) {
                groupSet.add(propertyDefinition.getPropertyGroupDefinition());
            }
        }

        List<PropertyGroupDefinition> groups = new ArrayList<PropertyGroupDefinition>(groupSet.size());
        for (PropertyGroupDefinition group : groupSet) {
            groups.add(group);
        }

        // TODO: Do this sorting at the DB query level instead.
        Collections.sort(groups, new GroupComparator());
        return groups;
    }

    /**
     * Retrieve property definitions for properties in a group with the provided name. The list is sorted by property
     * definition order index, then by name.
     *
     * <p/>NOTE: updates of the result List will not be reflected in the database!
     *
     * @param  groupName the name of the group
     *
     * @return the set of properties in a group with the provided name
     */
    @NotNull
    public List<PropertyDefinition> getPropertiesInGroup(String groupName) {
        List<PropertyDefinition> propertyDefinitions = new ArrayList<PropertyDefinition>();
        for (PropertyDefinition propertyDefinition : this.propertyDefinitions.values()) {
            PropertyGroupDefinition group = propertyDefinition.getPropertyGroupDefinition();
            if ((group != null) && group.getName().equals(groupName)) {
                propertyDefinitions.add(propertyDefinition);
            }
        }

        // TODO: Do this sorting at the DB query level instead.
        Collections.sort(propertyDefinitions, new PropertyDefinitionComparator());
        return propertyDefinitions;
    }

    /**
     * Retrieve property definitions for properties that are not in a group. The list is sorted by property definition
     * order index, then by name.
     *
     * <p/>DO NOT use this for updating the collection, as the updates will not be persisted!
     *
     * <p/>
     *
     * @return the set of properties that are not in a group
     *
     * @see    #propertyDefinitions
     * @see    #getPropertyDefinitions()
     */
    @NotNull
    public List<PropertyDefinition> getNonGroupedProperties() {
        List<PropertyDefinition> propertyDefinitions = new ArrayList<PropertyDefinition>();
        for (PropertyDefinition propertyDefinition : this.propertyDefinitions.values()) {
            if (propertyDefinition.getPropertyGroupDefinition() == null) {
                propertyDefinitions.add(propertyDefinition);
            }
        }

        // TODO: Do this sorting at the DB query level instead.
        Collections.sort(propertyDefinitions, new PropertyDefinitionComparator());
        return propertyDefinitions;
    }

    /**
     * This is a list of templates for this configuration definition. Each template relates to a single "configuration"
     * object which holds the values for the template.
     *
     * @return the set of templates for this configuration
     */
    @NotNull
    public Map<String, ConfigurationTemplate> getTemplates() {
        if (this.templates == null) {
            this.templates = new LinkedHashMap<String, ConfigurationTemplate>();
        }

        return templates;
    }

    public void putTemplate(ConfigurationTemplate template) {
        template.setConfigurationDefinition(this);
        getTemplates().put(template.getName(), template);
    }

    public ConfigurationTemplate removeTemplate(ConfigurationTemplate template) {
        return getTemplates().remove(template.getName());
    }

    /**
     * If there is a default template, return it, otherwise return null.
     *
     * @return a template if one is default, null otherwise
     */
    @Nullable
    public ConfigurationTemplate getDefaultTemplate() {
        return getTemplates().get(ConfigurationTemplate.DEFAULT_TEMPLATE_NAME);
    }

    /**
     * Returns the template indicated by the specified name
     *
     * @param  name name of the template to return
     *
     * @return <code>ConfigurationTemplate</code> with the specified name; <code>null</code> if no template by that name
     *         exists.
     */
    public ConfigurationTemplate getTemplate(@NotNull String name) {
        return getTemplates().get(name);
    }

    /**
     * Returns the set of all template names for this definition.
     *
     * @return set of template names.
     */
    public Set<String> templateNamesSet() {
        return getTemplates().keySet();
    }

    @Override
    public String toString() {
        return "ConfigurationDefinition[id=" + this.id + ", name=" + this.name + "]";
    }

    private class PropertyDefinitionComparator implements Comparator<PropertyDefinition> {
        public int compare(PropertyDefinition propDef1, PropertyDefinition propDef2) {
            return (propDef1.getOrder() == propDef2.getOrder()) ? propDef1.getName().compareTo(propDef2.getName())
                : Integer.valueOf(propDef1.getOrder()).compareTo(propDef2.getOrder());
        }
    }

    private class GroupComparator implements Comparator<PropertyGroupDefinition> {
        public int compare(PropertyGroupDefinition group1, PropertyGroupDefinition group2) {
            return (group1.getOrder() == group2.getOrder()) ? group1.getName().compareTo(group2.getName()) : Integer
                .valueOf(group1.getOrder()).compareTo(group2.getOrder());
        }
    }
}