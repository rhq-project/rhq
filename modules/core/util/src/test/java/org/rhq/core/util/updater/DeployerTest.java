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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

@Test
public class DeployerTest {

    // earlier we supported windows separators in the  map, but i think we want to always support /
    // this constant is here in case I want to move back to supporting windows paths explicitly
    // we'd do this by "fileSeparator = File.separator;"
    private static final String fileSeparator = "/";

    private TemplateEngine templateEngine;
    private String javaVersion;

    @BeforeClass
    public void beforeClass() {
        javaVersion = System.getProperty("java.version");

        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("rhq.system.hostname", "localhost");
        tokens.put("rhq.system.sysprop.java.version", javaVersion);

        templateEngine = new TemplateEngine(tokens);
    }

    @BeforeMethod
    public void beforeMethod() {
        System.out.println("\n\n=============== START OF NEW TEST ===============\n");
    }

    /**
     * Here is what our test zips and raw files are:
     * updater-test1.zip
     *    dir1
     *       file1
     *       file2
     *    dir2
     *       file3
     *    dir3
     *       dir4
     *          file4
     *    file0
     *
     * updater-test2.zip
     *    dir1
     *       file1
     *       file2
     *       fileB
     *    dir2
     *       file3
     *       fileC
     *    dir3
     *       dir4
     *          file4
     *    fileA
     *
     * updater-testA.txt
     * updater-testB.txt
     *
     * We need to test these cases (X, Y, Z, ? represent hashcodes; none means file doesn't exist):
     *    ORIGINAL CURRENT    NEW   What To Do...
     * a.        X       X      X   New file is installed over current*
     * b.        X       X      Y   New file is installed over current
     * c.        X       Y      X   Current file is left as-is
     * d.        X       Y      Y   New file is installed over current*
     * e.        X       Y      Z   New file is installed over current, current is backed up
     * f.     none       ?      ?   New file is installed over current, current is backed up
     * g.        X    none      ?   New file is installed
     * h.        ?       ?   none   Current file is deleted, if it changed from original, its backed up
     *
     * (*) means the new and current files will actually be the same content
     *
     * This test will be complex. We will initially install updater-test1.zip, updater-testA.txt:
     *    dir1/file1
     *    dir1/file2
     *    dir2/file3
     *    dir3/dir4/file4
     *    file0
     *    /ABSOLUTE/updater-testA.txt
     * Then we:
     *    - change dir1/file1
     *    - change updater-testA.txt
     *    - add updater-testB.txt
     *    - add fileB.txt
     *    - delete dir1/file2
     *    - add dir1/file999
     * Then the deployment is updated with updater-test2.zip, updater-testB.txt:
     *    dir1/file1 (we changed this and remains as we changed it)
     *    dir1/file2 (we deleted it but it comes back from updater-test2.zip)
     *    dir1/fileB (added from updater-test2.zip, our own current copy is backed up)
     *    dir2/file3
     *    dir2/fileC (added from updater-test2.zip)
     *    dir3/dir4/file4
     *    --file0-- (this is deleted, its no longer in our deployment zip)
     *    fileA (added from updater-test2.zip)
     *    --/ABSOLUTE/updater-testA.txt-- (should be deleted, no longer in our deployment, but it was changed from original, so its backed up)
     *    /ABSOLUTE/updater-testB.txt (added from our deployment, own current copy is backed up)
     *    --dir1/file999-- (should be deleted, not in our deployment - but is backed up)
     *
     * This means after the update the following will tested:
     * 1) deleted updater-testA.txt (h.)
     * 2) added updater-testB.txt, backed up our (absolute file) current (f.)
     * 3) deleted file0 (h.)
     * 4) added fileA, no backups (f.)
     * 5) added dir1/fileB, backed up our (relative file) current (f.)
     * 6) added dir2/fileC (f.)
     * 7) dir3/dir4/file4 is the same (a.)
     * 7) dir1/file1 is left in the changed state (c.)
     * 8) dir1/file2 is brought back again (g.)
     * 9) dir1/file999 is backed up and deleted (h.)
     * 10) dir2/file3 is the same (a.)
     *
     * We need to do the following afterwards in order to test b, d and e:
     *    - change updater-testB.txt
     *    - change the source updater-testB.txt
     * Then the deployment is updated with updater-test2.zip, updater-testB.txt, updater-testA.txt:
     *    dir1/file1
     *    dir1/file2
     *    dir1/fileB
     *    dir2/file3
     *    dir2/fileC
     *    dir3/dir4/file4
     *    fileA
     *    /ABSOLUTE/updater-testA.txt (added from our deployment)
     *    /ABSOLUTE/updater-testB.txt (changed - own current copy is backed up)
     *
     * This means after the update the following will be tested:
     * 11) updater-testB.txt is the changed source, backed up our current (e.)
     *
     * Next we do this:
     *    - change the source updater-testA.txt
     *    - change the source updater-testB.txt
     *    - change updater-testA.txt to the new changed source updater-testA.txt
     * Then the deployment is updated with updater-test2.zip, updater-testB.txt, updater-testA.txt:
     *    dir1/file1
     *    dir1/file2
     *    dir1/fileB
     *    dir2/file3
     *    dir2/fileC
     *    dir3/dir4/file4
     *    fileA
     *    /ABSOLUTE/updater-testA.txt (the content won't change, left as is)
     *    /ABSOLUTE/updater-testB.txt (changed but no backed up)
     * This means after the update the following will be tested:
     * 12) updater-testA.txt is the changed source (d.)
     * 13) updater-testB.txt is the changed source (b.)
     *
     * This does not test ignoring files on update nor does it test realizing files
     */
    public void testUpdateDeployZipsAndRawFiles() throws Exception {
        baseUpdateTest(false, false);
    }

