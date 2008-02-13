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
package org.rhq.core.system.pquery;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.core.system.ProcessInfo;

/**
 * Tests PIQL querying.
 *
 * @author John Mazzitelli
 */
@Test
public class ProcessInfoQueryTest {
    private ProcessInfo p1 = buildProcessInfo(1, "/foo/bin/java.exe", "org.jboss.Main", "-b", "127.0.0.1");
    private ProcessInfo p2 = buildProcessInfo(2, "/bin/sh");
    private ProcessInfo p3 = buildProcessInfo(3, "/home/product/bin/exec", "-verbose", "port=1098");
    private ProcessInfo p4 = buildProcessInfo(4, 3, "/home/product/bin/exec", "--daemon");
    private ProcessInfo p5 = buildProcessInfo(5, 3, "/home/product/bin/exec", "--daemon=true");
    private ProcessInfo p6 = buildProcessInfo(6, "C:\\Program Files\\App\\runme.bat", "arg1", "arg2");
    private ProcessInfo p7 = buildProcessInfo(7, "/foo/bar/java.exe", "org.abc.Boo");
    private ProcessInfo p8 = buildProcessInfo(8, "/proc.bin", "-Dbind.address=192.168.0.1", "/bin/sh");

    private ProcessInfoQuery query;
    private List<ProcessInfo> results;

    /**
     * Test being able to match on pid itself.
     */
    public void testPIQLPid() {
        results = query.query("process|pid|match=1");
        assert results.size() == 1 : "should have been one child processes of pid 1: " + results;
        assertPidExists(1, results, "pid 1 should be here");

        results = query.query("process|pid|match=[267]");
        assert results.size() == 3 : "should have been three child processes of pid 2, 6 and 7: " + results;
        assertPidExists(2, results, "pid 2 should be here");
        assertPidExists(6, results, "pid 6 should be here");
        assertPidExists(7, results, "pid 7 should be here");
    }

    /**
     * Tests the optional parent qualifier.
     */
    public void testPIQLParentQualifier() {
        // find all processes with a name of "exec" that have parent processes with a port=1098 arg
        // notice we test the parent qualifier on an arg conditional
        results = query.query("process|basename|match=exec,arg|port|match|parent=1098");
        assert results.size() == 2 : "should have been two child processes of pid 3: " + results;
        assertPidExists(4, results, "pid 4 is an exec process with a parent that matches");
        assertPidExists(5, results, "pid 5 is an exec process with a parent that matches");

        results = query.query("process|basename|match=exec,arg|blah|nomatch|parent=1098");
        assert results.size() == 0 : "no process has a parent that has a blah attribute: " + results;

        // notice we test the parent qualifier on a process conditional
        results = query.query("process|basename|match|parent=exec");
        assert results.size() == 2 : "there are two processes with parent process having name of 'exec'" + results;
        assertPidExists(4, results, "pid 4 is an exec process with a parent that matches");
        assertPidExists(5, results, "pid 5 is an exec process with a parent that matches");

        results = query.query("process|basename|match|parent=.*java.*");
        assert results.size() == 0 : "no processes with parent process having name contains 'java'" + results;

        results = query.query("process|basename|nomatch|parent=exec");
        assert results.size() == (query.getProcesses().size() - 2) : "should have all but the two child processes of pid 3"
            + results;
        assertPidDoesNotExist(4, results, "pid 4 has a parent with basename 'exec'; should not match");
        assertPidDoesNotExist(5, results, "pid 5 has a parent with basename 'exec'; should not match");

        results = query.query("arg|-verbose|match|parent=.*");
        assert results.size() == 2 : "there are two processes with parent process having -verbose arg" + results;
        assertPidExists(4, results, "pid 4 is an exec process with parent having -verbose");
        assertPidExists(5, results, "pid 5 is an exec process with parent having -verbose");

        results = query.query("arg|nowhere|match|parent=.*");
        assert results.size() == 0 : "no process has a parent with nowhere arg" + results;
    }

