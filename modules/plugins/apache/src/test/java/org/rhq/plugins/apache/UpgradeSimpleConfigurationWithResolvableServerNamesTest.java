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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.plugins.apache.util.ApacheDeploymentUtil.DeploymentConfig;
import org.rhq.test.pc.PluginContainerSetup;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test(groups = "apache-integration-tests")
public class UpgradeSimpleConfigurationWithResolvableServerNamesTest extends UpgradeTestBase {
    private static final Log LOG = LogFactory.getLog(UpgradeSimpleConfigurationWithResolvableServerNamesTest.class);

    private enum Apache {
        V_1_3_x {
            public String getConfigDirName() {
                return "1.3.x";
            }
        },

        V_2_2_x {
            public String getConfigDirName() {
                return "2.2.x";
            }
        };

        public abstract String getConfigDirName();
    }

    private static class TestConfiguration {
        public Apache version;
        public String configurationName;
        public String serverRoot;
        public String binPath;
        public Map<String, String> defaultOverrides;
        
        public void beforeTestSetup(TestSetup testSetup) {
            
        }
        
        public void beforePluginContainerStart(TestSetup setup) {
            
        }
        
        public void beforeTests() {
            
        }
    }
    
    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithResolvableServerNames_Apache2_upgradeFromRHQ1_3(final String installPath, final String exePath)
        throws Throwable {

        testUpgradeFromRHQ1_3(new TestConfiguration() {
            {
                serverRoot = installPath;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;
                version = Apache.V_2_2_x;
            }
        });
    }

    @Test(enabled = false)
    //ApacheServerOperationsDelegate doesn't work with apache 1.3
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache1.install.dir", "apache1.exe.path" })
    public void testWithResolvableServerNames_Apache1_upgradeFromRHQ1_3(final String installPath, final String exePath)
        throws Throwable {

        testUpgradeFromRHQ1_3(new TestConfiguration() {{
            serverRoot = installPath;
            binPath = exePath;
            configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;
            version = Apache.V_1_3_x;
        }});
    }

    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithUnresolvableServerNames_Apache2_upgradeFromRHQ1_3(final String installPath, final String exePath) throws Throwable {
        testUpgradeFromRHQ1_3(new TestConfiguration() {
            {
                configurationName = DEPLOYMENT_SIMPLE_WITH_UNRESOLVABLE_SERVER_NAMES;
                serverRoot = installPath;
                binPath = exePath;
                version = Apache.V_2_2_x;            
                
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

    private void testUpgradeFromRHQ1_3(TestConfiguration testConfiguration) throws Throwable {
        final TestSetup setup = new TestSetup(testConfiguration.configurationName);
        boolean testFailed = false;
        try {
            testConfiguration.beforeTestSetup(setup);
            
            String configPath = "/full-configurations/" + testConfiguration.version.getConfigDirName() + "/simple/httpd.conf";

            setup.withInventoryFrom("/mocked-inventories/rhq-1.3.x/simple/inventory.xml")
                .withPlatformResource(platform).withDefaultExpectations().withDefaultOverrides(testConfiguration.defaultOverrides)
                .withApacheSetup().withConfigurationFiles(configPath, "/snmpd.conf", "/mime.types")
                .withServerRoot(testConfiguration.serverRoot).withExePath(testConfiguration.binPath).setup();

            testConfiguration.beforePluginContainerStart(setup);
            
            startConfiguredPluginContainer();

            testConfiguration.beforeTests();
            
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

            expectedResourceKeys.add(ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY);
            expectedResourceKeys.add(ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                dc.vhost1.getServerName(), dc.vhost1.getAddresses()));
            expectedResourceKeys.add(ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                dc.vhost2.getServerName(), dc.vhost2.getAddresses()));
            expectedResourceKeys.add(ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                dc.vhost3.getServerName(), dc.vhost3.getAddresses()));
            expectedResourceKeys.add(ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(
                dc.vhost4.getServerName(), dc.vhost4.getAddresses()));

            for (Resource vhost : vhosts) {
                assertTrue(expectedResourceKeys.contains(vhost.getResourceKey()),
                    "Unexpected virtual host resource key: '" + vhost.getResourceKey() + "'.");
            }
        } catch (Throwable t) {
            testFailed = true;
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
}
