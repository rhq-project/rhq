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

package org.rhq.plugins.apache.upgrade.rhq1_3_x;

import java.util.HashMap;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import org.rhq.plugins.apache.upgrade.UpgradeTestBase;
import org.rhq.test.pc.PluginContainerSetup;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test(groups = "apache-integration-tests")
public class UpgradeSimpleConfigurationFromRHQ1_3Test extends UpgradeTestBase {

    String[] configuredApacheConfigurationFiles;
    String configuredInventoryFile;
    String configuredInventoryFileWithSingleVHost;
    
    public UpgradeSimpleConfigurationFromRHQ1_3Test() {
        configuredApacheConfigurationFiles = new String[] { "/full-configurations/2.2.x/simple/httpd.conf" };
        configuredInventoryFile = "/mocked-inventories/rhq-1.3.x/simple/inventory.xml";
        configuredInventoryFileWithSingleVHost = "/mocked-inventories/rhq-1.3.x/simple/inventory-single-vhost.xml";
    }
    
    protected UpgradeSimpleConfigurationFromRHQ1_3Test(String defaultInventoryFile, String defaultInventoryFileWithSingleVHost, String... defaultApacheConfigurationFiles) {
        this.configuredApacheConfigurationFiles = defaultApacheConfigurationFiles;
        this.configuredInventoryFile = defaultInventoryFile;
        this.configuredInventoryFileWithSingleVHost = defaultInventoryFileWithSingleVHost;
    }
    
    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithResolvableServerNames_Apache2(final String installPath, final String exePath)
        throws Throwable {

        testUpgrade(new TestConfiguration() {
            {
                serverRoot = installPath;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFile;
            }
        });
    }

    @Test(enabled = false)
    //ApacheServerOperationsDelegate doesn't work with apache 1.3
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache1.install.dir", "apache1.exe.path" })
    public void testWithResolvableServerNames_Apache1(final String installPath, final String exePath)
        throws Throwable {

        testUpgrade(new TestConfiguration() {{
            serverRoot = installPath;
            binPath = exePath;
            configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;
            apacheConfigurationFiles = configuredApacheConfigurationFiles;
            inventoryFile = configuredInventoryFile;
        }});
    }

    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithNonUniqueServerNames_Apache2(final String installPath, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                configurationName = DEPLOYMENT_SIMPLE_WITH_UNRESOLVABLE_SERVER_NAMES;
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithSingleVHost;
                serverRoot = installPath;
                binPath = exePath;
                            
                
                defaultOverrides = new HashMap<String, String>();
                defaultOverrides.put(variableName(configurationName, "servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost1.servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost2.servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost3.servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost4.servername.directive"), "ServerName ${unresolvable.host}");                
            }            
        });        
    }
    
    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path" })
    public void testWithAnyAddress(final String installPath, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                configurationName = DEPLOYMENT_SIMPLE_WITH_WILDCARD_LISTENS;
                
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFile;
                serverRoot = installPath;
                binPath = exePath;
                                            
                defaultOverrides.put(variableName(configurationName, "listen1"), "0.0.0.0:${port1}");
                defaultOverrides.put(variableName(configurationName, "listen2"), "0.0.0.0:${port2}");
                defaultOverrides.put(variableName(configurationName, "listen3"), "0.0.0.0:${port3}");
                defaultOverrides.put(variableName(configurationName, "listen4"), "0.0.0.0:${port4}");
                defaultOverrides.put(variableName(configurationName, "vhost1.urls"), "0.0.0.0:${port1}");
            }
        });
    }

    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path" })
    public void testWithWildcardAddress(final String installPath, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                configurationName = DEPLOYMENT_SIMPLE_WITH_WILDCARD_LISTENS;
                
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFile;
                serverRoot = installPath;
                binPath = exePath;
                                            
                defaultOverrides.put(variableName(configurationName, "listen1"), "*:${port1}");
                defaultOverrides.put(variableName(configurationName, "listen2"), "*:${port2}");
                defaultOverrides.put(variableName(configurationName, "listen3"), "*:${port3}");
                defaultOverrides.put(variableName(configurationName, "listen4"), "*:${port4}");
                defaultOverrides.put(variableName(configurationName, "vhost1.urls"), "*:${port1}");
            }
        });
    }
}