    /**
     * Tests querying technique that is kind of like the one used when we need to autodiscovery Apache.
     */
    public void testPIQLApacheUseCase() {
        ProcessInfo p100 = buildProcessInfo(100, "$1", "--dollararg");
        ProcessInfo p101 = buildProcessInfo(101, 100, "/dollar", "--dollararg");
        ProcessInfo p102 = buildProcessInfo(102, 100, "/dollar", "--dollararg");
        ProcessInfo p200 = buildProcessInfo(200, "/dollar", "--dollararg");
        ProcessInfo p201 = buildProcessInfo(201, 200, "/dollar", "--dollararg");
        ProcessInfo p202 = buildProcessInfo(202, 200, "/dollar", "--dollararg");

        List<ProcessInfo> processes = query.getProcesses();
        processes.add(p100);
        processes.add(p101);
        processes.add(p102);
        processes.add(p200);
        processes.add(p201);
        processes.add(p202);
        query = new ProcessInfoQuery(processes);

        results = query.query("arg|--dollararg|match=.*");
        assert results.size() == 6 : "six processes should have an arg of '--dollararg': " + results;
        assertPidExists(100, results, "");
        assertPidExists(101, results, "");
        assertPidExists(102, results, "");
        assertPidExists(200, results, "");
        assertPidExists(201, results, "");
        assertPidExists(202, results, "");

        // find all processes with an arg --dollararg but whose parent does not have the same name as itself
        results = query.query("arg|--dollararg|match=.*,process|name|nomatch|parent=/dollar");
        assert results.size() == 4 : "four processes should not have a parent of the same name: " + results;
        assertPidExists(100, results, "pid 100 is a process with arg '--dollararg' that has no parent of the same name");
        assertPidExists(101, results, "pid 101 is a process with arg '--dollararg' that has no parent of the same name");
        assertPidExists(102, results, "pid 102 is a process with arg '--dollararg' that has no parent of the same name");
        assertPidExists(200, results, "pid 200 is a process with arg '--dollararg' that has no parent of the same name");
    }

    /**
     * Test the arg conditional and specify the arg wildcard.
     */
    public void testPIQLArgAll() {
        results = query.query("arg|*|match=/bin/sh");
        assert results.size() == 2 : "pid 2 and 8 have /bin/sh as command line args";
        assertPidExists(2, results, "pid 2's #0 arg has /bin/sh");
        assertPidExists(8, results, "pid 8's #2 arg has /bin/sh");

        results = query.query("arg|*|match=.*daemon.*");
        assert results.size() == 2 : "pid 4 and 5 have an arg with the word daemon in it";
        assertPidExists(4, results, "");
        assertPidExists(5, results, "");

        // just show that we can have both an arg and process conditional in our piql criterias
        results = query.query("arg|*|match=.*daemon.*,process|basename|match=exec");
        assert results.size() == 2 : "pid 4 and 5 have an arg with the word daemon in it and basename is exec";
        assertPidExists(4, results, "");
        assertPidExists(5, results, "");
    }

    /**
     * Test the arg conditional and specify a specific argument number.
     */
    public void testPIQLArgNumber() {
        results = query.query("arg|1|match=-verbose");
        assert results.size() == 1 : "pid 3 should have been found";
        assertPidExists(3, results, "pid 3 is the only one with -verbose");

        results = query.query("arg|0|match=.*java.*");
        assert results.size() == 2 : "pid 1 and 7 should have been found";
        assertPidExists(1, results, "pid 1 has java in its arg #0");
        assertPidExists(7, results, "pid 7 has java in its arg #0");

        results = query.query("arg|2|match=arg2");
        assert results.size() == 1 : "pid 6 should have been found";
        assertPidExists(6, results, "pid 6 arg #2 is 'arg2'");

        results = query.query("arg|2|nomatch=arg2");
        assert results.size() == 3 : "1,3,8 all have an arg#2 that != arg2: " + results;
        assertPidExists(1, results, "pid 1 arg #2 is not 'arg2' and should have matched");
        assertPidExists(3, results, "pid 3 arg #2 is not 'arg2' and should have matched");
        assertPidExists(8, results, "pid 8 arg #2 is not 'arg2' and should have matched");

        results = query.query("arg|7|nomatch=sploingblat");
        assert results.size() == 0 : "no processes have 7 arguments";

        results = query.query("arg|-1|match=/bin/sh");
        assert results.size() == 2 : "pid 2 and 8 has /bin/sh as its last argument";
        assertPidExists(2, results, "pid 2 has /bin/sh as its last argument");
        assertPidExists(8, results, "pid 8 has /bin/sh as its last argument");
    }

