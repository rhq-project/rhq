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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileFilter;
import java.util.Set;
import java.util.jar.JarFile;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.plugins.jbossas.util.JarContentDelegate;

@PrepareForTest({ JarContentDelegate.class })
public class JarContentDelegateTest {

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @Test
    public void testDiscoverDeployedPackages() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        File mockDirectory = mock(File.class);
        when(mockDirectory.exists()).thenReturn(true);
        when(mockDirectory.isDirectory()).thenReturn(true);
        
        File mockFile = mock(File.class);
        when(mockDirectory.listFiles(any(FileFilter.class))).thenReturn(new File[] { mockFile });
        when(mockFile.getName()).thenReturn("testFile");
        
        JarFile mockJarFile = mock(JarFile.class);
        PowerMockito.whenNew(JarFile.class).withParameterTypes(File.class).withArguments(any(File.class))
            .thenReturn(mockJarFile);

        MessageDigestGenerator mockMessageDigestGenerator = mock(MessageDigestGenerator.class);
        PowerMockito.whenNew(MessageDigestGenerator.class).withParameterTypes(String.class).withArguments(anyString())
            .thenReturn(mockMessageDigestGenerator);
        when(mockMessageDigestGenerator.calcDigestString(any(File.class))).thenReturn("abcd1234");

        //create object to test and inject required dependencies
        JarContentDelegate objectUnderTest = new JarContentDelegate(mockDirectory, "jar");

        //run code under test
        Set<ResourcePackageDetails> resultSet = objectUnderTest.discoverDeployedPackages();

        //verify the results (Assert and mock verification)
        Assert.assertEquals(resultSet.size(), 1);

        ResourcePackageDetails resultPackageDetails = (ResourcePackageDetails) resultSet.toArray()[0];
        
        Assert.assertEquals(resultPackageDetails.getVersion(), "[sha256=abcd1234]");
        Assert.assertEquals(resultPackageDetails.getSHA256(), "abcd1234");
    }
}