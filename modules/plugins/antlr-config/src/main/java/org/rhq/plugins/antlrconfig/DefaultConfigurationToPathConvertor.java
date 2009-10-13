/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.antlrconfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;

/**
 * This default implementation can create a tree path out of the names of the properties/property definitions.
 * 
 * the name of a property that is supposed to be mapped to a configuration file has to start with <code>config://</code>
 * "scheme" definition followed by one of the following:
 * <ul>
 * <li><code>/name</code> - an absolute path within the tree to the given property
 * <li><code>name</code> - a path relative to the parent property
 * <li><code>$[number]</code> - ([number] stands for an actual number without the brackets) defines a position of the respective child in the parent in the config file AST.
 * </ul>
 * The <code>name</code> can be defined with the following regex:
 * <code>(\w+|$\d+)(\/(\w+|$\d+))*</code>
 * 
 * @author Lukas Krejci
 */
public class DefaultConfigurationToPathConvertor implements ConfigurationToPathConvertor {

    public static final String NAME_PREFIX = "config://";
    public static final int NAME_PREFIX_LENGTH = NAME_PREFIX.length();
    
    private ConfigurationDefinition configurationDefinition;
    
    public DefaultConfigurationToPathConvertor(ConfigurationDefinition configurationDefinition) {
        this.configurationDefinition = configurationDefinition;
    }
    
    public String getPathRelativeToParent(PropertyDefinition propertyDefinition) {
        if (propertyDefinition.getName().startsWith(NAME_PREFIX)) {
            if (propertyDefinition.getConfigurationDefinition() != null) {
                return propertyDefinition.getName().substring(NAME_PREFIX_LENGTH);
            } else {
                PropertyDefinition parentDef = getConfiguredParent(propertyDefinition);
                if (parentDef != null) {
                    return parentDef.getName().substring(NAME_PREFIX_LENGTH) + "/" + propertyDefinition.getName().substring(NAME_PREFIX_LENGTH);
                }
            }
        }
        
        return null;
    }
    
    
    /* (non-Javadoc)
     * @see org.rhq.core.antlr.ConfigurationToPathConvertor#getConcretePath(org.rhq.core.domain.configuration.Property)
     */
    public String getPathRelativeToParent(Property property) {
        if (property.getName().startsWith(NAME_PREFIX)) {
            if (property.getConfiguration() != null) {
                return property.getName().substring(NAME_PREFIX_LENGTH);
            } else {
                Property parent = getConfiguredParent(property);
                if (parent != null) {
                    String name = null;
                    if (parent instanceof PropertyList) {
                        int pos = 0;
                        for (Property p : ((PropertyList) parent).getList()) {
                           if (p.equals(property)) {
                               break;
                           }
                           pos++;
                        }
                        //position is 1 based
                        name = property.getName().substring(NAME_PREFIX_LENGTH) + "[" + (pos + 1) + "]";
                    } else {
                        name = property.getName().substring(NAME_PREFIX_LENGTH);
                    }
                    return parent.getName().substring(NAME_PREFIX_LENGTH) + "/" + name;
                } 
            }
        }
        return null;
    }

    
    public PropertyDefinition getPropertyDefinition(List<PathElement> treePath) {
        if (treePath == null || treePath.isEmpty()) return null;
        
        HashMap<PropertyDefinition, Integer> partialMatches = new HashMap<PropertyDefinition, Integer>();
        
        PathElement rootElement = new PathElement(treePath.get(0));
        rootElement.setTokenTypeName("/" + rootElement.getTokenTypeName());
        
        for(PropertyDefinition pd : configurationDefinition.getPropertyDefinitions().values()) {
            int followUp = getPartialMatchNextFollowupIndex(rootElement, 0, pd.getName());
            if (followUp >= 0) {
                partialMatches.put(pd, followUp);
            }
        }
        
        if (partialMatches.isEmpty()) return null;
        
        ArrayList<PathElement> treePathCopy = new ArrayList<PathElement>(treePath);
        //we've already processed that one.
        treePathCopy.remove(0);
        
        for(PathElement el : treePathCopy) {
            HashMap<PropertyDefinition, Integer> newPartialMatches = new HashMap<PropertyDefinition, Integer>();
            
            for(Map.Entry<PropertyDefinition, Integer> entry : partialMatches.entrySet()) {
                PropertyDefinition partialMatch = entry.getKey();
                String name = partialMatch.getName();
                int followupIdx = entry.getValue();
                
                if (followupIdx < name.length() - NAME_PREFIX_LENGTH) {
                    //this definition's still "follows" the path 
                    followupIdx = getPartialMatchNextFollowupIndex(el, followupIdx, name);
                    if (followupIdx >= 0) {
                        newPartialMatches.put(partialMatch, followupIdx);
                    }
                } else {
                    //this means that this property definition matched the path so far
                    //but contains no more path elements. It still might have children with
                    //relative paths that might match the rest of the path though.
                    if (partialMatch instanceof PropertyDefinitionList) {
                        PropertyDefinition child = ((PropertyDefinitionList)partialMatch).getMemberDefinition();
                        followupIdx = getPartialMatchNextFollowupIndex(el, 0, child.getName());
                        if (followupIdx >= 0) {
                            newPartialMatches.put(child, followupIdx);
                        }
                    } else if (partialMatch instanceof PropertyDefinitionMap) {
                        PropertyDefinitionMap map = (PropertyDefinitionMap)partialMatch;
                        
                        for(PropertyDefinition child : map.getPropertyDefinitions().values()) {
                            AbsoluteIndexAndFollowupIndex indexes = getAbsoluteIndexFromName(child.getName());
                            if (indexes.absoluteIndex > 0 && indexes.absoluteIndex == el.getAbsoluteTokenPosition()) {
                                newPartialMatches.put(child, indexes.followupIndex);
                            } else {
                                //k, this is not an absolute index, let's try matching by name
                                followupIdx = getPartialMatchNextFollowupIndex(el, 0, child.getName());
                                if (followupIdx >= 0) {
                                    newPartialMatches.put(child, followupIdx);
                                }                                
                            }
                        }
                    }
                }
            }
            
            partialMatches = newPartialMatches;
        }
        
        for (Map.Entry<PropertyDefinition, Integer> entry : partialMatches.entrySet()) {
            PropertyDefinition pd = entry.getKey();
            int followupIdx = entry.getValue();
            if (pd.getName().length() == followupIdx + NAME_PREFIX_LENGTH) {
                return pd;
            }
        }
        return null;
    }

