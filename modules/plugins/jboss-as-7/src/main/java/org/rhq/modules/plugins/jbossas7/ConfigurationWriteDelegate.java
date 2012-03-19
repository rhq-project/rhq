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
import org.rhq.modules.plugins.jbossas7.json.Remove;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.modules.plugins.jbossas7.json.WriteAttribute;

public class ConfigurationWriteDelegate implements ConfigurationFacet {

    final Log log = LogFactory.getLog(this.getClass());

    private Address _address;
    private ASConnection connection;
    private ConfigurationDefinition configurationDefinition;
    private String namePropLocator;
    private String type;
    private boolean addNewChildren;
    private boolean addDeleteModifiedChildren;

    /**
     * Create a new configuration delegate, that reads the attributes for the resource at address.
     * @param configDef Configuration definition for the configuration
     * @param connection asConnection to use
     * @param address address of the resource.
     */
    public ConfigurationWriteDelegate(ConfigurationDefinition configDef, ASConnection connection, Address address) {
        this.configurationDefinition = configDef;
        this.connection = connection;
        this._address = address;
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

        CompositeOperation cop = updateGenerateOperationFromProperties(conf, _address);

        Result result = connection.execute(cop);
        if (!result.isSuccess()) {
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            report.setErrorMessage(result.getFailureDescription());
        }
        else {
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
            // TODO how to signal "need reload"
        }

    }

