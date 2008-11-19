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

import org.jboss.metatype.api.types.GenericMetaType;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.values.GenericValue;
import org.jboss.metatype.api.values.GenericValueSupport;
import org.jboss.metatype.api.values.SimpleValue;
import org.jboss.metatype.api.values.SimpleValueSupport;
import org.rhq.plugins.jbossas5.adapter.impl.configuration.PropertyListToArrayMetaValueAdapter;
import org.rhq.plugins.jbossas5.adapter.impl.configuration.PropertyListToCollectionMetaValueAdapter;
import org.rhq.plugins.jbossas5.adapter.impl.configuration.PropertyMapToMapCompositeValueSupportAdapter;
import org.rhq.plugins.jbossas5.adapter.impl.configuration.PropertySimpleToSimpleMetaValueAdapter;
import org.rhq.plugins.jbossas5.adapter.impl.configuration.PropertyMapToGenericValueAdapter;
import org.rhq.plugins.jbossas5.util.ProfileServiceTestUtil;
import org.testng.annotations.Test;

import java.util.Map;

public class PropertyAdapterFactoryTest
{
    @Test
    public void testGetPropertyAdapterFromValue()
    {
        MetaType genericMetaType = ProfileServiceTestUtil.createMetaTypes().get(ProfileServiceTestUtil.GENERIC_TYPE);
        GenericValue genericValue = new GenericValueSupport((GenericMetaType) genericMetaType, "GenericValue");
        PropertyAdapter adapter1 = PropertyAdapterFactory.getPropertyAdapter(genericValue);
        assert adapter1 instanceof PropertyMapToGenericValueAdapter : "Should be the PropertyMap to Generic adapter";

        SimpleValue simpleValue = SimpleValueSupport.wrap("SimpleValue");
        PropertyAdapter adapter2 = PropertyAdapterFactory.getPropertyAdapter(simpleValue);
        assert adapter2 instanceof PropertySimpleToSimpleMetaValueAdapter : "Should be the PropertySimple to Simple adapter";

    }

    @Test
    public void testGetPropertyAdapterFromType()
    {
        Map<String, MetaType> metaTypeInstances = ProfileServiceTestUtil.createMetaTypes();

        PropertyAdapter adapter1 = PropertyAdapterFactory.getPropertyAdapter(metaTypeInstances.get(ProfileServiceTestUtil.GENERIC_TYPE));
        assert adapter1 instanceof PropertyMapToGenericValueAdapter : "Should be the PropertyMap to Generic adapter";

        MetaType simpleMetaType = metaTypeInstances.get(ProfileServiceTestUtil.SIMPLE_TYPE);
        PropertyAdapter adapter2 = PropertyAdapterFactory.getPropertyAdapter(simpleMetaType);
        assert adapter2 instanceof PropertySimpleToSimpleMetaValueAdapter : "Should be the PropertySimple to Simple adapter";

        MetaType compositeMetaType = metaTypeInstances.get(ProfileServiceTestUtil.COMPOSITE_TYPE);
        PropertyAdapter adapter3 = PropertyAdapterFactory.getPropertyAdapter(compositeMetaType);
        assert adapter3 instanceof PropertyMapToMapCompositeValueSupportAdapter : "Should be the PropertyMap to Composite adapter";

        PropertyAdapter adapter4 = PropertyAdapterFactory.getPropertyAdapter(metaTypeInstances.get(ProfileServiceTestUtil.COLLECTION_TYPE));
        assert adapter4 instanceof PropertyListToCollectionMetaValueAdapter : "Should be the PropertyList to Collection adapter";

        PropertyAdapter adapter5 = PropertyAdapterFactory.getPropertyAdapter(metaTypeInstances.get(ProfileServiceTestUtil.ARRAY_TYPE));
        assert adapter5 instanceof PropertyListToArrayMetaValueAdapter : "Should be the PropertyList to Array adapter";

        //PropertyAdapter adapter6 = PropertyAdapterFactory.getPropertyAdapter(metaTypeInstances.get(ProfileServiceTestUtil.TABLE_TYPE));
        //assert adapter6 instanceof PropertyMapToTableMetaValueAdapter : "Should be the PropertyMap to Table adapter";
    }

    @Test
    public void testGetCustomPropertyAdapter()
    {

    }
}
