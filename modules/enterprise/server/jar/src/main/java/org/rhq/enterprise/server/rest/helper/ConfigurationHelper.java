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

package org.rhq.enterprise.server.rest.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

/**
 * Helper class to deal with configuration objects
 * @author Heiko W. Rupp
 */
public class ConfigurationHelper {

    /**
     * Convert the passed map into a RHQ configuration object
     * @param in Map with items to convert. Map.Entry.Key is the name of the property and Map.Entry.Value the value
     * @return a new Configuration object
     */
    public static Configuration mapToConfiguration(Map<String, Object> in) {
        Configuration config = new Configuration();
        Set<String> mapKeys = in.keySet();
        for (String mapKey : mapKeys) {
            Object mapValue = in.get(mapKey);

            if (mapValue instanceof Map) {
                Map<String,Object> map = (Map<String, Object>) mapValue;
                PropertyMap propertyMap = getPropertyMap(mapKey, map);
                config.put(propertyMap);
            }
            else if (mapValue instanceof List) {
                List<Object> objects = (List<Object>) mapValue;
                PropertyList propertyList = getPropertyList(mapKey, objects);
                config.put(propertyList);
            }
            else {
                config.put(new PropertySimple(mapKey,mapValue));
            }
        }

        return config;

    }

    /**
     * convert passed configuration to generic map
     * @param configuration to be converted
     * @param definition to convert to proper types, can be null, but strict has to be set to false. If null, all simple properties will be rendered as strings 
     * @param strict if enabled all configuration properties must have appropriate property definitions
     * @return generic map
     * @throws IllegalArgumentException if strict is true and configuration contains property without matching property definition
     */
    public static Map<String,Object> configurationToMap(Configuration configuration, ConfigurationDefinition definition,
                                                        boolean strict) {

        Map<String,Object> result = new HashMap<String, Object>();

        if (configuration==null) {
            return result;
        }

        if (configuration.getProperties().isEmpty()) {
            return result;
        }

        for (Property property : configuration.getProperties()) {

            String propertyName = property.getName();
            PropertyDefinition propertyDefinition = null;
            if (definition != null) {
                propertyDefinition = definition.get(propertyName);
            }
            if (propertyDefinition==null) {
                if (strict) {
                    throw new IllegalArgumentException("No definition for property " + propertyName + "found");
                }
            }

            Object target = convertProperty(property, propertyDefinition, strict);
            result.put(propertyName,target);
        }

        return result;
    }

    private static Object convertProperty(Property property, PropertyDefinition propertyDefinition, boolean strict) {
        Object target;

        if (property instanceof PropertyMap) {
            PropertyMap propertyMap = (PropertyMap) property;
            target = getInnerMap(propertyMap,(PropertyDefinitionMap) propertyDefinition, strict);
        } else if (property instanceof PropertyList) {
            PropertyList propertyList = (PropertyList) property;
            target = getInnerList(propertyList, (PropertyDefinitionList)propertyDefinition, strict);
        } else {
            target= convertSimplePropertyValue((PropertySimple) property,
                ((PropertyDefinitionSimple) propertyDefinition), strict);
        }
        return target;
    }

    private static Map<String, Object> getInnerMap(PropertyMap propertyMap, PropertyDefinitionMap propertyDefinition,
                                                   boolean strict) {

        Map<String, Property> map = propertyMap.getMap();
        Map<String,Object> result = new HashMap<String, Object>(map.size());

        Set<String> names = map.keySet();
        for (String name : names ) {
            Property property = map.get(name);
            PropertyDefinition definition = null;
            if (propertyDefinition != null) {
                definition = propertyDefinition.get(name);
            }

            Object target = convertProperty(property,definition, strict);
            result.put(name,target);
        }

        return result;
    }

    private static List<Object> getInnerList(PropertyList propertyList, PropertyDefinitionList definition,
                                             boolean strict) {

        List<Object> result = new ArrayList<Object>(propertyList.getList().size());

        if (definition==null) {
            if (strict) {
                throw new IllegalArgumentException("No Definition exists for " + propertyList.getName());
            }
        }

        PropertyDefinition memberDefinition = null;
        if (definition != null) {
            memberDefinition = definition.getMemberDefinition();
        }
        for (Property property : propertyList.getList()) {
            Object target = convertProperty(property,memberDefinition, strict);
            result.add(target);
        }

        return result;
    }

