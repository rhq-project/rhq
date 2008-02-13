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
package org.rhq.core.util.exec;

import java.io.ByteArrayOutputStream;
import java.io.File;
import org.testng.annotations.Test;

/**
 * Tests executing processes.
 *
 * @author John Mazzitelli
 */
@Test
public class ProcessExecTest {
    public void testProcessExecOutputStream() {
        int tries = 0;

        while (true) {
            tries++;
            ProcessToStart start = new ProcessToStart();

            setupProgram(start);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            start.setOutputStream(baos);
            start.setCaptureOutput(Boolean.TRUE);
            start.setWaitForExit(Long.valueOf(30000));

            ProcessExecutor exec = new ProcessExecutor();
            ProcessExecutorResults results = exec.execute(start);
            assert results.getError() == null : "Should not have failed: " + results;
            assert results.getExitCode() != null : "Should have had exit code: " + results;

            // there are some times when we can't get the output - see comments in ProcessExecutor.startProgram
            // if we failed to get the output this time, let's try again.  This is just allowing that rare
            // condition to occur in our test - I know of no way via the Java API to avoid it, so let's not
            // fail our test just because it happened once (but do fail if we can't get the output after so many tries)
            byte[] output = baos.toByteArray();
            if (output.length > 0) {
                return; // we did get output so everything succeeded! we can pass the test now and just return
            }

            if (tries >= 3) {
                assert false : "Should have had some output: " + results;
            }
        }
    }

    public void testProcessExecNoWait() {
        ProcessToStart start = new ProcessToStart();

        setupProgram(start);

        ProcessExecutor exec = new ProcessExecutor();
        ProcessExecutorResults results = exec.execute(start);
        assert results.getError() == null : "Should not have failed: " + results;
        assert results.getExitCode() == null : "Didn't wait, should not have had exit code: " + results;

        start.setWaitForExit(Long.valueOf(0)); // explicitly tell it not to wait (the same as if leaving it null)
        results = exec.execute(start);
        assert results.getError() == null : "Should not have failed: " + results;
        assert results.getExitCode() == null : "Didn't wait, should not have had exit code: " + results;
    }

    public void testProcessExec() {
        ProcessToStart start = new ProcessToStart();

        setupProgram(start);

        start.setWaitForExit(Long.valueOf(30000));

        ProcessExecutor exec = new ProcessExecutor();
        ProcessExecutorResults results = exec.execute(start);
        assert results.getError() == null : "Should not have failed - check the error, this test might not be bullet proof: "
            + results;
        assert results.getExitCode() != null : "Missing exit code - check the error, this test might not be bullet proof: "
            + results;
    }

    /**
     * Picks a program that can be executed on the test platform. This is not fool proof, might need to tweek this in
     * case, for example, some platforms do not have "/bin/ls".
     *
     * @param start
     */
    private void setupProgram(ProcessToStart start) {
        // just pick some short-lived executable
        if (File.separatorChar == '\\') {
            start.setProgramDirectory("C:\\WINDOWS\\system32");
            start.setProgramExecutable("find.exe");
            start.setArguments(new String[] { "/C", "\"yaddayadda\"", "C:\\WINDOWS\\WIN.INI" });
        } else {
            start.setProgramDirectory("/bin");
            start.setProgramExecutable("ls");
            start.setArguments(new String[] { "/bin" });
        }
    }
}