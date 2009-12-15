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
package org.rhq.plugins.apache.mapping;

import java.util.Collection;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.rhqtransform.AugeasRhqException;
import org.rhq.rhqtransform.ConfigurationToAugeas;
/**
 * 
 * @author Filip Drabek
 *
 */
public abstract class ConfigurationToAugeasApacheBase implements ConfigurationToAugeas {

    protected AugeasTree tree;

    public void setTree(AugeasTree tree) {
        this.tree = tree;
    }

    public void updateResourceConfiguration(AugeasNode node, ConfigurationDefinition resourceConfigDef,
        Configuration resourceConfig) throws AugeasRhqException {

        Collection<PropertyDefinition> propDefs = resourceConfigDef.getPropertyDefinitions().values();
 
        for (PropertyDefinition propDef : propDefs) {
        	Property prop = resourceConfig.get(propDef.getName());
            updateProperty(propDef, prop, node, 0);
        }
    }

    public abstract void updateMap(PropertyDefinitionMap propDefMap, Property prop, AugeasNode mapNode, int seq)
        throws AugeasRhqException ;
    
    public abstract void updateList(PropertyDefinitionList propDef, Property prop, AugeasNode listNode, int seq)
        throws AugeasRhqException; 

    public abstract void updateSimple(AugeasNode parentNode, PropertyDefinitionSimple propDef, Property prop, int seq)
        throws AugeasRhqException;

    public void updateProperty(PropertyDefinition propDef, Property parentProp, AugeasNode parentNode, int seq)
        throws AugeasRhqException {

        if (propDef instanceof PropertyDefinitionSimple) {
            PropertyDefinitionSimple propDefSimple = (PropertyDefinitionSimple) propDef;
            updateSimple(parentNode, propDefSimple, parentProp, seq);
        } else if (propDef instanceof PropertyDefinitionMap) {
            PropertyDefinitionMap propDefMap = (PropertyDefinitionMap) propDef;
            updateMap(propDefMap, parentProp, parentNode, seq);
        } else if (propDef instanceof PropertyDefinitionList) {
            PropertyDefinitionList propDefList = (PropertyDefinitionList) propDef;
            updateList(propDefList, parentProp, parentNode, seq);
        } else {
            throw new IllegalStateException("Unsupported PropertyDefinition subclass: " + propDef.getClass().getName());
        }

    }
}
