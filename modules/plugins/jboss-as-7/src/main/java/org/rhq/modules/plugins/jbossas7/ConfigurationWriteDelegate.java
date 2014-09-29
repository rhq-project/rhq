/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenResources;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Remove;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.modules.plugins.jbossas7.json.WriteAttribute;

public class ConfigurationWriteDelegate implements ConfigurationFacet {

    /** A map where this error message has been set must not be written to the AS
     * @see #updateHandlePropertyMapSpecial
     */
    public static final String LOGICAL_REMOVED = "__logicalRemoved";

    final Log log = LogFactory.getLog(this.getClass());

    protected Address address;
    protected ASConnection connection;
    protected ConfigurationDefinition configurationDefinition;

    private String namePropLocator;
    private String type;
    private boolean addNewChildren;
    private boolean addDeleteModifiedChildren;
    protected boolean createChildRequested = false;

    /**
     * Create a new configuration delegate, that reads the attributes for the resource at address.
     * @param configDef Configuration definition for the configuration
     * @param connection asConnection to use
     * @param address address of the resource.
     */
    public ConfigurationWriteDelegate(ConfigurationDefinition configDef, ASConnection connection, Address address) {
        this.configurationDefinition = configDef;
        this.connection = connection;
        this.address = address;
    }

    /**
     * Trigger loading of a configuration by talking to the remote resource.
     * @return The initialized configuration
     * @throws Exception If anything goes wrong.
     */
    public Configuration loadResourceConfiguration() throws Exception {
        throw new IllegalAccessException("Please use ConfigurationLoadDelegate");
    }

    /**
     * Write the configuration back to the AS. Care must be taken, not to send properties that
     * are read-only, as AS will choke on them.
     * @param report Report containing the new configuration
     */
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        Configuration conf = report.getConfiguration();
        CompositeOperation cop = updateGenerateOperationFromProperties(conf, address);

