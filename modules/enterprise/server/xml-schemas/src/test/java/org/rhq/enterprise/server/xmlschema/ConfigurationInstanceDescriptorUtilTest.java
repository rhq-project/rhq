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

package org.rhq.enterprise.server.xmlschema;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.descriptor.configuration.MapProperty;
import org.rhq.core.clientapi.descriptor.configuration.SimpleProperty;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.ComplexValueMapDescriptor;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.ComplexValueSimpleDescriptor;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.ConfigurationInstanceDescriptor;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.ListPropertyInstanceDescriptor;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.SimplePropertyInstanceDescriptor;

/**
 *
 *
 * @author Lukas Krejci
 */
@Test
public class ConfigurationInstanceDescriptorUtilTest {
    private static final Log LOG = LogFactory.getLog(ConfigurationInstanceDescriptorUtilTest.class);

    @XmlRootElement
    public static final class StandaloneConfigurationInstance extends ConfigurationInstanceDescriptor {
        public static StandaloneConfigurationInstance createFrom(ConfigurationInstanceDescriptor instance) {
            StandaloneConfigurationInstance ret = new StandaloneConfigurationInstance();
            ret.configurationProperty = new ArrayList<JAXBElement<?>>(instance.getConfigurationProperty());

            return ret;
        }
    }

    private static final Marshaller CONFIGURATION_INSTANCE_MARSHALLER;
    private static final Unmarshaller CONFIGURATION_INSTANCE_UNMARSHALLER;
    static {
        try {
            JAXBContext context = JAXBContext.newInstance(StandaloneConfigurationInstance.class);
            CONFIGURATION_INSTANCE_MARSHALLER = context.createMarshaller();
            CONFIGURATION_INSTANCE_UNMARSHALLER = context.createUnmarshaller();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize the configuration instance marshaller.", e);
        }
    }

    public void testSimplePropertyConversion() throws Exception {
        ConfigurationDefinition def = new ConfigurationDefinition("null", null);
        PropertyDefinitionSimple propDef =
            new PropertyDefinitionSimple("prop", "prop descr", true, PropertySimpleType.BOOLEAN);
        def.put(propDef);

        Configuration config = new Configuration();
        PropertySimple prop = new PropertySimple("prop", "true");
        config.put(prop);

        ConfigurationInstanceDescriptor instance =
            ConfigurationInstanceDescriptorUtil.createConfigurationInstance(def, config);

        assertEquals(instance.getConfigurationProperty().size(), 1,
            "Unexpected number of properties in the config instance.");

        SimplePropertyInstanceDescriptor propInstance =
            (SimplePropertyInstanceDescriptor) instance.getConfigurationProperty().get(0).getValue();

        assertEquals(propInstance.getName(), "prop", "Unexpected property instance name");
        assertEquals(propInstance.getValue(), "true", "Unexpected property instance value");
        assertEquals(propInstance.getLongDescription(), "prop descr", "Unexpected property instance description");
        assertTrue(propInstance.isRequired(), "Unexpected property instance required flag");

        logInstance("Simple", instance);
    }

    public void testListOfSimplesPropertyConversion() throws Exception {
        ConfigurationDefinition def = new ConfigurationDefinition("null", null);
        PropertyDefinitionList listDef =
            new PropertyDefinitionList("list", "list descr", true, new PropertyDefinitionSimple("prop", "prop descr",
                false, PropertySimpleType.FLOAT));
        def.put(listDef);

        Configuration config = new Configuration();
        PropertyList list =
            new PropertyList("list", new PropertySimple("prop", "value1"), new PropertySimple("prop", "value2"));
        config.put(list);

        ConfigurationInstanceDescriptor instance =
            ConfigurationInstanceDescriptorUtil.createConfigurationInstance(def, config);

        assertEquals(instance.getConfigurationProperty().size(), 1,
            "Unexpected number of properties in configuration instance.");

        ListPropertyInstanceDescriptor listInstance =
            (ListPropertyInstanceDescriptor) instance.getConfigurationProperty().get(0).getValue();

        assertEquals(listInstance.getName(), "list", "Unexpected list instance name");
        assertEquals(listInstance.getLongDescription(), "list descr", "Unexpected list instance description");
        assertTrue(listInstance.isRequired(), "Unexpected list instance required flag");

        SimpleProperty propDef = (SimpleProperty) listInstance.getConfigurationProperty().getValue();
        assertEquals(propDef.getName(), "prop", "Unexpected simple instance name");
        assertEquals(propDef.getLongDescription(), "prop descr", "Unexpected simple instance description");
        assertTrue(!propDef.isRequired(), "Unexpected simple instance required flag");

        assertEquals(listInstance.getValues().getComplexValue().size(), 2, "Unexpected number of list values");

        ComplexValueSimpleDescriptor value1 =
            (ComplexValueSimpleDescriptor) listInstance.getValues().getComplexValue().get(0).getValue();
        ComplexValueSimpleDescriptor value2 =
            (ComplexValueSimpleDescriptor) listInstance.getValues().getComplexValue().get(1).getValue();

        assertEquals(value1.getValue(), "value1");
        assertEquals(value2.getValue(), "value2");

        logInstance("List of simples", instance);
    }

