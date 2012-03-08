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
package org.rhq.core.pluginapi.content;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.content.PackageDetails;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;

public class FileContentDelegateTest {

    private File deploymentDirectory;
    private File dataDirectory;
    private String resourceUuid;

    @BeforeMethod
    public void initTest() throws Exception {
        deploymentDirectory = new File(this.getClass().getResource("/").getFile() + "deploymentDirectory");
        deleteRecursive(deploymentDirectory);

        dataDirectory = new File(this.getClass().getResource("/").getFile() + "dataDirectory");
        deleteRecursive(dataDirectory);

        resourceUuid = UUID.randomUUID().toString();
    }

    @AfterMethod
    public void cleanTest() throws Exception {
        deleteRecursive(deploymentDirectory);
        deleteRecursive(dataDirectory);
    }

    @Test
    public void testDeployExplodedWithManifestInArchive() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        File sampleWithManifestWar = new File(this.getClass().getResource("/sampleWithManifest.war").getFile());
        Assert.assertTrue(sampleWithManifestWar.exists());

        //create object to test and inject required dependencies
        FileContentDelegate objectUnderTest = new FileContentDelegate(deploymentDirectory, "");

        PackageDetails mockPackageDetails = mock(PackageDetails.class);
        PackageDetailsKey mockPackageDetailsKey = mock(PackageDetailsKey.class);
        when(mockPackageDetails.getKey()).thenReturn(mockPackageDetailsKey);
        when(mockPackageDetailsKey.getName()).thenReturn("deploymentFile");

        //run code under test
        objectUnderTest.createContent(mockPackageDetails, sampleWithManifestWar, true);
        String actualShaSaved = objectUnderTest.saveDeploymentSHA(sampleWithManifestWar, deploymentDirectory,
            resourceUuid, dataDirectory);
        String actualShaRetrieved = objectUnderTest.retrieveDeploymentSHA(deploymentDirectory, resourceUuid,
            dataDirectory);

        //verify the results (Assert and mock verification)
        File sha256File = new File(dataDirectory, resourceUuid + ".sha");
        Assert.assertTrue(sha256File.exists());
        Assert.assertNotEquals(sha256File.length(), 0, "Empty SHA256 file!!");

        InputStream propertiesFileInputStream = new FileInputStream(sha256File);
        Properties prop = new Properties();
        prop.load(propertiesFileInputStream);
        String storedSha256Attribute = prop.getProperty("RHQ-Sha256");
        propertiesFileInputStream.close();

        String expectedSHA256 = "89b33caa5bf4cfd235f060c396cb1a5acb2734a1366db325676f48c5f5ed92e5";
        Assert.assertEquals(storedSha256Attribute, expectedSHA256);
        Assert.assertEquals(actualShaRetrieved, expectedSHA256);
        Assert.assertEquals(actualShaSaved, expectedSHA256);
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
        FileContentDelegate objectUnderTest = new FileContentDelegate(deploymentDirectory, null);
        String actualShaReturned = objectUnderTest.retrieveDeploymentSHA(deploymentDirectory, resourceUuid,
            dataDirectory);

        //verify the results (Assert and mock verification)
        File sha256File = new File(dataDirectory, resourceUuid + ".sha");
        Assert.assertTrue(sha256File.exists());
        Assert.assertNotEquals(sha256File.length(), 0, "Empty SHA256 file!!");

        InputStream propertiesFileInputStream = new FileInputStream(sha256File);
        Properties prop = new Properties();
        prop.load(propertiesFileInputStream);
        String storedSha256Attribute = prop.getProperty("RHQ-Sha256");
        propertiesFileInputStream.close();

        Assert.assertEquals(storedSha256Attribute, "bff7f7d63ae8e4f1efebb54fa727effe1b1a8246492ad9c36779d79a9771fb2b");
        Assert.assertEquals(actualShaReturned, "bff7f7d63ae8e4f1efebb54fa727effe1b1a8246492ad9c36779d79a9771fb2b");
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
        FileContentDelegate objectUnderTest = new FileContentDelegate(deploymentDirectory, "");

        PackageDetails mockPackageDetails = mock(PackageDetails.class);
        PackageDetailsKey mockPackageDetailsKey = mock(PackageDetailsKey.class);
        when(mockPackageDetails.getKey()).thenReturn(mockPackageDetailsKey);
        when(mockPackageDetailsKey.getName()).thenReturn(sampleWithoutManifestWar.getName());

        //run code under test
        objectUnderTest.createContent(mockPackageDetails, sampleWithoutManifestWar, false);
        String actualShaReturned = objectUnderTest.retrieveDeploymentSHA(deploymentFile, resourceUuid, dataDirectory);

        //verify the results (Assert and mock verification)
        Assert.assertTrue(deploymentDirectory.exists(), "Deployment did not happen.");
        Assert.assertTrue(deploymentDirectory.isDirectory(), "Deployment directory is no longer a directory!!");
        Assert.assertFalse(deploymentFile.isDirectory(), "Deployment was exploded when it should not have been.");

        File sha256File = new File(dataDirectory, resourceUuid + ".sha");
        Assert.assertFalse(sha256File.exists(), "SHA256 properties files was wrongly created for zipped deployment.");

        MessageDigestGenerator digest = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
        String expectedSHA256 = digest.calcDigestString(sampleWithoutManifestWar);
        String actualSHA256OfDeployment = digest.calcDigestString(deploymentFile);

        Assert.assertEquals(actualSHA256OfDeployment, expectedSHA256);
        Assert.assertEquals(actualShaReturned, expectedSHA256);
    }

    @Test
    public void testGetShaZipped() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion.
        File sampleWithoutManifestWar = new File(this.getClass().getResource("/sampleWithoutManifest.war").getFile());
        Assert.assertTrue(sampleWithoutManifestWar.exists());

        //create object to test and inject required dependencies
        FileContentDelegate objectUnderTest = new FileContentDelegate(sampleWithoutManifestWar, null);

        //run code under test
        String actualShaReturned = objectUnderTest.retrieveDeploymentSHA(sampleWithoutManifestWar, resourceUuid,
            dataDirectory);

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