        Result result = connection.execute(cop);
        if (!result.isSuccess()) {
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            report.setErrorMessage(result.getFailureDescription());
        } else {
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
            // signal "need reload"
            if (result.isReloadRequired()) {
                PropertySimple oobMessage = new PropertySimple("__OOB",
                    "The server needs a reload for the latest changes to come effective.");
                conf.put(oobMessage);
            }
            if (result.isRestartRequired()) {
                PropertySimple oobMessage = new PropertySimple("__OOB",
                    "The server needs a restart for the latest changes to come effective.");
                conf.put(oobMessage);
            }
        }
    }

    protected CompositeOperation updateGenerateOperationFromProperties(Configuration conf, Address address) {
        CompositeOperation cop = new CompositeOperation();

        for (PropertyDefinition propDef : configurationDefinition.getNonGroupedProperties()) {
            updateProperty(conf, cop, propDef, address);
        }

        for (PropertyGroupDefinition pgd : configurationDefinition.getGroupDefinitions()) {
            String groupName = pgd.getName();
            namePropLocator = null;
            if (groupName.startsWith("children:")) { // children, where the key in key=value from the path is known
                type = groupName.substring("children:".length());
                if (type.contains(":")) {
                    namePropLocator = type.substring(type.indexOf(":") + 1);
                    if (namePropLocator.endsWith("+")) { // ending in +  -> we need to :add new entries
                        namePropLocator = namePropLocator.substring(0, namePropLocator.length() - 1);
                        addNewChildren = true;
                    } else if (namePropLocator.endsWith("+-")) { // ending in +-  -> we need to :add new entries and remove/add to modify
                        namePropLocator = namePropLocator.substring(0, namePropLocator.length() - 2);
                        addNewChildren = true;
                        addDeleteModifiedChildren = true;
                    } else {
                        addNewChildren = false;
                    }
                    type = type.substring(0, type.indexOf(":"));
                } else {
                    log.error("Group name " + groupName + " contains no property name locator ");
                    return cop;
                }

                List<PropertyDefinition> definitions = configurationDefinition.getPropertiesInGroup(groupName);
                for (PropertyDefinition def : definitions) {
                    updateProperty(conf, cop, def, address);
                }
            } else if (groupName.startsWith("child:")) { // one named child resource
                String subPath = groupName.substring("child:".length());
                if (!subPath.contains("="))
                    throw new IllegalArgumentException("subPath of 'child:' expression has no =");

                String condition = null;
                boolean groupEnabled = true;
                boolean isEnabledConditionFound = false;
                if (subPath.contains(":")) {
                    condition = subPath.substring(subPath.indexOf(':') + 1);
                    subPath = subPath.substring(0, subPath.indexOf(':')); // strip off additional trailing options
                }
                if (condition != null && condition.startsWith("enabled=")) {
                    isEnabledConditionFound = true;
                    String tmp = condition.substring("enabled=".length());
                    if (!tmp.contains("="))
                        throw new IllegalArgumentException("Condition " + condition
                            + " does not have a = between key and value part (" + tmp + ")");
                    String key = tmp.substring(0, tmp.indexOf('='));
                    String targetValue = tmp.substring(tmp.indexOf('=') + 1);
                    PropertySimple conditionProperty = conf.getSimple(key);
                    if (conditionProperty != null) {
                        String realValue = conditionProperty.getStringValue();
                        if (realValue != null && !targetValue.equals(realValue))
                            groupEnabled = false;
                    }
                }

                Address address1 = new Address(address);
                address1.addSegment(subPath);

                List<PropertyDefinition> definitions = configurationDefinition.getPropertiesInGroup(groupName);
                //  if this is a single map (not list of maps), then unwind
                if (definitions.size() == 1 && definitions.get(0) instanceof PropertyDefinitionMap) {

                    PropertyDefinitionMap definitionMap = (PropertyDefinitionMap) definitions.get(0);
                    String mapName = definitionMap.getName();

                    boolean isAddEnabled = mapName.endsWith("+");

                    if (groupEnabled) {
                        if (isAddEnabled) {
                            // check if this exists already
                            Operation testOp = new ReadResource(address1);
                            Result res = connection.execute(testOp);
                            if (!res.isSuccess()) {
                                // and try to add it
                                Operation add = new Operation("add", address1);
                                res = connection.execute(add);
                                if (!res.isSuccess()) {
                                    // Not much we can do here when the add fails
                                    log.error("Adding of node " + address1 + " failed");
                                }
                            }
                        }

                        definitions = new ArrayList<PropertyDefinition>(definitionMap.getOrderedPropertyDefinitions());
                        PropertyMap map = conf.getMap(mapName);
                        for (PropertyDefinition def : definitions) {
                            createWriteAttribute(cop, address1, def, map.get(def.getName()));
                        }
                    } else {
                        // group is not enabled, but add is, so lets remove the subpath
                        Remove op = new Remove(address1);
                        Result res = connection.execute(op);

                        // Not much we can do here with the result
                    }

                } else {
                    for (PropertyDefinition def : definitions) {
                        updateProperty(conf, cop, def, address1);
                    }
                }

            }// child: case    TODO handle attribute: case
            else {//handle the base case with no special case handling
                  //get the properties from within the group and update as usual.
                for (PropertyDefinition propDef : configurationDefinition.getPropertiesInGroup(groupName)) {
                    updateProperty(conf, cop, propDef, address);
                }
            }
        }

        return cop;
    }

    private void updateProperty(Configuration conf, CompositeOperation cop, PropertyDefinition propDef,
        Address baseAddress) {

        // Skip over read-only properties, the AS can not use them anyway
        if (propDef.isReadOnly())
            return;

        // Handle the special case
        String propDefName = propDef.getName();
        if (propDef instanceof PropertyDefinitionList && propDefName.startsWith("*")) {
            propDef = ((PropertyDefinitionList) propDef).getMemberDefinition();
            PropertyList pl = (PropertyList) conf.get(propDefName);

            // check if we need to see if that property exists - get the current state of affairs from the AS
            List<String> existingPropnames = new ArrayList<String>();
            if (addNewChildren) {
                Operation op = new ReadChildrenResources(baseAddress, type);
                Result tmp = connection.execute(op);
                if (tmp.isSuccess()) {
                    Map<String, Object> tmpResMap = (Map<String, Object>) tmp.getResult();
                    existingPropnames.addAll(tmpResMap.keySet());
                }
            }

            // Loop over the list - i.e. the individual rows that come from the server
            for (Property prop2 : pl.getList()) {
                updateHandlePropertyMapSpecial(cop, (PropertyMap) prop2, (PropertyDefinitionMap) propDef, baseAddress,
                    existingPropnames);
            }
            // now check about removed properties
            for (String existingName : existingPropnames) {
                boolean found = false;
                for (Property prop2 : pl.getList()) {
                    PropertyMap propMap2 = (PropertyMap) prop2;
                    String itemName = propMap2.getSimple(namePropLocator).getStringValue();
                    if (itemName == null) {
                        throw new IllegalArgumentException("Map contains no entry with name [" + namePropLocator + "]");
                    }
                    if (itemName.equals(existingName)) {
                        found = true;
                        break;
                    }
                }
                // We may still have an entry in the map, that does not
                // match an existing name, but is marked as immutable
                if (!found) {
                    for (Property prop2 : pl.getList()) {
                        PropertyMap propMap2 = (PropertyMap) prop2;
                        String itemName = propMap2.getSimple(namePropLocator).getStringValue();
                        String errorMessage = propMap2.getErrorMessage();
                        boolean contains = existingPropnames.contains(itemName);
                        if (!contains && LOGICAL_REMOVED.equals(errorMessage)) {
                            found = true; // we pretend this still exists on the server, so nothing to update
                        }
                    }
                }

                // In properties on server and not in map or immutable, lets remove it
                if (!found) {
                    Address tmpAddr = new Address(baseAddress);
                    tmpAddr.add(type, existingName);
                    Operation operation = new Operation("remove", tmpAddr);
                    cop.addStep(operation);
                }
            }

        } else {
            // Normal cases
            Property prop = conf.get(propDefName);

            createWriteAttribute(cop, baseAddress, propDef, prop);
        }
    }

    private void createWriteAttribute(CompositeOperation cop, Address baseAddress, PropertyDefinition propDef,
        Property prop) {
        if (prop instanceof PropertySimple && propDef instanceof PropertyDefinitionSimple) {
            createWriteAttributePropertySimple(cop, (PropertySimple) prop, (PropertyDefinitionSimple) propDef,
                baseAddress);
        } else if (prop instanceof PropertyList && propDef instanceof PropertyDefinitionList) {
            createWriteAttributePropertyList(cop, (PropertyList) prop, (PropertyDefinitionList) propDef, baseAddress);
        } else if (prop instanceof PropertyMap && propDef instanceof PropertyDefinitionMap) {
            createWriteAttributePropertyMap(cop, (PropertyMap) prop, (PropertyDefinitionMap) propDef, baseAddress);
        } else {
            String s = "Property and definition are not matching:\n";
            s += "Property: " + prop + "\n";
            s += "PropDef : " + propDef;
            throw new IllegalArgumentException(s);
        }
    }

    private void updateHandlePropertyMapSpecial(CompositeOperation cop, PropertyMap prop,
        PropertyDefinitionMap propDef, Address address, List<String> existingPropNames) {
        if (prop == null)
            return;

        // Don't try to send this map to the server
        if (LOGICAL_REMOVED.equals(prop.getErrorMessage()))
            return;

        Map<String, Object> results = prepareSimplePropertyMap(prop, propDef);
        if (prop.get(namePropLocator) == null) {
            throw new IllegalArgumentException("There is no element in the map with the name " + namePropLocator);
        }
        String key = ((PropertySimple) prop.get(namePropLocator)).getStringValue();

        Operation operation;
        Address addr = new Address(address);
        addr.add(type, key);

        if (!addNewChildren || existingPropNames.contains(key)) {
            // update existing entry
            if (addDeleteModifiedChildren) {

                operation = new Remove(addr);
                cop.addStep(operation);

                operation = new Operation("add", addr);
                for (Map.Entry<String, Object> entry : results.entrySet()) {
                    String key1 = entry.getKey();
                    Object value = getValueWithType(entry, propDef);
                    if (key1.endsWith(":expr")) {
                        key1 = key1.substring(0, key1.indexOf(':'));
                        Map<String, Object> tmp = new HashMap<String, Object>();
                        tmp.put("EXPRESSION_VALUE", value);

                        operation.addAdditionalProperty(key1, tmp);
                    } else {
                        operation.addAdditionalProperty(key1, value);
                    }
                }
                cop.addStep(operation);
            } else {
                for (Map.Entry<String, Object> entry : results.entrySet()) {
                    String key1 = entry.getKey();
                    Object value = getValueWithType(entry, propDef);
                    if (key1.endsWith(":expr")) {
                        key1 = key1.substring(0, key1.indexOf(':'));
                        Map<String, Object> tmp = new HashMap<String, Object>();
                        tmp.put("EXPRESSION_VALUE", value);
                        operation = new WriteAttribute(addr, key1, tmp);
                    } else {
                        operation = new WriteAttribute(addr, key1, value);
                    }
                    cop.addStep(operation);
                }
            }

        } else {
            // write new child ( :name+ case )
            operation = new Operation("add", addr);
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                String key1 = entry.getKey();
                Object value = getValueWithType(entry, propDef);
                if (key1.endsWith(":expr")) {
                    key1 = key1.substring(0, key1.indexOf(':'));
                    Map<String, Object> tmp = new HashMap<String, Object>();
                    tmp.put("EXPRESSION_VALUE", value);
                    operation.addAdditionalProperty(key1, tmp);
                } else {
                    operation.addAdditionalProperty(key1, value);
                }
            }

            cop.addStep(operation);
        }
    }

    private void createWriteAttributePropertySimple(CompositeOperation cop, PropertySimple property,
        PropertyDefinitionSimple propertyDefinition, Address address) {

        if (property.getName().endsWith(":ignore")) // Caller takes care
            return;

        if (propertyDefinition.isReadOnly() && !createChildRequested)
            return;

        //If the property value is null and the property is optional,
        //then send default value or null to the server
        if (property.getStringValue() == null && !propertyDefinition.isRequired()) {
            String name = property.getName();
            if (name.indexOf(':') != -1) {
                name = name.substring(0, name.indexOf(":"));
            }

            Operation writeAttribute = new WriteAttribute(address, name, null);
            cop.addStep(writeAttribute);
            return;
        }

        SimpleEntry<String, Object> entry = this.preparePropertySimple(property, propertyDefinition);
        Operation writeAttribute = new WriteAttribute(address, entry.getKey(), entry.getValue());
        cop.addStep(writeAttribute);
    }

    private void createWriteAttributePropertyList(CompositeOperation cop, PropertyList property,
        PropertyDefinitionList propertyDefinition, Address address) {

        SimpleEntry<String, List<Object>> entry = preparePropertyList(property, propertyDefinition);
        Operation writeAttribute = new WriteAttribute(address, entry.getKey(), entry.getValue());
        cop.addStep(writeAttribute);
    }

    private void createWriteAttributePropertyMap(CompositeOperation cop, PropertyMap property,
        PropertyDefinitionMap propertyDefinition, Address address) {

        // A map of simples can be me an empty map if all of the simples are optional and unset
        SimpleEntry<String, Map<String, Object>> entry = this.preparePropertyMap(property, propertyDefinition);
        Operation writeAttribute = new WriteAttribute(address, entry.getKey(), entry.getValue());
        cop.addStep(writeAttribute);
    }

    /**
     * Simple property parsing.
     *
     * @param property raw simple property
     * @param propertyDefinition property definition
     * @return parsed simple property
     */
    protected SimpleEntry<String, Object> preparePropertySimple(PropertySimple property,
        PropertyDefinitionSimple propertyDefinition) {

        SimpleEntry<String, Object> entry = null;

        String name = stripNumberIdentifier(property.getName());
        if (name.endsWith(":expr")) {

            String realName = name.substring(0, name.indexOf(":"));

            if (property.getStringValue() != null) {
                try {
                    if (propertyDefinition.getType().equals(PropertySimpleType.LONG)) {
                        Long num = Long.parseLong(property.getStringValue());
                    } else {
                        Integer num = Integer.parseInt(property.getStringValue());
                    }

                    entry = new SimpleEntry<String, Object>(realName, property.getStringValue());
                } catch (NumberFormatException nfe) {
                    // Not a number, and expressions are allowed, so send an expression
                    Map<String, String> expr = new HashMap<String, String>(1);
                    expr.put("EXPRESSION_VALUE", property.getStringValue());
                    entry = new SimpleEntry<String, Object>(realName, expr);
                }
            } else {
                entry = new SimpleEntry<String, Object>(realName, null);
            }
        } else {
            Object o;
            /*
                        If no value is given in the property and the property is required,
                        we'll take the default value from the definition. This can e.g. happen
                        when you have
                        <c:simple-property name="mode" required="true" type="string" readOnly="false" default="SYNC" defaultValue="SYNC">
                          <c:property-options>
                            <c:option value="SYNC"/>
                            <c:option value="ASYNC"/>
                          </c:property-options>
                        </c:simple-property>
                        and the user chooses to just keep the default choice in the ui
            */

            if (property.getStringValue() == null && propertyDefinition.isRequired()) {
                o = getObjectWithType(propertyDefinition, propertyDefinition.getDefaultValue());
            } else {
                o = getObjectWithType(propertyDefinition, property.getStringValue());
            }
            entry = new SimpleEntry<String, Object>(name, o);
        }

        return entry;
    }

    /**
     * List property parsing.
     *
     * @param property raw list property
     * @param propertyDefinition property definition
     * @return parsed list
     */
    protected SimpleEntry<String, List<Object>> preparePropertyList(PropertyList property,
        PropertyDefinitionList propertyDefinition) {

        PropertyDefinition memberDef = propertyDefinition.getMemberDefinition();
        List<Property> embeddedProps = property.getList();

        String propertyName = property.getName();

        if (propertyName.endsWith(":nullable")) {
            propertyName = propertyName.substring(0, propertyName.indexOf(":nullable"));

            if (embeddedProps.isEmpty()) {
                return new SimpleEntry<String, List<Object>>(propertyName, null);
            }
        }

        List<Object> values = new ArrayList<Object>();
        for (Property inner : embeddedProps) {
            if (memberDef instanceof PropertyDefinitionSimple) {
                PropertySimple ps = (PropertySimple) inner;
                if (ps.getStringValue() != null)
                    values.add(ps.getStringValue()); // TODO handling of optional vs required

            }
            if (memberDef instanceof PropertyDefinitionMap) {
                Map<String, Object> mapResult = null;
                if (memberDef.getName().endsWith(":collapsed")) {
                    mapResult = prepareCollapsedPropertyMap((PropertyMap) inner, (PropertyDefinitionMap) memberDef);
                } else {
                    mapResult = prepareSimplePropertyMap((PropertyMap) inner, (PropertyDefinitionMap) memberDef);
                }
                values.add(mapResult);
            }
        }

        propertyName = stripNumberIdentifier(propertyName);

        return new SimpleEntry<String, List<Object>>(propertyName, values);
    }

    /**
     * Map property parsing.
     *
     * @param property raw map property
     * @param propertyDefinition property definition
     * @return otherwise the parsed map, note that the map can be empty if no map properties were set
     */
    protected SimpleEntry<String, Map<String, Object>> preparePropertyMap(PropertyMap property,
        PropertyDefinitionMap propertyDefinition) {
        Map<String, Object> results;

        String propName = stripNumberIdentifier(property.getName());
        if (propName.endsWith(":collapsed")) {
            propName = propName.substring(0, propName.indexOf(':'));
            results = prepareCollapsedPropertyMap(property, propertyDefinition);
        } else {
            results = prepareSimplePropertyMap(property, propertyDefinition);
        }

        return new SimpleEntry<String, Map<String, Object>>(propName, results);
    }

    /**
     * Collapsed map property parsing.
     *
     * @param property raw map property
     * @param propertyDefinition property definition
     * @return parsed map
     */
    protected Map<String, Object> prepareCollapsedPropertyMap(PropertyMap property,
        PropertyDefinitionMap propertyDefinition) {
        String key = null;
        String value = null;

        for (Map.Entry<String, PropertyDefinition> entry : propertyDefinition.getMap().entrySet()) {
            PropertyDefinition def = entry.getValue();
            if (!def.getName().contains(":"))
                throw new IllegalArgumentException("Member names in a :collapsed map must end in :0 and :1");

            Property prop = property.get(def.getName());
            if (prop == null) {
                throw new IllegalArgumentException("Property " + def.getName() + " was null - must not happen");
            }

            PropertySimple ps = (PropertySimple) prop;
            if (def.getName().endsWith(":0"))
                key = ps.getStringValue();
            else if (def.getName().endsWith(":1"))
                value = ps.getStringValue(); // TODO other types?
            else
                throw new IllegalArgumentException("Member names in a :collapsed map must end in :0 and :1");
        }

        if (key != null) {
            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(key, value);

            return resultMap;
        } else {
            return null;
        }
    }

    /**
     * Simple map property parsing.
     *
     * @param property raw map property
     * @param propertyDefinition property definition
     * @return parsed map, can be empty if no members were set.
     */
    protected Map<String, Object> prepareSimplePropertyMap(PropertyMap property,
        PropertyDefinitionMap propertyDefinition) {
        Map<String, PropertyDefinition> memberDefinitions = propertyDefinition.getMap();

        Map<String, Object> results = new HashMap<String, Object>();
        for (String name : memberDefinitions.keySet()) {
            PropertyDefinition memberDefinition = memberDefinitions.get(name);

            if (memberDefinition.isReadOnly())
                continue;

            if (memberDefinition instanceof PropertyDefinitionSimple) {
                PropertyDefinitionSimple pds = (PropertyDefinitionSimple) memberDefinition;
                PropertySimple ps = (PropertySimple) property.get(name);
                if ((ps == null || ps.getStringValue() == null) && !pds.isRequired())
                    continue;
                if (ps != null)
                    results.put(name, ps.getStringValue());
            } else {
                log.error(" *** not yet supported *** : " + memberDefinition.getName());
            }
        }
        return results;
    }

    private Object getValueWithType(Map.Entry<String, Object> entry, PropertyDefinitionMap definitions) {

        PropertyDefinitionSimple pds = (PropertyDefinitionSimple) definitions.get(entry.getKey());
        if (!(entry.getValue() instanceof String)) {
            return entry.getValue();
        }

        String val = (String) entry.getValue();
        Object ret = getObjectWithType(pds, val);

        return ret;
    }

    private Object getObjectWithType(PropertyDefinitionSimple pds, String val) {
        PropertySimpleType type = pds.getType();
        Object ret;
        switch (type) {
        case STRING:
            ret = val;
            break;
        case INTEGER:
            ret = Integer.valueOf(val);
            break;
        case BOOLEAN:
            ret = Boolean.valueOf(val);
            break;
        case LONG:
            ret = Long.valueOf(val);
            break;
        case FLOAT:
            ret = Float.valueOf(val);
            break;
        case DOUBLE:
            ret = Double.valueOf(val);
            break;
        default:
            ret = val;
        }
        return ret;
    }

    /**
     * Strip :number from the property name.
     * The post-fixed number was added in the descriptor as unique identifier but it is not
     * needed (and will result in an error) when writing the property back to AS configuration.
     *
     * @param name property name
     * @return
     */
    private String stripNumberIdentifier(String name) {
        if (name.contains(":")) {
            try {
                Integer.parseInt(name.substring(name.lastIndexOf(':') + 1));
                name = name.substring(0, name.lastIndexOf(':'));
            } catch (Exception e) {
                //do nothing, this means the property name does not end with :number, so nothing needs to be stripped
            }
        }
        return name;
    }
}
