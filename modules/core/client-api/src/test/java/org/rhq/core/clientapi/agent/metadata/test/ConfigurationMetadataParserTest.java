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
package org.rhq.core.clientapi.agent.metadata.test;

import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.clientapi.agent.metadata.InvalidPluginDescriptorException;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ActivationPolicy;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.definition.constraint.Constraint;
import org.rhq.core.domain.configuration.definition.constraint.FloatRangeConstraint;
import org.rhq.core.domain.configuration.definition.constraint.IntegerRangeConstraint;

/**
 * @author Jason Dobies
 */
public class ConfigurationMetadataParserTest {
    private static final Log LOG = LogFactory.getLog(ConfigurationMetadataParserTest.class);
    private static final String DESCRIPTOR_FILENAME = "test1-plugin.xml";

    private PluginDescriptor pluginDescriptor;

    @BeforeSuite
    public void loadPluginDescriptor() throws Exception {
        try {
            URL descriptorUrl = this.getClass().getClassLoader().getResource(DESCRIPTOR_FILENAME);
            LOG.info("Loading plugin descriptor at: " + descriptorUrl);

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

    @Test
    public void parseValidDefinitionServer1() throws InvalidPluginDescriptorException {
        ConfigurationDefinition definition = loadDescriptor("testServer1");
        Configuration defaultTemplate = definition.getDefaultTemplate().getConfiguration();

        assert definition != null : "Definition was returned as null from parser";

        assertSimplePropertyType(definition, "serverProperty2", PropertySimpleType.BOOLEAN);
        assertSimplePropertyType(definition, "serverProperty3", PropertySimpleType.DIRECTORY);
        assertSimplePropertyType(definition, "serverProperty4", PropertySimpleType.FILE);
        assertSimplePropertyType(definition, "serverProperty5", PropertySimpleType.FLOAT);
        assertSimplePropertyType(definition, "serverProperty5a", PropertySimpleType.DOUBLE);
        assertSimplePropertyType(definition, "serverProperty6", PropertySimpleType.INTEGER);
        assertSimplePropertyType(definition, "serverProperty6a", PropertySimpleType.LONG);
        assertSimplePropertyType(definition, "serverProperty7", PropertySimpleType.LONG_STRING);
        assertSimplePropertyType(definition, "serverProperty8", PropertySimpleType.PASSWORD);
        assertSimplePropertyType(definition, "serverProperty9", PropertySimpleType.STRING);

        assertSimpleActivationPolicy(definition, "serverProperty1", ActivationPolicy.IMMEDIATE);
        assertSimpleActivationPolicy(definition, "serverProperty10", ActivationPolicy.IMMEDIATE);
        assertSimpleActivationPolicy(definition, "serverProperty11", ActivationPolicy.RESTART);
        assertSimpleActivationPolicy(definition, "serverProperty12", ActivationPolicy.SHUTDOWN);

        PropertyDefinitionSimple simple;
        PropertySimple defaultValue;

        simple = definition.getPropertyDefinitionSimple("serverProperty15");
        assert simple != null : "serverProperty15 was not loaded";
        defaultValue = defaultTemplate.getSimple("serverProperty15");
        assert defaultValue != null : "serverProperty15 default value was not loaded";
        assert "Default String".equals(defaultValue.getStringValue()) : "serverProperty15 default value was incorrect";

        simple = definition.getPropertyDefinitionSimple("serverProperty16");
        assert simple != null : "serverProperty16 was not loaded";
        defaultValue = defaultTemplate.getSimple("serverProperty16");
        assert defaultValue != null : "serverProperty16 default value was not loaded";
        assert defaultValue.getIntegerValue() == 5 : "serverProperty16 default value was incorrect";

        simple = definition.getPropertyDefinitionSimple("serverProperty20");
        assert simple != null : "serverProperty20 was not loaded";
        assert "Test Description".equals(simple.getDescription()) : "serverProperty20 description was not loaded";
        assert "Server Property".equals(simple.getDisplayName()) : "serverProperty20 display name was not loaded";

        // TODO: jdobies, Jan 9, 2007: Units are not in domain model, does Greg want to delete them?

        simple = definition.getPropertyDefinitionSimple("serverProperty21");
        assert simple != null : "serverProperty21 was not loaded";
        assert "External Description".equals(simple.getDescription()) : "serverProperty21 description was not loaded";
        assert "serverProperty21".equals(simple.getName()) : "serverProperty21 name not as expected ["
            + simple.getName() + "]";
        assert "Server Property 21".equals(simple.getDisplayName()) : "serverProperty21 display name not as expected ["
            + simple.getDisplayName() + "]";

        simple = definition.getPropertyDefinitionSimple("serverProperty22");
        assert simple != null : "serverProperty22 was not loaded";
        assert "Internal Description".equals(simple.getDescription()) : "serverProperty22 description loaded from external instead of internal";

        simple = definition.getPropertyDefinitionSimple("serverProperty23");
        assert simple != null : "serverProperty23 was not loaded";
        assert "Internal Default Value".equals(simple.getDefaultValue()) : "serverProperty23 default value description loaded from external instead of internal";

        simple = definition.getPropertyDefinitionSimple("serverProperty25");
        assert simple != null : "serverProperty25 was not loaded";
        assert !simple.isRequired() : "serverProperty25 is incorrectly read as required";
        // TODO: jdobies, Jan 9, 2007: There is no tracking of read only in the domain model

        simple = definition.getPropertyDefinitionSimple("serverProperty26");
        assert simple != null : "serverProperty26 was not loaded";
        assert simple.isRequired() : "serverProperty26 is incorrectly read as not required";

        simple = definition.getPropertyDefinitionSimple("serverProperty31");
        assert simple != null : "serverProperty31 was not loaded";
        assert simple.getConstraints() != null : "serverProperty31 had no constraints loaded";
        assert simple.getConstraints().size() == 3 : "serverProperty31 had an incorrect number of constraints loaded";

        boolean noMaxRangeFound = false;
        boolean noMinRangeFound = false;
        boolean bothRangeFound = false;

        for (Constraint c : simple.getConstraints()) {
            assert c instanceof IntegerRangeConstraint : "serverProperty31 loaded with invalid constraint of class: "
                + c.getClass();
            IntegerRangeConstraint ic = (IntegerRangeConstraint) c;
            if ((ic.getMinimum() != null) && (ic.getMaximum() == null)) {
                assert ic.getMinimum().compareTo((long) 0) == 0 : "serverProperty31 minimum integer range read incorrectly with no maximum range";
                noMaxRangeFound = true;
            } else if ((ic.getMinimum() == null) && (ic.getMaximum() != null)) {
                assert ic.getMaximum().compareTo((long) 100) == 0 : "serverProperty31 maximum integer range read incorrectly with no minimum range";
                noMinRangeFound = true;
            } else if ((ic.getMinimum() != null) && (ic.getMaximum() != null)) {
                assert ic.getMinimum().compareTo((long) 20) == 0 : "serverProperty31 minimum integer range read incorrectly with maximum range";
                assert ic.getMaximum().compareTo((long) 80) == 0 : "serverProperty31 maximum integer range read incorrectly with minimum range";
                bothRangeFound = true;
            } else {
                assert false : "serverProperty31 loaded with unexpected integer range";
            }
        }

        assert noMaxRangeFound : "serverProperty31 loaded without no maximum range constraint";
        assert noMinRangeFound : "serverProperty31 loaded without no minimum range constraint";
        assert bothRangeFound : "serverProperty31 loaded without both ranges constraint";

        simple = definition.getPropertyDefinitionSimple("serverProperty32");
        assert simple != null : "serverProperty32 was not loaded";
        assert simple.getConstraints().size() == 3 : "serverProperty32 had an incorrect number of constraints loaded";

        noMaxRangeFound = false;
        noMinRangeFound = false;
        bothRangeFound = false;

        for (Constraint c : simple.getConstraints()) {
            assert c instanceof FloatRangeConstraint : "serverProperty32 loaded with invalid constraint of class: "
                + c.getClass();
            FloatRangeConstraint fc = (FloatRangeConstraint) c;
            if ((fc.getMinimum() != null) && (fc.getMaximum() == null)) {
                assert fc.getMinimum().compareTo(new Double("0.5")) == 0 : "serverProperty32 minimum float range read incorrectly with no maximum range";
                noMaxRangeFound = true;
            } else if ((fc.getMinimum() == null) && (fc.getMaximum() != null)) {
                assert fc.getMaximum().compareTo(new Double("99.9")) == 0 : "serverProperty32 maximum float range read incorrectly with no minimum range";
                noMinRangeFound = true;
            } else if ((fc.getMinimum() != null) && (fc.getMaximum() != null)) {
                assert fc.getMinimum().compareTo(new Double("20.2")) == 0 : "serverProperty32 minimum float range read incorrectly with maximum range";
                assert fc.getMaximum().compareTo(new Double("80.8")) == 0 : "serverProperty32 maximum float range read incorrectly with minimum range";
                bothRangeFound = true;
            } else {
                assert false : "serverProperty32 loaded with unexpected float range";
            }
        }

        assert noMaxRangeFound : "serverProperty32 loaded without no maximum range constraint";
        assert noMinRangeFound : "serverProperty32 loaded without no minimum range constraint";
        assert bothRangeFound : "serverProperty32 loaded without both ranges constraint";

        PropertyDefinitionList list;

        list = definition.getPropertyDefinitionList("serverProperty40");
        assert list != null : "serverProperty40 was not loaded";
        assert "serverProperty40".equals(list.getName());
        assert list.getMemberDefinition() == null : "serverProperty40 incorrectly loaded with member definition";

        list = definition.getPropertyDefinitionList("serverProperty41");
        assert list != null : "serverProperty41 was not loaded";
        assert list.getMemberDefinition() != null : "serverProperty41 member definition was not loaded";
        simple = (PropertyDefinitionSimple) list.getMemberDefinition();
        assert simple.getType() == PropertySimpleType.INTEGER : "serverProperty41 members type is incorrect";

        list = definition.getPropertyDefinitionList("serverProperty42");
        assert list != null : "serverProperty42 was not loaded";
        assert "List Description".equals(list.getDescription()) : "serverProperty42 description is incorrect";
        assert "List Property".equals(list.getDisplayName()) : "serverProperty42 display name is incorrect";

        list = definition.getPropertyDefinitionList("serverProperty43");
        assert list != null : "serverProperty43 was not loaded";
        assert "External Description".equals(list.getDescription()) : "serverProperty43 description is incorrect";
        assert "Server Property 43".equals(list.getDisplayName()) : "serverProperty43 display name is incorrect";

        list = definition.getPropertyDefinitionList("serverProperty44");
        assert list != null : "serverProperty44 was not loaded";
        assert "Internal Description".equals(list.getDescription()) : "serverProperty44 description loaded from external instead of internal";

        PropertyDefinitionMap map;
        Map<String, PropertyDefinition> nestedProperties;

        map = definition.getPropertyDefinitionMap("serverProperty50");
        assert map != null : "serverProperty50 was not loaded";
        assert map.get("invalidProperty") == null : "serverProperty50 returned an invalid nested property";

        map = definition.getPropertyDefinitionMap("serverProperty51");
        assert map != null : "serverProperty51 was not loaded";
        nestedProperties = map.getPropertyDefinitions();
        assert nestedProperties != null : "serverProperty51 did not have any nested properties";
        assert nestedProperties.size() == 3 : "serverProperty51 did not contain the correct number of nested properties";
        assert nestedProperties.get("simpleProperty51") instanceof PropertyDefinitionSimple : "serverProperty51 nested simple property was incorrect";
        assert nestedProperties.get("listProperty51") instanceof PropertyDefinitionList : "serverProperty51 nested list property was incorrect";
        assert nestedProperties.get("mapProperty51") instanceof PropertyDefinitionMap : "serverProperty51 nested map property was incorrect";

        map = definition.getPropertyDefinitionMap("serverProperty52");
        assert map != null : "serverProperty52 was not loaded";
        assert "Map Description".equals(map.getDescription()) : "serverProperty52 description is incorrect";
        assert "Map Property".equals(map.getDisplayName()) : "serverProperty52 display name is incorrect";

        map = definition.getPropertyDefinitionMap("serverProperty53");
        assert map != null : "serverProperty53 was not loaded";
        assert "External Description".equals(map.getDescription()) : "serverProperty53 description is incorrect";
        assert "Server Property 53".equals(map.getDisplayName()) : "serverProperty53 displayName is incorrect";

        map = definition.getPropertyDefinitionMap("serverProperty54");
        assert map != null : "serverProperty54 was not loaded";
        assert "Internal Description".equals(map.getDescription()) : "serverProperty54 description loaded from external instead of internal";

        simple = definition.getPropertyDefinitionSimple("myJDBCAcronymProperty");
        assert "My JDBC Acronym Property".equals(simple.getDisplayName()) : "myJDBCAcronymProperty display name not as expected ["
            + simple.getDisplayName() + "]";

        simple = definition.getPropertyDefinitionSimple("myJDBC33Property");
        assert "My JDBC 33 Property".equals(simple.getDisplayName()) : "myJDBC33Property display name not as expected ["
            + simple.getDisplayName() + "]";
    }

    private void assertSimplePropertyType(ConfigurationDefinition definition, String propertyName,
        PropertySimpleType type) {
        PropertyDefinitionSimple simple = definition.getPropertyDefinitionSimple(propertyName);
        assert simple != null : propertyName + " was not loaded";
        assert simple.getType() == type : propertyName + " was read with incorrect type";
        assert simple.getName().equals(propertyName) : propertyName + " was read with no name";
    }

    private void assertSimpleActivationPolicy(ConfigurationDefinition definition, String propertyName,
        ActivationPolicy policy) {
        PropertyDefinitionSimple simple = definition.getPropertyDefinitionSimple(propertyName);
        assert simple != null : propertyName + " was not loaded";
        assert simple.getActivationPolicy() == policy : propertyName + " was read with incorrect activation policy";
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
}