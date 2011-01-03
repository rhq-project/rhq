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
package org.rhq.core.clientapi.agent.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.configuration.AbstractPropertyMap;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyDefinitionDynamic;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;

/**
 * Utility methods for working with {@link Configuration}s.
 *
 * @author Ian Springer
 */
public abstract class ConfigurationUtility {

    /**
     * This will populate the given configuration definition with a default template.
     * A default template will only be created if one or more properties are required
     * or have default values. If no property definition is required or has a default value,
     * the default template will remain <code>null</code> in the given config definition.
     * 
     * Note that if the given configuration definition already has a default template defined
     * for it, this method is a no-op and will return immediately.
     * 
     * @param configDef the configuration definition whose default template is to be created and set
     */
    public static void initializeDefaultTemplate(ConfigurationDefinition configDef) {
        ConfigurationTemplate defaultTemplate = configDef.getDefaultTemplate();
        if (defaultTemplate == null) {
            Configuration defaultConfig = createDefaultConfiguration(configDef);
            // not everything should have a default template - only stuff that has default values
            if (!defaultConfig.getProperties().isEmpty()) {
                defaultTemplate = new ConfigurationTemplate(ConfigurationTemplate.DEFAULT_TEMPLATE_NAME,
                    ConfigurationTemplate.DEFAULT_TEMPLATE_NAME);
                defaultTemplate.setDefault(true);
                defaultTemplate.setConfiguration(defaultConfig);
                configDef.putTemplate(defaultTemplate);
            }
        }
        return;
    }

