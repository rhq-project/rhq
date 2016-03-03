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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.ComplexResult;
import org.rhq.modules.plugins.wildfly10.json.CompositeOperation;
import org.rhq.modules.plugins.wildfly10.json.Operation;

/**
 * Test updating the AS7 configuration
 * @author Heiko W. Rupp
 */
@Test(groups = "unit")
public class ConfigurationUpdatingTest extends AbstractConfigurationHandlingTest {

    ObjectMapper mapper;

    @BeforeSuite
    void loadPluginDescriptor() throws Exception {
        super.loadPluginDescriptor();

        mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    }

    public void test1() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("simple1");
        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();
        conf.put(new PropertySimple("needed", "test"));
        conf.put(new PropertySimple("optional", null));

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        Assert.assertEquals(cop.numberOfSteps(), 2);

        for (int i = 0; i < cop.numberOfSteps(); i++) {
            Operation step = cop.step(0);
            Assert.assertEquals(step.getOperation(), "write-attribute");
            Map<String, Object> stepProps = step.getAdditionalProperties();
            Assert.assertEquals(stepProps.size(), 2);

            if (stepProps.get("name").equals("needed")) {
                Assert.assertEquals(stepProps.get("value"), "test");
            } else if (stepProps.get("name").equals("optional")) {
                Assert.assertEquals(stepProps.get("value"), null);
            } else {
                Assert.fail("Unexepected property found!");
            }
        }
    }

    public void test2() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("listOfSimple1");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();
        PropertyList propertyList = new PropertyList("foo", new PropertySimple("optional", "Hello"),
            new PropertySimple("optional", null), new PropertySimple("optional", "world"));

        conf.put(propertyList);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 1 : "#Steps should be 1 but were " + cop.numberOfSteps();
        Operation step1 = cop.step(0);
        assert step1.getOperation().equals("write-attribute");
        Map<String, Object> props = step1.getAdditionalProperties();
        assert props.size() == 2;
        List<String> values = (List<String>) props.get("value");
        assert values.size() == 2 : "Values had " + values.size() + " entries"; // The optional null must not be present

        String result = mapper.writeValueAsString(cop);

    }

    public void test3() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("mapOfSimple1");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();
        PropertyMap propertyMap = new PropertyMap("foo", new PropertySimple("needed", "Hello"), new PropertySimple(
            "optional", "world"));

        conf.put(propertyMap);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 1 : "#Steps should be 1 but were " + cop.numberOfSteps();
        Operation step1 = cop.step(0);
        assert step1.getOperation().equals("write-attribute");
        Map<String, Object> props = step1.getAdditionalProperties();
        assert props.size() == 2;
        Map<String, Object> values = (Map<String, Object>) props.get("value");
        assert values.size() == 2 : "Values had " + values.size() + " entries instead of 2"; // The optional null must not be present

        String result = mapper.writeValueAsString(cop);

    }

    public void test4() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("mapOfSimple1");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();
        PropertyMap propertyMap = new PropertyMap("foo", new PropertySimple("needed", "Hello"), new PropertySimple(
            "readOnly", "world"));

        conf.put(propertyMap);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 1 : "#Steps should be 1 but were " + cop.numberOfSteps();
        Operation step1 = cop.step(0);
        assert step1.getOperation().equals("write-attribute");
        Map<String, Object> props = step1.getAdditionalProperties();
        assert props.size() == 2;
        Map<String, Object> values = (Map<String, Object>) props.get("value");
        assert values.size() == 1 : "Values had " + values.size() + " entries instead of 1"; // The optional null must not be present

        String result = mapper.writeValueAsString(cop);

    }

    public void test5() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("mapOfSimple1");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();
        PropertyMap propertyMap = new PropertyMap("foo", new PropertySimple("needed", "Hello"));

        conf.put(propertyMap);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 1 : "#Steps should be 1 but were " + cop.numberOfSteps();
        Operation step1 = cop.step(0);
        assert step1.getOperation().equals("write-attribute");
        Map<String, Object> props = step1.getAdditionalProperties();
        assert props.size() == 2;
        Map<String, Object> values = (Map<String, Object>) props.get("value");
        assert values.size() == 1 : "Values had " + values.size() + " entries instead of 1"; // The optional null must not be present

        String result = mapper.writeValueAsString(cop);

    }

    public void test6() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("listOfMaps1");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();
        PropertyMap propertyMap = new PropertyMap("theMap", new PropertySimple("needed", "Hello"), new PropertySimple(
            "optional", "World"));

        PropertyList propertyList = new PropertyList("foo", propertyMap);

        conf.put(propertyList);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 1 : "#Steps should be 1 but were " + cop.numberOfSteps();
        Operation step1 = cop.step(0);
        assert step1.getOperation().equals("write-attribute");
        Map<String, Object> props = step1.getAdditionalProperties();
        assert props.size() == 2;
        List<Map<String, Object>> values = (List<Map<String, Object>>) props.get("value");
        assert values.size() == 1 : "Values had " + values.size() + " entries instead of 1"; // The optional null must not be present
        Map<String, Object> map = values.get(0);
        assert map.size() == 2 : "Map had " + map.size() + " entries instead of two";

        String result = mapper.writeValueAsString(cop);

    }

    public void test7() throws Exception {
        ConfigurationDefinition definition = loadDescriptor("SocketBindingGroupStandalone");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();
        PropertyMap propertyMap = new PropertyMap("http");
        propertyMap.put(new PropertySimple("name", "http"));
        propertyMap.put(new PropertySimple("port", 18080));
        propertyMap.put(new PropertySimple("fixed-port", false));
        PropertyList propertyList = new PropertyList("*");
        propertyList.add(propertyMap);
        conf.put(propertyList);
        conf.put(new PropertySimple("port-offset", 0));

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 3 : "#Steps should be 3 but were " + cop.numberOfSteps();
        Operation step1 = cop.step(0);
        Operation step2 = cop.step(1);
        Operation step3 = cop.step(2);

        // As we do not specify a base address when creating the delegate 0 or 1 address element is ok.
        assert step1.getAddress().isEmpty();
        assert step2.getAddress().size() == 1;
        assert step3.getAddress().size() == 1;

        assert step1.getAdditionalProperties().get("name").equals("port-offset");
        assert step1.getAdditionalProperties().get("value").equals("0");

        assert step2.getAdditionalProperties().get("name").equals("port");
        Object value = step2.getAdditionalProperties().get("value");
        assert value != null;
        assert value instanceof Integer;
        assert (Integer) value == 18080;

        assert step3.getAdditionalProperties().get("name").equals("fixed-port");
        Object value1 = step3.getAdditionalProperties().get("value");
        assert value1 != null;
        assert value1 instanceof Boolean;
        assert !(Boolean) value1;

        assert step2.getAddress().get(0).equals("socket-binding=http");
        assert step3.getAddress().get(0).equals("socket-binding=http");
    }

    public void test8() throws Exception {
        ConfigurationDefinition definition = loadDescriptor("SocketBindingGroupStandalone");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();
        PropertyList propertyList = new PropertyList("*");
        PropertyMap propertyMap = new PropertyMap("binding");
        propertyMap.put(new PropertySimple("name", "http"));
        propertyMap.put(new PropertySimple("port", 18080));
        propertyMap.put(new PropertySimple("fixed-port", false));
        propertyList.add(propertyMap);

        propertyMap = new PropertyMap("binding");
        propertyMap.put(new PropertySimple("name", "https"));
        propertyMap.put(new PropertySimple("port", 18081));
        propertyMap.put(new PropertySimple("fixed-port", false));
        propertyList.add(propertyMap);

        conf.put(propertyList);
        conf.put(new PropertySimple("port-offset", 0));

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 5 : "#Steps should be 5 but were " + cop.numberOfSteps();
        Operation step1 = cop.step(0);
        Operation step2 = cop.step(1);
        Operation step3 = cop.step(2);
        Operation step4 = cop.step(3);
        Operation step5 = cop.step(4);

        // As we do not specify a base address when creating the delegate 0 or 1 address element is ok.
        assert step1.getAddress().isEmpty();
        assert step2.getAddress().size() == 1;
        assert step3.getAddress().size() == 1;

        assert step1.getAdditionalProperties().get("name").equals("port-offset");
        assert step1.getAdditionalProperties().get("value").equals("0");

        assert step2.getAdditionalProperties().get("name").equals("port");
        Object value = step2.getAdditionalProperties().get("value");
        assert value != null;
        assert value instanceof Integer;
        assert (Integer) value == 18080;

        assert step3.getAdditionalProperties().get("name").equals("fixed-port");
        Object value1 = step3.getAdditionalProperties().get("value");
        assert value1 != null;
        assert value1 instanceof Boolean;
        assert !(Boolean) value1;

        assert step2.getAddress().get(0).equals("socket-binding=http");
        assert step3.getAddress().get(0).equals("socket-binding=http");
        assert step4.getAddress().get(0).equals("socket-binding=https");
        assert step5.getAddress().get(0).equals("socket-binding=https");
    }

    public void test9() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("test9");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();

        conf.put(new PropertySimple("default-virtual-server", "hulla")); // this is read-only and must not show up in result
        conf.put(new PropertySimple("test-prop", "Heiko"));
        conf.put(new PropertySimple("check-interval", 23));
        conf.put(new PropertySimple("disabled", true));
        conf.put(new PropertySimple("listings", false));
        conf.put(new PropertySimple("max-depth", 17));

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 5 : "#Steps should be 5 but were " + cop.numberOfSteps();

        Operation step1 = cop.step(0);
        Operation step2 = cop.step(1);
        Operation step3 = cop.step(2);
        Operation step4 = cop.step(3);
        Operation step5 = cop.step(4);

        assert step1.getAddress().isEmpty();
        assert step2.getAddress().size() == 1;
        assert step3.getAddress().size() == 1;
        assert step4.getAddress().size() == 1;
        assert step5.getAddress().size() == 1;
        assert step2.getAddress().get(0).equals("configuration=jsp-configuration");
        assert step3.getAddress().get(0).equals("configuration=jsp-configuration");
        assert step4.getAddress().get(0).equals("configuration=static-resources");
        assert step5.getAddress().get(0).equals("configuration=static-resources");

        assert step1.getAdditionalProperties().get("name").equals("test-prop");
        assert step1.getAdditionalProperties().get("value").equals("Heiko");
        assert step2.getAdditionalProperties().get("name").equals("check-interval");
        assert step2.getAdditionalProperties().get("value").equals(23);
        assert step3.getAdditionalProperties().get("value").equals(true);

    }

    public void test10() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("test10");

        FakeConnection connection = new FakeConnection();
        String resultString = loadJsonFromFile("system-props.json");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        connection.setContent(json);

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();

        // We have properties 'bar' and 'hello' on the server
        // update 'bar', add 'hulla' and remove 'hello'

        PropertyList propertyList = new PropertyList("*2");
        PropertyMap propertyMap = new PropertyMap("*");
        propertyMap.put(new PropertySimple("name", "hulla"));
        propertyMap.put(new PropertySimple("value", "hopp"));
        propertyList.add(propertyMap);
        propertyMap = new PropertyMap("*");
        propertyMap.put(new PropertySimple("name", "bar"));
        propertyMap.put(new PropertySimple("value", "42!"));
        propertyList.add(propertyMap);
        conf.put(propertyList);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 3 : "#Steps should be 3 but were " + cop.numberOfSteps();

        Operation step1 = cop.step(0);
        Operation step2 = cop.step(1);
        Operation step3 = cop.step(2);

        assert step1.getAddress().size() == 1;
        assert step2.getAddress().size() == 1;
        assert step3.getAddress().size() == 1;
        assert step1.getAddress().get(0).equals("system-property=hulla");
        assert step2.getAddress().get(0).equals("system-property=bar");
        assert step3.getAddress().get(0).equals("system-property=hello");
        assert step1.getOperation().equals("add");
        assert step2.getOperation().equals("write-attribute");
        assert step3.getOperation().equals("remove");

        assert step1.getAdditionalProperties().get("value").equals("hopp");
        assert step2.getAdditionalProperties().get("value").equals("42!");
        assert step3.getAdditionalProperties().isEmpty();

    }

    // Like test10, but we mark one map as "to be ignored"
    public void test10a() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("test10");

        FakeConnection connection = new FakeConnection();
        String resultString = loadJsonFromFile("system-props.json");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        connection.setContent(json);

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();

        // We have properties 'bar' and 'hello' on the server
        // add 'hulla' and remove 'hello'
        // we mark bar as immutable , so we should not write that back

        PropertyList propertyList = new PropertyList("*2");
        PropertyMap propertyMap = new PropertyMap("*");
        propertyMap.put(new PropertySimple("name", "hulla"));
        propertyMap.put(new PropertySimple("value", "hopp"));
        propertyList.add(propertyMap);
        propertyMap = new PropertyMap("*");
        propertyMap.put(new PropertySimple("name", "bar"));
        propertyMap.put(new PropertySimple("value", "42!"));
        propertyMap.setErrorMessage(ConfigurationWriteDelegate.LOGICAL_REMOVED);
        propertyList.add(propertyMap);
        conf.put(propertyList);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 2 : "#Steps should be 2 but were " + cop.numberOfSteps();

        Operation step1 = cop.step(0);
        Operation step2 = cop.step(1);

        assert step1.getAddress().size() == 1;
        assert step2.getAddress().size() == 1;
        assert step1.getAddress().get(0).equals("system-property=hulla");
        assert step2.getAddress().get(0).equals("system-property=hello");
        assert step1.getOperation().equals("add");
        assert step2.getOperation().equals("remove");

        assert step1.getAdditionalProperties().get("value").equals("hopp");
        assert step2.getAdditionalProperties().isEmpty();

    }

    public void test11() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("test11");

        FakeConnection connection = new FakeConnection();
        String resultString = loadJsonFromFile("system-props.json");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        connection.setContent(json);

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();

        // We have properties 'bar' and 'hello' on the server
        // bar has a value of 44
        // update 'bar', add 'hulla' and remove 'hello'

        PropertyList propertyList = new PropertyList("*2");
        PropertyMap propertyMap = new PropertyMap("*");
        // add 'hulla'
        propertyMap.put(new PropertySimple("name", "hulla"));
        propertyMap.put(new PropertySimple("value", "hopp"));
        propertyList.add(propertyMap);
        propertyMap = new PropertyMap("*");
        // update 'bar' -> needs to trigger a remove + an :add
        propertyMap.put(new PropertySimple("name", "bar"));
        propertyMap.put(new PropertySimple("value", "42!"));
        propertyList.add(propertyMap);
        conf.put(propertyList);
        // 'hello' is not present -> needs to trigger a :remove for it

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 4 : "#Steps should be 4 but were " + cop.numberOfSteps();

        Operation step1 = cop.step(0);
        Operation step2 = cop.step(1);
        Operation step3 = cop.step(2);
        Operation step4 = cop.step(3);

        assert step1.getAddress().size() == 1;
        assert step2.getAddress().size() == 1;
        assert step3.getAddress().size() == 1;
        assert step4.getAddress().size() == 1;
        assert step1.getAddress().get(0).equals("system-property=hulla");
        assert step2.getAddress().get(0).equals("system-property=bar");
        assert step3.getAddress().get(0).equals("system-property=bar");
        assert step4.getAddress().get(0).equals("system-property=hello");
        assert step1.getOperation().equals("add");
        assert step2.getOperation().equals("remove");
        assert step3.getOperation().equals("add");
        assert step4.getOperation().equals("remove");

        assert step1.getAdditionalProperties().get("value").equals("hopp");
        assert step2.getAdditionalProperties().isEmpty();
        assert step3.getAdditionalProperties().get("value").equals("42!");
        assert step4.getAdditionalProperties().isEmpty();

    }

    public void test12() throws Exception {
        ConfigurationDefinition definition = loadDescriptor("test12");

        FakeConnection connection = new FakeConnection();
        String resultString = loadJsonFromFile("expressionTest.json");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        connection.setContent(json);

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();

        conf.put(new PropertySimple("foo:expr", 123));
        conf.put(new PropertySimple("foo2:expr", "${foo:42}"));
        conf.put(new PropertySimple("bar", 456));

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop != null;
        assert cop.numberOfSteps() == 3;

        Map<String, Object> additionalProperties = cop.step(0).getAdditionalProperties();
        assert additionalProperties.get("name").equals("foo");
        assert additionalProperties.get("value").equals("123");

        additionalProperties = cop.step(1).getAdditionalProperties();
        assert additionalProperties.get("name").equals("foo2");
        assert additionalProperties.get("value") != null;
        assert additionalProperties.get("value") instanceof Map;
        Map<String, Object> map = (Map<String, Object>) additionalProperties.get("value");
        assert map.containsKey("EXPRESSION_VALUE");
        assert map.get("EXPRESSION_VALUE").equals("${foo:42}");

        additionalProperties = cop.step(2).getAdditionalProperties();
        assert additionalProperties.get("name").equals("bar");
        assert additionalProperties.get("value").equals(456);

    }

    public void test13() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("test13");

        FakeConnection connection = new FakeConnection();
        String resultString = loadJsonFromFile("collapsedMapTest.json");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        connection.setContent(json);

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();

        PropertyMap pm = new PropertyMap("connector:collapsed");
        PropertySimple ps = new PropertySimple("name:0", "in-vm");
        pm.put(ps);
        ps = new PropertySimple("backup:1", "hulla-hoo");
        pm.put(ps);
        conf.put(pm);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop != null;
        assert cop.numberOfSteps() == 1;
        Operation step = cop.step(0);
        assert step != null;
        assert step.getOperation().equals("write-attribute") : "Step name was " + step.getOperation();
        Map<String, Object> additionalProperties = step.getAdditionalProperties();
        assert additionalProperties != null;
        assert additionalProperties.size() == 2;
        assert additionalProperties.get("name").equals("connector");
        Object value = additionalProperties.get("value");
        assert value instanceof Map;
        Map<String, Object> map = (Map<String, Object>) value;
        assert map.containsKey("in-vm");
        assert map.containsValue("hulla-hoo");
    }

    // Like test13, but have a "degenerated" map of simple with only one entry
    public void test13a() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("test13a");

        FakeConnection connection = new FakeConnection();
        String resultString = loadJsonFromFile("collapsedMapTest.json");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        connection.setContent(json);

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();

        PropertyMap pm = new PropertyMap("connector:collapsed");
        PropertySimple ps = new PropertySimple("name:0", "in-vm");
        pm.put(ps);
        conf.put(pm);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop != null;
        assert cop.numberOfSteps() == 1;
        Operation step = cop.step(0);
        assert step != null;
        assert step.getOperation().equals("write-attribute") : "Step name was " + step.getOperation();
        Map<String, Object> additionalProperties = step.getAdditionalProperties();
        assert additionalProperties != null;
        assert additionalProperties.size() == 2;
        assert additionalProperties.get("name").equals("connector");
        Object value = additionalProperties.get("value");
        assert value instanceof Map;
        Map<String, Object> map = (Map<String, Object>) value;
        assert map.containsKey("in-vm");
        Object obj = map.get("in-vm");
        assert obj == null : "There was some value for map key in-vm";
    }

    public void testListOfPlainMaps() throws Exception {
        String resultString = loadJsonFromFile("listofplainmaps.json");
        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationDefinition definition = loadDescriptor("listOfPlainMaps");

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        int index = 1;
        PropertyList list = new PropertyList("properties");
        for (int i = 0; i < 3; i++) {
            PropertyMap map = new PropertyMap("property:collapsed");

            PropertySimple nameProperty = new PropertySimple("name:0", "test" + index);
            map.put(nameProperty);
            index++;

            PropertySimple valueProperty = new PropertySimple("value:1", "test" + index);
            map.put(valueProperty);
            index++;

            list.add(map);
        }

        Configuration conf = new Configuration();
        conf.put(list);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        Assert.assertNotNull(cop);
        Assert.assertEquals(cop.numberOfSteps(), 1);
        Assert.assertEquals(cop.step(0).getOperation(), "write-attribute", "Step name was "
            + cop.step(0).getOperation());

        Map<String, Object> additionalProperties = cop.step(0).getAdditionalProperties();
        Assert.assertEquals(additionalProperties.size(), 2);
        Assert.assertEquals(additionalProperties.get("name"), "properties");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> value = (List<Map<String, Object>>) additionalProperties.get("value");
        Assert.assertEquals(value.size(), 3);

        index = 1;
        for (Map<String, Object> map : value) {
            Assert.assertTrue(map.containsKey("test" + index));
            index++;
            Assert.assertTrue(map.containsValue("test" + index));
            index++;

            Assert.assertEquals(map.size(), 1);
        }
    }

    @SuppressWarnings("unchecked")
    public void testGroupedPropertiesWithIdenticalNames() throws Exception {
        String resultString = loadJsonFromFile("groupedproperties.json");
        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString, ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        FakeConnection connection = new FakeConnection();
        connection.setContent(json);

        ConfigurationDefinition definition = loadDescriptor("groupedproperties");

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();
        for (int index = 1; index < 5; index += 3) {
            String stringValue = index + "" + (index + 1) + "" + (index + 2);

            String firstPropName = "firstprop:" + index;
            PropertySimple propertySimple = new PropertySimple(firstPropName, stringValue);
            conf.put(propertySimple);

            String secondPropName = "secondprop:" + (index + 1);
            PropertyList propertyList = new PropertyList(secondPropName);
            for (int i = 0; i < 3; i++) {
                String value = "test" + (i + index);
                PropertySimple tempPropertySimple = new PropertySimple("name", value);
                propertyList.add(tempPropertySimple);
            }
            conf.put(propertyList);

            String thirdPropName = "thirdprop:" + (index + 2);
            PropertyMap propertyMap = new PropertyMap(thirdPropName);
            PropertySimple tempPropertySimple = new PropertySimple("value", stringValue);
            propertyMap.put(tempPropertySimple);
            conf.put(propertyMap);
        }

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        Assert.assertNotNull(cop);
        Assert.assertEquals(cop.numberOfSteps(), 6);

        for (int index = 0; index < 6; index++) {
            Operation step = cop.step(index);
            Assert.assertEquals(step.getOperation(), "write-attribute", "Step name was " + step.getOperation());
            Map<String, Object> additionalProperties = step.getAdditionalProperties();
            Assert.assertEquals(additionalProperties.size(), 2);

            if (additionalProperties.get("name").equals("firstprop")) {
                String expectedValue = (index + 1) + "" + (index + 2) + "" + (index + 3);
                Assert.assertEquals(additionalProperties.get("value"), expectedValue);
            } else if (additionalProperties.get("name").equals("secondprop")) {
                List<String> listValue = (List<String>) additionalProperties.get("value");
                Assert.assertTrue(listValue.contains("test" + (index)));
                Assert.assertTrue(listValue.contains("test" + (index + 1)));
                Assert.assertTrue(listValue.contains("test" + (index + 2)));
            } else {
                Map<String, String> mapValue = (Map<String, String>) additionalProperties.get("value");
                Assert.assertEquals(mapValue.size(), 1);
                String expectedValue = (index - 1) + "" + (index) + "" + (index + 1);
                Assert.assertEquals(mapValue.values().iterator().next(), expectedValue);
            }
        }

    }

    /** Tests that c:group entries are updated correctly in addition to special c:group syntax handling.
     *  Ex. <c:group name="proxy" displayName="Proxy Options">
     *       <c:simple-property name="proxy-list" required="false" type="string" readOnly="false" defaultValue="" description="List of proxies, Format (hostname:port) separated with comas."/>
     *       <c:simple-property name="proxy-url" required="false" type="string" readOnly="false" defaultValue="/" description="Base URL for MCMP requests."/>
     *      </c:group>
     *
     * @throws Exception
     */
    public void testUpdateGroupConfiguration() throws Exception {

        ConfigurationDefinition definition = loadServiceDescriptorElement("simpleGroupNoSpecialUpdate");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("proxy-list", "127.0.0.1:7777,test.localhost.com:6666");
        properties.put("proxy-url", "/rootWebappUrl");
        for (String name : properties.keySet()) {//load all properties for update.
            conf.put(new PropertySimple(name, properties.get(name)));
        }

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == properties.size() : "Composite operation steps incorrect. Expected '"
            + properties.size() + "' but was '" + cop.numberOfSteps() + "'.";
        //check property values
        for (int i = 0; i < cop.numberOfSteps(); i++) {
            //each property maps to a separate operation.
            Operation step = cop.step(i);
            assert step.getOperation().equals("write-attribute") : "Write attribute not set correctly.";
            Map<String, Object> props = step.getAdditionalProperties();
            assert props.size() == 2 : "Property list not correct. Expected '2' property but there were '"
                + props.size() + "'.";
            //check that property was returned
            String[] keys = new String[2];
            props.keySet().toArray(keys);
            String name = (String) props.get("name");
            String value = (String) props.get("value");
            assert properties.containsKey(name) : "Property '" + name + "' was not found and should have been.";
            //check the contents of returned response.
            assert value.equals(properties.get(name)) : "Value for property '" + name
                + "' was not updated correctly. Expected '" + properties.get(name) + "' but was '" + value + "'.";
        }
    }

    public void testIgnoreProperty() throws Exception {
        ConfigurationDefinition definition = loadDescriptor("testIgnore");

        Configuration configuration = new Configuration();

        configuration.put(new PropertySimple("normal","0xdeadbeef"));
        configuration.put(new PropertySimple("test:ignore","Hello world!")); // should not create a step

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);
        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(configuration, new Address());

        assert cop != null;
        assert cop.numberOfSteps() == 1 : "One step was expected, but got " + cop.numberOfSteps();

    }

    /**
     * Test that if a property is required and has a defaultValue and the user just uses this,
     * we actually pass this default to the operation, as e.g. the CreateResourceReport may not
     * include the default value.
     * @throws Exception If anything goes wrong
     */
    public void testSimpleWithDefault1() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("simpleWithDefault1");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();
        PropertySimple ps = new PropertySimple("mode",null);
        conf.put(ps);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 1;
        Operation step1 = cop.step(0);
        assert step1.getOperation().equals("write-attribute");
        Map<String, Object> props = step1.getAdditionalProperties();
        assert props.size() == 2;
        assert props.get("name").equals("mode");
        assert props.get("value").equals("SYNC"); // the defaultValue
    }

    /**
     * Check that if a property is required and has no defaultValue, but the user provides a value,
     * that the user provided value ends up in the operation
     * @throws Exception
     */
    public void testSimpleWithDefault2() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("simpleWithDefault2");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();
        PropertySimple ps = new PropertySimple("mode","ASYNC");
        conf.put(ps);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 1;
        Operation step1 = cop.step(0);
        assert step1.getOperation().equals("write-attribute");
        Map<String, Object> props = step1.getAdditionalProperties();
        assert props.size() == 2;
        assert props.get("name").equals("mode");
        assert props.get("value").equals("ASYNC"); // the user provided value
    }

    /**
     * Check that if a property is required and has no defaultValue, and the user provides null
     * as value, that we set null
     * @throws Exception
     */
    public void testSimpleWithDefault3() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("simpleWithDefault2");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition, connection, null);

        Configuration conf = new Configuration();
        PropertySimple ps = new PropertySimple("mode",null);
        conf.put(ps);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 1;
        Operation step1 = cop.step(0);
        assert step1.getOperation().equals("write-attribute");
        Map<String, Object> props = step1.getAdditionalProperties();
        assert props.size() == 2;
        assert props.get("name").equals("mode");
        assert props.get("value")==null; // no value
    }

}
