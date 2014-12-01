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

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcCred;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.ProcExe;
import org.hyperic.sigar.ProcFd;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.ProcTime;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * A {@link ProcessInfo} unit test class (mocked SIGAR)
 *
 * @author Thomas Segismont
 */
public class ProcessInfoTest {

    private static final int PID = 15;

    private SigarProxy sigarProxy;

    private ProcExe procExe;

    private ProcState procState;

    private ProcTime procTime;

    private ProcMem procMem;

    private ProcCpu procCpu;

    private ProcFd procFd;

    private ProcCred procCred;

    private ProcCredName procCredName;

    private ProcessInfo processInfo;

    @BeforeMethod(alwaysRun = true)
    protected void setUp() throws Exception {
        sigarProxy = Mockito.mock(SigarProxy.class);
        procExe = Mockito.mock(ProcExe.class);
        when(sigarProxy.getProcExe(PID)).thenReturn(procExe);
        procState = Mockito.mock(ProcState.class);
        // Calls to refresh won't succeed if the process is reported down
        when(procState.getState()).thenReturn(ProcState.RUN);
        when(sigarProxy.getProcState(PID)).thenReturn(procState);
        procTime = Mockito.mock(ProcTime.class);
        when(sigarProxy.getProcTime(PID)).thenReturn(procTime);
        procMem = Mockito.mock(ProcMem.class);
        when(sigarProxy.getProcMem(PID)).thenReturn(procMem);
        procCpu = Mockito.mock(ProcCpu.class);
        when(sigarProxy.getProcCpu(PID)).thenReturn(procCpu);
        procFd = Mockito.mock(ProcFd.class);
        when(sigarProxy.getProcFd(PID)).thenReturn(procFd);
        procCred = Mockito.mock(ProcCred.class);
        when(sigarProxy.getProcCred(PID)).thenReturn(procCred);
        procCredName = Mockito.mock(ProcCredName.class);
        when(sigarProxy.getProcCredName(PID)).thenReturn(procCredName);
        processInfo = new ProcessInfo(PID, sigarProxy);
    }

    @Test
    public void testGetPid() {
        assertEquals(processInfo.getPid(), PID);
    }

    @Test
    public void testGetNameDefault() {
        assertEquals(processInfo.getName(), "?");
    }

    @Test
    public void testGetNameFromArgs() throws Exception {
        // Force new test instance because this test case work with data
        // which is initialized at instance creation
        when(sigarProxy.getProcArgs(PID)).thenReturn(new String[] { "onearg", "somearg", "otherarg" });
        processInfo = new ProcessInfo(PID, sigarProxy);
        assertEquals(processInfo.getName(), "onearg");
    }

    @Test
    public void testGetNameFromExe() throws Exception {
        // Force new test instance because this test case work with data
        // which is initialized at instance creation
        procExe = Mockito.mock(ProcExe.class);
        when(procExe.getName()).thenReturn("aprocname");
        when(sigarProxy.getProcExe(PID)).thenReturn(procExe);
        processInfo = new ProcessInfo(PID, sigarProxy);
        assertEquals(processInfo.getName(), "aprocname");
    }

    @Test
    public void testGetNameFromStateWithFileNotExists() throws Exception {
        // Force new test instance because this test case work with data
        // which is initialized at instance creation
        procState = Mockito.mock(ProcState.class);
        when(procState.getName()).thenReturn("/this/file/should/not/exists");
        when(sigarProxy.getProcState(PID)).thenReturn(procState);
        processInfo = new ProcessInfo(PID, sigarProxy);
        assertEquals(processInfo.getName(), "/this/file/should/not/exists");
    }

    @Test
    public void testGetNameFromStateWithFileExists() throws Exception {
        // Force new test instance because this test case work with data
        // which is initialized at instance creation
        File tmpFile = File.createTempFile("ProcessInfoTest-", ".tmp");
        tmpFile.deleteOnExit();
        procState = Mockito.mock(ProcState.class);
        when(procState.getName()).thenReturn(tmpFile.getAbsolutePath());
        when(sigarProxy.getProcState(PID)).thenReturn(procState);
        processInfo = new ProcessInfo(PID, sigarProxy);
        assertEquals(processInfo.getName(), tmpFile.getAbsolutePath());
    }

