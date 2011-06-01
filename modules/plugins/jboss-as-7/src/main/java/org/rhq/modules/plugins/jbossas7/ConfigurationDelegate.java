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

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.omg.CORBA.portable.ValueInputStream;

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
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.NameValuePair;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenResources;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

public class ConfigurationDelegate implements ConfigurationFacet {

    private static final String SUB_PATH = "_subPath";

    final Log log = LogFactory.getLog(this.getClass());

    private List<PROPERTY_VALUE> address;
    private ASConnection connection;
    private ConfigurationDefinition configurationDefinition;

    /**
     * Create a new configuration delegate, that reads the attributes for the resource at address.
     * @param configDef Configuration definition for the configuration
     * @param connection asConnection to use
     * @param address address of the resource.
     */
    public ConfigurationDelegate(ConfigurationDefinition configDef,ASConnection connection, List<PROPERTY_VALUE> address) {
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

        Configuration config = new Configuration();

        /*
         * Grouped definitions get a special treatment, as they may have a special property
         * that will be evaluated to look at a child resource or a special attribute or such
         */
        List<PropertyGroupDefinition> gdef = configurationDefinition.getGroupDefinitions();
        for (PropertyGroupDefinition pgDef : gdef) {
            handleGroup(config,pgDef);
        }
        /*
         * Now handle the non-grouped properties
         */
        List<PropertyDefinition> nonGroupdedDefs = configurationDefinition.getNonGroupedProperties();
        Operation op = new ReadResource(address);
        op.addAdditionalProperty("recursive", "true");
        handleProperties(config,nonGroupdedDefs,op);

        return config;
    }

    /**
     * Handle a set of grouped properties. The name of the group tells us how to deal with it:
     * <ul>
     *     <li>attribute: read the passed attribute of the resource</li>
     *     <li>children:  read the children of the given child-type</li>
     * </ul>
     * @param config Configuration to return
     * @param groupDefinition Definition of this group
     * @throws Exception If anything goes wrong
     */
    private void handleGroup(Configuration config, PropertyGroupDefinition groupDefinition) throws Exception{
        Operation operation = null;
        String groupName = groupDefinition.getName();
        if (groupName.startsWith("attribute:")) {
            String attr = groupName.substring("attribute:".length());
            operation = new ReadAttribute(address,attr);
        }
        else if (groupName.startsWith("children:")) {
            String type = groupName.substring("children:".length());
            operation = new ReadChildrenResources(address,type);
            operation.addAdditionalProperty("recursive", "true");
        }
        else {
            throw new IllegalArgumentException("Unknown operation in group name [" + groupName + "]");
        }
        List<PropertyDefinition> listedDefs = configurationDefinition.getPropertiesInGroup(groupName);
        handleProperties(config,listedDefs,operation);

    }


    private void handleProperties(Configuration config, List<PropertyDefinition> definitions, Operation op) throws Exception {
        if (definitions.size()==0)
            return;

        Result operationResult = connection.execute(op);
        if (!operationResult.isSuccess()) {
            throw new IOException("Operation " + op + " failed: " + operationResult.getFailureDescription());
        }


        if (operationResult.getResult() instanceof List) {
            PropertyList propertyList = handlePropertyList((PropertyDefinitionList) definitions.get(0),operationResult.getResult());

                if (propertyList!=null)
                    config.put(propertyList);
            return;
        }

        Map<String,Object> results = (Map<String, Object>) operationResult.getResult();


        for (PropertyDefinition propDef :definitions ) {
            String propertyName = propDef.getName();
/*
            if (!results.containsKey(propertyName)) {
                log.warn(
                        "No value for property [" + propertyName + "] found - check the descriptor (may be valid, \n"+
                                "as some attributes are different in domain vs standalone mode");
                continue;
            }
*/
            Object valueObject = results.get(propertyName);

            if (propDef instanceof PropertyDefinitionSimple) {

                PropertySimple value = handlePropertySimple((PropertyDefinitionSimple) propDef, valueObject);
                if (value!=null)
                    config.put(value);
            }

            else if (propDef instanceof PropertyDefinitionList) {
                PropertyList propertyList = handlePropertyList((PropertyDefinitionList) propDef,valueObject);

                if (propertyList!=null)
                    config.put(propertyList);
            }
            else if (propDef instanceof PropertyDefinitionMap) {
                PropertyMap propertyMap = handlePropertyMap((PropertyDefinitionMap) propDef,valueObject);

                if (propertyMap!=null)
                    config.put(propertyMap);
            }
        }
    }

