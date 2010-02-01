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

package org.rhq.plugins.apache.augeas.mappingImpl;

import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.rhqtransform.AugeasRhqException;
import org.rhq.rhqtransform.impl.AugeasToConfigurationSimple;

/**
 * A simple mapping strategy for simple properties.
 * 
 * @author Lukas Krejci
 */
public class MappingDirectiveToSimpleProperty extends AugeasToConfigurationSimple {

    @Override
    public Property createPropertySimple(PropertyDefinitionSimple propDef, AugeasNode parentNode)
        throws AugeasRhqException {

        String propertyName = propDef.getName();

        List<AugeasNode> simpleNode = parentNode.getChildByLabel(propertyName);

        if (simpleNode.size() > 1) {
            throw new AugeasRhqException("Found multiple values for a simple property " + propertyName);
        }

        if (simpleNode.isEmpty()) {
            return new PropertySimple(propertyName, null);
        } else {
            String value = null;
            List<AugeasNode> params = simpleNode.get(0).getChildByLabel("param");
            if (params.size() > 0) {
                StringBuilder valueBld = new StringBuilder();
                for (AugeasNode param : params) {
                    valueBld.append(param.getValue()).append(" ");
                }

                valueBld.deleteCharAt(valueBld.length() - 1);
                value = valueBld.toString();
            }
            return Util.createPropertySimple((PropertyDefinitionSimple) propDef, value);
        }
    }

}
