/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.util.updater;

import static org.rhq.core.util.stream.StreamUtil.copy;
import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * Tests deploying raw files to deployment locations specified with ".." in the path. This will require the deployer
 * code to transform the paths to canonical paths.
 *
 * @author John Mazzitelli
 */
@Test
public class DeployerCanonicalPathTest {

    private static final Log LOG = LogFactory.getLog(DeployerCanonicalPathTest.class);

    private TemplateEngine templateEngine;

    @BeforeClass
    public void beforeClass() {
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("rhq.system.hostname", "localhost");
        tokens.put("rhq.system.sysprop.java.version", System.getProperty("java.version"));

        templateEngine = new TemplateEngine(tokens);
    }

    @BeforeMethod
    public void beforeMethod() {
        System.out.println("\n\n=============== START OF NEW TEST ===============\n");
    }

    // any raw file path (absolute or relative) that contains ".." will be converted to an absolute, canonical path - this is what we test
    public void testInitialDeployRawFilesWithCanonicalPaths() throws Exception {
        File tmpDirDest = FileUtil.createTempDirectory("DeployerCanonicalPathTest", ".dest", null);
        File tmpDirSrc = FileUtil.createTempDirectory("DeployerCanonicalPathTest", ".src", null);
        File rawFileRelativeDest = new File(
            "dir-does-not-existA/../rawA.txt"); // relative to "tmpDirDest" that we just created above
        File rawFileRelativeDest2 = new File(
            "dir-does-not-existA/../../rawA.txt"); // relative to "tmpDirDest" but it takes us above it
        File rawFileAbsoluteDest = new File(System.getProperty("java.io.tmpdir"), "dir-does-not-existB/../rawB.txt");

        try {
            // put some source files in our tmpDirSrc location
            File testRawFileA = new File(tmpDirSrc, "updater-testA.txt");
            File testRawFileA2 = new File(tmpDirSrc, "updater-testA2.txt");
            File testRawFileB = new File(tmpDirSrc, "updater-testB.txt");
            copy(getResourceAsStream("updater-testA.txt"), new FileOutputStream(testRawFileA), true);
            copy(getResourceAsStream("updater-testA.txt"), new FileOutputStream(testRawFileA2), true);
            copy(getResourceAsStream("updater-testB.txt"), new FileOutputStream(testRawFileB), true);

            DeploymentProperties deploymentProps = new DeploymentProperties(0, "testbundle", "1.0.test", null);
            Set<File> zipFiles = null;
            Map<File, File> rawFiles = new HashMap<File, File>(3);
            rawFiles.put(testRawFileA, rawFileRelativeDest); // we will realize this one ...
            rawFiles.put(testRawFileA2, rawFileRelativeDest2); // and this one ...
            rawFiles.put(testRawFileB, rawFileAbsoluteDest); // and we will realize this one, too
            File destDir = tmpDirDest;
            Pattern ignoreRegex = null;
            Set<File> realizeRawFiles = new HashSet<File>(3);
            realizeRawFiles.add(testRawFileA);
            realizeRawFiles.add(testRawFileA2);
            realizeRawFiles.add(testRawFileB);
            DeploymentData dd = new DeploymentData(deploymentProps, zipFiles, rawFiles, tmpDirSrc, destDir, null,
                realizeRawFiles, templateEngine, ignoreRegex, true, null);
            Deployer deployer = new Deployer(dd);
            DeployDifferences diff = new DeployDifferences();
            FileHashcodeMap map = deployer.deploy(diff);
            System.out.println("map-->\n" + map);
            System.out.println("diff->\n" + diff);

            String rawFileRelativeDestAbsolute = FileUtil.normalizePath(new File(tmpDirDest, rawFileRelativeDest.getPath()))
                .getAbsolutePath();
            String rawFileRelativeDestAbsolute2 = FileUtil.normalizePath(new File(tmpDirDest, rawFileRelativeDest2.getPath()))
                .getAbsolutePath();
            String rawFileAbsoluteDestAbsolute = FileUtil.normalizePath(rawFileAbsoluteDest).getAbsolutePath();

            assert map.size() == 3 : map;

            assert map.containsKey("rawA.txt") : map;
            assert new File(rawFileRelativeDestAbsolute).exists();
            assert new File(rawFileRelativeDestAbsolute2).exists();
            assert MessageDigestGenerator.getDigestString(new File(rawFileRelativeDestAbsolute)).equals(
                map.get("rawA.txt"));

            // rawFileRelativeDestAbsolute2 should be treated just like an absolute, external file
            assert MessageDigestGenerator.getDigestString(new File(rawFileRelativeDestAbsolute2)).equals(
                map.get(rawFileRelativeDestAbsolute2));
            assert !MessageDigestGenerator.getDigestString(testRawFileA)
                .equals(map.get("rawA.txt")) : "should have different hash, we realize this one!";

            assert map.containsKey(rawFileAbsoluteDestAbsolute) : map;
            assert new File(rawFileAbsoluteDestAbsolute).exists();
            assert MessageDigestGenerator.getDigestString(new File(rawFileAbsoluteDestAbsolute)).equals(
                map.get(rawFileAbsoluteDestAbsolute));
            assert !MessageDigestGenerator.getDigestString(testRawFileB)
                .equals(map.get(rawFileAbsoluteDestAbsolute)) : "should have different hash, we realized this one";

            assert diff.getAddedFiles().size() == 3 : diff;
            assert diff.getAddedFiles().contains(diff.convertPath("rawA.txt")) : diff;
            assert diff.getAddedFiles().contains(diff.convertPath(rawFileRelativeDestAbsolute2)) : diff;
            assert diff.getAddedFiles().contains(diff.convertPath(rawFileAbsoluteDestAbsolute)) : diff;
            assert diff.getRealizedFiles().size() == 3 : diff;
            assert diff.getRealizedFiles().keySet().contains(diff.convertPath("rawA.txt")) : diff;
            assert diff.getRealizedFiles().keySet().contains(diff.convertPath(rawFileRelativeDestAbsolute2)) : diff;
            assert diff.getRealizedFiles().keySet().contains(diff.convertPath(rawFileAbsoluteDestAbsolute)) : diff;
        } finally {
            FileUtil.purge(tmpDirDest, true);
            FileUtil.purge(tmpDirSrc, true);
            rawFileAbsoluteDest.getCanonicalFile().delete();
        }
    }

