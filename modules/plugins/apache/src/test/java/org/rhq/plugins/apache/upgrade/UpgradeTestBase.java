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

package org.rhq.plugins.apache.upgrade;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jmock.Expectations;
import org.testng.annotations.BeforeClass;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataParser;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.upgrade.FakeServerInventory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.PluginContainerDeployment;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.plugins.apache.ApacheServerDiscoveryComponent;
import org.rhq.plugins.apache.ApacheVirtualHostServiceComponent;
import org.rhq.plugins.apache.ApacheVirtualHostServiceDiscoveryComponent;
import org.rhq.plugins.apache.parser.ApacheConfigReader;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.parser.ApacheParser;
import org.rhq.plugins.apache.parser.ApacheParserImpl;
import org.rhq.plugins.apache.util.ApacheDeploymentUtil;
import org.rhq.plugins.apache.util.ApacheDeploymentUtil.DeploymentConfig;
import org.rhq.plugins.apache.util.ApacheExecutionUtil;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.apache.util.VHostSpec;
import org.rhq.test.ObjectCollectionSerializer;
import org.rhq.test.TokenReplacingReader;
import org.rhq.test.pc.PluginContainerTest;


/**
 * Base class for the upgrade test classes.
 *
 * @author Lukas Krejci
 */
public class UpgradeTestBase extends PluginContainerTest {

    private static final Log LOG = LogFactory.getLog(UpgradeTestBase.class);
    
    protected class TestSetup {
        private String configurationName;
        private FakeServerInventory fakeInventory = new FakeServerInventory();
        private String inventoryFile;
        private Resource platform;
        private ApacheSetup apacheSetup = new ApacheSetup();
        private DeploymentConfig deploymentConfig;
        private Map<String, String> defaultOverrides = new HashMap<String, String>();
        private Map<String, String> inventoryFileReplacements;
        
        public class ApacheSetup {
            private String serverRoot;
            private String exePath;
            private Collection<String> configurationFiles;
            private ApacheExecutionUtil execution;
            private boolean deploy = true;
            
            private ApacheSetup() {
    
            }
    
            public ApacheSetup withServerRoot(String serverRoot) {
                this.serverRoot = serverRoot;
                //auto-define the server root property as if it was passed on the commandline
                System.getProperties().put(configurationName + ".server.root", serverRoot);
                deploymentConfig = ApacheDeploymentUtil.getDeploymentConfigurationFromSystemProperties(configurationName, defaultOverrides);
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
    
            public ApacheSetup withDeploymentOnSetup() {
                this.deploy = true;
                return this;
            }
    
            public ApacheSetup withNoDeploymentOnSetup() {
                this.deploy = false;
                return this;
            }
    
            public ApacheExecutionUtil getExecutionUtil() {
                return execution;
            }
    
            public void init() throws Exception {
                File serverRootDir = new File(serverRoot);
    
                assertTrue(serverRootDir.exists(), "The configured server root denotes a non-existant directory: '"
                    + serverRootDir + "'.");
    
                File confDir = new File(serverRootDir, "conf");
    
                assertTrue(confDir.exists(),
                    "The configured server root denotes a directory that doesn't have a 'conf' subdirectory. This is unexpected.");
    
                String confFilePath = confDir.getAbsolutePath() + File.separatorChar + "httpd.conf";
                
                String snmpHost = null;
                int snmpPort = 0;
                String pingUrl = null;
    
                if (configurationName != null) {
                    if (deploy) {
                        ApacheDeploymentUtil.deployConfiguration(confDir, configurationFiles, deploymentConfig);
                    }
                    
                    //ok, now try to find the ping URL. The best thing is to actually invoke
                    //the same code the apache server discovery does.
                    ApacheDirectiveTree tree = new ApacheDirectiveTree();
                    ApacheParser parser = new ApacheParserImpl(tree, serverRootDir.getAbsolutePath());
                    ApacheConfigReader.buildTree(confFilePath, parser);
                    
                    //XXX this hardcodes apache2 as the only option we have...
                    HttpdAddressUtility.Address addrToUse = HttpdAddressUtility.APACHE_2_x.getMainServerSampleAddress(tree, null, -1);
                    pingUrl = addrToUse.toString();
    
                    snmpHost = deploymentConfig.snmpHost;
                    snmpPort = deploymentConfig.snmpPort;
                }
    
                execution = new ApacheExecutionUtil(findApachePluginResourceTypeByName("Apache HTTP Server"),
                    serverRoot, exePath, confFilePath, pingUrl,
                    snmpHost, snmpPort);
                execution.init();
            }
    
            private void doSetup() throws Exception {
                init();
                execution.invokeOperation("start");
            }
    
            public TestSetup setup() throws Exception {
                return TestSetup.this.setup();
            }
        }
    
