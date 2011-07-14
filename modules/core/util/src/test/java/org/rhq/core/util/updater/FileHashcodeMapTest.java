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
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

@Test
public class FileHashcodeMapTest {

    // earlier we supported windows separators in the  map, but i think we want to always support /
    // this constant is here in case I want to move back to supporting windows paths explicitly
    // we'd do this by "fileSeparator = File.separator;"
    private static final String fileSeparator = "/";

    public void testError() throws Exception {
        try {
            FileHashcodeMap.generateFileHashcodeMap(new File("this/should/not/exist"), null, null);
            assert false : "should have thrown exception due to invalid directory";
        } catch (Exception ok) {
            // expected and ok
        }
    }

    public void testRescan() throws Exception {
        File absPathFile = null;
        File tmpDir = FileUtil.createTempDirectory("fileHashcodeMapTest", ".dir", null);
        try {
            File testFile1 = new File(tmpDir, "test1.txt");
            File testFile2 = new File(tmpDir, "test2.txt");
            File ignoreFile1 = new File(tmpDir, "ignoreme1.txt");
            Pattern ignoreRegex = Pattern.compile("ignoreme.*");

            StreamUtil.copy(new ByteArrayInputStream("test1".getBytes()), new FileOutputStream(testFile1));
            StreamUtil.copy(new ByteArrayInputStream("test2".getBytes()), new FileOutputStream(testFile2));
            StreamUtil.copy(new ByteArrayInputStream("ignore1".getBytes()), new FileOutputStream(ignoreFile1));

            Set<String> ignored = new HashSet<String>();
            FileHashcodeMap originalMap = FileHashcodeMap.generateFileHashcodeMap(tmpDir, ignoreRegex, ignored);
            assert originalMap.size() == 2 : originalMap;
            assert originalMap.containsKey("test1.txt") : originalMap;
            assert originalMap.containsKey("test2.txt") : originalMap;
            assert ignored.size() == 1 : ignored;
            assert ignored.contains("ignoreme1.txt") : ignored;

            // first test - see that no changes can be detected
            ChangesFileHashcodeMap currentMap = originalMap.rescan(tmpDir, ignoreRegex, true);
            assertSameMap(originalMap, currentMap);
            assert currentMap.getAdditions().isEmpty();
            assert currentMap.getDeletions().isEmpty();
            assert currentMap.getChanges().isEmpty();
            assert currentMap.getIgnored().size() == 1 : currentMap;
            assert currentMap.getIgnored().contains("ignoreme1.txt") : currentMap;
            assert currentMap.getSkipped().size() == 0;

            // second test - change an original file
            StreamUtil.copy(new ByteArrayInputStream("test1-change".getBytes()), new FileOutputStream(testFile1));
            currentMap = originalMap.rescan(tmpDir, ignoreRegex, true);
            assert currentMap.size() == 2 : currentMap;
            assert currentMap.containsKey("test1.txt") : currentMap;
            assert currentMap.containsKey("test2.txt") : currentMap;
            assert !currentMap.get("test1.txt").equals(originalMap.get("test1.txt")) : currentMap + ":" + originalMap;
            assert currentMap.get("test2.txt").equals(originalMap.get("test2.txt")) : currentMap + ":" + originalMap;
            assert currentMap.getAdditions().isEmpty();
            assert currentMap.getDeletions().isEmpty();
            assert currentMap.getChanges().size() == 1;
            assert currentMap.getChanges().get("test1.txt").equals(currentMap.get("test1.txt"));
            assert currentMap.getIgnored().size() == 1 : currentMap;
            assert currentMap.getIgnored().contains("ignoreme1.txt") : currentMap;
            assert currentMap.getSkipped().size() == 0;

            // third test - delete an original file
            assert testFile1.delete() : "could not delete file in order to test delete-detection";
            currentMap = originalMap.rescan(tmpDir, ignoreRegex, true);
            assert currentMap.size() == 2 : currentMap;
            assert currentMap.containsKey("test1.txt") : currentMap;
            assert currentMap.containsKey("test2.txt") : currentMap;
            assert currentMap.get("test1.txt").equals(FileHashcodeMap.DELETED_FILE_HASHCODE) : currentMap;
            assert currentMap.get("test2.txt").equals(originalMap.get("test2.txt")) : currentMap + ":" + originalMap;
            assert currentMap.getAdditions().isEmpty();
            assert currentMap.getDeletions().size() == 1;
            assert currentMap.getDeletions().get("test1.txt").equals(currentMap.get("test1.txt"));
            assert currentMap.getChanges().isEmpty();
            assert currentMap.getIgnored().size() == 1 : currentMap.getIgnored();
            assert currentMap.getIgnored().contains("ignoreme1.txt") : currentMap.getIgnored();
            assert currentMap.getSkipped().size() == 0;

            // fourth test - add a new file
            StreamUtil.copy(new ByteArrayInputStream("test1".getBytes()), new FileOutputStream(testFile1));
            File testFile3 = new File(tmpDir, "test3.txt");
            StreamUtil.copy(new ByteArrayInputStream("test3".getBytes()), new FileOutputStream(testFile3));
            currentMap = originalMap.rescan(tmpDir, ignoreRegex, true);
            assert currentMap.size() == 3 : currentMap;
            assert currentMap.containsKey("test1.txt") : currentMap;
            assert currentMap.containsKey("test2.txt") : currentMap;
            assert currentMap.containsKey("test3.txt") : currentMap;
            assert currentMap.get("test1.txt").equals(originalMap.get("test1.txt")) : currentMap + ":" + originalMap;
            assert currentMap.get("test2.txt").equals(originalMap.get("test2.txt")) : currentMap + ":" + originalMap;
            assert currentMap.get("test3.txt").equals(MessageDigestGenerator.getDigestString(testFile3)) : currentMap;
            assert currentMap.getAdditions().size() == 1;
            assert currentMap.getAdditions().get("test3.txt").equals(currentMap.get("test3.txt"));
            assert currentMap.getDeletions().isEmpty();
            assert currentMap.getChanges().isEmpty();
            assert currentMap.getIgnored().size() == 1 : currentMap.getIgnored();
            assert currentMap.getIgnored().contains("ignoreme1.txt") : currentMap.getIgnored();
            assert currentMap.getSkipped().size() == 0;

            // fifth test - concurrently change a file, delete a file add new file and add new file in new directory
            // changed file: testFile1
            // deleted file: testFile2
            // added file: testFile3 (we added it in the previous test, so no need to create it below)
            // added directory/file: testFile4
            // I'll add an "ignoreme" directory just to see that it still gets ignored
            StreamUtil.copy(new ByteArrayInputStream("test1-change".getBytes()), new FileOutputStream(testFile1));
            assert testFile2.delete() : "could not delete file in order to test delete-detection";
            File testFile4 = new File(tmpDir, "subdir/test4.txt");
            assert testFile4.getParentFile().mkdirs() : "could not create new subdirectory for test";
            StreamUtil.copy(new ByteArrayInputStream("test4".getBytes()), new FileOutputStream(testFile4));
            File ignoreFile2 = new File(tmpDir, "ignoreme/ignore2.txt");
            assert ignoreFile2.getParentFile().mkdirs() : "could not create new subdirectory for test";
            StreamUtil.copy(new ByteArrayInputStream("ignore2".getBytes()), new FileOutputStream(ignoreFile2));

            currentMap = originalMap.rescan(tmpDir, ignoreRegex, true);
            assert currentMap.size() == 4 : currentMap;
            assert currentMap.containsKey("test1.txt") : currentMap;
            assert currentMap.containsKey("test2.txt") : currentMap;
            assert currentMap.containsKey("test3.txt") : currentMap;
            assert currentMap.containsKey("subdir" + fileSeparator + "test4.txt") : currentMap;
            assert !currentMap.get("test1.txt").equals(originalMap.get("test1.txt")) : currentMap + ":" + originalMap;
            assert currentMap.get("test2.txt").equals(FileHashcodeMap.DELETED_FILE_HASHCODE) : currentMap;
            assert currentMap.get("test3.txt").equals(MessageDigestGenerator.getDigestString(testFile3)) : currentMap;
            assert currentMap.get("subdir" + fileSeparator + "test4.txt").equals(
                MessageDigestGenerator.getDigestString(testFile4)) : currentMap;
            assert currentMap.getAdditions().size() == 2;
            assert currentMap.getAdditions().get("test3.txt").equals(currentMap.get("test3.txt"));
            assert currentMap.getAdditions().get("subdir" + fileSeparator + "test4.txt").equals(
                currentMap.get("subdir" + fileSeparator + "test4.txt"));
            assert currentMap.getDeletions().size() == 1;
            assert currentMap.getDeletions().get("test2.txt").equals(currentMap.get("test2.txt"));
            assert currentMap.getChanges().size() == 1;
            assert currentMap.getChanges().get("test1.txt").equals(currentMap.get("test1.txt"));
            assert currentMap.getIgnored().size() == 2 : currentMap.getIgnored();
            assert currentMap.getIgnored().contains("ignoreme1.txt") : currentMap.getIgnored();
            assert currentMap.getIgnored().contains("ignoreme") : currentMap.getIgnored();
            assert currentMap.getSkipped().size() == 0;

            // sixth test - starting from 5th test above, add an absolute path file
            absPathFile = File.createTempFile("fileHashcodeMapTestFile", ".test");
            assert absPathFile.isAbsolute() : "this should be absolute for this test: " + absPathFile;
            StreamUtil.copy(new ByteArrayInputStream("abs".getBytes()), new FileOutputStream(absPathFile));
            originalMap.put(absPathFile.getAbsolutePath(), MessageDigestGenerator.getDigestString(absPathFile));

            currentMap = originalMap.rescan(tmpDir, ignoreRegex, true);
            assert currentMap.size() == 5 : currentMap;
            assert currentMap.containsKey("test1.txt") : currentMap;
            assert currentMap.containsKey("test2.txt") : currentMap;
            assert currentMap.containsKey("test3.txt") : currentMap;
            assert currentMap.containsKey("subdir" + fileSeparator + "test4.txt") : currentMap;
            assert currentMap.containsKey(absPathFile.getAbsolutePath()) : currentMap;
            assert !currentMap.get("test1.txt").equals(originalMap.get("test1.txt")) : currentMap + ":" + originalMap;
            assert currentMap.get("test2.txt").equals(FileHashcodeMap.DELETED_FILE_HASHCODE) : currentMap;
            assert currentMap.get("test3.txt").equals(MessageDigestGenerator.getDigestString(testFile3)) : currentMap;
            assert currentMap.get("subdir" + fileSeparator + "test4.txt").equals(
                MessageDigestGenerator.getDigestString(testFile4)) : currentMap;
            assert currentMap.get(absPathFile.getAbsolutePath()).equals(originalMap.get(absPathFile.getAbsolutePath())) : currentMap
                + ":" + originalMap;
            assert currentMap.getAdditions().size() == 2;
            assert currentMap.getAdditions().get("test3.txt").equals(currentMap.get("test3.txt"));
            assert currentMap.getAdditions().get("subdir" + fileSeparator + "test4.txt").equals(
                currentMap.get("subdir" + fileSeparator + "test4.txt"));
            assert currentMap.getDeletions().size() == 1;
            assert currentMap.getDeletions().get("test2.txt").equals(currentMap.get("test2.txt"));
            assert currentMap.getChanges().size() == 1;
            assert currentMap.getChanges().get("test1.txt").equals(currentMap.get("test1.txt"));
            assert currentMap.getIgnored().size() == 2 : currentMap.getIgnored();
            assert currentMap.getIgnored().contains("ignoreme1.txt") : currentMap.getIgnored();
            assert currentMap.getIgnored().contains("ignoreme") : currentMap.getIgnored();
            assert currentMap.getSkipped().size() == 0;

            // seventh test - detect that the absolute path file has changed
            StreamUtil.copy(new ByteArrayInputStream("abs-changed".getBytes()), new FileOutputStream(absPathFile));
            currentMap = originalMap.rescan(tmpDir, ignoreRegex, true);
            assert currentMap.size() == 5 : currentMap;
            assert currentMap.containsKey("test1.txt") : currentMap;
            assert currentMap.containsKey("test2.txt") : currentMap;
            assert currentMap.containsKey("test3.txt") : currentMap;
            assert currentMap.containsKey("subdir" + fileSeparator + "test4.txt") : currentMap;
            assert currentMap.containsKey(absPathFile.getAbsolutePath()) : currentMap;
            assert !currentMap.get("test1.txt").equals(originalMap.get("test1.txt")) : currentMap + ":" + originalMap;
            assert currentMap.get("test2.txt").equals(FileHashcodeMap.DELETED_FILE_HASHCODE) : currentMap;
            assert currentMap.get("test3.txt").equals(MessageDigestGenerator.getDigestString(testFile3)) : currentMap;
            assert currentMap.get("subdir" + fileSeparator + "test4.txt").equals(
                MessageDigestGenerator.getDigestString(testFile4)) : currentMap;
            assert !currentMap.get(absPathFile.getAbsolutePath())
                .equals(originalMap.get(absPathFile.getAbsolutePath())) : currentMap + ":" + originalMap;
            assert currentMap.getAdditions().size() == 2;
            assert currentMap.getAdditions().get("test3.txt").equals(currentMap.get("test3.txt"));
            assert currentMap.getAdditions().get("subdir" + fileSeparator + "test4.txt").equals(
                currentMap.get("subdir" + fileSeparator + "test4.txt"));
            assert currentMap.getDeletions().size() == 1;
            assert currentMap.getDeletions().get("test2.txt").equals(currentMap.get("test2.txt"));
            assert currentMap.getChanges().size() == 2;
            assert currentMap.getChanges().get("test1.txt").equals(currentMap.get("test1.txt"));
            assert currentMap.getChanges().get(currentMap.convertPath(absPathFile.getAbsolutePath())).equals(
                currentMap.get(absPathFile.getAbsolutePath()));
            assert currentMap.getIgnored().size() == 2 : currentMap.getIgnored();
            assert currentMap.getIgnored().contains("ignoreme1.txt") : currentMap.getIgnored();
            assert currentMap.getIgnored().contains("ignoreme") : currentMap.getIgnored();
            assert currentMap.getSkipped().size() == 0;

            // eighth test - detect absolute path file has been deleted 
            assert absPathFile.delete() : "could not delete the absolute path file for testing";
            currentMap = originalMap.rescan(tmpDir, ignoreRegex, true);
            assert currentMap.size() == 5 : currentMap;
            assert currentMap.containsKey("test1.txt") : currentMap;
            assert currentMap.containsKey("test2.txt") : currentMap;
            assert currentMap.containsKey("test3.txt") : currentMap;
            assert currentMap.containsKey("subdir" + fileSeparator + "test4.txt") : currentMap;
            assert currentMap.containsKey(absPathFile.getAbsolutePath()) : currentMap;
            assert !currentMap.get("test1.txt").equals(originalMap.get("test1.txt")) : currentMap + ":" + originalMap;
            assert currentMap.get("test2.txt").equals(FileHashcodeMap.DELETED_FILE_HASHCODE) : currentMap;
            assert currentMap.get("test3.txt").equals(MessageDigestGenerator.getDigestString(testFile3)) : currentMap;
            assert currentMap.get("subdir" + fileSeparator + "test4.txt").equals(
                MessageDigestGenerator.getDigestString(testFile4)) : currentMap;
            assert currentMap.get(absPathFile.getAbsolutePath()).equals(FileHashcodeMap.DELETED_FILE_HASHCODE) : currentMap;
            assert currentMap.getAdditions().size() == 2;
            assert currentMap.getAdditions().get("test3.txt").equals(currentMap.get("test3.txt"));
            assert currentMap.getAdditions().get("subdir" + fileSeparator + "test4.txt").equals(
                currentMap.get("subdir" + fileSeparator + "test4.txt"));
            assert currentMap.getDeletions().size() == 2;
            assert currentMap.getDeletions().get("test2.txt").equals(currentMap.get("test2.txt"));
            assert currentMap.getDeletions().get(currentMap.convertPath(absPathFile.getAbsolutePath())).equals(
                currentMap.get(absPathFile.getAbsolutePath()));
            assert currentMap.getChanges().size() == 1;
            assert currentMap.getChanges().get("test1.txt").equals(currentMap.get("test1.txt"));
            assert currentMap.getIgnored().size() == 2 : currentMap.getIgnored();
            assert currentMap.getIgnored().contains("ignoreme1.txt") : currentMap.getIgnored();
            assert currentMap.getIgnored().contains("ignoreme") : currentMap.getIgnored();
            assert currentMap.getSkipped().size() == 0;

        } finally {
            FileUtil.purge(tmpDir, true);
            if (absPathFile != null) {
                absPathFile.delete();
            }
        }
    }

