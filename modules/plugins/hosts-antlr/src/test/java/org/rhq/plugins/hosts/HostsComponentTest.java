/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.hosts;

import static org.testng.Assert.fail;
import static org.testng.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.configuration.ConfigurationManager;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;

/**
 * @author Jason Dobies
 */
public class HostsComponentTest {
    private static final File ITEST_DIR = new File("target/itest");
    private static final long ONE_WEEK_IN_SECONDS = 60L * 60 * 24;
    private static final String PLUGIN_NAME = "Hosts";
    private static final String RESOURCE_TYPE_NAME = "Hosts File";
    private static final int BIG_TIMEOUT = Integer.MAX_VALUE;
    
    private File hostsCopy;
    private String testingCopy;
    
    private final Log log = LogFactory.getLog(this.getClass());

    @BeforeSuite
    public void start() {
        try {
            PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
            File pluginDir = new File(ITEST_DIR, "plugins");
            pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
            pcConfig.setPluginDirectory(pluginDir);
            pcConfig.setInsideAgent(false);
            pcConfig.setCreateResourceClassloaders(true);
            
            // Because we communicate with the AS directly in the tests setups
            // we need a number of deps in the test classloader.
            // If we configured the plugin container to hide the classes below
            // we would run into issues of classes not being able to transfer the
            // classloader boundaries outside of our control (like for example during EJB3 
            // remote interface proxuing). It it therefore best just to include the complete
            // set of dependencies in the test scope in pom.xml and let the plugin container
            // take those over.
            // [mazz] i think this can be fixed, uncomment this if we can isolate the PC better
            //pcConfig.setRootPluginClassLoaderRegex(getRootClassLoaderClassesToHideRegex());

            // Set initial delays for all scheduled scans to one week to effectively disable them.
            pcConfig.setServerDiscoveryInitialDelay(ONE_WEEK_IN_SECONDS);
            pcConfig.setServiceDiscoveryInitialDelay(ONE_WEEK_IN_SECONDS);
            pcConfig.setAvailabilityScanInitialDelay(ONE_WEEK_IN_SECONDS);
            pcConfig.setConfigurationDiscoveryInitialDelay(ONE_WEEK_IN_SECONDS);
            pcConfig.setContentDiscoveryInitialDelay(ONE_WEEK_IN_SECONDS);

            File tmpDir = new File(ITEST_DIR, "tmp");
            tmpDir.mkdirs();
            if (!tmpDir.isDirectory() || !tmpDir.canWrite()) {
                throw new IOException("Failed to create temporary directory: " + tmpDir);
            }
            pcConfig.setTemporaryDirectory(tmpDir);
            PluginContainer.getInstance().setConfiguration(pcConfig);
            System.out.println("Starting PC...");
            PluginContainer.getInstance().initialize();
            Set<String> pluginNames = PluginContainer.getInstance().getPluginManager().getMetadataManager()
                .getPluginNames();
            System.out.println("PC started with the following plugins: " + pluginNames);
        
            System.out.println("Issuing inventory scan.");
            PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
            PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to start the plugin container", e);
        }
    }
    
    @BeforeSuite(dependsOnMethods = "start")
    @Parameters("hostsFilePath")
    public void configureResource(String hostsFilePath) throws Exception {
        Resource res = getHostFileResource();
        Configuration pluginConfig = res.getPluginConfiguration();
        pluginConfig.put(new PropertySimple("path", hostsFilePath));
        PluginContainer.getInstance().getInventoryManager().updatePluginConfiguration(res.getId(), pluginConfig);        
    }
    
    @BeforeSuite(dependsOnMethods = "start")
    @Parameters("hostsFilePath")
    public void backupHostsFile(String hostsFilePath) throws Exception {
        hostsCopy = File.createTempFile("hostsComponentTest", null);
        BufferedReader rdr = new BufferedReader(new FileReader(new File(hostsFilePath)));
        FileWriter wrt = new FileWriter(hostsCopy);
        
        String line;
        while((line = rdr.readLine()) != null) {
            wrt.write(line);
            wrt.write('\n');
        }
        
        rdr.close();
        wrt.close();
        
        testingCopy = hostsFilePath;
    }
    
    @AfterSuite
    public void stop() {
        System.out.println("Stopping PC...");
        PluginContainer.getInstance().shutdown();
        System.out.println("PC stopped.");
    }
    
    @AfterSuite
    @Parameters("hostsFilePath")
    public void restoreHostsFile(String hostsFilePath) throws Exception {
        BufferedReader rdr = new BufferedReader(new FileReader(hostsCopy));
        FileWriter wrt = new FileWriter(hostsFilePath);
        
        String line;
        while((line = rdr.readLine()) != null) {
            wrt.write(line);
            wrt.write('\n');
        }
        
        rdr.close();
        wrt.close();
    }
    
    public void getResourceTest() {
        assert getResources().size() > 0;
    }
    
