/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.pc.upgrade;

import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.rhq.core.clientapi.server.bundle.BundleServerService;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.clientapi.server.event.EventServerService;
import org.rhq.core.clientapi.server.inventory.ResourceFactoryServerService;
import org.rhq.core.clientapi.server.measurement.MeasurementServerService;
import org.rhq.core.clientapi.server.operation.OperationServerService;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 *
 * @author Lukas Krejci
 */
public abstract class ResourceUpgradeTestBase {

    private static final String PLUGINS_DIR_NAME = "plugins";
    private static final String DATA_DIR_NAME = "data";
    private static final String TMP_DIR_NAME = "tmp";
    private File tmpDir;
    private File pluginDir;
    private File dataDir;
    private BundleServerService currentBundleServerService;
    private ConfigurationServerService currentConfigurationServerService;
    private ContentServerService currentContentServerService;
    private CoreServerService currentCoreServerService;
    private DiscoveryServerService currentDiscoveryServerService;
    private EventServerService currentEventServerService;
    private MeasurementServerService currentMeasurementServerService;
    private OperationServerService currentOperationServerService;
    private ResourceFactoryServerService currentResourceFactoryServerService;
    private FakeServerInventory currentServerSideInventory;

    protected static class ResType {
        private String resourceTypeName;
        private String resourceTypePluginName;

        public ResType(String resourceTypeName, String resourceTypePluginName) {
            super();
            this.resourceTypeName = resourceTypeName;
            this.resourceTypePluginName = resourceTypePluginName;
        }

        public String getResourceTypeName() {
            return resourceTypeName;
        }

        public String getResourceTypePluginName() {
            return resourceTypePluginName;
        }

        @Override
        public int hashCode() {
            return resourceTypeName.hashCode() * resourceTypePluginName.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }

            if (!(other instanceof ResType)) {
                return false;
            }

            ResType o = (ResType)other;

            return resourceTypeName.equals(o.getResourceTypeName()) && resourceTypePluginName.equals(o.getResourceTypePluginName());
        }