    protected CompositeOperation updateGenerateOperationFromProperties(Configuration conf, Address address) {

        CompositeOperation cop = new CompositeOperation();

        for (PropertyDefinition propDef : configurationDefinition.getNonGroupedProperties()) {

            updateProperty(conf, cop, propDef, address);
        }
        for (PropertyGroupDefinition pgd: configurationDefinition.getGroupDefinitions()) {
            String groupName = pgd.getName();
            namePropLocator = null;
            if (groupName.startsWith("children:")) { // children, where the key in key=value from the path is known
                type = groupName.substring("children:".length());
                if (type.contains(":")) {
                    namePropLocator = type.substring(type.indexOf(":") + 1);
                    if (namePropLocator.endsWith("+")) { // ending in +  -> we need to :add new entries
                        namePropLocator=namePropLocator.substring(0,namePropLocator.length()-1);
                        addNewChildren = true;
                    }
                    else if (namePropLocator.endsWith("+-")) { // ending in +-  -> we need to :add new entries and remove/add to modify
                        namePropLocator=namePropLocator.substring(0,namePropLocator.length()-2);
                        addNewChildren = true;
                        addDeleteModifiedChildren = true;
                    }
                    else {
                        addNewChildren = false;
                    }
                    type = type.substring(0, type.indexOf(":"));
                }
                else {
                    log.error("Group name " + groupName + " contains no property name locator ");
                    return cop;
                }
                List<PropertyDefinition> definitions = configurationDefinition.getPropertiesInGroup(groupName);
                for (PropertyDefinition def : definitions) {
                    updateProperty(conf,cop,def, address);
                }
            } if (groupName.startsWith("child:")) { // one named child resource
                String subPath = groupName.substring("child:".length());
                if (!subPath.contains("="))
                    throw new IllegalArgumentException("subPath of 'child:' expression has no =");

                Address address1 = new Address(address);
                address1.addSegment(subPath);

                List<PropertyDefinition> definitions = configurationDefinition.getPropertiesInGroup(groupName);
                for (PropertyDefinition def : definitions) {
                    updateProperty(conf,cop,def, address1);
                }


            } // TODO handle attribute: case
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
                Operation op = new ReadChildrenResources(baseAddress,type);
                Result tmp = connection.execute(op);
                if (tmp.isSuccess()) {
                    Map<String,Object> tmpResMap = (Map<String, Object>) tmp.getResult();
                    existingPropnames.addAll(tmpResMap.keySet());
                }
            }

            // Loop over the list - i.e. the individual rows that come from the server
            for (Property prop2 : pl.getList()) {
                updateHandlePropertyMapSpecial(cop, (PropertyMap) prop2, (PropertyDefinitionMap) propDef, baseAddress,
                        existingPropnames);
            }
            // now check about removed properties
            for (String existingName : existingPropnames ) {
                boolean found=false;
                for (Property prop2 : pl.getList()) {
                    PropertyMap propMap2 = (PropertyMap) prop2;
                    String itemName = propMap2.getSimple(namePropLocator).getStringValue();
                    if (itemName==null) {
                        throw new IllegalArgumentException("Map contains no entry with name [" + namePropLocator + "]");
                    }
                    if (itemName.equals(existingName)) {
                        found=true;
                        break;
                    }
                }

                if (!found) {
                    Address tmpAddr = new Address(baseAddress);
                    tmpAddr.add(type, existingName);
                    Operation operation = new Operation("remove",tmpAddr);
                    cop.addStep(operation);
                }
            }

        }
        else {
            // Normal cases
            Property prop = conf.get(propDefName);

            if (prop instanceof PropertySimple && propDef instanceof PropertyDefinitionSimple) {
                updateHandlePropertySimple(cop, (PropertySimple) prop, (PropertyDefinitionSimple) propDef, baseAddress);
            }
            else if (prop instanceof PropertyList && propDef instanceof PropertyDefinitionList) {
                updateHandlePropertyList(cop, (PropertyList) prop, (PropertyDefinitionList) propDef, baseAddress);
            }
            else if (prop instanceof PropertyMap && propDef instanceof PropertyDefinitionMap) {
                updateHandlePropertyMap(cop,(PropertyMap)prop,(PropertyDefinitionMap)propDef, baseAddress);
            }
            else {
                String s = "Property and definition are not matching:\n";
                s += "Property: " + prop + "\n";
                s += "PropDef : " + propDef;
                throw new IllegalArgumentException(s);
            }
        }
    }

    private void updateHandlePropertyMap(CompositeOperation cop, PropertyMap prop, PropertyDefinitionMap propDef,
                                         Address address) {

        String propName = prop.getName();
        Operation writeAttribute;
        Map<String,Object> results;
        if (propName.endsWith(":collapsed")) {
            String realName = propName.substring(0, propName.indexOf(':'));
            results = handleCollapsedMap(prop, propDef);
            writeAttribute = new WriteAttribute(address, realName,results);
        }
        else {
            results = updateHandleMap(prop,propDef, address);
            writeAttribute = new WriteAttribute(address, propName,results);
        }
        cop.addStep(writeAttribute);
    }

    private Map<String, Object> handleCollapsedMap(PropertyMap propertyMap, PropertyDefinitionMap propertyDefinitionMap) {

        String first=null;
        String second=null;
        for (Map.Entry<String,PropertyDefinition> entry : propertyDefinitionMap.getPropertyDefinitions().entrySet()) {
            PropertyDefinition def = entry.getValue();
            if (!def.getName().contains(":"))
                throw new IllegalArgumentException("Member names in a :collapsed map must end in :0 and :1");

            Property prop = propertyMap.get(def.getName());
            if (prop==null) {
                throw new IllegalArgumentException("Property " + def.getName() + " was null - must not happen");
            }

            PropertySimple ps = (PropertySimple) prop;
            if (def.getName().endsWith(":0"))
                first = ps.getStringValue();
            else if (def.getName().endsWith(":1"))
                second = ps.getStringValue(); // TODO other types?
            else
                throw new IllegalArgumentException("Member names in a :collapsed map must end in :0 and :1");
        }


        Map<String,Object> resultMap = new HashMap<String,Object>();
        resultMap.put(first, second);

        return resultMap;
    }

    private void updateHandlePropertyMapSpecial(CompositeOperation cop, PropertyMap prop, PropertyDefinitionMap propDef,
                                                Address address, List<String> existingPropNames) {
        Map<String,Object> results = updateHandleMap(prop,propDef, address);
        if (prop.get(namePropLocator)==null) {
            throw new IllegalArgumentException("There is no element in the map with the name " + namePropLocator);
        }
        String key= ((PropertySimple)prop.get(namePropLocator)).getStringValue();


        Operation operation;
        Address addr = new Address(address);
        addr.add(type,key);

        if (!addNewChildren || existingPropNames.contains(key)) {
            // update existing entry
            if (addDeleteModifiedChildren) {

                operation = new Remove(addr);
                cop.addStep(operation);

                operation = new Operation("add",addr);
                for (Map.Entry<String,Object> entry : results.entrySet()) {
                    String key1= entry.getKey();
                    Object value = getValueWithType(entry, propDef);
                    if (key1.endsWith(":expr")) {
                        key1 = key1.substring(0, key1.indexOf(':'));
                        Map<String,Object> tmp = new HashMap<String, Object>();
                        tmp.put("EXPRESSION_VALUE", value);

                        operation.addAdditionalProperty(key1,tmp);
                    } else {
                        operation.addAdditionalProperty(key1, value);
                    }
                }
                cop.addStep(operation);
            }
            else {
                for (Map.Entry<String,Object> entry : results.entrySet()) {
                    String key1 = entry.getKey();
                    Object value = getValueWithType(entry,propDef);
                    if (key1.endsWith(":expr")) {
                        key1 = key1.substring(0, key1.indexOf(':'));
                        Map<String,Object> tmp = new HashMap<String, Object>();
                        tmp.put("EXPRESSION_VALUE", value);
                        operation = new WriteAttribute(addr, key1,tmp);
                    } else {
                        operation = new WriteAttribute(addr, key1, value);
                    }
                    cop.addStep(operation);
                }
            }

        }
        else {
            // write new child ( :name+ case )
            operation = new Operation("add",addr);
            for (Map.Entry<String,Object> entry : results.entrySet()) {
                String key1 = entry.getKey();
                Object value = getValueWithType(entry,propDef);
                if (key1.endsWith(":expr")) {
                    key1 = key1.substring(0, key1.indexOf(':'));
                    Map<String,Object> tmp = new HashMap<String, Object>();
                    tmp.put("EXPRESSION_VALUE", value);
                    operation.addAdditionalProperty(key1,tmp);
                } else {
                    operation.addAdditionalProperty(key1, value);
                }
            }
                cop.addStep(operation);
        }
    }


    private void updateHandlePropertyList(CompositeOperation cop, PropertyList prop, PropertyDefinitionList propDef,
                                          Address address) {
        PropertyDefinition memberDef = propDef.getMemberDefinition();

        // We need to collect the list members, create an array and attach this to the cop

        List<Property> embeddedProps = prop.getList();
        List<Object> values = new ArrayList<Object>();
        for (Property inner : embeddedProps) {
            if (memberDef instanceof PropertyDefinitionSimple) {
                PropertySimple ps = (PropertySimple) inner;
                if (ps.getStringValue()!=null)
                    values.add(ps.getStringValue()); // TODO handling of optional vs required

            }
            if (memberDef instanceof PropertyDefinitionMap) {
                Map<String,Object> mapResult = updateHandleMap((PropertyMap) inner,(PropertyDefinitionMap)memberDef,
                        address);
                values.add(mapResult);
            }
        }
        Operation writeAttribute = new WriteAttribute(address,prop.getName(),values);
        cop.addStep(writeAttribute);
    }


    private void updateHandlePropertySimple(CompositeOperation cop, PropertySimple propertySimple,
                                            PropertyDefinitionSimple propDef, Address address) {

        // If the property value is null and the property is optional, skip too
        if (propertySimple.getStringValue()==null && !propDef.isRequired())
            return;

        Operation writeAttribute = new WriteAttribute(address);
        String name = propertySimple.getName();
        if (name.endsWith(":expr")) {

            String realName = name.substring(0,name.indexOf(":"));
            try {
                Integer num = Integer.parseInt(propertySimple.getStringValue());
                writeAttribute.addAdditionalProperty("name",realName);
                writeAttribute.addAdditionalProperty("value",propertySimple.getStringValue());
            } catch (NumberFormatException nfe) {
                // Not a number, and expressions are allowed, so send an expression
                Map<String,String> expr = new HashMap<String, String>(1);
                expr.put("EXPRESSION_VALUE",propertySimple.getStringValue());
                writeAttribute.addAdditionalProperty("name", realName);
                writeAttribute.addAdditionalProperty("value",expr);
            }
        } else {
            writeAttribute.addAdditionalProperty("name",name);
            writeAttribute.addAdditionalProperty("value",propertySimple.getStringValue());
        }
        cop.addStep(writeAttribute);
    }

    private Map<String, Object> updateHandleMap(PropertyMap map, PropertyDefinitionMap mapDef, Address address) {
        Map<String,PropertyDefinition> memberDefinitions = mapDef.getPropertyDefinitions();

        Map<String,Object> results = new HashMap<String,Object>();
        for (String name : memberDefinitions.keySet()) {
            PropertyDefinition memberDefinition = memberDefinitions.get(name);

            if (memberDefinition.isReadOnly())
                continue;

            if (memberDefinition instanceof PropertyDefinitionSimple) {
                PropertyDefinitionSimple pds = (PropertyDefinitionSimple) memberDefinition;
                PropertySimple ps = (PropertySimple) map.get(name);
                if ((ps==null || ps.getStringValue()==null ) && !pds.isRequired())
                    continue;
                if (ps!=null)
                    results.put(name,ps.getStringValue());
            }
            else {
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
        PropertySimpleType type = pds.getType();
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


}
