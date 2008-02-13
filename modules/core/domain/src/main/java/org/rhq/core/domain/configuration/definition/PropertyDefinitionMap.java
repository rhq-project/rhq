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
package org.rhq.core.domain.configuration.definition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import org.jetbrains.annotations.NotNull;

/**
 * The definition of properties in a map. The map may be null allowing for the requirement of the map's existence but no
 * contraints on its contents. (i.e. an arbitrary map)
 *
 * @author Greg Hinkle
 */
@DiscriminatorValue("map")
@Entity(name = "PropertyDefinitionMap")
public class PropertyDefinitionMap extends PropertyDefinition {
    private static final long serialVersionUID = 1L;

    @MapKey(name = "name")
    @OneToMany(mappedBy = "parentPropertyMapDefinition", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    // Tell Hibernate to use a temporally-ordered map, whose order corresponds to the order the member properties were
    // specified in the plugin descriptor (because we initialize the map as a LinkedHashMap).
    @OrderBy
    private Map<String, PropertyDefinition> map;

    public PropertyDefinitionMap(@NotNull
    String name, String description, boolean required, PropertyDefinition... properties) {
        super(name, description, required);
        if (properties != null) {
            for (PropertyDefinition property : properties) {
                put(property);
            }
        }
    }

    protected PropertyDefinitionMap() {
    }

    @NotNull
    public Map<String, PropertyDefinition> getPropertyDefinitions() {
        if (this.map == null) {
            this.map = new LinkedHashMap<String, PropertyDefinition>();
        }

        return map;
    }

    public void setPropertyDefinitions(@NotNull
    Map<String, PropertyDefinition> propertyDefinitions) {
        this.map = propertyDefinitions;
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
        for (PropertyDefinition propertyDefinition : getPropertyDefinitions().values()) {
            if (propertyDefinition.isSummary()) {
                summaryDefinitions.add(propertyDefinition);
            }
        }

        if (summaryDefinitions.isEmpty()) {
            // No properties were defined as summary properties - return the full list of properties.
            summaryDefinitions.addAll(getPropertyDefinitions().values());
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
        return getPropertyDefinitions().get(name);
    }

    public void put(PropertyDefinition propertyDefinition) {
        getPropertyDefinitions().put(propertyDefinition.getName(), propertyDefinition);
        propertyDefinition.setParentPropertyMapDefinition(this);
    }
}