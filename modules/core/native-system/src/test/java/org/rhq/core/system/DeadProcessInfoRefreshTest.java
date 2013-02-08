/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.core.system;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.hyperic.sigar.OperatingSystem;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.rhq.core.util.stream.StreamUtil;

/**
 * @author Thomas Segismont
 */
public class DeadProcessInfoRefreshTest {

    private static final String KNOWN_ARG = "hellosigar";

    private Process testProcess;

    private ProcessInfo testProcessInfo;

    @BeforeTest(alwaysRun = true)
    public void setup() throws Exception {
        testProcess = createTestProcess();
        if (!isAlive(testProcess)) {
            throw new RuntimeException("Test process is not alive");
        }
        NativeSystemInfo systemInfo = new NativeSystemInfo();
        List<ProcessInfo> foundProcesses = systemInfo.getProcesses("arg|" + KNOWN_ARG + "|match=.*");
        if (foundProcesses.size() != 1) {
            throw new RuntimeException("Found " + foundProcesses.size() + " with arg [" + KNOWN_ARG + "]");
        }
        testProcessInfo = foundProcesses.iterator().next();
    }

    private Process createTestProcess() throws Exception {
        if (OperatingSystem.IS_WIN32) {
            // On Windows run a simple bat file which behaves like 'watch echo' 
            File batFile = File.createTempFile("win-watch-echo", ".bat");
            batFile.deleteOnExit();
            InputStream is = getClass().getClassLoader().getResourceAsStream("org/rhq/core/system/win-watch-echo.bat");
            OutputStream os = new FileOutputStream(batFile);
            StreamUtil.copy(is, os, true);
            ProcessBuilder processBuilder = new ProcessBuilder("cmd", batFile.getAbsolutePath(), KNOWN_ARG);
            return processBuilder.start();
        } else {
            // On other systems run a simple bash file which behaves like 'watch echo' 
            File bashFile = File.createTempFile("bash-watch-echo", ".sh");
            bashFile.deleteOnExit();
            InputStream is = getClass().getClassLoader().getResourceAsStream("org/rhq/core/system/bash-watch-echo.sh");
            OutputStream os = new FileOutputStream(bashFile);
            StreamUtil.copy(is, os, true);
            ProcessBuilder processBuilder = new ProcessBuilder("bash", bashFile.getAbsolutePath(), KNOWN_ARG);
            return processBuilder.start();
        }
    }

    @AfterTest(alwaysRun = true)
    public void tearDown() {
        if (testProcess != null) {
            testProcess.destroy();
            testProcess = null;
        }
        testProcessInfo = null;
    }

    /**
     * We want to be sure that once the process has been reported down, subsequent calls to refresh will not report it
     * up. See this thread on VMWare forum: http://communities.vmware.com/message/2187972#2187972
     * @throws Exception
     */
    @Test(timeOut = 1000 * 10)
    public void testRefreshInterval() throws Exception {
        // Sigar should see the process running
        assertTrue(testProcessInfo.freshSnapshot().isRunning());
        // Send kill
        testProcess.destroy();
        // Wait for death
        while (isAlive(testProcess)) {
            Thread.sleep(100);
        }
        // Wait at least two seconds to by-pass Sigar cache
        Thread.sleep(2000);
        assertFalse(testProcessInfo.freshSnapshot().isRunning());
        // It should no longer be necessary to wait two seconds
        for (int i = 0; i < 100; i++) {
            assertFalse(testProcessInfo.freshSnapshot().isRunning());
        }
    }

    private boolean isAlive(Process process) {
        if (process == null) {
            return false;
        }
        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

}
