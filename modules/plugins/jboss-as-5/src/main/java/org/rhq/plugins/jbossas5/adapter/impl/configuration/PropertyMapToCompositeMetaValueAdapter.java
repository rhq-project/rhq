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

import org.jboss.metatype.api.types.MapCompositeMetaType;
import org.jboss.metatype.api.types.SimpleMetaType;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.values.MapCompositeValueSupport;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.SimpleValue;
import org.jboss.metatype.api.values.SimpleValueSupport;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.plugins.jbossas5.adapter.api.AbstractPropertyMapAdapter;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapter;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapterFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.Set;

/**
 * @author Mark Spritzler
 */
public class PropertyMapToCompositeMetaValueAdapter extends AbstractPropertyMapAdapter implements PropertyAdapter<PropertyMap>
{

    private static final Log LOG = LogFactory.getLog(PropertyMapToCompositeMetaValueAdapter.class);

    public void setMetaValues(PropertyMap property, MetaValue metaValue, PropertyDefinition propertyDefinition)
    {
        PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) propertyDefinition;
        MapCompositeValueSupport valueSupport = (MapCompositeValueSupport) metaValue;
        Map<String, Property> mapProperty = property.getMap();

        Map<String, PropertyDefinition> definitionsInMap = propertyDefinitionMap.getPropertyDefinitions();
        Set<String> keySet = mapProperty.keySet();
        for (String key : keySet)
        {
            Property insideMapProperty = mapProperty.get(key);
            MetaValue value = valueSupport.get(key);
            PropertyDefinition keyDefinition = definitionsInMap.get(key);
            PropertyAdapter adapter = PropertyAdapterFactory.getPropertyAdapter(value);
            adapter.setMetaValues(insideMapProperty, value, keyDefinition);
        }
    }

    public MetaValue getMetaValue(PropertyMap property, PropertyDefinition propertyDefinition, MetaType type)
    {
        LOG.debug("GetMetaValue for property: " + property.getName() + " keys: " + property.getMap().keySet().toString() + "\n values " + property.getMap().values().toString());
        SimpleValue simpleValue = SimpleValueSupport.wrap("");
        SimpleMetaType simpleType = simpleValue.getMetaType();
        MapCompositeValueSupport valueSupport = new MapCompositeValueSupport(new MapCompositeMetaType(simpleType));
        setMetaValues(property, valueSupport, propertyDefinition);
        return valueSupport;
    }

    public void setPropertyValues(PropertyMap property, MetaValue metaValue, PropertyDefinition propertyDefinition)
    {
        /* This really should be going the other direction, but you can't get a collection of keys
        * from the CompositeValue object
        */
        if (metaValue != null)
        {
            MapCompositeValueSupport valueSupport = (MapCompositeValueSupport) metaValue;
            PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) propertyDefinition;
            Map<String, PropertyDefinition> definitionsInMap = propertyDefinitionMap.getPropertyDefinitions();
            Map<String, Property> map = property.getMap();
            Set<String> keySet = valueSupport.getMetaType().keySet();
            // There won't be any keys when loading a configuration for the first time.
            for (String key : keySet)
            {
                Property insideMapProperty = map.get(key);
                if (insideMapProperty == null)
                {
                    LOG.debug("Properties inside the map are null so creating new empty ones");
                    insideMapProperty = new PropertySimple(key, null);
                    map.put(key, insideMapProperty);
                }
                MetaValue value = valueSupport.get(key);
                PropertyDefinition keyDefinition = definitionsInMap.get(key);
                PropertyAdapter adapter = PropertyAdapterFactory.getPropertyAdapter(value);
                adapter.setPropertyValues(insideMapProperty, value, keyDefinition);
            }
        }
    }
}