    @Test
    public void testGetBaseName() throws Exception {
        // Force new test instance because this test case work with data
        // which is initialized at instance creation
        File tmpFile = File.createTempFile("ProcessInfoTest-", ".tmp");
        tmpFile.deleteOnExit();
        procState = Mockito.mock(ProcState.class);
        when(procState.getName()).thenReturn(tmpFile.getAbsolutePath());
        when(sigarProxy.getProcState(PID)).thenReturn(procState);
        processInfo = new ProcessInfo(PID, sigarProxy);
        assertEquals(processInfo.getBaseName(), tmpFile.getName());
    }

    @Test
    public void testGetEmptyCommandLine() throws Exception {
        assertEquals(processInfo.getCommandLine(), new String[] {});
    }

    @Test
    public void testGetCommandLine() throws Exception {
        // Force new proxy and test instance because this test case work on process static data
        // which is initialized at instance creation
        sigarProxy = Mockito.mock(SigarProxy.class);
        String[] array = new String[] { "one", "two", "three" };
        when(sigarProxy.getProcArgs(PID)).thenReturn(array);
        processInfo = new ProcessInfo(PID, sigarProxy);
        assertEquals(processInfo.getCommandLine(), array);
    }

    @Test
    public void testGetEmptyEnvironmentVariables() throws Exception {
        assertEquals(processInfo.getEnvironmentVariables(), Collections.EMPTY_MAP);
    }

    @Test
    public void testGetEnvironmentVariables() throws Exception {
        // Force new test instance because this test case work with data
        // which is initialized at instance creation
        Map<String, String> env = new HashMap<String, String>();
        env.put("one", "1");
        env.put("two", "2");
        env.put("three", "3");
        when(sigarProxy.getProcEnv(PID)).thenReturn(env);
        processInfo = new ProcessInfo(PID, sigarProxy);
        assertEquals(processInfo.getEnvironmentVariables(), env);
    }

    @Test
    public void testGetEnvironmentVariableWithNullEnv() throws Exception {
        assertEquals(processInfo.getEnvironmentVariable("anyvar"), null);
    }

    @Test
    public void testGetEnvironmentVariable() throws Exception {
        // Force new test instance because this test case work with data
        // which is initialized at instance creation
        Map<String, String> env = new HashMap<String, String>();
        env.put("one", "1");
        env.put("two", "2");
        env.put("three", "3");
        when(sigarProxy.getProcEnv(PID)).thenReturn(env);
        processInfo = new ProcessInfo(PID, sigarProxy);
        assertEquals(processInfo.getEnvironmentVariable("two"), "2");
    }

    @Test
    public void testGetParentPidWithNullState() throws Exception {
        // Force new test instance because this test case work with data
        // which is initialized at instance creation
        when(sigarProxy.getProcState(PID)).thenThrow(new SigarException());
        // This is to avoid NPE in test instance error checking code
        when(sigarProxy.getProcList()).thenReturn(new long[] { PID });
        processInfo = new ProcessInfo(PID, sigarProxy);
        assertEquals(processInfo.getParentPid(), 0);
    }

    @Test
    public void testGetParentPid() {
        when(procState.getPpid()).thenReturn(Long.valueOf(10));
        assertEquals(processInfo.getParentPid(), 10);
    }

    @Test
    public void testGetState() throws Exception {
        assertTrue(procState == processInfo.getState());
        // Change procState
        procState = Mockito.mock(ProcState.class);
        when(sigarProxy.getProcState(PID)).thenReturn(procState);
        // Should not be same until refresh is called
        assertFalse(procState == processInfo.getState());
        processInfo.refresh();
        assertTrue(procState == processInfo.getState());
    }

    @Test
    public void testGetExecutable() throws Exception {
        assertTrue(procExe == processInfo.getExecutable());
        // Change procExe
        procExe = Mockito.mock(ProcExe.class);
        when(sigarProxy.getProcExe(PID)).thenReturn(procExe);
        // Should not be same until refresh is called
        assertFalse(procExe == processInfo.getExecutable());
        processInfo.refresh();
        assertTrue(procExe == processInfo.getExecutable());
    }

