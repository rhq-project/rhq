/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pc.bundle;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.bundle.BundleScheduleRequest;
import org.rhq.core.clientapi.agent.bundle.BundleScheduleResponse;
import org.rhq.core.clientapi.server.bundle.BundleServerService;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration.BundleDestinationBaseDirectory.Context;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.measurement.MeasurementManager;
import org.rhq.core.pluginapi.bundle.BundleDeployRequest;
import org.rhq.core.pluginapi.bundle.BundleDeployResult;
import org.rhq.core.pluginapi.bundle.BundleFacet;
import org.rhq.core.pluginapi.bundle.BundlePurgeRequest;
import org.rhq.core.pluginapi.bundle.BundlePurgeResult;
import org.rhq.core.pluginapi.inventory.ResourceContext;

@Test
public class BundleManagerTest {
    private MockBundleManager mockBundleManager;
    private PluginContainerConfiguration pcConfig;

    @BeforeMethod
    public void beforeMethod() {
        ServerServices serverServices = new ServerServices();
        serverServices.setBundleServerService(new MockBundleServerService());

        pcConfig = new PluginContainerConfiguration();
        pcConfig.setServerServices(serverServices);

        mockBundleManager = new MockBundleManager();
        mockBundleManager.setConfiguration(pcConfig);
        mockBundleManager.initialize();

        // clear any past interrupted state
        Thread.interrupted();
    }

    @AfterMethod
    public void afterMethod() {
        mockBundleManager.shutdown();
        pcConfig = null;
    }

    public void testNonPlatformBundleDeploy_FileSystem_AbsolutePath() throws Exception {
        MockInventoryManager im = (MockInventoryManager) mockBundleManager.getInventoryManager();

        BundleType bundleType = new BundleType("bundleTypeName", im.bundleHandlerType);
        Bundle bundle = new Bundle("bundleName", bundleType, null, null);
        BundleVersion bundleVersion = new BundleVersion("bundleVersionName", "1.0", bundle, "");
        BundleDestination destination = new BundleDestination(bundle, "destName", null,
            MockInventoryManager.BUNDLE_CONFIG_NAME_FS, getPath("/tmp/dest")); // ABSOLUTE PATH
        BundleDeployment bundleDeployment = new BundleDeployment(bundleVersion, destination, "deploymentName");
        BundleResourceDeployment resourceDeployment = new BundleResourceDeployment(bundleDeployment, im.serverFS);
        BundleScheduleRequest request = new BundleScheduleRequest(resourceDeployment);

        // No matter what the CONTEXT_VALUE_FS is (i.e. the default context value in the plugin descriptor),
        // if the user specifies an absolute path for the destination, that will be used explicitly. So here in this test,
        // the destination was specified with a destDir that had an absolute path of /tmp/dest and it will be used as-is
        mockBundleManager.absolutePathToAssert = getPath("/tmp/dest");
        BundleScheduleResponse response = mockBundleManager.schedule(request);
        assertSuccess(response);
        assertBundleDeploymentStatus(BundleDeploymentStatus.SUCCESS);
    }

    private static String getPath(String path) {
        return ((File.separatorChar == '\\') && path.startsWith("/")) ? ("C:" + path) : path;
    }

    public void testNonPlatformBundleDeploy_FileSystem_RelativePath() throws Exception {
        MockInventoryManager im = (MockInventoryManager) mockBundleManager.getInventoryManager();

        BundleType bundleType = new BundleType("bundleTypeName", im.bundleHandlerType);
        Bundle bundle = new Bundle("bundleName", bundleType, null, null);
        BundleVersion bundleVersion = new BundleVersion("bundleVersionName", "1.0", bundle, "");
        BundleDestination destination = new BundleDestination(bundle, "destName", null,
            MockInventoryManager.BUNDLE_CONFIG_NAME_FS, "relative/path"); // RELATIVE PATH
        BundleDeployment bundleDeployment = new BundleDeployment(bundleVersion, destination, "deploymentName");
        BundleResourceDeployment resourceDeployment = new BundleResourceDeployment(bundleDeployment, im.serverFS);
        BundleScheduleRequest request = new BundleScheduleRequest(resourceDeployment);

        // in the real world, the context value for fileSystem contexts will probably always be "/" but
        // to test that we are really using this context value, our tests set it to something other than "/".
        // That's why we prepend CONTEXT_VALUE_FS to the front of the destination's destDir
        // note that we expect that relative path converted to absolute
        mockBundleManager.absolutePathToAssert = MockInventoryManager.BUNDLE_CONFIG_CONTEXT_VALUE_FS + "/relative/path";
        BundleScheduleResponse response = mockBundleManager.schedule(request);
        assertSuccess(response);
        assertBundleDeploymentStatus(BundleDeploymentStatus.SUCCESS);

    }

