/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.util.StringUtils;
import org.rhq.enterprise.server.rest.helper.ConfigurationHelper;

/**
 * Test the ConfigurationHelper class
 * @author Heiko W. Rupp
 */
public class ConfigurationHelperTest {

    @Test
    public void testConvertSimpleMap() throws Exception {

        Map<String,Object> map = new HashMap<String, Object>(2);
        map.put("Hello","World");
        map.put("Answer",42);

        Configuration config = ConfigurationHelper.mapToConfiguration(map);

        assert config!=null;
        Collection<Property> properties = config.getProperties();
        assert properties.size()==2;

        PropertySimple hello = config.getSimple("Hello");
        assert hello !=null;
        assert hello.getStringValue().equals("World");

        PropertySimple qu = config.getSimple("Answer");
        assert qu!=null;
        Integer value = qu.getIntegerValue();
        assert value !=null;
        assert value ==42;
    }

    @Test
    public void testConvertWithNestedMap() throws Exception {

        Map<String,Object> map = new HashMap<String, Object>(3);
        map.put("Hello","World");
        map.put("Answer",42);
        Map<String,Object> inner = new HashMap<String, Object>(1);
        inner.put("Foo","Bar");
        map.put("Inner",inner);

        Configuration config = ConfigurationHelper.mapToConfiguration(map);

        assert config!=null;
        Collection<Property> properties = config.getProperties();
        assert properties.size()==3 : "Expected 3 props, got " + properties.size();

        Property prop = config.get("Inner");
        assert prop!=null;
        assert prop instanceof PropertyMap : "Inner is no map";

        PropertyMap pm = (PropertyMap) prop;
        Map<String, Property> innerMap = pm.getMap();
        assert innerMap.size()==1;

        assert inner.containsKey("Foo");

    }

    @Test
    public void testConvertListOfMap() throws Exception {

        Map<String,Object> map = new HashMap<String, Object>(2);
        map.put("Hello","World");
        List<Map<String,Object>> list = new ArrayList<Map<String, Object>>();
        map.put("list",list);

        Map<String,Object> inner = new HashMap<String, Object>(1);
        inner.put("Foo","Bar");
        list.add(inner);

        Configuration config = ConfigurationHelper.mapToConfiguration(map);

        assert config!=null;
        Collection<Property> properties = config.getProperties();
        assert properties.size()==2 : "Expected 2 props, got " + properties.size();

        Property prop = config.get("Inner");
        assert prop==null;

        prop = config.get("list");
        assert prop instanceof PropertyList : "list is no list";

        PropertyList pl = (PropertyList) prop;
        List<Property> propertyList = pl.getList();
        assert propertyList.size()==1;
        PropertyMap innerMapProperty = (PropertyMap) propertyList.get(0);

        Map<String, Property> propertyMap = innerMapProperty.getMap();
        assert propertyMap.size()==1;
        Map<String,Property> innerMap = propertyMap;

        assert innerMap.containsKey("Foo");
        Property property = innerMapProperty.get("Foo");
        assert property != null;
        assert property instanceof PropertySimple;
        PropertySimple ps = (PropertySimple) property;
        assert ps.getStringValue().equals("Bar");

    }

    @Test
    public void testConvertWithListOfSimple() throws Exception {

        Map<String,Object> map = new HashMap<String, Object>(2);
        map.put("Hello","World");
        map.put("Answer",42);
        List<String> inner = new ArrayList<String>(2);
        inner.add("Foo");
        inner.add("Bar");
        map.put("Inner", inner);

        Configuration config = ConfigurationHelper.mapToConfiguration(map);

        assert config!=null;
        Collection<Property> properties = config.getProperties();
        assert properties.size()==3 : "Expected 3 props, got " + properties.size();

        Property prop = config.get("Inner");
        assert prop!=null;
        assert prop instanceof PropertyList : "Inner is no list";

        PropertyList plist = (PropertyList) prop;
        List<Property> propertyList = plist.getList();
        assert propertyList.size()==2;
        for (Property innerProp : propertyList) {
            assert innerProp instanceof PropertySimple;
            PropertySimple ps = (PropertySimple) innerProp;
            assert ps.getName().equals("Inner");
            assert ps.getStringValue().equals("Foo") || ps.getStringValue().equals("Bar");
        }

    }

