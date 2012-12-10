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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.OperatingSystem;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.rhq.core.util.stream.StreamUtil;

/**
 * @author Thomas Segismont
 */
public class ProcessInfoRefreshIntervalTest {

    private static final Log LOG = LogFactory.getLog(ProcessInfoRefreshIntervalTest.class);

    private static final String KNOWN_ARG = "hellosigar";

    private Process testProcess;

    private ProcessInfo testProcessInfo;

    private ExecutorService executorService;

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
        int threadCount = Runtime.getRuntime().availableProcessors();
        executorService = Executors.newFixedThreadPool(threadCount);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Created an executor service with " + threadCount + " threads");
        }
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
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    /**
     * We want to be sure that conccurent calls to refresh method will result in correct process state
     * detection - see comments in {@link ProcessInfo#refresh()}.
     * @throws Exception
     */
    @Test(timeOut = 1000 * 60)
    public void testRefreshInterval() throws Exception {
        // Create and execute tasks which will ask a freshSnapshot of the processInfo and check if it is running
        List<Future<Boolean>> futures = submitStateTestingTasks();
        for (Future<Boolean> future : futures) {
            // All tasks should see the process running
            assertTrue(future.get());
        }
        // Send kill
        testProcess.destroy();
        // Wait for death
        while (isAlive(testProcess)) {
            Thread.sleep(100);
        }
        // Create and execute tasks again
        futures = submitStateTestingTasks();
        for (Future<Boolean> future : futures) {
            // All tasks should now see the process down
            assertFalse(future.get());
        }
    }

    private List<Future<Boolean>> submitStateTestingTasks() {
        List<Future<Boolean>> futures = new LinkedList<Future<Boolean>>();
        for (int i = 0; i < 5; i++) {
            final int index = i;
            futures.add(executorService.submit(new Callable<Boolean>() {

                @Override
                public Boolean call() throws Exception {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Execute state testing task[" + index + "]");
                    }
                    boolean isRunning = testProcessInfo.freshSnapshot().isRunning();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("State testing task[" + index + "] result: " + isRunning);
                    }
                    return isRunning;
                }
            }));
        }
        return futures;
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
