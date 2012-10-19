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
package org.rhq.plugins.perftest.configuration;

import java.util.Collection;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.AbstractPropertyMap;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;

/**
 * Basic implementation of the configuration factory that fills in a valid value for each defined property.
 *
 * @author Ian Springer
 */
public class SimpleConfigurationFactory implements ConfigurationFactory {
    // ConfigurationFactory Implementation  --------------------------------------------

    public Configuration generateConfiguration(ConfigurationDefinition definition) {
        Collection<PropertyDefinition> allDefinitions = definition.getPropertyDefinitions().values();
        Configuration configuration = new Configuration();
        for (PropertyDefinition propertyDefinition : allDefinitions)
            generateProperty(propertyDefinition, configuration);
        return configuration;
    }

    private static void generateProperty(PropertyDefinition propertyDefinition, AbstractPropertyMap parentPropertyMap) {
        Property property;
        if (propertyDefinition instanceof PropertyDefinitionSimple) {
            property = generatePropertySimple((PropertyDefinitionSimple)propertyDefinition);
        } else if (propertyDefinition instanceof PropertyDefinitionMap) {
            property = generatePropertyMap((PropertyDefinitionMap)propertyDefinition);
        } else if (propertyDefinition instanceof PropertyDefinitionList) {
            property = new PropertyList(propertyDefinition.getName());
            PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList) propertyDefinition;
            PropertyDefinition listMemberPropertyDefinition = propertyDefinitionList.getMemberDefinition();
            // If the property is a List of Maps, add 10 members to it, then recursively populate them.
            if (listMemberPropertyDefinition instanceof PropertyDefinitionMap) {
                PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap)listMemberPropertyDefinition;
                PropertyList propertyList = (PropertyList)property;
                for (int i = 0; i < 10; i++) {
                    PropertyMap memberProperty = generatePropertyMap(propertyDefinitionMap);
                    propertyList.add(memberProperty);
                }
            }
        } else {
            throw new IllegalStateException("Unsupported PropertyDefinition subclass: "
                + propertyDefinition.getClass().getName());
        }
        parentPropertyMap.put(property);
    }

    private static PropertySimple generatePropertySimple(PropertyDefinitionSimple propertyDefinitionSimple)
    {
        String value = null;
        switch (propertyDefinitionSimple.getType()) {
            case STRING: value = "blah"; break;
            case LONG_STRING: value = "line 1\nline 2\nline 3\n"; break;
            case PASSWORD: value = "secret"; break;
            case BOOLEAN: value = "true"; break;
            case INTEGER: value = "42"; break;
            case LONG: value = "55555555555555555555555"; break;
            case FLOAT: value = "3.14"; break;
            case DOUBLE: value = "333333333333333333333333.0"; break;
            case FILE: value = "C:/autoexec.bat"; break;
            case DIRECTORY: value = "/usr/bin"; break;
        }
        return new PropertySimple(propertyDefinitionSimple.getName(), value);
    }

    private static PropertyMap generatePropertyMap(
                                            PropertyDefinitionMap propertyDefinitionMap) {
        PropertyMap propertyMap = new PropertyMap(propertyDefinitionMap.getName());
        Map<String, PropertyDefinition> childPropertyDefinitions = propertyDefinitionMap.getPropertyDefinitions();
        if (childPropertyDefinitions.isEmpty()) {
            // open map
            for (int i = 0; i < 10; i++)
                propertyMap.put(new PropertySimple("openMapMember" + i, "value" + i));
        } else {
            for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values())
                generateProperty(childPropertyDefinition, propertyMap);
        }
        return propertyMap;
    }
}