    @Test
    public void testConvertAndValidateBoolean() throws Exception {

        Map<String,Object> map = new HashMap<String, Object>();
        map.put("bool1","true");
        map.put("bool2","TruE");
        map.put("bool3","fAlSe");
        map.put("bool4", "false");
        map.put("bool5", 42);
        map.put("bool6", "Hugo");
        map.put("bool7",null);

        Configuration config = ConfigurationHelper.mapToConfiguration(map);

        assert config!=null;
        Collection<Property> properties = config.getProperties();
        assert properties.size()==7 : "Expected 7 props but got " + properties.size();

        ConfigurationDefinition definition = new ConfigurationDefinition("bla","For testing");
        definition.put(new PropertyDefinitionSimple("bool1","A boolean",true, PropertySimpleType.BOOLEAN));
        definition.put(new PropertyDefinitionSimple("bool2","A boolean",true, PropertySimpleType.BOOLEAN));
        definition.put(new PropertyDefinitionSimple("bool3","A boolean",true, PropertySimpleType.BOOLEAN));
        definition.put(new PropertyDefinitionSimple("bool4","A boolean",true, PropertySimpleType.BOOLEAN));
        definition.put(new PropertyDefinitionSimple("bool5","A boolean",true, PropertySimpleType.BOOLEAN));
        definition.put(new PropertyDefinitionSimple("bool6","A boolean",true, PropertySimpleType.BOOLEAN));
        definition.put(new PropertyDefinitionSimple("bool7","A boolean",true, PropertySimpleType.BOOLEAN));

        List<String> errors = ConfigurationHelper.checkConfigurationWrtDefinition(config,definition);

        assert errors!=null;
        assert errors.size()==3 : "Expected 3 errors, but got " + errors.size() + "\n" + StringUtils.getListAsString(errors,",\n ");

    }

    @Test
    public void testConvertAndValidateMissingRequired() throws Exception {

        Map<String,Object> map = new HashMap<String, Object>(1);
        map.put("bool1","true");
        // required 2nd entry is missing

        Configuration config = ConfigurationHelper.mapToConfiguration(map);

        assert config!=null;
        Collection<Property> properties = config.getProperties();
        assert properties.size()==1 : "Expected 1 props but got " + properties.size();

        ConfigurationDefinition definition = new ConfigurationDefinition("bla","For testing");
        definition.put(new PropertyDefinitionSimple("bool1","A boolean",true, PropertySimpleType.BOOLEAN));
        definition.put(new PropertyDefinitionSimple("bool2","A boolean",true, PropertySimpleType.BOOLEAN));

        List<String> errors = ConfigurationHelper.checkConfigurationWrtDefinition(config,definition);

        assert errors!=null;
        assert errors.size()==1 : "Expected 1 error, but got " + errors.size() + "\n" + StringUtils.getListAsString(errors,",\n ");

    }

    @Test
    public void testConvertAndValidateNotRequiredButNull() throws Exception {

        Map<String,Object> map = new HashMap<String, Object>(1);
        map.put("bool1","true");
        map.put("optional",null);

        Configuration config = ConfigurationHelper.mapToConfiguration(map);

        assert config!=null;
        Collection<Property> properties = config.getProperties();
        assert properties.size()==2 : "Expected 2 props but got " + properties.size();

        ConfigurationDefinition definition = new ConfigurationDefinition("bla","For testing");
        definition.put(new PropertyDefinitionSimple("bool1","A boolean",true, PropertySimpleType.BOOLEAN));
        definition.put(new PropertyDefinitionSimple("optional","null string",false, PropertySimpleType.STRING));

        List<String> errors = ConfigurationHelper.checkConfigurationWrtDefinition(config,definition);

        assert errors!=null;
        assert errors.size()==0 : "Expected 0 error, but got " + errors.size() + "\n" + StringUtils.getListAsString(errors,",\n ");

    }