    public void testNonPlatformBundleDeploy_PluginConfig() throws Exception {
        MockInventoryManager im = (MockInventoryManager) mockBundleManager.getInventoryManager();

        BundleType bundleType = new BundleType("bundleTypeName", im.bundleHandlerType);
        Bundle bundle = new Bundle("bundleName", bundleType, null, null);
        BundleVersion bundleVersion = new BundleVersion("bundleVersionName", "1.0", bundle, "");
        BundleDestination destination = new BundleDestination(bundle, "destName", null,
            MockInventoryManager.BUNDLE_CONFIG_NAME_PC, "relative/path/pc");
        BundleDeployment bundleDeployment = new BundleDeployment(bundleVersion, destination, "deploymentName");
        BundleResourceDeployment resourceDeployment = new BundleResourceDeployment(bundleDeployment, im.serverPC);
        BundleScheduleRequest request = new BundleScheduleRequest(resourceDeployment);

        mockBundleManager.absolutePathToAssert = MockInventoryManager.BUNDLE_CONFIG_LOCATION_PC + "/relative/path/pc";
        BundleScheduleResponse response = mockBundleManager.schedule(request);
        assertSuccess(response);
        assertBundleDeploymentStatus(BundleDeploymentStatus.SUCCESS);

    }

    public void testNonPlatformBundleDeploy_ResourceConfig() throws Exception {
        MockInventoryManager im = (MockInventoryManager) mockBundleManager.getInventoryManager();

        BundleType bundleType = new BundleType("bundleTypeName", im.bundleHandlerType);
        Bundle bundle = new Bundle("bundleName", bundleType, null, null);
        BundleVersion bundleVersion = new BundleVersion("bundleVersionName", "1.0", bundle, "");
        BundleDestination destination = new BundleDestination(bundle, "destName", null,
            MockInventoryManager.BUNDLE_CONFIG_NAME_RC, "relative/path/rc");
        BundleDeployment bundleDeployment = new BundleDeployment(bundleVersion, destination, "deploymentName");
        BundleResourceDeployment resourceDeployment = new BundleResourceDeployment(bundleDeployment, im.serverRC);
        BundleScheduleRequest request = new BundleScheduleRequest(resourceDeployment);

        mockBundleManager.absolutePathToAssert = MockInventoryManager.BUNDLE_CONFIG_LOCATION_RC + "/relative/path/rc";
        BundleScheduleResponse response = mockBundleManager.schedule(request);
        assertSuccess(response);
        assertBundleDeploymentStatus(BundleDeploymentStatus.SUCCESS);

    }

    public void testNonPlatformBundleDeploy_Trait() throws Exception {
        MockInventoryManager im = (MockInventoryManager) mockBundleManager.getInventoryManager();

        BundleType bundleType = new BundleType("bundleTypeName", im.bundleHandlerType);
        Bundle bundle = new Bundle("bundleName", bundleType, null, null);
        BundleVersion bundleVersion = new BundleVersion("bundleVersionName", "1.0", bundle, "");
        BundleDestination destination = new BundleDestination(bundle, "destName", null,
            MockInventoryManager.BUNDLE_CONFIG_NAME_MT, "relative/path/mt");
        BundleDeployment bundleDeployment = new BundleDeployment(bundleVersion, destination, "deploymentName");
        BundleResourceDeployment resourceDeployment = new BundleResourceDeployment(bundleDeployment, im.serverMT);
        BundleScheduleRequest request = new BundleScheduleRequest(resourceDeployment);

        mockBundleManager.absolutePathToAssert = MockInventoryManager.BUNDLE_CONFIG_LOCATION_MT + "/relative/path/mt";
        BundleScheduleResponse response = mockBundleManager.schedule(request);
        assertSuccess(response);
        assertBundleDeploymentStatus(BundleDeploymentStatus.SUCCESS);

    }