    /**
     * Test the arg conditional and specify an argument name.
     */
    public void testPIQLArgName() {
        results = query.query("arg|-b|match=127\\.0\\.0\\.1");
        assert results.size() == 1 : "pid 1 has a -b argument whose value is 127.0.0.1";
        assertPidExists(1, results, "pid 1 has -b 127.0.0.1");

        results = query.query("arg|-b|match=.*\\.0\\.0\\..*");
        assert results.size() == 1 : "pid 1 has a -b argument whose value contains .0.0.";
        assertPidExists(1, results, "pid 1 contains .0.0.");

        results = query.query("arg|-verbose|match=.*");
        assert results.size() == 1 : "pid 3 has a -verbose argument";
        assertPidExists(3, results, "");

        results = query.query("arg|-Dbind.address|match=.*");
        assert results.size() == 1 : "pid 8 has a -Dbind.address argument";
        assertPidExists(8, results, "");

        results = query.query("arg|-Dbind.address|match=192\\.168\\.0\\.1");
        assert results.size() == 1 : "pid 8 has a -Dbind.address argument equal to 192.168.0.1";
        assertPidExists(8, results, "");

        // here we want to explicitly make sure we can find the existence of an arg if its the last one
        results = query.query("arg|--daemon|match=.+");
        assert results.size() == 1 : "pid 4 and 5 have a --daemon arg but 4 has an empty value which doesn't match: "
            + results;
        assertPidExists(5, results, "");

        results = query.query("arg|--daemon|match=.*");
        assert results.size() == 2 : "pid 4 and 5 have a --daemon arg";
        assertPidExists(4, results, "");
        assertPidExists(5, results, "");

        results = query.query("arg|--daemon|match=true");
        assert results.size() == 1 : "pid 5 has a --daemon=true arg: " + results;
        assertPidExists(5, results, "");
    }

    /**
     * Test multiple criteria.
     */
    public void testPIQLMultipleCriteria() {
        results = query.query("process|basename|match=java.exe");
        assertPidExists(1, results, "pid 1 should match");
        assertPidExists(7, results, "pid 7 should match");
        assert results.size() == 2 : results;

        results = query.query("process|name|match=.*bar.*");
        assertPidExists(7, results, "only pid 7 should match");
        assert results.size() == 1 : results;

        results = query.query("process|basename|match=java.exe,process|name|match=.*bar.*");
        assertPidExists(7, results, "only pid 7 matches both criteria");
        assert results.size() == 1 : results;

        results = query.query("process|basename|match=java.exe,process|name|match=.*splatboing.*");
        assert results.size() == 0 : "Nothing should have matched all criteria" + results;

        results = query
            .query("process|basename|match=.*ex.*,process|basename|match=java.exe,process|name|match=.*foo.*,process|name|nomatch=dummy");
        assertPidExists(1, results, "pid 1 should match");
        assertPidExists(7, results, "pid 7 should match");
        assert results.size() == 2 : "pids 1 and 7 should have be the only ones to match" + results;
    }

    /**
     * Test PID files.
     *
     * @throws Exception
     */
    public void testPIQLPidfile() throws Exception {
        File pidfile = File.createTempFile("test", ".pid");
        try {
            FileOutputStream fos = new FileOutputStream(pidfile);
            fos.write("3".getBytes());
            fos.flush();
            fos.close();

            results = query.query("process|pidfile|match=" + pidfile.getCanonicalPath());
            assert results.size() == 1 : results;
            assertPidExists(3, results, "pidfile had pid #3 in it and should have matched pid 3");

            results = query.query("process|pidfile|nomatch=" + pidfile.getCanonicalPath());
            assert results.size() == (query.getProcesses().size() - 1) : "should match all but pid 3:" + results;
            assertPidDoesNotExist(3, results, "pidfile had pid #3 in it so .ne should not have matched pid 3");

            results = query.query("process|pidfile|match|parent=" + pidfile.getCanonicalPath());
            assert results.size() == 2 : "there are two child procs of parent process found in pidfile:" + results;
            assertPidExists(4, results, "");
            assertPidExists(5, results, "");
        } finally {
            pidfile.delete();
        }
    }

