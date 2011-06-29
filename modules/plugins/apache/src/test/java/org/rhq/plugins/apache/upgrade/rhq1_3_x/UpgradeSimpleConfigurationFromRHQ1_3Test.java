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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.plugins.apache.upgrade.UpgradeTestBase;
import org.rhq.plugins.apache.upgrade.UpgradeTestBase.TestConfiguration;
import org.rhq.plugins.apache.upgrade.UpgradeTestBase.TestSetup;
import org.rhq.test.pc.PluginContainerSetup;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test(groups = "apache-integration-tests")
public class UpgradeSimpleConfigurationFromRHQ1_3Test extends UpgradeTestBase {

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
                apacheConfigurationFiles = new String[] { "/full-configurations/2.2.x/simple/httpd.conf" };
                inventoryFile = "/mocked-inventories/rhq-1.3.x/simple/inventory.xml";
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
            apacheConfigurationFiles = new String[] { "/full-configurations/1.3.x/simple/httpd.conf" };
            inventoryFile = "/mocked-inventories/rhq-1.3.x/simple/inventory.xml";
        }});
    }

    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithNonUniqueServerNames_Apache2(final String installPath, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                configurationName = DEPLOYMENT_SIMPLE_WITH_UNRESOLVABLE_SERVER_NAMES;
                apacheConfigurationFiles = new String[] { "/full-configurations/2.2.x/simple/httpd.conf" };
                inventoryFile = "/mocked-inventories/rhq-1.3.x/simple/inventory.xml";
                serverRoot = installPath;
                binPath = exePath;
                            
                
                defaultOverrides = new HashMap<String, String>();
                defaultOverrides.put(variableName(configurationName, "servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost1.servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost2.servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost3.servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost4.servername.directive"), "ServerName ${unresolvable.host}");                
            }
            
            @Override
            public void beforePluginContainerStart(TestSetup setup) {
                //in this scenario, the RHQ 1.3 would only discover 1 vhost (and the main vhost), because they would have the same resource key
                //due to the same ServerName. I need to process the default inventory to reflect that otherwise I would get upgrade
                //failures.

                Set<Resource> vhosts = setup.getFakeInventory().findResourcesByType(findApachePluginResourceTypeByName("Apache Virtual Host"));
                Set<Resource> uniques = new TreeSet<Resource>(new Comparator<Resource>() {
                   public int compare(Resource a, Resource b) {
                       return a.getResourceKey().compareTo(b.getResourceKey());
                   }
                });
                
                for(Resource vhost : vhosts) {
                    if (uniques.contains(vhost)) {
                        //remove the vhost from the server's inventory
                        setup.getFakeInventory().removeResource(vhost);
                    } else {
                       uniques.add(vhost);
                    }
                }
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
                
                apacheConfigurationFiles = new String[] { "/full-configurations/2.2.x/simple/httpd.conf" };
                inventoryFile = "/mocked-inventories/rhq-1.3.x/simple/inventory.xml";
                serverRoot = installPath;
                binPath = exePath;
                                            
                defaultOverrides = new HashMap<String, String>();
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
                
                apacheConfigurationFiles = new String[] { "/full-configurations/2.2.x/simple/httpd.conf" };
                inventoryFile = "/mocked-inventories/rhq-1.3.x/simple/inventory.xml";
                serverRoot = installPath;
                binPath = exePath;
                                            
                defaultOverrides = new HashMap<String, String>();
                defaultOverrides.put(variableName(configurationName, "listen1"), "*:${port1}");
                defaultOverrides.put(variableName(configurationName, "listen2"), "*:${port2}");
                defaultOverrides.put(variableName(configurationName, "listen3"), "*:${port3}");
                defaultOverrides.put(variableName(configurationName, "listen4"), "*:${port4}");
                defaultOverrides.put(variableName(configurationName, "vhost1.urls"), "*:${port1}");
            }
        });
    }
}
