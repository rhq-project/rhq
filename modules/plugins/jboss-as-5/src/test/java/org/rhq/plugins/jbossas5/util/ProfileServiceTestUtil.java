 /*
  * Jopr Management Platform
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
package org.rhq.plugins.jbossas5.util;

 import java.util.HashMap;
 import java.util.Map;

 import org.rhq.core.domain.configuration.Configuration;
 import org.rhq.core.domain.configuration.Property;

 import org.jboss.metatype.api.types.ArrayMetaType;
 import org.jboss.metatype.api.types.CollectionMetaType;
 import org.jboss.metatype.api.types.CompositeMetaType;
 import org.jboss.metatype.api.types.GenericMetaType;
 import org.jboss.metatype.api.types.MapCompositeMetaType;
 import org.jboss.metatype.api.types.MetaType;
 import org.jboss.metatype.api.types.SimpleMetaType;
 import org.jboss.metatype.api.values.MetaValue;
 import org.jboss.metatype.api.values.SimpleValue;
 import org.jboss.metatype.api.values.SimpleValueSupport;
 import org.jboss.metatype.plugins.types.MutableCompositeMetaType;

public class ProfileServiceTestUtil
{
    public static final String GENERIC_TYPE = "Generic";
    public static final String SIMPLE_TYPE = "Simple";
    public static final String COMPOSITE_TYPE = "Composite";
    public static final String MAP_COMPOSITE_TYPE = "MapComposite";
    public static final String COLLECTION_TYPE = "Collection";
    public static final String ARRAY_TYPE = "Array";
    public static final String TABLE_TYPE = "Table";

    private static Map<String, MetaType> metaTypeInstances = new HashMap<String, MetaType>(6);

    private static Map<String, MetaValue> metaValueInstances = new HashMap<String, MetaValue>(6);

    private static Map<String, Property> propertyValueInstances = new HashMap<String, Property>(6);

    private static Configuration resourceConfiguration = new Configuration();

    public static Map<String, MetaType> createMetaTypes()
    {
        metaTypeInstances.clear();

        metaTypeInstances.put(GENERIC_TYPE, new GenericMetaType("String", "Just something"));
        SimpleValue simpleValue = SimpleValueSupport.wrap("SimpleValue");
        SimpleMetaType simpleMetaType = simpleValue.getMetaType();
        metaTypeInstances.put(SIMPLE_TYPE, simpleMetaType);
        CompositeMetaType compositeMetaType = new MutableCompositeMetaType("String", "Just something");
        metaTypeInstances.put(COMPOSITE_TYPE, compositeMetaType);
        CompositeMetaType mapCompositeMetaType = new MapCompositeMetaType(SimpleMetaType.STRING);
        metaTypeInstances.put(MAP_COMPOSITE_TYPE, mapCompositeMetaType);
        CollectionMetaType collectionMetaType = new CollectionMetaType("ClassName", simpleMetaType);
        metaTypeInstances.put(COLLECTION_TYPE, collectionMetaType);
        ArrayMetaType arrayMetaType = new ArrayMetaType(2, simpleMetaType);
        metaTypeInstances.put(ARRAY_TYPE, arrayMetaType);
        //TableMetaType tableMetaType = new ImmutableTableMetaType("TypeName", "Description", compositeMetaType, new String[]{"Tom", "Dick", "Harry"});
        //metaTypeInstances.put(TABLE_TYPE, tableMetaType);

        return metaTypeInstances;
    }

    public static Map<String, MetaValue> createMetaValues()
    {
        metaValueInstances.clear();
        initializePropertyValues();
        return metaValueInstances;
    }

    public static Map<String, Property> createPropertyValues()
    {
        propertyValueInstances.clear();
        initializePropertyValues();
        return propertyValueInstances;
    }

    public static Configuration createResourceConfigurationWithValues()
    {
        initializePropertyValues();
        resourceConfiguration.setProperties(propertyValueInstances.values());
        return resourceConfiguration;
    }

    public static Configuration createResourceConfigurationNoValues()
    {
        removePropertyValues();
        return resourceConfiguration;
    }

    private static void removePropertyValues()
    {
        resourceConfiguration.remove(GENERIC_TYPE);
        resourceConfiguration.remove(SIMPLE_TYPE);
        resourceConfiguration.remove(COMPOSITE_TYPE);
        resourceConfiguration.remove(MAP_COMPOSITE_TYPE);
        resourceConfiguration.remove(COLLECTION_TYPE);
        resourceConfiguration.remove(ARRAY_TYPE);
        resourceConfiguration.remove(TABLE_TYPE);
    }

    private static void initializePropertyValues()
    {
        propertyValueInstances.put(GENERIC_TYPE, null);
        propertyValueInstances.put(SIMPLE_TYPE, null);
        propertyValueInstances.put(COMPOSITE_TYPE, null);
        propertyValueInstances.put(MAP_COMPOSITE_TYPE, null);
        propertyValueInstances.put(COLLECTION_TYPE, null);
        propertyValueInstances.put(ARRAY_TYPE, null);
        propertyValueInstances.put(TABLE_TYPE, null);
    }
}
