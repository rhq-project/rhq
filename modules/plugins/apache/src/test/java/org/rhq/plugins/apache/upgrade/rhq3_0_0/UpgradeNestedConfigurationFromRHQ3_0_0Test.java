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

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import org.rhq.plugins.apache.upgrade.UpgradeTestBase;
import org.rhq.test.pc.PluginContainerSetup;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class UpgradeNestedConfigurationFromRHQ3_0_0Test extends UpgradeTestBase {
    String[] configuredApacheConfigurationFiles;
    private String configuredInventoryFileWithoutSNMP;
    private String configuredInventoryFileWithSNMP;

    public UpgradeNestedConfigurationFromRHQ3_0_0Test() {
        configuredApacheConfigurationFiles = new String[] { "/full-configurations/2.2.x/nested/httpd.conf" };
        configuredInventoryFileWithoutSNMP = "/mocked-inventories/rhq-3.0.0/nested/inventory-without-snmp.xml";
        configuredInventoryFileWithSNMP = "/mocked-inventories/rhq-3.0.0/nested/inventory-with-snmp.xml";
    }

    /**
     * @param configuredApacheConfigurationFiles
     * @param configuredInventoryFileWithoutSNMP
     * @param configuredInventoryFileWithSNMP
     * @param configuredInventoryFileWithSNMPWithAnyAddress
     */
    public UpgradeNestedConfigurationFromRHQ3_0_0Test(String configuredInventoryFileWithoutSNMP,
        String configuredInventoryFileWithSNMP, String... configuredApacheConfigurationFiles) {
        this.configuredApacheConfigurationFiles = configuredApacheConfigurationFiles;
        this.configuredInventoryFileWithoutSNMP = configuredInventoryFileWithoutSNMP;
        this.configuredInventoryFileWithSNMP = configuredInventoryFileWithSNMP;
    }

    /**
     * This tests the upgrade from RHQ 3.0.0 where the apache server was discovered without SNMP support.
     * The Main Server URL must have been set in this case, otherwise the component would fail to start
     * and therefore the vhost discovery wouldn't even take place.
     */
    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN }, numberOfInitialDiscoveries = 2)
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithResolvableNamesWithoutSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithoutSNMP;
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;

                //just define the servername value without actually setting the ${servername.directive} so that
                //we don't define a servername directive itself but do have a value for the actual server name.
                //this is deduced by apache and the plugin but tests aren't that clever.
                defaultOverrides.put("servername", "${localhost}");
            }

            @Override
            public String[] getExpectedResourceKeysAfterUpgrade(TestSetup setup) {
                return getVHostRKs(setup, new int[] { 0, 2, 4 }, null, ResourceKeyFormat.RHQ3);
            }
        });
    }

    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN }, numberOfInitialDiscoveries = 2)
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithUnresolvableNamesWithoutSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithoutSNMP;
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_UNRESOLVABLE_SERVER_NAMES;

                defaultOverrides.put(variableName(configurationName, "vhost1.servername.directive"),
                    "ServerName ${unresolvable.host}:${listen1}");
                defaultOverrides.put(variableName(configurationName, "vhost2.servername.directive"),
                    "ServerName ${unresolvable.host}:${listen2}");
                defaultOverrides.put(variableName(configurationName, "vhost3.servername.directive"),
                    "ServerName ${unresolvable.host}:${listen3}");
                defaultOverrides.put(variableName(configurationName, "vhost4.servername.directive"),
                    "ServerName ${unresolvable.host}:${listen4}");
            }

            @Override
            public String[] getExpectedResourceKeysAfterUpgrade(TestSetup setup) {
                return getVHostRKs(setup, new int[] { 0, 2, 4 }, null, ResourceKeyFormat.RHQ3);
            }
        });
    }

    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN }, numberOfInitialDiscoveries = 2)
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithNonUniqueNamesWithoutSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithoutSNMP;
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_UNRESOLVABLE_SERVER_NAMES;

                defaultOverrides.put(variableName(configurationName, "vhost1.servername.directive"),
                    "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost2.servername.directive"),
                    "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost3.servername.directive"),
                    "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost4.servername.directive"),
                    "ServerName ${unresolvable.host}");
            }

            @Override
            public String[] getExpectedResourceKeysAfterUpgrade(TestSetup setup) {
                return getVHostRKs(setup, new int[] { 0, 2, 4 }, null, ResourceKeyFormat.RHQ3);
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
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithResolvableNamesWithSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithSNMP;
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;
            }

            @Override
            public void beforeTestSetup(TestSetup testSetup) throws Throwable {
                defineRHQ3ResourceKeys(this, testSetup);
            }

            @Override
            public String[] getExpectedResourceKeysAfterUpgrade(TestSetup setup) {
                return getVHostRKs(setup, new int[] { 0, 2, 4 }, null, ResourceKeyFormat.RHQ3);
            }
        });
    }

    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithUnresolvableNamesWithSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithSNMP;
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

            @Override
            public void beforeTestSetup(TestSetup testSetup) throws Throwable {
                defineRHQ3ResourceKeys(this, testSetup);
            }

            @Override
            public String[] getExpectedResourceKeysAfterUpgrade(TestSetup setup) {
                return getVHostRKs(setup, new int[] { 0, 2, 4 }, null, ResourceKeyFormat.RHQ3);
            }
        });
    }

    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithNonUniqueNamesWithSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithSNMP;
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

            @Override
            public void beforeTestSetup(TestSetup testSetup) throws Throwable {
                defineRHQ3ResourceKeys(this, testSetup);
            }

            @Override
            public String[] getExpectedResourceKeysAfterUpgrade(TestSetup setup) {
                return getVHostRKs(setup, new int[] { 0, 2, 4 }, null, ResourceKeyFormat.RHQ3);
            }
        });
    }

    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithAnyAddressWithoutSNMP(final String installPath, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                configurationName = DEPLOYMENT_SIMPLE_WITH_WILDCARD_LISTENS;

                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithoutSNMP;
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

            @Override
            public String[] getExpectedResourceKeysAfterUpgrade(TestSetup setup) {
                return getVHostRKs(setup, new int[] { 0, 2, 4 }, null, ResourceKeyFormat.RHQ3);
            }
        });
    }

    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithAnyAddressWithSNMP(final String installPath, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                configurationName = DEPLOYMENT_SIMPLE_WITH_WILDCARD_LISTENS;

                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithSNMP;
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
                defineRHQ3ResourceKeys(this, testSetup);
            }

            @Override
            public String[] getExpectedResourceKeysAfterUpgrade(TestSetup setup) {
                return getVHostRKs(setup, new int[] { 0, 2, 4 }, null, ResourceKeyFormat.RHQ3);
            }
        });
    }

    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithWildcardAddressWithoutSNMP(final String installPath, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                configurationName = DEPLOYMENT_SIMPLE_WITH_WILDCARD_LISTENS;

                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithoutSNMP;
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

            @Override
            public String[] getExpectedResourceKeysAfterUpgrade(TestSetup setup) {
                return getVHostRKs(setup, new int[] { 0, 2, 4 }, null, ResourceKeyFormat.RHQ3);
            }
        });
    }

    @Test
    @PluginContainerSetup(plugins = { PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN })
    @Parameters({ "apache2.install.dir", "apache2.exe.path" })
    public void testWithWildcardAddressWithSNMP(final String installPath, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                configurationName = DEPLOYMENT_SIMPLE_WITH_WILDCARD_LISTENS;

                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithSNMP;
                serverRoot = installPath;
                binPath = exePath;

                defaultOverrides.put(variableName(configurationName, "listen1"), "*:${port1}");
                defaultOverrides.put(variableName(configurationName, "listen2"), "*:${port2}");
                defaultOverrides.put(variableName(configurationName, "listen3"), "*:${port3}");
                defaultOverrides.put(variableName(configurationName, "listen4"), "*:${port4}");
                defaultOverrides.put(variableName(configurationName, "vhost1.urls"), "*:${port1}");
            }

            @Override
            public void beforeTestSetup(TestSetup testSetup) throws Throwable {
                defineRHQ3ResourceKeys(this, testSetup);
            }

            @Override
            public String[] getExpectedResourceKeysAfterUpgrade(TestSetup setup) {
                return getVHostRKs(setup, new int[] { 0, 2, 4 }, null, ResourceKeyFormat.RHQ3);
            }
        });
    }
}
