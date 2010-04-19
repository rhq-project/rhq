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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

@Test
public class DeployerTest {

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
     * h.        ?       ?   none   Current file is deleted

     * This test will be complex. We will initially install updater-test1.zip, updater-testA.txt.
     * Then we:
     *   change dir1/file1
     *   change updater-testA.txt
     *   add updater-testB.txt
     *   add fileB.txt
     *   delete dir1/file2
     *   add dir1/file999
     * Then the deployment is updated with updater-test2.zip, updater-testB.txt.
     * 
     * This means after the update the following will tested:
     * 1) deleted updater-testA.txt (h.)
     * 2) added updater-testB.txt, backed up our (absolute file) current (f.)
     * 3) deleted file0 (h.)
     * 4) added fileA, no backups (f.)
     * 5) added dir1/fileB, backed up our (relative file) current (f.)
     * 6) added dir2/fileC (f.)
     * 7) dir3/file4 is the same (a.)
     * 7) dir1/file1 is left in the changed state (c.)
     * 8) dir1/file2 is brought back again (g.)
     * 9) dir1/file999 is deleted (h.)
     * 10) dir2/file3 is the same (a.)
     * 
     * We need to do the following afterwards in order to test b, d and e:
     *   change updater-testB.txt
     *   change the source updater-testB.txt
     *   install updater-test2.zip, updater-testB.txt, updater-testA.txt
     * 11) updater-testB.txt is the changed source, backed up our current (e.)
     *   change the source updater-testA.txt
     *   change the source updater-testB.txt
     *   change updater-testA.txt to the new changed source updater-testA.txt
     *   install updater-test2.zip, updater-testB.txt, updater-testA.txt
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
        final String backupExtension = ".rhqbackup";

        Pattern filesToRealizeRegex = realize ? Pattern.compile("fileA") : null;
        Pattern ignoreRegex = ignore ? Pattern.compile("ignoreme.*") : null;
        File fileToIgnore = null;

        File tmpDir = FileUtil.createTempDirectory("testDeployerTest", ".dir", null);
        File tmpDir2 = FileUtil.createTempDirectory("testDeployerTest2", ".dir", null);
        File testRawFileBChange1 = File.createTempFile("testUpdateDeployZipsAndRawFilesB1", ".txt");
        File testRawFileBChange2 = File.createTempFile("testUpdateDeployZipsAndRawFilesB2", ".txt");
        File testRawFileAChange = File.createTempFile("testUpdateDeployZipsAndRawFilesA", ".txt");
        try {
            if (ignore) {
                // create a file that will be retained because we will be ignoring it
                File ignoreDir = FileUtil.createTempDirectory("ignoreme", ".dir", tmpDir);
                fileToIgnore = new File(ignoreDir, "some-log.log");
                StreamUtil.copy(new ByteArrayInputStream("boo".getBytes()), new FileOutputStream(fileToIgnore));
            }

            File testZipFile1 = new File("target/test-classes/updater-test1.zip");
            File testZipFile2 = new File("target/test-classes/updater-test2.zip");
            File testRawFileA = new File("target/test-classes/updater-testA.txt");
            File testRawFileB = new File("target/test-classes/updater-testB.txt");
            StreamUtil.copy(new ByteArrayInputStream("B1prime".getBytes()), new FileOutputStream(testRawFileBChange1));
            StreamUtil.copy(new ByteArrayInputStream("B2prime".getBytes()), new FileOutputStream(testRawFileBChange2));
            StreamUtil.copy(new ByteArrayInputStream("Aprime".getBytes()), new FileOutputStream(testRawFileAChange));
            File updaterAabsolute = new File(tmpDir2, "updater-testA.txt");
            File updaterBabsolute = new File(tmpDir2, "updater-testB.txt");

            DeploymentProperties deploymentProps = new DeploymentProperties(1, "testbundle2", "1.0.test", null);
            Set<File> zipFiles = new HashSet<File>(1);
            zipFiles.add(testZipFile1);
            Map<File, File> rawFiles = new HashMap<File, File>(1);
            rawFiles.put(testRawFileA, updaterAabsolute); // raw file to absolute path
            File destDir = tmpDir;
            Deployer deployer = new Deployer(deploymentProps, zipFiles, rawFiles, destDir, filesToRealizeRegex,
                templateEngine, ignoreRegex);
            deployer.deploy();

            if (ignore) {
                assert "boo".equals(new String(StreamUtil.slurp(new FileInputStream(fileToIgnore))));
            }

            String file1 = "dir1" + File.separator + "file1";
            StreamUtil.copy(new ByteArrayInputStream("X".getBytes()), new FileOutputStream(new File(tmpDir, file1)));
            StreamUtil.copy(new ByteArrayInputStream("X".getBytes()), new FileOutputStream(updaterAabsolute));
            StreamUtil.copy(new ByteArrayInputStream("X".getBytes()), new FileOutputStream(updaterBabsolute));
            String file2 = "dir1" + File.separator + "file2";
            assert new File(tmpDir, file2).delete() : "could not delete file2 for test";
            String file999 = "dir1" + File.separator + "file999";
            StreamUtil.copy(new ByteArrayInputStream("X".getBytes()), new FileOutputStream(new File(tmpDir, file999)));
            String fileB = "dir1" + File.separator + "fileB";
            StreamUtil.copy(new ByteArrayInputStream("X".getBytes()), new FileOutputStream(new File(tmpDir, fileB)));

            deploymentProps = new DeploymentProperties(2, "testbundle2", "2.0.test", null);
            zipFiles = new HashSet<File>(1);
            zipFiles.add(testZipFile2);
            rawFiles = new HashMap<File, File>(1);
            rawFiles.put(testRawFileB, updaterBabsolute); // raw file to absolute path
            deployer = new Deployer(deploymentProps, zipFiles, rawFiles, destDir, filesToRealizeRegex, templateEngine,
                ignoreRegex);
            deployer.deploy();

            if (ignore) {
                assert "boo".equals(new String(StreamUtil.slurp(new FileInputStream(fileToIgnore))));
            }

            assert !updaterAabsolute.exists() : "updateA.txt should be deleted";
            assert updaterBabsolute.exists() : "updateB.txt should exist now";
            assert !"X".equals(new String(StreamUtil.slurp(new FileInputStream(updaterBabsolute))));
            String updaterBabsoluteBackup = updaterBabsolute.getAbsolutePath() + backupExtension;
            assert new File(updaterBabsoluteBackup).exists() : "missing updateB.txt backup";

            String file0 = "file0";
            assert !(new File(tmpDir, file0).exists()) : "file0 should be deleted";
            String fileA = "fileA";
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
            assert new File(tmpDir, fileB + backupExtension).exists() : "should have fileB backup";
            assert "X".equals(new String(StreamUtil
                .slurp(new FileInputStream(new File(tmpDir, fileB + backupExtension)))));
            String fileC = "dir2" + File.separator + "fileC";
            assert new File(tmpDir, fileC).exists() : "fileC should exist";
            String file4 = "dir3" + File.separator + "dir4" + File.separator + "file4";
            assert new File(tmpDir, file4).exists() : "file4 should exist";
            assert "X".equals(new String(StreamUtil.slurp(new FileInputStream(new File(tmpDir, file1)))));
            assert new File(tmpDir, file2).exists() : "file2 should exist again";
            assert !(new File(tmpDir, file999).exists()) : "file999 should be deleted";
            assert !(new File(tmpDir, file999 + backupExtension).exists()) : "file999 should not be backed up";
            String file3 = "dir2" + File.separator + "file3";
            assert new File(tmpDir, file3).exists() : "file3 should exist";

            StreamUtil.copy(new ByteArrayInputStream("Y".getBytes()), new FileOutputStream(updaterBabsolute));

            deploymentProps = new DeploymentProperties(3, "testbundle2", "3.0.test", null);
            zipFiles = new HashSet<File>(1);
            zipFiles.add(testZipFile2);
            rawFiles = new HashMap<File, File>(2);
            rawFiles.put(testRawFileA, updaterAabsolute); // source raw file to absolute path
            rawFiles.put(testRawFileBChange1, updaterBabsolute); // source raw file to absolute path
            deployer = new Deployer(deploymentProps, zipFiles, rawFiles, destDir, filesToRealizeRegex, templateEngine,
                ignoreRegex);
            deployer.deploy();

            if (ignore) {
                assert "boo".equals(new String(StreamUtil.slurp(new FileInputStream(fileToIgnore))));
            }

            assert new File(updaterBabsoluteBackup).exists() : "updaterB should be backed up";
            assert "B1prime".equals(new String(StreamUtil.slurp(new FileInputStream(updaterBabsolute))));
            assert "Y".equals(new String(StreamUtil.slurp(new FileInputStream(new File(updaterBabsoluteBackup)))));

            StreamUtil.copy(new ByteArrayInputStream("Aprime".getBytes()), new FileOutputStream(updaterAabsolute));

            deploymentProps = new DeploymentProperties(4, "testbundle2", "4.0.test", null);
            zipFiles = new HashSet<File>(1);
            zipFiles.add(testZipFile2);
            rawFiles = new HashMap<File, File>(2);
            rawFiles.put(testRawFileAChange, updaterAabsolute); // source raw file to absolute path
            rawFiles.put(testRawFileBChange2, updaterBabsolute); // source raw file to absolute path
            deployer = new Deployer(deploymentProps, zipFiles, rawFiles, destDir, filesToRealizeRegex, templateEngine,
                ignoreRegex);
            deployer.deploy();

            if (ignore) {
                assert "boo".equals(new String(StreamUtil.slurp(new FileInputStream(fileToIgnore))));
            }

            assert "Aprime".equals(new String(StreamUtil.slurp(new FileInputStream(updaterAabsolute))));
            assert "B2prime".equals(new String(StreamUtil.slurp(new FileInputStream(updaterBabsolute))));

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
            Set<File> zipFiles = null;
            Map<File, File> rawFiles = new HashMap<File, File>();
            rawFiles.put(testRawFileA, new File("rawA.txt")); // we will _not_ realize this one
            rawFiles.put(testRawFileB, rawFileDestination); // we will realize this one
            File destDir = tmpDir;
            Pattern ignoreRegex = null;

            Deployer deployer = new Deployer(deploymentProps, zipFiles, rawFiles, destDir, filesToRealizeRegex,
                templateEngine, ignoreRegex);
            FileHashcodeMap map = deployer.deploy();

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
            Set<File> zipFiles = new HashSet<File>(2);
            zipFiles.add(testZipFile1);
            zipFiles.add(testZipFile2);
            Map<File, File> rawFiles = new HashMap<File, File>();
            rawFiles.put(testRawFileA, new File("dirA/rawA.txt")); // we will _not_ realize this one
            rawFiles.put(testRawFileB, new File("dir100/rawB.txt")); // we will realize this one
            File destDir = tmpDir;
            Pattern ignoreRegex = null;

            Deployer deployer = new Deployer(deploymentProps, zipFiles, rawFiles, destDir, filesToRealizeRegex,
                templateEngine, ignoreRegex);
            deployer.deploy();

            FileHashcodeMap map = FileHashcodeMap.generateFileHashcodeMap(destDir, null);
            assert map.size() == 13 : map;
            String f = "dir1" + File.separator + "file1";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir1" + File.separator + "file2";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir2" + File.separator + "file3";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir3" + File.separator + "dir4" + File.separator + "file4";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "fileA";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir1" + File.separator + "fileB";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir2" + File.separator + "fileC";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "fileAAA";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir100" + File.separator + "file100";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir100" + File.separator + "file200";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir100" + File.separator + "fileBBB";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dirA" + File.separator + "rawA.txt";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert MessageDigestGenerator.getDigestString(testRawFileA).equals(map.get(f)) : "should have same hash, we didn't realize this one";
            f = "dir100" + File.separator + "rawB.txt";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            assert !MessageDigestGenerator.getDigestString(testRawFileB).equals(map.get(f)) : "should have different hash, we realized this one";

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
            Set<File> zipFiles = new HashSet<File>(1);
            zipFiles.add(testZipFile1);
            Map<File, File> rawFiles = null;
            File destDir = tmpDir;
            Pattern ignoreRegex = null;

            Deployer deployer = new Deployer(deploymentProps, zipFiles, rawFiles, destDir, filesToRealizeRegex,
                templateEngine, ignoreRegex);
            deployer.deploy();

            FileHashcodeMap map = FileHashcodeMap.generateFileHashcodeMap(destDir, null);
            assert map.size() == 7 : map;
            String f = "dir1" + File.separator + "file1";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir1" + File.separator + "file2";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir2" + File.separator + "file3";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir3" + File.separator + "dir4" + File.separator + "file4";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "fileA";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir1" + File.separator + "fileB";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));
            f = "dir2" + File.separator + "fileC";
            assert map.containsKey(f) : map;
            assert new File(tmpDir, f).exists();
            assert MessageDigestGenerator.getDigestString(new File(tmpDir, f)).equals(map.get(f));

        } finally {
            FileUtil.purge(tmpDir, true);
        }
    }
}