    public void testRescanSkipNewRootDirFiles() throws Exception {
        File absPathFile = null;
        File tmpDir = FileUtil.createTempDirectory("fileHashcodeMapTest", ".dir", null);
        try {
            File testFile1 = new File(tmpDir, "test1.txt");
            File testFile2 = new File(tmpDir, "testsubdir/test2.txt");
            File ignoreFile1 = new File(tmpDir, "ignoreme1.txt");
            Pattern ignoreRegex = Pattern.compile("ignoreme.*");

            assert testFile2.getParentFile().mkdirs() : "could not create testsubdir";

            StreamUtil.copy(new ByteArrayInputStream("test1".getBytes()), new FileOutputStream(testFile1));
            StreamUtil.copy(new ByteArrayInputStream("test2".getBytes()), new FileOutputStream(testFile2));
            StreamUtil.copy(new ByteArrayInputStream("ignore1".getBytes()), new FileOutputStream(ignoreFile1));

            Set<String> ignored = new HashSet<String>();
            FileHashcodeMap originalMap = FileHashcodeMap.generateFileHashcodeMap(tmpDir, ignoreRegex, ignored);
            assert originalMap.size() == 2 : originalMap;
            assert originalMap.containsKey("test1.txt") : originalMap;
            assert originalMap.containsKey("testsubdir/test2.txt") : originalMap;
            assert ignored.size() == 1 : ignored;
            assert ignored.contains("ignoreme1.txt") : ignored;

            // concurrently change a file, delete a file add new file and add new file in new directory
            // changed file: testFile1
            // deleted file: testFile2
            // added file: testFile3 (should not be reported as new, its unrelated)
            // added file in original directory: testsubdir/testFile4 (should be reported, its in related dir "testsubdir")
            // I'll add an "ignoreme" directory just to see that it still gets ignored (technically it will get skipped)
            StreamUtil.copy(new ByteArrayInputStream("test1-change".getBytes()), new FileOutputStream(testFile1));
            assert testFile2.delete() : "could not delete file in order to test delete-detection";
            File testFile3 = new File(tmpDir, "test3.txt");
            StreamUtil.copy(new ByteArrayInputStream("test3".getBytes()), new FileOutputStream(testFile3));
            File testFile4 = new File(tmpDir, "testsubdir/test4.txt");
            StreamUtil.copy(new ByteArrayInputStream("test4".getBytes()), new FileOutputStream(testFile4));
            File ignoreFile2 = new File(tmpDir, "ignoreme/ignore2.txt");
            assert ignoreFile2.getParentFile().mkdirs() : "could not create new subdirectory for test";
            StreamUtil.copy(new ByteArrayInputStream("ignore2".getBytes()), new FileOutputStream(ignoreFile2));

            // now add some more unrelated files/dirs - these should not be reported as new
            File unrelatedFile1 = new File(tmpDir, "unrelated1.txt");
            File unrelatedFile2 = new File(tmpDir, "unrelatedsubdir/unrelated2.txt");
            assert unrelatedFile2.getParentFile().mkdirs() : "could not create unrelated subdir";
            StreamUtil.copy(new ByteArrayInputStream("unrelated1".getBytes()), new FileOutputStream(unrelatedFile1));
            StreamUtil.copy(new ByteArrayInputStream("unrelated2".getBytes()), new FileOutputStream(unrelatedFile2));
            assert unrelatedFile1.exists(); // sanity check
            assert unrelatedFile2.exists(); // sanity check

            ChangesFileHashcodeMap currentMap = originalMap.rescan(tmpDir, ignoreRegex, false);
            assert currentMap.size() == 3 : currentMap;
            assert currentMap.containsKey("test1.txt") : currentMap;
            assert currentMap.containsKey("testsubdir/test2.txt") : currentMap;
            assert !currentMap.containsKey("test3.txt") : currentMap;
            assert currentMap.containsKey("testsubdir" + fileSeparator + "test4.txt") : currentMap;
            assert !currentMap.get("test1.txt").equals(originalMap.get("test1.txt")) : currentMap + ":" + originalMap;
            assert currentMap.get("testsubdir/test2.txt").equals(FileHashcodeMap.DELETED_FILE_HASHCODE) : currentMap;
            assert currentMap.get("testsubdir" + fileSeparator + "test4.txt").equals(
                MessageDigestGenerator.getDigestString(testFile4)) : currentMap;
            assert currentMap.getAdditions().size() == 1;
            assert currentMap.getAdditions().get("testsubdir" + fileSeparator + "test4.txt").equals(
                currentMap.get("testsubdir" + fileSeparator + "test4.txt"));
            assert currentMap.getDeletions().size() == 1;
            assert currentMap.getDeletions().get("testsubdir/test2.txt").equals(currentMap.get("testsubdir/test2.txt"));
            assert currentMap.getChanges().size() == 1;
            assert currentMap.getChanges().get("test1.txt").equals(currentMap.get("test1.txt"));
            // because we are not managing the root dir, those ignore dirs are totally skipped since they are in the root dir
            // therefore, they aren't ignored due to the regex, they are skipped due to reportNewRootFilesAsNew=false
            assert currentMap.getIgnored().size() == 0 : currentMap.getIgnored();
            // now we can look at what was really skipped which were:
            // test3.txt, unrelatedsubdir, ignoreme, unrelated1.txt, ignoreme1.txt
            assert currentMap.getSkipped().size() == 5 : currentMap.getSkipped();
            assert currentMap.getSkipped().contains("test3.txt") : currentMap.getSkipped();
            assert currentMap.getSkipped().contains("ignoreme1.txt") : currentMap.getSkipped();
            assert currentMap.getSkipped().contains("ignoreme") : currentMap.getSkipped();
            assert currentMap.getSkipped().contains("unrelatedsubdir") : currentMap.getSkipped();
            assert currentMap.getSkipped().contains("unrelated1.txt") : currentMap.getSkipped();

        } finally {
            FileUtil.purge(tmpDir, true);
            if (absPathFile != null) {
                absPathFile.delete();
            }
        }
    }

