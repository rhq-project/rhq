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
package org.rhq.core.system;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;

/**
 * Tests the Java-only non-native layer.
 *
 * @author John Mazzitelli
 */
@Test(groups = "native-system")
public class JavaTest {
    /**
     * Will return <code>true</code> if this is on Windows platform - this test needs Windows because it will want to
     * execute Windows executables (albeit using Java API).
     *
     * @param  testName the name of the test that is asking to be performed
     *
     * @return <code>true</code> if the platform is the correct one that our tests need
     */
    private boolean isCorrectTestPlatform(String testName) {
        if (System.getProperty("os.name").indexOf("Windows") == -1) {
            System.out.println("~~~ [" + testName + "] Test machine is not the correct platform for this test "
                + "- cannot test the Java non-native API");
            return false;
        }

        System.out.println("Running non-native java test: " + testName);
        return true;
    }

    /**
     * Tests getting network adapter information.
     */
    // since the move to SIGAR, Java-only sysinfo doesn't support network adapters
    public void testGetNetworkAdapterInfo() {
        SystemInfo sysinfo = SystemInfoFactory.createJavaSystemInfo();

        List<NetworkAdapterInfo> adapters = sysinfo.getAllNetworkAdapters();
        assert adapters != null;
        assert adapters.size() > 0 : "You can only run this on a machine with at least one detectable network interface";

        for (NetworkAdapterInfo adapter : adapters) {
            assert adapter.getName() != null : adapter;
            assert adapter.getDisplayName() != null : adapter;
            assert adapter.getDescription() != null : adapter;
            assert adapter.getUnicastAddresses() != null : adapter;
            assert adapter.getMacAddressString() == null : "How did Java find the Mac address? " + adapter;
            assert adapter.getType().equals("unknown") : "How did Java find the type? " + adapter;
            assert adapter.getOperationalStatus() == NetworkAdapterInfo.OperationState.UNKNOWN : "How did Java find the status? "
                + adapter;
            assert adapter.isDhcpEnabled() == null : "How did Java find the DHCP enabled flag? " + adapter;
            assert adapter.getDnsServers() == null : "How did Java find the DNS servers? " + adapter;
            assert adapter.getMulticastAddresses() == null : "How did Java find the multicast addresses? " + adapter;

            for (InetAddress addr : adapter.getUnicastAddresses()) {
                assert addr.getAddress() != null;
                assert addr.getHostAddress() != null;
            }

            System.out.println("Network adapter found: " + adapter);
        }
    }