    /**
     * Same as testUpdateDeployZipsAndRawFiles with the additional testing
     * of ignoring files and realizing files.
     */
    public void testUpdateDeployZipsAndRawFilesWithRealizeAndIgnore() throws Exception {
        baseUpdateTest(true, true);
    }

    /**
     * This is the base test used for both
     * testUpdateDeployZipsAndRawFiles and testUpdateDeployZipsAndRawFilesWithRealizeAndIgnore.
     */
    private void baseUpdateTest(boolean realize, boolean ignore) throws Exception {
        DeployDifferences diff;

        final Pattern realizeRegex = realize ? Pattern.compile("fileA") : null;
        final Pattern ignoreRegex = ignore ? Pattern.compile("ignoreme.*") : null;
        File fileToIgnore = null;

        final File tmpDir = FileUtil.createTempDirectory("testDeployerTest", ".dir", null);
        final File tmpDir2 = FileUtil.createTempDirectory("testDeployerTest_External_", ".dir", null); // simulates place outside of dest dir
        final File testRawFileBChange1 = File.createTempFile("testUpdateDeployZipsAndRawFilesB1", ".txt");
        final File testRawFileBChange2 = File.createTempFile("testUpdateDeployZipsAndRawFilesB2", ".txt");
        final File testRawFileAChange = File.createTempFile("testUpdateDeployZipsAndRawFilesA", ".txt");

        final File metadir = new File(tmpDir, ".rhqdeployments");

        final String file0 = "file0";
        final String file1 = "dir1" + fileSeparator + "file1";
        final String file2 = "dir1" + fileSeparator + "file2";
        final String file3 = "dir2" + fileSeparator + "file3";
        final String file4 = "dir3" + fileSeparator + "dir4" + fileSeparator + "file4";
        final String fileA = "fileA";
        final String fileB = "dir1" + fileSeparator + "fileB";
        final String fileC = "dir2" + fileSeparator + "fileC";
        final String file999 = "dir1" + fileSeparator + "file999";

        try {
            // this is a file that will be removed because our initial deployment is managing the root deploy dir
            // later we will recreate it to see that we ignore it during the upgrade.
            // note that during the initial deployment, we will still back it up.
            File ignoreDir = FileUtil.createTempDirectory("ignoreme", ".dir", tmpDir);
            fileToIgnore = new File(ignoreDir, "some-log.log");
            StreamUtil.copy(new ByteArrayInputStream("boo".getBytes()), new FileOutputStream(fileToIgnore));
            String fileToIgnorePath = ignoreDir.getName() + "/" + fileToIgnore.getName(); // yes, use /, even if we are on windows

            File testZipFile1 = new File("target/test-classes/updater-test1.zip");
            File testZipFile2 = new File("target/test-classes/updater-test2.zip");
            File testZipFile4 = new File("target/test-classes/updater-test4.zip");
            File testRawFileA = new File("target/test-classes/updater-testA.txt");
            File testRawFileB = new File("target/test-classes/updater-testB.txt");
            StreamUtil.copy(new ByteArrayInputStream("B1prime".getBytes()), new FileOutputStream(testRawFileBChange1));
            StreamUtil.copy(new ByteArrayInputStream("B2prime".getBytes()), new FileOutputStream(testRawFileBChange2));
            StreamUtil.copy(new ByteArrayInputStream("Aprime".getBytes()), new FileOutputStream(testRawFileAChange));
            File updaterAabsolute = new File(tmpDir2, "updater-testA.txt");
            File updaterBabsolute = new File(tmpDir2, "updater-testB.txt");

            DeploymentProperties deploymentProps = new DeploymentProperties(1, "testbundle2", "1.0.test", null);
            Map<File, File> zipFiles = new HashMap<File, File>(1);
            Map<File, Boolean> explodedZipFiles = new HashMap<File, Boolean>(1);
            zipFiles.put(testZipFile1, null);
            zipFiles.put(testZipFile4, tmpDir2);
            explodedZipFiles.put(testZipFile4, Boolean.FALSE);
            Map<File, File> rawFiles = new HashMap<File, File>(1);
            rawFiles.put(testRawFileA, updaterAabsolute); // raw file to absolute path
            File destDir = tmpDir;
            Map<File, Pattern> filesToRealizeRegex1 = new HashMap<File, Pattern>(2);
            filesToRealizeRegex1.put(testZipFile1, realizeRegex);
            filesToRealizeRegex1.put(testZipFile4, realizeRegex);
            DeploymentData dd = new DeploymentData(deploymentProps, zipFiles, rawFiles, tmpDir, destDir,
                filesToRealizeRegex1, null, templateEngine, ignoreRegex, true, explodedZipFiles);
            Deployer deployer = new Deployer(dd);
            diff = new DeployDifferences();

            DeploymentDiskUsage diskUsage = deployer.estimateDiskUsage();
            assert diskUsage.getMaxDiskUsable() > 0L;
            assert diskUsage.getDiskUsage() > 0L;
            assert diskUsage.getFileCount() == 11 : "should have been 10 files in zips and 1 raw file (found "
                + diskUsage.getFileCount() + ")";

            deployer.deploy(diff);

            // our initial deploy should have deleted this because we are managing the root dir
            assert !fileToIgnore.exists() : "should have removed this file since we are managing the root dir";
            assert !fileToIgnore.getParentFile().exists() : "should have removed this file since we are managing the root dir";
            assert diff.getIgnoredFiles().size() == 0 : "this was an initial deploy - nothing to ignore (ignore is only for updates)";
            assert diff.getAddedFiles().size() == 7 : diff;
            String zipFile4Path = FileUtil.useForwardSlash(new File(tmpDir2, testZipFile4.getName()).getPath());
            assert diff.getAddedFiles().contains(zipFile4Path) : diff;

            assert diff.getDeletedFiles().size() == 1 : diff;
            assert diff.getDeletedFiles().contains(fileToIgnorePath) : "should have deleted this unknown file" + diff;
            assert diff.getChangedFiles().size() == 0 : diff;
            assert diff.getRealizedFiles().size() == 0 : "No fileA to realize in this deployment: " + diff;
            assert diff.getBackedUpFiles().size() == 1 : diff;
            assert diff.getBackedUpFiles().get(fileToIgnorePath) != null : "should have backed up this file" + diff;

            if (ignore) {
                // let's create this again to make sure we really do ignore it
                ignoreDir = FileUtil.createTempDirectory("ignoreme", ".dir", tmpDir);
                fileToIgnore = new File(ignoreDir, "some-log.log");
                StreamUtil.copy(new ByteArrayInputStream("boo".getBytes()), new FileOutputStream(fileToIgnore));
                fileToIgnorePath = ignoreDir.getName() + "/" + fileToIgnore.getName(); // yes, use /, even if we are on windows
            }

            StreamUtil.copy(new ByteArrayInputStream("X".getBytes()), new FileOutputStream(new File(tmpDir, file1)));
            StreamUtil.copy(new ByteArrayInputStream("X".getBytes()), new FileOutputStream(updaterAabsolute));
            StreamUtil.copy(new ByteArrayInputStream("X".getBytes()), new FileOutputStream(updaterBabsolute));
            assert new File(tmpDir, file2).delete() : "could not delete file2 for test";
            StreamUtil.copy(new ByteArrayInputStream("X".getBytes()), new FileOutputStream(new File(tmpDir, file999)));
            StreamUtil.copy(new ByteArrayInputStream("X".getBytes()), new FileOutputStream(new File(tmpDir, fileB)));
            File tmpZipFile4 = new File(zipFile4Path + ".tmp");
            FileUtil.copyFile(new File(zipFile4Path), tmpZipFile4);
            ZipInputStream zin = new ZipInputStream(new FileInputStream(tmpZipFile4));
            ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipFile4Path));
            byte[] buffer = new byte[1024];
            ZipEntry entry;
            while (null != (entry = zin.getNextEntry())) {
                entry.setComment("X");
                zout.putNextEntry(entry);
                for(int read = zin.read(buffer); read > -1; read = zin.read(buffer))
                {
                    zout.write(buffer, 0, read);
                }
                zout.closeEntry();
            }
            zout.close();
            zin.close();
            tmpZipFile4.delete();

