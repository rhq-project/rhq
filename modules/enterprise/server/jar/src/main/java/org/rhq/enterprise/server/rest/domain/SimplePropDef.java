/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.enterprise.server.rest.domain;

import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

import org.rhq.core.domain.configuration.definition.PropertySimpleType;

/**
 * Simple property definition
 * @author Heiko W. Rupp
 */
@ApiClass(value = "A simple property definition", description = "This bean is e.g. used for parameters of operations")
@XmlRootElement
public class SimplePropDef {

    String name;
    boolean required;
    PropertySimpleType type;
    String defaultValue;

    public SimplePropDef() {
    }

    @ApiProperty("The name of the property")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiProperty("Indicates if the property is required i.e. has to have a value set")
    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    @ApiProperty(value = "The type of the property",
        allowableValues = "STRING, LONG_STRING, PASSWORD, BOOLEAN, INTEGER, LONG, FLOAT, DOUBLE, FILE, DIRECTORY")
    public PropertySimpleType getType() {
        return type;
    }

    public void setType(PropertySimpleType type) {
        this.type = type;
    }

    @ApiProperty("The string representation of a default value if defined.")
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
