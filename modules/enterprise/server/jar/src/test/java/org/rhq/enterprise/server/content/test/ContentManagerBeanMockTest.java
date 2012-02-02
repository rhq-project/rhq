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
package org.rhq.enterprise.server.content.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageBitsBlob;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.enterprise.server.content.ContentManagerBean;
import org.rhq.enterprise.server.content.ContentManagerHelper;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.plugin.pc.content.PackageTypeBehavior;

@PrepareForTest(ContentManagerHelper.class)
public class ContentManagerBeanMockTest {

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    /*
     * Test createPackageVersionWithDisplayVersion with the following hypothesis:
     * 1) There are no existing package version objects in the database
     * 2) A new package will be created because of (1)
     * 3) Focal point of the test is the first 10 lines of the method.
     */
    @Test
    @SuppressWarnings("rawtypes")
    public void testCreatePackageVersion() throws Exception {

        //set all the method call arguments
        Subject mockSubject = mock(Subject.class);
        String packageName = "PackageName";
        int packageTypeID = 1;
        String version = "PackageVersion";
        String displayVersion = "PackageDisplayVersion";
        int architectureId = 2;
        String sampleContent = "SampleContent";
        InputStream packageBitStream = new ByteArrayInputStream(sampleContent.getBytes());

        //tell the method story as it happens: mock dependencies and make them
        //behave in a way to get the method under test to completion.
        EntityManager mockEntityManager = mock(EntityManager.class);

        Query mockQuery = mock(Query.class);
        when(mockEntityManager.createNamedQuery(eq(PackageVersion.QUERY_FIND_BY_PACKAGE_VER_ARCH))).thenReturn(
            mockQuery);
        List mockList = mock(List.class);
        when(mockQuery.getResultList()).thenReturn(mockList);

        Architecture mockArchitecture = mock(Architecture.class);
        when(mockArchitecture.getName()).thenReturn("ArchitectureName");
        when(mockEntityManager.find(eq(Architecture.class), anyInt())).thenReturn(mockArchitecture);

        PackageType mockPackageType = mock(PackageType.class);
        when(mockEntityManager.find(eq(PackageType.class), eq(1))).thenReturn(mockPackageType);

        PackageTypeBehavior mockPackageTypeBehavior = mock(PackageTypeBehavior.class);
        PowerMockito.mockStatic(ContentManagerHelper.class);
        when(ContentManagerHelper.getPackageTypeBehavior(1)).thenReturn(mockPackageTypeBehavior);

        when(mockEntityManager.createNamedQuery(eq(Architecture.QUERY_FIND_BY_NAME))).thenReturn(mockQuery);
        when(mockEntityManager.createNamedQuery(eq(Package.QUERY_FIND_BY_NAME_PKG_TYPE_ID))).thenReturn(mockQuery);

        ContentManagerLocal mockContentManager = mock(ContentManagerLocal.class);
        Package mockPackage = mock(Package.class);
        when(mockContentManager.persistPackage(isNotNull(Package.class))).thenReturn(mockPackage);
        
        when(mockEntityManager.find(eq(Package.class), any())).thenReturn(mockPackage);
        when(mockPackage.getPackageType()).thenReturn(mockPackageType);

        PackageVersion mockPackageVersion = mock(PackageVersion.class);
        when(mockContentManager.persistPackageVersion(isNotNull(PackageVersion.class)))
            .thenReturn(mockPackageVersion);

        when(mockEntityManager.find(eq(PackageVersion.class), anyInt())).thenReturn(mockPackageVersion);
        when(mockPackageVersion.getGeneralPackage()).thenReturn(mockPackage);
        when(mockPackageVersion.getArchitecture()).thenReturn(mockArchitecture);

        PackageBitsBlob mockPackageBitsBlob = mock(PackageBitsBlob.class);
        when(mockEntityManager.find(eq(PackageBitsBlob.class), anyInt())).thenReturn(mockPackageBitsBlob);

        PackageBits mockBits = mock(PackageBits.class);
        when(mockEntityManager.find(eq(PackageBits.class), anyInt())).thenReturn(mockBits);

        DataSource mockDataSource = mock(DataSource.class);
        Connection mockConnection = mock(Connection.class);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        PreparedStatement mockPreparedStatement1 = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement1);
        ResultSet mockResultSet = mock(ResultSet.class);
        when(mockPreparedStatement1.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        Blob mockBlob = mock(Blob.class);
        when(mockResultSet.getBlob(anyInt())).thenReturn(mockBlob);
        OutputStream mockOutputStream = mock(OutputStream.class);
        when(mockBlob.setBinaryStream(anyLong())).thenReturn(mockOutputStream);

        //create object to test and inject required dependencies
        ContentManagerBean objectUnderTest = new ContentManagerBean();
        Field[] fields = ContentManagerBean.class.getDeclaredFields();

        for (Field field : fields) {
            if (field.getName().equals("entityManager")) {
                field.setAccessible(true);
                field.set(objectUnderTest, mockEntityManager);
                field.setAccessible(false);
            }
            else if (field.getName().equals("contentManager")) {
                field.setAccessible(true);
                field.set(objectUnderTest, mockContentManager);
                field.setAccessible(false);
            }
            else if (field.getName().equals("dataSource")) {
                field.setAccessible(true);
                field.set(objectUnderTest, mockDataSource);
                field.setAccessible(false);
            }
        }

        //run the code to be tested
        PackageVersion result = objectUnderTest.createPackageVersionWithDisplayVersion(mockSubject, packageName,
            packageTypeID, version, displayVersion, architectureId, packageBitStream);

        //verify the results (Assert and Mock Verification)
        verify(mockList, times(2)).size();
        verify(mockList, never()).get(anyInt());

        verify(result, times(2)).setPackageBits(any(PackageBits.class));
        MessageDigestGenerator digest = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
        verify(result).setSHA256(digest.calcDigestString(sampleContent));
        verify(result).setFileSize((long) sampleContent.getBytes().length);
        verify(result).setDisplayVersion(displayVersion);
    }