            deploymentProps = new DeploymentProperties(2, "testbundle2", "2.0.test", null);
            zipFiles = new HashMap<File, File>(1);
            zipFiles.put(testZipFile2, null);
            rawFiles = new HashMap<File, File>(1);
            rawFiles.put(testRawFileB, updaterBabsolute); // raw file to absolute path
            Map<File, Pattern> filesToRealizeRegex2 = new HashMap<File, Pattern>(1);
            filesToRealizeRegex2.put(testZipFile2, realizeRegex);
            dd = new DeploymentData(deploymentProps, zipFiles, rawFiles, tmpDir, destDir, filesToRealizeRegex2, null,
                templateEngine, ignoreRegex, true, null);
            deployer = new Deployer(dd);
            diff = new DeployDifferences();
            deployer.deploy(diff); // this is an upgrade

            if (ignore) {
                assert "boo".equals(new String(StreamUtil.slurp(new FileInputStream(fileToIgnore))));
                assert diff.getIgnoredFiles().size() == 1;
                assert diff.getIgnoredFiles().contains(fileToIgnore.getParentFile().getName());
            }

            assert !updaterAabsolute.exists() : "updateA.txt should be deleted";
            assert updaterBabsolute.exists() : "updateB.txt should exist now";
            assert !"X".equals(new String(StreamUtil.slurp(new FileInputStream(updaterBabsolute))));

