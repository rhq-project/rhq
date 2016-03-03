/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.wildfly10;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.modules.plugins.wildfly10.json.ComplexResult;
import org.rhq.modules.plugins.wildfly10.json.Result;

/**
 * Tests loading configurations
 * @author Heiko W. Rupp
 */
@Test(groups = "unit")
public class ConfigurationLoadingTest extends AbstractConfigurationHandlingTest {

    @BeforeSuite
    void loadPluginDescriptor() throws Exception {
        super.loadPluginDescriptor();
    }

    public void test1() throws Exception {

        //create a fake connection so that we control the execute behavior
        FakeConnection connection = new FakeConnection();

        //create/mock a configuration definition. Remember definition shared between N configurations.
        ConfigurationDefinition definition = new ConfigurationDefinition("foo", "Test1");

        //?
        definition.setConfigurationFormat(ConfigurationFormat.STRUCTURED);

        //Add a few properties that we will be testing/exercising
        definition.put(new PropertyDefinitionSimple("access-log", "Access-Log", false, PropertySimpleType.STRING));
        definition.put(new PropertyDefinitionSimple("rewrite", "Rewrite", false, PropertySimpleType.BOOLEAN));
        definition.put(new PropertyDefinitionSimple("notThere", "NotThere", false, PropertySimpleType.STRING));

        definition.put(new PropertyDefinitionList("alias", "Alias", true, new PropertyDefinitionSimple("alias",
            "alias", true, PropertySimpleType.STRING)));

        //Construct the result string that we would expect back.
        String resultString = " {\"outcome\" : \"success\", \"result\" : {\"alias\" : [\"example.com\",\"example2.com\"],"
            + " \"access-log\" : \"my.log\", \"rewrite\" : true}}";

        ObjectMapper mapper = new ObjectMapper();
        //deserialize string to ComplexResult.
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);

        //create json tree from result.
        JsonNode json = mapper.valueToTree(result);

        //add the created content to the fake connection so that we set the results to be returned.
        connection.setContent(json);

        //We pass in null here so that the fake connection will return the value passed into setContent().
        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(definition, connection, null);
        Configuration config = delegate.loadResourceConfiguration();

        assert config.get("alias") != null;
        assert config.get("alias") instanceof PropertyList;
        PropertyList aliases = (PropertyList) config.get("alias");
        List<Property> list = aliases.getList();
        assert list.size() == 2;
        int count = 2;
        for (Property p : list) {
            PropertySimple ps = (PropertySimple) p;
            if (ps.getStringValue().equals("example.com"))
                count--;
            if (ps.getStringValue().equals("example2.com"))
                count--;
        }
        assert count == 0 : "Did not find all needed aliases";

        Property notThere = config.get("notThere");
        assert notThere != null;
        assert ((PropertySimple) notThere).getStringValue() == null;

        PropertySimple property = (PropertySimple) config.get("rewrite");
        assert property != null;
        assert property.getBooleanValue();