    /*
     * Test createPackageVersionWithDisplayVersion with the following hypothesis:
     * 1) There is an existing package
     * 2) No package will be created because of (1)
     * 3) Focal point of the test is the first 10 lines of the method.
     * 4) No updates required to the packageVersion retrieved because input display version is null.
     */
    @Test
    @SuppressWarnings("rawtypes")
    public void testCreatePackageVersionExistingPackage() throws Exception {

        //set all the method call arguments
        Subject mockSubject = mock(Subject.class);
        String packageName = "PackageName";
        int packageTypeID = 1;
        String version = "PackageVersion";
        String displayVersion = null;
        int architectureId = 2;
        String sampleContent = "SampleContent";
        InputStream packageBitStream = new ByteArrayInputStream(sampleContent.getBytes());

        //tell the method story as it happens: mock dependencies and make them
        //behave in a way to get the method under test to completion.
        EntityManager mockEntityManager = mock(EntityManager.class);

        Query mockQuery = mock(Query.class);
        when(mockEntityManager.createNamedQuery(eq(PackageVersion.QUERY_FIND_BY_PACKAGE_VER_ARCH))).thenReturn(
            mockQuery);
        List mockList = mock(List.class);
        when(mockQuery.getResultList()).thenReturn(mockList);
        when(mockList.size()).thenReturn(1);
        PackageVersion mockPackageVersion = mock(PackageVersion.class);
        when(mockList.get(eq(0))).thenReturn(mockPackageVersion);

        //create object to test and inject required dependencies
        ContentManagerBean objectUnderTest = new ContentManagerBean();
        Field[] fields = ContentManagerBean.class.getDeclaredFields();

        for (Field field : fields) {
            if (field.getName().equals("entityManager")) {
                field.setAccessible(true);
                field.set(objectUnderTest, mockEntityManager);
                field.setAccessible(false);
            }
        }

        //run the code to be tested
        PackageVersion result = objectUnderTest.createPackageVersionWithDisplayVersion(mockSubject, packageName,
            packageTypeID, version, displayVersion, architectureId, packageBitStream);

        //verify the results (Assert and Mock Verification)
        verify(mockList, times(1)).size();
        verify(mockList, times(1)).get(eq(0));
        verify(mockEntityManager, times(1)).createNamedQuery(anyString());

        Assert.assertEquals(mockPackageVersion, result);
    }

