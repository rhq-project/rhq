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

import static org.rhq.core.clientapi.shared.PluginDescriptorUtil.toPluginDescriptor;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataParser;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftConfigurationDefinition;
import org.rhq.core.domain.drift.DriftConfiguration.BaseDirectory;
import org.rhq.core.domain.drift.DriftConfiguration.Filter;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext;
import org.rhq.core.domain.resource.ResourceType;

@Test
public class PluginMetadataParserTest {

    @Test
    void allTypesShouldHaveOneElementForDescriptorWithOnlyOneResourceType() throws Exception {
        PluginDescriptor pluginDescriptor = toPluginDescriptor("" + //
            "<plugin name='TestServer' displayName='Test Server' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'>" + //
            "  <server name='testServer'" + //
            "          class='org.rhq.plugins.test.TestServer'" + // 
            "          discovery='org.rhq.plugins.test.TestServerDiscoveryComponent'/>" + //
            "</plugin>");

        Map<String, PluginMetadataParser> parsersByPlugin = new HashMap<String, PluginMetadataParser>(0);
        PluginMetadataParser parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin);

        assertEquals(parser.getAllTypes().size(), 1,
            "The allTypes property should have 1 element for descriptor with a single resource type.");
        assertEquals(parser.getAllTypes().get(0).getName(), "testServer",
            "Expected to find resource type named 'testServer' in allTypes.");
    }

    @Test
    void allTypesShouldHaveTwoElementsForDescriptorWithOneServerAndOneService() throws Exception {
        PluginDescriptor pluginDescriptor = toPluginDescriptor("" + //
            "<plugin name='TestServer' displayName='Test Server' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'>" + //
            "  <server name='testServer'" + //
            "          class='TestServer'" + //
            "          discovery='TestServerDiscoveryComponent'>" + //
            "    <service name='testService'" + //
            "             class='TestService'" + //
            "             discovery='TestServiceDiscoveryComponent'/>" + //
            "  </server>" + //
            "</plugin>");

        Map<String, PluginMetadataParser> parsersByPlugin = new HashMap<String, PluginMetadataParser>(0);
        PluginMetadataParser parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin);

        assertEquals(parser.getAllTypes().size(), 2,
            "The allTypes property should have 2 elements for a descriptor with a server and a child service.");

        assertNotNull(findResourceType(parser, "testServer"));
        assertNotNull(findResourceType(parser, "testService"));
    }

    private ResourceType findResourceType(PluginMetadataParser parser, String resourceTypeName) {
        List<ResourceType> allTypes = parser.getAllTypes();
        for (ResourceType resourceType : allTypes) {
            if (resourceType.getName().equals(resourceTypeName)) {
                return resourceType;
            }
        }
        assert false : "expected to find resource type [" + resourceTypeName + "] in: " + allTypes;
        return null; // no-op since the above assert should always throw exception
    }

    @Test
    void rootResourceTypesShouldHaveOneElementForDescriptorWithOnlyOneResourceType() throws Exception {
        PluginDescriptor pluginDescriptor = toPluginDescriptor("" + //
            "<plugin name='TestServer' displayName='Test Server' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'>" + //
            "  <server name='testServer'" + //
            "          class='org.rhq.plugins.test.TestServer'" + //
            "          discovery='org.rhq.plugins.test.TestServerDiscoveryComponent'/>" + //
            "</plugin>");

        Map<String, PluginMetadataParser> parsersByPlugin = new HashMap<String, PluginMetadataParser>(0);
        PluginMetadataParser parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin);

        assertEquals(parser.getRootResourceTypes().size(), 1,
            "rootResourceTypes property should have 1 element for descriptor with a single resource type.");
        Set<ResourceType> rootResourceTypes = parser.getRootResourceTypes();
        ResourceType root = null;
        for (ResourceType resourceType : rootResourceTypes) {
            if (resourceType.getName().equals("testServer")) {
                root = resourceType;
                break;
            }
        }
        assertNotNull(root, "Expected to find resource type named 'testServer' in rootResouceTypes.");
    }

    @Test
    void getComponentClassShouldReturnValueOfClassAttributeWhenPackageNameIncludedInAttribute() throws Exception {
        PluginDescriptor pluginDescriptor = toPluginDescriptor("" + //
            "<plugin name='TestServer' displayName='Test Server' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'>" + //
            "  <server name='testServer'" + //
            "          class='org.rhq.plugins.test.TestServer'" + //
            "          discovery='org.rhq.plugins.test.TestServerDiscoveryComponent'/>" + //
            "</plugin>");

        Map<String, PluginMetadataParser> parsersByPlugin = new HashMap<String, PluginMetadataParser>(0);
        PluginMetadataParser parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin);

        ResourceType resourceType = findResourceType(parser, "testServer");

        String componentClass = parser.getComponentClass(resourceType);

        assertEquals(componentClass, "org.rhq.plugins.test.TestServer",
            "Expected the package and class name from the 'class' attribute when it includes the package name.");
    }

    @Test
    void getComponentClassShouldIncludePluginPackageWhenClassAttributeDoesNotSpecifyPackageName() throws Exception {
        PluginDescriptor pluginDescriptor = toPluginDescriptor("" + //
            "<plugin name='TestServer' displayName='Test Server' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'>" + //
            "  <server name='testServer'" + //
            "          class='TestServer'" + //
            "          discovery='TestServerDiscoveryComponent'/>" + //
            "</plugin>");

        Map<String, PluginMetadataParser> parsersByPlugin = new HashMap<String, PluginMetadataParser>(0);
        PluginMetadataParser parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin);

        ResourceType resourceType = findResourceType(parser, "testServer");

        String componentClass = parser.getComponentClass(resourceType);

        assertEquals(componentClass, "org.rhq.plugins.test.TestServer",
            "Expected the package name from the 'package' attribute of the <plugin> element to be included when the "
                + "package is not included in the 'class' attribute.");
    }

    @Test
    void getDiscoveryComponentClassShouldReturnValueOfDiscoveryComponentAttributeWhenPackageNameIncludedInAttribute()
        throws Exception {
        PluginDescriptor pluginDescriptor = toPluginDescriptor("" + //
            "<plugin name='TestServer' displayName='Test Server' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'>" + //
            "  <server name='testServer'" + //
            "          class='org.rhq.plugins.test.TestServer'" + //
            "          discovery='org.rhq.plugins.test.TestServerDiscoveryComponent'/>" + //
            "</plugin>");

        Map<String, PluginMetadataParser> parsersByPlugin = new HashMap<String, PluginMetadataParser>(0);
        PluginMetadataParser parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin);

        ResourceType resourceType = findResourceType(parser, "testServer");

        String discoveryComponent = parser.getDiscoveryComponentClass(resourceType);

        assertEquals(discoveryComponent, "org.rhq.plugins.test.TestServerDiscoveryComponent",
            "Expected the package and class name from 'discovery' attribute when it includes the package name.");
    }

    @Test
    void getDiscoveryComponentClassShouldIncludePluginPackageWhenDiscoveryAttributeDoesNotSpecifyPackageName()
        throws Exception {
        PluginDescriptor pluginDescriptor = toPluginDescriptor("" + //
            "<plugin name='TestServer' displayName='Test Server' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'>" + //
            "  <server name='testServer'" + //
            "          class='TestServer'" + //
            "          discovery='TestServerDiscoveryComponent'/>" + //
            "</plugin>");

        Map<String, PluginMetadataParser> parsersByPlugin = new HashMap<String, PluginMetadataParser>(0);
        PluginMetadataParser parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin);

        ResourceType resourceType = findResourceType(parser, "testServer");

        String discoveryComponent = parser.getDiscoveryComponentClass(resourceType);

        assertEquals(discoveryComponent, "org.rhq.plugins.test.TestServerDiscoveryComponent",
            "Expected the package name from the 'package' attribute of the <plugin> element to be included when the "
                + "pacage is not included in the 'discovery' attribute.");
    }

    @Test
    void childServiceResourceTypeShouldBeAddedToParentServerResourceType() throws Exception {
        PluginDescriptor pluginDescriptor = toPluginDescriptor("" + //
            "<plugin name='TestServer' displayName='Test Server' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'>" + //
            "  <server name='testServer'" + //
            "          class='TestServer'" + //
            "          discovery='TestServerDiscoveryComponent'>" + //
            "    <service name='testService'" + //
            "             class='TestService'" + //
            "             discovery='TestServiceDiscoveryComponent'/>" + //
            "  </server>" + //
            "</plugin>");

        Map<String, PluginMetadataParser> parsersByPlugin = new HashMap<String, PluginMetadataParser>(0);
        PluginMetadataParser parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin);

        ResourceType serverResourceType = findResourceType(parser, "testServer");
        Set<ResourceType> childResourceTypes = serverResourceType.getChildResourceTypes();
        ResourceType child = null;
        for (ResourceType resourceType : childResourceTypes) {
            if (resourceType.getName().equals("testService")) {
                child = resourceType;
                break;
            }
        }

        assertNotNull(child,
            "Expected resource type 'testService' to be added to its parent resource type 'testServer'");
    }

    @Test
    void resourceTypeHavingResourceConfigurationDefinitionShouldBeBuiltForServerWithResourceConfiguration()
        throws Exception {
        PluginDescriptor pluginDescriptor = toPluginDescriptor("" + //
            "<plugin name='TestServer' displayName='Test Server' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'" + //
            "    xmlns:c='urn:xmlns:rhq-configuration'>" + //
            "  <server name='testServer'" + //
            "          class='TestServer'" + //
            "          discovery='TestServerDiscoveryComponent'>" + //
            "    <resource-configuration>" + //
            "      <c:simple-property name='foo' defaultValue='bar'/>" + //
            "    </resource-configuration>" + //
            "  </server>" + //
            "</plugin>");

        Map<String, PluginMetadataParser> parsersByPlugin = new HashMap<String, PluginMetadataParser>(0);
        PluginMetadataParser parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin);

        ResourceType serverResourceType = findResourceType(parser, "testServer");

        assertNotNull(serverResourceType.getResourceConfigurationDefinition(),
            "Expected resource type to have a resource configuration definition.");
        assertNotNull(serverResourceType.getResourceConfigurationDefinition().getPropertyDefinitionSimple("foo"),
            "Expected resource configuration to contain a simple property named 'foo'.");
    }

    @Test
    void configurationDefinitionShouldHaveConfigurationFormatSetWhenSpecifiedInDescriptor() throws Exception {
        PluginDescriptor pluginDescriptor = toPluginDescriptor("" + //
            "<plugin name='TestServer' displayName='Test Server' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'" + //
            "    xmlns:c='urn:xmlns:rhq-configuration'>" + //
            "  <server name='testServer'" + //
            "          class='TestServer'" + //
            "          discovery='TestServerDiscoveryComponent'>" + //
            "    <resource-configuration configurationFormat='structured'>" + //
            "      <c:simple-property name='foo' defaultValue='bar'/>" + //
            "    </resource-configuration>" + //
            "  </server>" + //
            "</plugin>");

        Map<String, PluginMetadataParser> parsersByPlugin = new HashMap<String, PluginMetadataParser>(0);
        PluginMetadataParser parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin);

        ResourceType serverResourceType = findResourceType(parser, "testServer");
        ConfigurationDefinition resourceConfigurationDefinition = serverResourceType
            .getResourceConfigurationDefinition();

        assertEquals(resourceConfigurationDefinition.getConfigurationFormat(), ConfigurationFormat.STRUCTURED,
            "Expected configurationFormat property to be set on resource configuration definition.");
    }

    @Test
    void configurationFormatShouldDefaultToStructuredWhenNotDeclaredInDescriptor() throws Exception {
        PluginDescriptor pluginDescriptor = toPluginDescriptor("" + //
            "<plugin name='TestServer' displayName='Test Server' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'" + //
            "    xmlns:c='urn:xmlns:rhq-configuration'>" + //
            "  <server name='testServer'" + //
            "          class='TestServer'" + //
            "          discovery='TestServerDiscoveryComponent'>" + //
            "    <resource-configuration>" + //
            "      <c:simple-property name='foo' defaultValue='bar'/>" + //
            "    </resource-configuration>" + //
            "  </server>" + //
            "</plugin>");

        Map<String, PluginMetadataParser> parsersByPlugin = new HashMap<String, PluginMetadataParser>(0);
        PluginMetadataParser parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin);

        ResourceType serverResourceType = findResourceType(parser, "testServer");
        ConfigurationDefinition resourceConfigurationDefinition = serverResourceType
            .getResourceConfigurationDefinition();

        assertEquals(
            resourceConfigurationDefinition.getConfigurationFormat(),
            ConfigurationFormat.STRUCTURED,
            "The configurationFormat property of the configuration definition should default to stuctured when it is not declared in the plugin descriptor.");
    }

    @Test
    void rawResourceConfigurationShouldNotHaveToSpecifyAnyStructuredConfig() throws Exception {
        PluginDescriptor pluginDescriptor = toPluginDescriptor("" + //
            "<plugin name='TestServer' displayName='Test Server' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'" + //
            "    xmlns:c='urn:xmlns:rhq-configuration'>" + //
            "  <server name='testServer'" + //
            "          class='TestServer'" + //
            "          discovery='TestServerDiscoveryComponent'>" + //
            "    <resource-configuration configurationFormat='raw'/>" + //
            "  </server>" + //
            "</plugin>");

        Map<String, PluginMetadataParser> parsersByPlugin = new HashMap<String, PluginMetadataParser>(0);
        PluginMetadataParser parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin);
        assert parser.getAllTypes().size() > 0;
    }

    @Test
    void createDriftConfigurationBasedir() throws Exception {
        PluginDescriptor descriptor = toPluginDescriptor("" + //
            "<plugin name='drift-test-plugin' displayName='Drift Test' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'" + //
            "    xmlns:d='urn:xmlns:rhq-drift'>" + //
            "  <server name='TestServer'>" + //
            "    <drift-configuration name='test1'>" + //
            "      <d:basedir>" + //
            "          <d:value-context>pluginConfiguration</d:value-context>" + //
            "          <d:value-name>var.lib.test1</d:value-name>" + //
            "      </d:basedir>" + //
            "    </drift-configuration>" + //
            "  </server>" + //
            "</plugin>");

        verifyDriftConfiguration(descriptor, "TestServer", "test1", new AssertDriftTemplateRunnable() {
            @Override
            public void assertDriftTemplate(ConfigurationTemplate driftTemplate) throws Exception {
                DriftConfiguration dc = new DriftConfiguration(driftTemplate.getConfiguration());
                BaseDirectory basedir = dc.getBasedir();
                assertEquals(basedir.getValueContext(), BaseDirValueContext.pluginConfiguration, "Bad value context");
                assertEquals(basedir.getValueName(), "var.lib.test1", "Bad value name");
            }
        });
    }

    @Test
    void createDriftConfigurationInvalidBasedir() throws Exception {
        try {
            toPluginDescriptor("" + //
                "<plugin name='drift-test-plugin' displayName='Drift Test' package='org.rhq.plugins.test'" + //
                "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
                "    xmlns='urn:xmlns:rhq-plugin'" + //
                "    xmlns:d='urn:xmlns:rhq-drift'>" + //
                "  <server name='TestServer'>" + //
                "    <drift-configuration name='test1'>" + //
                "      <d:basedir>" + //
                "          <d:value-context>saywhat</d:value-context>" + // this is an invalid context
                "          <d:value-name>var.lib.test1</d:value-name>" + //
                "      </d:basedir>" + //
                "    </drift-configuration>" + //
                "  </server>" + //
                "</plugin>");
            assert false : "should not have reached here, the XML itself was invalid and should not have parsed";
        } catch (Exception ignore) {
            // this is OK and expected - the XML should not have parsed due to the invalid context
        }
    }

    @Test
    void createDriftConfigurationIntervalDefault() throws Exception {
        PluginDescriptor descriptor = toPluginDescriptor("" + //
            "<plugin name='drift-test-plugin' displayName='Drift Test' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'" + //
            "    xmlns:d='urn:xmlns:rhq-drift'>" + //
            "  <server name='TestServer'>" + //
            "    <drift-configuration name='test1'>" + //
            "      <d:basedir>" + //
            "          <d:value-context>pluginConfiguration</d:value-context>" + //
            "          <d:value-name>var.lib.test1</d:value-name>" + //
            "      </d:basedir>" + //
            "    </drift-configuration>" + //
            "  </server>" + //
            "</plugin>");

        verifyDriftConfiguration(descriptor, "TestServer", "test1", new AssertDriftTemplateRunnable() {
            @Override
            public void assertDriftTemplate(ConfigurationTemplate driftTemplate) throws Exception {
                assertEquals(driftTemplate.getConfiguration().getSimpleValue(
                    DriftConfigurationDefinition.PROP_INTERVAL, null), String
                    .valueOf(DriftConfigurationDefinition.DEFAULT_INTERVAL),
                    "Expected to find default property set for interval");
            }
        });
    }

    @Test
    void createDriftConfigurationIntervalDefaultWithSpecifiedValue() throws Exception {
        PluginDescriptor descriptor = toPluginDescriptor("" + //
            "<plugin name='drift-test-plugin' displayName='Drift Test' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'" + //
            "    xmlns:d='urn:xmlns:rhq-drift'>" + //
            "  <server name='TestServer'>" + //
            "    <drift-configuration name='test1'>" + //
            "      <d:basedir>" + //
            "          <d:value-context>pluginConfiguration</d:value-context>" + //
            "          <d:value-name>var.lib.test1</d:value-name>" + //
            "      </d:basedir>" + //
            "      <d:interval>3600</d:interval>" + //
            "    </drift-configuration>" + //
            "  </server>" + //
            "</plugin>");

        verifyDriftConfiguration(descriptor, "TestServer", "test1", new AssertDriftTemplateRunnable() {
            @Override
            public void assertDriftTemplate(ConfigurationTemplate driftTemplate) throws Exception {
                assertEquals(driftTemplate.getConfiguration().getSimpleValue(
                    DriftConfigurationDefinition.PROP_INTERVAL, null), "3600",
                    "Expected to find default property set for <interval>");
            }
        });

    }

    @Test
    void createDriftConfigurationIncludesDefaultWithSpecifiedValue() throws Exception {
        PluginDescriptor descriptor = toPluginDescriptor("" + //
            "<plugin name='drift-test-plugin' displayName='Drift Test' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'" + //
            "    xmlns:d='urn:xmlns:rhq-drift'>" + //
            "  <server name='TestServer'>" + //
            "    <drift-configuration name='test1'>" + //
            "      <d:basedir>" + //
            "          <d:value-context>pluginConfiguration</d:value-context>" + //
            "          <d:value-name>var.lib.test1</d:value-name>" + //
            "      </d:basedir>" + //
            "      <d:includes>" + //
            "        <d:include path='lib' pattern='*.jar'/>" + //
            "        <d:include path='conf' pattern='*.xml'/>" + //
            "      </d:includes>" + //
            "    </drift-configuration>" + //
            "  </server>" + //
            "</plugin>");

        verifyDriftConfiguration(descriptor, "TestServer", "test1", new AssertDriftTemplateRunnable() {
            @Override
            public void assertDriftTemplate(ConfigurationTemplate driftTemplate) throws Exception {
                Configuration config = driftTemplate.getConfiguration();
                PropertyList includes = config.getList(DriftConfigurationDefinition.PROP_INCLUDES);

                assertNotNull(includes, "Expected to find default property set for <includes>");
                assertEquals(includes.getList().size(), 2,
                    "Expected <includes> property list to have two property elements.");

                PropertyMap include1 = (PropertyMap) includes.getList().get(0);
                PropertySimple path1 = include1.getSimple(DriftConfigurationDefinition.PROP_PATH);
                PropertySimple pattern1 = include1.getSimple(DriftConfigurationDefinition.PROP_PATTERN);

                assertNotNull(path1, "Expected to find a simple property for the path of the first <include>");
                assertEquals(path1.getStringValue(), "lib", "The value is wrong for the path of the first <include>");

                assertNotNull(pattern1, "Expected to find a simple property for the pattern of the first <include>");
                assertEquals(pattern1.getStringValue(), "*.jar",
                    "The value is wrong for the pattern of the first <include>");

                PropertyMap include2 = (PropertyMap) includes.getList().get(1);
                PropertySimple path2 = include2.getSimple(DriftConfigurationDefinition.PROP_PATH);
                PropertySimple pattern2 = include2.getSimple(DriftConfigurationDefinition.PROP_PATTERN);

                assertNotNull(path2, "Expected to find a simple property for the path of the second <include>");
                assertEquals(path2.getStringValue(), "conf", "The value is wrong for the path of the second <include>");

                assertNotNull(pattern2, "Expected to find a simple property for the pattern of the second <include>");
                assertEquals(pattern2.getStringValue(), "*.xml",
                    "The value is wrong for the pattern of the second <include>");
            }
        });
    }

    @Test
    void createDriftConfigurationExcludesDefaultWithSpecifiedValue() throws Exception {
        PluginDescriptor descriptor = toPluginDescriptor("" + //
            "<plugin name='drift-test-plugin' displayName='Drift Test' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'" + //
            "    xmlns:d='urn:xmlns:rhq-drift'>" + //
            "  <server name='TestServer'>" + //
            "    <drift-configuration name='test1'>" + //
            "      <d:basedir>" + //
            "          <d:value-context>pluginConfiguration</d:value-context>" + //
            "          <d:value-name>var.lib.test1</d:value-name>" + //
            "      </d:basedir>" + //
            "      <d:interval>3600</d:interval>" + //
            "      <d:excludes>" + //
            "        <d:exclude path='lib' pattern='*.jar'/>" + //
            "        <d:exclude path='conf' pattern='*.xml'/>" + //
            "      </d:excludes>" + //
            "    </drift-configuration>" + //
            "  </server>" + //
            "</plugin>");

        verifyDriftConfiguration(descriptor, "TestServer", "test1", new AssertDriftTemplateRunnable() {
            @Override
            public void assertDriftTemplate(ConfigurationTemplate driftTemplate) throws Exception {
                Configuration config = driftTemplate.getConfiguration();
                PropertyList excludes = config.getList(DriftConfigurationDefinition.PROP_EXCLUDES);

                assertNotNull(excludes, "Expected to find default property set for <excludes>");
                assertEquals(excludes.getList().size(), 2,
                    "Expected <excludes> property list to have two property elements.");

                PropertyMap exclude1 = (PropertyMap) excludes.getList().get(0);
                PropertySimple path1 = exclude1.getSimple(DriftConfigurationDefinition.PROP_PATH);
                PropertySimple pattern1 = exclude1.getSimple(DriftConfigurationDefinition.PROP_PATTERN);

                assertNotNull(path1, "Expected to find a simple property for the path of the first <exclude>");
                assertEquals(path1.getStringValue(), "lib", "The value is wrong for the path of the first <exclude>");

                assertNotNull(pattern1, "Expected to find a simple property for the pattern of the first <exclude>");
                assertEquals(pattern1.getStringValue(), "*.jar",
                    "The value is wrong for the pattern of the first <exclude>");

                PropertyMap exclude2 = (PropertyMap) excludes.getList().get(1);
                PropertySimple path2 = exclude2.getSimple(DriftConfigurationDefinition.PROP_PATH);
                PropertySimple pattern2 = exclude2.getSimple(DriftConfigurationDefinition.PROP_PATTERN);

                assertNotNull(path2, "Expected to find a simple property for the path of the second <exclude>");
                assertEquals(path2.getStringValue(), "conf", "The value is wrong for the path of the second <exclude>");

                assertNotNull(pattern2, "Expected to find a simple property for the pattern of the second <exclude>");
                assertEquals(pattern2.getStringValue(), "*.xml",
                    "The value is wrong for the pattern of the second <exclude>");
            }
        });
    }

    /**
     * This also tests DriftConfiguration POJO.
     * 
     * @throws Exception
     */
    @Test
    void createDriftConfigurationMultipleAndTestDriftConfiguration() throws Exception {
        PluginDescriptor descriptor = toPluginDescriptor("" + //
            "<plugin name='drift-test-plugin' displayName='Drift Test' package='org.rhq.plugins.test'" + //
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + //
            "    xmlns='urn:xmlns:rhq-plugin'" + //
            "    xmlns:d='urn:xmlns:rhq-drift'>" + //
            "  <server name='TestServer'>" + //
            "    <drift-configuration name='test1'>" + //
            "      <d:basedir>" + //
            "          <d:value-context>pluginConfiguration</d:value-context>" + //
            "          <d:value-name>var.lib.test1</d:value-name>" + //
            "      </d:basedir>" + //
            "      <d:interval>11111</d:interval>" + //
            "      <d:includes>" + //
            "        <d:include path='ilib' pattern='*.ijar'/>" + //
            "        <d:include path='iconf' pattern='*.ixml'/>" + //
            "      </d:includes>" + //
            "    </drift-configuration>" + //
            "    <drift-configuration name='test2'>" + //
            "      <d:basedir>" + //
            "          <d:value-context>resourceConfiguration</d:value-context>" + //
            "          <d:value-name>var.lib.test2</d:value-name>" + //
            "      </d:basedir>" + //
            "      <d:interval>22222</d:interval>" + //
            "      <d:excludes>" + //
            "        <d:exclude path='elib' pattern='*.ejar'/>" + //
            "        <d:exclude path='econf' pattern='*.exml'/>" + //
            "      </d:excludes>" + //
            "    </drift-configuration>" + //
            "    <drift-configuration name='test3'>" + //
            "      <d:basedir>" + //
            "          <d:value-context>measurementTrait</d:value-context>" + //
            "          <d:value-name>var.lib.test3</d:value-name>" + //
            "      </d:basedir>" + //
            "      <d:interval>33333</d:interval>" + //
            "      <d:includes>" + //
            "        <d:include path='ilib' pattern='*.ijar'/>" + //
            "        <d:include path='iconf' pattern='*.ixml'/>" + //
            "      </d:includes>" + //
            "      <d:excludes>" + //
            "        <d:exclude path='elib' pattern='*.ejar'/>" + //
            "        <d:exclude path='econf' pattern='*.exml'/>" + //
            "      </d:excludes>" + //
            "    </drift-configuration>" + //
            "    <drift-configuration name='test4'>" + //
            "      <d:basedir>" + //
            "          <d:value-context>fileSystem</d:value-context>" + //
            "          <d:value-name>/wot/gorilla</d:value-name>" + //
            "      </d:basedir>" + //
            "      <d:interval>44444</d:interval>" + //
            "    </drift-configuration>" + //
            "  </server>" + //
            "</plugin>");

        verifyDriftConfiguration(descriptor, "TestServer", "test1", new AssertDriftTemplateRunnable() {
            @Override
            public void assertDriftTemplate(ConfigurationTemplate driftTemplate) throws Exception {
                Configuration config = driftTemplate.getConfiguration();
                DriftConfiguration dconfig = new DriftConfiguration(config);

                assertEquals(dconfig.getInterval().longValue(), 11111L);
                assertEquals(dconfig.getBasedir().getValueContext(), BaseDirValueContext.pluginConfiguration);
                assertEquals(dconfig.getBasedir().getValueName(), "var.lib.test1");

                assertNotNull(dconfig.getExcludes(), "though we have no excludes, still expect non-null empty list");
                assertEquals(dconfig.getExcludes().size(), 0);

                List<Filter> includes = dconfig.getIncludes();

                assertNotNull(includes, "Expected to find default property set for <includes>");
                assertEquals(includes.size(), 2, "Expected <includes> property list to have two property elements.");

                DriftConfiguration.Filter include1 = includes.get(0);
                String path1 = include1.getPath();
                String pattern1 = include1.getPattern();

                assertNotNull(path1, "Expected to find a simple property for the path of the first <include>");
                assertEquals(path1, "ilib", "The value is wrong for the path of the first <include>");

                assertNotNull(pattern1, "Expected to find a simple property for the pattern of the first <include>");
                assertEquals(pattern1, "*.ijar", "The value is wrong for the pattern of the first <include>");

                DriftConfiguration.Filter include2 = includes.get(1);
                String path2 = include2.getPath();
                String pattern2 = include2.getPattern();

                assertNotNull(path2, "Expected to find a simple property for the path of the second <include>");
                assertEquals(path2, "iconf", "The value is wrong for the path of the second <include>");

                assertNotNull(pattern2, "Expected to find a simple property for the pattern of the second <include>");
                assertEquals(pattern2, "*.ixml", "The value is wrong for the pattern of the second <include>");
            }
        });

        verifyDriftConfiguration(descriptor, "TestServer", "test2", new AssertDriftTemplateRunnable() {
            @Override
            public void assertDriftTemplate(ConfigurationTemplate driftTemplate) throws Exception {
                Configuration config = driftTemplate.getConfiguration();
                DriftConfiguration dconfig = new DriftConfiguration(config);

                assertEquals(dconfig.getInterval().longValue(), 22222L);
                assertEquals(dconfig.getBasedir().getValueContext(), BaseDirValueContext.resourceConfiguration);
                assertEquals(dconfig.getBasedir().getValueName(), "var.lib.test2");

                assertNotNull(dconfig.getIncludes(), "though we have no includes, still expect non-null empty list");
                assertEquals(dconfig.getIncludes().size(), 0);

                List<Filter> excludes = dconfig.getExcludes();

                assertNotNull(excludes, "Expected to find default property set for <excludes>");
                assertEquals(excludes.size(), 2, "Expected <excludes> property list to have two property elements.");

                DriftConfiguration.Filter exclude1 = excludes.get(0);
                String path1 = exclude1.getPath();
                String pattern1 = exclude1.getPattern();

                assertNotNull(path1, "Expected to find a simple property for the path of the first <exclude>");
                assertEquals(path1, "elib", "The value is wrong for the path of the first <exclude>");

                assertNotNull(pattern1, "Expected to find a simple property for the pattern of the first <exclude>");
                assertEquals(pattern1, "*.ejar", "The value is wrong for the pattern of the first <exclude>");

                DriftConfiguration.Filter exclude2 = excludes.get(1);
                String path2 = exclude2.getPath();
                String pattern2 = exclude2.getPattern();

                assertNotNull(path2, "Expected to find a simple property for the path of the second <exclude>");
                assertEquals(path2, "econf", "The value is wrong for the path of the second <exclude>");

                assertNotNull(pattern2, "Expected to find a simple property for the pattern of the second <exclude>");
                assertEquals(pattern2, "*.exml", "The value is wrong for the pattern of the second <exclude>");
            }
        });

        verifyDriftConfiguration(descriptor, "TestServer", "test3", new AssertDriftTemplateRunnable() {
            @Override
            public void assertDriftTemplate(ConfigurationTemplate driftTemplate) throws Exception {
                Configuration config = driftTemplate.getConfiguration();
                DriftConfiguration dconfig = new DriftConfiguration(config);

                assertEquals(dconfig.getInterval().longValue(), 33333L);
                assertEquals(dconfig.getBasedir().getValueContext(), BaseDirValueContext.measurementTrait);
                assertEquals(dconfig.getBasedir().getValueName(), "var.lib.test3");

                List<Filter> includes = dconfig.getIncludes();

                assertNotNull(includes, "Expected to find default property set for <includes>");
                assertEquals(includes.size(), 2, "Expected <includes> property list to have two property elements.");

                DriftConfiguration.Filter include1 = includes.get(0);
                String path1 = include1.getPath();
                String pattern1 = include1.getPattern();

                assertNotNull(path1, "Expected to find a simple property for the path of the first <include>");
                assertEquals(path1, "ilib", "The value is wrong for the path of the first <include>");

                assertNotNull(pattern1, "Expected to find a simple property for the pattern of the first <include>");
                assertEquals(pattern1, "*.ijar", "The value is wrong for the pattern of the first <include>");

                DriftConfiguration.Filter include2 = includes.get(1);
                String path2 = include2.getPath();
                String pattern2 = include2.getPattern();

                assertNotNull(path2, "Expected to find a simple property for the path of the second <include>");
                assertEquals(path2, "iconf", "The value is wrong for the path of the second <include>");

                assertNotNull(pattern2, "Expected to find a simple property for the pattern of the second <include>");
                assertEquals(pattern2, "*.ixml", "The value is wrong for the pattern of the second <include>");

                List<Filter> excludes = dconfig.getExcludes();

                assertNotNull(excludes, "Expected to find default property set for <excludes>");
                assertEquals(excludes.size(), 2, "Expected <excludes> property list to have two property elements.");

                DriftConfiguration.Filter exclude1 = excludes.get(0);
                path1 = exclude1.getPath();
                pattern1 = exclude1.getPattern();

                assertNotNull(path1, "Expected to find a simple property for the path of the first <exclude>");
                assertEquals(path1, "elib", "The value is wrong for the path of the first <exclude>");

                assertNotNull(pattern1, "Expected to find a simple property for the pattern of the first <exclude>");
                assertEquals(pattern1, "*.ejar", "The value is wrong for the pattern of the first <exclude>");

                DriftConfiguration.Filter exclude2 = excludes.get(1);
                path2 = exclude2.getPath();
                pattern2 = exclude2.getPattern();

                assertNotNull(path2, "Expected to find a simple property for the path of the second <exclude>");
                assertEquals(path2, "econf", "The value is wrong for the path of the second <exclude>");

                assertNotNull(pattern2, "Expected to find a simple property for the pattern of the second <exclude>");
                assertEquals(pattern2, "*.exml", "The value is wrong for the pattern of the second <exclude>");
            }
        });

        verifyDriftConfiguration(descriptor, "TestServer", "test4", new AssertDriftTemplateRunnable() {
            @Override
            public void assertDriftTemplate(ConfigurationTemplate driftTemplate) throws Exception {
                Configuration config = driftTemplate.getConfiguration();
                DriftConfiguration dconfig = new DriftConfiguration(config);

                assertEquals(dconfig.getInterval().longValue(), 44444L);
                assertEquals(dconfig.getBasedir().getValueContext(), BaseDirValueContext.fileSystem);
                assertEquals(dconfig.getBasedir().getValueName(), "/wot/gorilla");

                assertNotNull(dconfig.getIncludes(), "though we have no includes, still expect non-null empty list");
                assertEquals(dconfig.getIncludes().size(), 0);

                assertNotNull(dconfig.getExcludes(), "though we have no excludes, still expect non-null empty list");
                assertEquals(dconfig.getExcludes().size(), 0);
            }
        });
    }

    private interface AssertDriftTemplateRunnable {
        void assertDriftTemplate(ConfigurationTemplate driftTemplate) throws Exception;
    }

    private void verifyDriftConfiguration(PluginDescriptor descriptor, String resourceTypeName, String driftConfigName,
        AssertDriftTemplateRunnable test) throws Exception {

        Map<String, PluginMetadataParser> parsersByPlugin = new HashMap<String, PluginMetadataParser>(0);
        PluginMetadataParser parser = new PluginMetadataParser(descriptor, parsersByPlugin);
        ResourceType resourceType = findResourceType(parser, resourceTypeName);
        Set<ConfigurationTemplate> driftTemplates = resourceType.getDriftConfigurationTemplates();
        ConfigurationTemplate driftTemplate = null;
        for (ConfigurationTemplate template : driftTemplates) {
            if (template.getName().equals(driftConfigName)) {
                driftTemplate = template;
                break;
            }
        }
        assertNotNull(driftTemplate, "Failed to find drift configuration template [" + driftConfigName
            + "]. The name attribute may not have been parsed correctly.");

        PropertySimple name = driftTemplate.getConfiguration().getSimple(DriftConfigurationDefinition.PROP_NAME);
        PropertySimple enabled = driftTemplate.getConfiguration().getSimple(DriftConfigurationDefinition.PROP_ENABLED);

        assertNotNull(name, "Expected to find a simple property <name> for the drift configuration name");
        assertEquals(name.getStringValue(), driftConfigName,
            "The value is wrong for the <name> property that represents the drift configuration name");

        assertNotNull(enabled, "Expected to find simple property <enabled> for the drift configuration");
        assertFalse(enabled.getBooleanValue(), "The <enabled> property should be set to a default value of false");

        test.assertDriftTemplate(driftTemplate);
    }
}
