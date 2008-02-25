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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.Swap;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Tests the Native System stuff.
 *
 * @author John Mazzitelli
 */
@Test(groups = "native-system")
public class NativeSystemInfoTest {
    @AfterMethod
    public void terminateNativeLibrary() {
        try {
            SystemInfoFactory.shutdown();
        } catch (Throwable ignore) {
        }
    }

    /**
     * Tests getting network adapter information.
     */
    public void testGetNetworkAdapterInfo() {
        SystemInfo sysinfo = SystemInfoFactory.createSystemInfo();

        if (!sysinfo.isNative()) {
            System.out.println("~~~ Native library is not available - skipping testGetNetworkAdapterInfo");
            return;
        }

        List<NetworkAdapterInfo> adapters = sysinfo.getAllNetworkAdapters();
        assert adapters != null;
        assert adapters.size() > 0 : "You can only run this on a machine with at least one detectable network interface";

        for (NetworkAdapterInfo adapter : adapters) {
            assert adapter.getName() != null : adapter;
            assert adapter.getDisplayName() != null : adapter;
            assert adapter.getDescription() != null : adapter;

            //assert adapter.getMacAddressString() != null : adapter; // looks like loopbacks don't have mac addresses, who else doesn't?
            assert adapter.getType() != null : adapter;
            assert adapter.getOperationalStatus() != null : adapter;

            //assert adapter.isDhcpEnabled() != null : adapter; // SIGAR doesn't support this yet
            //assert adapter.getDnsServers() != null : adapter; // SIGAR doesn't support this yet
            assert adapter.getUnicastAddresses() != null : adapter;
            assert adapter.getMulticastAddresses() != null : adapter;

            // SIGAR doesn't support this yet
            /*
             * for ( InetAddress addr : adapter.getDnsServers() ) { assert addr.getAddress() != null; assert
             * addr.getHostAddress() != null; }
             */

            for (InetAddress addr : adapter.getUnicastAddresses()) {
                assert addr.getAddress() != null;
                assert addr.getHostAddress() != null;
            }

            for (InetAddress addr : adapter.getMulticastAddresses()) {
                assert addr.getAddress() != null;
                assert addr.getHostAddress() != null;
            }

            System.out.println("Network adapter found: " + adapter);
        }
    }

    /**
     * Tests getting service info.
     *
     * @throws Exception
     */
    public void testGetAllServices() throws Exception {
        final SystemInfo sysinfo = SystemInfoFactory.createSystemInfo();

        if (!sysinfo.isNative()) {
            System.out.println("~~~ Native library is not available - skipping testGetAllServices");
            return;
        }

        if (sysinfo.getOperatingSystemType() != OperatingSystemType.WINDOWS) {
            System.out.println("~~~ Native library is not windows - skipping testGetAllServices");
            return;
        }

        List<ServiceInfo> allServices = sysinfo.getAllServices();

        //System.out.println( "All installed services: " + allServices );

        assert allServices != null;
        assert allServices.size() > 0;
    }

    public void testNativeMemory() throws Exception {
        Sigar sigar = new Sigar();
        try {
            Mem allMemory = sigar.getMem();
            Swap allSwap = sigar.getSwap();

            NumberFormat nf = NumberFormat.getNumberInstance();
            System.out.println("All Memory:" + "\nActualFreeMem=" + nf.format(allMemory.getActualFree())
                + "\nActualUsedMem=" + nf.format(allMemory.getActualUsed()) + "\n       RamMem="
                + nf.format(allMemory.getRam()) + "\n      FreeMem=" + nf.format(allMemory.getFree())
                + "\n      UsedMem=" + nf.format(allMemory.getUsed()) + "\n     TotalMem="
                + nf.format(allMemory.getTotal()) + "\n     FreeSwap=" + nf.format(allSwap.getFree())
                + "\n     UsedSwap=" + nf.format(allSwap.getUsed()) + "\n    TotalSwap="
                + nf.format(allSwap.getTotal()));
            assert allMemory.getActualFree() > 0 : allMemory.getActualFree();
            assert allMemory.getFree() > 0 : allMemory.getFree();
            assert allSwap.getFree() > 0 : allSwap.getFree();
            assert allMemory.getTotal() > 1000000 : allMemory.getTotal();
            assert allSwap.getTotal() > 1000000 : allSwap.getTotal();

            ProcMem processMemory = sigar.getProcMem(sigar.getPid());

            System.out.println("Process Memory:" + "\n  MinorFaults=" + nf.format(processMemory.getMinorFaults())
                + "\n  MajorFaults=" + nf.format(processMemory.getMajorFaults()) + "\n   PageFaults="
                + nf.format(processMemory.getPageFaults()) + "\n     Resident="
                + nf.format(processMemory.getResident()) + "\n        Share=" + nf.format(processMemory.getShare())
                + "\n         Size=" + nf.format(processMemory.getSize()));
            assert processMemory.getResident() > 0 : processMemory.getResident();
            assert processMemory.getSize() > 0 : processMemory.getSize();
        } finally {
            sigar.close();
        }
    }