    /*
     * Test createPackageVersionWithDisplayVersion with the following hypothesis:
     * 1) There is an existing package
     * 2) No package will be created because of (1)
     * 3) Focal point of the test is the first 10 lines of the method.
     * 4) No updates required to the packageVersion retrieved because input display version is empty after trimming.
     */
    @Test
    @SuppressWarnings("rawtypes")
    public void testCreatePackageVersionExistingPackageEmptyDisplayVersion() throws Exception {

        //set all the method call arguments
        Subject mockSubject = mock(Subject.class);
        String packageName = "PackageName";
        int packageTypeID = 1;
        String version = "PackageVersion";
        String displayVersion = "       ";
        int architectureId = 2;
        String sampleContent = "SampleContent";
        InputStream packageBitStream = new ByteArrayInputStream(sampleContent.getBytes());

        //tell the method story as it happens: mock dependencies and make them
        //behave in a way to get the method under test to completion.
        EntityManager mockEntityManager = mock(EntityManager.class);

        Query mockQuery = mock(Query.class);
        when(mockEntityManager.createNamedQuery(eq(PackageVersion.QUERY_FIND_BY_PACKAGE_VER_ARCH))).thenReturn(
            mockQuery);
        List mockList = mock(List.class);
        when(mockQuery.getResultList()).thenReturn(mockList);
        when(mockList.size()).thenReturn(1);
        PackageVersion mockPackageVersion = mock(PackageVersion.class);
        when(mockList.get(eq(0))).thenReturn(mockPackageVersion);

        //create object to test and inject required dependencies
        ContentManagerBean objectUnderTest = new ContentManagerBean();
        Field[] fields = ContentManagerBean.class.getDeclaredFields();

        for (Field field : fields) {
            if (field.getName().equals("entityManager")) {
                field.setAccessible(true);
                field.set(objectUnderTest, mockEntityManager);
                field.setAccessible(false);
            }
        }

        //run the code to be tested
        PackageVersion result = objectUnderTest.createPackageVersionWithDisplayVersion(mockSubject, packageName,
            packageTypeID, version, displayVersion, architectureId, packageBitStream);

        //verify the results (Assert and Mock Verification)
        verify(mockList, times(1)).size();
        verify(mockList, times(1)).get(eq(0));
        verify(mockEntityManager, times(1)).createNamedQuery(anyString());

        Assert.assertEquals(mockPackageVersion, result);
    }

    /*
     * Test createPackageVersionWithDisplayVersion with the following hypothesis:
     * 1) There are two existing package version objects stored in the database
     * 2) Only the first package version is ever touched by this method
     * 2) No package will be created because of (1)
     * 3) Focal point of the test is the first 10 lines of the method.
     * 4) No updates required to the packageVersion retrieved because input display version is empty after trimming.
     */
    @Test
    @SuppressWarnings("rawtypes")
    public void testCreatePackageVersionExistingPackagePickOnlyFirst() throws Exception {

        //set all the method call arguments
        Subject mockSubject = mock(Subject.class);
        String packageName = "PackageName";
        int packageTypeID = 1;
        String version = "PackageVersion";
        String displayVersion = "       ";
        int architectureId = 2;
        String sampleContent = "SampleContent";
        InputStream packageBitStream = new ByteArrayInputStream(sampleContent.getBytes());

        //tell the method story as it happens: mock dependencies and make them
        //behave in a way to get the method under test to completion.
        EntityManager mockEntityManager = mock(EntityManager.class);

        Query mockQuery = mock(Query.class);
        when(mockEntityManager.createNamedQuery(eq(PackageVersion.QUERY_FIND_BY_PACKAGE_VER_ARCH))).thenReturn(
            mockQuery);
        List mockList = mock(List.class);
        when(mockQuery.getResultList()).thenReturn(mockList);
        when(mockList.size()).thenReturn(2);
        PackageVersion mockPackageVersion = mock(PackageVersion.class);
        when(mockList.get(eq(0))).thenReturn(mockPackageVersion);

        //create object to test and inject required dependencies
        ContentManagerBean objectUnderTest = new ContentManagerBean();
        Field[] fields = ContentManagerBean.class.getDeclaredFields();

        for (Field field : fields) {
            if (field.getName().equals("entityManager")) {
                field.setAccessible(true);
                field.set(objectUnderTest, mockEntityManager);
                field.setAccessible(false);
            }
        }

        //run the code to be tested
        PackageVersion result = objectUnderTest.createPackageVersionWithDisplayVersion(mockSubject, packageName,
            packageTypeID, version, displayVersion, architectureId, packageBitStream);

        //verify the results (Assert and Mock Verification)
        verify(mockList, times(1)).size();
        verify(mockList, times(1)).get(eq(0));
        verify(mockList, never()).get(eq(1));

        verify(mockEntityManager, times(1)).createNamedQuery(anyString());

        Assert.assertEquals(mockPackageVersion, result);
    }

