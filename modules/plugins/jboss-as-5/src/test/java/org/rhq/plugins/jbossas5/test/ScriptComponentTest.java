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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.plugins.jbossas5.ApplicationServerComponent;
import org.rhq.plugins.jbossas5.script.ScriptComponent;

@PrepareForTest({ ScriptComponent.class })
public class ScriptComponentTest {

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testDiscoverDeployedPackages() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        ResourceContext<ApplicationServerComponent<?>> mockResourceContext = mock(ResourceContext.class);

        ApplicationServerComponent mockApplicationServerComponent = mock(ApplicationServerComponent.class);
        when(mockResourceContext.getParentResourceComponent()).thenReturn(mockApplicationServerComponent);
        when(mockApplicationServerComponent.getResourceContext()).thenReturn(mockResourceContext);
        Configuration mockConfiguration = mock(Configuration.class);
        when(mockResourceContext.getPluginConfiguration()).thenReturn(mockConfiguration);
        when(mockConfiguration.getSimpleValue(eq("homeDir"), isNull(String.class))).thenReturn("testHomeDir");

        File mockDirectory = mock(File.class);
        PowerMockito.whenNew(File.class).withParameterTypes(String.class, String.class)
            .withArguments(eq("testHomeDir"), eq("bin"))
            .thenReturn(mockDirectory);

        when(mockResourceContext.getResourceKey()).thenReturn("testResource");

        File mockFile = mock(File.class);
        PowerMockito.whenNew(File.class).withParameterTypes(File.class, String.class)
            .withArguments(eq(mockDirectory), eq("testResource")).thenReturn(mockFile);

        when(mockFile.getName()).thenReturn("testResource");

        MessageDigestGenerator mockMessageDigestGenerator = mock(MessageDigestGenerator.class);
        PowerMockito.whenNew(MessageDigestGenerator.class).withParameterTypes(String.class).withArguments(anyString())
            .thenReturn(mockMessageDigestGenerator);
        when(mockMessageDigestGenerator.calcDigestString(any(File.class))).thenReturn("abcd1234");

        PackageType mockPackageType = mock(PackageType.class);
        when(mockPackageType.getName()).thenReturn("script");

        //create object to test and inject required dependencies
        ScriptComponent objectUnderTest = new ScriptComponent();
        objectUnderTest.start(mockResourceContext);

        //run code under test
        Set<ResourcePackageDetails> result = objectUnderTest.discoverDeployedPackages(mockPackageType);

        //verify the results (Assert and mock verification)
        Assert.assertEquals(result.size(), 1);

        ResourcePackageDetails resultPackageDetails = (ResourcePackageDetails) result.toArray()[0];

        Assert.assertEquals(resultPackageDetails.getSHA256(), "abcd1234");

        PowerMockito.verifyNew(File.class).withArguments(eq("testHomeDir"), eq("bin"));
        PowerMockito.verifyNew(File.class).withArguments(eq(mockDirectory), eq("testResource"));
        verify(mockMessageDigestGenerator).calcDigestString(any(File.class));
        verify(mockResourceContext).getParentResourceComponent();
    }
}