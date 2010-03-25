/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.server.configuration.metadata;

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.configuration.ConfigurationDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.resource.metadata.test.UpdateSubsytemTestBase;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.test.AssertUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.FileNotFoundException;
import java.net.URL;

public class ConfigurationMetadataManagerBeanTest extends AbstractEJB3Test {

    ConfigurationMetadataManagerLocal configurationMetadataMgr;

    EntityManager entityMgr;

    ConfigurationDefinition originalConfigurationDef;

    ConfigurationDefinition newConfigurationDef;

    @BeforeClass
    public void setupClass() throws Exception {
        String pluginFileBaseName = "configuration_metadata_manager_bean_test";
        String version1 = pluginFileBaseName + "_v1.xml";
        String version2 = pluginFileBaseName + "_v2.xml";

        getTransactionManager().begin();
        entityMgr = getEntityManager();

        configurationMetadataMgr = LookupUtil.getConfigurationMetadataManager();

        originalConfigurationDef = createAndSaveConfigurationDef(version1);
        newConfigurationDef = loadPluginConfigurationFromFile(version2);

        configurationMetadataMgr.updateConfigurationDefinition(newConfigurationDef, originalConfigurationDef);
    }

    @AfterClass
    public void tearDownClass() throws Exception {
        getTransactionManager().rollback();
    }

    private ConfigurationDefinition createAndSaveConfigurationDef(String file) throws Exception {
        ConfigurationDefinition configurationDef = loadPluginConfigurationFromFile(file);

        entityMgr.persist(configurationDef);
        entityMgr.flush();
        entityMgr.clear();

        return configurationDef;
    }

    private ConfigurationDefinition loadPluginConfigurationFromFile(String file) throws Exception {
        PluginDescriptor pluginDescriptor = loadPluginDescriptor(file);
        ConfigurationDescriptor configurationDescriptor = pluginDescriptor.getServers().get(0).getPluginConfiguration();
        return ConfigurationMetadataParser.parse("test", configurationDescriptor);
    }

    private PluginDescriptor loadPluginDescriptor(String file) throws Exception {
        URL pluginDescriptorURL = getClass().getResource(file);
        if (pluginDescriptorURL == null) {
            throw new FileNotFoundException("File " + file + " not found");
        }

        JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);
        URL pluginSchemaURL = this.getClass().getClassLoader().getResource("rhq-plugin.xsd");
        Schema pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(pluginSchemaURL);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ValidationEventCollector vec = new ValidationEventCollector();
        unmarshaller.setEventHandler(vec);
        unmarshaller.setSchema(pluginSchema);

        return (PluginDescriptor) unmarshaller.unmarshal(pluginDescriptorURL.openStream());
    }

    void assertPropertyDefinitionMatches(String msg, PropertyDefinitionSimple expected,
            PropertyDefinitionSimple actual) {
        AssertUtils.assertPropertiesMatch(expected, actual, msg);
    }

    @Test
    public void newUngroupedPropertyDefsShouldBeAddedToConfigurationDef() throws Exception {
        PropertyDefinitionSimple expected = newConfigurationDef.getPropertyDefinitionSimple("bar");
        PropertyDefinitionSimple actual = originalConfigurationDef.getPropertyDefinitionSimple("bar");

        assertPropertyDefinitionMatches("New ungrouped property defs shoould be added to the configuration definition",
            expected, actual);
    }

    @Test
    public void existingUngroupedPropertyDefShouldBeUpdated() throws Exception {
        PropertyDefinitionSimple expected = newConfigurationDef.getPropertyDefinitionSimple("foo");
        PropertyDefinitionSimple actual = originalConfigurationDef.getPropertyDefinitionSimple("foo");

        assertPropertyDefinitionMatches("Existing ungrouped property defs should be updated", expected, actual);
    }

}