    @Test
    public void testGetTime() throws Exception {
        assertTrue(procTime == processInfo.getTime());
        // Change procTime
        procTime = Mockito.mock(ProcTime.class);
        when(sigarProxy.getProcTime(PID)).thenReturn(procTime);
        // Should not be same until refresh is called
        assertFalse(procTime == processInfo.getTime());
        processInfo.refresh();
        assertTrue(procTime == processInfo.getTime());
    }

    @Test
    public void testGetMemory() throws Exception {
        assertTrue(procMem == processInfo.getMemory());
        // Change procMem
        procMem = Mockito.mock(ProcMem.class);
        when(sigarProxy.getProcMem(PID)).thenReturn(procMem);
        // Should not be same until refresh is called
        assertFalse(procMem == processInfo.getMemory());
        processInfo.refresh();
        assertTrue(procMem == processInfo.getMemory());
    }

    @Test
    public void testGetCpu() throws Exception {
        assertTrue(procCpu == processInfo.getCpu());
        // Change procCpu
        procCpu = Mockito.mock(ProcCpu.class);
        when(sigarProxy.getProcCpu(PID)).thenReturn(procCpu);
        // Should not be same until refresh is called
        assertFalse(procCpu == processInfo.getCpu());
        processInfo.refresh();
        assertTrue(procCpu == processInfo.getCpu());
    }

    @Test
    public void testGetFileDescriptor() throws Exception {
        assertTrue(procFd == processInfo.getFileDescriptor());
        // Change procFd
        procFd = Mockito.mock(ProcFd.class);
        when(sigarProxy.getProcFd(PID)).thenReturn(procFd);
        // Should not be same until refresh is called
        assertFalse(procFd == processInfo.getFileDescriptor());
        processInfo.refresh();
        assertTrue(procFd == processInfo.getFileDescriptor());
    }

    @Test
    public void testGetCredentials() throws Exception {
        assertTrue(procCred == processInfo.getCredentials());
        // Change procCred
        procCred = Mockito.mock(ProcCred.class);
        when(sigarProxy.getProcCred(PID)).thenReturn(procCred);
        // Should not be same until refresh is called
        assertFalse(procCred == processInfo.getCredentials());
        processInfo.refresh();
        assertTrue(procCred == processInfo.getCredentials());
    }

    @Test
    public void testGetCredentialsName() throws Exception {
        assertTrue(procCredName == processInfo.getCredentialsName());
        // Change procCred
        procCredName = Mockito.mock(ProcCredName.class);
        when(sigarProxy.getProcCredName(PID)).thenReturn(procCredName);
        // Should not be same until refresh is called
        assertFalse(procCredName == processInfo.getCredentialsName());
        processInfo.refresh();
        assertTrue(procCredName == processInfo.getCredentialsName());
    }

    @Test
    public void testGetCurrentWorkingDirectoryFailure() throws Exception {
        when(procExe.getCwd()).thenThrow(new RuntimeException());
        // This is to avoid NPE in test instance error checking code
        when(sigarProxy.getProcList()).thenReturn(new long[] { PID });
        assertNull(processInfo.getCurrentWorkingDirectory());
    }

    @Test
    public void testGetCurrentWorkingDirectory() throws Exception {
        when(procExe.getCwd()).thenReturn("adir");
        assertEquals(processInfo.getCurrentWorkingDirectory(), "adir");
    }

    @Test
    public void testIsRunning() {
        for (char state : new char[] { ProcState.RUN, ProcState.SLEEP, ProcState.IDLE }) {
            when(procState.getState()).thenReturn(state);
            assertTrue(processInfo.isRunning());
        }
        for (char state : new char[] { ProcState.ZOMBIE, ProcState.STOP }) {
            when(procState.getState()).thenReturn(state);
            assertFalse(processInfo.isRunning());
        }
    }

    @Test
    public void testGetParentProcess() {
        when(procState.getPpid()).thenReturn(Long.valueOf(10));
        assertEquals(processInfo.getParentProcess().getPid(), 10);
        when(procState.getPpid()).thenReturn(Long.valueOf(11));
        assertEquals(processInfo.getParentProcess().getPid(), 10, "Parent process should be cached");
    }

}
