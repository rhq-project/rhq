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
            throw new IllegalStateException("Cannot initialize SIGAR");
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

            ProcExe exe = sigar.getProcExe(pid);
            printNow("   exe:    " + exe.toMap());

            ProcState state = sigar.getProcState(pid);
            printNow("   state:  " + state.toMap());

            List<String> args = Arrays.asList(sigar.getProcArgs(pid));
            printNow("   args:   " + args);

            Map env = sigar.getProcEnv(pid);
            printNow("   env:    " + env);

            ProcCpu cpu = sigar.getProcCpu(pid);
            printNow("   cpu:    " + cpu.toMap());

            ProcCred cred = sigar.getProcCred(pid);
            printNow("   cred:   " + cred.toMap());

            ProcFd fd = sigar.getProcFd(pid);
            printNow("   fd:     " + fd.toMap());

            ProcMem mem = sigar.getProcMem(pid);
            printNow("   mem:    " + mem.toMap());

            List modules = sigar.getProcModules(pid);
            printNow("   modules:" + modules);

            ProcStat stat = sigar.getProcStat();
            printNow("   stat:   " + stat.toMap());

            ProcTime time = sigar.getProcTime(pid);
            printNow("   time:   " + time.toMap());

            printNow("");
        }
    }

    private static void printNow(String s) {
        System.out.println(s);
        System.out.flush();
    }
}