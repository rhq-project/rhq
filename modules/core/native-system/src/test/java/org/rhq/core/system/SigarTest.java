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
import org.hyperic.sigar.Sigar;

/**
 * The purpose of this class is to provide a simple main class that can be run from the command line so we can send it
 * to the SIGAR project team members when we need to report a bug and they need a simple replication test case. All
 * tests that we replicate with this class needs to also have a corresponding unit test. When the bug is fixed, we can
 * clean out this class's main() for the next replication procedure - we won't lose the test because it will have been
 * duplicated somewhere in our unit test suite.
 *
 * @author John Mazzitelli
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
            ProcessInfo p = new ProcessInfo(pid);
            System.out.println("-->info:   " + p);
            System.out.println("   name:   " + p.getName());
            System.out.println("   base:   " + p.getBaseName());
            System.out.println("   pid:    " + p.getPid());
            System.out.println("   ppid:   " + p.getParentPid());
            System.out.println("   cmdlin: "
                + ((p.getCommandLine() != null) ? Arrays.asList(p.getCommandLine()) : "<null>"));
            System.out.println("   envvar: " + p.getEnvironmentVariables());

            System.out.println("   aggr:   " + p.getAggregateProcessTree());
            System.out.println("   exec:   " + p.getExecutable());
            System.out.println("   memory: " + p.getMemory());
            System.out.println("   cpu:    " + p.getCpu());
            System.out.println("   state:  " + p.getState());
            System.out.println("   time:   " + p.getTime());
            System.out.println();
        }
    }
}