    @Test
    public void testConvertAndValidateMismatchingKind() throws Exception {

        Map<String,Object> map = new HashMap<String, Object>(1);
        map.put("bool1","true");
        map.put("optional",null);
        Map<String,Object> inner = new HashMap<String, Object>(1);
        inner.put("Foo","Bar");
        map.put("Inner",inner);

        Configuration config = ConfigurationHelper.mapToConfiguration(map);

        assert config!=null;
        Collection<Property> properties = config.getProperties();
        assert properties.size()==3 : "Expected 3 props but got " + properties.size();

        ConfigurationDefinition definition = new ConfigurationDefinition("bla","For testing");
        definition.put(new PropertyDefinitionSimple("bool1","A boolean",true, PropertySimpleType.BOOLEAN));
        definition.put(new PropertyDefinitionSimple("optional","null string",false, PropertySimpleType.STRING));
        // We define Inner as a simple property, but the user supplied a map
        definition.put(new PropertyDefinitionSimple("Inner","null string",false, PropertySimpleType.STRING));

        List<String> errors = ConfigurationHelper.checkConfigurationWrtDefinition(config,definition);

        assert errors!=null;
        assert errors.size()==1 : "Expected 1 error, but got " + errors.size() + "\n" + StringUtils.getListAsString(errors,",\n ");

    }

    @Test
    public void testConvertAndValidateMismatchingKind2() throws Exception {

        Map<String,Object> map = new HashMap<String, Object>(1);
        map.put("bool1","true");
        map.put("optional",null);
        map.put("Inner","Frobnitz");

        Configuration config = ConfigurationHelper.mapToConfiguration(map);

        assert config!=null;
        Collection<Property> properties = config.getProperties();
        assert properties.size()==3 : "Expected 3 props but got " + properties.size();

        ConfigurationDefinition definition = new ConfigurationDefinition("bla","For testing");
        definition.put(new PropertyDefinitionSimple("bool1","A boolean",true, PropertySimpleType.BOOLEAN));
        definition.put(new PropertyDefinitionSimple("optional","null string",false, PropertySimpleType.STRING));
        // We define Inner as a list property, but the user supplied a simple one
        definition.put(new PropertyDefinitionList("Inner","Bla",true,
            new PropertyDefinitionSimple("Inner","fasel",true,PropertySimpleType.STRING)));

        List<String> errors = ConfigurationHelper.checkConfigurationWrtDefinition(config,definition);

        assert errors!=null;
        assert errors.size()==1 : "Expected 1 error, but got " + errors.size() + "\n" + StringUtils.getListAsString(errors,",\n ");

    }

    @Test
    public void testConvertAndValidateNumeric() throws Exception {

        Map<String,Object> map = new HashMap<String, Object>(2);
        map.put("float1",1.1f);
        map.put("float2",Double.MAX_VALUE);
        map.put("float3",null);
        map.put("float4","abc");

        map.put("double1",1.1d);
        map.put("double2",null);
        map.put("double3","hugo");

        map.put("int1",42);
        map.put("int2",Long.MAX_VALUE);
        map.put("int3",null);
        map.put("int4","abc");

        map.put("long1",-5);
        map.put("long2",null);
        map.put("long3","Frobnitz");

        Configuration config = ConfigurationHelper.mapToConfiguration(map);

        assert config!=null;
        Collection<Property> properties = config.getProperties();
        assert properties.size()==14 : "Expected 14 props but got " + properties.size();

        ConfigurationDefinition definition = new ConfigurationDefinition("bla","For testing");
        definition.put(new PropertyDefinitionSimple("float1","A float",true, PropertySimpleType.FLOAT));
        definition.put(new PropertyDefinitionSimple("float2","A float",true, PropertySimpleType.FLOAT));
        definition.put(new PropertyDefinitionSimple("float3","A float",true, PropertySimpleType.FLOAT));
        definition.put(new PropertyDefinitionSimple("float4","A float",true, PropertySimpleType.FLOAT));

        definition.put(new PropertyDefinitionSimple("double1","A double",true, PropertySimpleType.DOUBLE));
        definition.put(new PropertyDefinitionSimple("double2","A double",true, PropertySimpleType.DOUBLE));
        definition.put(new PropertyDefinitionSimple("double3","A double",true, PropertySimpleType.DOUBLE));

        definition.put(new PropertyDefinitionSimple("int1","An int",true, PropertySimpleType.INTEGER));
        definition.put(new PropertyDefinitionSimple("int2","An int",true, PropertySimpleType.INTEGER));
        definition.put(new PropertyDefinitionSimple("int3","An int",true, PropertySimpleType.INTEGER));
        definition.put(new PropertyDefinitionSimple("int4","An int",true, PropertySimpleType.INTEGER));

        definition.put(new PropertyDefinitionSimple("long1","A long",true, PropertySimpleType.LONG));
        definition.put(new PropertyDefinitionSimple("long2","A long",true, PropertySimpleType.LONG));
        definition.put(new PropertyDefinitionSimple("long3","A long",true, PropertySimpleType.LONG));

        List<String> errors = ConfigurationHelper.checkConfigurationWrtDefinition(config,definition);

        assert errors!=null;
        assert errors.size()==10 : "Expected 10 errors, but got " + errors.size() + "\n" + StringUtils.getListAsString(errors,",\n");

    }