    public void testListOfMapsPropertyConversion() throws Exception {
        ConfigurationDefinition def = new ConfigurationDefinition("null", null);
        PropertyDefinitionList listDef =
            new PropertyDefinitionList("list", "list descr", true, new PropertyDefinitionMap("map", "map descr", true,
                new PropertyDefinitionSimple("prop1", "prop1 descr", true, PropertySimpleType.BOOLEAN),
                new PropertyDefinitionSimple("prop2", "prop2 descr", false, PropertySimpleType.PASSWORD)));
        def.put(listDef);

        Configuration config = new Configuration();
        PropertyList list =
            new PropertyList("list", new PropertyMap("map", new PropertySimple("prop1", "value1"), new PropertySimple(
                "prop2", "value1")), new PropertyMap("map", new PropertySimple("prop1", "value2"), new PropertySimple(
                "prop2", "value2")));
        config.put(list);

        ConfigurationInstanceDescriptor instance =
            ConfigurationInstanceDescriptorUtil.createConfigurationInstance(def, config);

        assertEquals(instance.getConfigurationProperty().size(), 1,
            "Unexpected number of properties in configuration instance.");

        ListPropertyInstanceDescriptor listInstance =
            (ListPropertyInstanceDescriptor) instance.getConfigurationProperty().get(0).getValue();

        assertEquals(listInstance.getName(), "list", "Unexpected list instance name");
        assertEquals(listInstance.getLongDescription(), "list descr", "Unexpected list instance description");
        assertTrue(listInstance.isRequired(), "Unexpected list instance required flag");

        MapProperty propDef = (MapProperty) listInstance.getConfigurationProperty().getValue();
        assertEquals(propDef.getName(), "map", "Unexpected simple instance name");
        assertEquals(propDef.getLongDescription(), "map descr", "Unexpected simple instance description");
        assertTrue(propDef.isRequired(), "Unexpected simple instance required flag");

        assertEquals(listInstance.getValues().getComplexValue().size(), 2, "Unexpected number of list values");

        ComplexValueMapDescriptor map1 =
            (ComplexValueMapDescriptor) listInstance.getValues().getComplexValue().get(0).getValue();
        ComplexValueMapDescriptor map2 =
            (ComplexValueMapDescriptor) listInstance.getValues().getComplexValue().get(1).getValue();

        assertEquals(map1.getComplexValue().size(), 2, "Unexpected number of map elements in the first map value.");
        assertEquals(map2.getComplexValue().size(), 2, "Unexpected number of map elements in the second map value.");

        ComplexValueSimpleDescriptor value11 = (ComplexValueSimpleDescriptor) map1.getComplexValue().get(0).getValue();
        ComplexValueSimpleDescriptor value12 = (ComplexValueSimpleDescriptor) map1.getComplexValue().get(1).getValue();
        ComplexValueSimpleDescriptor value21 = (ComplexValueSimpleDescriptor) map2.getComplexValue().get(0).getValue();
        ComplexValueSimpleDescriptor value22 = (ComplexValueSimpleDescriptor) map2.getComplexValue().get(1).getValue();

        assertEquals(value11.getPropertyName(), "prop1", "Unexpected name of the first property in the first map value");
        assertEquals(value11.getValue(), "value1", "Unexpected value of the first property in the first map value");
        assertEquals(value12.getPropertyName(), "prop2", "Unexpected name of the second property in the first map value");
        assertEquals(value12.getValue(), "value1", "Unexpected value of the second property in the first map value");
        assertEquals(value21.getPropertyName(), "prop1", "Unexpected name of the first property in the second map value");
        assertEquals(value21.getValue(), "value2", "Unexpected value of the first property in the second map value");
        assertEquals(value22.getPropertyName(), "prop2", "Unexpected name of the second property in the second map value");
        assertEquals(value22.getValue(), "value2", "Unexpected value of the second property in the second map value");

        logInstance("List of maps", instance);
    }