    /**
     * Just a test to run concurrent requests through the java layer.
     *
     * @throws Exception
     */
    public void testConcurrency() throws Exception {
        final SystemInfo sysinfo = SystemInfoFactory.createJavaSystemInfo();
        final int hostname = sysinfo.getHostname().hashCode();
        final int osname = sysinfo.getOperatingSystemName().hashCode();
        final int osver = sysinfo.getOperatingSystemVersion().hashCode();
        final List<Throwable> errors = new Vector<Throwable>();
        final AtomicInteger count = new AtomicInteger(0);

        class TestConcurrencyTask implements Callable<Object> {
            public Object call() {
                try {
                    count.incrementAndGet();

                    assert sysinfo.getHostname().hashCode() == hostname;
                    assert sysinfo.getOperatingSystemName().hashCode() == osname;
                    assert sysinfo.getOperatingSystemVersion().hashCode() == osver;
                } catch (Throwable t) {
                    errors.add(t);
                }

                return null;
            }
        }

        // run numTasks total tasks, with numConcurrency being the number we run concurrently at any one time
        int numTasks = 100;
        int numConcurrency = 50;

        Collection<Callable<Object>> tasks = new ArrayList<Callable<Object>>(numTasks);
        for (int i = 0; i < numTasks; i++) {
            tasks.add(new TestConcurrencyTask());
        }

        ExecutorService pool = Executors.newFixedThreadPool(numConcurrency);

        System.gc();
        long beforeFree = Runtime.getRuntime().freeMemory();
        long beforeTotal = Runtime.getRuntime().totalMemory();
        System.gc();
        List<Future<Object>> futures = pool.invokeAll(tasks, 120L, TimeUnit.SECONDS);
        for (Future<Object> future : futures) {
            future.get(30L, TimeUnit.SECONDS);
            assert future.isDone() : "A future failed to complete";
        }

        System.gc();
        long afterFree = Runtime.getRuntime().freeMemory();
        long afterTotal = Runtime.getRuntime().totalMemory();

        pool.shutdown();
        assert pool.awaitTermination(120, TimeUnit.SECONDS) : "Wow - needed to wait longer than 120 seconds to finish";
        assert count.get() == numTasks : "For some reason, we didn't run [" + numTasks + "] tasks: " + count.get();
        assert errors.size() == 0 : "Got some errors: " + errors;

        long beforeUsed = beforeTotal - beforeFree;
        long afterUsed = afterTotal - afterFree;

        System.out.println("1 native concurrency mem usage: before(free/total/used):after(free/total/used)\n"
            + beforeFree + '/' + beforeTotal + '/' + beforeUsed + ':' + afterFree + '/' + afterTotal + '/' + afterUsed);

        if (!((afterUsed - beforeUsed) < 500000)) {
            System.out.println("Hmm.. why did the concurrency test leave 500KB memory still used?");
        }

        long firstTestUsed = afterUsed;

        // run it a second time
        count.set(0);
        pool = Executors.newFixedThreadPool(numConcurrency);
        System.gc();
        beforeFree = Runtime.getRuntime().freeMemory();
        beforeTotal = Runtime.getRuntime().totalMemory();
        System.gc();
        futures = pool.invokeAll(tasks, 120L, TimeUnit.SECONDS);
        for (Future<Object> future : futures) {
            future.get(30L, TimeUnit.SECONDS);
            assert future.isDone() : "A future failed to complete";
        }

        System.gc();
        afterFree = Runtime.getRuntime().freeMemory();
        afterTotal = Runtime.getRuntime().totalMemory();

        pool.shutdown();
        assert pool.awaitTermination(120, TimeUnit.SECONDS) : "Wow - needed to wait longer than 120 seconds to finish";
        assert count.get() == numTasks : "For some reason, we didn't run [" + numTasks + "] tasks: " + count.get();
        assert errors.size() == 0 : "Got some errors: " + errors;

        beforeUsed = beforeTotal - beforeFree;
        afterUsed = afterTotal - afterFree;

        System.out.println("2 native concurrency mem usage: before(free/total/used):after(free/total/used)\n"
            + beforeFree + '/' + beforeTotal + '/' + beforeUsed + ':' + afterFree + '/' + afterTotal + '/' + afterUsed);

        if (!((afterUsed - beforeUsed) < 2500000)) {
            System.out.println("Hmm.. why did the concurrency test leave 250KB memory still used?");
        }

        long secondTestUsed = afterUsed;

        if (!(firstTestUsed >= (secondTestUsed - 50000))) {
            System.out.println("Hmm.. why did the second concurrency test leak more than 50K mem?");
        }
    }

    /**
     * Test waiting for a process and killing it if we timeout.
     */
    public void testExecuteWithWaitKill() {
        if (!isCorrectTestPlatform("testExecuteWithWaitKill")) {
            return;
        }

        SystemInfo platform = SystemInfoFactory.createJavaSystemInfo();

        ProcessExecution pe = new ProcessExecution("C:\\WINDOWS\\system32\\sol.exe");
        pe.setKillOnTimeout(true);
        pe.setWaitForCompletion(3000L);

        long before = System.currentTimeMillis();
        ProcessExecutionResults results = platform.executeProcess(pe);
        long after = System.currentTimeMillis();

        assert (after - before) >= 3000L : "Did not wait for 3000ms: Waited=" + (after - before);
        assert results.getError() == null : "Failed to exec process: " + results;
    }