    /**
     * Given a configuration definition, this will build and return a "default configuration" that
     * can be validated with the definition. All required properties are set and all properties
     * that define a default value are also set. If a required property does not have a default
     * value defined in the definition, the property value will be set to <code>null</code>.
     *
     * Use this to help create the definition's default template.
     * 
     * @param configurationDefinition the configuration definition whose default configuration is to be created
     * @return configuration the default configuration
     */
    public static Configuration createDefaultConfiguration(ConfigurationDefinition configurationDefinition) {
        if (configurationDefinition == null) {
            throw new IllegalArgumentException("configurationDefinition == null");
        }
        Configuration defaultConfig = new Configuration();
        Map<String, PropertyDefinition> childPropertyDefinitions = configurationDefinition.getPropertyDefinitions();
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
            createDefaultProperty(childPropertyDefinition, defaultConfig);
        }
        return defaultConfig;
    }

    /**
     * "Normalize" the given configuration according to the given configuration definition. That is, for any optional
     * properties that are not defined in the top-level configuration Map or any sub-Maps, set them. Simple properties
     * are set with a null value, Map properties as an empty Map, and List properties as an empty List.
     *
     * @param configuration           the configuration to be normalized
     * @param configurationDefinition the configuration definition to normalize the configuration against
     */
    public static void normalizeConfiguration(@NotNull Configuration configuration,
        @Nullable ConfigurationDefinition configurationDefinition) {
        //noinspection ConstantConditions
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration parameter is null.");
        }
        if (configurationDefinition != null) {
            Map<String, PropertyDefinition> childPropertyDefinitions = configurationDefinition.getPropertyDefinitions();
            for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
                normalizeProperty(childPropertyDefinition, configuration);
            }
        }
    }

    /**
     * Validate the given configuration according to the given configuration definition. That is, check that any
     * required properties in the top-level configuration Map or any sub-Maps, are defined and, in the case of simple
     * properties, check that they have a non-null value. A list of messages describing any errors that were found is
     * returned. Additionally, any undefined or null simple properties will be assigned a value of "".
     *
     * @param  configuration           the configuration to be validated
     * @param  configurationDefinition the configuration definition to validate the configuration against
     *
     * @return a list of messages describing any errors that were found
     */
    @NotNull
    public static List<String> validateConfiguration(@NotNull Configuration configuration,
        @Nullable ConfigurationDefinition configurationDefinition) {
        List<String> errorMessages = new ArrayList<String>();
        if (configurationDefinition != null) {
            Map<String, PropertyDefinition> childPropertyDefinitions = configurationDefinition.getPropertyDefinitions();
            for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
                validateProperty(childPropertyDefinition, configuration, errorMessages);
            }
        }
        return errorMessages;
    }

    private static void createDefaultProperty(PropertyDefinition propertyDefinition,
        AbstractPropertyMap parentPropertyMap) {

        Property property = null;

        if (propertyDefinition instanceof PropertyDefinitionSimple) {
            String defaultValue = ((PropertyDefinitionSimple) propertyDefinition).getDefaultValue();
            if (defaultValue != null || propertyDefinition.isRequired()) {
                property = new PropertySimple(propertyDefinition.getName(), defaultValue);
            }
        } else if (propertyDefinition.isRequired()) {
            if (propertyDefinition instanceof PropertyDefinitionMap) {
                property = new PropertyMap(propertyDefinition.getName());
                Map<String, PropertyDefinition> childPropertyDefinitions = ((PropertyDefinitionMap) propertyDefinition)
                    .getPropertyDefinitions();
                for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
                    createDefaultProperty(childPropertyDefinition, (PropertyMap) property);
                }
            } else if (propertyDefinition instanceof PropertyDefinitionList) {
                property = new PropertyList(propertyDefinition.getName());
                PropertyDefinition listMemberPropertyDefinition = ((PropertyDefinitionList) propertyDefinition)
                    .getMemberDefinition();
                if (listMemberPropertyDefinition.isRequired()) {
                    if (listMemberPropertyDefinition instanceof PropertyDefinitionMap) {
                        // member property is a list-o-maps, create a default child map if appropriate
                        PropertyDefinitionMap listMemberDefinitionMap = (PropertyDefinitionMap) listMemberPropertyDefinition;
                        PropertyMap listMap = new PropertyMap(listMemberDefinitionMap.getName());
                        createDefaultProperty(listMemberDefinitionMap, listMap);
                        ((PropertyList) property).add(listMap);
                    } else if (listMemberPropertyDefinition instanceof PropertyDefinitionSimple) {
                        // member property is a simple, create a single list entry as its default if appropriate
                        PropertyDefinitionSimple listMemberDefinitionSimple = (PropertyDefinitionSimple) listMemberPropertyDefinition;
                        String defaultValue = listMemberDefinitionSimple.getDefaultValue();
                        if (defaultValue != null || listMemberDefinitionSimple.isRequired()) {
                            PropertySimple listSimple = new PropertySimple(listMemberDefinitionSimple.getName(),
                                defaultValue);
                            ((PropertyList) property).add(listSimple);
                        }
                    }
                }
            } else if (propertyDefinition instanceof PropertyDefinitionDynamic) {
                // Dynamic property values should simply be stored as simple
                property = new PropertySimple(propertyDefinition.getName(), null);
            } else {
                throw new IllegalStateException("Unsupported PropertyDefinition subclass: "
                    + propertyDefinition.getClass().getName());
            }
        }

        if (property != null) {
            parentPropertyMap.put(property);
        }
    }

    private static void normalizeProperty(PropertyDefinition propertyDefinition, AbstractPropertyMap parentPropertyMap) {
        if (parentPropertyMap.getMap().keySet().contains(propertyDefinition.getName())) // property is already set
        {
            if (propertyDefinition instanceof PropertyDefinitionSimple) {
                PropertySimple propertySimple = parentPropertyMap.getSimple(propertyDefinition.getName());
                String value = propertySimple.getStringValue();
                if ((value != null) && (value.length() > PropertySimple.MAX_VALUE_LENGTH)) {
                    // Truncate the value to the max length allowed by the DB schema.
                    propertySimple.setStringValue(value.substring(0, PropertySimple.MAX_VALUE_LENGTH));
                }
            }

            // If the property is a Map, recurse into it and normalize its child properties.
            else if (propertyDefinition instanceof PropertyDefinitionMap) {
                PropertyMap propertyMap = parentPropertyMap.getMap(propertyDefinition.getName());
                PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) propertyDefinition;
                normalizePropertyMap(propertyMap, propertyDefinitionMap);
            } else if (propertyDefinition instanceof PropertyDefinitionList) {
                PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList) propertyDefinition;
                PropertyDefinition listMemberPropertyDefinition = propertyDefinitionList.getMemberDefinition();

                // If the property is a List of Maps, iterate the list, and recurse into each Map and verify its child
                // properties.
                if (listMemberPropertyDefinition instanceof PropertyDefinitionMap) {
                    PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) listMemberPropertyDefinition;
                    PropertyList propertyList = parentPropertyMap.getList(propertyDefinition.getName());
                    for (Property property : propertyList.getList()) {
                        PropertyMap propertyMap = (PropertyMap) property;
                        normalizePropertyMap(propertyMap, propertyDefinitionMap);
                    }
                }
            }
        } else // property is not set yet
        {
            Property property;
            if (propertyDefinition instanceof PropertyDefinitionSimple) {
                property = new PropertySimple(propertyDefinition.getName(), null);
            } else if (propertyDefinition instanceof PropertyDefinitionMap) {
                property = new PropertyMap(propertyDefinition.getName());
            } else if (propertyDefinition instanceof PropertyDefinitionList) {
                property = new PropertyList(propertyDefinition.getName());
            } else if (propertyDefinition instanceof PropertyDefinitionDynamic) {
                // Dynamic property values should simply be stored as simple
                property = new PropertySimple(propertyDefinition.getName(), null);
            } else {
                throw new IllegalStateException("Unsupported PropertyDefinition subclass: "
                    + propertyDefinition.getClass().getName());
            }

            parentPropertyMap.put(property);
        }
    }

    private static void normalizePropertyMap(AbstractPropertyMap propertyMap,
        PropertyDefinitionMap propertyDefinitionMap) {
        Map<String, PropertyDefinition> childPropertyDefinitions = propertyDefinitionMap.getPropertyDefinitions();
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
            normalizeProperty(childPropertyDefinition, propertyMap);
        }
    }

    private static void validateProperty(PropertyDefinition propertyDefinition, AbstractPropertyMap parentPropertyMap,
        List<String> errorMessages) {
        if (parentPropertyMap.getMap().keySet().contains(propertyDefinition.getName())) // property is already set
        {
            if (propertyDefinition instanceof PropertyDefinitionSimple) {
                PropertySimple propertySimple = parentPropertyMap.getSimple(propertyDefinition.getName());
                if (propertyDefinition.isRequired() && (propertySimple.getStringValue() == null)) {
                    errorMessages.add("Required property '" + propertyDefinition.getName() + "' has a null value in "
                        + parentPropertyMap + ".");
                    propertySimple.setStringValue("");
                }
            }

            // If the property is a Map, recurse into it and validate its child properties.
            else if (propertyDefinition instanceof PropertyDefinitionMap) {
                PropertyMap propertyMap = parentPropertyMap.getMap(propertyDefinition.getName());
                PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) propertyDefinition;
                validatePropertyMap(propertyMap, propertyDefinitionMap, errorMessages);
            } else if (propertyDefinition instanceof PropertyDefinitionList) {
                PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList) propertyDefinition;
                PropertyDefinition listMemberPropertyDefinition = propertyDefinitionList.getMemberDefinition();

                // If the property is a List of Maps, iterate the list, and recurse into each Map and validate its child
                // properties.
                if (listMemberPropertyDefinition instanceof PropertyDefinitionMap) {
                    PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) listMemberPropertyDefinition;
                    PropertyList propertyList = parentPropertyMap.getList(propertyDefinition.getName());
                    for (Property property : propertyList.getList()) {
                        PropertyMap propertyMap = (PropertyMap) property;
                        validatePropertyMap(propertyMap, propertyDefinitionMap, errorMessages);
                    }
                }
            }
        } else // property is not set yet
        {
            if (propertyDefinition.isRequired()) {
                errorMessages.add("Required property '" + propertyDefinition.getName() + "' was not set in "
                    + parentPropertyMap + ".");
                if (propertyDefinition instanceof PropertyDefinitionSimple) {
                    PropertySimple propertySimple = new PropertySimple(propertyDefinition.getName(), "");
                    parentPropertyMap.put(propertySimple);
                }
            }
        }
    }

    private static void validatePropertyMap(AbstractPropertyMap propertyMap,
        PropertyDefinitionMap propertyDefinitionMap, List<String> errorMessages) {
        Map<String, PropertyDefinition> childPropertyDefinitions = propertyDefinitionMap.getPropertyDefinitions();
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
            validateProperty(childPropertyDefinition, propertyMap, errorMessages);
        }
    }
}