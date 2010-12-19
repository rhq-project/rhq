/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.clientapi.agent.configuration;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

@Test
public class ConfigurationUtilityTest {
    public void testCreateDefaultNone() {
        // no defaults, no required props - returned config should be empty
        ConfigurationDefinition configDef = new ConfigurationDefinition("foo", null);

        PropertyDefinitionSimple simple = new PropertyDefinitionSimple("simple", null, false, PropertySimpleType.STRING);
        PropertyDefinitionMap map = new PropertyDefinitionMap("map", null, false, simple);
        PropertyDefinitionList list = new PropertyDefinitionList("list", null, false, simple);

        configDef.put(simple);
        configDef.put(map);
        configDef.put(list);

        Configuration config = ConfigurationUtility.createDefaultConfiguration(configDef);
        assert config != null;
        assert config.getProperties().size() == 0;
    }

    public void testCreateDefaultSimple() {
        ConfigurationDefinition configDef = new ConfigurationDefinition("foo", null);

        PropertyDefinitionSimple simple = new PropertyDefinitionSimple("simple", null, false, PropertySimpleType.STRING);
        PropertyDefinitionSimple simpleRequired = new PropertyDefinitionSimple("simpleRequired", null, true,
            PropertySimpleType.STRING);
        PropertyDefinitionSimple simpleDefault = new PropertyDefinitionSimple("simpleDefault", null, false,
            PropertySimpleType.STRING);
        PropertyDefinitionSimple simpleRequiredDefault = new PropertyDefinitionSimple("simpleRequiredDefault", null,
            true, PropertySimpleType.STRING);

        simpleDefault.setDefaultValue("!!simpleDefaultValue!!");
        simpleRequiredDefault.setDefaultValue("!!simpleRequiredDefaultValue!!");

        configDef.put(simple);
        configDef.put(simpleRequired);
        configDef.put(simpleDefault);
        configDef.put(simpleRequiredDefault);

        Configuration config = ConfigurationUtility.createDefaultConfiguration(configDef);
        assert config != null;
        assert config.getProperties().size() == 3; // simple is not required with no default - its not in the config
        assert config.getSimple(simple.getName()) == null;
        assert config.getSimpleValue(simpleRequired.getName(), null) == null;
        assert config.getSimpleValue(simpleDefault.getName(), null).equals("!!simpleDefaultValue!!");
        assert config.getSimpleValue(simpleRequiredDefault.getName(), null).equals("!!simpleRequiredDefaultValue!!");
    }

    public void testCreateDefaultMap() {
        ConfigurationDefinition configDef = new ConfigurationDefinition("foo", null);

        // a=not required, no default; c=not required, has default; d=required, has default
        PropertyDefinitionSimple a = new PropertyDefinitionSimple("a", null, false, PropertySimpleType.STRING);
        PropertyDefinitionSimple b = new PropertyDefinitionSimple("b", null, false, PropertySimpleType.STRING);
        PropertyDefinitionSimple c = new PropertyDefinitionSimple("c", null, true, PropertySimpleType.STRING);
        b.setDefaultValue("!!bDefaultValue!!");
        c.setDefaultValue("!!cDefaultValue!!");

        PropertyDefinitionMap map1 = new PropertyDefinitionMap("map1", null, false, a, b, c);
        PropertyDefinitionMap mapRequired1 = new PropertyDefinitionMap("mapRequired1", null, true, a, b, c);
        PropertyDefinitionMap mapRequiredDefault1 = new PropertyDefinitionMap("mapRequiredDefault1", null, true, a, b,
            c);

        configDef.put(map1);
        configDef.put(mapRequired1);
        configDef.put(mapRequiredDefault1);

        Configuration config = ConfigurationUtility.createDefaultConfiguration(configDef);
        assert config != null;
        assert config.getProperties().size() == 2; // map is not required with no default - its not in the config
        assert config.getMap(map1.getName()) == null;

        // the two required maps have the same definitions - a,b,c as above. since a isn't required with no default, its not there
        PropertyMap mapProp1 = config.getMap(mapRequired1.getName());
        assert mapProp1 != null;
        assert mapProp1.getSimple(a.getName()) == null;
        assert mapProp1.getSimpleValue(b.getName(), null).equals("!!bDefaultValue!!");
        assert mapProp1.getSimpleValue(c.getName(), null).equals("!!cDefaultValue!!");

        mapProp1 = config.getMap(mapRequiredDefault1.getName());
        assert mapProp1 != null;
        assert mapProp1.getSimple(a.getName()) == null;
        assert mapProp1.getSimpleValue(b.getName(), null).equals("!!bDefaultValue!!");
        assert mapProp1.getSimpleValue(c.getName(), null).equals("!!cDefaultValue!!");
    }