    public void testFileSeparator() throws Exception {
        // today the hashcode files allow either unix or windows file separators (/ or \)
        File tmpFile = File.createTempFile("fileHashcodeMapTest", ".test");
        try {
            PrintWriter writer = new PrintWriter(tmpFile);
            writer.println("directory1\\file1\tabcdef");
            writer.println("directory1/file2\tghijkl");
            writer.close();

            FileHashcodeMap map = FileHashcodeMap.loadFromFile(tmpFile);
            assert map.get("directory1\\file1").equals("abcdef") : map;
            assert map.get("directory1/file2").equals("ghijkl") : map;
            assert map.size() == 2 : map;
        } finally {
            tmpFile.delete();
        }

        FileHashcodeMap map = new FileHashcodeMap();
        map.put("directory1\\file1", "ABCDEF");
        map.put("directory1/file2", "GHIJKL");

        tmpFile = File.createTempFile("fileHashcodeMapTest", ".test");
        try {
            map.storeToFile(tmpFile);
            FileHashcodeMap mapDup = FileHashcodeMap.loadFromFile(tmpFile);
            assertSameMap(map, mapDup);
        } finally {
            tmpFile.delete();
        }
    }

    public void testLoadFile() throws Exception {
        File tmpFile = File.createTempFile("fileHashcodeMapTest", ".test");
        try {
            PrintWriter writer = new PrintWriter(tmpFile);
            writer.println("directory1/file1\tabcdef");
            writer.println("directory1/file2\tghijkl");
            writer.println("directory2/file3\t12345");
            writer.println("file4\t67890");
            writer.close();

            FileHashcodeMap map = FileHashcodeMap.loadFromFile(tmpFile);
            assert map.get("directory1/file1").equals("abcdef") : map;
            assert map.get("directory1/file2").equals("ghijkl") : map;
            assert map.get("directory2/file3").equals("12345") : map;
            assert map.get("file4").equals("67890") : map;
            assert map.size() == 4 : map;
        } finally {
            tmpFile.delete();
        }
    }