    /**
     * Test regular expressions.
     */
    public void testPIQLProcessNameRegularExpression() {
        results = query.query("process|name|match=^C:.*");
        assertPidExists(6, results, "pid 6 should match the regular expression");
        assert results.size() == 1 : results;

        results = query.query("process|name|match=.*(product|java).*");
        assertPidExists(1, results, "name should match the regular expression");
        assertPidExists(3, results, "name should match the regular expression");
        assertPidExists(4, results, "name should match the regular expression");
        assertPidExists(5, results, "name should match the regular expression");
        assertPidExists(7, results, "name should match the regular expression");
        assert results.size() == 5 : results;

        results = query.query("process|basename|match=.*(product|java).*");
        assertPidExists(1, results, "basename should match the regular expression");
        assertPidExists(7, results, "basename should match the regular expression");
        assert results.size() == 2 : results;
    }

    /**
     * Test contains value.
     */
    public void testPIQLProcessNameContainsValue() {
        results = query.query("process|name|match=.*product.*");
        assertPidExists(3, results, "pid 3 name should contain the value 'product'");
        assertPidExists(4, results, "pid 4 name should contain the value 'product'");
        assertPidExists(5, results, "pid 5 name should contain the value 'product'");
        assert results.size() == 3 : results;

        results = query.query("process|basename|match=.*product.*");
        assert results.size() == 0 : results;
    }

    /**
     * Test not equals.
     */
    public void testPIQLProcessNameNotEqual() {
        results = query.query("process|name|nomatch=/bin/sh");
        assertPidDoesNotExist(2, results, "pid 2 name is /bin/sh so it is !ne");
        assertPidExists(1, results, "");
        assertPidExists(3, results, "");
        assertPidExists(4, results, "");
        assertPidExists(5, results, "");
        assertPidExists(6, results, "");
        assertPidExists(7, results, "");

        results = query.query("process|basename|nomatch=/bin/sh");
        assertPidExists(2, results, "pid 2 basename is sh, name is not /bin/sh");
        assertPidExists(1, results, "");
        assertPidExists(3, results, "");
        assertPidExists(4, results, "");
        assertPidExists(5, results, "");
        assertPidExists(6, results, "");
        assertPidExists(7, results, "");
    }

    /**
     * Test equals.
     */
    public void testPIQLProcessNameEqual() {
        results = query.query("process|name|match=/bin/sh");
        assertPidExists(2, results, "pid 2 should have name /bin/sh");
        assert results.size() == 1 : results;

        results = query.query("process|basename|match=/bin/sh");
        assertPidDoesNotExist(2, results, "basename is only sh, not /bin/sh");
        assert results.size() == 0 : results;

        results = query.query("process|basename|match=java.exe");
        assertPidExists(1, results, "basename should be java.exe");
        assertPidExists(7, results, "basename should be java.exe");
        assert results.size() == 2 : results;
    }

    /**
     * Test ends with.
     */
    public void testPIQLProcessNameEndsWith() {
        results = query.query("process|name|match=.*h$");
        assertPidExists(2, results, "pid 2 name ends with 'h'");
        assert results.size() == 1 : results;

        results = query.query("process|basename|match=.*h$");
        assertPidExists(2, results, "pid 2 basename ends with 'h'");
        assert results.size() == 1 : results;

        results = query.query("process|name|match=.*n/sh$");
        assertPidExists(2, results, "pid 2 name ends with 'n/sh'");
        assert results.size() == 1 : results;

        results = query.query("process|basename|match=.*n/sh$");
        assertPidDoesNotExist(2, results, "pid 2 basename is only sh so it doesn't end with 'n/sh'");
        assert results.size() == 0 : results;
    }

