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

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
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

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf);

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

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf);

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

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf);

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

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf);

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

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf);

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

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf);

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

        CompositeOperation cop = delegate.updateGenerateOperationFromProperties(conf);

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

        assert step2.getAddress().get(0).getKey().equals("socket-binding");
        assert step2.getAddress().get(0).getValue().equals("http");
        assert step3.getAddress().get(0).getKey().equals("socket-binding");
        assert step3.getAddress().get(0).getValue().equals("http");
    }
}