            // TODO if this test is running on windows, we need to FileUtil.stripDriveLetter from the updater?absolute vars
            boolean isWindows = File.separatorChar == '\\';
            String updaterBabsoluteBackupTo1;
            String updaterBabsoluteBackupTo2;
            String updaterBabsoluteBackupTo3;
            if (!isWindows) {
                updaterBabsoluteBackupTo1 = new File(metadir, "1/ext-backup/" + updaterBabsolute.getAbsolutePath())
                    .getAbsolutePath();
                updaterBabsoluteBackupTo2 = new File(metadir, "2/ext-backup/" + updaterBabsolute.getAbsolutePath())
                    .getAbsolutePath();
                updaterBabsoluteBackupTo3 = new File(metadir, "3/ext-backup/" + updaterBabsolute.getAbsolutePath())
                    .getAbsolutePath();
            } else {
                StringBuilder str = new StringBuilder(updaterBabsolute.getAbsolutePath());
                String driveLetter = FileUtil.stripDriveLetter(str);
                if (driveLetter != null) {
                    driveLetter = "_" + driveLetter + fileSeparator;
                } else {
                    driveLetter = "";
                }
                updaterBabsoluteBackupTo1 = new File(metadir, "1/ext-backup/" + driveLetter + str.toString())
                    .getAbsolutePath();
                updaterBabsoluteBackupTo2 = new File(metadir, "2/ext-backup/" + driveLetter + str.toString())
                    .getAbsolutePath();
                updaterBabsoluteBackupTo3 = new File(metadir, "3/ext-backup/" + driveLetter + str.toString())
                    .getAbsolutePath();
            }
            assert !(new File(updaterBabsoluteBackupTo1).exists()) : "updateB.txt backup should not be in deploy #1";
            assert new File(updaterBabsoluteBackupTo2).exists() : "missing updateB.txt backup from deploy #2";

            String updaterAabsoluteBackupTo1;
            String updaterAabsoluteBackupTo2;
            if (!isWindows) {
                updaterAabsoluteBackupTo1 = new File(metadir, "1/ext-backup/" + updaterAabsolute.getAbsolutePath())
                    .getAbsolutePath();
                updaterAabsoluteBackupTo2 = new File(metadir, "2/ext-backup/" + updaterAabsolute.getAbsolutePath())
                    .getAbsolutePath();
            } else {
                StringBuilder str = new StringBuilder(updaterAabsolute.getAbsolutePath());
                String driveLetter = FileUtil.stripDriveLetter(str);
                if (driveLetter != null) {
                    driveLetter = "_" + driveLetter + fileSeparator;
                } else {
                    driveLetter = "";
                }
                updaterAabsoluteBackupTo1 = new File(metadir, "1/ext-backup/" + driveLetter + str.toString())
                    .getAbsolutePath();
                updaterAabsoluteBackupTo2 = new File(metadir, "2/ext-backup/" + driveLetter + str.toString())
                    .getAbsolutePath();
            }
            assert !(new File(updaterAabsoluteBackupTo1).exists()) : "should not have updateA.txt backup in #1";
            assert new File(updaterAabsoluteBackupTo2).exists() : "missing updateA.txt backup";