    public void testSaveLoadFile() throws Exception {
        FileHashcodeMap map = new FileHashcodeMap();
        map.put("directory1/file1", "ABCDEF");
        map.put("directory1/file2", "GHIJKL");
        map.put("directory2/file3", "54321");
        map.put("file4", "09876");

        File tmpFile = File.createTempFile("fileHashcodeMapTest", ".test");
        try {
            tmpFile.delete(); // got a tmp name, but I want to see this work when the file doesn't exist
            map.storeToFile(tmpFile);

            FileHashcodeMap mapDup = FileHashcodeMap.loadFromFile(tmpFile);
            assertSameMap(map, mapDup);

            assert null == map.getUnknownContent();
            assert null == mapDup.getUnknownContent();
        } finally {
            tmpFile.delete();
        }
    }

    public void testUnknownContent() throws Exception {
        FileHashcodeMap map = new FileHashcodeMap();
        map.put("directory1/file1", "ABCDEF");
        map.put("directory1/file2", FileHashcodeMap.UNKNOWN_FILE_HASHCODE);
        map.put("directory2", FileHashcodeMap.UNKNOWN_DIR_HASHCODE);
        map.put("file4", "09876");

        File tmpFile = File.createTempFile("fileHashcodeMapTest", ".test");
        try {
            map.storeToFile(tmpFile);
            FileHashcodeMap mapDup = FileHashcodeMap.loadFromFile(tmpFile);
            assertSameMap(map, mapDup);

            Map<String, String> unknowns = map.getUnknownContent();
            assert unknowns != null;
            assert unknowns.size() == 2 : unknowns;
            assert unknowns.get("directory1/file2").equals(FileHashcodeMap.UNKNOWN_FILE_HASHCODE) : unknowns;
            assert unknowns.get("directory2").equals(FileHashcodeMap.UNKNOWN_DIR_HASHCODE) : unknowns;
        } finally {
            tmpFile.delete();
        }
    }