    private InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    public void testUpdateDeployRawFileWithRelativePath() throws Exception {
        File tmpDirDest = FileUtil.createTempDirectory("DeployerCanonicalPathTest", ".dest", null);
        File tmpDirSrc = FileUtil.createTempDirectory("DeployerCanonicalPathTest", ".src", null);
        File rawFileRelativeDest = new File(
            "dir-does-not-existA/../rawA.txt"); // relative to "tmpDirDest" that we just created above
        File rawFileRelativeDest2 = new File(
            "dir-does-not-existA/../../rawA.txt"); // relative to "tmpDirDest" but it takes us above it
        File rawFileAbsoluteDest = new File(System.getProperty("java.io.tmpdir"), "dir-does-not-existB/../rawB.txt");

        try {
            // put some source files in our tmpDirSrc location
            File testRawFileA = new File(tmpDirSrc, "updater-testA.txt");
            File testRawFileA2 = new File(tmpDirSrc, "updater-testA2.txt");
            File testRawFileB = new File(tmpDirSrc, "updater-testB.txt");
            copy(getResourceAsStream("updater-testA.txt"), new FileOutputStream(testRawFileA), true);
            copy(getResourceAsStream("updater-testA.txt"), new FileOutputStream(testRawFileA2), true);
            copy(getResourceAsStream("updater-testB.txt"), new FileOutputStream(testRawFileB), true);

            DeploymentProperties deploymentProps = new DeploymentProperties(0, "testbundle", "1.0.test", null);
            Set<File> zipFiles = null;
            Map<File, File> rawFiles = new HashMap<File, File>(3);
            rawFiles.put(testRawFileA, rawFileRelativeDest);
            rawFiles.put(testRawFileA2, rawFileRelativeDest2);
            rawFiles.put(testRawFileB, rawFileAbsoluteDest);
            File destDir = tmpDirDest;
            Pattern ignoreRegex = null;
            DeploymentData dd = new DeploymentData(deploymentProps, zipFiles, rawFiles, tmpDirSrc, destDir, null, null,
                templateEngine, ignoreRegex, true, null);
            Deployer deployer = new Deployer(dd);
            DeployDifferences diff = new DeployDifferences();
            FileHashcodeMap map = deployer.deploy(diff);
            System.out.println("map-->\n" + map);
            System.out.println("diff->\n" + diff);
            assert map.size() == 3 : map;

            // make sure the first raw file is in the dest dir
            String f = rawFileRelativeDest.getPath();
            File destFile = new File(tmpDirDest, f)
                .getCanonicalFile(); // notice f is assumed relative to tmpDirDest, must convert to canonical path
            assert destFile.exists() : destFile;
            FileUtil.writeFile(new ByteArrayInputStream("modifiedR".getBytes()),
                destFile); // change the file so we back it up during update

            // make sure the second raw file, though specified originally as a relative file, is in the external location
            f = rawFileRelativeDest2.getPath();
            destFile = new File(tmpDirDest, f).getCanonicalFile(); // must convert to canonical path
            assert destFile.exists() : destFile;
            FileUtil.writeFile(new ByteArrayInputStream("modifiedR2".getBytes()),
                destFile); // change the file so we back it up during update

            // make sure the third raw file is in the external location
            destFile = rawFileAbsoluteDest.getCanonicalFile(); // must convert to canonical path
            assert destFile.exists() : destFile;
            FileUtil.writeFile(new ByteArrayInputStream("modifiedA".getBytes()),
                destFile); // change the file so we back it up during update

            // UPDATE
            // alter the src files so we backup our changed files
            FileUtil.writeFile(new ByteArrayInputStream("src.modifiedR".getBytes()), testRawFileA);
            FileUtil.writeFile(new ByteArrayInputStream("src.modifiedR2".getBytes()), testRawFileA2);
            FileUtil.writeFile(new ByteArrayInputStream("src.modifiedA".getBytes()), testRawFileB);

            deploymentProps = new DeploymentProperties(1, "testbundle", "2.0.test", null);
            dd = new DeploymentData(deploymentProps, zipFiles, rawFiles, tmpDirSrc, destDir, null, null,
                templateEngine, ignoreRegex, true, null);
            deployer = new Deployer(dd);
            diff = new DeployDifferences();
            map = deployer.deploy(diff);
            System.out.println("map-->\n" + map);
            System.out.println("diff->\n" + diff);

            String rawFileRelativeDestAbsolute = FileUtil.normalizePath(new File(tmpDirDest, rawFileRelativeDest.getPath()))
                .getAbsolutePath();
            String rawFileRelativeDestAbsolute2 = FileUtil.normalizePath(new File(tmpDirDest, rawFileRelativeDest2.getPath()))
                .getAbsolutePath();
            String rawFileAbsoluteDestAbsolute = FileUtil.normalizePath(rawFileAbsoluteDest).getAbsolutePath();

            assert new String(StreamUtil.slurp(new FileInputStream(new File(rawFileRelativeDestAbsolute))))
                .equals("src.modifiedR");
            assert new String(StreamUtil.slurp(new FileInputStream(new File(rawFileRelativeDestAbsolute2))))
                .equals("src.modifiedR2");
            assert new String(StreamUtil.slurp(new FileInputStream(new File(rawFileAbsoluteDestAbsolute))))
                .equals("src.modifiedA");

            boolean isWindows = File.separatorChar == '\\';
            final File metadir = new File(tmpDirDest, ".rhqdeployments");
            File backupRel = new File(metadir, "1/backup/rawA.txt");
            File backupRel2;
            // test the second raw file, the one that was specified originally as a relative file but took us out of the dest dir
            if (!isWindows) {
                backupRel2 = new File(metadir, "1/ext-backup/" + rawFileRelativeDestAbsolute2);
            } else {
                StringBuilder str = new StringBuilder(rawFileRelativeDestAbsolute2);
                String driveLetter = FileUtil.stripDriveLetter(str);
                if (driveLetter != null) {
                    driveLetter = "_" + driveLetter + '/';
                } else {
                    driveLetter = "";
                }
                backupRel2 = new File(metadir, "1/ext-backup/" + driveLetter + str.toString());
            }
            // test the third raw file, the one that was specified originally as an absolute, external file
            File backupAbs;
            if (!isWindows) {
                backupAbs = new File(metadir, "1/ext-backup/" + rawFileAbsoluteDestAbsolute);
            } else {
                StringBuilder str = new StringBuilder(rawFileAbsoluteDestAbsolute);
                String driveLetter = FileUtil.stripDriveLetter(str);
                if (driveLetter != null) {
                    driveLetter = "_" + driveLetter + '/';
                } else {
                    driveLetter = "";
                }
                backupAbs = new File(metadir, "1/ext-backup/" + driveLetter + str.toString());
            }

            // the backup files should exist
            assert backupRel.exists() : backupRel;
            assert backupRel2.exists() : backupRel2;
            assert backupAbs.exists() : backupAbs;

            assert map.size() == 3 : map;
            assert diff.getChangedFiles().size() == 3 : diff;
            assert diff.getChangedFiles().contains(diff.convertPath("rawA.txt")) : diff;
            assert diff.getChangedFiles().contains(diff.convertPath(rawFileRelativeDestAbsolute2)) : diff;
            assert diff.getChangedFiles().contains(diff.convertPath(rawFileAbsoluteDestAbsolute)) : diff;
            assert diff.getDeletedFiles().isEmpty() : diff;
            assert diff.getBackedUpFiles().size() == 3 : diff;
            assert diff.getBackedUpFiles().keySet().contains(diff.convertPath("rawA.txt")) : diff;
            assert diff.getBackedUpFiles().keySet().contains(diff.convertPath(rawFileRelativeDestAbsolute2)) : diff;
            assert diff.getBackedUpFiles().keySet().contains(diff.convertPath(rawFileAbsoluteDestAbsolute)) : diff;
        } finally {
            FileUtil.purge(tmpDirDest, true);
            FileUtil.purge(tmpDirSrc, true);
            rawFileAbsoluteDest.getCanonicalFile().delete();
        }
    }

