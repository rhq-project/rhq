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
package org.jboss.on.plugins.tomcat.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.Manifest;

import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.jboss.on.plugins.tomcat.TomcatServerComponent;
import org.jboss.on.plugins.tomcat.TomcatVHostComponent;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;

public class TomcatVHostComponentTest{

    @Test
    public void testCreateSource() throws Exception {
        //create the object under test as a partial mock because only one 
        //public method will be tested, while the rest will be mocked.
        TomcatVHostComponent objectUnderTest = mock(TomcatVHostComponent.class);

        //tell the method story as it happens: mock dependencies and configure
        //those dependencies to get the method under test to completion.
        CreateResourceReport mockCreateResourceReport = mock(CreateResourceReport.class);
        ResourceType mockResourceType = mock(ResourceType.class);
        when(mockCreateResourceReport.getResourceType()).thenReturn(mockResourceType);
        when(mockResourceType.getName()).thenReturn("Tomcat Web Application (WAR)");

        ResourcePackageDetails mockResourcePackageDetails = mock(ResourcePackageDetails.class);
        when(mockCreateResourceReport.getPackageDetails()).thenReturn(mockResourcePackageDetails);
        PackageDetailsKey mockPackageDetailsKey = mock(PackageDetailsKey.class);
        when(mockResourcePackageDetails.getKey()).thenReturn(mockPackageDetailsKey);
        when(mockPackageDetailsKey.getName()).thenReturn("testApplication.war");

        Configuration mockConfiguration = mock(Configuration.class);
        when(mockResourcePackageDetails.getDeploymentTimeConfiguration()).thenReturn(mockConfiguration);
        PropertySimple mockPropertySimple = mock(PropertySimple.class);
        when(mockConfiguration.getSimple(any(String.class))).thenReturn(mockPropertySimple);
        when(mockPropertySimple.getBooleanValue()).thenReturn(Boolean.TRUE);

        EmsBean mockEmsBean = mock(EmsBean.class);
        when(objectUnderTest.getEmsBean()).thenReturn(mockEmsBean);
        EmsAttribute mockEmsAttribute = mock(EmsAttribute.class);
        when(mockEmsBean.getAttribute(anyString())).thenReturn(mockEmsAttribute);
        when(mockEmsAttribute.getValue()).thenReturn(Boolean.TRUE);

        File deploymentDirectory = new File(this.getClass().getResource("/").getFile() + "deploymentDirectory");
        deleteRecursive(deploymentDirectory);
        when(objectUnderTest.getConfigurationPath()).thenReturn(deploymentDirectory);

        File tempDirectory = new File(this.getClass().getResource("/").getFile() + "tempDirectory");
        deleteRecursive(tempDirectory);
        tempDirectory.mkdirs();

        @SuppressWarnings("unchecked")
        ResourceContext<TomcatServerComponent<?>> mockResourceContext = mock(ResourceContext.class);
        when(objectUnderTest.getResourceContext()).thenReturn(mockResourceContext);
        when(mockResourceContext.getTemporaryDirectory()).thenReturn(tempDirectory);

        ContentContext mockContentContext = mock(ContentContext.class);
        when(mockResourceContext.getContentContext()).thenReturn(mockContentContext);
        ContentServices mockContentServices = mock(ContentServices.class);
        when(mockContentContext.getContentServices()).thenReturn(mockContentServices);

        when(objectUnderTest.isWebApplication(any(File.class))).thenReturn(Boolean.TRUE);

        //run code under test
        when(objectUnderTest.createResource(any(CreateResourceReport.class))).thenCallRealMethod();
        objectUnderTest.createResource(mockCreateResourceReport);

        //verify the results (Assert and mock verification)
        verify(objectUnderTest).getEmsBean();
        verify(objectUnderTest, times(2)).getResourceContext();
        verify(objectUnderTest).getConfigurationPath();
        verify(objectUnderTest).isWebApplication(any(File.class));

        verify(mockContentServices).downloadPackageBitsForChildResource(any(ContentContext.class), anyString(),
            any(PackageDetailsKey.class), any(OutputStream.class));

        verify(mockCreateResourceReport).setStatus(eq(CreateResourceStatus.SUCCESS));
        verify(mockCreateResourceReport).setResourceName(eq("testApplication"));

        File manifestFile = new File(deploymentDirectory.getAbsolutePath() + "/testApplication/META-INF/MANIFEST.MF");
        Assert.assertTrue(manifestFile.exists(), "Manifest file not created properly!");
        Assert.assertNotEquals(manifestFile.length(), 0, "Empty manifest!!");

        InputStream manifestStream = new FileInputStream(manifestFile);
        Manifest manifest = new Manifest(manifestStream);
        String actualSha256Attribute = manifest.getMainAttributes().getValue("RHQ-Sha256");
        manifestStream.close();

        Assert.assertNotNull(actualSha256Attribute);
        Assert.assertEquals(actualSha256Attribute.length(), 64);

        //cleanup resources created for this test
        deleteRecursive(deploymentDirectory);
        deleteRecursive(tempDirectory);
    }

    private void deleteRecursive(File fileToDelete) throws Exception {
        if (fileToDelete.exists()) {
            if (fileToDelete.isDirectory()) {
                for (File file : fileToDelete.listFiles()) {
                    if (file.isDirectory()) {
                        deleteRecursive(file);
                    } else {
                        file.delete();
                    }
                }
            }

            fileToDelete.delete();
        }
    }
}