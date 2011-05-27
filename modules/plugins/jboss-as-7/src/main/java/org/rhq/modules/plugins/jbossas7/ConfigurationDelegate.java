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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

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
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.modules.plugins.jbossas7.json.NameValuePair;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;

public class ConfigurationDelegate implements ConfigurationFacet {

    final Log log = LogFactory.getLog(this.getClass());
    ResourceContext context;
    private List<PROPERTY_VALUE> address;
    private ASConnection connection;

    public ConfigurationDelegate(ResourceContext context,ASConnection connection, List<PROPERTY_VALUE> address) {
        this.context = context;
        this.connection = connection;
        this.address = address;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();


        Operation op = new ReadResource(address); // TODO set recursive flag?  --> try to narrow it down
        op.addAdditionalProperty("recursive", "true");
        JsonNode json = connection.executeRaw(op);

        Configuration ret = new Configuration();
        ObjectMapper mapper = new ObjectMapper();

        Set<Map.Entry<String, PropertyDefinition>> entrySet = configDef.getPropertyDefinitions().entrySet();
        for (Map.Entry<String, PropertyDefinition> propDefEntry : entrySet) {
            PropertyDefinition propDef = propDefEntry.getValue();
            JsonNode sub = json.findValue(propDef.getName());
            if (sub == null) {
                log.error(
                        "No value for property [" + propDef.getName() + "] found - check the descriptor");
                continue;
            }
            if (propDef instanceof PropertyDefinitionSimple) {
                PropertySimple propertySimple;

                if (sub != null) {
                    // Property is non-null -> return it.
                    propertySimple = new PropertySimple(propDef.getName(), sub.getValueAsText());
                    ret.put(propertySimple);
                } else {
                    // property is null? Check if it is required
                    if (propDef.isRequired()) {
                        String defaultValue = ((PropertyDefinitionSimple) propDef).getDefaultValue();
                        propertySimple = new PropertySimple(propDef.getName(), defaultValue);
                        ret.put(propertySimple);
                    }
                }
            } else if (propDef instanceof PropertyDefinitionList) {
                PropertyList propertyList = new PropertyList(propDef.getName());
                PropertyDefinition memberDefinition = ((PropertyDefinitionList) propDef).getMemberDefinition();
                if (memberDefinition == null) {
                    if (sub.isObject()) {
                        Iterator<String> fields = sub.getFieldNames();
                        while (fields.hasNext()) {
                            String fieldName = fields.next();
                            JsonNode subNode = sub.get(fieldName);
                            PropertySimple propertySimple = new PropertySimple(propDef.getName(), fieldName);
                            propertyList.add(propertySimple);
                        }
                    } else {
                        System.out.println("===Sub not object==="); // TODO evaluate this branch again
                        Iterator<JsonNode> values = sub.getElements();
                        while (values.hasNext()) {
                            JsonNode node = values.next();
                            String value = node.getTextValue();
                            PropertySimple propertySimple = new PropertySimple(propDef.getName(), value);
                            propertyList.add(propertySimple);
                        }
                    }
                } else if (memberDefinition instanceof PropertyDefinitionMap) {
                    PropertySimple propertySimple;

                    if (sub.isArray()) {
                        Iterator<JsonNode> entries = sub.getElements();
                        while (entries.hasNext()) {
                            JsonNode entry = entries.next(); // -> one row in the list i.e. one map

                            // Distinguish here?

                            PropertyMap map = new PropertyMap(
                                    memberDefinition.getName()); // TODO : name from def or 'entryKey' ?
                            Iterator<JsonNode> fields = entry.getElements(); // TODO loop over fields from map and not from json
                            while (fields.hasNext()) {
                                JsonNode field = fields.next();
                                if (field.isObject()) {
                                    // TODO only works for tuples at the moment - migrate to some different kind of parsing!
                                    PROPERTY_VALUE prop = mapper.readValue(field, PROPERTY_VALUE.class);
                                    // now need to find the names of the properties
                                    List<PropertyDefinition> defList = ((PropertyDefinitionMap) memberDefinition).getSummaryPropertyDefinitions();
                                    if (defList.isEmpty())
                                        throw new IllegalArgumentException(
                                                "Map " + memberDefinition.getName() + " has no members");
                                    String key = defList.get(0).getName();
                                    String value = prop.getKey();
                                    propertySimple = new PropertySimple(key, value);
                                    map.put(propertySimple);
                                    if (defList.size() > 1) {
                                        key = defList.get(1).getName();
                                        value = prop.getValue();
                                        propertySimple = new PropertySimple(key, value);
                                        map.put(propertySimple);

                                    }
                                } else { // TODO reached?
                                    String key = field.getValueAsText();
                                    if (key.equals(
                                            "PROPERTY_VALUE")) { // TODO this may change in the future in the AS implementation
                                        JsonNode pv = entry.findValue(key);
                                        String k = pv.toString();
                                        String v = pv.getValueAsText();
                                        propertySimple = new PropertySimple(k, v);
                                        map.put(propertySimple);

                                    } else {
                                        JsonNode value = entry.findValue(key);
                                        if (value != null) {
                                            propertySimple = new PropertySimple(key, value.getValueAsText());
                                            map.put(propertySimple);
                                        }

                                    }
                                }
                            }
                            propertyList.add(map);
                        }
                    } else if (sub.isObject()) {
                        Iterator<String> keys = sub.getFieldNames();
                        while (keys.hasNext()) {
                            String entryKey = keys.next();

                            JsonNode node = sub.findPath(entryKey);
                            PropertyMap map = new PropertyMap(
                                    memberDefinition.getName()); // TODO : name from def or 'entryKey' ?
                            if (node.isObject()) {
                                Iterator<String> fields = node.getFieldNames(); // TODO loop over fields from map and not from json
                                while (fields.hasNext()) {
                                    String key = fields.next();

                                    propertySimple = new PropertySimple(key, node.findValue(key).getValueAsText());
                                    map.put(propertySimple);
                                }
                                propertyList.add(map);
                            } else if (sub.isNull()) {
                                List<PropertyDefinition> defList = ((PropertyDefinitionMap) memberDefinition).getSummaryPropertyDefinitions();
                                String key = defList.get(0).getName();
                                propertySimple = new PropertySimple(key, entryKey);
                                map.put(propertySimple);
                            }
                        }

                    }
                } else if (memberDefinition instanceof PropertyDefinitionSimple) {
                    String name = memberDefinition.getName();
                    Iterator<JsonNode> keys = sub.getElements();
                    while (keys.hasNext()) {
                        JsonNode entry = keys.next();

                        PropertySimple propertySimple = new PropertySimple(name, entry.getTextValue());
                        propertyList.add(propertySimple);
                    }
                }
                ret.put(propertyList);
            } // end List of ..
            else if (propDef instanceof PropertyDefinitionMap) {
                PropertyDefinitionMap mapDef = (PropertyDefinitionMap) propDef;
                PropertyMap pm = new PropertyMap(mapDef.getName());

                Map<String, PropertyDefinition> memberDefMap = mapDef.getPropertyDefinitions();
                for (Map.Entry<String, PropertyDefinition> maEntry : memberDefMap.entrySet()) {
                    JsonNode valueNode = json.findValue(maEntry.getKey());
                    Property p;
                    if (maEntry.getValue() instanceof PropertyDefinitionSimple) {
                        p = putProperty(valueNode, maEntry.getValue());
                        pm.put(p);
                    } else if (maEntry.getValue() instanceof PropertyDefinitionMap) { // TODO make this recursive?

                        PropertyDefinitionMap pdm = (PropertyDefinitionMap) maEntry.getValue();
                        Map<String, PropertyDefinition> mmDefMap = pdm.getPropertyDefinitions();
                        for (Map.Entry<String, PropertyDefinition> mmDefEntry : mmDefMap.entrySet()) {
                            if (valueNode != null) {
                                JsonNode node2 = valueNode.findValue(mmDefEntry.getKey());
                                System.err.println("Map not yet implemented " + node2.toString());
                            } else
                                System.err.println("Value node was null ");
                        }
                    } else { // PropDefList
                        System.err.println("List not yet implemented");
                    }

//                    pm.put(p);
                }
                ret.put(pm);
            }
        }

        return ret;
    }

    PropertySimple putProperty(JsonNode value, PropertyDefinition def) {
        String name = def.getName();
        PropertySimple ps;

        if (value == null) {
            if (def instanceof PropertyDefinitionSimple) {
                PropertyDefinitionSimple pds = (PropertyDefinitionSimple) def;
                return new PropertySimple(name, pds.getDefaultValue());
            } else
                return new PropertySimple(name, null);
        }
        PropertySimpleType type = ((PropertyDefinitionSimple) def).getType();

        switch (type) {
        case BOOLEAN:
            ps = new PropertySimple(name, value.getBooleanValue());
            break;
        case FLOAT:
        case DOUBLE:
            ps = new PropertySimple(name, value.getDoubleValue());
            break;
        case INTEGER:
            ps = new PropertySimple(name, value.getIntValue());
            break;
        case LONG:
            ps = new PropertySimple(name, value.getLongValue());
            break;
        default:
            ps = new PropertySimple(name, value.getTextValue());
        }

        return ps;
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