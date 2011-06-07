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

import org.testng.annotations.Test
import org.rhq.core.domain.configuration.definition.ConfigurationFormat

import static org.rhq.core.clientapi.shared.PluginDescriptorUtil.toPluginDescriptor

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

    assertEquals(parser.allTypes.size(), 1, "The allTypes property should have 1 element for descriptor with a single resource type.")
    assertEquals(parser.allTypes[0].name, "testServer", "Expected to find resource type named 'testServer' in allTypes.")
  }

  @Test
  void allTypesShouldHaveTwoElementsForDescriptorWithOneServerAndOneService() {
    def pluginDescriptor = toPluginDescriptor(
    """
    <plugin name="TestServer" displayName="Test Server" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin">
      <server name="testServer"
              class="TestServer"
              discovery="TestServerDiscoveryComponent">
        <service name="testService"
                 class="TestService"
                 discovery="TestServiceDiscoveryComponent"/>
      </server>
    </plugin>
    """
    )

    def parsersByPlugin = [:]

    def parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin)

    assertEquals(parser.allTypes.size(), 2, "The allTypes property should have 2 elements for a descriptor with a server and a child service.")

    assertNotNull(parser.allTypes.find { it.name == "testServer" }, "Expected to find 'testServer' resource type.")
    assertNotNull(parser.allTypes.find { it.name == "testService" }, "Expected to find 'testService' resource type.")
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

  @Test
  void getDiscoveryComponentClassShouldIncludePluginPackageWhenDiscoveryAttributeDoesNotSpecifyPackageName() {
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

  @Test
  void childServiceResourceTypeShouldBeAddedToParentServerResourceType() {
    def pluginDescriptor = toPluginDescriptor(
    """
    <plugin name="TestServer" displayName="Test Server" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin">
      <server name="testServer"
              class="TestServer"
              discovery="TestServerDiscoveryComponent">
        <service name="testService"
                 class="TestService"
                 discovery="TestServiceDiscoveryComponent"/>
      </server>
    </plugin>
    """
    )

    def parsersByPlugin = [:]

    def parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin)

    def serverResourceType = parser.allTypes.find { it.name == "testServer" }

    assertNotNull(
        serverResourceType.childResourceTypes.find { it.name == "testService" },
        "Expected resource type 'testService' to be added to its parent resource type 'testServer'"
    )
  }

  @Test
  void resourceTypeHavingResourceConfigurationDefinitionShouldBeBuiltForServerWithResourceConfiguration() {
    def pluginDescriptor = toPluginDescriptor(
    """
    <plugin name="TestServer" displayName="Test Server" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:c="urn:xmlns:rhq-configuration">
      <server name="testServer"
              class="TestServer"
              discovery="TestServerDiscoveryComponent">
        <resource-configuration>
          <c:simple-property name="foo" defaultValue="bar"/>
        </resource-configuration>
      </server>
    </plugin>
    """
    )

    def parsersByPlugin = [:]

    def parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin)

    def serverResourceType = parser.allTypes.find { it.name == "testServer" }

    assertNotNull(serverResourceType.resourceConfigurationDefinition, "Expected resource type to have a resource configuration definition.")
    assertNotNull(
        serverResourceType.resourceConfigurationDefinition.getPropertyDefinitionSimple("foo"),
        "Expected resource configuration to contain a simple property named 'foo'."
    )
  }

  @Test
  void configurationDefinitionShouldHaveConfigurationFormatSetWhenSpecifiedInDescriptor() {
    def pluginDescriptor = toPluginDescriptor(
    """
    <plugin name="TestServer" displayName="Test Server" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:c="urn:xmlns:rhq-configuration">
      <server name="testServer"
              class="TestServer"
              discovery="TestServerDiscoveryComponent">
        <resource-configuration configurationFormat="structured">
          <c:simple-property name="foo" defaultValue="bar"/>
        </resource-configuration>
      </server>
    </plugin>
    """
    )

    def parsersByPlugin = [:]

    def parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin)

    def serverResourceType = parser.allTypes.find { it.name == "testServer" }
    def resourceConfigurationDefinition = serverResourceType.resourceConfigurationDefinition

    assertEquals(
        resourceConfigurationDefinition.configurationFormat,
        ConfigurationFormat.STRUCTURED,
        "Expected configurationFormat property to be set on resource configuration definition."
    )
  }

  @Test
  void configurationFormatShouldDefaultToStructuredWhenNotDeclaredInDescriptor() {
    def pluginDescriptor = toPluginDescriptor(
    """
    <plugin name="TestServer" displayName="Test Server" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:c="urn:xmlns:rhq-configuration">
      <server name="testServer"
              class="TestServer"
              discovery="TestServerDiscoveryComponent">
        <resource-configuration>
          <c:simple-property name="foo" defaultValue="bar"/>
        </resource-configuration>
      </server>
    </plugin>
    """
    )

    def parsersByPlugin = [:]

    def parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin)

    def serverResourceType = parser.allTypes.find { it.name == "testServer" }
    def resourceConfigurationDefinition = serverResourceType.resourceConfigurationDefinition

    assertEquals(
        resourceConfigurationDefinition.configurationFormat,
        ConfigurationFormat.STRUCTURED,
        "The configurationFormat property of the configuration definition should default to stuctured when it is not declared in the plugin descriptor."
    )
  }

  @Test
  void rawResourceConfigurationShouldNotHaveToSpecifyAnyStructuredConfig() {
    def pluginDescriptor = toPluginDescriptor(
    """
    <plugin name="TestServer" displayName="Test Server" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:c="urn:xmlns:rhq-configuration">
      <server name="testServer"
              class="TestServer"
              discovery="TestServerDiscoveryComponent">
        <resource-configuration configurationFormat="raw"/>
      </server>
    </plugin>
    """
    )

    def parsersByPlugin = [:]

    def parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin)
    
  }

  @Test
  void parseMinimalDriftConfigurations() {
    def descriptor = toPluginDescriptor(
    """
    <plugin name="drift-test-plugin" displayName="Test Server" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:d="urn:xmlns:rhq-drift">
      <server name="TestServer"
              class="TestServer">
        <drift-configuration name="test1">
          <d:basedir>/var/lib/test1</d:basedir>
        </drift-configuration>
        <drift-configuration name="test2">
          <d:basedir>/var/lib/test2</d:basedir>
        </drift-configuration>
      </server>
    </plugin>
    """
    )

    def parser = new PluginMetadataParser(descriptor, [:])
    def resourceType = parser.allTypes.find { it.name == 'TestServer' }

    assertEquals(resourceType.getDriftConfigurationDefinitions().size(), 2, "Expected to find two drift configurations")

    assertNotNull(
        resourceType.driftConfigurationDefinitions.find { it.name == 'test1'},
        "Failed to find drift configuration <test1>"
    )

    assertNotNull(
        resourceType.driftConfigurationDefinitions.find { it.name == 'test2'},
        "Failed to find drift configuration <test2>"
    )
  }

//  @Test
//  parseDriftConfigurationWith

//  void assertDriftConfDefEquals(DrifConfigurationDefinition expected, Dri)
}
