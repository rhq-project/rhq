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
package org.rhq.plugins.apache.parser.mapping.load;

import java.util.List;

import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.apache.ApacheServerComponent;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheParserException;
import org.rhq.plugins.apache.parser.mapping.ApacheDirectiveRegExpression;

/**
 * A mapping strategy similar to {@link MappingDirectivePerMap}.
 * In addition to base class, the map definition is checked for
 * a property called {@link ApacheServerComponent#AUXILIARY_INDEX_PROP}
 * that is supposed to contain the index of the directive inside the
 * configuration file and if found the property is set the appropriate
 * value.
 * 
 * @author Lukas Krejci
 */
public class MappingDirectivePerMapIndex extends MappingDirectivePerMap {

    @Override
    public PropertyMap createPropertyMap(PropertyDefinitionMap propDefMap, ApacheDirective node) throws ApacheParserException {
        String directiveName = propDefMap.getName();

        List<String> params = ApacheDirectiveRegExpression.getParams(node);

        PropertyMap map = new PropertyMap(directiveName);

        int idx = 0;
        for (PropertyDefinition propDef : propDefMap.getPropertyDefinitions().values()) {
            if (propDef instanceof PropertyDefinitionSimple) {
                if (ApacheServerComponent.AUXILIARY_INDEX_PROP.equals(propDef.getName())) {
                    map.put(new PropertySimple(ApacheServerComponent.AUXILIARY_INDEX_PROP, node.getSeq()));
                    continue;
                }
                String value = params.get(idx);
                map.put(Util.createPropertySimple((PropertyDefinitionSimple) propDef, value));
            }
            idx++;
        }
        return map;
    }

    
}