            assert !(new File(tmpDir, file0).exists()) : "file0 should be deleted";
            assert new File(tmpDir, fileA).exists() : "fileA should exist";
            String fileAcontent = new String(StreamUtil.slurp(new FileInputStream(new File(tmpDir, fileA))));
            if (realize) {
                assert !fileAcontent.contains("@@rhq.system.hostname@@") : "should not have realized in this test: "
                    + fileAcontent;
            } else {
                assert fileAcontent.contains("@@rhq.system.hostname@@") : "should have realized in this test: "
                    + fileAcontent;
            }
            assert new File(tmpDir, fileB).exists() : "fileB should exist";
            assert !"X".equals(new String(StreamUtil.slurp(new FileInputStream(new File(tmpDir, fileB)))));
            File fileBbackupTo2 = new File(metadir, "2/backup/" + fileB);
            assert fileBbackupTo2.exists() : "should have fileB backed up in deploy 2 backup dir";
            assert "X".equals(new String(StreamUtil.slurp(new FileInputStream(fileBbackupTo2))));
            assert new File(tmpDir, fileC).exists() : "fileC should exist";
            assert new File(tmpDir, file4).exists() : "file4 should exist";
            assert "X".equals(new String(StreamUtil.slurp(new FileInputStream(new File(tmpDir, file1)))));
            assert new File(tmpDir, file2).exists() : "file2 should exist again";
            assert !(new File(tmpDir, file999).exists()) : "file999 should be deleted";
            File file999backupTo2 = new File(metadir, "2/backup/" + file999);
            assert file999backupTo2.exists() : "file999 should not be backed up";
            assert new File(tmpDir, file3).exists() : "file3 should exist";
            assert diff.getAddedFiles().size() == 3 : diff;
            assert diff.getAddedFiles().contains(file2) : diff;
            assert diff.getAddedFiles().contains(fileC) : diff;
            assert diff.getAddedFiles().contains(fileA) : diff;
            assert diff.getDeletedFiles().size() == 4 : diff;
            assert diff.getDeletedFiles().contains(file0) : diff;
            assert diff.getDeletedFiles().contains(diff.convertPath(updaterAabsolute.getAbsolutePath())) : diff;
            assert diff.getDeletedFiles().contains(file999) : diff;
            assert diff.getDeletedFiles().contains(zipFile4Path) : diff;
            assert diff.getChangedFiles().size() == 2 : diff;
            assert diff.getChangedFiles().contains(diff.convertPath(updaterBabsolute.getAbsolutePath())) : diff;
            assert diff.getChangedFiles().contains(fileB) : diff;
            assert diff.getBackedUpFiles().size() == 5 : diff;
            assert diff.getBackedUpFiles().containsKey(zipFile4Path) : diff;
            assert diff.getBackedUpFiles().containsKey(diff.convertPath(updaterAabsolute.getAbsolutePath())) : diff;
            assert diff.getBackedUpFiles().get(diff.convertPath(updaterAabsolute.getAbsolutePath()))
                .equals(diff.convertPath(updaterAabsoluteBackupTo2)) : diff;
            assert diff.getBackedUpFiles().containsKey(diff.convertPath(updaterBabsolute.getAbsolutePath())) : diff;
            assert diff.getBackedUpFiles().get(diff.convertPath(updaterBabsolute.getAbsolutePath()))
                .equals(diff.convertPath(updaterBabsoluteBackupTo2)) : diff;
            assert diff.getBackedUpFiles().containsKey(fileB) : diff;
            assert diff.getBackedUpFiles().get(fileB).equals(diff.convertPath(fileBbackupTo2.getAbsolutePath())) : diff;
            assert diff.getBackedUpFiles().containsKey(file999) : diff;
            assert diff.getBackedUpFiles().get(file999).equals(diff.convertPath(file999backupTo2.getAbsolutePath())) : diff;
            if (realize) {
                assert diff.getRealizedFiles().size() == 1 : diff;
                assert diff.getRealizedFiles().containsKey(fileA) : diff;
            } else {
                assert diff.getRealizedFiles().size() == 0 : diff;
            }

            StreamUtil.copy(new ByteArrayInputStream("Y".getBytes()), new FileOutputStream(updaterBabsolute));
            deploymentProps = new DeploymentProperties(3, "testbundle2", "3.0.test", null);
            zipFiles = new HashMap<File, File>(1);
            zipFiles.put(testZipFile2, null);
            rawFiles = new HashMap<File, File>(2);
            rawFiles.put(testRawFileA, updaterAabsolute); // source raw file to absolute path
            rawFiles.put(testRawFileBChange1, updaterBabsolute); // source raw file to absolute path
            dd = new DeploymentData(deploymentProps, zipFiles, rawFiles, tmpDir, destDir, filesToRealizeRegex2, null,
                templateEngine, ignoreRegex, true, null);
            deployer = new Deployer(dd);
            diff = new DeployDifferences();
            deployer.deploy(diff);

            if (ignore) {
                assert "boo".equals(new String(StreamUtil.slurp(new FileInputStream(fileToIgnore))));
                assert diff.getIgnoredFiles().size() == 1;
                assert diff.getIgnoredFiles().contains(fileToIgnore.getParentFile().getName());
            }

            assert new File(updaterBabsoluteBackupTo3).exists() : "updaterB should be backed up";
            assert "B1prime".equals(new String(StreamUtil.slurp(new FileInputStream(updaterBabsolute))));
            assert "Y".equals(new String(StreamUtil.slurp(new FileInputStream(new File(updaterBabsoluteBackupTo3)))));

            assert diff.getAddedFiles().size() == 1 : diff;
            assert diff.getAddedFiles().contains(diff.convertPath(updaterAabsolute.getAbsolutePath())) : diff;
            assert diff.getDeletedFiles().size() == 0 : diff;
            assert diff.getChangedFiles().size() == 1 : diff;
            assert diff.getChangedFiles().contains(diff.convertPath(updaterBabsolute.getAbsolutePath())) : diff;
            assert diff.getBackedUpFiles().size() == 1 : diff;
            assert diff.getBackedUpFiles().containsKey(diff.convertPath(updaterBabsolute.getAbsolutePath())) : diff;
            assert diff.getBackedUpFiles().get(diff.convertPath(updaterBabsolute.getAbsolutePath()))
                .equals(diff.convertPath(updaterBabsoluteBackupTo3)) : diff;
            if (realize) {
                assert diff.getRealizedFiles().size() == 1 : diff;
                assert diff.getRealizedFiles().containsKey(fileA) : diff;
            } else {
                assert diff.getRealizedFiles().size() == 0 : diff;
            }

            StreamUtil.copy(new ByteArrayInputStream("Aprime".getBytes()), new FileOutputStream(updaterAabsolute));

