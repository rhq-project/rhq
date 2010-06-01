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

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

/**
 * Methods common to the mapping classes in this package. 
 * 
 * @author Lukas Krejci
 */
class Util {
    private Util() {
        
    }
    
    /**
     * Creates a simple property from given definition and given value.
     * 
     * If the property is a boolean, it is true if the value is non-null.
     * 
     * If the property has enumerated possible values, the value is compared to them 
     * in case insensitive manner.
     * 
     * @param definition the definition of the property to create
     * @param value the value of the property
     * @return a new simple property
     */
    public static PropertySimple createPropertySimple(PropertyDefinitionSimple definition, String value) {
        String name = definition.getName();
        if (definition.getType() == PropertySimpleType.BOOLEAN) {
            return new PropertySimple(name, value != null);
        }

        if (!definition.getEnumeratedValues().isEmpty()) {
            //options with empty values correspond to null values
            if (value == null) value = "";
            
            //apache configuration values are usually case-insensitive.
            String valLowerCase = value.toLowerCase();
            for (PropertyDefinitionEnumeration option : definition.getEnumeratedValues()) {
                if (option.getValue().toLowerCase().equals(valLowerCase)) {
                    value = option.getValue();
                }
            }
        }
        
        return new PropertySimple(name, value);
    }
}