    public void testNonPlatformBundleDeploy_FileSystem_Failure() throws Exception {
        MockInventoryManager im = (MockInventoryManager) mockBundleManager.getInventoryManager();

        BundleType bundleType = new BundleType("bundleTypeName", im.bundleHandlerType);
        Bundle bundle = new Bundle("bundleName", bundleType, null, null);
        BundleVersion bundleVersion = new BundleVersion("bundleVersionName", "1.0", bundle, "");
        BundleDestination destination = new BundleDestination(bundle, "destName", null,
            MockInventoryManager.BUNDLE_CONFIG_NAME_FS, getPath("/tmp/dest")); // ABSOLUTE PATH
        BundleDeployment bundleDeployment = new BundleDeployment(bundleVersion, destination, "deploymentName");
        BundleResourceDeployment resourceDeployment = new BundleResourceDeployment(bundleDeployment, im.serverFS);
        BundleScheduleRequest request = new BundleScheduleRequest(resourceDeployment);

        mockBundleManager.absolutePathToAssert = "/should_fail_to_match"; // this will not match the /tmp/dest that we set the destination to
        BundleScheduleResponse response = mockBundleManager.schedule(request);
        assertSuccess(response);
        assertBundleDeploymentStatus(BundleDeploymentStatus.FAILURE);

    }

    private void assertSuccess(BundleScheduleResponse response) {
        assert response.isSuccess() : response;
    }

    private void assertBundleDeploymentStatus(BundleDeploymentStatus statusToAssert) throws Exception {
        MockBundleServerService bundleService;
        bundleService = (MockBundleServerService) pcConfig.getServerServices().getBundleServerService();
        assert bundleService.lastStatusLatch.await(30, TimeUnit.SECONDS) : "Test did not complete in a timely manner - is it hung?";
        assert bundleService.lastStatus == statusToAssert : "Deployment status [" + bundleService.lastStatus
            + "] did not match what was expected [" + statusToAssert + "]";
    }

    private class MockBundleManager extends BundleManager {
        public String absolutePathToAssert;

        @Override
        protected InventoryManager getInventoryManager() {
            return new MockInventoryManager();
        }

        @Override
        protected MeasurementManager getMeasurementManager() {
            return new MockMeasurementManager();
        }

        @Override
        protected BundleFacet getBundleFacet(int resourceId, long timeout) throws PluginContainerException {
            return new MockBundleFacet(this);
        }
    }

    private class MockBundleFacet implements BundleFacet {
        MockBundleManager manager;

        public MockBundleFacet(MockBundleManager mbm) {
            manager = mbm;
        }

        @Override
        public BundleDeployResult deployBundle(BundleDeployRequest request) {
            BundleDeployResult result = new BundleDeployResult();
            // tests should be setting MockBundleManager.absolutePathToAssert to the path that should be expected
            if (!request.getAbsoluteDestinationDirectory().equals(new File(manager.absolutePathToAssert))) {
                result.setErrorMessage("absolute path [" + request.getAbsoluteDestinationDirectory()
                    + "] did not match the expected path [" + manager.absolutePathToAssert + "]");
                System.out.println(result.getErrorMessage());
            }
            return result;
        }

        @Override
        public BundlePurgeResult purgeBundle(BundlePurgeRequest request) {
            BundlePurgeResult result = new BundlePurgeResult();
            return result;
        }
    }

    private class MockBundleServerService implements BundleServerService {
        public BundleDeploymentStatus lastStatus = null;
        public CountDownLatch lastStatusLatch = new CountDownLatch(1);

        @Override
        public void addDeploymentHistory(int bundleDeploymentId, BundleResourceDeploymentHistory history) {
            return;
        }

        @Override
        public long downloadPackageBits(PackageVersion packageVersion, OutputStream outputStream) {
            return 0;
        }

        @Override
        public List<PackageVersion> getAllBundleVersionPackageVersions(int bundleVersionId) {
            return new ArrayList<PackageVersion>(0);
        }

        @Override
        public void setBundleDeploymentStatus(int bundleDeploymentId, BundleDeploymentStatus status) {
            // only track success or failure status
            if ((status == BundleDeploymentStatus.SUCCESS) || (status == BundleDeploymentStatus.FAILURE)) {
                lastStatus = status;
                lastStatusLatch.countDown();
            }
            return;
        }
    }

    private static class MockInventoryManager extends InventoryManager {
        private static final String BUNDLE_CONFIG_NAME_FS = "fsBaseDirLocation";
        private static final String BUNDLE_CONFIG_CONTEXT_VALUE_FS = getPath("/blah");

        private static final String BUNDLE_CONFIG_NAME_PC = "pcBaseDirLocation";
        private static final String BUNDLE_CONFIG_CONTEXT_VALUE_PC = "pcPropBundle";
        private static final String BUNDLE_CONFIG_LOCATION_PC = getPath("/pluginconfig/base/dir");

        private static final String BUNDLE_CONFIG_NAME_RC = "rcBaseDirLocation";
        private static final String BUNDLE_CONFIG_CONTEXT_VALUE_RC = "rcPropBundle";
        private static final String BUNDLE_CONFIG_LOCATION_RC = getPath("/resourceconfig/base/dir");

