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
import java.io.PrintWriter;
import java.util.Map;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

import org.rhq.core.util.MessageDigestGenerator;

@Test
public class FileHashcodeMapTest {
    public void testError() throws Exception {
        try {
            FileHashcodeMap.generateFileHashcodeMap(new File("this/should/not/exist"), null);
            assert false : "should have thrown exception due to invalid directory";
        } catch (Exception ok) {
            // expected and ok
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
        FileHashcodeMap map = FileHashcodeMap.generateFileHashcodeMap(fileOrDir, null);
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
        FileHashcodeMap map = FileHashcodeMap.generateFileHashcodeMap(fileOrDir, regex);

        assert map.size() == 0 : "should have ignored everything from: " + fileOrDir + ": " + map;
    }

    private void assertSameMap(FileHashcodeMap map1, FileHashcodeMap map2) {
        assert map2.equals(map1) : map1 + "!=" + map2;
        assert map2.size() == map1.size() : map1 + " is not same size as " + map2;
    }
}
