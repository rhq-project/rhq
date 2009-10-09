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

import java.util.List;

import org.antlr.runtime.RecognitionException;

import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;

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
    
    private ConfigurationDefinition configurationDefinition;
    
    public DefaultConfigurationToPathConvertor(ConfigurationDefinition configurationDefinition) {
        this.configurationDefinition = configurationDefinition;
    }
    
    /* (non-Javadoc)
     * @see org.rhq.core.antlr.ConfigurationToPathConvertor#getConcretePath(org.rhq.core.domain.configuration.Property)
     */
    public String getTreePath(Property property) {
        String name = property.getName();
        String ret = null;
        if (name.startsWith(NAME_PREFIX)) {
            name = name.substring(NAME_PREFIX.length());
            
            if (name.startsWith("/")) {
                //absolute path
                ret = name;
            } else {
                //relative path.. we have to climb up the parents to get the full path
                Property parent = property.getParentList();
                if (parent != null && !name.startsWith("$")) {
                    //we're inside a list constructing a concrete path and the current name
                    //is a name reference.
                    //need to figure out the position of this property inside the parent list
                    int pos = 0;
                    for(Property p : ((PropertyList)parent).getList()) {
                        if (p.equals(property)) {
                            break;
                        }
                        pos++;
                    }
                    name += "[" + (pos + 1) + "]"; //position is 1 based.
                }
                if (parent == null) parent = property.getParentMap();
                if (parent == null) {
                    //well... this is a property with a relative path but is a top level property
                    //we have no option but to return the relative path here.
                    ret = name;
                } else {
                    String parentPath = getTreePath(parent);
                    ret = parentPath + "/" + name;
                }
            }
        }
        return ret;
    }

    
    public PropertyDefinition getPropertyDefinition(List<PathElement> treePath) {
        PropertyDefinition ret = null;
        
        //TODO implement this
        
        return ret;
    }

    public String getGenericPath(PropertyDefinition propertyDefinition) {
        String name = propertyDefinition.getName();
        String ret = null;
        
        if (name.startsWith(NAME_PREFIX)) {
            name = name.substring(NAME_PREFIX.length());
            
            if (name.startsWith("/")) {
                ret = name;
            } else {
                PropertyDefinition parent = propertyDefinition.getParentPropertyListDefinition();
                if (parent == null) parent = propertyDefinition.getParentPropertyMapDefinition();
                if (parent == null) {
                    ret = name;
                } else {
                    String parentPath = getGenericPath(parent);
                    ret = parentPath + "/" + name;
                }
            }
        }
        
        return ret;
    }

}
