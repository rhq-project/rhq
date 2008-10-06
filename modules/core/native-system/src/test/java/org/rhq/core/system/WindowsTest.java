 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.system;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.rhq.core.system.windows.RegistryEntry;
import org.rhq.core.system.windows.RegistryEntry.Root;

/**
 * Tests the native layer if running on Windows.
 *
 * @author John Mazzitelli
 */
@Test(groups = "native-system")
public class WindowsTest {
    @AfterMethod
    public void terminateNativeLibrary() {
        try {
            SystemInfoFactory.shutdown();
        } catch (Throwable ignore) {
        }
    }

    /**
     * Will return <code>true</code> if the platform this test is running on is the correct platform needed for the
     * tests.
     *
     * @param  testName the name of the test that is asking to be performed
     *
     * @return <code>true</code> if the platform is the correct one that our tests need
     */
    private boolean isCorrectTestPlatform(String testName) {
        if (System.getProperty("os.name").indexOf("Windows") == -1) {
            System.out.println("~~~ [" + testName + "] Test machine is not the correct platform for this test "
                + "- cannot test the native library");
            return false;
        }

        System.out.println("Running native test: " + testName);
        return true;
    }

    /**
     * Tests accessing the registry.
     */
    public void testRegistry() {
        if (!isCorrectTestPlatform("testRegistry")) {
            return;
        }

        WindowsNativeSystemInfo platform = (WindowsNativeSystemInfo) SystemInfoFactory.createSystemInfo();
        Root root = RegistryEntry.Root.HKEY_LOCAL_MACHINE;
        String baseKey = "SOFTWARE\\Microsoft";
        List<String> keys = platform.getRegistryChildKeys(root, baseKey);

        System.out.println("Child registry keys of: " + keys);
        assert keys.size() > 0;

        for (String key : keys) {
            // find a key that has more than 1 value - I assume SOFTWARE/Microsoft has a key that has more than one value
            String fullKey = baseKey + "\\" + key;
            List<String> names = platform.getRegistryValueNames(root, fullKey);
            if (names.size() > 1) {
                System.out.println("Value names of registry key [" + fullKey + "]: " + names);

                // we found a key that has values - make sure we can get their values and end the test
                for (String name : names) {
                    System.out.print("Value of registry key [" + fullKey + "\\" + name + "]: ");
                    RegistryEntry entry = platform.getRegistryEntry(root, fullKey, name);
                    System.out.println(entry);

                    assert entry.getValue() != null;
                }

                List<RegistryEntry> entries = platform.getRegistryEntries(root, fullKey);
                assert entries.size() == names.size();

                break; // end test - don't bother looking for more keys
            }
        }
    }

    /**
     * Test waiting for a process and killing it if we timeout.
     */
    public void testExecuteWithWaitKill() {
        if (!isCorrectTestPlatform("testExecuteWithWaitKill")) {
            return;
        }

        SystemInfo platform = SystemInfoFactory.createSystemInfo();

        ProcessExecution pe = new ProcessExecution("C:\\WINDOWS\\system32\\sol.exe");
        pe.setKillOnTimeout(true);
        pe.setWaitForCompletion(1500L);

        long before = System.currentTimeMillis();
        ProcessExecutionResults results = platform.executeProcess(pe);
        long after = System.currentTimeMillis();

        assert (after - before) >= 1500L : "Did not wait for 1500ms: Waited=" + (after - before);
        assert results.getError() == null : "Failed to exec process: " + results;
    }

    /**
     * Test executing a process.
     */
    public void testExecute() {
        if (!isCorrectTestPlatform("testExecute")) {
            return;
        }

        SystemInfo platform = SystemInfoFactory.createSystemInfo();
        ProcessExecution pe = new ProcessExecution("C:\\WINDOWS\\system32\\cmd.exe");
        pe.setWorkingDirectory("C:\\");
        pe.setCaptureOutput(true);
        pe.setWaitForCompletion(5000L);
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
     * Tests getting Windows stuff.
     */
    public void testWindows() {
        if (!isCorrectTestPlatform("testWindows")) {
            return;
        }

        assert SystemInfoFactory.isNativeSystemInfoAvailable() : "Native library should be available";
        assert !SystemInfoFactory.isNativeSystemInfoDisabled() : "Native library should not be disabled";

        SystemInfo platform = SystemInfoFactory.createSystemInfo();
        assert platform != null;

        boolean isNative = platform.isNative();
        String hostname = platform.getHostname();
        String operatingSystemName = platform.getOperatingSystemName();
        List<ProcessInfo> processes = platform.getAllProcesses();

        assert isNative : "Should have been native";
        assert platform instanceof NativeSystemInfo : "Should have be given a NativeSystemInfo object";
        assert hostname != null;
        assert operatingSystemName != null;
        assert processes.size() > 0 : "Should be at least 1 process running";

        System.out.println("is-native=" + isNative);
        System.out.println("hostname=" + hostname);
        System.out.println("os name=" + operatingSystemName);
        System.out.println("process count=" + processes.size());

        //      for ( ProcessInfo process : processes )
        //      {
        //         System.out.println( process.getPid() + " " + process.getName() + " : " + process.getCommandLine() );
        //      }
    }

    /**
     * We don't use JBNATIVE anymore, but let's keep this test anyway.
     */
    public void testJBNATIVE16() {
        if (!isCorrectTestPlatform("testJBNATIVE16")) {
            return;
        }

        assert SystemInfoFactory.isNativeSystemInfoAvailable() : "Native library should be available";
        assert !SystemInfoFactory.isNativeSystemInfoDisabled() : "Native library should not be disabled";

        SystemInfo platform = SystemInfoFactory.createSystemInfo();
        assert platform != null;

        boolean isNative = platform.isNative();
        String hostname = platform.getHostname();
        String operatingSystemName = platform.getOperatingSystemName();
        List<ProcessInfo> processes = platform.getAllProcesses();

        assert isNative : "Should have been native";
        assert platform instanceof NativeSystemInfo : "Should have be given a NativeSystemInfo object";
        assert hostname != null;
        assert operatingSystemName != null;
        assert processes.size() > 0 : "Should be at least 1 process running";

        System.out.println("is-native=" + isNative);
        System.out.println("hostname=" + hostname);
        System.out.println("os name=" + operatingSystemName);
        System.out.println("process count=" + processes.size());

        SystemInfoFactory.shutdown();

        platform = SystemInfoFactory.createSystemInfo();
        ProcessExecution pe = new ProcessExecution("C:\\WINDOWS\\system32\\cmd.exe");
        pe.setWorkingDirectory("C:\\");
        pe.setCaptureOutput(true);
        pe.setWaitForCompletion(5000L);
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
}