    public void testMapOfComplexPropertyConversion() {
        //TODO implement
    }

    public void testReverseSimplePropertyConversion() throws Exception {
        String xml = "" +
            "<standaloneConfigurationInstance xmlns:ci='urn:xmlns:rhq-configuration-instance' xmlns:c='urn:xmlns:rhq-configuration'>" +
            "    <ci:simple-property value='42' name='my-name' type='integer'/>" +
            "</standaloneConfigurationInstance>";

        ConfigurationInstanceDescriptor descriptor = (ConfigurationInstanceDescriptor) CONFIGURATION_INSTANCE_UNMARSHALLER.unmarshal(new StringReader(xml));

        ConfigurationInstanceDescriptorUtil.ConfigurationAndDefinition ccd = ConfigurationInstanceDescriptorUtil.createConfigurationAndDefinition(descriptor);

        ConfigurationDefinition def = ccd.definition;
        Configuration conf = ccd.configuration;

        assertEquals(def.getPropertyDefinitions().size(), 1, "Unexpected number of defined properties");
        assertEquals(conf.getProperties().size(), 1, "Unexpected number of properties");

        PropertyDefinition propDef = def.get("my-name");
        Property prop = conf.get("my-name");

        assertNotNull(propDef, "Could not find the expected property definition");
        assertNotNull(prop, "Could not find the expected property");

        assertEquals(propDef.getClass(), PropertyDefinitionSimple.class, "Unexpected type of the property definition");
        assertEquals(prop.getClass(), PropertySimple.class, "Unexpecetd type of the property");

        PropertyDefinitionSimple simpleDef = (PropertyDefinitionSimple) propDef;
        PropertySimple simpleProp = (PropertySimple) prop;

        assertEquals(simpleDef.getType(), PropertySimpleType.INTEGER, "Unexpected type of the simple property definition");
        assertEquals(simpleProp.getIntegerValue(), Integer.valueOf(42), "Unexpected value of the simple property");
    }

    public void testReverseListPropertyConversion() throws Exception {
        String xml = "" +
        "<standaloneConfigurationInstance xmlns:ci='urn:xmlns:rhq-configuration-instance' xmlns:c='urn:xmlns:rhq-configuration'>" +
        "    <ci:list-property name='list'>" +
        "        <c:simple-property name='member' type='integer'/>" +
        "        <ci:values>" +
        "            <ci:simple-value value='1'/>" +
        "            <ci:simple-value value='2'/>" +
        "            <ci:simple-value value='3'/>" +
        "        </ci:values>" +
        "    </ci:list-property>" +
        "</standaloneConfigurationInstance>";

        ConfigurationInstanceDescriptor descriptor = (ConfigurationInstanceDescriptor) CONFIGURATION_INSTANCE_UNMARSHALLER.unmarshal(new StringReader(xml));

        ConfigurationInstanceDescriptorUtil.ConfigurationAndDefinition ccd = ConfigurationInstanceDescriptorUtil.createConfigurationAndDefinition(descriptor);

        ConfigurationDefinition def = ccd.definition;
        Configuration conf = ccd.configuration;

        assertEquals(def.getPropertyDefinitions().size(), 1, "Unexpected number of defined properties");
        assertEquals(conf.getProperties().size(), 1, "Unexpected number of properties");

        PropertyDefinition propDef = def.get("list");
        Property prop = conf.get("list");

        assertNotNull(propDef, "Could not find the expected property definition");
        assertNotNull(prop, "Could not find the expected property");

        assertEquals(propDef.getClass(), PropertyDefinitionList.class, "Unexpected type of the property definition");
        assertEquals(prop.getClass(), PropertyList.class, "Unexpecetd type of the property");

        PropertyDefinitionList listDef = (PropertyDefinitionList) propDef;
        PropertyList listProp = (PropertyList) prop;

        PropertyDefinition memberDef = listDef.getMemberDefinition();
        assertEquals(memberDef.getClass(), PropertyDefinitionSimple.class, "Unexpected type of the list member property definition");

        PropertyDefinitionSimple memberSimpleDef = (PropertyDefinitionSimple) memberDef;
        assertEquals(memberSimpleDef.getName(), "member");
        assertEquals(memberSimpleDef.getType(), PropertySimpleType.INTEGER);

        assertEquals(listProp.getList().size(), 3, "Unexpected number of list members");

        for(int i = 0; i < 3; ++i) {
            Property memberProp = listProp.getList().get(i);
            assertEquals(memberProp.getClass(), PropertySimple.class, "Unexpected type of the property in the list on index " + i);
            assertEquals(memberProp.getName(), "member");
            assertEquals(((PropertySimple)memberProp).getIntegerValue(), Integer.valueOf(i + 1));
        }
    }