        private static final String BUNDLE_CONFIG_NAME_MT = "mtBaseDirLocation";
        private static final String BUNDLE_CONFIG_CONTEXT_VALUE_MT = "traitBundle";
        private static final String BUNDLE_CONFIG_LOCATION_MT = getPath("/trait/base/dir");

        // mocking the following:
        // - one platform type and a platform resource to be used as the root parent
        // - one bundle handler type and resource to mimic our bundle handler component
        // - one server resource for each kind of "destination base directory context"
        // ** FS = fileSystem (the bundle will be deployed directly to the root file system)
        // ** PC = pluginConfiguration (bundle deployed to a directory specified in a plugin config property)
        // ** RC = resourceConfiguration (bundle deployed to a directory specified in a resource config property)
        // ** MT = measurementTrait (bundle deployed to a directory specified in a measurement trait value)
        public ResourceType platformType;
        public ResourceType bundleHandlerType;
        public ResourceType serverTypeFS;
        public ResourceType serverTypePC;
        public ResourceType serverTypeRC;
        public ResourceType serverTypeMT;
        public Resource platform;
        public Resource bundleHandler;
        public Resource serverFS;
        public Resource serverPC;
        public Resource serverRC;
        public Resource serverMT;
        public HashMap<ResourceType, Resource> typeResourceMap = new HashMap<ResourceType, Resource>();
        public HashMap<Integer, ResourceContainer> idResourceContainerMap = new HashMap<Integer, ResourceContainer>();

