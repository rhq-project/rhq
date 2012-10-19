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
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;

/**
 * Implementations of this interface provide mapping of Augeas data
 * to the RHQ configuration.
 * 
 * @author Filip Drabek
 *
 */
public interface AugeasToConfiguration {

    /**
     * Sets the Augeas tree that should be worked with.
     * 
     * @param tree
     */
    public void setTree(AugeasTree tree);

    /**
     * Provides the mapper with name conversion from Augeas node names to RHQ
     * configuration property names.
     * 
     * @param nameMap
     */
    public void setNameMap(NameMap nameMap);

    /**
     * Loads the RHQ configuration instance from the Augeas tree.
     *  
     * @param startNode the node to start the mapping from
     * @param resourceConfigDef the configuration definition to use
     * @return the RHQ configuration with the values loaded from the Augeas tree 
     * @throws AugeasRhqException
     */
    public Configuration loadResourceConfiguration(AugeasNode startNode, ConfigurationDefinition resourceConfigDef)
        throws AugeasRhqException;

    /**
     * Loads a single property from given node.
     * 
     * @param propDef the definition of the property
     * @param parentNode the parent node from which the property should be loaded.
     * @return
     * @throws AugeasRhqException
     */
    public Property loadProperty(PropertyDefinition propDef, AugeasNode parentNode) throws AugeasRhqException;

    /**
     * TODO this should be removed from the interface and made protected abstract in AugeasToConfigurationSimple 
     * Creates a simple property from the property definition.
     * 
     * @param propDefSimple the definition of the property
     * @param parentNode the parent node where the property should be looked for
     * @return
     * @throws AugeasRhqException
     */
    public Property createPropertySimple(PropertyDefinitionSimple propDefSimple, AugeasNode parentNode) throws AugeasRhqException;

    /**
     * TODO this should be removed from the interface and made protected abstract in AugeasToConfigurationSimple 
     * Creates a map property from the data in the parent node.
     * 
     * @param propDefMap the definition of the property map
     * @param parentNode the parent node
     * @return
     * @throws AugeasRhqException
     */
    public PropertyMap createPropertyMap(PropertyDefinitionMap propDefMap, AugeasNode parentNode) throws AugeasRhqException;

    /**
     * TODO this should be removed from the interface and made protected abstract in AugeasToConfigurationSimple 
     * Creates a property list from the data in the parent node.
     * 
     * @param propDefList
     * @param parentNode
     * @return
     * @throws AugeasRhqException
     */
    public Property createPropertyList(PropertyDefinitionList propDefList, AugeasNode parentNode) throws AugeasRhqException;
}
