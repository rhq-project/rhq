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
package org.rhq.enterprise.gui.configuration.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

/**
 * @author Ian Springer
 */
public abstract class TestConfigurationFactory {
    public static ConfigurationDefinition createConfigurationDefinition() {
        ConfigurationDefinition configurationDefinition = new ConfigurationDefinition("TestConfig", "a test config");
        Map<String, PropertyDefinition> propertyDefinitions = new HashMap<String, PropertyDefinition>();
        configurationDefinition.setPropertyDefinitions(propertyDefinitions);

        PropertyDefinitionSimple simplePropDef;

        simplePropDef = createStringPropDef1();
        propertyDefinitions.put(simplePropDef.getName(), simplePropDef);

        simplePropDef = createStringPropDef2();
        propertyDefinitions.put(simplePropDef.getName(), simplePropDef);

        simplePropDef = new PropertyDefinitionSimple("LongString", "a Long String simple prop", false,
            PropertySimpleType.LONG_STRING);
        simplePropDef.setDisplayName(simplePropDef.getName());
        propertyDefinitions.put(simplePropDef.getName(), simplePropDef);

        simplePropDef = new PropertyDefinitionSimple("Password", "a Password simple prop", false,
            PropertySimpleType.PASSWORD);
        simplePropDef.setDisplayName(simplePropDef.getName());
        propertyDefinitions.put(simplePropDef.getName(), simplePropDef);

        simplePropDef = new PropertyDefinitionSimple("Boolean", "a required Boolean simple prop", true,
            PropertySimpleType.BOOLEAN);
        simplePropDef.setDisplayName(simplePropDef.getName());
        propertyDefinitions.put(simplePropDef.getName(), simplePropDef);
        simplePropDef.setRequired(true);

        simplePropDef = createIntegerPropDef();
        propertyDefinitions.put(simplePropDef.getName(), simplePropDef);

        simplePropDef = new PropertyDefinitionSimple("Float", "a Float simple prop", false, PropertySimpleType.FLOAT);
        simplePropDef.setDisplayName(simplePropDef.getName());
        propertyDefinitions.put(simplePropDef.getName(), simplePropDef);

        simplePropDef = new PropertyDefinitionSimple("StringEnum1",
            "a String enum prop with <=5 items - should be rendered as radio buttons", false, PropertySimpleType.STRING);
        simplePropDef.setDisplayName(simplePropDef.getName());
        ArrayList<PropertyDefinitionEnumeration> propDefEnums = new ArrayList<PropertyDefinitionEnumeration>();
        propDefEnums.add(new PropertyDefinitionEnumeration("NY", "NY"));
        propDefEnums.add(new PropertyDefinitionEnumeration("NJ", "NJ", true));
        propDefEnums.add(new PropertyDefinitionEnumeration("PA", "PA"));
        simplePropDef.setEnumeratedValues(propDefEnums, false);
        propertyDefinitions.put(simplePropDef.getName(), simplePropDef);

        simplePropDef = new PropertyDefinitionSimple("StringEnum2",
            "a String enum prop with >5 items - should be rendered as a popup menu", false, PropertySimpleType.STRING);
        simplePropDef.setDisplayName(simplePropDef.getName());
        propDefEnums = new ArrayList<PropertyDefinitionEnumeration>();
        propDefEnums.add(new PropertyDefinitionEnumeration("red", "red"));
        propDefEnums.add(new PropertyDefinitionEnumeration("orange", "orange", true));
        propDefEnums.add(new PropertyDefinitionEnumeration("yellow", "yellow"));
        propDefEnums.add(new PropertyDefinitionEnumeration("green", "green"));
        propDefEnums.add(new PropertyDefinitionEnumeration("blue", "blue"));
        propDefEnums.add(new PropertyDefinitionEnumeration("purple", "purple"));
        simplePropDef.setEnumeratedValues(propDefEnums, false);
        propertyDefinitions.put(simplePropDef.getName(), simplePropDef);

        PropertyDefinitionMap mapPropDef = new PropertyDefinitionMap("MapOfSimples", "a map of simples", false);
        mapPropDef.put(createStringPropDef1());
        mapPropDef.put(createStringPropDef2());
        mapPropDef.put(createIntegerPropDef());
        mapPropDef.setDisplayName(mapPropDef.getName());
        propertyDefinitions.put(mapPropDef.getName(), mapPropDef);

        PropertyDefinitionMap openMapPropDef = new PropertyDefinitionMap("OpenMapOfSimples", "an open map of simples",
            false);
        openMapPropDef.setDisplayName(openMapPropDef.getName());
        propertyDefinitions.put(openMapPropDef.getName(), openMapPropDef);

        PropertyDefinitionMap readOnlyOpenMapPropDef = new PropertyDefinitionMap("ReadOnlyOpenMapOfSimples",
            "a read-only open map of simples", false);
        readOnlyOpenMapPropDef.setDisplayName(readOnlyOpenMapPropDef.getName());
        readOnlyOpenMapPropDef.setReadOnly(true);
        propertyDefinitions.put(readOnlyOpenMapPropDef.getName(), readOnlyOpenMapPropDef);

        PropertyDefinitionList listOfSimplesPropDef = new PropertyDefinitionList("ListOfStrings",
            "another list of Strings", true, new PropertyDefinitionSimple("note", "a note", false,
                PropertySimpleType.STRING));
        listOfSimplesPropDef.setDisplayName(listOfSimplesPropDef.getName());
        propertyDefinitions.put(listOfSimplesPropDef.getName(), listOfSimplesPropDef);

        PropertyDefinitionMap mapInListPropDef = new PropertyDefinitionMap("MapOfSimplesInList", "a map of simples in a list", false);
        mapInListPropDef.put(createStringPropDef1());
        mapInListPropDef.put(createStringPropDef2());
        mapInListPropDef.put(createIntegerPropDef());
        mapInListPropDef.setDisplayName(mapInListPropDef.getName());

        PropertyDefinitionList listPropDef = new PropertyDefinitionList("ListOfMaps", "a list of maps", true,
            mapInListPropDef);
        listPropDef.setDisplayName(listPropDef.getName());
        propertyDefinitions.put(listPropDef.getName(), listPropDef);

        PropertyDefinitionMap mapInReadOnlyListPropDef = new PropertyDefinitionMap("MapOfSimplesInReadOnlyList", "a map of simples in a list", false);
        mapInReadOnlyListPropDef.put(createStringPropDef1());
        mapInReadOnlyListPropDef.put(createStringPropDef2());
        mapInReadOnlyListPropDef.put(createIntegerPropDef());
        mapInReadOnlyListPropDef.setDisplayName(mapInReadOnlyListPropDef.getName());

        PropertyDefinitionList readOnlyListPropDef = new PropertyDefinitionList("ReadOnlyListOfMaps",
            "a read-only list of maps", true, mapInReadOnlyListPropDef);
        readOnlyListPropDef.setDisplayName(readOnlyListPropDef.getName());
        readOnlyListPropDef.setReadOnly(true);
        propertyDefinitions.put(readOnlyListPropDef.getName(), readOnlyListPropDef);

        PropertyGroupDefinition propertyGroupDefinition = new PropertyGroupDefinition("myGroup");
        propertyGroupDefinition.setDisplayName(propertyGroupDefinition.getName());
        propertyGroupDefinition.setDescription("this is an example group");

        PropertyDefinitionSimple myString = new PropertyDefinitionSimple("myString1", "my little string", true,
            PropertySimpleType.STRING);
        myString.setDisplayName(myString.getName());
        myString.setSummary(true);
        propertyDefinitions.put(myString.getName(), myString);
        myString.setPropertyGroupDefinition(propertyGroupDefinition);

        PropertyDefinitionSimple myString2 = new PropertyDefinitionSimple("myString2", "my other little string", true,
            PropertySimpleType.STRING);
        myString2.setDisplayName(myString2.getName());
        myString2.setSummary(true);
        propertyDefinitions.put(myString2.getName(), myString2);
        myString2.setPropertyGroupDefinition(propertyGroupDefinition);

        PropertyGroupDefinition propertyGroupDefinition2 = new PropertyGroupDefinition("myGroup2");
        propertyGroupDefinition2.setDisplayName(propertyGroupDefinition2.getName());
        propertyGroupDefinition2.setDescription("this is another example group");

        PropertyDefinitionSimple myString3 = new PropertyDefinitionSimple("myString3", "my third string", true,
            PropertySimpleType.STRING);
        myString3.setDisplayName((myString3.getName()));
        myString3.setSummary(true);
        propertyDefinitions.put(myString3.getName(), myString3);
        myString3.setPropertyGroupDefinition(propertyGroupDefinition2);

        PropertyDefinitionSimple enumExample = new PropertyDefinitionSimple("myEnum",
            "a grouped enum prop with <=5 items", false, PropertySimpleType.STRING);
        enumExample.setDisplayName(enumExample.getName());
        ArrayList<PropertyDefinitionEnumeration> myEnums = new ArrayList<PropertyDefinitionEnumeration>();
        myEnums.add(new PropertyDefinitionEnumeration("Burlington", "Burlington"));
        myEnums.add(new PropertyDefinitionEnumeration("Camden", "Camden", true));
        myEnums.add(new PropertyDefinitionEnumeration("Gloucester", "Gloucester"));
        enumExample.setEnumeratedValues(myEnums, false);
        propertyDefinitions.put(enumExample.getName(), enumExample);
        enumExample.setPropertyGroupDefinition(propertyGroupDefinition2);

        return configurationDefinition;
    }