    @Test
    public void testValidateNullConfiguration() throws Exception {

        List<String> errors = ConfigurationHelper.checkConfigurationWrtDefinition(null,new ConfigurationDefinition("bla",null));
        assert errors != null;
        assert errors.size()==1;

    }

    @Test
    public void testValidateNullDefinition() throws Exception {

        List<String> errors = ConfigurationHelper.checkConfigurationWrtDefinition(new Configuration(),null);
        assert errors != null;
        assert errors.size()==1;

    }

    @Test
    public void testConvertAndValidateSimpleMap() throws Exception {

        Map<String,Object> map = new HashMap<String, Object>(2);
        map.put("Hello","World");
        map.put("Answer",42);

        Configuration config = ConfigurationHelper.mapToConfiguration(map);

        ConfigurationDefinition definition = new ConfigurationDefinition("bla",null);
        definition.put(new PropertyDefinitionSimple("Hello",null,true,PropertySimpleType.STRING));
        definition.put(new PropertyDefinitionSimple("Answer",null,true,PropertySimpleType.INTEGER));
        definition.put(new PropertyDefinitionSimple("Optional",null,false,PropertySimpleType.INTEGER));

        assert config!=null;
        Collection<Property> properties = config.getProperties();
        assert properties.size()==2;

        PropertySimple hello = config.getSimple("Hello");
        assert hello !=null;
        assert hello.getStringValue().equals("World");

        PropertySimple qu = config.getSimple("Answer");
        assert qu!=null;
        Integer integerValue = qu.getIntegerValue();
        assert integerValue !=null;
        assert integerValue ==42;
    }

    @Test
    public void testConvertValidateNonRequiredNotPresent() throws Exception {

        Map<String,Object> map = new HashMap<String, Object>(2);

        Configuration config = ConfigurationHelper.mapToConfiguration(map);

        assert config!=null;
        Collection<Property> properties = config.getProperties();
        assert properties.size()==0 : "Expected 0 props, got " + properties.size();

        ConfigurationDefinition definition = new ConfigurationDefinition("bla",null);
        definition.put(new PropertyDefinitionSimple("Answer",null,false,PropertySimpleType.INTEGER));

        List<String> errors = ConfigurationHelper.checkConfigurationWrtDefinition(config,definition);

        assert errors!=null;
        assert errors.size()==0 : "Expected 0 errors, but got " + errors.size() + "\n" + StringUtils.getListAsString(errors,",\n");

    }

    @Test
    public void testConvertAndValidateListOfMap() throws Exception {

        Map<String,Object> map = new HashMap<String, Object>(2);
        map.put("Hello","World");
        List<Map<String,Object>> list = new ArrayList<Map<String, Object>>();
        map.put("list",list);

        Map<String,Object> inner = new HashMap<String, Object>(1);
        inner.put("Foo","Bar");
        list.add(inner);

        Configuration config = ConfigurationHelper.mapToConfiguration(map);

        assert config!=null;
        Collection<Property> properties = config.getProperties();
        assert properties.size()==2 : "Expected 2 props, got " + properties.size();


        ConfigurationDefinition definition = new ConfigurationDefinition("bla",null);
        definition.put(new PropertyDefinitionSimple("Hello",null,true,PropertySimpleType.STRING));
        definition.put(new PropertyDefinitionSimple("Answer",null,false,PropertySimpleType.INTEGER));
        definition.put(new PropertyDefinitionList("list",null,true,
                new PropertyDefinitionMap("list",null,true,
                    new PropertyDefinitionSimple("list",null,true,PropertySimpleType.STRING))));
        definition.put(new PropertyDefinitionSimple("aString",null,false,PropertySimpleType.INTEGER));


        List<String> errors = ConfigurationHelper.checkConfigurationWrtDefinition(config,definition);

        assert errors!=null;
        assert errors.size()==0 : "Expected 0 errors, but got " + errors.size() + "\n" + StringUtils.getListAsString(errors,",\n");

    }

