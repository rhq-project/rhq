/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.adapter.impl.configuration.custom;

import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;

import org.rhq.plugins.jbossas5.adapter.api.AbstractPropertySimpleAdapter;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;

import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.CompositeValue;
import org.jboss.metatype.api.values.CompositeValueSupport;
import org.jboss.metatype.api.values.SimpleValueSupport;
import org.jboss.metatype.api.values.PropertiesMetaValue;
import org.jboss.metatype.api.values.SimpleValue;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.types.ImmutableCompositeMetaType;
import org.jboss.metatype.api.types.SimpleMetaType;
import org.jboss.metatype.api.types.PropertiesMetaType;

/**
 * This class provides code that maps back and forth between a {@link PropertySimple} and
 * a {@link CompositeValueSupport} with typeName 'javax.management.ObjectName' - i.e. a
 * {@link CompositeValue} representing a JMX {@link ObjectName}.
 *
 * @author Ian Springer
 */
public class PropertySimpleToObjectNameCompositeValueAdapter extends AbstractPropertySimpleAdapter
{
    private static final String DOMAIN_ITEM_NAME = "domain";
    private static final String KEY_PROPERTY_LIST_ITEM_NAME = "keyPropertyList";
    private static final String DOMAIN_ITEM_DESCRIPTION = "The domain name";
    private static final String KEY_PROPERTY_LIST_ITEM_DESCRIPTION = "The unordered set of keys and associated values";

    public void populateMetaValueFromProperty(PropertySimple propSimple, MetaValue metaValue,
                                              PropertyDefinitionSimple propertyDefSimple)
    {
        String value = propSimple.getStringValue();
        if (value == null)
            // nothing to populate  :-)
            return;
        ObjectName objectName;
        try
        {
            objectName = new ObjectName(value);
        }
        catch (MalformedObjectNameException e)
        {
            throw new RuntimeException("Value of property '" + propSimple.getName() + "' is invalid - '" + value
                    + "' is an invalid JMX ObjectName.", e);
        }
        // The Profile service doesn't support pattern-style ObjectNames (see DefaultMetaValueFactory.unwrapComposite()).
        if (objectName.isPattern())
            throw new RuntimeException("Value of property '" + propSimple.getName() + "' is invalid - '" + value
                    + "' is a pattern JMX ObjectName.");
        CompositeValueSupport compositeValue = (CompositeValueSupport)metaValue;
        SimpleValueSupport domainValue = (SimpleValueSupport)compositeValue.get(DOMAIN_ITEM_NAME);
        if (domainValue == null) {
            domainValue = new SimpleValueSupport(SimpleMetaType.STRING, objectName.getDomain());
            compositeValue.set(DOMAIN_ITEM_NAME, domainValue);
        } else {
            domainValue.setValue(objectName.getDomain());
        }
        PropertiesMetaValue keyPropertyListValue = (PropertiesMetaValue)compositeValue.get(KEY_PROPERTY_LIST_ITEM_NAME);
        if (keyPropertyListValue == null) {
            keyPropertyListValue = PropertiesMetaValue.wrap(objectName.getKeyPropertyList());
            compositeValue.set(KEY_PROPERTY_LIST_ITEM_NAME, keyPropertyListValue);
        } else {
            keyPropertyListValue.clear();
            keyPropertyListValue.putAll(objectName.getKeyPropertyList());
        }
    }

    public MetaValue convertToMetaValue(PropertySimple propSimple, PropertyDefinitionSimple propDefSimple,
                                        MetaType metaType)
    {
        if (propSimple.getStringValue() == null)
            return null;
        CompositeValue compositeValue = createObjectNameCompositeValue(metaType);
        populateMetaValueFromProperty(propSimple, compositeValue, propDefSimple);
        return compositeValue;
    }

    public void populatePropertyFromMetaValue(PropertySimple propSimple, MetaValue metaValue,
                                              PropertyDefinitionSimple propDefSimple)
    {
        CompositeValue compositeValue = (CompositeValue)metaValue;
        if (compositeValue == null)
            // nothing to populate  :-)
            return;
        SimpleValue domainValue = (SimpleValue)compositeValue.get(DOMAIN_ITEM_NAME);
        String domain = (String)domainValue.getValue();        
        PropertiesMetaValue keyPropertyListValue = (PropertiesMetaValue)compositeValue.get(KEY_PROPERTY_LIST_ITEM_NAME);
        Hashtable<String, String> keyProps = new Hashtable();
        Enumeration<?> keyPropNames = keyPropertyListValue.propertyNames();
        while (keyPropNames.hasMoreElements())
        {
            String keyPropName = (String)keyPropNames.nextElement();
            String keyPropValue = keyPropertyListValue.getProperty(keyPropName);
            keyProps.put(keyPropName, keyPropValue);
        }
        ObjectName objectName;
        try
        {
            objectName = new ObjectName(domain, keyProps);
        }
        catch (MalformedObjectNameException e)
        {
            throw new RuntimeException("Value of ManagedProperty '" + propSimple.getName() + "' is invalid - ["
                    + metaValue + "] could not be converted to a JMX ObjectName.", e);
        }
        propSimple.setStringValue(objectName.toString());
    }

    protected CompositeValue createObjectNameCompositeValue(MetaType metaType) {
        ImmutableCompositeMetaType compositeMetaType;
        if (metaType != null)
            compositeMetaType = (ImmutableCompositeMetaType)metaType;
        else {
            compositeMetaType = new ImmutableCompositeMetaType(ObjectName.class.getName(), ObjectName.class.getName(),
                    new String[] {DOMAIN_ITEM_NAME, KEY_PROPERTY_LIST_ITEM_NAME},
                    new String[] {DOMAIN_ITEM_DESCRIPTION, KEY_PROPERTY_LIST_ITEM_DESCRIPTION},
                    new MetaType[] {SimpleMetaType.STRING, new PropertiesMetaType(Properties.class.getName())});
        }
        return new CompositeValueSupport(compositeMetaType);
    }
}
