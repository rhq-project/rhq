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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

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
     * @return The <name,propDef> Map. This guarantees no ordering.
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
     * For public API compatibility.
     * @deprecated use {@link #setMap(Map)}
     */
    public void setPropertyDefinitions(@NotNull Map<String, PropertyDefinition> propertyDefinitions) {
        setMap(propertyDefinitions);
    }

    /**
     * This returned {@link Map} is backed by a {@link SortedMap} sorted on PropertyDefinition.order. This means that
     * result.keySet() and result.values() will be sorted by PropertyDefinition.order, ascending. Min(order) is 0.
     * <p>
     * For an unsorted Map use {@link #getMap()}.</p>
     * 
     * @return The map's property definitions sorted by PropertyDefinition.order, ascending. Min(order) is 0.
     */
    @NotNull
    public Map<String, PropertyDefinition> getPropertyDefinitions() {
        Map<String, PropertyDefinition> map = getMap();

        // if there is nothing to sort just return it.
        if (map.size() <= 1) {
            return map;
        }

        // a funky comparator that compares index order given prop def names (i.e. keys) 
        Comparator<String> orderComparator = new Comparator<String>() {
            public int compare(String o1, String o2) {
                return Integer.valueOf(get(o1).getOrder()).compareTo(get(o2).getOrder());
            }
        };

        final Map<String, PropertyDefinition> result = new TreeMap<String, PropertyDefinition>(orderComparator);
        for (String key : map.keySet()) {
            result.put(key, map.get(key));
        }

        return result;
    }

    /**
     * Convenience routine to get the ordered property definitions from the Map. 
     * 
     * @return The map's property definitions sorted by PropertyDefinition.order, ascending. Min(order) is 0.
     */
    @NotNull
    public Collection<PropertyDefinition> getOrderedPropertyDefinitions() {
        return getPropertyDefinitions().values();
    }

    /**
     * Convenience routine to get only the summary property definitions.
     *
     * @return the summary property definitions. If no property definitions were defined
     * as summary properties in the plugin descriptor, all property definitions will be returned.
     * The property definitions will be sorted by PropertyDefinition.order, ascending. Min(order) is 0.
     */
    @NotNull
    public List<PropertyDefinition> getSummaryPropertyDefinitions() {
        List<PropertyDefinition> result = new ArrayList<PropertyDefinition>();
        Collection<PropertyDefinition> propDefs = getOrderedPropertyDefinitions();

        for (PropertyDefinition pd : propDefs) {
            if (pd.isSummary()) {
                result.add(pd);
            }
        }

        if (result.isEmpty()) {
            result.addAll(propDefs);
        }

        return result;
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
     * If propertyDefinition.order is <= 0 or > Map.size() it will be set to the current number of propDefs for
     * the map (placing it at the end).  Otherwise, it will be inserted, incrementing the order of existing
     * Map entries.
     *  
     * @param propertyDefinition
     */
    public void put(PropertyDefinition propertyDefinition) {
        Map<String, PropertyDefinition> map = getMap();

        if (map.isEmpty()) {
            propertyDefinition.setOrder(0);

        } else {
            int order = propertyDefinition.getOrder();
            int size = map.size();

            if ((order <= 0) || (order >= size)) {
                propertyDefinition.setOrder(size);

            } else {
                // insert into existing ordering by bumping up existing entries
                for (PropertyDefinition p : map.values()) {
                    if (p.getOrder() >= order) {
                        p.setOrder(p.getOrder() + 1);
                    }
                }
            }
        }

        map.put(propertyDefinition.getName(), propertyDefinition);
        propertyDefinition.setParentPropertyMapDefinition(this);
    }
}