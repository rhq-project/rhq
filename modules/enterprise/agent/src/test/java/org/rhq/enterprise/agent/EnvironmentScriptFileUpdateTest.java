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
package org.rhq.enterprise.agent;

import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

import org.testng.annotations.Test;

@Test
public class EnvironmentScriptFileUpdateTest {
    private static final boolean DELETE_TEST_FILES_ON_SUCCESS = true;

    public void testMissingFile() throws Exception {
        File file = File.createTempFile("EnvironmentScriptFileUpdateTest-Missing", ".bat");
        assert file.delete() : "could not delete our tmp file - this test needs a missing file";
        assert !file.exists() : "how is this still here? this test needs a missing file";

        try {
            EnvironmentScriptFileUpdate up = EnvironmentScriptFileUpdate.create(file.getAbsolutePath());
            assert !file.exists() : "should not have created the file yet: " + file;
            assert up.loadExisting().size() == 0 : "There should not be any existing settings: " + file;

            up.update("one", "one value");
            assert file.exists() : "file should be created now: " + file;
            assert file.length() > 0 : "file should have something in it: " + file;
            Properties existing = up.loadExisting();
            assert existing.size() == 1 : "There should 1 setting now: " + file;
            assert existing.getProperty("one").equals("one value") : "Bad one setting: " + file;

            assert file.delete() : "could not delete - this test needs a missing file again: " + file;
            assert !file.exists() : "could not delete - this test needs a missing file: " + file;

            existing.put("two", "two value");
            existing.put("three", "three value");
            existing.remove("one");
            up.update(existing, false);
            assert file.exists() : "file should be created again: " + file;
            assert file.length() > 0 : "file should have something in it again: " + file;
            existing = up.loadExisting();
            assert existing.size() == 2 : "There should be 2 settings now: " + file;
            assert existing.getProperty("two").equals("two value") : "Bad two setting: " + file;
            assert existing.getProperty("three").equals("three value") : "Bad three setting: " + file;
        } finally {
            if (DELETE_TEST_FILES_ON_SUCCESS) {
                file.delete();
            }
        }
    }

    public void testJSWConf() throws Exception {
        File file = setupTestJSWConfFile();
        EnvironmentScriptFileUpdate up = EnvironmentScriptFileUpdate.create(file.getAbsolutePath());
        long originalLength = file.length();
        assert originalLength > 0 : "Test file wasn't written properly: " + file;

        Properties existing = up.loadExisting();
        assert existing.size() == 5 : "Missing test props: " + existing.toString();
        assert "true".equals(existing.getProperty("wrapper.JSW")) : existing.toString();
        assert existing.getProperty("UNIX") == null : "test is mixing up the wrong test file";
        assert existing.getProperty("JSWENV") == null : "test is mixing up the wrong test file";
        assert "onevalue".equals(existing.getProperty("wrapper.ONE")) : existing.toString();
        assert "".equals(existing.getProperty("wrapper.EMPTY")) : existing.toString();
        assert "lastvalue".equals(existing.getProperty("wrapper.LAST")) : existing.toString();
        assert "\"one two three\"".equals(existing.getProperty("wrapper.QUOTED")) : existing.toString();

        up.update("wrapper.testJSWConf", "testJSWConfValue");
        assert file.length() > originalLength : "Should have modified the test file: " + file;
        originalLength = file.length();
        existing = up.loadExisting();
        assert "testJSWConfValue".equals(existing.getProperty("wrapper.testJSWConf")) : "Failed to update: " + existing;

        existing.put("wrapper.quoted2", "\"four five six\"");
        up.update(existing, false);
        assert file.length() > originalLength : "Should have modified the test file: " + file;
        originalLength = file.length();
        existing = up.loadExisting();
        assert "\"four five six\"".equals(existing.getProperty("wrapper.quoted2")) : "Failed to update: " + existing;

        existing.remove("wrapper.quoted2");
        up.update(existing, true);
        assert file.length() < originalLength : "Should have deleted the setting: " + file;
        originalLength = file.length();
        existing = up.loadExisting();
        assert existing.getProperty("wrapper.quoted2") == null : "Failed to delete setting: " + existing;

        if (DELETE_TEST_FILES_ON_SUCCESS) {
            file.delete(); // only delete the file if we succeed - leave the file around on failure for diagnosis
        }
    }