        @Override
        public String toString() {
            return "ResType[name='" + resourceTypeName + "', plugin='" + resourceTypePluginName + "']";
        }
    }

    protected interface TestPayload {
        Expectations getExpectations(Mockery context) throws Exception;

        void test(Map<ResType,Set<Resource>> resourceUpgradeTestResources);

        boolean isClearInventoryDat();

        Set<ResType> getExpectedResourceTypes();
    }

    protected static abstract class AbstractTestPayload implements TestPayload {
        private boolean clearInventoryDat;
        private Set<ResType> resourceTypes;

        public AbstractTestPayload(boolean clearInventoryDat, Collection<ResType> resourceTypes) {
            this.clearInventoryDat = clearInventoryDat;
            this.resourceTypes = new HashSet<ResType>(resourceTypes);
        }

        public boolean isClearInventoryDat() {
            return clearInventoryDat;
        }

        public Set<ResType> getExpectedResourceTypes() {
            return resourceTypes;
        }
    }

    @BeforeClass
    public void init() {
        tmpDir = getTmpDirectory();
        pluginDir = new File(tmpDir, PLUGINS_DIR_NAME);
        assertTrue(pluginDir.mkdir(), "Could not create plugin deploy directory.");
        dataDir = new File(tmpDir, DATA_DIR_NAME);
        assertTrue(dataDir.mkdir(), "Could not create plugin container data directory.");
    }

    @BeforeClass
    public void verifyPluginsExist() {
        for (String plugin : getRequiredPlugins()) {
            verifyPluginExists(plugin);
        }
    }

    @AfterClass
    public void undeployPlugins() throws IOException {
        FileUtils.deleteDirectory(tmpDir);
    }

    protected abstract Collection<String> getRequiredPlugins();

    protected void setCurrentServerSideInventory(FakeServerInventory currentServerSideInventory) {
        this.currentServerSideInventory = currentServerSideInventory;
    }

    protected FakeServerInventory getCurrentServerSideInventory() {
        return currentServerSideInventory;
    }

    protected void setCurrentResourceFactoryServerService(
        ResourceFactoryServerService currentResourceFactoryServerService) {
        this.currentResourceFactoryServerService = currentResourceFactoryServerService;
    }

    protected ResourceFactoryServerService getCurrentResourceFactoryServerService() {
        return currentResourceFactoryServerService;
    }

    protected void setCurrentOperationServerService(OperationServerService currentOperationServerService) {
        this.currentOperationServerService = currentOperationServerService;
    }

    protected OperationServerService getCurrentOperationServerService() {
        return currentOperationServerService;
    }

    protected void setCurrentMeasurementServerService(MeasurementServerService currentMeasurementServerService) {
        this.currentMeasurementServerService = currentMeasurementServerService;
    }

    protected MeasurementServerService getCurrentMeasurementServerService() {
        return currentMeasurementServerService;
    }

    protected void setCurrentEventServerService(EventServerService currentEventServerService) {
        this.currentEventServerService = currentEventServerService;
    }

    protected EventServerService getCurrentEventServerService() {
        return currentEventServerService;
    }

    protected void setCurrentDiscoveryServerService(DiscoveryServerService currentDiscoveryServerService) {
        this.currentDiscoveryServerService = currentDiscoveryServerService;
    }

    protected DiscoveryServerService getCurrentDiscoveryServerService() {
        return currentDiscoveryServerService;
    }

    protected void setCurrentCoreServerService(CoreServerService currentCoreServerService) {
        this.currentCoreServerService = currentCoreServerService;
    }

    protected CoreServerService getCurrentCoreServerService() {
        return currentCoreServerService;
    }

    protected void setCurrentContentServerService(ContentServerService currentContentServerService) {
        this.currentContentServerService = currentContentServerService;
    }

    protected ContentServerService getCurrentContentServerService() {
        return currentContentServerService;
    }

    protected void setCurrentConfigurationServerService(ConfigurationServerService currentConfigurationServerService) {
        this.currentConfigurationServerService = currentConfigurationServerService;
    }

    protected ConfigurationServerService getCurrentConfigurationServerService() {
        return currentConfigurationServerService;
    }

    protected void setCurrentBundleServerService(BundleServerService currentBundleServerService) {
        this.currentBundleServerService = currentBundleServerService;
    }

    protected BundleServerService getCurrentBundleServerService() {
        return currentBundleServerService;
    }

    protected static File getTmpDirectory() {
        File ret = new File(System.getProperty("java.io.tmpdir"), "resource-upgrade-test" + System.currentTimeMillis());

        while (ret.exists() || !ret.mkdir()) {
            ret = new File(System.getProperty("java.io.tmpdir"), "resource-upgrade-test" + System.currentTimeMillis());
        }

        return ret;
    }

    private PluginContainerConfiguration createPluginContainerConfiguration(Mockery context) throws Exception {
        PluginContainerConfiguration conf = new PluginContainerConfiguration();

        conf.setPluginDirectory(new File(tmpDir, PLUGINS_DIR_NAME));
        conf.setDataDirectory(new File(tmpDir, DATA_DIR_NAME));
        conf.setTemporaryDirectory(new File(tmpDir, TMP_DIR_NAME));
        conf.setInsideAgent(true); //pc must think it's inside an agent so that it persists the inventory between restarts
        conf.setPluginFinder(new FileSystemPluginFinder(conf.getPluginDirectory()));
        conf.setCreateResourceClassloaders(false);

        //we're not interested in any scans happening out of our control
        conf.setAvailabilityScanInitialDelay(Long.MAX_VALUE);
        conf.setConfigurationDiscoveryInitialDelay(Long.MAX_VALUE);
        conf.setContentDiscoveryInitialDelay(Long.MAX_VALUE);
        conf.setEventSenderInitialDelay(Long.MAX_VALUE);
        conf.setMeasurementCollectionInitialDelay(Long.MAX_VALUE);
        conf.setServerDiscoveryInitialDelay(Long.MAX_VALUE);
        conf.setServiceDiscoveryInitialDelay(Long.MAX_VALUE);

        setCurrentBundleServerService(context.mock(BundleServerService.class));
        setCurrentConfigurationServerService(context.mock(ConfigurationServerService.class));
        setCurrentContentServerService(context.mock(ContentServerService.class));
        setCurrentCoreServerService(context.mock(CoreServerService.class));
        setCurrentDiscoveryServerService(context.mock(DiscoveryServerService.class));
        setCurrentEventServerService(context.mock(EventServerService.class));
        setCurrentMeasurementServerService(context.mock(MeasurementServerService.class));
        setCurrentOperationServerService(context.mock(OperationServerService.class));
        setCurrentResourceFactoryServerService(context.mock(ResourceFactoryServerService.class));

        ServerServices serverServices = new ServerServices();
        serverServices.setBundleServerService(getCurrentBundleServerService());
        serverServices.setConfigurationServerService(getCurrentConfigurationServerService());
        serverServices.setContentServerService(getCurrentContentServerService());
        serverServices.setCoreServerService(getCurrentCoreServerService());
        serverServices.setDiscoveryServerService(getCurrentDiscoveryServerService());
        serverServices.setEventServerService(getCurrentEventServerService());
        serverServices.setMeasurementServerService(getCurrentMeasurementServerService());
        serverServices.setOperationServerService(getCurrentOperationServerService());
        serverServices.setResourceFactoryServerService(getCurrentResourceFactoryServerService());

        conf.setServerServices(serverServices);

        return conf;
    }

    /**
     * @param pluginResourcePath
     */
    protected void verifyPluginExists(String pluginResourcePath) {
        URL url = getClass().getResource(pluginResourcePath);

        File pluginFile = FileUtils.toFile(url);

        assert pluginFile!=null : "pluginFile was null";
        assertTrue(pluginFile.exists(), pluginFile.getAbsoluteFile() + " plugin jar could not be found.");
    }

    private void copyPlugin(String pluginResourcePath, File pluginDirectory) throws IOException {
        URL pluginUrl = getClass().getResource(pluginResourcePath);

        File pluginFile = new File(pluginResourcePath);
        String pluginFileName = pluginFile.getName();

        FileUtils.copyURLToFile(pluginUrl, new File(pluginDirectory, pluginFileName));
    }

    private Set<Resource> getTestingResources(String resourceTypeName, String resourceTypePluginName) {
        ResourceType resType = PluginContainer.getInstance().getPluginManager().getMetadataManager()
            .getType(resourceTypeName, resourceTypePluginName);

        return getCurrentServerSideInventory().findResourcesByType(resType);
    }

    protected void executeTestWithPlugins(Set<String> pluginResourcePaths, TestPayload test) throws Exception {
        FileUtils.cleanDirectory(new File(tmpDir, PLUGINS_DIR_NAME));

        for (String pluginResourcePath : pluginResourcePaths) {
            copyPlugin(pluginResourcePath, pluginDir);
        }

        Mockery context = new Mockery();

        PluginContainerConfiguration pcConfig = createPluginContainerConfiguration(context);

        if (test.isClearInventoryDat()) {
            File inventoryDat = new File(pcConfig.getDataDirectory(), "inventory.dat");
            inventoryDat.delete();
        }

        context.checking(test.getExpectations(context));

        PluginContainer.getInstance().setConfiguration(pcConfig);
        PluginContainer.getInstance().initialize();

        try {
            //give the pc the time to finish resource upgrade
            while (!PluginContainer.getInstance().isStarted()) {
                Thread.sleep(500);
            }

            //execute full discovery
            InventoryManager im = PluginContainer.getInstance().getInventoryManager();
            im.executeServerScanImmediately();

            //do the service scan a couple of times so that we can commit
            //the resources deep in the type hierarchy
            for(int i = 0; i < 10; ++i) {
                im.executeServiceScanImmediately();
            }

            Map<ResType, Set<Resource>> resources = new HashMap<ResType, Set<Resource>>();

            for(ResType type : test.getExpectedResourceTypes()) {
                Set<Resource> rs = getTestingResources(type.getResourceTypeName(), type.getResourceTypePluginName());
                resources.put(type, rs);
            }

            test.test(resources);

            context.assertIsSatisfied();
        } finally {
            PluginContainer.getInstance().shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    protected void defineDefaultExpectations(Expectations expectations) {
        expectations.ignoring(getCurrentBundleServerService());
        expectations.ignoring(getCurrentConfigurationServerService());
        expectations.ignoring(getCurrentContentServerService());
        expectations.ignoring(getCurrentCoreServerService());
        expectations.ignoring(getCurrentEventServerService());
        expectations.ignoring(getCurrentMeasurementServerService());
        expectations.ignoring(getCurrentOperationServerService());
        expectations.ignoring(getCurrentResourceFactoryServerService());

        //just ignore these invocations if we get a availability scan in the PC...
        expectations.allowing(getCurrentDiscoveryServerService()).mergeAvailabilityReport(
            expectations.with(Expectations.any(AvailabilityReport.class)));

        expectations.allowing(getCurrentDiscoveryServerService()).getResources(
            expectations.with(Expectations.any(Set.class)), expectations.with(Expectations.any(boolean.class)));
        expectations.will(getCurrentServerSideInventory().getResources());

        expectations.allowing(getCurrentDiscoveryServerService()).postProcessNewlyCommittedResources(
            expectations.with(Expectations.any(Set.class)));
    }

    protected void cleanDataDir() throws IOException {
        FileUtils.cleanDirectory(new File(tmpDir, DATA_DIR_NAME));
    }
}