    /**
     * Test executing a process.
     */
    public void testExecute() {
        if (!isCorrectTestPlatform("testExecute")) {
            return;
        }

        SystemInfo platform = SystemInfoFactory.createJavaSystemInfo();
        ProcessExecution pe = new ProcessExecution("C:\\WINDOWS\\system32\\cmd.exe");
        pe.setWorkingDirectory("C:\\");
        pe.setCaptureOutput(true);
        pe.setArguments(new String[] { "/C", "dir" });
        ProcessExecutionResults results = platform.executeProcess(pe);
        assert results.getError() == null : "Failed to exec process: " + results;
        assert results.getExitCode().intValue() == 0 : "Failed to get a 0 exit code: " + results;
        assert results.getCapturedOutput().length() > 0 : "Missing output";

        Map envVars = new HashMap();
        envVars.put("WINTESTVAR", "hello there");
        pe.setEnvironmentVariables(envVars);
        pe.setArguments(new String[] { "/C", "echo", "%WINTESTVAR%" });
        results = platform.executeProcess(pe);
        assert results.getError() == null : "Failed to exec process: " + results;
        assert results.getExitCode().intValue() == 0 : "Failed to get a 0 exit code: " + results;
        assert results.getCapturedOutput().length() > 0 : "Missing output";
        assert results.getCapturedOutput().indexOf("hello there") > -1 : "Not the expected output: "
            + results.getCapturedOutput();
    }

    public void testJBNATIVE16() {
        if (!isCorrectTestPlatform("testJBNATIVE16")) {
            return;
        }

        SystemInfo platform = SystemInfoFactory.createJavaSystemInfo();
        assert platform != null;

        boolean isNative = platform.isNative();
        String hostname = platform.getHostname();
        String operatingSystemName = platform.getOperatingSystemName();

        assert !isNative : "Should not have been native";
        assert platform instanceof JavaSystemInfo : "Should have be given a JavaSystemInfo object";
        assert hostname != null;
        assert operatingSystemName != null;

        System.out.println("is-native=" + isNative);
        System.out.println("hostname=" + hostname);
        System.out.println("os name=" + operatingSystemName);

        platform = SystemInfoFactory.createJavaSystemInfo();
        ProcessExecution pe = new ProcessExecution("C:\\WINDOWS\\system32\\cmd.exe");
        pe.setWorkingDirectory("C:\\");
        pe.setCaptureOutput(true);
        pe.setArguments(new String[] { "/C", "dir" });
        ProcessExecutionResults results = platform.executeProcess(pe);
        assert results.getError() == null : "Failed to exec process: " + results;
        assert results.getExitCode().intValue() == 0 : "Failed to get a 0 exit code: " + results;
        assert results.getCapturedOutput().length() > 0 : "Missing output";

        Map envVars = new HashMap();
        envVars.put("WINTESTVAR", "hello there");
        pe.setEnvironmentVariables(envVars);
        pe.setArguments(new String[] { "/C", "echo", "%WINTESTVAR%" });
        results = platform.executeProcess(pe);
        assert results.getError() == null : "Failed to exec process: " + results;
        assert results.getExitCode().intValue() == 0 : "Failed to get a 0 exit code: " + results;
        assert results.getCapturedOutput().length() > 0 : "Missing output";
        assert results.getCapturedOutput().indexOf("hello there") > -1 : "Not the expected output: "
            + results.getCapturedOutput();
    }