    public void testJSWEnv() throws Exception {
        File file = setupTestJSWEnvFile();
        EnvironmentScriptFileUpdate up = EnvironmentScriptFileUpdate.create(file.getAbsolutePath());
        long originalLength = file.length();
        assert originalLength > 0 : "Test file wasn't written properly: " + file;

        Properties existing = up.loadExisting();
        assert existing.size() == 5 : "Missing test props: " + existing.toString();
        assert "true".equals(existing.getProperty("JSWENV")) : existing.toString();
        assert existing.getProperty("wrapper.JSW") == null : "test is mixing up the wrong test file";
        assert existing.getProperty("UNIX") == null : "test is mixing up the wrong test file";
        assert "onevalue".equals(existing.getProperty("ONE")) : existing.toString();
        assert "".equals(existing.getProperty("EMPTY")) : existing.toString();
        assert "lastvalue".equals(existing.getProperty("LAST")) : existing.toString();
        assert "\"one two three\"".equals(existing.getProperty("QUOTED")) : existing.toString();

        up.update("testJSWEnv", "testJSWEnvValue");
        assert file.length() > originalLength : "Should have modified the test file: " + file;
        originalLength = file.length();
        existing = up.loadExisting();
        assert "testJSWEnvValue".equals(existing.getProperty("testJSWEnv")) : "Failed to update: " + existing;

        existing.put("quoted2", "\"four five six\"");
        up.update(existing, false);
        assert file.length() > originalLength : "Should have modified the test file: " + file;
        originalLength = file.length();
        existing = up.loadExisting();
        assert "\"four five six\"".equals(existing.getProperty("quoted2")) : "Failed to update: " + existing;

        existing.remove("quoted2");
        up.update(existing, true);
        assert file.length() < originalLength : "Should have deleted the setting: " + file;
        originalLength = file.length();
        existing = up.loadExisting();
        assert existing.getProperty("quoted2") == null : "Failed to delete setting: " + existing;

        if (DELETE_TEST_FILES_ON_SUCCESS) {
            file.delete(); // only delete the file if we succeed - leave the file around on failure for diagnosis
        }
    }

    public void testUnix() throws Exception {
        File file = setupTestUnixFile();
        EnvironmentScriptFileUpdate up = EnvironmentScriptFileUpdate.create(file.getAbsolutePath());
        long originalLength = file.length();
        assert originalLength > 0 : "Test file wasn't written properly: " + file;

        Properties existing = up.loadExisting();
        assert existing.size() == 5 : "Missing test props: " + existing.toString();
        assert "true".equals(existing.getProperty("UNIX")) : existing.toString();
        assert existing.getProperty("WINDOWS") == null : "test is mixing up the wrong test file";
        assert "onevalue".equals(existing.getProperty("ONE")) : existing.toString();
        assert "".equals(existing.getProperty("EMPTY")) : existing.toString();
        assert "lastvalue".equals(existing.getProperty("LAST")) : existing.toString();
        assert "\"one two three\"".equals(existing.getProperty("QUOTED")) : existing.toString();

        up.update("testUnix", "testUnixValue");
        assert file.length() > originalLength : "Should have modified the test file: " + file;
        originalLength = file.length();
        existing = up.loadExisting();
        assert "testUnixValue".equals(existing.getProperty("testUnix")) : "Failed to update: " + existing;

        existing.put("quoted2", "\"four five six\"");
        up.update(existing, false);
        assert file.length() > originalLength : "Should have modified the test file: " + file;
        originalLength = file.length();
        existing = up.loadExisting();
        assert "\"four five six\"".equals(existing.getProperty("quoted2")) : "Failed to update: " + existing;

        existing.remove("quoted2");
        up.update(existing, true);
        assert file.length() < originalLength : "Should have deleted the setting: " + file;
        originalLength = file.length();
        existing = up.loadExisting();
        assert existing.getProperty("quoted2") == null : "Failed to delete setting: " + existing;

        if (DELETE_TEST_FILES_ON_SUCCESS) {
            file.delete(); // only delete the file if we succeed - leave the file around on failure for diagnosis
        }
    }

