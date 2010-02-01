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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hyperic.sigar.*;

 /**
 * The purpose of this class is to provide a simple main class that can be run from the command line so we can send it
 * to the SIGAR project team members when we need to report a bug and they need a simple replication test case. All
 * tests that we replicate with this class needs to also have a corresponding unit test. When the bug is fixed, we can
 * clean out this class's main() for the next replication procedure - we won't lose the test because it will have been
 * duplicated somewhere in our unit test suite. In order to make this class as simple as possible for others to compile
 * and run, it has no dependencies on other RHQ classes.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class SigarTest {
    static {
        try {
            Sigar.load();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize SIGAR.", e);
        }
    }

    public static void main(String[] args) throws Exception {
        testSigarProcesses();
    }

    public static void testSigarProcesses() throws Exception {
        Sigar sigar = new Sigar();
        long[] pids = sigar.getProcList();
        for (long pid : pids) {
            printNow("*** Retrieving process info for PID [" + pid + "]...");

            SigarException sigarException = null;

            ProcExe exe = null;
            try {
                exe = sigar.getProcExe(pid);
            } catch (SigarException e) {
                sigarException = e;
            }
            printNow("   exe:    " + ((exe != null) ? exe.toMap() : ("<UNKNOWN: " + sigarException + ">")));

            ProcState state = null;
            try {
                state = sigar.getProcState(pid);
            } catch (SigarException e) {
                sigarException = e;
            }
            printNow("   state:  " + ((state != null) ? state.toMap() : ("<UNKNOWN: " + sigarException + ">")));

            List<String> args = null;
            try {
                args = Arrays.asList(sigar.getProcArgs(pid));
            } catch (SigarException e) {
                sigarException = e;
            }
            printNow("   args:   " + ((args != null) ? args : ("<UNKNOWN: " + sigarException + ">")));

            Map env = null;
            try {
                env = sigar.getProcEnv(pid);
            } catch (SigarException e) {
                sigarException = e;
            }
            printNow("   env:    " + ((env != null) ? env : ("<UNKNOWN: " + sigarException + ">")));

            ProcCpu cpu = null;
            try {
                cpu = sigar.getProcCpu(pid);
            } catch (SigarException e) {
                sigarException = e;
            }
            printNow("   cpu:    " + ((cpu != null) ? cpu.toMap() : ("<UNKNOWN: " + sigarException + ">")));

            ProcCred cred = null;
            try {
                cred = sigar.getProcCred(pid);
            } catch (SigarException e) {
                sigarException = e;
            }
            printNow("   cred:   " + ((cred != null) ? cred.toMap() : ("<UNKNOWN: " + sigarException + ">")));

            ProcFd fd = null;
            try {
                fd = sigar.getProcFd(pid);
            } catch (SigarException e) {
                sigarException = e;
            }
            printNow("   fd:     " + ((fd != null) ? fd.toMap() : ("<UNKNOWN: " + sigarException + ">")));

            ProcMem mem = null;
            try {
                mem = sigar.getProcMem(pid);
            } catch (SigarException e) {
                sigarException = e;
            }
            printNow("   mem:    " + ((mem != null) ? mem.toMap() : ("<UNKNOWN: " + sigarException + ">")));

            List modules = null;
            try {
                modules = sigar.getProcModules(pid);
            } catch (SigarException e) {
                sigarException = e;
            }
            printNow("   modules:" + ((modules != null) ? modules : ("<UNKNOWN: " + sigarException + ">")));

            ProcStat stat = null;
            try {
                stat = sigar.getProcStat();
            } catch (SigarException e) {
                sigarException = e;
            }
            printNow("   stat:   " + ((stat != null) ? stat.toMap() : ("<UNKNOWN: " + sigarException + ">")));

            ProcTime time = null;
            try {
                time = sigar.getProcTime(pid);
            } catch (SigarException e) {
                sigarException = e;
            }
            printNow("   time:   " + ((time != null) ? time.toMap() : ("<UNKNOWN: " + sigarException + ">")));

            printNow("");
        }
    }

    private static void printNow(String s) {
        System.out.println(s);
        System.out.flush();
    }
}