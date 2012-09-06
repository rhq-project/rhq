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
package org.rhq.core.pluginapi.inventory;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.UUID;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.MessageDigestGenerator;

@PrepareForTest({ ResourceContext.class })
public class ResourceContextTest {

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testBasicResourceDataDirectory() throws Exception {
        //generate random data for the test
        String[] randomStrings = this.generateRandomStrings(3);
        String expectedSHA256 = this.computeSHA256(randomStrings[0], randomStrings[1]);
        String inputResourceKey = randomStrings[0];
        String inputParentResourceKey = randomStrings[1];
        String inputResourceUuid = randomStrings[2];

        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        ResourceType mockResourceType = mock(ResourceType.class);

        Resource mockResource = mock(Resource.class);
        when(mockResource.getResourceKey()).thenReturn(inputResourceKey);
        when(mockResource.getUuid()).thenReturn(inputResourceUuid);
        when(mockResource.getResourceType()).thenReturn(mockResourceType);

        ResourceContext<?> parentResourceContext = mock(ResourceContext.class);
        when(parentResourceContext.getResourceKey()).thenReturn(inputParentResourceKey);

        File mockNewResourceDirectory = mock(File.class);
        when(mockNewResourceDirectory.exists()).thenReturn(true);

        File mockOldResourceDirectory = mock(File.class);
        when(mockOldResourceDirectory.exists()).thenReturn(false);

        PowerMockito.whenNew(File.class).withParameterTypes(File.class, String.class)
            .withArguments(any(File.class), any(String.class)).thenReturn(mockNewResourceDirectory)
            .thenReturn(mockOldResourceDirectory);

        File mockTemporaryDirectory = mock(File.class);
        File mockDataDirectory = mock(File.class);

        //create object to test and inject required dependencies
        ResourceContext<?> objectUnderTest = new ResourceContext(mockResource, null, parentResourceContext, null, null,
            mockTemporaryDirectory, mockDataDirectory, null, null, null, null, null, null, null);

        //run code under test
        File result = objectUnderTest.getResourceDataDirectory();

        //verify the results (Assert and mock verification)
        PowerMockito.verifyNew(File.class, times(1)).withArguments(eq(mockDataDirectory), eq(expectedSHA256));
        PowerMockito.verifyNew(File.class, times(1)).withArguments(eq(mockDataDirectory), eq(inputResourceUuid));

        verify(mockNewResourceDirectory, times(1)).exists();
        verify(mockNewResourceDirectory, never()).mkdirs();
        verify(mockOldResourceDirectory, times(1)).exists();
        verify(mockOldResourceDirectory, never()).renameTo(any(File.class));

        Assert.assertEquals(result, mockNewResourceDirectory);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testThreeLevelsAncestryResourceDataDirectory() throws Exception {
        //generate random data for the test
        String[] randomStrings = this.generateRandomStrings(6);
        String expectedSHA256 = this.computeSHA256(randomStrings[0], randomStrings[1], randomStrings[2],
            randomStrings[3]);
        String inputResourceKey = randomStrings[0];
        String inputParent1ResourceKey = randomStrings[1];
        String inputParent2ResourceKey = randomStrings[2];
        String inputParent3ResourceKey = randomStrings[3];

        String inputResourceUuid = randomStrings[4];

        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        ResourceType mockResourceType = mock(ResourceType.class);

        Resource mockResource = mock(Resource.class);
        when(mockResource.getResourceKey()).thenReturn(inputResourceKey);
        when(mockResource.getUuid()).thenReturn(inputResourceUuid);
        when(mockResource.getResourceType()).thenReturn(mockResourceType);

        ResourceContext parent3ResourceContext = mock(ResourceContext.class);
        when(parent3ResourceContext.getResourceKey()).thenReturn(inputParent3ResourceKey);

        ResourceContext parent2ResourceContext = mock(ResourceContext.class);
        when(parent2ResourceContext.getResourceKey()).thenReturn(inputParent2ResourceKey);
        when(parent2ResourceContext.getParentResourceContext()).thenReturn((ResourceContext) parent3ResourceContext);

        ResourceContext parent1ResourceContext = mock(ResourceContext.class);
        when(parent1ResourceContext.getResourceKey()).thenReturn(inputParent1ResourceKey);
        when(parent1ResourceContext.getParentResourceContext()).thenReturn((ResourceContext) parent2ResourceContext);

        File mockNewResourceDirectory = mock(File.class);
        when(mockNewResourceDirectory.exists()).thenReturn(true);

        File mockOldResourceDirectory = mock(File.class);
        when(mockOldResourceDirectory.exists()).thenReturn(false);

        PowerMockito.whenNew(File.class).withParameterTypes(File.class, String.class)
            .withArguments(any(File.class), any(String.class)).thenReturn(mockNewResourceDirectory)
            .thenReturn(mockOldResourceDirectory);

        File mockTemporaryDirectory = mock(File.class);
        File mockDataDirectory = mock(File.class);

        //create object to test and inject required dependencies
        ResourceContext<?> objectUnderTest = new ResourceContext(mockResource, null, parent1ResourceContext, null,
            null, mockTemporaryDirectory, mockDataDirectory, null, null, null, null, null, null, null);

        //run code under test
        File result = objectUnderTest.getResourceDataDirectory();

        //verify the results (Assert and mock verification)
        PowerMockito.verifyNew(File.class, times(1)).withArguments(eq(mockDataDirectory), eq(expectedSHA256));
        PowerMockito.verifyNew(File.class, times(1)).withArguments(eq(mockDataDirectory), eq(inputResourceUuid));

        verify(mockNewResourceDirectory, times(1)).exists();
        verify(mockNewResourceDirectory, never()).mkdirs();
        verify(mockOldResourceDirectory, times(1)).exists();
        verify(mockOldResourceDirectory, never()).renameTo(any(File.class));

        Assert.assertEquals(result, mockNewResourceDirectory);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testRenameInvocationResourceDataDirectory() throws Exception {
        //generate random data for the test
        String[] randomStrings = this.generateRandomStrings(3);
        String expectedSHA256 = this.computeSHA256(randomStrings[0], randomStrings[1]);
        String inputResourceKey = randomStrings[0];
        String inputParentResourceKey = randomStrings[1];
        String inputResourceUuid = randomStrings[2];

        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        ResourceType mockResourceType = mock(ResourceType.class);

        Resource mockResource = mock(Resource.class);
        when(mockResource.getResourceKey()).thenReturn(inputResourceKey);
        when(mockResource.getUuid()).thenReturn(inputResourceUuid);
        when(mockResource.getResourceType()).thenReturn(mockResourceType);

        ResourceContext<?> parentResourceContext = mock(ResourceContext.class);
        when(parentResourceContext.getResourceKey()).thenReturn(inputParentResourceKey);

        File mockNewResourceDirectory = mock(File.class);
        when(mockNewResourceDirectory.exists()).thenReturn(true);

        File mockOldResourceDirectory = mock(File.class);
        when(mockOldResourceDirectory.exists()).thenReturn(true);

        PowerMockito.whenNew(File.class).withParameterTypes(File.class, String.class)
            .withArguments(any(File.class), any(String.class)).thenReturn(mockNewResourceDirectory)
            .thenReturn(mockOldResourceDirectory);

        File mockTemporaryDirectory = mock(File.class);
        File mockDataDirectory = mock(File.class);

        //create object to test and inject required dependencies
        ResourceContext<?> objectUnderTest = new ResourceContext(mockResource, null, parentResourceContext, null, null,
            mockTemporaryDirectory, mockDataDirectory, null, null, null, null, null, null, null);

        //run code under test
        File result = objectUnderTest.getResourceDataDirectory();

        //verify the results (Assert and mock verification)
        PowerMockito.verifyNew(File.class, times(1)).withArguments(eq(mockDataDirectory), eq(expectedSHA256));
        PowerMockito.verifyNew(File.class, times(1)).withArguments(eq(mockDataDirectory), eq(inputResourceUuid));

        verify(mockNewResourceDirectory, times(1)).exists();
        verify(mockNewResourceDirectory, never()).mkdirs();
        verify(mockOldResourceDirectory, times(1)).exists();
        verify(mockOldResourceDirectory, times(1)).renameTo(eq(mockNewResourceDirectory));

        Assert.assertEquals(result, mockNewResourceDirectory);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testMkdirsInvocationResourceDataDirectory() throws Exception {
        //generate random data for the test
        String[] randomStrings = this.generateRandomStrings(3);
        String expectedSHA256 = this.computeSHA256(randomStrings[0], randomStrings[1]);
        String inputResourceKey = randomStrings[0];
        String inputParentResourceKey = randomStrings[1];
        String inputResourceUuid = randomStrings[2];

        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        ResourceType mockResourceType = mock(ResourceType.class);

        Resource mockResource = mock(Resource.class);
        when(mockResource.getResourceKey()).thenReturn(inputResourceKey);
        when(mockResource.getUuid()).thenReturn(inputResourceUuid);
        when(mockResource.getResourceType()).thenReturn(mockResourceType);

        ResourceContext<?> parentResourceContext = mock(ResourceContext.class);
        when(parentResourceContext.getResourceKey()).thenReturn(inputParentResourceKey);

        File mockNewResourceDirectory = mock(File.class);
        when(mockNewResourceDirectory.exists()).thenReturn(false);

        File mockOldResourceDirectory = mock(File.class);
        when(mockOldResourceDirectory.exists()).thenReturn(false);

        PowerMockito.whenNew(File.class).withParameterTypes(File.class, String.class)
            .withArguments(any(File.class), any(String.class)).thenReturn(mockNewResourceDirectory)
            .thenReturn(mockOldResourceDirectory);

        File mockTemporaryDirectory = mock(File.class);
        File mockDataDirectory = mock(File.class);

        //create object to test and inject required dependencies
        ResourceContext<?> objectUnderTest = new ResourceContext(mockResource, null, parentResourceContext, null, null,
            mockTemporaryDirectory, mockDataDirectory, null, null, null, null, null, null, null);

        //run code under test
        File result = objectUnderTest.getResourceDataDirectory();

        //verify the results (Assert and mock verification)
        PowerMockito.verifyNew(File.class, times(1)).withArguments(eq(mockDataDirectory), eq(expectedSHA256));
        PowerMockito.verifyNew(File.class, times(1)).withArguments(eq(mockDataDirectory), eq(inputResourceUuid));

        verify(mockNewResourceDirectory, times(1)).exists();
        verify(mockNewResourceDirectory, times(1)).mkdirs();
        verify(mockOldResourceDirectory, times(1)).exists();
        verify(mockOldResourceDirectory, never()).renameTo(any(File.class));

        Assert.assertEquals(result, mockNewResourceDirectory);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testOldResouceDirectoryThrowsException() throws Exception {
        //generate random data for the test
        String[] randomStrings = this.generateRandomStrings(3);
        String expectedSHA256 = this.computeSHA256(randomStrings[0], randomStrings[1]);
        String inputResourceKey = randomStrings[0];
        String inputParentResourceKey = randomStrings[1];
        String inputResourceUuid = randomStrings[2];

        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        ResourceType mockResourceType = mock(ResourceType.class);

        Resource mockResource = mock(Resource.class);
        when(mockResource.getResourceKey()).thenReturn(inputResourceKey);
        when(mockResource.getUuid()).thenReturn(inputResourceUuid);
        when(mockResource.getResourceType()).thenReturn(mockResourceType);

        ResourceContext<?> parentResourceContext = mock(ResourceContext.class);
        when(parentResourceContext.getResourceKey()).thenReturn(inputParentResourceKey);

        File mockNewResourceDirectory = mock(File.class);
        when(mockNewResourceDirectory.exists()).thenReturn(false);

        File mockOldResourceDirectory = mock(File.class);
        when(mockOldResourceDirectory.exists()).thenThrow(new SecurityException());

        PowerMockito.whenNew(File.class).withParameterTypes(File.class, String.class)
            .withArguments(any(File.class), any(String.class)).thenReturn(mockNewResourceDirectory)
            .thenReturn(mockOldResourceDirectory);

        File mockTemporaryDirectory = mock(File.class);
        File mockDataDirectory = mock(File.class);

        //create object to test and inject required dependencies
        ResourceContext<?> objectUnderTest = new ResourceContext(mockResource, null, parentResourceContext, null, null,
            mockTemporaryDirectory, mockDataDirectory, null, null, null, null, null, null, null);

        //run code under test
        File result = objectUnderTest.getResourceDataDirectory();

        //verify the results (Assert and mock verification)
        PowerMockito.verifyNew(File.class, times(1)).withArguments(eq(mockDataDirectory), eq(expectedSHA256));
        PowerMockito.verifyNew(File.class, times(1)).withArguments(eq(mockDataDirectory), eq(inputResourceUuid));

        verify(mockNewResourceDirectory, times(1)).exists();
        verify(mockNewResourceDirectory, times(1)).mkdirs();
        verify(mockOldResourceDirectory, times(1)).exists();
        verify(mockOldResourceDirectory, never()).renameTo(any(File.class));

        Assert.assertEquals(result, mockNewResourceDirectory);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testThreeLevelsAncestryFutureChildDataDirectory() throws Exception {
        //generate random data for the test
        String[] randomStrings = this.generateRandomStrings(6);
        String expectedSHA256 = this.computeSHA256(randomStrings[0], randomStrings[1], randomStrings[2],
            randomStrings[3], randomStrings[4]);
        String inputChildResourceKey = randomStrings[0];
        String inputResourceKey = randomStrings[1];
        String inputParent1ResourceKey = randomStrings[2];
        String inputParent2ResourceKey = randomStrings[3];
        String inputParent3ResourceKey = randomStrings[4];

        String inputResourceUuid = randomStrings[5];

        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        ResourceType mockResourceType = mock(ResourceType.class);

        Resource mockResource = mock(Resource.class);
        when(mockResource.getResourceKey()).thenReturn(inputResourceKey);
        when(mockResource.getUuid()).thenReturn(inputResourceUuid);
        when(mockResource.getResourceType()).thenReturn(mockResourceType);

        ResourceContext parent3ResourceContext = mock(ResourceContext.class);
        when(parent3ResourceContext.getResourceKey()).thenReturn(inputParent3ResourceKey);

        ResourceContext parent2ResourceContext = mock(ResourceContext.class);
        when(parent2ResourceContext.getResourceKey()).thenReturn(inputParent2ResourceKey);
        when(parent2ResourceContext.getParentResourceContext()).thenReturn((ResourceContext) parent3ResourceContext);

        ResourceContext parent1ResourceContext = mock(ResourceContext.class);
        when(parent1ResourceContext.getResourceKey()).thenReturn(inputParent1ResourceKey);
        when(parent1ResourceContext.getParentResourceContext()).thenReturn((ResourceContext) parent2ResourceContext);

        File mockChildResourceDirectory = mock(File.class);
        when(mockChildResourceDirectory.exists()).thenReturn(false);

        PowerMockito.whenNew(File.class).withParameterTypes(File.class, String.class)
            .withArguments(any(File.class), any(String.class)).thenReturn(mockChildResourceDirectory);

        File mockTemporaryDirectory = mock(File.class);
        File mockDataDirectory = mock(File.class);

        //create object to test and inject required dependencies
        ResourceContext<?> objectUnderTest = new ResourceContext(mockResource, null, parent1ResourceContext, null,
            null, mockTemporaryDirectory, mockDataDirectory, null, null, null, null, null, null, null);

        //run code under test
        File result = objectUnderTest.getFutureChildResourceDataDirectory(inputChildResourceKey);

        //verify the results (Assert and mock verification)
        PowerMockito.verifyNew(File.class, times(1)).withArguments(eq(mockDataDirectory), eq(expectedSHA256));

        verify(mockChildResourceDirectory, times(1)).exists();
        verify(mockChildResourceDirectory, times(1)).mkdirs();

        Assert.assertEquals(result, mockChildResourceDirectory);
    }

    private String[] generateRandomStrings(int length) {
        String[] randomUuids = new String[length];
        for (int i = 0; i < length; i++) {
            randomUuids[i] = UUID.randomUUID().getLeastSignificantBits() + "";
        }
        return randomUuids;
    }

    private String computeSHA256(String... values) {
        MessageDigestGenerator messageDigest = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
        for (String value : values) {
            messageDigest.add(value.getBytes());
        }
        return messageDigest.getDigestString();
    }
}