/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.core.domain.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
public class ConfigurationUtility {

    /**
     * @deprecated do not create instances of this class. It is meant as a static utility class.
     */
    @Deprecated
    public ConfigurationUtility() {

    }

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
     * properties that are not defined in the top-level configuration Map or any sub-Maps, set them.
     * Map properties are set with an empty Map, and List properties with an empty List. By default, simple
     * properties that are missing will be created and set with a null value. However, if normalizeRequiredDefaults is true,
     * and a simple property is required with a default, this will set the required property to that default value.
     * If normalizeOptionalDefaults is true, and a simple property is not required but has a default, this will set the
     * optional property to that default. If a simple property does not have a default defined, no matter what those
     * "normalize" booleans are, the simple property will still be set to null since this method won't know what value
     * to set it to anyway.
     *
     * @param configuration             the configuration to be normalized (must not be null)
     * @param configurationDefinition   the configuration definition to normalize the configuration against (may be null)
     * @param normalizeRequiredDefaults if true, and a property is required, its default will be set as that property's value
     * @param normalizeOptionalDefaults if true, and a property is optional, its default will be set as that property's value
     */
    public static void normalizeConfiguration(Configuration configuration,
        ConfigurationDefinition configurationDefinition, boolean normalizeRequiredDefaults,
        boolean normalizeOptionalDefaults) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration parameter is null.");
        }
        if (configurationDefinition != null) {
            Map<String, PropertyDefinition> childPropertyDefinitions = configurationDefinition.getPropertyDefinitions();
            for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
                normalizeProperty(childPropertyDefinition, configuration, normalizeRequiredDefaults,
                    normalizeOptionalDefaults);
            }
        }
    }

    /**
     * Validate the given configuration according to the given configuration definition. That is, check that any
     * required properties in the top-level configuration Map or any sub-Maps, are defined and, in the case of simple
     * properties, check that they have a non-null value. A list of messages describing any errors that were found is
     * returned. Additionally, any undefined or null simple properties will be assigned a value of "".
     *
     * @param  configuration           the configuration to be validated (must not be null)
     * @param  configurationDefinition the configuration definition to validate the configuration against (may be null)
     *
     * @return a list of messages describing any errors that were found - will be empty if there are no messages
     */
    public static List<String> validateConfiguration(Configuration configuration,
        ConfigurationDefinition configurationDefinition) {
        return validateConfiguration(configuration, null, configurationDefinition);
    }

    /**
     * Validate the given configuration according to the given configuration definition. That is, check that any
     * required properties in the top-level configuration Map or any sub-Maps, are defined and, in the case of simple
     * properties, check that they have a non-null value. Optionally, ensure configuration does not alter readOnly
     * properties already defined in an existingConfiguration. A list of messages describing any errors that were found
     * is returned. Additionally, any undefined or null simple properties will be assigned a value of "".
     *
     * @param  configuration           the configuration to be validated (must not be null)
     * @param  currentConfiguration    if supplied, validate that readOnly properties do not differ between
     *                                 configuration and existingConfiguration. Ignored if null.
     * @param  configurationDefinition the configuration definition to validate the configuration against (may be null)
     *
     * @return a list of messages describing any errors that were found - will be empty if there are no messages
     */
    public static List<String> validateConfiguration(Configuration configuration, Configuration currentConfiguration,
        ConfigurationDefinition configurationDefinition) {
        List<String> errorMessages = new ArrayList<String>();
        if (configurationDefinition != null) {
            Map<String, PropertyDefinition> childPropertyDefinitions = configurationDefinition.getPropertyDefinitions();
            for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
                validateProperty(childPropertyDefinition, configuration, currentConfiguration, errorMessages);
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
                    .getMap();
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

    private static void normalizeProperty(PropertyDefinition propertyDefinition, AbstractPropertyMap parentPropertyMap,
        boolean normalizeRequiredDefaults, boolean normalizeOptionalDefaults) {
        if (parentPropertyMap.getMap().keySet().contains(propertyDefinition.getName())) // property is already set
        {
            if (propertyDefinition instanceof PropertyDefinitionSimple) {
                PropertySimple propertySimple = parentPropertyMap.getSimple(propertyDefinition.getName());
                String value = propertySimple.getStringValue();
                if (value != null) {
                    if (value.equals("")) {
                        // Normalize "" to null, since Oracle will do the same upon persistence.
                        propertySimple.setStringValue(null);
                    } else if (value.length() > PropertySimple.MAX_VALUE_LENGTH) {
                        // Truncate the value to the max length allowed by the DB schema.
                        propertySimple.setStringValue(value.substring(0, PropertySimple.MAX_VALUE_LENGTH));
                    }
                }
            }

            // If property is a Map, recurse into it and normalize its child properties.
            else if (propertyDefinition instanceof PropertyDefinitionMap) {
                PropertyMap propertyMap = parentPropertyMap.getMap(propertyDefinition.getName());
                PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) propertyDefinition;
                normalizePropertyMap(propertyMap, propertyDefinitionMap, false, false); // TODO do we want to pass normalizeRequired/OptionalDefaults?
            } else if (propertyDefinition instanceof PropertyDefinitionList) {
                PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList) propertyDefinition;
                PropertyDefinition listMemberPropertyDefinition = propertyDefinitionList.getMemberDefinition();

                // If property is a List of Maps, iterate the list and recurse into each Map and normalize its child properties.
                if (listMemberPropertyDefinition instanceof PropertyDefinitionMap) {
                    PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) listMemberPropertyDefinition;
                    PropertyList propertyList = parentPropertyMap.getList(propertyDefinition.getName());
                    for (Property property : propertyList.getList()) {
                        PropertyMap propertyMap = (PropertyMap) property;
                        normalizePropertyMap(propertyMap, propertyDefinitionMap, false, false); // TODO do we want to pass normalizeRequired/OptionalDefaults?
                    }
                }
            }
        } else // property is not set yet
        {
            Property property;
            if (propertyDefinition instanceof PropertyDefinitionSimple) {
                String value = null;
                if (normalizeRequiredDefaults || normalizeOptionalDefaults) {
                    if (propertyDefinition.isRequired()) {
                        if (normalizeRequiredDefaults) {
                            value = ((PropertyDefinitionSimple) propertyDefinition).getDefaultValue();
                        }
                    } else {
                        if (normalizeOptionalDefaults) {
                            value = ((PropertyDefinitionSimple) propertyDefinition).getDefaultValue();
                        }
                    }
                }
                property = new PropertySimple(propertyDefinition.getName(), value);
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
        PropertyDefinitionMap propertyDefinitionMap, boolean normalizeRequiredDefaults,
        boolean normalizeOptionalDefaults) {
        Map<String, PropertyDefinition> childPropertyDefinitions = propertyDefinitionMap.getMap();
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
            normalizeProperty(childPropertyDefinition, propertyMap, normalizeRequiredDefaults,
                normalizeOptionalDefaults);
        }
    }

    private static void validateProperty(PropertyDefinition propertyDefinition, AbstractPropertyMap parentPropertyMap,
        AbstractPropertyMap currentParentPropertyMap, List<String> errorMessages) {
        if (parentPropertyMap.getMap().keySet().contains(propertyDefinition.getName())) // property is already set
        {
            if (propertyDefinition instanceof PropertyDefinitionSimple) {
                PropertySimple propertySimple = parentPropertyMap.getSimple(propertyDefinition.getName());
                PropertySimple currentPropertySimple = (null == currentParentPropertyMap) ? null
                    : currentParentPropertyMap.getSimple(propertyDefinition.getName());
                validatePropertySimple(propertyDefinition, propertySimple, currentPropertySimple, errorMessages);
            }

            // If the property is a Map, validate it and recurse into it, validating its child properties.
            else if (propertyDefinition instanceof PropertyDefinitionMap) {
                PropertyMap propertyMap = parentPropertyMap.getMap(propertyDefinition.getName());
                PropertyMap currentPropertyMap = (null == currentParentPropertyMap) ? null : currentParentPropertyMap
                    .getMap(propertyDefinition.getName());
                PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) propertyDefinition;
                validatePropertyMap(propertyMap, currentPropertyMap, propertyDefinitionMap, errorMessages);

                // If the property is a List, validate each list member
            } else if (propertyDefinition instanceof PropertyDefinitionList) {
                PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList) propertyDefinition;
                PropertyList propertyList = parentPropertyMap.getList(propertyDefinition.getName());
                PropertyList currentPropertyList = (null == currentParentPropertyMap) ? null : currentParentPropertyMap
                    .getList(propertyDefinition.getName());

                if (propertyDefinitionList.isReadOnly()) {
                    if (null != currentPropertyList && !currentPropertyList.getList().isEmpty()) {
                        if (!currentPropertyList.getList().equals(propertyList.getList())) {
                            errorMessages.add("ReadOnly property '" + propertyDefinitionList.getName()
                                + "' has a value " + propertyList.getList() + " different than the current value "
                                + currentPropertyList.getList() + "]. It is not allowed to change.");
                        }
                    }
                }

                validatePropertyListSize(propertyList, propertyDefinitionList, errorMessages);

                PropertyDefinition listMemberPropertyDefinition = propertyDefinitionList.getMemberDefinition();
                for (Property property : propertyList.getList()) {
                    if (listMemberPropertyDefinition instanceof PropertyDefinitionSimple) {
                        validatePropertySimple(listMemberPropertyDefinition, (PropertySimple) property, null,
                            errorMessages);
                    } else if (listMemberPropertyDefinition instanceof PropertyDefinitionMap) {
                        validatePropertyMap((PropertyMap) property, null,
                            (PropertyDefinitionMap) listMemberPropertyDefinition, errorMessages);
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

    private static void validatePropertySimple(PropertyDefinition propertyDefinition, PropertySimple propertySimple,
        PropertySimple currentPropertySimple, List<String> errorMessages) {

        // make sure required properties have a value
        if (propertyDefinition.isRequired() && (propertySimple.getStringValue() == null)) {
            errorMessages.add("Required property '" + propertyDefinition.getName() + "' has a null value.");
            propertySimple.setStringValue("");
        }
        // make sure readOnly properties are not being changed
        if (propertyDefinition.isReadOnly() && null != currentPropertySimple) {
            String currentValue = currentPropertySimple.getStringValue();
            // if there is no current value allow an initial value to be set for the readOnly property.
            if (!(null == currentValue || currentValue.trim().isEmpty() || propertySimple.getStringValue().equals(
                currentValue))) {

                errorMessages.add("ReadOnly property '" + propertyDefinition.getName() + "' has a value ["
                    + propertySimple.getStringValue() + "] different than the current value [" + currentValue
                    + "]. It is not allowed to change.");
            }
        }
    }

    private static void validatePropertyMap(AbstractPropertyMap propertyMap, AbstractPropertyMap currentPropertyMap,
        PropertyDefinitionMap propertyDefinitionMap, List<String> errorMessages) {
        // if the entire map is read-only then the new map must match the current map if the current map is non-empty
        if (propertyDefinitionMap.isReadOnly() && null != currentPropertyMap && !currentPropertyMap.getMap().isEmpty()) {
            if (!propertyMap.getMap().equals(currentPropertyMap.getMap())) {
                errorMessages.add("ReadOnly property '" + propertyDefinitionMap.getName() + "' has a value "
                    + propertyMap.getMap() + " different than the current value " + currentPropertyMap.getMap()
                    + "]. It is not allowed to change.");
                return;
            }
        }

        Map<String, PropertyDefinition> childPropertyDefinitions = propertyDefinitionMap.getMap();
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
            validateProperty(childPropertyDefinition, propertyMap, currentPropertyMap, errorMessages);
        }
    }

    private static void validatePropertyListSize(PropertyList propertyList,
        PropertyDefinitionList propertyDefinitionList, List<String> errorMessages) {
        int listMin = propertyDefinitionList.getMin();
        int listMax = propertyDefinitionList.getMax();
        if (listMin == 0 && listMax == Integer.MAX_VALUE) {
            return;
        }
        int listSize = propertyList.getList().size();
        if (listMin == 0 && listMax < Integer.MAX_VALUE && listSize > listMax) {
            errorMessages.add("The list property '" + propertyDefinitionList.getName() + "' should contain " + listMax
                + " row(s) at most");
        } else if (listMin > 0 && listMax == Integer.MAX_VALUE && listSize < listMin) {
            errorMessages.add("The list property '" + propertyDefinitionList.getName() + "' should contain at least "
                + listMin + " row(s)");
        } else if (listSize < listMin || listSize > listMax) {
            errorMessages.add("The list property '%s' should contain a minimum of " + listMin + " and a maximum of "
                + listMax + " row(s)");
        }
    }
}
