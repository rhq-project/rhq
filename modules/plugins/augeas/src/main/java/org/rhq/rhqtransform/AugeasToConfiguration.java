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
 * 
 * @author Filip Drabek
 *
 */
public interface AugeasToConfiguration {

    public void setTree(AugeasTree tree);

    public void setNameMap(NameMap nameMap);

    public Configuration loadResourceConfiguration(AugeasNode startNode, ConfigurationDefinition resourceConfigDef)
        throws Exception;

    public Property loadProperty(PropertyDefinition propDef, AugeasNode parentNode) throws Exception;

    public Property createPropertySimple(PropertyDefinitionSimple propDefSimple, AugeasNode node) throws Exception;

    public PropertyMap createPropertyMap(PropertyDefinitionMap propDefMap, AugeasNode node) throws Exception;

    public Property createPropertyList(PropertyDefinitionList propDefList, AugeasNode node) throws Exception;
}