        property = (PropertySimple) config.get("access-log");
        assert property != null && property.getStringValue() != null;
        assert property.getStringValue().equals("my.log");
    }

    public void test2() throws Exception {
        String resultString = "{\n" + "  \"outcome\" : \"success\",\n" + "  \"result\" : {\n"
            + "    \"autoflush\" : true,\n" + "    \"encoding\" : null,\n"
            + "    \"formatter\" : \"%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n\",\n" + "    \"level\" : \"INFO\",\n"
            + "    \"file\" : {\n" + "      \"path\" : \"server.log\",\n"
            + "      \"relative-to\" : \"jboss.server.log.dir\"\n" + "    },\n" + "    \"suffix\" : \".yyyy-MM-dd\"\n"
            + "  },\n" + "  \"response-headers\" : null\n" + "}";

        FakeConnection connection = new FakeConnection();

        ConfigurationDefinition definition = new ConfigurationDefinition("foo", "Test1");

        PropertyDefinitionSimple propertyDefinition = new PropertyDefinitionSimple("autoflush", "Autoflush", false,
            PropertySimpleType.BOOLEAN);
        propertyDefinition.setDefaultValue("true");
        definition.put(propertyDefinition);
        propertyDefinition = new PropertyDefinitionSimple("encoding", "Encoding", false, PropertySimpleType.STRING);
        propertyDefinition.setDefaultValue("HelloWorld");
        definition.put(propertyDefinition);
        PropertyDefinitionSimple pathProperty = new PropertyDefinitionSimple("path", "File path", true,
            PropertySimpleType.STRING);
        PropertyDefinitionSimple relativeToProperty = new PropertyDefinitionSimple("relative-to", "Relative-To", true,
            PropertySimpleType.STRING);
        PropertyDefinitionMap fileMapDef = new PropertyDefinitionMap("file", "Log file", true, pathProperty,
            relativeToProperty);
        definition.put(fileMapDef);

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        connection.setContent(json);

        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(definition, connection, null);
        Configuration config = delegate.loadResourceConfiguration();

        assert config != null;
        assert config.get("autoflush") != null : "Autoflush was null";
        assert config.getSimple("autoflush").getBooleanValue() : "Autoflush was false";
        PropertyMap fileMap = (PropertyMap) config.get("file");
        assert fileMap != null : "File Map was null";
        PropertySimple path = (PropertySimple) fileMap.get("path");
        assert path != null : "File->path was null";
        assert path.getStringValue().equals("server.log") : "File->path wrong";

    }

    public void test3() throws Exception {

        String resultString = "{\n" + "  \"outcome\" : \"success\",\n" + "  \"result\" : {\n"
            + "    \"name\" : \"standard-sockets\",\n" + "    \"default-interface\" : \"default\",\n"
            + "    \"port-offset\" : \"0\",\n" + "    \"socket-binding\" : {\n" + "      \"jndi\" : {\n"
            + "        \"name\" : \"jndi\",\n" + "        \"interface\" : null,\n" + "        \"port\" : 1099,\n"
            + "        \"fixed-port\" : null,\n" + "        \"multicast-address\" : null,\n"
            + "        \"multicast-port\" : null\n" + "      },\n" + "      \"jmx-connector-registry\" : {\n"
            + "        \"name\" : \"jmx-connector-registry\",\n" + "        \"interface\" : null,\n"
            + "        \"port\" : 1090,\n" + "        \"fixed-port\" : null,\n"
            + "        \"multicast-address\" : null,\n" + "        \"multicast-port\" : null\n" + "      },\n"
            + "      \"jmx-connector-server\" : {\n" + "        \"name\" : \"jmx-connector-server\",\n"
            + "        \"interface\" : null,\n" + "        \"port\" : 1091,\n" + "        \"fixed-port\" : null,\n"
            + "        \"multicast-address\" : null,\n" + "        \"multicast-port\" : null\n" + "      },\n"
            + "      \"http\" : {\n" + "        \"name\" : \"http\",\n" + "        \"interface\" : null,\n"
            + "        \"port\" : 8080,\n" + "        \"fixed-port\" : null,\n"
            + "        \"multicast-address\" : null,\n" + "        \"multicast-port\" : null\n" + "      },\n"
            + "      \"https\" : {\n" + "        \"name\" : \"https\",\n" + "        \"interface\" : null,\n"
            + "        \"port\" : 8447,\n" + "        \"fixed-port\" : null,\n"
            + "        \"multicast-address\" : \"224.1.2.3\",\n" + "        \"multicast-port\" : 18447\n" + "      }"
            + "    }\n" + "  }\n" + "}";

        ConfigurationDefinition definition = loadDescriptor("socketBinding");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(definition, connection, null);
        Configuration config = delegate.loadResourceConfiguration();

        assert config != null;
        PropertyList propertyList = (PropertyList) config.get("socket-binding");
        assert propertyList != null;
        List<Property> list = propertyList.getList();
        assert list.size() == 5;
        for (Property prop : list) {
            PropertyMap propMap2 = (PropertyMap) prop;
            Map<String, Property> map2 = propMap2.getMap();
            assert map2.size() == 6;

            assert map2.containsKey("port");
            assert map2.containsKey("multicast-port");
            assert map2.containsKey("multicast-address");

            if (((PropertySimple) map2.get("name")).getStringValue().equals("https")) {
                assert ((PropertySimple) map2.get("port")).getIntegerValue() == 8447;
                assert ((PropertySimple) map2.get("multicast-port")).getIntegerValue() == 18447;
            }
        }
    }

    public void test4() throws Exception {

        String resultString = loadJsonFromFile("extensions.json");

        ConfigurationDefinition definition = loadDescriptor("test4");

        ObjectMapper mapper = new ObjectMapper();
        Result result = mapper.readValue(resultString, Result.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(definition, connection, null);
        Configuration config = delegate.loadResourceConfiguration();

        assert config != null;
        PropertyList extensions = (PropertyList) config.get("extension");
        assert extensions != null;
        List<Property> extensionList = extensions.getList();
        assert extensionList.size() == 22 : "Expected 22 extensions, got " + extensionList.size();
        PropertyMap propertyMap = (PropertyMap) extensionList.get(0);
        assert propertyMap != null;
        PropertyMap starMap = (PropertyMap) propertyMap.get("*");
        assert starMap != null;
        PropertySimple module = (PropertySimple) starMap.get("module");
        assert module != null : "Module was null, but should not";
        String stringValue = module.getStringValue();
        assert stringValue != null : "module property has no value";
        assert stringValue.equals("org.jboss.as.arquillian.service");

    }

    public void test5() throws Exception {

        String resultString = loadJsonFromFile("schema-locations.json");

        ConfigurationDefinition definition = loadDescriptor("test5");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(definition, connection, null);
        Configuration config = delegate.loadResourceConfiguration();

        assert config != null;
        PropertyList locations = (PropertyList) config.get("schema-locations");
        assert locations != null;
        List<Property> list = locations.getList();
        assert list.size() == 21 : "List does not contain 21 entries, but " + list.size();
        PropertyMap propertyMap = (PropertyMap) list.get(0);
        assert propertyMap != null;
        Map<String, Property> map = propertyMap.getMap();
        assert map.size() == 1;
        PropertySimple urnProp = (PropertySimple) map.get("*");
        String stringValue = urnProp.getStringValue();
        assert stringValue != null : "Location property has no value";
        assert stringValue.endsWith(".xsd");

    }

    public void test6() throws Exception {

        String resultString = loadJsonFromFile("loopback.json");

        ConfigurationDefinition definition = loadDescriptor("test6and7");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(definition, connection, null);
        Configuration config = delegate.loadResourceConfiguration();

        assert config != null;
        PropertySimple nameProperty = (PropertySimple) config.get("name");
        assert nameProperty != null;
        String stringValue = nameProperty.getStringValue();
        assert stringValue != null;
        assert stringValue.equals("default");

        Property criteria = config.get("criteria");
        assert criteria != null;
        PropertySimple critProp = (PropertySimple) criteria;
        stringValue = critProp.getStringValue();
        assert stringValue != null;

    }

    public void test7() throws Exception {

        String resultString = loadJsonFromFile("interfaces.json");

        ConfigurationDefinition definition = loadDescriptor("test6and7");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(definition, connection, null);
        Configuration config = delegate.loadResourceConfiguration();

        assert config != null;
        PropertySimple nameProperty = (PropertySimple) config.get("name");
        assert nameProperty != null;
        String stringValue = nameProperty.getStringValue();
        assert stringValue != null;
        assert stringValue.equals("public");

        Property criteria = config.get("criteria");
        assert criteria != null;
        PropertySimple critProp = (PropertySimple) criteria;
        stringValue = critProp.getStringValue();
        assert stringValue != null;
        assert stringValue.equals("any-ipv4-address");

    }

    public void test8() throws Exception {

        String resultString = loadJsonFromFile("connector.json");

        ConfigurationDefinition definition = loadDescriptor("test8");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(definition, connection, null);
        Configuration config = delegate.loadResourceConfiguration();
        assert config != null;
        assert config.getAllProperties().size() == 8 : "Did not find 8 properties, but "
            + config.getAllProperties().size();
        Property prop = config.get("bean-validation-enabled");
        assert prop != null;
        PropertySimple ps = (PropertySimple) prop;
        assert ps.getBooleanValue();
        prop = config.get("cached-connection-manager-error");
        assert prop != null;
        ps = (PropertySimple) prop;
        assert ps.getBooleanValue() == false;

    }

    public void test9() throws Exception {
        String resultString = loadJsonFromFile("web.json");
        ConfigurationDefinition definition = loadDescriptor("test9");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(definition, connection, null);
        Configuration config = delegate.loadResourceConfiguration();
        assert config != null;
        Collection<Property> properties = config.getProperties();
        assert properties.size() == 6 : "Got " + properties.size() + " props instead of 6: " + properties.toString();
        PropertySimple simple = config.getSimple("check-interval");
        assert simple != null;
        Integer integerValue = simple.getIntegerValue();
        assert integerValue != null : "check-interval was null";
        assert integerValue == 17 : "check-interval was not 17 but " + integerValue;
        PropertySimple disabled = config.getSimple("disabled");
        assert disabled != null : "disabled was null";
        Boolean booleanValue = disabled.getBooleanValue();
        assert booleanValue != null;
        assert booleanValue;
        PropertySimple listings = config.getSimple("listings");
        assert listings != null;
        Boolean booleanValue1 = listings.getBooleanValue();
        assert booleanValue1 != null;
        assert !booleanValue1;
        PropertySimple simple1 = config.getSimple("max-depth");
        assert simple1 != null;
        Integer integerValue1 = simple1.getIntegerValue();
        assert integerValue1 != null;
        assert integerValue1 == 3;
        PropertySimple simple2 = config.getSimple("default-virtual-server");
        assert simple2 != null;
        String stringValue = simple2.getStringValue();
        assert stringValue != null;
        assert stringValue.equals("default-host");

    }

    public void test12() throws Exception {

        String resultString = loadJsonFromFile("expressionTest.json");
        ConfigurationDefinition definition = loadDescriptor("test12");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(definition, connection, null);
        Configuration config = delegate.loadResourceConfiguration();
        Collection<Property> properties = config.getProperties();
        assert properties.size() == 3;
        PropertySimple foo = config.getSimple("foo:expr");
        PropertySimple foo2 = config.getSimple("foo2:expr");
        PropertySimple bar = config.getSimple("bar");

        assert foo != null;
        assert foo2 != null;
        assert bar != null;

        Integer tmp = foo.getIntegerValue();
        assert tmp != null;
        assert tmp == 123;
        String stringValue = foo2.getStringValue();
        assert stringValue != null;
        assert stringValue.equals("${foo2:42}");
        tmp = bar.getIntegerValue();
        assert tmp != null;
        assert tmp == 456;

    }

    public void test13() throws Exception {
        String resultString = loadJsonFromFile("collapsedMapTest.json");

        ConfigurationDefinition definition = loadDescriptor("test13");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(definition, connection, null);
        Configuration config = delegate.loadResourceConfiguration();
        Collection<Property> properties = config.getProperties();

        assert properties.size() == 1;
        Iterator<Property> iterator = properties.iterator();
        Property p = iterator.next();
        assert p instanceof PropertyMap;
        PropertyMap pm = (PropertyMap) p;
        assert pm.getMap().size() == 2;
        PropertySimple ps = pm.getSimple("name:0");
        assert ps != null : "No property with name 'name:0' was found";
        Assert.assertEquals(ps.getStringValue(), "in-vm", "Unexpected value for " + ps);
        ps = pm.getSimple("backup:1");
        assert ps != null : "No property with name 'backup:1' was found";
        Assert.assertNull(ps.getStringValue(), "Unexpected value for " + ps);
    }

    // Like test13, but using a degenerated map with only a key entry
    public void test13a() throws Exception {
        String resultString = loadJsonFromFile("collapsedMapTest2.json");

        ConfigurationDefinition definition = loadDescriptor("test13a");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(definition, connection, null);
        Configuration config = delegate.loadResourceConfiguration();
        Collection<Property> properties = config.getProperties();

        assert properties.size() == 1;
        Iterator<Property> iterator = properties.iterator();
        Property p = iterator.next();
        assert p instanceof PropertyMap;
        PropertyMap pm = (PropertyMap) p;
        assert pm.getMap().size() == 1;
        PropertySimple ps = pm.getSimple("name:0");
        assert ps != null : "No property with name 'name:0' was found";
        Assert.assertEquals(ps.getStringValue(), "in-vm", "Unexpected value for " + ps);
        ps = pm.getSimple("backup:1");
        assert ps == null : "A property with name 'backup:1' was found, but should not";
    }

    public void testListOfPlainMaps() throws Exception {
        String resultString = loadJsonFromFile("listofplainmaps.json");

        ConfigurationDefinition definition = loadDescriptor("listOfPlainMaps");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(definition, connection, null);
        Configuration config = delegate.loadResourceConfiguration();
        Collection<Property> properties = config.getProperties();

        Assert.assertEquals(properties.size(), 1);

        PropertyList propertyList = (PropertyList) properties.iterator().next();

        Assert.assertNotNull(propertyList);

        int index = 1;
        for (Property property : propertyList.getList()) {

            PropertyMap map = (PropertyMap) property;

            PropertySimple nameProperty = (PropertySimple) map.get("name:0");
            Assert.assertEquals(nameProperty.getStringValue(), "test" + index);
            index++;

            PropertySimple valueProperty = (PropertySimple) map.get("value:1");
            Assert.assertEquals(valueProperty.getStringValue(), "test" + index);
            index++;
        }
    }

    public void testGroupedPropertiesWithIdenticalNames() throws Exception {
        String resultString = loadJsonFromFile("groupedproperties.json");

        ConfigurationDefinition definition = loadDescriptor("groupedproperties");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(definition, connection, null);
        Configuration config = delegate.loadResourceConfiguration();
        Collection<Property> properties = config.getProperties();

        Assert.assertEquals(properties.size(), 6);

        for (int index = 1; index < 5; index += 3) {
            String stringValue = index + "" + (index + 1) + "" + (index + 2);

            String firstPropName = "firstprop:" + index;
            boolean firstPropFound = false;
            String secondPropName = "secondprop:" + (index + 1);
            boolean secondPropFound = false;
            String thirdPropName = "thirdprop:" + (index + 2);
            boolean thirdPropFound = false;

            for (Property property : properties) {
                if (property.getName().equals(firstPropName)) {
                    firstPropFound = true;
                    Assert.assertEquals(((PropertySimple) property).getStringValue(), stringValue);
                } else if (property.getName().equals(secondPropName)) {
                    secondPropFound = true;
                    PropertyList list = (PropertyList) property;
                    Assert.assertEquals(list.getList().size(), 3);
                    for (int i = 0; i < 3; i++) {
                        PropertySimple simpleProperty = (PropertySimple) list.getList().get(i);
                        String expectedValue = "test" + (i + index);
                        Assert.assertEquals(simpleProperty.getStringValue(), expectedValue);
                    }
                } else if (property.getName().equals(thirdPropName)) {
                    thirdPropFound = true;
                    PropertyMap map = (PropertyMap) property;
                    Assert.assertEquals(map.getMap().size(), 1);
                    String actualValue = ((PropertySimple) (map.get("value"))).getStringValue();
                    Assert.assertEquals(actualValue, stringValue);
                }
            }

            Assert.assertTrue(firstPropFound);
            Assert.assertTrue(secondPropFound);
            Assert.assertTrue(thirdPropFound);
        }
    }

    /** Tests that c:group entries are loaded correctly in addition to special c:group syntax handling. 
     *  Ex. <c:group name="proxy" displayName="Proxy Options">
     *       <c:simple-property name="proxy-list" required="false" type="string" readOnly="false" defaultValue="" description="List of proxies, Format (hostname:port) separated with comas."/>
     *       <c:simple-property name="proxy-url" required="false" type="string" readOnly="false" defaultValue="/" description="Base URL for MCMP requests."/>
     *      </c:group>
     * 
     * @throws Exception
     */
    public void testLoadGroupedConfiguration() throws Exception {

        //Fabricate the json string result.
        String resultString = "{\n" + "   \"outcome\" : \"success\",    \"result\" : {\n"
            + "      \"advertise\" : \"true\",\n" + "      \"advertise-socket\" : \"modcluster\",\n"
            + "      \"balancer\" : \"undefined\",\n" + "      \"connector\" : \"ajp\",\n"
            + "      \"proxy-list\" : \"undefined\",\n" + "      \"proxy-url\" : \"/\"\n" + "      }\n" + "}\n";

        ConfigurationDefinition definition = loadServiceDescriptorElement("simpleGroupNoSpecial");

        //Formally construct the json response
        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        //Create fake connection and prepopulate the response.
        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        //Test actual load using test-plugin.xml contents.
        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(definition, connection, null);
        Configuration config = delegate.loadResourceConfiguration();

        //Validate loaded config.
        assert config != null;

        //check boolean contents
        PropertySimple advertise = (PropertySimple) config.get("advertise");
        assert advertise != null : "Boolean 'advertise' property embedded in c:group not found.";
        boolean advertiseOn = advertise.getBooleanValue();
        assert advertiseOn != false : "Advertise value not set to true or not defaulting to true.";

        //Define properties and default values to check.
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("advertise-socket", "modcluster");
        properties.put("connector", "ajp");
        properties.put("proxy-list", "undefined");
        properties.put("proxy-url", "/");
        for (String prop : properties.keySet()) {
            PropertySimple property = (PropertySimple) config.get(prop);
            assert property != null : "Property '" + prop + "' could not be found.";
            String value = property.getStringValue();
            assert value != null : "The value for property '" + prop + "' was not located.";
            assert value.trim().equals(properties.get(prop)) : "Value for property '" + prop
                + "' does not match. Found '" + value + "' instead of '" + properties.get(prop) + "'";
        }

    }
}