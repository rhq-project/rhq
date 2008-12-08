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
    private static final boolean DELETE_TEST_FILES_ON_SUCCESS = false;

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
        up.update(existing);
        assert file.length() > originalLength : "Should have modified the test file: " + file;
        originalLength = file.length();
        existing = up.loadExisting();
        assert "\"four five six\"".equals(existing.getProperty("quoted2")) : "Failed to update: " + existing;

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
        up.update(existing);
        assert file.length() > originalLength : "Should have modified the test file: " + file;
        originalLength = file.length();
        existing = up.loadExisting();
        assert "\"four five six\"".equals(existing.getProperty("quoted2")) : "Failed to update: " + existing;

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
}