    @Test
    public void loadResourceConfiguration() throws Exception {
        Configuration configuration = loadResourceConfiguration(getHostFileResource());

        assert configuration != null : "Null configuration returned from load call";

        Collection<Property> allProperties = configuration.getProperties();

        assert allProperties.size() == 1 : "Incorrect number of properties found. Expected: 1, Found: "
            + allProperties.size();

        PropertyList entryList = (PropertyList) allProperties.iterator().next();

        for (Property property : entryList.getList()) {
            PropertyMap entry = (PropertyMap) property;

            Property ipProperty = entry.get("config://$1");
            Property canonicalProperty = entry.get("config://$2");

            assert ipProperty != null : "IP was null in entry";
            assert canonicalProperty != null : "Canonical was null in entry";

            log.info("IP: " + ((PropertySimple) ipProperty).getStringValue());
            log.info("Canonical: " + ((PropertySimple) canonicalProperty).getStringValue());
        }
    }
    
    @Test
    public void basicUpdateResourceConfiguration() throws Exception {
        Configuration configuration = loadResourceConfiguration(getHostFileResource());
        
        Configuration updatedConfiguration = updateResourceConfiguration(getHostFileResource(), configuration, BIG_TIMEOUT);
        
        assertEquals(configuration, updatedConfiguration);
        assert equals(new File(testingCopy), hostsCopy);
    }
    
    @Test(dependsOnMethods = "basicUpdateResourceConfiguration")
    public void updateModifiedResourceConfiguration() throws Exception {
        Configuration configuration = loadResourceConfiguration(getHostFileResource());
        
        PropertyMap newHostDef = new PropertyMap("config://host_def");
        newHostDef.put(new PropertySimple("config://$1", "1.1.1.1"));
        newHostDef.put(new PropertySimple("config://$2", "test-hostname"));
        newHostDef.put(new PropertySimple("config://$3", "test-alias1 test-alias2"));
        configuration.getList("config:///file").add(newHostDef);
        
        PropertyMap localhost = (PropertyMap) configuration.getList("config:///file").getList().get(0);
        localhost.getSimple("config://$2").setValue("updated-localhost");
        
        Configuration updatedConfiguration = updateResourceConfiguration(getHostFileResource(), configuration, BIG_TIMEOUT);
        
        assertEquals(configuration, updatedConfiguration);
    }
    
    private static boolean equals(File orig, File copy) throws IOException {
        
        if (orig.exists() && copy.exists() && orig.length() == copy.length()) {
            FileReader origReader = new FileReader(orig);
            FileReader copyReader = new FileReader(copy);
            int origByte;
            int copyByte;
            boolean ret = true;
            while((origByte = origReader.read()) != -1) {
                copyByte = copyReader.read();
                if (origByte != copyByte) {
                    ret = false;
                    break;
                }
            }
            origReader.close();
            copyReader.close();
            return ret;
        }
        return false;
    }
    
    private Resource getHostFileResource() {
        return getResources().iterator().next();
    }
    
    private Set<Resource> getResources() {
        return getResources(getResourceType(RESOURCE_TYPE_NAME, PLUGIN_NAME));
    }
    
    protected static ResourceType getResourceType(String resourceTypeName, String pluginName) {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginMetadataManager pluginMetadataManager = pluginManager.getMetadataManager();
        return pluginMetadataManager.getType(resourceTypeName, pluginName);        
    }    

    protected static Set<Resource> getResources(ResourceType resourceType) {
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        return inventoryManager.getResourcesWithType(resourceType);
    }

    protected static Configuration loadResourceConfiguration(Resource resource) throws Exception {
        ConfigurationManager configurationManager = PluginContainer.getInstance().getConfigurationManager();
        return configurationManager.loadResourceConfiguration(resource.getId());
    }

    protected static Configuration updateResourceConfiguration(Resource resource, Configuration newConfiguration) throws Exception {
        ConfigurationManager configurationManager = PluginContainer.getInstance().getConfigurationManager();
        ConfigurationUpdateRequest configurationUpdateRequest = new ConfigurationUpdateRequest(0,
            newConfiguration, resource.getId());
        configurationManager.updateResourceConfiguration(configurationUpdateRequest);
        
        //give the component and the managed resource some time to properly persist the update
        Thread.sleep(500);
        
        return configurationManager.loadResourceConfiguration(resource.getId());
    }
    
    protected static Configuration updateResourceConfiguration(Resource resource, Configuration newConfiguration, int timeout) throws Exception {
        Object configFacet = getResourceProxy(resource, ConfigurationFacet.class, timeout);
        
        Method updateMethod = configFacet.getClass().getMethod("updateResourceConfiguration", new Class<?>[] { ConfigurationUpdateReport.class });
        
        ConfigurationUpdateReport report = new ConfigurationUpdateReport(newConfiguration);
        
        updateMethod.invoke(configFacet, report);
        
        ConfigurationManager configurationManager = PluginContainer.getInstance().getConfigurationManager();
        return configurationManager.loadResourceConfiguration(resource.getId());
    }
    
    protected static <T> Object getResourceProxy(Resource resource, Class<T> facetInterface, long timeout) throws Exception {
        ClassLoader currenContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader cl = PluginContainer.getInstance().getPluginComponentFactory().getResourceClassloader(
                resource);
            Class<?> resourceSpecificFacetInterface = Class.forName(facetInterface.getName(), true, cl);

            //use the resource specific classloader for the proxy creation
            Thread.currentThread().setContextClassLoader(cl);
            
            return ComponentUtil.getComponent(resource.getId(), resourceSpecificFacetInterface, FacetLockType.WRITE,
                timeout, true, true);
        } finally {
            Thread.currentThread().setContextClassLoader(currenContextClassLoader);
        }
    }
}
