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

package org.rhq.plugins.apache.upgrade.rhq3_0_2;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import org.rhq.plugins.apache.upgrade.UpgradeTestBase;
import org.rhq.test.pc.PluginContainerSetup;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class UpgradeSimpleConfigurationFromRHQ3_0_2Test extends UpgradeTestBase {

    private String[] configuredApacheConfigurationFiles;
    private String configuredInventoryFile;

    public UpgradeSimpleConfigurationFromRHQ3_0_2Test() {
        configuredApacheConfigurationFiles = new String[] { "/full-configurations/2.2.x/simple/httpd.conf" };
        configuredInventoryFile = "/mocked-inventories/rhq-3.0.2/simple/inventory.xml";
    }

    /**
     * @param configuredApacheConfigurationFiles
     * @param configuredInventoryFile
     */
    protected UpgradeSimpleConfigurationFromRHQ3_0_2Test(String configuredInventoryFile,
        String... configuredApacheConfigurationFiles) {

        this.configuredApacheConfigurationFiles = configuredApacheConfigurationFiles;
        this.configuredInventoryFile = configuredInventoryFile;
    }

    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithResolvableNames(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFile;
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;
            }
        });
    }

    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithUnresolvableNames(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFile;
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;

                defaultOverrides.put(variableName(configurationName, "servername.directive"),
                    "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost1.servername.directive"),
                    "ServerName ${unresolvable.host}:${listen1}");
                defaultOverrides.put(variableName(configurationName, "vhost2.servername.directive"),
                    "ServerName ${unresolvable.host}:${listen2}");
                defaultOverrides.put(variableName(configurationName, "vhost3.servername.directive"),
                    "ServerName ${unresolvable.host}:${listen3}");
                defaultOverrides.put(variableName(configurationName, "vhost4.servername.directive"),
                    "ServerName ${unresolvable.host}:${listen4}");
            }
        });
    }

    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithNonUniqueNames(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFile;
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;

                defaultOverrides.put(variableName(configurationName, "servername.directive"),
                    "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost1.servername.directive"),
                    "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost2.servername.directive"),
                    "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost3.servername.directive"),
                    "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost4.servername.directive"),
                    "ServerName ${unresolvable.host}");
            }
        });
    }

    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithAnyAddress(final String installPath, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                configurationName = DEPLOYMENT_SIMPLE_WITH_WILDCARD_LISTENS;

                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFile;
                serverRoot = installPath;
                binPath = exePath;

                //just define the servername value without actually setting the ${servername.directive} so that
                //we don't define a servername directive itself but do have a value for the actual server name.
                //this is deduced by apache and the plugin but tests aren't that clever.
                defaultOverrides.put("servername", "${localhost.ip}");
                defaultOverrides.put(variableName(configurationName, "listen1"), "0.0.0.0:${port1}");
                defaultOverrides.put(variableName(configurationName, "listen2"), "0.0.0.0:${port2}");
                defaultOverrides.put(variableName(configurationName, "listen3"), "0.0.0.0:${port3}");
                defaultOverrides.put(variableName(configurationName, "listen4"), "0.0.0.0:${port4}");
                defaultOverrides.put(variableName(configurationName, "vhost1.urls"), "0.0.0.0:${port1}");
            }
        });
    }

    /**
     * This configuration will fail to upgrade the first vhost, because it doesn't uniquely match to new-style
     * vhosts - it could be either a main vhost or vhost1.
     */
    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithWildcardAddress(final String installPath, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                configurationName = DEPLOYMENT_SIMPLE_WITH_WILDCARD_LISTENS;

                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFile;
                serverRoot = installPath;
                binPath = exePath;

                //just define the servername value without actually setting the ${servername.directive} so that
                //we don't define a servername directive itself but do have a value for the actual server name.
                //this is deduced by apache and the plugin but tests aren't that clever.
                defaultOverrides.put("servername", "${localhost.ip}");
                defaultOverrides.put(variableName(configurationName, "listen1"), "*:${port1}");
                defaultOverrides.put(variableName(configurationName, "listen2"), "*:${port2}");
                defaultOverrides.put(variableName(configurationName, "listen3"), "*:${port3}");
                defaultOverrides.put(variableName(configurationName, "listen4"), "*:${port4}");
                defaultOverrides.put(variableName(configurationName, "vhost1.urls"), "*:${port1}");
            }
        });
    }
}