    @Test(timeOut = 1000l * 60)
    public void testInitialDeploymentGlossesOverSymlinksInParents() throws Exception {
        File lnExecutable = new File("/usr/bin/ln");
        if (!lnExecutable.canExecute()) {
            throw new SkipException("This test is only supported on systems with /usr/bin/ln");
        }

        File root = FileUtil.createTempDirectory("DeployerCanonicalPathTest", ".symlink-root", null);
        File symlinkTarget = FileUtil.createTempDirectory("DeployerCanonicalPathTest", ".symlink-target", null);

        File src = FileUtil.createTempDirectory("DeployerCanonicalPathTest", ".src", null);

        File parent = new File(root, "parent");
        parent.mkdirs();

        try {
            File destination = new File(parent, "destination");
            if (new ProcessBuilder(lnExecutable.getAbsolutePath(), "-s", symlinkTarget.getAbsolutePath(),
                destination.getAbsolutePath()).start().waitFor() != 0) {
                fail("Failed to create symlink");
            }

            // put some source files in our tmpDirSrc location
            File testRawFileA = new File(src, "updater-testA.txt");
            File testRawFileA2 = new File(src, "updater-testA2.txt");
            File testRawFileB = new File(src, "updater-testB.txt");
            File testRawFileADest = new File(destination, "../realDest/rawA.txt");
            File testRawFileA2Dest = new File(destination, "../realDest/rawA2.txt");
            File testRawFileBDest = new File(destination, "../../realDest/rawB.txt");
            copy(getResourceAsStream("updater-testA.txt"), new FileOutputStream(testRawFileA), true);
            copy(getResourceAsStream("updater-testA.txt"), new FileOutputStream(testRawFileA2), true);
            copy(getResourceAsStream("updater-testB.txt"), new FileOutputStream(testRawFileB), true);

         DeploymentProperties deploymentProps = new DeploymentProperties(0, "testbundle", "1.0.test", null);
            Set<File> zipFiles = null;
            Map<File, File> rawFiles = new HashMap<File, File>(3);
            rawFiles.put(testRawFileA, testRawFileADest);
            rawFiles.put(testRawFileA2, testRawFileA2Dest);
            rawFiles.put(testRawFileB, testRawFileBDest);

            DeploymentData dd = new DeploymentData(deploymentProps, zipFiles, rawFiles, src, destination, null, null,
                templateEngine, null, true, null);
            Deployer deployer = new Deployer(dd);
            DeployDifferences diff = new DeployDifferences();
            FileHashcodeMap map = deployer.deploy(diff);
            System.out.println("map-->\n" + map);
            System.out.println("diff->\n" + diff);

            assert map.size() == 3 : map;

            assert new File(parent, "realDest/rawA.txt").isFile() : "rawA.txt not deployed correctly";
            assert new File(parent, "realDest/rawA2.txt").isFile() : "rawA2.txt not deployed correctly";
            assert new File(root, "realDest/rawB.txt").isFile() : "rawB.txt not deployed correctly";

      //the symlink target, being the destination of the deployment should have the .rhqdeployments directory
            //specified. No other files should exist there though.
            assert new File(symlinkTarget, ".rhqdeployments").isDirectory() : "Could not find .rhqdeployments on the expected location";
            assert symlinkTarget.listFiles().length == 1 : "The target of the symlink should have no other files than .rhqdeployments";
        } finally {
            FileUtil.purge(root, true);
            FileUtil.purge(symlinkTarget, true);
            FileUtil.purge(src, true);
        }
    }
}
