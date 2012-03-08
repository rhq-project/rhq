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
package org.rhq.plugins.jbossas5.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Set;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;

import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.pluginapi.content.FileContentDelegate;
import org.rhq.plugins.jbossas5.AbstractManagedDeploymentComponent;
import org.rhq.plugins.jbossas5.StandaloneManagedDeploymentComponent;

@PrepareForTest({ StandaloneManagedDeploymentComponent.class })
public class StandaloneManagedDeploymentComponentTest {

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    //@Test
    public void testDiscoverDeployedPackages() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.getName()).thenReturn("testFileName");

        PackageType mockPackageType = mock(PackageType.class);

        FileContentDelegate mockFileContentDelegate = mock(FileContentDelegate.class);
        PowerMockito.whenNew(FileContentDelegate.class).withArguments(any(File.class), isNull())
            .thenReturn(mockFileContentDelegate);
        when(
            mockFileContentDelegate.saveDeploymentSHA(any(File.class), any(File.class), any(String.class),
                any(File.class))).thenReturn("abcd1234");

        //create object to test and inject required dependencies
        StandaloneManagedDeploymentComponent objectUnderTest = new StandaloneManagedDeploymentComponent();

        Field[] fields = AbstractManagedDeploymentComponent.class.getDeclaredFields();

        for (Field field : fields) {
            if (field.getName().equals("deploymentFile")) {
                field.setAccessible(true);
                field.set(objectUnderTest, mockFile);
            }
        }

        //run code under test
        Set<ResourcePackageDetails> result = objectUnderTest.discoverDeployedPackages(mockPackageType);

        //verify the results (Assert and mock verification)
        Assert.assertEquals(result.size(), 1);

        ResourcePackageDetails resultPackageDetails = (ResourcePackageDetails) result.toArray()[0];

        Assert.assertEquals(resultPackageDetails.getSHA256(), "abcd1234");
        Assert.assertEquals(resultPackageDetails.getVersion(), "[sha256=abcd1234]");

        verify(mockFileContentDelegate).saveDeploymentSHA(any(File.class), any(File.class), any(String.class),
            any(File.class));
    }
}