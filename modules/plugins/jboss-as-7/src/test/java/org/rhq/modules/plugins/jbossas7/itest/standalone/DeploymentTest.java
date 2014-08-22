/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7.itest.standalone;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.rhq.core.domain.util.ResourceTypeUtility.getMeasurementDefinitions;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.PLUGIN_NAME;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.STANDALONE_RESOURCE_KEY;
import static org.rhq.modules.plugins.jbossas7.test.util.Constants.STANDALONE_RESOURCE_TYPE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceRequest;
import org.rhq.core.clientapi.server.content.ContentDiscoveryReport;
import org.rhq.core.clientapi.server.content.ContentServiceResponse;
import org.rhq.core.clientapi.server.content.DeployPackagesRequest;
import org.rhq.core.clientapi.server.content.RetrievePackageBitsRequest;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.MeasurementDefinitionFilter;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.modules.plugins.jbossas7.itest.AbstractJBossAS7PluginTest;
import org.rhq.test.arquillian.DiscoveredResources;
import org.rhq.test.arquillian.MockingServerServices;
import org.rhq.test.arquillian.RunDiscovery;

/**
 *
 *
 * @author Lukas Krejci
 */
@Test(groups = { "integration", "pc", "standalone" }, singleThreaded = true)
public class DeploymentTest extends AbstractJBossAS7PluginTest {

    private Resource platform;
    private Resource serverResource;

    @DiscoveredResources(plugin = PLUGIN_NAME, resourceType = "Deployment")
    private Set<Resource> deploymentResources;

    @DiscoveredResources(plugin = PLUGIN_NAME, resourceType = "Web Runtime")
    private Set<Resource> webRuntimeResources;

    private static TestDeployments DEPLOYMENT_TO_SERVE = TestDeployments.DEPLOYMENT_1;

