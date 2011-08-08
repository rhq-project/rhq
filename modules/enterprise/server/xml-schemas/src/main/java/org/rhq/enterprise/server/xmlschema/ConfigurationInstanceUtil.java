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

package org.rhq.enterprise.server.xmlschema;

import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.ConfigurationInstanceDescriptor;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.SimplePropertyInstanceDescriptor;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ConfigurationInstanceUtil {

    public static final String NS_CONFIGURATION_INSTANCE = "urn:xmlns:rhq-configuration-instance";
    
    private ConfigurationInstanceUtil() {

    }

    /**
     * A configuration instance is a combination of a configuration definition and a concrete
     * configuration instance with defined values. This is used during the config synchronization
     * to output the default configuration of an importer directly in the export file so that the
     * users have an easy way of modifying that configuration.
     * 
     * @param definition
     * @param configuration
     * @return
     */
    public static ConfigurationInstanceDescriptor createConfigurationInstance(ConfigurationDefinition definition,
        Configuration configuration) {

        ConfigurationInstanceDescriptor ret = new ConfigurationInstanceDescriptor();
        
        addAll(ret.getConfigurationProperty(), definition.getPropertyDefinitions(), configuration.getMap());
                
        return ret;
    }
    
    private static void addAll(List<JAXBElement<?>> descriptors, Map<String, PropertyDefinition> defs, Map<String, Property> props) {
        for(Map.Entry<String, PropertyDefinition> e : defs.entrySet()) {
            String propName = e.getKey();
            PropertyDefinition def = e.getValue();
            Property prop = props.get(propName);
            
            addSingle(descriptors, def, prop);
        }
    }
    
    private static void addSingle(List<JAXBElement<?>> descriptors, PropertyDefinition def, Property prop) {
        Object descriptor = null;
        String tagName = null;
        
        if (def instanceof PropertyDefinitionSimple) {
            descriptor = createSimple(def, prop);
            tagName = "simple-property";
        } else if (def instanceof PropertyDefinitionList) {
            //TODO
        } else if (def instanceof PropertyDefinitionMap) {
            //TODO
        }
        
        addToJAXBElementList(descriptors, Object.class, descriptor, new QName(NS_CONFIGURATION_INSTANCE, tagName));
    }
    
    private static SimplePropertyInstanceDescriptor createSimple(PropertyDefinition def, Property prop) {
        SimplePropertyInstanceDescriptor ret = new SimplePropertyInstanceDescriptor();
        PropertyDefinitionSimple pds = (PropertyDefinitionSimple) def;
        ret.setName(pds.getName());
        ret.setDescription(pds.getDescription());
        ret.setLongDescription(pds.getDescription());
        ret.setDisplayName(pds.getDisplayName());
        
        //TODO add rest of the properties
        
        ret.setValue(((PropertySimple)prop).getStringValue());
        
        return ret;
    }
    
    
    private static <T> void addToJAXBElementList(List<JAXBElement<? extends T>> list, Class<T> baseClass, T property, QName tagName) {
        JAXBElement<? extends T> el = new JAXBElement<T>(tagName, baseClass, property.getClass(), property);
        list.add(el);
    }
}
