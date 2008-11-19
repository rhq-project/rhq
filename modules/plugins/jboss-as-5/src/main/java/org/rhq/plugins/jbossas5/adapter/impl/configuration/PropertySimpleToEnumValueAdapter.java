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
package org.rhq.plugins.jbossas5.adapter.impl.configuration;

import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.EnumValueSupport;
import org.jboss.metatype.api.values.EnumValue;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.types.EnumMetaType;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.jbossas5.adapter.api.AbstractPropertySimpleAdapter;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

 /**
 * @author Ian Springer
 */
public class PropertySimpleToEnumValueAdapter extends AbstractPropertySimpleAdapter implements PropertyAdapter<PropertySimple, PropertyDefinitionSimple>
{
    private static final Log LOG = LogFactory.getLog(PropertySimpleToEnumValueAdapter.class);

    public void setMetaValues(PropertySimple property, MetaValue metaValue, PropertyDefinitionSimple propertyDefinition)
    {
        EnumValueSupport enumValueSupport = (EnumValueSupport) metaValue;
        if (property != null)
        {
            String value = property.getStringValue();
            if (value != null && !value.equals(""))
            {
                if (metaValue != null)
                    enumValueSupport.setValue(property.getStringValue());
            }
        }
    }

    public void setPropertyValues(PropertySimple property, MetaValue metaValue, PropertyDefinitionSimple propertyDefinition)
    {
        Object value = (metaValue != null) ? ((EnumValue) metaValue).getValue() : null;
        property.setValue(value);
    }

    public MetaValue getMetaValue(PropertySimple property, PropertyDefinitionSimple propertyDefinition, MetaType type)
    {
        String value = property.getStringValue();
        MetaValue metaValue = null;
        if (value != null && !value.equals(""))
        {
            metaValue = new EnumValueSupport((EnumMetaType) type, value);
            setMetaValues(property, metaValue, propertyDefinition);
            LOG.debug("Delegating property adapter because metaValue was passed in as null for property: " + property.getName() + " value: " + property.getStringValue());
        }
        return metaValue;
    }
}