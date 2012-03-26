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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.jetbrains.annotations.NotNull;

/**
 * The definition of properties in a map. The map may be null allowing for the requirement of the map's existence but no
 * constraints on its contents. (i.e. an arbitrary map)
 *
 * @author Greg Hinkle
 */
@DiscriminatorValue("map")
@Entity(name = "PropertyDefinitionMap")
@XmlRootElement(name = "PropertyDefinitionMap")
@XmlSeeAlso({ PropertyDefinitionSimple.class, PropertyDefinitionList.class, PropertyDefinitionMap.class })
@XmlAccessorType(XmlAccessType.FIELD)
public class PropertyDefinitionMap extends PropertyDefinition {
    private static final long serialVersionUID = 1L;

    // use the propDef name as the map key
    @MapKey(name = "name")
    @OneToMany(mappedBy = "parentPropertyMapDefinition", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Map<String, PropertyDefinition> map;

    public PropertyDefinitionMap(@NotNull String name, String description, boolean required,
        PropertyDefinition... properties) {
        super(name, description, required);
        if (properties != null) {
            for (PropertyDefinition property : properties) {
                put(property);
            }
        }
    }

    protected PropertyDefinitionMap() {
    }

    /**
     * @return The <name,propDef> Mapping. This guarantees no ordering.
     */
    @NotNull
    public Map<String, PropertyDefinition> getMap() {
        if (this.map == null) {
            this.map = new HashMap<String, PropertyDefinition>();
        }

        return map;
    }

    public void setMap(@NotNull Map<String, PropertyDefinition> map) {
        this.map = map;
    }

    /**
     * Convenience routine to get the ordered properties from the Map. 
     * 
     * @return Not Null. The map's property definitions sorted by PropertyDefinition.order, ascending. Min(order) is 0.
     */
    public List<PropertyDefinition> getPropertyDefinitions() {
        final List<PropertyDefinition> propDefs = new ArrayList<PropertyDefinition>(getMap().values());

        Collections.sort(propDefs, new Comparator<PropertyDefinition>() {
            public int compare(PropertyDefinition o1, PropertyDefinition o2) {
                return Integer.valueOf(o1.getOrder()).compareTo(o2.getOrder());
            }
        });

        return propDefs;
    }

    /**
     * Helper to get only the summary properties for this map definition. If no properties were defined as summary
     * properties in the plugin descriptor, the full list of properties will be returned.
     *
     * @return the member properties that are marked as summary properties; the properties will be returned in the same
     *         order they were defined in the plugin descriptor; if no properties were defined as summary properties in
     *         the plugin descriptor, the full list of properties will be returned
     */
    @NotNull
    public List<PropertyDefinition> getSummaryPropertyDefinitions() {
        List<PropertyDefinition> summaryDefinitions = new ArrayList<PropertyDefinition>();
        for (PropertyDefinition propertyDefinition : getMap().values()) {
            if (propertyDefinition.isSummary()) {
                summaryDefinitions.add(propertyDefinition);
            }
        }

        if (summaryDefinitions.isEmpty()) {
            // No properties were defined as summary properties - return the full list of properties.
            summaryDefinitions.addAll(getMap().values());
        }

        return summaryDefinitions;
    }

    public PropertyDefinitionSimple getPropertyDefinitionSimple(String name) {
        return (PropertyDefinitionSimple) this.get(name);
    }

    public PropertyDefinitionList getPropertyDefinitionList(String name) {
        return (PropertyDefinitionList) this.get(name);
    }

    public PropertyDefinitionMap getPropertyDefinitionMap(String name) {
        return (PropertyDefinitionMap) this.get(name);
    }

    public PropertyDefinition get(String name) {
        return getMap().get(name);
    }

    /**
     * If an order index is not set on the propertyDefinition it will be set to the current number
     * of propDefs for the map. So, adding props to the map in the desired order will  
     * the 
     * @param propertyDefinition
     */
    public void put(PropertyDefinition propertyDefinition) {
        getMap().put(propertyDefinition.getName(), propertyDefinition);
        propertyDefinition.setParentPropertyMapDefinition(this);
    }
}