    private static PropertyList getPropertyList(String propertyName, List<Object> objects) {
        PropertyList propertyList = new PropertyList(propertyName);

        Property target;
        for (Object o : objects) {
            if (o instanceof List) {
                // Not sure if we actually support that at all inside RHQ
                List list = (List) o;
                target = getPropertyList(propertyName,list); // TODO propertyName?
            } else if (o instanceof Map) {
                Map map = (Map) o;
                target = getPropertyMap(propertyName,map); // TODO propertyName?
            } else {
                target = new PropertySimple(propertyName,o);
            }
            propertyList.add(target);
        }
        return propertyList;
    }

    private static PropertyMap getPropertyMap(String propertyName, Map<String, Object> map) {
        PropertyMap propertyMap = new PropertyMap(propertyName);
        Set<String> keys = map.keySet();
        for (String key : keys) {
            Object value = map.get(key);
            Property target;
            if (value instanceof Map) {
                target = getPropertyMap(key, (Map)value);
            } else if (value instanceof List) {
                target = getPropertyList(key, (List)value);
            } else {
                target = new PropertySimple(key,value);
            }
            propertyMap.put(target);
        }
        return propertyMap;
    }

    /**
     * Convert the passed simple property into an object of a matching type. The
     * type is determined with the help of the passed definition
     * @param property Property to convert
     * @param definition Definition of the Property
     * @return Object with the correct type
     */
    public static Object convertSimplePropertyValue(PropertySimple property, PropertyDefinitionSimple definition,
        boolean strict) {

        if (definition == null && strict) {
            throw new IllegalArgumentException("No definition provided");
        }

        if (property==null) {
            return null;
        }

        if (definition == null) {
            return property.getStringValue();
        }

        PropertySimpleType type = definition.getType();
        String val = property.getStringValue();

        Object ret;

        switch (type) {
            case STRING:
                ret= val;
                break;
            case INTEGER:
                ret= Integer.valueOf(val);
                break;
            case BOOLEAN:
                ret= Boolean.valueOf(val);
                break;
            case LONG:
                ret= Long.valueOf(val);
                break;
            case FLOAT:
                ret= Float.valueOf(val);
                break;
            case DOUBLE:
                ret= Double.valueOf(val);
                break;
            default:
                ret= val;
        }
        return ret;
    }


    /**
     * Check that the passed configuration is valid wrt the passed definition
     * @param configuration A Configuration to check
     * @param definition A Definition to check the Configuration against
     * @return List of validation failure messages. List is empty if no errors were found.
     */
    public static List<String> checkConfigurationWrtDefinition(Configuration configuration,
            ConfigurationDefinition definition) {

        List<String> messages = new ArrayList<String>();

        if (configuration==null) {
            messages.add("Configuration is null");

        }

        if (definition==null) {
            messages.add("Definition is null");
        }

        if (configuration==null || definition==null) {
            return messages;
        }

        // Basic validation is done, now have a look at the properties

        for (PropertyDefinition propDef : definition.getPropertyDefinitions().values()) {
            String name = propDef.getName();
            Property property = configuration.get(name);

            checkProperty(messages, propDef, property);
        }


        return messages;
    }

