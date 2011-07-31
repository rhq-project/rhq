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

package org.rhq.plugins.apache.upgrade.rhq3_0_1;

import java.util.Arrays;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import org.rhq.plugins.apache.upgrade.UpgradeTestBase;
import org.rhq.test.pc.PluginContainerSetup;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class UpgradeSimpleConfigurationFromRHQ3_0_1Test extends UpgradeTestBase {

    private String[] configuredApacheConfigurationFiles;
    private String configuredInventoryFileWithSNMP;
    private String configuredInventoryFileWithoutSNMP;
    private String configuredInventoryFileWithSNMPWithAnyAddress;
    private String configuredInventoryFileWithDuplicateMain;
    
    
    public UpgradeSimpleConfigurationFromRHQ3_0_1Test() {
        configuredApacheConfigurationFiles = new String[]{"/full-configurations/2.2.x/simple/httpd.conf"};
        configuredInventoryFileWithSNMP = "/mocked-inventories/rhq-3.0.1/simple/inventory-with-snmp.xml";
        configuredInventoryFileWithoutSNMP = "/mocked-inventories/rhq-3.0.1/simple/inventory-without-snmp.xml";
        configuredInventoryFileWithSNMPWithAnyAddress = "/mocked-inventories/rhq-3.0.1/simple/inventory-with-snmp-anyaddr.xml";
        configuredInventoryFileWithDuplicateMain = "/mocked-inventories/rhq-3.0.1/simple/inventory-with-duplicate-main.xml";        
    }

    /**
     * @param configuredApacheConfigurationFiles
     * @param configuredInventoryFileWithSNMP
     * @param configuredInventoryFileWithoutSNMP
     * @param configuredInventoryFileWithSNMPWithAnyAddress
     * @param configuredInventoryFileWithDuplicateMain
     */
    protected UpgradeSimpleConfigurationFromRHQ3_0_1Test(String configuredInventoryFileWithSNMP,
        String configuredInventoryFileWithoutSNMP, String configuredInventoryFileWithSNMPWithAnyAddress,
        String configuredInventoryFileWithDuplicateMain, String... configuredApacheConfigurationFiles) {

        this.configuredApacheConfigurationFiles = configuredApacheConfigurationFiles;
        this.configuredInventoryFileWithSNMP = configuredInventoryFileWithSNMP;
        this.configuredInventoryFileWithoutSNMP = configuredInventoryFileWithoutSNMP;
        this.configuredInventoryFileWithSNMPWithAnyAddress = configuredInventoryFileWithSNMPWithAnyAddress;
        this.configuredInventoryFileWithDuplicateMain = configuredInventoryFileWithDuplicateMain;
    }

    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path"})
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
        });
    }
    
    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path"})
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
                defaultOverrides.put("servername", "${localhost.ip}");
            }
            
            @Override
            public void beforeTestSetup(TestSetup testSetup) throws Throwable {
                defineRHQ3ResourceKeys(this, testSetup);
            }
            
            @Override
            public String[] getExpectedResourceKeysAfterUpgrade(TestSetup setup) {
                //the inventory has main, vhost1, vhost2, vhost3 and vhost4
                //the main vhost uses the URL of the server resource as its RK which is 
                //setup to be the IP address, whereas vhost1 uses the 'normal'
                //discovered RK with uses the domain name of that IP.
                //Therefore, unlike in the WithSNMP test cases, we have 5 vhosts discovered
                //by RHQ 3.0.1.
                //The vhost1 will fail to upgrade because it could be ambiguously matched
                //to either main vhost or vhost1.
                
                return getVHostRKs(setup, new int[] { 0, 2, 3, 4 }, new int[] { 1 }, ResourceKeyFormat.RHQ3);
            }
            
            @Override
            public String[] getExpectedResourceKeysWithFailures(TestSetup setup) {
                String failedRK = setup.getInventoryFileReplacements().get("vhost1.rhq3.resource.key");
                return new String[] { failedRK };
            }
        });
    }
    
    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path"})
    public void testWithUnresolvableNamesWithSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithSNMP;
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;
                
                defaultOverrides.put(variableName(configurationName, "servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost1.servername.directive"), "ServerName ${unresolvable.host}:${listen1}");
                defaultOverrides.put(variableName(configurationName, "vhost2.servername.directive"), "ServerName ${unresolvable.host}:${listen2}");
                defaultOverrides.put(variableName(configurationName, "vhost3.servername.directive"), "ServerName ${unresolvable.host}:${listen3}");
                defaultOverrides.put(variableName(configurationName, "vhost4.servername.directive"), "ServerName ${unresolvable.host}:${listen4}");                                
            }
            
            @Override
            public void beforeTestSetup(TestSetup testSetup) throws Throwable {
                defineRHQ3ResourceKeys(this, testSetup);
            }
        });
    }
    
    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path"})
    public void testWithUnresolvableNamesWithoutSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithoutSNMP;
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;
                
                defaultOverrides.put(variableName(configurationName, "servername.directive"), "ServerName ${unresolvable.host}");
                //we use this in the serialized inventory to init the URL of the main server. The localhost.ip is actually what
                //the RHQ 301 discovers even though unresolvable.host is specified in the ServerName directive.
                defaultOverrides.put("servername", "${localhost.ip}");
                defaultOverrides.put(variableName(configurationName, "vhost1.servername.directive"), "ServerName ${unresolvable.host}:${port1}");
                defaultOverrides.put(variableName(configurationName, "vhost2.servername.directive"), "ServerName ${unresolvable.host}:${port2}");
                defaultOverrides.put(variableName(configurationName, "vhost3.servername.directive"), "ServerName ${unresolvable.host}:${port3}");
                defaultOverrides.put(variableName(configurationName, "vhost4.servername.directive"), "ServerName ${unresolvable.host}:${port4}");                                
            }
            
            @Override
            public void beforeTestSetup(TestSetup testSetup) throws Throwable {
                defineRHQ3ResourceKeys(this, testSetup);
            }
        });
    }
    
    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path"})
    public void testWithNonUniqueNamesWithSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithSNMP;
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;
                
                defaultOverrides.put(variableName(configurationName, "servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost1.servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost2.servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost3.servername.directive"), "ServerName ${unresolvable.host}");
                defaultOverrides.put(variableName(configurationName, "vhost4.servername.directive"), "ServerName ${unresolvable.host}");                                
            }
            
            @Override
            public void beforeTestSetup(TestSetup testSetup) throws Throwable {
                defineRHQ3ResourceKeys(this, testSetup);
            }
        });
    }
    
    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path"})
    public void testWithNonUniqueNamesWithoutSNMP(final String installDir, final String exePath) throws Throwable {
        testUpgrade(new TestConfiguration() {
            {
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithoutSNMP;
                serverRoot = installDir;
                binPath = exePath;
                configurationName = DEPLOYMENT_SIMPLE_WITH_RESOLVABLE_SERVERNAMES;

                //we use this in the serialized inventory to init the URL of the main server. The localhost.ip is actually what
                //the RHQ 301 discovers even though unresolvable.host is specified in the ServerName directive.
                defaultOverrides.put("servername", "${localhost.ip}");
                defaultOverrides.put(variableName(configurationName, "servername.directive"), "ServerName ${unresolvable.host}");
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
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithDuplicateMain;
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
                //the inventory has main1, main2, vhost2, vhost3 and vhost4
                //vhost1 has the same resource key as the main vhost in RHQ 3.0.0
                //because the upgrade failed for main (because of the ambiguity),
                //no discoveries are being made
                //therefore we expect the main to have the old resource key and
                //vhost2, 3 and 4 to have the new resource keys.
                
                String[] ret = getVHostRKs(setup, new int[] { 2, 3, 4 }, new int[] { 0 }, ResourceKeyFormat.RHQ3);
                String mainVhost2RK = interpret("${localhost}:${port1}", setup.getInventoryFileReplacements());
                
                ret = Arrays.copyOf(ret, ret.length + 1);
                ret[ret.length - 1] = mainVhost2RK;
                
                return ret;
            }
            
            @Override
            public String[] getExpectedResourceKeysWithFailures(TestSetup setup) {
                String mainVhost1RK = setup.getInventoryFileReplacements().get("main.rhq3.resource.key");
                String mainVhost2RK = interpret("${localhost}:${port1}", setup.getInventoryFileReplacements());
                return new String[] { mainVhost1RK, mainVhost2RK };
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
                
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                //yes, the inventory file can be the same for both the with and without snmp tests because
                //they both yield the same inventory in this case.
                inventoryFile = configuredInventoryFileWithSNMPWithAnyAddress;
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
            
            @Override
            public void beforeTestSetup(TestSetup testSetup) throws Throwable {
                defineRHQ3ResourceKeys(this, testSetup);
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
                
                apacheConfigurationFiles = configuredApacheConfigurationFiles;
                inventoryFile = configuredInventoryFileWithSNMPWithAnyAddress;
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
        });
    }
    
    /**
     * This configuration will fail to upgrade the first vhost, because it doesn't uniquely match to new-style
     * vhosts - it could be either a main vhost or vhost1.
     */
    @Test
    @PluginContainerSetup(plugins = {PLATFORM_PLUGIN, AUGEAS_PLUGIN, APACHE_PLUGIN})
    @Parameters({"apache2.install.dir", "apache2.exe.path" })
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
                defaultOverrides.put("servername", "${localhost.ip}");
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
                //the inventory has main, vhost1, vhost2, vhost3 and vhost4
                //the main vhost uses the URL of the server resource as its RK which is 
                //setup to be the IP address, whereas vhost1 uses the 'normal'
                //discovered RK with uses the domain name of that IP.
                //Therefore, unlike in the WithSNMP test cases, we have 5 vhosts discovered
                //by RHQ 3.0.1.
                //The vhost1 will fail to upgrade because it could be ambiguously matched
                //to either main vhost or vhost1.
                
                return getVHostRKs(setup, new int[] { 0, 2, 3, 4 }, new int[] { 1 }, ResourceKeyFormat.RHQ3);
            }
            
            @Override
            public String[] getExpectedResourceKeysWithFailures(TestSetup setup) {
                String failedRK = setup.getInventoryFileReplacements().get("vhost1.rhq3.resource.key");
                return new String[] { failedRK };
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
        });
    }
}
