/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.util.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * Individually tests these situations:
 *
 * (X, Y, Z, ? represent hashcodes; none means file doesn't exist):
 *
 *    ORIGINAL CURRENT    NEW   What To Do...
 * a.        X       X      X   New file is installed over current*
 * b.        X       X      Y   New file is installed over current
 * c.        X       Y      X   Current file is left as-is
 * d.        X       Y      Y   New file is installed over current*
 * e.        X       Y      Z   New file is installed over current, current is backed up
 * f.     none       ?      ?   New file is installed over current, current is backed up
 * g.        X    none      ?   New file is installed
 * h.        ?       ?   none   Current file deleted, backed up if different than original
 *
 * (*) means the new and current files will actually be the same content
 *
 * @author John Mazzitelli
 */
@Test
public class SimpleDeployerTest {

    private final String originalContent = "original content";
    private final String originalFileName = "original_file_name.txt";
    private File tmpDir;
    private File deployDir;
    private File originalZipFile;
    private Map<File, File> originalZipFiles;
    private DeploymentProperties originalDeployProps;
    private FileHashcodeMap originalFileHashcodeMap;
    private String originalHashcode;
    private File currentFile;
    private DeploymentProperties newDeployProps;
    private DeployDifferences diff;
    private DeploymentsMetadata metadata;

    @BeforeMethod
    public void beforeMethod() throws Exception {
        this.tmpDir = FileUtil.createTempDirectory("simpleDeployer_TMP", ".test", null);
        this.deployDir = FileUtil.createTempDirectory("simpleDeployer", ".test", null);
        this.originalHashcode = MessageDigestGenerator.getDigestString(originalContent);
        this.originalZipFile = createZip(originalContent, tmpDir, "original.zip", originalFileName);
        this.originalZipFiles = new HashMap<File, File>(1);
        this.originalZipFiles.put(originalZipFile, null);
        this.originalDeployProps = new DeploymentProperties(1, "simple", "1.0", "original test deployment",
            DestinationComplianceMode.full);
        DeploymentData dd = new DeploymentData(originalDeployProps, tmpDir, deployDir, null, null, originalZipFiles,
            null, null, null, null);
        Deployer deployer = new Deployer(dd);
        this.originalFileHashcodeMap = deployer.deploy(null);
        this.currentFile = new File(deployDir, originalFileName);

        this.newDeployProps = new DeploymentProperties(2, "simple", "2.0", "new test deployment",
            DestinationComplianceMode.full);
        this.diff = new DeployDifferences();
        this.metadata = new DeploymentsMetadata(this.deployDir);

        // sanity check due to my paranoia
        assert this.currentFile.exists();
        assert this.originalHashcode.equals(MessageDigestGenerator.getDigestString(currentFile));
        assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
        assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() throws Exception {
        FileUtil.purge(this.tmpDir, true);
        FileUtil.purge(this.deployDir, true);
    }

    public void testX_X_X() throws Exception {
        baseX_X_X(false);
    }

    public void testX_X_Y() throws Exception {
        baseX_X_Y(false);
    }

    public void testX_Y_X() throws Exception {
        baseX_Y_X(false, false);
    }

    public void testX_Y_X_Clean() throws Exception {
        baseX_Y_X(false, true);
    }

    public void testX_Y_Y() throws Exception {
        baseX_Y_Y(false);
    }

    public void testX_Y_Z() throws Exception {
        baseX_Y_Z(false);
    }

    public void testX_Y_Z_Restore() throws Exception {
        baseX_Y_Z_Restore(false);
    }

    public void testX_Y_Z_Clean() throws Exception {
        baseX_Y_Z_Clean(false);
    }

    public void testNoOriginalNoCurrentWithNew() throws Exception {
        baseNoOriginalNoCurrentWithNew(false);
    }

    public void testNoOriginalWithCurrentWithNew() throws Exception {
        baseNoOriginalWithCurrentWithNew(false);
    }

    public void testNoOriginalWithCurrentNoNew() throws Exception {
        baseNoOriginalWithCurrentNoNew(false, false);
    }

    public void testNoOriginalWithCurrentNoNew_Clean() throws Exception {
        baseNoOriginalWithCurrentNoNew(false, true);
    }

    public void testNoCurrent() throws Exception {
        baseNoCurrent(false);
    }

    public void testNoNew() throws Exception {
        baseNoNew(false);
    }

    public void testNoNewWithCurrentDifferentThanOriginal() throws Exception {
        baseNoNewWithCurrentDifferentThanOriginal(false);
    }

    public void testX_X_X_DryRun() throws Exception {
        baseX_X_X(true);
    }

    public void testX_X_Y_DryRun() throws Exception {
        baseX_X_Y(true);
    }

    public void testX_Y_X_DryRun() throws Exception {
        baseX_Y_X(true, false);
    }

    public void testX_Y_X_DryRun_Clean() throws Exception {
        baseX_Y_X(true, true);
    }

    public void testX_Y_Y_DryRun() throws Exception {
        baseX_Y_Y(true);
    }

    public void testX_Y_Z_DryRun() throws Exception {
        baseX_Y_Z(true);
    }

    public void testX_Y_Z_Restore_DryRun() throws Exception {
        baseX_Y_Z_Restore(true);
    }

    public void testX_Y_Z_Clean_DryRun() throws Exception {
        baseX_Y_Z_Clean(true);
    }

    public void testNoOriginalNoCurrentWithNew_DryRun() throws Exception {
        baseNoOriginalNoCurrentWithNew(true);
    }

    public void testNoOriginalWithCurrentWithNew_DryRun() throws Exception {
        baseNoOriginalWithCurrentWithNew(true);
    }

    public void testNoOriginalWithCurrentNoNew_DryRun() throws Exception {
        baseNoOriginalWithCurrentNoNew(true, false);
    }

    public void testNoOriginalWithCurrentNoNew_DryRun_Clean() throws Exception {
        baseNoOriginalWithCurrentNoNew(true, true);
    }

    public void testNoCurrent_DryRun() throws Exception {
        baseNoCurrent(true);
    }

    public void testNoNew_DryRun() throws Exception {
        baseNoNew(true);
    }

    public void testNoNewWithCurrentDifferentThanOriginal_DryRun() throws Exception {
        baseNoNewWithCurrentDifferentThanOriginal(true);
    }

    public void testWithSubdirectoriesComplianceFull() throws Exception {
        testWithSubdirectories(DestinationComplianceMode.full);
    }

    public void testWithSubdirectoriesComplianceFilesAndDirs() throws Exception {
        testWithSubdirectories(DestinationComplianceMode.filesAndDirectories);
    }

    private void testWithSubdirectories(DestinationComplianceMode complianceMode) throws Exception {
        // this test is different than all the rest, start with clean tmp/dest dirs with no beforeMethod buildup
        FileUtil.purge(this.tmpDir, false);
        FileUtil.purge(this.deployDir, false);

        // fill the deployDir with some unrelated content
        String unrelatedFileName1 = "unrelated.txt";
        String unrelatedFileName2 = "unrelateddir/unrelated.txt";
        String unrelatedEmptyDir = "unrelateddir2";
        String inTheSubdirFileName1 = "subdir/extra.txt";
        File unrelated1 = writeFile("unrelated1", this.deployDir, unrelatedFileName1);
        File unrelated2 = writeFile("unrelated2", this.deployDir, unrelatedFileName2);
        File inTheSubdir1 = writeFile("inTheSubdir1", this.deployDir, inTheSubdirFileName1);
        File unrelatedEmpty = new File(this.deployDir, unrelatedEmptyDir);
        unrelatedEmpty.mkdir();

        assert unrelated1.exists();
        assert unrelated2.exists();
        assert inTheSubdir1.exists();
        assert unrelatedEmpty.exists();

        // deploy initial content
        String origFileName1 = "original-file1.txt";
        String origDirName = "subdir";
        String origFileName2 = origDirName + "/original-file2.txt";
        this.originalZipFile = createZip(new String[] { "content1", "content2" }, this.tmpDir, "original.zip",
            new String[] { origFileName1, origFileName2 });
        this.originalZipFiles = new HashMap<File, File>(1);
        this.originalZipFiles.put(originalZipFile, null);
        this.originalDeployProps = new DeploymentProperties(1, "simple", "1.0", "original test deployment",
            complianceMode);
        DeploymentData dd = new DeploymentData(originalDeployProps, tmpDir, deployDir, null, null, originalZipFiles,
            null, null, null, null);
        this.diff = new DeployDifferences();
        Deployer deployer = new Deployer(dd);
        this.originalFileHashcodeMap = deployer.deploy(this.diff);
        assert new File(this.deployDir, origFileName1).exists();
        assert new File(this.deployDir, origFileName2).exists();
        if (DestinationComplianceMode.full.equals(complianceMode)) {
            assert !unrelated1.exists() : "the deployment should have removed unrelated file1";
            assert !unrelated2.getParentFile().isDirectory() : "the deployment should have removed an unrelated dir";
            assert !unrelated2.exists() : "the deployment should have removed unrelated file2";
            assert !inTheSubdir1.exists() : "the deployment should have removed the in-the-subdir file in the subdir";
            assert !unrelatedEmpty.exists() : "the deplooyment should have removed unrelated empty dir";

            assert this.diff.getBackedUpFiles().size() == 3 : this.diff;
            assert new File(this.diff.getBackedUpFiles().get(unrelatedFileName1)).exists() : this.diff;
            assert new File(this.diff.getBackedUpFiles().get(unrelatedFileName2)).exists() : this.diff;
            assert new File(this.diff.getBackedUpFiles().get(inTheSubdirFileName1)).exists() : this.diff;
            assert this.diff.getDeletedFiles().size() == 3 : this.diff;
            assert this.diff.getDeletedFiles().contains(unrelatedFileName1) : this.diff;
            assert this.diff.getDeletedFiles().contains(unrelatedFileName2) : this.diff;
            assert this.diff.getDeletedFiles().contains(inTheSubdirFileName1) : this.diff;
        } else {
            assert unrelated1.exists() : "the deployment removed unrelated file1";
            assert unrelated2.getParentFile().isDirectory() : "the deployment removed an unrelated dir";
            assert unrelated2.exists() : "the deployment removed unrelated file2";
            assert !inTheSubdir1.exists() : "the deployment should have removed the in-the-subdir file in the subdir";

            assert this.diff.getBackedUpFiles().size() == 1 : this.diff;
            assert new File(this.diff.getBackedUpFiles().get(inTheSubdirFileName1)).exists() : this.diff;
            assert this.diff.getDeletedFiles().size() == 1 : this.diff;
            assert this.diff.getDeletedFiles().contains(inTheSubdirFileName1) : this.diff;
        }

        assert this.diff.getAddedFiles().size() == 2 : this.diff;
        assert this.diff.getAddedFiles().contains(origFileName1) : this.diff;
        assert this.diff.getAddedFiles().contains(origFileName2) : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getRestoredFiles().isEmpty() : this.diff;
        assert !this.diff.wasCleaned() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        unrelatedEmpty.mkdir();
        // deploy new content
        this.newDeployProps = new DeploymentProperties(2, "simple", "2.0", "new test deployment", complianceMode);
        this.diff = new DeployDifferences();
        this.metadata = new DeploymentsMetadata(this.deployDir);
        String newFileName1 = "new-file1.txt";
        String newDirName = "newsubdir";
        String newFileName2 = newDirName + "/new-file2.txt";
        File newZipFile = createZip(new String[] { "newcontent1", "newcontent2" }, this.tmpDir, "new.zip",
            new String[] { newFileName1, newFileName2 });
        HashMap<File, File> newZipFiles = new HashMap<File, File>(1);
        newZipFiles.put(newZipFile, null);
        dd = new DeploymentData(newDeployProps, tmpDir, deployDir, null, null, newZipFiles, null, null, null, null);
        deployer = new Deployer(dd);
        FileHashcodeMap newFileHashcodeMap = deployer.deploy(this.diff);
        assert newFileHashcodeMap != null;
        assert new File(this.deployDir, newFileName1).exists();
        assert new File(this.deployDir, newFileName2).exists();
        assert !new File(this.deployDir, origFileName1).exists();
        assert !new File(this.deployDir, origFileName2).exists();
        if (DestinationComplianceMode.full.equals(complianceMode)) {
            assert !unrelated1.exists() : "the deployment did not remove unrelated file1";
            assert !unrelated2.exists() : "the deployment did not remove unrelated file1";
            assert !unrelatedEmpty.exists() : "the deployment did not remove unrelated file2";
        } else {
            assert unrelated1.exists() : "the deployment removed unrelated file1 but we aren't managing the root dir";
            assert unrelated2.exists() : "the deployment removed unrelated file1 but we aren't managing the root dir";
            assert unrelatedEmpty.exists() : "the deployment removed unrelated file2 but we aren't managing the root dir";
        }

        assert this.diff.getAddedFiles().size() == 3 : this.diff;
        assert this.diff.getAddedFiles().contains(newFileName1) : this.diff;
        assert this.diff.getAddedFiles().contains(newDirName) : this.diff;
        assert this.diff.getAddedFiles().contains(newFileName2) : this.diff;
        if (DestinationComplianceMode.full.equals(complianceMode)) {
            assert this.diff.getDeletedFiles().size() == 4 : this.diff;
            assert this.diff.getDeletedFiles().contains(unrelatedEmptyDir) : this.diff;
            assert this.diff.getBackedUpFiles().containsKey(unrelatedEmptyDir) : this.diff;
        } else {
            assert this.diff.getDeletedFiles().size() == 3 : this.diff;
            assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        }
        assert this.diff.getDeletedFiles().contains(origFileName1) : this.diff;
        assert this.diff.getDeletedFiles().contains(origDirName) : this.diff;
        assert this.diff.getDeletedFiles().contains(origFileName2) : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getRestoredFiles().isEmpty() : this.diff;
        assert !this.diff.wasCleaned() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;
    }

    private void baseX_X_X(boolean dryRun) throws Exception {
        newDeployProps.setDestinationCompliance(DestinationComplianceMode.full);
        DeploymentData dd = new DeploymentData(newDeployProps, tmpDir, deployDir, null, null, originalZipFiles, null,
            null, null, null);
        Deployer deployer = new Deployer(dd);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // nothing changed!

        assert newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(this.originalContent);
        assert contentHash[1].equals(this.originalHashcode);

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getRestoredFiles().isEmpty() : this.diff;
        assert !this.diff.wasCleaned() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }
    }

    private void baseX_X_Y(boolean dryRun) throws Exception {
        String newContent = "testX_X_Y";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        File newZipFile = createZip(newContent, tmpDir, "new-content.zip", originalFileName);
        Map<File, File> newZipFiles = new HashMap<File, File>(1);
        newZipFiles.put(newZipFile, null);

        newDeployProps.setDestinationCompliance(DestinationComplianceMode.full);
        DeploymentData dd = new DeploymentData(newDeployProps, tmpDir, deployDir, null, null, newZipFiles, null, null,
            null, null);
        Deployer deployer = new Deployer(dd);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // The new file changed the original file. The current file was never touched, so this is a simple upgrade

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(originalFileName).equals(newHashcode);
        String[] contentHash = getOriginalFilenameContentHashcode();
        if (dryRun) {
            assert contentHash[0].equals(originalContent);
            assert contentHash[1].equals(originalHashcode);
        } else {
            assert contentHash[0].equals(newContent);
            assert contentHash[1].equals(newHashcode);
        }

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().size() == 1 : this.diff;
        assert this.diff.getChangedFiles().contains(originalFileName) : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }
    }

    private void baseX_Y_X(boolean dryRun, boolean clean) throws Exception {
        String newContent = "testX_Y_X";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        writeFile(newContent, this.currentFile);

        newDeployProps.setDestinationCompliance(DestinationComplianceMode.full);
        DeploymentData dd = new DeploymentData(newDeployProps, tmpDir, deployDir, null, null, originalZipFiles, null,
            null, null, null);
        Deployer deployer = new Deployer(dd);
        FileHashcodeMap newFileHashcodeMap;
        newFileHashcodeMap = deployer.deploy(this.diff, clean, dryRun);

        // very important to understand this - even though the current file is changed, the hashcode
        // stored in the map and the metadata directory is the ORIGINAL hashcode. This is to make it
        // known that the new deployment itself is the same as the original deployment. It is just
        // that we allow the user's manual changes to continue to live on in the filesystem. However,
        // if a newer deployment comes along in the future and changes the new file, this current file
        // must be updated/backed up as appropriate and the only way to know when that happens is if
        // the metadata retains the original/new hashcode and not the current one.

        assert newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        String[] contentHash = getOriginalFilenameContentHashcode();

        // if we are cleaning, then the old content is blown away anyway and the original is replaced
        // (but not if this is a dryRun - dryRun always means the changed/new content remains)
        if (clean && !dryRun) {
            assert contentHash[0].equals(originalContent);
            assert contentHash[1].equals(originalHashcode);
        } else {
            assert contentHash[0].equals(newContent);
            assert contentHash[1].equals(newHashcode);
        }

        // note nothing changed - our current file remains as is
        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        if (clean) {
            assert this.diff.getBackedUpFiles().size() == 1 : this.diff;
            assert this.diff.getBackedUpFiles().containsKey(originalFileName) : this.diff;
            File backupFile = new File(this.diff.getBackedUpFiles().get(originalFileName));
            if (dryRun) {
                assert !backupFile.exists() : "dry run should not create backup";
            } else {
                assert readFile(backupFile).equals(newContent) : "did not backup the correct file?";
            }
        } else {
            assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        }
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;
        assert this.diff.wasCleaned() == clean : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }
    }

    private void baseX_Y_Y(boolean dryRun) throws Exception {
        String newContent = "testX_Y_Y";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        writeFile(newContent, this.currentFile);
        File newZipFile = createZip(newContent, tmpDir, "new-content.zip", originalFileName);
        Map<File, File> newZipFiles = new HashMap<File, File>(1);
        newZipFiles.put(newZipFile, null);

        newDeployProps.setDestinationCompliance(DestinationComplianceMode.full);
        DeploymentData dd = new DeploymentData(newDeployProps, tmpDir, deployDir, null, null, newZipFiles, null, null,
            null, null);
        Deployer deployer = new Deployer(dd);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // The new file changed the original, but our current file has already been manually updated
        // to match the new file. Therefore, the current file doesn't have to change its content.
        // Technically, the file could be overwritten, but the content will still be the same.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(originalFileName).equals(newHashcode);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(newContent);
        assert contentHash[1].equals(newHashcode);

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }
    }

    private void baseX_Y_Z(boolean dryRun) throws Exception {
        String newContentY = "testX_Y_Z_YYY";
        writeFile(newContentY, this.currentFile);
        String newHashcodeY = MessageDigestGenerator.getDigestString(newContentY);

        String newContentZ = "testX_Y_Z_ZZZ";
        String newHashcodeZ = MessageDigestGenerator.getDigestString(newContentZ);
        File newZipFile = createZip(newContentZ, tmpDir, "new-content.zip", originalFileName);
        Map<File, File> newZipFiles = new HashMap<File, File>(1);
        newZipFiles.put(newZipFile, null);

        newDeployProps.setDestinationCompliance(DestinationComplianceMode.full);
        DeploymentData dd = new DeploymentData(newDeployProps, tmpDir, deployDir, null, null, newZipFiles, null, null,
            null, null);
        Deployer deployer = new Deployer(dd);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // The new file changed the original, and our current file has been manually updated
        // but that current file's change does not match the new file. Therefore, the current file
        // is out of date. The safest thing to do is backup the current and copy the new file
        // to become the current file.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(originalFileName).equals(newHashcodeZ);
        String[] contentHash = getOriginalFilenameContentHashcode();
        if (dryRun) {
            assert contentHash[0].equals(newContentY);
            assert contentHash[1].equals(newHashcodeY);
        } else {
            assert contentHash[0].equals(newContentZ);
            assert contentHash[1].equals(newHashcodeZ);
        }

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().size() == 1 : this.diff;
        assert this.diff.getChangedFiles().contains(originalFileName) : this.diff;
        assert this.diff.getBackedUpFiles().size() == 1 : this.diff;
        assert this.diff.getBackedUpFiles().containsKey(originalFileName) : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }

        // verify the backup copy
        File backupFile = new File(this.diff.getBackedUpFiles().get(originalFileName));
        if (dryRun) {
            assert !backupFile.exists() : "dry run should not create backup";
        } else {
            assert readFile(backupFile).equals(newContentY) : "did not backup the correct file?";
        }
    }

    private void baseNoOriginalNoCurrentWithNew(boolean dryRun) throws Exception {
        String newContent = "new content";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        String newFileName = "new_filename.new";
        File newZipFile = createZip(new String[] { originalContent, newContent }, tmpDir, "new.zip", new String[] {
            originalFileName, newFileName });
        Map<File, File> newZipFiles = new HashMap<File, File>(1);
        newZipFiles.put(newZipFile, null);

        newDeployProps.setDestinationCompliance(DestinationComplianceMode.full);
        DeploymentData dd = new DeploymentData(newDeployProps, tmpDir, deployDir, null, null, newZipFiles, null, null,
            null, null);
        Deployer deployer = new Deployer(dd);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // the new deployment introduces a new file. This is simple - its just added to the filesystem

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 2;
        assert newFileHashcodeMap.get(originalFileName).equals(originalHashcode);
        assert newFileHashcodeMap.get(newFileName).equals(newHashcode);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(originalContent);
        assert contentHash[1].equals(originalHashcode);
        try {
            contentHash = getFilenameContentHashcode(newFileName);
            assert contentHash[0].equals(newContent);
            assert contentHash[1].equals(newHashcode);
        } catch (FileNotFoundException e) {
            // this is expected if we are in a dry run
            if (!dryRun) {
                throw e;
            }
        }

        assert this.diff.getAddedFiles().size() == 1 : this.diff;
        assert this.diff.getAddedFiles().contains(newFileName) : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }
    }

    private void baseNoOriginalWithCurrentWithNew(boolean dryRun) throws Exception {
        String newContent = "new content";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        String newFileName = "new_filename.new";
        File newZipFile = createZip(new String[] { originalContent, newContent }, tmpDir, "new.zip", new String[] {
            originalFileName, newFileName });
        Map<File, File> newZipFiles = new HashMap<File, File>(1);
        newZipFiles.put(newZipFile, null);

        File inTheWayFile = new File(this.deployDir, newFileName);
        String inTheWayContent = "this is in the way";
        String inTheWayHashcode = MessageDigestGenerator.getDigestString(inTheWayContent);
        writeFile(inTheWayContent, inTheWayFile);

        newDeployProps.setDestinationCompliance(DestinationComplianceMode.full);
        DeploymentData dd = new DeploymentData(newDeployProps, tmpDir, deployDir, null, null, newZipFiles, null, null,
            null, null);
        Deployer deployer = new Deployer(dd);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // The new deployment introduces a new file. However, there is already a current file at the new file location.
        // That current file is unknown and in the way - it must be backed up and overwritten.
        // This is considered a "change" not an "addition" since the file system already had the file, it just got changed.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 2;
        assert newFileHashcodeMap.get(originalFileName).equals(originalHashcode);
        assert newFileHashcodeMap.get(newFileName).equals(newHashcode);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(originalContent);
        assert contentHash[1].equals(originalHashcode);
        contentHash = getFilenameContentHashcode(newFileName);
        if (dryRun) {
            assert contentHash[0].equals(inTheWayContent);
            assert contentHash[1].equals(inTheWayHashcode);
        } else {
            assert contentHash[0].equals(newContent);
            assert contentHash[1].equals(newHashcode);
        }

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().size() == 1 : this.diff;
        assert this.diff.getChangedFiles().contains(newFileName) : this.diff;
        assert this.diff.getBackedUpFiles().size() == 1 : this.diff;
        assert this.diff.getBackedUpFiles().containsKey(newFileName) : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }

        // verify the backup copy
        File backupFile = new File(this.diff.getBackedUpFiles().get(newFileName));
        if (dryRun) {
            assert !backupFile.exists() : "dry run should not create backup";
        } else {
            assert readFile(backupFile).equals(inTheWayContent) : "did not backup the correct file?";
        }
    }

    /**
     * This tests when there was no file in the bundle but some unknown file was added to the
     * destination directory. When redeploying the same bundle, there is no original file,
     * there is no new file, but there is a current file (which is unknown to the bundle).
     * It should be removed and backed up.
     */
    private void baseNoOriginalWithCurrentNoNew(boolean dryRun, boolean clean) throws Exception {
        String inTheWayFileName = "unknown.txt";
        File inTheWayFile = new File(this.deployDir, inTheWayFileName);
        String inTheWayContent = "this is a new file but shouldn't be here - its not part of the bundle";
        writeFile(inTheWayContent, inTheWayFile);

        newDeployProps.setDestinationCompliance(DestinationComplianceMode.full);
        DeploymentData dd = new DeploymentData(originalDeployProps, tmpDir, deployDir, null, null, originalZipFiles,
            null, null, null, null);
        Deployer deployer = new Deployer(dd);
        FileHashcodeMap newFileHashcodeMap;
        newFileHashcodeMap = deployer.deploy(this.diff, clean, dryRun);

        assert newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(originalFileName).equals(originalHashcode);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(originalContent);
        assert contentHash[1].equals(originalHashcode);

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().size() == 1 : this.diff;
        assert this.diff.getDeletedFiles().contains(inTheWayFileName) : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().size() == 1 : this.diff;
        assert this.diff.getBackedUpFiles().containsKey(inTheWayFileName) : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;
        assert this.diff.wasCleaned() == clean : this.diff;

        assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
        assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);

        // verify the backup copy
        File backupFile = new File(this.diff.getBackedUpFiles().get(inTheWayFileName));
        if (dryRun) {
            assert !backupFile.exists() : "dry run should not create backup";
        } else {
            assert readFile(backupFile).equals(inTheWayContent) : "did not backup the correct file?";
        }
    }

    private void baseNoCurrent(boolean dryRun) throws Exception {
        assert this.currentFile.delete() : "Failed to delete the current file, cannot prepare the test";

        newDeployProps.setDestinationCompliance(DestinationComplianceMode.full);
        DeploymentData dd = new DeploymentData(newDeployProps, tmpDir, deployDir, null, null, originalZipFiles, null,
            null, null, null);
        Deployer deployer = new Deployer(dd);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // its the same deployment as before, except someone deleted our current file.
        // This adds the new file back (which is the same as the original).

        assert newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        try {
            String[] contentHash = getOriginalFilenameContentHashcode();
            assert contentHash[0].equals(this.originalContent);
            assert contentHash[1].equals(this.originalHashcode);
        } catch (FileNotFoundException e) {
            // this is expected if we are in a dry run
            if (!dryRun) {
                throw e;
            }
        }

        assert this.diff.getAddedFiles().size() == 1 : this.diff;
        assert this.diff.getAddedFiles().contains(this.originalFileName) : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }
    }

    private void baseNoNew(boolean dryRun) throws Exception {
        String newContent = "new content";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        String newFileName = "new_filename.new";
        File newZipFile = createZip(newContent, tmpDir, "new.zip", newFileName);
        Map<File, File> newZipFiles = new HashMap<File, File>(1);
        newZipFiles.put(newZipFile, null);

        newDeployProps.setDestinationCompliance(DestinationComplianceMode.full);
        DeploymentData dd = new DeploymentData(newDeployProps, tmpDir, deployDir, null, null, newZipFiles, null, null,
            null, null);
        Deployer deployer = new Deployer(dd);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // The new deployment removes a file that was in the original (it also introduces a new file).
        // There is already a current file at the original file location that is the same as the original, as you would expect.
        // That current file is to be deleted (since its not in the new deployment) and is not backed up since it is
        // the same as the original.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(newFileName).equals(newHashcode);
        if (dryRun) {
            assert this.currentFile.exists() : "this should have been left as-is";
        } else {
            assert !this.currentFile.exists() : "this should have been deleted";
        }
        try {
            String[] contentHash = getFilenameContentHashcode(newFileName);
            assert contentHash[0].equals(newContent);
            assert contentHash[1].equals(newHashcode);
        } catch (FileNotFoundException e) {
            // this is expected if we are in a dry run
            if (!dryRun) {
                throw e;
            }
        }

        assert this.diff.getAddedFiles().size() == 1 : this.diff;
        assert this.diff.getAddedFiles().contains(newFileName) : this.diff;
        assert this.diff.getDeletedFiles().size() == 1 : this.diff;
        assert this.diff.getDeletedFiles().contains(originalFileName) : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }
    }

    private void baseNoNewWithCurrentDifferentThanOriginal(boolean dryRun) throws Exception {
        String currentContent = "modified content";
        writeFile(currentContent, this.currentFile);

        String newContent = "new content";
        String newHashcode = MessageDigestGenerator.getDigestString(newContent);
        String newFileName = "new_filename.new";
        File newZipFile = createZip(newContent, tmpDir, "new.zip", newFileName);
        Map<File, File> newZipFiles = new HashMap<File, File>(1);
        newZipFiles.put(newZipFile, null);

        newDeployProps.setDestinationCompliance(DestinationComplianceMode.full);
        DeploymentData dd = new DeploymentData(newDeployProps, tmpDir, deployDir, null, null, newZipFiles, null, null,
            null, null);
        Deployer deployer = new Deployer(dd);
        FileHashcodeMap newFileHashcodeMap;
        if (dryRun) {
            newFileHashcodeMap = deployer.dryRun(this.diff);
        } else {
            newFileHashcodeMap = deployer.deploy(this.diff);
        }

        // The new deployment removes a file that was in the original (it also introduces a new file).
        // However, there is already a current file at the original file location as you would expect but
        // its different than the original.
        // That current file is to be deleted (since its not in the new deployment) and it must be backed up
        // since it looks modified from the original.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(newFileName).equals(newHashcode);
        if (dryRun) {
            assert this.currentFile.exists() : "this should have been left as-is";
        } else {
            assert !this.currentFile.exists() : "this should have been deleted";
        }
        try {
            String[] contentHash = getFilenameContentHashcode(newFileName);
            assert contentHash[0].equals(newContent);
            assert contentHash[1].equals(newHashcode);
        } catch (FileNotFoundException e) {
            // this is expected if we are in a dry run
            if (!dryRun) {
                throw e;
            }
        }

        assert this.diff.getAddedFiles().size() == 1 : this.diff;
        assert this.diff.getAddedFiles().contains(newFileName) : this.diff;
        assert this.diff.getDeletedFiles().size() == 1 : this.diff;
        assert this.diff.getDeletedFiles().contains(originalFileName) : this.diff;
        assert this.diff.getChangedFiles().isEmpty() : this.diff;
        assert this.diff.getBackedUpFiles().size() == 1 : this.diff;
        assert this.diff.getBackedUpFiles().containsKey(originalFileName) : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }

        // verify the backup copy
        File backupFile = new File(this.diff.getBackedUpFiles().get(originalFileName));
        if (dryRun) {
            assert !backupFile.exists() : "dry run should not create backup";
        } else {
            assert readFile(backupFile).equals(currentContent) : "did not backup the correct file?";
        }
    }

    private void baseX_Y_Z_Restore(boolean dryRun) throws Exception {
        String newContentY = "testX_Y_Z_YYY";
        writeFile(newContentY, this.currentFile);
        String newHashcodeY = MessageDigestGenerator.getDigestString(newContentY);

        String newContentZ = "testX_Y_Z_ZZZ";
        String newHashcodeZ = MessageDigestGenerator.getDigestString(newContentZ);
        File newZipFile = createZip(newContentZ, tmpDir, "new-content.zip", originalFileName);
        Map<File, File> newZipFiles = new HashMap<File, File>(1);
        newZipFiles.put(newZipFile, null);

        newDeployProps.setDestinationCompliance(DestinationComplianceMode.full);
        DeploymentData dd = new DeploymentData(newDeployProps, tmpDir, deployDir, null, null, newZipFiles, null, null,
            null, null);
        Deployer deployer = new Deployer(dd);
        FileHashcodeMap newFileHashcodeMap;
        newFileHashcodeMap = deployer.deploy(this.diff); // no dry run - we need to do this to force backup file creation

        // The new file changed the original, and our current file has been manually updated
        // but that current file's change does not match the new file. Therefore, the current file
        // is out of date. The safest thing to do is backup the current and copy the new file
        // to become the current file.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(originalFileName).equals(newHashcodeZ);
        String[] contentHash = getOriginalFilenameContentHashcode();
        assert contentHash[0].equals(newContentZ);
        assert contentHash[1].equals(newHashcodeZ);

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().size() == 1 : this.diff;
        assert this.diff.getChangedFiles().contains(originalFileName) : this.diff;
        assert this.diff.getBackedUpFiles().size() == 1 : this.diff;
        assert this.diff.getBackedUpFiles().containsKey(originalFileName) : this.diff;
        assert this.diff.getRestoredFiles().isEmpty() : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
        assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);

        // verify the backup copy
        File backupFile = new File(this.diff.getBackedUpFiles().get(originalFileName));
        assert readFile(backupFile).equals(newContentY) : "did not backup the correct file?";

        // all we did so far was upgrade to v2 and created a backup file, now we need to redeploy v1 and see the backup restored
        DeploymentProperties v1Duplicate = new DeploymentProperties();
        originalDeployProps.setDestinationCompliance(DestinationComplianceMode.full);
        v1Duplicate.putAll(this.originalDeployProps);
        v1Duplicate.setDeploymentId(3); // this is the same as v1, but it needs a unique deployment ID
        dd = new DeploymentData(v1Duplicate, tmpDir, deployDir, null, null, originalZipFiles, null, null, null, null);
        deployer = new Deployer(dd);
        this.diff = new DeployDifferences();
        FileHashcodeMap restoreFileHashcodeMap;
        restoreFileHashcodeMap = deployer.redeployAndRestoreBackupFiles(this.diff, false, dryRun);

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().size() == 1 : this.diff;
        assert this.diff.getChangedFiles().contains(originalFileName) : this.diff;
        assert this.diff.getBackedUpFiles().isEmpty() : this.diff;
        assert this.diff.getRestoredFiles().size() == 1 : this.diff;
        assert this.diff.getRestoredFiles().containsKey(originalFileName) : this.diff;
        assert this.diff.getIgnoredFiles().isEmpty() : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        assert restoreFileHashcodeMap.get(originalFileName).equals(newHashcodeY) : "hashcode doesn't reflect restored backup";

        if (dryRun) {
            // still our v2
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        } else {
            // we reverted back to v1 with the manual changes
            assert this.metadata.getCurrentDeploymentProperties().equals(v1Duplicate);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(restoreFileHashcodeMap);
            assert MessageDigestGenerator.getDigestString(this.currentFile).equals(newHashcodeY) : "file wasn't restored";
        }
    }

    private void baseX_Y_Z_Clean(boolean dryRun) throws Exception {
        String newContentY = "testX_Y_Z_YYY";
        writeFile(newContentY, this.currentFile);
        String newHashcodeY = MessageDigestGenerator.getDigestString(newContentY);

        String newContentZ = "testX_Y_Z_ZZZ";
        String newHashcodeZ = MessageDigestGenerator.getDigestString(newContentZ);
        File newZipFile = createZip(newContentZ, tmpDir, "new-content.zip", originalFileName);
        Map<File, File> newZipFiles = new HashMap<File, File>(1);
        newZipFiles.put(newZipFile, null);

        File ignoredSubdir = new File(this.deployDir, "ignoreSubdir");
        File ignoredFile = new File(ignoredSubdir, "ignore-me.txt");
        ignoredSubdir.mkdirs();
        writeFile("ignored content", ignoredFile);
        Pattern iRegex = Pattern.compile(".*ignoreSubdir.*"); // this matches the subdirectory name, thus everything under it is ignored
        assert ignoredFile.exists() : "for some reason we couldn't create our test file; cannot know if clean worked";

        newDeployProps.setDestinationCompliance(DestinationComplianceMode.full);
        DeploymentData dd = new DeploymentData(newDeployProps, tmpDir, deployDir, null, null, newZipFiles, null, null,
            iRegex, null);
        Deployer deployer = new Deployer(dd);
        FileHashcodeMap newFileHashcodeMap;
        newFileHashcodeMap = deployer.deploy(this.diff, true, dryRun); // note: clean is true

        // The new file changed the original, and our current file has been manually updated
        // but that current file's change does not match the new file. Therefore, the current file
        // is out of date. The safest thing to do is backup the current and copy the new file
        // to become the current file.

        assert !newFileHashcodeMap.equals(this.originalFileHashcodeMap);
        assert newFileHashcodeMap.size() == 1;
        assert newFileHashcodeMap.get(originalFileName).equals(newHashcodeZ);
        String[] contentHash = getOriginalFilenameContentHashcode();
        if (dryRun) {
            assert contentHash[0].equals(newContentY);
            assert contentHash[1].equals(newHashcodeY);
        } else {
            assert contentHash[0].equals(newContentZ);
            assert contentHash[1].equals(newHashcodeZ);
        }

        assert this.diff.getAddedFiles().isEmpty() : this.diff;
        assert this.diff.getDeletedFiles().isEmpty() : this.diff;
        assert this.diff.getChangedFiles().size() == 1 : this.diff;
        assert this.diff.getChangedFiles().contains(originalFileName) : this.diff;
        assert this.diff.getBackedUpFiles().size() == 1 : this.diff;
        assert this.diff.getBackedUpFiles().containsKey(originalFileName) : this.diff;
        assert this.diff.getIgnoredFiles().size() == 1 : this.diff;
        assert this.diff.getIgnoredFiles().contains(ignoredSubdir.getName()) : this.diff;
        assert this.diff.getRealizedFiles().isEmpty() : this.diff;
        assert this.diff.wasCleaned() : this.diff;
        assert this.diff.getErrors().isEmpty() : this.diff;

        if (dryRun) {
            assert this.metadata.getCurrentDeploymentProperties().equals(originalDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(originalFileHashcodeMap);
        } else {
            assert this.metadata.getCurrentDeploymentProperties().equals(newDeployProps);
            assert this.metadata.getCurrentDeploymentFileHashcodes().equals(newFileHashcodeMap);
        }

        // verify the backup copy
        File backupFile = new File(this.diff.getBackedUpFiles().get(originalFileName));
        if (dryRun) {
            assert !backupFile.exists() : "dry run should not create backup";
        } else {
            assert readFile(backupFile).equals(newContentY) : "did not backup the correct file?";
        }

        // if we cleaned, the ignored subdir and its file should no longer exist
        if (dryRun) {
            assert ignoredSubdir.isDirectory() : "dry run should not have really cleaned";
            assert ignoredFile.exists() : "dry run should not have really cleaned";
        } else {
            assert !ignoredSubdir.exists() : "directory should have been deleted due to the clean option";
            assert !ignoredFile.exists() : "file should have been deleted due to the clean option";
        }
    }

    private String[] getOriginalFilenameContentHashcode() throws Exception {
        return getFilenameContentHashcode(this.originalFileName);
    }

    private String[] getFilenameContentHashcode(String filename) throws Exception {
        String content = readFile(new File(this.deployDir, filename));
        String hashcode = MessageDigestGenerator.getDigestString(content);
        String[] contentHash = new String[] { content, hashcode };
        return contentHash;
    }

    private String readFile(File file) throws Exception {
        return new String(StreamUtil.slurp(new FileInputStream(file)));
    }

    private File writeFile(String content, File fileToOverwrite) throws Exception {
        FileOutputStream out = null;

        try {
            fileToOverwrite.getParentFile().mkdirs();
            out = new FileOutputStream(fileToOverwrite);
            out.write(content.getBytes());
            return fileToOverwrite;
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private File writeFile(String content, File destDir, String fileName) throws Exception {
        File destFile = new File(destDir, fileName);
        return writeFile(content, destFile);
    }

    private File createZip(String content, File destDir, String zipName, String entryName) throws Exception {
        FileOutputStream stream = null;
        ZipOutputStream out = null;

        try {
            destDir.mkdirs();
            File zipFile = new File(destDir, zipName);
            stream = new FileOutputStream(zipFile);
            out = new ZipOutputStream(stream);

            ZipEntry zipAdd = new ZipEntry(entryName);
            zipAdd.setTime(System.currentTimeMillis());
            out.putNextEntry(zipAdd);
            out.write(content.getBytes());
            return zipFile;
        } finally {
            if (out != null) {
                out.close();
            }
            if (stream != null) {
                stream.close();
            }
        }
    }

    private File createZip(String[] content, File destDir, String zipName, String[] entryName) throws Exception {
        FileOutputStream stream = null;
        ZipOutputStream out = null;

        try {
            destDir.mkdirs();
            File zipFile = new File(destDir, zipName);
            stream = new FileOutputStream(zipFile);
            out = new ZipOutputStream(stream);

            assert content.length == entryName.length;
            for (int i = 0; i < content.length; i++) {
                ZipEntry zipAdd = new ZipEntry(entryName[i]);
                zipAdd.setTime(System.currentTimeMillis());
                out.putNextEntry(zipAdd);
                out.write(content[i].getBytes());
            }
            return zipFile;
        } finally {
            if (out != null) {
                out.close();
            }
            if (stream != null) {
                stream.close();
            }
        }
    }
}