    public void testCreateDefaultList() {
        ConfigurationDefinition configDef = new ConfigurationDefinition("foo", null);

        // a=not required, no default, b=required, has default
        PropertyDefinitionSimple a = new PropertyDefinitionSimple("a", null, false, PropertySimpleType.STRING);
        PropertyDefinitionSimple c = new PropertyDefinitionSimple("c", null, true, PropertySimpleType.STRING);
        c.setDefaultValue("!!cDefaultValue!!");

        PropertyDefinitionList list1 = new PropertyDefinitionList("list1", null, false, a);
        PropertyDefinitionList listRequired1 = new PropertyDefinitionList("listRequired1", null, true, a);
        PropertyDefinitionList listRequiredDefault1 = new PropertyDefinitionList("listRequiredDefault1", null, true, c);

        configDef.put(list1);
        configDef.put(listRequired1);
        configDef.put(listRequiredDefault1);

        Configuration config = ConfigurationUtility.createDefaultConfiguration(configDef);
        assert config != null;
        assert config.getProperties().size() == 2; // list is not required with no default - its not in the config
        assert config.getList(list1.getName()) == null;

        PropertyList listProp1 = config.getList(listRequired1.getName());
        assert listProp1 != null;
        assert listProp1.getList().isEmpty(); // has "a" definition, which is not required and has no default

        listProp1 = config.getList(listRequiredDefault1.getName());
        assert listProp1 != null;
        assert listProp1.getList().get(0).getName().equals(c.getName());
        assert ((PropertySimple) listProp1.getList().get(0)).getStringValue().equals("!!cDefaultValue!!");
    }

    public void testCreateDefaultListOMaps() {
        ConfigurationDefinition configDef = new ConfigurationDefinition("foo", null);

        // a=not required, no default; c=not required, has default; d=required, has default
        PropertyDefinitionSimple a = new PropertyDefinitionSimple("a", null, false, PropertySimpleType.STRING);
        PropertyDefinitionSimple b = new PropertyDefinitionSimple("b", null, false, PropertySimpleType.STRING);
        PropertyDefinitionSimple c = new PropertyDefinitionSimple("c", null, true, PropertySimpleType.STRING);
        b.setDefaultValue("!!bDefaultValue!!");
        c.setDefaultValue("!!cDefaultValue!!");

        PropertyDefinitionMap map2 = new PropertyDefinitionMap("map2", null, false, a, b, c);
        PropertyDefinitionMap mapRequired2 = new PropertyDefinitionMap("mapRequired2", null, true, a, b, c);
        PropertyDefinitionMap mapRequiredDefault2 = new PropertyDefinitionMap("mapRequiredDefault2", null, true, a, b,
            c);

        PropertyDefinitionList listX = new PropertyDefinitionList("listX", null, true, map2);
        PropertyDefinitionList listY = new PropertyDefinitionList("listY", null, true, mapRequired2);
        PropertyDefinitionList listZ = new PropertyDefinitionList("listZ", null, true, mapRequiredDefault2);

        configDef.put(listX);
        configDef.put(listY);
        configDef.put(listZ);

        Configuration config = ConfigurationUtility.createDefaultConfiguration(configDef);

        assert config != null;
        assert config.getProperties().size() == 3;

        PropertyList listPropTest = config.getList(listX.getName());
        assert listPropTest != null;
        assert listPropTest.getList().isEmpty();

        listPropTest = config.getList(listY.getName());
        assert listPropTest != null;
        PropertyMap childMap2 = (PropertyMap) listPropTest.getList().get(0);

        listPropTest = config.getList(listZ.getName());
        assert listPropTest != null;
        PropertyMap childMap3 = (PropertyMap) listPropTest.getList().get(0);

        assert childMap2.getName().equals(mapRequired2.getName());
        assert childMap3.getName().equals(mapRequiredDefault2.getName());
    }

