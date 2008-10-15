 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.adapter.api;

import org.jboss.metatype.api.values.MetaValue;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

import java.io.Serializable;


public abstract class AbstractPropertySimpleAdapter implements PropertyAdapter<PropertySimple>
{

    public PropertySimple getProperty(MetaValue metaValue, PropertyDefinition propertyDefinition)
    {
        String key = propertyDefinition.getName();
        PropertySimple property = new PropertySimple(key, null);
        setPropertyValues(property, metaValue, propertyDefinition);
        return property;
    }

    public Serializable getSimplePropertyValue(PropertySimple property, PropertyDefinition definition)
    {
        Serializable value = null;
        if (definition != null)
        {
            PropertyDefinitionSimple simpleDefinition = (PropertyDefinitionSimple) definition;
            PropertySimpleType type = simpleDefinition.getType();
            switch (type)
            {
                case BOOLEAN:
                {
                    value = property.getBooleanValue();
                    break;
                }
                case INTEGER:
                {
                    value = property.getIntegerValue();
                    break;
                }
                case LONG:
                {
                    value = property.getLongValue();
                    break;
                }
                case FLOAT:
                {
                    value = property.getFloatValue();
                    break;
                }
                case DOUBLE:
                {
                    value = property.getDoubleValue();
                    break;
                }
            }
        }
        if (value == null)
        {
            value = property.getStringValue();
        }
        return value;
    }
}