    /**
     * Test starts with.
     */
    public void testPIQLProcessNameStartsWith() {
        results = query.query("process|name|match=^java.exe.*");
        assertPidDoesNotExist(1, results, "pid 1 name doesn't start with java.exe");
        assertPidDoesNotExist(7, results, "pid 7 name doesn't start with java.exe");
        assert results.size() == 0 : results;

        results = query.query("process|basename|match=^java.exe.*");
        assertPidExists(1, results, "pid 1 basename starts with java.exe");
        assertPidExists(7, results, "pid 7 basename starts with java.exe");
        assert results.size() == 2 : results;

        results = query.query("process|name|match=^/foo.*");
        assertPidExists(1, results, "pid 1 basename starts with /foo");
        assertPidExists(7, results, "pid 7 basename starts with /foo");
        assert results.size() == 2 : results;

        results = query.query("process|basename|match=^/foo.*");
        assert results.size() == 0 : results;

        results = query.query("process|name|match=^/foo/bin.*");
        assertPidExists(1, results, "pid 1 basename starts with /foo/bin");
        assertPidDoesNotExist(7, results, "pid 7 basename does not start with /foo/bin");
        assert results.size() == 1 : results;
    }

    /**
     * Tests the ability to change separators.
     */
    public void testPIQLSeparators() {
        // sanity check - make sure this works, then use a different separator in our next query
        results = query.query("process|basename|match=exec,arg|port|match|parent=1098");
        assert results.size() == 2 : "should have been two child processes of pid 3: " + results;
        assertPidExists(4, results, "pid 4 is an exec process with a parent that matches");
        assertPidExists(5, results, "pid 5 is an exec process with a parent that matches");

        results = query.query(".process.basename.match=exec,.arg.port.match.parent=1098");
        assert results.size() == 2 : "should have been two child processes of pid 3: " + results;
        assertPidExists(4, results, "pid 4 is an exec process with a parent that matches");
        assertPidExists(5, results, "pid 5 is an exec process with a parent that matches");
    }

    public void testSimpleProcessInfo() {
        // sanity checking - making sure our test ProcessInfos are what we think they are

        assert query.getProcesses().size() == 8;

        assert p1.getPid() == 1;
        assert p1.getParentPid() == 0;
        assert p1.getName().equals("/foo/bin/java.exe");
        assert p1.getBaseName().equals("java.exe");
        assert p1.getCommandLine().length == 4;
        assert p1.getCommandLine()[0].equals(p1.getName());
        assert p1.getCommandLine()[1].equals("org.jboss.Main");
        assert p1.getCommandLine()[2].equals("-b");
        assert p1.getCommandLine()[3].equals("127.0.0.1");

        assert p2.getPid() == 2;
        assert p2.getParentPid() == 0;
        assert p2.getName().equals("/bin/sh");
        assert p2.getBaseName().equals("sh");
        assert p2.getCommandLine().length == 1;
        assert p2.getCommandLine()[0].equals(p2.getName());

        assert p3.getPid() == 3;
        assert p3.getParentPid() == 0;
        assert p3.getName().equals("/home/product/bin/exec");
        assert p3.getBaseName().equals("exec");
        assert p3.getCommandLine().length == 3;
        assert p3.getCommandLine()[0].equals(p3.getName());
        assert p3.getCommandLine()[1].equals("-verbose");
        assert p3.getCommandLine()[2].equals("port=1098");

        assert p4.getPid() == 4;
        assert p4.getParentPid() == 3;
        assert p4.getName().equals("/home/product/bin/exec");
        assert p4.getBaseName().equals("exec");
        assert p4.getCommandLine().length == 2;
        assert p4.getCommandLine()[0].equals(p4.getName());
        assert p4.getCommandLine()[1].equals("--daemon");

        assert p5.getPid() == 5;
        assert p5.getParentPid() == 3;
        assert p5.getName().equals("/home/product/bin/exec");
        assert p5.getBaseName().equals("exec");
        assert p5.getCommandLine().length == 2;
        assert p5.getCommandLine()[0].equals(p5.getName());
        assert p5.getCommandLine()[1].equals("--daemon=true");

        assert p6.getPid() == 6;
        assert p6.getParentPid() == 0;
        assert p6.getName().equals("C:\\Program Files\\App\\runme.bat");
        assert p6.getBaseName().equals("runme.bat");
        assert p6.getCommandLine().length == 3;
        assert p6.getCommandLine()[0].equals(p6.getName());
        assert p6.getCommandLine()[1].equals("arg1");
        assert p6.getCommandLine()[2].equals("arg2");

        assert p7.getPid() == 7;
        assert p7.getParentPid() == 0;
        assert p7.getName().equals("/foo/bar/java.exe");
        assert p7.getBaseName().equals("java.exe");
        assert p7.getCommandLine().length == 2;
        assert p7.getCommandLine()[0].equals(p7.getName());
        assert p7.getCommandLine()[1].equals("org.abc.Boo");

        assert p8.getPid() == 8;
        assert p8.getParentPid() == 0;
        assert p8.getName().equals("/proc.bin");
        assert p8.getBaseName().equals("proc.bin");
        assert p8.getCommandLine().length == 3;
        assert p8.getCommandLine()[0].equals(p8.getName());
        assert p8.getCommandLine()[1].equals("-Dbind.address=192.168.0.1");
        assert p8.getCommandLine()[2].equals("/bin/sh");
    }