    public static Configuration createConfiguration() {
        Configuration configuration = new Configuration();
        configuration.setNotes("a test config");
        configuration.setVersion(1);

        configuration.put(new PropertySimple("String1", "blah"));
        configuration.put(new PropertySimple("String2",
            "a really, really, really, really, really long value that won't fit in the text input box"));
        configuration.put(new PropertySimple("LongString", "blah blah blah\nblah blah blah"));
        configuration.put(new PropertySimple("Password", null));
        configuration.put(new PropertySimple("Boolean", false));
        configuration.put(new PropertySimple("Integer", 666));
        configuration.put(new PropertySimple("Float", Math.PI));

        configuration.put(new PropertySimple("StringEnum1", "PA"));
        configuration.put(new PropertySimple("StringEnum2", "blue"));

        PropertyMap propMap1 = new PropertyMap("MapOfSimples");
        propMap1.put(new PropertySimple("String1", "One"));
        propMap1.put(new PropertySimple("String2", "Two"));
        propMap1.put(new PropertySimple("Integer", 11));
        configuration.put(propMap1);

        PropertyMap openPropMap1 = new PropertyMap("OpenMapOfSimples");
        openPropMap1.put(new PropertySimple("PROCESSOR_ARCHITECTURE", "x86"));
        openPropMap1.put(new PropertySimple("PROCESSOR_IDENTIFIER", "x86 Family 6 Model 15 Stepping 6, GenuineIntel"));
        openPropMap1.put(new PropertySimple("PROCESSOR_LEVEL", "6"));
        openPropMap1.put(new PropertySimple("PROCESSOR_REVISION", "0f06"));
        configuration.put(openPropMap1);

        PropertyMap openPropMap2 = new PropertyMap("ReadOnlyOpenMapOfSimples");
        openPropMap2.put(new PropertySimple("ANT_HOME", "C:\\opt\\ant-1.6.5"));
        openPropMap2.put(new PropertySimple("ANT_OPTS", "-Xms128M -Xmx256M"));
        configuration.put(openPropMap2);

        configuration.put(new PropertyList("ListOfStrings", new PropertySimple("note", "Do"), new PropertySimple(
            "note", "Re"), new PropertySimple("note", "Mi"), new PropertySimple("note", "Fa"), new PropertySimple(
            "note", "So"), new PropertySimple("note", "La"), new PropertySimple("note", "Ti")));

        PropertyMap propMap2 = new PropertyMap("MapOfSimples");
        propMap2.put(new PropertySimple("String1", "Uno"));
        propMap2.put(new PropertySimple("String2", "Dos"));
        propMap2.put(new PropertySimple("Integer", Integer.MIN_VALUE));
        PropertyMap propMap3 = new PropertyMap("MapOfSimples");
        propMap3.put(new PropertySimple("String1", "Un"));
        propMap3.put(new PropertySimple("String2", "Deux"));
        propMap3.put(new PropertySimple("Integer", Integer.MAX_VALUE));
        configuration.put(new PropertyList("ListOfMaps", propMap2, propMap3));

        PropertyMap propMap4 = new PropertyMap("MapOfSimples");
        propMap4.put(new PropertySimple("String1", "A"));
        propMap4.put(new PropertySimple("String2", "B"));
        propMap4.put(new PropertySimple("Integer", 999));
        PropertyMap propMap5 = new PropertyMap("MapOfSimples");
        propMap5.put(new PropertySimple("String1", "a"));
        propMap5.put(new PropertySimple("String2", "b"));
        propMap5.put(new PropertySimple("Integer", 0));
        configuration.put(new PropertyList("ReadOnlyListOfMaps", propMap4, propMap5));

        configuration.put(new PropertySimple("myString1", "grouped String 1"));
        configuration.put(new PropertySimple("myString2", "grouped String 2"));
        configuration.put(new PropertySimple("myString3", "strings are cool"));
        configuration.put(new PropertySimple("myEnum", "Burlington"));

        return configuration;
    }

    private static PropertyDefinitionSimple createStringPropDef1()
    {
        PropertyDefinitionSimple stringPropDef1;
        stringPropDef1 = new PropertyDefinitionSimple("String1",
            "an optional String simple prop", false, PropertySimpleType.STRING);
        stringPropDef1.setDisplayName(stringPropDef1.getName());
        return stringPropDef1;
    }

    private static PropertyDefinitionSimple createStringPropDef2()
    {
        PropertyDefinitionSimple stringPropDef2;
        stringPropDef2 = new PropertyDefinitionSimple("String2",
            "a read-only String simple prop", false, PropertySimpleType.STRING);
        stringPropDef2.setDisplayName(stringPropDef2.getName());
        stringPropDef2.setReadOnly(true);
        return stringPropDef2;
    }

    private static PropertyDefinitionSimple createIntegerPropDef()
    {
        PropertyDefinitionSimple integerPropDef;
        integerPropDef = new PropertyDefinitionSimple("Integer",
            "a required summary Integer simple prop", true, PropertySimpleType.INTEGER);
        integerPropDef.setDisplayName(integerPropDef.getName());
        integerPropDef.setSummary(true);
        return integerPropDef;
    }    
}