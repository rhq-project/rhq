/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.enterprise.server.configuration.util;

import org.jetbrains.annotations.NotNull;
import org.rhq.core.domain.configuration.AbstractPropertyMap;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

import java.util.List;
import java.util.Map;

/**
 * A class that provides static methods for masking and unmasking password properties within {@link Configuration}s,
 * The reason for masking a property's value is so that the current values of such properties cannot be viewed by a
 * user by viewing the HTML source of a Configuration GUI page, e.g.:
 *
 * &lt;input type="password" value="********" .../&gt;
 *
 * would be rendered, rather than:
 *
 * &lt;input type="password" value="ACTUAL_PASSWORD" .../&gt;
 *
 * @author Ian Springer
 */
public class ConfigurationMaskingUtility {

    /**
     * Mask the values of all simple properties of type PASSWORD in the configuration. The configuration does not
     * need to be normalized; that is, properties defined by the configuration definition do not need to exist in the
     * configuration.
     *
     * @param configuration the configuration to be masked
     * @param configurationDefinition the configuration definition corresponding to the specified configuration; this is
     *                                used to determine which properties to mask - all simple properties of type
     *                                PASSWORD at any level within the configuration are masked
     */
    public static void maskConfiguration(@NotNull
                                         Configuration configuration, @NotNull
    ConfigurationDefinition configurationDefinition) {
        if (configurationDefinition == null)
            return;

        Map<String, PropertyDefinition> childPropertyDefinitions = configurationDefinition.getPropertyDefinitions();
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
            maskProperty(childPropertyDefinition, configuration);
        }
    }

    /**
     * Unmask the values of all masked simple properties of type PASSWORD in the configuration. The configuration does not
     * need to be normalized; that is, properties defined by the configuration definition do not need to exist in the
     * configuration.
     *
     * @param configuration the configuration to be unmasked
     * @param unmaskedConfiguration the unmasked configuration that should be used as the reference to unmask the
     *                              configuration
     */
    public static void unmaskConfiguration(@NotNull
    Configuration configuration, @NotNull
    Configuration unmaskedConfiguration) {
        Map<String, Property> memberProperties = configuration.getAllProperties();
        for (Property memberProperty : memberProperties.values()) {
            unmaskProperty(memberProperty.getName(), configuration, unmaskedConfiguration);
        }
    }

    private static void maskProperty(PropertyDefinition propertyDefinition, AbstractPropertyMap parentPropertyMap) {
        if (parentPropertyMap.get(propertyDefinition.getName()) == null) {
            // If the property doesn't even exist, there's nothing to mask.
            return;
        }
        if (propertyDefinition instanceof PropertyDefinitionSimple) {
            PropertyDefinitionSimple propertyDefinitionSimple = (PropertyDefinitionSimple) propertyDefinition;
            if (propertyDefinitionSimple.getType() == PropertySimpleType.PASSWORD) {
                PropertySimple propertySimple = parentPropertyMap.getSimple(propertyDefinition.getName());
                propertySimple.mask();
            }
        }
        // If the property is a Map, recurse into it and mask its child properties.
        else if (propertyDefinition instanceof PropertyDefinitionMap) {
            PropertyMap propertyMap = parentPropertyMap.getMap(propertyDefinition.getName());
            PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) propertyDefinition;
            maskPropertyMap(propertyMap, propertyDefinitionMap);
        } else if (propertyDefinition instanceof PropertyDefinitionList) {
            PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList) propertyDefinition;
            PropertyDefinition listMemberPropertyDefinition = propertyDefinitionList.getMemberDefinition();
            // If the property is a List of Maps, iterate the list, and recurse into each Map and mask its child
            // properties.
            if (listMemberPropertyDefinition instanceof PropertyDefinitionMap) {
                PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) listMemberPropertyDefinition;
                PropertyList propertyList = parentPropertyMap.getList(propertyDefinition.getName());
                for (Property property : propertyList.getList()) {
                    PropertyMap propertyMap = (PropertyMap) property;
                    maskPropertyMap(propertyMap, propertyDefinitionMap);
                }
            }
        }
    }

    private static void maskPropertyMap(AbstractPropertyMap propertyMap, PropertyDefinitionMap propertyDefinitionMap) {
        Map<String, PropertyDefinition> childPropertyDefinitions = propertyDefinitionMap.getPropertyDefinitions();
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
            maskProperty(childPropertyDefinition, propertyMap);
        }
    }

    private static void unmaskProperty(String propertyName, AbstractPropertyMap parentPropertyMap,
                                       AbstractPropertyMap unmaskedParentPropertyMap) {
        Property property = parentPropertyMap.get(propertyName);
        if (property == null) {
            // The property doesn't even exist, so there's nothing to unmask.
            return;
        }
        if (property instanceof PropertySimple) {
            PropertySimple propertySimple = (PropertySimple) property;
            unmaskPropertySimple(propertySimple, unmaskedParentPropertyMap);
        }
        // If the property is a Map, recurse into it and unmask its child properties.
        else if (property instanceof PropertyMap) {
            PropertyMap propertyMap = (PropertyMap) property;
            PropertyMap unmaskedPropertyMap = unmaskedParentPropertyMap.getMap(property.getName());
            unmaskPropertyMap(propertyMap, unmaskedPropertyMap);
        } else if (property instanceof PropertyList) {
            PropertyList propertyList = (PropertyList) property;
            List<Property> memberProperties = propertyList.getList();
            // If the property is a List of Maps, iterate the list, and recurse into each Map and unmask its child
            // properties.
            if (!memberProperties.isEmpty() && memberProperties.get(0) instanceof PropertyMap) {
                PropertyList unmaskedPropertyList = unmaskedParentPropertyMap.getList(propertyList.getName());
                for (int i = 0; i < propertyList.getList().size(); i++) {
                    PropertyMap propertyMap = (PropertyMap) memberProperties.get(i);
                    PropertyMap unmaskedPropertyMap = (PropertyMap) unmaskedPropertyList.getList().get(i);
                    unmaskPropertyMap(propertyMap, unmaskedPropertyMap);
                }
            }
        }
    }

    private static void unmaskPropertySimple(PropertySimple propertySimple, AbstractPropertyMap unmaskedParentPropertyMap) {
        if (propertySimple.isMasked()) {
            PropertySimple unmaskedPropertySimple = unmaskedParentPropertyMap.getSimple(propertySimple.getName());
            String unmaskedValue = (unmaskedPropertySimple != null) ? unmaskedPropertySimple.getStringValue() : null;
            propertySimple.setStringValue(unmaskedValue);
        }
    }

    private static void unmaskPropertyMap(AbstractPropertyMap propertyMap, PropertyMap unmaskedPropertyMap) {
        Map<String, Property> memberProperties = propertyMap.getMap();
        for (Property memberProperty : memberProperties.values()) {
            unmaskProperty(memberProperty.getName(), propertyMap, unmaskedPropertyMap);
        }
    }

}
