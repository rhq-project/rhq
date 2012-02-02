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
package org.rhq.enterprise.server.plugins.disk.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.File;
import java.util.Collection;

import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.file.ContentFileInfo;
import org.rhq.core.util.file.ContentFileInfoFactory;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetails;
import org.rhq.enterprise.server.plugin.pc.content.PackageSyncReport;
import org.rhq.enterprise.server.plugins.disk.DiskSource;

@PrepareForTest({ DiskSource.class, ContentFileInfoFactory.class })
public class DiskSourceTest{

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void noPackageDiscoveredOnDisk() throws Exception {

        //set all the method call arguments
        String repoName = "testRepo";
        PackageSyncReport mockPackageSyncReport = mock(PackageSyncReport.class);
        Collection<ContentProviderPackageDetails> mockCollection = mock(Collection.class);

        //tell the method story as it happens: mock dependencies and make them
        //behave in a way to get the method under test to completion.
        Configuration mockConfiguration = mock(Configuration.class);
        PropertySimple mockProperty = mock(PropertySimple.class);
        when(mockConfiguration.get("packageSourceEnabled")).thenReturn(mockProperty);
        when(mockConfiguration.get("repoSourceEnabled")).thenReturn(mockProperty);
        when(mockProperty.getBooleanValue()).thenReturn(Boolean.TRUE, Boolean.FALSE);

        final String directoryPath = "mock path";

        when(mockConfiguration.getSimpleValue(eq("rootDirectory"), anyString())).thenReturn(directoryPath);
        when(mockConfiguration.getSimpleValue(eq("packageTypeName"), anyString())).thenReturn("packageTypeName");
        when(mockConfiguration.getSimpleValue(eq("architectureName"), anyString())).thenReturn("architectureName");
        when(mockConfiguration.getSimpleValue(eq("resourceType"), anyString())).thenReturn("resource-type");

        File mockRootFolder = mock(File.class);
        whenNew(File.class).withArguments(directoryPath).thenReturn(mockRootFolder);
        when(mockRootFolder.exists()).thenReturn(true);
        when(mockRootFolder.canRead()).thenReturn(true);
        when(mockRootFolder.isDirectory()).thenReturn(true);

        when(mockCollection.toArray()).thenReturn(new Object[0]);

        File mockRepoFolder = mock(File.class);
        when(mockRootFolder.listFiles()).thenReturn(new File[] { mockRepoFolder });
        when(mockRepoFolder.isDirectory()).thenReturn(true);
        when(mockRepoFolder.getName()).thenReturn(repoName);
        when(mockRepoFolder.listFiles()).thenReturn(new File[0]);

        //create object to test and inject required dependencies
        DiskSource objectUnderTest = new DiskSource();

        //run the code to be tested
        objectUnderTest.initialize(mockConfiguration);
        objectUnderTest.synchronizePackages(repoName, mockPackageSyncReport, mockCollection);

        //verify the results (Assert and Mock Verification)
        verify(mockPackageSyncReport, never()).addUpdatedPackage(any(ContentProviderPackageDetails.class));
        verify(mockPackageSyncReport, never()).addNewPackage(any(ContentProviderPackageDetails.class));
        verify(mockConfiguration, times(2)).get(anyString());
        verifyNew(File.class).withArguments(directoryPath);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onePackageDiscoveredOnDisk() throws Exception {

        //set all the method call arguments
        String repoName = "testRepo";
        PackageSyncReport mockPackageSyncReport = mock(PackageSyncReport.class);
        Collection<ContentProviderPackageDetails> mockCollection = mock(Collection.class);

        //tell the method story as it happens: mock dependencies and make them
        //behave in a way to get the method under test to completion.
        Configuration mockConfiguration = mock(Configuration.class);
        PropertySimple mockProperty = mock(PropertySimple.class);
        when(mockConfiguration.get("packageSourceEnabled")).thenReturn(mockProperty);
        when(mockConfiguration.get("repoSourceEnabled")).thenReturn(mockProperty);
        when(mockProperty.getBooleanValue()).thenReturn(Boolean.TRUE, Boolean.FALSE);

        final String directoryPath = "mock path";

        when(mockConfiguration.getSimpleValue(eq("rootDirectory"), anyString())).thenReturn(directoryPath);
        when(mockConfiguration.getSimpleValue(eq("packageTypeName"), anyString())).thenReturn("packageTypeName");
        when(mockConfiguration.getSimpleValue(eq("architectureName"), anyString())).thenReturn("architectureName");
        when(mockConfiguration.getSimpleValue(eq("resourceType"), anyString())).thenReturn("resource-type");
        when(mockConfiguration.getSimpleValue(eq("filenameFilter"), anyString())).thenReturn(".*");

        File mockRootFolder = mock(File.class);
        whenNew(File.class).withArguments(directoryPath).thenReturn(mockRootFolder);
        when(mockRootFolder.getAbsolutePath()).thenReturn(directoryPath);
        when(mockRootFolder.exists()).thenReturn(true);
        when(mockRootFolder.canRead()).thenReturn(true);
        when(mockRootFolder.isDirectory()).thenReturn(true);

        when(mockCollection.toArray()).thenReturn(new Object[0]);

        File mockRepoFolder = mock(File.class);
        when(mockRootFolder.listFiles()).thenReturn(new File[] { mockRepoFolder });
        when(mockRepoFolder.isDirectory()).thenReturn(true);
        when(mockRepoFolder.getName()).thenReturn(repoName);

        File mockFile = mock(File.class);
        when(mockRepoFolder.listFiles()).thenReturn(new File[] { mockFile });
        when(mockFile.getAbsolutePath()).thenReturn("test");

        ContentFileInfo contentFileInfo = mock(ContentFileInfo.class);
        PowerMockito.mockStatic(ContentFileInfoFactory.class);
        when(ContentFileInfoFactory.createContentFileInfo(any(File.class))).thenReturn(contentFileInfo);

        MessageDigestGenerator mockDigest = mock(MessageDigestGenerator.class);
        whenNew(MessageDigestGenerator.class).withArguments(anyString()).thenReturn(mockDigest);
        when(mockDigest.calcDigestString(any(File.class))).thenReturn("sha256");

        when(mockFile.getName()).thenReturn("fileName");
        when(mockFile.getAbsolutePath()).thenReturn("mock path");

        //create object to test and inject required dependencies
        DiskSource objectUnderTest = new DiskSource();

        //run the code to be tested
        objectUnderTest.initialize(mockConfiguration);
        objectUnderTest.synchronizePackages(repoName, mockPackageSyncReport, mockCollection);

        //verify the results (Assert and Mock Verification)
        verify(mockPackageSyncReport, never()).addUpdatedPackage(any(ContentProviderPackageDetails.class));
        verify(mockConfiguration, times(2)).get(anyString());
        verifyNew(File.class).withArguments(directoryPath);

        ArgumentCaptor<ContentProviderPackageDetails> argument = ArgumentCaptor
            .forClass(ContentProviderPackageDetails.class);
        verify(mockPackageSyncReport, times(1)).addNewPackage(argument.capture());
        Assert.assertEquals("sha256", argument.getValue().getSHA256());
        Assert.assertEquals("[sha256=sha256]", argument.getValue().getKey().getVersion());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onePackageDiscoveredOnDiskWithRedundantSubfolder() throws Exception {

        //set all the method call arguments
        String repoName = "testRepo";
        PackageSyncReport mockPackageSyncReport = mock(PackageSyncReport.class);
        Collection<ContentProviderPackageDetails> mockCollection = mock(Collection.class);

        //tell the method story as it happens: mock dependencies and make them
        //behave in a way to get the method under test to completion.
        Configuration mockConfiguration = mock(Configuration.class);
        PropertySimple mockProperty = mock(PropertySimple.class);
        when(mockConfiguration.get("packageSourceEnabled")).thenReturn(mockProperty);
        when(mockConfiguration.get("repoSourceEnabled")).thenReturn(mockProperty);
        when(mockProperty.getBooleanValue()).thenReturn(Boolean.TRUE, Boolean.FALSE);

        final String directoryPath = "mock path";

        when(mockConfiguration.getSimpleValue(eq("rootDirectory"), anyString())).thenReturn(directoryPath);
        when(mockConfiguration.getSimpleValue(eq("packageTypeName"), anyString())).thenReturn("packageTypeName");
        when(mockConfiguration.getSimpleValue(eq("architectureName"), anyString())).thenReturn("architectureName");
        when(mockConfiguration.getSimpleValue(eq("resourceType"), anyString())).thenReturn("resource-type");
        when(mockConfiguration.getSimpleValue(eq("filenameFilter"), anyString())).thenReturn(".*");

        File mockRootFolder = mock(File.class);
        whenNew(File.class).withArguments(directoryPath).thenReturn(mockRootFolder);
        when(mockRootFolder.getAbsolutePath()).thenReturn(directoryPath);
        when(mockRootFolder.exists()).thenReturn(true);
        when(mockRootFolder.canRead()).thenReturn(true);
        when(mockRootFolder.isDirectory()).thenReturn(true);

        when(mockCollection.toArray()).thenReturn(new Object[0]);

        File mockRepoFolder = mock(File.class);
        when(mockRootFolder.listFiles()).thenReturn(new File[] { mockRepoFolder });
        when(mockRepoFolder.isDirectory()).thenReturn(true);
        when(mockRepoFolder.getName()).thenReturn(repoName);

        File mockExtraFolder = mock(File.class);
        File mockFile = mock(File.class);
        when(mockRepoFolder.listFiles()).thenReturn(new File[] { mockFile, mockExtraFolder });
        when(mockFile.getAbsolutePath()).thenReturn("test");
        when(mockExtraFolder.isDirectory()).thenReturn(true);

        ContentFileInfo contentFileInfo = mock(ContentFileInfo.class);
        PowerMockito.mockStatic(ContentFileInfoFactory.class);
        when(ContentFileInfoFactory.createContentFileInfo(any(File.class))).thenReturn(contentFileInfo);

        MessageDigestGenerator mockDigest = mock(MessageDigestGenerator.class);
        whenNew(MessageDigestGenerator.class).withArguments(anyString()).thenReturn(mockDigest);
        when(mockDigest.calcDigestString(any(File.class))).thenReturn("sha256");

        when(mockFile.getName()).thenReturn("fileName");
        when(mockFile.getAbsolutePath()).thenReturn("mock path");

        //create object to test and inject required dependencies
        DiskSource objectUnderTest = new DiskSource();

        //run the code to be tested
        objectUnderTest.initialize(mockConfiguration);
        objectUnderTest.synchronizePackages(repoName, mockPackageSyncReport, mockCollection);

        //verify the results (Assert and Mock Verification)
        verify(mockPackageSyncReport, never()).addUpdatedPackage(any(ContentProviderPackageDetails.class));
        verify(mockConfiguration, times(2)).get(anyString());
        verifyNew(File.class).withArguments(directoryPath);

        ArgumentCaptor<ContentProviderPackageDetails> argument = ArgumentCaptor
            .forClass(ContentProviderPackageDetails.class);
        verify(mockPackageSyncReport, times(1)).addNewPackage(argument.capture());
        Assert.assertEquals("sha256", argument.getValue().getSHA256());
        Assert.assertEquals("[sha256=sha256]", argument.getValue().getKey().getVersion());

        verify(mockExtraFolder, times(1)).isDirectory();
    }

}