    @Test
    public void testConvertAndValidateMapOfMap() throws Exception {

        Map<String,Object> map = new HashMap<String, Object>(2);
        map.put("Hello","World");
        Map<String,Map<String,Object>> list = new HashMap<String, Map<String, Object>>();
        map.put("list",list);

        Map<String,Object> inner = new HashMap<String, Object>(1);
        inner.put("Foo","Bar");
        list.put("outer", inner);

        Configuration config = ConfigurationHelper.mapToConfiguration(map);

        assert config!=null;
        Collection<Property> properties = config.getProperties();
        assert properties.size()==2 : "Expected 2 props, got " + properties.size();


        ConfigurationDefinition definition = new ConfigurationDefinition("bla",null);
        definition.put(new PropertyDefinitionSimple("Hello",null,true,PropertySimpleType.STRING));
        definition.put(new PropertyDefinitionMap("list",null,true,
                new PropertyDefinitionMap("list",null,true,
                    new PropertyDefinitionSimple("list",null,true,PropertySimpleType.STRING))));


        List<String> errors = ConfigurationHelper.checkConfigurationWrtDefinition(config,definition);

        assert errors!=null;
        assert errors.size()==0 : "Expected 0 errors, but got " + errors.size() + "\n" + StringUtils.getListAsString(errors,",\n");

    }

    @Test
    public void testConvertAndValidateMapWithListOfSimple() throws Exception {

        Map<String,Object> map = new HashMap<String, Object>(2);
        map.put("Hello","World");

        Map<String,List<String>> list = new HashMap<String, List<String>>();
        map.put("list",list);

        List<String> inner = new ArrayList<String>();
        inner.add("Foo");
        inner.add("Bar");
        list.put("outer", inner);

        Configuration config = ConfigurationHelper.mapToConfiguration(map);

        assert config!=null;
        Collection<Property> properties = config.getProperties();
        assert properties.size()==2 : "Expected 2 props, got " + properties.size();


        ConfigurationDefinition definition = new ConfigurationDefinition("bla",null);
        definition.put(new PropertyDefinitionSimple("Hello",null,true,PropertySimpleType.STRING));
        definition.put(new PropertyDefinitionMap("list",null,true,
                new PropertyDefinitionList("list",null,true,
                    new PropertyDefinitionSimple("list",null,true,PropertySimpleType.STRING))));


        List<String> errors = ConfigurationHelper.checkConfigurationWrtDefinition(config,definition);

        assert errors!=null;
        assert errors.size()==0 : "Expected 0 errors, but got " + errors.size() + "\n" + StringUtils.getListAsString(errors,",\n");

    }

    @Test
    public void testConfigToMapSimple() throws Exception {

        Configuration config = new Configuration();
        config.put(new PropertySimple("number",42));
        config.put(new PropertySimple("string","Hello"));
        config.put(new PropertySimple("bool",true));
        config.put(new PropertySimple("float",1.1f));
        config.put(new PropertySimple("double",2.3d));
        config.put(new PropertySimple("long",Long.MAX_VALUE));


        ConfigurationDefinition definition = new ConfigurationDefinition("bla",null);
        definition.put(new PropertyDefinitionSimple("number",null,false,PropertySimpleType.INTEGER));
        definition.put(new PropertyDefinitionSimple("string",null,false,PropertySimpleType.STRING));
        definition.put(new PropertyDefinitionSimple("bool",null,false,PropertySimpleType.BOOLEAN));
        definition.put(new PropertyDefinitionSimple("float",null,false,PropertySimpleType.FLOAT));
        definition.put(new PropertyDefinitionSimple("double",null,false,PropertySimpleType.DOUBLE));
        definition.put(new PropertyDefinitionSimple("long",null,false,PropertySimpleType.LONG));

        Map<String,Object> map = ConfigurationHelper.configurationToMap(config,definition, true);

        assert map != null;
        assert map.entrySet().size()==6;

        assert map.containsKey("number");
        assert map.containsKey("string");

        assert map.get("number") != null;
        assert (Integer)map.get("number") == 42;

        assert map.get("string") != null;
        assert map.get("string").equals("Hello");

        assert (Boolean)map.get("bool") == true;

        assert map.get("float") != null;
        assert (Float)map.get("float") ==1.1f;

        assert map.get("double") != null;
        assert (Double) map.get("double") ==2.3d;

        assert map.get("long") != null;
        assert (Long)map.get("long") == Long.MAX_VALUE;

    }