    public void testReverseMapPropertyConversion() throws Exception {
        String xml = "" +
        "<standaloneConfigurationInstance xmlns:ci='urn:xmlns:rhq-configuration-instance' xmlns:c='urn:xmlns:rhq-configuration'>" +
        "    <ci:map-property name='map'>" +
        "        <c:simple-property name='m1' type='integer'/>" +
        "        <c:simple-property name='m2' type='string'/>" +
        "        <ci:values>" +
        "            <ci:simple-value property-name='m1' value='1'/>" +
        "            <ci:simple-value property-name='m2' value='v'/>" +
        "        </ci:values>" +
        "    </ci:map-property>" +
        "</standaloneConfigurationInstance>";

        ConfigurationInstanceDescriptor descriptor = (ConfigurationInstanceDescriptor) CONFIGURATION_INSTANCE_UNMARSHALLER.unmarshal(new StringReader(xml));

        ConfigurationInstanceDescriptorUtil.ConfigurationAndDefinition ccd = ConfigurationInstanceDescriptorUtil.createConfigurationAndDefinition(descriptor);

        ConfigurationDefinition def = ccd.definition;
        Configuration conf = ccd.configuration;

        assertEquals(def.getPropertyDefinitions().size(), 1, "Unexpected number of defined properties");
        assertEquals(conf.getProperties().size(), 1, "Unexpected number of properties");

        PropertyDefinition propDef = def.get("map");
        Property prop = conf.get("map");

        assertNotNull(propDef, "Could not find the expected property definition");
        assertNotNull(prop, "Could not find the expected property");

        assertEquals(propDef.getClass(), PropertyDefinitionMap.class, "Unexpected type of the property definition");
        assertEquals(prop.getClass(), PropertyMap.class, "Unexpecetd type of the property");

        PropertyDefinitionMap mapDef = (PropertyDefinitionMap) propDef;
        PropertyMap mapProp = (PropertyMap) prop;

        assertEquals(mapDef.getMap().size(), 2, "Unexpected number of map member definitions");
        assertEquals(mapProp.getMap().size(), 2, "Unexpected number of map members");

        PropertyDefinition m1Def = mapDef.get("m1");
        PropertyDefinition m2Def = mapDef.get("m2");
        Property m1Prop = mapProp.get("m1");
        Property m2Prop = mapProp.get("m2");

        assertEquals(m1Def.getClass(), PropertyDefinitionSimple.class);
        assertEquals(m2Def.getClass(), PropertyDefinitionSimple.class);
        assertEquals(m1Prop.getClass(), PropertySimple.class);
        assertEquals(m2Prop.getClass(), PropertySimple.class);

        PropertyDefinitionSimple m1SimpleDef = (PropertyDefinitionSimple) m1Def;
        PropertyDefinitionSimple m2SimpleDef = (PropertyDefinitionSimple) m2Def;
        PropertySimple m1SimpleProp = (PropertySimple) m1Prop;
        PropertySimple m2SimpleProp = (PropertySimple) m2Prop;

        assertEquals(m1SimpleDef.getName(), "m1");
        assertEquals(m2SimpleDef.getName(), "m2");
        assertEquals(m1SimpleDef.getType(), PropertySimpleType.INTEGER);
        assertEquals(m2SimpleDef.getType(), PropertySimpleType.STRING);

        assertEquals(m1SimpleProp.getName(), "m1");
        assertEquals(m2SimpleProp.getName(), "m2");
        assertEquals(m1SimpleProp.getIntegerValue(), Integer.valueOf(1));
        assertEquals(m2SimpleProp.getStringValue(), "v");
    }