    /**
     * Recursively check the passed property against the passed property definition
     * @param messages Validation error messages are added here
     * @param propertyDefinition The definition to check against
     * @param property The property to check
     */
    private static void checkProperty(List<String> messages, PropertyDefinition propertyDefinition,
                                      Property property) {

        String name = propertyDefinition.getName();

        // If a property is required and not present we can bail out early
        if (propertyDefinition.isRequired() && property ==null) {
            messages.add("Required property [" + name + "] not found");
            return;
        }

        // If a property is not required and is null, it is fine either
        if (!propertyDefinition.isRequired() && property==null) {
            return;
        }

        // Check if the property and definition are of the same kind (simple, map, list)
        boolean good = checkIfCompatible(propertyDefinition, property,messages);
        // We only need to do this dance if the kinds are matching
        if (good) {
            if (property instanceof PropertySimple) {
                checkDataTypeOfSimpleProperty((PropertyDefinitionSimple) propertyDefinition, (PropertySimple) property,
                    messages);
            } else if (property instanceof PropertyList) {
                PropertyList propertyList = (PropertyList) property;
                PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList) propertyDefinition;
                for (Property prop : propertyList.getList()) {
                    checkProperty(messages, propertyDefinitionList.getMemberDefinition(), prop);
                }
            } else if (property instanceof PropertyMap) {
                PropertyMap propertyMap = (PropertyMap) property;
                PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) propertyDefinition;
                for (Map.Entry<String,Property> entry : propertyMap.getMap().entrySet()) {
                    Property prop = entry.getValue();
                    PropertyDefinition definition = propertyDefinitionMap.get(name);
                    checkProperty(messages,definition,prop);
                }
            }
        }
    }

    /**
     * Check that for a Property that is defined with one of the non-string data types, the
     * stored value is actually valid according to this data type.
     * This also checks if a property is required, but its value is actually null.
     *
     * @param propDef Definition of the property, that contains the data type
     * @param property The property to check
     * @param messages Validation issues are added to this list.
     */
    private static void checkDataTypeOfSimpleProperty(PropertyDefinitionSimple propDef, PropertySimple property,
                                                      List<String> messages) {

        String prefix = "Property [" + property.getName() + "] is ";
        String val = property.getStringValue();

        // If a property is not required and its value is null, we can just return
        if (!propDef.isRequired() && property.getStringValue()==null) {
            return;
        }

        // If a property is required and its value is null, we can just return
        if (propDef.isRequired() && property.getStringValue()==null) {
            messages.add(prefix + "required but was 'null'");
            return;
        }


        switch (propDef.getType()) {
            case DOUBLE:
                try {
                    Double.parseDouble(property.getStringValue());
                } catch (NumberFormatException nfe ) {
                    messages.add(prefix + "no double : " + val);
                }
                break;
            case FLOAT:
                float f;
                try {
                    f = Float.parseFloat(property.getStringValue());
                } catch (NumberFormatException nfe ) {
                    messages.add(prefix + "no float : " + val);
                    break;
                }
                if (f < Float.MIN_VALUE || f > Float.MAX_VALUE) {
                    messages.add(prefix + "no valid float : " + val);
                }
                break;
            case INTEGER:
                try {
                    Integer.parseInt(property.getStringValue());
                } catch (NumberFormatException nfe ) {
                    messages.add(prefix + "no integer : " + val);
                }
                break;
            case LONG:
                try {
                    Long.parseLong(property.getStringValue());
                } catch (NumberFormatException nfe ) {
                    messages.add(prefix + "no long : " + val);
                }
                break;
            case BOOLEAN:
                String s = val.toLowerCase();
                if (!(s.equals("true") || s.equals("false"))) {
                    messages.add(prefix + "no boolean : " + val);
                }
                break;
            default:
                // Strings and long strings and directories and files
        }
    }

    /**
     * Check if the Kind of Definition and Property match. I.e. if a PropertyMap corresponds to a PropertyDefinitionMap
     * @param propDef PropertyDefinition to match
     * @param property Property to match with the definition
     * @param messages List of messages to add validation errors to.
     * @return true if the kinds are matching
     */
    private static boolean checkIfCompatible(final PropertyDefinition propDef, final Property property,
                                          final List<String> messages) {

        boolean good = false ;
        if (propDef instanceof PropertyDefinitionSimple && property instanceof PropertySimple) {
            good = true;
        } else if (propDef instanceof PropertyDefinitionMap && property instanceof PropertyMap) {
            good = true;
        } else if (propDef instanceof PropertyDefinitionList && property instanceof PropertyList) {
            good = true;
        }
        if (!good) {
            String name = propDef.getName();
            messages.add("The type of property for [" + name + "] does not match the definition");
        }
        return good;
    }
}