    /**
     * Tests getting process memory usage information.
     */
    public void testProcessMemory() {
        final SystemInfo sysinfo = SystemInfoFactory.createSystemInfo();

        if (!sysinfo.isNative()) {
            System.out.println("~~~ Native library is not available - skipping testProcessMemory");
            return;
        }

        // just a side test - need to make sure this works
        assert SystemInfoFactory.getNativeSystemInfoVersion() != null;

        Mem allMemory = sysinfo.getMemoryInfo();
        ProcMem procMemory = sysinfo.getThisProcess().getMemory();

        assert allMemory != null;
        assert procMemory != null;
        assert allMemory.getUsed() > procMemory.getSize() : allMemory.getUsed() + "->" + procMemory.getSize();
    }

    /**
     * Try to create alot of native objects to see if we hit memory leaks.
     *
     * @throws Exception
     */
    public void testCreateAlotOfNativeObjects() throws Exception {
        System.out.println("Creating alot of native Who objects");
        for (int i = 0; i < 1000; i++) {
            Sigar sigar = new Sigar();
            sigar.getWhoList();
            sigar.close();
        }

        System.out.println("Creating alot of native Cpu objects");
        for (int i = 0; i < 1000; i++) {
            Sigar sigar = new Sigar();
            sigar.getCpuList();
            sigar.close();
        }

        System.out.println("Creating alot of native FileInfo objects");
        File tmpFile = File.createTempFile("native", "test");
        String tmp = tmpFile.getAbsolutePath();
        try {
            for (int i = 0; i < 500; i++) {
                Sigar sigar = new Sigar();
                sigar.getFileInfo(tmp);
                sigar.close();
            }
        } finally {
            tmpFile.delete();
        }
    }

    /**
     * Make sure we don't crash the VM when we shutdown then reinitialize the native libraries.
     *
     * @throws Exception
     */
    public void testShutdownInitialize() throws Exception {
        Sigar sigar = null;
        try {
            sigar = new Sigar();
            sigar.getCpu();
        } catch (Throwable t) {
            System.out.println("~~~ Native library is not available - cannot test the shutdown/init cycle");
            return;
        } finally {
            if (sigar != null) {
                sigar.close();
            }
        }
    }

    /**
     * Test the ability to disable the native library and then reenable it.
     */
    public void testDisable() {
        SystemInfoFactory.disableNativeSystemInfo();
        assert SystemInfoFactory.isNativeSystemInfoDisabled() : "Should have been disabled";
        assert !SystemInfoFactory.isNativeSystemInfoInitialized() : "Should never have been initialized";

        // we disabled the native stuff
        processJavaSystemInfo(SystemInfoFactory.createSystemInfo());
        assert !SystemInfoFactory.isNativeSystemInfoInitialized() : "Should never have been initialized";

        SystemInfoFactory.enableNativeSystemInfo();
        assert !SystemInfoFactory.isNativeSystemInfoDisabled() : "Should have been re-enabled";
    }

    /**
     * Tests the "non-native" Java system info.
     */
    public void testJava() {
        processJavaSystemInfo(new JavaSystemInfo());
    }

