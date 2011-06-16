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

package org.rhq.plugins.apache;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jmock.Expectations;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataParser;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.upgrade.FakeServerInventory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.PluginContainerDeployment;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.plugins.apache.util.ApacheDeploymentUtil;
import org.rhq.plugins.apache.util.ApacheDeploymentUtil.DeploymentConfig;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.apache.util.TokenReplacingReader;
import org.rhq.plugins.platform.PlatformComponent;
import org.rhq.test.ObjectCollectionSerializer;
import org.rhq.test.pc.PluginContainerSetup;
import org.rhq.test.pc.PluginContainerTest;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test(groups = "apache-integration-tests")
public class UpgradeTest extends PluginContainerTest {

    private static final String PLATFORM_PLUGIN = "file:target/itest/plugins/rhq-platform-plugin-for-apache-test.jar";
    private static final String AUGEAS_PLUGIN = "file:target/itest/plugins/rhq-augeas-plugin-for-apache-test.jar";
    private static final String APACHE_PLUGIN = "file:target/itest/plugins/rhq-apache-plugin-for-apache-test.jar";

    private static final String DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES = "simpleWithResolvableServerNames";

    private List<ResourceType> resourceTypesInApachePlugin;

    private Resource platform;

    private static class DiscoveryInput {
        public String serverRootPath;
        public String exePath;
        public String httpdConfPath;
        public String snmpHost;
        public int snmpPort;
        public String pingUrl;
    }

    private class TestSetup {
        private String configurationName;
        private FakeServerInventory fakeInventory = new FakeServerInventory();
        private String inventoryFile;
        private Resource platform;
        private ApacheSetup apacheSetup = new ApacheSetup();

        public class ApacheSetup {
            private String serverRoot;
            private String exePath;
            private Collection<String> configurationFiles;

            private ApacheSetup() {

            }

            public ApacheSetup withServerRoot(String serverRoot) {
                this.serverRoot = serverRoot;
                return this;
            }

            public ApacheSetup withExePath(String exePath) {
                this.exePath = exePath;
                return this;
            }

            public ApacheSetup withConfigurationFiles(String... classPathUris) {
                return withConfigurationFiles(Arrays.asList(classPathUris));
            }

            public ApacheSetup withConfigurationFiles(Collection<String> classPathUris) {
                this.configurationFiles = classPathUris;
                return this;
            }

            private void doSetup() throws Exception {
                prepareApacheForTest(TestSetup.this.configurationName, serverRoot, exePath, configurationFiles);
            }

            public TestSetup setup() throws Exception {
                return TestSetup.this.setup();
            }

            private void prepareApacheForTest(String configurationName, String apacheInstallationDirectory,
                String apacheExePath, Collection<String> configurationFiles) throws Exception {
                File apacheInstallationDir = new File(apacheInstallationDirectory);

                assertTrue(apacheInstallationDir.exists(),
                    "The 'apache2.install.dir' system property denotes a non-existant directory: '"
                        + apacheInstallationDirectory + "'.");

                File confDir = new File(apacheInstallationDir, "conf");

                assertTrue(
                    confDir.exists(),
                    "The 'apache2.install.dir' system property denotes a directory that doesn't have a 'conf' subdirectory. This is unexpected.");

                DeploymentConfig deploymentConfig = ApacheDeploymentUtil
                    .getDeploymentConfigurationFromSystemProperties(configurationName);

                ApacheDeploymentUtil.deployConfiguration(confDir, configurationFiles, deploymentConfig);

                DiscoveryInput discoveryInput = new DiscoveryInput();
                discoveryInput.serverRootPath = apacheInstallationDirectory;
                discoveryInput.exePath = apacheExePath;
                discoveryInput.httpdConfPath = confDir.getAbsolutePath() + File.separatorChar + "httpd.conf";
                discoveryInput.snmpHost = deploymentConfig.snmpHost;
                discoveryInput.snmpPort = deploymentConfig.snmpPort;

                HttpdAddressUtility.Address addr = deploymentConfig.mainServer.address1;
                HttpdAddressUtility.Address addrToUse = new HttpdAddressUtility.Address(null, null,
                    HttpdAddressUtility.Address.NO_PORT_SPECIFIED_VALUE);
                addrToUse.scheme = addr.scheme == null ? "http" : addr.scheme;
                addrToUse.host = addr.host == null ? "localhost" : addr.host;
                addrToUse.port = addr.port;

                discoveryInput.pingUrl = addrToUse.toString();

                restartApache(discoveryInput);
            }

            private void restartApache(DiscoveryInput discoveryInput) throws Exception {
                ApacheServerComponent serverComponent = findAndStartServerComponent(discoveryInput);

                try {
                    serverComponent.invokeOperation("restart", null);
                } catch (Exception e) {
                    serverComponent.invokeOperation("start", null);
                }
            }
        }

        public TestSetup(String configurationName) {
            this.configurationName = configurationName;
        }