    public void testWindows() throws Exception {
        File file = setupTestWindowsFile();
        EnvironmentScriptFileUpdate up = EnvironmentScriptFileUpdate.create(file.getAbsolutePath());
        long originalLength = file.length();
        assert originalLength > 0 : "Test file wasn't written properly: " + file;

        Properties existing = up.loadExisting();
        assert existing.size() == 5 : "Missing test props: " + existing.toString();
        assert "true".equals(existing.getProperty("WINDOWS")) : existing.toString();
        assert existing.getProperty("UNIX") == null : "test is mixing up the wrong test file";
        assert "onevalue".equals(existing.getProperty("ONE")) : existing.toString();
        assert "".equals(existing.getProperty("EMPTY")) : existing.toString();
        assert "lastvalue".equals(existing.getProperty("LAST")) : existing.toString();
        assert "\"one two three\"".equals(existing.getProperty("QUOTED")) : existing.toString();

        up.update("testWin", "testWinValue");
        assert file.length() > originalLength : "Should have modified the test file: " + file;
        originalLength = file.length();
        existing = up.loadExisting();
        assert "testWinValue".equals(existing.getProperty("testWin")) : "Failed to update: " + existing;

        existing.put("quoted2", "\"four five six\"");
        up.update(existing, false);
        assert file.length() > originalLength : "Should have modified the test file: " + file;
        originalLength = file.length();
        existing = up.loadExisting();
        assert "\"four five six\"".equals(existing.getProperty("quoted2")) : "Failed to update: " + existing;

        existing.remove("quoted2");
        up.update(existing, true);
        assert file.length() < originalLength : "Should have deleted the setting: " + file;
        originalLength = file.length();
        existing = up.loadExisting();
        assert existing.getProperty("quoted2") == null : "Failed to delete setting: " + existing;

        if (DELETE_TEST_FILES_ON_SUCCESS) {
            file.delete(); // only delete the file if we succeed - leave the file around on failure for diagnosis
        }
    }

    private File setupTestUnixFile() throws Exception {
        File file = File.createTempFile("EnvironmentScriptFileUpdateTest", ".sh");
        FileWriter writer = new FileWriter(file);
        try {
            writer.write("  #  comment\n");
            writer.write("  ONE=onevalue \n"); // useless space at end of line
            writer.write("#COMMENTED=not set\n");
            writer.write("\n"); // empty line
            writer.write("EMPTY=\n"); // blank value
            writer.write("UNIX=true\n");
            writer.write("QUOTED=\"one two three\"\n");
            writer.write("LAST=lastvalue"); // no end-of-line newline
            return file;
        } finally {
            writer.close();
        }
    }

    private File setupTestWindowsFile() throws Exception {
        File file = File.createTempFile("EnvironmentScriptFileUpdateTest", ".bat");
        FileWriter writer = new FileWriter(file);
        try {
            writer.write(" @ rem comment\r\n");
            writer.write(" @ set  ONE=onevalue \r\n"); // lots of useless space in here
            writer.write("rem COMMENTED=not set\r\n");
            writer.write("\r\n");
            writer.write("set EMPTY=\r\n");
            writer.write("set WINDOWS=true\r\n");
            writer.write("set QUOTED=\"one two three\"\r\n");
            writer.write("@set LAST=lastvalue\r\n");
            return file;
        } finally {
            writer.close();
        }
    }

    private File setupTestJSWConfFile() throws Exception {
        File file = File.createTempFile("EnvironmentScriptFileUpdateTest", ".conf");
        FileWriter writer = new FileWriter(file);
        try {
            writer.write("  #  comment\n");
            writer.write("  wrapper.ONE=onevalue \n"); // useless space at end of line
            writer.write("#wrapper.COMMENTED=not set\n");
            writer.write("\n"); // empty line
            writer.write("wrapper.EMPTY=\n"); // blank value
            writer.write("wrapper.JSW=true\n");
            writer.write("wrapper.QUOTED=\"one two three\"\n");
            writer.write("wrapper.LAST=lastvalue"); // no end-of-line newline
            return file;
        } finally {
            writer.close();
        }
    }

    private File setupTestJSWEnvFile() throws Exception {
        File file = File.createTempFile("EnvironmentScriptFileUpdateTest", ".env");
        FileWriter writer = new FileWriter(file);
        try {
            writer.write("  #  comment\n");
            writer.write("  set.ONE=onevalue \n"); // useless space at end of line
            writer.write("#set.COMMENTED=not set\n");
            writer.write("\n"); // empty line
            writer.write("set.EMPTY=\n"); // blank value
            writer.write("set.JSWENV=true\n");
            writer.write("set.QUOTED=\"one two three\"\n");
            writer.write("set.LAST=lastvalue"); // no end-of-line newline
            return file;
        } finally {
            writer.close();
        }
    }
}