    /*
     * Test createPackageVersionWithDisplayVersion with the following hypothesis:
     * 1) There is an existing package
     * 2) No package will be created because of (1)
     * 3) Focal point of the test is the first 10 lines of the method.
     * 4) Updates required to the packageVersion because display version needs to be updated.
     */
    @Test
    @SuppressWarnings("rawtypes")
    public void testCreatePackageVersionExistingPackageUpdateDisplayVersion() throws Exception {

        //set all the method call arguments
        Subject mockSubject = mock(Subject.class);
        String packageName = "PackageName";
        int packageTypeID = 1;
        String version = "PackageVersion";
        String displayVersion = "PackageDisplayVersion";
        int architectureId = 2;
        String sampleContent = "SampleContent";
        InputStream packageBitStream = new ByteArrayInputStream(sampleContent.getBytes());

        //tell the method story as it happens: mock dependencies and make them
        //behave in a way to get the method under test to completion.
        EntityManager mockEntityManager = mock(EntityManager.class);

        Query mockQuery = mock(Query.class);
        when(mockEntityManager.createNamedQuery(eq(PackageVersion.QUERY_FIND_BY_PACKAGE_VER_ARCH))).thenReturn(
            mockQuery);
        List mockList = mock(List.class);
        when(mockQuery.getResultList()).thenReturn(mockList);
        when(mockList.size()).thenReturn(1);
        final PackageVersion mockPackageVersion = mock(PackageVersion.class);
        when(mockList.get(eq(0))).thenReturn(mockPackageVersion);

        when(mockPackageVersion.getId()).thenReturn(0);
        ContentManagerLocal mockContentManager = mock(ContentManagerLocal.class);
        when(mockContentManager.persistPackageVersion(any(PackageVersion.class))).thenAnswer(
            new Answer<PackageVersion>() {
                public PackageVersion answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    return (PackageVersion) args[0];
                }
            });

        when(mockEntityManager.find(eq(PackageVersion.class), eq(0))).thenAnswer(new Answer<PackageVersion>() {
            public PackageVersion answer(InvocationOnMock invocation) throws Throwable {
                return mockPackageVersion;
            }
        });

        Package mockPackage = mock(Package.class);
        when(mockPackageVersion.getGeneralPackage()).thenReturn(mockPackage);
        Architecture mockArchitecture = mock(Architecture.class);
        when(mockPackageVersion.getArchitecture()).thenReturn(mockArchitecture);
        when(mockPackageVersion.getExtraProperties()).thenReturn(null);

        //create object to test and inject required dependencies
        ContentManagerBean objectUnderTest = new ContentManagerBean();
        Field[] fields = ContentManagerBean.class.getDeclaredFields();

        for (Field field : fields) {
            if (field.getName().equals("entityManager")) {
                field.setAccessible(true);
                field.set(objectUnderTest, mockEntityManager);
                field.setAccessible(false);
            }
            else if (field.getName().equals("contentManager")) {
                field.setAccessible(true);
                field.set(objectUnderTest, mockContentManager);
                field.setAccessible(false);
            }
        }

        //run the code to be tested
        PackageVersion result = objectUnderTest.createPackageVersionWithDisplayVersion(mockSubject, packageName,
            packageTypeID, version, displayVersion, architectureId, packageBitStream);

        //verify the results (Assert and Mock Verification)
        verify(mockList, times(1)).size();
        verify(mockList, times(1)).get(eq(0));
        verify(mockEntityManager, times(1)).createNamedQuery(anyString());

        verify(mockPackageVersion, times(1)).setDisplayVersion(displayVersion);

        Assert.assertEquals(mockPackageVersion, result);
    }
}
