/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.core.domain.configuration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.configuration.definition.PropertyDefinition;

/**
 * Describes a property whose values are retrieved from a dynamic source. Currently, the only source supported
 * is database. Changes will likely need to be made when plugin based value sources are introduced.
 * 
 * @author Jason Dobies
 */
@DiscriminatorValue("dynamic")
@Entity(name = "PropertyDefinitionDynamic")
@XmlRootElement(name = "PropertyDefinitionDynamic")
@XmlAccessorType(XmlAccessType.FIELD)
public class PropertyDefinitionDynamic extends PropertyDefinition {

    private static final long serialVersionUID = 1L;

    @Column(name = "DYNAMIC_TYPE")
    @Enumerated(EnumType.STRING)
    private PropertyDynamicType dynamicType;

    @Column(name = "DYNAMIC_KEY")
    private String key;

    public PropertyDefinitionDynamic() {
    }

    public PropertyDefinitionDynamic(String name, String description, boolean required,
                                     PropertyDynamicType dynamicType, String key) {
        super(name, description, required);
        this.dynamicType = dynamicType;
        this.key = key;
    }

    public PropertyDynamicType getDynamicType() {
        return dynamicType;
    }

    public void setDynamicType(PropertyDynamicType dynamicType) {
        this.dynamicType = dynamicType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