    @Test
    public void testEmptyConfigToMap() throws Exception {

        Configuration config = new Configuration();

        ConfigurationDefinition definition = new ConfigurationDefinition("bla",null);
        definition.put(new PropertyDefinitionSimple("number",null,false,PropertySimpleType.INTEGER));
        definition.put(new PropertyDefinitionSimple("string",null,false,PropertySimpleType.STRING));

        Map<String,Object> map = ConfigurationHelper.configurationToMap(config,definition, true);

        assert map != null;
        assert map.entrySet().size()==0;


    }

    @Test
    public void testNullConfigToMap() throws Exception {

        Configuration config = null;

        ConfigurationDefinition definition = new ConfigurationDefinition("bla",null);
        definition.put(new PropertyDefinitionSimple("number",null,false,PropertySimpleType.INTEGER));
        definition.put(new PropertyDefinitionSimple("string",null,false,PropertySimpleType.STRING));

        Map<String,Object> map = ConfigurationHelper.configurationToMap(config,definition, true);

        assert map != null;
        assert map.entrySet().size()==0;

    }

    @Test
    public void testConfigToMapComplexList() throws Exception {

        Configuration config = new Configuration();
        PropertyList propertyList = new PropertyList("aList");
        propertyList.add(new PropertySimple("string", "Hello"));
        propertyList.add(new PropertySimple("string", "World"));
        config.put(propertyList);


        ConfigurationDefinition definition = new ConfigurationDefinition("bla",null);
        definition.put(new PropertyDefinitionList("aList",null,false,
            new PropertyDefinitionSimple("string",null,false,PropertySimpleType.STRING)));

        Map<String,Object> map = ConfigurationHelper.configurationToMap(config,definition, true);

        assert map != null;
        assert map.entrySet().size()==1;

        assert map.containsKey("aList");

    }

    @Test(expectedExceptions={IllegalArgumentException.class})
    public void testConfigToMapNoDefinitionStrict() throws Exception {
        ConfigurationHelper.configurationToMap(Configuration.builder().addSimple("test", "test").build(), null, true);
    }

    @Test()
    public void testConfigToMapNoDefinition() throws Exception {
        Configuration config = Configuration.builder()
            .openList("list1", "map")
                .openMap()
                    .addSimple("val1", "true")
                    .addSimple("val2", "123")
                .closeMap()
            .closeList()
            .addSimple("simple1", "simple1")
            .openMap("map2")
                .addSimple("val3", "FOO")
             .closeMap()
            .build();

        Map<String, Object> map = ConfigurationHelper.configurationToMap(config, null, false);
        assert map != null;
        assert map.size() == 3;
        assert map.get("list1") instanceof List;
        List list1 = (List) map.get("list1");
        assert list1.size() == 1;
        assert list1.get(0) instanceof Map;
        Map<String, Object> map1 = (Map<String, Object>) list1.get(0);
        assert map1.get("val1") instanceof String;
        assert "true".equals(map1.get("val1"));
        assert map1.get("val2") instanceof String;
        assert "123".equals(map1.get("val2"));
        assert map.get("simple1") instanceof String;
        assert "simple1".equals(map.get("simple1"));
        assert map.get("map2") instanceof Map;
        Map<String, Object> map2 = (Map<String, Object>) map.get("map2");
        assert map2.size() == 1;
        assert map2.get("val3") instanceof String;
        assert "FOO".equals(map2.get("val3"));
    }