        public TestSetup withInventoryFrom(String classPathUri) {
            inventoryFile = classPathUri;
            return this;
        }

        public TestSetup withPlatformResource(Resource platform) {
            this.platform = platform;
            return this;
        }

        public ApacheSetup withApacheSetup() {
            return apacheSetup;
        }

        public TestSetup withDefaultExpectations() throws Exception {
            context.checking(new Expectations() {
                {
                    addDefaultExceptations(this);
                }
            });

            return this;
        }

        @SuppressWarnings("unchecked")
        public void addDefaultExceptations(Expectations expectations) throws Exception {
            ServerServices ss = getCurrentPluginContainerConfiguration().getServerServices();
            
            expectations.allowing(ss.getDiscoveryServerService()).mergeInventoryReport(expectations.with(Expectations.any(InventoryReport.class)));
            expectations.will(fakeInventory.mergeInventoryReport(InventoryStatus.COMMITTED));

            expectations.allowing(ss.getDiscoveryServerService()).upgradeResources(expectations.with(Expectations.any(Set.class)));
            expectations.will(fakeInventory.upgradeResources());
            
            expectations.allowing(ss.getDiscoveryServerService()).getResources(expectations.with(Expectations.any(Set.class)), expectations.with(Expectations.any(boolean.class)));
            expectations.will(fakeInventory.getResources());
            
            expectations.allowing(ss.getDiscoveryServerService()).mergeAvailabilityReport(expectations.with(Expectations.any(AvailabilityReport.class)));
            expectations.allowing(ss.getDiscoveryServerService()).postProcessNewlyCommittedResources(expectations.with(Expectations.any(Set.class)));
        }
        
        public FakeServerInventory getFakeInventory() {
            return fakeInventory;
        }

        public TestSetup setup() throws Exception {
            Map<String, String> replacements = new HashMap<String, String>();
            replacements.put("server.root", apacheSetup.serverRoot);
            replacements.put("exe.path", apacheSetup.exePath);

            InputStream dataStream = getClass().getResourceAsStream(inventoryFile);

            Reader rdr = new TokenReplacingReader(new InputStreamReader(dataStream), replacements);

            @SuppressWarnings("unchecked")
            List<Resource> inventory = (List<Resource>) new ObjectCollectionSerializer().deserialize(rdr);
            
            //fix up the parent relationships, because they might not be reconstructed correctly by 
            //JAXB - we're missing XmlID and XmlIDRef annotations in our model
            fixupParent(null, inventory);
            
            fakeInventory.prepopulateInventory(platform, inventory);

            apacheSetup.doSetup();

            return this;
        }
        
        private void fixupParent(Resource parent, Collection<Resource> children) {
            for (Resource child : children) {
                child.setParentResource(parent);
                if (child.getChildResources() != null) {
                    fixupParent(child, child.getChildResources());
                }
            }
        }
    }

    @BeforeClass
    public void parseResourceTypesFromApachePlugin() throws Exception {
        resourceTypesInApachePlugin = getResourceTypesInPlugin(APACHE_PLUGIN);
        platform = discoverPlatform();
    }

    @AfterClass
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void shutdownApache(String apacheInstallationDirectory, String exePath) throws Exception {

        File apacheInstallationDir = new File(apacheInstallationDirectory);
        File confDir = new File(apacheInstallationDir, "conf");

        //it really doesn't matter which configuration i use here
        DeploymentConfig deploymentConfig = ApacheDeploymentUtil
            .getDeploymentConfigurationFromSystemProperties(DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES);

        DiscoveryInput discoveryInput = new DiscoveryInput();
        discoveryInput.serverRootPath = apacheInstallationDirectory;
        discoveryInput.exePath = exePath;
        discoveryInput.httpdConfPath = confDir.getAbsolutePath() + File.separatorChar + "httpd.conf";
        discoveryInput.snmpHost = deploymentConfig.snmpHost;
        discoveryInput.snmpPort = deploymentConfig.snmpPort;

        HttpdAddressUtility.Address addr = deploymentConfig.mainServer.address1;
        HttpdAddressUtility.Address addrToUse = new HttpdAddressUtility.Address(null, null,
            HttpdAddressUtility.Address.NO_PORT_SPECIFIED_VALUE);
        addrToUse.scheme = addr.scheme == null ? "http" : addr.scheme;
        addrToUse.host = addr.host == null ? "localhost" : addr.host;
        addrToUse.port = addr.port;

        discoveryInput.pingUrl = addrToUse.toString();

        ApacheServerComponent serverComponent = findAndStartServerComponent(discoveryInput);

        serverComponent.invokeOperation("stop", null);
    }

    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testSimpleConfigurationWithResolvableServerNames_Apache2_upgradeFromRHQ1_3(
        String apacheInstallationDirectory, String exePath) throws Exception {

        final TestSetup setup = new TestSetup(DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES)
            .withInventoryFrom("/mocked-inventories/rhq-1.3.x/includes/inventory.xml").withPlatformResource(platform)
            .withDefaultExpectations().withApacheSetup()
            .withConfigurationFiles("/full-configurations/simple/httpd.conf", "/snmpd.conf", "/mime.types")
            .withServerRoot(apacheInstallationDirectory).withExePath(exePath).setup();

        startConfiguredPluginContainer();

        //ok, now we should see the resources upgraded in the fake server inventory.
        ResourceType serverResourceType = findApachePluginResourceTypeByName("Apache HTTP Server");
        ResourceType vhostResourceType = findApachePluginResourceTypeByName("Apache Virtual Host");

        Set<Resource> servers = setup.getFakeInventory().findResourcesByType(serverResourceType);

        assertTrue(servers.size() == 1, "There should be exactly one apache server discovered.");

        Resource server = servers.iterator().next();

        String expectedResourceKey = ApacheServerDiscoveryComponent.formatResourceKey(apacheInstallationDirectory,
            apacheInstallationDirectory + "/conf/httpd.conf");

        assertEquals(server.getResourceKey(), expectedResourceKey,
            "The server resource key doesn't seem to be upgraded.");

        //TODO test the vhosts
    }