    public void testReverseListOfMapsConversion() throws Exception {
        String xml = "" +
        "<standaloneConfigurationInstance xmlns:ci='urn:xmlns:rhq-configuration-instance' xmlns:c='urn:xmlns:rhq-configuration'>" +
        "    <ci:list-property name='list'>" +
        "        <c:map-property name='map'>" +
        "            <c:simple-property name='m1' type='integer'/>" +
        "            <c:simple-property name='m2' type='string'/>" +
        "        </c:map-property>" +
        "        <ci:values>" +
        "            <ci:map-value>" +
        "              <ci:simple-value property-name='m1' value='1'/>" +
        "              <ci:simple-value property-name='m2' value='m1'/>" +
        "            </ci:map-value>" +
        "            <ci:map-value>" +
        "              <ci:simple-value property-name='m1' value='2'/>" +
        "              <ci:simple-value property-name='m2' value='m2'/>" +
        "            </ci:map-value>" +
        "        </ci:values>" +
        "    </ci:list-property>" +
        "</standaloneConfigurationInstance>";

        ConfigurationInstanceDescriptor descriptor = (ConfigurationInstanceDescriptor) CONFIGURATION_INSTANCE_UNMARSHALLER.unmarshal(new StringReader(xml));

        ConfigurationInstanceDescriptorUtil.ConfigurationAndDefinition ccd = ConfigurationInstanceDescriptorUtil.createConfigurationAndDefinition(descriptor);

        ConfigurationDefinition def = ccd.definition;
        Configuration conf = ccd.configuration;

        assertEquals(def.getPropertyDefinitions().size(), 1, "Unexpected number of defined properties");
        assertEquals(conf.getProperties().size(), 1, "Unexpected number of properties");

        PropertyDefinitionList listDef = (PropertyDefinitionList) def.get("list");
        PropertyList listProp = (PropertyList) conf.get("list");

        PropertyDefinitionMap mapDef = (PropertyDefinitionMap) listDef.getMemberDefinition();
        PropertyDefinitionSimple m1Def = (PropertyDefinitionSimple) mapDef.get("m1");
        PropertyDefinitionSimple m2Def = (PropertyDefinitionSimple) mapDef.get("m2");

        assertEquals(mapDef.getName(), "map");
        assertEquals(m1Def.getType(), PropertySimpleType.INTEGER);
        assertEquals(m2Def.getType(), PropertySimpleType.STRING);

        assertEquals(listProp.getList().size(), 2, "Unexpected number of maps in the list");

        PropertyMap firstMapValue = (PropertyMap) listProp.getList().get(0);
        PropertyMap secondMapValue = (PropertyMap) listProp.getList().get(1);

        assertEquals(firstMapValue.getName(), "map");
        assertEquals(secondMapValue.getName(), "map");

        assertEquals(firstMapValue.getSimpleValue("m1", null), "1", "Unexpected value of m1 property in the first map.");
        assertEquals(firstMapValue.getSimpleValue("m2", null), "m1", "Unexpected value of m2 property in the first map.");

        assertEquals(secondMapValue.getSimpleValue("m1", null), "2", "Unexpected value of m1 property in the second map.");
        assertEquals(secondMapValue.getSimpleValue("m2", null), "m2", "Unexpected value of m2 property in the second map.");
    }

    private static void logInstance(String message, ConfigurationInstanceDescriptor instance) throws JAXBException,
        IOException {
        StringWriter wrt = new StringWriter();
        try {
            CONFIGURATION_INSTANCE_MARSHALLER.marshal(StandaloneConfigurationInstance.createFrom(instance), wrt);
            LOG.debug(message + "\n" + wrt.toString());
        } finally {
            wrt.close();
        }
    }
}
