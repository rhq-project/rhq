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

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.clientapi.agent.metadata.InvalidPluginDescriptorException;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
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
import org.rhq.modules.plugins.jbossas7.json.Operation;

/**
 * Tests loading and writing configurations
 * @author Heiko W. Rupp
 */
@Test
public class ConfigurationTest {

    private static final String DESCRIPTOR_FILENAME = "test-plugin.xml";
    private Log log = LogFactory.getLog(getClass());

    private PluginDescriptor pluginDescriptor;



    public void test1() throws Exception {
        FakeConnection connection = new FakeConnection();

        ConfigurationDefinition definition = new ConfigurationDefinition("foo","Test1");


        definition.setConfigurationFormat(ConfigurationFormat.STRUCTURED);
        definition.put(new PropertyDefinitionSimple("access-log", "Access-Log", false,
                PropertySimpleType.STRING));
        definition.put(new PropertyDefinitionSimple("rewrite", "Rewrite", false,
            PropertySimpleType.STRING));
        definition.put(new PropertyDefinitionSimple("notThere", "NotThere", false,
            PropertySimpleType.STRING));

        definition.put(new PropertyDefinitionList("alias", "Alias", true, new PropertyDefinitionSimple(
            "alias", "alias", true, PropertySimpleType.STRING)));



        String resultString = " {\"outcome\" : \"success\", \"result\" : {\"alias\" : [\"example.com\",\"example2.com\"],"+
                " \"access-log\" : null, \"rewrite\" : null}, \"compensating-operation\" : null}";

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
        assert config.get("notThere")==null;
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



    @BeforeSuite
    private void loadPluginDescriptor() throws Exception {
        try {
            URL descriptorUrl = this.getClass().getClassLoader().getResource(DESCRIPTOR_FILENAME);
            log.info("Loading plugin descriptor at: " + descriptorUrl);

            JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ValidationEventCollector vec = new ValidationEventCollector();
            unmarshaller.setEventHandler(vec);
            pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(descriptorUrl.openStream());
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    private ConfigurationDefinition loadDescriptor(String serverName) throws InvalidPluginDescriptorException {
        List<ServerDescriptor> servers = pluginDescriptor.getServers();

        ServerDescriptor serverDescriptor = findServer(serverName, servers);
        assert serverDescriptor != null : "Server descriptor not found in test plugin descriptor";

        return ConfigurationMetadataParser.parse("null", serverDescriptor.getResourceConfiguration());
    }

    private ServerDescriptor findServer(String name, List<ServerDescriptor> servers) {
        for (ServerDescriptor server : servers) {
            if (server.getName().equals(name)) {
                return server;
            }
        }

        return null;
    }



    /**
     * Provide a fake connection, that will return the
     * content we provide via #setContent
     *
     */
    private class FakeConnection extends ASConnection {

        JsonNode content;

        public FakeConnection() {
            super("localhost", 1234);
        }

        public void setContent(JsonNode content) {
            this.content = content;
        }

        @Override
        public JsonNode executeRaw(Operation operation) {
            if (content==null)
                throw new IllegalStateException("Content not yet set");
            return content;
        }
    }
}
