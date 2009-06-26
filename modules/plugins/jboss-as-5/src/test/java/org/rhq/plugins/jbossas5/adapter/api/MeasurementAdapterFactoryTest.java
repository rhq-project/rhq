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
package org.rhq.plugins.jbossas5.adapter.api;

import java.util.Map;

import org.rhq.plugins.jbossas5.adapter.impl.measurement.SimpleMetaValueMeasurementAdapter;
import org.rhq.plugins.jbossas5.util.ProfileServiceTestUtil;
import org.testng.annotations.Test;

import org.jboss.metatype.api.types.MetaType;

@Test
public class MeasurementAdapterFactoryTest
{
    public void testGetMeasurementAdapter()
    {
        Map<String, MetaType> metaTypeInstances = ProfileServiceTestUtil.createMetaTypes();

        MetaType simpleMetaType = metaTypeInstances.get(ProfileServiceTestUtil.SIMPLE_TYPE);
        MeasurementAdapter adapter = MeasurementAdapterFactory.getMeasurementPropertyAdapter(simpleMetaType);
        assert adapter instanceof SimpleMetaValueMeasurementAdapter : "MeasurementAdapterFactory did not return a Simple adapter as expected";
    }

    public void testGetCustomMeasurementAdapter()
    {
        MeasurementAdapter adapter = MeasurementAdapterFactory.getCustomMeasurementPropertyAdapter("messageCounters");
        assert adapter == null : "Until we have custom measurement adapters defined, this shoud be null";
        //assert adapter instanceof SimpleMetaValueMeasurementAdapter : "MeasurementAdapterFactory did not return the correct custom adapter";
    }
}
