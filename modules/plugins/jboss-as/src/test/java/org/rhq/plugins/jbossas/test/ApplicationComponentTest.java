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
package org.rhq.plugins.jbossas.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Set;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.util.file.JarContentFileInfo;
import org.rhq.plugins.jbossas.ApplicationComponent;
import org.rhq.plugins.jbossas.JBossASServerComponent;
import org.rhq.plugins.jbossas.util.FileContentDelegate;

@PrepareForTest({ ApplicationComponent.class })
public class ApplicationComponentTest {

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @Test
    public void testDiscoverDeployedPackagesWithDisplayVersionAndSha256Generation() throws Exception {
        //create the object under test as a partial mock because only one
        //public method will be tested, while the rest will be mocked.
        ApplicationComponent objectUnderTest = mock(ApplicationComponent.class);

        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        PackageType mockPackageType = mock(PackageType.class);

        @SuppressWarnings("unchecked")
        ResourceContext<JBossASServerComponent<?>> mockResourceContext = mock(ResourceContext.class);
        when(objectUnderTest.getResourceContext()).thenReturn(mockResourceContext);

        Configuration mockConfiguration = mock(Configuration.class);
        when(mockResourceContext.getPluginConfiguration()).thenReturn(mockConfiguration);

        when(mockConfiguration.getSimpleValue(eq("filename"), isNull(String.class))).thenReturn("testFileName");

        File mockFile = mock(File.class);
        PowerMockito.whenNew(File.class).withParameterTypes(String.class).withArguments(any(String.class))
            .thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(true);

        when(mockFile.getName()).thenReturn("testFileName");

        FileContentDelegate mockFileContentDelegate = mock(FileContentDelegate.class);
        PowerMockito.whenNew(FileContentDelegate.class).withArguments(any(File.class), isNull(), isNull())
            .thenReturn(mockFileContentDelegate);
        when(mockFileContentDelegate.getSHA(any(File.class))).thenReturn("abcd1234");

        JarContentFileInfo mockJarContentFileInfo = mock(JarContentFileInfo.class);
        PowerMockito.whenNew(JarContentFileInfo.class).withParameterTypes(File.class).withArguments(any(File.class))
            .thenReturn(mockJarContentFileInfo);
        when(mockJarContentFileInfo.getVersion(isNull(String.class))).thenReturn("testDisplayVersion");

        //create object to test and inject required dependencies
        when(objectUnderTest.discoverDeployedPackages(any(PackageType.class))).thenCallRealMethod();

        //run code under test
        Set<ResourcePackageDetails> result = objectUnderTest.discoverDeployedPackages(mockPackageType);

        //verify the results (Assert and mock verification)
        Assert.assertEquals(result.size(), 1);

        ResourcePackageDetails resultPackageDetails = (ResourcePackageDetails) result.toArray()[0];

        verifyNoMoreInteractions(mockPackageType);

        Assert.assertEquals(resultPackageDetails.getVersion(), "[sha256=abcd1234]");
        Assert.assertEquals(resultPackageDetails.getDisplayVersion(), "testDisplayVersion");
        Assert.assertEquals(resultPackageDetails.getSHA256(), "abcd1234");

        verify(mockFileContentDelegate).getSHA(any(File.class));
        verify(mockJarContentFileInfo).getVersion(isNull(String.class));
    }
}