    /**
     * Just a test to run concurrent requests through the java layer that executes processes.
     *
     * @throws Exception
     */
    public void testConcurrencyExec() throws Exception {
        final SystemInfo sysinfo = SystemInfoFactory.createJavaSystemInfo();
        final List<Throwable> errors = new Vector<Throwable>();
        final int[] count = new int[] { 0 };
        final ProcessExecution start = new ProcessExecution("dummy");

        setupProgram(start);

        class TestExecConcurrencyTask implements Callable<Object> {
            public Object call() {
                try {
                    synchronized (count) {
                        count[0]++;
                    }

                    ProcessExecutionResults results = sysinfo.executeProcess(start);
                    if (results.getError() != null) {
                        throw results.getError();
                    }

                    if (start.isCaptureOutput()) {
                        assert results.getCapturedOutput() != null : "no captured output";

                        for (int i = 0; i < 10; i++) {
                            if (results.getCapturedOutput().length() > 0) {
                                break;
                            }

                            Thread.sleep(500L); // give it a chance to slurp the output
                        }

                        assert results.getCapturedOutput().length() > 0 : "captured output is empty";
                    }
                } catch (Throwable t) {
                    errors.add(t);
                }

                return null;
            }
        }

        // run numTasks total tasks, with numConcurrency being the number we run concurrently at any one time
        int numTasks = 25;
        int numConcurrency = 15;

        Collection<Callable<Object>> tasks = new ArrayList<Callable<Object>>(numTasks);
        for (int i = 0; i < numTasks; i++) {
            tasks.add(new TestExecConcurrencyTask());
        }

        ExecutorService pool = Executors.newFixedThreadPool(numConcurrency);

        System.gc();
        long beforeFree = Runtime.getRuntime().freeMemory();
        long beforeTotal = Runtime.getRuntime().totalMemory();
        System.gc();
        pool.invokeAll(tasks, 30L, TimeUnit.SECONDS);
        System.gc();
        long afterFree = Runtime.getRuntime().freeMemory();
        long afterTotal = Runtime.getRuntime().totalMemory();

        pool.shutdown();
        assert pool.awaitTermination(120, TimeUnit.SECONDS) : "Wow - needed to wait longer than 120 seconds to finish";
        assert count[0] == numTasks : "For some reason, we didn't run [" + numTasks + "] tasks: " + count[0];
        assert errors.size() == 0 : "Got some errors: " + errors;

        long beforeUsed = beforeTotal - beforeFree;
        long afterUsed = afterTotal - afterFree;

        System.out.println("1 native concurrency mem usage: before(free/total/used):after(free/total/used)\n"
            + beforeFree + '/' + beforeTotal + '/' + beforeUsed + ':' + afterFree + '/' + afterTotal + '/' + afterUsed);

        if (!((afterUsed - beforeUsed) < 500000)) {
            System.out.println("Hmm.. why did the concurrency test leave 500KB memory still used?");
        }

        long firstTestUsed = afterUsed;

        // run it a second time
        start.setCaptureOutput(true); // see if this causes any problems
        start.setWaitForCompletion(1000L);

        count[0] = 0;
        pool = Executors.newFixedThreadPool(numConcurrency);
        System.gc();
        beforeFree = Runtime.getRuntime().freeMemory();
        beforeTotal = Runtime.getRuntime().totalMemory();
        System.gc();
        pool.invokeAll(tasks, 30L, TimeUnit.SECONDS);
        System.gc();
        afterFree = Runtime.getRuntime().freeMemory();
        afterTotal = Runtime.getRuntime().totalMemory();

        pool.shutdown();
        assert pool.awaitTermination(120, TimeUnit.SECONDS) : "Wow - needed to wait longer than 120 seconds to finish";
        assert count[0] == numTasks : "For some reason, we didn't run [" + numTasks + "] tasks: " + count[0];
        assert errors.size() == 0 : "Got some errors: " + errors;

        beforeUsed = beforeTotal - beforeFree;
        afterUsed = afterTotal - afterFree;

        System.out.println("2 native concurrency mem usage: before(free/total/used):after(free/total/used)\n"
            + beforeFree + '/' + beforeTotal + '/' + beforeUsed + ':' + afterFree + '/' + afterTotal + '/' + afterUsed);

        if (!((afterUsed - beforeUsed) < 2500000)) {
            System.out.println("Hmm.. why did the concurrency test leave 250KB memory still used?");
        }

        long secondTestUsed = afterUsed;

        if (!(firstTestUsed >= (secondTestUsed - 50000))) {
            System.out.println("Hmm.. why did the second concurrency test leak more than 50K mem?");
        }
    }

    /**
     * Picks a program that can be executed on the test platform. This is not fool proof, might need to tweek this in
     * case, for example, some platforms do not have "/bin/ls".
     *
     * @param start
     */
    private void setupProgram(ProcessExecution start) {
        // just pick some short-lived executable
        if (File.separatorChar == '\\') {
            start.setExecutable("C:\\WINDOWS\\system32\\find.exe");
            start.setArguments(new String[] { "/C", "\"yaddayadda\"", "C:\\WINDOWS\\WIN.INI" });
        } else {
            start.setExecutable("/bin/ls");
            start.setArguments(new String[] { "/bin" });
        }

        start.setCaptureOutput(false);
        start.setWaitForCompletion(-1);

        return;
    }
}