        public MockInventoryManager() {
            platformType = new ResourceType("platformResourceTypeName", "pluginName", ResourceCategory.PLATFORM, null);
            bundleHandlerType = new ResourceType("bhRTypeName", "pluginName", ResourceCategory.SERVER, platformType);
            serverTypeFS = new ResourceType("typeName-fileSystem", "pluginName", ResourceCategory.SERVER, platformType);
            serverTypePC = new ResourceType("typeName-plugConfig", "pluginName", ResourceCategory.SERVER, platformType);
            serverTypeRC = new ResourceType("typeName-reSconfig", "pluginName", ResourceCategory.SERVER, platformType);
            serverTypeMT = new ResourceType("typeName-trait", "pluginName", ResourceCategory.SERVER, platformType);

            int id = 1;
            platform = new Resource("platformKey", "platformName", platformType);
            platform.setId(id++);
            bundleHandler = new Resource("bhKey", "bhName", bundleHandlerType);
            bundleHandler.setId(id++);
            bundleHandler.setParentResource(platform);
            bundleHandler.setUuid(UUID.randomUUID().toString());
            serverFS = new Resource("serverKey-fileSystem", "serverName-fileSystem", serverTypeFS);
            serverFS.setId(id++);
            serverFS.setParentResource(platform);
            serverPC = new Resource("serverKey-plugConfig", "serverName-plugConfig", serverTypePC);
            serverPC.setId(id++);
            serverPC.setParentResource(platform);
            serverRC = new Resource("serverKey-resConfig", "serverName-resConfig", serverTypeRC);
            serverRC.setId(id++);
            serverRC.setParentResource(platform);
            serverMT = new Resource("serverKey-traitConfig", "serverName-traitConfig", serverTypeMT);
            serverMT.setId(id++);
            serverMT.setParentResource(platform);

            typeResourceMap.put(platformType, platform);
            typeResourceMap.put(bundleHandlerType, bundleHandler);
            typeResourceMap.put(serverTypeFS, serverFS);
            typeResourceMap.put(serverTypePC, serverPC);
            typeResourceMap.put(serverTypeRC, serverRC);
            typeResourceMap.put(serverTypeMT, serverMT);

            ResourceContainer platformContainer = new ResourceContainer(platform, null);
            ResourceContainer bundleHandlerContainer = new ResourceContainer(bundleHandler, null);
            ResourceContainer serverContainerFS = new ResourceContainer(serverFS, null);
            ResourceContainer serverContainerPC = new ResourceContainer(serverPC, null);
            ResourceContainer serverContainerRC = new ResourceContainer(serverRC, null);
            ResourceContainer serverContainerMT = new ResourceContainer(serverMT, null);
            idResourceContainerMap.put(platform.getId(), platformContainer);
            idResourceContainerMap.put(bundleHandler.getId(), bundleHandlerContainer);
            idResourceContainerMap.put(serverFS.getId(), serverContainerFS);
            idResourceContainerMap.put(serverPC.getId(), serverContainerPC);
            idResourceContainerMap.put(serverRC.getId(), serverContainerRC);
            idResourceContainerMap.put(serverMT.getId(), serverContainerMT);

            bundleHandlerContainer.setResourceContext(new MockResourceContext(bundleHandler));

            // each different resource type that supports bundle deployments needs to define its
            // bundle configuration to denote where the base directory location is found.
            // Today we support four ways: via plugin config property, resource config property,
            // measurement trait value, or strictly on the root file system (using no resource specific value)
            ResourceTypeBundleConfiguration rtbc = new ResourceTypeBundleConfiguration(new Configuration());
            rtbc.addBundleDestinationBaseDirectory(BUNDLE_CONFIG_NAME_FS, Context.fileSystem.name(),
                BUNDLE_CONFIG_CONTEXT_VALUE_FS, null);
            serverTypeFS.setResourceTypeBundleConfiguration(rtbc);

            rtbc = new ResourceTypeBundleConfiguration(new Configuration());
            rtbc.addBundleDestinationBaseDirectory(BUNDLE_CONFIG_NAME_PC, Context.pluginConfiguration.name(),
                BUNDLE_CONFIG_CONTEXT_VALUE_PC, null);
            serverTypePC.setResourceTypeBundleConfiguration(rtbc);

            rtbc = new ResourceTypeBundleConfiguration(new Configuration());
            rtbc.addBundleDestinationBaseDirectory(BUNDLE_CONFIG_NAME_RC, Context.resourceConfiguration.name(),
                BUNDLE_CONFIG_CONTEXT_VALUE_RC, null);
            serverTypeRC.setResourceTypeBundleConfiguration(rtbc);

            rtbc = new ResourceTypeBundleConfiguration(new Configuration());
            rtbc.addBundleDestinationBaseDirectory(BUNDLE_CONFIG_NAME_MT, Context.measurementTrait.name(),
                BUNDLE_CONFIG_CONTEXT_VALUE_MT, null);
            serverTypeMT.setResourceTypeBundleConfiguration(rtbc);

            // each different resource needs to specify where exactly it wants the bundles deployed
            // using the different contexts that are supported.
            Configuration pluginConfiguration = new Configuration();
            pluginConfiguration.put(new PropertySimple(BUNDLE_CONFIG_CONTEXT_VALUE_PC, BUNDLE_CONFIG_LOCATION_PC));
            serverPC.setPluginConfiguration(pluginConfiguration);

            Configuration resourceConfiguration = new Configuration();
            resourceConfiguration.put(new PropertySimple(BUNDLE_CONFIG_CONTEXT_VALUE_RC, BUNDLE_CONFIG_LOCATION_RC));
            serverRC.setResourceConfiguration(resourceConfiguration);

            MeasurementDefinition definition = new MeasurementDefinition(serverTypeMT, BUNDLE_CONFIG_CONTEXT_VALUE_MT);
            definition.setDataType(DataType.TRAIT);
            definition.setId(123);
            MeasurementSchedule schedule = new MeasurementSchedule(definition, serverMT);
            schedule.setId(123123);
            MeasurementScheduleRequest scheduleRequest = new MeasurementScheduleRequest(schedule);
            Set<MeasurementScheduleRequest> schedules = new HashSet<MeasurementScheduleRequest>(1);
            schedules.add(scheduleRequest);
            serverContainerMT.setMeasurementSchedule(schedules);
        }

        @Override
        public Set<Resource> getResourcesWithType(ResourceType type) {
            HashSet<Resource> set = new HashSet<Resource>(1);
            set.add(typeResourceMap.get(type));
            return set;
        }

        @Override
        public ResourceContainer getResourceContainer(int resourceId) {
            return idResourceContainerMap.get(resourceId);
        }

        @Override
        public ResourceContainer getResourceContainer(Resource resource) {
            return idResourceContainerMap.get(resource.getId());
        }
    }

    private class MockMeasurementManager extends MeasurementManager {
        @Override
        public Set<MeasurementData> getRealTimeMeasurementValue(int resourceId, Set<MeasurementScheduleRequest> requests) {
            // anytime this method gets called, it means our tests are asking for the test trait value. It will
            // always be the same value for all tests.
            MeasurementDataTrait data = new MeasurementDataTrait(requests.iterator().next(),
                MockInventoryManager.BUNDLE_CONFIG_LOCATION_MT);
            Set<MeasurementData> values = new HashSet<MeasurementData>();
            values.add(data);
            return values;
        }
    }

    @SuppressWarnings("unchecked")
    private static class MockResourceContext extends ResourceContext {
        public MockResourceContext(Resource resource) {
            super(resource, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }
    }
}