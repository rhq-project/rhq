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

import static org.testng.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.PluginContainerDeployment;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.plugins.apache.util.ApacheDeploymentUtil;
import org.rhq.plugins.apache.util.ApacheDeploymentUtil.DeploymentConfig;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.platform.PlatformComponent;
import org.rhq.test.pc.PluginContainerSetup;
import org.rhq.test.pc.PluginContainerTest;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class UpgradeTest extends PluginContainerTest {

    private static final String PLATFORM_PLUGIN = "file:target/itest/plugins/rhq-platform-plugin-for-apache-test.jar";
    private static final String AUGEAS_PLUGIN = "file:target/itest/plugins/rhq-augeas-plugin-for-apache-test.jar";
    private static final String APACHE_PLUGIN = "file:target/itest/plugins/rhq-apache-plugin-for-apache-test.jar";

    private static final String DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES = "simpleWithResolvableServerNames";

    private List<ResourceType> resourceTypesInApachePlugin;

    private static class DiscoveryInput {
        public String serverRootPath;
        public String exePath;
        public String httpdConfPath;
        public String snmpHost;
        public int snmpPort;
        public String pingUrl;
    }

    @BeforeClass
    public void parseResourceTypesFromApachePlugin() throws Exception {
        PluginDescriptor descriptor = AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(new URI(APACHE_PLUGIN)
            .toURL());
        PluginMetadataParser parser = new PluginMetadataParser(descriptor,
            Collections.<String, PluginMetadataParser> emptyMap());

        resourceTypesInApachePlugin = parser.getAllTypes();
    }

    @AfterClass
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void shutdownApache(String apacheInstallationDirectory,
        String exePath) throws Exception {
        
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
    public void testSimpleConfigurationWithResolvableServerNames_Apache2_upgradeFromRHQ1_3(String apacheInstallationDirectory,
        String exePath) throws Exception {

        final ServerServices ss = getCurrentPluginContainerConfiguration().getServerServices();
        
        context.checking(new Expectations() {
            {
                allowing(ss.getDiscoveryServerService().mergeInventoryReport(with(any(InventoryReport.class))));
                //will(/* push the mocked inventory down to the agent */);
            }
        });

        prepareApacheForTest(DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES, apacheInstallationDirectory, exePath,
            Arrays.asList("/full-configurations/simple/httpd.conf", "/snmpd.conf"));

        startConfiguredPluginContainer();
        
        //TODO check the results of the upgrade
    }

    private void prepareApacheForTest(String configurationName, String apacheInstallationDirectory,
        String apacheExePath, List<String> configurationFiles) throws Exception {
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

        serverComponent.invokeOperation("restart", null);
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

    private ResourceType findApachePluginResourceTypeByName(String resourceTypeName) {
        for (ResourceType rt : resourceTypesInApachePlugin) {
            if (resourceTypeName.equals(rt.getName())) {
                return rt;
            }
        }

        return null;
    }
}
