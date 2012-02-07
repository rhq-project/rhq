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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.jar.Manifest;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.domain.content.PackageDetails;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;
import org.rhq.plugins.jbossas.util.FileContentDelegate;

public class FileContentDelegateTest {

    @Test
    public void testDeployExplodedWithManifestInArchive() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        File deploymentDirectory = new File(this.getClass().getResource("/").getFile() + "deploymentDirectory");
        deleteRecursive(deploymentDirectory);

        File sampleWithManifestWar = new File(this.getClass().getResource("/sampleWithManifest.war").getFile());
        Assert.assertTrue(sampleWithManifestWar.exists());

        //create object to test and inject required dependencies
        FileContentDelegate objectUnderTest = new FileContentDelegate(deploymentDirectory, "", null);

        PackageDetails mockPackageDetails = mock(PackageDetails.class);
        PackageDetailsKey mockPackageDetailsKey = mock(PackageDetailsKey.class);
        when(mockPackageDetails.getKey()).thenReturn(mockPackageDetailsKey);
        when(mockPackageDetailsKey.getName()).thenReturn("deploymentFile");

        //run code under test
        objectUnderTest.createContent(mockPackageDetails, sampleWithManifestWar, true, false);
        String actualShaReturned = objectUnderTest.getSHA(new File(deploymentDirectory, "/deploymentFile"));

        //verify the results (Assert and mock verification)
        File manifestFile = new File(deploymentDirectory.getAbsolutePath() + "/deploymentFile/META-INF/MANIFEST.MF");
        Assert.assertTrue(manifestFile.exists(), "Manifest file not created properly!");
        Assert.assertNotEquals(manifestFile.length(), 0, "Empty manifest!!");

        InputStream manifestStream = new FileInputStream(manifestFile);
        Manifest manifest = new Manifest(manifestStream);
        String actualSha256Attribute = manifest.getMainAttributes().getValue("RHQ-Sha256");
        manifestStream.close();

        MessageDigestGenerator digest = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
        String expectedSHA256 = digest.calcDigestString(sampleWithManifestWar);

        Assert.assertEquals(actualSha256Attribute, expectedSHA256);
        Assert.assertEquals(actualShaReturned, expectedSHA256);