        public TestSetup(String configurationName) {
            this.configurationName = configurationName;
            deploymentConfig = ApacheDeploymentUtil.getDeploymentConfigurationFromSystemProperties(configurationName, defaultOverrides);
        }
    
        public TestSetup withInventoryFrom(String classPathUri) {
            inventoryFile = classPathUri;
            return this;
        }

        public TestSetup withDefaultOverrides(Map<String, String> defaultOverrides) {            
            this.defaultOverrides = defaultOverrides == null ? new HashMap<String, String>() : defaultOverrides;
            deploymentConfig = ApacheDeploymentUtil.getDeploymentConfigurationFromSystemProperties(configurationName, this.defaultOverrides);
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
    
            expectations.allowing(ss.getDiscoveryServerService()).mergeInventoryReport(
                expectations.with(Expectations.any(InventoryReport.class)));
            expectations.will(fakeInventory.mergeInventoryReport(InventoryStatus.COMMITTED));
    
            expectations.allowing(ss.getDiscoveryServerService()).upgradeResources(
                expectations.with(Expectations.any(Set.class)));
            expectations.will(fakeInventory.upgradeResources());
    
            expectations.allowing(ss.getDiscoveryServerService()).getResources(
                expectations.with(Expectations.any(Set.class)), expectations.with(Expectations.any(boolean.class)));
            expectations.will(fakeInventory.getResources());
    
            expectations.allowing(ss.getDiscoveryServerService()).setResourceError(expectations.with(Expectations.any(ResourceError.class)));
            expectations.will(fakeInventory.setResourceError());
            
            expectations.allowing(ss.getDiscoveryServerService()).mergeAvailabilityReport(
                expectations.with(Expectations.any(AvailabilityReport.class)));
    
            expectations.allowing(ss.getDiscoveryServerService()).postProcessNewlyCommittedResources(
                expectations.with(Expectations.any(Set.class)));
    
            expectations.allowing(ss.getDiscoveryServerService()).clearResourceConfigError(
                expectations.with(Expectations.any(int.class)));
            
            expectations.ignoring(ss.getBundleServerService());
            expectations.ignoring(ss.getConfigurationServerService());
            expectations.ignoring(ss.getContentServerService());
            expectations.ignoring(ss.getCoreServerService());
            expectations.ignoring(ss.getEventServerService());
            expectations.ignoring(ss.getMeasurementServerService());
            expectations.ignoring(ss.getOperationServerService());
            expectations.ignoring(ss.getResourceFactoryServerService());
        }
    
        public FakeServerInventory getFakeInventory() {
            return fakeInventory;
        }
    
        public DeploymentConfig getDeploymentConfig() {
            return deploymentConfig;
        }
        
        public TestSetup setup() throws Exception {
            apacheSetup.doSetup();
    
            Map<String, String> replacements = deploymentConfig.getTokenReplacements();                  
            replacements.put("server.root", apacheSetup.serverRoot);
            replacements.put("exe.path", apacheSetup.exePath);
            
            ApacheDeploymentUtil.addDefaultVariables(replacements, null);
    
            HttpdAddressUtility addressUtility = apacheSetup.getExecutionUtil().getServerComponent()
                .getAddressUtility();
            ApacheDirectiveTree runtimeConfig = apacheSetup.getExecutionUtil().getRuntimeConfiguration();
    
            replacements.put("snmp.identifier",
                addressUtility.getHttpdInternalMainServerAddressRepresentation(runtimeConfig).toString(false, false));
    
            replacements.put("main.rhq4.resource.key", ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY);
            
            VHostSpec vhost1 = deploymentConfig.vhost1 == null ? null : deploymentConfig.vhost1.getVHostSpec(replacements);
            VHostSpec vhost2 = deploymentConfig.vhost2 == null ? null : deploymentConfig.vhost2.getVHostSpec(replacements);
            VHostSpec vhost3 = deploymentConfig.vhost3 == null ? null : deploymentConfig.vhost3.getVHostSpec(replacements);
            VHostSpec vhost4 = deploymentConfig.vhost4 == null ? null : deploymentConfig.vhost4.getVHostSpec(replacements);
            
            if (vhost1 != null) {
                replacements.put(
                    "vhost1.snmp.identifier",
                    addressUtility.getHttpdInternalVirtualHostAddressRepresentation(runtimeConfig, vhost1.hosts.get(0),
                        vhost1.serverName).toString(false, false));
                
                replacements.put(
                    "vhost1.rhq4.resource.key",
                    ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                        vhost1.serverName, vhost1.hosts));
            }
    
