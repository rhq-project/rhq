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
package org.rhq.rhqtransform;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;

/**
 * Mapping from RHQ configuration to Augeas tree.
 * 
 * @author Filip Drabek
 *
 */
public interface ConfigurationToAugeas {

    /**
     * Sets the tree the mapping should work with.
     * 
     * @param tree
     */
    public void setTree(AugeasTree tree);

    /**
     * Updates the augeas tree with the data from the RHQ configuration
     * 
     * @param node the node to start updating the data from
     * @param resourceConfigDef configuration definition of the configuration object
     * @param resourceConfig the configuration object to read the data from
     *  
     * @throws AugeasRhqException
     */
    public void updateResourceConfiguration(AugeasNode node, ConfigurationDefinition resourceConfigDef,
        Configuration resourceConfig) throws AugeasRhqException;

    /**
     * TODO this should be removed from the interface and made protected abstract in ConfigurationToAugeasSimple 
     * 
     * @param propDefMap
     * @param prop
     * @param mapNode
     * @param seq
     * @throws AugeasRhqException
     */
    public void updateMap(PropertyDefinitionMap propDefMap, Property prop, AugeasNode mapNode, int seq)
        throws AugeasRhqException;

    /**
     * TODO this should be removed from the interface and made protected abstract in ConfigurationToAugeasSimple 
     * 
     * @param propDef
     * @param prop
     * @param listNode
     * @param seq
     * @throws AugeasRhqException
     */
    public void updateList(PropertyDefinitionList propDef, Property prop, AugeasNode listNode, int seq)
        throws AugeasRhqException;

    /**
     * TODO this should be removed from the interface and made protected abstract in ConfigurationToAugeasSimple 
     * 
     * @param parentNode
     * @param propDef
     * @param prop
     * @param seq
     * @throws AugeasRhqException
     */
    public void updateSimple(AugeasNode parentNode, PropertyDefinitionSimple propDef, Property prop, int seq)
        throws AugeasRhqException;

    /**
     * Performs updates in the tree based on the values in the single property (and its descendants).
     * 
     * @param propDef the property definition of the property we are applying to the tree 
     * @param parentProp the parent property of the property we are dealing with
     * @param parentNode the parent node under which we should apply the property
     * @param seq the sequence number of the property we are applying inside the parentProp
     * 
     * @throws AugeasRhqException
     */
    public void updateProperty(PropertyDefinition propDef, Property parentProp, AugeasNode parentNode, int seq)
        throws AugeasRhqException;
}
