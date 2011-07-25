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
import java.util.ArrayList;
import java.util.Collection;
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
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenResources;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.modules.plugins.jbossas7.json.WriteAttribute;

public class ConfigurationWriteDelegate implements ConfigurationFacet {

    final Log log = LogFactory.getLog(this.getClass());

    private Address address;
    private ASConnection connection;
    private ConfigurationDefinition configurationDefinition;
    private String namePropLocator;
    private String type;

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
     * @param report
     */
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        Configuration conf = report.getConfiguration();

        CompositeOperation cop = updateGenerateOperationFromProperties(conf);

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

    protected CompositeOperation updateGenerateOperationFromProperties(Configuration conf) {

        CompositeOperation cop = new CompositeOperation();

        for (PropertyDefinition propDef : configurationDefinition.getNonGroupedProperties()) {

            updateProperty(conf, cop, propDef);
        }
        for (PropertyGroupDefinition pgd: configurationDefinition.getGroupDefinitions()) {
            String groupName = pgd.getName();
            namePropLocator = null;
            if (groupName.startsWith("children:")) {
                type = groupName.substring("children:".length());
                if (type.contains(":")) {
                    namePropLocator = type.substring(type.indexOf(":") + 1);
                    type = type.substring(0, type.indexOf(":"));
                }
                else {
                    log.error("Group name " + groupName + " contains no property name locator ");
                    return cop;
                }
                List<PropertyDefinition> definitions = configurationDefinition.getPropertiesInGroup(groupName);
                for (PropertyDefinition def : definitions) {
                    updateProperty(conf,cop,def);
                }

            } // TODO handle attribute: case
        }

        return cop;
    }

    private void updateProperty(Configuration conf, CompositeOperation cop, PropertyDefinition propDef) {

        // Skip over read-only properties, the AS can not use them anyway
        if (propDef.isReadOnly())
            return;

        boolean isSpecial = false;

        Property prop;
        // Handle the special case
        if (propDef instanceof PropertyDefinitionList && propDef.getName().equals("*")) {
            propDef = ((PropertyDefinitionList) propDef).getMemberDefinition();
            PropertyList pl = (PropertyList) conf.get("*");
            prop = pl.getList().get(0);
            isSpecial = true;
        }
        else {
            prop = conf.get(propDef.getName());
        }

        if (prop instanceof PropertySimple) {
            updateHandlePropertySimple(cop, (PropertySimple)prop, (PropertyDefinitionSimple) propDef);
        }
        else if (prop instanceof PropertyList) {
            updateHandlePropertyList(cop, (PropertyList) prop, (PropertyDefinitionList) propDef);
        }
        else {
            if(isSpecial)
                updateHandlePropertyMapSpecial(cop, (PropertyMap) prop, (PropertyDefinitionMap) propDef);
            else
                updateHandlePropertyMap(cop,(PropertyMap)prop,(PropertyDefinitionMap)propDef);
        }
    }

    private void updateHandlePropertyMap(CompositeOperation cop, PropertyMap prop, PropertyDefinitionMap propDef) {
        Map<String,Object> results = updateHandleMap(prop,propDef);
        Operation writeAttribute = new WriteAttribute(address,prop.getName(),results);
        cop.addStep(writeAttribute);
    }

    private void updateHandlePropertyMapSpecial(CompositeOperation cop, PropertyMap prop, PropertyDefinitionMap propDef) {
        Map<String,Object> results = updateHandleMap(prop,propDef);
        if (prop.get(namePropLocator)==null) {
            throw new IllegalArgumentException("There is no element in the map with the name " + namePropLocator);
        }
        String addrVal= ((PropertySimple)prop.get(namePropLocator)).getStringValue();
        Address addr = new Address(address);
        addr.add(type,addrVal);
        for (Map.Entry<String,Object> entry : results.entrySet()) {
            Operation writeAttribute = new WriteAttribute(addr,entry.getKey(),entry.getValue());
            cop.addStep(writeAttribute);
        }
    }

    private void updateHandlePropertyList(CompositeOperation cop, PropertyList prop, PropertyDefinitionList propDef) {
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
                Map<String,Object> mapResult = updateHandleMap((PropertyMap) inner,(PropertyDefinitionMap)memberDef);
                values.add(mapResult);
            }
        }
        Operation writeAttribute = new WriteAttribute(address,prop.getName(),values);
        cop.addStep(writeAttribute);
    }


    private void updateHandlePropertySimple(CompositeOperation cop, PropertySimple propertySimple, PropertyDefinitionSimple propDef) {

        // If the property value is null and the property is optional, skip too
        if (propertySimple.getStringValue()==null && !propDef.isRequired())
            return;

        Operation writeAttribute = new WriteAttribute(
                address, propertySimple.getName(),propertySimple.getStringValue());
        cop.addStep(writeAttribute);
    }

    private Map<String, Object> updateHandleMap(PropertyMap map, PropertyDefinitionMap mapDef) {
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

}