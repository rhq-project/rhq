/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.core.domain.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ObfuscatedPropertySimple;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;

/**
 * A utility to obfuscate all the password entries in a configuration object.
 *
 * @author Lukas Krejci
 */
public class PasswordObfuscationUtility {

    private PasswordObfuscationUtility() {
        
    }
    
    public static void obfuscatePasswords(ConfigurationDefinition definition, Configuration config) {
        if (config == null || config.getMap().isEmpty()) {
            return;
        }
        
        if (definition == null || definition.getPropertyDefinitions().isEmpty()) {
            return;
        }
        
        List<PropertySimple> replacementCandidates = new ArrayList<PropertySimple>();
        for(PropertyDefinition def : definition.getPropertyDefinitions().values()) {
            Property prop = config.get(def.getName());
            propertySwitch(def, prop, replacementCandidates);
        }
        
        for(PropertySimple prop : replacementCandidates) {
            replace(prop);
        }
    }   
    
    private static void propertySwitch(PropertyDefinition def, Property prop, List<PropertySimple> replacementCandidates) {
        if (prop != null) {
            if (def instanceof PropertyDefinitionMap) {
                traverse((PropertyDefinitionMap) def, (PropertyMap) prop, replacementCandidates);
            } else if (def instanceof PropertyDefinitionList) {
                traverse((PropertyDefinitionList) def, (PropertyList) prop, replacementCandidates);
            } else if (def instanceof PropertyDefinitionSimple) {
                addIfShouldBeReplaced((PropertyDefinitionSimple) def, (PropertySimple) prop, replacementCandidates);
            }
        }
    }
    
    private static void traverse(PropertyDefinitionMap definition, PropertyMap map, List<PropertySimple> replacementCandidates) {
        for(PropertyDefinition def: definition.getMap().values()) {
            Property prop = map.get(def.getName());
            propertySwitch(def, prop, replacementCandidates);
        }
    }
    
    private static void traverse(PropertyDefinitionList definition, PropertyList list, List<PropertySimple> replacementCandidates) {
        PropertyDefinition memberDef = definition.getMemberDefinition();
        List<Property> members = list.getList();
        for(Property prop : members) {
            propertySwitch(memberDef, prop, replacementCandidates);
        }
    }

    private static void addIfShouldBeReplaced(PropertyDefinitionSimple def, PropertySimple prop, List<PropertySimple> candidates) {
        if(def.shouldBeObfuscated() && !(prop instanceof ObfuscatedPropertySimple)) {
            candidates.add(prop);
        }
    }
    
    private static void replace(PropertySimple prop) {
        ObfuscatedPropertySimple replacement = new ObfuscatedPropertySimple(prop);
        replacement.setParentList(prop.getParentList());
        replacement.setParentMap(prop.getParentMap());
        replacement.setConfiguration(prop.getConfiguration());
        
        if (prop.getParentList() != null) {
            List<Property> list = prop.getParentList().getList();
            int idx = list.indexOf(prop);
            list.remove(prop);
            
            list.add(idx, replacement);
        } else if (prop.getParentMap() != null) {
            Map<String, Property> map = prop.getParentMap().getMap();
            replaceInMap(replacement, map);
        } else {            
            Configuration conf = prop.getConfiguration();
            replaceInMap(replacement, conf.getMap());
        }
    }
    
    private static void replaceInMap(ObfuscatedPropertySimple replacement, Map<String, Property> map) {
        //we need to maintain the order, so let's take a slightly more 
        //complicated approach
        Iterator<Map.Entry<String, Property>> it = map.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, Property> entry = it.next();
            if (entry.getKey().equals(replacement.getName())) {
                entry.setValue(replacement);
                break;
            }
        }        
    }
}
