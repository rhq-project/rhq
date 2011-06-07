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
import org.rhq.core.domain.drift.definition.DriftConfigurationDefinition
import org.rhq.core.domain.resource.ResourceType
import org.rhq.core.domain.configuration.definition.PropertySimpleType
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor

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
  void createDriftConfigurationBasedirDefinition() {
    def descriptor = toPluginDescriptor(
    """
    <plugin name="drift-test-plugin" displayName="Drift Test" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:d="urn:xmlns:rhq-drift">
      <server name="TestServer">
        <drift-configuration name="test1">
          <d:basedir>/var/lib/test1</d:basedir>
        </drift-configuration>
      </server>
    </plugin>
    """
    )

    verifyDriftConfiguration(descriptor, 'TestServer', 'test1') { driftConfigDef ->
      def configDef = driftConfigDef.configurationDefinition
      def basedirDef = configDef.getPropertyDefinitionSimple("basedir")

      assertNotNull(basedirDef, "Expected to find property definition <basedir>")
      assertEquals(basedirDef.displayName, "Base Directory", "The displayName property is not set correctly")
      assertTrue(basedirDef.required, "The required property should be set to true")
      assertEquals(
          basedirDef.description,
          "The root directory from which snapshots will be generated during drift monitoring",
          "The description property is not set correctly"
      )
      assertEquals(basedirDef.type, PropertySimpleType.STRING, "The type property is not set correctly")
    }
  }

  @Test
  void createDriftConfigurationBasedirDefault() {
    def descriptor = toPluginDescriptor(
    """
    <plugin name="drift-test-plugin" displayName="Drift Test" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:d="urn:xmlns:rhq-drift">
      <server name="TestServer">
        <drift-configuration name="test1">
          <d:basedir>/var/lib/test1</d:basedir>
        </drift-configuration>
      </server>
    </plugin>
    """
    )

    verifyDriftConfiguration(descriptor, 'TestServer', 'test1') { driftConfigDef ->
      def configDef = driftConfigDef.configurationDefinition
      def defaultConfig = configDef.defaultTemplate.configuration

      assertEquals(defaultConfig.getSimpleValue("basedir", null),
          "/var/lib/test1", "Expected to find default property set for basedir")
    }
  }

  @Test
  void createDriftConfigurationIntervalDefinition() {
    def descriptor = toPluginDescriptor(
    """
    <plugin name="drift-test-plugin" displayName="Drift Test" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:d="urn:xmlns:rhq-drift">
      <server name="TestServer">
        <drift-configuration name="test1">
          <d:basedir>/var/lib/test1</d:basedir>
        </drift-configuration>
      </server>
    </plugin>
    """
    )

    verifyDriftConfiguration(descriptor, 'TestServer', 'test1') { driftConfigDef ->
      def intervalDef = driftConfigDef.configurationDefinition.get("interval")

      assertNotNull(intervalDef, "Expected to find property definition <interval>")
      assertEquals(intervalDef.displayName, "Drift Monitoring Interval",
          "The displayName property is not set correctly")
      assertFalse(intervalDef.required, "The required property should be set to true")
      assertEquals(
        intervalDef.description,
        "The frequency in seconds in which drift monitoring should run. Defaults to thirty minutes.",
        "The description property is not set correctly"
      )
      assertEquals(intervalDef.type, PropertySimpleType.LONG, "The type property is not set correctly")
    }
  }

  @Test
  void createDriftConfigurationIntervalDefault() {
    def descriptor = toPluginDescriptor(
    """
    <plugin name="drift-test-plugin" displayName="Drift Test" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:d="urn:xmlns:rhq-drift">
      <server name="TestServer">
        <drift-configuration name="test1">
          <d:basedir>/var/lib/test1</d:basedir>
        </drift-configuration>
      </server>
    </plugin>
    """
    )

    verifyDriftConfiguration(descriptor, 'TestServer', 'test1') { driftConfigDef ->
      def configDef = driftConfigDef.configurationDefinition
      def defaultConfig = configDef.defaultTemplate.configuration

      assertEquals(defaultConfig.getSimpleValue("interval", null), "1800",
          "Expected to find default property set for interval")
    }
  }

  @Test
  void createDriftConfigurationIntervalDefaultWithSpecifiedValue() {
    def descriptor = toPluginDescriptor(
    """
    <plugin name="drift-test-plugin" displayName="Drift Test" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:d="urn:xmlns:rhq-drift">
      <server name="TestServer">
        <drift-configuration name="test1">
          <d:basedir>/var/lib/test1</d:basedir>
          <d:interval>3600</d:interval>
        </drift-configuration>
      </server>
    </plugin>
    """
    )

    verifyDriftConfiguration(descriptor, 'TestServer', 'test1') { driftConfigDef ->
      def configDef = driftConfigDef.configurationDefinition
      def defaultConfig = configDef.defaultTemplate.configuration

      assertEquals(defaultConfig.getSimpleValue("interval", null), "3600",
          "Expected to find default property set for interval")
    }
  }

  def verifyDriftConfiguration(PluginDescriptor descriptor, String resourceTypeName, String driftConfigName,
      Closure test) {
    def parser = new PluginMetadataParser(descriptor, [:])
    def resourceType = parser.allTypes.find { it.name == resourceTypeName }
    def driftConfigDef = resourceType.driftConfigurationDefinitions.find { it.name == 'test1' }

    assertNotNull(
        resourceType.driftConfigurationDefinitions.find { it.name == driftConfigName},
        "Failed to find drift configuration <$driftConfigName>. The name attribute may not have been parsed correctly."
    )

    test(driftConfigDef)
  }
}
