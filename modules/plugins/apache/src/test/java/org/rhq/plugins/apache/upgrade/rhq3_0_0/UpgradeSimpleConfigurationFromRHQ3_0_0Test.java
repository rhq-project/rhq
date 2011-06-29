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

package org.rhq.plugins.apache.upgrade.rhq3_0_0;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.plugins.apache.ApacheServerComponent;
import org.rhq.plugins.apache.ApacheServerDiscoveryComponent;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.upgrade.UpgradeTestBase;
import org.rhq.plugins.apache.upgrade.UpgradeTestBase.TestConfiguration;
import org.rhq.plugins.apache.upgrade.UpgradeTestBase.TestSetup;
import org.rhq.plugins.apache.util.ApacheDeploymentUtil.DeploymentConfig;
import org.rhq.plugins.apache.util.RuntimeApacheConfiguration;
import org.rhq.plugins.apache.util.VirtualHostLegacyResourceKeyUtil;
import org.rhq.test.pc.PluginContainerSetup;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class UpgradeSimpleConfigurationFromRHQ3_0_0Test extends UpgradeTestBase {
    
    /**
     * This tests the upgrade from RHQ 3.0.0 where the apache server was discovered without SNMP support.
     * The Main Server URL must have been set in this case, otherwise the component would fail to start
     * and therefore the vhost discovery wouldn't even take place.
     */
    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN}, numberOfInitialDiscoveries = 2)
    @Parameters({"apache2.install.dir", "apache2.exe.path"})
    public void testWithResolvableNamesWithoutSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = new String[]{"/full-configurations/2.2.x/simple/httpd.conf"};
                inventoryFile = "/mocked-inventories/rhq-3.0.0/simple/inventory-without-snmp.xml";
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;
                
                //just define the servername value without actually setting the ${servername.directive} so that
                //we don't define a servername directive itself but do have a value for the actual server name.
                //this is deduced by apache and the plugin but tests aren't that clever.
                defaultOverrides.put("servername", "${localhost}");
            }
        });
    }
    
    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN}, numberOfInitialDiscoveries = 2)
    @Parameters({"apache2.install.dir", "apache2.exe.path"})
    public void testWithUnresolvableNamesWithoutSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = new String[]{"/full-configurations/2.2.x/simple/httpd.conf"};
                inventoryFile = "/mocked-inventories/rhq-3.0.0/simple/inventory-without-snmp.xml";
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_UNRESOLVABLE_SERVER_NAMES;
                
                defaultOverrides.put(variableName(configurationName, "servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost1.servername.directive"), "ServerName ${unresolvable.host}:${listen1}");
                defaultOverrides.put(variableName(configurationName, "vhost2.servername.directive"), "ServerName ${unresolvable.host}:${listen2}");
                defaultOverrides.put(variableName(configurationName, "vhost3.servername.directive"), "ServerName ${unresolvable.host}:${listen3}");
                defaultOverrides.put(variableName(configurationName, "vhost4.servername.directive"), "ServerName ${unresolvable.host}:${listen4}");                                
            }
        });
    }

    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN}, numberOfInitialDiscoveries = 2)
    @Parameters({"apache2.install.dir", "apache2.exe.path"})
    public void testWithNonUniqueNamesWithoutSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = new String[]{"/full-configurations/2.2.x/simple/httpd.conf"};
                inventoryFile = "/mocked-inventories/rhq-3.0.0/simple/inventory-without-snmp.xml";
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_UNRESOLVABLE_SERVER_NAMES;
                
                defaultOverrides.put(variableName(configurationName, "servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost1.servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost2.servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost3.servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost4.servername.directive"), "ServerName ${unresolvable.host}");                                
            }
        });
    }

    /**
     * This tests the upgrade from RHQ 3.0.0 where the Apache server was discovered with the following:
     * <p>
     * <ol>
     * <li> SNMP enabled
     * <li> Main URL set or unset (it doesn't make a difference here)
     * </ol>
     * @param installDir
     * @param exePath
     * @throws Throwable
     */
    @Test    
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path"})
    public void testWithResolvableNamesWithSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = new String[]{"/full-configurations/2.2.x/simple/httpd.conf"};
                inventoryFile = "/mocked-inventories/rhq-3.0.0/simple/inventory-with-snmp.xml";
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;                     
                upgradeShouldSucceed = false;                
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
                inventoryFile = "/mocked-inventories/rhq-3.0.0/simple/inventory-with-snmp.xml";
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;                     
                upgradeShouldSucceed = false;
                
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
                inventoryFile = "/mocked-inventories/rhq-3.0.0/simple/inventory-with-snmp.xml";
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;                     
                upgradeShouldSucceed = false;
                
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
    
    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path" })
    public void testWithAnyAddressWithoutSNMP(final String installPath, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                configurationName = DEPLOYMENT_SIMPLE_WITH_WILDCARD_LISTENS;
                
                apacheConfigurationFiles = new String[] { "/full-configurations/2.2.x/simple/httpd.conf" };
                inventoryFile = "/mocked-inventories/rhq-3.0.0/simple/inventory-without-snmp.xml";
                serverRoot = installPath;
                binPath = exePath;
                                                            
                //just define the servername value without actually setting the ${servername.directive} so that
                //we don't define a servername directive itself but do have a value for the actual server name.
                //this is deduced by apache and the plugin but tests aren't that clever.
                defaultOverrides.put("servername", "${localhost}");
                defaultOverrides.put(variableName(configurationName, "listen1"), "0.0.0.0:${port1}");
                defaultOverrides.put(variableName(configurationName, "listen2"), "0.0.0.0:${port2}");
                defaultOverrides.put(variableName(configurationName, "listen3"), "0.0.0.0:${port3}");
                defaultOverrides.put(variableName(configurationName, "listen4"), "0.0.0.0:${port4}");
                defaultOverrides.put(variableName(configurationName, "vhost1.urls"), "0.0.0.0:${port1}");
            }
        });
    }

    /**
     * Unlike other SNMP tests this one actually succeeds to upgrade because of the messed up discovery that
     * RHQ 3.0.0 would perform. In case of any-address (0.0.0.0), the main vhost would get the MainServer
     * resource key even with SNMP, because RHQ 3 codebase wouldn't be able to match what it think should
     * have been the SNMP record with the actual results from SNMP module.
     * <p>
     * Because of this, RHQ 3 discovers all 5 vhosts and the upgrade code is therefore able to successfully
     * upgrade all of them.
     * 
     * @param installPath
     * @param exePath
     * @throws Throwable
     */
    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path" })
    public void testWithAnyAddressWithSNMP(final String installPath, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                configurationName = DEPLOYMENT_SIMPLE_WITH_WILDCARD_LISTENS;
                
                apacheConfigurationFiles = new String[] { "/full-configurations/2.2.x/simple/httpd.conf" };
                inventoryFile = "/mocked-inventories/rhq-3.0.0/simple/inventory-with-snmp-anyaddr.xml";
                serverRoot = installPath;
                binPath = exePath;
                                                            
                defaultOverrides.put(variableName(configurationName, "listen1"), "0.0.0.0:${port1}");
                defaultOverrides.put(variableName(configurationName, "listen2"), "0.0.0.0:${port2}");
                defaultOverrides.put(variableName(configurationName, "listen3"), "0.0.0.0:${port3}");
                defaultOverrides.put(variableName(configurationName, "listen4"), "0.0.0.0:${port4}");
                defaultOverrides.put(variableName(configurationName, "vhost1.urls"), "0.0.0.0:${port1}");
            }

            @Override
            public void beforeTestSetup(TestSetup testSetup) throws Throwable {
                beforeTestSetupWithSNMP(this, testSetup);
            }
        });
    }

    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path" })
    public void testWithWildcardAddressWithoutSNMP(final String installPath, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                configurationName = DEPLOYMENT_SIMPLE_WITH_WILDCARD_LISTENS;
                
                apacheConfigurationFiles = new String[] { "/full-configurations/2.2.x/simple/httpd.conf" };
                inventoryFile = "/mocked-inventories/rhq-3.0.0/simple/inventory-without-snmp.xml";
                serverRoot = installPath;
                binPath = exePath;                                           
                
                //just define the servername value without actually setting the ${servername.directive} so that
                //we don't define a servername directive itself but do have a value for the actual server name.
                //this is deduced by apache and the plugin but tests aren't that clever.
                defaultOverrides.put("servername", "${localhost}");
                defaultOverrides.put(variableName(configurationName, "listen1"), "*:${port1}");
                defaultOverrides.put(variableName(configurationName, "listen2"), "*:${port2}");
                defaultOverrides.put(variableName(configurationName, "listen3"), "*:${port3}");
                defaultOverrides.put(variableName(configurationName, "listen4"), "*:${port4}");
                defaultOverrides.put(variableName(configurationName, "vhost1.urls"), "*:${port1}");
            }
        });
    }
    
    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path" })
    public void testWithWildcardAddressWithSNMP(final String installPath, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                configurationName = DEPLOYMENT_SIMPLE_WITH_WILDCARD_LISTENS;
                
                apacheConfigurationFiles = new String[] { "/full-configurations/2.2.x/simple/httpd.conf" };
                inventoryFile = "/mocked-inventories/rhq-3.0.0/simple/inventory-with-snmp.xml";
                serverRoot = installPath;
                binPath = exePath;
                upgradeShouldSucceed = false;
                
                defaultOverrides.put(variableName(configurationName, "listen1"), "*:${port1}");
                defaultOverrides.put(variableName(configurationName, "listen2"), "*:${port2}");
                defaultOverrides.put(variableName(configurationName, "listen3"), "*:${port3}");
                defaultOverrides.put(variableName(configurationName, "listen4"), "*:${port4}");
                defaultOverrides.put(variableName(configurationName, "vhost1.urls"), "*:${port1}");
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
}