    public void testCreateDefaultAllSimpleMapList() {
        // tests a big config def that has simples, lists, maps - combination of the other individual tests
        ConfigurationDefinition configDef = new ConfigurationDefinition("foo", null);

        // SETUP SIMPLE

        PropertyDefinitionSimple simple = new PropertyDefinitionSimple("simple", null, false, PropertySimpleType.STRING);
        PropertyDefinitionSimple simpleRequired = new PropertyDefinitionSimple("simpleRequired", null, true,
            PropertySimpleType.STRING);
        PropertyDefinitionSimple simpleDefault = new PropertyDefinitionSimple("simpleDefault", null, false,
            PropertySimpleType.STRING);
        PropertyDefinitionSimple simpleRequiredDefault = new PropertyDefinitionSimple("simpleRequiredDefault", null,
            true, PropertySimpleType.STRING);

        simpleDefault.setDefaultValue("!!simpleDefaultValue!!");
        simpleRequiredDefault.setDefaultValue("!!simpleRequiredDefaultValue!!");

        configDef.put(simple);
        configDef.put(simpleRequired);
        configDef.put(simpleDefault);
        configDef.put(simpleRequiredDefault);

        // SETUP MAP

        // a=not required, no default; c=not required, has default; d=required, has default
        PropertyDefinitionSimple a = new PropertyDefinitionSimple("a", null, false, PropertySimpleType.STRING);
        PropertyDefinitionSimple b = new PropertyDefinitionSimple("b", null, false, PropertySimpleType.STRING);
        PropertyDefinitionSimple c = new PropertyDefinitionSimple("c", null, true, PropertySimpleType.STRING);
        b.setDefaultValue("!!bDefaultValue!!");
        c.setDefaultValue("!!cDefaultValue!!");

        PropertyDefinitionMap map1 = new PropertyDefinitionMap("map1", null, false, a, b, c);
        PropertyDefinitionMap mapRequired1 = new PropertyDefinitionMap("mapRequired1", null, true, a, b, c);
        PropertyDefinitionMap mapRequiredDefault1 = new PropertyDefinitionMap("mapRequiredDefault1", null, true, a, b,
            c);

        configDef.put(map1);
        configDef.put(mapRequired1);
        configDef.put(mapRequiredDefault1);

        // SETUP LIST

        PropertyDefinitionList list1 = new PropertyDefinitionList("list1", null, false, a);
        PropertyDefinitionList listRequired1 = new PropertyDefinitionList("listRequired1", null, true, a);
        PropertyDefinitionList listRequiredDefault1 = new PropertyDefinitionList("listRequiredDefault1", null, true, c);

        configDef.put(list1);
        configDef.put(listRequired1);
        configDef.put(listRequiredDefault1);

        // SETUP LIST-O-MAPS

        PropertyDefinitionMap map2 = new PropertyDefinitionMap("map2", null, false, a, b, c);
        PropertyDefinitionMap mapRequired2 = new PropertyDefinitionMap("mapRequired2", null, true, a, b, c);
        PropertyDefinitionMap mapRequiredDefault2 = new PropertyDefinitionMap("mapRequiredDefault2", null, true, a, b,
            c);

        PropertyDefinitionList listX = new PropertyDefinitionList("listX", null, true, map2);
        PropertyDefinitionList listY = new PropertyDefinitionList("listY", null, true, mapRequired2);
        PropertyDefinitionList listZ = new PropertyDefinitionList("listZ", null, true, mapRequiredDefault2);

        configDef.put(listX);
        configDef.put(listY);
        configDef.put(listZ);

        // get the default config
        Configuration config = ConfigurationUtility.createDefaultConfiguration(configDef);
        assert config != null;
        assert config.getProperties().size() == 10;

        // ASSERT SIMPLE

        assert config.getSimple(simple.getName()) == null;
        assert config.getSimpleValue(simpleRequired.getName(), null) == null;
        assert config.getSimpleValue(simpleDefault.getName(), null).equals("!!simpleDefaultValue!!");
        assert config.getSimpleValue(simpleRequiredDefault.getName(), null).equals("!!simpleRequiredDefaultValue!!");

        // ASSERT MAP

        assert config.getMap(map1.getName()) == null;

        // the two required maps have the same definitions - a,b,c as above. since a isn't required with no default, its not there
        PropertyMap mapProp1 = config.getMap(mapRequired1.getName());
        assert mapProp1 != null;
        assert mapProp1.getSimple(a.getName()) == null;
        assert mapProp1.getSimpleValue(b.getName(), null).equals("!!bDefaultValue!!");
        assert mapProp1.getSimpleValue(c.getName(), null).equals("!!cDefaultValue!!");

        mapProp1 = config.getMap(mapRequiredDefault1.getName());
        assert mapProp1 != null;
        assert mapProp1.getSimple(a.getName()) == null;
        assert mapProp1.getSimpleValue(b.getName(), null).equals("!!bDefaultValue!!");
        assert mapProp1.getSimpleValue(c.getName(), null).equals("!!cDefaultValue!!");

        // ASSERT LIST

        assert config.getList(list1.getName()) == null;

        PropertyList listProp1 = config.getList(listRequired1.getName());
        assert listProp1 != null;
        assert listProp1.getList().isEmpty(); // has "a" definition, which is not required and has no default

        listProp1 = config.getList(listRequiredDefault1.getName());
        assert listProp1 != null;
        assert listProp1.getList().get(0).getName().equals(c.getName());
        assert ((PropertySimple) listProp1.getList().get(0)).getStringValue().equals("!!cDefaultValue!!");

        // ASSERT LIST-O-MAPS

        PropertyList listPropTest = config.getList(listX.getName());
        assert listPropTest != null;
        assert listPropTest.getList().isEmpty();

        listPropTest = config.getList(listY.getName());
        assert listPropTest != null;
        PropertyMap childMap2 = (PropertyMap) listPropTest.getList().get(0);

        listPropTest = config.getList(listZ.getName());
        assert listPropTest != null;
        PropertyMap childMap3 = (PropertyMap) listPropTest.getList().get(0);

        assert childMap2.getName().equals(mapRequired2.getName());
        assert childMap3.getName().equals(mapRequiredDefault2.getName());
    }
}