    @Test
    public void testConfigToMapComplexMap() throws Exception {

        Configuration config = new Configuration();
        PropertyMap propertyMap = new PropertyMap("aMap");
        config.put(propertyMap);
        PropertyList propertyList = new PropertyList("aList");
        propertyList.add(new PropertySimple("string", "Hello"));
        propertyList.add(new PropertySimple("string", "World"));
        propertyMap.put(propertyList);

        propertyMap.put(new PropertySimple("aString","Frobnitz"));


        ConfigurationDefinition definition = new ConfigurationDefinition("bla",null);
        definition.put(new PropertyDefinitionMap("aMap",null,false,
            new PropertyDefinitionList("aList",null,false,
                new PropertyDefinitionSimple("string",null,false,PropertySimpleType.STRING)),
            new PropertyDefinitionSimple("aString",null,false,PropertySimpleType.STRING)));

        Map<String,Object> map = ConfigurationHelper.configurationToMap(config,definition, true);

        assert map != null;
        assert map.entrySet().size()==1;

        assert map.containsKey("aMap");
        assert map.get("aMap") instanceof Map;
        Map<String,Object> innerMap = (Map<String, Object>) map.get("aMap");

        assert innerMap.containsKey("aString");
        assert innerMap.containsKey("aList");

    }

    @Test
    public void testConfigToMapComplexMapWithBadSetupStrict() throws Exception {

        Configuration config = new Configuration();
        PropertyMap propertyMap = new PropertyMap("aMap");
        config.put(propertyMap);
        PropertyList propertyList = new PropertyList("aList");
        propertyList.add(new PropertySimple("string", "Hello"));
        propertyList.add(new PropertySimple("string", "World"));
        propertyMap.put(propertyList);

        propertyMap.put(new PropertySimple("aString","Frobnitz"));


        ConfigurationDefinition definition = new ConfigurationDefinition("bla",null);
        definition.put(new PropertyDefinitionMap("aMap",null,false,
            new PropertyDefinitionList("aBla",null,false,
                new PropertyDefinitionSimple("string",null,false,PropertySimpleType.STRING)),
            new PropertyDefinitionSimple("aFoo",null,false,PropertySimpleType.STRING)));

        try {
            ConfigurationHelper.configurationToMap(config,definition, true);
            assert false;
        } catch (IllegalArgumentException iae ) {
            System.out.println("Yep, caught the error");
        }


    }

    @Test(enabled = false)
    public void testConfigToMapComplexMapWithBadSetupLenient() throws Exception {

        Configuration config = new Configuration();
        PropertyMap propertyMap = new PropertyMap("aMap");
        config.put(propertyMap);
        PropertyList propertyList = new PropertyList("aList");
        propertyList.add(new PropertySimple("string", "Hello"));
        propertyList.add(new PropertySimple("string", "World"));
        propertyMap.put(propertyList);

        propertyMap.put(new PropertySimple("aString","Frobnitz"));


        ConfigurationDefinition definition = new ConfigurationDefinition("bla",null);
        definition.put(new PropertyDefinitionMap("aMap",null,false,
            new PropertyDefinitionList("aBla",null,false,
                new PropertyDefinitionSimple("string",null,false,PropertySimpleType.STRING)),
            new PropertyDefinitionSimple("aFoo",null,false,PropertySimpleType.STRING)));

        Map<String,Object> map = ConfigurationHelper.configurationToMap(config,definition, false);

        assert map != null;
        assert map.entrySet().size()==1;

        assert map.containsKey("aMap");

    }

    @Test
    public void testConvertSingleValueNoProperty() throws Exception {

        Object o = ConfigurationHelper.convertSimplePropertyValue(null, new PropertyDefinitionSimple("dummy", null,
            false, PropertySimpleType.STRING), true);

        assert o == null;

    }

    @Test
    public void testConvertSingleValueNoDefinition() throws Exception {

        try {
            ConfigurationHelper.convertSimplePropertyValue(new PropertySimple("foo", "bar"), null, true);
            assert false;
        }
        catch (IllegalArgumentException iae) {
            System.out.println("Yep, good");
        }

    }
}
