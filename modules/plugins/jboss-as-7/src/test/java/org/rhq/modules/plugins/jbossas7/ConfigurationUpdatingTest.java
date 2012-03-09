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

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;

/**
 * Test updating the AS7 configuration
 * @author Heiko W. Rupp
 */
@Test
public class ConfigurationUpdatingTest extends AbstractConfigurationHandlingTest {

    ObjectMapper mapper ;
    @BeforeSuite
    void loadPluginDescriptor() throws Exception {
        super.loadPluginDescriptor();

        mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    }

    public void test1() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("simple1");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition,connection,null);

        Configuration conf = new Configuration();
        conf.put(new PropertySimple("needed","test"));
        conf.put(new PropertySimple("optional",null));

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 1;
        Operation step1 = cop.step(0);
        assert step1.getOperation().equals("write-attribute");
        Map<String,Object> props = step1.getAdditionalProperties();
        assert props.size()==2;


    }

    public void test2() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("listOfSimple1");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition,connection,null);

        Configuration conf = new Configuration();
        PropertyList propertyList = new PropertyList("foo",
                new PropertySimple("optional","Hello"),
                new PropertySimple("optional",null),
                new PropertySimple("optional","world"));

        conf.put(propertyList);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 1 : "#Steps should be 1 but were " + cop.numberOfSteps();
        Operation step1 = cop.step(0);
        assert step1.getOperation().equals("write-attribute");
        Map<String,Object> props = step1.getAdditionalProperties();
        assert props.size()==2;
        List<String> values = (List<String>) props.get("value");
        assert values.size()==2 : "Values had "+ values.size() + " entries"; // The optional null must not be present



        String result = mapper.writeValueAsString(cop);

    }

    public void test3() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("mapOfSimple1");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition,connection,null);

        Configuration conf = new Configuration();
        PropertyMap propertyMap = new PropertyMap("foo",
                new PropertySimple("needed","Hello"),
                new PropertySimple("optional","world"));

        conf.put(propertyMap);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 1 : "#Steps should be 1 but were " + cop.numberOfSteps();
        Operation step1 = cop.step(0);
        assert step1.getOperation().equals("write-attribute");
        Map<String,Object> props = step1.getAdditionalProperties();
        assert props.size()==2;
        Map<String,Object> values = (Map<String,Object>) props.get("value");
        assert values.size()==2 : "Values had "+ values.size() + " entries instead of 2"; // The optional null must not be present

        String result = mapper.writeValueAsString(cop);

    }
    public void test4() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("mapOfSimple1");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition,connection,null);

        Configuration conf = new Configuration();
        PropertyMap propertyMap = new PropertyMap("foo",
                new PropertySimple("needed","Hello"),
                new PropertySimple("readOnly","world"));

        conf.put(propertyMap);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 1 : "#Steps should be 1 but were " + cop.numberOfSteps();
        Operation step1 = cop.step(0);
        assert step1.getOperation().equals("write-attribute");
        Map<String,Object> props = step1.getAdditionalProperties();
        assert props.size()==2;
        Map<String,Object> values = (Map<String,Object>) props.get("value");
        assert values.size()==1 : "Values had "+ values.size() + " entries instead of 1"; // The optional null must not be present

        String result = mapper.writeValueAsString(cop);

    }

    public void test5() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("mapOfSimple1");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition,connection,null);

        Configuration conf = new Configuration();
        PropertyMap propertyMap = new PropertyMap("foo",
                new PropertySimple("needed","Hello"));

        conf.put(propertyMap);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 1 : "#Steps should be 1 but were " + cop.numberOfSteps();
        Operation step1 = cop.step(0);
        assert step1.getOperation().equals("write-attribute");
        Map<String,Object> props = step1.getAdditionalProperties();
        assert props.size()==2;
        Map<String,Object> values = (Map<String,Object>) props.get("value");
        assert values.size()==1 : "Values had "+ values.size() + " entries instead of 1"; // The optional null must not be present

        String result = mapper.writeValueAsString(cop);

    }

    public void test6() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("listOfMaps1");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition,connection,null);

        Configuration conf = new Configuration();
        PropertyMap propertyMap = new PropertyMap("theMap",
                new PropertySimple("needed","Hello"),
                new PropertySimple("optional","World"));

        PropertyList propertyList = new PropertyList("foo",propertyMap);

        conf.put(propertyList);

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 1 : "#Steps should be 1 but were " + cop.numberOfSteps();
        Operation step1 = cop.step(0);
        assert step1.getOperation().equals("write-attribute");
        Map<String,Object> props = step1.getAdditionalProperties();
        assert props.size()==2;
        List<Map<String,Object>> values = (List<Map<String, Object>>) props.get("value");
        assert values.size()==1 : "Values had "+ values.size() + " entries instead of 1"; // The optional null must not be present
        Map<String,Object> map = values.get(0);
        assert map.size()==2 : "Map had " + map.size() + " entries instead of two";

        String result = mapper.writeValueAsString(cop);

    }

    public void test7() throws Exception {
        ConfigurationDefinition definition = loadDescriptor("SocketBindingGroupStandalone");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition,connection,null);

        Configuration conf = new Configuration();
        PropertyMap propertyMap = new PropertyMap("http");
        propertyMap.put(new PropertySimple("name","http"));
        propertyMap.put(new PropertySimple("port",18080));
        propertyMap.put(new PropertySimple("fixed-port",false));
        PropertyList propertyList = new PropertyList("*");
        propertyList.add(propertyMap);
        conf.put(propertyList);
        conf.put(new PropertySimple("port-offset",0));

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 3 : "#Steps should be 3 but were " + cop.numberOfSteps();
        Operation step1 = cop.step(0);
        Operation step2 = cop.step(1);
        Operation step3 = cop.step(2);

        // As we do not specify a base address when creating the delegate 0 or 1 address element is ok.
        assert step1.getAddress().isEmpty();
        assert step2.getAddress().size()==1;
        assert step3.getAddress().size()==1;

        assert step1.getAdditionalProperties().get("name").equals("port-offset");
        assert step1.getAdditionalProperties().get("value").equals("0");

        assert step2.getAdditionalProperties().get("name").equals("port");
        assert step2.getAdditionalProperties().get("value").equals("18080");

        assert step3.getAdditionalProperties().get("name").equals("fixed-port");
        assert step3.getAdditionalProperties().get("value").equals("false");

        assert step2.getAddress().get(0).equals("socket-binding=http");
        assert step3.getAddress().get(0).equals("socket-binding=http");
    }

    public void test8() throws Exception {
        ConfigurationDefinition definition = loadDescriptor("SocketBindingGroupStandalone");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition,connection,null);

        Configuration conf = new Configuration();
        PropertyList propertyList = new PropertyList("*");
        PropertyMap propertyMap = new PropertyMap("binding");
        propertyMap.put(new PropertySimple("name","http"));
        propertyMap.put(new PropertySimple("port",18080));
        propertyMap.put(new PropertySimple("fixed-port",false));
        propertyList.add(propertyMap);

        propertyMap = new PropertyMap("binding");
        propertyMap.put(new PropertySimple("name","https"));
        propertyMap.put(new PropertySimple("port",18081));
        propertyMap.put(new PropertySimple("fixed-port",false));
        propertyList.add(propertyMap);

        conf.put(propertyList);
        conf.put(new PropertySimple("port-offset",0));

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 5 : "#Steps should be 5 but were " + cop.numberOfSteps();
        Operation step1 = cop.step(0);
        Operation step2 = cop.step(1);
        Operation step3 = cop.step(2);
        Operation step4 = cop.step(3);
        Operation step5 = cop.step(4);

        // As we do not specify a base address when creating the delegate 0 or 1 address element is ok.
        assert step1.getAddress().isEmpty();
        assert step2.getAddress().size()==1;
        assert step3.getAddress().size()==1;

        assert step1.getAdditionalProperties().get("name").equals("port-offset");
        assert step1.getAdditionalProperties().get("value").equals("0");

        assert step2.getAdditionalProperties().get("name").equals("port");
        assert step2.getAdditionalProperties().get("value").equals("18080");

        assert step3.getAdditionalProperties().get("name").equals("fixed-port");
        assert step3.getAdditionalProperties().get("value").equals("false");

        assert step2.getAddress().get(0).equals("socket-binding=http");
        assert step3.getAddress().get(0).equals("socket-binding=http");
        assert step4.getAddress().get(0).equals("socket-binding=https");
        assert step5.getAddress().get(0).equals("socket-binding=https");
    }

    public void test9() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("test9");

        FakeConnection connection = new FakeConnection();

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition,connection,null);

        Configuration conf = new Configuration();

        conf.put(new PropertySimple("default-virtual-server","hulla")); // this is read-only and must not show up in result
        conf.put(new PropertySimple("test-prop","Heiko"));
        conf.put(new PropertySimple("check-interval",23));
        conf.put(new PropertySimple("disabled",true));
        conf.put(new PropertySimple("listings",false));
        conf.put(new PropertySimple("max-depth",17));

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 5 : "#Steps should be 5 but were " + cop.numberOfSteps();

        Operation step1 = cop.step(0);
        Operation step2 = cop.step(1);
        Operation step3 = cop.step(2);
        Operation step4 = cop.step(3);
        Operation step5 = cop.step(4);

        assert step1.getAddress().isEmpty();
        assert step2.getAddress().size()==1;
        assert step3.getAddress().size()==1;
        assert step4.getAddress().size()==1;
        assert step5.getAddress().size()==1;
        assert step2.getAddress().get(0).equals("configuration=jsp-configuration");
        assert step3.getAddress().get(0).equals("configuration=jsp-configuration");
        assert step4.getAddress().get(0).equals("configuration=static-resources");
        assert step5.getAddress().get(0).equals("configuration=static-resources");

        assert step1.getAdditionalProperties().get("name").equals("test-prop");
        assert step1.getAdditionalProperties().get("value").equals("Heiko");
        assert step2.getAdditionalProperties().get("name").equals("check-interval");
        assert step2.getAdditionalProperties().get("value").equals("23");

    }

    public void test10() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("test10");

        FakeConnection connection = new FakeConnection();
        String resultString = loadJsonFromFile("system-props.json");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString,ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        connection.setContent(json);


        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition,connection,null);

        Configuration conf = new Configuration();

        // We have properties 'bar' and 'hello' on the server
        // update 'bar', add 'hulla' and remove 'hello'

        PropertyList propertyList = new PropertyList("*2");
        PropertyMap propertyMap = new PropertyMap("*");
        propertyMap.put(new PropertySimple("name","hulla"));
        propertyMap.put(new PropertySimple("value","hopp"));
        propertyList.add(propertyMap);
        propertyMap = new PropertyMap("*");
        propertyMap.put(new PropertySimple("name","bar"));
        propertyMap.put(new PropertySimple("value","42!"));
        propertyList.add(propertyMap);
        conf.put(propertyList);


        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 3 : "#Steps should be 3 but were " + cop.numberOfSteps();

        Operation step1 = cop.step(0);
        Operation step2 = cop.step(1);
        Operation step3 = cop.step(2);

        assert step1.getAddress().size()==1;
        assert step2.getAddress().size()==1;
        assert step3.getAddress().size()==1;
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

    public void test11() throws Exception {

        ConfigurationDefinition definition = loadDescriptor("test11");

        FakeConnection connection = new FakeConnection();
        String resultString = loadJsonFromFile("system-props.json");

        ObjectMapper mapper = new ObjectMapper();
        ComplexResult result = mapper.readValue(resultString,ComplexResult.class);
        JsonNode json = mapper.valueToTree(result);

        connection.setContent(json);


        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(definition,connection,null);

        Configuration conf = new Configuration();

        // We have properties 'bar' and 'hello' on the server
        // bar has a value of 44
        // update 'bar', add 'hulla' and remove 'hello'

        PropertyList propertyList = new PropertyList("*2");
        PropertyMap propertyMap = new PropertyMap("*");
        // add 'hulla'
        propertyMap.put(new PropertySimple("name","hulla"));
        propertyMap.put(new PropertySimple("value","hopp"));
        propertyList.add(propertyMap);
        propertyMap = new PropertyMap("*");
        // update 'bar' -> needs to trigger a remove + an :add
        propertyMap.put(new PropertySimple("name","bar"));
        propertyMap.put(new PropertySimple("value","42!"));
        propertyList.add(propertyMap);
        conf.put(propertyList);
        // 'hello' is not present -> needs to trigger a :remove for it


        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf, new Address());

        assert cop.numberOfSteps() == 4 : "#Steps should be 4 but were " + cop.numberOfSteps();

        Operation step1 = cop.step(0);
        Operation step2 = cop.step(1);
        Operation step3 = cop.step(2);
        Operation step4 = cop.step(3);

        assert step1.getAddress().size()==1;
        assert step2.getAddress().size()==1;
        assert step3.getAddress().size()==1;
        assert step4.getAddress().size()==1;
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
}