    PropertySimple handlePropertySimple(PropertyDefinitionSimple propDef, Object valueObject) {
        PropertySimple propertySimple;

        String name = propDef.getName();
        if (valueObject != null) {
            // Property is non-null -> return it.
            propertySimple = new PropertySimple(name, valueObject);
        } else {
            // property is null? Check if it is required
            if (propDef.isRequired()) {
                String defaultValue = ((PropertyDefinitionSimple) propDef).getDefaultValue();
                propertySimple = new PropertySimple(name, defaultValue);
            }
            else { // Not required and null -> return null
                propertySimple = new PropertySimple(name,null);
            }
        }
        return propertySimple;

    }

    /**
     * Handle a Map of ...
     * @param propDef Definition of the map
     * @param valueObject the objects to put into the map
     * @return the populated map
     */
    PropertyMap handlePropertyMap(PropertyDefinitionMap propDef, Object valueObject) {
        PropertyMap propertyMap = new PropertyMap(propDef.getName());

        Map<String, PropertyDefinition> memberDefMap = propDef.getPropertyDefinitions();
        Map<String,Object> objects = (Map<String, Object>) valueObject;
        for (Map.Entry<String, PropertyDefinition> maEntry : memberDefMap.entrySet()) {
            String key = maEntry.getKey();
            // special case: if the key is "*", we just pick the first element
            Object o ;
            if (key.equals("*"))
                o = objects.entrySet().iterator().next().getValue();
            else
                o = objects.get(key);
            Property property;
            PropertyDefinition value = maEntry.getValue();
            if (value instanceof PropertyDefinitionSimple)
                property = handlePropertySimple((PropertyDefinitionSimple) value,o);
            else if (value instanceof PropertyDefinitionList)
                property = handlePropertyList((PropertyDefinitionList) value,o);
            else if (value instanceof PropertyDefinitionMap)
                property = handlePropertyMap((PropertyDefinitionMap) value,o);
            else
                throw new IllegalArgumentException("Unknown property type in map property [" + propDef.getName() +"]");

            if (property!=null)
                propertyMap.put(property);
            else
                System.out.println("Property " + key + " was null");

        }

        return propertyMap;
    }

    /**
     * Handle a List of ...
     * @param propDef Definition of this list
     * @param valueObject The objects to put into the list
     * @return the property that describes the list.
     */
    PropertyList handlePropertyList(PropertyDefinitionList propDef,Object valueObject) {
        String propertyName = propDef.getName();
        PropertyList propertyList = new PropertyList(propertyName);
        PropertyDefinition memberDefinition = propDef.getMemberDefinition();
        if (memberDefinition==null)
            throw new IllegalArgumentException("Member definition for property [" + propertyName + "] was null");

        if (valueObject==null) {
//            System.out.println("vo null");
            return null;
        }

        Collection<Object> objects;
        if (valueObject instanceof List)
            objects = (List<Object>) valueObject;
        else /*if (valueObject instanceof Map)*/ {
            objects = ((Map)valueObject).values();
        }

        if (memberDefinition instanceof PropertyDefinitionSimple) {
            for (Object obj : objects) {
                PropertySimple property = handlePropertySimple((PropertyDefinitionSimple) memberDefinition,
                        obj);
                if (property!=null)
                    propertyList.add(property);
            }
        }
        else if (memberDefinition instanceof PropertyDefinitionMap) {
            for (Object obj : objects) {
                Map<String,Object>  map = (Map<String, Object>) obj;

                PropertyMap propertyMap = handlePropertyMap(
                        (PropertyDefinitionMap) propDef.getMemberDefinition(),map);
                if (propertyMap!=null)
                    propertyList.add(propertyMap);
            }
        }
        // TODO List of lists ?
        return propertyList;
    }



    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        Configuration conf = report.getConfiguration();
        for (Map.Entry<String, PropertySimple> entry : conf.getSimpleProperties().entrySet()) {

            NameValuePair nvp = new NameValuePair(entry.getKey(), entry.getValue().getStringValue());
            Operation writeAttribute = new Operation("write-attribute",
                    address, nvp); // TODO test path
            JsonNode result = connection.executeRaw(writeAttribute);
            if (ASConnection.isErrorReply(result)) {
                report.setStatus(ConfigurationUpdateStatus.FAILURE);
                report.setErrorMessage(ASConnection.getFailureDescription(result));
            }
        }

    }
}