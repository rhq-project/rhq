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

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.descriptor.configuration.MapProperty;
import org.rhq.core.clientapi.descriptor.configuration.SimpleProperty;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
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
    static {
        try {
            JAXBContext context = JAXBContext.newInstance(StandaloneConfigurationInstance.class);
            CONFIGURATION_INSTANCE_MARSHALLER = context.createMarshaller();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize the configuration instance marshaller.", e);
        }
    }

    public void testSimplePropertyConversion() throws Exception {
        ConfigurationDefinition def = new ConfigurationDefinition(null, null);
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
        ConfigurationDefinition def = new ConfigurationDefinition(null, null);
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
        ConfigurationDefinition def = new ConfigurationDefinition(null, null);
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
