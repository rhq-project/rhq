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
package org.rhq.modules.plugins.jbossas7;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;


import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
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
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Tests loading configurations
 * @author Heiko W. Rupp
 */
@Test
public class ConfigurationLoadingTest extends AbstractConfigurationHandlingTest {

    @BeforeSuite
    void loadPluginDescriptor() throws Exception {
        super.loadPluginDescriptor();
    }

    public void test1() throws Exception {
        FakeConnection connection = new FakeConnection();

        ConfigurationDefinition definition = new ConfigurationDefinition("foo","Test1");


        definition.setConfigurationFormat(ConfigurationFormat.STRUCTURED);
        definition.put(new PropertyDefinitionSimple("access-log", "Access-Log", false,
                PropertySimpleType.STRING));
        definition.put(new PropertyDefinitionSimple("rewrite", "Rewrite", false,
                PropertySimpleType.BOOLEAN));
        definition.put(new PropertyDefinitionSimple("notThere", "NotThere", false,
            PropertySimpleType.STRING));

        definition.put(new PropertyDefinitionList("alias", "Alias", true, new PropertyDefinitionSimple(
            "alias", "alias", true, PropertySimpleType.STRING)));



        String resultString = " {\"outcome\" : \"success\", \"result\" : {\"alias\" : [\"example.com\",\"example2.com\"],"+
                " \"access-log\" : \"my.log\", \"rewrite\" : true}, \"compensating-operation\" : null}";

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString,ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        connection.setContent(json);

        ConfigurationDelegate delegate = new ConfigurationDelegate(definition,connection,null);
        Configuration config = delegate.loadResourceConfiguration();

        assert config.get("alias")!=null;
        assert config.get("alias") instanceof PropertyList;
        PropertyList aliases = (PropertyList) config.get("alias");
        List<Property> list = aliases.getList();
        assert list.size()==2;
        int count=2;
        for (Property p: list) {
            PropertySimple ps = (PropertySimple) p;
            if (ps.getStringValue().equals("example.com"))
                count--;
            if (ps.getStringValue().equals("example2.com"))
                count--;
        }
        assert count==0 : "Did not find all needed aliases";

        Property notThere = config.get("notThere");
        assert notThere !=null;
        assert ((PropertySimple)notThere).getStringValue()==null;

        PropertySimple property = (PropertySimple) config.get("rewrite");
        assert property!=null;
        assert property.getBooleanValue();

        property = (PropertySimple) config.get("access-log");
        assert property!=null && property.getStringValue()!=null;
        assert property.getStringValue().equals("my.log");
    }

    public void test2() throws Exception {
        String resultString = "{\n" +
                "  \"outcome\" : \"success\",\n" +
                "  \"result\" : {\n" +
                "    \"autoflush\" : true,\n" +
                "    \"encoding\" : null,\n" +
                "    \"formatter\" : \"%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n\",\n" +
                "    \"level\" : \"INFO\",\n" +
                "    \"file\" : {\n" +
                "      \"path\" : \"server.log\",\n" +
                "      \"relative-to\" : \"jboss.server.log.dir\"\n" +
                "    },\n" +
                "    \"suffix\" : \".yyyy-MM-dd\"\n" +
                "  },\n" +
                "  \"compensating-operation\" : null\n" +
                "}";

        FakeConnection connection = new FakeConnection();

        ConfigurationDefinition definition = new ConfigurationDefinition("foo","Test1");

        PropertyDefinitionSimple propertyDefinition = new PropertyDefinitionSimple("autoflush", "Autoflush", false,
                PropertySimpleType.BOOLEAN);
        propertyDefinition.setDefaultValue("true");
        definition.put(propertyDefinition);
        propertyDefinition = new PropertyDefinitionSimple("encoding", "Encoding", false,
                PropertySimpleType.STRING);
        propertyDefinition.setDefaultValue("HelloWorld");
        definition.put(propertyDefinition);
        PropertyDefinitionSimple pathProperty = new PropertyDefinitionSimple("path","File path",true,PropertySimpleType.STRING);
        PropertyDefinitionSimple relativeToProperty = new PropertyDefinitionSimple("relative-to","Relative-To",true,PropertySimpleType.STRING);
        PropertyDefinitionMap fileMapDef = new PropertyDefinitionMap("file","Log file",true,pathProperty,relativeToProperty);
        definition.put(fileMapDef);

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString,ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        connection.setContent(json);

        ConfigurationDelegate delegate = new ConfigurationDelegate(definition,connection,null);
        Configuration config = delegate.loadResourceConfiguration();

        assert config!=null;
        assert config.get("autoflush")!=null : "Autoflush was null";
        assert config.getSimple("autoflush").getBooleanValue() : "Autoflush was false";
        PropertyMap fileMap = (PropertyMap) config.get("file");
        assert fileMap!=null : "File Map was null";
        PropertySimple path = (PropertySimple) fileMap.get("path");
        assert path!=null : "File->path was null";
        assert path.getStringValue().equals("server.log") : "File->path wrong";


    }


