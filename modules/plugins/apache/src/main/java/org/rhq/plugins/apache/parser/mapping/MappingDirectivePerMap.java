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
package org.rhq.plugins.apache.parser.mapping;

import java.util.List;

import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheParserException;
import org.rhq.rhqtransform.AugeasRhqException;

/**
 * A mapping strategy that creates a map for each directive it finds
 * in the augeas tree. The map is supposed to be enclosed in a list.  
 * The name of the map definition is supposed to represent the name of the directives
 * to create the maps for.
 * 
 * @author Lukas Krejci
 */
public class MappingDirectivePerMap extends ApacheToConfigurationSimple {

    @Override
    public Property createPropertyList(PropertyDefinitionList propDefList, ApacheDirective node) throws ApacheParserException {
        PropertyList propList = new PropertyList(propDefList.getName());

        PropertyDefinition listMemberPropDef = propDefList.getMemberDefinition();

        List<ApacheDirective> nodes = tree.search(node, listMemberPropDef.getName());

        for (ApacheDirective nd : nodes) {
            propList.add(loadProperty(listMemberPropDef, nd));
        }

        return propList;
    }

    @Override
    public PropertyMap createPropertyMap(PropertyDefinitionMap propDefMap, ApacheDirective node) throws AugeasRhqException {
        String directiveName = propDefMap.getName();

        List<String> params = ApacheDirectiveRegExpression.getParams(node);

        PropertyMap map = new PropertyMap(directiveName);

        int idx = 0;
        for (PropertyDefinition propDef : propDefMap.getPropertyDefinitions().values()) {
            if (propDef instanceof PropertyDefinitionSimple) {
            	if (params.size()>idx)
                {
            	String value = params.get(idx);
                map.put(Util.createPropertySimple((PropertyDefinitionSimple) propDef, value));
                }
            }
            idx++;
        }
        return map;
    }

}
