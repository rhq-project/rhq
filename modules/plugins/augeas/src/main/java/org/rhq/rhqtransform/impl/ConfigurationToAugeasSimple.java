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

package org.rhq.rhqtransform.impl;

import java.util.Collection;
import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
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
public class ConfigurationToAugeasSimple implements ConfigurationToAugeas {

    protected AugeasTree tree;

    public ConfigurationToAugeasSimple() {

    }

    public void setTree(AugeasTree tree) {
        this.tree = tree;
    }

    public void updateResourceConfiguration(AugeasNode node, ConfigurationDefinition resourceConfigDef,
        Configuration resourceConfig) throws AugeasRhqException {

        Collection<PropertyDefinition> propDefs = resourceConfigDef.getPropertyDefinitions().values();
        PropertyMap startProp = new PropertyMap();

        for (Property property : resourceConfig.getProperties())
            startProp.put(property);

        for (PropertyDefinition propDef : propDefs) {
            updateProperty(propDef, startProp, node, 0);
        }
    }

    public void updateMap(PropertyDefinitionMap propDefMap, Property prop, AugeasNode mapNode, int seq)
        throws AugeasRhqException {

        PropertyMap map = null;

        if (prop instanceof PropertyList) {
            PropertyList lst = (PropertyList) prop;
            List<Property> props = lst.getList();
            map = (PropertyMap) props.get(seq - 1);
        }

        if (prop instanceof PropertyMap) {
            PropertyMap mp = (PropertyMap) prop;
            map = (PropertyMap) mp.get(propDefMap.getName());
        }

        List<AugeasNode> nodes = tree.matchRelative(mapNode, propDefMap.getName());

        AugeasNode node;
        int i = 0;
        if (nodes.isEmpty() | nodes.size() < seq) {
            node = tree.createNode(mapNode, propDefMap.getName(), null, seq);
            nodes.add(node);
            i = ((seq == 0) ? 0 : seq - 1);
        } else if (seq == 0) {
            node = nodes.get(0);
            i = 0;
        } else {
            node = nodes.get(seq - 1);
            i = seq - 1;
        }

        for (PropertyDefinition mapEntryPropDef : propDefMap.getPropertyDefinitions().values()) {
            updateProperty(mapEntryPropDef, map, nodes.get(i), 0);
        }

    }

    public void updateList(PropertyDefinitionList propDef, Property prop, AugeasNode listNode, int seq)
        throws AugeasRhqException {

        PropertyList listProperty = null;
        PropertyDefinition childDefinition = propDef.getMemberDefinition();

        if (prop instanceof PropertyList) {
            PropertyList lst = (PropertyList) prop;
            listProperty = (PropertyList) lst.getList().get(seq - 1);
        }

        if (prop instanceof PropertyMap) {
            PropertyMap map = (PropertyMap) prop;
            listProperty = (PropertyList) map.get(propDef.getName());
        }

        List<AugeasNode> nodes = tree.matchRelative(listNode, propDef.getName());
        AugeasNode node = null;

        if (nodes.isEmpty() | nodes.size() < seq) {
            node = tree.createNode(listNode, propDef.getName(), null, seq);
            nodes.add(node);
        } else
            node = nodes.get(seq);

        int i = 1;
        for (Property prp : listProperty.getList()) {
            System.out.println(prp.getName());
            updateProperty(childDefinition, listProperty, node, i);
            i = i + 1;
        }
    }

    public void updateSimple(AugeasNode parentNode, PropertyDefinitionSimple propDef, Property prop, int seq)
        throws AugeasRhqException {

        PropertySimple simpleProp = null;
        AugeasNode node = null;
        if (prop instanceof PropertyList) {
            PropertyList lst = (PropertyList) prop;
            List<Property> props = lst.getList();
            simpleProp = (PropertySimple) props.get(seq - 1);
        }

        if (prop instanceof PropertyMap) {
            PropertyMap map = (PropertyMap) prop;
            simpleProp = (PropertySimple) map.get(propDef.getName());
        }

        List<AugeasNode> nodes = tree.matchRelative(parentNode, propDef.getName());

        if (nodes.isEmpty()) {
            node = tree.createNode(parentNode, propDef.getName(), null, 1);
        } else
            node = nodes.get(0);

        node.setValue(simpleProp.getStringValue());

    }

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