    public void test3() throws Exception {

        String resultString = "{\n" +
                "  \"outcome\" : \"success\",\n" +
                "  \"result\" : {\n" +
                "    \"name\" : \"standard-sockets\",\n" +
                "    \"default-interface\" : \"default\",\n" +
                "    \"port-offset\" : \"0\",\n" +
                "    \"socket-binding\" : {\n" +
                "      \"jndi\" : {\n" +
                "        \"name\" : \"jndi\",\n" +
                "        \"interface\" : null,\n" +
                "        \"port\" : 1099,\n" +
                "        \"fixed-port\" : null,\n" +
                "        \"multicast-address\" : null,\n" +
                "        \"multicast-port\" : null\n" +
                "      },\n" +
                "      \"jmx-connector-registry\" : {\n" +
                "        \"name\" : \"jmx-connector-registry\",\n" +
                "        \"interface\" : null,\n" +
                "        \"port\" : 1090,\n" +
                "        \"fixed-port\" : null,\n" +
                "        \"multicast-address\" : null,\n" +
                "        \"multicast-port\" : null\n" +
                "      },\n" +
                "      \"jmx-connector-server\" : {\n" +
                "        \"name\" : \"jmx-connector-server\",\n" +
                "        \"interface\" : null,\n" +
                "        \"port\" : 1091,\n" +
                "        \"fixed-port\" : null,\n" +
                "        \"multicast-address\" : null,\n" +
                "        \"multicast-port\" : null\n" +
                "      },\n" +
                "      \"http\" : {\n" +
                "        \"name\" : \"http\",\n" +
                "        \"interface\" : null,\n" +
                "        \"port\" : 8080,\n" +
                "        \"fixed-port\" : null,\n" +
                "        \"multicast-address\" : null,\n" +
                "        \"multicast-port\" : null\n" +
                "      },\n" +
                "      \"https\" : {\n" +
                "        \"name\" : \"https\",\n" +
                "        \"interface\" : null,\n" +
                "        \"port\" : 8447,\n" +
                "        \"fixed-port\" : null,\n" +
                "        \"multicast-address\" : \"224.1.2.3\",\n" +
                "        \"multicast-port\" : 18447\n" +
                "      }"+
                "    }\n" +
                "  },\n" +
                "  \"compensating-operation\" : null\n" +
                "}";

        ConfigurationDefinition definition = loadDescriptor("socketBinding");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString,ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationDelegate delegate = new ConfigurationDelegate(definition,connection,null);
        Configuration config = delegate.loadResourceConfiguration();


        assert config != null;
        PropertyList propertyList = (PropertyList) config.get("socket-binding");
        assert propertyList!=null;
        List<Property> list = propertyList.getList();
        assert list.size()==5;
        for (Property prop : list) {
            PropertyMap propMap2 = (PropertyMap) prop;
            Map<String,Property> map2 = propMap2.getMap();
            assert map2.size()==6;

            assert map2.containsKey("port");
            assert map2.containsKey("multicast-port");
            assert map2.containsKey("multicast-address");

            if (((PropertySimple)map2.get("name")).getStringValue().equals("https")) {
                assert ((PropertySimple)map2.get("port")).getIntegerValue()==8447;
                assert ((PropertySimple)map2.get("multicast-port")).getIntegerValue()==18447;
            }
        }
    }