    private int getPartialMatchNextFollowupIndex(PathElement element, int followupIndex, String name) {
        if (!name.startsWith(NAME_PREFIX)) return -1;
        if (NAME_PREFIX_LENGTH + followupIndex > name.length()) return -1;
        
        name = name.substring(NAME_PREFIX_LENGTH + followupIndex);
        
        if (name.startsWith(element.getTokenTypeName())) {
            int ret = element.getTokenTypeName().length();
            if (name.length() > ret && name.charAt(ret) == '/') ret += 1;
            
            return ret;
        }
        
        return -1;
    }
    
    private AbsoluteIndexAndFollowupIndex getAbsoluteIndexFromName(String name) {
        if (name.length() < NAME_PREFIX_LENGTH + 2) return new AbsoluteIndexAndFollowupIndex(); // config://$n
        if (!name.startsWith(NAME_PREFIX)) return new AbsoluteIndexAndFollowupIndex();
        if (name.charAt(NAME_PREFIX_LENGTH) != '$') return new AbsoluteIndexAndFollowupIndex();
        
        String idxString = name.substring(NAME_PREFIX_LENGTH + 1);
        
        try {
            int toIdx = idxString.indexOf('/');
            int followupIdx = toIdx + 1;
            
            if (toIdx == -1) {
                toIdx = idxString.length();
                followupIdx = name.length() - NAME_PREFIX_LENGTH;
            }
            return new AbsoluteIndexAndFollowupIndex(Integer.parseInt(idxString), followupIdx);
        } catch (NumberFormatException e) {
            return new AbsoluteIndexAndFollowupIndex();
        }
    }
 
    private PropertyDefinition getConfiguredParent(PropertyDefinition def) {
        PropertyDefinition parent = def.getParentPropertyListDefinition();
        if (parent == null) parent = def.getParentPropertyMapDefinition();
        
        while (parent != null) {
            if (parent.getName().startsWith(NAME_PREFIX)) {
                return parent;
            }
            PropertyDefinition p = parent.getParentPropertyListDefinition();
            if (p == null) p = parent.getParentPropertyMapDefinition();
            
            parent = p;
        }
        
        return null;
    }
    
    private Property getConfiguredParent(Property prop) {
        Property parent = prop.getParentList();
        if (parent == null) parent = prop.getParentMap();
        
        while (parent != null) {
            if (parent.getName().startsWith(NAME_PREFIX)) {
                return parent;
            }
            Property p = parent.getParentList();
            if (p == null) p = parent.getParentMap();
            parent = p;
        }
        
        return null;
    }
    private static class AbsoluteIndexAndFollowupIndex {
        public int absoluteIndex;
        public int followupIndex;
        
        public AbsoluteIndexAndFollowupIndex() {
            absoluteIndex = -1;
            followupIndex = -1;
        }
        
        public AbsoluteIndexAndFollowupIndex(int absoluteIndex, int followupIndex) {
            this.absoluteIndex = absoluteIndex;
            this.followupIndex = followupIndex;
        }
    }
}
