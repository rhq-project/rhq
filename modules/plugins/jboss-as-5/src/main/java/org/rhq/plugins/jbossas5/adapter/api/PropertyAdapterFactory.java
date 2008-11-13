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

import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.values.MetaValue;
import org.rhq.plugins.jbossas5.adapter.impl.configuration.PropertyListToArrayMetaValueAdapter;
import org.rhq.plugins.jbossas5.adapter.impl.configuration.PropertyListToCollectionMetaValueAdapter;
import org.rhq.plugins.jbossas5.adapter.impl.configuration.PropertyMapToCompositeMetaValueAdapter;
import org.rhq.plugins.jbossas5.adapter.impl.configuration.PropertyMapToTableMetaValueAdapter;
import org.rhq.plugins.jbossas5.adapter.impl.configuration.PropertySimpleToGenericMetaValueAdapter;
import org.rhq.plugins.jbossas5.adapter.impl.configuration.PropertySimpleToSimpleMetaValueAdapter;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Mark Spritzler
 */
public class PropertyAdapterFactory
{
    private static final Log LOG = LogFactory.getLog(PropertyAdapterFactory.class);

    public static PropertyAdapter getPropertyAdapter(MetaValue metaValue)
    {
        if (metaValue == null)
        {
            LOG.debug("The MetaValue passed in is null");
        }
        MetaType metaType = metaValue.getMetaType();
        return getPropertyAdapter(metaType);
    }

    public static PropertyAdapter getPropertyAdapter(MetaType metaType)
    {
        PropertyAdapter propertyAdapter = null;
        if (metaType.isSimple())
        {
            propertyAdapter = new PropertySimpleToSimpleMetaValueAdapter();
        }
        else if (metaType.isGeneric())
        {
            propertyAdapter = new PropertySimpleToGenericMetaValueAdapter();
        }
        else if (metaType.isComposite())
        {
            propertyAdapter = new PropertyMapToCompositeMetaValueAdapter();
        }
        else if (metaType.isTable())
        {
            propertyAdapter = new PropertyMapToTableMetaValueAdapter();
        }
        else if (metaType.isCollection())
        {
            propertyAdapter = new PropertyListToCollectionMetaValueAdapter();
        }
        else if (metaType.isArray())
        {
            propertyAdapter = new PropertyListToArrayMetaValueAdapter();
        }
        return propertyAdapter;
    }

    public static PropertyAdapter getCustomPropertyAdapter(PropertyDefinitionSimple definition)
    {
        String adapterClassName = definition.getDefaultValue();
        String propertyName = definition.getName();
        PropertyAdapter propertyAdapter = null;
        try
        {
            Class clazz = Class.forName(adapterClassName);
            propertyAdapter = (PropertyAdapter) clazz.newInstance();
        }
        catch (InstantiationException e)
        {
            LOG.error("Unable to create custom adapter class for " + propertyName, e);
        }
        catch (IllegalAccessException e)
        {
            LOG.error("Illegal Access Exception thrown when creating custom adapter class for " + propertyName, e);
        }
        catch (ClassNotFoundException e)
        {
            LOG.error("Class not found for custom adapter class for " + propertyName, e);
        }

        return propertyAdapter;
    }
}