    public void test4() throws Exception {

        String resultString = loadJsonFromFile("extensions.json");

        ConfigurationDefinition definition = loadDescriptor("test4");

        ObjectMapper mapper = new ObjectMapper();
        Result result = mapper.readValue(resultString,Result.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationDelegate delegate = new ConfigurationDelegate(definition,connection,null);
        Configuration config = delegate.loadResourceConfiguration();

        assert config != null;
        PropertyList extensions = (PropertyList) config.get("extension");
        assert  extensions !=null;
        List<Property> extensionList = extensions.getList();
        assert  extensionList.size()==22 : "Expected 22 extensions, got " + extensionList.size();
        PropertyMap propertyMap = (PropertyMap) extensionList.get(0);
        assert propertyMap != null;
        PropertyMap starMap = (PropertyMap) propertyMap.get("*");
        assert starMap!=null;
        PropertySimple module = (PropertySimple) starMap.get("module");
        assert module!=null : "Module was null, but should not";
        String stringValue = module.getStringValue();
        assert stringValue!=null : "module property has no value";
        assert stringValue.equals("org.jboss.as.arquillian.service");


    }

    public void test5() throws Exception {

        String resultString = loadJsonFromFile("schema-locations.json");

        ConfigurationDefinition definition = loadDescriptor("test5");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString,ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationDelegate delegate = new ConfigurationDelegate(definition,connection,null);
        Configuration config = delegate.loadResourceConfiguration();

        assert config != null;
        PropertyList locations = (PropertyList) config.get("schema-locations");
        assert locations!=null;
        List<Property> list = locations.getList();
        assert list.size()==21 : "List does not contain 21 entries, but " + list.size();
        PropertyMap propertyMap = (PropertyMap) list.get(0);
        assert propertyMap !=null;
        Map<String,Property> map = propertyMap.getMap();
        assert map.size()==1;
        PropertySimple urnProp = (PropertySimple) map.get("*");
        String stringValue = urnProp.getStringValue();
        assert stringValue!=null : "Location property has no value";
        assert stringValue.endsWith(".xsd");

    }

    public void test6() throws Exception {

        String resultString = loadJsonFromFile("loopback.json");

        ConfigurationDefinition definition = loadDescriptor("test6and7");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString,ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationDelegate delegate = new ConfigurationDelegate(definition,connection,null);
        Configuration config = delegate.loadResourceConfiguration();

        assert config != null;
        PropertySimple nameProperty = (PropertySimple) config.get("name");
        assert nameProperty !=null;
        String stringValue = nameProperty.getStringValue();
        assert stringValue!=null;
        assert stringValue.equals("default");

        Property criteria = config.get("criteria");
        assert  criteria !=null;
        PropertySimple critProp = (PropertySimple) criteria;
        stringValue = critProp.getStringValue();
        assert stringValue!=null;

    }

    public void test7() throws Exception {

        String resultString = loadJsonFromFile("interfaces.json");

        ConfigurationDefinition definition = loadDescriptor("test6and7");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString,ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationDelegate delegate = new ConfigurationDelegate(definition,connection,null);
        Configuration config = delegate.loadResourceConfiguration();

        assert config != null;
        PropertySimple nameProperty = (PropertySimple) config.get("name");
        assert nameProperty !=null;
        String stringValue = nameProperty.getStringValue();
        assert stringValue!=null;
        assert stringValue.equals("public");

        Property criteria = config.get("criteria");
        assert  criteria !=null;
        PropertySimple critProp = (PropertySimple) criteria;
        stringValue = critProp.getStringValue();
        assert stringValue!=null;
        assert stringValue.equals("any-ipv4-address");

    }

    public void test8() throws Exception {

        String resultString = loadJsonFromFile("connector.json");

        ConfigurationDefinition definition = loadDescriptor("test8");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString,ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationDelegate delegate = new ConfigurationDelegate(definition,connection,null);
        Configuration config = delegate.loadResourceConfiguration();
        assert config!=null;
        assert config.getAllProperties().size()==8 : "Did not find 8 properties, but " + config.getAllProperties().size();
        Property prop = config.get("bean-validation-enabled");
        assert prop != null;
        PropertySimple ps = (PropertySimple) prop;
        assert ps.getBooleanValue();
        prop = config.get("cached-connection-manager-error");
        assert prop!=null;
        ps = (PropertySimple) prop;
        assert ps.getBooleanValue()==false;

    }

    private String loadJsonFromFile(String fileName) throws Exception {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
        finally {
            reader.close();
        }
    }

}
