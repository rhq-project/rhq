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

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

/**
 * 
 * 
 * @author Lukas Krejci
 */
public class Util {
    private Util() {
        
    }
    
    public static PropertySimple createPropertySimple(PropertyDefinitionSimple definition, String value) {
        String name = definition.getName();
        if (definition.getType() == PropertySimpleType.BOOLEAN) {
            return new PropertySimple(name, value != null);
        }

        //options with empty values correspond to null values
        if (!definition.getEnumeratedValues().isEmpty()) {
            if (value == null) value = "";
        }
        
        return new PropertySimple(name, value);
    }
}
