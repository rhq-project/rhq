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
import java.util.List;
import java.util.Set;

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
        V_1_3_x{
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
    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testSimpleConfigurationWithResolvableServerNames_Apache2_upgradeFromRHQ1_3(
        String apacheInstallationDirectory, String exePath) throws Throwable {

        testUpgradeFromRHQ1_3(Apache.V_2_2_x, DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES, apacheInstallationDirectory, exePath);
    }

    @Test(enabled = false) //ApacheServerOperationsDelegate doesn't work with apache 1.3
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache1.install.dir", "apache1.exe.path" })
    public void testSimpleConfigurationWithResolvableServerNames_Apache1_upgradeFromRHQ1_3(
        String apacheInstallationDirectory, String exePath) throws Throwable {

        testUpgradeFromRHQ1_3(Apache.V_1_3_x, DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES, apacheInstallationDirectory, exePath);
    }

    private void testUpgradeFromRHQ1_3(Apache version, String configurationName, String serverRoot, String binPath) throws Throwable {
        final TestSetup setup = new TestSetup(configurationName);
        boolean testFailed = false;
        try {
            
            String configPath = "/full-configurations/" + version.getConfigDirName() + "/simple/httpd.conf";
            
            setup.withInventoryFrom("/mocked-inventories/rhq-1.3.x/includes/inventory.xml")
                .withPlatformResource(platform).withDefaultExpectations().withApacheSetup()
                .withConfigurationFiles(configPath, "/snmpd.conf", "/mime.types")
                .withServerRoot(serverRoot).withExePath(binPath).setup();

            startConfiguredPluginContainer();

            //ok, now we should see the resources upgraded in the fake server inventory.
            ResourceType serverResourceType = findApachePluginResourceTypeByName("Apache HTTP Server");
            ResourceType vhostResourceType = findApachePluginResourceTypeByName("Apache Virtual Host");

            Set<Resource> servers = setup.getFakeInventory().findResourcesByType(serverResourceType);

            assertEquals(servers.size(), 1, "There should be exactly one apache server discovered.");

            Resource server = servers.iterator().next();

            String expectedResourceKey = ApacheServerDiscoveryComponent.formatResourceKey(serverRoot, serverRoot
                + "/conf/httpd.conf");

            assertEquals(server.getResourceKey(), expectedResourceKey,
                "The server resource key doesn't seem to be upgraded.");

            Set<Resource> vhosts = setup.getFakeInventory().findResourcesByType(vhostResourceType);

            assertEquals(vhosts.size(), 5, "There should be 5 vhosts discovered but found " + vhosts.size());

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
