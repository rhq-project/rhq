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

import java.io.Serializable;

import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.SimpleValueSupport;
import org.jboss.metatype.api.values.SimpleValue;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.types.SimpleMetaType;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.jbossas5.adapter.api.AbstractPropertySimpleAdapter;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapter;

 /**
 * @author Mark Spritzler
 */
public class PropertySimpleToSimpleValueAdapter extends AbstractPropertySimpleAdapter implements PropertyAdapter<PropertySimple, PropertyDefinitionSimple>
{
    public void populateMetaValueFromProperty(PropertySimple propSimple, MetaValue metaValue, PropertyDefinitionSimple propDefSimple)
    {
        if (propSimple == null || metaValue == null)
            return;
        SimpleValueSupport simpleValueSupport = (SimpleValueSupport) metaValue;
        Serializable value = getSimplePropertyValue(propSimple, propDefSimple);
        simpleValueSupport.setValue(value);
    }

    public void populatePropertyFromMetaValue(PropertySimple propSimple, MetaValue metaValue, PropertyDefinitionSimple propDefSimple)
    {
        Object value = (metaValue != null) ? ((SimpleValue) metaValue).getValue() : null;
        propSimple.setValue(value);
    }

    public MetaValue convertToMetaValue(PropertySimple propSimple, PropertyDefinitionSimple propDefSimple, MetaType metaType)
    {
        if (propSimple.getStringValue() == null)
            return null;
        SimpleValue simpleValue = new SimpleValueSupport((SimpleMetaType) metaType,
                getSimplePropertyValue(propSimple, propDefSimple));
        populateMetaValueFromProperty(propSimple, simpleValue, propDefSimple);                    
        return simpleValue;
    }
}