    private ResourceType findApachePluginResourceTypeByName(String resourceTypeName) {
        for (ResourceType rt : resourceTypesInApachePlugin) {
            if (resourceTypeName.equals(rt.getName())) {
                return rt;
            }
        }

        return null;
    }

    private static List<ResourceType> getResourceTypesInPlugin(String pluginUri) throws Exception {
        PluginDescriptor descriptor = AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(new URI(pluginUri).toURL());
        PluginMetadataParser parser = new PluginMetadataParser(descriptor,
            Collections.<String, PluginMetadataParser> emptyMap());

        return parser.getAllTypes();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Resource discoverPlatform() throws Exception {
        PluginDescriptor descriptor = AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(new URI(PLATFORM_PLUGIN)
            .toURL());
        PluginMetadataParser parser = new PluginMetadataParser(descriptor,
            Collections.<String, PluginMetadataParser> emptyMap());

        List<ResourceType> platformTypes = parser.getAllTypes();

        for (ResourceType rt : platformTypes) {
            Class discoveryClass = Class.forName(parser.getDiscoveryComponentClass(rt));

            ResourceDiscoveryComponent discoveryComponent = (ResourceDiscoveryComponent) discoveryClass.newInstance();

            ResourceDiscoveryContext context = new ResourceDiscoveryContext(rt, null, null,
                SystemInfoFactory.createSystemInfo(), null, null, PluginContainerDeployment.AGENT);

            Set<DiscoveredResourceDetails> results = discoveryComponent.discoverResources(context);

            if (!results.isEmpty()) {
                DiscoveredResourceDetails details = results.iterator().next();

                Resource platform = new Resource();

                platform.setDescription(details.getResourceDescription());
                platform.setResourceKey(details.getResourceKey());
                platform.setName(details.getResourceName());
                platform.setVersion(details.getResourceVersion());
                platform.setPluginConfiguration(details.getPluginConfiguration());
                platform.setResourceType(rt);
                platform.setUuid(UUID.randomUUID().toString());
                platform.setId(1);
                
                return platform;
            }
        }

        return null;
    }

    private ApacheServerComponent findAndStartServerComponent(DiscoveryInput discoveryInput) throws Exception {
        ApacheServerDiscoveryComponent discoveryComponent = new ApacheServerDiscoveryComponent();

        ResourceType resourceType = findApachePluginResourceTypeByName("Apache HTTP Server");
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

        ResourceDiscoveryContext<PlatformComponent> discoveryContext = new ResourceDiscoveryContext<PlatformComponent>(
            resourceType, null, null, systemInfo, null, null, PluginContainerDeployment.AGENT);

        Configuration config = discoveryContext.getDefaultPluginConfiguration();
        config.put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT,
            discoveryInput.serverRootPath));
        config
            .put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_EXECUTABLE_PATH, discoveryInput.exePath));
        config
            .put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_HTTPD_CONF, discoveryInput.httpdConfPath));
        config
            .put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_SNMP_AGENT_HOST, discoveryInput.snmpHost));
        config
            .put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_SNMP_AGENT_PORT, discoveryInput.snmpPort));
        config.put(new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_URL, discoveryInput.pingUrl));

        DiscoveredResourceDetails result = discoveryComponent.discoverResource(config, discoveryContext);

        ApacheServerComponent serverComponent = new ApacheServerComponent();

        Resource resource = new Resource(result.getResourceKey(), null, resourceType);
        resource.setPluginConfiguration(config);
        ResourceContext<PlatformComponent> resourceContext = new ResourceContext<PlatformComponent>(resource, null,
            null, systemInfo, null, null, null, null, null, null, null, null);

        serverComponent.start(resourceContext);
        return serverComponent;
    }
}
