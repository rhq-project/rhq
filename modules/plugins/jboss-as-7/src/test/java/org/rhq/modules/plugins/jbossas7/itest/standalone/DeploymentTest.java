/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.modules.plugins.jbossas7.itest.standalone;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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
import org.rhq.core.clientapi.server.content.RetrievePackageBitsRequest;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.modules.plugins.jbossas7.StandaloneASComponent;
import org.rhq.modules.plugins.jbossas7.itest.AbstractJBossAS7PluginTest;
import org.rhq.test.arquillian.DiscoveredResources;
import org.rhq.test.arquillian.ResourceComponentInstances;
import org.rhq.test.arquillian.RunDiscovery;
import org.rhq.test.arquillian.ServerServicesSetup;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test(groups = { "integration", "pc", "standalone" }, singleThreaded = true)
public class DeploymentTest extends AbstractJBossAS7PluginTest {

    private static final String TEST_DEPLOYMENT_NAME = "test-simple.war";
    private static final String TEST_DEPLOYMENT_RESOURCE_PATH = "itest/test-simple.war";

    @ResourceComponentInstances(plugin = PLUGIN_NAME, resourceType = "JBossAS7 Standalone Server")
    private Set<StandaloneASComponent> standalones;

    @DiscoveredResources(plugin = PLUGIN_NAME, resourceType = "JBossAS7 Standalone Server")
    private Set<Resource> standaloneResources;

    private Resource serverResource;

    @DiscoveredResources(plugin = PLUGIN_NAME, resourceType = "Deployment")
    private Set<Resource> deploymentResources;

    private static final byte[] DEPLOYMENT_HASH;
    static {
        DEPLOYMENT_HASH =
            computeHash(DeploymentTest.class.getClassLoader().getResourceAsStream(TEST_DEPLOYMENT_RESOURCE_PATH));
    }

    //this is no test method
    @ServerServicesSetup
    @Test(enabled = false)
    public void setupContentServices() {
        Mockito.when(
            serverServices.getContentServerService().downloadPackageBitsForChildResource(Mockito.anyInt(),
                Mockito.anyString(), Mockito.any(PackageDetailsKey.class), Mockito.any(OutputStream.class))).then(
            new Answer<Long>() {
                @Override
                public Long answer(InvocationOnMock invocation) throws Throwable {
                    InputStream str = getClass().getClassLoader().getResourceAsStream(TEST_DEPLOYMENT_RESOURCE_PATH);
                    OutputStream out = (OutputStream) invocation.getArguments()[invocation.getArguments().length - 1];
                    int data;
                    long cnt = 0;
                    while ((data = str.read()) != -1) {
                        if (out != null) {
                            out.write(data);
                        }
                        cnt++;
                    }

                    return cnt;
                }
            });
    }

    @Test(priority = 10)
    @RunDiscovery
    public void assignServerResource() {
        assert standalones != null && standalones.size() == 1 : "Exactly 1 AS7 standalone server component should be present.";
        assert standaloneResources != null && standaloneResources.size() == 1 : "Exactly 1 AS7 standalone server resource should be present.";
        serverResource = standaloneResources.iterator().next();
    }

    //(dependsOnMethods = "assignServerResource")
    @Test(priority = 11)
    public void testDeploy() throws Exception {
        ResourcePackageDetails packageDetails = getTestDeploymentPackageDetails();

        packageDetails.setFileName(TEST_DEPLOYMENT_NAME);
        packageDetails.setDeploymentTimeConfiguration(new Configuration());

        Configuration deploymentConfig = new Configuration();
        deploymentConfig.put(new PropertySimple("runtimeName", packageDetails.getName()));

        CreateResourceRequest request = new CreateResourceRequest();
        request.setPackageDetails(packageDetails);
        request.setParentResourceId(serverResource.getId());
        request.setPluginConfiguration(null);
        request.setPluginName(PLUGIN_NAME);
        request.setResourceConfiguration(deploymentConfig);
        request.setResourceName(packageDetails.getName());
        request.setResourceTypeName("Deployment");

        CreateResourceResponse response =
            pluginContainer.getResourceFactoryManager().executeCreateResourceImmediately(request);

        assert response.getStatus() == CreateResourceStatus.SUCCESS : "The deoloyment failed with an error mesasge: "
            + response.getErrorMessage();
    }

    @Test(priority = 12)
    public void testPackageDetectedForArchivedDeployment() throws Exception {
        assert deploymentResources != null && deploymentResources.size() == 1 : "The new deployment should have been discovered";

        Resource deployment = deploymentResources.iterator().next();

        assert TEST_DEPLOYMENT_NAME.equals(deployment.getName()) : "The deployment doesn't seem to have the expected name";
        
        ContentDiscoveryReport report = pluginContainer.getContentManager().executeResourcePackageDiscoveryImmediately(deployment.getId(), "file");
        
        Set<ResourcePackageDetails> details = report.getDeployedPackages();
        
        assert details != null && details.size() == 1 : "The archived deployment should be backed by exactly 1 package.";
        
        ResourcePackageDetails actualDetails = details.iterator().next();
        ResourcePackageDetails expectedDetails = getTestDeploymentPackageDetails();
        
        //we don't expect the resource details to be equal, because the version field
        //will be different - the test code simplistically just assigns "1.0"
        //but the actual code assigns a sha256, because the test war doesn't contain
        //explicit version information.
        //We therefore just compare the name and type...
        String actualName = actualDetails.getName();
        String actualType = actualDetails.getPackageTypeName();
        String expectedName = expectedDetails.getName();
        String expectedType = expectedDetails.getPackageTypeName();
        
        Assert.assertEquals(actualName, expectedName, "The deployment's package details are called differently than expected.");
        Assert.assertEquals(actualType, expectedType, "The deployment's package details have different type than expected.");
    }

    @Test(priority = 13)
    public void testDeploymentContentRetrieval() throws Exception {
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
                        lockAndTestResult[1] = Arrays.equals(DEPLOYMENT_HASH, md5);
                        lockAndTestResult.notifyAll();
                    }

                    return null;
                }
            })
            .when(serverServices.getContentServerService())
            .completeRetrievePackageBitsRequest(Mockito.any(ContentServiceResponse.class),
                Mockito.any(InputStream.class));

        //setup the content retrieval request and execute it
        Resource deployment = deploymentResources.iterator().next();
        RetrievePackageBitsRequest request =
            new RetrievePackageBitsRequest(0, deployment.getId(), getTestDeploymentPackageDetails());
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

    @Test(priority = 14)
    public void testUndeploy() throws Exception {
        Resource deployment = deploymentResources.iterator().next();
        DeleteResourceRequest request = new DeleteResourceRequest(0, deployment.getId());
        getServerInventory().removeResource(deployment);
        pluginContainer.getResourceFactoryManager().deleteResource(request);
    }
    
    private ResourcePackageDetails getTestDeploymentPackageDetails() {
        return new ResourcePackageDetails(new PackageDetailsKey(TEST_DEPLOYMENT_NAME, "1.0", "file", "noarch"));
    }

    private static byte[] computeHash(InputStream str) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            int data;
            while ((data = str.read()) != -1) {
                out.write(data);
            }

            return MessageDigest.getInstance("md5").digest(out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Could not determine the MD5 of the test deployment.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "Could not instantiate MD5 message digest algorithm, this should not happen.", e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
            }
            try {
                str.close();
            } catch (IOException e) {
            }
        }
    }
}