    public void testGenerate() throws Exception {

        // pick someplace we know we have something in a relative path
        File fileOrDir = new File("target");
        assert fileOrDir.exists() && fileOrDir.listFiles().length > 0 : "empty dir: " + fileOrDir;

        // now generate hashcodes for all files
        FileHashcodeMap map = FileHashcodeMap.generateFileHashcodeMap(fileOrDir, null, null);
        assert map.size() > 0 : "should have generated something from: " + fileOrDir;

        // just check the first entry to see that the generate hash matches what we expect
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        File file = new File(fileOrDir, entry.getKey());
        assert file.exists() : "file should exist: " + file;
        String hashcode = MessageDigestGenerator.getDigestString(file);
        assert hashcode.equals(entry.getValue()) : "file [" + file + "] hash [" + hashcode + "] doesn't match map: "
            + entry.getValue();

        File tmpFile = File.createTempFile("fileHashcodeMapTest", ".test");
        try {
            map.storeToFile(tmpFile);
            FileHashcodeMap mapDup = FileHashcodeMap.loadFromFile(tmpFile);
            assertSameMap(map, mapDup);
        } finally {
            tmpFile.delete();
        }
    }

    public void testIgnore() throws Exception {

        // pick someplace we know we have something in a relative path
        File fileOrDir = new File("target");
        assert fileOrDir.exists() && fileOrDir.listFiles().length > 0 : "empty dir: " + fileOrDir;

        // now ask to generate hashcodes for all files, but ignore everything
        Pattern regex = Pattern.compile(".*");
        Set<String> ignored = new HashSet<String>();
        FileHashcodeMap map = FileHashcodeMap.generateFileHashcodeMap(fileOrDir, regex, ignored);

        assert map.size() == 0 : "should have ignored everything from: " + fileOrDir + ": " + map;
        assert ignored.size() > 0 : "should have ignored some files";
    }

    private void assertSameMap(FileHashcodeMap map1, FileHashcodeMap map2) {
        assert map2.equals(map1) : map1 + "!=" + map2;
        assert map2.size() == map1.size() : map1 + " is not same size as " + map2;
    }
}
