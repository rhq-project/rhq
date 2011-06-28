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
import static org.testng.Assert.assertNotNull;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.util.ApacheDeploymentUtil.DeploymentConfig;
import org.rhq.plugins.apache.util.RuntimeApacheConfiguration;
import org.rhq.plugins.apache.util.VirtualHostLegacyResourceKeyUtil;
import org.rhq.test.TokenReplacingReader;
import org.rhq.test.pc.PluginContainerSetup;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class UpgradeSimpleConfigurationFromRHQ3_0_1Test extends UpgradeTestBase {

    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path"})
    public void testWithResolvableNamesWithSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = new String[]{"/full-configurations/2.2.x/simple/httpd.conf"};
                inventoryFile = "/mocked-inventories/rhq-3.0.1/simple/inventory-with-snmp.xml";
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;
                upgradeShouldSucceed = false;
                
                defaultOverrides = new HashMap<String, String>();
            }
            
            @Override
            public void beforeTestSetup(TestSetup testSetup) throws Throwable {
                beforeTestSetupWithSNMP(this, testSetup);
            }

            /**
             * Do our own tests here, because the generic test method won't do much, since
             * we told it that the upgrade won't succeed.
             */
            @Override
            public void beforeTests(TestSetup setup) throws Throwable {
                testWithSNMP(this, setup);
            }
        });
    }
    
    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path"})
    public void testWithUnresolvableNamesWithSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = new String[]{"/full-configurations/2.2.x/simple/httpd.conf"};
                inventoryFile = "/mocked-inventories/rhq-3.0.1/simple/inventory-with-snmp.xml";
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;
                upgradeShouldSucceed = false;
                
                defaultOverrides = new HashMap<String, String>();
                defaultOverrides.put(variableName(configurationName, "servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost1.servername.directive"), "ServerName ${unresolvable.host}:${listen1}");
                defaultOverrides.put(variableName(configurationName, "vhost2.servername.directive"), "ServerName ${unresolvable.host}:${listen2}");
                defaultOverrides.put(variableName(configurationName, "vhost3.servername.directive"), "ServerName ${unresolvable.host}:${listen3}");
                defaultOverrides.put(variableName(configurationName, "vhost4.servername.directive"), "ServerName ${unresolvable.host}:${listen4}");                                
            }
            
            @Override
            public void beforeTestSetup(TestSetup testSetup) throws Throwable {
                beforeTestSetupWithSNMP(this, testSetup);
            }

            /**
             * Do our own tests here, because the generic test method won't do much, since
             * we told it that the upgrade won't succeed.
             */
            @Override
            public void beforeTests(TestSetup setup) throws Throwable {
                testWithSNMP(this, setup);
            }
        });
    }
    
    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path"})
    public void testWithNonUniqueNamesWithSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = new String[]{"/full-configurations/2.2.x/simple/httpd.conf"};
                inventoryFile = "/mocked-inventories/rhq-3.0.1/simple/inventory-with-snmp.xml";
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;
                upgradeShouldSucceed = false;
                
                defaultOverrides = new HashMap<String, String>();
                defaultOverrides.put(variableName(configurationName, "servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost1.servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost2.servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost3.servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost4.servername.directive"), "ServerName ${unresolvable.host}");                                
            }
            
            @Override
            public void beforeTestSetup(TestSetup testSetup) throws Throwable {
                beforeTestSetupWithSNMP(this, testSetup);
            }

            /**
             * Do our own tests here, because the generic test method won't do much, since
             * we told it that the upgrade won't succeed.
             */
            @Override
            public void beforeTests(TestSetup setup) throws Throwable {
                testWithSNMP(this, setup);
            }
        });
    }
    
    /**
     * The duplicate Main vhosts "condition" was triggered by:
     * <ol>
     * <li>Disabling SNMP by putting in an invalid hostname
     * <li>Changing the URL of the apache server to some "other" value
     * <li>Running discovery
     * </ol>
     * From that point on, 2 Main vhosts live in the inventory and the inventory has the "rest"
     * of the resource keys coming from the SNMP based discovery.
     * <p>
     * It is not possible to recover from such condition automatically during
     * the resource upgrade, so the only thing we need to check here is that
     * the two main vhosts are marked as failed upgrade and that the rest of the vhosts
     * are not upgraded and have no upgrade errors attached.
     * 
     * @param installDir
     * @param exePath
     * @throws Throwable
     */
    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path"})
    public void testDuplicateMainVhostsMarkedAsFailedUpgrade(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = new String[]{"/full-configurations/2.2.x/simple/httpd.conf"};
                inventoryFile = "/mocked-inventories/rhq-3.0.1/simple/inventory-with-duplicate-main.xml";
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;
                upgradeShouldSucceed = false;
                
                defaultOverrides = new HashMap<String, String>();
            }
            
            @Override
            public void beforeTestSetup(TestSetup testSetup) throws Throwable {
                beforeTestSetupWithSNMP(this, testSetup);
            }

            /**
             * Do our own tests here, because the generic test method won't do much, since
             * we told it that the upgrade won't succeed.
             */
            @Override
            public void beforeTests(TestSetup setup) throws Throwable {
                //ok, now we should see the resources upgraded in the fake server inventory.
                ResourceType serverResourceType = findApachePluginResourceTypeByName("Apache HTTP Server");
                ResourceType vhostResourceType = findApachePluginResourceTypeByName("Apache Virtual Host");

                Set<Resource> servers = setup.getFakeInventory().findResourcesByType(serverResourceType);

                assertEquals(servers.size(), 1, "There should be exactly one apache server discovered.");

                Resource server = servers.iterator().next();

                String expectedResourceKey = ApacheServerDiscoveryComponent.formatResourceKey(this.serverRoot, this.serverRoot
                    + "/conf/httpd.conf");

                assertEquals(server.getResourceKey(), expectedResourceKey,
                    "The server resource key doesn't seem to be upgraded.");

                Set<Resource> vhosts = setup.getFakeInventory().findResourcesByType(vhostResourceType);

                assertEquals(vhosts.size(), 5, "Unexpected number of vhosts discovered found");

                //let's check that the main vhost has a an upgrade error attached to it
                ApacheServerComponent component = setup.withApacheSetup().getExecutionUtil().getServerComponent();
                ApacheDirectiveTree config = component.loadParser();
                config = RuntimeApacheConfiguration.extract(config, component.getCurrentProcessInfo(), component.getCurrentBinaryInfo(), component.getModuleNames(), false);
                VirtualHostLegacyResourceKeyUtil keyUtil = new VirtualHostLegacyResourceKeyUtil(component, config);

                String mainVhost1RK = keyUtil.getRHQ3NonSNMPLegacyMainServerResourceKey();
                String mainVhost2RK = interpret("${localhost.ip}:${listen1}", setup.getInventoryFileReplacements());
                Resource mainVhost1 = null;
                Resource mainVhost2 = null;
                for(Resource r : vhosts) {
                    if (mainVhost1RK.equals(r.getResourceKey())) {
                        mainVhost1 = r;
                        break;
                    }
                }
                for(Resource r : vhosts) {
                    if (mainVhost2RK.equals(r.getResourceKey())) {
                        mainVhost2 = r;
                        break;
                    }
                }
                
                assertNotNull(mainVhost1, "Couldn't find the main vhost with the expected resource key '" + mainVhost1RK + "'.");
                assertNotNull(mainVhost2, "Couldn't find the main vhost with the expected resource key '" + mainVhost2RK + "'.");
                
                List<ResourceError> errors = mainVhost1.getResourceErrors(ResourceErrorType.UPGRADE);
                assertNotNull(errors, "The main vhost doesn't have any upgrade errors.");
                assertEquals(errors.size(), 1, "There should be exactly one upgrade error on the main vhost.");

                errors = mainVhost2.getResourceErrors(ResourceErrorType.UPGRADE);
                assertNotNull(errors, "The main vhost doesn't have any upgrade errors.");
                assertEquals(errors.size(), 1, "There should be exactly one upgrade error on the main vhost.");
                
                //check that all other vhosts were not upgraded but have no errors
                for(Resource r : vhosts) {
                    if (r.equals(mainVhost1) || r.equals(mainVhost2)) {
                        continue;
                    }
                    
                    assertEquals(r.getResourceErrors(ResourceErrorType.UPGRADE).size(), 0, "Unexpected number of resource upgrade errors on non-main vhost " + r);
                }
            }
        });
    }
    
    private void beforeTestSetupWithSNMP(TestConfiguration testConfig, TestSetup setup) throws Exception {
        setup.withApacheSetup().init();
        ApacheServerComponent component = setup.withApacheSetup().getExecutionUtil().getServerComponent();
        ApacheDirectiveTree config = component.loadParser();
        config = RuntimeApacheConfiguration.extract(config, component.getCurrentProcessInfo(), component.getCurrentBinaryInfo(), component.getModuleNames(), false);

        DeploymentConfig deployConfig = setup.getDeploymentConfig();
        
        VirtualHostLegacyResourceKeyUtil keyUtil = new VirtualHostLegacyResourceKeyUtil(component, config);
        
        Map<String, String> replacements = deployConfig.getTokenReplacements();
        
        testConfig.defaultOverrides.put("main.rhq3.resource.key", keyUtil.getRHQ3NonSNMPLegacyMainServerResourceKey());
        
        if (deployConfig.vhost1 != null) {                    
            testConfig.defaultOverrides.put("vhost1.rhq3.resource.key", keyUtil.getRHQ3NonSNMPLegacyVirtualHostResourceKey(deployConfig.vhost1.getVHostSpec(replacements)));
        }
        
        if (deployConfig.vhost2 != null) {
            testConfig.defaultOverrides.put("vhost2.rhq3.resource.key", keyUtil.getRHQ3NonSNMPLegacyVirtualHostResourceKey(deployConfig.vhost2.getVHostSpec(replacements)));
        }
        
        if (deployConfig.vhost3 != null) {
            testConfig.defaultOverrides.put("vhost3.rhq3.resource.key", keyUtil.getRHQ3NonSNMPLegacyVirtualHostResourceKey(deployConfig.vhost3.getVHostSpec(replacements)));
        }
        
        if (deployConfig.vhost4 != null) {
            testConfig.defaultOverrides.put("vhost4.rhq3.resource.key", keyUtil.getRHQ3NonSNMPLegacyVirtualHostResourceKey(deployConfig.vhost4.getVHostSpec(replacements)));
        }
        
        setup.withDefaultOverrides(testConfig.defaultOverrides);
    }
    
    private void testWithSNMP(TestConfiguration testConfig, TestSetup setup) {
        //ok, now we should see the resources upgraded in the fake server inventory.
        ResourceType serverResourceType = findApachePluginResourceTypeByName("Apache HTTP Server");
        ResourceType vhostResourceType = findApachePluginResourceTypeByName("Apache Virtual Host");

        Set<Resource> servers = setup.getFakeInventory().findResourcesByType(serverResourceType);

        assertEquals(servers.size(), 1, "There should be exactly one apache server discovered.");

        Resource server = servers.iterator().next();

        String expectedResourceKey = ApacheServerDiscoveryComponent.formatResourceKey(testConfig.serverRoot, testConfig.serverRoot
            + "/conf/httpd.conf");

        assertEquals(server.getResourceKey(), expectedResourceKey,
            "The server resource key doesn't seem to be upgraded.");

        Set<Resource> vhosts = setup.getFakeInventory().findResourcesByType(vhostResourceType);

        assertEquals(vhosts.size(), 4, "Unexpected number of vhosts discovered found");

        //let's check that the main vhost has a an upgrade error attached to it
        ApacheServerComponent component = setup.withApacheSetup().getExecutionUtil().getServerComponent();
        ApacheDirectiveTree config = component.loadParser();
        config = RuntimeApacheConfiguration.extract(config, component.getCurrentProcessInfo(), component.getCurrentBinaryInfo(), component.getModuleNames(), false);
        VirtualHostLegacyResourceKeyUtil keyUtil = new VirtualHostLegacyResourceKeyUtil(component, config);

        String mainVhostRK = keyUtil.getRHQ3NonSNMPLegacyMainServerResourceKey();
        
        Resource mainVhost = null;
        for(Resource r : vhosts) {
            if (mainVhostRK.equals(r.getResourceKey())) {
                mainVhost = r;
                break;
            }
        }
        
        assertNotNull(mainVhost, "Couldn't find the main vhost with the expected resource key '" + mainVhostRK + "'.");
        
        List<ResourceError> errors = mainVhost.getResourceErrors(ResourceErrorType.UPGRADE);
        assertNotNull(errors, "The main vhost doesn't have any upgrade errors.");
        assertEquals(errors.size(), 1, "There should be exactly one upgrade error on the main vhost.");
        
        //check that all other vhosts were not upgraded but have no errors
        for(Resource r : vhosts) {
            if (r.equals(mainVhost)) {
                continue;
            }
            
            assertEquals(r.getResourceErrors(ResourceErrorType.UPGRADE).size(), 0, "Unexpected number of resource upgrade errors on non-main vhost " + r);
        }
    }    
    
    private String interpret(String string, Map<String, String> variables) {
        return StreamUtil.slurp(new TokenReplacingReader(new StringReader(string), variables));
    }
}