            deploymentProps = new DeploymentProperties(4, "testbundle2", "4.0.test", null);
            zipFiles = new HashMap<File, File>(1);
            zipFiles.put(testZipFile2, null);
            rawFiles = new HashMap<File, File>(2);
            rawFiles.put(testRawFileAChange, updaterAabsolute); // source raw file to absolute path
            rawFiles.put(testRawFileBChange2, updaterBabsolute); // source raw file to absolute path
            dd = new DeploymentData(deploymentProps, zipFiles, rawFiles, tmpDir, destDir, filesToRealizeRegex2, null,
                templateEngine, ignoreRegex, true, null);
            deployer = new Deployer(dd);
            diff = new DeployDifferences();
            deployer.deploy(diff);

            if (ignore) {
                assert "boo".equals(new String(StreamUtil.slurp(new FileInputStream(fileToIgnore))));
                assert diff.getIgnoredFiles().size() == 1;
                assert diff.getIgnoredFiles().contains(fileToIgnore.getParentFile().getName());
            }

            assert "Aprime".equals(new String(StreamUtil.slurp(new FileInputStream(updaterAabsolute))));
            assert "B2prime".equals(new String(StreamUtil.slurp(new FileInputStream(updaterBabsolute))));

            assert diff.getAddedFiles().size() == 0 : diff;
            assert diff.getDeletedFiles().size() == 0 : diff;
            assert diff.getChangedFiles().size() == 1 : diff;
            assert diff.getChangedFiles().contains(diff.convertPath(updaterBabsolute.getAbsolutePath())) : diff;
            assert diff.getBackedUpFiles().size() == 0 : diff;
            if (realize) {
                assert diff.getRealizedFiles().size() == 1 : diff;
                assert diff.getRealizedFiles().containsKey(fileA) : diff;
            } else {
                assert diff.getRealizedFiles().size() == 0 : diff;
            }