        //cleanup resources created for this test
        deleteRecursive(deploymentDirectory);
    }

    @Test
    public void testGetShaExplodedWithManifest() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        File deploymentDirectory = new File(this.getClass().getResource("/").getFile() + "deploymentDirectory");
        deleteRecursive(deploymentDirectory);

        File sampleWithManifestWar = new File(this.getClass().getResource("/sampleWithManifest.war").getFile());
        Assert.assertTrue(sampleWithManifestWar.exists());

        ZipUtil.unzipFile(sampleWithManifestWar, deploymentDirectory);

        //create object to test and inject required dependencies
        FileContentDelegate objectUnderTest = new FileContentDelegate(deploymentDirectory, null, null);

        //run code under test
        String actualShaReturned = objectUnderTest.getSHA(deploymentDirectory);

        //verify the results (Assert and mock verification)
        File manifestFile = new File(deploymentDirectory.getAbsolutePath() + "/META-INF/MANIFEST.MF");
        Assert.assertTrue(manifestFile.exists(), "Manifest file not created properly!");
        Assert.assertNotEquals(manifestFile.length(), 0, "Empty manifest!!");

        InputStream manifestStream = new FileInputStream(manifestFile);
        Manifest manifest = new Manifest(manifestStream);
        String actualSha256Attribute = manifest.getMainAttributes().getValue("RHQ-Sha256");
        manifestStream.close();

        Assert.assertEquals(actualSha256Attribute, "a30a576b5ee15c709469517e9f529989f9ae85bddde131c014a799ca9d7c1c0f");
        Assert.assertEquals(actualShaReturned, "a30a576b5ee15c709469517e9f529989f9ae85bddde131c014a799ca9d7c1c0f");

        deleteRecursive(deploymentDirectory);
    }

    @Test
    public void testDeployExplodedWithoutManifestInArchive() throws Exception {
        //tell the method story as it happens: mock dependencies and configure
        //those dependencies to get the method under test to completion.
        File deploymentDirectory = new File(this.getClass().getResource("/").getFile() + "deploymentDirectory");
        deleteRecursive(deploymentDirectory);

        File sampleWithoutManifestWar = new File(this.getClass().getResource("/sampleWithoutManifest.war").getFile());
        Assert.assertTrue(sampleWithoutManifestWar.exists());

        //create object to test and inject required dependencies
        FileContentDelegate objectUnderTest = new FileContentDelegate(deploymentDirectory, "", null);

        PackageDetails mockPackageDetails = mock(PackageDetails.class);
        PackageDetailsKey mockPackageDetailsKey = mock(PackageDetailsKey.class);
        when(mockPackageDetails.getKey()).thenReturn(mockPackageDetailsKey);
        when(mockPackageDetailsKey.getName()).thenReturn("deploymentFile");

        //run code under test
        objectUnderTest.createContent(mockPackageDetails, sampleWithoutManifestWar, true, false);
        String actualShaReturned = objectUnderTest.getSHA(new File(deploymentDirectory, "/deploymentFile"));

        //verify the results (Assert and mock verification)
        File manifestFile = new File(deploymentDirectory.getAbsolutePath() + "/deploymentFile/META-INF/MANIFEST.MF");
        Assert.assertTrue(manifestFile.exists(), "Manifest file not created properly!");
        Assert.assertNotEquals(manifestFile.length(), 0, "Empty manifest!!");

        InputStream manifestStream = new FileInputStream(manifestFile);
        Manifest manifest = new Manifest(manifestStream);
        String actualSha256Attribute = manifest.getMainAttributes().getValue("RHQ-Sha256");
        manifestStream.close();

        MessageDigestGenerator digest = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
        String expectedSHA256 = digest.calcDigestString(sampleWithoutManifestWar);

        Assert.assertEquals(actualSha256Attribute, expectedSHA256);
        Assert.assertEquals(actualShaReturned, expectedSHA256);

        deleteRecursive(deploymentDirectory);
    }

    @Test
    public void testGetShaExplodedWithoutManifest() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        File deploymentDirectory = new File(this.getClass().getResource("/").getFile() + "deploymentDirectory");
        deleteRecursive(deploymentDirectory);

        File sampleWithoutManifestWar = new File(this.getClass().getResource("/sampleWithoutManifest.war").getFile());
        Assert.assertTrue(sampleWithoutManifestWar.exists());

        //create object to test and inject required dependencies
        ZipUtil.unzipFile(sampleWithoutManifestWar, deploymentDirectory);

        //run code under test
        FileContentDelegate objectUnderTest = new FileContentDelegate(deploymentDirectory, null, null);
        String actualShaReturned = objectUnderTest.getSHA(deploymentDirectory);

        //verify the results (Assert and mock verification)
        File manifestFile = new File(deploymentDirectory.getAbsolutePath() + "/META-INF/MANIFEST.MF");
        Assert.assertTrue(manifestFile.exists(), "Manifest file not created properly!");
        Assert.assertNotEquals(manifestFile.length(), 0, "Empty manifest!!");

        InputStream manifestStream = new FileInputStream(manifestFile);
        Manifest manifest = new Manifest(manifestStream);
        String actualSha256Attribute = manifest.getMainAttributes().getValue("RHQ-Sha256");
        manifestStream.close();

        Assert.assertEquals(actualSha256Attribute, "bff7f7d63ae8e4f1efebb54fa727effe1b1a8246492ad9c36779d79a9771fb2b");
        Assert.assertEquals(actualShaReturned, "bff7f7d63ae8e4f1efebb54fa727effe1b1a8246492ad9c36779d79a9771fb2b");

        //cleanup resources created for this test
        deleteRecursive(deploymentDirectory);
    }

    @Test
    public void testDeployZipped() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        File deploymentDirectory = new File(this.getClass().getResource("/").getFile() + "deploymentDirectory");
        deleteRecursive(deploymentDirectory);
        deploymentDirectory.mkdirs();

        File sampleWithoutManifestWar = new File(this.getClass().getResource("/sampleWithoutManifest.war").getFile());
        Assert.assertTrue(sampleWithoutManifestWar.exists());

        File deploymentFile = new File(deploymentDirectory, sampleWithoutManifestWar.getName());

        //create object to test and inject required dependencies
        FileContentDelegate objectUnderTest = new FileContentDelegate(deploymentDirectory, "", null);

        PackageDetails mockPackageDetails = mock(PackageDetails.class);
        PackageDetailsKey mockPackageDetailsKey = mock(PackageDetailsKey.class);
        when(mockPackageDetails.getKey()).thenReturn(mockPackageDetailsKey);
        when(mockPackageDetailsKey.getName()).thenReturn(sampleWithoutManifestWar.getName());

        //run code under test
        objectUnderTest.createContent(mockPackageDetails, sampleWithoutManifestWar, false, false);
        String actualShaReturned = objectUnderTest.getSHA(sampleWithoutManifestWar);

        //verify the results (Assert and mock verification)
        Assert.assertTrue(deploymentDirectory.exists(), "Deployment did not happen.");
        Assert.assertTrue(deploymentDirectory.isDirectory(), "Deployment directory is no longer a directory!!");
        Assert.assertFalse(deploymentFile.isDirectory(), "Deployment was exploded when it should not have been.");

        MessageDigestGenerator digest = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
        String expectedSHA256 = digest.calcDigestString(sampleWithoutManifestWar);
        String actualSHA256OfDeployment = digest.calcDigestString(deploymentFile);

        Assert.assertEquals(actualSHA256OfDeployment, expectedSHA256);
        Assert.assertEquals(actualShaReturned, expectedSHA256);

        //cleanup resources created for this test
        deleteRecursive(deploymentFile);
    }

    @Test
    public void testGetShaZipped() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        File sampleWithoutManifestWar = new File(this.getClass().getResource("/sampleWithoutManifest.war").getFile());
        Assert.assertTrue(sampleWithoutManifestWar.exists());

        //create object to test and inject required dependencies
        FileContentDelegate objectUnderTest = new FileContentDelegate(sampleWithoutManifestWar, null, null);

        //run code under test
        String actualShaReturned = objectUnderTest.getSHA(sampleWithoutManifestWar);

        //verify the results (Assert and mock verification)
        MessageDigestGenerator digest = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
        String expectedSHA256 = digest.calcDigestString(sampleWithoutManifestWar);

        //cleanup resources created for this test
        Assert.assertEquals(actualShaReturned, expectedSHA256);
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