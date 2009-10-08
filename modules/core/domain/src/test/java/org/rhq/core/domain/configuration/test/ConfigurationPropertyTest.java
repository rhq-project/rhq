 /*
  * RHQ Management Platform
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
package org.rhq.core.domain.configuration.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * @author Charles Crouch
 * @author Ian Springer
 */
public class ConfigurationPropertyTest {
    // really just exercising the API rather than testing the implementation
    @Test
    public void testConfigurationPropertyGetters() {
        Configuration config = new Configuration();

        PropertyMap map = new PropertyMap("map1");
        PropertySimple mapValue = new PropertySimple("mapval1", 1);
        map.put(mapValue);

        config.put(map);

        PropertyList list = new PropertyList("list1");
        PropertySimple listValue = new PropertySimple("listval1", 1);
        list.add(listValue);

        config.put(list);

        PropertySimple simple = new PropertySimple("simple1", 1);

        config.put(simple);

        Collection<Property> allProperties = config.getProperties();
        assert allProperties.size() == 3;

        Map<String, PropertyMap> mapProperties = config.getMapProperties();
        assert mapProperties.size() == 1;

        Map<String, PropertyList> listProperties = config.getListProperties();
        assert listProperties.size() == 1;

        Map<String, PropertySimple> simpleProperties = config.getSimpleProperties();
        assert simpleProperties.size() == 1;
    }

    // really just exercising the API rather than testing the implementation
    @Test
    public void testPropertySimpleSetters() {
        PropertySimple simpleInteger = new PropertySimple("a", 1);
        simpleInteger.setIntegerValue(2);
        assert simpleInteger.getIntegerValue().equals(2);

        PropertySimple simpleLong = new PropertySimple("a", (long) 1);
        simpleLong.setLongValue((long) 2);
        assert simpleLong.getLongValue().equals((long) 2);

        PropertySimple simpleDouble = new PropertySimple("a", (double) 1);
        simpleDouble.setDoubleValue((double) 2);
        assert simpleDouble.getDoubleValue().equals((double) 2);

        PropertySimple simpleFloat = new PropertySimple("a", (float) 1);
        simpleFloat.setFloatValue((float) 2);
        assert simpleFloat.getFloatValue().equals((float) 2);

        PropertySimple simpleBoolean = new PropertySimple("a", false);
        simpleBoolean.setBooleanValue(true);
        assert simpleBoolean.getBooleanValue().equals(true);

        PropertySimple simpleString = new PropertySimple("a", "a");
        simpleString.setStringValue("b");
        assert simpleString.getStringValue().equals("b");
    }

    @Test
    public void testEquals() {
        PropertySimple simpleProp1 = new PropertySimple("a", "a");
        PropertySimple simpleProp2 = new PropertySimple("a", "a");
        assert simpleProp1.equals(simpleProp2);
        PropertySimple simpleProp3 = new PropertySimple("a", "b");
        assert !simpleProp1.equals(simpleProp3);
        PropertySimple simpleProp4 = new PropertySimple("b", "a");
        assert !simpleProp1.equals(simpleProp4);
        PropertySimple simpleProp5 = new PropertySimple("b", "b");
        assert !simpleProp1.equals(simpleProp5);
    }

    @Test
    public void testPropertyListMemberPropertyNameValidation() {
        try {
            new PropertyList("foo", new PropertySimple("a", "a"), new PropertySimple("b", "b"));
            assert false : "creation of PropertyList with mixed-name members succeeded.";
        } catch (IllegalStateException e) {
            // expected behavior
        }

        PropertyList list = new PropertyList("foo");
        List<Property> members = new ArrayList<Property>();
        members.add(new PropertySimple("a", "a"));
        members.add(new PropertySimple("b", "b"));
        try {
            list.setList(members);
            assert false : "setting of PropertyList with mixed-name members succeeded.";
        } catch (Exception e) {
            // expected behavior
        }

        list = new PropertyList("foo", new PropertySimple("a", "a"));
        try {
            list.add(new PropertySimple("b", "b"));
            assert false : "addition of mixed-name member to PropertyList succeeded.";
        } catch (IllegalStateException e) {
            // expected behavior
        }
    }
}