            File previousDeployment1 = new File(metadir, "1/previous-deployment.properties");
            File previousDeployment2 = new File(metadir, "2/previous-deployment.properties");
            File previousDeployment3 = new File(metadir, "3/previous-deployment.properties");
            File previousDeployment4 = new File(metadir, "4/previous-deployment.properties");
            assert !previousDeployment1.exists() : "there was no previous deployment for #1";
            assert previousDeployment2.exists() : "there was a previous deployment";
            assert previousDeployment3.exists() : "there was a previous deployment";
            assert previousDeployment4.exists() : "there was a previous deployment";
            assert DeploymentProperties.loadFromFile(previousDeployment2).getDeploymentId() == 1;
            assert DeploymentProperties.loadFromFile(previousDeployment3).getDeploymentId() == 2;
            assert DeploymentProperties.loadFromFile(previousDeployment4).getDeploymentId() == 3;
        } finally {
            FileUtil.purge(tmpDir, true);
            FileUtil.purge(tmpDir2, true);
            testRawFileBChange1.delete();
            testRawFileBChange2.delete();
            testRawFileAChange.delete();
        }
    }

    public void testDeployRawFileToAbsolutePath() throws Exception {
        File tmpDir = FileUtil.createTempDirectory("testDeployerTest", ".dir", null);
        File rawFileDestination = new File(System.getProperty("java.io.tmpdir"), "rawB.txt");

        try {
            File testRawFileA = new File("target/test-classes/updater-testA.txt");
            File testRawFileB = new File("target/test-classes/updater-testB.txt");

            Pattern filesToRealizeRegex = Pattern.compile(".*rawB.txt");

            DeploymentProperties deploymentProps = new DeploymentProperties(0, "testbundle2", "2.0.test", null);
            Map<File, File> zipFiles = null;
            Map<File, File> rawFiles = new HashMap<File, File>();
            rawFiles.put(testRawFileA, new File("rawA.txt")); // we will _not_ realize this one
            rawFiles.put(testRawFileB, rawFileDestination); // we will realize this one
            File destDir = tmpDir;
            Pattern ignoreRegex = null;
            Set<File> realizeRawFiles1 = new HashSet<File>(1);
            realizeRawFiles1.add(testRawFileB);
            DeploymentData dd = new DeploymentData(deploymentProps, zipFiles, rawFiles, tmpDir, destDir, null,
                realizeRawFiles1, templateEngine, ignoreRegex, true, null);
            Deployer deployer = new Deployer(dd);
            DeployDifferences diff = new DeployDifferences();
            FileHashcodeMap map = deployer.deploy(diff);

            assert diff.getAddedFiles().size() == 2 : diff;
            assert diff.getAddedFiles().contains("rawA.txt") : diff;
            assert diff.getAddedFiles().contains(diff.convertPath(rawFileDestination.getAbsolutePath())) : diff;

            assert map.size() == 2 : map;
            String f = "rawA.txt";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert MessageDigestGenerator.getDigestString(testRawFileA).equals(map.get(f)) : "should have same hash, we didn't realize this one";
            f = rawFileDestination.getAbsolutePath();
            assert map.containsKey(f) : map;
            assert new File(f).exists();
            assert MessageDigestGenerator.getDigestString(new File(f)).equals(map.get(f));
            assert !MessageDigestGenerator.getDigestString(testRawFileB).equals(map.get(f)) : "should have different hash, we realized this one";

        } finally {
            FileUtil.purge(tmpDir, true);
            rawFileDestination.delete();
        }
    }

    public void testInitialDeployZipsAndRawFiles() throws Exception {
        File tmpDir = FileUtil.createTempDirectory("testDeployerTest", ".dir", null);
        try {
            File testZipFile1 = new File("target/test-classes/updater-test2.zip");
            File testZipFile2 = new File("target/test-classes/updater-test3.zip");
            File testRawFileA = new File("target/test-classes/updater-testA.txt");
            File testRawFileB = new File("target/test-classes/updater-testB.txt");

            // '.' in place of file separator to support running test on windows & unix
            Pattern filesToRealizeRegex = Pattern
                .compile("(fileA)|(dir1.fileB)|(fileAAA)|(dir100.fileBBB)|(dir100.rawB.txt)");

            DeploymentProperties deploymentProps = new DeploymentProperties(0, "testbundle2", "2.0.test", null);
            Map<File, File> zipFiles = new HashMap<File, File>(2);
            zipFiles.put(testZipFile1, null);
            zipFiles.put(testZipFile2, null);
            Map<File, File> rawFiles = new HashMap<File, File>();
            rawFiles.put(testRawFileA, new File("dirA/rawA.txt")); // we will _not_ realize this one
            rawFiles.put(testRawFileB, new File("dir100/rawB.txt")); // we will realize this one
            File destDir = tmpDir;
            Pattern ignoreRegex = null;

            Set<File> realizeRawFiles1 = new HashSet<File>(3);
            realizeRawFiles1.add(testZipFile1);
            realizeRawFiles1.add(testZipFile2);
            realizeRawFiles1.add(testRawFileB);
            DeploymentData dd = new DeploymentData(deploymentProps, zipFiles, rawFiles, tmpDir, destDir, null,
                realizeRawFiles1, templateEngine, ignoreRegex, true, null);
            Deployer deployer = new Deployer(dd);
            DeployDifferences listener = new DeployDifferences();
            deployer.deploy(listener);

            FileHashcodeMap map = FileHashcodeMap.generateFileHashcodeMap(destDir, null, null);
            assert map.size() == 13 : map;
            assert listener.getAddedFiles().size() == 13 : listener;
            String f = "dir1" + fileSeparator + "file1";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "dir1" + fileSeparator + "file2";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "dir2" + fileSeparator + "file3";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "dir3" + fileSeparator + "dir4" + fileSeparator + "file4";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "fileA";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "dir1" + fileSeparator + "fileB";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "dir2" + fileSeparator + "fileC";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "fileAAA";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "dir100" + fileSeparator + "file100";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "dir100" + fileSeparator + "file200";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "dir100" + fileSeparator + "fileBBB";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "dirA" + fileSeparator + "rawA.txt";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert MessageDigestGenerator.getDigestString(testRawFileA).equals(map.get(f)) : "should have same hash, we didn't realize this one";
            assert listener.getAddedFiles().contains(f) : listener;
            f = "dir100" + fileSeparator + "rawB.txt";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert !MessageDigestGenerator.getDigestString(testRawFileB).equals(map.get(f)) : "should have different hash, we realized this one";
            assert listener.getAddedFiles().contains(f) : listener;

        } finally {
            FileUtil.purge(tmpDir, true);
        }
    }

    public void testInitialDeployOneZip() throws Exception {
        File tmpDir = FileUtil.createTempDirectory("testDeployerTest", ".dir", null);
        try {
            File testZipFile1 = new File("target/test-classes/updater-test2.zip");
            Pattern filesToRealizeRegex = Pattern.compile("(fileA)|(dir1.fileB)"); // '.' in place of file separator to support running test on windows & unix

            DeploymentProperties deploymentProps = new DeploymentProperties(0, "testbundle", "1.0.test", null);
            Map<File, File> zipFiles = new HashMap<File, File>(1);
            zipFiles.put(testZipFile1, null);
            Map<File, File> rawFiles = null;
            File destDir = tmpDir;
            Pattern ignoreRegex = null;

            Map<File, Pattern> realizeRegex1 = new HashMap<File, Pattern>(1);
            realizeRegex1.put(testZipFile1, filesToRealizeRegex);

            DeploymentData dd = new DeploymentData(deploymentProps, zipFiles, rawFiles, tmpDir, destDir, realizeRegex1,
                null, templateEngine, ignoreRegex, true, null);
            Deployer deployer = new Deployer(dd);
            DeployDifferences listener = new DeployDifferences();
            deployer.deploy(listener);

            FileHashcodeMap map = FileHashcodeMap.generateFileHashcodeMap(destDir, null, null);
            assert map.size() == 7 : map;
            assert listener.getAddedFiles().size() == 7 : listener;
            String f = "dir1" + fileSeparator + "file1";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "dir1" + fileSeparator + "file2";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "dir2" + fileSeparator + "file3";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "dir3" + fileSeparator + "dir4" + fileSeparator + "file4";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "fileA";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "dir1" + fileSeparator + "fileB";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;
            f = "dir2" + fileSeparator + "fileC";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert listener.getAddedFiles().contains(f) : listener;

        } finally {
            FileUtil.purge(tmpDir, true);
        }
    }

    public void testInitialDeployOneZipAbsoluteDestDir() throws Exception {
        File tmpDir = FileUtil.createTempDirectory("testDeployerTest", ".dir", null);
        File tmpAlternateDir = FileUtil.createTempDirectory("testDeployerTestAlternate", ".dir", null);
        try {
            File testZipFile1 = new File("target/test-classes/updater-test2.zip");
            Pattern filesToRealizeRegex = Pattern.compile("(fileA)|(dir1.fileB)"); // '.' in place of file separator to support running test on windows & unix

            DeploymentProperties deploymentProps = new DeploymentProperties(0, "testbundle", "1.0.test", null);
            Map<File, File> zipFiles = new HashMap<File, File>(1);
            zipFiles.put(testZipFile1, tmpAlternateDir);
            Map<File, Boolean> zipsExploded = new HashMap<File, Boolean>(1);
            zipsExploded.put(testZipFile1, Boolean.FALSE);
            Map<File, File> rawFiles = null;
            File destDir = tmpDir;
            Pattern ignoreRegex = null;

            Map<File, Pattern> realizeRegex1 = new HashMap<File, Pattern>(1);
            realizeRegex1.put(testZipFile1, filesToRealizeRegex);

            DeploymentData dd = new DeploymentData(deploymentProps, zipFiles, rawFiles, tmpDir, destDir, realizeRegex1,
                null, templateEngine, ignoreRegex, true, zipsExploded);
            Deployer deployer = new Deployer(dd);
            DeployDifferences listener = new DeployDifferences();
            deployer.deploy(listener);

            FileHashcodeMap map = FileHashcodeMap.generateFileHashcodeMap(destDir, null, null);
            assert map.size() == 0 : map;

            map = FileHashcodeMap.generateFileHashcodeMap(tmpAlternateDir, null, null);
            assert map.size() == 1 : map;
            assert listener.getAddedFiles().size() == 1 : listener;
            String f = testZipFile1.getName();
            assert map.containsKey(f) : map;
            File deployedZipFile = new File(tmpAlternateDir, f);
            assert deployedZipFile.exists();
            assert MessageDigestGenerator.getDigestString(deployedZipFile).equals(map.get(f));
            assert listener.getAddedFiles().contains(FileUtil.useForwardSlash(deployedZipFile.getPath())) : listener;

        } finally {
            FileUtil.purge(tmpDir, true);
            FileUtil.purge(tmpAlternateDir, true);
        }
    }

    public void testInitialDeployOneZipRelativeDestDir() throws Exception {
        File tmpDir = FileUtil.createTempDirectory("testDeployerTest", ".dir", null);

        try {
            File testZipFile1 = new File("target/test-classes/updater-test2.zip");
            Pattern filesToRealizeRegex = Pattern.compile("(fileA)|(dir1.fileB)"); // '.' in place of file separator to support running test on windows & unix

            DeploymentProperties deploymentProps = new DeploymentProperties(0, "testbundle", "1.0.test", null);
            Map<File, File> zipFiles = new HashMap<File, File>(1);
            String relativeAlternatePath = "relative/deploy/directory";
            File relativeAlternateDir = new File(tmpDir, relativeAlternatePath);
            zipFiles.put(testZipFile1, relativeAlternateDir);
            Map<File, Boolean> zipsExploded = new HashMap<File, Boolean>(1);
            zipsExploded.put(testZipFile1, Boolean.FALSE);
            Map<File, File> rawFiles = null;
            File destDir = tmpDir;
            Pattern ignoreRegex = null;

            Map<File, Pattern> realizeRegex1 = new HashMap<File, Pattern>(1);
            realizeRegex1.put(testZipFile1, filesToRealizeRegex);

            DeploymentData dd = new DeploymentData(deploymentProps, zipFiles, rawFiles, tmpDir, destDir, realizeRegex1,
                null, templateEngine, ignoreRegex, true, zipsExploded);
            Deployer deployer = new Deployer(dd);
            DeployDifferences listener = new DeployDifferences();
            deployer.deploy(listener);

            FileHashcodeMap map = FileHashcodeMap.generateFileHashcodeMap(destDir, null, null);
            assert map.size() == 1 : map;
            assert listener.getAddedFiles().size() == 1 : listener;
            String f = "relative" + fileSeparator + "deploy" + fileSeparator + "directory" + fileSeparator
                + testZipFile1.getName();
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            File deployedZipFile = new File(tmpDir, f);
            assert listener.getAddedFiles().contains(FileUtil.useForwardSlash(deployedZipFile.getPath())) : listener;
        } finally {
            FileUtil.purge(tmpDir, true);
        }
    }

}