    @Override
    protected void injectMocks(MockingServerServices serverServices) {
        Mockito.when(
            serverServices.getContentServerService().downloadPackageBitsForChildResource(Mockito.anyInt(),
                Mockito.anyString(), Mockito.any(PackageDetailsKey.class), Mockito.any(OutputStream.class))).then(
            new Answer<Long>() {
                @Override
                public Long answer(InvocationOnMock invocation) throws Throwable {
                    OutputStream out = (OutputStream) invocation.getArguments()[invocation.getArguments().length - 1];
                    return copyStreamAndReturnCount(out);
                }
            });

        Mockito.when(
            serverServices.getContentServerService().downloadPackageBitsGivenResource(Mockito.anyInt(),
                Mockito.any(PackageDetailsKey.class), Mockito.any(OutputStream.class))).then(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                OutputStream out = (OutputStream) invocation.getArguments()[invocation.getArguments().length - 1];
                return copyStreamAndReturnCount(out);
            }
        });
    }

    @Test(priority = 10)
    @RunDiscovery
    public void initialDiscoveryTest() throws Exception {
        platform = validatePlatform();
        serverResource = waitForResourceByTypeAndKey(platform, platform, STANDALONE_RESOURCE_TYPE,
            STANDALONE_RESOURCE_KEY);
    }

    @Test(priority = 11)
    public void testDeploy() throws Exception {
        ResourcePackageDetails packageDetails = getTestDeploymentPackageDetails(TestDeployments.DEPLOYMENT_1);

        Configuration deploymentConfig = new Configuration();
        deploymentConfig.put(new PropertySimple("runtimeName", packageDetails.getName()));

        CreateResourceRequest request = new CreateResourceRequest();
        request.setPackageDetails(packageDetails);
        request.setParentResourceId(serverResource.getId());
        request.setPluginConfiguration(null);
        request.setPluginName(PLUGIN_NAME);
        request.setResourceConfiguration(deploymentConfig);
        request.setResourceName(TestDeployments.DEPLOYMENT_1.getResourceKey());
        request.setResourceTypeName("Deployment");

        CreateResourceResponse response = pluginContainer.getResourceFactoryManager().executeCreateResourceImmediately(
            request);

        assert response.getStatus() == CreateResourceStatus.SUCCESS : "The deployment failed with an error mesasge: "
            + response.getErrorMessage();

        Resource deployment = waitForResourceByTypeAndKey(platform, serverResource, new ResourceType("Deployment",
            PLUGIN_NAME, ResourceCategory.SERVICE, null), "deployment=" + TestDeployments.DEPLOYMENT_1.getResourceKey());
        // these tests may depend on the deployment children to be in inventory, make sure they are
        waitForAsyncDiscoveryToStabilize(deployment, 5000L, 10);
    }

    @Test(priority = 12)
    public void testPackageDetectedForArchivedDeployment() throws Exception {
        assert deploymentResources != null && deploymentResources.size() == 1 : "The new deployment should have been discovered";

        Resource deployment = deploymentResources.iterator().next();

        // the resource key and resource name are the same, and should be the filename stripped of version.
        assert TestDeployments.DEPLOYMENT_1.getResourceKey().equals(deployment.getName()) : "The deployment doesn't seem to have the expected name";

        ContentDiscoveryReport report = pluginContainer.getContentManager().executeResourcePackageDiscoveryImmediately(
            deployment.getId(), "file");

        Set<ResourcePackageDetails> details = report.getDeployedPackages();

        assert details != null && details.size() == 1 : "The archived deployment should be backed by exactly 1 package.";

        ResourcePackageDetails actualDetails = details.iterator().next();
        ResourcePackageDetails expectedDetails = getTestDeploymentPackageDetails(TestDeployments.DEPLOYMENT_1);

        //we don't expect the resource details to be equal, because the version field
        //will be different - the test code simplistically just assigns "1.0"
        //but the actual code assigns a sha256, because the test war doesn't contain
        //explicit version information.
        //We therefore just compare the name and type...
        String actualName = actualDetails.getName();
        String actualType = actualDetails.getPackageTypeName();
        String expectedName = expectedDetails.getName();
        String expectedType = expectedDetails.getPackageTypeName();

        Assert.assertEquals(actualName, expectedName,
            "The deployment's package details are called differently than expected.");
        Assert.assertEquals(actualType, expectedType,
            "The deployment's package details have different type than expected.");
    }

    @Test(priority = 13)
    public void testDeploymentContentRetrieval() throws Exception {
        testContentRetrieval(TestDeployments.DEPLOYMENT_1);
    }

    @Test(priority = 14)
    public void testRedeploy() throws Exception {
        Resource deployment = deploymentResources.iterator().next();
        //we are updating the deployment 1. So provide a key to the first deployment but later on actually
        //deliver bits of the deployment 2, so that we get the update we "wanted".
        ResourcePackageDetails packageDetails = getTestDeploymentPackageDetails(TestDeployments.DEPLOYMENT_1);

        //this is what our mocked serverside is going to serve the package bits for
        TestDeployments origServedDeployemnt = DEPLOYMENT_TO_SERVE;
        DEPLOYMENT_TO_SERVE = TestDeployments.DEPLOYMENT_2;

        try {
            DeployPackagesRequest request = new DeployPackagesRequest(1, deployment.getId(),
                Collections.singleton(packageDetails));

            DeployPackagesResponse response = pluginContainer.getContentManager().deployPackagesImmediately(request);

            testContentRetrieval(TestDeployments.DEPLOYMENT_2);
        } finally {
            //switch the served deployment back, so that other tests aren't affected
            DEPLOYMENT_TO_SERVE = origServedDeployemnt;
        }
    }

    @Test(priority = 15)
    public void testWebRuntimeMetricsHaveNonNullValues() throws Exception {
        assertTrue(webRuntimeResources != null && !webRuntimeResources.isEmpty(),
            "Web Runtime resource should have been discovered");
        assertEquals(webRuntimeResources.size(), 1, "Found more than one Web Runtime resource: " + webRuntimeResources);

        Resource webRuntimeResource = webRuntimeResources.iterator().next();
        ResourceContainer webRuntimeResourceContainer = pluginContainer.getInventoryManager().getResourceContainer(
            webRuntimeResource);
        MeasurementFacet measurementFacet = webRuntimeResourceContainer.createResourceComponentProxy(
            MeasurementFacet.class, FacetLockType.READ, SECONDS.toMillis(5), false, false, false);
        MeasurementReport report = new MeasurementReport();
        Set<MeasurementScheduleRequest> measurementScheduleRequests = getMeasurementScheduleRequests(webRuntimeResource);
        measurementFacet.getValues(report, measurementScheduleRequests);
        assertEquals(report.getCallTimeData().size(), 0, "No calltime data was requested");
        assertTrue(
            report.getNumericData().size() + report.getTraitData().size() == measurementScheduleRequests.size(),
            "Some requested measurements are missing: "
                + getMissingMeasurements(measurementScheduleRequests, report.getNumericData(), report.getTraitData()));
    }

    @Test(priority = 16)
    public void testUndeploy() throws Exception {
        Resource deployment = deploymentResources.iterator().next();
        DeleteResourceRequest request = new DeleteResourceRequest(0, deployment.getId());
        getServerInventory().removeResource(deployment);
        pluginContainer.getResourceFactoryManager().deleteResource(request);
    }

    private void testContentRetrieval(final TestDeployments deployment) throws Exception {
        final boolean[] lockAndTestResult = new boolean[2];

        //perform the test in the mocked server-side.
        Mockito
            .doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    InputStream data = (InputStream) invocation.getArguments()[1];
                    byte[] md5 = computeHash(data);

                    synchronized (lockAndTestResult) {
                        lockAndTestResult[0] = true;
                        //transfer the test result to the test thread so that
                        //the assert is captured.
                        lockAndTestResult[1] = Arrays.equals(deployment.getHash(), md5);
                        lockAndTestResult.notifyAll();
                    }

                    return null;
                }

                private byte[] computeHash(InputStream data) {
                    try {
                        return MessageDigestGenerator.getDigest(data);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            })
            .when(serverServices.getContentServerService())
            .completeRetrievePackageBitsRequest(Mockito.any(ContentServiceResponse.class),
                Mockito.any(InputStream.class));

        //setup the content retrieval request and execute it
        Resource deploymentResource = deploymentResources.iterator().next();
        RetrievePackageBitsRequest request = new RetrievePackageBitsRequest(0, deploymentResource.getId(),
            getTestDeploymentPackageDetails(deployment));
        pluginContainer.getContentManager().retrievePackageBits(request);

        //the content retrieval is async so wait in the test method until the mocked server-side
        //completed the test (unless it has already done so).
        synchronized (lockAndTestResult) {
            if (!lockAndTestResult[0]) {
                lockAndTestResult.wait();
            }
            assert lockAndTestResult[1] : "The data obtained from the deployment are different to what should have been deployed.";
        }
    }

    private long copyStreamAndReturnCount(OutputStream out) throws IOException {
        if (null == out) {
            System.out.println("**** Unexepected null output stream in mock code!!");
            return 0L;
        }

        String path = DEPLOYMENT_TO_SERVE.getResourcePath();
        InputStream in = getClass().getClassLoader().getResourceAsStream(path);

        long cnt = 0;

        try {
            int data;
            while ((data = in.read()) != -1) {
                if (out != null) {
                    out.write(data);
                }
                cnt++;
            }
        } finally {
            in.close();
            out.flush();
        }
        return cnt;
    }

    static ResourcePackageDetails getTestDeploymentPackageDetails(TestDeployments deployment) {
        ResourcePackageDetails details = new ResourcePackageDetails(new PackageDetailsKey(
            deployment.getDeploymentName(), "1.0", "file", "noarch"));
        details.setFileName(deployment.getDeploymentName());
        details.setDeploymentTimeConfiguration(new Configuration());
        return details;
    }

    static Set<String> getMissingMeasurements(Set<MeasurementScheduleRequest> measurementScheduleRequests,
        Set<MeasurementDataNumeric> numericData, Set<MeasurementDataTrait> traitData) {
        Set<String> missingMeasurements = new HashSet<String>();
        for (MeasurementScheduleRequest measurementScheduleRequest : measurementScheduleRequests) {
            missingMeasurements.add(measurementScheduleRequest.getName());
        }
        for (MeasurementDataNumeric measurementDataNumeric : numericData) {
            missingMeasurements.remove(measurementDataNumeric.getName());
        }
        for (MeasurementDataTrait measurementDataTrait : traitData) {
            missingMeasurements.remove(measurementDataTrait.getName());
        }
        return missingMeasurements;
    }

    static Set<MeasurementScheduleRequest> getMeasurementScheduleRequests(Resource webRuntimeResource) {
        Set<MeasurementDefinition> measurementDefinitions = getMeasurementDefinitions(
            webRuntimeResource.getResourceType(), new MeasurementDefinitionFilter() {
                private final Set<DataType> acceptableDataTypes = EnumSet.of(DataType.MEASUREMENT, DataType.TRAIT);

                @Override
                public boolean accept(MeasurementDefinition measurementDefinition) {
                    return acceptableDataTypes.contains(measurementDefinition.getDataType());
                }
            });
        Set<MeasurementScheduleRequest> measurementScheduleRequests = new HashSet<MeasurementScheduleRequest>();
        for (MeasurementDefinition measurementDefinition : measurementDefinitions) {
            measurementScheduleRequests.add(new MeasurementScheduleRequest(-1, measurementDefinition.getName(), -1,
                true, measurementDefinition.getDataType(), measurementDefinition.getRawNumericType()));
        }
        return measurementScheduleRequests;
    }
}