            if (vhost2 != null) {
                replacements.put(
                    "vhost2.snmp.identifier",
                    addressUtility.getHttpdInternalVirtualHostAddressRepresentation(runtimeConfig, vhost2.hosts.get(0),
                        vhost2.serverName).toString(false, false));
                
                replacements.put(
                    "vhost2.rhq4.resource.key",
                    ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                        vhost2.serverName, vhost2.hosts));
            }
    
            if (vhost3 != null) {
                replacements.put(
                    "vhost3.snmp.identifier",
                    addressUtility.getHttpdInternalVirtualHostAddressRepresentation(runtimeConfig, vhost3.hosts.get(0),
                        vhost3.serverName).toString(false, false));
                
                replacements.put(
                    "vhost3.rhq4.resource.key",
                    ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                        vhost3.serverName, vhost3.hosts));
            }
    
            if (vhost4 != null) {
                replacements.put(
                    "vhost4.snmp.identifier",
                    addressUtility.getHttpdInternalVirtualHostAddressRepresentation(runtimeConfig, vhost4.hosts.get(0),
                        vhost4.serverName).toString(false, false));
                
                replacements.put(
                    "vhost4.rhq4.resource.key",
                    ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                        vhost4.serverName, vhost4.hosts));
            }
    
            //let the user override everything we just did
            replacements.putAll(defaultOverrides);
            
            inventoryFileReplacements = replacements;
            
            InputStream dataStream = getClass().getResourceAsStream(inventoryFile);
    
            Reader rdr = new TokenReplacingReader(new InputStreamReader(dataStream), replacements);
    
            @SuppressWarnings("unchecked")
            List<Resource> inventory = (List<Resource>) new ObjectCollectionSerializer().deserialize(rdr);
    
            //fix up the parent relationships, because they might not be reconstructed correctly by 
            //JAXB - we're missing XmlID and XmlIDRef annotations in our model
            fixupParent(null, inventory);
    
            fakeInventory.prepopulateInventory(platform, inventory);
    
            return this;
        }
    
        /**
         * After the setup, this returns all the variables used to update the tokens in the inventory file. 
         * 
         * @return
         */
        public Map<String, String> getInventoryFileReplacements() {
            return inventoryFileReplacements;
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

    protected static class TestConfiguration {
        public String[] apacheConfigurationFiles;
        public String inventoryFile;
        public String configurationName;
        public String serverRoot;
        public String binPath;
        public Map<String, String> defaultOverrides = new HashMap<String, String>();
        public boolean upgradeShouldSucceed = true;
        
        public void beforeTestSetup(TestSetup testSetup) throws Throwable {
            
        }
        
        public void beforePluginContainerStart(TestSetup setup) throws Throwable {
            
        }
        
        public void beforeTests(TestSetup setup) throws Throwable {
            
        }
    }

    protected static final String PLATFORM_PLUGIN = "file:target/itest/plugins/rhq-platform-plugin-for-apache-test.jar";
    protected static final String AUGEAS_PLUGIN = "file:target/itest/plugins/rhq-augeas-plugin-for-apache-test.jar";
    protected static final String APACHE_PLUGIN = "file:target/itest/plugins/rhq-apache-plugin-for-apache-test.jar";

    protected static final String DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES = "simpleWithResolvableServerNames";
    protected static final String DEPLOYMENT_SIMPLE_WITH_UNRESOLVABLE_SERVER_NAMES = "simpleWithUnresolvableServerNames";
    protected static final String DEPLOYMENT_SIMPLE_WITH_WILDCARD_LISTENS = "simpleWithWildcardListens";
    
    private List<ResourceType> resourceTypesInApachePlugin;
    protected Resource platform;

    @BeforeClass
    public void parseResourceTypesFromApachePlugin() throws Exception {
        resourceTypesInApachePlugin = getResourceTypesInPlugin(APACHE_PLUGIN);
        platform = discoverPlatform();
    }

    protected ResourceType findApachePluginResourceTypeByName(String resourceTypeName) {
        for (ResourceType rt : resourceTypesInApachePlugin) {
            if (resourceTypeName.equals(rt.getName())) {
                return rt;
            }
        }
    
        return null;
    }

    protected void testUpgrade(TestConfiguration testConfiguration) throws Throwable {
        final TestSetup setup = new TestSetup(testConfiguration.configurationName);
        boolean testFailed = false;
        try {
            
            String[] configFiles = Arrays.copyOf(testConfiguration.apacheConfigurationFiles, testConfiguration.apacheConfigurationFiles.length + 2);
            configFiles[testConfiguration.apacheConfigurationFiles.length] = "/snmpd.conf";
            configFiles[testConfiguration.apacheConfigurationFiles.length + 1] = "/mime.types";
            
            setup.withInventoryFrom(testConfiguration.inventoryFile)
                .withPlatformResource(platform).withDefaultExpectations().withDefaultOverrides(testConfiguration.defaultOverrides)
                .withApacheSetup().withConfigurationFiles(configFiles)
                .withServerRoot(testConfiguration.serverRoot).withExePath(testConfiguration.binPath);
            
            testConfiguration.beforeTestSetup(setup);
            
            setup.setup();
    
            testConfiguration.beforePluginContainerStart(setup);
            
            startConfiguredPluginContainer();
    
            testConfiguration.beforeTests(setup);
            
            if (!testConfiguration.upgradeShouldSucceed) {
                return;
            }
            
            //ok, now we should see the resources upgraded in the fake server inventory.
            ResourceType serverResourceType = findApachePluginResourceTypeByName("Apache HTTP Server");
            ResourceType vhostResourceType = findApachePluginResourceTypeByName("Apache Virtual Host");
    
            Set<Resource> servers = setup.getFakeInventory().findResourcesByType(serverResourceType);

            assertEquals(servers.size(), 1, "There should be exactly one apache server discovered.");
    
            Resource server = servers.iterator().next();
    
            String expectedResourceKey = ApacheServerDiscoveryComponent.formatResourceKey(testConfiguration.serverRoot, testConfiguration.serverRoot
                + "/conf/httpd.conf");
    
            assertEquals(server.getResourceKey(), expectedResourceKey,
                "The server resource key doesn't seem to be upgraded.");
    
            Set<Resource> vhosts = setup.getFakeInventory().findResourcesByType(vhostResourceType);
    
            assertEquals(vhosts.size(), 5, "Unexpected number of vhosts discovered found");
    
            List<String> expectedResourceKeys = new ArrayList<String>(5);
    
            DeploymentConfig dc = setup.getDeploymentConfig();
            Map<String, String> replacements = dc.getTokenReplacements();
            
            VHostSpec vh1 = dc.vhost1.getVHostSpec(replacements);
            VHostSpec vh2 = dc.vhost2.getVHostSpec(replacements);
            VHostSpec vh3 = dc.vhost3.getVHostSpec(replacements);
            VHostSpec vh4 = dc.vhost4.getVHostSpec(replacements);
            
            expectedResourceKeys.add(ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY);
            expectedResourceKeys.add(ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                vh1.serverName, vh1.hosts));
            expectedResourceKeys.add(ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                vh2.serverName, vh2.hosts));
            expectedResourceKeys.add(ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                vh3.serverName, vh3.hosts));
            expectedResourceKeys.add(ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                vh4.serverName, vh4.hosts));
    
            for (Resource vhost : vhosts) {
                assertTrue(expectedResourceKeys.contains(vhost.getResourceKey()),
                    "Unexpected virtual host resource key: '" + vhost.getResourceKey() + "'. Only expecting " + expectedResourceKeys);
            }
        } catch (Throwable t) {
            testFailed = true;
            LOG.error("Error during test upgrade execution.", t);
            throw t;
        } finally {
            try {
                setup.withApacheSetup().getExecutionUtil().invokeOperation("stop");
            } catch (Exception e) {
                if (testFailed) {
                    LOG.error("Failed to stop apache.", e);
                } else {
                    throw e;
                }
            }
        }
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

    protected static String variableName(String prefix, String name) {
        StringBuilder bld = new StringBuilder();
        if (prefix != null && !prefix.isEmpty()) {
            bld.append(prefix).append(".");
        }
        
        bld.append(name);
        
        return bld.toString();
    }
    
    protected static InetAddress determineLocalhost() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            try {
                return InetAddress.getByName("127.0.0.1");
            } catch (UnknownHostException ee) {
                //doesn't happen
                return null;
            }
        }
    }
}
