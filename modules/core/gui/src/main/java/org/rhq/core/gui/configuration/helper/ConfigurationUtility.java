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
package org.rhq.core.gui.configuration.helper;

import java.util.LinkedList;

import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

/**
 * @author Ian Springer
 */
public class ConfigurationUtility
{
    public static PropertyDefinition getPropertyDefinitionForProperty(Property property,
                                                                      ConfigurationDefinition configurationDefinition) {
        LinkedList<Property> propertyHierarchy = getPropertyHierarchy(property);
        Property topLevelProperty = propertyHierarchy.get(0);
        PropertyDefinition propertyDefinition =
                configurationDefinition.getPropertyDefinitions().get(topLevelProperty.getName());
        for (int i = 1; i < propertyHierarchy.size(); i++)
        {
            Property subProperty = propertyHierarchy.get(i);
            if (propertyDefinition instanceof PropertyDefinitionMap) {
                if (((PropertyDefinitionMap)propertyDefinition).getPropertyDefinitions().isEmpty()) {
                    //this is an open map, i.e. a map with no child definitions, meaning that any simple property
                    //can go in it.
                    //let's create a definition that mimics the current property...
                    propertyDefinition = new PropertyDefinitionSimple(subProperty.getName(), null, false, PropertySimpleType.STRING);
                } else {
                    propertyDefinition = ((PropertyDefinitionMap)propertyDefinition).get(subProperty.getName());                    
                }
            } else if (propertyDefinition instanceof PropertyDefinitionList) {
                propertyDefinition = ((PropertyDefinitionList)propertyDefinition).getMemberDefinition();
            }
        }
        return propertyDefinition;
    }

    public static LinkedList<Property> getPropertyHierarchy(Property property)
    {
        LinkedList<Property> propertyHierarchy = new LinkedList<Property>();
        Property parentProperty = property;
        while ((parentProperty = getParentProperty(parentProperty)) != null) {
            propertyHierarchy.addFirst(parentProperty);
        }

        propertyHierarchy.add(property);
        return propertyHierarchy;
    }

    @Nullable
    public static Property getParentProperty(Property property) {
        Property parentProperty;
        if (property.getParentList() != null) {
            parentProperty = property.getParentList();
        } else if (property.getParentMap() != null) {
            parentProperty = property.getParentMap();
        } else {
            parentProperty = null;
        }

        return parentProperty;
    }
}