    /**
     * Just a test to run concurrent requests through the native layer.
     *
     * @throws Exception
     */
    public void testNativeConcurrency() throws Exception {
        final SystemInfo sysinfo = SystemInfoFactory.createSystemInfo();

        if (!sysinfo.isNative()) {
            System.out.println("~~~ Native library is not available - skipping testNativeConcurrency");
            return;
        }

        final int hostname = sysinfo.getHostname().hashCode();
        final int osname = sysinfo.getOperatingSystemName().hashCode();
        final int osver = sysinfo.getOperatingSystemVersion().hashCode();
        final List<Throwable> errors = new Vector<Throwable>();
        final int[] count = new int[] { 0 };

        class TestConcurrencyTask implements Callable<Object> {
            public Object call() {
                try {
                    synchronized (count) {
                        count[0]++;
                    }

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
        int numTasks = 2000;
        int numConcurrency = 500;

        Collection<Callable<Object>> tasks = new ArrayList<Callable<Object>>(numTasks);
        for (int i = 0; i < numTasks; i++) {
            tasks.add(new TestConcurrencyTask());
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
     * Tests getting stuff if there is no native library available (only can test if this is running on a platform that
     * doesn't have the native libraries in java.library.path)
     */
    public void testNonNative() {
        if (SystemInfoFactory.isNativeSystemInfoAvailable()) {
            System.out.println("~~~ Native library is available - cannot test the fallback-to-Java feature");
            return;
        }

        // since there is no native libraries available, this should fallback to the Java implementation
        processJavaSystemInfo(SystemInfoFactory.createSystemInfo());
    }

    /**
     * Test for memory leaks in the native layer.
     */
    public void testMemoryLeak() {
        final SystemInfo sysinfo = SystemInfoFactory.createSystemInfo();

        if (!sysinfo.isNative()) {
            System.out.println("~~~ Native library is not available - skipping testMemoryLeak");
            return;
        }

        // get memory
        MemoryLeakChecker.checkForMemoryLeak(new Runnable() {
            public void run() {
                sysinfo.getMemoryInfo();
            }
        }, "native-getMemory", 10, 2500, MemoryLeakChecker.JAVA_AND_NATIVE);

        // get operating system name/version
        MemoryLeakChecker.checkForMemoryLeak(new Runnable() {
            public void run() {
                sysinfo.getOperatingSystemName();
                sysinfo.getOperatingSystemVersion();
            }
        }, "native-getOSName/Version", 10, 2500, MemoryLeakChecker.JAVA_AND_NATIVE);

        // get hostname
        MemoryLeakChecker.checkForMemoryLeak(new Runnable() {
            public void run() {
                sysinfo.getHostname();
            }
        }, "native-getHostname", 5, 250, MemoryLeakChecker.JAVA_AND_NATIVE);

        // execute processes
        MemoryLeakChecker.checkForMemoryLeak(new Runnable() {
            public void run() {
                ProcessExecution start = new ProcessExecution("");
                setupProgram(start);
                sysinfo.executeProcess(start);
            }
        }, "native-executeProcess", 4, 50, MemoryLeakChecker.JAVA_AND_NATIVE);

        // get process table information
        MemoryLeakChecker.checkForMemoryLeak(new Runnable() {
            public void run() {
                sysinfo.getAllProcesses();
            }
        }, "native-getAllProcesses", 4, 5, MemoryLeakChecker.JAVA_AND_NATIVE);
    }

    /**
     * Just a test to run concurrent requests through the java layer that executes processes.
     *
     * @throws Exception
     */
    public void testNativeConcurrencyExec() throws Exception {
        final SystemInfo sysinfo = SystemInfoFactory.createSystemInfo();

        if (!sysinfo.isNative()) {
            System.out.println("~~~ Native library is not available - skipping testNativeConcurrencyExec");
            return;
        }

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
        int numTasks = 50;
        int numConcurrency = 30;

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
        start.setWaitForCompletion(5000L);

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

    /**
     * Performs some tests on the given system info, where the sysinfo must be of type {@link JavaSystemInfo}, otherwise
     * the test will fail.
     *
     * @param sysinfo
     */
    private void processJavaSystemInfo(SystemInfo sysinfo) {
        boolean isNative = sysinfo.isNative();
        String hostname = sysinfo.getHostname();
        String operatingSystemName = sysinfo.getOperatingSystemName();

        assert sysinfo instanceof JavaSystemInfo : "The SystemInfo is not the Java fallback implementation: "
            + sysinfo.getClass();

        try {
            sysinfo.getAllProcesses();
            assert false : "Java sysinfo should not have been able to give us process table information";
        } catch (UnsupportedOperationException ok) {
        }

        assert !isNative : "Should not have been native";
        assert !(sysinfo instanceof NativeSystemInfo) : "Should not have been in the NativeSystemInfo hierarchy";
        assert hostname != null;
        assert operatingSystemName != null;

        System.out.println("JAVA: is-native=" + isNative);
        System.out.println("JAVA: hostname=" + hostname);
        System.out.println("JAVA: os name=" + operatingSystemName);
    }
}