    @BeforeMethod
    protected void setup() {
        this.query = new ProcessInfoQuery(buildTestProcesses());
        this.results = null;
    }

    private void assertPidExists(int pid, List<ProcessInfo> queryResults, String error) {
        for (ProcessInfo process : queryResults) {
            if (process.getPid() == pid) {
                return;
            }
        }

        assert false : "pid [" + pid + "] did not exist in query results [" + error + "]: queryResults=" + queryResults;
    }

    private void assertPidDoesNotExist(int pid, List<ProcessInfo> queryResults, String error) {
        for (ProcessInfo process : queryResults) {
            if (process.getPid() == pid) {
                assert false : "pid [" + pid + "] exists in query results but should not have [" + error
                    + "]: queryResults=" + queryResults;
            }
        }

        return;
    }

    private List<ProcessInfo> buildTestProcesses() {
        ArrayList<ProcessInfo> list = new ArrayList<ProcessInfo>();
        list.add(p1);
        list.add(p2);
        list.add(p3);
        list.add(p4);
        list.add(p5);
        list.add(p6);
        list.add(p7);
        list.add(p8);

        return list;
    }

    private ProcessInfo buildProcessInfo(long pid, long ppid, String... args) {
        int lastSlash = args[0].lastIndexOf('/');
        if (lastSlash == -1) {
            lastSlash = args[0].lastIndexOf('\\');
        }

        return new MockProcessInfo(pid, args[0], args[0].substring(lastSlash + 1), args, ppid);
    }

    private ProcessInfo buildProcessInfo(int pid, String... args) {
        return buildProcessInfo(pid, 0, args);
    }

    private class MockProcessInfo extends ProcessInfo {
        private long mockPid;
        private String mockName;
        private String mockBaseName;
        private String[] mockCommandLine;
        private long mockPpid;

        public MockProcessInfo(long pid, String name, String baseName, String[] commandLine, long ppid) {
            this.mockPid = pid;
            this.mockName = name;
            this.mockBaseName = baseName;
            this.mockCommandLine = commandLine;
            this.mockPpid = ppid;
        }

        @Override
        public long getPid() {
            return this.mockPid;
        }

        @Override
        public String getName() {
            return this.mockName;
        }

        @Override
        public String getBaseName() {
            return this.mockBaseName;
        }

        @Override
        public String[] getCommandLine() {
            return this.mockCommandLine;
        }

        @Override
        public long getParentPid() {
            return this.mockPpid;
        }

        @Override
        public void refresh() {
            // do nothing
        }
    }
}