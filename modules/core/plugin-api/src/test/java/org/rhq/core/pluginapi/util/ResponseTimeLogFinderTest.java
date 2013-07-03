/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.core.pluginapi.util;

import static org.rhq.core.pluginapi.util.ResponseTimeLogFinder.findResponseTimeLogFileInDirectory;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;

import org.testng.annotations.Test;

/**
 * @author Thomas Segismont
 */
public class ResponseTimeLogFinderTest {

    private static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    @Test
    public void shouldThrowIllegalArgumentExceptionForInvalidParameters() {
        assertThrowsIllegalArgumentException(new Runnable() {
            @Override
            public void run() {
                findResponseTimeLogFileInDirectory(null, null);
            }
        }, "Should fail when args are null");
        assertThrowsIllegalArgumentException(new Runnable() {
            @Override
            public void run() {
                findResponseTimeLogFileInDirectory("/pipo", null);
            }
        }, "Should fail when directory arg is null");
        assertThrowsIllegalArgumentException(new Runnable() {
            @Override
            public void run() {
                findResponseTimeLogFileInDirectory(null, new File(""));
            }
        }, "Should fail when context arg is null");
        assertThrowsIllegalArgumentException(new Runnable() {
            @Override
            public void run() {
                findResponseTimeLogFileInDirectory("pipo", new File(""));
            }
        }, "Should fail when context arg does not start with a slash");
    }

    private void assertThrowsIllegalArgumentException(Runnable runnable, String failureMessage) {
        try {
            runnable.run();
            fail(failureMessage);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException, "Expected instance of IllegalArgumentException but got "
                + e.getClass().getSimpleName());
        }
    }

    @Test
    public void shouldReturnNullWhenDirectoryArgDoesNotExist() {
        File notExistingFile = null;
        for (int i = 0; i < 2000; i++) {
            File f = new File(TMP_DIR, "pipo" + i);
            if (!f.exists()) {
                notExistingFile = f;
                break;
            }
        }
        if (notExistingFile == null) {
            fail("Could not denote a non existing file");
        }
        assertNull(findResponseTimeLogFileInDirectory("/pipo", notExistingFile));
    }

    @Test
    public void shouldReturnNullWhenDirectoryArgIsNotADirectory() throws Exception {
        File tempFile = File.createTempFile("pipo-", ".tmp");
        tempFile.deleteOnExit();
        assertNull(findResponseTimeLogFileInDirectory("/pipo", tempFile));
    }

    @Test
    public void shouldReturnNullWhenNoFileWasFound() throws Exception {
        File logFileDir = createLogFileDir();
        assertNull(findResponseTimeLogFileInDirectory("/pipo", logFileDir));
    }

    @Test
    public void shouldReturnFoundFile() throws Exception {
        File logFileDir = createLogFileDir();
        File logFile = File.createTempFile("log", "pipo_rt.log", logFileDir);
        logFile.deleteOnExit();
        assertNotNull(findResponseTimeLogFileInDirectory("/pipo", logFileDir));
    }

    @Test
    public void shouldReturnNullIfFoundMoreThanOneFile() throws Exception {
        File logFileDir = createLogFileDir();
        File logFile1 = File.createTempFile("log", "pipo_rt.log", logFileDir);
        logFile1.deleteOnExit();
        File logFile2 = File.createTempFile("log", "pipo_rt.log", logFileDir);
        logFile2.deleteOnExit();
        assertNull(findResponseTimeLogFileInDirectory("/pipo", logFileDir));
    }

    @Test
    public void shouldReturnFoundFileForRootContext() throws Exception {
        File logFileDir = createLogFileDir();
        File logFile = File.createTempFile("log", "ROOT_rt.log", logFileDir);
        logFile.deleteOnExit();
        assertNotNull(findResponseTimeLogFileInDirectory("/", logFileDir));
    }

    @Test
    public void shouldReturnFoundFileForSubContexts() throws Exception {
        File logFileDir = createLogFileDir();
        File logFile = File.createTempFile("log", "pipo_molo_molette_rt.log", logFileDir);
        logFile.deleteOnExit();
        assertNotNull(findResponseTimeLogFileInDirectory("/pipo/molo/molette", logFileDir));
    }

    private static File createLogFileDir() throws Exception {
        // Java 1.6 has no temp dir creation util
        File tempDir = File.createTempFile("pipo-", ".tmp");
        if (tempDir.delete() && tempDir.mkdir()) {
            tempDir.deleteOnExit();
            return tempDir;
        }
        throw new RuntimeException("Could not create temp directory");
    }
}
