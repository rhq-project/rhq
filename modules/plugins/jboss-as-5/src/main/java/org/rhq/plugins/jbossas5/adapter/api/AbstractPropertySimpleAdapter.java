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
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

import java.io.Serializable;

/**
 * A base class for PropertySimple <-> ???MetaValue adapters.
 */
public abstract class AbstractPropertySimpleAdapter implements PropertyAdapter<PropertySimple, PropertyDefinitionSimple>
{
    public PropertySimple getProperty(MetaValue metaValue, PropertyDefinitionSimple propDef)
    {
        String key = propDef.getName();
        PropertySimple prop = new PropertySimple(key, null);
        setPropertyValues(prop, metaValue, propDef);
        return prop;
    }

    public Serializable getSimplePropertyValue(PropertySimple prop, PropertyDefinitionSimple propDef)
    {
        Serializable value = null;
        if (propDef != null)
        {
            PropertySimpleType type = propDef.getType();
            switch (type)
            {
                case BOOLEAN:
                {
                    value = prop.getBooleanValue();
                    break;
                }
                case INTEGER:
                {
                    value = prop.getIntegerValue();
                    break;
                }
                case LONG:
                {
                    value = prop.getLongValue();
                    break;
                }
                case FLOAT:
                {
                    value = prop.getFloatValue();
                    break;
                }
                case DOUBLE:
                {
                    value = prop.getDoubleValue();
                    break;
                }
            }
        }
        if (value == null)
        {
            value = prop.getStringValue();
        }
        return value;
    }
}
