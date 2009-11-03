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
package org.rhq.core.clientapi.agent.metadata

import static org.testng.Assert.*

import javax.xml.XMLConstants
import javax.xml.bind.JAXBContext
import javax.xml.bind.Unmarshaller
import javax.xml.bind.util.ValidationEventCollector
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import org.rhq.core.clientapi.descriptor.DescriptorPackages
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor
import org.testng.annotations.Test

class PluginMetadataParserTest {


  @Test
  void allTypesShouldHaveOneElementForDescriptorWithOnlyOneResourceType() {
    def pluginDescriptor = toPluginDescriptor(
    """
    <plugin name="TestServer" displayName="Test Server" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin">
      <server name="testServer"
              class="org.rhq.plugins.test.TestServer"
              discovery="org.rhq.plugins.test.TestServerDiscoveryComponent"/>
    </plugin>
    """
    )

    def parsersByPlugin = [:]

    def parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin)

    assertEquals(parser.allTypes.size(), 1, "allTypes property should have 1 element for descriptor with a single resource type.")
    assertEquals(parser.allTypes[0].name, "testServer", "Expected to find resource type named 'testServer' in allTypes.")
  }

  @Test
  void rootResourceTypesShouldHaveOneElementForDescriptorWithOnlyOneResourceType() {
    def pluginDescriptor = toPluginDescriptor(
    """
    <plugin name="TestServer" displayName="Test Server" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin">
      <server name="testServer"
              class="org.rhq.plugins.test.TestServer"
              discovery="org.rhq.plugins.test.TestServerDiscoveryComponent"/>
    </plugin>
    """
    )

    def parsersByPlugin = [:]

    def parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin)

    assertEquals(parser.rootResourceTypes.size(), 1, "rootResourceTypes property should have 1 element for descriptor with a single resource type.")
    assertNotNull(parser.rootResourceTypes.find { it.name == 'testServer' }, "Expected to find resource type named 'testServer' in rootResouceTypes.")
  }

  @Test
  void getComponentClassShouldReturnValueOfClassAttributeWhenPackageNameIncludedInAttribute() {
    def pluginDescriptor = toPluginDescriptor(
    """
    <plugin name="TestServer" displayName="Test Server" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin">
      <server name="testServer"
              class="org.rhq.plugins.test.TestServer"
              discovery="org.rhq.plugins.test.TestServerDiscoveryComponent"/>
    </plugin>
    """
    )

    def parsersByPlugin = [:]

    def parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin)

    def resourceType = parser.allTypes.find { it.name == "testServer" }

    def componentClass = parser.getComponentClass(resourceType)

    assertEquals(
        componentClass,
        "org.rhq.plugins.test.TestServer",
        "Expected the package and class name from the 'class' attribute when it includes the package name.")
  }

  @Test
  void getComponentClassShouldIncludePluginPackageWhenClassAttributeDoesNotSpecifyPackageName() {
    def pluginDescriptor = toPluginDescriptor(
    """
    <plugin name="TestServer" displayName="Test Server" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin">
      <server name="testServer"
              class="TestServer"
              discovery="TestServerDiscoveryComponent"/>
    </plugin>
    """
    )

    def parsersByPlugin = [:]

    def parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin)

    def resourceType = parser.allTypes.find { it.name == "testServer" }

    def componentClass = parser.getComponentClass(resourceType)

    assertEquals(
        componentClass,
        "org.rhq.plugins.test.TestServer",
        "Expected the package name from the 'package' attribute of the <plugin> element to be included when the " +
            "package is not included in the 'class' attribute."
    )
  }

  @Test
  void getDiscoveryComponentClassShouldReturnValueOfDiscoveryComponentAttributeWhenPackageNameIncludedInAttribute() {
    def pluginDescriptor = toPluginDescriptor(
    """
    <plugin name="TestServer" displayName="Test Server" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin">
      <server name="testServer"
              class="org.rhq.plugins.test.TestServer"
              discovery="org.rhq.plugins.test.TestServerDiscoveryComponent"/>
    </plugin>
    """
    )

    def parsersByPlugin = [:]

    def parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin)

    def resourceType = parser.allTypes.find { it.name == "testServer" }

    def discoveryComponent = parser.getDiscoveryComponentClass(resourceType)

    assertEquals(
        discoveryComponent,
        "org.rhq.plugins.test.TestServerDiscoveryComponent",
        "Expected the package and class name from 'discovery' attribute when it includes the package name."
    )
  }

  @Test getDiscoveryComponentClassShouldIncludePluginPackageWhenDiscoveryAttributeDoesNotSpecifyPackageName() {
    def pluginDescriptor = toPluginDescriptor(
    """
    <plugin name="TestServer" displayName="Test Server" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin">
      <server name="testServer"
              class="TestServer"
              discovery="TestServerDiscoveryComponent"/>
    </plugin>
    """
    )

    def parsersByPlugin = [:]

    def parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin)

    def resourceType = parser.allTypes.find { it.name == "testServer" }

    def discoveryComponent = parser.getDiscoveryComponentClass(resourceType)

    assertEquals(
        discoveryComponent,
        "org.rhq.plugins.test.TestServerDiscoveryComponent",
        "Expected the package name from the 'package' attribute of the <plugin> element to be included when the " +
            "pacage is not included in the 'discovery' attribute."
    )
  }

  static PluginDescriptor toPluginDescriptor(String string) {
    JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN)
    URL pluginSchemaURL = PluginMetadataParser.class.getClassLoader().getResource("rhq-plugin.xsd")
    Schema pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(pluginSchemaURL)

    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller()
    ValidationEventCollector vec = new ValidationEventCollector()
    unmarshaller.setEventHandler(vec)
    unmarshaller.setSchema(pluginSchema)

    StringReader reader = new StringReader(string)

    return (PluginDescriptor) unmarshaller.unmarshal(reader);
  }

}
